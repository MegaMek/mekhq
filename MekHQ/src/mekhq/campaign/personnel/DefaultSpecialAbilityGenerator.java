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
package mekhq.campaign.personnel;

import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;

public class DefaultSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {

    @Override
    public boolean generateSpecialAbilities(Person person, int expLvl) {
        if (getCampaignOptions(person).useAbilities()) {
            SingleSpecialAbilityGenerator generator = new SingleSpecialAbilityGenerator();
            generator.setSkillPreferences(getSkillPreferences());

            int nabil = Utilities.rollSpecialAbilities(getSkillPreferences().getSpecialAbilBonus(expLvl));
            while (nabil > 0 && generator.generateSpecialAbilities(person, expLvl)) {
                nabil--;
            }

            return true;
        }

        return false;
    }

    private CampaignOptions getCampaignOptions(Person person) {
        return person.getCampaign().getCampaignOptions();
    }
}
