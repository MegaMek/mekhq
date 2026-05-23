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
package mekhq.gui.model;

import mekhq.campaign.base.AbstractBase;

/**
 * A dropdown item for the Location filter present on the Hangar, Warehouse, and Personnel tabs.
 *
 * <p>Three kinds exist:</p>
 * <ul>
 *   <li>{@link #ALL} — show items from every location.</li>
 *   <li>{@link #MAIN_FORCE} — show items belonging to the campaign's main force.</li>
 *   <li>A base entry created via {@link #forBase(AbstractBase)} — show items at that base only.</li>
 * </ul>
 */
public final class LocationFilterItem {

    /** Sentinel: show items from every location, including main force and all bases. */
    public static final LocationFilterItem ALL = new LocationFilterItem(null, "All (Main Force)");

    /** Sentinel: show items that belong to the campaign's main force. */
    public static final LocationFilterItem MAIN_FORCE = new LocationFilterItem(null, "Main Force");

    /** {@code null} for the ALL and MAIN_FORCE sentinels, non-null for a specific base. */
    private final AbstractBase base;
    private final String label;

    private LocationFilterItem(AbstractBase base, String label) {
        this.base = base;
        this.label = label;
    }

    /**
     * Creates a filter item for a specific {@link AbstractBase}.
     *
     * @param base the base to filter to; must not be {@code null}
     * @return a new {@code LocationFilterItem} representing that base
     */
    public static LocationFilterItem forBase(AbstractBase base) {
        String name = base.getDisplayName();
        return new LocationFilterItem(base, name != null ? name : "Unnamed Base");
    }

    /** Returns {@code true} if this item represents the "All" sentinel. */
    public boolean isAll() {
        return this == ALL;
    }

    /** Returns {@code true} if this item represents the "Main Force" sentinel. */
    public boolean isMainForce() {
        return this == MAIN_FORCE;
    }

    /**
     * Returns the base associated with this item, or {@code null} for the ALL / MAIN_FORCE sentinels.
     */
    public AbstractBase getBase() {
        return base;
    }

    @Override
    public String toString() {
        return label;
    }
}
