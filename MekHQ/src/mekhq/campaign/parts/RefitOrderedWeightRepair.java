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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.MissingEquipmentPart;
import mekhq.campaign.unit.Unit;

/**
 * Puts right the weight of parts an older version ordered ahead for a refit. Equipment whose weight matters, such as a
 * targeting computer, was ordered at the weight the unit's old design gives it, so a Summoner refitting to the
 * Summoner-H ordered a 0 ton targeting computer that the refit then refused (issue #11313). Each such order and part
 * is given the weight the refit's new design needs.
 */
public final class RefitOrderedWeightRepair {
    private static final MMLogger LOGGER = MMLogger.create(RefitOrderedWeightRepair.class);

    /** The equipment and size a refit needs, which together say which weight it needs */
    private record NeededEquipment(EquipmentType type, double size) {}

    private RefitOrderedWeightRepair() {}

    /**
     * Repairs the parts ordered ahead for every unit's pending refit, in the main force's hangar and each base's.
     *
     * @param campaign the campaign being loaded
     *
     * @return how many orders and parts were given another weight
     */
    public static int repair(Campaign campaign) {
        List<Unit> units = new ArrayList<>(campaign.getPlayerForce().getHangar().getUnits());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            units.addAll(base.getBaseHangar().getUnits());
        }
        int repairedCount = 0;
        for (Unit unit : units) {
            repairedCount += repair(campaign, unit);
        }
        if (repairedCount > 0) {
            LOGGER.info("[WeightMatters] Gave {} orders and parts ordered ahead for refits the weight the new design "
                  + "needs", repairedCount);
        }
        return repairedCount;
    }

    /**
     * Repairs the parts ordered ahead for one unit's pending refit.
     *
     * @return how many orders and parts were given another weight
     */
    static int repair(Campaign campaign, Unit unit) {
        if (unit.getPendingRefitModel() == null) {
            return 0;
        }
        List<Part> orderedAhead = RefitPartOrders.findOrderedAheadFor(campaign, unit);
        if (orderedAhead.isEmpty()) {
            return 0;
        }
        Refit refit = RefitPartOrders.planPendingRefit(unit);
        if (refit == null) {
            LOGGER.debug("[WeightMatters] {}: the pending refit could not be planned, so its parts are left as they are",
                  unit.getName());
            return 0;
        }
        return repair(unit, orderedAhead, refit);
    }

    /**
     * Gives each order and part ordered ahead the weight the planned refit's new design needs.
     *
     * @param unit         the unit with a pending refit
     * @param orderedAhead the orders and parts ordered ahead for its refit
     * @param refit        the planned refit
     *
     * @return how many orders and parts were given another weight
     */
    static int repair(Unit unit, List<Part> orderedAhead, Refit refit) {
        Map<NeededEquipment, Double> neededWeights = findNeededWeights(refit);
        int repairedCount = 0;
        for (Part part : orderedAhead) {
            if (repairWeight(unit, part, neededWeights)) {
                repairedCount++;
            }
        }
        return repairedCount;
    }

    /**
     * @return the weight the refit's new design needs for each piece of equipment whose weight matters that it still
     *       has to buy
     */
    private static Map<NeededEquipment, Double> findNeededWeights(Refit refit) {
        Map<NeededEquipment, Double> neededWeights = new HashMap<>();
        for (Part part : refit.getShoppingList()) {
            if ((part instanceof MissingEquipmentPart missingPart) && missingPart.isWeightMatters()) {
                neededWeights.put(new NeededEquipment(missingPart.getType(), missingPart.getSize()),
                      missingPart.getTonnage());
            }
        }
        return neededWeights;
    }

    /**
     * Gives an order or part the weight the refit needs, if its weight matters and differs.
     *
     * @return {@code true} if it was given another weight
     */
    private static boolean repairWeight(Unit unit, Part part, Map<NeededEquipment, Double> neededWeights) {
        Double neededWeight;
        if ((part instanceof EquipmentPart equipmentPart) && equipmentPart.isWeightMatters()) {
            neededWeight = neededWeights.get(new NeededEquipment(equipmentPart.getType(), equipmentPart.getSize()));
            if ((neededWeight == null) || (neededWeight == equipmentPart.getTonnage())) {
                return false;
            }
            equipmentPart.setEquipTonnage(neededWeight);
        } else if ((part instanceof MissingEquipmentPart missingPart) && missingPart.isWeightMatters()) {
            neededWeight = neededWeights.get(new NeededEquipment(missingPart.getType(), missingPart.getSize()));
            if ((neededWeight == null) || (neededWeight == missingPart.getTonnage())) {
                return false;
            }
            missingPart.setEquipTonnage(neededWeight);
        } else {
            return false;
        }
        LOGGER.debug("[WeightMatters] {}: {} ordered ahead for its refit now weighs the {} tons the new design needs",
              unit.getName(), part.getName(), neededWeight);
        return true;
    }
}
