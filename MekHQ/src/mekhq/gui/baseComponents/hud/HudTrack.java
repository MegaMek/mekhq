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
import static mekhq.gui.baseComponents.hud.HudStyle.*;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import javax.swing.JComponent;

import megamek.common.annotations.Nullable;

/**
 * A plot thread's progress track, drawn like the paper track sheet: numbered cells in rows of five, with flashpoints
 * framed in amber, the conclusion framed in the accent colour, revealed cells filled, and the next cell outlined.
 * Hovering a revealed cell shows the tooltip its owner supplies for that step.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudTrack extends JComponent {
    private static final int CELLS_PER_ROW = 5;
    private static final int CELL_HEIGHT = 46;
    private static final int GAP = 4;

    private int steps;
    private int revealed;
    private transient IntPredicate isFlashpoint = number -> false;
    private transient IntFunction<String> tooltips = number -> null;
    private String flashpointLabel = "";
    private String conclusionLabel = "";

    public HudTrack() {
        setOpaque(false);
        // Registers with the tooltip manager; the text itself comes from getToolTipText(MouseEvent).
        setToolTipText("");
    }

    /**
     * Shows a track.
     *
     * @param steps           how many steps the track has
     * @param revealed        how many steps are revealed
     * @param isFlashpoint    tells whether a step number is a flashpoint
     * @param tooltips        the tooltip for a revealed step number, or {@code null} for none
     * @param flashpointLabel the caption under flashpoint cells
     * @param conclusionLabel the caption under the final cell
     */
    public void setTrack(int steps, int revealed, IntPredicate isFlashpoint, IntFunction<String> tooltips,
          String flashpointLabel, String conclusionLabel) {
        this.steps = steps;
        this.revealed = revealed;
        this.isFlashpoint = isFlashpoint;
        this.tooltips = tooltips;
        this.flashpointLabel = flashpointLabel;
        this.conclusionLabel = conclusionLabel;
        revalidate();
        repaint();
    }

    private int rows() {
        return Math.max(1, (steps + CELLS_PER_ROW - 1) / CELLS_PER_ROW);
    }

    @Override
    public Dimension getPreferredSize() {
        int rows = rows();
        return new Dimension(scaleForGUI(300), rows * scaleForGUI(CELL_HEIGHT) + (rows - 1) * scaleForGUI(GAP));
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(scaleForGUI(200), getPreferredSize().height);
    }

    private int cellWidth() {
        return (getWidth() - (CELLS_PER_ROW - 1) * scaleForGUI(GAP)) / CELLS_PER_ROW;
    }

    private @Nullable Integer stepAt(int x, int y) {
        int width = cellWidth();
        int column = x / (width + scaleForGUI(GAP));
        int row = y / (scaleForGUI(CELL_HEIGHT) + scaleForGUI(GAP));
        int number = row * CELLS_PER_ROW + column + 1;
        return (column < CELLS_PER_ROW && number >= 1 && number <= steps) ? number : null;
    }

    @Override
    public @Nullable String getToolTipText(MouseEvent event) {
        Integer number = stepAt(event.getX(), event.getY());
        return (number == null || number > revealed) ? null : tooltips.apply(number);
    }

    /**
     * @return the colour of a cell's number: dark on a filled (revealed) cell, bright on the next one, and otherwise
     *       the cell's own colour
     */
    private static Color textColor(boolean done, boolean next, boolean flashpoint, boolean conclusion) {
        if (done) {
            return GROUND;
        }
        if (next || conclusion) {
            return ACCENT_BRIGHT;
        }
        return flashpoint ? AMBER : TEXT_FAINT;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D canvas = (Graphics2D) graphics.create();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int width = cellWidth();
            int height = scaleForGUI(CELL_HEIGHT);
            Font numberFont = hudFont(Font.BOLD, 1.0f, 0.0f);
            Font captionFont = hudFont(Font.BOLD, 0.58f, 0.12f);
            for (int number = 1; number <= steps; number++) {
                int index = number - 1;
                int x = (index % CELLS_PER_ROW) * (width + scaleForGUI(GAP));
                int y = (index / CELLS_PER_ROW) * (height + scaleForGUI(GAP));
                boolean conclusion = number == steps;
                boolean flashpoint = !conclusion && isFlashpoint.test(number);
                boolean done = number <= revealed;
                boolean next = number == revealed + 1;

                Color frame = conclusion ? ACCENT : flashpoint ? AMBER : BORDER;
                if (done) {
                    Color fill = flashpoint ? AMBER : ACCENT;
                    canvas.setPaint(new GradientPaint(0, y, translucent(fill, 110), 0, y + height, translucent(fill, 50)));
                } else {
                    canvas.setColor(SURFACE_DEEP);
                }
                canvas.fillRect(x, y, width, height);
                canvas.setColor(done ? (flashpoint ? AMBER : ACCENT) : frame);
                canvas.drawRect(x, y, width - 1, height - 1);
                if (next) {
                    canvas.setColor(ACCENT_BRIGHT);
                    canvas.setStroke(new BasicStroke(scaleForGUI(1), BasicStroke.CAP_BUTT,
                          BasicStroke.JOIN_MITER, 10f, new float[] { 4f, 3f }, 0f));
                    canvas.drawRect(x + scaleForGUI(2), y + scaleForGUI(2), width - scaleForGUI(5),
                          height - scaleForGUI(5));
                    canvas.setStroke(new BasicStroke());
                }

                String caption = conclusion ? conclusionLabel : flashpoint ? flashpointLabel : "";
                canvas.setFont(numberFont);
                FontMetrics numberMetrics = canvas.getFontMetrics();
                String label = Integer.toString(number);
                canvas.setColor(textColor(done, next, flashpoint, conclusion));
                int numberY = y + (caption.isEmpty() ? (height + numberMetrics.getAscent()) / 2 - scaleForGUI(2)
                                         : height / 2);
                canvas.drawString(label, x + (width - numberMetrics.stringWidth(label)) / 2, numberY);
                if (!caption.isEmpty()) {
                    canvas.setFont(captionFont);
                    FontMetrics captionMetrics = canvas.getFontMetrics();
                    String text = caption.toUpperCase(Locale.ROOT);
                    canvas.drawString(text, x + (width - captionMetrics.stringWidth(text)) / 2,
                          y + height - scaleForGUI(6));
                }
            }
        } finally {
            canvas.dispose();
        }
    }
}
