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

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.module.api.PersonnelMarketMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Generation method for personnel market that adds a random number of recruits of a random type
 * each day and removes them based on skill (with more experienced leaving more quickly).
 *
 * @author Neoancient
 */
public class PersonnelMarketRandom implements PersonnelMarketMethod {

    @Override
    public String getModuleName() {
        return "Random";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        List<Person> personnel = new ArrayList<>();
        int q = generateRandomQuantity();

        final PersonnelRole[] personnelRoles = PersonnelRole.values();
        for (int i = 0; i < q; i++) {
            int roll = Compute.randomInt(personnelRoles.length - PersonnelRole.getCivilianCount());
            Person p = c.newPerson(personnelRoles[roll]);
            personnel.add(p);
        }
        return personnel;
    }

    @Override
    public List<Person> removePersonnelForDay(final Campaign campaign, final List<Person> current) {
        return current.stream()
                .filter(person -> campaign.getCampaignOptions().getPersonnelMarketRandomRemovalTargets()
                        .get(person.getSkillLevel(campaign, false)) > Compute.d6(2))
                .collect(Collectors.toList());
    }

    int generateRandomQuantity() {
        int roll = Compute.d6(2);
        int retval = 0;
        if (roll == 12) {
            retval = 6;
        } else if (roll > 10) {
            retval = 5;
        } else if (roll > 8) {
            retval = 4;
        } else if (roll > 5) {
            retval = 3;
        } else if (roll > 3) {
            retval = 2;
        } else if (roll > 2) {
            retval = 1;
        }
        return retval;
    }
}
