/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import static megamek.client.generator.RandomGenderGenerator.getPercentFemale;

import java.util.EnumMap;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import mekhq.campaign.RandomOriginOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.enums.AgeGroup;
import mekhq.campaign.personnel.enums.FamilialRelationshipDisplayLevel;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.Systems;

class BiographyOptionsModel {
    boolean useDylansRandomXP;
    int percentFemale;
    int nonBinaryDiceSize;
    FamilialRelationshipDisplayLevel familyDisplayLevel;
    boolean announceOfficersOnly;
    boolean announceBirthdays;
    boolean announceChildBirthdays;
    boolean announceRecruitmentAnniversaries;
    boolean announceRetireeDeath;
    boolean announceRetireeDeathExpanded;
    boolean showLifeEventDialogBirths;
    boolean showLifeEventDialogComingOfAge;
    boolean showLifeEventDialogCelebrations;
    boolean awardVeterancySPAs;
    boolean awardRelevantVeterancySPAs;
    boolean rewardComingOfAgeAbilities;
    boolean rewardComingOfAgeRPSkills;
    boolean useRandomPersonalities;
    boolean useRandomTalent;
    boolean usePersonalityLabelsOnly;
    boolean useRandomPersonalityReputation;
    boolean useReasoningXpMultiplier;
    boolean useSimulatedRelationships;
    boolean randomizeOrigin;
    boolean randomizeDependentOrigin;
    boolean randomizeAroundSpecifiedPlanet;
    @Nullable Planet specifiedPlanet;
    int originSearchRadius;
    double originDistanceScale;
    boolean allowClanOrigins;
    boolean extraRandomOrigin;
    boolean useRandomDeathSuicideCause;
    double randomDeathMultiplier;
    final Map<AgeGroup, Boolean> enabledRandomDeathAgeGroups = new EnumMap<>(AgeGroup.class);
    boolean useEducationModule;
    int curriculumXpRate;
    int maximumJumpCount;
    boolean useReeducationCamps;
    boolean enableOverrideRequirements;
    boolean enableShowIneligibleAcademies;
    int entranceExamBaseTargetNumber;
    boolean enableLocalAcademies;
    boolean enablePrestigiousAcademies;
    boolean enableUnitEducation;
    boolean enableBonuses;
    double facultyXpRate;
    int adultDropoutChance;
    int childrenDropoutChance;
    boolean allAges;
    int militaryAcademyAccidents;
    boolean useOriginFactionForNames;
    String factionNames;
    boolean assignPortraitOnRoleChange;
    boolean allowDuplicatePortraits;
    boolean useGenderedPortraitsOnly;
    boolean noRandomPortraitsForChildren;
    boolean childPortraitsWhenComingOfAge;
    final boolean[] usePortraitForRole;

    BiographyOptionsModel(@Nonnull CampaignOptions options, @Nonnull RandomOriginOptions originOptions) {
        useDylansRandomXP = options.get(CampaignOption.USE_DYLANS_RANDOM_XP);
        percentFemale = getPercentFemale();
        nonBinaryDiceSize = options.get(CampaignOption.NON_BINARY_DICE_SIZE);
        familyDisplayLevel = options.get(CampaignOption.FAMILY_DISPLAY_LEVEL);
        announceOfficersOnly = options.get(CampaignOption.ANNOUNCE_OFFICERS_ONLY);
        announceBirthdays = options.get(CampaignOption.ANNOUNCE_BIRTHDAYS);
        announceChildBirthdays = options.get(CampaignOption.ANNOUNCE_CHILD_BIRTHDAYS);
        announceRecruitmentAnniversaries = options.get(CampaignOption.ANNOUNCE_RECRUITMENT_ANNIVERSARIES);
        announceRetireeDeath = options.get(CampaignOption.ANNOUNCE_RETIREE_DEATH);
        announceRetireeDeathExpanded = options.get(CampaignOption.ANNOUNCE_RETIREE_DEATH_EXPANDED);
        showLifeEventDialogBirths = options.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS);
        showLifeEventDialogComingOfAge = options.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE);
        showLifeEventDialogCelebrations = options.get(CampaignOption.SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS);
        awardVeterancySPAs = options.get(CampaignOption.AWARD_VETERANCY_SP_AS);
        awardRelevantVeterancySPAs = options.get(CampaignOption.AWARD_RELEVANT_VETERANCY_SP_AS);
        rewardComingOfAgeAbilities = options.get(CampaignOption.REWARD_COMING_OF_AGE_ABILITIES);
        rewardComingOfAgeRPSkills = options.get(CampaignOption.REWARD_COMING_OF_AGE_RP_SKILLS);
        useRandomPersonalities = options.get(CampaignOption.USE_RANDOM_PERSONALITIES);
        useRandomTalent = options.get(CampaignOption.USE_RANDOM_TALENT);
        usePersonalityLabelsOnly = options.get(CampaignOption.USE_PERSONALITY_LABELS_ONLY);
        useRandomPersonalityReputation = options.get(CampaignOption.USE_RANDOM_PERSONALITY_REPUTATION);
        useReasoningXpMultiplier = options.get(CampaignOption.USE_REASONING_XP_MULTIPLIER);
        useSimulatedRelationships = options.get(CampaignOption.USE_SIMULATED_RELATIONSHIPS);
        randomizeOrigin = originOptions.isRandomizeOrigin();
        randomizeDependentOrigin = originOptions.isRandomizeDependentOrigin();
        randomizeAroundSpecifiedPlanet = originOptions.isRandomizeAroundSpecifiedPlanet();
        specifiedPlanet = originOptions.getSpecifiedPlanet();
        originSearchRadius = originOptions.getOriginSearchRadius();
        originDistanceScale = originOptions.getOriginDistanceScale();
        allowClanOrigins = originOptions.isAllowClanOrigins();
        extraRandomOrigin = originOptions.isExtraRandomOrigin();
        useRandomDeathSuicideCause = options.get(CampaignOption.USE_RANDOM_DEATH_SUICIDE_CAUSE);
        randomDeathMultiplier = options.get(CampaignOption.RANDOM_DEATH_MULTIPLIER);
        enabledRandomDeathAgeGroups.putAll(options.get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS));
        useEducationModule = options.get(CampaignOption.USE_EDUCATION_MODULE);
        curriculumXpRate = options.get(CampaignOption.CURRICULUM_XP_RATE);
        maximumJumpCount = options.get(CampaignOption.MAXIMUM_JUMP_COUNT);
        useReeducationCamps = options.get(CampaignOption.USE_REEDUCATION_CAMPS);
        enableOverrideRequirements = options.get(CampaignOption.ENABLE_OVERRIDE_REQUIREMENTS);
        enableShowIneligibleAcademies = options.get(CampaignOption.ENABLE_SHOW_INELIGIBLE_ACADEMIES);
        entranceExamBaseTargetNumber = options.get(CampaignOption.ENTRANCE_EXAM_BASE_TARGET_NUMBER);
        enableLocalAcademies = options.get(CampaignOption.ENABLE_LOCAL_ACADEMIES);
        enablePrestigiousAcademies = options.get(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES);
        enableUnitEducation = options.get(CampaignOption.ENABLE_UNIT_EDUCATION);
        enableBonuses = options.get(CampaignOption.ENABLE_BONUSES);
        facultyXpRate = options.get(CampaignOption.FACULTY_XP_RATE);
        adultDropoutChance = options.get(CampaignOption.ADULT_DROPOUT_CHANCE);
        childrenDropoutChance = options.get(CampaignOption.CHILDREN_DROPOUT_CHANCE);
        allAges = options.get(CampaignOption.ALL_AGES);
        militaryAcademyAccidents = options.get(CampaignOption.MILITARY_ACADEMY_ACCIDENTS);
        useOriginFactionForNames = options.get(CampaignOption.USE_ORIGIN_FACTION_FOR_NAMES);
        factionNames = RandomNameGenerator.getInstance().getChosenFaction();
        assignPortraitOnRoleChange = options.get(CampaignOption.ASSIGN_PORTRAIT_ON_ROLE_CHANGE);
        allowDuplicatePortraits = options.get(CampaignOption.ALLOW_DUPLICATE_PORTRAITS);
        useGenderedPortraitsOnly = options.get(CampaignOption.USE_GENDERED_PORTRAITS_ONLY);
        noRandomPortraitsForChildren = options.get(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN);
        childPortraitsWhenComingOfAge = options.get(CampaignOption.CHILD_PORTRAITS_WHEN_COMING_OF_AGE);
        usePortraitForRole = options.get(CampaignOption.USE_PORTRAIT_FOR_ROLE).clone();
    }

    void applyTo(@Nonnull CampaignOptions options, @Nonnull RandomOriginOptions originOptions) {
        options.set(CampaignOption.USE_DYLANS_RANDOM_XP, useDylansRandomXP);
        RandomGenderGenerator.setPercentFemale(percentFemale);
        options.set(CampaignOption.NON_BINARY_DICE_SIZE, nonBinaryDiceSize);
        options.set(CampaignOption.FAMILY_DISPLAY_LEVEL, familyDisplayLevel);
        options.set(CampaignOption.ANNOUNCE_OFFICERS_ONLY, announceOfficersOnly);
        options.set(CampaignOption.ANNOUNCE_BIRTHDAYS, announceBirthdays);
        options.set(CampaignOption.ANNOUNCE_CHILD_BIRTHDAYS, announceChildBirthdays);
        options.set(CampaignOption.ANNOUNCE_RECRUITMENT_ANNIVERSARIES, announceRecruitmentAnniversaries);
        options.set(CampaignOption.ANNOUNCE_RETIREE_DEATH, announceRetireeDeath);
        options.set(CampaignOption.ANNOUNCE_RETIREE_DEATH_EXPANDED, announceRetireeDeathExpanded);
        options.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_BIRTHS, showLifeEventDialogBirths);
        options.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_COMING_OF_AGE, showLifeEventDialogComingOfAge);
        options.set(CampaignOption.SHOW_LIFE_EVENT_DIALOG_CELEBRATIONS, showLifeEventDialogCelebrations);
        options.set(CampaignOption.AWARD_VETERANCY_SP_AS, awardVeterancySPAs);
        options.set(CampaignOption.AWARD_RELEVANT_VETERANCY_SP_AS, awardRelevantVeterancySPAs);
        options.set(CampaignOption.REWARD_COMING_OF_AGE_ABILITIES, rewardComingOfAgeAbilities);
        options.set(CampaignOption.REWARD_COMING_OF_AGE_RP_SKILLS, rewardComingOfAgeRPSkills);
        options.set(CampaignOption.USE_RANDOM_PERSONALITIES, useRandomPersonalities);
        options.set(CampaignOption.USE_RANDOM_TALENT, useRandomTalent);
        options.set(CampaignOption.USE_PERSONALITY_LABELS_ONLY, usePersonalityLabelsOnly);
        options.set(CampaignOption.USE_RANDOM_PERSONALITY_REPUTATION, useRandomPersonalityReputation);
        options.set(CampaignOption.USE_REASONING_XP_MULTIPLIER, useReasoningXpMultiplier);
        options.set(CampaignOption.USE_SIMULATED_RELATIONSHIPS, useSimulatedRelationships);
        originOptions.setRandomizeOrigin(randomizeOrigin);
        originOptions.setRandomizeDependentOrigin(randomizeDependentOrigin);
        originOptions.setRandomizeAroundSpecifiedPlanet(randomizeAroundSpecifiedPlanet);
        originOptions.setSpecifiedPlanet(specifiedPlanet == null ?
                                              Systems.getInstance().getSystemById("Terra").getPrimaryPlanet() :
                                              specifiedPlanet);
        originOptions.setOriginSearchRadius(originSearchRadius);
        originOptions.setOriginDistanceScale(originDistanceScale);
        originOptions.setAllowClanOrigins(allowClanOrigins);
        originOptions.setExtraRandomOrigin(extraRandomOrigin);
        options.set(CampaignOption.RANDOM_ORIGIN_OPTIONS, originOptions);
        options.set(CampaignOption.USE_RANDOM_DEATH_SUICIDE_CAUSE, useRandomDeathSuicideCause);
        options.set(CampaignOption.RANDOM_DEATH_MULTIPLIER, randomDeathMultiplier);
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            options.get(CampaignOption.ENABLED_RANDOM_DEATH_AGE_GROUPS).put(ageGroup,
                  enabledRandomDeathAgeGroups.getOrDefault(ageGroup, false));
        }
        options.set(CampaignOption.USE_EDUCATION_MODULE, useEducationModule);
        options.set(CampaignOption.CURRICULUM_XP_RATE, curriculumXpRate);
        options.set(CampaignOption.MAXIMUM_JUMP_COUNT, maximumJumpCount);
        options.set(CampaignOption.USE_REEDUCATION_CAMPS, useReeducationCamps);
        options.set(CampaignOption.ENABLE_OVERRIDE_REQUIREMENTS, enableOverrideRequirements);
        options.set(CampaignOption.ENABLE_SHOW_INELIGIBLE_ACADEMIES, enableShowIneligibleAcademies);
        options.set(CampaignOption.ENTRANCE_EXAM_BASE_TARGET_NUMBER, entranceExamBaseTargetNumber);
        options.set(CampaignOption.ENABLE_LOCAL_ACADEMIES, enableLocalAcademies);
        options.set(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES, enablePrestigiousAcademies);
        options.set(CampaignOption.ENABLE_UNIT_EDUCATION, enableUnitEducation);
        options.set(CampaignOption.ENABLE_BONUSES, enableBonuses);
        options.set(CampaignOption.FACULTY_XP_RATE, facultyXpRate);
        options.set(CampaignOption.ADULT_DROPOUT_CHANCE, adultDropoutChance);
        options.set(CampaignOption.CHILDREN_DROPOUT_CHANCE, childrenDropoutChance);
        options.set(CampaignOption.ALL_AGES, allAges);
        options.set(CampaignOption.MILITARY_ACADEMY_ACCIDENTS, militaryAcademyAccidents);
        options.set(CampaignOption.USE_ORIGIN_FACTION_FOR_NAMES, useOriginFactionForNames);
        options.set(CampaignOption.ASSIGN_PORTRAIT_ON_ROLE_CHANGE, assignPortraitOnRoleChange);
        options.set(CampaignOption.ALLOW_DUPLICATE_PORTRAITS, allowDuplicatePortraits);
        options.set(CampaignOption.USE_GENDERED_PORTRAITS_ONLY, useGenderedPortraitsOnly);
        options.set(CampaignOption.NO_RANDOM_PORTRAITS_FOR_CHILDREN, noRandomPortraitsForChildren);
        options.set(CampaignOption.CHILD_PORTRAITS_WHEN_COMING_OF_AGE, childPortraitsWhenComingOfAge);
        RandomNameGenerator.getInstance().setChosenFaction(factionNames);
        for (int i = 0; i < usePortraitForRole.length; i++) {
            options.setUsePortraitForRole(i, usePortraitForRole[i]);
        }
    }
}
