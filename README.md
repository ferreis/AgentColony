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

O sistema usa JADE com os seguintes agentes principais:

- WorkerAgent: executa tarefas, move-se no mapa, coleta/consome recursos, descansa, luta e ganha XP.
- ManagerAgent: cria tarefas, distribui por score, controla urgência/prazo, reforça estoque mínimo e pede novas construções, incluindo expansão de armazéns quando a capacidade de recursos atinge o limite.
- AnalystAgent: audita tarefas, analisa terreno/colônia e gera recomendações para o gerente.
- WildlifeAgent: faz spawn e simulação de animais.
- GuiAgent: recebe eventos e atualiza a interface Swing.

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
