/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market;

import static mekhq.campaign.mission.resupplyAndCaches.Resupply.isProhibitedUnitType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.WeaponType;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.Quartermaster;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.EnginePart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInUse;
import mekhq.campaign.parts.StructuralIntegrity;
import mekhq.campaign.parts.TankLocation;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.equipment.JumpJet;
import mekhq.campaign.parts.meks.MekActuator;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IAcquisitionWork;

/**
 * Manages the tracking and automatic stocking of parts currently in use across a campaign.
 *
 * <p>This class provides functionality for analyzing warehouse inventory, tracking parts in use on units, managing
 * spare parts, and automatically maintaining stock levels according to configured targets.</p>
 *
 * <p>It supports both normal acquisition through the shopping list and Game Master instant acquisition modes.</p>
 *
 * <p>This functionality previously lived in {@link Campaign} and was extracted into its own class during the 0.50.10
 * development cycle to improve maintainability.</p>
 *
 * <p>Key capabilities include:</p>
 * <ul>
 *     <li>Analyzing parts in use across all units</li>
 *     <li>Tracking spare parts in the warehouse</li>
 *     <li>Monitoring parts in transit and on order</li>
 *     <li>Automatically stocking parts to maintain configured inventory levels</li>
 *     <li>Filtering based on unit status (e.g., mothballed) and part quality</li>
 *     <li>Supporting different stock percentage targets for different part types</li>
 * </ul>
 */
public class PartsInUseManager {
    private final Campaign campaign;
    private final CampaignOptions campaignOptions;
    private final Warehouse warehouse;
    private final ShoppingList shoppingList;
    private final Quartermaster quartermaster;
    private final Map<String, Double> partsInUseRequestedStockMap;

    /**
     * Creates a new {@link PartsInUseManager} manager for the specified campaign.
     *
     * @param campaign the {@link Campaign} to manage parts for
     */
    public PartsInUseManager(Campaign campaign) {
        this.campaign = campaign;
        this.campaignOptions = campaign.getCampaignOptions();
        this.warehouse = campaign.getWarehouse();
        this.shoppingList = campaign.getShoppingList();
        this.quartermaster = campaign.getQuartermaster();
        this.partsInUseRequestedStockMap = campaign.getPartsInUseRequestedStockMap();
    }

    /**
     * Creates a {@link PartInUse} wrapper for the specified part, if applicable.
     *
     * <p>This method determines whether a part should be tracked for stock management and returns an appropriate
     * {@link PartInUse} instance with the default stock percentage set. Certain part types are excluded:</p>
     *
     * <ul>
     *     <li>{@link StructuralIntegrity} - not a proper part</li>
     *     <li>{@link Armor} with unknown type - represents absent armor (0 points)</li>
     *     <li>{@link EquipmentPart} with chassis modifications - should not be purchased separately from the chassis</li>
     * </ul>
     *
     * <p>If the part is a {@link MissingPart}, it is converted to its corresponding new part before wrapping.</p>
     *
     * @param part the {@link Part} to wrap
     *
     * @return a {@link PartInUse} instance with default stock settings, or {@code null} if the part should not be
     *       tracked
     */
    private PartInUse getPartInUse(Part part) {
        // SI isn't a proper "part"
        if (part instanceof StructuralIntegrity) {
            return null;
        }
        // Skip out on "not armor" (as in 0 point armor on men or field guns)
        if ((part instanceof Armor armor) && (armor.getType() == EquipmentType.T_ARMOR_UNKNOWN)) {
            return null;
        }
        // Makes no sense buying those separately from the chassis
        if ((part instanceof EquipmentPart equipmentPart) &&
                  (equipmentPart.getType() instanceof MiscType miscType) &&
                  (miscType.hasFlag(MiscType.F_CHASSIS_MODIFICATION))) {
            return null;
        }
        // Replace a "missing" part with a corresponding "new" one.
        if (part instanceof MissingPart missingPart) {
            part = missingPart.getNewPart();
        }
        PartInUse result = new PartInUse(part);
        result.setRequestedStock(getDefaultStockPercent(part));
        return (null != result.getPartToBuy()) ? result : null;
    }

    /**
     * Determines the default stock percentage for a given part type.
     *
     * <p>This method uses the type of the provided {@link Part} to decide which default stock percentage to return.
     * The values for each part type are retrieved from the campaign options.</p>
     *
     * @param part The {@link Part} for which the default stock percentage is to be determined. The part must not be
     *             {@code null}.
     *
     * @return An {@code int} representing the default stock percentage for the given part type, as defined in the
     *       campaign options.
     */
    private int getDefaultStockPercent(Part part) {
        if (part instanceof HeatSink) {
            return campaignOptions.getAutoLogisticsHeatSink();
        } else if (part instanceof MekLocation) {
            if (((MekLocation) part).getLoc() == Mek.LOC_HEAD) {
                return campaignOptions.getAutoLogisticsMekHead();
            }

            if (((MekLocation) part).getLoc() == Mek.LOC_CENTER_TORSO) {
                return campaignOptions.getAutoLogisticsNonRepairableLocation();
            }

            return campaignOptions.getAutoLogisticsMekLocation();
        } else if (part instanceof TankLocation) {
            return campaignOptions.getAutoLogisticsNonRepairableLocation();
        } else if (part instanceof AmmoBin || part instanceof AmmoStorage) {
            return campaignOptions.getAutoLogisticsAmmunition();
        } else if (part instanceof Armor) {
            return campaignOptions.getAutoLogisticsArmor();
        } else if (part instanceof MekActuator) {
            return campaignOptions.getAutoLogisticsActuators();
        } else if (part instanceof JumpJet) {
            return campaignOptions.getAutoLogisticsJumpJets();
        } else if (part instanceof EnginePart) {
            return campaignOptions.getAutoLogisticsEngines();
        } else if (part instanceof EquipmentPart equipmentPart) {
            if (equipmentPart.getType() instanceof WeaponType) {
                return campaignOptions.getAutoLogisticsWeapons();
            }
        }

        return campaignOptions.getAutoLogisticsOther();
    }


    /**
     * Updates a {@link PartInUse} record with data from an incoming {@link Part}.
     *
     * <p>This method processes the incoming part to update the usage, storage, or transfer count of the specified part
     * in use, based on the type, quality, and associated unit of the incoming part. Certain parts are ignored based on
     * their state or configuration, such as being part of conventional infantry, salvage, or mothballed units.</p>
     *
     * @param partInUse                the {@link PartInUse} record to update.
     * @param incomingPart             the new {@link Part} that is being processed for this record.
     * @param ignoreMothballedUnits    if {@code true}, parts belonging to mothballed units are excluded.
     * @param ignoreSparesUnderQuality spares with a quality lower than this threshold are excluded from counting.
     */
    private void updatePartInUseData(PartInUse partInUse, Part incomingPart, boolean ignoreMothballedUnits,
          PartQuality ignoreSparesUnderQuality) {
        Unit unit = incomingPart.getUnit();
        if (unit != null) {
            // Ignore conventional infantry
            if (unit.isConventionalInfantry()) {
                return;
            }

            // Ignore parts if they are from mothballed units and the flag is set
            if (ignoreMothballedUnits && incomingPart.getUnit() != null && incomingPart.getUnit().isMothballed()) {
                return;
            }

            // Ignore units set to salvage
            if (unit.isSalvage()) {
                return;
            }
        }

        // Case 1: Part is associated with a unit or is a MissingPart
        if ((unit != null) || (incomingPart instanceof MissingPart)) {
            partInUse.setUseCount(partInUse.getUseCount() + incomingPart.getQuantityForPartsInUse());
            return;
        }

        // Case 2: Part is present and meets quality requirements
        if (incomingPart.isPresent()) {
            if (incomingPart.getQuality().toNumeric() >= ignoreSparesUnderQuality.toNumeric()) {
                partInUse.setStoreCount(partInUse.getStoreCount() + incomingPart.getQuantityForPartsInUse());
                partInUse.addSpare(incomingPart);
            }
            return;
        }

        // Case 3: Part is not present, update transfer count
        partInUse.setTransferCount(partInUse.getTransferCount() + incomingPart.getQuantityForPartsInUse());
    }


    /**
     * Find all the parts that match this PartInUse and update their data
     *
     * @param partInUse                part in use record to update
     * @param ignoreMothballedUnits    don't count parts in mothballed units
     * @param ignoreSparesUnderQuality don't count spare parts lower than this quality
     */
    public void updatePartInUse(PartInUse partInUse, boolean ignoreMothballedUnits,
          PartQuality ignoreSparesUnderQuality) {
        partInUse.setUseCount(0);
        partInUse.setStoreCount(0);
        partInUse.setTransferCount(0);
        partInUse.setPlannedCount(0);
        warehouse.forEachPart(incomingPart -> {
            PartInUse newPartInUse = getPartInUse(incomingPart);
            if (partInUse.equals(newPartInUse)) {
                updatePartInUseData(partInUse, incomingPart, ignoreMothballedUnits, ignoreSparesUnderQuality);
            }
        });
        for (IAcquisitionWork maybePart : shoppingList.getPartList()) {
            PartInUse newPartInUse = getPartInUse((Part) maybePart);
            if (partInUse.equals(newPartInUse)) {
                Part newPart = (maybePart instanceof MissingPart)
                                     ? ((MissingPart) maybePart).getNewPart()
                                     : (Part) maybePart;
                partInUse.setPlannedCount(
                      partInUse.getPlannedCount() +
                            newPart.getQuantityForPartsInUse() * maybePart.getQuantity()
                );
            }
        }
    }

    /**
     * Analyzes the warehouse inventory and returns a data set that summarizes the usage state of all parts, including
     * their use counts, store counts, and planned counts, while filtering based on specific conditions.
     *
     * <p>This method aggregates all parts currently in use or available as spares, while taking into account
     * constraints like ignoring mothballed units or filtering spares below a specific quality. It uses a map structure
     * to efficiently track and update parts during processing.</p>
     *
     * @param ignoreMothballedUnits    If {@code true}, parts from mothballed units will not be included in the
     *                                 results.
     * @param isResupply               If {@code true}, specific units (e.g., prohibited unit types) are skipped based
     *                                 on the current context as defined in {@code Resupply.isProhibitedUnitType()}.
     * @param ignoreSparesUnderQuality Spare parts of a lower quality than the specified value will be excluded from the
     *                                 results.
     *
     * @return A {@link Set} of {@link PartInUse} objects detailing the state of each relevant part, including:
     *       <ul>
     *           <li>Use count: How many of this part are currently in use.</li>
     *           <li>Store count: How many of this part are available as spares in the warehouse.</li>
     *           <li>Planned count: The quantity of this part included in acquisition orders or planned
     *           procurement.</li>
     *           <li>Requested stock: The target or default quantity to maintain, as derived from settings or
     *           requests.</li>
     *       </ul>
     *       Only parts with non-zero counts (use, store, or planned) will be included in the result.
     */
    public Set<PartInUse> getPartsInUse(boolean ignoreMothballedUnits, boolean isResupply,
          PartQuality ignoreSparesUnderQuality) {
        // java.util.Set doesn't supply a get(Object) method, so we have to use a
        // java.util.Map
        Map<PartInUse, PartInUse> inUse = new HashMap<>();
        warehouse.forEachPart(incomingPart -> {
            if (isResupply) {
                Unit unit = incomingPart.getUnit();

                Entity entity = null;
                if (unit != null) {
                    entity = unit.getEntity();
                }

                if (entity != null) {
                    if (isProhibitedUnitType(entity, false, false)) {
                        return;
                    }
                }
            }

            PartInUse partInUse = getPartInUse(incomingPart);
            if (null == partInUse) {
                return;
            }

            String stockKey = partInUse.getDescription();
            stockKey += Part.getTechBaseName(partInUse.getTechBase());

            if (inUse.containsKey(partInUse)) {
                partInUse = inUse.get(partInUse);
            } else {
                if (partsInUseRequestedStockMap.containsKey(stockKey)) {
                    partInUse.setRequestedStock(partsInUseRequestedStockMap.get(stockKey));
                } else {
                    partInUse.setRequestedStock(getDefaultStockPercent(incomingPart));
                }
                inUse.put(partInUse, partInUse);
            }
            updatePartInUseData(partInUse, incomingPart, ignoreMothballedUnits, ignoreSparesUnderQuality);
        });

        for (IAcquisitionWork maybePart : shoppingList.getPartList()) {
            if (!(maybePart instanceof Part)) {
                continue;
            }
            PartInUse partInUse = getPartInUse((Part) maybePart);
            if (null == partInUse) {
                continue;
            }

            String stockKey = partInUse.getDescription();
            stockKey += Part.getTechBaseName(partInUse.getTechBase());

            if (inUse.containsKey(partInUse)) {
                partInUse = inUse.get(partInUse);
            } else {
                if (partsInUseRequestedStockMap.containsKey(stockKey)) {
                    partInUse.setRequestedStock(partsInUseRequestedStockMap.get(stockKey));
                } else {
                    partInUse.setRequestedStock(getDefaultStockPercent((Part) maybePart));
                }
                inUse.put(partInUse, partInUse);
            }

            Part newPart = (maybePart instanceof MissingPart)
                                 ? ((MissingPart) maybePart).getNewPart()
                                 : (Part) maybePart;
            partInUse.setPlannedCount(
                  partInUse.getPlannedCount() +
                        newPart.getQuantityForPartsInUse() * maybePart.getQuantity()
            );
        }
        return inUse.keySet()
                     .stream()
                     // Hacky but otherwise we end up with zero lines when filtering things out
                     .filter(p -> p.getUseCount() != 0 || p.getStoreCount() != 0 || p.getPlannedCount() != 0)
                     .collect(Collectors.toSet());
    }

    /**
     * Adds parts to the shopping list to stock up to the requested levels for all parts currently in use.
     *
     * <p>For each part in the provided set, this method calculates how many units need to be purchased to meet
     * the requested stock level and adds them to the shopping list if the quantity is positive.</p>
     *
     * @param partsInUse the set of {@link PartInUse} instances to stock up
     *
     * @return the number of distinct part types added to the shopping list
     */
    public int stockUpPartsInUse(Set<PartInUse> partsInUse) {
        int bought = 0;
        for (PartInUse partInUse : partsInUse) {
            int toBuy = findStockUpAmount(partInUse);
            if (toBuy > 0) {
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                shoppingList.addShoppingItem(partToBuy, toBuy, campaign);
                bought += 1;
            }
        }
        return bought;
    }

    /**
     * Immediately adds parts to inventory (GM mode) to stock up to the requested levels for all parts currently in
     * use.
     *
     * <p>This is the Game Master version of {@link #stockUpPartsInUse(Set)}, which bypasses the shopping list and
     * acquisition process, directly adding parts to the quartermaster's inventory with no delay or cost.</p>
     *
     * @param partsInUse the set of {@link PartInUse} instances to stock up
     *
     * @see #stockUpPartsInUse(Set)
     */
    public void stockUpPartsInUseGM(Set<PartInUse> partsInUse) {
        for (PartInUse partInUse : partsInUse) {
            int toBuy = findStockUpAmount(partInUse);
            while (toBuy > 0) {
                IAcquisitionWork partToBuy = partInUse.getPartToBuy();
                quartermaster.addPart((Part) partToBuy.getNewEquipment(), 0, true);
                --toBuy;
            }
        }
    }


    /**
     * Calculates the number of units needed to stock up a part to its requested level.
     *
     * <p>The calculation considers:</p>
     * <ul>
     *     <li>Current inventory: parts in storage, in transfer, and planned acquisitions</li>
     *     <li>Required stock: the requested stock percentage applied to the part's usage count</li>
     *     <li>Bundle adjustment: for bundled items (e.g., armor sold in 5-ton blocks), the quantity is adjusted
     *         based on tonnage per item</li>
     * </ul>
     *
     * @param PartInUse the {@link PartInUse} instance to calculate stock requirements for
     *
     * @return the number of units to purchase, or {@code 0} if no additional stock is needed
     */
    private int findStockUpAmount(PartInUse PartInUse) {
        int inventory = PartInUse.getStoreCount() + PartInUse.getTransferCount() + PartInUse.getPlannedCount();
        int needed = (int) Math.ceil(PartInUse.getRequestedStock() / 100.0 * PartInUse.getUseCount());
        int toBuy = needed - inventory;

        if (PartInUse.getIsBundle()) {
            toBuy = (int) Math.ceil((float) toBuy * PartInUse.getTonnagePerItem() / 5);
            // special case for armor only, as it's bought in 5 ton blocks. Armor is the
            // only kind of item that's assigned isBundle()
        }

        return toBuy;
    }
}
