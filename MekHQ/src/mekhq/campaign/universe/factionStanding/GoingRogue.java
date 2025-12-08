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
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.MAXIMUM_STANDING_LEVEL;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.POLITICAL_ROLES;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.calculateFactionStandingLevel;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.processMassLoyaltyChange;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionCensureGoingRogueDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentDialog;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentNewsArticle;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionJudgmentSceneDialog;

/**
 * Handles the "going rogue" event for a campaign, where a force defects or leaves its current faction.
 *
 * <p>This class orchestrates the campaign logic when a player chooses to go rogue, possibly changing their campaign's
 * faction, modifying personnel statuses, and updating inter-faction standings. It uses a dialog to confirm and process
 * the event and performs all necessary changes to campaign data.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class GoingRogue {
    /** Target number for loyalty checks determining defection or loyalty. */
    private final static int LOYALTY_TARGET_NUMBER = 6;
    /** Die size used to resolve chances of homicide in defection scenarios. */
    private final static int MURDER_DIE_SIZE = 10;
    private final static String DEFECTION_GREETING_LOOKUP = "HELLO";
    private final static String DEFECTION_NEWS_ARTICLE_LOOKUP = "LEAVE";

    /** Stores whether the user confirmed the "going rogue" action. */
    private final boolean wasConfirmed;

    /**
     * Returns whether the "going rogue" event was canceled.
     *
     * @return {@code true} if the event was canceled; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasCanceled() {
        return !wasConfirmed;
    }

    /**
     * Constructs a {@code GoingRogue} event for the given campaign and key personnel.
     *
     * <p>The constructor shows a dialog to the user to select a new faction (if desired). If the action is confirmed,
     * it performs all consequences of a force going rogue—modifying personnel statuses and campaign faction, as well as
     * updating faction standings.</p>
     *
     * @param campaign  the campaign context
     * @param commander the commanding officer of the force
     * @param second    the second-in-command may be {@code null}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public GoingRogue(Campaign campaign, Person commander, @Nullable Person second) {
        boolean isUsingFactionStandings = campaign.getCampaignOptions().isTrackFactionStanding();
        FactionCensureGoingRogueDialog dialog = new FactionCensureGoingRogueDialog(campaign, isUsingFactionStandings);
        wasConfirmed = dialog.wasConfirmed();
        if (!wasConfirmed) {
            return;
        }

        Faction chosenFaction = dialog.getChosenFaction();
        if (chosenFaction == null) {
            return;
        }

        new FactionJudgmentSceneDialog(campaign,
              commander,
              second,
              FactionJudgmentSceneType.GO_ROGUE,
              campaign.getFaction());

        processGoingRogue(campaign, chosenFaction, commander, second, isUsingFactionStandings, false);
    }

    /**
     * Handles the processing of forces going rogue by transitioning their alignment to a new faction and determining
     * the nature of the event (defection or not). It delegates further handling and the consequences of other
     * specialized methods within the class.
     *
     * @param campaign                the current campaign context
     * @param chosenFaction           the new faction the force is aligning with; may be the same or different from the
     *                                old faction
     * @param commander               the commanding officer of the force
     * @param second                  the second-in-command, may be {@code null}
     * @param isUsingFactionStandings {@code true} if the player has faction standings enabled
     * @param isUltimatum             whether the 'going rogue' action was the result of an ultimatum (but not to the
     *                                mercenary faction)
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void processGoingRogue(Campaign campaign, Faction chosenFaction, Person commander,
          @Nullable Person second, boolean isUsingFactionStandings, boolean isUltimatum) {
        boolean isDefection = !chosenFaction.isAggregate() && !campaign.getFaction().isAggregate();

        processGoingRogue(campaign,
              chosenFaction,
              commander,
              second,
              isDefection,
              isUsingFactionStandings,
              isUltimatum);
    }

    /**
     * Carries out the narrative and data changes when the force goes rogue.
     *
     * <p>Changes personnel statuses, mass-loyalty, and adjusts faction standings.</p>
     *
     * @param campaign                the current campaign context
     * @param chosenFaction           the new faction may be the same or a new one
     * @param commander               the force commander
     * @param second                  secondary command personnel
     * @param isDefection             whether the 'going rogue' action counts as defection
     * @param isUltimatum             whether the 'going rogue' action was the result of an ultimatum
     * @param isUsingFactionStandings {@code true} if the player has faction standings enabled
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void processGoingRogue(Campaign campaign, Faction chosenFaction, Person commander,
          @Nullable Person second, boolean isDefection, boolean isUltimatum, boolean isUsingFactionStandings) {
        Faction currentFaction = campaign.getFaction();
        String chosenFactionCode = chosenFaction.getShortName();

        if (isUsingFactionStandings) {
            processPersonnel(campaign, isDefection, commander, second);

            if (currentFaction.equals(chosenFaction)) {
                processRegardBump(campaign);
            } else {
                processFactionStandingChangeForOldFaction(campaign);
            }
            processFactionStandingChangeForNewFaction(campaign, chosenFaction);
        }

        processMassLoyaltyChange(campaign, true, true);

        if (!isUltimatum) {
            new FactionJudgmentNewsArticle(campaign, commander, null, DEFECTION_NEWS_ARTICLE_LOOKUP, currentFaction,
                  FactionStandingJudgmentType.WELCOME, false, chosenFaction);
        }

        boolean isMercenaryFaction = MERCENARY_FACTION_CODE.equals(chosenFactionCode);
        Faction newFaction = chosenFaction;
        if (isMercenaryFaction) {
            newFaction = Faction.getActiveMercenaryOrganization(campaign.getGameYear());
        }

        if (!currentFaction.equals(chosenFaction)) {
            PersonnelRole role = chosenFaction.isClan() ? PersonnelRole.MEKWARRIOR : PersonnelRole.MILITARY_LIAISON;
            Person speaker = campaign.newPerson(role, chosenFactionCode, Gender.RANDOMIZE);
            new FactionJudgmentDialog(campaign, speaker, commander, DEFECTION_GREETING_LOOKUP, newFaction,
                  FactionStandingJudgmentType.WELCOME, ImmersiveDialogWidth.MEDIUM, null, null);
        }

        campaign.setFaction(chosenFaction);
    }

    /**
     * Evaluates and updates all personnel in the campaign for defection, murder, or leaving statuses, based on
     * political roles and loyalty checks. The commander and second-in-command are exempted.
     *
     * @param campaign    the current campaign context
     * @param isDefection whether this event counts as a defection to a new faction
     * @param commander   the commanding officer
     * @param second      the second-in-command
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processPersonnel(Campaign campaign, boolean isDefection, Person commander,
          @Nullable Person second) {
        final LocalDate today = campaign.getLocalDate();
        Collection<Person> allPersonnel = campaign.getPersonnel();
        Set<Person> preProcessedPersonnel = new HashSet<>();

        preProcessedPersonnel.add(commander);
        if (second != null) {
            preProcessedPersonnel.add(second);
        }

        for (Person person : allPersonnel) {
            if (isExempt(person, today)) {
                continue;
            }

            if (preProcessedPersonnel.contains(person)) {
                continue;
            }

            // Political roles always become homicide victims in defection events
            if (isDefection) {
                if (POLITICAL_ROLES.contains(person.getPrimaryRole())
                          || POLITICAL_ROLES.contains(person.getSecondaryRole())) {
                    person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
                    processGenealogicallyLinkedPersonnel(campaign, person, today, preProcessedPersonnel);
                    continue;
                }
            }

            // Loyalty check: personnel with low loyalty may leave or be killed (homicide/deserted), others remain
            boolean loyaltyEnabled = campaign.getCampaignOptions().isUseLoyaltyModifiers();
            boolean altAdvancedMedicalEnabled = campaign.getCampaignOptions().isUseAlternativeAdvancedMedical();
            int loyalty = loyaltyEnabled ?
                                person.getAdjustedLoyalty(campaign.getFaction(), altAdvancedMedicalEnabled) :
                                0;
            int modifier = loyaltyEnabled ? person.getLoyaltyModifier(loyalty) : 0;
            int roll = Compute.d6(2);

            if (roll < (LOYALTY_TARGET_NUMBER + modifier)) {
                person.changeStatus(campaign, today, isDefection ? PersonnelStatus.HOMICIDE : PersonnelStatus.DESERTED);
            } else if (isDefection) {
                // Small chance a person still gets murdered when defecting
                roll = randomInt(MURDER_DIE_SIZE);
                if (roll == 0) {
                    person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
                }
            }

            processGenealogicallyLinkedPersonnel(campaign, person, today, preProcessedPersonnel);
        }
    }

    /**
     * Processes all genealogically linked personnel (spouse and children) of the specified person, updating their
     * statuses in accordance with event resolution logic.
     *
     * <p>When we determine how a character will react to the campaign going rogue, their spouse will automatically
     * adopt the same status, ensuring relationship continuity. All such processed spouses are added to the
     * {@code preProcessedPersonnel} set to prevent duplicate handling.</p>
     *
     * <p>Each child of the person is also processed as follows:</p>
     * <ul>
     *   <li>If a child is a minor at the specified date, their status is set to {@code PersonnelStatus.LEFT} unless
     *   their parent was killed at which point they will remain with the campaign but suffer a loyalty penalty.</li>
     *   <li>For adult children, their status will mirror the fate of their parents.</li>
     *   <li>All children processed are added to {@code preProcessedPersonnel}.</li>
     * </ul>
     *
     * @param campaign              the current campaign context
     * @param person                the {@link Person} whose genealogical relations are to be processed
     * @param today                 the current {@link LocalDate} for age/status determination
     * @param preProcessedPersonnel a set of {@link Person} objects already processed during this operation
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processGenealogicallyLinkedPersonnel(Campaign campaign, Person person, LocalDate today,
          Set<Person> preProcessedPersonnel) {
        Genealogy genealogy = person.getGenealogy();
        Person spouse = genealogy.getSpouse();
        List<Person> children = genealogy.getChildren();

        // Spouses follow each other to their fate. This prevents us from needing to add handlers for split
        // relationships
        if (spouse != null) {
            spouse.changeStatus(campaign, today, person.getStatus());
            preProcessedPersonnel.add(spouse);
        }

        // Non-adult children follow their parents if their parents are still alive, otherwise they awkwardly
        // remain with the campaign. Their fate is left up to the player. Adult children follow the fate of their
        // parents.
        for (Person child : children) {
            if (child.isChild(today)) {
                if (person.getStatus().isDeserted()) {
                    child.changeStatus(campaign, today, PersonnelStatus.DESERTED);
                } else {
                    child.performForcedDirectionLoyaltyChange(campaign, false, true, true);
                }
            } else {
                child.changeStatus(campaign, today, person.getStatus());
            }

            preProcessedPersonnel.add(child);
        }
    }


    /**
     * Processes the standing change for the current campaign with its old faction, using the campaign's default
     * faction.
     *
     * @param campaign the current campaign context
     */
    private static void processFactionStandingChangeForOldFaction(Campaign campaign) {
        processFactionStandingChangeForOldFaction(campaign, campaign.getFaction());
    }

    /**
     * Adjusts the campaign's standing with the old faction, if leaving, reducing regard to the minimum allowed for
     * {@link FactionStandingLevel#STANDING_LEVEL_1} if necessary.
     *
     * @param campaign   the current campaign context
     * @param oldFaction the faction the campaign is departing
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void processFactionStandingChangeForOldFaction(Campaign campaign, Faction oldFaction) {
        if (oldFaction.isAggregate()) {
            return;
        }

        String factionCode = oldFaction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();

        double targetRegard = FactionStandingLevel.STANDING_LEVEL_1.getMinimumRegard();
        double currentRegard = factionStandings.getRegardForFaction(factionCode, false);
        if (currentRegard <= targetRegard) {
            return;
        }

        String report = factionStandings.setRegardForFaction(campaign.getFaction().getShortName(),
              factionCode,
              targetRegard,
              campaign.getGameYear(),
              true);
        campaign.addReport(GENERAL, report);
    }

    /**
     * Increases the standing (regard) of the campaign's active faction by a single level if possible, as part of
     * resolving a Faction Standing ultimatum event.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Checks that the faction is not aggregate (i.e., not a combined or abstract group).</li>
     *   <li>Calculates the next standing level for the faction, if above the current level and within the maximum
     *   allowed.</li>
     *   <li>If the faction's current regard is below the minimum threshold for the next standing level, updates it
     *   to exactly that threshold.</li>
     *   <li>Generates and adds an appropriate standing report to the campaign.</li>
     *   <li>If the faction is already at or above the new target level, or at the maximum standing, no changes are
     *   made.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} instance whose faction standing should be bumped up in response to an
     *                 ultimatum
     */
    public static void processRegardBump(Campaign campaign) {
        Faction faction = campaign.getFaction();
        if (faction.isAggregate()) {
            return;
        }

        String factionCode = faction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();
        double currentRegard = factionStandings.getRegardForFaction(factionCode, false);
        FactionStandingLevel currentStanding = calculateFactionStandingLevel(currentRegard);
        int nextLevel = currentStanding.getStandingLevel() + 1;

        if (nextLevel > MAXIMUM_STANDING_LEVEL) {
            return;
        }

        FactionStandingLevel newStanding = FactionStandingLevel.fromString(String.valueOf(nextLevel));
        if (newStanding == null) {
            // Defensive: Can't find the next standing
            return;
        }

        double targetRegard = newStanding.getMinimumRegard();
        if (currentRegard >= targetRegard) {
            // No bump needed
            return;
        }

        String report = factionStandings.setRegardForFaction(
              campaign.getFaction().getShortName(),
              factionCode,
              targetRegard,
              campaign.getGameYear(),
              true
        );
        campaign.addReport(GENERAL, report);
    }

    /**
     * Improves the campaign's standing with the new faction (if applicable), raising regard to at least the minimum
     * allowed for {@link FactionStandingLevel#STANDING_LEVEL_5}.
     *
     * @param campaign   the current campaign context
     * @param newFaction the faction now joined
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processFactionStandingChangeForNewFaction(Campaign campaign, Faction newFaction) {
        if (newFaction.isAggregate()) {
            return;
        }

        String factionCode = newFaction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();

        double targetRegard = FactionStandingLevel.STANDING_LEVEL_5.getMinimumRegard();
        double currentRegard = factionStandings.getRegardForFaction(factionCode, false);
        if (currentRegard >= targetRegard) {
            return;
        }

        String report = factionStandings.setRegardForFaction(campaign.getFaction().getShortName(),
              factionCode,
              targetRegard,
              campaign.getGameYear(),
              true);
        campaign.addReport(GENERAL, report);
    }

    /**
     * Determines if a person is exempt from loyalty/status change checks during a rogue event.
     *
     * @param person the person to evaluate
     * @param today  the date of evaluation
     *
     * @return {@code true} if the person is exempt; {@code false} otherwise
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
