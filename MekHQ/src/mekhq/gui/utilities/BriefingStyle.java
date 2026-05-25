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
package mekhq.gui.utilities;

import static megamek.client.ui.util.UIUtil.scaleForGUI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;

import megamek.client.ui.util.UIUtil;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public final class BriefingStyle {
    private static final String FLATLAF_STYLE_CLASS = "FlatLaf.styleClass";
    private static final int SECTION_CONTENT_GAP = scaleForGUI(6);
    private static final int SECTION_TOP_MARGIN = scaleForGUI(4);
    private static final int SECTION_BORDER_THICKNESS = scaleForGUI(2);
    private static final int SECTION_BORDER_ARC = scaleForGUI(10);
    private static final int SECTION_BORDER_PADDING = scaleForGUI(8);

    private BriefingStyle() {
    }

    public static JPanel createSectionPanel(String title) {
        return createSectionPanel(title, 0, 0, 0, 0);
    }

    public static JPanel createSectionPanel(String title, int top, int left, int bottom, int right) {
        JPanel panel = new JPanel(new BorderLayout(0, SECTION_CONTENT_GAP));
        panel.setBorder(createSectionBorder(title, top, left, bottom, right));
        return panel;
    }

    public static Border createSectionBorder(String title) {
        return createSectionBorder(title, 0, 0, 0, 0);
    }

    public static Border createSectionBorder(String title, int top, int left, int bottom, int right) {
        TitledBorder titledBorder = RoundedLineBorder.createRoundedLineBorder(title,
              getSubtleBorderColor(),
              SECTION_BORDER_THICKNESS,
              SECTION_BORDER_ARC,
              SECTION_BORDER_PADDING);
        titledBorder.setTitleFont(getSectionTitleFont());

        Border sectionBorder = BorderFactory.createCompoundBorder(
              BorderFactory.createEmptyBorder(SECTION_TOP_MARGIN, 0, 0, 0),
              titledBorder);
        if ((top == 0) && (left == 0) && (bottom == 0) && (right == 0)) {
            return sectionBorder;
        }
        return BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(scaleForGUI(top),
                scaleForGUI(left),
                scaleForGUI(bottom),
                scaleForGUI(right)),
              sectionBorder);
    }

    private static Font getSectionTitleFont() {
        JLabel title = new JLabel();
        title.putClientProperty(FLATLAF_STYLE_CLASS, "small");
        return title.getFont().deriveFont(Font.BOLD, title.getFont().getSize2D() + 2.0f);
    }

    private static Color getSubtleBorderColor() {
        Color color = UIManager.getColor("Component.borderColor");
        if (color == null) {
            color = UIManager.getColor("Separator.foreground");
        }
        if (color == null) {
            color = UIManager.getColor("controlShadow");
        }
        return (color == null) ? UIUtil.uiIndependentGray() : color;
    }
}