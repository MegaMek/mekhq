/*
 * Copyright (c) 2018  - The MegaMek Team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.market;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Generation method for personnel market that adds a random number of recruits of a random type
 * each day and removes them based on skill (with more experienced leaving more quickly).
 * 
 * @author Neoancient
 *
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

        for (int i = 0; i < q; i++) {
            int roll = Compute.randomInt(Person.T_NUM - 1);
            while (roll == Person.T_NONE) {
                roll = Compute.randomInt(Person.T_NUM - 1);
            }
            Person p = c.newPerson(roll);
            UUID id = UUID.randomUUID();
            p.setId(id);
            personnel.add(p);
        }
        return personnel;
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        List<Person> toRemove = new ArrayList<>();
        for (Person p : current) {
            int roll = Compute.d6(2);
            if (p.getExperienceLevel(false) == SkillType.EXP_ELITE
                && roll < c.getCampaignOptions().getPersonnelMarketRandomEliteRemoval()) {
                toRemove.add(p);
            } else if (p.getExperienceLevel(false) == SkillType.EXP_VETERAN
                       && roll < c.getCampaignOptions().getPersonnelMarketRandomVeteranRemoval()) {
                toRemove.add(p);
            } else if (p.getExperienceLevel(false) == SkillType.EXP_REGULAR
                       && roll < c.getCampaignOptions().getPersonnelMarketRandomRegularRemoval()) {
                toRemove.add(p);
            } else if (p.getExperienceLevel(false) == SkillType.EXP_GREEN
                       && roll < c.getCampaignOptions().getPersonnelMarketRandomGreenRemoval()) {
                toRemove.add(p);
            } else if (p.getExperienceLevel(false) == SkillType.EXP_ULTRA_GREEN
                       && roll < c.getCampaignOptions().getPersonnelMarketRandomUltraGreenRemoval()) {
                toRemove.add(p);
            }
        }
        return toRemove;
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
