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

import java.awt.Dimension;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

/**
 * A label for HTML text that wraps to the width its layout gives it and grows tall enough for every line. A plain
 * {@link JLabel} keeps the one-line height it measured before it knew its width, so wrapped lines overlap whatever sits
 * below. Its preferred width never exceeds the width it was last given, and it can shrink to nothing, so it never
 * widens the row it sits in. In a vertical {@code BoxLayout} it stretches to the column's width, so it grows again when
 * the window widens.
 *
 * <p>Give it HTML text, such as {@code "<html>" + text + "</html>"}; plain text does not wrap.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudWrappingLabel extends JLabel {
    /** An empty label; set its text later. */
    public HudWrappingLabel() {
        super();
    }

    /**
     * @param html the label's HTML text
     */
    public HudWrappingLabel(String html) {
        super(html);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        int width = getWidth();
        View view = (View) getClientProperty(BasicHTML.propertyKey);
        if (view == null || width <= 0 || natural.width <= width) {
            return natural;
        }
        Insets insets = getInsets();
        int textWidth = Math.max(1, width - insets.left - insets.right);
        view.setSize(textWidth, 0);
        int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom;
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(0, getPreferredSize().height);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
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
