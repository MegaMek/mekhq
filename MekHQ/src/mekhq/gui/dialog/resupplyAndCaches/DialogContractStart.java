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
package mekhq.gui.dialog.resupplyAndCaches;

import static mekhq.campaign.Campaign.AdministratorSpecialization.LOGISTICS;
import static mekhq.campaign.force.FormationType.CONVOY;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.isProhibitedUnitType;
import static mekhq.campaign.mission.resupplyAndCaches.ResupplyUtilities.estimateCargoRequirements;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.UUID;
import javax.swing.JDialog;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * This class provides utility methods to display dialogs related to the beginning of a contract. It generates
 * user-friendly messages summarizing cargo requirements, player convoy capabilities, and mission details.
 */
public class DialogContractStart extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    private final Campaign campaign;
    private final AtBContract contract;

    /**
     * Displays a dialog at the start of a contract, providing summarized details about the mission and player convoy
     * capabilities. The content is dynamically generated based on the given {@link Campaign} and {@link AtBContract}.
     * <p>
     * This method: - Generates a message summarizing the player's convoy capabilities and cargo capacity. - Fetches
     * localized text from the resource bundle based on the contract type and command rights. - Displays a dialog with
     * visuals (e.g., faction icon) and a confirmation button to proceed.
     *
     * @param campaign the current {@link Campaign}.
     * @param contract the active contract.
     */
    public DialogContractStart(Campaign campaign, AtBContract contract) {
        this.campaign = campaign;
        this.contract = contract;

        String outOfCharacterMessageKey = "outOfCharacter.contractStart." +
                                                (contract.getContractType().isGuerrillaType() ?
                                                       "guerrilla" :
                                                       "normal");

        new ImmersiveDialogSimple(campaign,
              campaign.getSeniorAdminPerson(LOGISTICS),
              null,
              generateContractStartMessage(),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, outOfCharacterMessageKey),
              null,
              false);
    }

    /**
     * Generates an HTML-formatted message to display in the start-of-contract dialog. The message includes details such
     * as - Total player convoy cargo capacity. - Number of operational player convoys. - Cargo requirements for the
     * contract.
     * <p>
     * The message format adapts based on the contract type (e.g., guerrilla warfare vs. general contract) and the
     * player's command rights (e.g., independent command).
     * <p>
     * This method: - Iterates through all player forces to calculate total convoy cargo capacity. - Checks for convoy
     * readiness, excluding units that are damaged, uncrewed, or prohibited. - Formats the message using localized
     * templates from the resource bundle.
     *
     * @return an HTML-formatted string message summarizing the player's readiness and convoy details in the context of
     *       the contract.
     */
    private String generateContractStartMessage() {
        int playerConvoys = 0;
        double totalPlayerCargoCapacity = 0;

        for (Formation formation : campaign.getAllFormations()) {
            if (!formation.isFormationType(CONVOY)) {
                continue;
            }

            if (formation.getParentFormation() != null && formation.getParentFormation().isFormationType(CONVOY)) {
                continue;
            }

            double cargoCapacitySubTotal = 0;
            boolean hasCargo = false;
            for (UUID unitId : formation.getAllUnits(false)) {
                try {
                    Unit unit = campaign.getUnit(unitId);
                    Entity entity = unit.getEntity();

                    if (unit.isDamaged() || !unit.isFullyCrewed() || isProhibitedUnitType(entity, true, true)) {
                        continue;
                    }

                    double individualCargo = unit.getCargoCapacity();

                    if (individualCargo > 0) {
                        hasCargo = true;
                    }

                    cargoCapacitySubTotal += individualCargo;
                } catch (Exception ignored) {
                    // If we run into an exception, it's because we failed to get Unit or Entity.
                    // In either case, we just ignore that unit.
                }
            }

            if (hasCargo) {
                if (cargoCapacitySubTotal > 0) {
                    totalPlayerCargoCapacity += cargoCapacitySubTotal;
                    playerConvoys++;
                }
            }
        }

        String convoyMessage;
        String commanderTitle = campaign.getCommanderAddress();

        if (contract.getContractType().isGuerrillaType() || campaign.isPirateCampaign()) {
            String convoyMessageTemplate = "contractStartMessageGuerrilla.text";
            convoyMessage = getFormattedTextAt(RESOURCE_BUNDLE, convoyMessageTemplate, commanderTitle);
        } else {
            String convoyMessageTemplate = "contractStartMessageGeneric.text";
            if (contract.getCommandRights().isIndependent()) {
                convoyMessageTemplate = "contractStartMessageIndependent.text";
            }

            convoyMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                  convoyMessageTemplate,
                  commanderTitle,
                  estimateCargoRequirements(campaign, contract),
                  totalPlayerCargoCapacity,
                  playerConvoys,
                  playerConvoys != 1 ? "s" : "");
        }

        return convoyMessage;
    }
}
