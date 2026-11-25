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
package mekhq.campaign.finances.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import jakarta.annotation.Nullable;
import megamek.common.loaders.MekSummary;
import megamek.common.units.Entity;

/**
 * Controls how the campaign values its units: by their construction cost, or by Battle Value under the Alternate Unit
 * Cost rule from <i>Hot Spots: Draconis Reach</i>.
 *
 * <ul>
 *     <li>{@link #TOTAL_WARFARE} - units are bought at their construction cost and sold for the value of their parts,
 *     so damage lowers the sell value.</li>
 *     <li>{@link #CHAOS_CAMPAIGN} - every unit is bought for its base Battle Value and sold for half that, adjusted for
 *     quality and obsolescence. Damage never lowers either value.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum UnitCostModel {
    TOTAL_WARFARE("TOTAL_WARFARE"),
    CHAOS_CAMPAIGN("CHAOS_CAMPAIGN");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.UnitCostModel";

    private final String label;
    private final String tooltip;

    UnitCostModel(final String lookUpName) {
        this.label = getTextAt(RESOURCE_BUNDLE, "UnitCostModel." + lookUpName + ".label");
        this.tooltip = getTextAt(RESOURCE_BUNDLE, "UnitCostModel." + lookUpName + ".tooltip");
    }

    /**
     * Parses a saved unit cost model. Unknown values, including the removed {@code MEKHQ} model, fall back to
     * {@link #TOTAL_WARFARE}, the default.
     *
     * @param text the saved name
     *
     * @return the matching model, or {@link #TOTAL_WARFARE} if none matches
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static UnitCostModel fromString(@Nullable String text) {
        for (UnitCostModel model : values()) {
            if (model.name().equalsIgnoreCase(text)) {
                return model;
            }
        }
        return TOTAL_WARFARE;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isTotalWarfare() {
        return this == TOTAL_WARFARE;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isChaosCampaign() {
        return this == CHAOS_CAMPAIGN;
    }

    /**
     * @param unitEntity the unit's entity; may be {@code null}
     *
     * @return {@code true} if this model values the unit by its Battle Value rather than its construction cost;
     *       always {@code false} for a {@code null} entity
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isBattleValueCost(@Nullable Entity unitEntity) {
        return (unitEntity != null) && isChaosCampaign();
    }

    /**
     * @param summary the unit's summary
     *
     * @return {@code true} if this model values the unit by its Battle Value rather than its construction cost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isBattleValueCost(MekSummary summary) {
        return isChaosCampaign();
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    public String getLabel() {
        return label;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    public String getTooltip() {
        return tooltip;
    }

    @Override
    public String toString() {
        return label;
    }
}
