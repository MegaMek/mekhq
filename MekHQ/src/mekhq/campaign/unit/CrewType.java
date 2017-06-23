/*
 * Copyright (C) 2016 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit;

import java.util.function.BiConsumer;

import mekhq.campaign.personnel.Person;

public enum CrewType {
    DRIVER((u, p) -> u.addDriver(p)),
    GUNNER((u, p) -> u.addGunner(p)),
    VESSEL_CREW((u, p) -> u.addVesselCrew(p)),
    NAVIGATOR((u, p) -> u.setNavigator(p)),
    PILOT((u, p) -> u.addPilotOrSoldier(p)),
    SOLDIER((u, p) -> u.addPilotOrSoldier(p)),
    TECH_OFFICER((u, p) -> u.setTechOfficer(p));
    
    public final BiConsumer<Unit, Person> addMethod;
    
    private CrewType(BiConsumer<Unit, Person> addMethod) {
        this.addMethod = addMethod;
    }
}
