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
package mekhq.campaign.parts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.unit.Unit;

/**
 * Frees spares set aside for overnight replacements whose task no longer exists. A tech replacing a part over several
 * days reserves one spare for the job, and a reserved spare is never offered as a spare again until the job releases
 * it. If the unit leaves the campaign first - sold, lost in battle, or removed - the job goes with it and the spare
 * would stay reserved forever.
 */
public final class ReservedSpares {
    private static final MMLogger LOGGER = MMLogger.create(ReservedSpares.class);

    private ReservedSpares() {}

    /**
     * Gives back every spare reserved by the unit's replacement tasks. Call before the unit leaves the campaign.
     *
     * @param unit the unit about to leave
     */
    public static void releaseForDepartingUnit(Unit unit) {
        int releasedCount = 0;
        for (Part part : unit.getParts()) {
            if (part.getReplacementPart() != null) {
                part.cancelReservation();
                releasedCount++;
            }
        }
        if (releasedCount > 0) {
            LOGGER.debug("[ReservedSpares] {} leaves the campaign; {} reserved spares given back", unit.getName(),
                  releasedCount);
        }
    }

    /**
     * Frees every reserved spare that no replacement task holds any more. Such spares were left behind by units that
     * left the campaign before this was handled, and would otherwise stay reserved in the save forever.
     *
     * @param campaign the loaded campaign
     *
     * @return how many spares were freed
     */
    public static int releaseOrphanedReservations(Campaign campaign) {
        Set<Part> heldSpares = Collections.newSetFromMap(new IdentityHashMap<>());
        campaign.getPlayerForce().getHangar().forEachUnit(unit -> collectHeldSpares(unit, heldSpares));
        List<LocalWarehouse> warehouses = new ArrayList<>();
        warehouses.add(campaign.getPlayerForce().getWarehouse());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            base.getBaseHangar().forEachUnit(unit -> collectHeldSpares(unit, heldSpares));
            warehouses.add(base.getBaseWarehouse());
        }

        int releasedCount = 0;
        for (LocalWarehouse warehouse : warehouses) {
            // Copied first: giving a spare back can merge it into another stack and remove it from the warehouse
            for (Part part : new ArrayList<>(warehouse.getParts())) {
                boolean isOrphaned = part.isReservedForReplacement() && !heldSpares.contains(part);
                if (isOrphaned) {
                    part.setReservedBy(null);
                    warehouse.addPart(part, true);
                    releasedCount++;
                }
            }
        }
        if (releasedCount > 0) {
            LOGGER.info("[ReservedSpares] {} spares reserved for tasks that no longer exist were freed",
                  releasedCount);
        }
        return releasedCount;
    }

    private static void collectHeldSpares(Unit unit, Set<Part> heldSpares) {
        for (Part part : unit.getParts()) {
            Part replacement = part.getReplacementPart();
            if (replacement != null) {
                heldSpares.add(replacement);
            }
        }
    }
}
