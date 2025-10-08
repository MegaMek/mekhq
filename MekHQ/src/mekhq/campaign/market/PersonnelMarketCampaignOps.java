/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Method for personnel market generation given in the replacement personnel section of Campaign Operations
 */
@Deprecated(since = "0.50.06")
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
                p = c.newPerson(PersonnelRole.PROTOMEK_PILOT);
            } else {
                p = c.newPerson(PersonnelRole.AEROSPACE_PILOT);
            }
        } else if (roll == 4 || roll == 10) { // MW
            p = c.newPerson(PersonnelRole.MEKWARRIOR);
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
