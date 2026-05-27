package com.colony.gui;

import com.colony.model.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.function.IntConsumer;

public class ColonyGUI {
  private final JFrame frame;
  private final DefaultTableModel workerModel;
  private final DefaultTableModel taskActiveModel;
  private final DefaultTableModel taskDoneModel;
  private final JTextArea logArea;
  private final JTextArea resourceArea;
  private final JLabel statusLabel;
  private final JLabel statsLabel;
  private final JLabel speedLabel;
  private final MapPanel mapPanel;
  private final Map<String, String> workerDetails;
  private final JTable workerTable;
  private int totalTasks = 0;

  public ColonyGUI(ColonyMap colonyMap, ColonyResources colonyResources, IntConsumer speedChangeListener) {
    workerDetails = new HashMap<>();

    frame = new JFrame("Gerenciador de Colônia - Inspirado em Dwarf Fortress");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setSize(960, 720);
    frame.setMinimumSize(new Dimension(800, 600));
    frame.setLocationRelativeTo(null);

    // ── Top status bar ──
    JPanel topBar = new JPanel(new BorderLayout());
    topBar.setBackground(new Color(50, 50, 60));

    statusLabel = new JLabel(" Colônia inicializando... ");
    statusLabel.setForeground(Color.WHITE);
    statusLabel.setFont(new Font("Monospaced", Font.BOLD, 13));
    statusLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    topBar.add(statusLabel, BorderLayout.WEST);

    JLabel versionLabel = new JLabel("v0.2.0");
    versionLabel.setForeground(new Color(150, 180, 220));
    versionLabel.setFont(new Font("Monospaced", Font.BOLD, 12));
    versionLabel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 14));

    JPanel topRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
    topRight.setOpaque(false);

    speedLabel = new JLabel("Velocidade: " + SimulationSpeed.getLabel());
    speedLabel.setForeground(new Color(190, 220, 255));
    speedLabel.setFont(new Font("Monospaced", Font.BOLD, 12));

    JComboBox<String> speedSelector = new JComboBox<>(new String[] { "1x", "2x", "5x", "10x" });
    speedSelector.setSelectedItem(SimulationSpeed.getLabel());
    speedSelector.setFocusable(false);
    speedSelector.addActionListener(e -> {
      Object selected = speedSelector.getSelectedItem();
      if (selected == null) {
        return;
      }

      String label = selected.toString().trim().toLowerCase(Locale.ROOT);
      int multiplier = switch (label) {
        case "2x" -> SimulationSpeed.SPEED_2X;
        case "5x" -> SimulationSpeed.SPEED_5X;
        case "10x" -> SimulationSpeed.SPEED_10X;
        default -> SimulationSpeed.SPEED_1X;
      };

      speedChangeListener.accept(multiplier);
      updateSpeedLabel();
      addLog("Velocidade alterada para " + SimulationSpeed.getLabel() + ".");
    });

    topRight.add(speedLabel);
    topRight.add(speedSelector);
    topRight.add(versionLabel);
    topBar.add(topRight, BorderLayout.EAST);

    frame.add(topBar, BorderLayout.NORTH);

    // ── Tabbed pane ──
    JTabbedPane tabs = new JTabbedPane();
    tabs.setFont(new Font("SansSerif", Font.PLAIN, 13));

    // ---- MAP TAB ----
    mapPanel = new MapPanel(colonyMap);
    JScrollPane mapScroll = new JScrollPane(mapPanel);
    mapScroll.setBorder(BorderFactory.createTitledBorder("Mapa da Colônia (scroll/zoom)"));
    tabs.addTab("Mapa", mapScroll);

    // ---- WORKERS TAB ----
    workerModel = new DefaultTableModel(
        new String[] { "Trabalhador", "Tipo", "Habilidade", "Nível", "Rank", "Status", "Vida", "Energia", "Fome", "Sede" }, 0) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    };
    workerTable = new JTable(workerModel);
    workerTable.setFillsViewportHeight(true);
    workerTable.setRowHeight(22);
    workerTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseMoved(java.awt.event.MouseEvent e) {
        int row = workerTable.rowAtPoint(e.getPoint());
        if (row >= 0) {
          String name = (String) workerModel.getValueAt(row, 0);
          String tip = workerDetails.getOrDefault(name, name);
          workerTable.setToolTipText(tip);
        }
      }
    });
    JScrollPane workerScroll = new JScrollPane(workerTable);
    workerScroll.setBorder(BorderFactory.createTitledBorder("Trabalhadores"));
    tabs.addTab("Trabalhadores", workerScroll);

    // ---- TASKS TAB (SubTabs) ----
    JTabbedPane tasksSubTabs = new JTabbedPane();

    taskActiveModel = new DefaultTableModel(new String[] { "ID Tarefa", "Tipo", "Status" }, 0) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    };
    JTable activeTable = new JTable(taskActiveModel);
    activeTable.setFillsViewportHeight(true);
    tasksSubTabs.addTab("Sendo Feitas", new JScrollPane(activeTable));

    taskDoneModel = new DefaultTableModel(new String[] { "ID Tarefa", "Tipo", "Status" }, 0) {
      @Override
      public boolean isCellEditable(int r, int c) {
        return false;
      }
    };
    JTable doneTable = new JTable(taskDoneModel);
    doneTable.setFillsViewportHeight(true);
    tasksSubTabs.addTab("Concluídas", new JScrollPane(doneTable));

    JPanel tasksPanel = new JPanel(new BorderLayout());
    tasksPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    tasksPanel.add(tasksSubTabs, BorderLayout.CENTER);
    tabs.addTab("Tarefas", tasksPanel);

    // ---- RESOURCES TAB ----
    resourceArea = new JTextArea();
    resourceArea.setEditable(false);
    resourceArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
    resourceArea.setBackground(new Color(245, 240, 230));
    updateResourceDisplay(colonyResources);
    JScrollPane resScroll = new JScrollPane(resourceArea);
    resScroll.setBorder(BorderFactory.createTitledBorder("Recursos"));
    tabs.addTab("Recursos", resScroll);

    // ---- LOG TAB ----
    logArea = new JTextArea();
    logArea.setEditable(false);
    logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
    logArea.setBackground(new Color(15, 15, 20));
    logArea.setForeground(new Color(200, 230, 200));
    JScrollPane logScroll = new JScrollPane(logArea);
    logScroll.setBorder(BorderFactory.createTitledBorder("Registro de Eventos"));
    tabs.addTab("Eventos", logScroll);

    frame.add(tabs, BorderLayout.CENTER);

    // ── Bottom stats bar ──
    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 3));
    bottom.setBackground(new Color(50, 50, 60));
    statsLabel = new JLabel(" Trabalhadores: 0  |  Tarefas: 0  |  Mapa: "
        + ColonyMap.WIDTH + "x" + ColonyMap.HEIGHT + " | v0.2.0");
    statsLabel.setForeground(Color.LIGHT_GRAY);
    statsLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
    bottom.add(statsLabel);
    frame.add(bottom, BorderLayout.SOUTH);

    frame.setVisible(true);
  }

  // ── Public update methods (thread-safe) ──

  public void updateNpcPosition(String npcId, int x, int y) {
    SwingUtilities.invokeLater(() -> mapPanel.setNpcPosition(npcId, x, y));
  }

  public MapPanel getMapPanel() {
    return mapPanel;
  }

  public void updateSpeedLabel() {
    SwingUtilities.invokeLater(() -> speedLabel.setText("Velocidade: " + SimulationSpeed.getLabel()));
  }

  private void updateStats() {
    int w = workerModel.getRowCount();
    statsLabel.setText(" Trabalhadores: " + w + "  |  Tarefas (Total): " + totalTasks + "  |  Mapa: "
        + ColonyMap.WIDTH + "x" + ColonyMap.HEIGHT + " | v0.2.0");
    statusLabel.setText(" Trabalhadores Ativos: " + w + "  |  Tarefas Abertas: " + taskActiveModel.getRowCount());
  }

  public void updateWorker(String name, String skill, String level,
      String rank, String status, String vida, String energia, String fome, String sede) {
    SwingUtilities.invokeLater(() -> {
      String displayName = formatWorkerDisplayName(name);
      String skillPt = traduzirSkill(skill);
      for (int i = 0; i < workerModel.getRowCount(); i++) {
        if (workerModel.getValueAt(i, 0).equals(displayName)) {
          workerModel.setValueAt(skillPt, i, 2);
          workerModel.setValueAt(level, i, 3);
          workerModel.setValueAt(rank, i, 4);
          workerModel.setValueAt(status, i, 5);
          workerModel.setValueAt(vida, i, 6);
          workerModel.setValueAt(energia, i, 7);
          workerModel.setValueAt(fome, i, 8);
          workerModel.setValueAt(sede, i, 9);
          updateStats();
          return;
        }
      }
      String type = traduzirTipo(skill);
      workerModel.addRow(new Object[] { displayName, type, skillPt, level, rank, status, vida, energia, fome, sede });
      updateStats();
    });
  }

  private String formatWorkerDisplayName(String workerName) {
    if (workerName == null || workerName.isBlank()) {
      return "";
    }

    String display = workerName.replace('_', ' ');
    display = display.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    return display.trim().replaceAll("\\s+", " ");
  }

  private String traduzirTipo(String skill) {
    return switch (skill.toLowerCase()) {
      case "construction", "builder" -> "Construtor";
      case "carpenter", "bowyer" -> "Carpinteiro";
      case "mason", "stonecutter" -> "Pedreiro";
      case "stone carver", "stone crafter" -> "Marmorista";
      case "mining", "miner" -> "Minerador";
      case "woodcutting", "wood cutter" -> "Lenhador";
      case "smithing", "blacksmith", "armorsmith", "weaponsmith" -> "Ferreiro";
      case "farming", "planter", "brewer", "cook" -> "Fazendeiro";
      case "diagnostician", "surgeon", "bone doctor" -> "Médico";
      case "fighter", "axeman", "swordsman" -> "Guerreiro";
      case "mechanic", "siege engineer" -> "Engenheiro";
      case "wood crafter", "bone carver", "potter" -> "Artesão";
      default -> "Geral";
    };
  }

  private void removeTaskFromAll(String taskId) {
    for (int i = 0; i < taskActiveModel.getRowCount(); i++) {
      if (taskActiveModel.getValueAt(i, 0).equals(taskId)) {
        taskActiveModel.removeRow(i);
        return;
      }
    }
    for (int i = 0; i < taskDoneModel.getRowCount(); i++) {
      if (taskDoneModel.getValueAt(i, 0).equals(taskId)) {
        taskDoneModel.removeRow(i);
        return;
      }
    }
  }

  public void updateTask(String taskId, String taskType, String status) {
    SwingUtilities.invokeLater(() -> {
      String tipoPt = traduzirTaskType(taskType);
      String statusLow = status.toLowerCase();

      // Remove from current tab
      removeTaskFromAll(taskId);

      // Add to correct tab
      Object[] row = new Object[] { taskId, tipoPt, status };
      if (statusLow.contains("espera") || statusLow.contains("pending") || statusLow.contains("nova")
          || statusLow.contains("reprovada")) {
        taskActiveModel.addRow(row);
      } else if (statusLow.contains("concluída") || statusLow.contains("done") || statusLow.contains("finalizada")
          || statusLow.contains("aprovada") || statusLow.contains("entregue")) {
        taskDoneModel.addRow(row);
      } else {
        taskActiveModel.addRow(row);
      }

      totalTasks = taskActiveModel.getRowCount() + taskDoneModel.getRowCount();
      updateStats();
    });
  }

  private String traduzirSkill(String skill) {
    SkillType skillType = SkillType.fromKey(skill);
    if (skillType != null) {
      return skillType.getDisplayName();
    }

    return switch (skill.toLowerCase()) {
      case "construction" -> "construção";
      case "mining" -> "mineração";
      case "woodcutting" -> "corte de madeira";
      case "smithing" -> "ferraria";
      case "farming" -> "agricultura";
      default -> "geral";
    };
  }

  private String traduzirTaskType(String type) {
    return switch (type.toLowerCase()) {
      case "build", "construct" -> "construção";
      case "mine", "mining" -> "mineração";
      case "woodcut", "woodcutting" -> "corte de madeira";
      case "smith", "smithing", "forge" -> "forjaria";
      case "farm", "farming" -> "agricultura";
      case "carpentry" -> "carpintaria";
      case "masonry" -> "alvenaria";
      default -> type;
    };
  }

  public void updateWorkerDetails(String name, int health, int energy,
      String type, java.util.Map<String, Integer> skills) {
    SwingUtilities.invokeLater(() -> {
      String displayName = formatWorkerDisplayName(name);
      StringBuilder sb = new StringBuilder();
      sb.append("<html><b>").append(displayName).append("</b><hr>");
      String corVida = health > 50 ? "#4a4" : health > 25 ? "#aa4" : "#a44";
      String corEn = energy > 50 ? "#44a" : energy > 25 ? "#aa4" : "#a44";
      sb.append("❤ <span style='color:").append(corVida).append("'>HP ").append(health).append("</span>");
      sb.append(" &nbsp; ⚡ <span style='color:").append(corEn).append("'>EN ").append(energy).append("</span>");
      sb.append(" &nbsp; 🏷 ").append(type).append("<br>");
      sb.append("<table>");
      for (java.util.Map.Entry<String, Integer> e : skills.entrySet()) {
        String rank = SkillType.rankForLevel(e.getValue());
        sb.append("<tr><td>").append(e.getKey()).append("</td>")
            .append("<td> lv").append(e.getValue()).append("</td>")
            .append("<td><i>").append(rank).append("</i></td></tr>");
      }
      sb.append("</table></html>");
      workerDetails.put(displayName, sb.toString());
    });
  }

  public void updateResources(ColonyResources res) {
    SwingUtilities.invokeLater(() -> updateResourceDisplay(res));
  }

  private void updateResourceDisplay(ColonyResources res) {
    StringBuilder sb = new StringBuilder("  RECURSOS DA COLÔNIA\n");
    sb.append("  ────────────────────\n");
    for (Map.Entry<String, Integer> e : res.getAll().entrySet()) {
      if ("ouro".equalsIgnoreCase(e.getKey())) {
        continue;
      }
      String bar = "█".repeat(Math.min(e.getValue() / 2, 20));
      sb.append(String.format("  %-10s %3d  %s\n", e.getKey() + ":", e.getValue(), bar));
    }
    resourceArea.setText(sb.toString());
  }

  public void addLog(String message) {
    SwingUtilities.invokeLater(() -> {
      logArea.append("[" + java.time.LocalTime.now().format(
          java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + message + "\n");
      logArea.setCaretPosition(logArea.getDocument().getLength());
    });
  }

  public void setStatus(String text) {
    SwingUtilities.invokeLater(() -> statusLabel.setText(" " + text));
  }
}
