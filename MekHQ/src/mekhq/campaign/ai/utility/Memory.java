/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
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

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMemories(String key) {
        return (List<Map<String, Object>>) memory.computeIfAbsent(key, k -> new ArrayList<Map<String, Object>>());
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
