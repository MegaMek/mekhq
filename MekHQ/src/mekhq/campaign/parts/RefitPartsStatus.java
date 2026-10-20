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

import java.util.Arrays;
import java.util.List;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;

/**
 * Whether a planned refit needs parts bought, so the refit list can show at a glance which refits can start from the
 * warehouse. For example, refitting a Javelin JVN-10A to the JVN-10N with the right lasers already on the shelf needs
 * nothing ordered, while a refit needing a Clan part the campaign may not buy cannot finish until one is found.
 *
 * @param state the state the refit is in
 * @param parts the parts to order, or the parts that cannot be bought; empty when everything is in stock
 */
public record RefitPartsStatus(State state, List<String> parts) {
    private static final MMLogger LOGGER = MMLogger.create(RefitPartsStatus.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Parts";

    /** How a refit stands for parts. */
    public enum State {
        /** Every part is in the warehouse */
        IN_STOCK,
        /** Some parts must be bought, and the campaign may buy them */
        TO_ORDER,
        /** Some parts are not in stock and the campaign's purchasing options do not allow buying them */
        CANNOT_BUY
    }

    /**
     * Works out how a refit stands for parts.
     *
     * @param campaign the campaign, for its warehouse and purchasing options
     * @param refit    the planned refit
     *
     * @return the refit's parts status
     */
    public static RefitPartsStatus of(Campaign campaign, Refit refit) {
        List<String> partsThatCannotBeBought = RefitPurchaseCheck.findPartsThatCannotBeBought(campaign, refit);
        if (!partsThatCannotBeBought.isEmpty()) {
            LOGGER.debug("[RefitParts] {}: {} parts cannot be bought", refit.getNewEntity().getShortName(),
                  partsThatCannotBeBought.size());
            return new RefitPartsStatus(State.CANNOT_BUY, partsThatCannotBeBought);
        }
        List<String> partsToOrder = Arrays.asList(refit.getShoppingListDescription());
        if (!partsToOrder.isEmpty()) {
            return new RefitPartsStatus(State.TO_ORDER, partsToOrder);
        }
        return new RefitPartsStatus(State.IN_STOCK, List.of());
    }

    /**
     * @return a sentence for the player saying where the refit stands for parts
     */
    public String describe() {
        return switch (state) {
            case IN_STOCK -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.inStock");
            case TO_ORDER -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.toOrder", String.join(", ", parts));
            case CANNOT_BUY -> getFormattedTextAt(RESOURCE_BUNDLE, "RefitPartsStatus.cannotBuy",
                  String.join(", ", parts));
        };
    }
}
