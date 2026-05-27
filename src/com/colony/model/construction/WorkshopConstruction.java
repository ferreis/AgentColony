package com.colony.model.construction;

import com.colony.model.BuildingType;
import java.util.Map;

public class WorkshopConstruction extends ConstructionModel {
  private static final Map<String, Integer> COST = Map.of(
      "pedra", 70,
      "ferro", 50,
      "madeira", 30);

  public WorkshopConstruction(BuildingType type) {
    super(type, type.getName(), COST);
  }
}
