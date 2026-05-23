package com.colony.model;

import java.util.*;

public class ColonyData {
  private final ColonyResources resources;
  private final ColonyMap map;
  private final Map<String, WorkerInfo> workers;
  private final Map<String, TaskInfo> tasks;
  private final List<String> eventLog;

  public ColonyData() {
    this.resources = new ColonyResources();
    this.map = new ColonyMap();
    this.workers = new LinkedHashMap<>();
    this.tasks = new LinkedHashMap<>();
    this.eventLog = new ArrayList<>();
  }

  public ColonyResources getResources() {
    return resources;
  }

  public ColonyMap getMap() {
    return map;
  }

  public void addWorker(String name, String type, String skill) {
    workers.put(name, new WorkerInfo(name, type, skill));
  }

  public void updateWorkerStatus(String name, String status) {
    WorkerInfo w = workers.get(name);
    if (w != null)
      w.setStatus(status);
  }

  public void updateWorkerSkill(String name, String skill) {
    WorkerInfo w = workers.get(name);
    if (w != null)
      w.setSkill(skill);
  }

  public WorkerInfo getWorker(String name) {
    return workers.get(name);
  }

  public Collection<WorkerInfo> getWorkers() {
    return workers.values();
  }

  public int getWorkerCount() {
    return workers.size();
  }

  public void addTask(String taskId, String taskType) {
    tasks.put(taskId, new TaskInfo(taskId, taskType));
  }

  public void updateTaskStatus(String taskId, String status) {
    TaskInfo t = tasks.get(taskId);
    if (t != null)
      t.setStatus(status);
  }

  public TaskInfo getTask(String taskId) {
    return tasks.get(taskId);
  }

  public Collection<TaskInfo> getTasks() {
    return tasks.values();
  }

  public void addEvent(String event) {
    eventLog.add(event);
    if (eventLog.size() > 500)
      eventLog.remove(0);
  }

  public List<String> getEventLog() {
    return new ArrayList<>(eventLog);
  }

  public String getWorkerAnalysis() {
    if (workers.isEmpty())
      return "Nenhum trabalhador registrado ainda.";
    int total = workers.size();
    Map<String, Integer> bySkill = new HashMap<>();
    for (WorkerInfo w : workers.values()) {
      bySkill.merge(w.getSkill(), 1, Integer::sum);
    }
    StringBuilder sb = new StringBuilder("Trabalhadores: " + total + " total\n");
    for (Map.Entry<String, Integer> e : bySkill.entrySet()) {
      sb.append("  ").append(e.getKey()).append(": ").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }
}