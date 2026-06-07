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

import mekhq.campaign.campaignOptions.CampaignOptions;
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

    RelationshipsOptionsModel(CampaignOptions options) {
        useManualMarriages = options.isUseManualMarriages();
        useClanPersonnelMarriages = options.isUseClanPersonnelMarriages();
        usePrisonerMarriages = options.isUsePrisonerMarriages();
        checkMutualAncestorsDepth = options.getCheckMutualAncestorsDepth();
        logMarriageNameChanges = options.isLogMarriageNameChanges();
        randomMarriageMethod = options.getRandomMarriageMethod();
        useRandomClanPersonnelMarriages = options.isUseRandomClanPersonnelMarriages();
        useRandomPrisonerMarriages = options.isUseRandomPrisonerMarriages();
        randomMarriageAgeRange = options.getRandomMarriageAgeRange();
        randomMarriageDiceSize = options.getRandomMarriageDiceSize();
        randomNewDependentMarriage = options.getRandomNewDependentMarriage();
        useManualDivorce = options.isUseManualDivorce();
        useClanPersonnelDivorce = options.isUseClanPersonnelDivorce();
        usePrisonerDivorce = options.isUsePrisonerDivorce();
        randomDivorceMethod = options.getRandomDivorceMethod();
        useRandomOppositeSexDivorce = options.isUseRandomOppositeSexDivorce();
        useRandomSameSexDivorce = options.isUseRandomSameSexDivorce();
        useRandomClanPersonnelDivorce = options.isUseRandomClanPersonnelDivorce();
        useRandomPrisonerDivorce = options.isUseRandomPrisonerDivorce();
        randomDivorceDiceSize = options.getRandomDivorceDiceSize();
        useManualProcreation = options.isUseManualProcreation();
        useClanPersonnelProcreation = options.isUseClanPersonnelProcreation();
        usePrisonerProcreation = options.isUsePrisonerProcreation();
        multiplePregnancyOccurrences = options.getMultiplePregnancyOccurrences();
        babySurnameStyle = options.getBabySurnameStyle();
        assignNonPrisonerBabiesFounderTag = options.isAssignNonPrisonerBabiesFounderTag();
        assignChildrenOfFoundersFounderTag = options.isAssignChildrenOfFoundersFounderTag();
        determineFatherAtBirth = options.isDetermineFatherAtBirth();
        displayTrueDueDate = options.isDisplayTrueDueDate();
        noInterestInChildrenDiceSize = options.getNoInterestInChildrenDiceSize();
        useMaternityLeave = options.isUseMaternityLeave();
        logProcreation = options.isLogProcreation();
        randomProcreationMethod = options.getRandomProcreationMethod();
        useRelationshiplessRandomProcreation = options.isUseRelationshiplessRandomProcreation();
        useRandomClanPersonnelProcreation = options.isUseRandomClanPersonnelProcreation();
        useRandomPrisonerProcreation = options.isUseRandomPrisonerProcreation();
        randomProcreationRelationshipDiceSize = options.getRandomProcreationRelationshipDiceSize();
        randomProcreationRelationshiplessDiceSize = options.getRandomProcreationRelationshiplessDiceSize();
        noInterestInRelationshipsDiceSize = options.getNoInterestInRelationshipsDiceSize();
        interestedInSameSexDiceSize = options.getInterestedInSameSexDiceSize();
        interestedInBothSexesDiceSize = options.getInterestedInBothSexesDiceSize();
    }

    void applyTo(CampaignOptions options) {
        options.setUseManualMarriages(useManualMarriages);
        options.setUseClanPersonnelMarriages(useClanPersonnelMarriages);
        options.setUsePrisonerMarriages(usePrisonerMarriages);
        options.setCheckMutualAncestorsDepth(checkMutualAncestorsDepth);
        options.setLogMarriageNameChanges(logMarriageNameChanges);
        options.setRandomMarriageMethod(randomMarriageMethod);
        options.setUseRandomClanPersonnelMarriages(useRandomClanPersonnelMarriages);
        options.setUseRandomPrisonerMarriages(useRandomPrisonerMarriages);
        options.setRandomMarriageAgeRange(randomMarriageAgeRange);
        options.setRandomMarriageDiceSize(randomMarriageDiceSize);
        options.setRandomNewDependentMarriage(randomNewDependentMarriage);
        options.setUseManualDivorce(useManualDivorce);
        options.setUseClanPersonnelDivorce(useClanPersonnelDivorce);
        options.setUsePrisonerDivorce(usePrisonerDivorce);
        options.setRandomDivorceMethod(randomDivorceMethod);
        options.setUseRandomOppositeSexDivorce(useRandomOppositeSexDivorce);
        options.setUseRandomSameSexDivorce(useRandomSameSexDivorce);
        options.setUseRandomClanPersonnelDivorce(useRandomClanPersonnelDivorce);
        options.setUseRandomPrisonerDivorce(useRandomPrisonerDivorce);
        options.setRandomDivorceDiceSize(randomDivorceDiceSize);
        options.setUseManualProcreation(useManualProcreation);
        options.setUseClanPersonnelProcreation(useClanPersonnelProcreation);
        options.setUsePrisonerProcreation(usePrisonerProcreation);
        options.setMultiplePregnancyOccurrences(multiplePregnancyOccurrences);
        options.setBabySurnameStyle(babySurnameStyle);
        options.setAssignNonPrisonerBabiesFounderTag(assignNonPrisonerBabiesFounderTag);
        options.setAssignChildrenOfFoundersFounderTag(assignChildrenOfFoundersFounderTag);
        options.setDetermineFatherAtBirth(determineFatherAtBirth);
        options.setDisplayTrueDueDate(displayTrueDueDate);
        options.setNoInterestInChildrenDiceSize(noInterestInChildrenDiceSize);
        options.setUseMaternityLeave(useMaternityLeave);
        options.setLogProcreation(logProcreation);
        options.setRandomProcreationMethod(randomProcreationMethod);
        options.setUseRelationshiplessRandomProcreation(useRelationshiplessRandomProcreation);
        options.setUseRandomClanPersonnelProcreation(useRandomClanPersonnelProcreation);
        options.setUseRandomPrisonerProcreation(useRandomPrisonerProcreation);
        options.setRandomProcreationRelationshipDiceSize(randomProcreationRelationshipDiceSize);
        options.setRandomProcreationRelationshiplessDiceSize(randomProcreationRelationshiplessDiceSize);
        options.setInterestedInSameSexDiceSize(interestedInSameSexDiceSize);
        options.setNoInterestInRelationshipsDiceSize(noInterestInRelationshipsDiceSize);
        options.setInterestedInBothSexesDiceSize(interestedInBothSexesDiceSize);
    }
}