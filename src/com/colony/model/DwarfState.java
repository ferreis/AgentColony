package com.colony.model;

import java.util.*;

public class DwarfState {
  private String npc_id;
  private DwarfStats stats;
  private List<DwarfSkill> skills;
  private String current_task;
  private DwarfManagerTask manager_task;
  private List<DwarfItem> inventory;
  private DwarfLocation location;
  private List<String> urgent_needs;
  private List<String> fortress_priorities;

  public static class DwarfStats {
    public int health = 100;
    public int energy = 100;
    public String mood = "neutral";
  }

  public static class DwarfSkill {
    public String name;
    public int level;
    public String rank;
  }

  public static class DwarfManagerTask {
    public String name;
    public int priority;
    public String required_skill;
  }

  public static class DwarfItem {
    public String item;
    public int quantity;
  }

  public static class DwarfLocation {
    public int x, y, z;
    public String zone;
  }

  public String getNpcId() {
    return npc_id;
  }

  public void setNpcId(String id) {
    this.npc_id = id;
  }

  public DwarfStats getStats() {
    return stats;
  }

  public void setStats(DwarfStats s) {
    this.stats = s;
  }

  public List<DwarfSkill> getSkills() {
    return skills != null ? skills : Collections.emptyList();
  }

  public void setSkills(List<DwarfSkill> s) {
    this.skills = s;
  }

  public String getCurrentTask() {
    return current_task;
  }

  public void setCurrentTask(String t) {
    this.current_task = t;
  }

  public DwarfManagerTask getManagerTask() {
    return manager_task;
  }

  public void setManagerTask(DwarfManagerTask t) {
    this.manager_task = t;
  }

  public List<DwarfItem> getInventory() {
    return inventory != null ? inventory : Collections.emptyList();
  }

  public void setInventory(List<DwarfItem> i) {
    this.inventory = i;
  }

  public DwarfLocation getLocation() {
    return location;
  }

  public void setLocation(DwarfLocation l) {
    this.location = l;
  }

  public List<String> getUrgentNeeds() {
    return urgent_needs != null ? urgent_needs : Collections.emptyList();
  }

  public void setUrgentNeeds(List<String> n) {
    this.urgent_needs = n;
  }

  public List<String> getFortressPriorities() {
    return fortress_priorities != null ? fortress_priorities : Collections.emptyList();
  }

  public void setFortressPriorities(List<String> p) {
    this.fortress_priorities = p;
  }

  public int getSkillLevel(String skillName) {
    if (skills == null)
      return 0;
    for (DwarfSkill s : skills) {
      if (s.name != null && s.name.equalsIgnoreCase(skillName))
        return s.level;
    }
    return 0;
  }

  public boolean hasItem(String itemName) {
    if (inventory == null)
      return false;
    for (DwarfItem i : inventory) {
      if (i.item != null && i.item.equalsIgnoreCase(itemName) && i.quantity > 0)
        return true;
    }
    return false;
  }

  // ─── JSON PARSER ───

  public static DwarfState fromJson(String json) {
    DwarfState state = new DwarfState();
    if (json == null || json.trim().isEmpty())
      return state;
    json = json.trim();

    state.setNpcId(extractString(json, "npc_id"));

    String statsB = extractObject(json, "stats");
    if (statsB != null) {
      DwarfStats s = new DwarfStats();
      s.health = extractInt(statsB, "health", 100);
      s.energy = extractInt(statsB, "energy", 100);
      s.mood = extractString(statsB, "mood");
      state.setStats(s);
    }

    String skillsB = extractArray(json, "skills");
    if (skillsB != null) {
      List<DwarfSkill> list = new ArrayList<>();
      for (String item : splitArray(skillsB)) {
        DwarfSkill sk = new DwarfSkill();
        sk.name = extractString(item, "name");
        sk.level = extractInt(item, "level", 0);
        sk.rank = extractString(item, "rank");
        if (sk.name != null)
          list.add(sk);
      }
      state.setSkills(list);
    }

    state.setCurrentTask(extractString(json, "current_task"));

    String mgrB = extractObject(json, "manager_task");
    if (mgrB != null) {
      DwarfManagerTask mt = new DwarfManagerTask();
      mt.name = extractString(mgrB, "name");
      mt.priority = extractInt(mgrB, "priority", 1);
      mt.required_skill = extractString(mgrB, "required_skill");
      if (mt.name != null)
        state.setManagerTask(mt);
    }

    String invB = extractArray(json, "inventory");
    if (invB != null) {
      List<DwarfItem> list = new ArrayList<>();
      for (String item : splitArray(invB)) {
        DwarfItem di = new DwarfItem();
        di.item = extractString(item, "item");
        di.quantity = extractInt(item, "quantity", 0);
        if (di.item != null)
          list.add(di);
      }
      state.setInventory(list);
    }

    String locB = extractObject(json, "location");
    if (locB != null) {
      DwarfLocation loc = new DwarfLocation();
      loc.x = extractInt(locB, "x", 0);
      loc.y = extractInt(locB, "y", 0);
      loc.z = extractInt(locB, "z", 0);
      loc.zone = extractString(locB, "zone");
      state.setLocation(loc);
    }

    String needsB = extractArray(json, "urgent_needs");
    if (needsB != null) {
      List<String> list = new ArrayList<>();
      for (String s : splitArray(needsB)) {
        list.add(stripQuotes(s));
      }
      state.setUrgentNeeds(list);
    }

    String prioB = extractArray(json, "fortress_priorities");
    if (prioB != null) {
      List<String> list = new ArrayList<>();
      for (String s : splitArray(prioB)) {
        list.add(stripQuotes(s));
      }
      state.setFortressPriorities(list);
    }

    return state;
  }

  // ─── Raw JSON extraction helpers ───

  private static String extractRawValue(String json, String key) {
    String search = "\"" + key + "\":";
    int idx = json.indexOf(search);
    if (idx < 0)
      return null;
    int pos = idx + search.length();
    while (pos < json.length() && json.charAt(pos) == ' ')
      pos++;
    if (pos >= json.length())
      return null;

    char first = json.charAt(pos);
    if (first == '"') {
      int end = pos + 1;
      while (end < json.length()) {
        if (json.charAt(end) == '\\') {
          end += 2;
          continue;
        }
        if (json.charAt(end) == '"')
          break;
        end++;
      }
      return json.substring(pos, end + 1);
    }
    if (first == '{' || first == '[') {
      int depth = 0;
      int end = pos;
      for (; end < json.length(); end++) {
        if (json.charAt(end) == '{' || json.charAt(end) == '[')
          depth++;
        if (json.charAt(end) == '}' || json.charAt(end) == ']') {
          depth--;
          if (depth == 0)
            break;
        }
      }
      return json.substring(pos, end + 1);
    }
    // number, null, true, false
    int end = pos;
    while (end < json.length()) {
      char c = json.charAt(end);
      if (c == ',' || c == '}' || c == ']' || Character.isWhitespace(c))
        break;
      end++;
    }
    return json.substring(pos, end);
  }

  private static String extractString(String json, String key) {
    String raw = extractRawValue(json, key);
    if (raw == null)
      return null;
    raw = raw.trim();
    if (raw.equals("null"))
      return null;
    if (raw.startsWith("\"") && raw.endsWith("\"")) {
      return raw.substring(1, raw.length() - 1)
          .replace("\\\"", "\"").replace("\\\\", "\\");
    }
    return raw;
  }

  private static int extractInt(String json, String key, int defaultVal) {
    String raw = extractRawValue(json, key);
    if (raw == null)
      return defaultVal;
    try {
      return Integer.parseInt(raw.trim());
    } catch (NumberFormatException e) {
      return defaultVal;
    }
  }

  private static String extractObject(String json, String key) {
    String raw = extractRawValue(json, key);
    if (raw != null && raw.startsWith("{") && raw.endsWith("}"))
      return raw;
    return null;
  }

  private static String extractArray(String json, String key) {
    String raw = extractRawValue(json, key);
    if (raw != null && raw.startsWith("[") && raw.endsWith("]"))
      return raw;
    return null;
  }

  private static String[] splitArray(String array) {
    List<String> items = new ArrayList<>();
    String inner = array.trim();
    if (inner.startsWith("["))
      inner = inner.substring(1);
    if (inner.endsWith("]"))
      inner = inner.substring(0, inner.length() - 1);
    inner = inner.trim();
    if (inner.isEmpty())
      return new String[0];

    int depth = 0;
    int start = 0;
    boolean inStr = false;
    for (int i = 0; i < inner.length(); i++) {
      char c = inner.charAt(i);
      if (c == '"' && (i == 0 || inner.charAt(i - 1) != '\\'))
        inStr = !inStr;
      if (!inStr) {
        if (c == '{' || c == '[')
          depth++;
        if (c == '}' || c == ']')
          depth--;
        if (c == ',' && depth == 0) {
          items.add(inner.substring(start, i).trim());
          start = i + 1;
        }
      }
    }
    String last = inner.substring(start).trim();
    if (!last.isEmpty())
      items.add(last);
    return items.toArray(new String[0]);
  }

  private static String stripQuotes(String s) {
    if (s == null)
      return "";
    s = s.trim();
    if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2)
      return s.substring(1, s.length() - 1);
    return s;
  }

  // ─── JSON OUTPUT ───

  public String toJson() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"npc_id\": \"").append(escape(npc_id)).append("\",\n");
    if (stats != null) {
      sb.append("  \"stats\": {\"health\":").append(stats.health)
          .append(",\"energy\":").append(stats.energy)
          .append(",\"mood\":\"").append(escape(stats.mood)).append("\"},\n");
    }
    if (skills != null && !skills.isEmpty()) {
      sb.append("  \"skills\": [");
      for (int i = 0; i < skills.size(); i++) {
        DwarfSkill sk = skills.get(i);
        if (i > 0)
          sb.append(",");
        sb.append("{\"name\":\"").append(escape(sk.name))
            .append("\",\"level\":").append(sk.level)
            .append(",\"rank\":\"").append(escape(sk.rank)).append("\"}");
      }
      sb.append("],\n");
    } else {
      sb.append("  \"skills\": [],\n");
    }
    sb.append("  \"current_task\": ");
    sb.append(current_task != null ? "\"" + escape(current_task) + "\"" : "null").append(",\n");
    if (manager_task != null) {
      sb.append("  \"manager_task\": {\"name\":\"").append(escape(manager_task.name))
          .append("\",\"priority\":").append(manager_task.priority)
          .append(",\"required_skill\":\"").append(escape(manager_task.required_skill)).append("\"},\n");
    } else {
      sb.append("  \"manager_task\": null,\n");
    }
    sb.append("  \"inventory\": [],\n");
    if (location != null) {
      sb.append("  \"location\": {\"x\":").append(location.x)
          .append(",\"y\":").append(location.y)
          .append(",\"z\":").append(location.z)
          .append(",\"zone\":\"").append(escape(location.zone)).append("\"},\n");
    }
    sb.append("  \"urgent_needs\": [");
    if (urgent_needs != null) {
      for (int i = 0; i < urgent_needs.size(); i++) {
        if (i > 0)
          sb.append(",");
        sb.append("\"").append(escape(urgent_needs.get(i))).append("\"");
      }
    }
    sb.append("],\n");
    sb.append("  \"fortress_priorities\": [");
    if (fortress_priorities != null) {
      for (int i = 0; i < fortress_priorities.size(); i++) {
        if (i > 0)
          sb.append(",");
        sb.append("\"").append(escape(fortress_priorities.get(i))).append("\"");
      }
    }
    sb.append("]\n}");
    return sb.toString();
  }

  private static String escape(String s) {
    if (s == null)
      return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
