package com.colony.agent;

import com.colony.Main;
import com.colony.model.Animal;
import com.colony.model.ColonyMap;
import com.colony.model.SimulationSpeed;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.core.AID;
import java.util.Random;

public class WildlifeAgent extends ColonyAgentBase {
  private static final int MAX_WILD_ANIMALS = 5;
  private static final long WILDLIFE_TICK_INTERVAL_MS = 3000;
  private static final long WILDLIFE_LOOP_BLOCK_MS = 250;
  private final Random random = new Random();
  private ColonyMap colonyMap;
  private long nextWildlifeTickAt = 0L;

  @Override
  protected void setup() {
    registerService("wildlife");
    Object[] args = getArguments();
    if (args != null && args.length > 0 && args[0] instanceof ColonyMap) {
      this.colonyMap = (ColonyMap) args[0];
    } else {
      this.colonyMap = Main.colonyMap;
    }

    System.out.println(getLocalName() + ": Agente de vida selvagem iniciado.");

    nextWildlifeTickAt = System.currentTimeMillis();
    addBehaviour(new CyclicBehaviour() {
      @Override
      public void action() {
        long now = System.currentTimeMillis();
        if (now < nextWildlifeTickAt) {
          block(SimulationSpeed.scaleDelay(WILDLIFE_LOOP_BLOCK_MS));
          return;
        }

        nextWildlifeTickAt = now + SimulationSpeed.scaleDelay(WILDLIFE_TICK_INTERVAL_MS);
        ColonyMap map = colonyMap;

        if (map.getAnimals().size() < MAX_WILD_ANIMALS && random.nextInt(3) == 0) {
          int x = random.nextInt(ColonyMap.WIDTH);
          int y = random.nextInt(ColonyMap.HEIGHT);
          if (!map.getTile(x, y).isBlocksMovement()) {
            boolean aggro = random.nextInt(4) == 0;
            String type = aggro ? "Lobo" : "Cervo";
            map.addAnimal(new Animal(x, y, aggro ? 50 : 20, aggro, type));
          }
        }

        for (Animal a : map.getAnimals()) {
          if (a.dead) {
            a.rotTimer--;
            if (a.rotTimer <= 0) {
              map.removeAnimal(a);
            }
            continue;
          }

          int nx = a.x + random.nextInt(3) - 1;
          int ny = a.y + random.nextInt(3) - 1;
          if (map.inBounds(nx, ny) && !map.getTile(nx, ny).isBlocksMovement()) {
            a.x = nx;
            a.y = ny;
          }
        }

        notifyGuiMapRefresh();
      }
    });
  }

  private void notifyGuiMapRefresh() {
    AID gui = resolveService("gui", "gui");
    if (gui == null) {
      return;
    }

    ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
    msg.addReceiver(gui);
    msg.setContent("BUILD_UPDATE:-1:-1");
    send(msg);
  }
}
