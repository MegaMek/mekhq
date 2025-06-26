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

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_ALLIED_FACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandings.STARTING_REGARD_SAME_FACTION;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

/**
 * Represents a faction censure event within a campaign, handling the narrative and mechanical consequences associated
 * with various censure outcomes, such as going rogue, seppuku, changes in leadership, and more.
 *
 * <p>This class encapsulates the logic needed to process different scenarios based on the severity and nature of the
 * censure, modifying the campaign, involved personnel, and force status accordingly. It provides helper methods to
 * apply specific outcomes and manage related characters.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionCensureEvent {
    private final static int GO_ROGUE_DIALOG_CHOICE_INDEX = 3;
    private final static int SEPPUKU_DIALOG_CHOICE_INDEX = 4;

    /**
     * List of personnel roles considered political in the context of censure effects.
     */
    final static List<PersonnelRole> POLITICAL_ROLES = List.of(
          PersonnelRole.MORALE_OFFICER,
          PersonnelRole.LOYALTY_MONITOR,
          PersonnelRole.LOYALTY_AUDITOR);

    private final Campaign campaign;
    private final Person commander;
    private final Person secondInCommand;

    /**
     * Constructs a new FactionCensureEvent for the given campaign and censure level.
     *
     * @param campaign     the campaign in which the event takes place
     * @param censureLevel the censure level triggering this event
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureEvent(Campaign campaign, FactionCensureLevel censureLevel) {
        this.campaign = campaign;
        commander = campaign.getCommander();
        secondInCommand = campaign.getSecondInCommand();

        // There is nobody to censure
        if (commander == null) {
            return;
        }

        FactionCensureDialog initialDialog = new FactionCensureDialog(campaign, censureLevel, commander);
        int choiceIndex = initialDialog.getDialogChoiceIndex();
        boolean isSeppuku = choiceIndex == SEPPUKU_DIALOG_CHOICE_INDEX;
        boolean isGoingRogue = choiceIndex == GO_ROGUE_DIALOG_CHOICE_INDEX;

        FactionCensureConfirmationDialog confirmationDialog = new FactionCensureConfirmationDialog(campaign,
              censureLevel, commander, isSeppuku, isGoingRogue);
        if (!confirmationDialog.wasConfirmed()) {
            new FactionCensureEvent(campaign, censureLevel);
            return;
        }

        boolean committedSeppuku = false;
        if (isGoingRogue) {
            processGoingRogue(censureLevel);
            return;
        } else if (isSeppuku) {
            processPerformingSeppuku();
            committedSeppuku = true;
            // Seppuku doesn't prevent what comes next, so don't add a return here.
        }

        handleCensureEffects(censureLevel, committedSeppuku);
    }

    /**
     * Handles the mechanical and narrative consequences when a character performs seppuku.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processPerformingSeppuku() {
        new FactionJudgmentSceneDialog(campaign,
              commander,
              secondInCommand,
              FactionJudgmentSceneType.SEPPUKU);
        commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.SEPPUKU);
    }

    /**
     * Handles the consequences and procedures when the force chooses to go rogue as a result of a faction censure.
     *
     * @param censureLevel the current censure level
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processGoingRogue(FactionCensureLevel censureLevel) {
        GoingRogue goingRogueDialog = new GoingRogue(campaign, commander, secondInCommand);
        if (!goingRogueDialog.wasConfirmed()) {
            new FactionCensureEvent(campaign, censureLevel);
        }
    }

    /**
     * Applies the effects of a censure to the campaign and personnel, based on the level of censure and whether
     * seppuku was performed.
     *
     * @param censureLevel the level of censure
     * @param committedSeppuku {@code true} if seppuku was performed
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void handleCensureEffects(FactionCensureLevel censureLevel,
          boolean committedSeppuku) {
        switch (censureLevel) {
            case NO_CENSURE -> {
                return;
            }
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

    /**
     * Handles the process of a forced retirement of the commander as a result of censure.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processCensureCommanderRetirement() {
        commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.RETIRED);
    }

    /**
     * Processes a mass change in loyalty for all relevant personnel, typically in response to a major positive or
     * negative censure outcome.
     *
     * @param campaign the campaign instance
     * @param isMajor whether this is a major change
     * @param isPositiveChange {@code true} for positive, {@code false} for negative shifts
     *
     * @author Illiani
     * @since 0.50.07
     */
    static void processMassLoyaltyChange(Campaign campaign, boolean isMajor, boolean isPositiveChange) {
        LocalDate today = campaign.getLocalDate();
        for (Person person : campaign.getPersonnel()) {
            if (isExempt(person, today)) {
                continue;
            }

            person.performForcedDirectionLoyaltyChange(campaign, isPositiveChange, isMajor, false);
        }
    }

    /**
     * Adjusts the faction standing for the force, based on whether the change is major or minor.
     *
     * @param isMajor {@code true} if the standing change is significant
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processFactionStandingChange(boolean isMajor) {
        double delta = isMajor ? STARTING_REGARD_SAME_FACTION : STARTING_REGARD_ALLIED_FACTION;
        Faction faction = campaign.getFaction();
        String factionCode = faction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();
        String report = factionStandings.changeRegardForFaction(campaign.getFaction().getShortName(), factionCode,
              delta, campaign.getGameYear());

        campaign.addReport(report);
    }

    /**
     * Handles the consequences when the commander is imprisoned as a result of censure.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processCensureCommanderImprisonment() {
        commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.IMPRISONED);
    }

    /**
     * Manages leadership replacement due to a censure event.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processCensureLeadershipReplacement() {
        Set<Person> replacedPersonnel = new HashSet<>();
        replacedPersonnel.add(commander);

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

    /**
     * Finds a suitable replacement character when needed.
     *
     * @param seniorPerson the most senior person available
     *
     * @return the replacement character, or {@code null} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    private @Nullable Person getReplacementCharacter(Person seniorPerson) {
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

    /**
     * Determines a political role to be assigned during the censure process, if applicable.
     *
     * @return the chosen {@link PersonnelRole}
     *
     * @author Illiani
     * @since 0.50.07
     */
    private PersonnelRole getPoliticalRole() {
        return ObjectUtility.getRandomItem(POLITICAL_ROLES);
    }

    /**
     * Handles the complete disbanding of the force as an effect of censure.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processCensureDisband() {
        new FactionJudgmentSceneDialog(campaign,
              commander,
              secondInCommand,
              FactionJudgmentSceneType.DISBAND);
    }

    /**
     * Determines if a person is exempt from certain censure actions on the given date.
     *
     * @param person the person to evaluate
     * @param today the current date
     * @return {@code true} if the person is exempt, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
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
}
