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
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.gui.baseComponents.hud.HudStyle.translucent;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * The debrief console's verdict banner: a left accent stripe and a fading accent tint over the surface, with a verdict
 * line, a muted reason line beneath it, and a boxed badge on the right. The accent colour carries the verdict's
 * severity.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudVerdictBanner extends JPanel {
    private final JLabel verdictLabel = new JLabel();
    private final JLabel reasonLabel = new JLabel();
    private final JLabel badgeLabel = new JLabel();
    private Color accent = ACCENT;

    /** Builds an empty banner; {@link #setVerdict} fills it in. */
    public HudVerdictBanner() {
        setOpaque(false);
        setLayout(new BorderLayout(scaleForGUI(16), 0));
        int inset = scaleForGUI(11);
        setBorder(BorderFactory.createEmptyBorder(inset, inset + scaleForGUI(4), inset, inset));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        verdictLabel.setFont(hudFont(Font.BOLD, 1.05f, 0.12f));
        text.add(leftAligned(verdictLabel));
        text.add(Box.createVerticalStrut(scaleForGUI(3)));

        reasonLabel.setForeground(TEXT_MUTED);
        reasonLabel.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        text.add(leftAligned(reasonLabel));
        add(text, BorderLayout.CENTER);

        badgeLabel.setFont(hudFont(Font.BOLD, 0.72f, 0.14f));
        badgeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JPanel badgeHolder = new JPanel(new GridBagLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(badgeLabel, new GridBagConstraints());
        add(badgeHolder, BorderLayout.LINE_END);
    }

    /**
     * Shows a verdict.
     *
     * @param verdict the verdict line
     * @param reason  the reason line; HTML is allowed
     * @param badge   the badge text
     * @param accent  the severity colour, used for the stripe, tint, verdict, and badge
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setVerdict(String verdict, String reason, String badge, Color accent) {
        this.accent = accent;
        verdictLabel.setText(verdict.toUpperCase(Locale.ROOT));
        verdictLabel.setForeground(accent);
        reasonLabel.setText(reason);
        badgeLabel.setText(badge.toUpperCase(Locale.ROOT));
        badgeLabel.setForeground(accent);
        badgeLabel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(accent, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(10), scaleForGUI(5), scaleForGUI(10))));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            int width = getWidth();
            int height = getHeight();
            g2.setColor(SURFACE);
            g2.fillRect(0, 0, width, height);
            g2.setPaint(new GradientPaint(0, 0, translucent(accent, 30), width * 0.65f, 0, SURFACE));
            g2.fillRect(0, 0, width, height);
            g2.setColor(BORDER);
            g2.drawRect(0, 0, width - 1, height - 1);
            g2.setColor(accent);
            g2.fillRect(0, 0, scaleForGUI(3), height);
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
