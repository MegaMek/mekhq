/*
 * Copyright (C) 2016-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.unit;

import mekhq.campaign.personnel.Person;

import java.util.function.BiConsumer;

public enum CrewType {
    DRIVER(Unit::addDriver),
    GUNNER(Unit::addGunner),
    VESSEL_CREW(Unit::addVesselCrew),
    NAVIGATOR(Unit::setNavigator),
    PILOT(Unit::addPilotOrSoldier),
    SOLDIER(Unit::addPilotOrSoldier),
    TECH_OFFICER(Unit::setTechOfficer);

    private final BiConsumer<Unit, Person> addMethod;

    CrewType(BiConsumer<Unit, Person> addMethod) {
        this.addMethod = addMethod;
    }

    public BiConsumer<Unit, Person> getAddMethod() {
        return addMethod;
    }
}
