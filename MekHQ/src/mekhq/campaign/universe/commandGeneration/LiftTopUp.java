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
import java.util.Set;
import java.util.UUID;

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
 * Adds the DropShips and JumpShips the units a build created after its rolls still need.
 *
 * <p>The transport stage sizes lift for the combat units it rolled. The support sections - the technicians,
 * medical and administrative staff organised into platoons and squads, and the support vehicles - are generated
 * afterwards, so no ship was ever sized for them. This runs after support generation and sizes lift for those
 * units alone, against what the hangar already has free: every ship's bays and collars, less what the rest of the
 * hangar would take. A command whose Overlord has spare bays gets no new hull; one whose support platoons have
 * nowhere to ride gets the smallest hull that takes them.</p>
 *
 * <p>Only the new units are lifted on purpose. A combat unit without a ship is one whose ship the player struck
 * out of the preview, and that choice stands: excluding a ship means no ship, not a different one.</p>
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
     * The hangar sorted into the units that want lift and everything already there, ships included.
     *
     * @param wantingLift  the units the build just created, which nothing was sized for
     * @param alreadyThere every other unit, ships and all; what they need comes off the free lift first
     */
    record Hangar(List<Entity> wantingLift, List<Entity> alreadyThere) {

        /**
         * @param hangarUnits every unit in the hangar
         * @param newUnitIds  the ids of the units the build created after its rolls
         */
        static Hangar of(Collection<Unit> hangarUnits, Set<UUID> newUnitIds) {
            List<Entity> wantingLift = new ArrayList<>();
            List<Entity> alreadyThere = new ArrayList<>();
            for (Unit unit : hangarUnits) {
                Entity entity = unit.getEntity();
                if (entity == null) {
                    continue;
                }
                boolean isNew = newUnitIds.contains(unit.getId());
                boolean isShip = entity.isLargeCraft();
                if (isNew && !isShip) {
                    wantingLift.add(entity);
                } else {
                    alreadyThere.add(entity);
                }
            }
            return new Hangar(wantingLift, alreadyThere);
        }
    }

    private LiftTopUp() {
    }

    /**
     * Adds the ships the units a build just created still need.
     *
     * @param campaign    the campaign whose hangar is checked and receives the ships
     * @param factionCode the faction whose ship tables are drawn from; {@code null} for the general tables
     * @param year        the year the ships are drawn for
     * @param rating      the command's rating, or {@code null} for any
     * @param dropshipPct the share of the units to provide DropShip bays for, 1.0 being all of them
     * @param jumpshipPct the share of the DropShips to provide docking collars for
     * @param newUnitIds  the ids of the units the build created after its rolls; only these are lifted
     *
     * @return the ships added; {@link Result#none()} when nothing was needed or DropShips are not wanted
     */
    public static Result topUp(Campaign campaign, @Nullable String factionCode, int year, @Nullable String rating,
          double dropshipPct, double jumpshipPct, Set<UUID> newUnitIds) {
        if (dropshipPct <= 0) {
            LOGGER.info("{} DropShip percentage is {}; the command hires its lift, so nothing is added",
                  LOG_TAG, dropshipPct);
            return Result.none();
        }
        Hangar hangar = Hangar.of(campaign.getUnits(), newUnitIds);
        if (hangar.wantingLift().isEmpty()) {
            LOGGER.info("{} the build added no units after its rolls; nothing to lift", LOG_TAG);
            return Result.none();
        }
        ExistingLift owned = ExistingLift.of(hangar.alreadyThere());
        LOGGER.info("{} {} new unit(s) checked against the hangar's free bays {} and {} free docking collar(s)"
                    + " ({} unit(s) already there, ships included)",
              LOG_TAG, hangar.wantingLift().size(), owned.freeBays(), owned.freeDockingCollars(),
              hangar.alreadyThere().size());

        FactionRecord factionRecord = (factionCode == null) ? null : RATGenerator.getInstance().getFaction(factionCode);
        TransportCalculator calculator = new TransportCalculator(factionRecord, factionCode, year, rating,
              hangar.wantingLift(), owned);
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
