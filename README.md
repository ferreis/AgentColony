# Regras e Mecânicas Atuais do Colony System

Este documento descreve as regras que estão realmente implementadas no código atual em src/com/colony.

## 0. Como Compilar e Executar

Comandos de apoio ao desenvolvimento (conforme Command.txt):

1. Compilar o projeto:

```bash
mkdir -p bin && javac -cp "lib/*:src" -d bin $(find src -name "*.java")
```

2. Executar o projeto:

```bash
java -cp "lib/*:bin" com.colony.Main
```

## 1. Arquitetura Multiagente

Esta seção descreve a arquitetura do sistema baseada em JADE, detalhando cada agente, o ambiente compartilhado, sensores, atuadores, comportamentos e o protocolo de comunicação ACL.

### 1.1 Topologia e bootstrap do sistema

O sistema inicializa um container JADE principal e sobe os agentes nesta ordem: Manager, Analyst, GUI, Wildlife e Workers iniciais.

```java
Runtime rt = Runtime.instance();
Profile p = new ProfileImpl(null, 1200, null);
AgentContainer mainContainer = rt.createMainContainer(p);

mainContainer.createNewAgent("manager", ManagerAgent.class.getName(), new Object[] { colonyMap, resources }).start();
mainContainer.createNewAgent("analyst", AnalystAgent.class.getName(), new Object[] { colonyMap, resources }).start();
mainContainer.createNewAgent("gui", GuiAgent.class.getName(), new Object[] { colonyMap, resources }).start();
mainContainer.createNewAgent("wildlife", WildlifeAgent.class.getName(), new Object[] { colonyMap }).start();
```

### 1.2 Ambiente compartilhado e propriedades

Todos os agentes operam sobre um ambiente comum composto por mapa, recursos e velocidade de simulação.

Propriedades principais do ambiente:

- `ColonyMap`: grade 2D (`WIDTH=200`, `HEIGHT=200`), tiles, construções, posições de NPC, vínculo NPC-casa e fauna.
- `ColonyBuilding`: posição (`x`,`y`), tipo, progresso de construção, dono e estado de custo de construção.
- `ColonyResources`: inventário global, capacidade total por recurso baseada em construções concluídas e operações de `add/consume`.
- `SimulationSpeed`: multiplicador global (`1x/2x/5x/10x`) e escalonamento temporal (`scaleDelay`).

```java
public static final int WIDTH = 200;
public static final int HEIGHT = 200;
private final TerrainTile[][] tiles;
private final List<ColonyBuilding> buildings;
private final Map<String, int[]> npcPositions;
private final List<Animal> animals = new CopyOnWriteArrayList<>();
```

```java
private final Map<String, Integer> resources;
private final Map<String, Integer> totalStorageCapacity;

public synchronized int add(String resource, int amount) { ... }
public synchronized boolean consume(String resource, int amount) { ... }
public synchronized boolean syncStorageCapacityFromMap(ColonyMap colonyMap) { ... }
```

```java
public static long scaleDelay(long baseMs) {
  long safeBase = Math.max(1L, baseMs);
  int multiplier = Math.max(1, getMultiplier());
  return Math.max(1L, safeBase / multiplier);
}
```

### 1.3 Infraestrutura JADE comum (`ColonyAgentBase`)

Todos os agentes de domínio estendem `ColonyAgentBase`, que centraliza descoberta e registro de serviços no DF do JADE.

Responsabilidades da base:

- Registrar serviço no DF (`registerService`).
- Resolver serviços por tipo com cache local e fallback por nome (`resolveService`).
- Desregistrar do DF no encerramento (`takeDown`).

```java
protected void registerService(String serviceType) { ... DFService.register(this, dfd); }

protected AID resolveService(String serviceType, String fallbackLocalName) { ... }

@Override
protected void takeDown() {
  DFService.deregister(this);
}
```

### 1.4 WorkerAgent

Objetivo:

- Executar tarefas operacionais (coleta, construção, produção, combate), manter estado fisiológico e reportar continuamente o próprio estado.

Sensores:

- Mensagens ACL: `ASSIGN_TASK`, confirmações `REGISTERED`.
- Estado do mapa: tiles, construções, casa atribuída e oficinas concluídas.
- Estado de recursos globais: disponibilidade e consumo para tarefas.
- Estado interno: `health`, `energy`, `fome`, `sede`, skills.

Atuadores:

- Movimentação no mapa e execução de ações de trabalho.
- Consumo/produção de recursos (`consume/add`).
- Envio de status, conclusão/rejeição de tarefa e eventos para Manager/Analyst/GUI.
- Auto-encerramento (`doDelete`) quando morre.

Comportamentos principais:

- `CyclicBehaviour` de mensageria e registro em Analyst/Manager.
- `CyclicBehaviour` de ação automática quando ocioso.
- `CyclicBehaviour` periódico de status (fome/sede/HP e relatório).
- `OneShotBehaviour` para execução da tarefa aceita.

```java
addBehaviour(new CyclicBehaviour() {
  public void action() {
    if (!regAnalyst) {
      AID analyst = resolveService("analyst", "analyst");
      if (analyst != null) {
        ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
        m.addReceiver(analyst);
        m.setContent("REGISTER_SKILL:" + npcName + ":" + primarySkill.getKey());
        send(m);
      }
    }

    if (!regManager) {
      AID manager = resolveService("manager", "manager");
      if (manager != null) {
        ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
        m.addReceiver(manager);
        m.setContent("REGISTER_WORKER");
        send(m);
      }
    }

    ACLMessage msg = receive();
    if (msg != null && msg.getContent().startsWith("ASSIGN_TASK:")) {
      String[] p = msg.getContent().split(":");
      acceptTask(p[1], p[2]);
    } else {
      block(SimulationSpeed.scaleDelay(MESSAGE_LOOP_BLOCK_MS));
    }
  }
});
```

```java
ACLMessage done = new ACLMessage(ACLMessage.INFORM);
done.addReceiver(manager);
done.setContent(messageType + taskId + "|" + reportX + "|" + reportY + "|" + progress + "|" + taskType);
send(done);
```

### 1.5 ManagerAgent

Objetivo:

- Coordenar a colônia: criação e distribuição de tarefas, gerenciamento de estoque/capacidade, expansão de infraestrutura e criação de novos workers.

Sensores:

- Mensagens de workers: `REGISTER_WORKER`, `WORKER_INFO`, `TASK_COMPLETE`, `TASK_TIMEOUT`, `TASK_REJECTED`.
- Mensagens do analista: `VERIFICATION_RESULT`, `TERRAIN_ANALYSIS`, `COLONY_ANALYSIS`, `DEADLINE_REPORT`, `RESOURCE_ABUNDANCE_RESULT`.
- Estado do ambiente: recursos, capacidade e construções concluídas/incompletas.

Atuadores:

- Criar tarefas e tarefas de construção.
- Atribuir tarefas aos workers por score.
- Ajustar estoque e requisitar produção.
- Criar novos agentes worker no container JADE.
- Publicar eventos de estado para GUI e dados para Analyst.

Comportamentos principais:

- `CyclicBehaviour` de caixa de mensagens (`receive -> handleMessage`).
- `CyclicBehaviour` de scheduler dinâmico com múltiplos ciclos temporizados.

```java
addBehaviour(new CyclicBehaviour() {
  public void action() {
    ACLMessage msg = receive();
    if (msg != null) handleMessage(msg);
    else block();
  }
});
```

```java
if (now >= nextDistributeTasksAt) distributeTasks();
if (now >= nextResourceAnalysisAt) requestResourceAbundanceAnalysis();
if (now >= nextEnsureWorkerAt) ensureWorkerForAvailableHouse();
if (now >= nextStockCheckAt) {
  ensureStockForWorkers();
  ensureWarehouseCapacityExpansion();
}
```

```java
ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
msg.addReceiver(new AID(best.name, AID.ISLOCALNAME));
msg.setContent("ASSIGN_TASK:" + task.id + ":" + task.type + extra + ":" + task.deadlineAt + ":" + task.urgency);
send(msg);
```

### 1.6 AnalystAgent

Objetivo:

- Auditar qualidade/consistência das tarefas, analisar capacidade da colônia e recomendar ações táticas ao gerente.

Sensores:

- Mensagens ACL: `VERIFY_TASK`, `REGISTER_SKILL`, `WORKER_INFO`, `TASK_QUEUE_REPORT`, `REQUEST_RESOURCE_ABUNDANCE`.
- Estado de mapa/recursos para inferir necessidades (casas, poço, oficinas, continuidade de obra).

Atuadores:

- Responder auditoria com `VERIFICATION_RESULT`.
- Emitir sinais para o gerente (`COLONY_ANALYSIS`, `TERRAIN_ANALYSIS`, `DEADLINE_REPORT`, `SCALE_ALERT`, `RESOURCE_ABUNDANCE_RESULT`).
- Publicar análises para GUI (`WORKER_ANALYSIS`, logs e terreno).

Comportamentos principais:

- `CyclicBehaviour` reativo de mensagens (auditoria e registro).
- `CyclicBehaviour` periódico para análise de força de trabalho, terreno e necessidades da colônia.

```java
if (content.startsWith("VERIFY_TASK:")) {
  String result = verifyTask(payload);
  ACLMessage reply = msg.createReply();
  reply.setContent("VERIFICATION_RESULT:" + result);
  send(reply);
}
```

```java
if (now >= nextPeriodicAnalysisAt) {
  sendToGui("WORKER_ANALYSIS:" + analyzeWorkforce());
  sendToManager("TERRAIN_ANALYSIS:" + analyzeTerrain());
  sendToManager("COLONY_ANALYSIS:" + analyzeColonyNeeds());
}
```

### 1.7 WildlifeAgent

Objetivo:

- Simular fauna (spawn, movimentação e decomposição de carcaças) e acionar atualização visual do mapa.

Sensores:

- Estado do mapa e da lista de animais.
- Tempo de simulação escalado (`SimulationSpeed.scaleDelay`).

Atuadores:

- Inserir/remover/mover animais no ambiente.
- Notificar GUI para repaint do mapa.

Comportamento principal:

- `CyclicBehaviour` com tick periódico de fauna.

```java
if (map.getAnimals().size() < MAX_WILD_ANIMALS && random.nextInt(3) == 0) {
  int x = random.nextInt(ColonyMap.WIDTH);
  int y = random.nextInt(ColonyMap.HEIGHT);
  if (!map.getTile(x, y).isBlocksMovement()) {
    boolean aggro = random.nextInt(4) == 0;
    String type = aggro ? "Lobo" : "Cervo";
    map.addAnimal(new Animal(x, y, aggro ? 50 : 20, aggro, type));
  }
}

for (Animal a : map.getAnimals()) {
  if (a.dead) {
    a.rotTimer--;
    if (a.rotTimer <= 0) map.removeAnimal(a);
  } else {
    int nx = a.x + random.nextInt(3) - 1;
    int ny = a.y + random.nextInt(3) - 1;
    if (map.inBounds(nx, ny) && !map.getTile(nx, ny).isBlocksMovement()) {
      a.x = nx;
      a.y = ny;
    }
  }
}

notifyGuiMapRefresh();
```

### 1.8 GuiAgent

Objetivo:

- Ser o adaptador entre mensagens ACL do sistema e atualizações visuais da interface Swing.

Sensores:

- Mensagens ACL de múltiplos agentes (`WORKER_STATUS`, `WORKER_DETAILS`, `NPC_POSITION`, `TASK_STATUS`, `BUILD_UPDATE`, `UPDATE_RESOURCES`, `LOG`, `WORKER_ANALYSIS`, `TERRAIN_ANALYSIS`).

Atuadores:

- Atualizar componentes da GUI (`ColonyGUI`) e disparar `repaint` de mapa.

Comportamento principal:

- `CyclicBehaviour` de roteamento de mensagens para métodos de UI.

```java
if (gui == null) {
  block();
  return;
}

ACLMessage msg = receive();
if (msg == null) {
  block();
  return;
}

String content = msg.getContent();
if (content.startsWith("WORKER_STATUS:")) gui.updateWorker(...);
else if (content.startsWith("BUILD_UPDATE:")) { gui.getMapPanel().repaint(); gui.updateResources(resources); }
else if (content.startsWith("TASK_STATUS:")) gui.updateTask(...);
```

### 1.9 Protocolo de comunicação ACL (mensagens principais)

Fluxos mais relevantes do sistema:

- Worker -> Analyst: `REGISTER_SKILL`, `WORKER_INFO`
- Worker -> Manager: `REGISTER_WORKER`, `WORKER_INFO`, `TASK_COMPLETE`, `TASK_TIMEOUT`, `TASK_REJECTED`
- Manager -> Worker: `ASSIGN_TASK`
- Manager -> Analyst: `VERIFY_TASK`, `TASK_QUEUE_REPORT`, `REQUEST_RESOURCE_ABUNDANCE`
- Analyst -> Manager: `VERIFICATION_RESULT`, `COLONY_ANALYSIS`, `TERRAIN_ANALYSIS`, `DEADLINE_REPORT`, `RESOURCE_ABUNDANCE_RESULT`, `SCALE_ALERT`
- Manager/Worker/Analyst/Wildlife -> GUI: `LOG`, `WORKER_STATUS`, `WORKER_DETAILS`, `TASK_STATUS`, `BUILD_UPDATE`, `UPDATE_RESOURCES`, `WORKER_ANALYSIS`, `TERRAIN_ANALYSIS`

## 2. Interface (GUI)

Abas da interface:

- Mapa
- Trabalhadores
- Tarefas
- Recursos
- Eventos

No painel de tarefas, atualmente existem 2 sub-abas:

- Sendo Feitas: recebe tarefas pendentes, atribuídas e em correção.
- Concluídas: recebe tarefas aprovadas/finalizadas.

Observações importantes:

- O rodapé mostra contagem consolidada de tarefas (ativas + concluídas).
- A aba Recursos exibe cada item no formato textual "quantidade atual / capacidade máxima".
- Não existe barra de progresso visual na aba Recursos.
- A atualização dos recursos permanece dinâmica (coleta, consumo e mudanças de capacidade por construção/remoção de armazéns).

## 3. Mapa, Construções e Navegação

### 3.1 Terreno e início da colônia

- Mapa 200x200 com geração procedural.
- Zona inicial central em piso (floor).
- O mapa já inicia com:
  - 1 Armazém (WAREHOUSE) com progresso em 100%.
  - 1 Poço (WELL) com progresso em 100%.

### 3.2 Regras de colocação de prédios

- Regra de espaçamento de 1 tile para toda construção.
- Não permite sobreposição de construções.
- O terreno do footprint da construção não pode bloquear movimento.

### 3.3 Paredes e portas

- Construções com telhado (hasRoof=true) viram estrutura fechada.
- A borda externa vira parede intransponível.
- A porta fica no centro da face inferior (sul), como único tile de passagem.

### 3.4 Estradas

- O tipo ROAD existe no modelo e tem custo de construção (pedra).
- No estado atual, não há rotina de planejamento/execução automática de estradas pelo gerente.
- Não existe bônus específico de velocidade/energia por andar em estrada implementado no WorkerAgent.

### 3.5 Pathfinding

- A navegação atual usa busca em grade por fila (BFS), com 8 direções.
- Evita tiles bloqueantes (parede, árvore, montanha, pedra etc.).
- Não é um algoritmo A\* no estado atual.

## 4. Regras dos Trabalhadores

### 4.1 Atributos e morte

Cada trabalhador mantém:

- Health (HP): 0 a 100
- Energy: 0 a 100
- Fome: 0 a 100
- Sede: 0 a 100

Se HP <= 0, fome <= 0 ou sede <= 0, o trabalhador morre e executa doDelete().

### 4.2 Degradação e regeneração periódica

No loop de status periódico:

- Sede cai 2 por tick.
- Fome cai à metade da taxa da sede (acumulada).
- HP regenera +2 por tick quando HP < 100.

Importante: não existe penalidade de acelerar fome/sede quando HP < 100.

### 4.3 Regras de descanso

- Se energia <= 30, entra em descanso até energia 100.
- Tenta dormir em casa (tile central da casa atribuída).
- Se chega em casa: recupera de 10 a 24 de energia por ciclo.
- Sem casa ou sem conseguir chegar: descansa no relento e recupera de 5 a 14.

### 4.4 Regras de fome e sede

- Com sede <= 40, prioriza beber no armazém.
- Com fome <= 40, prioriza comer no armazém.
- Se faltar água e existir poço concluído, pode coletar de 2 a 4 de água no poço.
- Se não conseguir atender necessidade básica, perde HP.

### 4.5 Aceitação/rejeição de tarefa

O trabalhador rejeita tarefa quando:

- Está descansando até energia cheia.
- HP < 100.
- Energia < 15.
- A tarefa exige oficina específica que ainda não existe concluída.

Se não tiver a skill da tarefa, aprende automaticamente no nível 1.

### 4.6 Execução de tarefa e recursos

- Tarefas de construção em alvo aumentam progresso com ganho baseado em skill e urgência.
- Antes da primeira etapa de construção, o custo de materiais é reservado/consumido.
- Se faltar recurso para o custo da construção, a tarefa é rejeitada.
- Pesca consome 1 vara de pesca por tentativa.
- Coleta (madeira/pedra/comida) gera rendimento escalonado por nível da skill.

### 4.7 Combate, caça e carcaça

- Wildlife faz spawn de Lobos (agressivos) e Cervos (passivos).
- Aproximação de lobo em raio curto já causa dano imediato no trabalhador.
- No fluxo de combate agressivo, trabalhador especializado (fighter/hunter/lenhador) toma menos dano e causa mais dano.
- Animais mortos viram carcaça (dead=true) e depois podem:
  - ser esfolados por worker (10 comida se caçador, 1 comida se não); ou
  - apodrecer e sumir quando o timer expira.

## 5. Regras do Gerente

### 5.1 Fila, estados, urgência e prazos

Estados de tarefa:

- pending
- assigned
- audit
- approved

Parâmetros atuais:

- Deadline padrão: 18s
- Deadline de construção: 26s
- Urgência máxima: 5

### 5.2 Distribuição por score

Para selecionar trabalhador:

- Ignora trabalhadores ocupados ou com energia < 30.
- Bônus por skill compatível (exata ou mesma categoria).
- Bônus por proximidade do alvo.
- Bônus por energia atual.
- Bônus adicional por urgência x nível prático.

### 5.3 Estoque e produção

- O gerente trabalha com perfil de estoque (balanceado/agressivo/econômico).
- Se recurso fica abaixo do mínimo, o gerente reforça automaticamente o estoque mínimo, respeitando a capacidade total disponível dos armazéns.
- Também cria tarefas de produção para buscar estoque-alvo (madeira, pedra/ferro, comida, vara de pesca), limitado pela capacidade total de armazenamento.

### 5.4 Capacidade de armazenamento por Armazém

Cada Armazém (WAREHOUSE) adiciona a seguinte capacidade máxima:

- Pedra: 250
- Madeira: 250
- Ferro: 200
- Comida: 200
- Água: 100
- Vara de pesca: 30

Regras operacionais:

- O fluxo operacional de armazenamento usa apenas Armazém (o tipo Depósito foi removido do fluxo).
- Casas (HOUSE) não armazenam recursos. Elas servem apenas como moradia/descanso e atribuição de dono.
- O inventário de recursos é global (ColonyResources), e a capacidade total depende da quantidade de armazéns concluídos.
- A capacidade total por recurso = capacidade por armazém x número de armazéns concluídos.
- A GUI reflete essa capacidade em tempo real na aba Recursos, no formato textual "atual / máximo".
- Se qualquer recurso atingir o limite máximo, ou se todos os recursos estiverem no máximo, o gerente agenda a construção de um novo Armazém.

### 5.5 Escalabilidade da colônia

- O gerente tenta criar novo trabalhador em ciclo, a cada 10s de simulação.
- A criação depende de casa concluída sem dono e cooldown mínimo de 8s desde a última criação.
- A abundância de recursos para gatilho de expansão usa: comida > 50 e água > 50.

## 6. Regras do Analista

### 6.1 Auditoria de tarefa

O analista reprova/reenvia quando:

- Trabalhador não registrado.
- Skill incompatível com a skill requerida.
- Coordenada de trabalho inválida.
- Entrega atrasada.
- Construção reportada sem progresso real de 100%.

Em rework, recalcula urgência/prazo e devolve para a fila do gerente.

### 6.2 Análises periódicas

Periodicamente, o analista:

- Mapeia terreno e informa zonas potenciais.
- Avalia necessidade de casas, poço e oficinas por perfil de trabalhadores.
- Sinaliza fila atrasada e sobrecarga de workforce.
- Solicita continuidade de construções inacabadas.

## 7. Vida Selvagem

- Limite de animais simultâneos no mapa: 5.
- Tick da fauna em intervalo fixo.
- Spawn com chance aleatória quando abaixo do limite.
- Animais vivos se movem aleatoriamente em tiles transitáveis.
- Animais mortos perdem rotTimer a cada tick e são removidos quando chega a 0.
