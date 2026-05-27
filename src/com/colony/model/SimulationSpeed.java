package com.colony.model;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public final class SimulationSpeed {
  public static final int SPEED_1X = 1;
  public static final int SPEED_2X = 2;
  public static final int SPEED_5X = 5;
  public static final int SPEED_10X = 10;

  private static final AtomicInteger MULTIPLIER = new AtomicInteger(SPEED_1X);

  private SimulationSpeed() {
  }

  public static int getMultiplier() {
    return MULTIPLIER.get();
  }

  public static String getLabel() {
    return getMultiplier() + "x";
  }

  public static boolean setMultiplier(int multiplier) {
    if (!isSupported(multiplier)) {
      return false;
    }
    MULTIPLIER.set(multiplier);
    return true;
  }

  public static void initializeFromSystemProperty() {
    String raw = System.getProperty("colony.speed", "1x");
    setFromText(raw);
  }

  public static boolean setFromText(String value) {
    if (value == null) {
      return false;
    }

    String normalized = value.trim().toLowerCase(Locale.ROOT);
    return switch (normalized) {
      case "1", "1x", "normal" -> setMultiplier(SPEED_1X);
      case "2", "2x", "rapido", "rápido" -> setMultiplier(SPEED_2X);
      case "5", "5x", "muito-rapido", "muito-rápido" -> setMultiplier(SPEED_5X);
      case "10", "10x", "ultra", "ultra-rapido", "ultra-rápido" -> setMultiplier(SPEED_10X);
      default -> false;
    };
  }

  public static long scaleDelay(long baseMs) {
    long safeBase = Math.max(1L, baseMs);
    int multiplier = Math.max(1, getMultiplier());
    return Math.max(1L, safeBase / multiplier);
  }

  private static boolean isSupported(int multiplier) {
    return multiplier == SPEED_1X
        || multiplier == SPEED_2X
        || multiplier == SPEED_5X
        || multiplier == SPEED_10X;
  }
}
