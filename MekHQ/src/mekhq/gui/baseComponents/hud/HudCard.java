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
import static mekhq.gui.baseComponents.hud.HudStyle.DIVIDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_MUTED;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;
import static mekhq.gui.baseComponents.hud.HudStyle.leftAligned;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import megamek.common.annotations.Nullable;

/**
 * A board card in the style of the StratCon deployment wizard: a leading status ring, a bold name followed by a muted
 * role and small coloured tags, a muted sub-line, and an optional figure on the right. A selected card is raised to
 * the surface colour with an accent bar on its left.
 *
 * <p>A card is meant to be reused as a list cell renderer: configure it with {@link #show} for each row. A
 * {@link #sectionHeader(String) section header} renders the divider rows that caption groups of cards.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudCard extends JPanel {
    private static final int MAXIMUM_TAGS = 4;

    private final HudRing ring = new HudRing();
    private final JLabel nameLabel = new JLabel();
    private final JLabel roleLabel = new JLabel();
    private final List<JLabel> tagLabels = new ArrayList<>();
    private final JLabel subLabel = new JLabel();
    private final JLabel figureLabel = new JLabel();
    private final JLabel figureUnitLabel = new JLabel();

    private final JPanel header = new JPanel(new BorderLayout());
    private final JLabel headerLabel = new JLabel();

    /**
     * A small coloured tag shown after a card's name.
     *
     * @param text  the tag text; shown in upper case
     * @param color the tag's colour
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record Tag(String text, Color color) {}

    /** Builds an empty card; {@link #show} fills it in for each row it renders. */
    public HudCard() {
        super(new BorderLayout(scaleForGUI(10), 0));

        nameLabel.setFont(hudFont(Font.BOLD, 1.0f, 0.02f));
        nameLabel.setForeground(TEXT);
        roleLabel.setFont(hudFont(Font.PLAIN, 0.82f, 0.06f));
        roleLabel.setForeground(TEXT_MUTED);
        subLabel.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        subLabel.setForeground(TEXT_MUTED);

        JPanel nameRow = new JPanel();
        nameRow.setOpaque(false);
        nameRow.setLayout(new BoxLayout(nameRow, BoxLayout.X_AXIS));
        nameRow.add(nameLabel);
        nameRow.add(Box.createHorizontalStrut(scaleForGUI(8)));
        nameRow.add(roleLabel);
        for (int index = 0; index < MAXIMUM_TAGS; index++) {
            JLabel tagLabel = new JLabel();
            tagLabel.setFont(hudFont(Font.BOLD, 0.72f, 0.12f));
            nameRow.add(Box.createHorizontalStrut(scaleForGUI(8)));
            nameRow.add(tagLabel);
            tagLabels.add(tagLabel);
        }

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(leftAligned(nameRow));
        text.add(leftAligned(subLabel));

        figureLabel.setFont(hudFont(Font.BOLD, 1.0f, 0.0f));
        figureLabel.setForeground(TEXT);
        figureLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        figureUnitLabel.setFont(hudFont(Font.PLAIN, 0.74f, 0.0f));
        figureUnitLabel.setForeground(TEXT_FAINT);
        figureUnitLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        JPanel figure = new JPanel();
        figure.setOpaque(false);
        figure.setLayout(new BoxLayout(figure, BoxLayout.Y_AXIS));
        figureLabel.setAlignmentX(RIGHT_ALIGNMENT);
        figureUnitLabel.setAlignmentX(RIGHT_ALIGNMENT);
        figure.add(figureLabel);
        figure.add(figureUnitLabel);

        add(ring, BorderLayout.WEST);
        add(text, BorderLayout.CENTER);
        add(figure, BorderLayout.EAST);

        headerLabel.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        headerLabel.setForeground(TEXT_FAINT);
        header.setOpaque(true);
        header.setBackground(SURFACE_DEEP);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(10), scaleForGUI(4), scaleForGUI(10))));
        header.add(headerLabel, BorderLayout.CENTER);
    }

    /**
     * Configures the card for one row.
     *
     * @param ringColor   the status ring's colour
     * @param isRingFilled {@code true} to fill the ring with a tint of its colour
     * @param name        the row's name
     * @param role        a muted word or two after the name, or {@code null} for none
     * @param tags        the tags to show after the name; any beyond four are dropped
     * @param sub         the muted sub-line
     * @param figure      the figure on the right, or {@code null} for none
     * @param figureUnit  the unit shown under the figure, or {@code null} for none
     * @param isSelected  {@code true} if the row is selected
     *
     * @return this card, ready to be returned by a list cell renderer
     *
     * @author Illiani
     * @since 0.51.01
     */
    public HudCard show(Color ringColor, boolean isRingFilled, String name, @Nullable String role, List<Tag> tags,
          String sub, @Nullable String figure, @Nullable String figureUnit, boolean isSelected) {
        ring.setColor(ringColor, isRingFilled);
        nameLabel.setText(name);
        roleLabel.setText((role == null) ? "" : role);
        for (int index = 0; index < tagLabels.size(); index++) {
            JLabel tagLabel = tagLabels.get(index);
            if (index < tags.size()) {
                tagLabel.setText(tags.get(index).text().toUpperCase(Locale.ROOT));
                tagLabel.setForeground(tags.get(index).color());
                tagLabel.setVisible(true);
            } else {
                tagLabel.setVisible(false);
            }
        }
        subLabel.setText(sub);
        figureLabel.setText((figure == null) ? "" : figure);
        figureUnitLabel.setText((figureUnit == null) ? "" : figureUnit);

        setOpaque(true);
        setBackground(isSelected ? SURFACE : SURFACE_DEEP);
        setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, scaleForGUI(isSelected ? 3 : 0), 0, 0, ACCENT),
                    BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(isSelected ? 7 : 10), scaleForGUI(8),
                          scaleForGUI(10)))));
        return this;
    }

    /**
     * Configures and returns the divider row that captions a group of cards.
     *
     * @param label the group's caption; shown in upper case
     *
     * @return the header row, ready to be returned by a list cell renderer
     *
     * @author Illiani
     * @since 0.51.01
     */
    public JPanel sectionHeader(String label) {
        headerLabel.setText(label.toUpperCase(Locale.ROOT));
        return header;
    }
}
