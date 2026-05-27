package com.colony.model.construction;

import com.colony.model.BuildingType;
import java.util.Map;

public class WarehouseConstruction extends ConstructionModel {
  private static final Map<String, Integer> COST = Map.of(
      "madeira", 40,
      "pedra", 55);

  public WarehouseConstruction(BuildingType type) {
    super(type, type.getName(), COST);
  }
}
