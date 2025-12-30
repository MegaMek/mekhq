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

import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.Person.*;
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
import mekhq.campaign.personnel.skills.Attributes;
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
                  (primaryRole.isMekWarrior() ||
                         primaryRole.isVehicleCrewGround() ||
                         primaryRole.isVehicleCrewNaval() ||
                         primaryRole.isVehicleCrewVTOL() ||
                         primaryRole.isSoldier()) &&
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

    /**
     * Generates and assigns attribute scores for the specified person based on their profession, phenotype, and
     * randomization settings.
     *
     * <p>This method performs the following steps:</p>
     * <ol>
     *     <li><b>Reset:</b> All attributes are reset to {@link Attributes#DEFAULT_ATTRIBUTE_SCORE}</li>
     *     <li><b>Early Exit:</b> If attributes are disabled via {@link RandomSkillPreferences#isUseAttributes()},
     *         the method returns immediately</li>
     *     <li><b>Base Assignment:</b> Attribute scores are calculated by combining:
     *         <ul>
     *             <li>Profession-based modifiers from {@link PersonnelRole#getAttributeModifier(SkillAttribute)}</li>
     *             <li>Phenotype-based modifiers from {@link Phenotype#getAttributeModifier(SkillAttribute)}</li>
     *         </ul>
     *     </li>
     *     <li><b>Randomization:</b> If enabled via {@link RandomSkillPreferences#isRandomizeAttributes()}, each
     *     attribute receives an additional random adjustment using {@link #performTraitRoll()}, which produces
     *     values ranging from -2 to +2</li>
     * </ol>
     *
     * <p>All final attribute scores are clamped within the valid range defined by
     * {@link Attributes#MINIMUM_ATTRIBUTE_SCORE} and {@link Attributes#MAXIMUM_ATTRIBUTE_SCORE}.</p>
     *
     * @param person the {@link Person} whose attributes will be generated and assigned
     */
    @Override
    public void generateAttributes(Person person, boolean isUseEdge) {
        RandomSkillPreferences skillPreferences = getSkillPreferences();

        // Reset Attribute Scores to default
        for (SkillAttribute attribute : SkillAttribute.values()) {
            if (attribute.isNone()) {
                continue;
            }

            if (attribute != SkillAttribute.EDGE) {
                person.setAttributeScore(attribute, DEFAULT_ATTRIBUTE_SCORE);
            } else {
                person.setAttributeScore(attribute, 0);
            }
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
            if (randomizeAttributes) {
                boolean isEdge = attribute == SkillAttribute.EDGE;
                int delta;
                if (isEdge && isUseEdge) {
                    delta = d6(2) == 12 ? 1 : 0;
                } else {
                    delta = performTraitRoll();
                }

                if (delta != 0) {
                    person.changeAttributeScore(attribute, delta);
                }
            }
        }
    }

    /**
     * Generates traits for the specified person based on random rolls.
     *
     * <p>When randomization is enabled via {@link RandomSkillPreferences#isRandomizeTraits()}, this method
     * assigns the following traits using 2d6-based rolls that produce values ranging from -2 to +2:</p>
     *
     * <ul>
     *     <li><b>Connections</b>: Social network strength (clamped to valid range)</li>
     *     <li><b>Reputation</b>: Public standing and renown (clamped to valid range)</li>
     *     <li><b>Wealth</b>: Personal financial resources (clamped to valid range)</li>
     *     <li><b>Unlucky</b>: Degree of bad fortune (clamped to valid range)</li>
     *     <li><b>Bloodmark</b>: Clan honor debt (assigned only on rare occasions)</li>
     * </ul>
     *
     * <p><b>Bloodmark Assignment:</b></p>
     * <ul>
     *     <li>Pirates: ~11.11% chance of receiving a bloodmark</li>
     *     <li>Non-pirates: ~1.11% chance of receiving a bloodmark</li>
     *     <li>Severity is determined by {@link #performBloodmarkRoll()}, producing values 0-2</li>
     * </ul>
     *
     * <p>If trait randomization is disabled, no traits are modified.</p>
     *
     * @param person the {@link Person} whose traits will be generated and assigned
     *
     * @see #performTraitRoll()
     * @see #performBloodmarkRoll()
     */
    @Override
    public void generateTraits(Person person) {
        if (!getSkillPreferences().isRandomizeTraits()) {
            return;
        }

        person.setConnections(clamp(performTraitRoll(), MINIMUM_CONNECTIONS, MAXIMUM_CONNECTIONS));
        person.setReputation(clamp(performTraitRoll(), MINIMUM_REPUTATION, MAXIMUM_REPUTATION));
        person.setWealth(clamp(performTraitRoll(), MINIMUM_WEALTH, MAXIMUM_WEALTH));
        person.setExtraIncomeFromTraitLevel(clamp(performTraitRoll(), MINIMUM_EXTRA_INCOME, MAXIMUM_EXTRA_INCOME));

        int baseUnluckyDiceSize = 5;
        int unluckyRoll = randomInt(baseUnluckyDiceSize);
        if (unluckyRoll == 0) { // 5% chance of positive value
            person.setUnlucky(clamp(performTraitRoll(), MINIMUM_UNLUCKY, MAXIMUM_UNLUCKY));
        }
        // We want the chance of a Bloodmark to be low as it can be quite disruptive
        int baseBloodmarkDiceSize = person.getOriginFaction().isPirate() ? 5 : 50;
        // pirates = approx 11.11% chance of a bloodmark
        // non-pirates = approx 1.11% chance of a bloodmark
        int bloodmarkRoll = randomInt(baseBloodmarkDiceSize);
        if (bloodmarkRoll == 0) {
            person.setBloodmark(clamp(performBloodmarkRoll(), MINIMUM_BLOODMARK, MAXIMUM_BLOODMARK));
        }
    }

    /**
     * Performs a 2d6 roll to determine a trait modifier value.
     *
     * <p>This method rolls two six-sided dice and converts the result into a trait modifier
     * using the following distribution:</p>
     * <ul>
     *     <li><b>2</b>: returns {@code -2} (exceptional negative trait)</li>
     *     <li><b>3-5</b>: returns {@code -1} (below average trait)</li>
     *     <li><b>6-8</b>: returns {@code 0} (average trait)</li>
     *     <li><b>9-11</b>: returns {@code 1} (above average trait)</li>
     *     <li><b>12</b>: returns {@code 2} (exceptional positive trait)</li>
     * </ul>
     *
     * <p>This creates a bell curve distribution centered on average (0), with exceptional results being rare.</p>
     *
     * @return a trait modifier value ranging from {@code -2} to {@code 2}
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int performTraitRoll() {
        int roll = d6(2);
        return switch (roll) {
            case 2 -> -2;
            case 3, 4, 5 -> -1;
            case 9, 10, 11 -> 1;
            case 12 -> 2;
            default -> 0;
        };
    }

    /**
     * Performs a 2d6 roll to determine a bloodmark severity value.
     *
     * <p>This method rolls two six-sided dice and converts the result into a bloodmark value
     * using the following distribution:</p>
     *
     * <ul>
     *     <li><b>2 or 12</b>: returns {@code 2} (~5.56% chance) - severe bloodmark</li>
     *     <li><b>3-5 or 9-11</b>: returns {@code 1} (~50% chance) - moderate bloodmark</li>
     *     <li><b>6-8</b>: returns {@code 0} (~44.44% chance) - no bloodmark assigned</li>
     * </ul>
     *
     * <p>This creates a bell curve distribution where most results produce a moderate bloodmark, with severe
     * bloodmarks being rare and no bloodmark being moderately common.</p>
     *
     * @return a bloodmark severity value: {@code 0} (none), {@code 1} (moderate), or {@code 2} (severe)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static int performBloodmarkRoll() {
        int roll = d6(2);
        return switch (roll) {
            case 2, 12 -> 2;
            case 3, 4, 5, 9, 10, 11 -> 1;
            default -> 0;
        };
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
