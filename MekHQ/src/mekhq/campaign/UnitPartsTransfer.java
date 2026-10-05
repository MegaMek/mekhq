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
package mekhq.campaign;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import megamek.logging.MMLogger;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.unit.Unit;

/**
 * Moves a unit's parts, or spare parts, between the main force's warehouse and a base's when they travel.
 *
 * <p>Everything that belongs to a unit goes with it: its installed parts and the parts that belong to them, such as a
 * bay's doors, the spares set aside for its repairs, and its refit kit. Each part is taken from the warehouse that
 * keeps it and arrives as it was, keeping its identity and its links to other parts.</p>
 */
public final class UnitPartsTransfer {
    private static final MMLogger LOGGER = MMLogger.create(UnitPartsTransfer.class);

    private UnitPartsTransfer() {}

    /**
     * Moves everything that belongs to the unit to the destination warehouse.
     *
     * @param campaign    the campaign
     * @param unit        the unit whose parts move
     * @param destination the warehouse they move to
     *
     * @return how many parts moved
     */
    public static int moveUnitParts(Campaign campaign, Unit unit, LocalWarehouse destination) {
        Set<Part> partsToMove = new LinkedHashSet<>();
        for (Part part : unit.getParts()) {
            addWithParts(part, partsToMove);
            Part reservedSpare = part.getReplacementPart();
            if (reservedSpare != null) {
                addWithParts(reservedSpare, partsToMove);
            }
        }
        partsToMove.addAll(findRefitKit(campaign, unit));
        int movedCount = move(campaign, partsToMove, destination);
        LOGGER.debug("[PartIdentity] {}: {} parts moved with it", unit.getName(), movedCount);
        return movedCount;
    }

    /**
     * Moves spare parts, with the parts that belong to them, to the destination warehouse.
     *
     * @param campaign    the campaign
     * @param spareParts  the spares that move
     * @param destination the warehouse they move to
     *
     * @return how many parts moved
     */
    public static int moveSpareParts(Campaign campaign, Collection<Part> spareParts, LocalWarehouse destination) {
        Set<Part> partsToMove = new LinkedHashSet<>();
        for (Part sparePart : spareParts) {
            addWithParts(sparePart, partsToMove);
        }
        return move(campaign, partsToMove, destination);
    }

    private static void addWithParts(Part part, Set<Part> partsToMove) {
        if (partsToMove.add(part)) {
            for (Part childPart : part.getChildParts()) {
                addWithParts(childPart, partsToMove);
            }
        }
    }

    /**
     * The spares bought or set aside for the unit's refit, wherever they are kept.
     */
    private static List<Part> findRefitKit(Campaign campaign, Unit unit) {
        List<Part> refitKit = new ArrayList<>();
        List<LocalWarehouse> warehouses = new ArrayList<>();
        warehouses.add(campaign.getPlayerForce().getWarehouse());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            warehouses.add(base.getBaseWarehouse());
        }
        for (LocalWarehouse warehouse : warehouses) {
            for (Part part : warehouse.getParts()) {
                boolean isReservedForThisRefit = (part.getUnit() == null) && (part.getRefitUnit() == unit);
                if (isReservedForThisRefit) {
                    refitKit.add(part);
                }
            }
        }
        Refit refit = unit.getRefit();
        if ((refit != null) && (refit.getNewArmorSupplies() != null)) {
            refitKit.add(refit.getNewArmorSupplies());
        }
        return refitKit;
    }

    private static int move(Campaign campaign, Set<Part> partsToMove, LocalWarehouse destination) {
        CampaignLocationManager locationManager = campaign.getCampaignLocationManager();
        int movedCount = 0;
        for (Part part : partsToMove) {
            LocalWarehouse holdingWarehouse = locationManager.findHoldingWarehouse(campaign, part);
            boolean isElsewhere = (holdingWarehouse != null) && (holdingWarehouse != destination);
            if (isElsewhere && holdingWarehouse.transferPart(part, destination)) {
                movedCount++;
            }
        }
        return movedCount;
    }
}
