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
import megamek.common.units.UnitType;

/**
 * Controls how the campaign values its units: by their construction cost, or by Battle Value under the Alternate Unit
 * Cost rule from <i>Hot Spots: Draconis Reach</i>.
 *
 * <ul>
 *     <li>{@link #TOTAL_WARFARE} - units are valued by their construction cost.</li>
 *     <li>{@link #CHAOS_CAMPAIGN} - every unit is valued by its Battle Value.</li>
 *     <li>{@link #MEKHQ} - as {@link #CHAOS_CAMPAIGN}, except large craft (DropShips,
 *     JumpShips, WarShips, and space stations) keep their construction cost. Their Battle Value is tiny next to their
 *     construction cost, so valuing them by it makes them absurdly cheap.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum UnitCostModel {
    TOTAL_WARFARE("TOTAL_WARFARE"),
    CHAOS_CAMPAIGN("CHAOS_CAMPAIGN"),
    MEKHQ("MEKHQ");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.UnitCostModel";

    private final String label;
    private final String tooltip;

    UnitCostModel(final String lookUpName) {
        this.label = getTextAt(RESOURCE_BUNDLE, "UnitCostModel." + lookUpName + ".label");
        this.tooltip = getTextAt(RESOURCE_BUNDLE, "UnitCostModel." + lookUpName + ".tooltip");
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
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isMekHQ() {
        return this == MEKHQ;
    }

    /**
     * @param isLargeCraft whether the unit is a DropShip, JumpShip, WarShip, or space station
     *
     * @return {@code true} if this model values such a unit by its Battle Value rather than its construction cost
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isBattleValueCost(boolean isLargeCraft) {
        return switch (this) {
            case TOTAL_WARFARE -> false;
            case CHAOS_CAMPAIGN -> true;
            case MEKHQ -> !isLargeCraft;
        };
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
        return (unitEntity != null) && isBattleValueCost(unitEntity.isLargeCraft());
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
        return isBattleValueCost(isLargeCraft(UnitType.determineUnitTypeCode(summary.getUnitType())));
    }

    /**
     * Matches {@link Entity#isLargeCraft()}, which counts DropShips and every kind of JumpShip.
     *
     * @param unitTypeCode the unit's {@link UnitType} code
     *
     * @return {@code true} for DropShips, JumpShips, WarShips, and space stations
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isLargeCraft(int unitTypeCode) {
        return (unitTypeCode == UnitType.DROPSHIP)
                     || (unitTypeCode == UnitType.JUMPSHIP)
                     || (unitTypeCode == UnitType.WARSHIP)
                     || (unitTypeCode == UnitType.SPACE_STATION);
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
