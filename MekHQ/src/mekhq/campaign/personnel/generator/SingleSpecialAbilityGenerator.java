/*
 * Copyright (C) 2019-2021 - The MegaMek Team. All Rights Reserved.
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
        switch (name) {
            case OptionsConstants.GUNNERY_SPECIALIST: {
                final String special;
                switch (Compute.randomInt(2)) {
                    case 0:
                        special = Crew.SPECIAL_ENERGY;
                        break;
                    case 1:
                        special = Crew.SPECIAL_BALLISTIC;
                        break;
                    case 2:
                    default:
                        special = Crew.SPECIAL_MISSILE;
                        break;
                }
                person.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
                break;
            }
            case OptionsConstants.GUNNERY_RANGE_MASTER: {
                final String special;
                switch (Compute.randomInt(2)) {
                    case 0:
                        special = Crew.RANGEMASTER_MEDIUM;
                        break;
                    case 1:
                        special = Crew.RANGEMASTER_LONG;
                        break;
                    case 2:
                    default:
                        special = Crew.RANGEMASTER_EXTREME;
                        break;
                }
                person.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
                break;
            }
            case OptionsConstants.MISC_HUMAN_TRO: {
                final String special;
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
                    default:
                        special = Crew.HUMANTRO_BA;
                        break;
                }
                person.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, special);
                break;
            }
            case OptionsConstants.GUNNERY_WEAPON_SPECIALIST:
                person.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                        SpecialAbility.chooseWeaponSpecialization(person,
                                getCampaignOptions(person).getTechLevel(), person.getCampaign().getGameYear(), false));
                break;
            case OptionsConstants.GUNNERY_SANDBLASTER:
                person.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, name,
                        SpecialAbility.chooseWeaponSpecialization(person,
                                getCampaignOptions(person).getTechLevel(), person.getCampaign().getGameYear(), true));
                break;
            default:
                person.getOptions().acquireAbility(PilotOptions.LVL3_ADVANTAGES, name, true);
                break;
        }

        return name;
    }

    private List<SpecialAbility> getEligibleSPAs(Person person) {
        List<SpecialAbility> eligible = new ArrayList<>();
        for (Enumeration<IOption> i = person.getOptions(PilotOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (!ability.booleanValue()) {
                SpecialAbility spa = SpecialAbility.getAbility(ability.getName());
                if ((spa == null) || (spa.getWeight() <= 0)
                        || (!spa.isEligible(person.isClanner(), person.getSkills(), person.getOptions()))) {
                    continue;
                }
                eligible.add(spa);
            }
        }
        return eligible;
    }

    @Deprecated
    private CampaignOptions getCampaignOptions(Person person) {
        return person.getCampaign().getCampaignOptions();
    }
}
