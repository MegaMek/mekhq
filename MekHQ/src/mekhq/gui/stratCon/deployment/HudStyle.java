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
package mekhq.gui.stratCon.deployment;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;

/**
 * The heads-up-display styling for the StratCon deployment wizard: a hardcoded cyan-on-dark palette, letter-tracked
 * fonts, and stat-tile factories that give the wizard the same look as the interstellar-map tab (and the contract
 * debrief console it is modelled on). The palette is deliberately fixed rather than theme-derived, because the map
 * chrome it echoes is always dark.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class HudStyle {
    public static final Color GROUND = new Color(5, 13, 23);
    public static final Color SURFACE = new Color(15, 30, 43);
    public static final Color SURFACE_DEEP = new Color(7, 16, 27);
    public static final Color SURFACE_HIGHLIGHT = new Color(18, 45, 56);
    public static final Color BORDER = new Color(35, 66, 82);
    public static final Color BORDER_CYAN = new Color(65, 210, 224, 107);
    public static final Color DIVIDER = new Color(65, 210, 224, 40);
    public static final Color ACCENT = new Color(65, 210, 224);
    public static final Color ACCENT_BRIGHT = new Color(125, 230, 238);
    public static final Color AMBER = new Color(235, 166, 66);
    public static final Color TEXT = new Color(218, 231, 235);
    public static final Color TEXT_MUTED = new Color(158, 179, 187);
    public static final Color TEXT_FAINT = new Color(132, 153, 161);

    // Readiness / status colours, aligned with the debrief console's outcome palette.
    public static final Color READY = new Color(82, 199, 160);
    public static final Color CAUTION = new Color(235, 166, 66);
    public static final Color DANGER = new Color(215, 87, 79);
    public static final Color INELIGIBLE = new Color(132, 153, 161);

    private HudStyle() {}

    /**
     * @param style      a {@link Font} style constant
     * @param sizeFactor multiplier on the base label font size
     * @param tracking   letter spacing (0 for none)
     *
     * @return a HUD font derived from the label font
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Font hudFont(int style, float sizeFactor, float tracking) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) {
            base = new JLabel().getFont();
        }
        Font sized = base.deriveFont(style, base.getSize2D() * sizeFactor);
        if (tracking == 0.0f) {
            return sized;
        }
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return sized.deriveFont(attributes);
    }

    public static Color translucent(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    /** @return the {@code #RRGGBB} string for a colour, for use in HTML spans */
    public static String hex(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    /** A small caps-style key label, matching the map's inset-panel headings. */
    public static JLabel keyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        return label;
    }

    /**
     * Builds a stat tile - a deep-surface cell with a small key, a large coloured value, and an optional sub-line -
     * matching the debrief console's stat tiles. Used for the wizard's budget meters.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static JComponent statTile(String key, String value, @Nullable String sub, Color valueColor) {
        JPanel tile = new JPanel();
        tile.setLayout(new BoxLayout(tile, BoxLayout.Y_AXIS));
        tile.setOpaque(true);
        tile.setBackground(SURFACE_DEEP);
        int inset = UIUtil.scaleForGUI(11);
        tile.setBorder(BorderFactory.createEmptyBorder(inset, inset, inset, inset));

        tile.add(leftAligned(keyLabel(key)));
        tile.add(Box.createVerticalStrut(UIUtil.scaleForGUI(4)));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(valueColor);
        valueLabel.setFont(hudFont(Font.BOLD, 1.55f, 0.0f));
        tile.add(leftAligned(valueLabel));

        if (sub != null) {
            tile.add(Box.createVerticalStrut(UIUtil.scaleForGUI(3)));
            JLabel subLabel = new JLabel(sub);
            subLabel.setForeground(TEXT_FAINT);
            subLabel.setFont(hudFont(Font.PLAIN, 0.78f, 0.0f));
            tile.add(leftAligned(subLabel));
        }
        return tile;
    }

    public static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }
}
