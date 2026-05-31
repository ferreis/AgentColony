# Regras e Mecanicas Atuais do Colony System

Este documento descreve as regras que estao realmente implementadas no codigo atual em src/com/colony.

## 1. Arquitetura Multiagente

O sistema usa JADE com os seguintes agentes principais:

- WorkerAgent: executa tarefas, se move no mapa, coleta/consome recursos, descansa, luta e ganha XP.
- ManagerAgent: cria tarefas, distribui por score, controla urgencia/prazo, reforca estoque minimo e pede novas construcoes, incluindo expansao de armazens quando a capacidade de recursos atinge o limite.
- AnalystAgent: audita tarefas, analisa terreno/colonia e gera recomendacoes para o gerente.
- WildlifeAgent: faz spawn e simulacao de animais.
- GuiAgent: recebe eventos e atualiza a interface Swing.

## 2. Interface (GUI)

Abas da interface:

- Mapa
- Trabalhadores
- Tarefas
- Recursos
- Eventos

No painel de tarefas, atualmente existem 2 sub-abas:

- Sendo Feitas: recebe tarefas pendentes, atribuídas e em correcao.
- Concluidas: recebe tarefas aprovadas/finalizadas.

Observacoes importantes:

- Nao existe sub-aba separada "Em Espera" no estado atual.
- O rodape mostra contagem consolidada de tarefas (ativas + concluidas).
- A aba Recursos exibe cada item no formato textual "quantidade atual / capacidade maxima".
- Nao existe barra de progresso visual na aba Recursos.
- A atualizacao dos recursos permanece dinamica (coleta, consumo e mudancas de capacidade por construcao/remocao de armazens).
- Nao ha regra especial para ocultar "ouro" na exibicao de recursos.

## 3. Mapa, Construcoes e Navegacao

### 3.1 Terreno e inicio da colonia

- Mapa 200x200 com geracao procedural.
- Zona inicial central em piso (floor).
- O mapa ja inicia com:
  - 1 Armazem (WAREHOUSE) com progresso 100%.
  - 1 Poco (WELL) com progresso 100%.

### 3.2 Regras de colocacao de predios

- Regra de espacamento de 1 tile para toda construcao.
- Nao permite sobreposicao de construcoes.
- O terreno do footprint da construcao nao pode bloquear movimento.

### 3.3 Paredes e portas

- Construcoes com telhado (hasRoof=true) viram estrutura fechada.
- Borda externa e parede intransponivel.
- A porta fica no centro da face inferior (sul), como unico tile de passagem.

### 3.4 Estradas

- O tipo ROAD existe no modelo e tem custo de construcao (pedra).
- No estado atual, nao ha rotina de planejamento/execucao automatica de estradas pelo gerente.
- Nao existe bonus especifico de velocidade/energia por andar em estrada implementado no WorkerAgent.

### 3.5 Pathfinding

- A navegacao atual usa busca em grade por fila (BFS), com 8 direcoes.
- Evita tiles bloqueantes (parede, arvore, montanha, pedra, etc.).
- Nao e um algoritmo A\* no estado atual.

## 4. Regras dos Trabalhadores

### 4.1 Atributos e morte

Cada trabalhador mantem:

- Health (HP): 0 a 100
- Energy: 0 a 100
- Fome: 0 a 100
- Sede: 0 a 100

Se HP <= 0, fome <= 0 ou sede <= 0, o trabalhador morre e executa doDelete().

### 4.2 Degradacao e regeneracao periodica

No loop de status periodico:

- Sede cai 2 por tick.
- Fome cai metade da taxa da sede (acumulada).
- HP regenera +2 por tick quando HP < 100.

Importante: nao existe penalidade de acelerar fome/sede quando HP < 100.

### 4.3 Regras de descanso

- Se energia <= 30, entra em descanso ate energia 100.
- Tenta dormir em casa (tile central da casa atribuida).
- Se chega em casa: recupera de 10 a 24 de energia por ciclo.
- Sem casa ou sem conseguir chegar: descansa no relento e recupera de 5 a 14.

### 4.4 Regras de fome e sede

- Com sede <= 40, prioriza beber no armazem.
- Com fome <= 40, prioriza comer no armazem.
- Se falta agua e existir poco concluido, pode coletar 2 a 4 de agua no poco.
- Se nao conseguir atender necessidade basica, perde HP.

### 4.5 Aceitacao/rejeicao de tarefa

O trabalhador rejeita tarefa quando:

- Esta descansando ate energia cheia.
- HP < 100.
- Energia < 15.
- A tarefa exige oficina especifica que ainda nao existe concluida.

Se nao tiver a skill da tarefa, aprende automaticamente no nivel 1.

### 4.6 Execucao de tarefa e recursos

- Tarefas de construcao em alvo aumentam progresso com ganho baseado em skill e urgencia.
- Antes da primeira etapa de construcao, custo de materiais e reservado/consumido.
- Se faltar recurso para o custo da construcao, a tarefa e rejeitada.
- Pesca consome 1 vara de pesca por tentativa.
- Coleta (madeira/pedra/comida) gera rendimento escalonado por nivel da skill.

### 4.7 Combate, caca e carcaca

- Wildlife faz spawn de Lobos (agressivos) e Cervos (passivos).
- Aproximacao de lobo em raio curto ja causa dano imediato no trabalhador.
- No fluxo de combate agressivo, trabalhador especializado (fighter/hunter/lenhador) toma menos dano e causa mais dano.
- Animais mortos viram carcaca (dead=true) e depois podem:
  - ser esfolados por worker (10 comida se cacador, 1 comida se nao), ou
  - apodrecer e sumir quando o timer expira.

## 5. Regras do Gerente

### 5.1 Fila, estados, urgencia e prazos

Estados de tarefa:

- pending
- assigned
- audit
- approved

Parametros atuais:

- Deadline padrao: 18s
- Deadline de construcao: 26s
- Urgencia maxima: 5

### 5.2 Distribuicao por score

Para selecionar trabalhador:

- Ignora trabalhadores ocupados ou com energia < 30.
- Bonus por skill compativel (exata ou mesma categoria).
- Bonus por proximidade do alvo.
- Bonus por energia atual.
- Bonus adicional por urgencia x nivel pratico.

### 5.3 Estoque e producao

- O gerente trabalha com perfil de estoque (balanceado/agressivo/economico).
- Se recurso fica abaixo do minimo, o gerente reforca automaticamente o estoque minimo respeitando a capacidade total disponivel dos armazens.
- Tambem cria tarefas de producao para buscar estoque alvo (madeira, pedra/ferro, comida, vara de pesca), limitado pela capacidade total de armazenamento.

### 5.4 Capacidade de armazenamento por Armazem

Cada Armazem (WAREHOUSE) adiciona a seguinte capacidade maxima:

- Pedra: 250
- Madeira: 250
- Ferro: 200
- Comida: 200
- Agua: 100
- Vara de pesca: 30

Regras operacionais:

- O fluxo operacional de armazenamento usa apenas Armazem (o tipo Deposito foi removido do fluxo).
- Casas (HOUSE) nao armazenam recursos. Elas servem apenas como moradia/descanso e atribuicao de dono.
- O inventario de recursos e global (ColonyResources) e a capacidade total depende da quantidade de armazens concluidos.
- A capacidade total por recurso = capacidade por armazem x numero de armazens concluidos.
- A GUI reflete essa capacidade em tempo real na aba Recursos, no formato textual "atual / maximo".
- Se qualquer recurso atingir o limite maximo, ou se todos os recursos estiverem no maximo, o gerente agenda construcao de um novo Armazem.

### 5.5 Escalabilidade da colonia

- Quando ha casa concluida sem dono e condicoes atendidas, o gerente pode criar novo trabalhador.
- A criacao depende de cooldown e disponibilidade de casa.
- A abundancia de comida/agua e usada como gatilho de expansao.

## 6. Regras do Analista

### 6.1 Auditoria de tarefa

O analista reprova/reenvia quando:

- Trabalhador nao registrado.
- Skill incompativel com skill requerida.
- Coordenada de trabalho invalida.
- Entrega atrasada.
- Construcao reportada sem progresso real de 100%.

Em rework, recalcula urgencia/prazo e devolve para a fila do gerente.

### 6.2 Analises periodicas

Periodicamente, o analista:

- Mapeia terreno e informa zonas potenciais.
- Avalia necessidade de casas, poco e oficinas por perfil de trabalhadores.
- Sinaliza fila atrasada e sobrecarga de workforce.
- Solicita continuidade de construcoes inacabadas.

## 7. Vida Selvagem

- Limite de animais simultaneos no mapa: 5.
- Tick da fauna em intervalo fixo.
- Spawn com chance aleatoria quando abaixo do limite.
- Animais vivos se movem aleatoriamente em tiles transitaveis.
- Animais mortos perdem rotTimer a cada tick e sao removidos quando chega a 0.

## 8. Resumo das Correcos Aplicadas Neste README

Este README foi alinhado ao comportamento atual do codigo, incluindo correcoes de:

- caminho do codigo-fonte (src/com/colony);
- estrutura real das abas de tarefas na GUI;
- algoritmo de navegacao (BFS em vez de A\*);
- regras efetivas de fome/sede/HP;
- papel atual de estradas;
- regras de auditoria, prazos e rework implementadas;
- remocao do Deposito (STOCKPILE) do fluxo operacional;
- capacidade por Armazem e expansao automatica quando lota;
- exibicao da aba Recursos simplificada para texto (atual/maximo), sem barra de progresso;
- remocao de regra especial de ouro na exibicao da GUI.
