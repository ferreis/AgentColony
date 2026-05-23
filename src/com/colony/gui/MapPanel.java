package com.colony.gui;

import com.colony.model.*;
import com.colony.model.BuildingType;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

public class MapPanel extends JPanel {
  private final ColonyMap map;
  private double scale = 6.0;
  private int viewX = ColonyMap.WIDTH / 2, viewY = ColonyMap.HEIGHT / 2;
  private int mouseX, mouseY;
  private String selectedNpc;
  private final Map<String, Color> npcColors;
  private final Map<String, int[]> npcPositions;
  private BufferedImage dwarfSpriteSheet;
  private static final int FRAME_W = 32;
  private static final int FRAME_H = 32;
  private static final int COLS = 12;
  private final Map<String, int[]> npcSpriteFrames;

  public MapPanel(ColonyMap map) {
    this.map = map;
    this.npcColors = new HashMap<>();
    this.npcPositions = new HashMap<>();
    this.npcSpriteFrames = new HashMap<>();
    loadSpriteSheet();
    setBackground(new Color(30, 30, 35));
    setPreferredSize(new Dimension(
        (int) (ColonyMap.WIDTH * scale),
        (int) (ColonyMap.HEIGHT * scale)));

    MouseAdapter ma = new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        int mx = (int) ((e.getX() - getWidth() / 2) / scale + viewX);
        int my = (int) ((e.getY() - getHeight() / 2) / scale + viewY);
        if (map.inBounds(mx, my)) {
          // Check if clicked on NPC
          selectedNpc = null;
          for (Map.Entry<String, int[]> npc : npcPositions.entrySet()) {
            int[] pos = npc.getValue();
            if (Math.abs(pos[0] - mx) <= 1 && Math.abs(pos[1] - my) <= 1) {
              selectedNpc = npc.getKey();
              break;
            }
          }
          if (selectedNpc == null) {
            showTileInfo(mx, my);
          }
        }
      }

      public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
      }

      public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.isControlDown()) {
          double oldScale = scale;
          scale = Math.max(4, Math.min(32, scale - e.getWheelRotation() * 2));
          setPreferredSize(new Dimension(
              (int) (ColonyMap.WIDTH * scale),
              (int) (ColonyMap.HEIGHT * scale)));
          revalidate();
          repaint();
          e.consume(); // Prevents JScrollPane from scrolling vertically
        } else {
          // Allow JScrollPane to handle normal scrolling
          getParent().dispatchEvent(e);
        }
      }
    };
    addMouseListener(ma);
    addMouseMotionListener(ma);
    addMouseWheelListener(ma);

    // Scroll with arrow keys
    setFocusable(true);
    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        int step = 15;
        if (e.isShiftDown())
          step = 45;
        switch (e.getKeyCode()) {
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_A:
            viewX -= step;
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_D:
            viewX += step;
            break;
          case KeyEvent.VK_UP:
          case KeyEvent.VK_W:
            viewY -= step;
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_S:
            viewY += step;
            break;
          case KeyEvent.VK_SPACE:
            viewX = ColonyMap.WIDTH / 2;
            viewY = ColonyMap.HEIGHT / 2;
            break;
          case KeyEvent.VK_MINUS:
            scale = Math.max(4, scale - 2);
            break;
          case KeyEvent.VK_PLUS:
          case KeyEvent.VK_EQUALS:
            scale = Math.min(32, scale + 2);
            break;
        }
        setPreferredSize(new Dimension((int) (ColonyMap.WIDTH * scale), (int) (ColonyMap.HEIGHT * scale)));
        revalidate();
        repaint();
      }
    });
  }

  public void setNpcPosition(String npcId, int x, int y) {
    npcPositions.put(npcId, new int[] { x, y });
    if (!npcColors.containsKey(npcId)) {
      Random r = new Random(npcId.hashCode());
      npcColors.put(npcId, new Color(r.nextInt(200) + 55, r.nextInt(200) + 55, r.nextInt(200) + 55));
    }
    repaint();
  }

  public String getSelectedNpc() {
    return selectedNpc;
  }

  private void loadSpriteSheet() {
    // Tenta 1: classpath (mais confiável em JARs/IDE)
    try {
      java.io.InputStream is = getClass().getResourceAsStream("/com/colony/dwarves.png");
      if (is != null) {
        dwarfSpriteSheet = ImageIO.read(is);
        is.close();
      }
    } catch (Exception e) {
      /* fallback */ }

    // Tenta 2: caminho relativo ao working directory
    if (dwarfSpriteSheet == null) {
      try {
        dwarfSpriteSheet = ImageIO.read(new File("src/com/colony/dwarves.png"));
      } catch (IOException e) {
        /* fallback */ }
    }

    // Tenta 3: caminho absoluto (último recurso)
    if (dwarfSpriteSheet == null) {
      try {
        dwarfSpriteSheet = ImageIO.read(new File(
            System.getProperty("user.dir"), "src/com/colony/dwarves.png"));
      } catch (IOException e) {
        /* fallback */ }
    }

    if (dwarfSpriteSheet != null) {
      System.out.println("[MapPanel] Spritesheet carregado: "
          + dwarfSpriteSheet.getWidth() + "x" + dwarfSpriteSheet.getHeight());
    } else {
      System.err.println("[MapPanel] AVISO: dwarves.png NÃO carregado. user.dir="
          + System.getProperty("user.dir") + ". Usando círculos coloridos.");
    }
  }

  private int[] getSpriteFrame(String npcId) {
    int[] frame = npcSpriteFrames.get(npcId);
    if (frame == null) {
      Random r = new Random(npcId.hashCode());
      int col = r.nextInt(COLS);
      int row = 5 + r.nextInt(14);
      frame = new int[] { col, row };
      npcSpriteFrames.put(npcId, frame);
    }
    return frame;
  }

  public int[] getMouseTile() {
    return new int[] {
        (int) ((mouseX - getWidth() / 2) / scale + viewX),
        (int) ((mouseY - getHeight() / 2) / scale + viewY)
    };
  }

  private void showTileInfo(int tx, int ty) {
    TerrainTile t = map.getTile(tx, ty);
    ColonyMap.ColonyBuilding b = map.getBuildingAt(tx, ty);
    StringBuilder sb = new StringBuilder();
    sb.append("<html>Tile: ").append(tx).append(", ").append(ty).append("<br>");
    sb.append("Terreno: ").append(t.getName());
    if (b != null) {
      sb.append("<br>Construção: ").append(b.getType().getName())
          .append(" (").append(b.getProgress()).append("%)");
      if (b.getOwner() != null)
        sb.append("<br>Dono: ").append(b.getOwner());
    }
    JOptionPane.showMessageDialog(this, sb.toString(), "Info do Tile", JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    int w = getWidth(), h = getHeight();
    int halfW = w / 2, halfH = h / 2;

    int startX = Math.max(0, (int) (viewX - halfW / scale) - 1);
    int startY = Math.max(0, (int) (viewY - halfH / scale) - 1);
    int endX = Math.min(ColonyMap.WIDTH, (int) (viewX + halfW / scale) + 2);
    int endY = Math.min(ColonyMap.HEIGHT, (int) (viewY + halfH / scale) + 2);

    // ─── Draw terrain tiles ───
    for (int y = startY; y < endY; y++) {
      for (int x = startX; x < endX; x++) {
        TerrainTile tile = map.getTile(x, y);
        int px = halfW + (int) ((x - viewX) * scale);
        int py = halfH + (int) ((y - viewY) * scale);
        int sz = (int) Math.ceil(scale) + 1;

        Color cor = tileColor(tile);
        g2.setColor(cor);
        g2.fillRect(px, py, sz, sz);

        // Graphical improvements for specific tiles
        if (scale >= 6) {
          if (tile == TerrainTile.TREE) {
            g2.setColor(new Color(60, 40, 20)); // Trunk
            g2.fillRect(px + sz / 2 - 1, py + sz / 2, 3, sz / 2);
            g2.setColor(new Color(30, 110, 30)); // Canopy
            g2.fillOval(px, py, sz, (int) (sz * 0.8));
            g2.setColor(new Color(40, 140, 40));
            g2.fillOval(px + 2, py + 2, sz - 4, (int) (sz * 0.6));
          } else if (tile == TerrainTile.MOUNTAIN) {
            g2.setColor(new Color(70, 70, 75));
            int[] pxs = { px, px + sz / 2, px + sz };
            int[] pys = { py + sz, py, py + sz };
            g2.fillPolygon(pxs, pys, 3);
            g2.setColor(new Color(90, 90, 95));
            int[] pxsLight = { px + sz / 4, px + sz / 2, px + sz };
            int[] pysLight = { py + sz, py, py + sz };
            g2.fillPolygon(pxsLight, pysLight, 3);
          } else if (tile == TerrainTile.WATER) {
            g2.setColor(new Color(255, 255, 255, 30));
            g2.drawLine(px, py + sz / 2, px + sz, py + sz / 2);
          } else if (scale >= 10) {
            // Subtle grid for grass/dirt
            g2.setColor(new Color(0, 0, 0, 10));
            g2.drawRect(px, py, sz, sz);
          }
        }
      }
    }

    // ─── Draw buildings with 3D shadow ───
    for (ColonyMap.ColonyBuilding b : map.getBuildings()) {
      int px = halfW + (int) ((b.getX() - viewX) * scale);
      int py = halfH + (int) ((b.getY() - viewY) * scale);
      int bw = (int) (b.getType().getWidth() * scale);
      int bh = (int) (b.getType().getHeight() * scale);

      // Shadow
      g2.setColor(new Color(0, 0, 0, 80));
      g2.fillRect(px + 3, py + 3, bw, bh);

      Color cor = buildingColor(b.getType());
      g2.setColor(cor);
      g2.fillRect(px, py, bw, bh);

      // Highlight
      g2.setColor(new Color(255, 255, 255, 40));
      g2.fillRect(px, py, bw, (int) (bh * 0.2));

      // Border
      g2.setColor(cor.darker().darker());
      g2.setStroke(new BasicStroke(2));
      g2.drawRect(px, py, bw, bh);
      g2.setStroke(new BasicStroke(1));

      // Porta
      if (b.getType().hasRoof() && scale >= 6) {
        int doorX = px + (bw / b.getType().getWidth()) * (b.getType().getWidth() / 2);
        int doorY = py + bh - (bh / b.getType().getHeight());
        int tileW = bw / b.getType().getWidth();
        int tileH = bh / b.getType().getHeight();
        g2.setColor(new Color(80, 50, 30)); // Marrom escuro para a porta
        g2.fillRect(doorX, doorY, tileW, tileH);
        g2.setColor(Color.BLACK);
        g2.drawRect(doorX, doorY, tileW, tileH);
      }

      // Ícone/baseado no tipo
      if (scale >= 6) {
        String icon = b.getType().hasRoof() ? "\u2302" : "\u2591";
        g2.setFont(new Font("Monospaced", Font.PLAIN, Math.max(10, (int) (scale * 0.9))));
        g2.setColor(Color.WHITE);
        g2.drawString(icon, px + 4, py + (int) (scale));
      }

      // Nome do dono se for casa
      if (b.getOwner() != null && scale >= 10) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, (int) (scale * 0.5))));
        g2.drawString(b.getOwner(), px + 2, py + bh - 4);
      }

      // Barra de progresso
      if (b.getProgress() < 100) {
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect(px, py + bh + 4, bw, 4);
        g2.setColor(new Color(50, 220, 50));
        g2.fillRect(px, py + bh + 4, bw * b.getProgress() / 100, 4);
        g2.setColor(Color.BLACK);
        g2.drawRect(px, py + bh + 4, bw, 4);
      }
    }

    // ─── Draw Animals ───
    for (com.colony.model.Animal a : map.getAnimals()) {
      int px = halfW + (int) ((a.x - viewX) * scale);
      int py = halfH + (int) ((a.y - viewY) * scale);
      int r = Math.max(4, (int) (scale * 0.6));

      g2.setColor(a.dead ? Color.GRAY : (a.aggressive ? Color.RED : new Color(139, 69, 19)));
      g2.fillOval(px - r / 2, py - r / 2, r, r);

      if (scale >= 6) {
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, (int) (scale / 1.5))));
        String icon = a.dead ? "\u2620" : (a.aggressive ? "\uD83D\uDC3A" : "\uD83E\uDD8C"); // Caveira, Lobo ou Cervo
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(icon, px - fm.stringWidth(icon) / 2, py + r);
      }
    }

    // ─── Draw NPCs ───
    for (Map.Entry<String, int[]> npc : npcPositions.entrySet()) {
      int[] pos = npc.getValue();
      int px = halfW + (int) ((pos[0] - viewX) * scale);
      int py = halfH + (int) ((pos[1] - viewY) * scale);
      int r = Math.max(4, (int) (scale * 0.6));

      if (dwarfSpriteSheet != null) {
        int[] frame = getSpriteFrame(npc.getKey());
        int sx = frame[0] * FRAME_W;
        int sy = frame[1] * FRAME_H;
        int spriteSize = Math.max(6, (int) (scale * 1.5)); // slightly larger sprite

        // Shadow for sprite
        g2.setColor(new Color(0, 0, 0, 60));
        g2.fillOval(px - spriteSize / 3, py + spriteSize / 4, spriteSize * 2 / 3, spriteSize / 4);

        g2.drawImage(dwarfSpriteSheet,
            px - spriteSize / 2, py - spriteSize / 2,
            px + spriteSize / 2, py + spriteSize / 2,
            sx, sy, sx + FRAME_W, sy + FRAME_H, null);

        if (npc.getKey().equals(selectedNpc)) {
          g2.setColor(Color.YELLOW);
          g2.setStroke(new BasicStroke(2));
          g2.drawRect(px - spriteSize / 2 - 1, py - spriteSize / 2 - 1, spriteSize + 2, spriteSize + 2);
          g2.setStroke(new BasicStroke(1));
        }
      } else {
        Color cor = npcColors.getOrDefault(npc.getKey(), Color.WHITE);

        // Shadow
        g2.setColor(new Color(0, 0, 0, 50));
        g2.fillOval(px - r + 2, py - r + 2, r * 2, r * 2);

        g2.setColor(cor);
        g2.fillOval(px - r, py - r, r * 2, r * 2);
        g2.setColor(cor.darker().darker());
        g2.setStroke(new BasicStroke(2));
        g2.drawOval(px - r, py - r, r * 2, r * 2);
        g2.setStroke(new BasicStroke(1));

        // Little eye details
        if (scale >= 8) {
          g2.setColor(Color.WHITE);
          g2.fillOval(px - r / 2, py - r / 2, r / 2, r / 2);
          g2.fillOval(px + 1, py - r / 2, r / 2, r / 2);
          g2.setColor(Color.BLACK);
          g2.fillOval(px - r / 2 + 1, py - r / 2 + 1, r / 4, r / 4);
          g2.fillOval(px + 2, py - r / 2 + 1, r / 4, r / 4);
        }

        if (npc.getKey().equals(selectedNpc)) {
          g2.setColor(Color.YELLOW);
          g2.drawOval(px - r - 3, py - r - 3, r * 2 + 6, r * 2 + 6);
        }
      }

      // Name label when zoomed in
      if (scale >= 14) {
        String name = npc.getKey();
        g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(10, (int) (scale * 0.5))));
        FontMetrics fm = g2.getFontMetrics();
        int textW = fm.stringWidth(name);

        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(px - textW / 2 - 2, py - r - fm.getHeight(), textW + 4, fm.getHeight() + 2);
        g2.setColor(Color.WHITE);
        g2.drawString(name, px - textW / 2, py - r - 2);
      }
    }

    // ─── Mouse hover coordinates ───
    int[] tile = getMouseTile();
    if (map.inBounds(tile[0], tile[1])) {
      g2.setColor(new Color(255, 255, 255, 80));
      int px = halfW + (int) ((tile[0] - viewX) * scale);
      int py = halfH + (int) ((tile[1] - viewY) * scale);
      int sz = (int) Math.ceil(scale) + 1;
      g2.fillRect(px, py, sz, sz);
      g2.setColor(Color.WHITE);
      g2.drawRect(px, py, sz, sz);

      g2.setFont(new Font("SansSerif", Font.BOLD, 12));
      g2.drawString("(" + tile[0] + "," + tile[1] + ")", 10, getHeight() - 10);
    }
  }

  private Color buildingColor(BuildingType type) {
    return switch (type.getZone()) {
      case "residencial" -> new Color(190, 140, 90);
      case "industrial" -> new Color(150, 110, 70);
      case "agrícola" -> new Color(130, 190, 80);
      case "serviços" -> new Color(210, 110, 110);
      case "militar" -> new Color(190, 70, 70);
      case "armazenamento" -> new Color(170, 160, 100);
      case "infraestrutura" -> new Color(110, 150, 190);
      case "comércio" -> new Color(210, 190, 70);
      default -> new Color(139, 90, 43);
    };
  }

  private Color tileColor(TerrainTile tile) {
    return switch (tile.getName()) {
      case "grama" -> new Color(90, 180, 70);
      case "terra" -> new Color(160, 130, 80);
      case "pedra" -> new Color(140, 140, 145);
      case "montanha" -> new Color(80, 75, 85); // base color
      case "árvore" -> new Color(70, 160, 50); // grass under tree
      case "água" -> new Color(60, 120, 210);
      case "areia" -> new Color(230, 210, 150);
      case "parede" -> new Color(110, 85, 65);
      case "chão" -> new Color(180, 165, 125);
      default -> Color.GRAY;
    };
  }
}
