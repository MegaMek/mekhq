/*
 * Copyright (c) 2018 - The MegaMek Team. All Rights Reserved.
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

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import mekhq.campaign.personnel.enums.PersonnelRole;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Method for personnel market generation given in the replacement personnel section of Campaign Operations
 */
public class PersonnelMarketCampaignOps implements PersonnelMarketMethod {
    @Override
    public String getModuleName() {
        return "Campaign Ops";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        final List<PersonnelRole> techRoles = PersonnelRole.getTechRoles();
        final List<PersonnelRole> vesselRoles = PersonnelRole.getVesselRoles();

        Person p = null;
        int roll = Compute.d6(2);
        if (roll == 2) { // Medical
            p = c.newPerson(PersonnelRole.DOCTOR);
        } else if (roll == 3) { // ASF or Proto Pilot
            if (c.getFaction().isClan() && c.getLocalDate().isAfter(LocalDate.of(3059, 1, 1))
                    && Compute.d6(2) < 6) {
                p = c.newPerson(PersonnelRole.PROTOMECH_PILOT);
            } else {
                p = c.newPerson(PersonnelRole.AEROSPACE_PILOT);
            }
        } else if (roll == 4 || roll == 10) { // MW
            p = c.newPerson(PersonnelRole.MECHWARRIOR);
        } else if (roll == 5 || roll == 9) { // Vehicle Crews
            p = c.newPerson((Compute.d6() < 3) ? PersonnelRole.GROUND_VEHICLE_DRIVER : PersonnelRole.VEHICLE_GUNNER);
        } else if (roll == 6 || roll == 8) { // Infantry
            p = c.newPerson((c.getFaction().isClan() && Compute.d6(2) > 3)
                    ? PersonnelRole.BATTLE_ARMOUR : PersonnelRole.SOLDIER);
        } else if (roll == 11) { // Tech
            p = c.newPerson(techRoles.get(Compute.randomInt(techRoles.size())));
        } else if (roll == 12) { // Vessel Crew
            p = c.newPerson(vesselRoles.get(Compute.randomInt(vesselRoles.size())));
        }
        if (p != null) {
            return Collections.singletonList(p);
        }
        return null;
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        return current;
    }
}
