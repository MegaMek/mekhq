/*
 * Copyright (c) 2016-2022 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
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
