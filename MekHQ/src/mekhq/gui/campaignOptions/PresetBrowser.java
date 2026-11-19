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
package mekhq.gui.campaignOptions;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.text.html.HTMLEditorKit;

import jakarta.annotation.Nullable;
import mekhq.CampaignPreset;
import mekhq.MHQConstants;
import mekhq.campaign.campaignOptions.CampaignOptionsSystemChanges.TrackedSystem;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCard;
import mekhq.gui.baseComponents.hud.HudCard.Tag;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudGuideDrawer;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.ExampleLine;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Guide;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Step;
import mekhq.gui.baseComponents.hud.HudGuideDrawer.Term;
import mekhq.gui.campaignOptions.PresetGuidePreferences.GuideScreen;

/**
 * A heads-up display browser for campaign presets: a card list on the left, grouped into stock presets and the
 * player's own, and the selected preset's details on the right. Shared by every screen that asks the player to pick a
 * preset, so they look and behave the same.
 *
 * <p>Each stock preset carries a short tag (such as "Recommended") and a one-line summary from this class's resource
 * bundle; the player's own presets fall back to the first sentence of their description. Titles drop the number that
 * sorts the stock presets. A screen can add its own sections beneath the description through a
 * {@link DetailSection}.</p>
 *
 * <p>This class also builds the preset screens' guides, so they share their tabs and preset advice.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class PresetBrowser extends JPanel {
    public static final String RESOURCE_BUNDLE = "mekhq.resources.PresetBrowser";
    private static final String KEY = "PresetBrowser.";
    private static final int LIST_WIDTH = 340;
    private static final int LIST_MAXIMUM_WIDTH = 480;
    private static final int SUB_LINE_LENGTH = 80;

    /**
     * Adds a screen's own sections to a preset's details, beneath its description.
     */
    @FunctionalInterface
    public interface DetailSection {
        /**
         * @param column the details column; add components to it from top to bottom
         * @param preset the preset being shown
         */
        void addTo(JPanel column, CampaignPreset preset);
    }

    private final List<HudCard> cards = new ArrayList<>();
    private final JPanel detail = new JPanel(new BorderLayout());
    private final Consumer<CampaignPreset> onSelect;
    private final @Nullable Consumer<CampaignPreset> onActivate;
    private final @Nullable DetailSection extraDetail;
    private @Nullable CampaignPreset selected;

    /**
     * @param presets     the presets to offer, in order
     * @param onSelect    called when the player selects a preset
     * @param onActivate  called when the player double-clicks a preset or presses Enter on it, to confirm it straight
     *                    away; {@code null} if selecting is all those gestures should do
     * @param extraDetail the screen's own sections for the details, or {@code null} for none
     */
    public PresetBrowser(List<CampaignPreset> presets, Consumer<CampaignPreset> onSelect,
          @Nullable Consumer<CampaignPreset> onActivate, @Nullable DetailSection extraDetail) {
        super(new BorderLayout(scaleForGUI(14), 0));
        setOpaque(false);
        this.onSelect = onSelect;
        this.onActivate = onActivate;
        this.extraDetail = extraDetail;

        JPanel list = new WidthTrackingPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(true);
        list.setBackground(SURFACE_DEEP);
        addGroup(list, text("list.stock"), presets.stream().filter(PresetBrowser::isStockPreset).toList());
        addGroup(list, text("list.yours"), presets.stream().filter(preset -> !isStockPreset(preset)).toList());
        refreshCards(); // fill the cards so the list can be measured

        // The list itself is the view, so it takes the viewport's width and long rows shorten instead of spilling out
        JScrollPane listScroll = new JScrollPane(list);
        Hud.styleScroll(listScroll, SURFACE_DEEP, true);
        listScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        // Wide enough for every row at the player's font size, so names and tags are never cut; sub-lines past the
        // cap shorten with an ellipsis instead
        int contentWidth = list.getPreferredSize().width + listScroll.getVerticalScrollBar().getPreferredSize().width
                                 + scaleForGUI(4);
        int listWidth = Math.clamp(contentWidth, scaleForGUI(LIST_WIDTH), scaleForGUI(LIST_MAXIMUM_WIDTH));
        listScroll.setPreferredSize(new Dimension(listWidth, scaleForGUI(400)));

        detail.setOpaque(true);
        detail.setBackground(SURFACE_DEEP);
        detail.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        refreshDetail();

        add(listScroll, BorderLayout.WEST);
        add(detail, BorderLayout.CENTER);
    }

    /**
     * @return the selected preset, or {@code null} if none is selected
     */
    public @Nullable CampaignPreset getSelected() {
        return selected;
    }

    /**
     * Selects a preset without telling the listener, for example to set the starting selection.
     *
     * @param preset the preset to select, or {@code null} to clear the selection
     */
    public void setSelected(@Nullable CampaignPreset preset) {
        selected = preset;
        refreshCards();
        refreshDetail();
    }

    /**
     * Rebuilds the selected preset's details, for when a {@link DetailSection} would show something different.
     */
    public void refreshDetail() {
        detail.removeAll();
        if (selected == null) {
            JLabel notice = Hud.notice(text("detail.empty"));
            notice.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(60), scaleForGUI(20), 0, scaleForGUI(20)));
            detail.add(notice, BorderLayout.NORTH);
        } else {
            detail.add(buildDetailHeader(selected), BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(buildDetailBody(selected));
            Hud.styleScroll(scroll, SURFACE_DEEP, false);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            detail.add(scroll, BorderLayout.CENTER);
            SwingUtilities.invokeLater(() -> scroll.getVerticalScrollBar().setValue(0));
        }
        detail.revalidate();
        detail.repaint();
    }

    // region List

    private void addGroup(JPanel list, String label, List<CampaignPreset> group) {
        if (group.isEmpty()) {
            return;
        }
        list.add(leftAligned(new HudCard().sectionHeader(label)));
        for (CampaignPreset preset : group) {
            HudCard card = new HudCard();
            card.putClientProperty(CampaignPreset.class, preset);
            card.setFocusable(true);
            card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            card.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    card.requestFocusInWindow();
                    choose(preset);
                }

                @Override
                public void mouseClicked(MouseEvent event) {
                    if ((event.getClickCount() == 2) && (onActivate != null)) {
                        onActivate.accept(preset);
                    }
                }
            });
            card.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent event) {
                    switch (event.getKeyCode()) {
                        case KeyEvent.VK_SPACE -> choose(preset);
                        case KeyEvent.VK_ENTER -> {
                            choose(preset);
                            if (onActivate != null) {
                                onActivate.accept(preset);
                            }
                        }
                        case KeyEvent.VK_UP -> moveSelection(-1);
                        case KeyEvent.VK_DOWN -> moveSelection(1);
                        default -> {
                            return;
                        }
                    }
                    event.consume();
                }
            });
            cards.add(card);
            list.add(leftAligned(card));
        }
    }

    private void choose(CampaignPreset preset) {
        if (preset != selected) {
            setSelected(preset);
        }
        onSelect.accept(preset);
    }

    private void moveSelection(int step) {
        if (cards.isEmpty()) {
            return;
        }
        int current = -1;
        for (int index = 0; index < cards.size(); index++) {
            if (cards.get(index).getClientProperty(CampaignPreset.class) == selected) {
                current = index;
            }
        }
        int next = Math.clamp(current + step, 0, cards.size() - 1);
        HudCard card = cards.get(next);
        choose((CampaignPreset) card.getClientProperty(CampaignPreset.class));
        card.requestFocusInWindow();
        card.scrollRectToVisible(new Rectangle(card.getSize()));
    }

    private void refreshCards() {
        for (HudCard card : cards) {
            CampaignPreset preset = (CampaignPreset) card.getClientProperty(CampaignPreset.class);
            List<Tag> tags = tagsFor(preset);
            Color ring = tags.isEmpty() ? (isStockPreset(preset) ? TEXT_FAINT : ACCENT) : tags.get(0).color();
            card.show(ring, preset == selected, displayTitle(preset), null, tags, subLineFor(preset), null, null,
                  preset == selected);
        }
    }

    // endregion List

    // region Details

    private JComponent buildDetailHeader(CampaignPreset preset) {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(18), scaleForGUI(10), scaleForGUI(18))));
        JLabel name = new JLabel(displayTitle(preset).toUpperCase(Locale.ROOT));
        name.setForeground(ACCENT_BRIGHT);
        name.setFont(hudFont(Font.BOLD, 1.2f, 0.14f));
        header.add(leftAligned(name));
        List<Tag> tags = tagsFor(preset);
        if (!tags.isEmpty()) {
            JPanel tagRow = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(10), 0));
            tagRow.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(4), -scaleForGUI(10), 0, 0));
            for (Tag tag : tags) {
                JLabel label = new JLabel(tag.text().toUpperCase(Locale.ROOT));
                label.setForeground(tag.color());
                label.setFont(hudFont(Font.BOLD, 0.72f, 0.12f));
                tagRow.add(label);
            }
            header.add(leftAligned(tagRow));
        }
        return header;
    }

    private JComponent buildDetailBody(CampaignPreset preset) {
        JPanel column = new WidthTrackingPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(true);
        column.setBackground(SURFACE_DEEP);
        column.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(18), scaleForGUI(16),
              scaleForGUI(18)));

        String description = preset.getDescription();
        if ((description != null) && !description.isBlank()) {
            column.add(leftAligned(descriptionPane(description)));
            column.add(Box.createVerticalStrut(scaleForGUI(16)));
        } else {
            column.add(leftAligned(Hud.hint(text("detail.noDescription"))));
            column.add(Box.createVerticalStrut(scaleForGUI(16)));
        }
        if (extraDetail != null) {
            extraDetail.addTo(column, preset);
        }
        return column;
    }

    /**
     * A read-only HTML view of a preset's description, styled for the HUD.
     */
    private static JComponent descriptionPane(String html) {
        JEditorPane pane = new JEditorPane();
        pane.setEditable(false);
        pane.setFocusable(false);
        pane.setOpaque(false);
        HTMLEditorKit kit = new HTMLEditorKit();
        pane.setEditorKit(kit);
        Font font = hudFont(Font.PLAIN, 0.95f, 0.0f);
        kit.getStyleSheet().addRule("body { color: " + hex(TEXT_MUTED) + "; font-family: '" + font.getFamily()
                                          + "'; font-size: " + font.getSize() + "pt; margin: 0; }");
        kit.getStyleSheet().addRule("h1, h2, h3 { color: " + hex(TEXT) + "; font-size: " + (font.getSize() + 1)
                                          + "pt; margin-top: 10px; margin-bottom: 2px; }");
        kit.getStyleSheet().addRule("b { color: " + hex(TEXT) + "; }");
        kit.getStyleSheet().addRule("ul { margin-top: 2px; margin-left: 16px; }");
        pane.setText("<html><body>" + html + "</body></html>");
        pane.setCaretPosition(0);
        return pane;
    }

    // endregion Details

    // region Presets

    /**
     * @param preset a preset
     *
     * @return {@code true} if the preset ships with MekHQ, {@code false} if the player made it
     */
    public static boolean isStockPreset(CampaignPreset preset) {
        File file = preset.getPresetFile();
        if (file == null) {
            return !preset.isUserData();
        }
        try {
            File stockDirectory = new File(MHQConstants.CAMPAIGN_PRESET_DIRECTORY).getCanonicalFile();
            File parent = file.getCanonicalFile().getParentFile();
            return stockDirectory.equals(parent);
        } catch (IOException exception) {
            return false;
        }
    }

    /**
     * @param preset a preset
     *
     * @return the preset's title without the number that sorts the stock presets, such as the "2 - " in
     *       "2 - Veteran Experience"
     */
    public static String displayTitle(CampaignPreset preset) {
        return preset.getTitle().replaceFirst("^\\s*\\d+\\s*-\\s*", "").trim();
    }

    /**
     * @param preset a preset
     *
     * @return the key that looks up the preset's tag and sub-line, such as "veteran_experience"
     */
    static String presetKey(CampaignPreset preset) {
        return displayTitle(preset).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_")
                     .replaceAll("^_|_$", "");
    }

    private static List<Tag> tagsFor(CampaignPreset preset) {
        List<Tag> tags = new ArrayList<>();
        if (isStockPreset(preset)) {
            String tag = text("preset." + presetKey(preset) + ".tag");
            if (isResourceKeyValid(tag)) {
                tags.add(new Tag(tag, colorNamed(text("preset." + presetKey(preset) + ".tagColor"))));
            }
        } else {
            tags.add(new Tag(text("list.yours.tag"), ACCENT));
        }
        return tags;
    }

    private static String subLineFor(CampaignPreset preset) {
        if (isStockPreset(preset)) {
            String sub = text("preset." + presetKey(preset) + ".sub");
            if (isResourceKeyValid(sub)) {
                return sub;
            }
        }
        return firstSentence(preset.getDescription());
    }

    /**
     * @param html a preset description, which may contain HTML
     *
     * @return the description's first sentence as plain text, shortened to fit a list row
     */
    static String firstSentence(@Nullable String html) {
        if (html == null) {
            return "";
        }
        String plain = html.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
        int end = plain.indexOf(". ");
        String sentence = (end < 0) ? plain : plain.substring(0, end + 1);
        if (sentence.length() > SUB_LINE_LENGTH) {
            sentence = sentence.substring(0, SUB_LINE_LENGTH - 1).trim() + "\u2026";
        }
        return sentence;
    }

    private static Color colorNamed(String name) {
        return switch (name.trim().toUpperCase(Locale.ROOT)) {
            case "READY" -> READY;
            case "DANGER" -> DANGER;
            case "CAUTION" -> CAUTION;
            case "INELIGIBLE" -> INELIGIBLE;
            default -> ACCENT;
        };
    }

    /**
     * @param system a campaign system
     *
     * @return the system's name, for showing to the player
     */
    public static String systemName(TrackedSystem system) {
        return text("system." + system.name());
    }

    // endregion Presets

    // region Guide

    /**
     * Builds a guide drawer for a preset screen, with the shared Steps, Which Preset, and Questions tabs and a
     * checkbox that keeps it opening every time.
     *
     * @param screen          the screen, whose preferences the checkbox changes
     * @param alwaysOpenLabel the checkbox's label
     * @param onClose         called when the player closes the guide
     *
     * @return the drawer; install it on the screen's root pane
     */
    public static HudGuideDrawer createGuideDrawer(GuideScreen screen, String alwaysOpenLabel, Runnable onClose) {
        HudCheckBox alwaysOpen = new HudCheckBox(alwaysOpenLabel);
        alwaysOpen.setSelected(PresetGuidePreferences.getInstance().isGuideAlwaysOpen(screen));
        alwaysOpen.addActionListener(event -> PresetGuidePreferences.getInstance()
                                                    .setGuideAlwaysOpen(screen, alwaysOpen.isSelected()));
        return new HudGuideDrawer(List.of(text("guide.tab.steps"), text("guide.tab.presets"),
              text("guide.tab.questions")), alwaysOpen, text("guide.close"), onClose);
    }

    /**
     * Builds a preset screen's guide from its resource bundle. The bundle supplies {@code <prefix>guide.eyebrow},
     * {@code guide.title}, {@code guide.presetsIntro}, numbered {@code guide.step.N.title}/{@code .text}, numbered
     * {@code guide.question.N.name}/{@code .text}, and optionally numbered {@code guide.preset.N.who}/{@code .text}
     * that follow the shared advice on which preset suits whom.
     *
     * @param bundle the screen's resource bundle
     * @param prefix the prefix of the screen's keys, such as {@code "CampaignUpgradeConsole."}
     *
     * @return the guide
     */
    public static Guide buildGuide(String bundle, String prefix) {
        List<Step> steps = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(getTextAt(bundle, prefix + "guide.step." + number + ".title"));
              number++) {
            steps.add(new Step(getTextAt(bundle, prefix + "guide.step." + number + ".title"),
                  getTextAt(bundle, prefix + "guide.step." + number + ".text")));
        }
        List<ExampleLine> advice = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(text("guide.preset." + number + ".who")); number++) {
            advice.add(new ExampleLine(text("guide.preset." + number + ".who"),
                  text("guide.preset." + number + ".text")));
        }
        for (int number = 1; isResourceKeyValid(getTextAt(bundle, prefix + "guide.preset." + number + ".who"));
              number++) {
            advice.add(new ExampleLine(getTextAt(bundle, prefix + "guide.preset." + number + ".who"),
                  getTextAt(bundle, prefix + "guide.preset." + number + ".text")));
        }
        List<Term> questions = new ArrayList<>();
        for (int number = 1; isResourceKeyValid(getTextAt(bundle, prefix + "guide.question." + number + ".name"));
              number++) {
            questions.add(new Term(getTextAt(bundle, prefix + "guide.question." + number + ".name"),
                  getTextAt(bundle, prefix + "guide.question." + number + ".text")));
        }
        return new Guide(getTextAt(bundle, prefix + "guide.eyebrow"), getTextAt(bundle, prefix + "guide.title"),
              steps, null, getTextAt(bundle, prefix + "guide.presetsIntro"), advice, questions, null);
    }

    // endregion Guide

    private static String text(String key) {
        return getTextAt(RESOURCE_BUNDLE, KEY + key);
    }

    /** A panel that takes the width of the scroll pane it sits in, so wrapped text never scrolls sideways. */
    private static final class WidthTrackingPanel extends JPanel implements Scrollable {
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
