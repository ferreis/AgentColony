package com.colony.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import com.colony.model.*;
import com.colony.model.ColonyMap.ColonyBuilding;
import com.colony.Main;
import java.util.*;

public class AnalystAgent extends Agent {
  private final Map<String, String> workerSkills = new HashMap<>();
  private final Map<String, Integer> workerCountBySkill = new HashMap<>();
  private final Map<String, WorkerStatus> workerStatuses = new HashMap<>();
  private static final int FOOD_THRESHOLD = 30;
  private static final int WATER_THRESHOLD = 20;

  static class WorkerStatus {
    String name;
    String skill;
    int level;
    int energy;
    int x, y;
    boolean hasHome;
    boolean hasWorkshop;
  }

  protected void setup() {
    System.out.println(getLocalName() + ": Agente Analista iniciado.");

    addBehaviour(new CyclicBehaviour() {
      public void action() {
        ACLMessage msg = receive();
        if (msg != null) {
          String content = msg.getContent();

          if (content.startsWith("VERIFY_TASK:")) {
            String payload = content.substring("VERIFY_TASK:".length());
            String[] p = payload.split("\\|", -1);
            if (p.length >= 2) {
              String result = verifyTask(p);
              ACLMessage reply = msg.createReply();
              reply.setContent("VERIFICATION_RESULT:" + result);
              send(reply);
            }
          } else if (content.startsWith("REGISTER_SKILL:")) {
            String[] p = content.split(":");
            if (p.length >= 3) {
              registerWorkerSkill(p[1], p[2]);
              ACLMessage reply = msg.createReply();
              reply.setContent("REGISTERED");
              send(reply);
              sendToGui("WORKER_STATUS:" + p[1] + ":" + p[2] + ":1:Iniciante:registrado");
              sendToGui("LOG:Analista registrou " + p[1] + " como " + traduzirSkill(p[2]));
            }
          } else if (content.startsWith("WORKER_INFO:")) {
            String[] p = content.substring(12).split("\\|");
            if (p.length >= 7) {
              WorkerStatus ws = new WorkerStatus();
              ws.name = p[0];
              ws.skill = p[1];
              ws.level = Integer.parseInt(p[2]);
              ws.x = Integer.parseInt(p[3]);
              ws.y = Integer.parseInt(p[4]);
              ws.energy = Integer.parseInt(p[5]);
              ws.hasHome = "1".equals(p[6]);
              ws.hasWorkshop = p.length > 7 && "1".equals(p[7]);
              workerStatuses.put(ws.name, ws);
              // Analisa imediatamente ao receber info de cada worker
              String colonyReport = analyzeColonyNeeds();
              if (!colonyReport.isEmpty()) {
                sendToManager("COLONY_ANALYSIS:" + colonyReport);
              }
            }
          } else if (content.startsWith("TASK_QUEUE_REPORT:")) {
            analyzeTaskQueue(content.substring("TASK_QUEUE_REPORT:".length()));
          } else if ("REQUEST_RESOURCE_ABUNDANCE".equals(content)) {
            boolean abundant = isResourceAbundant();
            ACLMessage reply = msg.createReply();
            reply.setContent("RESOURCE_ABUNDANCE_RESULT:" + abundant);
            send(reply);
          }
        } else {
          block();
        }
      }
    });

    addBehaviour(new TickerBehaviour(this, 8000) {
      protected void onTick() {
        String analysis = analyzeWorkforce();
        sendToGui("WORKER_ANALYSIS:" + analysis);

        String terrain = analyzeTerrain();
        sendToGui("TERRAIN_ANALYSIS:" + terrain);
        sendToManager("TERRAIN_ANALYSIS:" + terrain);

        String colonyReport = analyzeColonyNeeds();
        if (!colonyReport.isEmpty()) {
          sendToManager("COLONY_ANALYSIS:" + colonyReport);
        }
      }
    });
  }

  private boolean isResourceAbundant() {
    int food = Main.resources.get("comida");
    int water = Main.resources.get("agua");
    return food > FOOD_THRESHOLD && water > WATER_THRESHOLD;
  }

  private String analyzeColonyNeeds() {
    ColonyMap map = Main.colonyMap;
    StringBuilder report = new StringBuilder();

    // 1. Contar trabalhadores sem casa
    int homeless = 0;
    int housed = 0;
    for (WorkerStatus ws : workerStatuses.values()) {
      if (ws.hasHome)
        housed++;
      else
        homeless++;
    }
    long houseCount = map.getBuildings().stream()
        .filter(b -> b.getType() == BuildingType.HOUSE)
        .count();
    int neededHouses = Math.min(2, homeless - (int) houseCount + housed);
    if (neededHouses > 0) {
      report.append("NEED_HOUSE:").append(neededHouses).append("|");
    }

    // 2. Verificar workshops necessários
    Set<BuildingType> neededWorkshops = new HashSet<>();
    for (WorkerStatus ws : workerStatuses.values()) {
      BuildingType bt = workshopForType(ws.skill);
      if (bt != null) {
        boolean exists = map.getBuildings().stream()
            .anyMatch(b -> b.getType() == bt);
        if (!exists)
          neededWorkshops.add(bt);
      }
    }
    for (BuildingType bt : neededWorkshops) {
      report.append("NEED_WORKSHOP:").append(bt.name()).append("|");
    }

    // 3. Construções inacabadas
    for (ColonyBuilding b : map.getBuildings()) {
      if (b.getProgress() < 100) {
        report.append("CONTINUE_BUILD:").append(b.getX()).append(",")
            .append(b.getY()).append(",").append(b.getType().name()).append("|");
      }
    }

    // 4. Trabalhadores ociosos com energia
    int idleCount = 0;
    for (WorkerStatus ws : workerStatuses.values()) {
      if (ws.energy >= 25)
        idleCount++;
    }
    if (idleCount >= 1) {
      report.append("IDLE_WORKERS:").append(idleCount).append("|");
    }

    sendToGui("LOG:Analista analisou colônia: " + report);
    return report.toString();
  }

  private BuildingType workshopForType(String skill) {
    if (skill == null)
      return null;
    String s = skill.toLowerCase();
    if (s.contains("carpenter") || s.contains("bowyer") || s.contains("wood craft"))
      return BuildingType.CARPENTER;
    if (s.contains("mason") || s.contains("stone") || s.contains("engrave"))
      return BuildingType.MASON;
    if (s.contains("smith") || s.contains("forge") || s.contains("furnace"))
      return BuildingType.SMITH;
    if (s.contains("craft") || s.contains("potter") || s.contains("bone") || s.contains("weaver"))
      return BuildingType.CRAFTER;
    if (s.contains("cook") || s.contains("brew") || s.contains("butcher"))
      return BuildingType.KITCHEN;
    if (s.contains("diagnos") || s.contains("surgeon") || s.contains("bone doctor"))
      return BuildingType.HOSPITAL;
    if (s.contains("farm") || s.contains("plant"))
      return BuildingType.FARM;
    return null;
  }

  private String verifyTask(String[] fields) {
    String taskId = fields[0];
    String workerName = fields[1];
    String taskType = field(fields, 2, "general");
    int targetX = parseInt(field(fields, 3, "-1"), -1);
    int targetY = parseInt(field(fields, 4, "-1"), -1);
    String targetType = field(fields, 5, "NONE");
    int reportedProgress = parseInt(field(fields, 6, "0"), 0);
    long deadlineAt = parseLong(field(fields, 7, "0"), 0L);
    long completedAt = parseLong(field(fields, 8, Long.toString(System.currentTimeMillis())),
        System.currentTimeMillis());
    int urgency = parseInt(field(fields, 9, "1"), 1);
    int corrections = parseInt(field(fields, 10, "0"), 0);
    boolean timeout = Boolean.parseBoolean(field(fields, 11, "false"));
    int workX = parseInt(field(fields, 12, "-1"), -1);
    int workY = parseInt(field(fields, 13, "-1"), -1);
    String requiredSkill = field(fields, 14, "general");

    String workerSkill = workerSkills.getOrDefault(workerName, "unknown");
    if ("unknown".equals(workerSkill)) {
      return rejection(taskId, urgency, "Trabalhador não registrado no Analista.");
    }

    if (!isSkillCompatible(workerSkill, requiredSkill)) {
      return rejection(taskId, urgency, "Habilidade incompatível. Esperado: "
          + traduzirSkill(requiredSkill) + ", recebido: " + traduzirSkill(workerSkill) + ".");
    }

    if ("build".equalsIgnoreCase(taskType)) {
      ColonyMap.ColonyBuilding building = Main.colonyMap.getBuildingAt(targetX, targetY);
      if (building == null) {
        return rejection(taskId, urgency, "Construção alvo não existe no mapa.");
      }
      if (!"NONE".equals(targetType) && !building.getType().name().equals(targetType)) {
        return rejection(taskId, urgency, "Tipo construído diferente do planejado.");
      }

      int actualProgress = building.getProgress();
      if (actualProgress < 100) {
        return rework(taskId, urgency, corrections,
            "Construção incompleta: " + actualProgress + "%. Requer continuação.");
      }

      String note = timeout || completedAt > deadlineAt
          ? "Construção correta, mas entregue fora do prazo."
          : "Construção correta.";
      return approval(taskId, 100, note);
    }

    if (workX < 0 || workY < 0 || !Main.colonyMap.inBounds(workX, workY)) {
      return rejection(taskId, urgency, "Área de trabalho inválida.");
    }

    if (timeout || completedAt > deadlineAt) {
      return rework(taskId, urgency, corrections, "Entrega atrasada. Prazo e urgência recalculados.");
    }

    int quality = Math.max(60, Math.min(100, reportedProgress > 0 ? reportedProgress : 90));
    return approval(taskId, quality, "Trabalho executado dentro das regras físicas.");
  }

  private String approval(String taskId, int quality, String note) {
    return taskId + "|APPROVED|" + quality + "|" + note + "|0|0";
  }

  private String rejection(String taskId, int urgency, String note) {
    long newDeadline = System.currentTimeMillis() + 12000;
    int newUrgency = Math.min(5, urgency + 1);
    return taskId + "|REWORK|0|" + note + "|" + newUrgency + "|" + newDeadline;
  }

  private String rework(String taskId, int urgency, int corrections, String note) {
    long extraTime = Math.max(8000, 16000 - (long) corrections * 2000);
    long newDeadline = System.currentTimeMillis() + extraTime;
    int newUrgency = Math.min(5, urgency + 1);
    return taskId + "|REWORK|50|" + note + "|" + newUrgency + "|" + newDeadline;
  }

  private boolean isSkillCompatible(String workerSkill, String requiredSkill) {
    if (requiredSkill == null || requiredSkill.isBlank() || "general".equalsIgnoreCase(requiredSkill)) {
      return true;
    }

    SkillType worker = SkillType.fromKey(workerSkill);
    SkillType required = SkillType.fromKey(requiredSkill);
    if (worker == null || required == null) {
      return workerSkill.equalsIgnoreCase(requiredSkill);
    }

    return worker == required || worker.getCategory() == required.getCategory();
  }

  private void analyzeTaskQueue(String payload) {
    if (payload.isBlank())
      return;

    int activeTasks = 0;
    int lateTasks = 0;
    int energeticWorkers = 0;
    Map<String, Integer> tasksBySkill = new HashMap<>();
    Map<String, Integer> workersBySkill = new HashMap<>();
    StringBuilder deadlineReport = new StringBuilder();
    long now = System.currentTimeMillis();

    for (WorkerStatus workerStatus : workerStatuses.values()) {
      if (workerStatus.energy >= 30) {
        energeticWorkers++;
        workersBySkill.merge(workerStatus.skill, 1, Integer::sum);
      }
    }

    String[] taskItems = payload.split("\\|");
    for (String taskItem : taskItems) {
      if (taskItem.isBlank())
        continue;

      String[] fields = taskItem.split("~", -1);
      if (fields.length < 12)
        continue;

      String taskId = fields[0];
      String status = fields[2];
      int urgency = parseInt(fields[4], 1);
      long deadlineAt = parseLong(fields[5], now);
      String requiredSkill = fields[11];

      if ("approved".equals(status))
        continue;

      activeTasks++;
      tasksBySkill.merge(requiredSkill, 1, Integer::sum);

      if (now > deadlineAt) {
        lateTasks++;
        int newUrgency = Math.min(5, urgency + 1);
        long newDeadline = now + Math.max(8000, 18000 - newUrgency * 1500L);
        deadlineReport.append(taskId).append("~LATE~")
            .append(newUrgency).append("~")
            .append(newDeadline).append("|");
      }
    }

    if (deadlineReport.length() > 0) {
      sendToManager("DEADLINE_REPORT:" + deadlineReport);
      sendToGui("LOG:Analista recalculou prazo de " + lateTasks + " tarefa(s) atrasada(s).");
    }

    if (activeTasks > Math.max(1, energeticWorkers) * 2) {
      sendToManager("SCALE_ALERT:Fila alta: " + activeTasks
          + " tarefas para " + energeticWorkers + " trabalhador(es) com energia.");
    }

    for (Map.Entry<String, Integer> taskEntry : tasksBySkill.entrySet()) {
      String requiredSkill = taskEntry.getKey();
      int workerCount = countCompatibleWorkers(requiredSkill, workersBySkill);
      if (workerCount == 0) {
        sendToManager("SCALE_ALERT:Sem trabalhador ativo para "
            + traduzirSkill(requiredSkill) + ".");
      }
    }
  }

  private int countCompatibleWorkers(String requiredSkill, Map<String, Integer> workersBySkill) {
    int count = 0;
    for (Map.Entry<String, Integer> workerEntry : workersBySkill.entrySet()) {
      if (isSkillCompatible(workerEntry.getKey(), requiredSkill)) {
        count += workerEntry.getValue();
      }
    }
    return count;
  }

  private String analyzeWorkforce() {
    if (workerSkills.isEmpty())
      return "Nenhum trabalhador registrado ainda.";
    int total = workerSkills.size();
    StringBuilder sb = new StringBuilder("Mão de obra: " + total + " total\n");
    for (Map.Entry<String, Integer> e : workerCountBySkill.entrySet()) {
      sb.append("  ").append(traduzirSkill(e.getKey())).append(": ").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }

  private String analyzeTerrain() {
    ColonyMap map = Main.colonyMap;
    int centerX = ColonyMap.WIDTH / 2;
    int centerY = ColonyMap.HEIGHT / 2;
    int radius = 90;

    int stoneCount = 0;
    int woodCount = 0;
    int waterCount = 0;
    int openCount = 0;
    int[] nearestStone = null;
    int[] nearestWood = null;
    int[] nearestOpen = null;

    for (int y = centerY - radius; y <= centerY + radius; y++) {
      for (int x = centerX - radius; x <= centerX + radius; x++) {
        if (!map.inBounds(x, y))
          continue;

        TerrainTile tile = map.getTile(x, y);
        if (tile == TerrainTile.STONE || tile == TerrainTile.MOUNTAIN) {
          stoneCount++;
          nearestStone = nearest(centerX, centerY, nearestStone, x, y);
        } else if (tile == TerrainTile.TREE) {
          woodCount++;
          nearestWood = nearest(centerX, centerY, nearestWood, x, y);
        } else if (tile == TerrainTile.WATER) {
          waterCount++;
        }

        if (!tile.isBlocksMovement() && map.getBuildingAt(x, y) == null) {
          openCount++;
          nearestOpen = nearest(centerX, centerY, nearestOpen, x, y);
        }
      }
    }

    StringBuilder report = new StringBuilder();
    if (nearestStone != null) {
      report.append("STONE~").append(nearestStone[0]).append("~")
          .append(nearestStone[1]).append("~").append(stoneCount).append("|");
    }
    if (nearestWood != null) {
      report.append("WOOD~").append(nearestWood[0]).append("~")
          .append(nearestWood[1]).append("~").append(woodCount).append("|");
    }
    if (nearestOpen != null) {
      report.append("BUILD_ZONE~").append(nearestOpen[0]).append("~")
          .append(nearestOpen[1]).append("~").append(openCount).append("|");
    }

    String summary = "mountain:" + stoneCount + " forest:" + woodCount
        + " water:" + waterCount + " open:" + openCount;
    report.append("SUMMARY~0~0~0~").append(summary);
    sendToGui("LOG:Analista mapeou terreno: " + summary);
    return report.toString();
  }

  private int[] nearest(int centerX, int centerY, int[] current, int candidateX, int candidateY) {
    if (current == null) {
      return new int[] { candidateX, candidateY };
    }

    double currentDistance = Math.hypot(current[0] - centerX, current[1] - centerY);
    double candidateDistance = Math.hypot(candidateX - centerX, candidateY - centerY);
    return candidateDistance < currentDistance ? new int[] { candidateX, candidateY } : current;
  }

  private void registerWorkerSkill(String name, String skill) {
    String old = workerSkills.put(name, skill);
    if (old != null) {
      workerCountBySkill.merge(old, -1, Integer::sum);
      if (workerCountBySkill.get(old) <= 0)
        workerCountBySkill.remove(old);
    }
    workerCountBySkill.merge(skill, 1, Integer::sum);
    System.out.println(getLocalName() + ": Registrou " + name + " como " + traduzirSkill(skill));
  }

  private String traduzirSkill(String skill) {
    SkillType st = SkillType.fromKey(skill);
    if (st != null)
      return st.getDisplayName();
    return switch (skill.toLowerCase()) {
      case "construction" -> "construção";
      case "mining" -> "mineração";
      case "woodcutting" -> "corte de madeira";
      case "smithing" -> "ferraria";
      case "farming" -> "agricultura";
      default -> skill;
    };
  }

  private String field(String[] fields, int index, String fallback) {
    if (index < 0 || index >= fields.length || fields[index] == null || fields[index].isEmpty()) {
      return fallback;
    }
    return fields[index];
  }

  private int parseInt(String value, int fallback) {
    try {
      return Integer.parseInt(value);
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private long parseLong(String value, long fallback) {
    try {
      return Long.parseLong(value);
    } catch (Exception ignored) {
      return fallback;
    }
  }

  private void sendToManager(String content) {
    try {
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.addReceiver(new AID("manager", AID.ISLOCALNAME));
      msg.setContent(content);
      send(msg);
    } catch (Exception ignored) {
    }
  }

  private void sendToGui(String content) {
    try {
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.addReceiver(new AID("gui", AID.ISLOCALNAME));
      msg.setContent(content);
      send(msg);
    } catch (Exception ignored) {
    }
  }
}
