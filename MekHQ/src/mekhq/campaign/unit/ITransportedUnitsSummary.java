/**
 * Copyright (c) 2025-2025 The MegaMek Team. All Rights Reserved.
 *
 *  This file is part of MekHQ.
 *
 *  MekHQ is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MekHQ is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.unit;

import megamek.common.Entity;
import megamek.common.Infantry;
import megamek.common.InfantryBay;
import megamek.common.Transporter;
import mekhq.campaign.Campaign;

import java.util.Set;
import java.util.Vector;

public interface ITransportedUnitsSummary {

    /**
     * Gets a value indicating whether or not this unit is
     * transporting units.
     * @return true if the unit has any transported units
     */
    boolean hasTransportedUnits();

    /**
     * @return the set of units being transported by this unit.
     */
    Set<Unit> getTransportedUnits();

    /**
     * Adds a unit to our set of transported units.
     *
     * @param unit The unit being transported by this instance.
     */
    void addTransportedUnit(Unit unit);

    /**
     * Removes a unit from our set of transported units.
     *
     * @param unit The unit to remove from our set of transported units.
     * @return True if the unit was removed, otherwise false.
     */
    boolean removeTransportedUnit(Unit unit);

    /**
     * Clears the set of units being transported by this unit.
     */
    void clearTransportedUnits();

    /**
     * If this unit is capable of transporting another unit, return true
     * @return true if the unit can transport another unit
     */
    boolean hasTransportCapacity();

    /**
     * Gets the different kinds of transporters the transport has
     * @return Set of Transporter classes
     */
    Set<Class<? extends Transporter>> getTransportCapabilities();

    /**
     * Returns true if the unit has capacity left for a transporter type
     * @param transporterType Does the unit have free capacity in this type?
     * @return True if the unit has capacity, false if not
     */
    boolean hasTransportCapacity(Class<? extends Transporter> transporterType);

    /**
     * Returns the current capacity of a transporter type
     * @param transporterType What kind of transporter types are we checking?
     * @return The current capacity of the transporter
     */
    double getCurrentTransportCapacity(Class<? extends Transporter> transporterType);

    /**
     * Sets the current transport capacity for the provided transport type
     * @param transporterType What kind of transporter are we changing the capacity of?
     * @param capacity What is the new capacity?
     */
    void setCurrentTransportCapacity(Class<? extends Transporter> transporterType, double capacity);

    /**
     * Recalculates transport capacity
     * @param transporters What transporters are we tracking the details of?
     */
    void recalculateTransportCapacity(Vector<Transporter> transporters);

    /**
     * When fixing references we need to replace the transported units
     * @param newTransportedUnits The units that should be transported
     */
    void replaceTransportedUnits(Set<Unit> newTransportedUnits);

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit
     * This removes all units assigned to the transport from it
     *
     * @param campaign used to remove this unit as a transport from any other units in the campaign
     */
    void clearTransportedUnits(Campaign campaign);

    /**
     * Main method to be used for loading units onto a transport
     * @param transportedUnits Units we wish to load
     * @return the old transports the transportedUnits were assigned to, or an empty set
     */
    //Set<Unit> loadTransport(Unit... transportedUnits);

    /**
     * Main method to be used for unloading units from a transport
     * @param transportedUnits Units we wish to unload
     */
    void unloadTransport(Unit... transportedUnits);

    /**
     * Fixes references after loading
     */
    void fixReferences(Campaign campaign, Unit unit);

    /**
     * Calculates transport bay space required by an infantry platoon,
     * which is not the same as the flat weight of that platoon
     *
     * @param unit The Entity that we need the weight for
     */
    static double calcInfantryBayWeight(Entity unit) {
        InfantryBay.PlatoonType type = InfantryBay.PlatoonType.getPlatoonType(unit);
        if ((unit instanceof Infantry) && (type == InfantryBay.PlatoonType.MECHANIZED)) {
            return type.getWeight() * ((Infantry) unit).getSquadCount();
        } else {
            return type.getWeight();
        }
    }
}
