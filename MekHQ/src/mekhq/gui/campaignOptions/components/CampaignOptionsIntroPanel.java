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
package mekhq.gui.campaignOptions.components;

import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.CAMPAIGN_OPTIONS_PANEL_WIDTH;

import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;

/**
 * Optional explanatory text shown before a sectioned campaign-options page.
 */
public class CampaignOptionsIntroPanel extends JPanel {
    /**
     * Creates an intro panel that renders the given HTML text, wrapped to the
     * supplied width.
     *
     * @param name      the panel's base name; the Swing component name becomes
     *                  {@code "pnl" + name}
     * @param text      the intro text; may contain HTML markup
     * @param textWidth the width to wrap the text to, in pixels
     */
    public CampaignOptionsIntroPanel(String name, String text, int textWidth) {
        setName("pnl" + name);
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEADING, 0, 0));

        JEditorPane textPane = new JEditorPane("text/html", formatText(text));
        textPane.setName("txt" + name);
        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.setOpaque(false);
        textPane.setBorder(BorderFactory.createEmptyBorder());
        textPane.putClientProperty("JEditorPane.honorDisplayProperties", Boolean.TRUE);
        setFontScaling(textPane, false, 1);

        Dimension textSize = getWrappedTextSize(textPane, getTextWidth(textWidth));
        textPane.setPreferredSize(textSize);
        textPane.setMinimumSize(textSize);
        add(textPane);
    }

    private static int getTextWidth(int textWidth) {
        return Math.max(1, Math.min(textWidth, CAMPAIGN_OPTIONS_PANEL_WIDTH));
    }

    private static Dimension getWrappedTextSize(JEditorPane textPane, int textWidth) {
        textPane.setSize(textWidth, Short.MAX_VALUE);
        Dimension preferredSize = textPane.getPreferredSize();
        return new Dimension(textWidth, preferredSize.height);
    }

    private static String formatText(String text) {
        return "<html><body style='margin: 0; padding: 0;'>" + text + "</body></html>";
    }
}
