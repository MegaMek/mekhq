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

import static mekhq.gui.stratCon.deployment.HudStyle.*;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;

/**
 * A custom-painted HUD button matching the interstellar-map chrome: a translucent cyan-gradient primary button, or a
 * flat surface secondary button, both with a hairline border and a hover lift. Fires on mouse release (a click is
 * swallowed by the slightest pointer drift) and on space/enter.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudButton extends JPanel {
    private final JLabel label;
    private final boolean primary;
    private boolean hovered;
    private boolean armed = true;
    private final transient List<ActionListener> listeners = new ArrayList<>();

    public HudButton(String text, boolean primary) {
        this.primary = primary;
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(9), UIUtil.scaleForGUI(22),
              UIUtil.scaleForGUI(9), UIUtil.scaleForGUI(22)));
        setFocusable(true);

        label = new JLabel(text);
        label.setFont(hudFont(Font.BOLD, 0.82f, 0.12f));
        label.setForeground(primary ? ACCENT_BRIGHT : TEXT_MUTED);
        add(label);

        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (armed && contains(event.getPoint())) {
                    fire();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event) {
                hovered = true;
                refreshForeground();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hovered = false;
                refreshForeground();
                repaint();
            }
        };
        addMouseListener(mouseAdapter);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                if (armed && ((event.getKeyCode() == KeyEvent.VK_SPACE) || (event.getKeyCode() == KeyEvent.VK_ENTER))) {
                    fire();
                }
            }
        });
    }

    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void setText(String text) {
        label.setText(text);
    }

    /** Enables or disables the button (a disabled button dims and stops firing). */
    public void setArmed(boolean armed) {
        this.armed = armed;
        refreshForeground();
        repaint();
    }

    public boolean isArmed() {
        return armed;
    }

    private void refreshForeground() {
        if (!armed) {
            label.setForeground(TEXT_MUTED);
            return;
        }
        if (primary) {
            label.setForeground(hovered ? Color.WHITE : ACCENT_BRIGHT);
        } else {
            label.setForeground(hovered ? TEXT : TEXT_MUTED);
        }
    }

    private void fire() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "clicked");
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            int width = getWidth();
            int height = getHeight();
            float dim = armed ? 1.0f : 0.5f;
            if (primary) {
                g2.setPaint(new GradientPaint(0, 0, translucent(ACCENT, (int) ((hovered ? 66 : 40) * dim)),
                      0, height, translucent(ACCENT, (int) ((hovered ? 26 : 13) * dim))));
                g2.fillRect(0, 0, width, height);
                g2.setColor(BORDER_CYAN);
            } else {
                g2.setColor(hovered && armed ? SURFACE_HIGHLIGHT : SURFACE);
                g2.fillRect(0, 0, width, height);
                g2.setColor(BORDER);
            }
            g2.drawRect(0, 0, width - 1, height - 1);
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }
}
