/*
 * ContractAutomation.java
 *
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.contractMarket;

import megamek.common.Entity;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.JumpPath;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.unit.actions.ActivateUnitAction;
import mekhq.campaign.unit.actions.MothballUnitAction;
import mekhq.gui.dialog.ContractAutomationDialog;

import java.util.*;

/**
 * The ContractAutomation class provides a suite of methods
 * used in automating actions when a contract starts.
 * This includes actions like mothballing of units,
 * transit to mission location and the automated activation of units when arriving in system.
 */
public class ContractAutomation {
    private final static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.ContractAutomation");
    private static final MMLogger logger = MMLogger.create(ContractAutomation.class);

    /**
     * Main function to initiate a sequence of automated tasks when a contract is started.
     * The tasks include prompt and execution for unit mothballing, calculating and starting the
     * journey to the target system.
     *
     * @param campaign The current campaign.
     * @param contract Selected contract.
     */
    public static void contractStartPrompt(Campaign campaign, Contract contract) {
        // If we're already in the right system there is no need to automate these actions
        if (Objects.equals(campaign.getLocation().getCurrentSystem(), contract.getSystem())) {
            return;
        }

        // Initial setup
        final String commanderAddress = campaign.getCommanderAddress(false);

        // Mothballing
        String message = String.format(resources.getString("mothballDescription.text"), commanderAddress);

        ContractAutomationDialog mothballDialog = new ContractAutomationDialog(campaign, message, true);
        if (mothballDialog.isDialogConfirmed()) {
            campaign.setAutomatedMothballUnits(performAutomatedMothballing(campaign));
        }

        // Transit
        String targetSystem = contract.getSystemName(campaign.getLocalDate());
        String employerName = contract.getEmployer();

        if (!employerName.contains("Clan")) {
            employerName = String.format(resources.getString("generalNonClan.text"), employerName);
        }

        JumpPath jumpPath = contract.getJumpPath(campaign);
        int travelDays = contract.getTravelDays(campaign);

        Money costPerJump = campaign.calculateCostPerJump(true,
                campaign.getCampaignOptions().isEquipmentContractBase());
        String totalCost = costPerJump.multipliedBy(jumpPath.getJumps()).toAmountAndSymbolString();

        message = String.format(resources.getString("transitDescription.text"),
            targetSystem, employerName, travelDays, totalCost);

        ContractAutomationDialog transitDialog = new ContractAutomationDialog(campaign, message, false);
        if (transitDialog.isDialogConfirmed()) {
            campaign.getLocation().setJumpPath(jumpPath);
            campaign.getUnits().forEach(unit -> unit.setSite(Unit.SITE_FACILITY_BASIC));
            campaign.getApp().getCampaigngui().refreshAllTabs();
            campaign.getApp().getCampaigngui().refreshLocation();

            campaign.addReport(String.format(resources.getString("transitDescription.supplemental"),
                targetSystem, travelDays));
        }
    }

    /**
     * This method identifies all non-mothballed units within a campaign that are currently
     * assigned to a {@code Force}. Those units are then GM Mothballed.
     *
     * @param campaign The current campaign.
     * @return A list of all newly mothballed units.
     */
    private static List<Unit> performAutomatedMothballing(Campaign campaign) {
        List<Unit> mothballTargets = new ArrayList<>();
        MothballUnitAction mothballUnitAction = new MothballUnitAction(null, true);

        for (Force force : campaign.getAllForces()) {
            for (UUID unitId : force.getUnits()) {
                Unit unit = campaign.getUnit(unitId);

                if (unit == null) {
                    logger.error(String.format("Failed to get unit for unit ID %s", unitId));
                    continue;
                }

                try {
                    Entity entity = unit.getEntity();

                    if (entity.isLargeCraft()) {
                        continue;
                    }
                } catch (Exception e) {
                    logger.error(String.format("Failed to get entity for %s", unit.getName()));
                    continue;
                }

                if (unit.isAvailable(false) && !unit.isUnderRepair()) {
                    mothballTargets.add(unit);
                } else {
                    campaign.addReport(String.format(resources.getString("mothballingFailed.text"),
                        unit.getName()));
                }
            }
        }

        // This needs to be a separate list as the act of mothballing the unit removes it from the
        // list of units attached to the relevant force, resulting in a ConcurrentModificationException
        for (Unit unit : mothballTargets) {
            mothballUnitAction.execute(campaign, unit);
            MekHQ.triggerEvent(new UnitChangedEvent(unit));
        }

        return mothballTargets;
    }

    /**
     * Perform automated activation of units.
     * Identifies all units that were mothballed previously and are now needing activation.
     * The activation action is executed for each unit, and they are returned to their prior Force
     * if it still exists.
     *
     * @param campaign The current campaign.
     */
    public static void performAutomatedActivation(Campaign campaign) {
        List<Unit> units = campaign.getAutomatedMothballUnits();

        ActivateUnitAction activateUnitAction = new ActivateUnitAction(null, true);

        for (Unit unit : units) {
            if (unit.isMothballed()) {
                activateUnitAction.execute(campaign, unit);
                MekHQ.triggerEvent(new UnitChangedEvent(unit));

                if (unit.isMothballed()) {
                    campaign.addReport(String.format(resources.getString("activationFailed.text"),
                        unit.getName()));
                }
            }
        }

        // We still want to clear out any units
        campaign.setAutomatedMothballUnits(new ArrayList<>());
    }
}
