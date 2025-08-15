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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.Crew;
import megamek.common.annotations.Nullable;
import megamek.common.options.IOption;
import megamek.common.options.OptionsConstants;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;

/**
 * Generates a single special ability for a {@link Person}.
 */
public class SingleSpecialAbilityGenerator extends AbstractSpecialAbilityGenerator {
    @Override
    public boolean generateSpecialAbilities(final Campaign campaign, final Person person,
          final int expLvl) {
        return campaign.getCampaignOptions().isUseAbilities() && (rollSPA(campaign, person) != null);
    }

    /**
     * @param person the person to roll and assign the SPA for
     *
     * @return the display name of the rolled SPA, or null if one wasn't rolled
     */
    public @Nullable String rollSPA(final Campaign campaign, final Person person) {
        final List<SpecialAbility> abilityList = getEligibleSPAs(person);
        if (abilityList.isEmpty()) {
            return null;
        }

        // create a weighted list based on XP
        final List<SpecialAbility> weightedList = SpecialAbility.getWeightedSpecialAbilities(abilityList);

        final String name = ObjectUtility.getRandomItem(weightedList).getName();
        String displayName = SpecialAbility.getDisplayName(name);
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
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
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
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.MISC_ENV_SPECIALIST: {
                final String special;
                switch (Compute.randomInt(4)) {
                    case 0:
                        special = Crew.ENVSPC_FOG;
                        break;
                    case 1:
                        special = Crew.ENVSPC_LIGHT;
                        break;
                    case 2:
                        special = Crew.ENVSPC_RAIN;
                        break;
                    case 3:
                        special = Crew.ENVSPC_SNOW;
                        break;
                    case 4:
                    default:
                        special = Crew.ENVSPC_WIND;
                        break;
                }
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.MISC_HUMAN_TRO: {
                final String special;
                switch (Compute.randomInt(3)) {
                    case 0:
                        special = Crew.HUMANTRO_MEK;
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
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.GUNNERY_WEAPON_SPECIALIST: {
                final String special = SpecialAbility.chooseWeaponSpecialization(person,
                      campaign.getCampaignOptions().getTechLevel(), campaign.getGameYear(), false);
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            case OptionsConstants.GUNNERY_SANDBLASTER: {
                final String special = SpecialAbility.chooseWeaponSpecialization(person,
                      campaign.getCampaignOptions().getTechLevel(), campaign.getGameYear(), true);
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, special);
                displayName += " " + special;
                break;
            }
            default: {
                person.getOptions().acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, name, true);
                break;
            }
        }

        return displayName;
    }

    private List<SpecialAbility> getEligibleSPAs(Person person) {
        List<SpecialAbility> eligible = new ArrayList<>();
        for (Enumeration<IOption> i = person.getOptions(PersonnelOptions.LVL3_ADVANTAGES); i.hasMoreElements(); ) {
            IOption ability = i.nextElement();
            if (!ability.booleanValue()) {
                SpecialAbility spa = SpecialAbility.getAbility(ability.getName());
                if ((spa == null) || (spa.getWeight() <= 0)
                          || (!spa.isEligible(person.isClanPersonnel(), person.getSkills(), person.getOptions()))) {
                    continue;
                }
                eligible.add(spa);
            }
        }
        return eligible;
    }
}
