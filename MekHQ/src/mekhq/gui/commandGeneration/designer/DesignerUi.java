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
package mekhq.gui.commandGeneration.designer;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_HIGHLIGHT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.keyLabel;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;
import static mekhq.gui.baseComponents.hud.HudStyle.translucent;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.commandGeneration.components.CommandGenerationUtilities;

/**
 * Small building blocks shared by the Command Designer's pages: localized text, stacked columns, section headings,
 * captions, notes and scrolling, all in the heads-up style of the Oracle Console.
 */
public final class DesignerUi {
    static final String BUNDLE = CommandGenerationUtilities.getCommandGenerationResourceBundle();

    /** The gap between sections in a column. */
    static final int SECTION_GAP = 16;
    /** The gap between rows inside a section. */
    static final int ROW_GAP = 6;
    /** How far a sub-option is indented under the option it belongs to. */
    static final int INDENT = 20;

    private DesignerUi() {
    }

    /** @return the text for a key in the Command Generation bundle */
    static String text(String key) {
        return getTextAt(BUNDLE, key);
    }

    /** @return the text for a key in the Command Generation bundle, with its placeholders filled */
    static String formatted(String key, Object... arguments) {
        return getFormattedTextAt(BUNDLE, key, arguments);
    }

    static String upper(String text) {
        return text.toUpperCase(Locale.ROOT);
    }

    /** @return a transparent panel that stacks its children top to bottom */
    static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /** @return the panel, with padding around it */
    static JComponent padded(JComponent component, int top, int left, int bottom, int right) {
        component.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(top), scaleForGUI(left), scaleForGUI(bottom),
              scaleForGUI(right)));
        return component;
    }

    /**
     * Adds components to a column, left-aligned, with a small gap between them.
     *
     * @param column     a panel from {@link #column()}
     * @param components the components, top to bottom; {@code null} entries are skipped
     */
    static void addRows(JPanel column, JComponent... components) {
        for (JComponent component : components) {
            if (component == null) {
                continue;
            }
            if (column.getComponentCount() > 0) {
                column.add(Box.createVerticalStrut(scaleForGUI(ROW_GAP)));
            }
            column.add(leftAligned(component));
        }
    }

    /**
     * A section: a heading with an optional faint note on its right, then the given rows.
     *
     * @param title the heading
     * @param aside a faint note beside the heading, or {@code null}
     * @param rows  the section's rows, top to bottom; {@code null} entries are skipped
     *
     * @return the section
     */
    static JPanel section(String title, @Nullable String aside, JComponent... rows) {
        JPanel section = column();
        JPanel heading = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        heading.add(Hud.sectionHeading(title), BorderLayout.CENTER);
        if (aside != null) {
            JLabel note = new JLabel(aside);
            note.setForeground(TEXT_FAINT);
            note.setFont(hudFont(Font.PLAIN, 0.8f, 0.0f));
            heading.add(note, BorderLayout.EAST);
        }
        heading.setMaximumSize(new Dimension(Short.MAX_VALUE, heading.getPreferredSize().height));
        section.add(leftAligned(heading));
        section.add(Box.createVerticalStrut(scaleForGUI(8)));
        JPanel body = column();
        addRows(body, rows);
        section.add(leftAligned(body));
        return section;
    }

    /**
     * Stacks sections with the usual gap between them.
     *
     * @param sections the sections, top to bottom; {@code null} entries are skipped
     *
     * @return the stacked sections
     */
    static JPanel stack(JComponent... sections) {
        JPanel stack = column();
        for (JComponent section : sections) {
            if (section == null) {
                continue;
            }
            if (stack.getComponentCount() > 0) {
                stack.add(Box.createVerticalStrut(scaleForGUI(SECTION_GAP)));
            }
            stack.add(leftAligned(section));
        }
        return stack;
    }

    /** @return a small, tracked, upper-case caption for a field */
    static JLabel caption(String text) {
        return keyLabel(upper(text));
    }

    /** @return a field: a caption above its control */
    static JPanel field(String caption, JComponent control) {
        JPanel field = column();
        field.add(leftAligned(caption(caption)));
        field.add(Box.createVerticalStrut(scaleForGUI(4)));
        field.add(leftAligned(control));
        return field;
    }

    /** @return a muted line of text */
    static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_MUTED);
        label.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
        return label;
    }

    /** @return a faint line of text */
    static JLabel faint(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_FAINT);
        label.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        return label;
    }

    /**
     * @param html  the text, which may carry simple HTML such as bold
     * @param width the width to wrap at, before GUI scaling
     *
     * @return a muted label that wraps at the given width
     */
    static JLabel wrapped(String html, int width) {
        JLabel label = new JLabel(wrapHtml(html, width));
        label.setForeground(TEXT_MUTED);
        label.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        return label;
    }

    /** @return the text wrapped for a {@link JLabel} at the given width, before GUI scaling */
    static String wrapHtml(String html, int width) {
        return "<html><div style='width:" + scaleForGUI(width) + "px'>" + html + "</div></html>";
    }

    /**
     * A tinted note with a coloured stripe on its left, for something worth a second look.
     *
     * @param html   the note's text, which may carry simple HTML
     * @param accent the stripe and tint colour, such as amber for a caution
     * @param width  the width to wrap at, before GUI scaling
     *
     * @return the note
     */
    static JComponent note(String html, Color accent, int width) {
        JLabel label = wrapped(html, width);
        JPanel note = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(translucent(accent, 18));
                graphics.fillRect(0, 0, getWidth(), getHeight());
                graphics.setColor(translucent(accent, 110));
                graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                graphics.setColor(accent);
                graphics.fillRect(0, 0, scaleForGUI(3), getHeight());
            }
        };
        note.setOpaque(false);
        note.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(7), scaleForGUI(11), scaleForGUI(7),
              scaleForGUI(9)));
        note.add(label, BorderLayout.CENTER);
        return note;
    }

    /**
     * @param text    the checkbox's label
     * @param tooltip its tooltip, or {@code null}
     *
     * @return a HUD checkbox
     */
    static HudCheckBox check(String text, @Nullable String tooltip) {
        HudCheckBox box = new HudCheckBox(text);
        if (tooltip != null) {
            box.setToolTipText(CommandGenerationUtilities.wrapTooltip(tooltip, null));
        }
        return box;
    }

    /**
     * A checkbox labelled from an existing {@code lbl<name>} pair of the bundle, so the option reads and explains
     * itself as it always has.
     *
     * @param name the option's name, such as {@code AssignFounderFlag}
     *
     * @return the checkbox
     */
    static HudCheckBox optionCheck(String name) {
        return check(text("lbl" + name + ".text"), text("lbl" + name + ".tooltip"));
    }

    /** @return the component, indented as a sub-option of the one above it */
    static JComponent indented(JComponent component) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(INDENT), 0, 0));
        holder.add(component, BorderLayout.CENTER);
        return holder;
    }

    /**
     * Wraps content in a vertical scroll pane whose content always fits the pane's width.
     *
     * @param content    the content
     * @param background the colour behind it
     *
     * @return the scroll pane
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

    /** A scroll pane's content that always fits the pane's width, so only vertical scrolling is ever needed. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
        private WidthTrackingPanel() {
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

    /**
     * A thin fill bar, as the stat tiles draw their meters.
     */
    static final class Meter extends JComponent {
        private double fill;
        private Color color = ACCENT;

        Meter() {
            setPreferredSize(new Dimension(scaleForGUI(60), scaleForGUI(6)));
            setMinimumSize(new Dimension(scaleForGUI(20), scaleForGUI(6)));
            setMaximumSize(new Dimension(Short.MAX_VALUE, scaleForGUI(6)));
        }

        /**
         * @param fill  how full, from 0 to 1
         * @param color the fill colour
         */
        void setFill(double fill, Color color) {
            this.fill = Math.clamp(fill, 0.0, 1.0);
            this.color = color;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            int barHeight = scaleForGUI(4);
            int top = (getHeight() - barHeight) / 2;
            graphics.setColor(SURFACE_HIGHLIGHT);
            graphics.fillRect(0, top, getWidth(), barHeight);
            graphics.setColor(color);
            graphics.fillRect(0, top, (int) Math.round(getWidth() * fill), barHeight);
        }
    }
}
