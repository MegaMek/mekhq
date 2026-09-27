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
package mekhq.gui.roleplay;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.Locale;

/**
 * The pages of the {@link OracleConsole}.
 */
public enum ConsolePage {
    ASK("ASK"),
    THREADS("THREADS"),
    CAST("CAST"),
    JOURNAL("JOURNAL");

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final String key;

    ConsolePage(String key) {
        this.key = key;
    }

    /**
     * @return the page's name in the page selector
     */
    public String getLabel() {
        return getTextAt(RESOURCE_BUNDLE, "OracleConsole.page." + key);
    }

    /**
     * @return the label of the page's guide button, such as "How asking works"
     */
    public String getGuideLabel() {
        return getTextAt(RESOURCE_BUNDLE, "OracleConsole.guide." + key);
    }

    /**
     * @return the key this page's guide text is stored under
     */
    String guideKey() {
        return key.toLowerCase(Locale.ROOT);
    }
}
