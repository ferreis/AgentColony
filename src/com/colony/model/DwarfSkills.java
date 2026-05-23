package com.colony.model;

import java.util.*;

public class DwarfSkills {
  private final Map<String, Integer> xpMap;

  public DwarfSkills() {
    this.xpMap = new HashMap<>();
  }

  public void setLevel(SkillType skill, int level) {
    xpMap.put(skill.getKey(), SkillType.xpForLevel(level));
  }

  public int getLevel(SkillType skill) {
    return SkillType.levelForXp(getXp(skill));
  }

  public int getLevel(String skillKey) {
    SkillType st = SkillType.fromKey(skillKey);
    return st != null ? getLevel(st) : 0;
  }

  public int getXp(SkillType skill) {
    return xpMap.getOrDefault(skill.getKey(), 0);
  }

  public String getRank(SkillType skill) {
    return SkillType.rankForLevel(getLevel(skill));
  }

  public String getRank(String skillKey) {
    int lvl = getLevel(skillKey);
    return SkillType.rankForLevel(lvl);
  }

  public int addXp(SkillType skill, int amount) {
    int before = getLevel(skill);
    xpMap.merge(skill.getKey(), amount, Integer::sum);
    int after = getLevel(skill);
    return after - before;
  }

  public int addXp(String skillKey, int amount) {
    SkillType st = SkillType.fromKey(skillKey);
    if (st == null) {
      for (SkillType s : SkillType.values()) {
        if (s.getKey().toLowerCase().contains(skillKey.toLowerCase())) {
          st = s;
          break;
        }
      }
    }
    if (st == null)
      return 0;
    int before = getLevel(st);
    addXp(st, amount);
    return getLevel(st) - before;
  }

  public boolean hasSkill(SkillType skill) {
    return getLevel(skill) > 0;
  }

  public SkillType getHighestSkill() {
    SkillType best = null;
    int bestLvl = -1;
    for (SkillType s : SkillType.values()) {
      int lvl = getLevel(s);
      if (lvl > bestLvl) {
        bestLvl = lvl;
        best = s;
      }
    }
    return best;
  }

  public SkillType getLowestSkill() {
    SkillType worst = null;
    int worstLvl = Integer.MAX_VALUE;
    for (SkillType s : SkillType.values()) {
      int lvl = getLevel(s);
      if (lvl < worstLvl) {
        worstLvl = lvl;
        worst = s;
      }
    }
    return worst;
  }

  public List<SkillType> getSkillsByCategory(SkillType.Category category) {
    List<SkillType> result = new ArrayList<>();
    for (SkillType s : SkillType.values()) {
      if (s.getCategory() == category && getLevel(s) > 0) {
        result.add(s);
      }
    }
    return result;
  }

  public List<SkillEntry> getAllSkills() {
    List<SkillEntry> list = new ArrayList<>();
    for (SkillType s : SkillType.values()) {
      int xp = getXp(s);
      if (xp > 0) {
        list.add(new SkillEntry(s, xp));
      }
    }
    list.sort((a, b) -> b.xp - a.xp);
    return list;
  }

  public int getTotalSkillsCount() {
    int count = 0;
    for (SkillType s : SkillType.values()) {
      if (getXp(s) > 0)
        count++;
    }
    return count;
  }

  public static SkillType inferFromTaskType(String taskType) {
    if (taskType == null)
      return null;
    String t = taskType.toLowerCase();
    if (t.contains("mine") || t.contains("dig") || t.contains("miner"))
      return SkillType.MINER;
    if (t.contains("wood") && (t.contains("cut") || t.contains("cutter")))
      return SkillType.WOOD_CUTTER;
    if (t.contains("wood") && (t.contains("craft") || t.contains("crafter")))
      return SkillType.WOOD_CRAFTER;
    if (t.contains("carpenter") || t.contains("carpentry"))
      return SkillType.CARPENTER;
    if (t.contains("bowyer"))
      return SkillType.BOWYER;
    if (t.contains("mason") || t.contains("masonry"))
      return SkillType.MASON;
    if (t.contains("stone") && (t.contains("carver") || t.contains("carve")))
      return SkillType.STONE_CARVER;
    if (t.contains("stone") && (t.contains("cutter") || t.contains("cut")))
      return SkillType.STONECUTTER;
    if (t.contains("engraver") || t.contains("engrave"))
      return SkillType.ENGRAVER;
    if (t.contains("smith") || t.contains("smithing") || t.contains("forge")) {
      if (t.contains("armor"))
        return SkillType.ARMORSMITH;
      if (t.contains("weapon"))
        return SkillType.WEAPONSMITH;
      if (t.contains("black"))
        return SkillType.BLACKSMITH;
      return SkillType.BLACKSMITH;
    }
    if (t.contains("cook") || t.contains("kitchen"))
      return SkillType.COOK;
    if (t.contains("brew") || t.contains("beer"))
      return SkillType.BREWER;
    if (t.contains("butcher"))
      return SkillType.BUTCHER;
    if (t.contains("farm") || t.contains("plant"))
      return SkillType.PLANTER;
    if (t.contains("hospital") || t.contains("treat") || t.contains("diagnos"))
      return SkillType.DIAGNOSTICIAN;
    if (t.contains("surgeon") || t.contains("surgery"))
      return SkillType.SURGEON;
    if (t.contains("fighter") || t.contains("fight") || t.contains("guard"))
      return SkillType.FIGHTER;
    if (t.contains("axe") || t.contains("axeman"))
      return SkillType.AXEMAN;
    if (t.contains("sword") || t.contains("swordsman"))
      return SkillType.SWORDSMAN;
    if (t.contains("bow") || t.contains("archer") || t.contains("marksdwarf"))
      return SkillType.BOWMAN;
    if (t.contains("crossbow"))
      return SkillType.CROSSBOWMAN;
    if (t.contains("spear") || t.contains("pike"))
      return SkillType.SPEARMAN;
    if (t.contains("hammer") || t.contains("mace"))
      return SkillType.HAMMERMAN;
    if (t.contains("shield"))
      return SkillType.SHIELD_USER;
    if (t.contains("armor"))
      return SkillType.ARMOR_USER;
    if (t.contains("dodge"))
      return SkillType.DODGER;
    if (t.contains("wrestle"))
      return SkillType.WRESTLER;
    if (t.contains("mechanic") || t.contains("pump"))
      return SkillType.MECHANIC;
    if (t.contains("siege"))
      return SkillType.SIEGE_ENGINEER;
    if (t.contains("glass"))
      return SkillType.GLASSMAKER;
    if (t.contains("leather"))
      return SkillType.LEATHERWORKER;
    if (t.contains("potter") || t.contains("pottery"))
      return SkillType.POTTER;
    if (t.contains("weaver") || t.contains("cloth"))
      return SkillType.WEAVER;
    if (t.contains("jewel") || t.contains("gem") || t.contains("cutter"))
      return SkillType.GEM_CUTTER;
    if (t.contains("setter"))
      return SkillType.GEM_SETTER;
    if (t.contains("bone"))
      return SkillType.BONE_CARVER;
    if (t.contains("fish") || t.contains("fisher"))
      return SkillType.FISHERDWARF;
    if (t.contains("trap"))
      return SkillType.TRAPPER;
    if (t.contains("animal"))
      return SkillType.ANIMAL_CARETAKER;
    if (t.contains("train"))
      return SkillType.ANIMAL_TRAINER;
    if (t.contains("tanner") || t.contains("tanning"))
      return SkillType.TANNER;
    if (t.contains("furnace") || t.contains("smelt"))
      return SkillType.FURNACE_OPERATOR;
    return null;
  }

  public static class SkillEntry {
    public final SkillType skill;
    public final int xp;
    public final int level;
    public final String rank;

    public SkillEntry(SkillType skill, int xp) {
      this.skill = skill;
      this.xp = xp;
      this.level = SkillType.levelForXp(xp);
      this.rank = SkillType.rankForLevel(this.level);
    }
  }
}
