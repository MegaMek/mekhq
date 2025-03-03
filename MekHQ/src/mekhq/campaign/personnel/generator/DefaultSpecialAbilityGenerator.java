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

            return true;
        }

        return false;
    }
}
