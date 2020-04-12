/*
 * Copyright (C) 2019 MegaMek team
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
package mekhq.campaign.personnel.generator;

import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

public class DefaultSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {

    @Override
    public boolean generateSpecialAbilities(Person person, int expLvl) {
        if (getCampaignOptions(person).useAbilities()) {
            SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
            singleSpecialAbilityGenerator.setSkillPreferences(getSkillPreferences());

            // base number of special abilities is based on a random roll
            int numAbilities = Utilities.rollSpecialAbilities(getSkillPreferences().getSpecialAbilBonus(expLvl));

            // Based on the AtB rules, recruits of veteran gain one and elites gain two
            // additional special abilities
            if (getCampaignOptions(person).getUseAtB()) {
                switch (expLvl) {
                    case SkillType.EXP_VETERAN:
                        numAbilities += 1;
                        break;
                    case SkillType.EXP_ELITE:
                        numAbilities += 2;
                        break;
                }
            }

            while ((numAbilities > 0)
                    && singleSpecialAbilityGenerator.generateSpecialAbilities(person, expLvl)) {
                numAbilities--;
            }

            return true;
        }

        return false;
    }

    private CampaignOptions getCampaignOptions(Person person) {
        return person.getCampaign().getCampaignOptions();
    }
}
