package com.colony.model.construction;

import com.colony.model.BuildingType;
import java.util.Map;

public class HouseConstruction extends ConstructionModel {
  private static final Map<String, Integer> COST = Map.of(
      "madeira", 50,
  "pedra", 50);

  public HouseConstruction(BuildingType type) {
    super(type, type.getName(), COST);
  }
}
