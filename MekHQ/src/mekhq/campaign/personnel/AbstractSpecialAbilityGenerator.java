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

import java.util.Objects;

import mekhq.campaign.CampaignOptions;
import mekhq.campaign.RandomSkillPreferences;

public abstract class AbstractSpecialAbilityGenerator {

    private final CampaignOptions options;

    private final RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();

    protected AbstractSpecialAbilityGenerator(CampaignOptions options) {
        this.options = Objects.requireNonNull(options);
    }

    protected CampaignOptions getCampaignOptions() {
        return options;
    }

    protected RandomSkillPreferences getRandomSkillPreferences() {
        return rskillPrefs;
    }

    public abstract void generateSpecialAbilities(Person person, int expLvl);
}
