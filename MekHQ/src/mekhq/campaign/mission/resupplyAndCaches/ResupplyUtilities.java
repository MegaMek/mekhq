/*
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
package mekhq.campaign.mission.resupplyAndCaches;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.resupplyAndCaches.DialogAbandonedConvoy;

import java.util.UUID;
import java.util.Vector;

import static java.lang.Math.floor;
import static java.lang.Math.max;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.CARGO_MULTIPLIER;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.RESUPPLY_AMMO_TONNAGE;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.RESUPPLY_ARMOR_TONNAGE;
import static mekhq.campaign.mission.resupplyAndCaches.Resupply.calculateTargetCargoTonnage;
import static mekhq.campaign.personnel.enums.PersonnelStatus.KIA;
import static mekhq.utilities.EntityUtilities.getEntityFromUnitId;

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
    /**
     * Processes an abandoned convoy, managing the removal of units and determining the fate of the
     * convoy's crew members.
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

            if (parentForce != null && (force.getParentForce().isConvoyForce())) {
                continue;
            }

            if (force.isConvoyForce() && force.getScenarioId() == scenarioId) {
                new DialogAbandonedConvoy(campaign, contract, force);

                for (UUID unitID : force.getAllUnits(false)) {
                    Unit unit = campaign.getUnit(unitID);

                    for (Person crewMember : unit.getCrew()) {
                        decideCrewMemberFate(campaign, crewMember);
                    }

                    campaign.removeUnit(unitID);
                }
            }
        }
    }

    /**
     * Determines the fate of a crew member based on random chance, assigning their status
     * to either killed in action (KIA) or prisoner of war (POW).
     *
     * <p>The fate is decided randomly via a 2d6 roll:
     * <ul>
     *     <li>If the roll is greater than 7, the crew member becomes a POW.</li>
     *     <li>Otherwise, the crew member is marked as KIA.</li>
     * </ul>
     *
     * The survival chances are based on the infantry survival rules in Campaign Operations.
     *
     * @param campaign the {@link Campaign} instance for date tracking and updating crew member status.
     * @param person   the {@link Person} representing the crew member whose fate is being decided.
     */
    private static void decideCrewMemberFate(Campaign campaign, Person person) {
        PersonnelStatus status = KIA;

        if (Compute.d6(2) > 7) {
            status = PersonnelStatus.POW;
        }

        person.changeStatus(campaign, campaign.getLocalDate(), status);
    }

    /**
     * Estimates the total cargo requirements for a resupply operation based on the campaign
     * and the associated contract details. These cargo requirements are specifically modified for
     * player-owned convoys.
     *
     * <p>This estimation is calculated as follows:
     * <ul>
     *     <li>Determines the target cargo tonnage using the {@link Campaign} and {@link AtBContract} data.</li>
     *     <li>Applies a cargo multiplier defined in {@link Resupply#CARGO_MULTIPLIER}.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} instance to calculate cargo requirements for.
     * @param contract the {@link AtBContract} defining the parameters of the mission.
     * @return the estimated cargo requirement in tons.
     */
    public static int estimateCargoRequirements(Campaign campaign, AtBContract contract) {
        double targetTonnage = calculateTargetCargoTonnage(campaign, contract) * CARGO_MULTIPLIER;

        // Armor and ammo are always delivered in blocks, so cargo will never be less than the sum
        // of those blocks
        return max(RESUPPLY_AMMO_TONNAGE + RESUPPLY_ARMOR_TONNAGE, (int) Math.ceil(targetTonnage));
    }

    /**
     * Determines if a convoy only contains VTOL or similar units.
     *
     * @param campaign the {@link Campaign} instance the convoy belongs to.
     * @param convoy   the {@link Force} representing the convoy to check.
     * @return {@code true} if the convoy only contains VTOL units, {@code false} otherwise.
     */
    public static boolean forceContainsOnlyVTOLForces(Campaign campaign, Force convoy) {
        for (UUID unitId : convoy.getAllUnits(false)) {
            Entity entity = getEntityFromUnitId(campaign, unitId);

            if (entity == null) {
                continue;
            }

            if (!entity.isAirborneVTOLorWIGE()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Determines if a convoy contains a majority of VTOL (or similar) units.
     *
     * <p>This is calculated by checking if at least half of the units are VTOLs or similarly
     * airborne types.</p>
     *
     * @param campaign      the {@link Campaign} instance the convoy belongs to.
     * @param convoy  the {@link Force} representing the convoy being evaluated.
     * @return {@code true} if the VTOL units constitute at least half of the units, {@code false} otherwise.
     */
    public static boolean forceContainsMajorityVTOLForces(Campaign campaign, Force convoy) {
        Vector<UUID> allUnits = convoy.getAllUnits(false);
        int convoySize = allUnits.size();
        int vtolCount = 0;

        for (UUID unitId : convoy.getAllUnits(false)) {
            Entity entity = getEntityFromUnitId(campaign, unitId);

            if (entity == null) {
                continue;
            }

            if (!entity.isAirborneVTOLorWIGE()) {
                vtolCount++;
            }
        }

        return vtolCount >= floor((double) convoySize / 2);
    }

    /**
     * Determines if a convoy only contains aerial units, such as aerospace or conventional fighters.
     *
     * @param campaign the {@link Campaign} instance the convoy belongs to.
     * @param convoy   the {@link Force} representing the convoy to check.
     * @return {@code true} if the convoy only contains aerial units, {@code false} otherwise.
     */
    public static boolean forceContainsOnlyAerialForces(Campaign campaign, Force convoy) {
        for (UUID unitId : convoy.getAllUnits(false)) {
            Entity entity = getEntityFromUnitId(campaign, unitId);

            if (entity == null) {
                continue;
            }

            if (!entity.isAerospace() && !entity.isConventionalFighter()) {
                return false;
            }
        }

        return true;
    }
}
