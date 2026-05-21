package com.colony;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import com.colony.model.ColonyMap;

public class Main {
    public static final ColonyMap colonyMap = new ColonyMap();
    public static final com.colony.model.ColonyResources resources = new com.colony.model.ColonyResources();

    public static void main(String[] args) {
        // Initialize the JADE runtime and container
        Runtime rt = Runtime.instance();
        rt.setCloseVM(true); // Terminate JVM when no more agents
        
        Profile p = new ProfileImpl(null, 1200, null);
        AgentContainer mainContainer = rt.createMainContainer(p);
        
        try {
            // Create and start the Manager agent first
            AgentController manager = mainContainer.createNewAgent("manager", com.colony.agent.ManagerAgent.class.getName(), null);
            manager.start();
            
            // Create and start the Analyst agent
            AgentController analyst = mainContainer.createNewAgent("analyst", com.colony.agent.AnalystAgent.class.getName(), null);
            analyst.start();
            
            // Create and start the GUI agent
            AgentController gui = mainContainer.createNewAgent("gui", com.colony.gui.GuiAgent.class.getName(), null);
            gui.start();
            
            // ─── Cria os NPCs (cada um = 1 anão) ───
            AgentController[] workers = {
                mainContainer.createNewAgent("Urist", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"builder"}),
                mainContainer.createNewAgent("Doren", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"miner"}),
                mainContainer.createNewAgent("Logem", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"woodcutter"}),
                mainContainer.createNewAgent("Kikrost", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"carpenter"}),
                mainContainer.createNewAgent("Stinthad", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"smith"}),
                mainContainer.createNewAgent("Meng", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"mason"}),
                mainContainer.createNewAgent("Zasit", com.colony.agent.WorkerAgent.class.getName(), new Object[]{"marbleworker"}),
            };
            for (AgentController w : workers) w.start();
            
            System.out.println("Todos os agentes iniciados com sucesso!");
            
            // Thread de Animais Selvagens
            new Thread(() -> {
                java.util.Random rand = new java.util.Random();
                while (true) {
                    try { Thread.sleep(3000); } catch (Exception e) {}
                    // Spawna animais (até max 15 no mapa)
                    if (colonyMap.getAnimals().size() < 15 && rand.nextInt(3) == 0) {
                        int x = rand.nextInt(ColonyMap.WIDTH);
                        int y = rand.nextInt(ColonyMap.HEIGHT);
                        if (!colonyMap.getTile(x, y).isBlocksMovement()) {
                            boolean aggro = rand.nextInt(4) == 0;
                            String type = aggro ? "Lobo" : "Cervo";
                            colonyMap.addAnimal(new com.colony.model.Animal(x, y, aggro ? 50 : 20, aggro, type));
                        }
                    }
                    // Move animais
                    for (com.colony.model.Animal a : colonyMap.getAnimals()) {
                        if (a.dead) {
                            a.rotTimer--;
                            if (a.rotTimer <= 0) {
                                colonyMap.removeAnimal(a);
                            }
                            continue;
                        }
                        int nx = a.x + rand.nextInt(3) - 1;
                        int ny = a.y + rand.nextInt(3) - 1;
                        if (colonyMap.inBounds(nx, ny) && !colonyMap.getTile(nx, ny).isBlocksMovement()) {
                            a.x = nx;
                            a.y = ny;
                        }
                    }
                }
            }).start();
            
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}
