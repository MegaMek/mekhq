/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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

package mekhq.campaign.unit;

import java.util.Set;
import java.util.Vector;

import megamek.common.Transporter;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.enums.TransporterType;

public interface ITransportedUnitsSummary {

    /**
     * Gets a value indicating whether or not this unit is transporting units.
     *
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
     *
     * @return True if the unit was removed, otherwise false.
     */
    boolean removeTransportedUnit(Unit unit);

    /**
     * Clears the set of units being transported by this unit.
     */
    void clearTransportedUnits();

    /**
     * If this unit is capable of transporting another unit, return true
     *
     * @return true if the unit can transport another unit
     */
    boolean hasTransportCapacity();

    /**
     * Gets the different kinds of transporters the transport has
     *
     * @return Set of Transporter types
     */
    Set<TransporterType> getTransportCapabilities();

    /**
     * Returns true if the unit has capacity left for a transporter type
     *
     * @param transporterType Does the unit have free capacity in this type?
     *
     * @return True if the unit has capacity, false if not
     */
    boolean hasTransportCapacity(TransporterType transporterType);

    /**
     * Returns the current capacity of a transporter type
     *
     * @param transporterType What kind of transporter types are we checking?
     *
     * @return The current capacity of the transporter
     */
    double getCurrentTransportCapacity(TransporterType transporterType);

    /**
     * Sets the current transport capacity for the provided transport type
     *
     * @param transporterType What kind of transporter are we changing the capacity of?
     * @param capacity        What is the new capacity?
     */
    void setCurrentTransportCapacity(TransporterType transporterType, double capacity);

    /**
     * Recalculates transport capacity
     *
     * @param transporters What transporters are we tracking the details of?
     */
    void recalculateTransportCapacity(Vector<Transporter> transporters);

    /**
     * When fixing references we need to replace the transported units
     *
     * @param newTransportedUnits The units that should be transported
     */
    void replaceTransportedUnits(Set<Unit> newTransportedUnits);

    /**
     * Bay unloading utility used when removing a bay-equipped Transport unit This removes all units assigned to the
     * transport from it
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
     *
     * @param transportedUnits Units we wish to unload
     */
    void unloadTransport(Set<Unit> transportedUnits);

    /**
     * Fixes references after loading
     */
    void fixReferences(Campaign campaign, Unit unit);

}
