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

package mekhq.campaign.location;

import java.util.ArrayList;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Hangar;
import mekhq.campaign.Personnel;
import mekhq.campaign.Warehouse;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;

/**
 * A sub-interface of {@link ILocation} that marks a node in the {@link LocationNode} tree as a
 * "place" — an anchor that owns campaign resources such as a {@link Hangar}, {@link Warehouse},
 * and personnel roster.
 *
 * <p>
 * {@link ILocation#getHangar()}, {@link ILocation#getWarehouse()}, and
 * {@link ILocation#getPersonnel()} walk <em>up</em> the {@link LocationNode} tree until they reach
 * an {@code IPlace}. {@code IPlace} terminates that traversal by providing its own overrides.
 * Concrete implementations such as {@link mekhq.campaign.Campaign} override each method to return
 * the resource they own directly; implementations that do not own a given resource inherit these
 * defaults and return {@code null}.
 * </p>
 */
public interface IPlace extends ILocation {

    /**
     * Returns the {@link Hangar} owned by this place, or {@code null} if this place does not own
     * one.
     *
     * <p>Overrides {@link ILocation#getHangar()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default Hangar getHangar() {
        return null;
    }

    /**
     * Returns the {@link Warehouse} owned by this place, or {@code null} if this place does not
     * own one.
     *
     * <p>Overrides {@link ILocation#getWarehouse()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default Warehouse getWarehouse() {
        return null;
    }

    /**
     * Returns the personnel roster owned by this place, or {@code null} if this place does not own
     * one.
     *
     * <p>Overrides {@link ILocation#getPersonnel()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default Personnel getPersonnel() {
        return null;
    }

    /**
     * Returns a {@link PartInventory} counting spare parts that match {@code part} in this place's warehouse.
     *
     * <p>Supply counts present spares; transit counts non-present spares. The ordered count is
     * zero by default — {@link mekhq.campaign.Campaign} overrides this to add shopping-list orders.</p>
     */
    default PartInventory getPartInventory(Part part) {
        PartInventory inventory = new PartInventory();
        Warehouse warehouse = getWarehouse();
        if (warehouse == null) {
            return inventory;
        }
        int localSupplyCount = 0;
        int countInTransit = 0;
        for (Part warehousePart : warehouse.getParts()) {
            if (!warehousePart.isSpare()) {
                continue;
            }
            if (part.isSamePartType(warehousePart)) {
                if (warehousePart.isPresent()) {
                    localSupplyCount += warehousePart.getTotalQuantity();
                } else {
                    countInTransit += warehousePart.getTotalQuantity();
                }
            }
        }
        inventory.setSupply(localSupplyCount);
        inventory.setTransit(countInTransit);

        String countModifier = "";
        if (part instanceof Armor) {
            countModifier = "points";
        }
        if (part instanceof AmmoStorage) {
            countModifier = "shots";
        }
        inventory.setCountModifier(countModifier);
        return inventory;
    }

    /**
     * Processes arriving travel nodes parented to this place.
     *
     * <p>Landing hangar and warehouse default to this place's own resources; if either is
     * {@code null} (e.g. a campus that owns no hangar or warehouse) the campaign's resource is
     * used as a fallback. {@link mekhq.campaign.Campaign} overrides this with its own
     * implementation and is unaffected.</p>
     */
    @Override
    default void processArrivals(Campaign campaign) {
        Personnel personnel = getPersonnel();
        if (personnel == null || !hasLocationNode()) {
            return;
        }
        Hangar hangar = getHangar() != null ? getHangar() : campaign.getHangar();
        Warehouse warehouse = getWarehouse() != null ? getWarehouse() : campaign.getWarehouse();
        for (LocationNode child : new ArrayList<>(getLocationNode().getChildren())) {
            if (!(child.getLocatable() instanceof CurrentLocation travelNode)) {
                continue;
            }
            if (!travelNode.isOnPlanet()) {
                continue;
            }
            LocationDispatch.landFromTravelNode(travelNode, personnel, hangar, warehouse, campaign);
        }
    }
}
