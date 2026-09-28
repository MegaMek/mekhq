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
package mekhq.campaign.digitalGM.stratCon.deployment;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * The four pages of the StratCon deployment wizard. Each page shares the same board-plus-inspector shape but browses a
 * different tier of the table of organization and spends a different budget:
 *
 * <ul>
 *     <li>{@link #PRIMARY} - assign a scenario's primary forces (formation tier).</li>
 *     <li>{@link #REINFORCE} - assign reinforcement forces at a support-point cost and reinforcement roll (formation
 *     tier).</li>
 *     <li>{@link #AUXILIARIES} - add leadership units against a battle-value budget (unit tier). Formerly
 *     "Leadership".</li>
 *     <li>{@link #UTILITY} - add supporting units that trade against the scenario's defensive points and minefields
 *     (unit tier). Formerly "Defensive".</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum DeploymentMode {
    PRIMARY("deploymentWizard.mode.primary"),
    REINFORCE("deploymentWizard.mode.reinforce"),
    AUXILIARIES("deploymentWizard.mode.auxiliaries"),
    UTILITY("deploymentWizard.mode.utility");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.AtBStratCon";

    private final String labelKey;

    DeploymentMode(String labelKey) {
        this.labelKey = labelKey;
    }

    /**
     * @return the localized, human-readable label for this mode, for the page's tab in the mode strip
     *
     * @author Illiani
     * @since 0.51.01
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, labelKey);
    }

    /**
     * @return {@code true} if this mode browses individual units ({@link #AUXILIARIES}, {@link #UTILITY}) rather than
     *       whole formations ({@link #PRIMARY}, {@link #REINFORCE})
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isUnitTier() {
        return this == AUXILIARIES || this == UTILITY;
    }
}
