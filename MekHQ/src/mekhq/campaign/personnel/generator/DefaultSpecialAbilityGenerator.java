/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.generator;

import static megamek.common.options.PilotOptions.LVL3_ADVANTAGES;

import java.util.List;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Phenotype;

public class DefaultSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {
    @Override
    public boolean generateSpecialAbilities(final Campaign campaign, final Person person, final int expLvl) {
        if (campaign.getCampaignOptions().isUseAbilities()) {
            SingleSpecialAbilityGenerator singleSpecialAbilityGenerator = new SingleSpecialAbilityGenerator();
            singleSpecialAbilityGenerator.setSkillPreferences(getSkillPreferences());

            // base number of special abilities is based on a random roll
            int numAbilities = Utilities.rollSpecialAbilities(getSkillPreferences().getSpecialAbilityBonus(expLvl));

            // Then we generate up to that number, stopping if there are no potential
            // abilities to generate
            while ((numAbilities > 0) &&
                         singleSpecialAbilityGenerator.generateSpecialAbilities(campaign, person, expLvl)) {
                numAbilities--;
            }

            // Finally, we add any SPAs from the character's phenotype
            Phenotype phenotype = person.getPhenotype();
            if (phenotype != null) {
                List<String> bonusTraits = phenotype.getBonusTraits();

                for (String bonusTrait : bonusTraits) {
                    person.getOptions().acquireAbility(LVL3_ADVANTAGES, bonusTrait, true);
                }
            }

            return true;
        }

        return false;
    }
}
