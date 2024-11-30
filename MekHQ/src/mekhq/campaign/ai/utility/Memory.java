package mekhq.campaign.ai.utility;

import java.util.*;
import java.util.function.Function;

public class Memory {

    private final Map<String, Object> memory = new HashMap<>();

    public void remove(String key) {
        memory.remove(key);
    }

    public Object computeIfAbsent(String key, Function<String, Object> mappingFunction) {
        return memory.computeIfAbsent(key, mappingFunction);
    }

    public void clear() {
        memory.clear();
    }

    public void put(String key, Object value) {
        memory.put(key, value);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(memory.getOrDefault(key, null));
    }

    public List<Map<String, Object>> getMemories(String key) {
        return (List<Map<String, Object>>) memory.getOrDefault(key, new ArrayList<Map<String, Object>>());
    }

    public boolean containsKey(String key) {
        return memory.containsKey(key);
    }

    public Optional<String> getString(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof String) {
            return Optional.of((String) memory.get(key));
        }
        return Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Integer) {
            return Optional.of((Integer) memory.get(key));
        }
        return Optional.empty();
    }

    public Optional<Double> getDouble(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Double) {
            return Optional.of((Double) memory.get(key));
        }
        return Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Boolean) {
            return Optional.of((Boolean) memory.get(key));
        }
        return Optional.empty();
    }

}
