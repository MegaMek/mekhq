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
package mekhq.gui.dialog.quartermaster;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.stratCon.deployment.HudStyle.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import megamek.client.ui.WrapLayout;

/**
 * A single selectable kit tile shared by every tab of the kit-issue dialog (armor kits and tool kits alike), drawn as
 * a heads-up-display card in the style of the interstellar-map chrome: a deep-surface panel with a hairline border and
 * a thin coloured band along the top, a bold name, muted detail lines, optional sealing badges, and a stock/price foot.
 * Hovering lifts the card; the selected card is raised to the surface colour, outlined in the accent colour, and lit
 * by a faint accent glow under its band. The caller supplies the content and a {@link BooleanSupplier} that tells the
 * card whether it is currently the selection, plus a click handler; the card owns only its look.
 *
 * @author Illiani
 * @since 0.51.01
 */
class KitCard extends JPanel {
    /** A small pill shown on a card: {@code accented} draws it in the accent colour, otherwise faint. */
    record Badge(String text, boolean accented) {}

    private static final int BAND_HEIGHT = 3;

    private final Color band;
    private final boolean special;
    private final transient BooleanSupplier selected;
    private final JLabel nameLabel;
    private boolean hovered;
    /** The selection state last drawn, so a refresh only restyles a card whose state actually changed. */
    private Boolean drawnSelected;

    /**
     * @param band      the colour of the card's top band, identifying its kit group
     * @param title     the kit name (word-wrapped to the card width)
     * @param special   {@code true} for a non-kit action tile (strip / return-to-designed): faint band, no foot
     * @param detail    muted detail lines under the name (e.g. divisor/encumbrance, acquisition difficulty, effect)
     * @param badges    optional pills under the detail lines (environmental seals, "no protection", etc.)
     * @param stockText the foot's left text (e.g. "0 in stock"), or {@code null} for no foot
     * @param stockWarn draw the stock text in the shortage colour
     * @param priceText the foot's right text (e.g. "250 ea"), or {@code null}
     * @param selected  tells the card whether it is the current selection, re-read on every {@link #refreshSelected()}
     * @param onClick   run when the card is clicked
     */
    KitCard(Color band, String title, boolean special, List<String> detail, List<Badge> badges, String stockText,
          boolean stockWarn, String priceText, BooleanSupplier selected, Runnable onClick) {
        super(new BorderLayout());
        this.band = special ? TEXT_FAINT : band;
        this.special = special;
        this.selected = selected;

        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = scaleForGUI(250, 168);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setAlignmentY(TOP_ALIGNMENT);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(10 + BAND_HEIGHT), scaleForGUI(12),
              scaleForGUI(10), scaleForGUI(12)));

        nameLabel = label(title, 26, hudFont(Font.BOLD, 1.0f, 0.02f), TEXT);
        body.add(nameLabel);

        Font detailFont = hudFont(Font.PLAIN, 0.84f, 0.0f);
        for (String line : detail) {
            body.add(Box.createVerticalStrut(scaleForGUI(3)));
            body.add(label(line, 34, detailFont, TEXT_MUTED));
        }

        if ((badges != null) && !badges.isEmpty()) {
            body.add(Box.createVerticalStrut(scaleForGUI(6)));
            JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            badgeRow.setOpaque(false);
            badgeRow.setAlignmentX(LEFT_ALIGNMENT);
            for (Badge badge : badges) {
                badgeRow.add(badge(badge.text(), badge.accented() ? ACCENT : TEXT_FAINT));
                badgeRow.add(Box.createHorizontalStrut(scaleForGUI(4)));
            }
            body.add(badgeRow);
        }

        if (stockText != null) {
            body.add(Box.createVerticalGlue());
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.setAlignmentX(LEFT_ALIGNMENT);
            foot.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, DIVIDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(6), 0, 0, 0)));
            JLabel stockLabel = new JLabel(stockText.toUpperCase(Locale.ROOT));
            stockLabel.setFont(hudFont(Font.BOLD, 0.72f, 0.12f));
            stockLabel.setForeground(stockWarn ? AMBER : TEXT_FAINT);
            foot.add(stockLabel, BorderLayout.WEST);
            if (priceText != null) {
                JLabel price = new JLabel(priceText);
                price.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
                price.setForeground(TEXT);
                foot.add(price, BorderLayout.EAST);
            }
            body.add(foot);
        }

        add(body, BorderLayout.CENTER);

        refreshSelected();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                // Released, not clicked: a click is lost if the pointer drifts at all between press and release.
                // Releasing outside the card cancels, as a button would.
                if (SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint())) {
                    onClick.run();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }
        });
    }

    /**
     * A label with its font and colour applied before its text, so an HTML label is parsed once rather than again on
     * each style change. HTML is only used when the text actually needs wrapping; short text stays plain, which is far
     * cheaper to build and to restyle.
     */
    private static JLabel label(String text, int wrapAt, Font font, Color color) {
        JLabel label = new JLabel();
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(LEFT_ALIGNMENT);
        boolean needsWrap = (text.length() > wrapAt) || text.contains("<br>");
        label.setText(needsWrap ? wordWrap(text, wrapAt) : text);
        return label;
    }

    /**
     * Re-reads the selection state and repaints; call after the selection changes. A card whose state is unchanged is
     * left alone, so a click restyles only the cards it selected or deselected.
     */
    void refreshSelected() {
        boolean isSelected = selected.getAsBoolean();
        if (Boolean.valueOf(isSelected).equals(drawnSelected)) {
            return;
        }
        drawnSelected = isSelected;
        nameLabel.setForeground(isSelected ? ACCENT_BRIGHT : TEXT);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            int width = getWidth();
            int height = getHeight();
            boolean isSelected = Boolean.TRUE.equals(drawnSelected);

            g2.setColor(isSelected ? SURFACE : (hovered ? SURFACE_HIGHLIGHT : SURFACE_DEEP));
            g2.fillRect(0, 0, width, height);

            int bandHeight = scaleForGUI(BAND_HEIGHT);
            if (isSelected) {
                // A faint accent glow falling away beneath the band, as the map lights its selected system.
                g2.setPaint(new GradientPaint(0, bandHeight, translucent(ACCENT, 46),
                      0, height / 2.0f, translucent(ACCENT, 0)));
                g2.fillRect(0, bandHeight, width, height / 2);
            }

            g2.setColor(isSelected ? ACCENT : (special ? translucent(band, 150) : band));
            g2.fillRect(0, 0, width, bandHeight);

            g2.setColor(isSelected ? ACCENT : (hovered ? translucent(ACCENT, 90) : BORDER));
            g2.drawRect(0, 0, width - 1, height - 1);
        } finally {
            g2.dispose();
        }
        super.paintComponent(graphics);
    }

    /**
     * Lays a set of cards out as a top-anchored grid that wraps to as many columns as the width allows, on the
     * dialog's dark ground. Returned ready to drop into a vertically scrolling pane.
     */
    static JComponent grid(List<KitCard> cards, Color background) {
        JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(10)));
        grid.setOpaque(true);
        grid.setBackground(background);
        for (KitCard card : cards) {
            grid.add(card);
        }
        // NORTH pins the wrap grid to the top at full width, so cards fill from the top-left instead of centering.
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(true);
        top.setBackground(background);
        top.add(grid, BorderLayout.NORTH);
        return top;
    }

    /** A pill: tracked, upper-case small text inside a hairline outline in its colour. */
    private static JLabel badge(String text, Color color) {
        JLabel badge = new JLabel(text.toUpperCase(Locale.ROOT));
        badge.setFont(hudFont(Font.BOLD, 0.66f, 0.1f));
        badge.setForeground(color);
        badge.setOpaque(false);
        badge.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(translucent(color, 120), 1),
              BorderFactory.createEmptyBorder(scaleForGUI(2), scaleForGUI(6), scaleForGUI(2), scaleForGUI(6))));
        return badge;
    }

    /** Renders a damage divisor without a trailing ".0" for whole numbers. */
    static String formatDivisor(double divisor) {
        return (divisor == Math.rint(divisor)) ? String.valueOf((int) divisor) : String.valueOf(divisor);
    }
}
