/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.contractMarket;

import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
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
     * Main function to initiate a sequence of automated tasks when a contract is started. The tasks include prompt and
     * execution for unit mothballing, calculating and starting the journey to the target system.
     *
     * @param campaign The current campaign.
     * @param contract Selected contract.
     */
    public static void contractStartPrompt(Campaign campaign, Contract contract) {
        // If we're already in the right system, there is no need to automate these actions
        if (Objects.equals(campaign.getLocation().getCurrentSystem(), contract.getSystem())) {
            return;
        }

        // Initial setup
        final String commanderAddress = campaign.getCommanderAddress();
        final List<String> buttonLabels = List.of(getTextAt(RESOURCE_BUNDLE, "generalConfirm.text"),
              getTextAt(RESOURCE_BUNDLE, "generalDecline.text"));
        final Person speaker = campaign.getSeniorAdminPerson(TRANSPORT);

        // Mothballing
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "mothballDescription.text", commanderAddress);
        if (contract instanceof AtBContract atBContract) {
            String employerCode = atBContract.getEmployerCode();
            if (employerCode.equals(PIRATE_FACTION_CODE)) {
                inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "mothballDescription.text.PIR",
                      commanderAddress);
            }
        }

        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "mothballDescription.addendum");

        ImmersiveDialogSimple mothballDialog = new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              buttonLabels,
              outOfCharacterMessage,
              null,
              false);

        if (mothballDialog.getDialogChoice() == DIALOG_CONFIRM_OPTION) {
            campaign.setAutomatedMothballUnits(performAutomatedMothballing(campaign));
        }

        // Transit
        String targetSystem = contract.getSystemName(campaign.getLocalDate());
        JumpPath jumpPath = contract.getJumpPath(campaign);
        int travelDays = contract.getTravelDays(campaign);

        boolean isUseTwoWayPay = campaign.getCampaignOptions().isUseTwoWayPay();
        String totalCost = contract.getTransportAmount().dividedBy(isUseTwoWayPay ? 2 : 1).toAmountString();

        inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "transitDescription.text",
              targetSystem,
              travelDays,
              totalCost);

        ImmersiveDialogSimple transitDialog = new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              buttonLabels,
              null,
              null,
              false);

        if (transitDialog.getDialogChoice() == DIALOG_CONFIRM_OPTION) {
            if (!JumpBlockers.areAllUnitsJumpCapable(campaign)) {
                return;
            }

            campaign.getLocation().setJumpPath(jumpPath);
            campaign.getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));
            campaign.getApp().getCampaigngui().refreshAllTabs();
            campaign.getApp().getCampaigngui().refreshLocation();
            boolean useTwoWayPay = campaign.getCampaignOptions().isUseTwoWayPay();

            // This will return an empty string if the transaction was successful
            String jumpReport = TransportCostCalculations.performJumpTransaction(campaign.getFinances(), jumpPath,
                  campaign.getLocalDate(), contract.getTransportAmount().dividedBy(useTwoWayPay ? 2 : 1),
                  campaign.getCurrentSystem());

            if (jumpReport.isBlank()) {
                campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE,
                      "transitDescription.report",
                      targetSystem,
                      travelDays));
            } else {
                campaign.addReport(GENERAL, jumpReport);
            }
        }
    }

    /**
     * This method identifies all non-mothballed units within a campaign that are currently assigned to a {@link Formation}.
     * Those units are then GM Mothballed.
     *
     * @param campaign The current campaign.
     *
     * @return A list of all newly mothballed units.
     */
    public static List<UUID> performAutomatedMothballing(Campaign campaign) {
        List<UUID> mothballTargets = new ArrayList<>();
        MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);

        for (Formation formation : campaign.getAllForces()) {
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

        List<UUID> unitIds = campaign.getAutomatedMothballUnits();
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
        campaign.setAutomatedMothballUnits(new ArrayList<>());
    }

    public static void outOfContractMothballAutomation(Campaign campaign) {
        final List<String> buttonLabels = List.of(getTextAt(RESOURCE_BUNDLE, "generalConfirm.text"),
              getTextAt(RESOURCE_BUNDLE, "generalDecline.text"));

        final Person speaker = campaign.getSeniorAdminPerson(TRANSPORT);

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
            campaign.setAutomatedMothballUnits(performAutomatedMothballing(campaign));
        }
    }
}
