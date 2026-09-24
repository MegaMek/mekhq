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
package mekhq.campaign.enums;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Controls how Lithium-Fusion (LF) batteries fitted to the campaign's own JumpShips and WarShips affect interstellar
 * travel.
 *
 * <p>The benefit only applies when every ship performing the jump is player-owned and carries a working LF battery;
 * see {@link mekhq.campaign.utilities.LithiumFusionBatteries}.</p>
 *
 * <ul>
 *     <li>{@link #DISABLED} - LF batteries have no effect on travel.</li>
 *     <li>{@link #HALVED_RECHARGE} - the recharge time between jumps is halved, modelling the sourcebook practice of
 *     LF-equipped ships jumping roughly every four days rather than stressing the drive with back-to-back jumps.</li>
 *     <li>{@link #DOUBLE_JUMP} - the battery holds a second full jump charge, allowing two jumps in immediate
 *     succession. The drive core recharges first, then the battery.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum LithiumFusionBatteryMode {
    DISABLED("DISABLED"),
    HALVED_RECHARGE("HALVED_RECHARGE"),
    DOUBLE_JUMP("DOUBLE_JUMP");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.LithiumFusionBatteryMode";

    private final String label;
    private final String tooltip;

    LithiumFusionBatteryMode(final String lookUpName) {
        this.label = getTextAt(RESOURCE_BUNDLE, "LithiumFusionBatteryMode." + lookUpName + ".label");
        this.tooltip = getTextAt(RESOURCE_BUNDLE, "LithiumFusionBatteryMode." + lookUpName + ".tooltip");
    }

    /**
     * @return {@code true} unless this mode is {@link #DISABLED}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isEnabled() {
        return this != DISABLED;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isHalvedRecharge() {
        return this == HALVED_RECHARGE;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isDoubleJump() {
        return this == DOUBLE_JUMP;
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
