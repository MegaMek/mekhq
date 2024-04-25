/*
 * Copyright (c) 2018-2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Personnel market generation method that uses Dylan's method
 */
public class PersonnelMarketDylan extends PersonnelMarketRandom {

    @Override
    public String getModuleName() {
        return "Dylan's Method";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        // TODO: Add in extra infantry and vehicle crews
        List<Person> retVal = new ArrayList<>();
        int q = generateRandomQuantity();

        ArrayList<Long> mtf = new ArrayList<>();
        long mostTypes = PersonnelMarket.getUnitMainForceTypes(c);
        if ((mostTypes & Entity.ETYPE_MECH) != 0) {
            mtf.add(Entity.ETYPE_MECH);
        } else if ((mostTypes & Entity.ETYPE_TANK) != 0) {
            mtf.add(Entity.ETYPE_TANK);
        } else if ((mostTypes & Entity.ETYPE_AEROSPACEFIGHTER) != 0) {
            mtf.add(Entity.ETYPE_AEROSPACEFIGHTER);
        } else if ((mostTypes & Entity.ETYPE_BATTLEARMOR) != 0) {
            mtf.add(Entity.ETYPE_BATTLEARMOR);
        } else if ((mostTypes & Entity.ETYPE_INFANTRY) != 0) {
            mtf.add(Entity.ETYPE_INFANTRY);
        } else if ((mostTypes & Entity.ETYPE_PROTOMECH) != 0) {
            mtf.add(Entity.ETYPE_PROTOMECH);
        } else if ((mostTypes & Entity.ETYPE_CONV_FIGHTER) != 0) {
            mtf.add(Entity.ETYPE_CONV_FIGHTER);
        } else if ((mostTypes & Entity.ETYPE_SMALL_CRAFT) != 0) {
            mtf.add(Entity.ETYPE_SMALL_CRAFT);
        } else if ((mostTypes & Entity.ETYPE_DROPSHIP) != 0) {
            mtf.add(Entity.ETYPE_DROPSHIP);
        } else {
            mtf.add(Entity.ETYPE_MECH);
        }

        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        final List<PersonnelRole> vesselRoles = PersonnelRole.getVesselRoles();
        Person p;
        int weight = (int) (c.getCampaignOptions().getPersonnelMarketDylansWeight() * 100);
        for (int i = 0; i < q; i++) {
            long choice = mtf.get(Compute.randomInt(Math.max(mtf.size() - 1, 1)));
            if (Compute.randomInt(99) < weight) {
                if (choice == Entity.ETYPE_MECH) {
                    p = c.newPerson(PersonnelRole.MECHWARRIOR);
                } else if (choice == Entity.ETYPE_TANK) {
                    p = c.newPerson((Compute.d6() < 3) ? PersonnelRole.GROUND_VEHICLE_DRIVER
                            : PersonnelRole.VEHICLE_GUNNER);
                } else if (choice == Entity.ETYPE_AEROSPACEFIGHTER) {
                    p = c.newPerson(PersonnelRole.AEROSPACE_PILOT);
                } else if (choice == Entity.ETYPE_BATTLEARMOR) {
                    p = c.newPerson(PersonnelRole.BATTLE_ARMOUR);
                } else if (choice == Entity.ETYPE_INFANTRY) {
                    p = c.newPerson(PersonnelRole.SOLDIER);
                } else if (choice == Entity.ETYPE_PROTOMECH) {
                    p = c.newPerson(PersonnelRole.PROTOMECH_PILOT);
                } else if (choice == Entity.ETYPE_CONV_FIGHTER) {
                    p = c.newPerson(PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT);
                } else if (choice == Entity.ETYPE_SMALL_CRAFT) {
                    p = c.newPerson(PersonnelRole.VESSEL_PILOT);
                } else if (choice == Entity.ETYPE_DROPSHIP) {
                    p = c.newPerson(vesselRoles.get(Compute.randomInt(vesselRoles.size())));
                } else {
                    p = c.newPerson(PersonnelRole.NONE);
                }
            } else {
                int roll = Compute.randomInt(personnelRoles.length - PersonnelRole.getCivilianCount());
                p = c.newPerson(personnelRoles[roll]);
            }
            retVal.add(p);
        }
        return retVal;
    }
}
