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
package mekhq.gui.dialog.factionStanding;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;

import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.hud.HudButton;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudChip;

/**
 * The Oracle Console-styled picker for a Faction Standing ultimatum's decision.
 *
 * <p>Every side of the ultimatum, followed by going rogue as a Mercenary or a Pirate, is a card in a carousel. The
 * player browses with the arrow buttons, the arrow keys, the chip row, or by clicking a neighboring card, and then
 * commits. Browsing never commits anything. The player can instead ignore the ultimatum outright, with the
 * "Ignore Ultimatum (GM)" button, which is always offered.</p>
 *
 * <p>The dialog is modal: construct it, then read {@link #getSelectedChoice()} or {@link #wasIgnored()}. Closing it
 * from the window frame leaves both unset.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class UltimatumSidePickerDialog extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingUltimatumDialog";
    private static final String KEY_PREFIX = "FactionStandingUltimatumDialog.picker.";

    private static final int FOCUSED_CODE_BOX_SIZE = 72;
    private static final int NEIGHBOR_CODE_BOX_SIZE = 44;
    private static final int PITCH_WIDTH = 360;
    private static final Color ROGUE_BORDER = translucent(AMBER, 115);
    private static final Color ROGUE_CODE_BOX = translucent(AMBER, 28);
    private static final Color DISSENT_WASH = translucent(DANGER, 30);

    /** What a card in the carousel represents. */
    public enum ChoiceKind {
        SIDE,
        MERCENARY,
        PIRATE;

        /**
         * @return {@code true} if this choice abandons every side to go rogue
         *
         * @author Illiani
         * @since 0.51.01
         */
        public boolean isGoingRogue() {
            return this != SIDE;
        }
    }

    /**
     * One card in the carousel, with everything needed to draw it.
     *
     * @param kind           what the card represents
     * @param sideIndex      the index of the ultimatum side this card represents, or {@code -1} for a rogue card
     * @param code           the faction short name, shown in the card's code box and on its chip
     * @param title          the faction's name
     * @param subtitle       the person asking for support and their role, or a description of going rogue
     * @param pitch          the card's appeal to the player
     * @param tagRows        the consequence tags, worked out from the rules, grouped into one row per context
     * @param dissenterStays {@code true} if the dissenting officer stays with the unit on this choice
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record PickerChoice(ChoiceKind kind, int sideIndex, String code, String title, String subtitle,
          String pitch, List<List<Tag>> tagRows, boolean dissenterStays) {}

    /**
     * Everything the picker shows around the carousel.
     *
     * @param choices             the cards, in carousel order
     * @param dateLabel           the ultimatum's date, for the header
     * @param situationText       the situation briefing
     * @param dissenterLabel      the dissenting officer's name and position, or {@code null} if there is none
     * @param isViolentTransition {@code true} if the ultimatum is violent, which changes the dissenter's lines
     * @param gameInformation     the out-of-character rules summary
     *
     * @author Illiani
     * @since 0.51.01
     */
    public record PickerContent(List<PickerChoice> choices, String dateLabel, String situationText,
          @Nullable String dissenterLabel, boolean isViolentTransition, String gameInformation) {}

    private final PickerContent content;
    private int focusedIndex;
    private @Nullable PickerChoice selectedChoice;
    private boolean wasIgnored;

    private final JPanel stage = new JPanel(new GridBagLayout());
    private final JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.CENTER, scaleForGUI(6), 0));
    private final List<HudChip> chips = new ArrayList<>();
    private final JLabel counterLabel = new JLabel();
    private final JLabel dissentLineLabel = new JLabel();
    private final HudButton commitButton = new HudButton("", true);

    /**
     * Builds and shows the picker. Returns once the player commits, ignores the ultimatum, or closes the window.
     *
     * @param owner   the owning frame
     * @param content what the picker shows
     *
     * @author Illiani
     * @since 0.51.01
     */
    public UltimatumSidePickerDialog(@Nullable JFrame owner, PickerContent content) {
        super(owner, true);
        this.content = content;
        setTitle(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "title"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(GROUND);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        setContentPane(root);
        bindArrowKeys(root);

        refresh();
        pack();
        setLocationRelativeTo(owner);
        setVisible(true);
    }

    /**
     * @return the choice the player committed to, or {@code null} if they ignored the ultimatum or closed the window
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable PickerChoice getSelectedChoice() {
        return selectedChoice;
    }

    /**
     * @return {@code true} if the player chose to ignore the ultimatum
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean wasIgnored() {
        return wasIgnored;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        header.setBackground(SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(2), 0, ACCENT),
              BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(18), scaleForGUI(12), scaleForGUI(18))));

        JLabel title = new JLabel(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "title").toUpperCase(Locale.ROOT));
        title.setFont(hudFont(Font.BOLD, 1.15f, 0.12f));
        title.setForeground(ACCENT);
        JLabel signal = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "signal", content.dateLabel()));
        signal.setFont(hudFont(Font.BOLD, 0.78f, 0.12f));
        signal.setForeground(ACCENT);

        header.add(title, BorderLayout.WEST);
        header.add(signal, BorderLayout.EAST);
        return header;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(SURFACE_DEEP);
        body.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(18), scaleForGUI(18), scaleForGUI(18),
              scaleForGUI(18)));

        body.add(leftAligned(keyLabel(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "situation").toUpperCase(Locale.ROOT))));
        body.add(verticalGap(6));
        body.add(leftAligned(wrappedLabel(content.situationText(), TEXT, 1.0f, PITCH_WIDTH * 2)));
        body.add(verticalGap(18));
        body.add(leftAligned(buildCarousel()));

        body.add(verticalGap(14));
        body.add(leftAligned(buildDecisionRow()));
        body.add(verticalGap(14));
        body.add(leftAligned(buildGameInformation()));
        return body;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildCarousel() {
        JPanel carousel = new JPanel(new BorderLayout(0, scaleForGUI(10)));
        carousel.setOpaque(false);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        heading.add(keyLabel(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "choose").toUpperCase(Locale.ROOT)),
              BorderLayout.WEST);
        counterLabel.setFont(hudFont(Font.BOLD, 0.8f, 0.04f));
        counterLabel.setForeground(TEXT_MUTED);
        heading.add(counterLabel, BorderLayout.EAST);

        HudButton previous = new HudButton("‹", false);
        previous.addActionListener(event -> moveFocus(-1));
        HudButton next = new HudButton("›", false);
        next.addActionListener(event -> moveFocus(1));
        stage.setOpaque(false);

        JPanel track = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        track.setOpaque(false);
        track.add(previous, BorderLayout.WEST);
        track.add(stage, BorderLayout.CENTER);
        track.add(next, BorderLayout.EAST);

        chipRow.setOpaque(false);
        List<PickerChoice> choices = content.choices();
        for (int index = 0; index < choices.size(); index++) {
            int chipIndex = index;
            HudChip chip = new HudChip(choices.get(index).code(), () -> focus(chipIndex));
            chips.add(chip);
            chipRow.add(chip);
        }

        carousel.add(heading, BorderLayout.NORTH);
        carousel.add(track, BorderLayout.CENTER);
        carousel.add(chipRow, BorderLayout.SOUTH);
        return carousel;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildDissentStrip() {
        JPanel strip = new JPanel(new BorderLayout(scaleForGUI(14), 0));
        strip.setBackground(DISSENT_WASH);
        strip.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), scaleForGUI(3), scaleForGUI(1), scaleForGUI(1),
                    DANGER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JLabel key = keyLabel(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "dissent").toUpperCase(Locale.ROOT));
        key.setForeground(DANGER);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel who = new JLabel(content.dissenterLabel());
        who.setFont(hudFont(Font.BOLD, 0.95f, 0.0f));
        who.setForeground(TEXT);
        dissentLineLabel.setFont(hudFont(Font.PLAIN, 0.88f, 0.0f));
        dissentLineLabel.setForeground(TEXT_MUTED);
        text.add(leftAligned(who));
        text.add(leftAligned(dissentLineLabel));

        strip.add(key, BorderLayout.WEST);
        strip.add(text, BorderLayout.CENTER);
        return strip;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildDecisionRow() {
        // The dissent strip and the buttons share one row: the strip stretches, the buttons keep their size
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(14), 0));
        row.setOpaque(false);
        if (content.dissenterLabel() != null) {
            row.add(buildDissentStrip(), BorderLayout.CENTER);
        }

        // Centered vertically against the strip, which is usually taller than the buttons
        JPanel buttonHolder = new JPanel(new GridBagLayout());
        buttonHolder.setOpaque(false);
        buttonHolder.add(buildButtons());
        row.add(buttonHolder, BorderLayout.EAST);
        return row;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildButtons() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, scaleForGUI(10), 0));
        bar.setOpaque(false);

        HudButton ignoreButton = new HudButton(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "ignore")
                                                     .toUpperCase(Locale.ROOT), false);
        ignoreButton.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "ignore.tooltip")));
        ignoreButton.addActionListener(event -> {
            wasIgnored = true;
            dispose();
        });
        bar.add(ignoreButton);

        commitButton.addActionListener(event -> {
            selectedChoice = content.choices().get(focusedIndex);
            dispose();
        });
        bar.add(commitButton);
        return bar;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildGameInformation() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(translucent(AMBER, 18));
        panel.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(3), 0, 0, AMBER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(14), scaleForGUI(10), scaleForGUI(14))));

        JLabel key = keyLabel(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "gameInformation").toUpperCase(Locale.ROOT));
        key.setForeground(AMBER);
        panel.add(leftAligned(key));
        panel.add(verticalGap(6));
        panel.add(leftAligned(wrappedLabel(content.gameInformation(), TEXT_MUTED, 0.88f, PITCH_WIDTH * 2)));
        return panel;
    }

    /**
     * Binds the left and right arrow keys to move through the carousel.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void bindArrowKeys(JComponent root) {
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "ultimatumPrevious");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
              .put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "ultimatumNext");
        root.getActionMap().put("ultimatumPrevious", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                moveFocus(-1);
            }
        });
        root.getActionMap().put("ultimatumNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                moveFocus(1);
            }
        });
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void moveFocus(int step) {
        focus(wrapIndex(focusedIndex + step, content.choices().size()));
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private void focus(int index) {
        focusedIndex = index;
        refresh();
    }

    /**
     * Wraps an index around the carousel, so stepping past either end continues from the other.
     *
     * @param index the index, possibly out of range by one step
     * @param count the number of cards
     *
     * @return the wrapped index
     *
     * @author Illiani
     * @since 0.51.01
     */
    static int wrapIndex(int index, int count) {
        return ((index % count) + count) % count;
    }

    /**
     * Redraws the carousel, chips, counter, dissent line, and commit button for the focused card.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void refresh() {
        List<PickerChoice> choices = content.choices();
        int count = choices.size();
        PickerChoice focused = choices.get(focusedIndex);

        stage.removeAll();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weighty = 1;
        constraints.insets = new Insets(0, scaleForGUI(5), 0, scaleForGUI(5));

        constraints.gridx = 0;
        constraints.weightx = 1;
        stage.add(count > 1 ? buildNeighborCard(wrapIndex(focusedIndex - 1, count)) : emptyCard(), constraints);
        constraints.gridx = 1;
        constraints.weightx = 1.7;
        stage.add(buildFocusedCard(focused), constraints);
        constraints.gridx = 2;
        constraints.weightx = 1;
        stage.add(count > 2 ? buildNeighborCard(wrapIndex(focusedIndex + 1, count)) : emptyCard(), constraints);
        stage.revalidate();
        stage.repaint();

        for (int index = 0; index < chips.size(); index++) {
            chips.get(index).setActive(index == focusedIndex);
        }
        counterLabel.setText(String.format("%02d / %02d", focusedIndex + 1, count));

        String dissentKey = focused.dissenterStays() ? "dissent.stays"
                                  : (content.isViolentTransition() ? "dissent.fights" : "dissent.leaves");
        dissentLineLabel.setText(getTextAt(RESOURCE_BUNDLE, KEY_PREFIX + dissentKey));

        commitButton.setText(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY_PREFIX + (focused.kind().isGoingRogue() ? "commit.rogue" : "commit.side"),
              focused.title()).toUpperCase(Locale.ROOT));
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildFocusedCard(PickerChoice choice) {
        boolean isRogue = choice.kind().isGoingRogue();
        JPanel card = cardShell(SURFACE, isRogue ? ROGUE_BORDER : BORDER_CYAN, isRogue ? AMBER : ACCENT);

        card.add(leftAligned(identity(choice, FOCUSED_CODE_BOX_SIZE, 1.0f, TEXT, 1.05f)));
        card.add(verticalGap(12));

        JLabel pitch = wrappedLabel("<i>" + choice.pitch() + "</i>", TEXT, 0.95f, PITCH_WIDTH);
        pitch.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, DIVIDER),
              BorderFactory.createEmptyBorder(0, scaleForGUI(12), 0, 0)));
        card.add(leftAligned(pitch));
        card.add(verticalGap(12));

        // One row per context (where the unit ends up, then standing changes), tags side by side within a row
        JPanel tagRows = new JPanel();
        tagRows.setLayout(new BoxLayout(tagRows, BoxLayout.Y_AXIS));
        tagRows.setOpaque(false);
        for (List<Tag> rowTags : choice.tagRows()) {
            if (tagRows.getComponentCount() > 0) {
                tagRows.add(verticalGap(6));
            }
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(6), 0));
            row.setOpaque(false);
            for (Tag tag : rowTags) {
                JLabel tagLabel = new JLabel(tag.text().toUpperCase(Locale.ROOT));
                tagLabel.setFont(hudFont(Font.BOLD, 0.68f, 0.1f));
                tagLabel.setForeground(tag.color());
                tagLabel.setBorder(BorderFactory.createCompoundBorder(
                      BorderFactory.createLineBorder(translucent(tag.color(), 110), scaleForGUI(1)),
                      BorderFactory.createEmptyBorder(scaleForGUI(4), scaleForGUI(7), scaleForGUI(4),
                            scaleForGUI(7))));
                row.add(tagLabel);
            }
            tagRows.add(leftAligned(row));
        }
        card.add(leftAligned(tagRows));
        return card;
    }

    /**
     * A dimmed card beside the focused one. Clicking it moves focus to it; it never commits.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private JPanel buildNeighborCard(int index) {
        PickerChoice choice = content.choices().get(index);
        JPanel card = cardShell(SURFACE_DEEP, BORDER, null);
        card.add(leftAligned(identity(choice, NEIGHBOR_CODE_BOX_SIZE, 0.78f, TEXT_FAINT, 0.9f)));
        card.setToolTipText(wordWrap(choice.title()));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                if (card.contains(event.getPoint())) {
                    focus(index);
                }
            }
        });
        return card;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static JPanel emptyCard() {
        JPanel card = new JPanel();
        card.setOpaque(false);
        return card;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static JPanel cardShell(Color background, Color border, @Nullable Color rail) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(background);
        card.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), scaleForGUI(rail == null ? 1 : 3), scaleForGUI(1),
                    scaleForGUI(1), rail == null ? border : rail),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(14), scaleForGUI(14), scaleForGUI(14))));
        return card;
    }

    /**
     * The faction code box, faction name, and subtitle block shared by focused and neighboring cards.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static JPanel identity(PickerChoice choice, int codeBoxSize, float codeSize, Color titleColor,
          float titleSize) {
        JPanel identity = new JPanel(new BorderLayout(scaleForGUI(12), 0));
        identity.setOpaque(false);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(choice.title());
        title.setFont(hudFont(Font.BOLD, titleSize, 0.0f));
        title.setForeground(titleColor);
        JLabel subtitle = new JLabel(choice.subtitle());
        subtitle.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        subtitle.setForeground(TEXT_MUTED);
        text.add(leftAligned(title));
        text.add(leftAligned(subtitle));

        // Held at the top, so the box stays square instead of stretching to the text's height
        JPanel codeHolder = new JPanel(new BorderLayout());
        codeHolder.setOpaque(false);
        codeHolder.add(codeBox(choice, codeBoxSize, codeSize), BorderLayout.NORTH);

        identity.add(codeHolder, BorderLayout.WEST);
        identity.add(text, BorderLayout.CENTER);
        return identity;
    }

    /**
     * A square box showing the faction's short name, cyan for a side and amber for going rogue.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static JLabel codeBox(PickerChoice choice, int size, float codeSize) {
        boolean isRogue = choice.kind().isGoingRogue();
        JLabel codeBox = new JLabel(choice.code(), SwingConstants.CENTER);
        codeBox.setFont(hudFont(Font.BOLD, codeSize, 0.06f));
        codeBox.setForeground(isRogue ? AMBER : ACCENT_BRIGHT);
        codeBox.setOpaque(true);
        codeBox.setBackground(isRogue ? ROGUE_CODE_BOX : SURFACE_HIGHLIGHT);
        codeBox.setBorder(BorderFactory.createLineBorder(isRogue ? ROGUE_BORDER : BORDER_CYAN, scaleForGUI(1)));
        Dimension boxSize = new Dimension(scaleForGUI(size), scaleForGUI(size));
        codeBox.setPreferredSize(boxSize);
        codeBox.setMinimumSize(boxSize);
        return codeBox;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static JLabel wrappedLabel(String html, Color color, float size, int width) {
        JLabel label = new JLabel("<html><div style='width:" + scaleForGUI(width) + "px'>" + html + "</div></html>");
        label.setFont(hudFont(Font.PLAIN, size, 0.0f));
        label.setForeground(color);
        return label;
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static JComponent verticalGap(int height) {
        JPanel gap = new JPanel();
        gap.setOpaque(false);
        gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(height)));
        gap.setPreferredSize(new Dimension(0, scaleForGUI(height)));
        return leftAligned(gap);
    }

    /**
     * @author Illiani
     * @since 0.51.01
     */
    private static <T extends JComponent> T leftAligned(T component) {
        component.setAlignmentX(LEFT_ALIGNMENT);
        return component;
    }
}
