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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.BayDoor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.TransportBayPart;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekCockpit;
import mekhq.campaign.parts.meks.MekGyro;
import mekhq.campaign.parts.meks.MekLifeSupport;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.parts.missing.MissingBayDoor;
import mekhq.campaign.parts.missing.MissingEnginePart;
import mekhq.campaign.parts.missing.MissingMekActuator;
import mekhq.campaign.parts.missing.MissingMekCockpit;
import mekhq.campaign.parts.missing.MissingMekGyro;
import mekhq.campaign.parts.missing.MissingMekLifeSupport;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.parts.missing.MissingMekSensor;
import mekhq.campaign.unit.Unit;

/**
 * Repairs, as a campaign loads, the damage older versions did to the parts of units at bases.
 *
 * <p>Moving a unit to or from a base, and loading a campaign with one, could leave a unit with a second copy of its
 * parts kept in another warehouse, some of its parts kept away from it, and bays carrying more missing doors than they
 * have. Each unit is put right: a second part for a place on the unit that holds only one, such as a second engine,
 * is removed, keeping the one in the unit's own warehouse; missing doors beyond what a bay has are removed; and parts
 * still kept elsewhere are moved to the unit's warehouse. Removed parts were never bought, so nothing is paid for them.
 * </p>
 */
public final class UnitPartsRepair {
    private static final MMLogger LOGGER = MMLogger.create(UnitPartsRepair.class);

    private UnitPartsRepair() {}

    /**
     * Repairs every unit in the campaign, in the main force's hangar and each base's.
     *
     * @param campaign the campaign being loaded
     */
    public static void repair(Campaign campaign) {
        List<Unit> units = new ArrayList<>(campaign.getPlayerForce().getHangar().getUnits());
        for (PlayerBase base : campaign.getCampaignLocationManager().getPlayerBases()) {
            units.addAll(base.getBaseHangar().getUnits());
        }
        int repairedUnitCount = 0;
        for (Unit unit : units) {
            if (repair(campaign, unit)) {
                repairedUnitCount++;
            }
        }
        if (repairedUnitCount > 0) {
            LOGGER.info("[PartIdentity] Parts put right on {} units", repairedUnitCount);
        }
    }

    /**
     * Repairs one unit's parts.
     *
     * @param campaign the campaign
     * @param unit     the unit
     *
     * @return {@code true} if anything about the unit's parts was changed
     */
    static boolean repair(Campaign campaign, Unit unit) {
        LocalWarehouse homeWarehouse = unit.getWarehouse();
        if (homeWarehouse == null) {
            return false;
        }
        int duplicateCount = removeDuplicates(campaign, unit, homeWarehouse);
        int surplusDoorCount = removeSurplusMissingDoors(campaign, unit);
        int movedCount = 0;
        for (Part part : List.copyOf(unit.getParts())) {
            LocalWarehouse holdingWarehouse = campaign.getCampaignLocationManager().findHoldingWarehouse(campaign, part);
            boolean isKeptElsewhere = (holdingWarehouse != null) && (holdingWarehouse != homeWarehouse);
            if (isKeptElsewhere && holdingWarehouse.transferPart(part, homeWarehouse)) {
                movedCount++;
            }
        }
        boolean isRepaired = (duplicateCount + surplusDoorCount + movedCount) > 0;
        if (isRepaired) {
            LOGGER.info("[PartIdentity] {}: removed {} duplicate parts and {} surplus missing doors, moved {} parts "
                              + "to its warehouse", unit.getName(), duplicateCount, surplusDoorCount, movedCount);
        }
        return isRepaired;
    }

    /**
     * Removes each second part found for a place on the unit that holds only one, keeping the part in the unit's own
     * warehouse.
     */
    private static int removeDuplicates(Campaign campaign, Unit unit, LocalWarehouse homeWarehouse) {
        Map<String, Part> keptBySlot = new HashMap<>();
        List<Part> duplicates = new ArrayList<>();
        for (Part part : unit.getParts()) {
            String slot = slotOf(part);
            if (slot == null) {
                continue;
            }
            Part kept = keptBySlot.get(slot);
            if (kept == null) {
                keptBySlot.put(slot, part);
            } else if (isKeptBy(homeWarehouse, part) && !isKeptBy(homeWarehouse, kept)) {
                keptBySlot.put(slot, part);
                duplicates.add(kept);
            } else {
                duplicates.add(part);
            }
        }
        for (Part duplicate : duplicates) {
            removeFromCampaign(campaign, unit, duplicate);
        }
        return duplicates.size();
    }

    /**
     * The one place on a unit a part fills, or {@code null} for parts a unit can carry several of in one place.
     */
    static @Nullable String slotOf(Part part) {
        return switch (part) {
            case MekLocation location -> "location " + location.getLoc();
            case MissingMekLocation location -> "location " + location.getLocation();
            case MekActuator actuator -> "actuator " + actuator.getLocation() + " " + actuator.getType();
            case MissingMekActuator actuator -> "actuator " + actuator.getLocation() + " " + actuator.getType();
            case Armor armor -> "armor " + armor.getLocation() + (armor.isRearMounted() ? " rear" : " front");
            default -> onlyOneOnAUnit(part);
        };
    }

    /**
     * The name of a part a unit carries only one of, whatever its location, or {@code null} for any other part.
     */
    private static @Nullable String onlyOneOnAUnit(Part part) {
        if ((part instanceof EnginePart) || (part instanceof MissingEnginePart)) {
            return "engine";
        }
        if ((part instanceof MekGyro) || (part instanceof MissingMekGyro)) {
            return "gyro";
        }
        if ((part instanceof MekCockpit) || (part instanceof MissingMekCockpit)) {
            return "cockpit";
        }
        if ((part instanceof MekSensor) || (part instanceof MissingMekSensor)) {
            return "sensors";
        }
        if ((part instanceof MekLifeSupport) || (part instanceof MissingMekLifeSupport)) {
            return "life support";
        }
        return null;
    }

    /**
     * Removes missing doors beyond what the unit's bays have: a bay keeps its doors, present ones first, up to its
     * number of doors, and once every bay has all its doors, missing doors no bay lists are removed.
     */
    private static int removeSurplusMissingDoors(Campaign campaign, Unit unit) {
        List<Part> surplusDoors = new ArrayList<>();
        int doorsStillNeeded = 0;
        for (Part part : unit.getParts()) {
            if (part instanceof TransportBayPart bay) {
                doorsStillNeeded += collectSurplusDoors(bay, surplusDoors);
            }
        }
        if (doorsStillNeeded == 0) {
            for (Part part : unit.getParts()) {
                boolean isListedByABay = (part.getParentPart() != null)
                                               && part.getParentPart().getChildParts().contains(part);
                if ((part instanceof MissingBayDoor) && !isListedByABay && !surplusDoors.contains(part)) {
                    surplusDoors.add(part);
                }
            }
        }
        for (Part surplusDoor : surplusDoors) {
            removeFromCampaign(campaign, unit, surplusDoor);
        }
        return surplusDoors.size();
    }

    /**
     * Adds the bay's missing doors beyond its number of doors to the surplus list.
     *
     * @return how many doors the bay is still short of
     */
    private static int collectSurplusDoors(TransportBayPart bay, List<Part> surplusDoors) {
        if (bay.getBay() == null) {
            return 0;
        }
        int doorCount = bay.getBay().getDoors();
        int presentDoors = 0;
        List<Part> missingDoors = new ArrayList<>();
        for (Part childPart : bay.getChildParts()) {
            if (childPart instanceof BayDoor) {
                presentDoors++;
            } else if (childPart instanceof MissingBayDoor) {
                missingDoors.add(childPart);
            }
        }
        int missingDoorsAllowed = Math.max(0, doorCount - presentDoors);
        for (int index = missingDoorsAllowed; index < missingDoors.size(); index++) {
            surplusDoors.add(missingDoors.get(index));
        }
        return Math.max(0, missingDoorsAllowed - missingDoors.size());
    }

    private static boolean isKeptBy(LocalWarehouse warehouse, Part part) {
        return warehouse.getPart(part.getUniqueId()) == part;
    }

    private static void removeFromCampaign(Campaign campaign, Unit unit, Part part) {
        unit.removePart(part);
        Part parentPart = part.getParentPart();
        if (parentPart != null) {
            parentPart.removeChildPart(part);
        }
        LocalWarehouse holdingWarehouse = campaign.getCampaignLocationManager().findHoldingWarehouse(campaign, part);
        if (holdingWarehouse != null) {
            holdingWarehouse.removePart(part);
        }
    }
}
