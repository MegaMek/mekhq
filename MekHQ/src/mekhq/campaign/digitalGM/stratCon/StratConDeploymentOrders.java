/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.digitalGM.stratCon;

import java.time.LocalDate;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConDeploymentOrderBehavior;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;
import mekhq.campaign.universe.Faction;

/**
 * Deployment Orders: how a contract's command rights are felt on the map. Every so often the employer orders the
 * player to move a formation to a position in the Area of Operations, placed as a point of interest that is also a
 * strategic objective (see {@link StratConDeploymentOrderBehavior}). Letting one lapse costs the player a Complete
 * Success.
 *
 * <p>How many orders come is set by the contract's command step (see {@link #getCommandStep}): its command rights,
 * shifted by how controlling the employer is. Each month brings the command step's worth of orders per point of the
 * contract's Scale, rounded normally, each on its own random day of the month:</p>
 *
 * <table>
 *     <caption>Deployment Orders per month, by command step</caption>
 *     <tr><th>Step</th><th>Orders</th></tr>
 *     <tr><td>0</td><td>None (Independent, or a lenient employer under Liaison command rights)</td></tr>
 *     <tr><td>1</td><td>1 x Scale (Liaison)</td></tr>
 *     <tr><td>2</td><td>2 x Scale (House)</td></tr>
 *     <tr><td>3</td><td>3 x Scale (Integrated)</td></tr>
 *     <tr><td>4</td><td>4 x Scale (Integrated, under a controlling employer)</td></tr>
 * </table>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class StratConDeploymentOrders {
    private static final MMLogger LOGGER = MMLogger.create(StratConDeploymentOrders.class);

    static final int MINIMUM_COMMAND_STEP = 0;
    static final int MAXIMUM_COMMAND_STEP = 4;

    private static final int LIAISON_COMMAND_STEP = 1;
    private static final int HOUSE_COMMAND_STEP = 2;
    private static final int INTEGRATED_COMMAND_STEP = 3;

    private StratConDeploymentOrders() {
    }

    /**
     * Works out how tightly the employer directs the contract. Liaison command rights start at step 1, House at 2, and
     * Integrated at 3; a controlling employer then adds a step, and a lenient one takes one away. Independent command
     * rights are always step 0, however controlling the employer is: the player answers to no one on the ground.
     *
     * @param commandRights   the contract's command rights, or {@code null} if it has none
     * @param employerFaction the contract's employer, or {@code null} if it has none
     *
     * @return the command step, from 0 to 4
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getCommandStep(@Nullable ContractCommandRights commandRights,
          @Nullable Faction employerFaction) {
        if (commandRights == null) {
            return MINIMUM_COMMAND_STEP;
        }

        int commandStep = switch (commandRights) {
            case INDEPENDENT -> MINIMUM_COMMAND_STEP;
            case LIAISON -> LIAISON_COMMAND_STEP;
            case HOUSE -> HOUSE_COMMAND_STEP;
            case INTEGRATED -> INTEGRATED_COMMAND_STEP;
        };

        if ((commandStep == MINIMUM_COMMAND_STEP) || (employerFaction == null)) {
            return commandStep;
        }

        if (employerFaction.isControlling()) {
            commandStep++;
        }

        if (employerFaction.isLenient()) {
            commandStep--;
        }

        return Math.clamp(commandStep, MINIMUM_COMMAND_STEP, MAXIMUM_COMMAND_STEP);
    }

    /**
     * Works out how many Deployment Orders arrive each month.
     *
     * @param commandStep the contract's command step (see {@link #getCommandStep})
     * @param exactScale  the contract's exact Scale
     *
     * @return the command step times the Scale, rounded normally and never below one; none at command step 0
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private rather than private so the rate can be tested directly.
    static int getMonthlyOrderCount(int commandStep, double exactScale) {
        if (commandStep <= MINIMUM_COMMAND_STEP) {
            return 0;
        }
        return StratConSpawnRates.getCount(exactScale, commandStep);
    }

    /**
     * Schedules a contract's Deployment Orders over its run, each on a random day of its month (see
     * {@link StratConContractInitializer#rollSpawnDates}), for the daily StratCon lifecycle to place as their days come.
     * Several may fall on the same day. Each is a strategic objective.
     *
     * <p>The schedule is laid out for the whole contract and only the days from {@code fromDate} on are kept, so a
     * schedule laid out again partway through leaves the days already past alone. Any order that would land on or after
     * the contract's last day is moved to the day before, so it has a chance to be carried out.</p>
     *
     * <p>Does nothing if the contract's command step is 0 (see {@link #getCommandStep}), it has no settled start date,
     * or the Deployment Order type is not defined.</p>
     *
     * @param contract      the contract
     * @param campaignState the campaign state to store the scheduled orders on
     * @param fromDate      the first day to keep; the contract's start date when it is accepted
     *
     * @author Illiani
     * @since 0.51.01
     */
    static void scheduleDeploymentOrders(AbstractContract contract, StratConCampaignState campaignState,
          @Nullable LocalDate fromDate) {
        int commandStep = getCommandStep(contract.getCommandRights(), contract.getEmployerFaction());
        LocalDate startDate = contract.getStartDate();
        if ((commandStep == MINIMUM_COMMAND_STEP) || (startDate == null)) {
            return;
        }

        if (StratConPointOfInterestDefinitions.getDefinition(StratConDeploymentOrderBehavior.TYPE_ID) == null) {
            LOGGER.warn("The {} point of interest type is not defined, so contract {} receives no Deployment Orders.",
                  StratConDeploymentOrderBehavior.TYPE_ID,
                  contract.getName());
            return;
        }

        List<Integer> schedule = StratConSpawnRates.getMonthlySchedule(contract.getLengthInMonths(),
              getMonthlyOrderCount(commandStep, contract.getExactScale()));

        LocalDate endDate = contract.getEndingDate();
        for (LocalDate spawnDate : StratConContractInitializer.rollSpawnDates(startDate,
              schedule,
              contract.getLengthInMonths())) {
            if ((fromDate != null) && spawnDate.isBefore(fromDate)) {
                continue;
            }
            if ((endDate != null) && endDate.isAfter(startDate) && !spawnDate.isBefore(endDate)) {
                spawnDate = endDate.minusDays(1);
            }

            campaignState.addScheduledPointOfInterest(new StratConScheduledPointOfInterest(spawnDate,
                  StratConDeploymentOrderBehavior.TYPE_ID,
                  true));
        }
    }
}
