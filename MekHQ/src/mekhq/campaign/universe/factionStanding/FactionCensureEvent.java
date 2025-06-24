/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.universe.Faction;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionCensureConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionCensureDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentSceneDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentSceneType;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_ALLIED_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_SAME_FACTION;

public class FactionCensureEvent {
    private final static int GO_ROGUE_DIALOG_CHOICE_INDEX = 3;
    private final static int SEPPUKU_DIALOG_CHOICE_INDEX = 4;

    final static List<PersonnelRole> POLITICAL_ROLES = List.of(
          PersonnelRole.MORALE_OFFICER,
          PersonnelRole.LOYALTY_MONITOR,
          PersonnelRole.LOYALTY_AUDITOR);

    private final Campaign campaign;
    private final Person mostSeniorCharacter;
    private final Person secondCharacter;

    public FactionCensureEvent(Campaign campaign, FactionCensureLevel censureLevel) {
        this.campaign = campaign;
        mostSeniorCharacter = getMostSeniorCharacter();
        secondCharacter = getSecondCharacter(mostSeniorCharacter);

        // There is nobody to censure
        if (mostSeniorCharacter == null) {
            return;
        }

        FactionCensureDialog initialDialog = new FactionCensureDialog(campaign, censureLevel, mostSeniorCharacter);
        int choiceIndex = initialDialog.getDialogChoiceIndex();

        FactionCensureConfirmationDialog confirmationDialog = new FactionCensureConfirmationDialog(campaign,
              mostSeniorCharacter);
        if (!confirmationDialog.wasConfirmed()) {
            new FactionCensureEvent(campaign, censureLevel);
            return;
        }

        boolean committedSeppuku = false;
        if (choiceIndex == GO_ROGUE_DIALOG_CHOICE_INDEX) {
            processGoingRogue(censureLevel);
            return;
        } else if (choiceIndex == SEPPUKU_DIALOG_CHOICE_INDEX) {
            processPerformingSeppuku();
            committedSeppuku = true;
        }

        handleCensureEffects(censureLevel, committedSeppuku);
    }

    private void processPerformingSeppuku() {
        new FactionJudgmentSceneDialog(campaign,
              mostSeniorCharacter,
              secondCharacter,
              FactionJudgmentSceneType.SEPPUKU);
        mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.SEPPUKU);
    }

    private void processGoingRogue(FactionCensureLevel censureLevel) {
        GoingRogue goingRogueDialog = new GoingRogue(campaign, mostSeniorCharacter, secondCharacter);
        if (!goingRogueDialog.wasConfirmed()) {
            new FactionCensureEvent(campaign, censureLevel);
            return;
        }

        FactionJudgmentSceneType sceneType = switch (censureLevel) {
            case NONE -> null;
            case WARNING -> FactionJudgmentSceneType.GO_ROGUE_WARNING;
            case COMMANDER_RETIREMENT -> FactionJudgmentSceneType.GO_ROGUE_RETIRED;
            case COMMANDER_IMPRISONMENT -> FactionJudgmentSceneType.GO_ROGUE_IMPRISONED;
            case LEADERSHIP_REPLACEMENT -> FactionJudgmentSceneType.GO_ROGUE_REPLACED;
            case DISBAND -> FactionJudgmentSceneType.GO_ROGUE_DISBAND;
        };

        if (sceneType != null) {
            new FactionJudgmentSceneDialog(campaign, mostSeniorCharacter, secondCharacter, sceneType);
        }
    }

    private void handleCensureEffects(FactionCensureLevel censureLevel,
          boolean committedSeppuku) {
        switch (censureLevel) {
            case WARNING -> {
                if (committedSeppuku) {
                    processMassLoyaltyChange(campaign, false, true);
                }
            }
            case COMMANDER_RETIREMENT -> {
                if (committedSeppuku) {
                    processMassLoyaltyChange(campaign, false, true);
                }
                processCensureCommanderRetirement();
            }
            case COMMANDER_IMPRISONMENT -> {
                processMassLoyaltyChange(campaign, false, committedSeppuku);
                processCensureCommanderImprisonment();
            }
            case LEADERSHIP_REPLACEMENT -> {
                processMassLoyaltyChange(campaign, true, committedSeppuku);
                processCensureLeadershipReplacement();
            }
            case DISBAND -> processCensureDisband();
        }

        processFactionStandingChange(committedSeppuku);
    }

    private void processCensureCommanderRetirement() {
        mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.RETIRED);
    }

    static void processMassLoyaltyChange(Campaign campaign, boolean isMajor, boolean isPositiveChange) {
        LocalDate today = campaign.getLocalDate();
        for (Person person : campaign.getPersonnel()) {
            if (isExempt(person, today)) {
                continue;
            }

            person.performForcedDirectionLoyaltyChange(campaign, isPositiveChange, isMajor, false);
        }
    }

    private void processFactionStandingChange(boolean isMajor) {
        double delta = isMajor ? STARTING_REGARD_SAME_FACTION : STARTING_REGARD_ALLIED_FACTION;
        Faction faction = campaign.getFaction();
        String factionCode = faction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();
        factionStandings.changeRegardForFaction(campaign.getFaction().getShortName(), factionCode, delta, campaign.getGameYear());
    }

    private void processCensureCommanderImprisonment() {
        mostSeniorCharacter.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.IMPRISONED);
    }

    private void processCensureLeadershipReplacement() {
        Set<Person> replacedPersonnel = new HashSet<>();
        replacedPersonnel.add(mostSeniorCharacter);

        LocalDate today = campaign.getLocalDate();
        for (Person officer : campaign.getPersonnel()) {
            if (isExempt(officer, today)) {
                continue;
            }

            if (!officer.getRank().isOfficer()) {
                continue;
            }

            replacedPersonnel.add(officer);
        }

        for (Person seniorPerson : replacedPersonnel) {
            // We shouldn't end up with any exempt people, but in case we do...
            if (isExempt(seniorPerson, today)) {
                continue;
            }

            final int level = seniorPerson.getRankLevel();
            final int rank = seniorPerson.getRankNumeric();

            seniorPerson.changeStatus(campaign, today, PersonnelStatus.DISHONORABLY_DISCHARGED);

            Person replacement = getReplacementCharacter(seniorPerson);
            replacement.changeRank(campaign, rank, level, false);
            campaign.recruitPerson(replacement, true, true);
        }
    }

    private Person getReplacementCharacter(Person seniorPerson) {
        Person replacement = campaign.newPerson(seniorPerson.getPrimaryRole(), getPoliticalRole());
        if (!replacement.hasSkill(S_LEADER)) {
            replacement.addSkill(S_LEADER, randomInt(3) + 1, 0);
        }
        if (!replacement.hasSkill(S_ADMIN)) {
            replacement.addSkill(S_ADMIN, randomInt(3) + 1, 0);
        }
        replacement.setLoyalty(Compute.d6(3) + 2);
        return replacement;
    }

    private PersonnelRole getPoliticalRole() {
        return ObjectUtility.getRandomItem(POLITICAL_ROLES);
    }

    private void processCensureDisband() {
        new FactionJudgmentSceneDialog(campaign,
              mostSeniorCharacter,
              secondCharacter,
              FactionJudgmentSceneType.DISBAND);
    }

    public Person getMostSeniorCharacter() {
        Person flaggedCommander = campaign.getFlaggedCommander();
        if (flaggedCommander != null) {
            return flaggedCommander;
        }

        LocalDate today = campaign.getLocalDate();

        Collection<Person> personnel = campaign.getPersonnel();
        Person highestRankedPerson = null;
        for (Person person : personnel) {
            if (isExempt(person, today)) {
                continue;
            }

            if (highestRankedPerson == null) {
                highestRankedPerson = person;
                continue;
            }

            if (person.outRanksUsingSkillTiebreaker(campaign, highestRankedPerson)) {
                highestRankedPerson = person;
            }
        }

        return highestRankedPerson;
    }

    private static boolean isExempt(Person person, LocalDate today) {
        if (person.getStatus().isDepartedUnit()) {
            return true;
        }

        if (person.isChild(today)) {
            return true;
        }

        if (!person.isEmployed()) {
            return true;
        }

        if (!person.getPrisonerStatus().isFreeOrBondsman()) {
            return false;
        }

        return person.isDependent();
    }

    private @Nullable Person getSecondCharacter(Person commander) {
        Person second = campaign.getSeniorAdminPerson(Campaign.AdministratorSpecialization.COMMAND);
        if (second != null && !second.equals(commander)) {
            return second;
        }

        for (Person person : campaign.getActivePersonnel(false)) {
            if (person == commander) {
                continue;
            }

            if (second == null || person.outRanksUsingSkillTiebreaker(campaign, second)) {
                second = person;
            }
        }

        return second;
    }
}
