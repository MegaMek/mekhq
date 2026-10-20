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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.DIVIDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_HIGHLIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.translucent;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.text.DefaultCaret;

import jakarta.annotation.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudStyle;

/**
 * Layout pieces shared by the contract market's board, dossier and comparison, built on the HUD kit in the style of the
 * Oracle Console: label-and-value rows, compact readouts, outlined badges, the difficulty pip meter and small painted
 * icons. Colours and fonts come from {@link HudStyle}.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class MarketHud {
    private static final int PIP_COUNT = 10;

    private MarketHud() {
    }

    // region Containers

    /** @return a transparent panel that stacks its children vertically */
    static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /**
     * Wraps content in a HUD scroll pane that only scrolls vertically: the content is pinned to the top and always
     * fits the pane's width, so wrapped text reflows instead of scrolling sideways.
     *
     * @param content    the content
     * @param background the colour behind the content
     */
    static JScrollPane scroll(JComponent content, Color background) {
        JPanel holder = new WidthTrackingPanel();
        holder.setOpaque(true);
        holder.setBackground(background);
        holder.add(content, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        Hud.styleScroll(scroll, background, false);
        return scroll;
    }

    /**
     * A label-and-value section: a section heading over rows added with {@link #addRow}.
     *
     * @param heading the section's heading
     */
    static JPanel section(String heading) {
        JPanel section = Hud.transparentPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, scaleForGUI(4), 0);
        section.add(Hud.sectionHeading(heading), constraints);
        return section;
    }

    /**
     * Adds one label-and-value row to a {@link #section}, with a hairline divider beneath it.
     *
     * @param section    the section
     * @param label      the faint label on the left
     * @param value      the value on the right
     * @param valueColor the value's colour, or {@code null} for the standard text colour
     * @param toolTip    a tooltip for the whole row, or {@code null}
     * @param emphasize  {@code true} to draw the row in bold, for totals
     */
    static void addRow(JPanel section, String label, String value, @Nullable Color valueColor,
          @Nullable String toolTip, boolean emphasize) {
        int row = section.getComponentCount();
        Color divider = DIVIDER;

        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(emphasize ? TEXT : TEXT_FAINT);
        labelComponent.setFont(hudFont(emphasize ? Font.BOLD : Font.PLAIN, 0.9f, 0.0f));
        labelComponent.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, divider),
              BorderFactory.createEmptyBorder(scaleForGUI(3), 0, scaleForGUI(3), scaleForGUI(10))));

        JLabel valueComponent = new JLabel(value, SwingConstants.RIGHT);
        valueComponent.setForeground(valueColor == null ? TEXT : valueColor);
        valueComponent.setFont(hudFont(emphasize ? Font.BOLD : Font.PLAIN, emphasize ? 0.96f : 0.9f, 0.0f));
        valueComponent.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, divider),
              BorderFactory.createEmptyBorder(scaleForGUI(3), 0, scaleForGUI(3), 0)));
        // Long values (an opposition's full name, say) are cut short in a narrow column rather than pushing it wider;
        // a label's minimum width is otherwise its whole text. The tooltip keeps them readable.
        valueComponent.setMinimumSize(new Dimension(scaleForGUI(24), valueComponent.getPreferredSize().height));
        valueComponent.setToolTipText(toolTip == null ? value : toolTip);
        if (toolTip != null) {
            labelComponent.setToolTipText(toolTip);
        }

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelConstraints.anchor = GridBagConstraints.WEST;
        section.add(labelComponent, labelConstraints);

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1.0;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.anchor = GridBagConstraints.EAST;
        section.add(valueComponent, valueConstraints);
    }

    /**
     * Adds a full-width component, such as a cost bar or a wrapped caveat, beneath a {@link #section}'s rows.
     *
     * @param section the section
     * @param content the component
     * @param top     the gap above it, before scaling
     */
    static void addSpanning(JPanel section, JComponent content, int top) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = section.getComponentCount();
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(scaleForGUI(top), 0, 0, 0);
        section.add(content, constraints);
    }

    /**
     * Pins a section to the top of a grid cell, so a short column does not stretch to match a tall neighbour.
     */
    static JPanel topAligned(JComponent content) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(content, BorderLayout.NORTH);
        return holder;
    }

    // endregion Containers

    // region Pieces

    /**
     * A compact readout: a small tracked key over a bold value, on the deep surface. The command bar and the dossier's
     * money column use it where a full {@code HudStatTile} would be too tall.
     *
     * @param key        the key
     * @param value      the value
     * @param valueColor the value's colour
     * @param background the readout's surface, set apart from what is behind it
     */
    static JPanel readout(String key, String value, Color valueColor, Color background) {
        JPanel readout = new JPanel();
        readout.setLayout(new BoxLayout(readout, BoxLayout.Y_AXIS));
        readout.setOpaque(true);
        readout.setBackground(background);
        readout.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(5), scaleForGUI(10), scaleForGUI(5),
              scaleForGUI(10)));
        JLabel keyLabel = HudStyle.keyLabel(key.toUpperCase(Locale.ROOT));
        keyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(valueColor);
        valueLabel.setFont(hudFont(Font.BOLD, 1.08f, 0.0f));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        readout.add(keyLabel);
        readout.add(valueLabel);
        return readout;
    }

    /**
     * An outlined, tracked badge such as "Batchall" or "Proving Ground", explained on hover.
     *
     * @param text    the badge's text
     * @param color   the text and outline colour
     * @param toolTip what the badge means, or {@code null}
     */
    static JLabel badge(String text, Color color, @Nullable String toolTip) {
        JLabel badge = new JLabel(text.toUpperCase(Locale.ROOT));
        badge.setForeground(color);
        badge.setFont(hudFont(Font.BOLD, 0.68f, 0.12f));
        badge.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(color, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(2), scaleForGUI(7), scaleForGUI(2), scaleForGUI(7))));
        if (toolTip != null) {
            badge.setToolTipText(wrapToolTip(toolTip));
        }
        return badge;
    }

    /**
     * Read-only text that wraps to whatever width its container gives it.
     *
     * @param text  the text
     * @param color the text colour
     * @param style a {@link Font} style constant
     * @param size  a multiplier on the base label font size
     */
    static JTextArea wrappedText(String text, Color color, int style, float size) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setFocusable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setForeground(color);
        area.setFont(hudFont(style, size, 0.0f));
        // Read-only text never needs its caret on screen, and a caret that follows the text would scroll the page.
        ((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        return area;
    }

    /**
     * A square portrait of a person, or a blank square of the same size when there is no one to show.
     *
     * @param person the person, or {@code null}
     * @param size   the scaled edge length
     */
    static JLabel portrait(@Nullable Person person, int size) {
        JLabel portrait = new JLabel();
        portrait.setPreferredSize(new Dimension(size, size));
        portrait.setMinimumSize(new Dimension(size, size));
        portrait.setOpaque(true);
        portrait.setBackground(SURFACE_HIGHLIGHT);
        portrait.setBorder(BorderFactory.createLineBorder(HudStyle.BORDER_CYAN, scaleForGUI(1)));
        if (person != null) {
            ImageIcon icon = person.getPortraitImageIconWithFallback(true);
            if (icon != null) {
                portrait.setIcon(new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH)));
            }
        }
        return portrait;
    }

    /** @return the tooltip wrapped to a comfortable reading width */
    static String wrapToolTip(String toolTip) {
        return "<html><div style='width:" + scaleForGUI(280) + "px'>" + escape(toolTip) + "</div></html>";
    }

    static String escape(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // endregion Pieces

    // region Painted pieces

    /** A filled square in a faction's colour, for marking an employer beside its name. */
    record SwatchIcon(Color color, int size) implements Icon {
        SwatchIcon(Color color) {
            this(color, scaleForGUI(8));
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            graphics.setColor(color);
            graphics.fillRect(x, y, size, size);
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /** The board's status ring, as a small icon for headers: an outlined circle tinted inside with its colour. */
    record RingIcon(Color color, int size) implements Icon {
        RingIcon(Color color) {
            this(color, scaleForGUI(12));
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int stroke = scaleForGUI(2);
                canvas.setColor(translucent(color, 90));
                canvas.fillOval(x + stroke, y + stroke, size - 2 * stroke, size - 2 * stroke);
                canvas.setColor(color);
                canvas.setStroke(new BasicStroke(stroke));
                canvas.drawOval(x + stroke / 2, y + stroke / 2, size - stroke, size - stroke);
            } finally {
                canvas.dispose();
            }
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }

    /**
     * The 1-10 difficulty as a row of ten short pips, the first {@code difficulty} lit in the severity colour.
     */
    static final class PipMeter extends JComponent {
        private int filled;
        private Color color;

        PipMeter(int filled, Color color) {
            set(filled, color);
            int width = PIP_COUNT * scaleForGUI(11) + (PIP_COUNT - 1) * scaleForGUI(2);
            setPreferredSize(new Dimension(width, scaleForGUI(5)));
            setMinimumSize(getPreferredSize());
        }

        void set(int filled, Color color) {
            this.filled = filled;
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int pipWidth = scaleForGUI(11);
            int gap = scaleForGUI(2);
            int height = scaleForGUI(3);
            int top = (getHeight() - height) / 2;
            for (int index = 0; index < PIP_COUNT; index++) {
                graphics.setColor(index < filled ? color : SURFACE_HIGHLIGHT);
                graphics.fillRect(index * (pipWidth + gap), top, pipWidth, height);
            }
        }
    }

    /**
     * A thin bar split into coloured segments in proportion to their sizes, such as the parts of a contract's
     * operating costs.
     */
    static final class SegmentBar extends JComponent {
        private final List<Double> sizes;
        private final List<Color> colors;

        SegmentBar(List<Double> sizes, List<Color> colors) {
            this.sizes = sizes;
            this.colors = colors;
            setPreferredSize(new Dimension(scaleForGUI(100), scaleForGUI(4)));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(SURFACE_HIGHLIGHT);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            double total = sizes.stream().mapToDouble(size -> Math.max(0, size)).sum();
            if (total <= 0) {
                return;
            }
            int x = 0;
            for (int index = 0; index < sizes.size(); index++) {
                int width = (index == sizes.size() - 1) ? getWidth() - x
                                  : (int) Math.round(getWidth() * Math.max(0, sizes.get(index)) / total);
                graphics.setColor(colors.get(index));
                graphics.fillRect(x, 0, width, getHeight());
                x += width;
            }
        }
    }

    // endregion Painted pieces

    /** A scroll pane's content that always fits the pane's width, so only vertical scrolling is ever needed. */
    static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return scaleForGUI(16);
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /** @return a hairline-bordered surface panel, for setting off one block such as an empty-state notice */
    static JPanel framedSurface(int padding) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(true);
        panel.setBackground(SURFACE);
        int inset = scaleForGUI(padding);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(inset, inset, inset, inset)));
        return panel;
    }
}
