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
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe.commandGeneration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import megamek.client.ratgenerator.ExistingLift;
import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.RATGenerator;
import megamek.client.ratgenerator.TransportCalculator;
import megamek.common.annotations.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;

/**
 * Adds the DropShips and JumpShips a command still needs once everything it will carry exists.
 *
 * <p>The transport stage sizes lift for the combat units it rolled. The support sections - the technicians,
 * medical and administrative staff organised into platoons and squads - are generated afterwards, so no ship was
 * ever sized for them. This runs after support generation and checks the whole hangar against the ships it owns:
 * every unit without a ship counts against the bays that could take it, most restrictive kind first, and a hull is
 * drawn only for what is left over. A command whose Overlord has spare bays gets no new hull; one whose support
 * platoons have nowhere to ride gets the smallest hull that takes them.</p>
 *
 * <p>Ships already owned count for their bays and docking collars only; they are not units wanting lift, apart
 * from a DropShip with no JumpShip to dock to, which counts against the collars.</p>
 */
public final class LiftTopUp {

    private static final MMLogger LOGGER = MMLogger.create(LiftTopUp.class);
    private static final String LOG_TAG = "[CompanyGen][LiftTopUp]";

    /**
     * What a run added.
     *
     * @param dropShips the DropShips added to the campaign, in creation order
     * @param jumpShips the JumpShips added to berth them
     */
    public record Result(List<Unit> dropShips, List<Unit> jumpShips) {

        public static Result none() {
            return new Result(List.of(), List.of());
        }
    }

    /**
     * The hangar sorted into the ships that offer lift and the units that want it.
     *
     * @param ships the DropShips, JumpShips, WarShips and stations
     * @param units everything else
     */
    record Hangar(List<Entity> ships, List<Entity> units) {

        static Hangar of(Collection<Unit> hangarUnits) {
            List<Entity> ships = new ArrayList<>();
            List<Entity> units = new ArrayList<>();
            for (Unit unit : hangarUnits) {
                Entity entity = unit.getEntity();
                if (entity == null) {
                    continue;
                }
                if (entity.isLargeCraft()) {
                    ships.add(entity);
                } else {
                    units.add(entity);
                }
            }
            return new Hangar(ships, units);
        }
    }

    private LiftTopUp() {
    }

    /**
     * Adds the ships the hangar's units still need.
     *
     * @param campaign    the campaign whose hangar is checked and receives the ships
     * @param factionCode the faction whose ship tables are drawn from; {@code null} for the general tables
     * @param year        the year the ships are drawn for
     * @param rating      the command's rating, or {@code null} for any
     * @param dropshipPct the share of the units to provide DropShip bays for, 1.0 being all of them
     * @param jumpshipPct the share of the DropShips to provide docking collars for
     *
     * @return the ships added; {@link Result#none()} when nothing was needed or DropShips are not wanted
     */
    public static Result topUp(Campaign campaign, @Nullable String factionCode, int year, @Nullable String rating,
          double dropshipPct, double jumpshipPct) {
        if (dropshipPct <= 0) {
            LOGGER.info("{} DropShip percentage is {}; the command hires its lift, so nothing is added",
                  LOG_TAG, dropshipPct);
            return Result.none();
        }
        Hangar hangar = Hangar.of(campaign.getUnits());
        ExistingLift owned = ExistingLift.of(hangar.ships());
        LOGGER.info("{} {} unit(s) checked against {} ship(s) offering free bays {} and {} free docking collar(s)",
              LOG_TAG, hangar.units().size(), hangar.ships().size(), owned.freeBays(), owned.freeDockingCollars());

        FactionRecord factionRecord = (factionCode == null) ? null : RATGenerator.getInstance().getFaction(factionCode);
        TransportCalculator calculator = new TransportCalculator(factionRecord, factionCode, year, rating,
              hangar.units(), owned);
        List<MekSummary> dropshipHulls = calculator.calcDropships(dropshipPct);
        List<MekSummary> jumpshipHulls = calculator.calcJumpShips(jumpshipPct, dropshipHulls.size());
        if (dropshipHulls.isEmpty() && jumpshipHulls.isEmpty()) {
            LOGGER.info("{} the command's ships already carry everything; no ships added", LOG_TAG);
            return Result.none();
        }

        List<Unit> dropShips = GeneratedShipUnits.addCrewedShips(campaign, dropshipHulls, LOG_TAG);
        List<Unit> jumpShips = GeneratedShipUnits.addCrewedShips(campaign, jumpshipHulls, LOG_TAG);
        // addNewUnit only reaches the hangar; the ships have to be filed to appear in the order of battle.
        if (!dropShips.isEmpty()) {
            AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, dropShips,
                  SupportTOEFormationTypes.TRANSPORT_COMMAND, SupportTOEFormationTypes.TROOPSHIP_COMMAND);
        }
        if (!jumpShips.isEmpty()) {
            AddSupportUnitsToTOE.addSupportUnitsToTOE(campaign, jumpShips,
                  SupportTOEFormationTypes.TRANSPORT_COMMAND, SupportTOEFormationTypes.JUMPSHIP_COMMAND);
        }
        LOGGER.info("{} added {} DropShip(s) and {} JumpShip(s) for what the command could not already carry",
              LOG_TAG, dropShips.size(), jumpShips.size());
        return new Result(dropShips, jumpShips);
    }
}
