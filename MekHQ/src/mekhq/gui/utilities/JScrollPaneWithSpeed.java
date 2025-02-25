/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.utilities;

import megamek.client.ui.swing.GUIPreferences;

import javax.swing.*;
import java.awt.*;

/**
 * It's a JScrollPane that manages its scrollspeed based on the UI scale
 */
public class JScrollPaneWithSpeed extends JScrollPane {
    final static int BASE_INCREMENT = 16;

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
