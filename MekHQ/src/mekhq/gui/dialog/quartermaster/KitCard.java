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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BooleanSupplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A single selectable kit tile shared by every tab of the kit-issue dialog (armor kits and tool kits alike). It renders
 * the same way for both families - an accent band, a bold name, muted detail lines, optional badges, and a stock/price
 * foot - and highlights itself when selected. The caller supplies the content and a {@link BooleanSupplier} that tells
 * the card whether it is currently the selection, plus a click handler; the card owns only its look.
 *
 * @author Illiani
 * @since 0.51.01
 */
class KitCard extends JPanel {
    /** A small pill shown on a card: {@code accented} draws it in the card's accent color, otherwise muted. */
    record Badge(String text, boolean accented) {}

    private final Color accent;
    private final boolean special;
    private final Color baseBackground;
    private final transient BooleanSupplier selected;
    private final JPanel band;
    private final JPanel body;
    private final JLabel nameLabel;
    private final Color defaultForeground;

    /**
     * @param accent    the card's accent color (band, selection highlight, accented badges)
     * @param title     the kit name (word-wrapped to the card width)
     * @param special   {@code true} for a non-kit action tile (strip / return-to-designed): gray band, no foot
     * @param detail    muted detail lines under the name (e.g. divisor/encumbrance, acquisition difficulty, effect)
     * @param badges    optional pills under the detail lines (environmental seals, "no protection", etc.)
     * @param stockText the foot's left text (e.g. "0 in stock"), or {@code null} for no foot
     * @param stockWarn draw the stock text in the shortage color
     * @param priceText the foot's right text (e.g. "250 ea"), or {@code null}
     * @param selected  tells the card whether it is the current selection, re-read on every {@link #refreshSelected()}
     * @param onClick   run when the card is clicked
     */
    KitCard(Color accent, String title, boolean special, List<String> detail, List<Badge> badges, String stockText,
          boolean stockWarn, String priceText, BooleanSupplier selected, Runnable onClick) {
        super(new BorderLayout());
        this.accent = accent;
        this.special = special;
        this.selected = selected;
        this.baseBackground = getBackground();

        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        Dimension size = scaleForGUI(250, 168);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setAlignmentY(TOP_ALIGNMENT);

        band = new JPanel();
        band.setPreferredSize(scaleForGUI(1, 6));
        add(band, BorderLayout.NORTH);

        nameLabel = new JLabel(wordWrap(title, 26));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        nameLabel.setAlignmentX(LEFT_ALIGNMENT);
        this.defaultForeground = nameLabel.getForeground();

        body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(8),
              scaleForGUI(10)));
        body.add(nameLabel);

        for (String line : detail) {
            body.add(Box.createVerticalStrut(scaleForGUI(3)));
            JLabel label = new JLabel(wordWrap(line, 34));
            label.setForeground(mutedColor());
            label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() - 1f));
            label.setAlignmentX(LEFT_ALIGNMENT);
            body.add(label);
        }

        if ((badges != null) && !badges.isEmpty()) {
            body.add(Box.createVerticalStrut(scaleForGUI(5)));
            JPanel badgeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(4), 0));
            badgeRow.setOpaque(false);
            badgeRow.setAlignmentX(LEFT_ALIGNMENT);
            for (Badge b : badges) {
                badgeRow.add(badge(b.text(), b.accented() ? accent : mutedColor()));
            }
            body.add(badgeRow);
        }

        if (stockText != null) {
            body.add(Box.createVerticalGlue());
            JPanel foot = new JPanel(new BorderLayout());
            foot.setOpaque(false);
            foot.setAlignmentX(LEFT_ALIGNMENT);
            JLabel stockLabel = new JLabel(stockText);
            if (stockWarn) {
                stockLabel.setForeground(new Color(0xC0, 0x70, 0x1F));
            }
            foot.add(stockLabel, BorderLayout.WEST);
            if (priceText != null) {
                JLabel price = new JLabel(priceText);
                price.setFont(price.getFont().deriveFont(Font.BOLD));
                foot.add(price, BorderLayout.EAST);
            }
            body.add(foot);
        }

        add(body, BorderLayout.CENTER);

        refreshSelected();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onClick.run();
            }
        });
    }

    /** Re-reads the selection state and repaints the highlight; call after the selection changes. */
    void refreshSelected() {
        if (selected.getAsBoolean()) {
            Color tint = blend(accent, baseBackground, 0.72f);
            setBackground(tint);
            body.setOpaque(true);
            body.setBackground(tint);
            band.setBackground(accent);
            nameLabel.setForeground(accent);
            setBorder(new RoundedLineBorder(accent, scaleForGUI(3), scaleForGUI(16)));
        } else {
            setBackground(baseBackground);
            body.setOpaque(false);
            band.setBackground(special ? mutedColor() : accent);
            nameLabel.setForeground(defaultForeground);
            setBorder(RoundedLineBorder.createSubtleRoundedLineBorder());
        }
        repaint();
    }

    /**
     * Lays a set of cards out as a top-anchored grid that wraps to as many columns as the width allows. Returned ready
     * to drop into a vertically scrolling pane.
     */
    static JComponent grid(List<KitCard> cards) {
        JPanel grid = new JPanel(new megamek.client.ui.WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(10)));
        for (KitCard card : cards) {
            grid.add(card);
        }
        // NORTH pins the wrap grid to the top at full width, so cards fill from the top-left instead of centering.
        JPanel top = new JPanel(new BorderLayout());
        top.add(grid, BorderLayout.NORTH);
        return top;
    }

    private static JLabel badge(String text, Color color) {
        JLabel badge = new JLabel(text);
        badge.setFont(badge.getFont().deriveFont(badge.getFont().getSize2D() - 2f));
        badge.setForeground(color);
        badge.setBorder(BorderFactory.createCompoundBorder(new RoundedLineBorder(color, 1, scaleForGUI(8)),
              BorderFactory.createEmptyBorder(scaleForGUI(1), scaleForGUI(5), scaleForGUI(1), scaleForGUI(5))));
        return badge;
    }

    /** A foreground/background blend used for the muted detail text and the selected tint. */
    static Color mutedColor() {
        return blend(new JLabel().getForeground(), new JPanel().getBackground(), 0.45f);
    }

    static Color blend(Color a, Color b, float t) {
        return new Color(
              Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
              Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
              Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    /** Renders a damage divisor without a trailing ".0" for whole numbers. */
    static String formatDivisor(double divisor) {
        return (divisor == Math.rint(divisor)) ? String.valueOf((int) divisor) : String.valueOf(divisor);
    }
}
