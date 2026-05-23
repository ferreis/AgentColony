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
import java.util.stream.*;

public class ManagerAgent extends Agent {
  private final List<WorkerInfo> workers = new ArrayList<>();
  private final List<TaskEntry> tasks = new ArrayList<>();
  private final Map<String, String> taskResults = new HashMap<>();
  private final Set<String> mappedZones = new HashSet<>();
  private int taskCounter = 0;

  // Task types that don't need a building
  private static final Set<String> RAW_TASK_TYPES = Set.of("mine", "woodcut", "fish", "harvest", "gather");
  private static final long DEFAULT_DEADLINE_MS = 18000;
  private static final long BUILD_DEADLINE_MS = 26000;
  private static final int MAX_URGENCY = 5;

  static class WorkerInfo {
    String name;
    String skill;
    int level = 1;
    int energy = 100;
    int x = ColonyMap.WIDTH / 2, y = ColonyMap.HEIGHT / 2;
    boolean busy = false;

    WorkerInfo(String name, String skill) {
      this.name = name;
      this.skill = skill;
    }
  }

  static class TaskEntry {
    String id;
    String type;
    String status; // pending, assigned, audit, approved
    String worker;
    ColonyBuilding target;
    int urgency = 1;
    long createdAt;
    long deadlineAt;
    int corrections = 0;
    boolean correctionRequired = false;
    int lastWorkX = -1;
    int lastWorkY = -1;
    int lastProgress = 0;
    String auditNote = "";

    TaskEntry(String id, String type) {
      this.id = id;
      this.type = type;
      this.status = "pending";
      this.createdAt = System.currentTimeMillis();
      this.deadlineAt = this.createdAt + DEFAULT_DEADLINE_MS;
    }
  }

  protected void setup() {
    System.out.println(getLocalName() + ": Agente Gerente iniciado.");

    addBehaviour(new CyclicBehaviour() {
      public void action() {
        ACLMessage msg = receive();
        if (msg != null)
          handleMessage(msg);
        else
          block();
      }
    });

    addBehaviour(new TickerBehaviour(this, 4000) {
      protected void onTick() {
        distributeTasks();
      }
    });

    addBehaviour(new TickerBehaviour(this, 5000) {
      protected void onTick() {
        sendTaskQueueToAnalyst();
      }
    });

    // Análise é feita pelo Analista (COLONY_ANALYSIS) - este ticker é fallback
    addBehaviour(new TickerBehaviour(this, 15000) {
      protected void onTick() {
        // fallback: só cria matérias-primas se precisar
        long idleCount = workers.stream().filter(w -> !w.busy).count();
        if (idleCount >= 2 && getPendingTaskCount("mine") == 0) {
          createTask("mine", "mine");
        }
        if (idleCount >= 2 && getPendingTaskCount("woodcut") == 0) {
          createTask("woodcut", "woodcut");
        }
      }
    });
  }

  private void handleMessage(ACLMessage msg) {
    String content = msg.getContent();
    String sender = msg.getSender().getLocalName();

    if ("REGISTER_WORKER".equals(content)) {
      ACLMessage reply = msg.createReply();
      reply.setContent("REGISTERED");
      send(reply);
      // Add basic worker entry immediately (updated later by WORKER_INFO)
      if (workers.stream().noneMatch(w -> w.name.equals(sender))) {
        workers.add(new WorkerInfo(sender, "unknown"));
        sendToGui("LOG:" + sender + " registrado no gerente.");
      }
    } else if (content.startsWith("WORKER_INFO:")) {
      String[] p = content.substring(12).split("\\|");
      if (p.length >= 5) {
        String name = p[0];
        String skill = p[1];
        int level = Integer.parseInt(p[2]);
        int x = Integer.parseInt(p[3]);
        int y = Integer.parseInt(p[4]);
        int energy = p.length > 5 ? Integer.parseInt(p[5]) : 100;
        registerOrUpdateWorker(name, skill, level, x, y, energy);
      }
    } else if (content.startsWith("TASK_COMPLETE:")) {
      handleWorkerFinish(content.substring("TASK_COMPLETE:".length()), sender, false);
    } else if (content.startsWith("TASK_TIMEOUT:")) {
      handleWorkerFinish(content.substring("TASK_TIMEOUT:".length()), sender, true);
    } else if (content.startsWith("TASK_REJECTED:")) {
      String taskId = content.split(":")[1];
      for (TaskEntry t : tasks) {
        if (t.id.equals(taskId)) {
          t.status = "pending";
          t.worker = null;
          for (WorkerInfo w : workers) {
            if (w.name.equals(sender))
              w.busy = false;
          }
          sendToGui("LOG:" + sender + " rejeitou tarefa " + taskId);
          break;
        }
      }
    } else if (content.startsWith("VERIFICATION_RESULT:")) {
      handleVerificationResult(content.substring("VERIFICATION_RESULT:".length()));
    } else if (content.startsWith("TERRAIN_ANALYSIS:")) {
      handleTerrainAnalysis(content.substring("TERRAIN_ANALYSIS:".length()));
      sendToGui(content);
    } else if (content.startsWith("WORKER_ANALYSIS:")) {
      sendToGui(content);
    } else if (content.startsWith("COLONY_ANALYSIS:")) {
      handleColonyAnalysis(content.substring("COLONY_ANALYSIS:".length()));
    } else if (content.startsWith("DEADLINE_REPORT:")) {
      handleDeadlineReport(content.substring("DEADLINE_REPORT:".length()));
    } else if (content.startsWith("SCALE_ALERT:")) {
      sendToGui("LOG:Analista: " + content.substring("SCALE_ALERT:".length()));
    } else if ("REQUEST_WORKFORCE".equals(content)) {
      sendWorkforceTo(msg.getSender().getLocalName());
    }
  }

  private void registerOrUpdateWorker(String name, String skill, int level, int x, int y, int energy) {
    for (WorkerInfo w : workers) {
      if (w.name.equals(name)) {
        w.skill = skill;
        w.level = level;
        w.x = x;
        w.y = y;
        w.energy = energy;
        return;
      }
    }
    WorkerInfo w = new WorkerInfo(name, skill);
    w.level = level;
    w.x = x;
    w.y = y;
    w.energy = energy;
    workers.add(w);
    sendToGui("LOG:" + name + " registrado no gerente (" + skill + " lv" + level + ")");
  }

  private void handleWorkerFinish(String payload, String workerName, boolean timeout) {
    String[] parts = payload.split("\\|");
    if (parts.length == 0)
      return;

    String taskId = parts[0];
    TaskEntry task = findTask(taskId);
    if (task == null)
      return;

    task.lastWorkX = parts.length > 1 ? parseInt(parts[1], -1) : -1;
    task.lastWorkY = parts.length > 2 ? parseInt(parts[2], -1) : -1;
    task.lastProgress = parts.length > 3 ? parseInt(parts[3], 0) : 0;
    task.worker = workerName;
    task.status = "audit";

    for (WorkerInfo worker : workers) {
      if (worker.name.equals(workerName)) {
        worker.busy = false;
      }
    }

    String event = timeout ? "estourou o prazo" : "foi concluída";
    sendToGui("LOG:Tarefa " + taskId + " " + event + " por " + workerName + ". Enviando para auditoria.");
    sendTaskToAnalyst(task, timeout);
  }

  private void handleVerificationResult(String payload) {
    String[] parts = payload.split("\\|", -1);
    if (parts.length < 2)
      return;

    String taskId = parts[0];
    String result = parts[1];
    TaskEntry task = findTask(taskId);
    if (task == null)
      return;

    taskResults.put(taskId, result);
    task.auditNote = parts.length > 3 ? parts[3] : "";

    if ("APPROVED".equals(result)) {
      task.status = "approved";
      task.correctionRequired = false;
      sendToGui("TASK_STATUS:" + task.id + ":" + task.type + ":aprovada pelo Analista");
      sendToGui("LOG:Analista aprovou tarefa " + task.id + ". " + task.auditNote);
      return;
    }

    int newUrgency = parts.length > 4 ? parseInt(parts[4], Math.min(MAX_URGENCY, task.urgency + 1)) : task.urgency + 1;
    long newDeadline = parts.length > 5 ? parseLong(parts[5], System.currentTimeMillis() + DEFAULT_DEADLINE_MS)
        : System.currentTimeMillis() + DEFAULT_DEADLINE_MS;
    task.urgency = Math.min(MAX_URGENCY, Math.max(1, newUrgency));
    task.deadlineAt = newDeadline;
    task.status = "pending";
    task.correctionRequired = true;
    task.corrections++;

    sendToGui("TASK_STATUS:" + task.id + ":" + task.type + ":correção pendente (urgência " + task.urgency + ")");
    sendToGui("LOG:Analista reprovou tarefa " + task.id + ": " + task.auditNote);
  }

  private void handleDeadlineReport(String payload) {
    if (payload.isBlank())
      return;

    String[] items = payload.split("\\|");
    for (String item : items) {
      if (item.isBlank())
        continue;

      String[] parts = item.split("~", -1);
      if (parts.length < 4)
        continue;

      TaskEntry task = findTask(parts[0]);
      if (task == null || "approved".equals(task.status))
        continue;

      task.urgency = Math.min(MAX_URGENCY, Math.max(task.urgency, parseInt(parts[2], task.urgency + 1)));
      task.deadlineAt = parseLong(parts[3], System.currentTimeMillis() + DEFAULT_DEADLINE_MS);
      task.correctionRequired = true;

      if (!"assigned".equals(task.status)) {
        task.status = "pending";
      }

      sendToGui("TASK_STATUS:" + task.id + ":" + task.type + ":prazo recalculado (urgência " + task.urgency + ")");
    }
  }

  private void sendTaskToAnalyst(TaskEntry task, boolean timeout) {
    String targetType = task.target != null ? task.target.getType().name() : "NONE";
    int targetX = task.target != null ? task.target.getX() : -1;
    int targetY = task.target != null ? task.target.getY() : -1;
    int progress = task.target != null ? task.target.getProgress() : task.lastProgress;

    String payload = String.join("|",
        task.id,
        nullToEmpty(task.worker),
        task.type,
        Integer.toString(targetX),
        Integer.toString(targetY),
        targetType,
        Integer.toString(progress),
        Long.toString(task.deadlineAt),
        Long.toString(System.currentTimeMillis()),
        Integer.toString(task.urgency),
        Integer.toString(task.corrections),
        Boolean.toString(timeout),
        Integer.toString(task.lastWorkX),
        Integer.toString(task.lastWorkY),
        requiredSkillKey(task));

    ACLMessage verify = new ACLMessage(ACLMessage.REQUEST);
    verify.addReceiver(new AID("analyst", AID.ISLOCALNAME));
    verify.setContent("VERIFY_TASK:" + payload);
    send(verify);
  }

  private void distributeTasks() {
    List<TaskEntry> pending = tasks.stream()
        .filter(t -> "pending".equals(t.status))
        .sorted(Comparator
            .comparingInt((TaskEntry task) -> task.urgency).reversed()
            .thenComparingLong(task -> task.deadlineAt))
        .collect(Collectors.toList());

    for (TaskEntry task : pending) {
      WorkerInfo best = findBestWorker(task);
      if (best != null) {
        best.busy = true;
        task.status = "assigned";
        task.worker = best.name;

        String extra = ":-1:-1:NONE";
        if (task.target != null) {
          extra = ":" + task.target.getX() + ":" + task.target.getY()
              + ":" + task.target.getType().name();
        }
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID(best.name, AID.ISLOCALNAME));
        msg.setContent("ASSIGN_TASK:" + task.id + ":" + task.type + extra
            + ":" + task.deadlineAt + ":" + task.urgency + ":" + task.correctionRequired);
        send(msg);

        String action = task.correctionRequired ? "correção atribuída a " : "atribuída a ";
        sendToGui("TASK_STATUS:" + task.id + ":" + task.type + ":" + action + best.name
            + " (urgência " + task.urgency + ")");
      }
    }
  }

  private WorkerInfo findBestWorker(TaskEntry task) {
    WorkerInfo best = null;
    int bestScore = -1;

    for (WorkerInfo w : workers) {
      if (w.busy)
        continue;
      if (w.energy < 30)
        continue;

      SkillType requiredSkill = requiredSkill(task);
      int score = 0;

      // Skill match
      if (requiredSkill != null) {
        String reqKey = requiredSkill.getKey().toLowerCase();
        if (w.skill.toLowerCase().contains(reqKey)) {
          score += 50 + w.level * 10;
        } else {
          // Check if skill is in the same category
          SkillType ws = SkillType.fromKey(w.skill);
          if (ws != null && requiredSkill.getCategory() == ws.getCategory()) {
            score += 20 + w.level * 5;
          }
        }
      } else {
        score += 10;
      }

      // Proximity bonus
      if (task.target != null) {
        double dist = Math.hypot(w.x - task.target.getX(), w.y - task.target.getY());
        score += Math.max(0, 30 - (int) dist / 5);
      }

      // Energy bonus
      score += w.energy / 10;

      // Urgent tasks should prefer workers with higher practical level.
      score += task.urgency * Math.max(1, w.level);

      if (score > bestScore) {
        bestScore = score;
        best = w;
      }
    }
    return best;
  }

  private void handleColonyAnalysis(String report) {
    if (report.isEmpty())
      return;
    ColonyMap map = Main.colonyMap;
    int tasksCreated = 0;

    String[] items = report.split("\\|");
    for (String item : items) {
      if (item.isEmpty())
        continue;

      if (item.startsWith("NEED_HOUSE:")) {
        int needed = Integer.parseInt(item.split(":")[1]);
        for (int i = 0; i < Math.min(2, needed); i++) {
          if (!hasIncompleteBuildFor(BuildingType.HOUSE)) {
            createBuildTask(BuildingType.HOUSE);
            tasksCreated++;
          }
        }
      } else if (item.startsWith("NEED_WORKSHOP:")) {
        String typeName = item.split(":")[1];
        try {
          BuildingType bt = BuildingType.valueOf(typeName);
          if (!hasWorkshop(bt) && !hasIncompleteBuildFor(bt)) {
            createBuildTask(bt);
            tasksCreated++;
          }
        } catch (IllegalArgumentException e) {
        }
      } else if (item.startsWith("CONTINUE_BUILD:")) {
        String[] coord = item.split(":")[1].split(",");
        if (coord.length >= 3) {
          try {
            int bx = Integer.parseInt(coord[0]);
            int by = Integer.parseInt(coord[1]);
            ColonyBuilding b = map.getBuildingAt(bx, by);
            if (b != null && b.getProgress() < 100 && !hasTaskFor(b)) {
              createBuildTask(b);
              tasksCreated++;
            }
          } catch (Exception e) {
          }
        }
      } else if (item.startsWith("IDLE_WORKERS:")) {
        int idleCount = parseInt(item.split(":")[1], 0);
        if (idleCount > 0 && getOpenTaskCount("mine") == 0) {
          createTask("mine", "mine");
          tasksCreated++;
        }
        if (idleCount > 1 && getOpenTaskCount("woodcut") == 0) {
          createTask("woodcut", "woodcut");
          tasksCreated++;
        }
      }
    }

    if (tasksCreated > 0) {
      sendToGui("LOG:Gerente: " + tasksCreated + " tarefa(s) criada(s) pela análise do analista.");
    }
  }

  private boolean hasWorkshop(BuildingType type) {
    return Main.colonyMap.getBuildings().stream()
        .anyMatch(b -> b.getType() == type);
  }

  private boolean hasTaskFor(ColonyBuilding b) {
    return tasks.stream().anyMatch(t -> t.target == b && !"approved".equals(t.status));
  }

  private boolean hasIncompleteBuildFor(BuildingType bt) {
    return tasks.stream().anyMatch(t -> !"approved".equals(t.status) && t.target != null && t.target.getType() == bt);
  }

  private long getPendingTaskCount(String type) {
    return tasks.stream()
        .filter(t -> t.type.equals(type) && "pending".equals(t.status))
        .count();
  }

  private long getOpenTaskCount(String type) {
    return tasks.stream()
        .filter(t -> t.type.equals(type) && !"approved".equals(t.status))
        .count();
  }

  private void createBuildTask(BuildingType type) {
    ColonyMap map = Main.colonyMap;
    int[] spot = map.findSpotFor(type);
    if (spot == null) {
      sendToGui("LOG:Gerente: não achou lugar para " + type.getName());
      return;
    }
    ColonyBuilding building = map.addBuilding(spot[0], spot[1], type);
    sendToGui("LOG:Gerente: nova construção planejada: " + type.getName()
        + " em (" + spot[0] + "," + spot[1] + ")");
    createTask(type.name() + "_build", "build", building);
  }

  private void createBuildTask(ColonyBuilding building) {
    if (hasTaskFor(building))
      return;
    sendToGui("LOG:Gerente: continuando construção em (" + building.getX() + "," + building.getY() + ")");
    createTask("build_" + building.getX() + "_" + building.getY(), "build", building);
  }

  private void createTask(String id, String type) {
    createTask(id, type, null);
  }

  private void createTask(String id, String type, ColonyBuilding target) {
    taskCounter++;
    String taskId = String.valueOf(taskCounter);
    TaskEntry te = new TaskEntry(taskId, type);
    te.target = target;
    te.urgency = initialUrgency(type, target);
    te.deadlineAt = System.currentTimeMillis() + deadlineFor(type, target);
    tasks.add(te);
    String status = "criada";
    if (target != null) {
      status = "criada (" + target.getType().getName() + ")";
    }
    sendToGui("TASK_STATUS:" + taskId + ":" + type + ":" + status + " | urgência " + te.urgency);
  }

  private int initialUrgency(String type, ColonyBuilding target) {
    if (target == null) {
      return RAW_TASK_TYPES.contains(type) ? 2 : 1;
    }
    if (target.getType() == BuildingType.HOUSE)
      return 4;
    if (target.getType() == BuildingType.STOCKPILE)
      return 3;
    return 3;
  }

  private long deadlineFor(String type, ColonyBuilding target) {
    if (target != null || "build".equals(type)) {
      return BUILD_DEADLINE_MS;
    }
    return DEFAULT_DEADLINE_MS;
  }

  private void sendWorkforceTo(String requester) {
    StringBuilder sb = new StringBuilder("WORKFORCE_DATA:");
    for (WorkerInfo w : workers) {
      sb.append(w.name).append("~").append(w.skill).append("~").append(w.level)
          .append("~").append(w.energy).append("~").append(w.busy ? 1 : 0)
          .append("~").append(w.x).append("~").append(w.y).append("|");
    }
    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    msg.addReceiver(new AID(requester, AID.ISLOCALNAME));
    msg.setContent(sb.toString());
    send(msg);
  }

  private void sendTaskQueueToAnalyst() {
    if (tasks.isEmpty())
      return;

    StringBuilder payload = new StringBuilder("TASK_QUEUE_REPORT:");
    for (TaskEntry task : tasks) {
      if ("approved".equals(task.status))
        continue;

      int targetX = task.target != null ? task.target.getX() : -1;
      int targetY = task.target != null ? task.target.getY() : -1;
      String building = task.target != null ? task.target.getType().name() : "NONE";

      payload.append(task.id).append("~")
          .append(task.type).append("~")
          .append(task.status).append("~")
          .append(nullToEmpty(task.worker)).append("~")
          .append(task.urgency).append("~")
          .append(task.deadlineAt).append("~")
          .append(task.createdAt).append("~")
          .append(targetX).append("~")
          .append(targetY).append("~")
          .append(building).append("~")
          .append(task.corrections).append("~")
          .append(requiredSkillKey(task))
          .append("|");
    }

    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    msg.addReceiver(new AID("analyst", AID.ISLOCALNAME));
    msg.setContent(payload.toString());
    send(msg);
  }

  private void handleTerrainAnalysis(String report) {
    if (report.isBlank())
      return;

    String[] items = report.split("\\|");
    for (String item : items) {
      String[] parts = item.split("~");
      if (parts.length < 4)
        continue;

      String zoneType = parts[0];
      int zoneX = parseInt(parts[1], -1);
      int zoneY = parseInt(parts[2], -1);
      if (zoneX < 0 || zoneY < 0)
        continue;

      String key = zoneType + ":" + zoneX + ":" + zoneY;
      if (!mappedZones.add(key))
        continue;

      if ("STONE".equals(zoneType)) {
        Main.colonyMap.setZoneName(zoneX, zoneY, "extração de pedra");
        if (getOpenTaskCount("mine") == 0) {
          createTask("mine", "mine");
        }
        sendToGui("LOG:Gerente definiu zona de mineração em (" + zoneX + "," + zoneY + ")");
      } else if ("WOOD".equals(zoneType)) {
        Main.colonyMap.setZoneName(zoneX, zoneY, "extração de madeira");
        if (getOpenTaskCount("woodcut") == 0) {
          createTask("woodcut", "woodcut");
        }
        sendToGui("LOG:Gerente definiu zona de corte de madeira em (" + zoneX + "," + zoneY + ")");
      } else if ("BUILD_ZONE".equals(zoneType)) {
        Main.colonyMap.setZoneName(zoneX, zoneY, "construção planejada");
        sendToGui("LOG:Gerente reservou zona de construção em (" + zoneX + "," + zoneY + ")");
      }
    }
  }

  private TaskEntry findTask(String taskId) {
    for (TaskEntry task : tasks) {
      if (task.id.equals(taskId))
        return task;
    }
    return null;
  }

  private SkillType requiredSkill(TaskEntry task) {
    if (task.target != null) {
      return switch (task.target.getType()) {
        case HOUSE, CARPENTER, STOCKPILE, WELL -> SkillType.CARPENTER;
        case MASON, CRAFTER, TRADER, HOSPITAL, BARRACKS -> SkillType.MASON;
        case SMITH -> SkillType.BLACKSMITH;
        case FARM -> SkillType.PLANTER;
        case KITCHEN -> SkillType.COOK;
      };
    }

    SkillType inferred = DwarfSkills.inferFromTaskType(task.type);
    return inferred != null ? inferred : SkillType.CARPENTER;
  }

  private String requiredSkillKey(TaskEntry task) {
    SkillType skill = requiredSkill(task);
    return skill != null ? skill.getKey() : "general";
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

  private String nullToEmpty(String value) {
    return value == null ? "" : value;
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
