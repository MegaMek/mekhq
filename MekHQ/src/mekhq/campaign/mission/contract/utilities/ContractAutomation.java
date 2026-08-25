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
package mekhq.campaign.mission.contract.utilities;

import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_REGULAR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Detachment;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.utilities.ContractUtilities;
import mekhq.campaign.mission.utilities.TransportCostCalculations;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.utilities.JumpBlockers;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * The {@link ContractAutomation} class provides a suite of methods used in automating actions when a contract starts.
 *
 * <p>This includes actions like mothballing of units, transit to mission location and the automated activation of
 * units when arriving in the system.</p>
 */
public class ContractAutomation {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractAutomation";
    private static final MMLogger logger = MMLogger.create(ContractAutomation.class);

    private static final int DIALOG_CONFIRM_OPTION = 0;

    /**
     * Runs the contract-start automation directly from pre-made choices instead of prompting the player, because the
     * market dialog has already captured those choices as checkboxes.
     *
     * <p>Regardless of the transit choice the contract is dated to <b>start on the day the force should arrive</b> at
     * the target system (today plus the computed travel time; today if already there). When {@code mothball} is set,
     * eligible units are GM-mothballed first. When {@code travel} is set and a jump path exists, the jump is plotted
     * and the journey is charged; if it is not set, the player is left to make the trip themselves but the contract
     * still starts on the projected arrival day.</p>
     *
     * @param campaign the current campaign
     * @param contract the contract being started
     * @param mothball {@code true} to GM-mothball eligible units before departure
     * @param travel   {@code true} to plot and charge the jump to the target system now
     */
    public static void performContractStart(Campaign campaign, AbstractContract contract, boolean mothball,
          boolean travel) {
        PlayerForce playerForce = campaign.getPlayerForce();
        AbstractLocation currentLocation = playerForce.getForceDetachment().getCurrentLocation();

        if (mothball) {
            List<UUID> automatedMothballUnits = performAutomatedMothballing(campaign);
            playerForce.setAutomatedMothballUnits(automatedMothballUnits);
        }

        // Work out the journey. If we are already in the target system there is no jump and travel time is zero.
        boolean alreadyAtTarget = Objects.equals(campaign.getPlayerForce()
                                                       .getForceDetachment()
                                                       .getCurrentLocation()
                                                       .getCurrentSystem(),
              contract.getTargetSystem());
        JumpPath jumpPath = alreadyAtTarget ? null : ContractUtilities.getJumpPath(campaign, contract, currentLocation);
        int travelDays = (jumpPath == null) ? 0
                               : ContractUtilities.getTravelDays(campaign, contract, currentLocation,
              playerForce.isOverridingCommandCircuitRequirements(),
              playerForce.getFactionStandings());

        // The contract starts on the day the force should arrive, whether or not transit is automated here.
        contract.setStartAndEndDate(campaign.getLocalDate().plusDays(travelDays));

        if (!travel || (jumpPath == null)) {
            return;
        }

        if (!JumpBlockers.areAllUnitsJumpCapable(campaign)) {
            return;
        }

            campaign.getPlayerForce().getForceDetachment().getCurrentLocation().setJumpPath(jumpPath);
            campaign.getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));
            campaign.getGUI().refreshAllTabs();

        Detachment detachment = playerForce.getForceDetachment();
        TransportCostCalculations costCalculations = new TransportCostCalculations(detachment.getHangar().getUnits(),
              playerForce.getWarehouse().getSpareParts(),
              detachment.getPersonnel().values(),
              EXP_REGULAR);
        Money cost = costCalculations.calculateJumpCostForEntireJourney(travelDays, jumpPath.getJumps());

        // Hyperlinked so the player can jump straight to the destination on the interstellar map.
        PlanetarySystem targetPlanetarySystem = contract.getTargetSystem();
        String targetSystem = (targetPlanetarySystem == null) ?
                                    contract.getTargetSystemName(campaign.getLocalDate()) :
                                    targetPlanetarySystem.getHyperlinkedName(campaign.getLocalDate());
        // performJumpTransaction returns an empty string when the charge succeeded.
        String jumpReport = TransportCostCalculations.performJumpTransaction(playerForce.getFinances(),
              jumpPath,
              campaign.getLocalDate(),
              cost,
              campaign.getCurrentSystem());
        if (jumpReport.isBlank()) {
            campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "transitDescription.report",
                  targetSystem, travelDays));
        } else {
            campaign.addReport(GENERAL, jumpReport);
        }

        campaign.getGUI().refreshAllTabs();
    }

    /**
     * This method identifies all non-mothballed units within a campaign that are currently assigned to a
     * {@link Formation}. Those units are then GM Mothballed.
     *
     * @param campaign The current campaign.
     *
     * @return A list of all newly mothballed units.
     */
    public static List<UUID> performAutomatedMothballing(Campaign campaign) {
        List<UUID> mothballTargets = new ArrayList<>();
        MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);

        for (Formation formation : campaign.getPlayerForce().getAllFormations()) {
            List<UUID> iterationSafeUnitIds = new ArrayList<>(formation.getUnits());
            for (UUID unitId : iterationSafeUnitIds) {
                Unit unit = campaign.getUnit(unitId);

                if (unit == null) {
                    logger.error("Failed to get unit for unit ID {}", unitId);
                    continue;
                }

                try {
                    Entity entity = unit.getEntity();

                    if (entity.isLargeCraft()) {
                        continue;
                    }
                } catch (Exception e) {
                    logger.error("Failed to get entity for {}", unit.getName());
                    continue;
                }

                if (unit.isAvailable(false) && !unit.isUnderRepair()) {
                    mothballTargets.add(unitId);

                    mothballUnitAction.execute(campaign, unit);
                    MekHQ.triggerEvent(new UnitChangedEvent(unit));
                } else {
                    campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE,
                          "mothballingFailed.text",
                          unit.getHyperlinkedName()));
                }
            }
        }

        return mothballTargets;
    }

    /**
     * Perform automated activation of units. Identifies all units that were mothballed previously and are now needing
     * activation. The activation action is executed for each unit, and they are returned to their prior Force if it
     * still exists.
     *
     * @param campaign The current campaign.
     */
    public static void performAutomatedActivation(Campaign campaign) {
        ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);

        List<UUID> unitIds = campaign.getPlayerForce().getAutomatedMothballUnits();
        for (UUID unitId : unitIds) {
            Unit unit = campaign.getUnit(unitId);

            if (unit == null) {
                campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "activationFailed.uuid",
                      unitId.toString()));
                continue;
            }

            if (unit.isMothballed()) {
                activateUnitAction.execute(campaign, unit);
                MekHQ.triggerEvent(new UnitChangedEvent(unit));

                if (unit.isMothballed()) {
                    campaign.addReport(TECHNICAL, getFormattedTextAt(RESOURCE_BUNDLE, "activationFailed.text"),
                          unit.getHyperlinkedName());
                }
            }
        }

        // We still want to clear out any units
        campaign.getPlayerForce().setAutomatedMothballUnits(new ArrayList<UUID>());
    }

    public static void outOfContractMothballAutomation(Campaign campaign) {
        final List<String> buttonLabels = List.of(getTextAt(RESOURCE_BUNDLE, "generalConfirm.text"),
              getTextAt(RESOURCE_BUNDLE, "generalDecline.text"));

        final Person speaker = campaign.getPlayerForce().getHumanResources()
                                     .getSeniorAdminPerson(campaign.getCampaignOptions(),
                                           campaign.getPlayerForce().isClanForce(),
                                           campaign.getLocalDate());

        final String commanderAddress = campaign.getCommanderAddress();
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "mothballDescription.text.noContract",
              commanderAddress);

        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "mothballDescription.addendum.noContract");

        ImmersiveDialogSimple mothballDialog = new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              buttonLabels,
              outOfCharacterMessage,
              null,
              false);

        if (mothballDialog.getDialogChoice() == DIALOG_CONFIRM_OPTION) {
            List<UUID> automatedMothballUnits = performAutomatedMothballing(campaign);
            campaign.getPlayerForce().setAutomatedMothballUnits(automatedMothballUnits);
        }
    }
}
