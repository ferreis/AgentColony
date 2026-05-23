package com.colony.model;

public enum BuildingType {
  HOUSE("Casa", 3, 3, true, "residencial"),
  CARPENTER("Oficina de Carpintaria", 5, 4, true, "industrial"),
  MASON("Oficina de Alvenaria", 5, 4, true, "industrial"),
  SMITH("Forja", 5, 4, true, "industrial"),
  CRAFTER("Oficina de Artesanato", 5, 4, true, "industrial"),
  FARM("Fazenda", 6, 5, false, "agrícola"),
  KITCHEN("Cozinha", 4, 3, true, "industrial"),
  HOSPITAL("Hospital", 5, 5, true, "serviços"),
  BARRACKS("Quartel", 5, 5, true, "militar"),
  STOCKPILE("Depósito", 5, 5, false, "armazenamento"),
  WELL("Poço", 2, 2, false, "infraestrutura"),
  TRADER("Posto Comercial", 4, 4, true, "comércio");

  private final String name;
  private final int width;
  private final int height;
  private final boolean hasRoof;
  private final String zone;

  BuildingType(String name, int width, int height, boolean hasRoof, String zone) {
    this.name = name;
    this.width = width;
    this.height = height;
    this.hasRoof = hasRoof;
    this.zone = zone;
  }

  public String getName() {
    return name;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public boolean hasRoof() {
    return hasRoof;
  }

  public String getZone() {
    return zone;
  }
}
