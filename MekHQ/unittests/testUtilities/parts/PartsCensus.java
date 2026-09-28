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
package testUtilities.parts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

/**
 * Counts parts by name so a test can compare the whole contents of a unit or a warehouse in one assertion, and so a
 * failure message shows exactly which part appeared, vanished or changed in number.
 *
 * <p>Each count adds up what the part holds: its quantity for ordinary parts, its shots for {@link AmmoStorage} and
 * its armor points for {@link Armor}. The name of a warehouse part is followed by {@code [reserved]} when it is set
 * aside for a refit and by {@code [in transit]} when it has not arrived yet, because those states decide whether the
 * part can still be used.</p>
 *
 * <pre>{@code
 * SortedMap<String, Integer> sparesBefore = PartsCensus.ofWarehouseStock(scenario.getWarehouse());
 * // ... run the refit ...
 * assertEquals(Map.of("Machine Gun", 2), PartsCensus.ofWarehouseStock(scenario.getWarehouse()));
 * }</pre>
 */
public final class PartsCensus {
    /** Appended to the name of a warehouse part that is set aside for a refit. */
    public static final String RESERVED_TAG = " [reserved]";
    /** Appended to the name of a warehouse part that has been bought but has not arrived. */
    public static final String IN_TRANSIT_TAG = " [in transit]";

    private PartsCensus() {}

    /**
     * Counts the parts installed on a unit, keyed by part name and location, for example
     * {@code "Medium Laser @ Right Arm"}.
     *
     * @param unit the unit to count
     *
     * @return the count for each name and location, sorted by key
     */
    public static SortedMap<String, Integer> ofUnit(Unit unit) {
        SortedMap<String, Integer> census = new TreeMap<>();
        for (Part part : new ArrayList<>(unit.getParts())) {
            String locationName = part.getLocationName();
            String key = ((locationName == null) || locationName.isBlank())
                               ? part.getName()
                               : part.getName() + " @ " + locationName;
            census.merge(key, amountOf(part), Integer::sum);
        }
        return census;
    }

    /**
     * Counts the parts in a warehouse that are not installed on any unit: spares, parts reserved for a refit, and
     * parts still in transit.
     *
     * @param warehouse the warehouse to count
     *
     * @return the count for each name, with the reserved and in transit tags, sorted by key
     */
    public static SortedMap<String, Integer> ofWarehouseStock(LocalWarehouse warehouse) {
        SortedMap<String, Integer> census = new TreeMap<>();
        for (Part part : new ArrayList<>(warehouse.getParts())) {
            boolean isInstalled = (part.getUnit() != null) || (part.getParentPart() != null);
            if (isInstalled) {
                continue;
            }
            census.merge(stockKey(part), amountOf(part), Integer::sum);
        }
        return census;
    }

    /**
     * Counts a loose list of parts, such as a refit's shopping list, keyed by part name only. Each entry adds its
     * quantity, with ammunition and armor counted by shots and points as above.
     *
     * @param parts the parts to count
     *
     * @return the count for each name, sorted by key
     */
    public static SortedMap<String, Integer> ofParts(Collection<? extends Part> parts) {
        SortedMap<String, Integer> census = new TreeMap<>();
        for (Part part : parts) {
            census.merge(part.getName(), amountOf(part), Integer::sum);
        }
        return census;
    }

    private static String stockKey(Part part) {
        String key = part.getName();
        if (part.isReservedForRefit()) {
            key += RESERVED_TAG;
        }
        if (!part.isPresent()) {
            key += IN_TRANSIT_TAG;
        }
        return key;
    }

    private static int amountOf(Part part) {
        if (part instanceof AmmoStorage ammoStorage) {
            return ammoStorage.getShots();
        }
        if (part instanceof Armor armor) {
            return armor.getAmount();
        }
        return part.getQuantity();
    }
}
