/*
 * Copyright (c) 2014 - Carl Spain. All rights reserved.
 * Copyright (C) 2014-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.againstTheBot;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;

class WeightedTable<T> {
    private final List<Integer> weights = new ArrayList<>();
    private final List<T> values = new ArrayList<>();

    public void add(Integer weight, T value) {
        weights.add(weight);
        values.add(value);
    }

    public @Nullable T select() {
        return select(0f);
    }

    /**
     * Select random entry proportionally to the weight values
     *
     * @param rollMod - a modifier to the die roll, expressed as a fraction of the total weight
     *
     */
    public @Nullable T select(float rollMod) {
        int total = weights.stream().mapToInt(Integer::intValue).sum();
        if (total > 0) {
            int roll = Math.min(Compute.randomInt(total) + (int) (total * rollMod + 0.5f),
                  total - 1);
            for (int i = 0; i < weights.size(); i++) {
                if (roll < weights.get(i)) {
                    return values.get(i);
                }
                roll -= weights.get(i);
            }
        }
        return null;
    }
}
