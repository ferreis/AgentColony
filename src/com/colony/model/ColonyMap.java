package com.colony.model;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.*;

public class ColonyMap {
  public static final int WIDTH = 200;
  public static final int HEIGHT = 200;
  private final TerrainTile[][] tiles;
  private final List<ColonyBuilding> buildings;
  private final Map<String, int[]> npcPositions;
  private final Map<String, ColonyBuilding> npcHomes;
  private final Map<String, String> zoneNames;
  private final List<Animal> animals = new CopyOnWriteArrayList<>();

  public ColonyMap() {
    this.tiles = new TerrainTile[HEIGHT][WIDTH];
    this.buildings = new CopyOnWriteArrayList<>();
    this.npcPositions = new HashMap<>();
    this.npcHomes = new HashMap<>();
    this.zoneNames = new HashMap<>();
    generateTerrain();
    placeStartingZone();
  }

  public List<Animal> getAnimals() {
    return animals;
  }

  public void addAnimal(Animal a) {
    animals.add(a);
  }

  public void removeAnimal(Animal a) {
    animals.remove(a);
  }

  private void generateTerrain() {
    for (int y = 0; y < HEIGHT; y++) {
      for (int x = 0; x < WIDTH; x++) {
        double h = noise(x * 0.02, y * 0.02) * 0.6
            + noise(x * 0.04, y * 0.04) * 0.25
            + noise(x * 0.08, y * 0.08) * 0.15;
        double dx = (x - WIDTH / 2.0) / (WIDTH / 2.0);
        double dy = (y - HEIGHT / 2.0) / (HEIGHT / 2.0);
        h = h * 0.7 + (dx * dx + dy * dy) * 0.5 - 0.3;

        if (h < -0.3)
          tiles[y][x] = TerrainTile.WATER;
        else if (h < -0.1)
          tiles[y][x] = TerrainTile.SAND;
        else if (h < 0.05)
          tiles[y][x] = TerrainTile.GRASS;
        else if (h < 0.15)
          tiles[y][x] = TerrainTile.DIRT;
        else if (h < 0.25)
          tiles[y][x] = TerrainTile.FLOOR;
        else if (h < 0.35)
          tiles[y][x] = TerrainTile.TREE;
        else if (h < 0.45)
          tiles[y][x] = TerrainTile.STONE;
        else
          tiles[y][x] = TerrainTile.MOUNTAIN;
      }
    }
  }

  private double noise(double x, double y) {
    int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
    double fx = x - ix, fy = y - iy;
    int seed = 12345;
    double v00 = hash(ix + seed, iy + seed);
    double v10 = hash(ix + 1 + seed, iy + seed);
    double v01 = hash(ix + seed, iy + 1 + seed);
    double v11 = hash(ix + 1 + seed, iy + 1 + seed);
    double sx = smoothstep(fx), sy = smoothstep(fy);
    return lerp(lerp(v00, v10, sx), lerp(v01, v11, sx), sy);
  }

  private double hash(int x, int y) {
    int h = x * 374761393 + y * 668265263;
    h = (h ^ (h >> 13)) * 1274126177;
    h = h ^ (h >> 16);
    return (h & 0xFFFF) / 32768.0 - 1.0;
  }

  private double smoothstep(double t) {
    return t * t * (3 - 2 * t);
  }

  private double lerp(double a, double b, double t) {
    return a + t * (b - a);
  }

  private void placeStartingZone() {
    int cx = WIDTH / 2, cy = HEIGHT / 2;
    for (int dy = -15; dy <= 15; dy++) {
      for (int dx = -15; dx <= 15; dx++) {
        if (inBounds(cx + dx, cy + dy))
          tiles[cy + dy][cx + dx] = TerrainTile.FLOOR;
      }
    }
    // Já coloca um depósito inicial e um poço funcional para água.
    ColonyBuilding stockpile = addBuilding(cx - 8, cy - 2, BuildingType.STOCKPILE);
    stockpile.setProgress(100);

    ColonyBuilding well = addBuilding(cx + 8, cy - 1, BuildingType.WELL);
    well.setProgress(100);
  }

  // ─── Tile access ───

  public TerrainTile getTile(int x, int y) {
    if (!inBounds(x, y))
      return TerrainTile.WALL;
    ColonyBuilding b = getBuildingAt(x, y);
    if (b != null) {
      if (b.getType().hasRoof()) {
        int bx = b.getX();
        int by = b.getY();
        int bw = b.getType().getWidth();
        int bh = b.getType().getHeight();

        // Door at the bottom center
        if (x == bx + bw / 2 && y == by + bh - 1) {
          return TerrainTile.FLOOR; // Porta
        }

        // Walls on the border
        if (x == bx || x == bx + bw - 1 || y == by || y == by + bh - 1) {
          return TerrainTile.WALL;
        }

        // Interior floor
        return TerrainTile.FLOOR;
      } else {
        return TerrainTile.FLOOR;
      }
    }
    return tiles[y][x];
  }

  public void setTile(int x, int y, TerrainTile tile) {
    if (inBounds(x, y))
      tiles[y][x] = tile;
  }

  public boolean inBounds(int x, int y) {
    return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
  }

  // ─── Buildings ───

  public static class ColonyBuilding {
    private final int x, y;
    private final BuildingType type;
    private int progress;
    private String owner;
    private boolean constructionCostPaid;

    public ColonyBuilding(int x, int y, BuildingType type) {
      this.x = x;
      this.y = y;
      this.type = type;
      this.progress = 0;
      this.owner = null;
      this.constructionCostPaid = false;
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public BuildingType getType() {
      return type;
    }

    public int getProgress() {
      return progress;
    }

    public void setProgress(int p) {
      progress = Math.min(100, p);
    }

    public String getOwner() {
      return owner;
    }

    public void setOwner(String o) {
      this.owner = o;
    }

    public boolean isConstructionCostPaid() {
      return constructionCostPaid;
    }

    public void setConstructionCostPaid(boolean paid) {
      this.constructionCostPaid = paid;
    }
  }

  public List<ColonyBuilding> getBuildings() {
    return buildings;
  }

  public ColonyBuilding getBuildingAt(int x, int y) {
    for (ColonyBuilding b : buildings) {
      if (x >= b.x && x < b.x + b.type.getWidth() &&
          y >= b.y && y < b.y + b.type.getHeight())
        return b;
    }
    return null;
  }

  public ColonyBuilding addBuilding(int x, int y, BuildingType type) {
    ColonyBuilding b = new ColonyBuilding(x, y, type);
    buildings.add(b);
    return b;
  }

  public void buildProgress(String npcId, int tx, int ty) {
    ColonyBuilding b = getBuildingAt(tx, ty);
    if (b != null) {
      b.setProgress(b.getProgress() + 10 + new Random().nextInt(20));
    }
  }

  public ColonyBuilding findNearestUnowned(BuildingType type, int fromX, int fromY) {
    ColonyBuilding best = null;
    double bestDist = Double.MAX_VALUE;
    for (ColonyBuilding b : buildings) {
      if (b.getType() == type && b.getOwner() == null) {
        double d = Math.hypot(b.x - fromX, b.y - fromY);
        if (d < bestDist) {
          best = b;
          bestDist = d;
        }
      }
    }
    return best;
  }

  public ColonyBuilding findNearestCompleted(BuildingType type, int fromX, int fromY) {
    ColonyBuilding best = null;
    double bestDist = Double.MAX_VALUE;
    for (ColonyBuilding b : buildings) {
      if (b.getType() != type || b.getProgress() < 100) {
        continue;
      }

      double d = Math.hypot(b.x - fromX, b.y - fromY);
      if (d < bestDist) {
        best = b;
        bestDist = d;
      }
    }
    return best;
  }

  public int[] findSpotFor(BuildingType type) {
    return findSpotFor(type, WIDTH / 2, HEIGHT / 2);
  }

  public int[] findSpotFor(BuildingType type, int nearX, int nearY) {
    Random r = new Random();
    // Search expanding rings around the preferred location
    for (int radius = 0; radius < 60; radius += 3) {
      for (int attempt = 0; attempt < 30; attempt++) {
        int x = nearX - radius + r.nextInt(radius * 2 + 1);
        int y = nearY - radius + r.nextInt(radius * 2 + 1);
        if (canPlace(x, y, type))
          return new int[] { x, y };
      }
    }
    // Fallback: random search over whole map
    for (int attempt = 0; attempt < 200; attempt++) {
      int x = 10 + r.nextInt(WIDTH - 20);
      int y = 10 + r.nextInt(HEIGHT - 20);
      if (canPlace(x, y, type))
        return new int[] { x, y };
    }
    return null;
  }

  private boolean canPlace(int x, int y, BuildingType type) {
    // Enforce 1-tile distance around the building footprint
    for (int dy = -1; dy <= type.getHeight(); dy++) {
      for (int dx = -1; dx <= type.getWidth(); dx++) {
        int nx = x + dx, ny = y + dy;

        if (!inBounds(nx, ny)) {
          return false;
        }

        // Only check for terrain blocking inside the actual footprint
        if (dx >= 0 && dx < type.getWidth() && dy >= 0 && dy < type.getHeight()) {
          if (type == BuildingType.HOUSE && tiles[ny][nx] == TerrainTile.WATER) {
            return false;
          }
          if (getTile(nx, ny).isBlocksMovement()) {
            return false;
          }
        }

        // But check for overlapping buildings in the expanded footprint (1-tile
        // padding)
        if (getBuildingAt(nx, ny) != null) {
          return false;
        }
      }
    }
    return true;
  }

  // ─── NPC Homes ───

  public void assignHome(String npcId, ColonyBuilding house) {
    npcHomes.put(npcId, house);
    house.setOwner(npcId);
  }

  public ColonyBuilding getHome(String npcId) {
    return npcHomes.get(npcId);
  }

  public boolean hasHome(String npcId) {
    return npcHomes.containsKey(npcId);
  }

  public ColonyBuilding findAvailableHouse() {
    for (ColonyBuilding b : buildings) {
      if (b.getType() == BuildingType.HOUSE && b.getOwner() == null
          && b.getProgress() >= 100)
        return b;
    }
    return null;
  }

  // ─── NPC Positions ───

  public void setNpcPosition(String npcId, int x, int y) {
    npcPositions.put(npcId, new int[] { x, y });
  }

  public int[] getNpcPosition(String npcId) {
    return npcPositions.getOrDefault(npcId, new int[] { WIDTH / 2, HEIGHT / 2 });
  }

  public Map<String, int[]> getAllNpcPositions() {
    return new HashMap<>(npcPositions);
  }

  // ─── Zones ───

  public void setZoneName(int x, int y, String name) {
    zoneNames.put(x + "," + y, name);
  }

  public String getZoneName(int x, int y) {
    // Check building zones
    ColonyBuilding b = getBuildingAt(x, y);
    if (b != null) {
      String zn = b.getType().getZone();
      return zn != null ? zn : zoneNames.getOrDefault(x + "," + y, null);
    }
    return zoneNames.getOrDefault(x + "," + y, null);
  }

  // ─── Pathfinding ───

  public List<int[]> findPath(int sx, int sy, int tx, int ty) {
    if (!inBounds(sx, sy) || !inBounds(tx, ty))
      return Collections.emptyList();
    if (sx == tx && sy == ty)
      return Collections.emptyList();

    Queue<int[]> queue = new LinkedList<>();
    Map<String, int[]> cameFrom = new HashMap<>();
    Set<String> visited = new HashSet<>();

    queue.add(new int[] { sx, sy });
    visited.add(sx + "," + sy);

    int[][] dirs = { { 0, -1 }, { 0, 1 }, { -1, 0 }, { 1, 0 }, { -1, -1 }, { -1, 1 }, { 1, -1 }, { 1, 1 } };

    while (!queue.isEmpty()) {
      int[] cur = queue.poll();
      if (cur[0] == tx && cur[1] == ty) {
        List<int[]> path = new ArrayList<>();
        String key = cur[0] + "," + cur[1];
        while (cameFrom.containsKey(key)) {
          path.add(0, new int[] { cur[0], cur[1] });
          int[] prev = cameFrom.get(key);
          cur = prev;
          key = cur[0] + "," + cur[1];
        }
        return path;
      }
      for (int[] d : dirs) {
        int nx = cur[0] + d[0], ny = cur[1] + d[1];
        String key = nx + "," + ny;
        if (inBounds(nx, ny) && !visited.contains(key)) {
          TerrainTile t = getTile(nx, ny);
          if (!t.isBlocksMovement()) {
            visited.add(key);
            cameFrom.put(key, new int[] { cur[0], cur[1] });
            queue.add(new int[] { nx, ny });
          }
        }
      }
    }
    return Collections.emptyList();
  }

  public int[] findNearestOpenTile(int cx, int cy) {
    for (int r = 0; r < 20; r++) {
      for (int dx = -r; dx <= r; dx++) {
        for (int dy = -r; dy <= r; dy++) {
          int nx = cx + dx, ny = cy + dy;
          if (inBounds(nx, ny) && !getTile(nx, ny).isBlocksMovement()
              && getBuildingAt(nx, ny) == null) {
            return new int[] { nx, ny };
          }
        }
      }
    }
    return new int[] { cx, cy };
  }
}
