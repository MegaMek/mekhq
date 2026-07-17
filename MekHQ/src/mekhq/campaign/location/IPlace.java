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
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.AbstractMobileLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.LocalPersonnel;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.RequestedStockLevels;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.PartInventory;
import mekhq.campaign.personnel.medical.advancedMedicalAlternate.Inoculations;

/**
 * A sub-interface of {@link ILocation} that marks a node in the {@link LocationNode} tree as a
 * "place" — an anchor that owns campaign resources such as a {@link LocalHangar}, {@link LocalWarehouse},
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
     * Returns the {@link LocalHangar} owned by this place, or {@code null} if this place does not own
     * one.
     *
     * <p>Overrides {@link ILocation#getHangar()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default LocalHangar getHangar() {
        return null;
    }

    /**
     * Returns the {@link LocalWarehouse} owned by this place, or {@code null} if this place does not
     * own one.
     *
     * <p>Overrides {@link ILocation#getWarehouse()} to stop the upward tree traversal.</p>
     */
    @Override
    @Nullable
    default LocalWarehouse getWarehouse() {
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
    default LocalPersonnel getPersonnel() {
        return null;
    }

    /**
     * Returns the per-part "requested stock percent" targets this place maintains for its warehouse.
     *
     * <p>Only the main force's {@link mekhq.campaign.force.Detachment} and {@link mekhq.campaign.base.AbstractBase} own
     * real stock levels; every other place inherits the empty {@link RequestedStockLevels#NO_RESTOCK}, so its parts fall
     * through to default stock percentages.</p>
     */
    default RequestedStockLevels getRequestedStockLevels() {
        return RequestedStockLevels.NO_RESTOCK;
    }

    /**
     * Returns a {@link PartInventory} counting spare parts that match {@code part} in this place's warehouse.
     *
     * <p>Supply counts present spares; transit counts non-present spares. The ordered count is zero here;
     * {@link mekhq.campaign.Campaign#getPartInventory} wraps this result to add the main force's shopping-list
     * orders.</p>
     */
    default PartInventory getPartInventory(Part part) {
        PartInventory inventory = new PartInventory();
        LocalWarehouse warehouse = getWarehouse();
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
     * Called when this place has just completed in-system transit and is now on-planet.
     *
     * <p>The default implementation fires disease and inoculation checks, which apply to every
     * arriving {@code IPlace}. Implementors such as {@link mekhq.campaign.Campaign} override this
     * (calling {@code IPlace.super.onArrival} at the appropriate point) to add place-specific
     * behaviors such as mothball activation, early-arrival contract checks, and personnel market
     * refresh.</p>
     *
     * @param campaign           the root campaign context
     * @param isSilentProcessing {@code true} when processing happens without user interaction (e.g.
     *                           fast-forward), which suppresses dialog prompts
     */
    default void onArrival(Campaign campaign, boolean isSilentProcessing) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (campaignOptions.isUseRandomDiseases() && campaignOptions.isUseAlternativeAdvancedMedical()) {
            if (!isSilentProcessing) {
                Inoculations.triggerInoculationPrompt(campaign, false);
            } else if (getParentLocation() instanceof AbstractLocation loc) {
                Inoculations.autoInoculateAll(campaign, loc);
            }
        }
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
        LocalPersonnel personnel = getPersonnel();
        if (personnel == null || !hasLocationNode()) {
            return;
        }
        LocalHangar hangar;
        hangar = getHangar() != null ? getHangar() : campaign.getPlayerForce().getHangar();
        LocalWarehouse warehouse;
        warehouse = getWarehouse() != null ? getWarehouse() : campaign.getPlayerForce().getWarehouse();
        for (LocationNode child : new ArrayList<>(getLocationNode().getChildren())) {
            if (!(child.getLocatable() instanceof AbstractMobileLocation travelNode)) {
                continue;
            }
            if (!travelNode.hasArrived()) {
                continue;
            }
            LocationDispatch.landFromTravelNode(travelNode, personnel, hangar, warehouse,
                  campaign.getPlayerForce().getForceDetachment(), campaign.getCampaignLocationManager());
        }
    }
}
