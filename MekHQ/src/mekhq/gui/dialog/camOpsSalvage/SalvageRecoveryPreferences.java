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
package mekhq.gui.dialog.camOpsSalvage;

import megamek.client.ui.preferences.PreferencesNode;
import mekhq.MekHQ;
import mekhq.gui.preferences.PreferenceFlags;

/**
 * The {@link SalvageRecoveryConsole}'s remembered choices: currently, whether the salvage techs work carefully, which
 * is on until the player turns it off.
 *
 * <p>They are stored per player in MekHQ's preferences file ({@code mmconf/mhq.preferences}), under this class's node,
 * so they follow the MekHQ install rather than a campaign.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SalvageRecoveryPreferences {
    static final String CAREFUL_SALVAGE = "carefulSalvage";

    private static SalvageRecoveryPreferences instance;

    private final PreferenceFlags flags;

    /**
     * @param node the preferences node to store the settings under
     *
     * @author Illiani
     * @since 0.51.01
     */
    SalvageRecoveryPreferences(final PreferencesNode node) {
        flags = new PreferenceFlags(node);
        flags.register(CAREFUL_SALVAGE, true);
    }

    /**
     * @return the settings, stored in MekHQ's preferences file
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static synchronized SalvageRecoveryPreferences getInstance() {
        if (instance == null) {
            instance = new SalvageRecoveryPreferences(MekHQ.getMHQPreferences()
                                                            .forClass(SalvageRecoveryPreferences.class));
        }
        return instance;
    }

    /**
     * @return {@code true} if the salvage techs work carefully, taking longer to avoid accidents
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isCarefulSalvage() {
        return flags.get(CAREFUL_SALVAGE);
    }

    /**
     * @param isCarefulSalvage {@code true} to have the salvage techs work carefully
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setCarefulSalvage(final boolean isCarefulSalvage) {
        flags.set(CAREFUL_SALVAGE, isCarefulSalvage);
    }
}
