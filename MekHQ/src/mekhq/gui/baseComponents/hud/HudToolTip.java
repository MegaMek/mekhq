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

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER_CYAN;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.hex;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.Font;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JToolTip;

import jakarta.annotation.Nullable;

/**
 * A tooltip in the HUD style: the deep surface with a hairline cyan border, as the interstellar map draws the tooltips
 * of its layer controls. A tooltip made with {@link #html(String, String)} also carries a small tracked, upper-case
 * title above its text.
 *
 * <p>Swing asks a component for its tooltip through {@link JComponent#createToolTip()}, so a component opts in by
 * returning {@code new HudToolTip(this)} from that method.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudToolTip extends JToolTip {
    /** The width, before GUI scaling, that long tooltip text wraps at. */
    private static final int WRAP_WIDTH = 280;

    /**
     * @param owner the component the tooltip belongs to
     */
    public HudToolTip(JComponent owner) {
        setComponent(owner);
        setOpaque(true);
        setBackground(SURFACE_DEEP);
        setForeground(TEXT);
        setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
        setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(BORDER_CYAN, Math.max(1, scaleForGUI(1))),
              BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(8), scaleForGUI(5), scaleForGUI(8))));
    }

    /**
     * Builds tooltip text with an optional title line above a body that wraps at a comfortable width. Both are
     * plain text: any markup in them is shown as written.
     *
     * @param title the title, shown tracked and upper-case in the accent colour, or {@code null} for none
     * @param body  the text
     *
     * @return the tooltip text, as HTML
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String html(@Nullable String title, String body) {
        StringBuilder html = new StringBuilder("<html><div style='width:").append(scaleForGUI(WRAP_WIDTH))
                                   .append("px'>");
        if ((title != null) && !title.isBlank()) {
            html.append("<div style='color:").append(hex(ACCENT)).append("; font-weight:bold; letter-spacing:1px'>")
                  .append(escape(title.toUpperCase(Locale.ROOT)))
                  .append("</div>");
        }
        html.append(escape(body)).append("</div></html>");
        return html.toString();
    }

    /**
     * @param text plain text
     *
     * @return the text with the characters HTML treats specially replaced by entities
     *
     * @author Illiani
     * @since 0.51.01
     */
    static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (char character : text.toCharArray()) {
            switch (character) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&#39;");
                case '\n' -> escaped.append("<br>");
                default -> escaped.append(character);
            }
        }
        return escaped.toString();
    }
}
