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
package mekhq.gui.dialog.quartermaster;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.AbstractKitIssuer;

/**
 * Per-dialog cache of each kit's unit price, acquisition-difficulty text, and stock. All are costly to work out (a
 * template part and a full acquisition check per kit, a full pass over each warehouse for stock) and never change
 * while the dialog is open, so they are computed once and shared by every tab — the armor tab and both equipment-kit
 * tabs list many of the same kits, and the two equipment-kit tabs share a roster.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class KitPricing {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.IssueEquipmentDialog";

    private final Campaign campaign;
    private final Map<EquipmentType, Money> prices = new HashMap<>();
    private final Map<EquipmentType, String> acquisitionTexts = new HashMap<>();
    /** Stock per group of people, keyed by the group list itself, so tabs sharing a roster tally stores once. */
    private final Map<List<Person>, Map<EquipmentType, Integer>> stockByGroup = new IdentityHashMap<>();

    KitPricing(Campaign campaign) {
        this.campaign = campaign;
    }

    /**
     * @return the kit's unit price
     *
     * @author Illiani
     * @since 0.51.01
     */
    Money price(EquipmentType kit) {
        return prices.computeIfAbsent(kit, candidate -> AbstractKitIssuer.unitPrice(candidate, campaign));
    }

    /**
     * @return how hard a Regular acquirer would find the kit, rendered for a card
     *
     * @author Illiani
     * @since 0.51.01
     */
    String acquisitionText(EquipmentType kit) {
        return acquisitionTexts.computeIfAbsent(kit, this::computeAcquisitionText);
    }

    /**
     * @return kit stock across the distinct stores this group draws from, tallied once per group list
     *
     * @author Illiani
     * @since 0.51.01
     */
    Map<EquipmentType, Integer> stockFor(List<Person> people) {
        return stockByGroup.computeIfAbsent(people, group -> AbstractKitIssuer.localStock(group, campaign));
    }

    private String computeAcquisitionText(EquipmentType kit) {
        TargetRoll target = AbstractKitIssuer.acquisitionTarget(kit, campaign);
        if (target.getValue() == TargetRoll.AUTOMATIC_SUCCESS) {
            return getTextAt(RESOURCE_BUNDLE, "card.acquire.automatic");
        }
        if (target.cannotSucceed()) {
            return getTextAt(RESOURCE_BUNDLE, "card.acquire.unavailable");
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "card.acquire.tn", target.getValue());
    }
}
