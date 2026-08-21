/*
 * Copyright (C) 2024-2026 The MegaMek Team. All Rights Reserved.
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

import jakarta.annotation.Nonnull;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.enums.BabySurnameStyle;
import mekhq.campaign.personnel.enums.RandomDivorceMethod;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;
import mekhq.campaign.personnel.enums.RandomProcreationMethod;

class RelationshipsOptionsModel {
    boolean useManualMarriages;
    boolean useClanPersonnelMarriages;
    boolean usePrisonerMarriages;
    int checkMutualAncestorsDepth;
    boolean logMarriageNameChanges;
    RandomMarriageMethod randomMarriageMethod;
    boolean useRandomClanPersonnelMarriages;
    boolean useRandomPrisonerMarriages;
    int randomMarriageAgeRange;
    int randomMarriageDiceSize;
    int randomNewDependentMarriage;
    boolean useManualDivorce;
    boolean useClanPersonnelDivorce;
    boolean usePrisonerDivorce;
    RandomDivorceMethod randomDivorceMethod;
    boolean useRandomOppositeSexDivorce;
    boolean useRandomSameSexDivorce;
    boolean useRandomClanPersonnelDivorce;
    boolean useRandomPrisonerDivorce;
    int randomDivorceDiceSize;
    boolean useManualProcreation;
    boolean useClanPersonnelProcreation;
    boolean usePrisonerProcreation;
    int multiplePregnancyOccurrences;
    BabySurnameStyle babySurnameStyle;
    boolean assignNonPrisonerBabiesFounderTag;
    boolean assignChildrenOfFoundersFounderTag;
    boolean determineFatherAtBirth;
    boolean displayTrueDueDate;
    int noInterestInChildrenDiceSize;
    boolean useMaternityLeave;
    boolean logProcreation;
    RandomProcreationMethod randomProcreationMethod;
    boolean useRelationshiplessRandomProcreation;
    boolean useRandomClanPersonnelProcreation;
    boolean useRandomPrisonerProcreation;
    int randomProcreationRelationshipDiceSize;
    int randomProcreationRelationshiplessDiceSize;
    int noInterestInRelationshipsDiceSize;
    int interestedInSameSexDiceSize;
    int interestedInBothSexesDiceSize;

    RelationshipsOptionsModel(@Nonnull CampaignOptions options) {
        useManualMarriages = options.get(CampaignOption.USE_MANUAL_MARRIAGES);
        useClanPersonnelMarriages = options.get(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES);
        usePrisonerMarriages = options.get(CampaignOption.USE_PRISONER_MARRIAGES);
        checkMutualAncestorsDepth = options.get(CampaignOption.CHECK_MUTUAL_ANCESTORS_DEPTH);
        logMarriageNameChanges = options.get(CampaignOption.LOG_MARRIAGE_NAME_CHANGES);
        randomMarriageMethod = options.get(CampaignOption.RANDOM_MARRIAGE_METHOD);
        useRandomClanPersonnelMarriages = options.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES);
        useRandomPrisonerMarriages = options.get(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES);
        randomMarriageAgeRange = options.get(CampaignOption.RANDOM_MARRIAGE_AGE_RANGE);
        randomMarriageDiceSize = options.get(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE);
        randomNewDependentMarriage = options.get(CampaignOption.RANDOM_NEW_DEPENDENT_MARRIAGE);
        useManualDivorce = options.get(CampaignOption.USE_MANUAL_DIVORCE);
        useClanPersonnelDivorce = options.get(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE);
        usePrisonerDivorce = options.get(CampaignOption.USE_PRISONER_DIVORCE);
        randomDivorceMethod = options.get(CampaignOption.RANDOM_DIVORCE_METHOD);
        useRandomOppositeSexDivorce = options.get(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE);
        useRandomSameSexDivorce = options.get(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE);
        useRandomClanPersonnelDivorce = options.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE);
        useRandomPrisonerDivorce = options.get(CampaignOption.USE_RANDOM_PRISONER_DIVORCE);
        randomDivorceDiceSize = options.get(CampaignOption.RANDOM_DIVORCE_DICE_SIZE);
        useManualProcreation = options.get(CampaignOption.USE_MANUAL_PROCREATION);
        useClanPersonnelProcreation = options.get(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION);
        usePrisonerProcreation = options.get(CampaignOption.USE_PRISONER_PROCREATION);
        multiplePregnancyOccurrences = options.get(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES);
        babySurnameStyle = options.get(CampaignOption.BABY_SURNAME_STYLE);
        assignNonPrisonerBabiesFounderTag = options.get(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG);
        assignChildrenOfFoundersFounderTag = options.get(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG);
        determineFatherAtBirth = options.get(CampaignOption.DETERMINE_FATHER_AT_BIRTH);
        displayTrueDueDate = options.get(CampaignOption.DISPLAY_TRUE_DUE_DATE);
        noInterestInChildrenDiceSize = options.get(CampaignOption.NO_INTEREST_IN_CHILDREN_DICE_SIZE);
        useMaternityLeave = options.get(CampaignOption.USE_MATERNITY_LEAVE);
        logProcreation = options.get(CampaignOption.LOG_PROCREATION);
        randomProcreationMethod = options.get(CampaignOption.RANDOM_PROCREATION_METHOD);
        useRelationshiplessRandomProcreation = options.get(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION);
        useRandomClanPersonnelProcreation = options.get(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION);
        useRandomPrisonerProcreation = options.get(CampaignOption.USE_RANDOM_PRISONER_PROCREATION);
        randomProcreationRelationshipDiceSize = options.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE);
        randomProcreationRelationshiplessDiceSize = options.get(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE);
        noInterestInRelationshipsDiceSize = options.get(CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE);
        interestedInSameSexDiceSize = options.get(CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE);
        interestedInBothSexesDiceSize = options.get(CampaignOption.INTERESTED_IN_BOTH_SEXES_DICE_SIZE);
    }

    void applyTo(@Nonnull CampaignOptions options) {
        options.set(CampaignOption.USE_MANUAL_MARRIAGES, useManualMarriages);
        options.set(CampaignOption.USE_CLAN_PERSONNEL_MARRIAGES, useClanPersonnelMarriages);
        options.set(CampaignOption.USE_PRISONER_MARRIAGES, usePrisonerMarriages);
        options.set(CampaignOption.CHECK_MUTUAL_ANCESTORS_DEPTH, checkMutualAncestorsDepth);
        options.set(CampaignOption.LOG_MARRIAGE_NAME_CHANGES, logMarriageNameChanges);
        options.set(CampaignOption.RANDOM_MARRIAGE_METHOD, randomMarriageMethod);
        options.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_MARRIAGES, useRandomClanPersonnelMarriages);
        options.set(CampaignOption.USE_RANDOM_PRISONER_MARRIAGES, useRandomPrisonerMarriages);
        options.set(CampaignOption.RANDOM_MARRIAGE_AGE_RANGE, randomMarriageAgeRange);
        options.set(CampaignOption.RANDOM_MARRIAGE_DICE_SIZE, randomMarriageDiceSize);
        options.set(CampaignOption.RANDOM_NEW_DEPENDENT_MARRIAGE, randomNewDependentMarriage);
        options.set(CampaignOption.USE_MANUAL_DIVORCE, useManualDivorce);
        options.set(CampaignOption.USE_CLAN_PERSONNEL_DIVORCE, useClanPersonnelDivorce);
        options.set(CampaignOption.USE_PRISONER_DIVORCE, usePrisonerDivorce);
        options.set(CampaignOption.RANDOM_DIVORCE_METHOD, randomDivorceMethod);
        options.set(CampaignOption.USE_RANDOM_OPPOSITE_SEX_DIVORCE, useRandomOppositeSexDivorce);
        options.set(CampaignOption.USE_RANDOM_SAME_SEX_DIVORCE, useRandomSameSexDivorce);
        options.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_DIVORCE, useRandomClanPersonnelDivorce);
        options.set(CampaignOption.USE_RANDOM_PRISONER_DIVORCE, useRandomPrisonerDivorce);
        options.set(CampaignOption.RANDOM_DIVORCE_DICE_SIZE, randomDivorceDiceSize);
        options.set(CampaignOption.USE_MANUAL_PROCREATION, useManualProcreation);
        options.set(CampaignOption.USE_CLAN_PERSONNEL_PROCREATION, useClanPersonnelProcreation);
        options.set(CampaignOption.USE_PRISONER_PROCREATION, usePrisonerProcreation);
        options.set(CampaignOption.MULTIPLE_PREGNANCY_OCCURRENCES, multiplePregnancyOccurrences);
        options.set(CampaignOption.BABY_SURNAME_STYLE, babySurnameStyle);
        options.set(CampaignOption.ASSIGN_NON_PRISONER_BABIES_FOUNDER_TAG, assignNonPrisonerBabiesFounderTag);
        options.set(CampaignOption.ASSIGN_CHILDREN_OF_FOUNDERS_FOUNDER_TAG, assignChildrenOfFoundersFounderTag);
        options.set(CampaignOption.DETERMINE_FATHER_AT_BIRTH, determineFatherAtBirth);
        options.set(CampaignOption.DISPLAY_TRUE_DUE_DATE, displayTrueDueDate);
        options.set(CampaignOption.NO_INTEREST_IN_CHILDREN_DICE_SIZE, noInterestInChildrenDiceSize);
        options.set(CampaignOption.USE_MATERNITY_LEAVE, useMaternityLeave);
        options.set(CampaignOption.LOG_PROCREATION, logProcreation);
        options.set(CampaignOption.RANDOM_PROCREATION_METHOD, randomProcreationMethod);
        options.set(CampaignOption.USE_RELATIONSHIPLESS_RANDOM_PROCREATION, useRelationshiplessRandomProcreation);
        options.set(CampaignOption.USE_RANDOM_CLAN_PERSONNEL_PROCREATION, useRandomClanPersonnelProcreation);
        options.set(CampaignOption.USE_RANDOM_PRISONER_PROCREATION, useRandomPrisonerProcreation);
        options.set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIP_DICE_SIZE, randomProcreationRelationshipDiceSize);
        options.set(CampaignOption.RANDOM_PROCREATION_RELATIONSHIPLESS_DICE_SIZE, randomProcreationRelationshiplessDiceSize);
        options.set(CampaignOption.INTERESTED_IN_SAME_SEX_DICE_SIZE, interestedInSameSexDiceSize);
        options.set(CampaignOption.NO_INTEREST_IN_RELATIONSHIPS_DICE_SIZE, noInterestInRelationshipsDiceSize);
        options.set(CampaignOption.INTERESTED_IN_BOTH_SEXES_DICE_SIZE, interestedInBothSexesDiceSize);
    }
}
