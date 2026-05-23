package com.colony.model;

public enum TerrainTile {
  GRASS("grama", ":", false),
  DIRT("terra", ".", false),
  STONE("pedra", "#", true),
  MOUNTAIN("montanha", "^", true),
  TREE("árvore", "T", true),
  WATER("água", "~", false),
  SAND("areia", "s", false),
  WALL("parede", "|", true),
  FLOOR("chão", "_", false);

  private final String name;
  private final String symbol;
  private final boolean blocksMovement;

  TerrainTile(String name, String symbol, boolean blocksMovement) {
    this.name = name;
    this.symbol = symbol;
    this.blocksMovement = blocksMovement;
  }

  public String getName() {
    return name;
  }

  public String getSymbol() {
    return symbol;
  }

  public boolean isBlocksMovement() {
    return blocksMovement;
  }
}
