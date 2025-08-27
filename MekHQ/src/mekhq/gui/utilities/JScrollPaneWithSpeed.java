/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.utilities;

import java.awt.Component;
import javax.swing.JScrollPane;

import megamek.client.ui.clientGUI.GUIPreferences;
import megamek.common.ui.FastJScrollPane;

/**
 * Use {@link FastJScrollPane} instead
 */
@Deprecated(since = "0.50.07")
public class JScrollPaneWithSpeed extends JScrollPane {
    static final int BASE_INCREMENT = 16;

    /**
     * Use {@link FastJScrollPane#FastJScrollPane()} instead
     */
    public JScrollPaneWithSpeed() {
        super(null);
        setScaleIncrement();
    }

    /**
     * Use {@link FastJScrollPane#FastJScrollPane()} instead
     */
    public JScrollPaneWithSpeed(Component view) {
        super(view);
        setScaleIncrement();
    }

    /**
     * Use {@link FastJScrollPane#FastJScrollPane()} instead
     */
    public JScrollPaneWithSpeed(Component view, int vsbPolicy, int hsbPolicy) {
        super(view, vsbPolicy, hsbPolicy);
        setScaleIncrement();
    }

    /**
     * Set the panel's scroll increments based on the UI scale
     */
    private void setScaleIncrement() {
        float scale = GUIPreferences.getInstance().getGUIScale();

        int increment = (int) (scale * BASE_INCREMENT);

        getVerticalScrollBar().setUnitIncrement(increment);
        getHorizontalScrollBar().setUnitIncrement(increment);
    }
}
