package com.colony.model;

public enum SkillType {
  MINER("Miner", "Miner", Category.MINING),
  MINER_R("MinerR", "Miner (R)", Category.MINING),

  BOWYER("Bowyer", "Bowyer", Category.WOODWORKING),
  CARPENTER("Carpenter", "Carpenter", Category.WOODWORKING),
  WOOD_CUTTER("Wood cutter", "Wood Cutter", Category.WOODWORKING),

  ENGRAVER("Engraver", "Engraver", Category.STONEWORKING),
  STONECUTTER("Stonecutter", "Stonecutter", Category.STONEWORKING),
  STONE_CARVER("Stone carver", "Stone Carver", Category.STONEWORKING),
  MASON("Mason", "Mason", Category.STONEWORKING),

  AMBUSHER("Ambusher", "Ambusher", Category.RANGER),
  ANIMAL_CARETAKER("Animal caretaker", "Animal Caretaker", Category.RANGER),
  ANIMAL_DISSECTOR("Animal dissector", "Animal Dissector", Category.RANGER),
  ANIMAL_TRAINER("Animal trainer", "Animal Trainer", Category.RANGER),
  TRAPPER("Trapper", "Trapper", Category.RANGER),

  BONE_DOCTOR("Bone doctor", "Bone Doctor", Category.DOCTOR),
  CRUTCH_WALKER("Crutch-walker", "Crutch-Walker", Category.DOCTOR),
  DIAGNOSTICIAN("Diagnostician", "Diagnostician", Category.DOCTOR),
  SURGEON("Surgeon", "Surgeon", Category.DOCTOR),
  SUTURER("Suturer", "Suturer", Category.DOCTOR),
  WOUND_DRESSER("Wound dresser", "Wound Dresser", Category.DOCTOR),

  BREWER("Brewer", "Brewer", Category.FARMER),
  BUTCHER("Butcher", "Butcher", Category.FARMER),
  CHEESE_MAKER("Cheese maker", "Cheese Maker", Category.FARMER),
  COOK("Cook", "Cook", Category.FARMER),
  DYER("Dyer", "Dyer", Category.FARMER),
  PLANTER("Planter", "Planter", Category.FARMER),
  HERBALIST("Herbalist", "Herbalist", Category.FARMER),
  MILLER("Miller", "Miller", Category.FARMER),
  TANNER("Tanner", "Tanner", Category.FARMER),

  FISH_CLEANER("Fish cleaner", "Fish Cleaner", Category.FISHERY),
  FISH_DISSECTOR("Fish dissector", "Fish Dissector", Category.FISHERY),
  FISHERDWARF("Fisherdwarf", "Fisherdwarf", Category.FISHERY),

  ARMORSMITH("Armorsmith", "Armorsmith", Category.METALSMITH),
  FURNACE_OPERATOR("Furnace operator", "Furnace Operator", Category.METALSMITH),
  METAL_CRAFTER("Metal crafter", "Metal Crafter", Category.METALSMITH),
  BLACKSMITH("Blacksmith", "Blacksmith", Category.METALSMITH),
  WEAPONSMITH("Weaponsmith", "Weaponsmith", Category.METALSMITH),

  GEM_CUTTER("Gem cutter", "Gem Cutter", Category.JEWELER),
  GEM_SETTER("Gem setter", "Gem Setter", Category.JEWELER),

  BONE_CARVER("Bone carver", "Bone Carver", Category.CRAFTSDWARF),
  CLOTHIER("Clothier", "Clothier", Category.CRAFTSDWARF),
  GLASSMAKER("Glassmaker", "Glassmaker", Category.CRAFTSDWARF),
  LEATHERWORKER("Leatherworker", "Leatherworker", Category.CRAFTSDWARF),
  POTTER("Potter", "Potter", Category.CRAFTSDWARF),
  STONE_CRAFTER("Stone crafter", "Stone Crafter", Category.CRAFTSDWARF),
  WEAVER("Weaver", "Weaver", Category.CRAFTSDWARF),
  WOOD_CRAFTER("Wood crafter", "Wood Crafter", Category.CRAFTSDWARF),

  MECHANIC("Mechanic", "Mechanic", Category.ENGINEER),
  PUMP_OPERATOR("Pump operator", "Pump Operator", Category.ENGINEER),
  SIEGE_ENGINEER("Siege engineer", "Siege Engineer", Category.ENGINEER),
  SIEGE_OPERATOR("Siege operator", "Siege Operator", Category.ENGINEER),

  APPRAISER("Appraiser", "Appraiser", Category.ADMINISTRATOR),
  ORGANIZER("Organizer", "Organizer", Category.ADMINISTRATOR),
  RECORD_KEEPER("Record keeper", "Record Keeper", Category.ADMINISTRATOR),

  FIGHTER("Fighter", "Fighter", Category.MILITARY),
  AXEMAN("Axeman", "Axeman", Category.MILITARY),
  SWORDSMAN("Swordsman", "Swordsman", Category.MILITARY),
  BOWMAN("Bowman", "Bowman", Category.MILITARY),
  CROSSBOWMAN("Crossbowman", "Crossbowman", Category.MILITARY),
  SPEARMAN("Spearman", "Spearman", Category.MILITARY),
  HAMMERMAN("Hammerman", "Hammerman", Category.MILITARY),
  MACEMAN("Maceman", "Maceman", Category.MILITARY),
  PIKE_MAN("Pikeman", "Pikeman", Category.MILITARY),
  SHIELD_USER("Shield user", "Shield User", Category.MILITARY),
  ARMOR_USER("Armor user", "Armor User", Category.MILITARY),
  DODGER("Dodger", "Dodger", Category.MILITARY),
  WRESTLER("Wrestler", "Wrestler", Category.MILITARY),
  THROWER("Thrower", "Thrower", Category.MILITARY),
  KNIFE_USER("Knife user", "Knife User", Category.MILITARY),
  LASHER("Lasher", "Lasher", Category.MILITARY),
  MILITARY_TACTICS("Military tactics", "Military Tactics", Category.MILITARY),
  DISCIPLINE("Discipline", "Discipline", Category.MILITARY),

  NEGOTIATOR("Negotiator", "Negotiator", Category.BROKER),
  JUDGE_OF_INTENT("Judge of intent", "Judge of Intent", Category.BROKER),
  PERSUADER("Persuader", "Persuader", Category.BROKER),
  LIAR("Liar", "Liar", Category.BROKER),
  INTIMIDATOR("Intimidator", "Intimidator", Category.BROKER),
  CONVERSATIONALIST("Conversationalist", "Conversationalist", Category.BROKER),
  FLATTERER("Flatterer", "Flatterer", Category.BROKER),
  COMEDIAN("Comedian", "Comedian", Category.BROKER),

  LEADER("Leader", "Leader", Category.MISCELLANEOUS),
  OBSERVER("Observer", "Observer", Category.MISCELLANEOUS),
  STUDENT("Student", "Student", Category.MISCELLANEOUS),
  TEACHER("Teacher", "Teacher", Category.MISCELLANEOUS),
  CONCENTRATION("Concentration", "Concentration", Category.MISCELLANEOUS),
  CONSOLER("Consoler", "Consoler", Category.MISCELLANEOUS),
  PACIFIER("Pacifier", "Pacifier", Category.MISCELLANEOUS),
  TRACKER("Tracker", "Tracker", Category.MISCELLANEOUS),
  SWIMMER("Swimmer", "Swimmer", Category.MISCELLANEOUS),
  CLIMBER("Climber", "Climber", Category.MISCELLANEOUS),
  READER("Reader", "Reader", Category.MISCELLANEOUS),
  SCHEMER("Schemer", "Schemer", Category.MISCELLANEOUS),
  RIDER("Rider", "Rider", Category.MISCELLANEOUS),

  DANCER("Dancer", "Dancer", Category.PERFORMANCE),
  SINGER("Singer", "Singer", Category.PERFORMANCE),
  MUSICIAN("Musician", "Musician", Category.PERFORMANCE),
  POET("Poet", "Poet", Category.PERFORMANCE),
  SPEAKER("Speaker", "Speaker", Category.PERFORMANCE),
  KEYBOARDIST("Keyboardist", "Keyboardist", Category.PERFORMANCE),
  STRINGED_INSTRUMENTALIST("Stringed instrumentalist", "Stringed Instrumentalist", Category.PERFORMANCE),
  WIND_INSTRUMENTALIST("Wind instrumentalist", "Wind Instrumentalist", Category.PERFORMANCE),
  PERCUSSIONIST("Percussionist", "Percussionist", Category.PERFORMANCE),

  CRITICAL_THINKER("Critical thinker", "Critical Thinker", Category.SCHOLAR),
  LOGICIAN("Logician", "Logician", Category.SCHOLAR),
  MATHEMATICIAN("Mathematician", "Mathematician", Category.SCHOLAR),
  ASTRONOMER("Astronomer", "Astronomer", Category.SCHOLAR),
  CHEMIST("Chemist", "Chemist", Category.SCHOLAR),
  GEOGRAPHER("Geographer", "Geographer", Category.SCHOLAR),
  OPTICS_ENGINEER("Optics engineer", "Optics Engineer", Category.SCHOLAR),
  FLUID_ENGINEER("Fluid engineer", "Fluid Engineer", Category.SCHOLAR),
  WORDSMITH("Wordsmith", "Wordsmith", Category.SCHOLAR),
  WRITER("Writer", "Writer", Category.SCHOLAR);

  private final String key;
  private final String displayName;
  private final Category category;

  SkillType(String key, String displayName, Category category) {
    this.key = key;
    this.displayName = displayName;
    this.category = category;
  }

  public String getKey() {
    return key;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Category getCategory() {
    return category;
  }

  public enum Category {
    MINING("Mineração"),
    WOODWORKING("Marcenaria"),
    STONEWORKING("Alvenaria"),
    RANGER("Patrulheiro"),
    DOCTOR("Médico"),
    FARMER("Agricultura"),
    FISHERY("Pesca"),
    METALSMITH("Ferraria"),
    JEWELER("Joalheria"),
    CRAFTSDWARF("Artesanato"),
    ENGINEER("Engenheiro"),
    ADMINISTRATOR("Administrador"),
    MILITARY("Militar"),
    BROKER("Negociador"),
    MISCELLANEOUS("Diversos"),
    PERFORMANCE("Performance"),
    SCHOLAR("Estudioso");

    private final String displayName;

    Category(String displayName) {
      this.displayName = displayName;
    }

    public String getDisplayName() {
      return displayName;
    }
  }

  public static SkillType fromKey(String key) {
    for (SkillType s : values()) {
      if (s.key.equalsIgnoreCase(key))
        return s;
    }
    return null;
  }

  public static String[] getRankNames() {
    return new String[] {
        "Sem habilidade", // 0
        "Iniciante", // 1 (Dabbling)
        "Novato", // 2 (Novice)
        "Adequado", // 3 (Adequate)
        "Competente", // 4 (Competent)
        "Hábil", // 5 (Skilled/Proficient)
        "Talentoso", // 6 (Talented)
        "Capaz", // 7 (Adept)
        "Especialista", // 8 (Expert)
        "Profissional", // 9 (Professional)
        "Consumado", // 10 (Accomplished)
        "Grande", // 11 (Great)
        "Mestre", // 12 (Master)
        "Alto Mestre", // 13 (High Master)
        "Grão Mestre", // 14 (Grand Master)
        "Lendário" // 15+ (Legendary)
    };
  }

  public static int xpForLevel(int level) {
    if (level <= 0)
      return 0;
    return level * level * 100;
  }

  public static int levelForXp(int xp) {
    int lvl = 0;
    while (xpForLevel(lvl + 1) <= xp && lvl < 20)
      lvl++;
    return lvl;
  }

  public static String rankForLevel(int level) {
    String[] ranks = getRankNames();
    if (level < 0)
      return ranks[0];
    if (level >= ranks.length)
      return ranks[ranks.length - 1] + " " + (level - ranks.length + 2);
    return ranks[level];
  }
}
