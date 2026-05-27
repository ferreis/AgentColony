package com.colony;

import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import com.colony.model.ColonyMap;
import com.colony.model.SimulationSpeed;

public class Main {
	public static final ColonyMap colonyMap = new ColonyMap();
	public static final com.colony.model.ColonyResources resources = new com.colony.model.ColonyResources();

	public static void main(String[] args) {
		SimulationSpeed.initializeFromSystemProperty();
		System.out.println("Velocidade da simulação: " + SimulationSpeed.getLabel());

		// Initialize the JADE runtime and container
		Runtime rt = Runtime.instance();
		rt.setCloseVM(true); // Terminate JVM when no more agents

		Profile p = new ProfileImpl(null, 1200, null);
		AgentContainer mainContainer = rt.createMainContainer(p);

		try {
			// Create and start the Manager agent first
			AgentController manager = mainContainer.createNewAgent(
					"manager",
					com.colony.agent.ManagerAgent.class.getName(),
					new Object[] { colonyMap, resources });
			manager.start();

			// Create and start the Analyst agent
			AgentController analyst = mainContainer.createNewAgent(
					"analyst",
					com.colony.agent.AnalystAgent.class.getName(),
					new Object[] { colonyMap, resources });
			analyst.start();

			// Create and start the GUI agent
			AgentController gui = mainContainer.createNewAgent(
					"gui",
					com.colony.gui.GuiAgent.class.getName(),
					new Object[] { colonyMap, resources });
			gui.start();

			// Create and start the wildlife simulation as a JADE agent
			AgentController wildlife = mainContainer.createNewAgent(
					"wildlife",
					com.colony.agent.WildlifeAgent.class.getName(),
					new Object[] { colonyMap });
			wildlife.start();

			// ─── Cria os NPCs (cada um = 1 anão) ───
			AgentController[] workers = {
					mainContainer.createNewAgent("Urist", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "builder", colonyMap, resources }),
					mainContainer.createNewAgent("Doren", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "miner", colonyMap, resources }),
					mainContainer.createNewAgent("Logem", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "woodcutter", colonyMap, resources }),
					mainContainer.createNewAgent("Kikrost", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "carpenter", colonyMap, resources }),
					mainContainer.createNewAgent("Stinthad", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "smith", colonyMap, resources }),
					mainContainer.createNewAgent("Meng", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "mason", colonyMap, resources }),
					mainContainer.createNewAgent("Zasit", com.colony.agent.WorkerAgent.class.getName(),
							new Object[] { "marbleworker", colonyMap, resources }),
			};
			for (AgentController w : workers)
				w.start();

			System.out.println("Todos os agentes JADE iniciados com sucesso!");

		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
}
