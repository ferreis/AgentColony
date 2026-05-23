package com.colony.model;

public class Animal {
  public int x, y;
  public int hp;
  public boolean aggressive;
  public String type;
  public boolean dead = false;
  public int rotTimer = 20;

  public Animal(int x, int y, int hp, boolean aggressive, String type) {
    this.x = x;
    this.y = y;
    this.hp = hp;
    this.aggressive = aggressive;
    this.type = type;
  }
}
