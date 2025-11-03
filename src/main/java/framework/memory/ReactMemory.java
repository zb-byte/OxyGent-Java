package framework.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * ReAct内存管理（框架核心）
 * 用于记录ReAct循环中的思考和观察
 */
public class ReactMemory {
    
    private List<Round> rounds = new ArrayList<>();
    
    public void addRound(String thought, String observation) {
        rounds.add(new Round(thought, observation));
    }
    
    public List<Round> getRounds() {
        return rounds;
    }
    
    public List<Map<String, String>> toHistory() {
        List<Map<String, String>> history = new ArrayList<>();
        for (Round round : rounds) {
            Map<String, String> entry = new HashMap<>();
            entry.put("thought", round.getThought());
            entry.put("observation", round.getObservation());
            history.add(entry);
        }
        return history;
    }
    
    public static class Round {
        private String thought;
        private String observation;
        
        public Round(String thought, String observation) {
            this.thought = thought;
            this.observation = observation;
        }
        
        public String getThought() {
            return thought;
        }
        
        public String getObservation() {
            return observation;
        }
    }
}

