/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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

import megamek.codeUtilities.MathUtility;
import mekhq.campaign.personnel.enums.ModifierValue;

/**
 * A modifier is some kind of (usually temporary) effect that influences the character's base values.
 * <p>
 * Modifiers have three values: Which value they apply to, how much they change the value by (can be positive or
 * negative) and optionally what type of modifier they are. If a person has multiple modifiers of the same type, only
 * the highest positive one and the lowest negative one applies. All modifiers without a type apply fully.
 * <p>
 * In addition, modifiers can have a set of string tags, used for filtering and searching in them.
 */
public class Modifier {
    public final ModifierValue value;
    public final int mod;
    public final String type;
    public final Set<String> tags;

    public static int calcTotalModifier(Stream<Modifier> mods, ModifierValue val) {
        final Map<String, Integer> posMods = new HashMap<>();
        final Map<String, Integer> negMods = new HashMap<>();
        final Collection<Integer> untypedMods = new ArrayList<>();
        long result = 0;
        mods.filter(mod -> (mod.value == val)).forEach(mod -> {
            if (null != mod.type) {
                int posMod = Math.max(0, mod.mod);
                int negMod = Math.min(0, mod.mod);
                if (posMods.containsKey(mod.type)) {
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

        for (String type : posMods.keySet()) {
            result += posMods.get(type);
            result += negMods.get(type);
        }

        result += untypedMods.stream()
                        .mapToLong(mod -> mod)
                        .sum();

        return (int) MathUtility.clamp(result, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public Modifier(ModifierValue value, int mod) {
        this(value, mod, null);
    }

    public Modifier(ModifierValue value, int mod, String type, String... tags) {
        this.value = Objects.requireNonNull(value);
        this.mod = mod;
        this.type = type;
        this.tags = (null != tags) ? Arrays.stream(tags).collect(Collectors.toSet()) : new HashSet<>();
    }
}
