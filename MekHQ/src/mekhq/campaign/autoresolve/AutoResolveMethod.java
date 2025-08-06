/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.autoresolve;

import java.util.Optional;

import mekhq.utilities.MHQInternationalization;

/**
 * @author Luana Coppio
 */
public enum AutoResolveMethod {
    PRINCESS("AutoResolveMethod.PRINCESS.text", "AutoResolveMethod.PRINCESS.toolTipText"),
    ABSTRACT_COMBAT("AutoResolveMethod.ABSTRACT_COMBAT.text", "AutoResolveMethod.ABSTRACT_COMBAT.toolTipText");

    private final String name;
    private final String toolTipText;

    AutoResolveMethod(final String name, final String toolTipText) {
        this.name = MHQInternationalization.getText(name);
        this.toolTipText = MHQInternationalization.getText(toolTipText);
    }

    public String getToolTipText() {
        return toolTipText;
    }

    public String getName() {
        return name;
    }

    public static Optional<AutoResolveMethod> fromIntSafe(int index) {
        if (index < 0 || index >= values().length) {
            return Optional.empty();
        }
        return Optional.of(values()[index]);
    }

    public static Optional<AutoResolveMethod> fromStringSafe(String method) {
        return switch (method.toUpperCase()) {
            case "PRINCESS" -> Optional.of(PRINCESS);
            case "ABSTRACT_COMBAT" -> Optional.of(ABSTRACT_COMBAT);
            default -> Optional.empty();
        };
    }

    @Override
    public String toString() {
        return name;
    }
}
