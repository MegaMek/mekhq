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
package mekhq.campaign.universe.WarriorsAlmanac;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * The development phases tracked by the Warrior's Almanac, in the order they occur in a technology's lifecycle.
 *
 * <p>Each phase carries a resource key for its localized, plain-text label, used as the value of the "Development"
 * column in the almanac's per-type tables.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum AlmanacTechAdvancementPhase {
    PROTOTYPE("WarriorsAlmanacDialog.development.prototype"),
    PRODUCTION("WarriorsAlmanacDialog.development.production"),
    COMMON("WarriorsAlmanacDialog.development.common"),
    EXTINCT("WarriorsAlmanacDialog.development.extinct");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.WarriorsAlmanacDialog";

    private final String labelKey;

    AlmanacTechAdvancementPhase(String labelKey) {
        this.labelKey = labelKey;
    }

    /**
     * @return the localized, plain-text label for this phase
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, labelKey);
    }
}
