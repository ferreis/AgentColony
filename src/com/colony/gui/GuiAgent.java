package com.colony.gui;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import com.colony.agent.ColonyAgentBase;
import com.colony.Main;
import com.colony.model.ColonyMap;
import com.colony.model.ColonyResources;
import com.colony.model.SimulationSpeed;
import javax.swing.*;

public class GuiAgent extends ColonyAgentBase {
  private ColonyGUI gui;
  private ColonyMap colonyMap;
  private ColonyResources resources;

  protected void setup() {
    registerService("gui");
    Object[] args = getArguments();
    if (args != null && args.length >= 2
        && args[0] instanceof ColonyMap
        && args[1] instanceof ColonyResources) {
      this.colonyMap = (ColonyMap) args[0];
      this.resources = (ColonyResources) args[1];
    } else {
      this.colonyMap = Main.colonyMap;
      this.resources = Main.resources;
    }

    System.out.println(getLocalName() + ": Agente GUI iniciando...");

    SwingUtilities.invokeLater(() -> {
      gui = new ColonyGUI(colonyMap, resources, multiplier -> {
        SimulationSpeed.setMultiplier(multiplier);
      });
      gui.updateSpeedLabel();
      gui.addLog("GUI inicializada. Aguardando agentes...");
    });

    addBehaviour(new CyclicBehaviour() {
      public void action() {
        ACLMessage msg = receive();
        if (msg != null && gui != null) {
          String content = msg.getContent();

          if (content.startsWith("WORKER_STATUS:")) {
            String[] p = content.substring("WORKER_STATUS:".length()).split(":", 10);
            if (p.length >= 5) {
              String vida = p.length >= 6 ? p[5] : "100";
              String energia = p.length >= 7 ? p[6] : "100";
              String fome = p.length >= 8 ? p[7] : "100";
              String sede = p.length >= 9 ? p[8] : "100";
              gui.updateWorker(p[0], p[1], p[2], p[3], p[4], vida, energia, fome, sede);
            }
          } else if (content.startsWith("WORKER_DETAILS:")) {
            String data = content.substring("WORKER_DETAILS:".length());
            String[] parts = data.split("\\|");
            if (parts.length >= 4) {
              String name = parts[0];
              int health = parseInt(parts[1], 100);
              int energy = parseInt(parts[2], 100);
              String type = parts[3];
              java.util.Map<String, Integer> skills = new java.util.LinkedHashMap<>();
              for (int i = 4; i < parts.length; i++) {
                String[] sk = parts[i].split("~", 2);
                if (sk.length == 2) {
                  try {
                    skills.put(sk[0], Integer.parseInt(sk[1]));
                  } catch (Exception ex) {
                  }
                }
              }
              gui.updateWorkerDetails(name, health, energy, type, skills);
            }
          } else if (content.startsWith("NPC_POSITION:")) {
            String[] p = content.substring("NPC_POSITION:".length()).split(":");
            if (p.length >= 3) {
              int nx = parseInt(p[1], 0);
              int ny = parseInt(p[2], 0);
              gui.updateNpcPosition(p[0], nx, ny);
            }
          } else if (content.startsWith("BUILD_UPDATE:")) {
            gui.getMapPanel().repaint();
          } else if (content.startsWith("TASK_STATUS:")) {
            String rest = content.substring("TASK_STATUS:".length());
            int firstColon = rest.indexOf(':');
            if (firstColon > 0) {
              String taskId = rest.substring(0, firstColon);
              String rest2 = rest.substring(firstColon + 1);
              int secondColon = rest2.indexOf(':');
              if (secondColon > 0) {
                String taskType = rest2.substring(0, secondColon);
                String status = rest2.substring(secondColon + 1);
                gui.updateTask(taskId, taskType, status);
              }
            }
          } else if (content.startsWith("LOG:")) {
            gui.addLog(content.substring(4));
          } else if (content.startsWith("WORKER_ANALYSIS:")) {
            gui.addLog("[Analista] " + content.substring("WORKER_ANALYSIS:".length()));
          } else if (content.startsWith("UPDATE_RESOURCES")) {
            gui.updateResources(resources);
          } else if (content.startsWith("TERRAIN_ANALYSIS:")) {
            String terrain = content.substring("TERRAIN_ANALYSIS:".length());
            gui.addLog("[Terreno] " + terrain);
            if (!terrain.startsWith("Nenhum")) {
              decodeTerrainAndUpdate(terrain);
            }
          }
        } else {
          block();
        }
      }
    });
  }

  private void decodeTerrainAndUpdate(String terrain) {
    // Terrain analysis is informational; resources change only via in-game actions.
    gui.updateResources(resources);
  }

  private int parseInt(String s, int defaultVal) {
    try {
      return Integer.parseInt(s);
    } catch (Exception e) {
      return defaultVal;
    }
  }
}
