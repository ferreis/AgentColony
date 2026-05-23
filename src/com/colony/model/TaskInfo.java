package com.colony.model;

public class TaskInfo {
  private String taskId;
  private String taskType;
  private String status;
  private String requiredSkill;
  private String assignedWorker;
  private int requiredWorkers;
  private int urgencyLevel;
  private long deadline;

  public TaskInfo() {
  }

  public TaskInfo(String taskId, String taskType) {
    this.taskId = taskId;
    this.taskType = taskType;
    this.status = "pending";
    this.requiredWorkers = 1;
    this.urgencyLevel = 1;
  }

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getTaskType() {
    return taskType;
  }

  public void setTaskType(String taskType) {
    this.taskType = taskType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRequiredSkill() {
    return requiredSkill;
  }

  public void setRequiredSkill(String requiredSkill) {
    this.requiredSkill = requiredSkill;
  }

  public String getAssignedWorker() {
    return assignedWorker;
  }

  public void setAssignedWorker(String assignedWorker) {
    this.assignedWorker = assignedWorker;
  }

  public int getRequiredWorkers() {
    return requiredWorkers;
  }

  public void setRequiredWorkers(int requiredWorkers) {
    this.requiredWorkers = requiredWorkers;
  }

  public int getUrgencyLevel() {
    return urgencyLevel;
  }

  public void setUrgencyLevel(int urgencyLevel) {
    this.urgencyLevel = urgencyLevel;
  }

  public long getDeadline() {
    return deadline;
  }

  public void setDeadline(long deadline) {
    this.deadline = deadline;
  }

  public static String inferSkillFromType(String taskType) {
    switch (taskType.toLowerCase()) {
      case "build":
      case "construct":
      case "carpentry":
      case "masonry":
        return "construction";
      case "mine":
      case "mining":
        return "mining";
      case "woodcut":
      case "woodcutting":
        return "woodcutting";
      case "smith":
      case "smithing":
      case "forge":
        return "smithing";
      case "farm":
      case "farming":
        return "farming";
      default:
        return "general";
    }
  }
}