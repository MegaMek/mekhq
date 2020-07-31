/*
 * Copyright (c) 2018, 2020 - The MegaMek Team. All Rights Reserved.
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
        } else if ((mostTypes & Entity.ETYPE_AERO) != 0) {
            mtf.add(Entity.ETYPE_AERO);
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

        Person p;
        int weight = (int) (c.getCampaignOptions().getPersonnelMarketDylansWeight() * 100);
        for (int i = 0; i < q; i++) {
            long choice = mtf.get(Compute.randomInt(Math.max(mtf.size() - 1, 1)));
            if (Compute.randomInt(99) < weight) {
                if (choice == Entity.ETYPE_MECH) {
                    p = c.newPerson(Person.T_MECHWARRIOR);
                } else if (choice == Entity.ETYPE_TANK) {
                    if (Compute.d6() < 3) {
                        p = c.newPerson(Person.T_GVEE_DRIVER);
                    } else {
                        p = c.newPerson(Person.T_VEE_GUNNER);
                    }
                } else if (choice == Entity.ETYPE_AERO) {
                    p = c.newPerson(Person.T_AERO_PILOT);
                } else if (choice == Entity.ETYPE_BATTLEARMOR) {
                    p = c.newPerson(Person.T_BA);
                } else if (choice == Entity.ETYPE_INFANTRY) {
                    p = c.newPerson(Person.T_INFANTRY);
                } else if (choice == Entity.ETYPE_PROTOMECH) {
                    p = c.newPerson(Person.T_PROTO_PILOT);
                } else if (choice == Entity.ETYPE_CONV_FIGHTER) {
                    p = c.newPerson(Person.T_CONV_PILOT);
                } else if (choice == Entity.ETYPE_SMALL_CRAFT) {
                    p = c.newPerson(Person.T_SPACE_PILOT);
                } else if (choice == Entity.ETYPE_DROPSHIP) {
                    int space = Compute.randomInt(Person.T_SPACE_GUNNER);
                    while (space < Person.T_SPACE_PILOT) {
                        space = Compute.randomInt(Person.T_SPACE_GUNNER);
                    }
                    p = c.newPerson(space);
                } else {
                    p = c.newPerson(Person.T_NONE);
                }
            } else {
                int roll = Compute.randomInt(Person.T_NUM - 1);
                while (roll == Person.T_NONE) {
                    roll = Compute.randomInt(Person.T_NUM - 1);
                }
                p = c.newPerson(roll);
            }
            retVal.add(p);
        }
        return retVal;
    }
}
