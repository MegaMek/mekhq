/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A modifier is some kind of (usually temporary) effect that influences the character's base values.
 * <p>
 * Modifiers have three values: Which value they apply to, how much they change the value by (can
 * be positive or negative) and optionally what type of modifier they are. If a person has
 * multiple modifiers of the same type, only the highest positive one and the lowest negative
 * one applies. All modifiers without a type apply fully.
 * <p>
 * In addition, modifiers can have a set of string tags, used for filtering and searching in them.
 */
public class Modifier {
    public final Value value;
    public final int mod;
    public final String type;
    public final Set<String> tags;
    
    public static int calcTotalModifier(Collection<Modifier> mods, Value val) {
        return calcTotalModifier(mods.stream(), val);
    }
    
    public static int calcTotalModifier(Stream<Modifier> mods, Value val) {
        final Map<String, Integer> posMods = new HashMap<>();
        final Map<String, Integer> negMods = new HashMap<>();
        final Collection<Integer> untypedMods = new ArrayList<>();
        long result = 0;
        mods.filter(mod -> (mod.value == val)).forEach(mod -> {
            if(null != mod.type) {
                int posMod = Math.max(0, mod.mod);
                int negMod = Math.min(0, mod.mod);
                if(posMods.containsKey(mod.type)) {
                    posMods.put(mod.type, Math.max(posMod, posMods.get(mod.type)));
                    negMods.put(mod.type, Math.min(negMod, posMods.get(mod.type)));
                } else {
                    posMods.put(mod.type, posMod);
                    negMods.put(mod.type, negMod);
                }
            } else {
                untypedMods.add(mod.mod);
            }
        });
        for(String type : posMods.keySet()) {
            result += posMods.get(type);
            result += negMods.get(type);
        }
        for(Integer mod : untypedMods) {
            result += mod.intValue();
        }
        if(result > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if(result < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) result;
    }
    
    public Modifier(Value value, int mod) {
        this(value, mod, null);
    }
    
    public Modifier(Value value, int mod, String type, String ... tags) {
        this.value = Objects.requireNonNull(value);
        this.mod = mod;
        this.type = type;
        this.tags = (null != tags) ? Arrays.stream(tags).collect(Collectors.toSet()) : new HashSet<>();
    }
    
    public static enum Value {
        PILOTING, GUNNERY
    }
}
