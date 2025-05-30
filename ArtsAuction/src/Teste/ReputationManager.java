package Teste;

import jade.core.AID;
import java.util.HashMap;
import java.util.Map;

public class ReputationManager {
    private static final Map<AID, Double> reputationMap = new HashMap<>();
    private static final double MIN_REPUTATION = 0.3;
    private static final double INITIAL_REPUTATION = 0.5;
    
    public static double getReputation(AID agent) {
        return reputationMap.getOrDefault(agent, INITIAL_REPUTATION);
    }
    
    public static void updateReputation(AID agent, double newReputation) {
        newReputation = Math.max(0, Math.min(1, newReputation));
        reputationMap.put(agent, newReputation);
    }
    
    public static double getMinReputationThreshold() {
        return MIN_REPUTATION;
    }
}
