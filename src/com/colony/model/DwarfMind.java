package com.colony.model;

import java.util.List;

public class DwarfMind {

  public DwarfAction decide(DwarfState state) {
    if (state == null || state.getStats() == null) {
      return idle("No state available");
    }

    int health = state.getStats().health;
    int energy = state.getStats().energy;
    String mood = state.getStats().mood;
    List<String> needs = state.getUrgentNeeds();
    List<String> priorities = state.getFortressPriorities();

    // ─── REGRA 1: MORTE IMINENTE ───
    if (health <= 10) {
      if (needs.contains("injury")) {
        return action("treat_injury", null, null, 800, 0.95,
            "Sa\u00fade cr\u00edtica (HP=" + health + "). Tratamento emergencial obrigat\u00f3rio.");
      }
      return action("rest", null, null, 600, 0.9,
          "HP cr\u00edtico (" + health + "). Descanso for\u00e7ado para evitar colapso.");
    }

    // ─── REGRA 2: TAREFA DO GERENTE (OBRIGATÓRIA) ───
    if (state.getManagerTask() != null && health > 10) {
      String taskName = state.getManagerTask().name;
      String reqSkill = state.getManagerTask().required_skill;
      int priority = state.getManagerTask().priority;
      int skillLevel = state.getSkillLevel(reqSkill);
      String zone = inferZone(taskName, reqSkill, state);

      if (needs.contains("fatigue") && energy <= 20) {
        return action("rest", reqSkill, zone, 400, 0.85,
            "Gerente ordenou \"" + taskName + "\" mas energia cr\u00edtica (" + energy
                + "). Descanso curto antes de cumprir a ordem.");
      }
      if (needs.contains("hunger") && energy <= 25) {
        return action("eat", reqSkill, zone, 200, 0.8,
            "Gerente ordenou \"" + taskName + "\" mas fome extrema. Pausa r\u00e1pida para comer, depois retoma.");
      }

      int duration = calcWorkDuration(skillLevel, priority);
      double conf = calcConfidence(skillLevel, health, energy, priority);
      String reason = "Ordem do gerente: \"" + taskName + "\" (prioridade " + priority
          + ", skill " + reqSkill + " nível " + skillLevel + "). "
          + "Trabalho obrigatório para progressão da fortaleza.";

      return action("work", reqSkill, zone, duration, conf, reason);
    }

    // ─── REGRA 3: SOBREVIVÊNCIA ───
    if (needs.contains("threat")) {
      int milLevel = state.getSkillLevel("Fighter") + state.getSkillLevel("Axedwarf")
          + state.getSkillLevel("Swordsdwarf") + state.getSkillLevel("Marksdwarf");
      if (milLevel >= 3) {
        return action("defend", null, null, 1200, 0.85,
            "Ameaça detectada. Skill militar " + milLevel + " ≥ 3. Defendendo a fortaleza.");
      }
    }

    if (energy <= 20) {
      return action("rest", null, null, 500, 0.75,
          "Energia esgotada (" + energy + "). Descanso necessário para evitar colapso.");
    }

    if (needs.contains("hunger") && energy <= 30) {
      return action("eat", null, null, 200, 0.7,
          "Fome detectada. Pausa para se alimentar.");
    }

    // ─── REGRA 4: CONTINUAR TAREFA ATUAL ───
    if (state.getCurrentTask() != null && energy > 20) {
      String currentTask = state.getCurrentTask();
      String skill = inferSkillFromTask(currentTask);
      int skillLevel = state.getSkillLevel(skill);
      String zone = state.getLocation() != null ? state.getLocation().zone : null;
      int duration = calcWorkDuration(skillLevel, 3);
      double conf = calcConfidence(skillLevel, health, energy, 3);

      return action("work", skill, zone, duration, conf,
          "Continuando tarefa: \"" + currentTask + "\". Consistência maximiza ganho de XP.");
    }

    // ─── REGRA 5: FORTRESS PRIORITIES ───
    if (priorities != null && !priorities.isEmpty() && energy > 30) {
      String topPrio = priorities.get(0);
      String skill = inferSkillFromTask(topPrio);
      int skillLevel = state.getSkillLevel(skill);
      String zone = inferZone(topPrio, skill, state);
      int duration = calcWorkDuration(skillLevel, 2);
      double conf = calcConfidence(skillLevel, health, energy, 2);

      return action("work", skill, zone, duration, conf,
          "Prioridade da fortaleza: \"" + topPrio + "\". Contribuindo para o objetivo coletivo.");
    }

    // ─── REGRA 6: TREINO / LAZER ───
    if (energy > 70) {
      String bestSkill = findBestSkill(state);
      if (bestSkill != null) {
        return action("learn", bestSkill, null, 1200, 0.5,
            "Sem ordens ativas. Treinando " + bestSkill + " para melhorar.");
      }
    }

    // ─── IDLE ───
    return idle("Aguardando ordens do gerente.");
  }

  // ─── Helpers ───

  private DwarfAction action(String action, String skill, String zone, int ticks, double conf, String reason) {
    return new DwarfAction(action, skill, zone, ticks, conf, reason);
  }

  private DwarfAction idle(String reason) {
    return new DwarfAction("idle", null, null, 100, 0.3, reason);
  }

  private int calcWorkDuration(int skillLevel, int priority) {
    int base = 1200;
    int skillBonus = skillLevel * 60;
    int prioBonus = priority * 120;
    return Math.min(base + skillBonus + prioBonus, 3600);
  }

  private double calcConfidence(int skillLevel, int health, int energy, int priority) {
    double conf = 0.5;
    conf += Math.min(skillLevel * 0.03, 0.3);
    conf += (health / 100.0) * 0.15;
    conf += (energy / 100.0) * 0.10;
    conf += priority * 0.03;
    return Math.min(Math.max(conf, 0.1), 1.0);
  }

  private String inferSkillFromTask(String task) {
    if (task == null)
      return "general";
    String t = task.toLowerCase();
    if (t.contains("mine") || t.contains("dig"))
      return "Miner";
    if (t.contains("build") || t.contains("construct") || t.contains("mason") || t.contains("carpenter"))
      return "Builder";
    if (t.contains("wood") || t.contains("cut"))
      return "Woodcutter";
    if (t.contains("smith") || t.contains("forge") || t.contains("metal"))
      return "Smith";
    if (t.contains("farm") || t.contains("plant") || t.contains("cook"))
      return "Farmer";
    if (t.contains("hospital") || t.contains("treat") || t.contains("diagnos") || t.contains("surgeon"))
      return "Diagnostician";
    if (t.contains("fight") || t.contains("defend") || t.contains("guard") || t.contains("military"))
      return "Fighter";
    if (t.contains("craft") || t.contains("engrave") || t.contains("jewel"))
      return "Craftsdwarf";
    return "general";
  }

  private String findBestSkill(DwarfState state) {
    List<DwarfState.DwarfSkill> skills = state.getSkills();
    if (skills == null || skills.isEmpty())
      return "general";
    DwarfState.DwarfSkill best = null;
    for (DwarfState.DwarfSkill s : skills) {
      if (best == null || s.level < best.level)
        best = s;
    }
    return best != null ? best.name : "general";
  }

  private String inferZone(String task, String skill, DwarfState state) {
    if (task == null && skill == null)
      return null;
    if (state.getLocation() != null && state.getLocation().zone != null)
      return state.getLocation().zone;

    if (skill != null) {
      switch (skill.toLowerCase()) {
        case "miner":
          return "mining_tunnels";
        case "builder":
        case "carpenter":
        case "mason":
          return "construction_site";
        case "woodcutter":
          return "forest";
        case "smith":
          return "forge";
        case "farmer":
          return "farm_plots";
        case "diagnostician":
          return "hospital";
        case "fighter":
          return "barracks";
        case "craftsdwarf":
          return "workshop";
        default:
          return "meeting_hall";
      }
    }
    return "meeting_hall";
  }
}
