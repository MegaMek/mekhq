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

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;

/**
 * The shared heads-up-display styling used by the salvage dialogs, the quartermaster kit dialog and the StratCon
 * deployment wizard: a hardcoded cyan-on-dark palette and letter-tracked fonts that give these screens the same look
 * as the interstellar-map tab (and the contract debrief console it is modelled on). The palette is deliberately fixed
 * rather than theme-derived, because the map chrome it echoes is always dark.
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
    /** The scrollbar thumb colour the debrief console uses. */
    public static final Color SCROLLBAR_THUMB = new Color(54, 101, 113);

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

    /**
     * @param color the base colour
     * @param alpha the new alpha, from {@code 0} (fully transparent) to {@code 255} (opaque)
     *
     * @return a copy of {@code color} with the given alpha
     */
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
     * Left-aligns a component for use in a vertical {@code BoxLayout}.
     *
     * @param component the component to align
     *
     * @return the same component, for chaining
     */
    public static JComponent leftAligned(JComponent component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        return component;
    }
}
