package com.colony.model.construction;

import com.colony.model.BuildingType;
import java.util.Map;

public class RoadConstruction extends ConstructionModel {
  private static final Map<String, Integer> COST = Map.of(
      "pedra", 10);

  public RoadConstruction() {
    super(BuildingType.ROAD, BuildingType.ROAD.getName(), COST);
  }
}
