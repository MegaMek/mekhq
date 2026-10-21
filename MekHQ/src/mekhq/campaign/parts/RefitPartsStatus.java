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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

/**
 * How far the warehouse covers a planned refit, so the refit list can show at a glance which refits can start now,
 * which are partly covered, which need everything ordered, and which cannot be finished by buying parts at all. For
 * example, refitting a Timber Wolf D to the Prime with its lasers, actuators and ammunition on the shelf can start
 * now, while a refit needing a Clan part the campaign may not buy cannot finish until one is found.
 *
 * @param state        how the refit stands
 * @param itemsInStock how many of the refit's needs the warehouse covers
 * @param itemsNeeded  how many needs the refit has in all
 * @param parts        the items to order, or the parts that cannot be bought; empty when the refit can start now
 */
public record RefitPartsStatus(State state, int itemsInStock, int itemsNeeded, List<String> parts) {
    private static final MMLogger LOGGER = MMLogger.create(RefitPartsStatus.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /** How a refit stands for parts. */
    public enum State {
        /** Everything the refit needs is in the warehouse */
        READY,
        /** Nothing is left to order, but some of it is on order or on its way */
        WAITING_FOR_DELIVERY,
        /** The warehouse covers some of the refit's needs; the rest must be ordered */
        PARTLY_IN_STOCK,
        /** Nothing the refit needs is in the warehouse; everything must be ordered */
        TO_ORDER,
        /** Some parts are not in stock and the campaign's purchasing options do not allow buying them */
        CANNOT_BUY
    }

    /**
     * Works out how far the warehouse covers a planned refit.
     *
     * @param campaign the campaign, for its purchasing options
     * @param refit    the planned refit
     *
     * @return the refit's parts status
     */
    public static RefitPartsStatus of(Campaign campaign, Refit refit) {
        List<Refit.RefitNeed> needs = refit.describeNeeds();
        int itemsInStock = (int) needs.stream().filter(Refit.RefitNeed::isInStock).count();
        List<String> itemsToOrder = needs.stream()
                                          .filter(need -> need.supply() == Refit.Supply.TO_ORDER)
                                          .map(Refit.RefitNeed::description)
                                          .toList();
        List<String> itemsComing = needs.stream()
                                         .filter(need -> (need.supply() == Refit.Supply.ON_ITS_WAY)
                                               || (need.supply() == Refit.Supply.ON_ORDER))
                                         .map(Refit.RefitNeed::description)
                                         .toList();
        List<String> partsThatCannotBeBought = RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit);
        LOGGER.debug("[RefitParts] {}: {} of {} needs in stock, {} on order or on their way, {} cannot be bought",
              refit.getNewEntity().getShortName(), itemsInStock, needs.size(), itemsComing.size(),
              partsThatCannotBeBought.size());
        if (!partsThatCannotBeBought.isEmpty()) {
            return new RefitPartsStatus(State.CANNOT_BUY, itemsInStock, needs.size(), partsThatCannotBeBought);
        }
        if (itemsToOrder.isEmpty()) {
            return itemsComing.isEmpty()
                         ? new RefitPartsStatus(State.READY, itemsInStock, needs.size(), List.of())
                         : new RefitPartsStatus(State.WAITING_FOR_DELIVERY, itemsInStock, needs.size(), itemsComing);
        }
        boolean isAnythingCovered = (itemsInStock > 0) || !itemsComing.isEmpty();
        State state = isAnythingCovered ? State.PARTLY_IN_STOCK : State.TO_ORDER;
        return new RefitPartsStatus(state, itemsInStock, needs.size(), itemsToOrder);
    }

    /**
     * @return a sentence for the player saying how far the warehouse covers the refit
     */
    public String describe() {
        String partList = String.join(", ", parts);
        return switch (state) {
            case READY -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.ready");
            case WAITING_FOR_DELIVERY -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.waiting", partList);
            case PARTLY_IN_STOCK -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.partly", itemsInStock,
                  itemsNeeded, partList);
            case TO_ORDER -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.toOrder", partList);
            case CANNOT_BUY -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.cannotBuy", partList);
        };
    }
}
