/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.randomEvents.prisoners;

import megamek.common.Compute;
import megamek.common.ITechnology;
import megamek.common.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.enums.HonorRating;
import mekhq.gui.dialog.DefectionOffer;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.round;
import static megamek.common.Board.T_SPACE;
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static megamek.common.MiscType.createBeagleActiveProbe;
import static megamek.common.MiscType.createCLImprovedSensors;
import static megamek.common.MiscType.createISImprovedSensors;
import static mekhq.campaign.personnel.enums.PersonnelStatus.BONDSREF;
import static mekhq.campaign.personnel.enums.PersonnelStatus.DEFECTED;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ENEMY_BONDSMAN;
import static mekhq.campaign.personnel.enums.PersonnelStatus.POW;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.BECOMING_BONDSMAN;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * Handles events and processes related to capturing prisoners.
 *
 * <p>This class manages the capture mechanics for both NPCs and player characters, based on
 * SAR (Search and Rescue) quality, faction alignment, and campaign rules. It applies various
 * modifiers to determine the likelihood of successfully capturing prisoners and processes
 * their capture outcome (e.g., statuses such as prisoner, bondsman, or defector).</p>
 *
 * <p>The class supports different capture styles (e.g., MekHQ Capture Mode) and adjusts outcomes
 * based on honor ratings, faction-specific rules (e.g., Clan or IS), and campaign configurations.</p>
 *
 * <p>It also accounts for faction-specific behaviors, such as Clan dezgra multipliers and bondsman
 * mechanics, and provides support for defection offers.</p>
 */
public class CapturePrisoners {
    private final Campaign campaign;
    private final Faction searchingFaction;
    private final boolean searchingFactionIsClan;

    private final int ATTEMPT_COUNT = 3; // This will need tweaking till we're happy with the result
    private final int DEFECTION_CHANCE = 100;
    private final double MERCENARY_MULTIPLIER = 0.95;
    private final int CLAN_DEZGRA_MULTIPLIER = 5;

    // SAR Modifiers (based on CamOps pg 223)
    final int HAS_BATTLEFIELD_CONTROL = 1;
    final int GOING_TO_GROUND = -4;

    final int SAR_CONTAINS_VTOL_OR_WIGE = 1;
    final int SAR_HAS_IMPROVED_SENSORS = 2; // largest only
    final int SAR_HAS_ACTIVE_PROBE = 1; // largest only

    final int NOT_IN_PLANET_ORBIT = -2;
    final int SAR_INCLUDES_DROPSHIP = 2; // largest only

    private TargetRoll sarTargetNumber = new TargetRoll(8, "Base TN"); // Target Number (CamOps pg 223)

    /**
     * Constructs a {@link CapturePrisoners} object and initializes modifiers
     * based on the faction, scenario, and SAR (Search and Rescue) qualities.
     *
     * <p>This constructor applies SAR-related modifiers, including checking for active probes,
     * improved sensors, VTOL/DropShips, and orbit-related penalties. It also ensures proper
     * faction-related checks, such as whether the searching faction is Clan, and sets the
     * appropriate base target rolls for capturing prisoners.</p>
     *
     * @param campaign          The active {@link Campaign} being played.
     * @param searchingFaction  The {@link Faction} conducting the prisoner search.
     * @param scenario          The {@link Scenario} representing the current mission or battle.
     * @param sarQuality        Search and Rescue quality level, affecting capture difficulty.
     */
    public CapturePrisoners(Campaign campaign, Faction searchingFaction, Scenario scenario, int sarQuality) {
        this.campaign = campaign;
        this.searchingFaction = searchingFaction;

        sarTargetNumber.addModifier(GOING_TO_GROUND, "Potential Prisoner Going to Ground");
        sarTargetNumber.addModifier(HAS_BATTLEFIELD_CONTROL, "Searcher Has Battlefield Control");

        int today = campaign.getLocalDate().getYear();
        searchingFactionIsClan = searchingFaction != null && searchingFaction.isClan();

        int techFaction = searchingFactionIsClan ? ITechnology.getCodeFromMMAbbr("CLAN") : ITechnology.getCodeFromMMAbbr("IS");
        try {
            // searchingFaction being null is fine because we're just ignoring any exceptions
            techFaction = ITechnology.getCodeFromMMAbbr(searchingFaction.getShortName());
        } catch (Exception ignored) {
            // if we can't get the tech faction, we just use the fallbacks already assigned.
        }

        if (scenario.getBoardType() == T_SPACE) {
            sarTargetNumber.addModifier(NOT_IN_PLANET_ORBIT, "Not in Planet Orbit");
            sarTargetNumber.addModifier(SAR_INCLUDES_DROPSHIP, "SAR Includes DropShip");
        } else {
            sarTargetNumber.addModifier(SAR_CONTAINS_VTOL_OR_WIGE, "SAR Contains VTOL or WIGE");
            sarTargetNumber.addModifier(SAR_INCLUDES_DROPSHIP, "SAR Includes DropShip");


            final int isImprovedSensorsAvailability = createISImprovedSensors().calcYearAvailability(
                today, searchingFactionIsClan, techFaction);
            final int clanImprovedSensorsAvailability = createCLImprovedSensors().calcYearAvailability(
                today, searchingFactionIsClan, techFaction);

            final int improvedSensorsAvailability = searchingFactionIsClan ? clanImprovedSensorsAvailability : isImprovedSensorsAvailability;

            final int activeProbeAvailability = createBeagleActiveProbe().calcYearAvailability(
                today, searchingFactionIsClan, techFaction);

            if (sarQuality >= improvedSensorsAvailability) {
                sarTargetNumber.addModifier(SAR_HAS_IMPROVED_SENSORS, "SAR has Improved Sensors");
            } else if (sarQuality >= activeProbeAvailability) {
                sarTargetNumber.addModifier(SAR_HAS_ACTIVE_PROBE, "SAR has Active Probe");
            }
        }
    }

    /**
     * Attempts to determine the capture of an NPC prisoner.
     *
     * @param wasPickedUp Whether the target prisoner was already picked up.
     * @return {@code true} if the prisoner capture was successful, otherwise {@code false}.
     */
    public boolean attemptCaptureOfNPC(boolean wasPickedUp) {
        if (wasPickedUp) {
            return true;
        }

        return rollForCapture();
    }

    /**
     * Processes the capture of an NPC prisoner.
     *
     * <p>This method evaluates the capture style and adjusts the handling of the prisoner
     * accordingly. It accounts for different capture styles, such as MekHQ's mode, and processes
     * unique rules for Clan-related factions.</p>
     *
     * @param prisoner The {@link Person} object representing the captured NPC.
     */
    public void processCaptureOfNPC(Person prisoner) {
        PrisonerCaptureStyle prisonerCaptureStyle = campaign.getCampaignOptions().getPrisonerCaptureStyle();
        boolean isMekHQCaptureStyle = prisonerCaptureStyle.isMekHQ();
        Faction prisonerFaction = prisoner.getOriginFaction();
        Faction campaignFaction = campaign.getFaction();

        // if the campaign faction is Clan, we do things a little differently
        if (campaignFaction.isClan()) {
            processPrisoner(prisoner, campaignFaction, isMekHQCaptureStyle, true);

            handlePostCapture(prisoner, prisoner.getPrisonerStatus());
            return;
        }

        // If MekHQ Capture Style is disabled, we can use a shortcut
        if (!isMekHQCaptureStyle) {
            if (prisonerFaction.isClan()) {
                processPrisoner(prisoner, prisonerFaction, false, true);
            } else {
                prisoner.setPrisonerStatus(campaign, PRISONER, true);
            }

            handlePostCapture(prisoner, prisoner.getPrisonerStatus());

            return;
        }

        // Otherwise, we attempt defection
        int defectionRoll = attemptDefection(prisoner, true);

        if (defectionRoll == 0) {
            processPrisoner(prisoner, prisonerFaction, true, true);

            PrisonerStatus newStatus = prisoner.getPrisonerStatus();
            if (newStatus.isBecomingBondsman() || newStatus.isPrisonerDefector()) {
                new DefectionOffer(campaign, prisoner, prisoner.isClanPersonnel());
            }
        } else {
            prisoner.setPrisonerStatus(campaign, PRISONER, true);
        }

        handlePostCapture(prisoner, prisoner.getPrisonerStatus());
    }

    /**
     * Processes the outcome for a captured prisoner based on faction-related logic.
     *
     * <p>The method determines if the prisoner should be made a bondsman, added as a prisoner of
     * war, or handled according to ruler rules defined for the faction. It differentiates between
     * NPCs and player characters.</p>
     *
     * @param prisoner               The {@link Person} being processed.
     * @param faction                The {@link Faction} processing the prisoner.
     * @param isMekHQCaptureStyle    Indicates whether MekHQ's custom capture style is active.
     * @param isNPC                  {@code true} if the prisoner is an NPC, otherwise {@code false}.
     */
    private void processPrisoner(Person prisoner, Faction faction, boolean isMekHQCaptureStyle, boolean isNPC) {
        LocalDate today = campaign.getLocalDate();
        HonorRating honorRating = faction.getHonorRating(campaign);

        int bondsmanRoll = d6();
        if (faction.isClan()) {
            if (isMekHQCaptureStyle && prisoner.isClanPersonnel() && (bondsmanRoll == 1)) {
                prisoner.changeStatus(campaign, today, BONDSREF);
                return;
            } else if (d6() >= honorRating.getBondsmanTargetNumber()) {
                if (isNPC) {
                    prisoner.setPrisonerStatus(campaign, BECOMING_BONDSMAN, true);
                    prisoner.setBecomingBondsmanEndDate(today.plusWeeks(d6()));
                } else {
                    prisoner.changeStatus(campaign, today, ENEMY_BONDSMAN);
                }
                return;
            }
        }

        if (isNPC) {
            prisoner.setPrisonerStatus(campaign, PRISONER, true);
        } else {
            prisoner.changeStatus(campaign, today, POW);
        }
    }

    /**
     * Handles the defection mechanics for a captured prisoner.
     *
     * <p>This method calculates the base likelihood of defection and adjusts the probabilities
     * based on faction traits, such as whether the faction is mercenary or subject to Clan-specific
     * dezgra rules. It also accounts for NPCs or player-specific conditions in the defection process.</p>
     *
     * @param potentialDefector The {@link Person} object representing the potential defector.
     * @param isNPC             {@code true} if the defection attempt is for an NPC, otherwise {@code false}.
     * @return The defection roll value, where {@code 0} indicates a successful defection.
     */
    private int attemptDefection(Person potentialDefector, boolean isNPC) {
        int adjustedDefectionChance = DEFECTION_CHANCE;

        if (potentialDefector.getOriginFaction().isMercenary()) {
            adjustedDefectionChance *= (int) round(adjustedDefectionChance * MERCENARY_MULTIPLIER);
        }

        if (potentialDefector.isClanPersonnel()) {
            if (isNPC) {
                Faction campaignFaction = campaign.getFaction();
                if (campaignFaction.isPirate() || campaignFaction.isMercenary()) {
                    adjustedDefectionChance *= CLAN_DEZGRA_MULTIPLIER;
                }
            } else {
                if (searchingFaction.isPirate() || searchingFaction.isMercenary()) {
                    adjustedDefectionChance *= CLAN_DEZGRA_MULTIPLIER;
                }
            }
        }

        return randomInt(adjustedDefectionChance);
    }

    /**
     * Attempts to capture or determine the fate of a player character prisoner.
     *
     * <p>If the capture attempt fails, the player character is marked as missing in action (MIA).
     * Otherwise, the prisoner is processed further, using either standard or MekHQ-specific capture
     * rules. Defection rolls are applied if applicable, and post-capture events are handled.</p>
     *
     * @param prisoner     The {@link Person} representing the player-character prisoner.
     * @param wasPickedUp  Whether the prisoner was picked up as part of the scenario outcome.
     */
    public void attemptCaptureOfPlayerCharacter(Person prisoner, boolean wasPickedUp) {
        // Attempt capture
        boolean captureSuccessful = wasPickedUp;

        if (!captureSuccessful) {
            captureSuccessful = rollForCapture();
        }

        // Early exit is capture was unsuccessful
        if (!captureSuccessful) {
            prisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.MIA);
            return;
        }

        PrisonerCaptureStyle prisonerCaptureStyle = campaign.getCampaignOptions().getPrisonerCaptureStyle();
        boolean isMekHQCaptureStyle = prisonerCaptureStyle.isMekHQ();
        Faction prisonerFaction = prisoner.getOriginFaction();

        if (searchingFactionIsClan) {
            Faction campaignFaction = campaign.getFaction();
            processPrisoner(prisoner, campaignFaction, isMekHQCaptureStyle, false);
            return;
        }

        if (!isMekHQCaptureStyle) {
            if (prisonerFaction.isClan()) {
                processPrisoner(prisoner, prisonerFaction, false, false);
            } else {
                prisoner.changeStatus(campaign, campaign.getLocalDate(), POW);
            }

            return;
        }

        // Otherwise, we attempt defection
        int defectionRoll = attemptDefection(prisoner, false);

        if (defectionRoll == 0) {
            processPrisoner(prisoner, prisonerFaction, true, false);

            PrisonerStatus newStatus = prisoner.getPrisonerStatus();
            if (newStatus.isBecomingBondsman() || newStatus.isPrisonerDefector()) {
                new DefectionOffer(campaign, prisoner, prisoner.isClanPersonnel());
            }
        } else {
            prisoner.changeStatus(campaign, campaign.getLocalDate(), DEFECTED);
        }
    }

    /**
     * Rolls dice to determine if a prisoner is successfully captured based on current modifiers.
     *
     * <p>This method makes multiple attempts (based on {@code ATTEMPT_COUNT}) to roll dice against
     * a target number calculated from SAR modifiers and campaign settings.</p>
     *
     * @return {@code true} if any roll meets or exceeds the target number, otherwise {@code false}.
     */
    private boolean rollForCapture() {
        int targetNumber = sarTargetNumber.getValue();
        for (int attempt = 0; attempt < ATTEMPT_COUNT; attempt++) {
            int roll = d6(2);

            if (roll >= targetNumber) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handles post-capture adjustments and records for a newly captured prisoner.
     *
     * <p>This includes loyalty adjustments, prisoner recruitment, and campaign interaction logging
     * (e.g., offers of defection or bondsman status).</p>
     *
     * @param prisoner   The {@link Person} being processed as a captured prisoner.
     * @param newStatus  The resulting {@link PrisonerStatus} of the prisoner post-capture.
     */
    private void handlePostCapture(Person prisoner, PrisonerStatus newStatus) {
        final String RESOURCE_BUNDLE = "mekhq.resources.DefectionOffer";

        // non-clan prisoners should generate with lower than average loyalty, so drop the highest roll
        if (!newStatus.isBecomingBondsman()) {
            List<Integer> rolls = new ArrayList<>();

            for (int roll = 0; roll < 4; roll++) {
                rolls.add(Compute.d6(1));
            }

            Collections.sort(rolls);

            prisoner.setLoyalty(rolls.get(0) + rolls.get(1) + rolls.get(2));
        }

        // 'Recruit' prisoner
        PrisonerStatus prisonerStatus = prisoner.getPrisonerStatus();
        campaign.recruitPerson(prisoner, prisonerStatus);

        if (prisonerStatus.isPrisonerDefector()) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "defection.report",
                prisoner.getHyperlinkedName()));
        }

        if (prisonerStatus.isBecomingBondsman()) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "bondsman.report",
                prisoner.getHyperlinkedName()));
        }
    }
}
