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
        useDylansRandomXP = options.isUseDylansRandomXP();
        percentFemale = getPercentFemale();
        nonBinaryDiceSize = options.getNonBinaryDiceSize();
        familyDisplayLevel = options.getFamilyDisplayLevel();
        announceOfficersOnly = options.isAnnounceOfficersOnly();
        announceBirthdays = options.isAnnounceBirthdays();
        announceChildBirthdays = options.isAnnounceChildBirthdays();
        announceRecruitmentAnniversaries = options.isAnnounceRecruitmentAnniversaries();
        announceRetireeDeath = options.isAnnounceRetireeDeath();
        announceRetireeDeathExpanded = options.isAnnounceRetireeDeathExpanded();
        showLifeEventDialogBirths = options.isShowLifeEventDialogBirths();
        showLifeEventDialogComingOfAge = options.isShowLifeEventDialogComingOfAge();
        showLifeEventDialogCelebrations = options.isShowLifeEventDialogCelebrations();
        awardVeterancySPAs = options.isAwardVeterancySPAs();
        awardRelevantVeterancySPAs = options.isAwardRelevantVeterancySPAs();
        rewardComingOfAgeAbilities = options.isRewardComingOfAgeAbilities();
        rewardComingOfAgeRPSkills = options.isRewardComingOfAgeRPSkills();
        useRandomPersonalities = options.isUseRandomPersonalities();
        useRandomPersonalityReputation = options.isUseRandomPersonalityReputation();
        useReasoningXpMultiplier = options.isUseReasoningXpMultiplier();
        useSimulatedRelationships = options.isUseSimulatedRelationships();
        randomizeOrigin = originOptions.isRandomizeOrigin();
        randomizeDependentOrigin = originOptions.isRandomizeDependentOrigin();
        randomizeAroundSpecifiedPlanet = originOptions.isRandomizeAroundSpecifiedPlanet();
        specifiedPlanet = originOptions.getSpecifiedPlanet();
        originSearchRadius = originOptions.getOriginSearchRadius();
        originDistanceScale = originOptions.getOriginDistanceScale();
        allowClanOrigins = originOptions.isAllowClanOrigins();
        extraRandomOrigin = originOptions.isExtraRandomOrigin();
        useRandomDeathSuicideCause = options.isUseRandomDeathSuicideCause();
        randomDeathMultiplier = options.getRandomDeathMultiplier();
        enabledRandomDeathAgeGroups.putAll(options.getEnabledRandomDeathAgeGroups());
        useEducationModule = options.isUseEducationModule();
        curriculumXpRate = options.getCurriculumXpRate();
        maximumJumpCount = options.getMaximumJumpCount();
        useReeducationCamps = options.isUseReeducationCamps();
        enableOverrideRequirements = options.isEnableOverrideRequirements();
        enableShowIneligibleAcademies = options.isEnableShowIneligibleAcademies();
        entranceExamBaseTargetNumber = options.getEntranceExamBaseTargetNumber();
        enableLocalAcademies = options.isEnableLocalAcademies();
        enablePrestigiousAcademies = options.isEnablePrestigiousAcademies();
        enableUnitEducation = options.isEnableUnitEducation();
        enableBonuses = options.isEnableBonuses();
        facultyXpRate = options.getFacultyXpRate();
        adultDropoutChance = options.getAdultDropoutChance();
        childrenDropoutChance = options.getChildrenDropoutChance();
        allAges = options.isAllAges();
        militaryAcademyAccidents = options.getMilitaryAcademyAccidents();
        useOriginFactionForNames = options.isUseOriginFactionForNames();
        factionNames = RandomNameGenerator.getInstance().getChosenFaction();
        assignPortraitOnRoleChange = options.isAssignPortraitOnRoleChange();
        allowDuplicatePortraits = options.isAllowDuplicatePortraits();
        useGenderedPortraitsOnly = options.isUseGenderedPortraitsOnly();
        noRandomPortraitsForChildren = options.isNoRandomPortraitsForChildren();
        childPortraitsWhenComingOfAge = options.isChildPortraitsWhenComingOfAge();
        usePortraitForRole = options.isUsePortraitForRoles().clone();
    }

    void applyTo(@Nonnull CampaignOptions options, @Nonnull RandomOriginOptions originOptions) {
        options.setUseDylansRandomXP(useDylansRandomXP);
        RandomGenderGenerator.setPercentFemale(percentFemale);
        options.setNonBinaryDiceSize(nonBinaryDiceSize);
        options.setFamilyDisplayLevel(familyDisplayLevel);
        options.setAnnounceOfficersOnly(announceOfficersOnly);
        options.setAnnounceBirthdays(announceBirthdays);
        options.setAnnounceChildBirthdays(announceChildBirthdays);
        options.setAnnounceRecruitmentAnniversaries(announceRecruitmentAnniversaries);
        options.setAnnounceRetireeDeath(announceRetireeDeath);
        options.setAnnounceRetireeDeathExpanded(announceRetireeDeathExpanded);
        options.setShowLifeEventDialogBirths(showLifeEventDialogBirths);
        options.setShowLifeEventDialogComingOfAge(showLifeEventDialogComingOfAge);
        options.setShowLifeEventDialogCelebrations(showLifeEventDialogCelebrations);
        options.setAwardVeterancySPAs(awardVeterancySPAs);
        options.setAwardRelevantVeterancySPAs(awardRelevantVeterancySPAs);
        options.setRewardComingOfAgeAbilities(rewardComingOfAgeAbilities);
        options.setRewardComingOfAgeRPSkills(rewardComingOfAgeRPSkills);
        options.setUseRandomPersonalities(useRandomPersonalities);
        options.setUseRandomPersonalityReputation(useRandomPersonalityReputation);
        options.setUseReasoningXpMultiplier(useReasoningXpMultiplier);
        options.setUseSimulatedRelationships(useSimulatedRelationships);
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
        options.setRandomOriginOptions(originOptions);
        options.setUseRandomDeathSuicideCause(useRandomDeathSuicideCause);
        options.setRandomDeathMultiplier(randomDeathMultiplier);
        for (final AgeGroup ageGroup : AgeGroup.values()) {
            options.getEnabledRandomDeathAgeGroups().put(ageGroup,
                  Boolean.TRUE.equals(enabledRandomDeathAgeGroups.get(ageGroup)));
        }
        options.setUseEducationModule(useEducationModule);
        options.setCurriculumXpRate(curriculumXpRate);
        options.setMaximumJumpCount(maximumJumpCount);
        options.setUseReeducationCamps(useReeducationCamps);
        options.setEnableOverrideRequirements(enableOverrideRequirements);
        options.setEnableShowIneligibleAcademies(enableShowIneligibleAcademies);
        options.setEntranceExamBaseTargetNumber(entranceExamBaseTargetNumber);
        options.setEnableLocalAcademies(enableLocalAcademies);
        options.setEnablePrestigiousAcademies(enablePrestigiousAcademies);
        options.setEnableUnitEducation(enableUnitEducation);
        options.setEnableBonuses(enableBonuses);
        options.setFacultyXpRate(facultyXpRate);
        options.setAdultDropoutChance(adultDropoutChance);
        options.setChildrenDropoutChance(childrenDropoutChance);
        options.setAllAges(allAges);
        options.setMilitaryAcademyAccidents(militaryAcademyAccidents);
        options.setUseOriginFactionForNames(useOriginFactionForNames);
        options.setAssignPortraitOnRoleChange(assignPortraitOnRoleChange);
        options.setAllowDuplicatePortraits(allowDuplicatePortraits);
        options.setUseGenderedPortraitsOnly(useGenderedPortraitsOnly);
        options.setNoRandomPortraitsForChildren(noRandomPortraitsForChildren);
        options.setChildPortraitsWhenComingOfAge(childPortraitsWhenComingOfAge);
        RandomNameGenerator.getInstance().setChosenFaction(factionNames);
        for (int i = 0; i < usePortraitForRole.length; i++) {
            options.setUsePortraitForRole(i, usePortraitForRole[i]);
        }
    }
}
