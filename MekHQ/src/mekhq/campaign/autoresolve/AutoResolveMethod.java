/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 */
package mekhq.campaign.autoresolve;


import megamek.common.internationalization.Internationalization;
import mekhq.utilities.MHQInternationalization;

import java.util.Optional;

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
        this.toolTipText = Internationalization.getText(toolTipText);
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
