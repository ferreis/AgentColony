package com.colony;

import com.colony.model.*;

public class DwarfConsole {
  public static void main(String[] args) {
    DwarfMind mind = new DwarfMind();
    System.out.println("==============================================");
    System.out.println("  MOTOR DE DECISÃO DO ANÃO (Dwarf Mind v1.0)");
    System.out.println("==============================================\n");

    test(mind, "TESTE 1: Gerente ordena expandir mina (Miner level 1, energia 35)",
        "{\"npc_id\":\"D4\",\"stats\":{\"health\":70,\"energy\":35,\"mood\":\"neutral\"},"
            + "\"skills\":[{\"name\":\"Miner\",\"level\":1,\"rank\":\"Dabbling\"}],"
            + "\"current_task\":null,"
            + "\"manager_task\":{\"name\":\"expand_mine\",\"priority\":3,\"required_skill\":\"Miner\"},"
            + "\"inventory\":[{\"item\":\"pick\",\"quantity\":1}],"
            + "\"location\":{\"x\":10,\"y\":5,\"z\":0,\"zone\":\"mining_tunnels\"},"
            + "\"urgent_needs\":[\"none\"],"
            + "\"fortress_priorities\":[\"expand_mine\"]}");

    test(mind, "TESTE 2: Diagnostician nível 6, ferido, energia=15",
        "{\"npc_id\":\"D7\",\"stats\":{\"health\":45,\"energy\":15,\"mood\":\"stressed\"},"
            + "\"skills\":[{\"name\":\"Diagnostician\",\"level\":6,\"rank\":\"Talented\"}],"
            + "\"current_task\":\"treat_patient\","
            + "\"manager_task\":{\"name\":\"manage_hospital\",\"priority\":5,\"required_skill\":\"Diagnostician\"},"
            + "\"inventory\":[],"
            + "\"location\":{\"x\":20,\"y\":12,\"z\":1,\"zone\":\"hospital\"},"
            + "\"urgent_needs\":[\"injury\"],"
            + "\"fortress_priorities\":[\"stabilize_wounded\"]}");

    test(mind, "TESTE 3: HP=5 morte iminente → treat_injury, ignora gerente",
        "{\"npc_id\":\"D3\",\"stats\":{\"health\":5,\"energy\":40,\"mood\":\"stressed\"},"
            + "\"skills\":[{\"name\":\"Miner\",\"level\":4,\"rank\":\"Competent\"}],"
            + "\"current_task\":null,"
            + "\"manager_task\":{\"name\":\"expand_mine\",\"priority\":5,\"required_skill\":\"Miner\"},"
            + "\"inventory\":[{\"item\":\"pick\",\"quantity\":1}],"
            + "\"location\":{\"x\":10,\"y\":5,\"z\":0,\"zone\":\"mining_tunnels\"},"
            + "\"urgent_needs\":[\"injury\"],"
            + "\"fortress_priorities\":[\"expand_mine\"]}");

    test(mind, "TESTE 4: Energia=10, sem gerente → rest obrigatório",
        "{\"npc_id\":\"D5\",\"stats\":{\"health\":80,\"energy\":10,\"mood\":\"stressed\"},"
            + "\"skills\":[{\"name\":\"Woodcutter\",\"level\":3,\"rank\":\"Adequate\"}],"
            + "\"current_task\":null,"
            + "\"manager_task\":null,"
            + "\"inventory\":[],"
            + "\"location\":{\"x\":30,\"y\":20,\"z\":0,\"zone\":\"forest\"},"
            + "\"urgent_needs\":[\"fatigue\"],"
            + "\"fortress_priorities\":[\"gather_wood\"]}");

    test(mind, "TESTE 5: Ameaça, Fighter level 5 → defend",
        "{\"npc_id\":\"D9\",\"stats\":{\"health\":90,\"energy\":80,\"mood\":\"happy\"},"
            + "\"skills\":[{\"name\":\"Fighter\",\"level\":5,\"rank\":\"Skilled\"},"
            + "{\"name\":\"Axedwarf\",\"level\":3,\"rank\":\"Adequate\"}],"
            + "\"current_task\":null,"
            + "\"manager_task\":null,"
            + "\"inventory\":[{\"item\":\"axe\",\"quantity\":1}],"
            + "\"location\":{\"x\":15,\"y\":8,\"z\":0,\"zone\":\"entrance\"},"
            + "\"urgent_needs\":[\"threat\"],"
            + "\"fortress_priorities\":[\"defend_fortress\"]}");

    test(mind, "TESTE 6: Já minerando, sem gerente → continua",
        "{\"npc_id\":\"D2\",\"stats\":{\"health\":95,\"energy\":60,\"mood\":\"neutral\"},"
            + "\"skills\":[{\"name\":\"Miner\",\"level\":7,\"rank\":\"Expert\"}],"
            + "\"current_task\":\"mining_south_shaft\","
            + "\"manager_task\":null,"
            + "\"inventory\":[{\"item\":\"pick\",\"quantity\":1}],"
            + "\"location\":{\"x\":5,\"y\":15,\"z\":-1,\"zone\":\"deep_tunnels\"},"
            + "\"urgent_needs\":[\"none\"],"
            + "\"fortress_priorities\":[\"explore_deep\"]}");

    test(mind, "TESTE 7: Fome, energia=28, sem gerente → eat rápido",
        "{\"npc_id\":\"D6\",\"stats\":{\"health\":70,\"energy\":28,\"mood\":\"stressed\"},"
            + "\"skills\":[{\"name\":\"Farmer\",\"level\":2,\"rank\":\"Novice\"}],"
            + "\"current_task\":null,"
            + "\"manager_task\":null,"
            + "\"inventory\":[],"
            + "\"location\":{\"x\":25,\"y\":30,\"z\":0,\"zone\":\"farm_plots\"},"
            + "\"urgent_needs\":[\"hunger\"],"
            + "\"fortress_priorities\":[]}");

    test(mind, "TESTE 8: Sem ordens, energia=85 → learn melhor skill",
        "{\"npc_id\":\"D1\",\"stats\":{\"health\":100,\"energy\":85,\"mood\":\"happy\"},"
            + "\"skills\":[{\"name\":\"Craftsdwarf\",\"level\":2,\"rank\":\"Novice\"},"
            + "{\"name\":\"Miner\",\"level\":4,\"rank\":\"Competent\"}],"
            + "\"current_task\":null,"
            + "\"manager_task\":null,"
            + "\"inventory\":[],"
            + "\"location\":{\"x\":40,\"y\":40,\"z\":0,\"zone\":\"meeting_hall\"},"
            + "\"urgent_needs\":[\"none\"],"
            + "\"fortress_priorities\":[]}");

    System.out.println("==============================================");
    System.out.println("  TODOS OS TESTES CONCLUÍDOS");
    System.out.println("==============================================");
  }

  static void test(DwarfMind mind, String label, String jsonInput) {
    System.out.println("─── " + label + " ───");
    DwarfState state = DwarfState.fromJson(jsonInput);
    System.out.println("  INPUT PARSED OK: npc=" + state.getNpcId()
        + " health=" + (state.getStats() != null ? state.getStats().health : "?")
        + " energy=" + (state.getStats() != null ? state.getStats().energy : "?")
        + " mgr=" + (state.getManagerTask() != null ? state.getManagerTask().name : "none"));
    DwarfAction action = mind.decide(state);
    System.out.println("  OUTPUT: " + action.toJson());
    System.out.println();
  }
}
