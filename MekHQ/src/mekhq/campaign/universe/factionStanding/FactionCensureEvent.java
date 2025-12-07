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

import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.VETERAN;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.personnel.PersonUtility.overrideSkills;
import static mekhq.campaign.personnel.skills.SkillType.S_ADMIN;
import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.universe.factionStanding.FactionCensureAction.FINE;
import static mekhq.campaign.universe.factionStanding.FactionCensureAction.FORMAL_WARNING;
import static mekhq.campaign.universe.factionStanding.FactionCensureAction.NO_ACTION;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.POLITICAL_ROLES;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.isExempt;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.processMassLoyaltyChange;
import static mekhq.campaign.universe.factionStanding.FactionStandings.REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER;
import static mekhq.campaign.universe.factionStanding.FactionStandings.REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import megamek.codeUtilities.ObjectUtility;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionCensureConfirmationDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentNewsArticle;
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
    private static final MMLogger LOGGER = MMLogger.create(FactionCensureEvent.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingJudgments";

    private static final String DIALOG_OOC_KEY = "FactionJudgmentDialog.message.CENSURE.";
    private static final String DIALOG_OOC_KEY_AFFIX = ".ooc";

    private final static int GO_ROGUE_DIALOG_CHOICE_INDEX = 3;
    private final static int SEPPUKU_DIALOG_CHOICE_INDEX = 4;

    private final Campaign campaign;
    private final Faction censuringFaction;
    private Person commander;
    private Person secondInCommand;

    /**
     * Constructs a new {@link FactionCensureEvent} for the given campaign and censure level.
     *
     * @param campaign         the campaign in which the event takes place
     * @param censureLevel     the censure level triggering this event
     * @param censuringFaction the {@link Faction} performing the censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionCensureEvent(Campaign campaign, FactionCensureLevel censureLevel, Faction censuringFaction) {
        this.campaign = campaign;
        this.censuringFaction = censuringFaction;

        FactionCensureAction censureAction = censureLevel.getFactionAppropriateAction(censuringFaction);
        if (censureAction == NO_ACTION) {
            // There isn't anything to do. Assuming I've done my job properly, this shouldn't trigger. Which is why
            // we have a warning if it does.
            LOGGER.warn("NO_ACTION censureAction passed into FactionCensureEvent");
            return;
        }

        commander = campaign.getCommander(); // Can be null if the campaign is effectively empty
        if (commander == null) {
            // If there isn't a commander in the campaign, we're going to invent someone. This avoids us needing to
            // add null protection throughout this class (and the dialogs it spawns). This clause should only trigger
            // in the event the campaign is effectively empty. So it shouldn't come up during normal play.
            commander = campaign.newPerson(PersonnelRole.MEKWARRIOR,
                  campaign.getFaction().getShortName(),
                  Gender.RANDOMIZE);
            LOGGER.warn("Commander was null in FactionCensureEvent. Using a fallback commander: {}.",
                  commander.getFullName());
        }

        secondInCommand = campaign.getSecondInCommand(); // Can be null if the campaign is effectively empty
        if (secondInCommand == null) {
            // See comments for the 'commander == null' clause
            secondInCommand = campaign.newPerson(PersonnelRole.MEKWARRIOR,
                  campaign.getFaction().getShortName(),
                  Gender.RANDOMIZE);
            LOGGER.warn("Second in command was null in FactionCensureEvent. Using a fallback secondInCommand: {}.",
                  secondInCommand.getFullName());
        }

        int dialogChoice;
        boolean isGoingRogue = false;
        boolean isSeppuku = false;
        switch (censureAction) {
            case BARRED,
                 COMMANDER_RETIREMENT,
                 DISBAND,
                 FINE,
                 BRIBE_OFFICIALS,
                 FORMAL_WARNING,
                 LEADERSHIP_REPLACEMENT,
                 LEGAL_CHALLENGE -> {
                // We keep presenting the user with the dialog until they confirm their choice
                while (true) {
                    String outOfCharacterMessage = getOutOfCharacterMessage(censureAction);

                    PersonnelRole role = censuringFaction.isClan()
                                               ? PersonnelRole.MEKWARRIOR
                                               : PersonnelRole.MILITARY_LIAISON;
                    Person speaker = campaign.newPerson(role, censuringFaction.getShortName(), Gender.RANDOMIZE);

                    ImmersiveDialogWidth dialogWidth;
                    if (censureAction.equals(FINE) || censureAction.equals(FORMAL_WARNING)) {
                        dialogWidth = ImmersiveDialogWidth.LARGE;
                    } else {
                        dialogWidth = ImmersiveDialogWidth.MEDIUM;
                    }

                    FactionJudgmentDialog censureDialog = new FactionJudgmentDialog(campaign, speaker, commander,
                          censureAction.getLookupName(), censuringFaction, FactionStandingJudgmentType.CENSURE,
                          dialogWidth, outOfCharacterMessage, null);

                    dialogChoice = censureDialog.getChoiceIndex();
                    isGoingRogue = dialogChoice == GO_ROGUE_DIALOG_CHOICE_INDEX;
                    isSeppuku = dialogChoice == SEPPUKU_DIALOG_CHOICE_INDEX;

                    FactionCensureConfirmationDialog confirmationDialog = new FactionCensureConfirmationDialog(campaign);

                    if (confirmationDialog.wasConfirmed()) {
                        break;
                    }
                }
            }
            case CLAN_TRIAL_OF_GRIEVANCE_UNSUCCESSFUL, CLAN_TRIAL_OF_GRIEVANCE_SUCCESSFUL -> {
                String dialogKey = DIALOG_OOC_KEY + censureAction.getLookupName();
                String message = getFormattedTextAt(RESOURCE_BUNDLE, dialogKey, commander.getFullName(),
                      commander.getGivenName(), secondInCommand.getFullName(), secondInCommand.getGivenName());
                new ImmersiveDialogSimple(campaign, commander, secondInCommand, message, null, null, null, false);
            }
            case COMMANDER_MURDERED,
                 COMMANDER_IMPRISONMENT,
                 LEADERSHIP_IMPRISONED,
                 NEWS_ARTICLE,
                 CHATTER_WEB_DISCUSSION ->
                  new FactionJudgmentNewsArticle(campaign, commander, secondInCommand, censureAction.getLookupName(),
                        censuringFaction, FactionStandingJudgmentType.CENSURE, false);
        }

        if (isGoingRogue) {
            processGoingRogue(campaign, censureLevel, censuringFaction);
            return;
        }

        if (isSeppuku) {
            processPerformingSeppuku(campaign, censuringFaction);
        }

        handleCensureEffects(censureAction, isSeppuku);
    }

    /**
     * Generates an out-of-character message based on the given censure action.
     *
     * <p>This message is retrieved using a resource bundle and the lookup name of the censure action.</p>
     *
     * @param censureAction the {@link FactionCensureAction} that determines the content of the out-of-character
     *                      message
     *
     * @return the out-of-character message corresponding to the given censure action
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getOutOfCharacterMessage(FactionCensureAction censureAction) {
        String eventKey = DIALOG_OOC_KEY + censureAction.getLookupName() + DIALOG_OOC_KEY_AFFIX;
        String resourceBundle = FactionJudgmentDialog.getFactionJudgmentDialogResourceBundle();
        return getTextAt(resourceBundle, eventKey);
    }

    /**
     * Processes the event where the commander chooses to perform seppuku as a result of a faction censure.
     *
     * <p>This method displays the seppuku judgment scene to the player, updates the commander's status to seppuku,
     * and applies loyalty changes across the affected personnel.</p>
     *
     * @param campaign         the current {@link Campaign} instance
     * @param censuringFaction the {@link Faction} initiating the censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processPerformingSeppuku(Campaign campaign, Faction censuringFaction) {
        new FactionJudgmentSceneDialog(campaign, commander, secondInCommand, FactionJudgmentSceneType.SEPPUKU,
              censuringFaction);
        commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.SEPPUKU);
        processMassLoyaltyChange(campaign, true, true);
    }

    /**
     * Handles the scenario in which the commander chooses to go rogue in response to a faction censure.
     *
     * <p>Presents the going rogue dialog, and if the action is canceled, falls back to a standard censure event.</p>
     *
     * @param campaign         the current {@link Campaign} instance
     * @param censureLevel     the {@link FactionCensureLevel} for the event
     * @param censuringFaction the {@link Faction} issuing the censure
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processGoingRogue(Campaign campaign, FactionCensureLevel censureLevel, Faction censuringFaction) {
        GoingRogue goingRogueDialog = new GoingRogue(campaign,
              commander,
              secondInCommand);
        if (goingRogueDialog.wasCanceled()) {
            new FactionCensureEvent(campaign, censureLevel, censuringFaction);
        }
    }

    private void handleCensureEffects(FactionCensureAction censureAction, boolean isSeppuku) {
        switch (censureAction) {
            case NO_ACTION -> {
                return;
            }
            case BARRED -> new FactionJudgmentSceneDialog(campaign, commander, null,
                  FactionJudgmentSceneType.BARRED, censuringFaction);
            case CHATTER_WEB_DISCUSSION, LEGAL_CHALLENGE, NEWS_ARTICLE, FORMAL_WARNING ->
                  processMassLoyaltyChange(campaign, false, false);
            case CLAN_TRIAL_OF_GRIEVANCE_UNSUCCESSFUL -> processClanTrial(false);
            case CLAN_TRIAL_OF_GRIEVANCE_SUCCESSFUL -> processClanTrial(true);
            case COMMANDER_MURDERED -> {
                if (!isSeppuku) {
                    // The loyalty change is wrapped into the status change handling
                    commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.HOMICIDE);
                }
                secondInCommand.setCommander(true);
            }
            case COMMANDER_IMPRISONMENT -> {
                if (!isSeppuku) {
                    // The loyalty change is wrapped into the status change handling
                    commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.IMPRISONED);
                }
            }
            case COMMANDER_RETIREMENT -> {
                if (!isSeppuku) {
                    // The loyalty change is wrapped into the status change handling
                    commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.RETIRED);
                }
            }
            case DISBAND -> new FactionJudgmentSceneDialog(campaign, commander, null,
                  FactionJudgmentSceneType.DISBAND, censuringFaction);
            case FINE, BRIBE_OFFICIALS -> {
                Finances finances = campaign.getFinances();

                Money fine = finances.getBalance().multipliedBy(0.1);
                String fineMessage;
                if (censureAction.equals(FactionCensureAction.BRIBE_OFFICIALS)) {
                    fineMessage = getTextAt(RESOURCE_BUNDLE, "FactionCensureEvent.bribe");
                } else {
                    fineMessage = getTextAt(RESOURCE_BUNDLE, "FactionCensureEvent.fine");
                }

                finances.debit(TransactionType.FINE, campaign.getLocalDate(), fine, fineMessage);

                processMassLoyaltyChange(campaign, false, false);
            }
            case LEADERSHIP_REPLACEMENT -> {
                processMassLoyaltyChange(campaign, true, false);
                processSeniorPersonnelConsequences(false);
            }
            case LEADERSHIP_IMPRISONED -> {
                processMassLoyaltyChange(campaign, true, false);
                processSeniorPersonnelConsequences(true);
            }
        }

        processFactionStandingChange(isSeppuku);
    }

    private void processClanTrial(boolean isSuccessful) {
        boolean useAdvancedMedical = campaign.getCampaignOptions().isUseAdvancedMedical();
        int commanderInjuries = isSuccessful ? 6 : randomInt(5) + 1;
        int secondInCommandInjuries = isSuccessful ? randomInt(3) + 1 : randomInt(6) + 1;
        if (useAdvancedMedical) {
            InjuryUtil.resolveCombatDamage(campaign, commander, commanderInjuries);
            if (commander.getInjuries().size() > 5) {
                commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
            }

            InjuryUtil.resolveCombatDamage(campaign, secondInCommand, secondInCommandInjuries);
        } else {
            int currentHits = commander.getHits();
            int newHits = currentHits + commanderInjuries;
            commander.setHits(newHits);
            if (newHits > 5) {
                commander.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
            }

            currentHits = secondInCommand.getHits();
            newHits = currentHits + secondInCommandInjuries;
            secondInCommand.setHits(newHits);
            if (newHits > 5 && !isSuccessful) {
                secondInCommand.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.KIA);
            }
        }

        if (isSuccessful) {
            if (secondInCommand.getRecruitment() == null) {
                campaign.recruitPerson(secondInCommand, true, true);
            }

            secondInCommand.setCommander(true);
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
        double delta = isMajor ? REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER : REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER;
        Faction faction = campaign.getFaction();
        String factionCode = faction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();
        String report = factionStandings.changeRegardForFaction(campaign.getFaction().getShortName(), factionCode,
              delta, campaign.getGameYear(), campaign.getCampaignOptions().getRegardMultiplier());

        campaign.addReport(GENERAL, report);
    }

    private void processSeniorPersonnelConsequences(boolean isImprisoned) {
        LocalDate today = campaign.getLocalDate();
        Set<Person> seniorPersonnel = getSeniorPersonnel(today);

        for (Person seniorPerson : seniorPersonnel) {
            final int level = seniorPerson.getRankLevel();
            final int rank = seniorPerson.getRankNumeric();

            seniorPerson.changeStatus(campaign, today, isImprisoned
                                                             ? PersonnelStatus.IMPRISONED
                                                             : PersonnelStatus.DISHONORABLY_DISCHARGED);

            if (!isImprisoned) {
                Person replacement = getReplacementCharacter(seniorPerson);
                replacement.changeRank(campaign, rank, level, false);
                campaign.recruitPerson(replacement, true, true);
            }
        }
    }

    private Set<Person> getSeniorPersonnel(LocalDate today) {
        Set<Person> seniorPersonnel = new HashSet<>();

        if (commander != null) {
            seniorPersonnel.add(commander);
        }

        if (secondInCommand != null) {
            seniorPersonnel.add(secondInCommand);
        }

        for (Person officer : campaign.getPersonnel()) {
            if (isExempt(officer, today)) {
                continue;
            }

            if (!officer.getRank().isOfficer()) {
                continue;
            }

            seniorPersonnel.add(officer);
        }

        return seniorPersonnel;
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
        boolean useExtraRandomness = campaign.getRandomSkillPreferences().randomizeSkill();
        boolean isUseArtillery = campaign.getCampaignOptions().isUseArtillery();

        PersonnelRole primaryRole = seniorPerson.getPrimaryRole();
        PersonnelRole politicalRole = getPoliticalRole();
        Person replacement = campaign.newPerson(primaryRole, politicalRole);

        overrideSkills(false,
              false,
              false,
              isUseArtillery,
              useExtraRandomness,
              replacement,
              primaryRole,
              VETERAN);

        overrideSkills(false,
              false,
              false,
              isUseArtillery,
              useExtraRandomness,
              replacement,
              politicalRole,
              VETERAN);

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
}
