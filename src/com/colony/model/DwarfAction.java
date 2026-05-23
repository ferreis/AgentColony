package com.colony.model;

public class DwarfAction {
  private String action;
  private String target_skill;
  private String target_zone;
  private int duration_ticks;
  private double confidence;
  private String reason;

  public DwarfAction() {
  }

  public DwarfAction(String action, String targetSkill, String targetZone,
      int durationTicks, double confidence, String reason) {
    this.action = action;
    this.target_skill = targetSkill;
    this.target_zone = targetZone;
    this.duration_ticks = durationTicks;
    this.confidence = confidence;
    this.reason = reason;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String a) {
    this.action = a;
  }

  public String getTargetSkill() {
    return target_skill;
  }

  public void setTargetSkill(String s) {
    this.target_skill = s;
  }

  public String getTargetZone() {
    return target_zone;
  }

  public void setTargetZone(String z) {
    this.target_zone = z;
  }

  public int getDurationTicks() {
    return duration_ticks;
  }

  public void setDurationTicks(int d) {
    this.duration_ticks = d;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setConfidence(double c) {
    this.confidence = c;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String r) {
    this.reason = r;
  }

  public String toJson() {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"action\": \"").append(escape(action)).append("\",\n");
    sb.append("  \"target_skill\": ").append(target_skill != null ? "\"" + escape(target_skill) + "\"" : "null")
        .append(",\n");
    sb.append("  \"target_zone\": ").append(target_zone != null ? "\"" + escape(target_zone) + "\"" : "null")
        .append(",\n");
    sb.append("  \"duration_ticks\": ").append(duration_ticks).append(",\n");
    sb.append("  \"confidence\": ").append(String.format("%.1f", confidence)).append(",\n");
    sb.append("  \"reason\": \"").append(escape(reason)).append("\"\n");
    sb.append("}");
    return sb.toString();
  }

  private static String escape(String s) {
    if (s == null)
      return "";
    return s.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
