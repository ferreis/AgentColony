package com.colony.model;

public class WorkerInfo {
  private String name;
  private String type;
  private String skill;
  private String status;
  private String currentTaskId;

  public WorkerInfo() {
  }

  public WorkerInfo(String name, String type, String skill) {
    this.name = name;
    this.type = type;
    this.skill = skill;
    this.status = "idle";
    this.currentTaskId = null;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getSkill() {
    return skill;
  }

  public void setSkill(String skill) {
    this.skill = skill;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCurrentTaskId() {
    return currentTaskId;
  }

  public void setCurrentTaskId(String currentTaskId) {
    this.currentTaskId = currentTaskId;
  }

  public boolean isBusy() {
    return "busy".equals(status);
  }
}