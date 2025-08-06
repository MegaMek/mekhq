/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import megamek.common.annotations.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;

/**
 * Represents a hangar which contains zero or more units.
 */
public class Hangar {
    private Map<UUID, Unit> units = new LinkedHashMap<>();

    /**
     * Adds a unit to the hangar.
     * <p>
     * If the unit does not have an ID, one is assigned to it.
     *
     * @param unit The unit to add to the hangar.
     */
    public void addUnit(final @Nullable Unit unit) {
        if (unit == null) {
            return;
        }

        if (unit.getId() == null) {
            unit.setId(UUID.randomUUID());
        }

        units.put(unit.getId(), unit);
    }

    /**
     * Gets a unit by a given ID.
     *
     * @param id The unique identifier of a unit.
     *
     * @return The unit matching the unique identifier, otherwise null if that unit does not exist.
     */
    public @Nullable Unit getUnit(UUID id) {
        return units.get(id);
    }

    /**
     * Gets a collection of units in the hangar.
     *
     * @return A collection of units in the hangar.
     */
    public Collection<Unit> getUnits() {
        return units.values();
    }

    /**
     * Gets a Stream of units in the hangar.
     *
     * @return A Stream of units in the hangar.
     */
    public Stream<Unit> getUnitsStream() {
        return units.values().stream();
    }

    /**
     * Calculates the total costs for the units in the hangar.
     *
     * @param getCosts A function which returns a cost for a unit.
     *
     * @return The total costs for the units.
     */
    public Money getUnitCosts(Function<Unit, Money> getCosts) {
        return getUnitsStream().map(getCosts).reduce(Money.zero(), Money::plus);
    }

    /**
     * Calculates the total costs for the units matching a predicate in the hangar.
     *
     * @param predicate A function to use to select a unit.
     * @param getCosts  A function which returns a cost for a selected unit.
     *
     * @return The total costs for the units selected by the predicate.
     */
    public Money getUnitCosts(Predicate<Unit> predicate, Function<Unit, Money> getCosts) {
        return getUnitsStream().filter(predicate).map(getCosts).reduce(Money.zero(), Money::plus);
    }

    /**
     * Executes a function for each unit in the hangar.
     *
     * @param consumer A function to apply to each unit.
     */
    public void forEachUnit(Consumer<Unit> consumer) {
        units.forEach((id, unit) -> consumer.accept(unit));
    }

    /**
     * Executes a function for each unit in the hangar.
     *
     * @param consumer A function to apply to each ID-unit pair.
     */
    public void forEachUnit(BiConsumer<UUID, Unit> consumer) {
        units.forEach(consumer);
    }

    /**
     * Searches for a specific unit using the given predicate.
     *
     * @param predicate A function to use to select a given unit.
     *
     * @return The first unit found which matches the predicate, otherwise null if no unit was found.
     */
    public @Nullable Unit findUnit(final @Nullable Predicate<Unit> predicate) {
        if (predicate == null) {
            return null;
        }

        for (Unit unit : units.values()) {
            if (predicate.test(unit)) {
                return unit;
            }
        }
        return null;
    }

    /**
     * Removes a unit from the hangar.
     *
     * @param id The unit ID.
     *
     * @return true if the unit was removed, otherwise false.
     */
    public boolean removeUnit(UUID id) {
        return null != units.remove(id);
    }

    public void writeToXML(final PrintWriter pw, final int indent, final String tag) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent, tag);
        forEachUnit(unit -> unit.writeToXML(pw, indent + 1));
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, indent, tag);
    }
}
