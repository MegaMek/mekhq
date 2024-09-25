/*
 * Copyright (c) 2019-2022 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.generator;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

public class DefaultSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {
    @Override
    public boolean generateSpecialAbilities(final Campaign campaign, final Person person,
            final int expLvl) {
        if (campaign.getCampaignOptions().isUseAbilities()) {
            SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
            singleSpecialAbilityGenerator.setSkillPreferences(getSkillPreferences());

            // base number of special abilities is based on a random roll
            int numAbilities = Utilities.rollSpecialAbilities(getSkillPreferences().getSpecialAbilityBonus(expLvl));

            // Then we generate up to that number, stopping if there are no potential
            // abilities to generate
            while ((numAbilities > 0)
                    && singleSpecialAbilityGenerator.generateSpecialAbilities(campaign, person, expLvl)) {
                numAbilities--;
            }

            // Based on the AtB rules, veteran recruits gain one and elite recruits gain two
            // additional special abilities. Further, these are converted to edge if they
            // cannot be
            // generated.
            if (campaign.getCampaignOptions().isUseAtB() && (expLvl >= SkillType.EXP_VETERAN)) {
                // If we have more than 0 remaining abilities we can skip trying to generate
                // after
                // deciding on the number of further rolls and just convert it into edge
                final boolean instantEdgeConversion = numAbilities != 0;

                // then we set the number of abilities to generate at 1 for veteran and 2 for
                // elite
                switch (expLvl) {
                    case SkillType.EXP_VETERAN:
                        numAbilities = 1;
                        break;
                    case SkillType.EXP_ELITE:
                        numAbilities = 2;
                        break;
                }

                // If we aren't going to immediately convert into edge, we attempt to generate
                // special
                // abilities for the person
                if (!instantEdgeConversion) {
                    while ((numAbilities > 0)
                            && singleSpecialAbilityGenerator.generateSpecialAbilities(campaign, person, expLvl)) {
                        numAbilities--;
                    }
                }

                // If edge is enabled and there are any leftover abilities that cannot be
                // generated
                // we assign edge
                if (campaign.getCampaignOptions().isUseEdge() && (instantEdgeConversion || (numAbilities > 0))) {
                    person.changeEdge(numAbilities);
                }
            }

            return true;
        }

        return false;
    }
}
