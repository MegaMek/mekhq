/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios.salvage;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;

/**
 * How many units the player already owns of each exact variant and of each chassis, so the salvage screen can show,
 * for example, that the player already has two Wolverine WVR-6R and five Wolverines of any model.
 *
 * @since 0.51.01
 */
public final class OwnedUnitCounts {
    private final Map<String, Integer> variantCounts = new HashMap<>();
    private final Map<String, Integer> chassisCounts = new HashMap<>();

    private OwnedUnitCounts() {}

    /**
     * Counts the given units by exact variant (chassis and model) and by chassis. Units without an entity are skipped.
     *
     * @param ownedUnits the units the player owns, usually everything in the hangar
     *
     * @return the counts for those units
     */
    public static OwnedUnitCounts of(Collection<Unit> ownedUnits) {
        OwnedUnitCounts counts = new OwnedUnitCounts();
        for (Unit ownedUnit : ownedUnits) {
            Entity ownedEntity = ownedUnit.getEntity();
            if (ownedEntity != null) {
                counts.variantCounts.merge(variantKey(ownedEntity), 1, Integer::sum);
                counts.chassisCounts.merge(ownedEntity.getChassis(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /**
     * @param entity the unit to look up, or {@code null}
     *
     * @return how many owned units share this unit's chassis and model, or {@code 0} for a {@code null} entity
     */
    public int getVariantCount(@Nullable Entity entity) {
        if (entity == null) {
            return 0;
        }
        return variantCounts.getOrDefault(variantKey(entity), 0);
    }

    /**
     * @param entity the unit to look up, or {@code null}
     *
     * @return how many owned units share this unit's chassis, whatever their model, or {@code 0} for a {@code null}
     *       entity
     */
    public int getChassisCount(@Nullable Entity entity) {
        if (entity == null) {
            return 0;
        }
        return chassisCounts.getOrDefault(entity.getChassis(), 0);
    }

    private static String variantKey(Entity entity) {
        return entity.getChassis() + ' ' + entity.getModel();
    }
}
