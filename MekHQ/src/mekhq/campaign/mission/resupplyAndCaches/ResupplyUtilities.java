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
package mekhq.campaign.mission.resupplyAndCaches;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.force.ForceType.CONVOY;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.CARGO_MULTIPLIER;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.RESUPPLY_AMMO_TONNAGE;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.RESUPPLY_ARMOR_TONNAGE;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.calculateTargetCargoTonnage;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.isProhibitedUnitType;
import static mekhq.campaign.personnel.enums.PersonnelStatus.KIA;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.UUID;

import megamek.common.compute.Compute;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Utility class for managing resupply operations and events in MekHQ campaigns.
 *
 * <p>The functionalities provided by this class assist in handling various resupply and convoy-related
 * scenarios, including:
 * <ul>
 *     <li>Managing abandoned convoy scenarios by removing units, updating crew statuses, and triggering dialogs.</li>
 *     <li>Estimating cargo requirements for resupply missions based on campaign context and contract details.</li>
 *     <li>Determining the outcomes for personnel (e.g., prisoners of war, killed in action) following combat-related events.</li>
 * </ul>
 *
 * <p>The class interacts heavily with the campaign's {@link Campaign} context and dynamic scenarios
 * from AtB (Against the Bot) contracts. It also integrates random computations for decision-making,
 * ensuring variability in outcomes for immersive campaign simulation.</p>
 *
 * <p>This utility is central to the logistics and event-handling systems present in MekHQ's resupply mechanics.</p>
 */
public class ResupplyUtilities {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Resupply";

    /**
     * Processes an abandoned convoy, managing the removal of units and determining the fate of the convoy's crew
     * members.
     *
     * <p>This method performs the following tasks:
     * <ul>
     *     <li>Identifies the player's convoy force from the scenario's template force IDs.</li>
     *     <li>Resolves each unit and its crew within the convoy:</li>
     *     <li>- Determines each crew member's fate, either captured or killed in action.</li>
     *     <li>- Removes units from the campaign force.</li>
     *     <li>Displays a dialog about the abandoned convoy.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} instance to which the convoy belongs.
     * @param contract the {@link AtBContract} related to the abandoned convoy scenario.
     * @param scenario the {@link AtBDynamicScenario} containing details of the abandoned convoy event.
     */
    public static void processAbandonedConvoy(Campaign campaign, AtBContract contract,
          AtBDynamicScenario scenario) {
        final int scenarioId = scenario.getId();

        for (Force force : campaign.getAllForces()) {
            Force parentForce = force.getParentForce();

            if (parentForce != null && (force.getParentForce().isForceType(CONVOY))) {
                continue;
            }

            if (force.isForceType(CONVOY) && force.getScenarioId() == scenarioId) {
                Person speaker = campaign.getPerson(force.getForceCommanderID());

                String commanderAddress = campaign.getCommanderAddress();
                String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                      "statusUpdateAbandoned" + randomInt(20) + ".text",
                      commanderAddress);
                String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "outOfCharacter.abandoned");

                new ImmersiveDialogSimple(campaign,
                      speaker,
                      null,
                      inCharacterMessage,
                      null,
                      outOfCharacterMessage,
                      null,
                      false);

                for (UUID unitID : force.getAllUnits(false)) {
                    Unit unit = campaign.getUnit(unitID);

                    for (Person crewMember : unit.getCrew()) {
                        decideCrewMemberFate(campaign, contract, crewMember);
                    }

                    campaign.removeUnit(unitID);
                }
            }
        }
    }

    /**
     * Determines the fate of a crew member based on random chance, assigning their status to either killed in action
     * (KIA) or prisoner of war (POW).
     *
     * <p>The fate is decided randomly via a 2d6 roll:
     * <ul>
     *     <li>If the roll is greater than 7, the crew member becomes a POW.</li>
     *     <li>Otherwise, the crew member is marked as KIA.</li>
     * </ul>
     * <p>
     * The survival chances are based on the infantry survival rules in Campaign Operations.
     *
     * @param campaign the {@link Campaign} instance for date tracking and updating crew member status.
     * @param person   the {@link Person} representing the crew member whose fate is being decided.
     */
    private static void decideCrewMemberFate(Campaign campaign, AtBContract contract, Person person) {
        PersonnelStatus status = KIA;

        if (Compute.d6(2) > 7) {
            if (contract.getEnemy().isClan()) {
                status = PersonnelStatus.ENEMY_BONDSMAN;
            } else {
                status = PersonnelStatus.POW;
            }
        }

        person.changeStatus(campaign, campaign.getLocalDate(), status);
    }

    /**
     * Estimates the total cargo requirements for a resupply operation based on the campaign and the associated contract
     * details. These cargo requirements are specifically modified for player-owned convoys.
     *
     * <p>This estimation is calculated as follows:
     * <ul>
     *     <li>Determines the target cargo tonnage using the {@link Campaign} and {@link AtBContract} data.</li>
     *     <li>Applies a cargo multiplier defined in {@link Resupply#CARGO_MULTIPLIER}.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} instance to calculate cargo requirements for.
     * @param contract the {@link AtBContract} defining the parameters of the mission.
     *
     * @return the estimated cargo requirement in tons.
     */
    public static int estimateCargoRequirements(Campaign campaign, AtBContract contract) {
        double targetTonnage = calculateTargetCargoTonnage(campaign, contract) * CARGO_MULTIPLIER;

        // Armor and ammo are always delivered in blocks, so cargo will never be less than the sum
        // of those blocks
        return max(RESUPPLY_AMMO_TONNAGE + RESUPPLY_ARMOR_TONNAGE, (int) Math.ceil(targetTonnage));
    }

    public static int estimateAvailablePlayerCargo(Campaign campaign) {
        double totalPlayerCargoCapacity = 0;

        for (Force force : campaign.getAllForces()) {
            if (!force.isForceType(CONVOY)) {
                continue;
            }

            if (force.getParentForce() != null && force.getParentForce().isForceType(CONVOY)) {
                continue;
            }

            double cargoCapacitySubTotal = 0;
            boolean hasCargo = false;
            for (UUID unitId : force.getAllUnits(false)) {
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
                }
            }
        }

        return (int) round(totalPlayerCargoCapacity);
    }
}
