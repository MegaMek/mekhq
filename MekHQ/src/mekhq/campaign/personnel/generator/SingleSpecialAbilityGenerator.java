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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import megamek.common.options.PilotOptions;
import mekhq.Utilities;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.generator.AbstractSpecialAbilityGenerator;

/**
 * Generates a single special ability for a {@link Person}.
 */
public class SingleSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {

    @Override
    public boolean generateSpecialAbilities(Person person, int expLvl) {
        if (getCampaignOptions(person).useAbilities()) {
            return rollSPA(person) != null;
        }
        return false;
    }

    public String rollSPA(Person person) {
        List<SpecialAbility> abilityList = getEligibleSPAs(person);
        if (abilityList.isEmpty()) {
            return null;
        }

        // create a weighted list based on XP
        List<SpecialAbility> weightedList = SpecialAbility.getWeightedSpecialAbilities(abilityList);

        String name = Utilities.getRandomItem(weightedList).getName();
        if (name.equals(OptionsConstants.GUNNERY_SPECIALIST)) {
            String special = Crew.SPECIAL_NONE;
            switch (Compute.randomInt(2)) {
                case 0:
                    special = Crew.SPECIAL_ENERGY;
                    break;
                case 1:
                    special = Crew.SPECIAL_BALLISTIC;
                    break;
                case 2:
                    special = Crew.SPECIAL_MISSILE;
                    break;
            }
            person.getOptions()
                .acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
        } else if (name.equals(OptionsConstants.GUNNERY_RANGE_MASTER)) {
            String special = Crew.RANGEMASTER_NONE;
            switch (Compute.randomInt(2)) {
                case 0:
                    special = Crew.RANGEMASTER_MEDIUM;
                    break;
                case 1:
                    special = Crew.RANGEMASTER_LONG;
                    break;
                case 2:
                    special = Crew.RANGEMASTER_EXTREME;
                    break;
            }
            person.getOptions()
                .acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
        } else if (name.equals(OptionsConstants.MISC_HUMAN_TRO)) {
            String special = Crew.HUMANTRO_NONE;
            switch (Compute.randomInt(3)) {
                case 0:
                    special = Crew.HUMANTRO_MECH;
                    break;
                case 1:
                    special = Crew.HUMANTRO_AERO;
                    break;
                case 2:
                    special = Crew.HUMANTRO_VEE;
                    break;
                case 3:
                    special = Crew.HUMANTRO_BA;
                    break;
            }
            person.getOptions()
                .acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
        } else if (name.equals(OptionsConstants.GUNNERY_WEAPON_SPECIALIST)) {
            person.getOptions()
                .acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                    SpecialAbility.chooseWeaponSpecialization(person.getPrimaryRole(), person.getOriginFaction().isClan(),
                            getCampaignOptions(person).getTechLevel(), person.getCampaign().getGameYear(), false));
        } else if (name.equals(OptionsConstants.GUNNERY_SANDBLASTER)) {
            person.getOptions()
                .acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                    SpecialAbility.chooseWeaponSpecialization(person.getPrimaryRole(), person.getOriginFaction().isClan(),
                            getCampaignOptions(person).getTechLevel(), person.getCampaign().getGameYear(), true));
        } else {
            person.getOptions()
                .acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, true);
        }
        return name;
    }

    private List<SpecialAbility> getEligibleSPAs(Person person) {
        List<SpecialAbility> eligible = new ArrayList<>();
        for (Enumeration<IOption> i = person.getOptions(PilotOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (!ability.booleanValue()) {
                SpecialAbility spa = SpecialAbility.getAbility(ability.getName());
                if(null == spa) {
                    continue;
                }
                if(!spa.isEligible(person.isClanner(), person.getSkills(),
                        person.getOptions())) {
                    continue;
                }
                if(spa.getWeight() <= 0) {
                    continue;
                }
                eligible.add(spa);
            }
        }
        return eligible;
    }

    private CampaignOptions getCampaignOptions(Person person) {
        return person.getCampaign().getCampaignOptions();
    }
}
