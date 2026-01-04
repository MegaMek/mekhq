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
package mekhq.gui.campaignOptions;

import java.util.MissingResourceException;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * Special flags that can be applied to campaign options to indicate important characteristics.
 * Each flag is displayed as a symbol in the UI.
 */
public enum CampaignOptionFlag {
    /** Custom system unique to MekHQ */
    CUSTOM_SYSTEM("CUSTOM_SYSTEM"),

    /** Documentation included in MekHQ/docs */
    DOCUMENTED("DOCUMENTED"),

    /** Tooltip contains important information */
    IMPORTANT("IMPORTANT"),

    /** Tooltip contains a recommendation */
    RECOMMENDED("RECOMMENDED");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.CampaignOptionsDialog";

    private final String key;

    CampaignOptionFlag(String key) {
        this.key = key;
    }

    /**
     * Gets the symbol for this flag from the properties file.
     *
     * @return the symbol, or "?" if not found
     */
    private String loadSymbol() {
        try {
            return getTextAt(RESOURCE_BUNDLE, "flag." + key + ".symbol");
        } catch (MissingResourceException e) {
            return "?";
        }
    }

    /**
     * Gets the description for this flag from the properties file.
     *
     * @return the description, or the flag name if not found
     */
    private String loadDescription() {
        try {
            return getTextAt(RESOURCE_BUNDLE, "flag." + key + ".description");
        } catch (MissingResourceException e) {
            return key;
        }
    }

    /**
     * Gets the Unicode symbol for this flag.
     *
     * @return the symbol string (e.g., "\u270E", "\u2318", "\u26A0", "\u2714")
     */
    public String getSymbol() {
        return loadSymbol();
    }

    /**
     * Gets the human-readable description of what this flag means.
     *
     * @return the description
     */
    public String getDescription() {
        return loadDescription();
    }
}
