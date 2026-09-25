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
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_HIGHLIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * A segmented tab strip in the style of the deployment wizard's mode selector: one cell per page, the selected cell
 * raised to the surface colour with the map's accent underline. Fires on mouse release and on space/enter.
 * {@code compact} draws a lighter, smaller strip for a second level of tabs.
 *
 * <p>Use {@link HudModeSelector} when the pages are the values of an enum.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudTabStrip extends JPanel {
    private final List<Cell> cells = new ArrayList<>();
    private final transient IntConsumer onSelect;

    public HudTabStrip(List<String> labels, boolean compact, IntConsumer onSelect) {
        this.onSelect = onSelect;
        setLayout(new GridLayout(1, labels.size(), 0, 0));
        setOpaque(true);
        setBackground(SURFACE_DEEP);
        setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        for (int index = 0; index < labels.size(); index++) {
            Cell cell = new Cell(index, labels.get(index), compact, index < labels.size() - 1);
            cells.add(cell);
            add(cell);
        }
    }

    /** Highlights a cell without firing the selection callback. */
    public void setSelected(int selectedIndex) {
        for (int index = 0; index < cells.size(); index++) {
            cells.get(index).setSelected(index == selectedIndex);
        }
    }

    /** Replaces a cell's label (for example to update a count). */
    public void setLabel(int index, String label) {
        cells.get(index).title.setText(label.toUpperCase(Locale.ROOT));
    }

    private final class Cell extends JPanel {
        private final int index;
        private final boolean rightBorder;
        private final JLabel title;
        private boolean selected;
        private boolean hovered;

        private Cell(int index, String label, boolean compact, boolean rightBorder) {
            this.index = index;
            this.rightBorder = rightBorder;
            setOpaque(false);
            setFocusable(true);
            int vertical = scaleForGUI(compact ? 7 : 10);
            setBorder(BorderFactory.createEmptyBorder(vertical, scaleForGUI(6), vertical, scaleForGUI(6)));
            setLayout(new GridBagLayout());

            title = new JLabel(label.toUpperCase(Locale.ROOT), SwingConstants.CENTER);
            title.setForeground(TEXT_MUTED);
            title.setFont(hudFont(Font.BOLD, compact ? 0.76f : 0.85f, compact ? 0.1f : 0.06f));
            add(title);

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
                    refreshTitleColor();
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent event) {
                    hovered = false;
                    refreshTitleColor();
                    repaint();
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
        }

        private void choose() {
            requestFocusInWindow();
            onSelect.accept(index);
        }

        private void setSelected(boolean selected) {
            this.selected = selected;
            refreshTitleColor();
            repaint();
        }

        private void refreshTitleColor() {
            title.setForeground((selected || hovered) ? TEXT : TEXT_MUTED);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                int width = getWidth();
                int height = getHeight();
                g2.setColor(selected ? SURFACE : (hovered ? SURFACE_HIGHLIGHT : SURFACE_DEEP));
                g2.fillRect(0, 0, width, height);
                if (rightBorder) {
                    g2.setColor(BORDER);
                    g2.fillRect(width - scaleForGUI(1), 0, scaleForGUI(1), height);
                }
                if (selected) {
                    int underline = scaleForGUI(2);
                    g2.setColor(ACCENT);
                    g2.fillRect(0, height - underline, width, underline);
                }
            } finally {
                g2.dispose();
            }
            super.paintComponent(graphics);
        }
    }
}
