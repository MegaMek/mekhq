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
package mekhq.gui.dialog.markets.personnelMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.Locale;
import javax.swing.*;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.RATGenerator;
import megamek.common.icons.Portrait;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.hud.Hud;

/**
 * Layout pieces shared by the personnel market's applicant board and dossier, built on the HUD kit in the style of the
 * Oracle Console: stacked columns, vertical-only scroll panes, label-and-value sections, outlined badges and portraits.
 * Colours and fonts come from {@link mekhq.gui.baseComponents.hud.HudStyle}.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PersonnelMarketHud {
    /** The margin around a faction logo standing in for a portrait, as a share of the portrait's edge. */
    private static final float LOGO_MARGIN = 0.12f;

    private PersonnelMarketHud() {
    }

    /** @return a transparent panel that stacks its children vertically */
    static JPanel column() {
        JPanel column = Hud.transparentPanel(null);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        return column;
    }

    /**
     * Wraps content in a HUD scroll pane that only scrolls vertically: the content is pinned to the top and always fits
     * the pane's width, so wrapped text reflows instead of scrolling sideways.
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
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
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
     * @param value      the value on the right; may be HTML
     * @param valueColor the value's colour, or {@code null} for the standard text colour
     * @param toolTip    a tooltip for the whole row, or {@code null}
     * @param emphasize  {@code true} to draw the row in bold, for totals
     */
    static void addRow(JPanel section, String label, String value, @Nullable Color valueColor,
          @Nullable String toolTip, boolean emphasize) {
        int row = section.getComponentCount();

        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(emphasize ? TEXT : TEXT_FAINT);
        labelComponent.setFont(hudFont(emphasize ? Font.BOLD : Font.PLAIN, 0.9f, 0.0f));
        labelComponent.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(3), 0, scaleForGUI(3), scaleForGUI(10))));

        JLabel valueComponent = new JLabel(value, SwingConstants.RIGHT);
        valueComponent.setForeground(valueColor == null ? TEXT : valueColor);
        valueComponent.setFont(hudFont(emphasize ? Font.BOLD : Font.PLAIN, emphasize ? 0.96f : 0.9f, 0.0f));
        valueComponent.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(3), 0, scaleForGUI(3), 0)));
        // A long value is cut short in a narrow column rather than pushing it wider; the tooltip keeps it readable.
        valueComponent.setMinimumSize(new Dimension(scaleForGUI(24), valueComponent.getPreferredSize().height));
        if (toolTip != null) {
            labelComponent.setToolTipText(toolTip);
            valueComponent.setToolTipText(toolTip);
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
     * Adds a full-width component, such as a row of badges or a wrapped note, beneath a {@link #section}'s rows.
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

    /** Pins a component to the top of its cell, so it does not stretch to match a taller neighbour. */
    static JPanel topAligned(JComponent content) {
        JPanel holder = Hud.transparentPanel(new BorderLayout());
        holder.add(content, BorderLayout.NORTH);
        return holder;
    }

    /**
     * An outlined, tracked badge such as "Rare" or a special ability's name, explained on hover.
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
        if (toolTip != null && !toolTip.isBlank()) {
            badge.setToolTipText(wrapToolTip(toolTip));
        }
        return badge;
    }

    /**
     * A boxed fact on the deep surface, such as an applicant's age, for the row of facts beneath a name.
     *
     * @param text  the fact
     * @param color the text colour
     */
    static JLabel fact(String text, Color color) {
        JLabel fact = new JLabel(text);
        fact.setOpaque(true);
        fact.setBackground(SURFACE_DEEP);
        fact.setForeground(color);
        fact.setFont(hudFont(Font.PLAIN, 0.85f, 0.0f));
        fact.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(8), scaleForGUI(3), scaleForGUI(8))));
        return fact;
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
        portrait.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)));
        portrait.setIcon(portraitIcon(person, size));
        return portrait;
    }

    /**
     * @param person the person, or {@code null}
     * @param size   the scaled edge length
     *
     * @return the person's portrait scaled to a square; their origin faction's logo in the accent colour if they have
     *       no portrait of their own; or {@code null} if there is neither
     */
    static @Nullable Icon portraitIcon(@Nullable Person person, int size) {
        if (person == null) {
            return null;
        }
        if (hasDefaultPortrait(person)) {
            Icon logo = factionLogoIcon(person, size);
            if (logo != null) {
                return logo;
            }
        }
        ImageIcon icon = person.getPortraitImageIconWithFallback(true);
        if (icon == null || icon.getIconWidth() <= 0) {
            return null;
        }
        return new ImageIcon(icon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH));
    }

    /**
     * @return {@code true} if the person has no portrait of their own, so the faction logo stands in for one; matches
     *       the test {@link Person#getPortraitImageIconWithFallback(boolean)} uses
     */
    private static boolean hasDefaultPortrait(Person person) {
        Portrait portrait = person.getPortrait();
        if (portrait == null) {
            return true;
        }
        String fileName = portrait.getFilename();
        return fileName == null || fileName.equalsIgnoreCase(Portrait.DEFAULT_PORTRAIT_FILENAME)
                     || fileName.equalsIgnoreCase(Portrait.NO_PORTRAIT_NAME);
    }

    /**
     * The person's origin faction logo, drawn in the accent colour and centred in a square with a margin, so a person
     * without a portrait reads as part of the HUD instead of as a dark smudge. The logos are grey shapes on a
     * transparent ground, so filling only their opaque pixels keeps every detail of the shape.
     *
     * @param person the person
     * @param size   the scaled edge length of the square
     *
     * @return the tinted logo, or {@code null} if the faction has no logo that can be read
     */
    private static @Nullable Icon factionLogoIcon(Person person, int size) {
        Faction origin = person.getOriginFaction();
        LocalDate birth = person.getDateOfBirth();
        if (origin == null || birth == null || size <= 0) {
            return null;
        }
        // The logo is chosen by birth year, as the personnel view's fallback portrait chooses it.
        File file = RATGenerator.getFactionLogoFile(birth.getYear(), origin.getShortName());
        if (!file.isFile()) {
            return null;
        }
        Image logo = new ImageIcon(file.getPath()).getImage();
        int logoWidth = logo.getWidth(null);
        int logoHeight = logo.getHeight(null);
        if (logoWidth <= 0 || logoHeight <= 0) {
            return null;
        }

        BufferedImage tinted = new BufferedImage(logoWidth, logoHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tint = tinted.createGraphics();
        try {
            tint.drawImage(logo, 0, 0, null);
            tint.setComposite(AlphaComposite.SrcAtop);
            tint.setColor(ACCENT);
            tint.fillRect(0, 0, logoWidth, logoHeight);
        } finally {
            tint.dispose();
        }

        // Fit the logo inside the square with a margin, keeping its proportions.
        int margin = Math.max(1, Math.round(size * LOGO_MARGIN));
        int room = Math.max(1, size - 2 * margin);
        double scale = Math.min(room / (double) logoWidth, room / (double) logoHeight);
        int width = Math.max(1, (int) Math.round(logoWidth * scale));
        int height = Math.max(1, (int) Math.round(logoHeight * scale));
        Image scaled = tinted.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        BufferedImage square = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D canvas = square.createGraphics();
        try {
            canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            canvas.drawImage(new ImageIcon(scaled).getImage(), (size - width) / 2, (size - height) / 2, null);
        } finally {
            canvas.dispose();
        }
        return new ImageIcon(square);
    }

    /**
     * Sizes an HTML label to wrap at the given width and be tall enough for every line. Swing's HTML view reports how
     * tall the text is when laid out that wide; a CSS width would not do, as Swing scales its {@code px} unit and the
     * text would overrun whatever sits beside it.
     *
     * @param label the label, holding HTML text
     * @param width the width to wrap at
     */
    static void fitHtmlToWidth(JLabel label, int width) {
        View view = (View) label.getClientProperty(BasicHTML.propertyKey);
        if (view == null) {
            label.setPreferredSize(null);
            return;
        }
        view.setSize(width, 0);
        int height = (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS));
        label.setPreferredSize(new Dimension(width, height));
        label.setMinimumSize(new Dimension(0, height));
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

    /** @return the tooltip wrapped to a comfortable reading width */
    static String wrapToolTip(String toolTip) {
        return "<html><div style='width:" + scaleForGUI(280) + "px'>" + escape(toolTip) + "</div></html>";
    }

    /** @return the text with the characters HTML treats specially escaped, so it reads as written */
    static String escape(@Nullable String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
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
}
