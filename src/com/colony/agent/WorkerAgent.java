package com.colony.agent;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import com.colony.model.*;
import com.colony.model.ColonyMap.ColonyBuilding;
import com.colony.Main;
import java.util.*;

public class WorkerAgent extends Agent {
  private String npcName;
  private String workerType;
  private int health = 100;
  private int energy = 100;
  private int fome = 100;
  private int sede = 100;
  private DwarfSkills skills = new DwarfSkills();
  private SkillType primarySkill;
  private boolean regAnalyst = false;
  private boolean regManager = false;
  private String currentTaskId = null;
  private int ticksWorked = 0;
  private int npcX = ColonyMap.WIDTH / 2 + (int) (Math.random() * 12 - 6);
  private int npcY = ColonyMap.HEIGHT / 2 + (int) (Math.random() * 12 - 6);
  private int targetX = npcX, targetY = npcY;
  private boolean hasHouse = false;
  private boolean hasWorkshop = false;
  private int taskTargetX = -1, taskTargetY = -1;
  private String taskTargetBuilding; // building type name for construction tasks
  private long currentTaskDeadline = 0L;
  private int currentTaskUrgency = 1;
  private boolean currentTaskCorrection = false;

  // Tipos de trabalho que NÃO precisam de construção
  private static final Set<String> RAW_TASKS = Set.of(
      "mine", "mining", "dig", "woodcut", "woodcutting", "chop",
      "harvest", "gather", "collect", "fish", "fishing", "water");

  protected void setup() {
    Object[] args = getArguments();
    if (args != null && args.length > 0) {
      this.workerType = args[0].toString();
    } else {
      this.workerType = "general";
    }
    this.npcName = getLocalName();
    this.primarySkill = initSkills();

    System.out.println(npcName + ": " + traduzirTipo(workerType) + " iniciado"
        + " | HP:" + health + " EN:" + energy
        + " | Skill: " + primarySkill.getDisplayName()
        + " (" + skills.getRank(primarySkill) + " lv" + skills.getLevel(primarySkill) + ")");

    addBehaviour(new CyclicBehaviour() {
      public void action() {
        if (!regAnalyst) {
          ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
          m.addReceiver(new AID("analyst", AID.ISLOCALNAME));
          m.setContent("REGISTER_SKILL:" + npcName + ":" + primarySkill.getKey());
          send(m);
        }
        if (!regManager) {
          ACLMessage m = new ACLMessage(ACLMessage.REQUEST);
          m.addReceiver(new AID("manager", AID.ISLOCALNAME));
          m.setContent("REGISTER_WORKER");
          send(m);
        }

        ACLMessage msg = receive();
        if (msg != null) {
          String content = msg.getContent();
          String sender = msg.getSender().getLocalName();

          if ("REGISTERED".equals(content)) {
            if (sender.equals("analyst") && !regAnalyst) {
              regAnalyst = true;
              regComplete();
            } else if (sender.equals("manager") && !regManager) {
              regManager = true;
              regComplete();
            }
          } else if (content.startsWith("ASSIGN_TASK:")) {
            String[] p = content.split(":");
            String taskId = p[1];
            String taskType = p[2];
            taskTargetX = -1;
            taskTargetY = -1;
            taskTargetBuilding = null;
            if (p.length >= 6) {
              try {
                taskTargetX = Integer.parseInt(p[3]);
                taskTargetY = Integer.parseInt(p[4]);
                taskTargetBuilding = "NONE".equals(p[5]) ? null : p[5];
              } catch (NumberFormatException e) {
              }
            }
            currentTaskDeadline = p.length >= 7 ? parseLong(p[6], 0L) : 0L;
            currentTaskUrgency = p.length >= 8 ? parseInt(p[7], 1) : 1;
            currentTaskCorrection = p.length >= 9 && Boolean.parseBoolean(p[8]);
            acceptTask(taskId, taskType);
          }
        } else {
          block(300);
        }
      }
    });

    addBehaviour(new CyclicBehaviour() {
      public void action() {
        if (regManager && currentTaskId == null) {
          autoAct();
        }
        block(3000);
      }
    });

    // Periodic info report to manager
    addBehaviour(new TickerBehaviour(this, 5000) {
      protected void onTick() {
        if (health <= 0 || fome <= 0 || sede <= 0) {
          sendGui("LOG: ☠️ " + npcName + " MORREU (Fome:" + fome + " Sede:" + sede + " HP:" + health + ")");
          doDelete();
          return;
        }

        int dropRate = (health < 100) ? 2 : 1;
        fome -= dropRate;
        sede -= dropRate;

        if (health < 100) {
          health = Math.min(100, health + 2); // Regen lentamente
        }

        if (regManager) {
          sendInfoToManager();
        }
      }
    });
  }

  private void regComplete() {
    if (regAnalyst && regManager) {
      Main.colonyMap.setNpcPosition(npcName, npcX, npcY);
      sendGui("NPC_POSITION:" + npcName + ":" + npcX + ":" + npcY);
      sendGui("WORKER_STATUS:" + npcName + ":" + primarySkill.getKey()
          + ":" + skills.getLevel(primarySkill) + ":" + skills.getRank(primarySkill) + ":ocioso:" + energy + ":" + fome
          + ":" + sede);
      sendWorkerDetails();
      sendGui("LOG:" + npcName + " (" + traduzirTipo(workerType) + ") registrado.");
    }
  }

  private SkillType initSkills() {
    Random rand = new Random();
    String[][] pools = {
        { "Wood cutter" }, { "Carpenter", "Bowyer", "Wood crafter" },
        { "Miner", "Stonecutter" }, { "Mason", "Stone carver", "Engraver", "Stone crafter" },
        { "Blacksmith", "Armorsmith", "Weaponsmith", "Furnace operator", "Metal crafter" },
        { "Planter", "Cook", "Brewer", "Butcher", "Herbalist" },
        { "Diagnostician", "Surgeon", "Bone doctor" },
        { "Fighter", "Axeman", "Shield user", "Dodger", "Wrestler" },
        { "Mechanic", "Siege engineer" },
        { "Gem cutter", "Gem setter", "Bone carver", "Potter", "Weaver", "Clothier", "Glassmaker" },
        { "Fisherdwarf", "Fish cleaner" },
        { "Trapper", "Animal caretaker", "Animal trainer" },
    };
    int idx = switch (workerType) {
      case "woodcutter" -> 0;
      case "builder", "carpenter" -> 1;
      case "miner" -> 2;
      case "mason", "stonemason", "marbleworker", "marmorist" -> 3;
      case "smith" -> 4;
      case "farmer" -> 5;
      case "doctor" -> 6;
      case "fighter" -> 7;
      case "engineer" -> 8;
      case "craftsman" -> 9;
      case "fisher" -> 10;
      default -> rand.nextInt(pools.length);
    };
    String[] skillPool = pools[Math.min(idx, pools.length - 1)];
    SkillType primary = null;
    for (String sk : skillPool) {
      SkillType st = SkillType.fromKey(sk);
      if (st != null) {
        skills.setLevel(st, 1 + rand.nextInt(3));
        if (primary == null)
          primary = st;
      }
    }
    if ("marbleworker".equals(workerType) || "marmorist".equals(workerType)) {
      primary = SkillType.STONE_CARVER;
      skills.setLevel(primary, 2 + rand.nextInt(2));
    }
    if (primary == null) {
      primary = SkillType.WOOD_CUTTER;
      skills.setLevel(primary, 1);
    }
    return primary;
  }

  private void acceptTask(String taskId, String taskType) {
    String t = taskType.toLowerCase();
    boolean isRaw = RAW_TASKS.stream().anyMatch(t::contains);

    // Rejeita se estiver muito cansado ou machucado
    if (health < 100) {
      sendGui("LOG:" + npcName + " está ferido (HP=" + health + "), rejeitou tarefa");
      sendReject(taskId);
      return;
    }

    if (energy < 15) {
      sendGui("LOG:" + npcName + " está exausto (energia=" + energy + "), rejeitou tarefa");
      sendReject(taskId);
      return;
    }

    // Verifica se precisa de oficina e não tem
    if (!isRaw && !t.contains("build")) {
      BuildingType needed = workshopTypeForTask(taskType);
      if (needed != null && !hasCompletedBuilding(needed)) {
        sendGui("LOG:" + npcName + " não tem oficina de " + needed.getName()
            + ", rejeitou tarefa");
        sendReject(taskId);
        return;
      }
    }

    SkillType taskSkill = DwarfSkills.inferFromTaskType(taskType);
    if (taskSkill == null)
      taskSkill = primarySkill;

    if (skills.getLevel(taskSkill) == 0) {
      skills.setLevel(taskSkill, 1);
      sendGui("LOG:" + npcName + " aprendeu " + taskSkill.getDisplayName() + " (nível 1)");
    }

    currentTaskId = taskId;
    int skLv = skills.getLevel(taskSkill);
    sendGui("WORKER_STATUS:" + npcName + ":" + taskSkill.getKey()
        + ":" + skLv + ":" + skills.getRank(taskSkill) + ":ocupado:" + energy + ":" + fome + ":" + sede);
    String correctionText = currentTaskCorrection ? "correção de " : "";
    sendGui("LOG:" + npcName + " começou " + correctionText + taskSkill.getDisplayName()
        + " (" + skills.getRank(taskSkill) + " lv" + skLv + ", urgência " + currentTaskUrgency + ")");

    final SkillType finalSkill = taskSkill;
    addBehaviour(new OneShotBehaviour() {
      public void action() {
        workOnTask(taskId, finalSkill, taskType);
      }
    });
  }

  private void sendReject(String taskId) {
    try {
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.addReceiver(new AID("manager", AID.ISLOCALNAME));
      msg.setContent("TASK_REJECTED:" + taskId);
      send(msg);
      currentTaskId = null;
      currentTaskCorrection = false;
    } catch (Exception ignored) {
    }
  }

  private BuildingType workshopTypeForTask(String taskType) {
    if (taskType == null)
      return null;
    String t = taskType.toLowerCase();
    if (t.contains("carpenter") || t.contains("bowyer") || t.contains("wood craft"))
      return BuildingType.CARPENTER;
    if (t.contains("mason") || t.contains("stone") || t.contains("engrave"))
      return BuildingType.MASON;
    if (t.contains("smith") || t.contains("forge") || t.contains("furnace"))
      return BuildingType.SMITH;
    if (t.contains("craft") || t.contains("potter") || t.contains("bone") || t.contains("weaver"))
      return BuildingType.CRAFTER;
    if (t.contains("cook") || t.contains("brew") || t.contains("butcher"))
      return BuildingType.KITCHEN;
    if (t.contains("diagnos") || t.contains("surgeon") || t.contains("bone doctor"))
      return BuildingType.HOSPITAL;
    if (t.contains("farm") || t.contains("plant"))
      return BuildingType.FARM;
    return null;
  }

  private void workOnTask(String taskId, SkillType taskSkill, String taskType) {
    ColonyMap map = Main.colonyMap;
    Random rand = new Random();
    String t = taskType.toLowerCase();
    boolean isRaw = RAW_TASKS.stream().anyMatch(t::contains);

    int workX, workY;
    int actionX = -1, actionY = -1;
    if (isRaw) {
      int[] rawTile = findRawWorkTile(taskType);
      actionX = rawTile[0];
      actionY = rawTile[1];
      int[] openTile = map.findNearestOpenTile(actionX, actionY);
      workX = openTile[0];
      workY = openTile[1];
    } else if (taskTargetX >= 0 && taskTargetY >= 0) {
      ColonyBuilding target = map.getBuildingAt(taskTargetX, taskTargetY);
      if (target != null) {
        int centerX = target.getX() + target.getType().getWidth() / 2;
        int centerY = target.getY() + target.getType().getHeight() / 2;
        int[] openTile = map.findNearestOpenTile(centerX, centerY);
        workX = openTile[0];
        workY = openTile[1];
      } else {
        workX = taskTargetX;
        workY = taskTargetY;
      }
    } else {
      BuildingType bt = workshopTypeForTask(taskType);
      ColonyBuilding workshop = map.findNearestUnowned(bt, npcX, npcY);
      if (workshop != null) {
        // Fetch materials from stockpile first
        ColonyBuilding stockpile = map.findNearestUnowned(BuildingType.STOCKPILE, npcX, npcY);
        if (stockpile != null) {
          sendGui("LOG:" + npcName + " está buscando materiais no armazém...");
          int stx = stockpile.getX() + stockpile.getType().getWidth() / 2;
          int sty = stockpile.getY() + stockpile.getType().getHeight() / 2;
          moveTowards(stx, sty);
          sleep(500); // Pegando recursos
        } else {
          sendGui("LOG:" + npcName + " precisava de materiais mas não há depósito!");
        }

        workX = workshop.getX() + workshop.getType().getWidth() / 2;
        workY = workshop.getY() + workshop.getType().getHeight() / 2;
      } else {
        workX = npcX + rand.nextInt(11) - 5;
        workY = npcY + rand.nextInt(11) - 5;
      }
    }
    workX = Math.max(1, Math.min(ColonyMap.WIDTH - 2, workX));
    workY = Math.max(1, Math.min(ColonyMap.HEIGHT - 2, workY));
    moveTowards(workX, workY);

    int duration = 1000 + rand.nextInt(2000);
    int xpGain = 30 + rand.nextInt(80);

    System.out.println(npcName + ": " + taskSkill.getDisplayName()
        + " (lv" + skills.getLevel(taskSkill) + ")");

    // Consumo de material e lógica de pesca
    if (t.contains("fish")) {
      if (!Main.resources.consume("vara de pesca", 1)) {
        sendGui("LOG:" + npcName + " tentou pescar mas não tem vara de pesca no armazém!");
        sendReject(taskId);
        return;
      } else {
        sendGui("UPDATE_RESOURCES");
      }
    }

    // Execute task
    if (isRaw && map.inBounds(actionX, actionY)) {
      TerrainTile tile = map.getTile(actionX, actionY);
      if (tile == TerrainTile.TREE && t.contains("wood")) {
        map.setTile(actionX, actionY, TerrainTile.GRASS);
      } else if ((tile == TerrainTile.STONE || tile == TerrainTile.MOUNTAIN)
          && (t.contains("mine") || t.contains("dig"))) {
        map.setTile(actionX, actionY, TerrainTile.DIRT);
      }
      sendGui("BUILD_UPDATE:" + actionX + ":" + actionY);
    } else if (taskTargetX >= 0 && taskTargetY >= 0) {
      // Building construction: work on the target building
      ColonyBuilding target = map.getBuildingAt(taskTargetX, taskTargetY);
      if (target != null && target.getProgress() < 100) {
        int progressGain = 20 + Math.min(15, skills.getLevel(taskSkill) * 3) + currentTaskUrgency;
        target.setProgress(target.getProgress() + progressGain);
        sendGui("LOG:" + npcName + " construiu " + target.getType().getName()
            + " (" + target.getProgress() + "%) em (" + taskTargetX + "," + taskTargetY + ")");
        sendGui("BUILD_UPDATE:" + taskTargetX + ":" + taskTargetY);
      }
      if (taskTargetBuilding != null) {
        BuildingType bt = BuildingType.valueOf(taskTargetBuilding);
        if (bt == BuildingType.HOUSE && !hasHouse && target != null && target.getProgress() >= 100) {
          map.assignHome(npcName, target);
          hasHouse = true;
          sendGui("LOG:" + npcName + " agora tem casa!");
        } else if (!hasWorkshop && target != null && target.getProgress() >= 100) {
          hasWorkshop = true;
          sendGui("LOG:" + npcName + " agora tem " + target.getType().getName());
        }
      }
    } else if (!isRaw) {
      map.buildProgress(npcName, npcX, npcY);
      sendGui("BUILD_UPDATE:-1:-1");
    }

    int energyCost = 10 + rand.nextInt(6);
    energy = Math.max(0, energy - energyCost);
    ticksWorked++;
    sleep(Math.min(duration, 1500));

    int lvGain = skills.addXp(taskSkill, xpGain);
    int newLv = skills.getLevel(taskSkill);

    sendGui("LOG:" + npcName + " " + (isRaw ? "coletou" : "produziu") + " em (" + npcX + "," + npcY + ")"
        + " (-" + energyCost + " EN, restam " + energy + ") (+" + xpGain + " XP " + taskSkill.getDisplayName()
        + " lv" + newLv + " " + skills.getRank(taskSkill) + ")");
    if (lvGain > 0) {
      sendGui("LOG:" + npcName + " SUBIU " + taskSkill.getDisplayName()
          + " para nível " + newLv + " (" + skills.getRank(taskSkill) + ")!");
    }

    // Gera recursos (ex: pesca, lenha, pedra) e gasta (ex: constrói)
    if (t.contains("fish") || t.contains("farm") || t.contains("harvest") || t.contains("plant")) {
      Main.resources.add("comida", 1 + rand.nextInt(2));
      sendGui("UPDATE_RESOURCES");
    } else if (t.contains("wood") || t.contains("chop")) {
      Main.resources.add("madeira", 1 + rand.nextInt(2));
      sendGui("UPDATE_RESOURCES");
    } else if (t.contains("mine") || t.contains("dig")) {
      Main.resources.add("pedra", 1 + rand.nextInt(2));
      if (rand.nextInt(8) == 0)
        Main.resources.add("ferro", 1);
      sendGui("UPDATE_RESOURCES");
    } else if (t.contains("build") || t.contains("construct")) {
      if (Main.resources.consume("madeira", 2)) {
        sendGui("UPDATE_RESOURCES");
      } else {
        sendGui("LOG: ⚠️ " + npcName + " construiu sem material (simulado)");
      }
    } else if (t.contains("craft") || t.contains("carpenter")) {
      // Cria uma vara de pesca as vezes
      if (rand.nextInt(5) == 0 && Main.resources.consume("madeira", 1)) {
        Main.resources.add("vara de pesca", 1);
        sendGui("UPDATE_RESOURCES");
      }
    }

    int progress = 100;
    if (taskTargetX >= 0 && taskTargetY >= 0) {
      ColonyBuilding target = map.getBuildingAt(taskTargetX, taskTargetY);
      progress = target != null ? target.getProgress() : 0;
    }

    boolean late = currentTaskDeadline > 0 && System.currentTimeMillis() > currentTaskDeadline;
    ACLMessage done = new ACLMessage(ACLMessage.INFORM);
    done.addReceiver(new AID("manager", AID.ISLOCALNAME));
    String messageType = late ? "TASK_TIMEOUT:" : "TASK_COMPLETE:";
    int reportX = isRaw ? actionX : npcX;
    int reportY = isRaw ? actionY : npcY;
    done.setContent(messageType + taskId + "|" + reportX + "|" + reportY + "|" + progress + "|" + taskType);
    send(done);
    if (late) {
      sendGui("LOG:" + npcName + " avisou estouro de prazo na tarefa " + taskId);
    }

    currentTaskId = null;
    currentTaskCorrection = false;
    sendGui("WORKER_STATUS:" + npcName + ":" + taskSkill.getKey()
        + ":" + newLv + ":" + skills.getRank(taskSkill) + ":ocioso:" + energy + ":" + fome + ":" + sede);
  }

  private void autoAct() {
    ColonyMap map = Main.colonyMap;
    Random rand = new Random();

    // Descansa se estiver com pouca energia (prioridade)
    if (energy <= 30) {
      ColonyBuilding home = map.getHome(npcName);
      if (home != null) {
        int hx = home.getX() + home.getType().getWidth() / 2;
        int hy = home.getY() + home.getType().getHeight() / 2;
        moveTowards(hx, hy);

        if (npcX == hx && npcY == hy) {
          int restAmount = 10 + rand.nextInt(15);
          energy = Math.min(100, energy + restAmount);
          sendGui("LOG:" + npcName + " descansou em sua casa (+" + restAmount + " EN, agora " + energy + "/100)");
          sleep(1000);
        } else {
          sendGui("LOG:" + npcName + " não conseguiu chegar em casa para dormir!");
          sleep(1000);
        }
      } else {
        sendGui("LOG:" + npcName + " não tem casa e dormiu no relento!");
        int restAmount = 5 + rand.nextInt(10);
        energy = Math.min(100, energy + restAmount);
        sleep(1500);
      }
      return;
    }

    // Bebe água se estiver com sede
    if (sede <= 40) {
      ColonyBuilding stockpile = map.findNearestUnowned(BuildingType.STOCKPILE, npcX, npcY);
      if (stockpile != null)
        moveTowards(stockpile.getX() + stockpile.getType().getWidth() / 2,
            stockpile.getY() + stockpile.getType().getHeight() / 2);

      if (Main.resources.consume("agua", 1)) {
        sede = 100;
        sendGui("LOG: 💧 " + npcName + " bebeu água no armazém.");
        sendGui("UPDATE_RESOURCES");
      } else {
        sendGui("LOG: ⚠️ " + npcName + " está morrendo de sede e não há água!");
        health -= 5;
      }
      sleep(1000);
      return;
    }

    // Come se estiver com fome
    if (fome <= 40) {
      ColonyBuilding stockpile = map.findNearestUnowned(BuildingType.STOCKPILE, npcX, npcY);
      if (stockpile != null)
        moveTowards(stockpile.getX() + stockpile.getType().getWidth() / 2,
            stockpile.getY() + stockpile.getType().getHeight() / 2);

      if (Main.resources.consume("comida", 1)) {
        fome = 100;
        sendGui("LOG: 🍗 " + npcName + " comeu no armazém.");
        sendGui("UPDATE_RESOURCES");
      } else {
        sendGui("LOG: ⚠️ " + npcName + " está faminto e não há comida!");
        health -= 5;
      }
      sleep(1000);
      return;
    }

    // Combate e Caça
    for (com.colony.model.Animal a : map.getAnimals()) {
      if (Math.abs(a.x - npcX) <= 2 && Math.abs(a.y - npcY) <= 2) {
        boolean isHunter = primarySkill == SkillType.WOOD_CUTTER || workerType.toLowerCase().contains("fighter")
            || workerType.toLowerCase().contains("hunter");

        if (a.dead) {
          // Esfolar
          int meat = isHunter ? 10 : 1;
          Main.resources.add("comida", meat);
          sendGui("LOG: 🥩 " + npcName + " esfolou a " + a.type + " e pegou " + meat + " de carne!");
          map.removeAnimal(a);
          sendGui("UPDATE_RESOURCES");
          sleep(1000);
          return;
        }

        if (a.aggressive) {
          sendGui("LOG: ⚔️ " + npcName + " está sendo atacado por um " + a.type + "!");
          int dmg = isHunter ? 5 : 15;
          health -= dmg;
          a.hp -= isHunter ? 30 : 10;
          if (a.hp <= 0) {
            a.dead = true;
            a.rotTimer = 30;
            sendGui("LOG: ☠️ " + npcName + " matou o " + a.type + "!");
            a.type = "Carcaça de " + a.type;
          }
          sleep(1000);
          return;
        } else if (isHunter || fome <= 30) {
          sendGui("LOG: 🏹 " + npcName + " está caçando um " + a.type + "...");
          a.hp -= 20;
          if (a.hp <= 0) {
            a.dead = true;
            a.rotTimer = 30;
            sendGui("LOG: ☠️ " + npcName + " abateu o " + a.type + "!");
            a.type = "Carcaça de " + a.type;
          }
          sleep(1000);
          return;
        }
      }
    }

    // Anda pelo mapa (só se tiver energia e não estiver morrendo)
    if (energy > 40 && rand.nextInt(4) == 0) {
      int dx = rand.nextInt(11) - 5;
      int dy = rand.nextInt(11) - 5;
      int nx = Math.max(1, Math.min(ColonyMap.WIDTH - 2, npcX + dx));
      int ny = Math.max(1, Math.min(ColonyMap.HEIGHT - 2, npcY + dy));
      if (!map.getTile(nx, ny).isBlocksMovement() && map.getBuildingAt(nx, ny) == null) {
        moveTowards(nx, ny);
      }
    }

    if (energy > 60) {
      SkillType lowest = skills.getLowestSkill();
      if (lowest != null && skills.getLevel(lowest) < 5) {
        int xp = 10 + rand.nextInt(15);
        int lv = skills.addXp(lowest, xp);
        if (lv > 0)
          sendGui("LOG:" + npcName + " SUBIU " + lowest.getDisplayName()
              + " para nível " + skills.getLevel(lowest) + "!");
      }
      energy = Math.max(0, energy - 5);
    }
  }

  private int[] findRawWorkTile(String taskType) {
    ColonyMap map = Main.colonyMap;
    String type = taskType.toLowerCase();
    TerrainTile firstChoice = type.contains("wood") ? TerrainTile.TREE : TerrainTile.STONE;
    TerrainTile secondChoice = type.contains("wood") ? TerrainTile.TREE : TerrainTile.MOUNTAIN;

    for (int radius = 1; radius <= 80; radius++) {
      for (int offsetY = -radius; offsetY <= radius; offsetY++) {
        for (int offsetX = -radius; offsetX <= radius; offsetX++) {
          if (Math.abs(offsetX) != radius && Math.abs(offsetY) != radius)
            continue;

          int candidateX = npcX + offsetX;
          int candidateY = npcY + offsetY;
          if (!map.inBounds(candidateX, candidateY))
            continue;

          TerrainTile tile = map.getTile(candidateX, candidateY);
          if (tile == firstChoice || tile == secondChoice) {
            return new int[] { candidateX, candidateY };
          }
        }
      }
    }

    return new int[] {
        Math.max(1, Math.min(ColonyMap.WIDTH - 2, npcX)),
        Math.max(1, Math.min(ColonyMap.HEIGHT - 2, npcY))
    };
  }

  private boolean hasCompletedBuilding(BuildingType type) {
    for (ColonyBuilding building : Main.colonyMap.getBuildings()) {
      if (building.getType() == type && building.getProgress() >= 100) {
        return true;
      }
    }
    return false;
  }

  private void moveTowards(int tx, int ty) {
    targetX = tx;
    targetY = ty;
    List<int[]> path = Main.colonyMap.findPath(npcX, npcY, targetX, targetY);

    if (path != null && !path.isEmpty()) {
      for (int[] step : path) {
        npcX = step[0];
        npcY = step[1];
        Main.colonyMap.setNpcPosition(npcName, npcX, npcY);
        sendGui("NPC_POSITION:" + npcName + ":" + npcX + ":" + npcY);
        sleep(80);
      }
    } else {
      // Fallback (for example if already there or no path)
      Main.colonyMap.setNpcPosition(npcName, npcX, npcY);
      sendGui("NPC_POSITION:" + npcName + ":" + npcX + ":" + npcY);
    }
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
    }
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

  private void sendWorkerDetails() {
    StringBuilder sb = new StringBuilder();
    sb.append("WORKER_DETAILS:").append(npcName)
        .append("|").append(health)
        .append("|").append(energy)
        .append("|").append(workerType);
    for (DwarfSkills.SkillEntry se : skills.getAllSkills()) {
      sb.append("|").append(se.skill.getKey()).append("~").append(se.level);
    }
    sendGui(sb.toString());
  }

  private void sendInfoToManager() {
    try {
      hasHouse = hasHouse || Main.colonyMap.hasHome(npcName);
      BuildingType primaryWorkshop = workshopTypeForTask(primarySkill.getKey());
      if (primaryWorkshop != null) {
        hasWorkshop = hasCompletedBuilding(primaryWorkshop);
      }
      String info = "WORKER_INFO:" + npcName + "|" + primarySkill.getKey()
          + "|" + skills.getLevel(primarySkill) + "|" + npcX + "|" + npcY + "|" + energy
          + "|" + (hasHouse ? "1" : "0") + "|" + (hasWorkshop ? "1" : "0");
      ACLMessage toManager = new ACLMessage(ACLMessage.INFORM);
      toManager.addReceiver(new AID("manager", AID.ISLOCALNAME));
      toManager.setContent(info);
      send(toManager);
      ACLMessage toAnalyst = new ACLMessage(ACLMessage.INFORM);
      toAnalyst.addReceiver(new AID("analyst", AID.ISLOCALNAME));
      toAnalyst.setContent(info);
      send(toAnalyst);
      // Atualiza energia na GUI
      String status = (currentTaskId != null) ? "ocupado" : "ocioso";
      sendGui("WORKER_STATUS:" + npcName + ":" + primarySkill.getKey()
          + ":" + skills.getLevel(primarySkill) + ":" + skills.getRank(primarySkill)
          + ":" + status + ":" + energy + ":" + fome + ":" + sede);
    } catch (Exception ignored) {
    }
  }

  private void sendGui(String content) {
    try {
      ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
      msg.addReceiver(new AID("gui", AID.ISLOCALNAME));
      msg.setContent(content);
      send(msg);
    } catch (Exception ignored) {
    }
  }

  private String traduzirTipo(String type) {
    return switch (type.toLowerCase()) {
      case "builder" -> "Construtor";
      case "miner" -> "Mineiro";
      case "woodcutter" -> "Lenhador";
      case "carpenter" -> "Carpinteiro";
      case "smith" -> "Ferreiro";
      case "farmer" -> "Fazendeiro";
      case "doctor" -> "Médico";
      case "fighter" -> "Guerreiro";
      case "craftsman" -> "Artesão";
      case "engineer" -> "Engenheiro";
      case "mason", "stonemason" -> "Pedreiro";
      case "marbleworker", "marmorist" -> "Marmorista";
      case "fisher" -> "Pescador";
      default -> "Geral";
    };
  }
}
