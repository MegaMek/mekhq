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

import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.skills.Attributes.DEFAULT_ATTRIBUTE_SCORE;
import static mekhq.campaign.personnel.skills.InfantryGunnerySkills.INFANTRY_GUNNERY_SKILLS;
import static mekhq.campaign.personnel.skills.SkillDeprecationTool.DEPRECATED_SKILLS;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ELITE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.campaign.personnel.skills.SkillType.EXP_ULTRA_GREEN;
import static mekhq.campaign.personnel.skills.SkillType.EXP_VETERAN;
import static mekhq.campaign.personnel.skills.enums.SkillSubType.UTILITY_COMMAND;

import java.util.ArrayList;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.skills.Skills;
import mekhq.campaign.personnel.skills.enums.SkillAttribute;

public class DefaultSkillGenerator extends AbstractSkillGenerator {
    //region Constructors
    public DefaultSkillGenerator(final RandomSkillPreferences randomSkillPreferences) {
        super(randomSkillPreferences);
    }
    //endregion Constructors

    @Override
    public void generateSkills(final Campaign campaign, final Person person, final int expLvl) {
        PersonnelRole primaryRole = person.getPrimaryRole();
        PersonnelRole secondaryRole = person.getSecondaryRole();
        RandomSkillPreferences skillPreferences = getSkillPreferences();

        int bonus = getPhenotypeBonus(person);
        int mod = 0;

        if (primaryRole.isLAMPilot()) {
            mod = -2;
        }

        generateDefaultSkills(person, primaryRole, expLvl, bonus, mod);
        generateDefaultSkills(person, secondaryRole, expLvl, bonus, mod);

        // apply phenotype bonus only to primary skills
        bonus = 0;

        // roll small arms skill
        if (!person.getSkills().hasSkill(SkillType.S_SMALL_ARMS)) {
            int smallArmsLevel = generateExpLevel((primaryRole.isSupport(true) ||
                                                         secondaryRole.isSupport(true)) ?
                                                        skillPreferences.getSupportSmallArmsBonus() :
                                                        skillPreferences.getCombatSmallArmsBonus());

            if (primaryRole.isCivilian()) {
                smallArmsLevel = 0;
            }

            if (smallArmsLevel > SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, SkillType.S_SMALL_ARMS, smallArmsLevel, skillPreferences.randomizeSkill(), bonus);
            }
        }

        if (primaryRole.isCombat()) {
            generateCommandUtilitySkills(person, expLvl, skillPreferences);
        }

        generateRoleplaySkills(person);
        generateUtilitySkills(person, expLvl);

        final CampaignOptions campaignOptions = campaign.getCampaignOptions();

        // roll artillery skill
        if (campaignOptions.isUseArtillery() &&
                  (primaryRole.isMekWarrior() || primaryRole.isVehicleGunner() || primaryRole.isSoldier()) &&
                  Utilities.rollProbability(skillPreferences.getArtilleryProb())) {
            generateArtillerySkill(person, bonus);
        }

        // roll Negotiation skill
        if (campaignOptions.isAdminsHaveNegotiation() && (primaryRole.isAdministrator())) {
            addSkill(person, SkillType.S_NEGOTIATION, expLvl, skillPreferences.randomizeSkill(), 0, mod);
        }

        // roll Administration skill
        if (campaignOptions.isTechsUseAdministration() && (person.isTech() || primaryRole.isVesselCrew())) {
            addSkill(person, SkillType.S_ADMIN, expLvl, skillPreferences.randomizeSkill(), 0, mod);
        }

        if (campaignOptions.isDoctorsUseAdministration() && (primaryRole.isDoctor())) {
            addSkill(person, SkillType.S_ADMIN, expLvl, skillPreferences.randomizeSkill(), 0, mod);
        }

        // roll Infantry Gunnery Skills
        if (!campaignOptions.isUseSmallArmsOnly()) {
            if (primaryRole.isSoldier() || secondaryRole.isSoldier()) {
                Skills skills = person.getSkills();
                for (String skillName : INFANTRY_GUNNERY_SKILLS) {
                    if (!skills.hasSkill(skillName) && (d6(1) == 1)) {
                        addSkill(person, skillName, expLvl, skillPreferences.randomizeSkill(), 0, mod);
                    }
                }
            }
        }

        // roll random secondary skill
        if (Utilities.rollProbability(skillPreferences.getSecondSkillProb())) {
            boolean isUseArtillery = campaignOptions.isUseArtillery();
            List<String> possibleSkills = new ArrayList<>();
            for (String skillType : SkillType.skillList) {
                SkillType type = SkillType.getType(skillType);
                if (!person.getSkills().hasSkill(skillType)
                          && !DEPRECATED_SKILLS.contains(type)
                          // The next lines are to prevent double-dipping
                          && !type.isUtilitySkill()
                          && !type.isRoleplaySkill()) {
                    if (SkillType.S_ARTILLERY.equals(type.getName()) && !isUseArtillery) {
                        continue;
                    }

                    possibleSkills.add(skillType);
                }
            }

            String selSkill = possibleSkills.get(randomInt(possibleSkills.size()));
            int secondLvl = generateExpLevel(skillPreferences.getSecondSkillBonus());
            addSkill(person, selSkill, secondLvl, skillPreferences.randomizeSkill(), 0);
        }
    }

    private static void generateCommandUtilitySkills(Person person, int expLvl,
          RandomSkillPreferences skillPreferences) {
        for (String skillName : SkillType.getSkillsBySkillSubType(List.of(UTILITY_COMMAND))) {
            if (person.getSkills().hasSkill(skillName)) {
                continue;
            }

            int skillLevel = generateExpLevel(skillPreferences.getCommandSkillsModifier(expLvl));
            if (skillLevel >= SkillType.EXP_ULTRA_GREEN) {
                addSkill(person, skillName, skillLevel, skillPreferences.randomizeSkill(), 0);
            }
        }
    }

    @Override
    public void generateAttributes(Person person) {
        RandomSkillPreferences skillPreferences = getSkillPreferences();

        // Reset Attribute Scores to default
        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute.isNone()) {
                continue;
            }

            person.setAttributeScore(attribute, DEFAULT_ATTRIBUTE_SCORE);
        }

        // If we're not using attributes, early exit
        if (!skillPreferences.isUseAttributes()) {
            return;
        }

        boolean randomizeAttributes = skillPreferences.isRandomizeAttributes();

        PersonnelRole profession = person.getPrimaryRole();
        Phenotype phenotype = person.getPhenotype();
        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute.isNone()) {
                continue;
            }

            // Profession && Phenotype adjustments
            int baseAttributeScore = profession.getAttributeModifier(attribute);
            int attributeModifier = phenotype.getAttributeModifier(attribute);
            person.setAttributeScore(attribute, baseAttributeScore + attributeModifier);

            // Attribute randomization
            int roll = d6();
            if (randomizeAttributes) {
                if (roll == 1) {
                    person.changeAttributeScore(attribute, -1);
                } else if (roll == 6) {
                    person.changeAttributeScore(attribute, 1);
                }
            }
        }
    }

    /**
     * Generates traits for the specified person based on random or pre-determined criteria.
     *
     * <p>When randomization is enabled, this method calculates and assigns specific traits such as connections,
     * reputation, wealth, and bad luck using random rolls. Each trait has its own set of rules for adjustment.</p>
     *
     * @param person The person whose traits will be updated. Traits are adjusted based on random rolls when
     *               randomization is enabled.
     */
    @Override
    public void generateTraits(Person person) {
        if (!getSkillPreferences().isRandomizeTraits()) {
            return;
        }

        // Connections
        if (d6() == 6) {
            person.setConnections(1);
        } else {
            person.setConnections(0);
        }

        // Reputation
        int roll = d6();
        if (roll == 6 || roll == 1) {
            person.setReputation(roll == 6 ? 1 : -1);
        } else {
            person.setReputation(0);
        }

        // Wealth
        roll = d6();
        if (roll == 6 || roll == 1) {
            person.setWealth(roll == 6 ? 1 : -1);
        } else {
            person.setWealth(0);
        }

        // Extra Income
        roll = d6();
        if (roll == 6 || roll == 1) {
            person.setExtraIncomeFromTraitLevel(roll == 6 ? 1 : -1);
        } else {
            person.setExtraIncomeFromTraitLevel(0);
        }

        // Unlucky
        roll = randomInt(20);
        if (roll == 0) {
            person.setUnlucky(1);
        } else {
            person.setUnlucky(0);
        }

        // Bloodmark
        // We want the chance of a Bloodmark to be low as it can be quite disruptive
        roll = randomInt(person.getOriginFaction().isPirate() ? 50 : 100);
        if (roll == 0) {
            person.setBloodmark(1);
        } else {
            person.setBloodmark(0);
        }
    }

    /**
     * Generates an experience level constant based on a dice roll and a provided bonus.
     *
     * <p>This method rolls 2d6 (using {@link Compute#d6(int)} with argument {@code 2}), adds the specified bonus,
     * and caps the total at 12. The result is mapped to an experience level constant.</p>
     *
     * @param bonus the value to add to the dice roll before determining level, capped, so the total does not exceed 12
     *
     * @return an experience level constant corresponding to the final (capped) roll result
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int generateExpLevel(int bonus) {
        int roll = Math.min(Compute.d6(2) + bonus, 12);

        return switch (roll) {
            case 1 -> EXP_ULTRA_GREEN;
            case 2, 3, 4, 5 -> EXP_GREEN;
            case 6, 7, 8, 9 -> EXP_REGULAR;
            case 10, 11 -> EXP_VETERAN;
            case 12 -> EXP_ELITE;
            default -> EXP_NONE;
        };
    }
}
