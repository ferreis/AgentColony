package com.colony.model.construction;

import com.colony.model.BuildingType;
import java.util.EnumMap;
import java.util.Map;

public final class ConstructionCatalog {
  private static final Map<BuildingType, ConstructionModel> MODELS = createCatalog();

  private ConstructionCatalog() {
  }

  public static ConstructionModel forType(BuildingType type) {
    return MODELS.getOrDefault(type, new GenericConstruction(type));
  }

  private static Map<BuildingType, ConstructionModel> createCatalog() {
    Map<BuildingType, ConstructionModel> catalog = new EnumMap<>(BuildingType.class);

    catalog.put(BuildingType.HOUSE, new HouseConstruction(BuildingType.HOUSE));

    registerWorkshop(catalog, BuildingType.WORKSHOP);
    registerWorkshop(catalog, BuildingType.CARPENTER);
    registerWorkshop(catalog, BuildingType.MASON);
    registerWorkshop(catalog, BuildingType.SMITH);
    registerWorkshop(catalog, BuildingType.CRAFTER);
    registerWorkshop(catalog, BuildingType.KITCHEN);
    registerWorkshop(catalog, BuildingType.HOSPITAL);
    registerWorkshop(catalog, BuildingType.BARRACKS);
    registerWorkshop(catalog, BuildingType.TRADER);

    catalog.put(BuildingType.WAREHOUSE, new WarehouseConstruction(BuildingType.WAREHOUSE));

    catalog.put(BuildingType.ROAD, new RoadConstruction());

    catalog.put(BuildingType.FARM, new GenericConstruction(BuildingType.FARM));
    catalog.put(BuildingType.WELL, new GenericConstruction(BuildingType.WELL));

    return Map.copyOf(catalog);
  }

  private static void registerWorkshop(Map<BuildingType, ConstructionModel> catalog, BuildingType type) {
    catalog.put(type, new WorkshopConstruction(type));
  }
}
