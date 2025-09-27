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
package mekhq.campaign.randomEvents.prisoners;

import static megamek.common.equipment.MiscType.createBeagleActiveProbe;
import static megamek.common.equipment.MiscType.createCLImprovedSensors;
import static megamek.common.equipment.MiscType.createISImprovedSensors;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.campaign.personnel.enums.PersonnelStatus.BONDSREF;
import static mekhq.campaign.personnel.enums.PersonnelStatus.DEFECTED;
import static mekhq.campaign.personnel.enums.PersonnelStatus.ENEMY_BONDSMAN;
import static mekhq.campaign.personnel.enums.PersonnelStatus.KIA;
import static mekhq.campaign.personnel.enums.PersonnelStatus.MIA;
import static mekhq.campaign.personnel.enums.PersonnelStatus.POW;
import static mekhq.campaign.personnel.enums.PersonnelStatus.SEPPUKU;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.BECOMING_BONDSMAN;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER;
import static mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus.PRISONER_DEFECTOR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.interfaces.ITechnology;
import megamek.common.loaders.MapSettings;
import megamek.common.rolls.TargetRoll;
import megamek.common.universe.HonorRating;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerCaptureStyle;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.ReportingUtilities;

/**
 * Handles events and processes related to capturing prisoners.
 *
 * <p>This class manages the capture mechanics for both NPCs and player characters, based on
 * SAR (Search and Rescue) quality, faction alignment, and campaign rules. It applies various modifiers to determine the
 * likelihood of successfully capturing prisoners and processes their capture outcome (e.g., statuses such as prisoner,
 * bondsman, or defector).</p>
 *
 * <p>The class supports different capture styles (e.g., MekHQ Capture Mode) and adjusts outcomes
 * based on honor ratings, faction-specific rules (e.g., Clan or IS), and campaign configurations.</p>
 *
 * <p>It also accounts for faction-specific behaviors, such as Clan dezgra multipliers and bondsman
 * mechanics, and provides support for defection offers.</p>
 */
public class CapturePrisoners {
    private static final MMLogger LOGGER = MMLogger.create(CapturePrisoners.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

    private final Campaign campaign;
    private final Faction searchingFaction;
    private final boolean searchingFactionIsClan;

    private final int ATTEMPT_COUNT = 2; // This will need tweaking till we're happy with the result
    final static int DEFECTION_CHANCE = 100;
    final static double MERCENARY_MULTIPLIER = 0.80;
    final static int CLAN_DEZGRA_MULTIPLIER = 5;

    // SAR Modifiers (based on CamOps pg 223)
    final static int HAS_BATTLEFIELD_CONTROL = -1;
    final static int GOING_TO_GROUND = 4;

    // Ground
    final static int SAR_CONTAINS_VTOL_OR_WIGE = -1;
    final static int SAR_HAS_IMPROVED_SENSORS = -2; // largest only
    final static int SAR_HAS_ACTIVE_PROBE = -1; // largest only

    // Space
    final static int NOT_IN_PLANET_ORBIT = 2;
    final static int SAR_INCLUDES_DROPSHIP = -2;

    final static int BASE_TARGET_NUMBER = 8; // Base Target Number (CamOps pg 223)
    private final TargetRoll sarTargetNumber = new TargetRoll(BASE_TARGET_NUMBER, "Base TN");

    /**
     * Constructs a {@link CapturePrisoners} object and initializes modifiers based on the faction, scenario, and SAR
     * (Search and Rescue) qualities.
     *
     * <p>This constructor applies SAR-related modifiers, including checking for active probes,
     * improved sensors, VTOL/DropShips, and orbit-related penalties. It also ensures proper faction-related checks,
     * such as whether the searching faction is Clan, and sets the appropriate base target rolls for capturing
     * prisoners.</p>
     *
     * @param campaign         The active {@link Campaign} being played.
     * @param searchingFaction The {@link Faction} conducting the prisoner search.
     * @param scenario         The {@link Scenario} representing the current mission or battle.
     * @param sarQuality       Search and Rescue quality level, affecting capture difficulty.
     */
    public CapturePrisoners(Campaign campaign, @Nullable Faction searchingFaction, Scenario scenario, int sarQuality) {
        this.campaign = campaign;

        if (searchingFaction == null) {
            searchingFaction = Factions.getInstance().getFaction("MERC");
        }
        this.searchingFaction = searchingFaction;

        sarTargetNumber.addModifier(HAS_BATTLEFIELD_CONTROL, "Searcher Has Battlefield Control");

        int today = campaign.getLocalDate().getYear();
        searchingFactionIsClan = searchingFaction != null && searchingFaction.isClan();

        megamek.common.enums.Faction techFaction = searchingFactionIsClan ?
                                                         ITechnology.getFactionFromMMAbbr("CLAN") :
                                                         ITechnology.getFactionFromMMAbbr("IS");
        try {
            if (searchingFaction != null) {
                techFaction = ITechnology.getFactionFromMMAbbr(searchingFaction.getShortName());
            }
        } catch (Exception ignored) {
            // if we can't get the tech faction, we just use the fallbacks already assigned.
        }

        if (scenario.getBoardType() == MapSettings.MEDIUM_SPACE) {
            // It doesn't make sense for a character to 'go to ground' in space. Where are they
            // going to go when their air runs out?
            sarTargetNumber.addModifier(NOT_IN_PLANET_ORBIT, "Not in Planet Orbit");
            sarTargetNumber.addModifier(SAR_INCLUDES_DROPSHIP, "SAR Includes DropShip");
        } else {
            sarTargetNumber.addModifier(GOING_TO_GROUND, "Potential Prisoner Going to Ground");
            sarTargetNumber.addModifier(SAR_CONTAINS_VTOL_OR_WIGE, "SAR Contains VTOL or WIGE");

            final AvailabilityValue isImprovedSensorsAvailability = createISImprovedSensors().calcYearAvailability(today,
                  searchingFactionIsClan,
                  techFaction);
            final AvailabilityValue clanImprovedSensorsAvailability = createCLImprovedSensors().calcYearAvailability(
                  today,
                  searchingFactionIsClan,
                  techFaction);

            final AvailabilityValue improvedSensorsAvailability = searchingFactionIsClan ?
                                                                        clanImprovedSensorsAvailability :
                                                                        isImprovedSensorsAvailability;

            final AvailabilityValue activeProbeAvailability = createBeagleActiveProbe().calcYearAvailability(today,
                  searchingFactionIsClan,
                  techFaction);

            // TODO: sarQuality is evaluated against the index of a AvailabilityValue. doesn't seems very nice. Refactor the whole constructor.
            if (sarQuality >= improvedSensorsAvailability.getIndex()) {
                sarTargetNumber.addModifier(SAR_HAS_IMPROVED_SENSORS, "SAR has Improved Sensors");
            } else if (sarQuality >= activeProbeAvailability.getIndex()) {
                sarTargetNumber.addModifier(SAR_HAS_ACTIVE_PROBE, "SAR has Active Probe");
            }
        }

        LOGGER.info(sarTargetNumber.toString());
    }

    /**
     * Retrieves the target roll number used for Search and Rescue (SAR) operations to determine the success of
     * capturing prisoners.
     *
     * @return The {@link TargetRoll} object representing the SAR target number.
     */
    TargetRoll getSarTargetNumber() {
        return sarTargetNumber;
    }

    /**
     * Attempts to determine the capture of an NPC prisoner.
     *
     * @param wasPickedUp Whether the target prisoner was already picked up.
     *
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
     * accordingly. It accounts for different capture styles, such as MekHQ's mode, and processes unique rules for
     * Clan-related factions.</p>
     *
     * @param prisoner The {@link Person} object representing the captured NPC.
     */
    public void processCaptureOfNPC(Person prisoner) {
        PrisonerCaptureStyle prisonerCaptureStyle = campaign.getCampaignOptions().getPrisonerCaptureStyle();

        if (prisonerCaptureStyle.isNone()) {
            return;
        }

        Faction campaignFaction = campaign.getFaction();
        processPrisoner(prisoner, campaignFaction, prisonerCaptureStyle.isMekHQ(), true);

        // Have they been removed via Bondsref or Seppuku?
        if (prisoner.getStatus().isDead()) {
            return;
        }

        // If MekHQ Capture Style is disabled, we can use a shortcut
        if (!prisonerCaptureStyle.isMekHQ() || prisoner.getPrisonerStatus().isBecomingBondsman()) {
            handlePostCapture(prisoner, prisoner.getPrisonerStatus());
            return;
        }

        // Attempt defection
        if (!campaignFaction.isClan()) {
            int defectionChance = determineDefectionChance(prisoner, true);

            if (randomInt(defectionChance) == 0) {
                if (prisoner.isClanPersonnel()) {
                    prisoner.setPrisonerStatus(campaign, BECOMING_BONDSMAN, true);

                    LocalDate today = campaign.getLocalDate();
                    prisoner.setBecomingBondsmanEndDate(today.plusWeeks(d6(1)));
                } else {
                    prisoner.setPrisonerStatus(campaign, PRISONER_DEFECTOR, false);
                }

                boolean isBondsman = prisoner.isClanPersonnel();
                new ImmersiveDialogSimple(campaign,
                      campaign.getSeniorAdminPerson(HR),
                      null,
                      createInCharacterMessage(prisoner, isBondsman),
                      null,
                      getFormattedTextAt(RESOURCE_BUNDLE, (isBondsman ? "bondsman" : "defector") + ".ooc"),
                      null,
                      false);
            }
        }

        handlePostCapture(prisoner, prisoner.getPrisonerStatus());
    }


    /**
     * Generates the in-character message for the dialog based on the defection offer.
     *
     * <p>This message customizes its narrative based on the type of defector
     * (standard or bondsman). It provides details about the prisoner, their origin faction, and their offer to defect,
     * addressing the player by their in-game title.</p>
     *
     * @param defector   The prisoner making the defection offer.
     * @param isBondsman {@code true} if the defector is a bondsman, {@code false} otherwise.
     *
     * @return A formatted string containing the immersive in-character message for the player.
     */
    private String createInCharacterMessage(Person defector, boolean isBondsman) {
        String typeKey = isBondsman ? "bondsman" : "defector";
        String commanderAddress = campaign.getCommanderAddress();

        if (isBondsman) {
            String originFaction = defector.getOriginFaction().getFullName(campaign.getGameYear());

            if (!originFaction.contains("Clan")) {
                originFaction = "The " + originFaction;
            }
            return getFormattedTextAt(RESOURCE_BUNDLE,
                  typeKey + ".message",
                  commanderAddress,
                  defector.getFullName(),
                  originFaction,
                  defector.getFirstName());
        }

        return getFormattedTextAt(RESOURCE_BUNDLE,
              typeKey + ".message",
              commanderAddress,
              defector.getFullName(),
              defector.getOriginFaction().getFullName(campaign.getGameYear()));
    }

    /**
     * Processes the outcome for a captured prisoner based on faction-related logic.
     *
     * <p>The method determines if the prisoner should be made a bondsman, added as a prisoner of
     * war, or handled according to ruler rules defined for the faction. It differentiates between NPCs and player
     * characters.</p>
     *
     * @param prisoner            The {@link Person} being processed.
     * @param capturingFaction    The {@link Faction} processing the prisoner.
     * @param isMekHQCaptureStyle Indicates whether MekHQ's custom capture style is active.
     * @param isNPC               {@code true} if the prisoner is an NPC, otherwise {@code false}.
     */
    void processPrisoner(Person prisoner, @Nullable Faction capturingFaction, boolean isMekHQCaptureStyle,
          boolean isNPC) {
        LocalDate today = campaign.getLocalDate();
        HonorRating prisonerHonorRating = prisoner.getOriginFaction().getHonorRating(campaign);

        int bondsmanRoll = d6(1);
        if (prisoner.isClanPersonnel()) {
            if (capturingFaction != null && capturingFaction.isClan()) {
                if (isMekHQCaptureStyle && (bondsmanRoll + d6(1) == 2)) {
                    if (isNPC) {
                        campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                              "bondsref.report",
                              prisoner.getFullName(),
                              spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                              CLOSING_SPAN_TAG));
                        prisoner.setStatus(BONDSREF);
                    } else {
                        prisoner.changeStatus(campaign, today, BONDSREF);
                    }
                    return;
                } else if (d6(1) >= prisonerHonorRating.getBondsmanTargetNumber()) {
                    if (isNPC) {
                        prisoner.setPrisonerStatus(campaign, BECOMING_BONDSMAN, true);
                        prisoner.setBecomingBondsmanEndDate(today.plusWeeks(d6(1)));
                    } else {
                        prisoner.changeStatus(campaign, today, ENEMY_BONDSMAN);
                    }
                    return;
                }
            } else if (capturingFaction != null && capturingFaction.getHonorRating(campaign) == HonorRating.NONE) {
                if (bondsmanRoll == 1) {
                    if (isNPC) {
                        campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                              "bondsref.report",
                              prisoner.getFullName(),
                              spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                              CLOSING_SPAN_TAG));

                        prisoner.setStatus(BONDSREF);
                    } else {
                        prisoner.changeStatus(campaign, today, BONDSREF);
                    }
                    return;
                }
            }
        }

        if (isMekHQCaptureStyle) {
            if (Objects.equals(prisoner.getOriginFaction().getShortName(), "DC")) {
                if (d6(2) == 2) {
                    if (isNPC) {
                        campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE,
                              "seppuku.report",
                              prisoner.getFullName(),
                              spanOpeningWithCustomColor(ReportingUtilities.getNegativeColor()),
                              CLOSING_SPAN_TAG));

                        prisoner.setStatus(SEPPUKU);
                    } else {
                        prisoner.changeStatus(campaign, today, SEPPUKU);
                    }
                    return;
                }
            }
        }

        if (isNPC) {
            prisoner.setPrisonerStatus(campaign, PRISONER, true);
        } else {
            prisoner.changeStatus(campaign, today, POW);
        }
    }

    /**
     * Attempts to calculate the likelihood of a defection by evaluating various factors including the origin faction of
     * the potential defector, their status as clan personnel, and whether the defector is controlled by the AI or the
     * player.
     *
     * @param potentialDefector The {@link Person} being evaluated for a potential defection. This person may originate
     *                          from a faction that affects defection probability.
     * @param isNPC             {@code true} if the defection attempt involves a non-player character (NPC), otherwise
     *                          {@code false}.
     *
     * @return The adjusted defection probability as an integer value based on the base defection chance and applicable
     *       multipliers.
     */
    int determineDefectionChance(Person potentialDefector, boolean isNPC) {
        int adjustedDefectionChance = DEFECTION_CHANCE;

        if (potentialDefector.getOriginFaction().isMercenary()) {
            adjustedDefectionChance = (int) Math.round(adjustedDefectionChance * MERCENARY_MULTIPLIER);
        }

        if (potentialDefector.isClanPersonnel()) {
            if (isNPC) {
                Faction campaignFaction = campaign.getFaction();
                if (campaignFaction.isPirate() || campaignFaction.isMercenary()) {
                    adjustedDefectionChance *= CLAN_DEZGRA_MULTIPLIER;
                }
            } else {
                if (searchingFaction == null || searchingFaction.isPirate() || searchingFaction.isMercenary()) {
                    adjustedDefectionChance *= CLAN_DEZGRA_MULTIPLIER;
                }
            }
        }

        return adjustedDefectionChance;
    }

    /**
     * Attempts to capture or determine the fate of a player character prisoner.
     *
     * <p>If the capture attempt fails, the player character is marked as missing in action (MIA).
     * Otherwise, the prisoner is processed further, using either standard or MekHQ-specific capture rules. Defection
     * rolls are applied if applicable, and post-capture events are handled.</p>
     *
     * @param prisoner    The {@link Person} representing the player-character prisoner.
     * @param wasPickedUp Whether the prisoner was picked up as part of the scenario outcome.
     */
    public void attemptCaptureOfPlayerCharacter(Person prisoner, boolean wasPickedUp, boolean isSpace) {
        LocalDate today = campaign.getLocalDate();

        // Attempt capture
        boolean captureSuccessful = wasPickedUp;

        if (!captureSuccessful) {
            captureSuccessful = rollForCapture();
        }

        // Early exit is capture was unsuccessful
        if (!captureSuccessful) {
            if (isSpace) {
                // If you're not found in space, you're not going to be found.
                // At least, not until we have a more robust SAR system.
                prisoner.changeStatus(campaign, campaign.getLocalDate(), KIA);
            } else {
                prisoner.changeStatus(campaign, campaign.getLocalDate(), MIA);
            }
            return;
        }

        PrisonerCaptureStyle prisonerCaptureStyle = campaign.getCampaignOptions().getPrisonerCaptureStyle();

        if (prisonerCaptureStyle.isNone()) {
            prisoner.changeStatus(campaign, campaign.getLocalDate(), MIA);
            return;
        }

        processPrisoner(prisoner, searchingFaction, prisonerCaptureStyle.isMekHQ(), false);

        // Have they dead? Usually from performing Bondsref
        if (prisoner.getStatus().isDead()) {
            return;
        }

        // If MekHQ Capture Style is disabled, we can use a shortcut
        if (!prisonerCaptureStyle.isMekHQ() || prisoner.getStatus().isEnemyBondsman()) {
            return;
        }

        // Otherwise, we attempt defection
        int defectionChance = determineDefectionChance(prisoner, false);

        if (randomInt(defectionChance) == 0) {
            if (prisoner.isClanPersonnel()) {
                prisoner.changeStatus(campaign, today, ENEMY_BONDSMAN);
            } else {
                prisoner.changeStatus(campaign, today, DEFECTED);
            }
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
    boolean rollForCapture() {
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
     * @param prisoner  The {@link Person} being processed as a captured prisoner.
     * @param newStatus The resulting {@link PrisonerStatus} of the prisoner post-capture.
     */
    private void handlePostCapture(Person prisoner, PrisonerStatus newStatus) {
        final String RESOURCE_BUNDLE = "mekhq.resources.PrisonerEvents";

        // non-clan prisoners should generate with lower than average loyalty, so drop the highest roll
        if (!newStatus.isBecomingBondsman()) {
            setLoyalty(prisoner);
        }

        // 'Recruit' prisoner
        PrisonerStatus prisonerStatus = prisoner.getPrisonerStatus();
        campaign.recruitPerson(prisoner, prisonerStatus, false, true, false);

        if (prisonerStatus.isPrisonerDefector()) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "defection.report", prisoner.getHyperlinkedName()));
        }

        if (prisonerStatus.isBecomingBondsman()) {
            campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "bondsman.report", prisoner.getHyperlinkedName()));
        }
    }

    /**
     * Sets the loyalty level for a captured prisoner.
     *
     * <p>This method calculates the loyalty value by performing four dice rolls,
     * taking the highest three rolls, and summing their results. The calculated value is then assigned as the
     * prisoner's loyalty attribute.</p>
     *
     * @param prisoner The captured prisoner whose loyalty is being calculated and set.
     */
    private void setLoyalty(Person prisoner) {
        List<Integer> rolls = new ArrayList<>();

        for (int roll = 0; roll < 4; roll++) {
            rolls.add(d6(1));
        }

        Collections.sort(rolls);

        prisoner.setLoyalty(rolls.get(0) + rolls.get(1) + rolls.get(2));
    }

    /**
     * Performs a die roll using a specified number of dice and returns the total.
     *
     * <p>This method allows us to pass in explicit values in during Unit Testing.</p>
     *
     * @param dice The number of six-sided dice to roll.
     *
     * @return The total result of rolling the specified number of six-sided dice.
     */
    protected int d6(int dice) {
        return Compute.d6(dice);
    }

    /**
     * Generates a random integer value between 0 (inclusive) and the specified maximum value (exclusive).
     *
     * @param maxValue The upper bound (exclusive) for the random integer generation. Must be a positive integer.
     *
     * @return A randomly generated integer value between 0 (inclusive) and maxValue (exclusive).
     */
    protected int randomInt(int maxValue) {
        return Compute.randomInt(maxValue);
    }
}
