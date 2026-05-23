# Regras e Mecânicas do Colony System

Este documento detalha o funcionamento, as regras e a arquitetura do ecossistema multiagente da simulação da colônia, baseado no código fonte atual (`Testes/src/com/colony/`).

## 1. Visão Geral e Interface

A colônia é gerenciada por uma arquitetura multiagente (utilizando o framework JADE) dividida em três papéis principais:

- **WorkerAgent (Trabalhador / NPC):** A unidade física da colônia. Executa as ordens, movimenta-se pelo mapa, gasta energia e ganha experiência.
- **ManagerAgent (Gerente):** O cérebro logístico. Recebe as necessidades da colônia, cria uma fila de tarefas (Tasks), calcula urgências e prazos, e distribui as missões para o melhor trabalhador disponível. (Se for muito necessário, o gerente tem a habilidade de instanciar e invocar novos trabalhadores para a colônia, mas vale lembrar que cada novo colono é uma nova boca para alimentar).
- **AnalystAgent (Analista):** O auditor e planejador urbano. Analisa o terreno, decide onde construir, verifica se os trabalhadores precisam de casas/oficinas, e audita rigorosamente o trabalho concluído.

### Interface do Usuário (GUI)

- As tarefas (Tasks) no painel de controle são organizadas em três sub-abas (Tabs) para separar as missões:
  - **Em Espera:** Tarefas pendentes ou na fila aguardando ação.
  - **Sendo Feitas:** Tarefas em execução ativa por um colono.
  - **Concluídas:** Tarefas auditadas e terminadas com sucesso.
- **Aba Trabalhadores:** Exibe em tempo real o Nível, Profissão, Energia, Fome, e Sede de cada NPC.
- **Rodapé (Footer):** Exibe informações em tempo real calculadas a partir das tarefas em andamento, incluindo contagem total consolidada.

---

## 2. Regras de Movimentação, Infraestrutura e Construção

### Loteamento de Construções (Espaçamento)

- **Regra do 1-Tile:** Nenhuma construção pode encostar em outra. É obrigatório haver pelo menos 1 tile livre (espaço de chão) entre o perímetro de qualquer edifício e o de seus vizinhos. Isso garante vielas e passagens livres pela vila.

### Edifícios, Paredes e Portas

- Todas as construções fechadas (como Casas, Hospitais, Oficinas) possuem **Paredes** e **Portas**.
- **Paredes:** Ocupam a borda externa da construção e são intransponíveis (`blocksMovement = true`).
- **Portas:** Localizam-se sempre no centro da face inferior (sul) do edifício. É o único tile aberto por onde o NPC pode entrar ou sair do interior livre da construção.

### Estradas (Roads)

- Trabalhadores podem construir **Estradas**.
- **Benefícios:** Mover-se por uma estrada aumenta a velocidade de locomoção e reduz significativamente o consumo de energia da caminhada.
- **Regras de Construção:** Devem formar **linhas contínuas**, com no máximo pontos de bifurcação, evitando poluição caótica no mapa.

### Navegação (Pathfinding A\*)

- Os colonos utilizam um sistema de rotas (A\*) que reconhece e desvia de paredes e obstáculos, garantindo que não atravessem construções indevidamente.

---

## 3. Regras dos Trabalhadores (Workers) e Sobrevivência

### Fome, Sede e Morte Permanente

- **Fome e Sede:** Ambas variam de 0 a 100.
  - Elas caem lentamente com o tempo (a cada ciclo).
  - Se alguma delas chegar a `<= 40`, o colono para de trabalhar, entra em modo de urgência e caminha até o armazém para comer/beber, consumindo os itens globais do painel de recursos.
  - **Morte Real:** Se a Fome ou a Sede chegarem a `0`, o agente literalmente "morre" (é ejetado do JADE usando `doDelete()`) e para de existir na simulação.

### Vida (HP), Dano e Ferimentos

- **Saúde (Health):** Varia de 0 a 100.
  - Se tomar dano de animais, o HP diminui.
  - **Punição por Ferimentos:** Se o HP estiver baixo (`< 100`), a Fome e a Sede **caem duas vezes mais rápido**, pois o corpo precisa de energia para se recuperar. Além disso, se o dano for grande o suficiente, ele pode rejeitar tarefas para focar em repouso.
  - O HP se regenera bem devagar com o passar do tempo (+2 de vida por ciclo, contanto que não esteja tomando dano nem morrendo de fome).
  - Se o HP chegar a `0`, o anão morre e também é removido do mapa.

### Descanso e Energia

- **Energia (Energy):** Varia de 0 a 100. Gasta a cada ação e locomoção.
- **Descanso Obrigatório em Casa:** Para ter um descanso eficiente, o NPC **deve caminhar fisicamente para dentro de sua casa** (atravessando a porta). Se não conseguir chegar ou não tiver casa, ele dorme no relento recuperando muito menos energia e gastando mais tempo.

### Vida Selvagem e Caça 🐺🦌

- Animais agressivos (Lobos) e passivos (Cervos) dão respawn de tempos em tempos no mapa de forma autônoma.
- **O Combate:**
  - Se um lobo agressivo estiver perto, ele ataca o anão, causando grande dano (15 de dano) por turno.
  - Se o NPC for um **Caçador / Guerreiro / Lenhador**, ele sabe lutar: Toma apenas 5 de dano do lobo e desfere golpes críticos contra os animais.
  - Caçadores buscarão caçar passivamente os animais se chegarem perto deles.
- **Carcaça e Esfolamento:**
  - Quando um animal tem o HP zerado, ele **não dropa a carne imediatamente**. Em vez disso, ele vira uma **Carcaça** no chão.
  - A carcaça tem um "prazo de validade" (tempo de decomposição). Se esse timer expirar antes de alguém chegar nela, ela apodrece e some do mapa.
  - **Loot:** Se um NPC passa por cima da carcaça para esfolar, ela é coletada. **Regra Importante:** Se o NPC for um _Caçador_, ele coleta as carnes perfeitamente ganhando `10 de Comida`. Se for um civil (ferreiro, pedreiro), ele não sabe esfolar e resgata apenas `1 de Comida`.

### Logística de Oficinas, Pesca e Materiais

- **Trabalho Interno:** Um colono com profissão específica **obrigatoriamente** deve se deslocar para dentro do perímetro de sua **Oficina** para executar a ação de craft.
- **Busca de Materiais:** O material não aparece magicamente. O trabalhador precisa andar até o **Armazém (Stockpile)** para buscar recursos antes de trabalhar.
- **Pesca:** A profissão de pescador difere das demais por não usar uma oficina física, bastando caminhar até a beira de um Rio (água). Contudo, a tarefa de pesca só acontece se houver uma **"vara de pesca"** em estoque, fabricada passivamente por Carpinteiros.

### Profissões e Sistema de Experiência (Skills)

- Trabalhar gera XP. Subir de Nível (Level) faz o colono trabalhar mais rápido e com mais qualidade, ganhando prioridade com o Gerente.
- Se for designado para uma tarefa que não sabe, ele a aprenderá no nível 1.

---

## 4. Regras de Distribuição de Trabalho (Manager)

O Gerente realiza um "leilão" logístico baseado em **Score**:

- Apenas trabalhadores desocupados e com Energia `>= 30` são considerados.
- **Habilidade:** Dá +50 pontos iniciais, mais +10 por nível.
- **Distância e Disposição:** Favorece quem estiver mais perto e com mais energia.
- **Urgência:** Multiplica o peso da habilidade, exigindo "funcionários seniores" para problemas críticos.

---

## 5. Auditoria, Prazos e Expansão de Zonas (Analista)

O AnalystAgent impõe regras estritas de qualidade:

- **Auditoria de Tarefas:** Se a construção de um prédio for reportada como pronta, mas o progresso real for `< 100%`, o Analista **Reprova** a tarefa. Tarefas com prazos estourados ou executadas com a skill errada também são rejeitadas.
- **Zonas de Construção:** Os colonos podem expandir as zonas ativamente. Novas áreas podem ser demarcadas para planejamento.
- **Gestão de Casas e Oficinas:** O Analista conta os trabalhadores "Sem-Teto" e emite ordens para construção de casas (até 2 por vez). Se uma profissão necessita de uma oficina específica que não existe, o Analista ordena a construção automaticamente.
- **Zonas de Matéria-Prima:** Mapeamento autônomo de jazidas e florestas para trabalhadores ociosos.
