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
 */
package mekhq.gui.utilities;

import java.awt.Component;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.client.ui.clientGUI.GUIPreferences;

/**
 * Use the version housed in MegaMek, instead
 */
@Deprecated(since = "0.50.07")
public class JScrollPaneWithSpeed extends JScrollPane {
    static final int BASE_INCREMENT = 16;

    /**
     * @see JPanel#JPanel()
     */
    public JScrollPaneWithSpeed() {
        super(null);
        setScaleIncrement();
    }

    /**
     * @see JPanel#JPanel()
     */
    public JScrollPaneWithSpeed(Component view) {
        super(view);
        setScaleIncrement();
    }

    /**
     * @see JPanel#JPanel()
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
