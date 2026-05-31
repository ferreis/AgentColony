package com.colony.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.EnumMap;

import java.util.concurrent.ConcurrentHashMap;
import com.colony.model.ColonyMap.ColonyBuilding;

public class ColonyResources {
  private final Map<String, Integer> resources;
  private final Map<BuildingType, Integer> completedStorageCounts;
  private final Map<String, Integer> totalStorageCapacity;

  private static final Map<BuildingType, Map<String, Integer>> STORAGE_CAPACITY_BY_BUILDING = Map.of(
      BuildingType.WAREHOUSE, Map.of(
          "pedra", 250,
          "madeira", 250,
          "ferro", 200,
          "comida", 200,
          "agua", 100,
          "vara de pesca", 30));

  public ColonyResources() {
    resources = new ConcurrentHashMap<>();
    completedStorageCounts = new EnumMap<>(BuildingType.class);
    totalStorageCapacity = new ConcurrentHashMap<>();

    resources.put("madeira", 200);
    resources.put("pedra", 200);
    resources.put("ferro", 120);
    resources.put("comida", 50);
    resources.put("agua", 20);
    resources.put("vara de pesca", 1);

    // Default to one active warehouse capacity until manager syncs from map state.
    setWarehouseCount(1);
  }

  public synchronized int get(String resource) {
    return resources.getOrDefault(resource.toLowerCase(), 0);
  }

  public synchronized int add(String resource, int amount) {
    if (amount <= 0) {
      return 0;
    }

    String key = resource.toLowerCase();
    int current = resources.getOrDefault(key, 0);
    Integer totalCap = totalStorageCapacity.get(key);
    if (totalCap == null) {
      resources.merge(key, amount, Integer::sum);
      return amount;
    }

    int max = Math.max(0, totalCap);
    int availableSpace = Math.max(0, max - current);
    int added = Math.min(amount, availableSpace);
    if (added > 0) {
      resources.put(key, current + added);
    }
    return added;
  }

  public synchronized boolean consume(String resource, int amount) {
    String key = resource.toLowerCase();
    int current = resources.getOrDefault(key, 0);
    if (current >= amount) {
      resources.put(key, current - amount);
      return true;
    }
    return false;
  }

  private Set<String> trackedResourceKeys() {
    Set<String> keys = new java.util.HashSet<>();
    for (Map<String, Integer> profile : STORAGE_CAPACITY_BY_BUILDING.values()) {
      keys.addAll(profile.keySet());
    }
    return keys;
  }

  private boolean recalculateCapacitiesFromCounts() {
    Map<String, Integer> newTotals = new HashMap<>();

    for (Map.Entry<BuildingType, Map<String, Integer>> capacityEntry : STORAGE_CAPACITY_BY_BUILDING.entrySet()) {
      BuildingType type = capacityEntry.getKey();
      int count = Math.max(0, completedStorageCounts.getOrDefault(type, 0));
      if (count == 0) {
        continue;
      }

      for (Map.Entry<String, Integer> resourceEntry : capacityEntry.getValue().entrySet()) {
        int resourceCap = Math.max(0, resourceEntry.getValue());
        newTotals.merge(resourceEntry.getKey(), resourceCap * count, Integer::sum);
      }
    }

    for (String resource : trackedResourceKeys()) {
      newTotals.putIfAbsent(resource, 0);
    }

    if (newTotals.equals(totalStorageCapacity)) {
      return false;
    }

    totalStorageCapacity.clear();
    totalStorageCapacity.putAll(newTotals);
    return true;
  }

  public synchronized boolean syncStorageCapacityFromMap(ColonyMap colonyMap) {
    Map<BuildingType, Integer> newCounts = new EnumMap<>(BuildingType.class);

    for (ColonyBuilding building : colonyMap.getBuildings()) {
      if (building.getProgress() < 100) {
        continue;
      }

      BuildingType type = building.getType();
      if (!STORAGE_CAPACITY_BY_BUILDING.containsKey(type)) {
        continue;
      }

      newCounts.merge(type, 1, Integer::sum);
    }

    boolean countsChanged = !newCounts.equals(completedStorageCounts);
    completedStorageCounts.clear();
    completedStorageCounts.putAll(newCounts);

    boolean capacityChanged = recalculateCapacitiesFromCounts();
    return countsChanged || capacityChanged;
  }

  public synchronized void setWarehouseCount(int warehouseCount) {
    completedStorageCounts.put(BuildingType.WAREHOUSE, Math.max(0, warehouseCount));
    recalculateCapacitiesFromCounts();
  }

  public synchronized int getWarehouseCount() {
    return completedStorageCounts.getOrDefault(BuildingType.WAREHOUSE, 0);
  }

  public synchronized int getTotalCapacity(String resource) {
    Integer capacity = totalStorageCapacity.get(resource.toLowerCase());
    if (capacity == null) {
      return Integer.MAX_VALUE;
    }
    return capacity;
  }

  public synchronized boolean isAtCapacity(String resource) {
    String key = resource.toLowerCase();
    Integer totalCap = totalStorageCapacity.get(key);
    if (totalCap == null) {
      return false;
    }
    return resources.getOrDefault(key, 0) >= totalCap;
  }

  public synchronized boolean isAnyStorageAtCapacity() {
    for (String resource : trackedResourceKeys()) {
      if (isAtCapacity(resource)) {
        return true;
      }
    }
    return false;
  }

  public synchronized boolean isCompletelyFull() {
    for (String resource : trackedResourceKeys()) {
      int capacity = Math.max(0, totalStorageCapacity.getOrDefault(resource, 0));
      if (resources.getOrDefault(resource, 0) < capacity) {
        return false;
      }
    }
    return true;
  }

  public synchronized Map<String, Integer> getCapacitySnapshot() {
    return new HashMap<>(totalStorageCapacity);
  }

  public Map<String, Integer> getAll() {
    return new HashMap<>(resources);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, Integer> e : resources.entrySet()) {
      sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
    }
    return sb.toString();
  }
}