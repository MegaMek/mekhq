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
import static mekhq.gui.baseComponents.hud.HudStyle.ACCENT;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

import jakarta.annotation.Nullable;
import megamek.client.ratgenerator.RATGenerator;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.hud.Hud;

/**
 * Pieces of the personnel market's applicant board and dossier that the HUD kit does not have: boxed facts, portraits
 * that show a faction logo in the accent colour for applicants without one, and sizing for wrapped HTML labels. The
 * general pieces (columns, scroll panes, label-and-value sections and badges) are in {@link Hud}. Colours and fonts
 * come from {@link mekhq.gui.baseComponents.hud.HudStyle}.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PersonnelMarketHud {
    /** The margin around a faction logo standing in for a portrait, as a share of the portrait's edge. */
    private static final float LOGO_MARGIN = 0.12f;

    private PersonnelMarketHud() {
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
     * A square, framed portrait of a person, or a blank frame of the same size when there is no one to show. A person
     * without a portrait of their own shows their origin faction's logo, flat-filled in the accent colour and inset
     * with a margin; see {@link #portraitIcon}. The contract market's {@link Hud#portrait} recolours the logo by
     * brightness instead, and fills the frame with it.
     *
     * @param person the person, or {@code null}
     * @param size   the scaled edge length
     */
    static JLabel portrait(@Nullable Person person, int size) {
        return Hud.portraitFrame(portraitIcon(person, size), size);
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
        if (person.hasDefaultPortrait()) {
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
}
