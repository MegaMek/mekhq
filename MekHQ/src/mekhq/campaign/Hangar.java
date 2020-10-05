/*
 * Hangar.java
 *
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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

import mekhq.MekHqXmlUtil;
import mekhq.campaign.finances.Money;
import mekhq.campaign.unit.Unit;

/**
 * Represents a hangar which contains zero or more units.
 */
public class Hangar {
    private Map<UUID, Unit> units = new LinkedHashMap<>();

    /**
     * Adds a unit to the hangar.
     *
     * If the unit does not have an ID, one is
     * assigned to it.
     *
     * @param unit The unit to add to the hangar.
     */
    public void addUnit(Unit unit) {
        if (unit == null) {
            return;
        }

        UUID id = unit.getId();
        if (id == null) {
            id = UUID.randomUUID();
            unit.setId(id);
        }

        units.put(id, unit);
    }

    /**
     * Gets a unit by a given ID.
     * @param id The unique identifier of a unit.
     * @return The unit matching the unique identifier,
     *         otherwise null if that unit does not exist.
     */
    public Unit getUnit(UUID id) {
        return units.get(id);
    }

    /**
     * Gets a collection of units in the hangar.
     * @return A collection of units in the hangar.
     */
    public Collection<Unit> getUnits() {
        return units.values();
    }

    /**
     * Gets a Stream of units in the hangar.
     * @return A Stream of units in the hangar.
     */
    public Stream<Unit> getUnitsStream() {
        return units.values().stream();
    }

    /**
     * Calculates the total costs for the units in the hangar.
     * @param getCosts A function which returns a cost for a unit.
     * @return The total costs for the units.
     */
    public Money getUnitCosts(Function<Unit, Money> getCosts) {
        return getUnitsStream().map(getCosts).reduce(Money.zero(), Money::plus);
    }

    /**
     * Calculates the total costs for the units matching a predicate
     * in the hangar.
     * @param predicate A function to use to select a unit.
     * @param getCosts A function which returns a cost for a selected unit.
     * @return The total costs for the units selected by the predicate.
     */
    public Money getUnitCosts(Predicate<Unit> predicate, Function<Unit, Money> getCosts) {
        return getUnitsStream().filter(predicate).map(getCosts).reduce(Money.zero(), Money::plus);
    }

    /**
     * Executes a function for each unit in the hangar.
     * @param consumer A function to apply to each unit.
     */
    public void forEachUnit(Consumer<Unit> consumer) {
        units.forEach((id, unit) -> consumer.accept(unit));
    }

    /**
     * Executes a function for each unit in the hangar.
     * @param consumer A function to apply to each ID-unit pair.
     */
    public void forEachUnit(BiConsumer<UUID, Unit> consumer) {
        units.forEach(consumer);
    }

    /**
     * Searches for a specific unit using the given predicate.
     * @param predicate A function to use to select a given unit.
     * @return The first unit found which matches the predicate,
     *         otherwise null if no unit was found.
     */
    public Unit findUnit(Predicate<Unit> predicate) {
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
     * @param id The unit ID.
     * @return true if the unit was removed, otherwise false.
     */
    public boolean removeUnit(UUID id) {
        return null != units.remove(id);
    }

	public void writeToXml(PrintWriter pw1, int indent, String tag) {
        pw1.println(MekHqXmlUtil.indentStr(indent) + "<" + tag + ">");

        forEachUnit(unit -> {
            unit.writeToXml(pw1, indent + 1);
        });

        pw1.println(MekHqXmlUtil.indentStr(indent) + "</" + tag + ">");
	}
}
