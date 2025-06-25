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
import static mekhq.campaign.universe.factionStanding.FactionCensureEvent.POLITICAL_ROLES;
import static mekhq.campaign.universe.factionStanding.FactionCensureEvent.processMassLoyaltyChange;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.universe.Faction;
import mekhq.gui.dialog.factionStanding.factionJudgment.FactionCensureGoingRogueDialog;

/**
 * Handles the "going rogue" event for a campaign, where a force defects or leaves its current faction.
 *
 * <p>This class orchestrates the campaign logic when a player chooses to go rogue, possibly changing their campaign's
 * faction, modifying personnel statuses, and updating inter-faction standings. It utilizes a dialog to confirm and
 * process the event and performs all necessary changes to campaign data.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class GoingRogue {
    /** Target number for loyalty checks determining defection or loyalty. */
    private final static int LOYALTY_TARGET_NUMBER = 6;
    /** Die size used to resolve chances of homicide in defection scenarios. */
    private final static int MURDER_DIE_SIZE = 10;

    /** The campaign context where the event is processed. */
    private final Campaign campaign;
    /** Stores whether the user confirmed the "going rogue" action. */
    private final boolean wasConfirmed;

    /**
     * Returns whether the "going rogue" event was confirmed and applied.
     *
     * @return {@code true} if the event was confirmed; {@code false} otherwise.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean wasConfirmed() {
        return wasConfirmed;
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
        this.campaign = campaign;

        FactionCensureGoingRogueDialog dialog = new FactionCensureGoingRogueDialog(campaign);
        wasConfirmed = dialog.wasConfirmed();
        if (!wasConfirmed) {
            return;
        }

        Faction chosenFaction = dialog.getChosenFaction();
        if (chosenFaction == null) {
            return;
        }

        processGoingRogue(chosenFaction, commander, second);
    }

    /**
     * Carries out the narrative and data changes when the force goes rogue.
     *
     * <p>Changes personnel statuses, mass-loyalty, and adjusts faction standings.</p>
     *
     * @param chosenFaction the new faction may be the same or a new one
     * @param commander     the force commander
     * @param second        secondary command personnel
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processGoingRogue(Faction chosenFaction, Person commander, Person second) {
        boolean isDefection = !chosenFaction.isAggregate();
        processPersonnel(isDefection, commander, second);
        processMassLoyaltyChange(campaign, true, true);

        processFactionStandingChangeForOldFaction();
        processFactionStandingChangeForNewFaction(chosenFaction);

        campaign.setFaction(chosenFaction);
    }

    /**
     * Evaluates and updates all personnel in the campaign for defection, murder, or leaving statuses, based on
     * political roles and loyalty checks. The commander and second-in-command are exempted.
     *
     * @param isDefection whether this event counts as a defection to a new faction
     * @param commander   the commanding officer
     * @param second      the second-in-command
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processPersonnel(boolean isDefection, Person commander, Person second) {
        final LocalDate today = campaign.getLocalDate();
        Collection<Person> allPersonnel = campaign.getPersonnel();
        Set<Person> preProcessedPersonnel = new HashSet<>();
        preProcessedPersonnel.add(commander);
        preProcessedPersonnel.add(second);

        for (Person person : allPersonnel) {
            if (isExempt(person, today)) {
                continue;
            }

            if (preProcessedPersonnel.contains(person)) {
                continue;
            }

            // Political roles always become homicide victims in rogue events
            if (POLITICAL_ROLES.contains(person.getPrimaryRole())
                      || POLITICAL_ROLES.contains(person.getSecondaryRole())) {
                person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
                continue;
            }

            // Loyalty check: personnel with low loyalty may leave or be killed (homicide/left), others remain
            boolean loyaltyEnabled = campaign.getCampaignOptions().isUseLoyaltyModifiers();
            int loyalty = loyaltyEnabled ? person.getLoyalty() : 0;
            int modifier = loyaltyEnabled ? person.getLoyaltyModifier(loyalty) : 0;
            int roll = Compute.d6(2);

            if (roll < (LOYALTY_TARGET_NUMBER + modifier)) {
                person.changeStatus(campaign, today, isDefection ? PersonnelStatus.HOMICIDE : PersonnelStatus.LEFT);
            } else if (isDefection) {
                // Small chance a person still gets murdered when defecting
                roll = randomInt(MURDER_DIE_SIZE);
                if (roll == 0) {
                    person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
                }
            }

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
                    child.changeStatus(campaign, today, PersonnelStatus.LEFT);
                } else {
                    child.changeStatus(campaign, today, person.getStatus());
                }

                preProcessedPersonnel.add(child);
            }
        }
    }

    /**
     * Adjusts the campaign's standing with the old faction, if leaving, reducing regard to the minimum allowed for
     * {@link FactionStandingLevel#STANDING_LEVEL_1} if necessary.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processFactionStandingChangeForOldFaction() {
        Faction faction = campaign.getFaction();

        if (faction.isAggregate()) {
            return;
        }

        String factionCode = faction.getShortName();
        FactionStandings factionStandings = campaign.getFactionStandings();

        double targetRegard = FactionStandingLevel.STANDING_LEVEL_1.getMinimumRegard();
        double currentRegard = factionStandings.getRegardForFaction(factionCode, false);
        if (currentRegard < targetRegard) {
            return;
        }

        String report = factionStandings.setRegardForFaction(campaign.getFaction().getShortName(), factionCode, targetRegard, campaign.getGameYear(), true);
        campaign.addReport(report);
    }

    /**
     * Improves the campaign's standing with the new faction (if applicable), raising regard to at least the minimum
     * allowed for {@link FactionStandingLevel#STANDING_LEVEL_5}.
     *
     * @param newFaction the faction now joined
     *
     * @author Illiani
     * @since 0.50.07
     */
    private void processFactionStandingChangeForNewFaction(Faction newFaction) {
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

        String report = factionStandings.setRegardForFaction(campaign.getFaction().getShortName(), factionCode, targetRegard, campaign.getGameYear(), true);
        campaign.addReport(report);
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
