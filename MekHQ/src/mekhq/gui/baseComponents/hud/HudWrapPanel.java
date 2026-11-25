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
package mekhq.gui.baseComponents.hud;

import java.awt.FlowLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import mekhq.gui.utilities.WrapLayout;

/**
 * A transparent row of components that wraps to the width its layout gives it and grows tall enough for every line. A
 * plain panel with a {@link WrapLayout} is first measured before it knows its width, against whichever container around
 * it does; in anything narrower than that container, such as a column beside a portrait, it keeps a height that is too
 * short for its wrapped lines, and they are cut off.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudWrapPanel extends JPanel {
    /**
     * @param horizontalGap the scaled gap between components, and around the row
     * @param verticalGap   the scaled gap between lines, and around the row
     */
    public HudWrapPanel(int horizontalGap, int verticalGap) {
        super(new WrapLayout(FlowLayout.LEFT, horizontalGap, verticalGap));
        setOpaque(false);
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean resized = width != getWidth();
        super.setBounds(x, y, width, height);
        if (resized) {
            // Its height depends on its width, so lay out again now the width is known.
            SwingUtilities.invokeLater(this::revalidate);
        }
    }
}
