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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT_BRIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER_CYAN;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A small filter chip: a tracked, upper-case label with a hairline border, raised and outlined in cyan when active.
 * Chips are usually grouped so that exactly one is active, filtering a board. Fires on mouse release and on
 * space/enter.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudChip extends JPanel {
    private final JLabel label = new JLabel();
    private final transient Runnable onChoose;
    private boolean isActive;
    private boolean hovered;

    /**
     * @param text     the chip's text
     * @param onChoose called when the player chooses the chip
     */
    public HudChip(String text, Runnable onChoose) {
        this.onChoose = onChoose;
        setOpaque(false);
        setFocusable(true);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(8), scaleForGUI(3), scaleForGUI(8)));
        label.setFont(hudFont(Font.BOLD, 0.72f, 0.1f));
        setText(text);
        add(label);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (contains(event.getPoint())) {
                    choose();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                refresh();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                refresh();
            }
        });
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if ((event.getKeyCode() == KeyEvent.VK_SPACE) || (event.getKeyCode() == KeyEvent.VK_ENTER)) {
                    choose();
                }
            }
        });
        refresh();
    }

    /**
     * Replaces the chip's text, for example to update a count.
     *
     * @param text the new text
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setText(String text) {
        label.setText(text.toUpperCase(Locale.ROOT));
    }

    /**
     * Marks the chip active or inactive, without notifying the listener.
     *
     * @param isActive {@code true} if the chip's filter is in effect
     *
     * @author Illiani
     * @since 0.51.01
     */
    public void setActive(boolean isActive) {
        this.isActive = isActive;
        refresh();
    }

    private void choose() {
        requestFocusInWindow();
        onChoose.run();
    }

    private void refresh() {
        label.setForeground(isActive ? ACCENT_BRIGHT : (hovered ? TEXT : TEXT_FAINT));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            int width = getWidth();
            int height = getHeight();
            g2.setColor(isActive ? SURFACE : SURFACE_DEEP);
            g2.fillRect(0, 0, width, height);
            g2.setColor(isActive ? BORDER_CYAN : BORDER);
            g2.drawRect(0, 0, width - 1, height - 1);
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
