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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Rectangle;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import megamek.common.annotations.Nullable;

/**
 * A guide panel that slides over the right of a HUD window to teach the page beneath it, with three tabs: numbered
 * <b>Steps</b>, a worked <b>Example</b>, and the page's <b>Terms</b>. A footer holds an optional checkbox (for example
 * "show when I first open a page") and a close button.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class HudGuideDrawer extends JPanel {
    /** One numbered step. */
    public record Step(String title, String body) {}

    /** One line of a worked example: who speaks ("You", "Oracle") and what they say. */
    public record ExampleLine(String who, String text) {}

    /** One term and its plain-language meaning. */
    public record Term(String name, String meaning) {}

    /**
     * The content of one guide.
     *
     * @param eyebrow    the small caption above the title
     * @param title      the guide's title
     * @param steps      the numbered steps
     * @param stepsExtra an extra component shown after the steps (such as a key to reading results), or {@code null}
     * @param exampleIntro the line introducing the example
     * @param example    the example's lines
     * @param terms      the terms
     * @param termsFooter a component shown after the terms (such as a link to a glossary), or {@code null}
     */
    public record Guide(String eyebrow, String title, List<Step> steps, @Nullable JComponent stepsExtra,
          String exampleIntro, List<ExampleLine> example, List<Term> terms, @Nullable JComponent termsFooter) {}

    private static final int WIDTH = 440;

    private final JLabel eyebrowLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JPanel steps = column();
    private final JPanel example = column();
    private final JPanel terms = column();
    private final CardLayout cards = new CardLayout();
    private final JPanel body = new JPanel(cards);
    private final HudTabStrip tabs;
    private final HudButton closeButton;

    /**
     * @param tabLabels  the labels of the Steps, Example and Terms tabs, in that order
     * @param footerLeft a component for the left of the footer, such as a checkbox, or {@code null}
     * @param closeLabel the close button's label
     * @param onClose    called when the close button is pressed
     */
    public HudGuideDrawer(List<String> tabLabels, @Nullable JComponent footerLeft, String closeLabel,
          Runnable onClose) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(SURFACE_DEEP);
        setBorder(BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER_CYAN));

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(10),
              scaleForGUI(16)));
        eyebrowLabel.setForeground(TEXT_FAINT);
        eyebrowLabel.setFont(hudFont(Font.BOLD, 0.7f, 0.18f));
        titleLabel.setForeground(ACCENT_BRIGHT);
        titleLabel.setFont(hudFont(Font.BOLD, 1.25f, 0.12f));
        head.add(leftAligned(eyebrowLabel));
        head.add(Box.createVerticalStrut(scaleForGUI(3)));
        head.add(leftAligned(titleLabel));

        tabs = new HudTabStrip(tabLabels, true, index -> showTab(index));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(head, BorderLayout.NORTH);
        top.add(tabs, BorderLayout.SOUTH);

        body.setOpaque(false);
        body.add(scroll(steps), "0");
        body.add(scroll(example), "1");
        body.add(scroll(terms), "2");

        JPanel footer = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        footer.setOpaque(true);
        footer.setBackground(SURFACE_DEEP);
        footer.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(16), scaleForGUI(10), scaleForGUI(16))));
        if (footerLeft != null) {
            footer.add(footerLeft, BorderLayout.CENTER);
        }
        closeButton = new HudButton(closeLabel.toUpperCase(Locale.ROOT), true, true);
        closeButton.addActionListener(event -> onClose.run());
        footer.add(closeButton, BorderLayout.EAST);

        add(top, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    /**
     * @return the drawer's preferred width, scaled for the GUI
     */
    public static int drawerWidth() {
        return scaleForGUI(WIDTH);
    }

    /**
     * Shows a guide, opening on its Steps tab.
     *
     * @param guide the guide to show
     */
    public void showGuide(Guide guide) {
        eyebrowLabel.setText(guide.eyebrow().toUpperCase(Locale.ROOT));
        titleLabel.setText(guide.title().toUpperCase(Locale.ROOT));

        steps.removeAll();
        for (int index = 0; index < guide.steps().size(); index++) {
            steps.add(stepRow(index + 1, guide.steps().get(index)));
            steps.add(Box.createVerticalStrut(scaleForGUI(10)));
        }
        if (guide.stepsExtra() != null) {
            steps.add(leftAligned(guide.stepsExtra()));
        }

        example.removeAll();
        example.add(leftAligned(wrapped(guide.exampleIntro(), TEXT_MUTED, Font.PLAIN)));
        example.add(Box.createVerticalStrut(scaleForGUI(10)));
        for (ExampleLine line : guide.example()) {
            example.add(exampleRow(line));
            example.add(Box.createVerticalStrut(scaleForGUI(8)));
        }

        terms.removeAll();
        for (Term term : guide.terms()) {
            terms.add(termRow(term));
            terms.add(Box.createVerticalStrut(scaleForGUI(8)));
        }
        if (guide.termsFooter() != null) {
            terms.add(leftAligned(guide.termsFooter()));
        }

        showTab(0);
        revalidate();
        repaint();
    }

    /**
     * Moves the keyboard focus to the close button, so Space or Enter dismisses the guide.
     */
    public void focusClose() {
        closeButton.requestFocusInWindow();
    }

    private void showTab(int index) {
        tabs.setSelected(index);
        cards.show(body, Integer.toString(index));
    }

    private static JPanel column() {
        JPanel column = new ScrollablePanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        return column;
    }

    private static JScrollPane scroll(JPanel content) {
        JScrollPane scroll = new JScrollPane(content);
        Hud.styleScroll(scroll, SURFACE_DEEP, false);
        return scroll;
    }

    /**
     * A label that wraps its text to the drawer's width.
     */
    private static JLabel wrapped(String text, Color color, int style) {
        int width = scaleForGUI(WIDTH - 170);
        JLabel label = new JLabel("<html><div style='width:" + width + "px'>" + escape(text) + "</div></html>");
        label.setForeground(color);
        label.setFont(hudFont(style, 0.92f, 0.0f));
        return label;
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static JComponent stepRow(int number, Step step) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(false);
        JLabel badge = new JLabel(Integer.toString(number), SwingConstants.CENTER);
        badge.setForeground(ACCENT_BRIGHT);
        badge.setFont(hudFont(Font.BOLD, 0.85f, 0.0f));
        badge.setBorder(BorderFactory.createLineBorder(BORDER_CYAN, scaleForGUI(1)));
        badge.setPreferredSize(new Dimension(scaleForGUI(24), scaleForGUI(24)));
        JPanel badgeHolder = new JPanel(new BorderLayout());
        badgeHolder.setOpaque(false);
        badgeHolder.add(badge, BorderLayout.NORTH);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(leftAligned(wrapped(step.title(), TEXT, Font.BOLD)));
        text.add(Box.createVerticalStrut(scaleForGUI(2)));
        text.add(leftAligned(wrapped(step.body(), TEXT_MUTED, Font.PLAIN)));

        row.add(badgeHolder, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);
        return leftAligned(row);
    }

    private static JComponent exampleRow(ExampleLine line) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(true);
        row.setBackground(translucent(AMBER, 16));
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, AMBER),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(10), scaleForGUI(6), scaleForGUI(10))));
        JLabel who = new JLabel(line.who().toUpperCase(Locale.ROOT));
        who.setForeground(AMBER);
        who.setFont(hudFont(Font.BOLD, 0.7f, 0.14f));
        row.add(leftAligned(who));
        row.add(Box.createVerticalStrut(scaleForGUI(2)));
        row.add(leftAligned(wrapped(line.text(), TEXT, Font.PLAIN)));
        return leftAligned(row);
    }

    private static JComponent termRow(Term term) {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              BorderFactory.createEmptyBorder(0, 0, scaleForGUI(8), 0)));
        JLabel name = new JLabel(term.name().toUpperCase(Locale.ROOT));
        name.setForeground(ACCENT_BRIGHT);
        name.setFont(hudFont(Font.BOLD, 0.8f, 0.12f));
        row.add(leftAligned(name));
        row.add(Box.createVerticalStrut(scaleForGUI(2)));
        row.add(leftAligned(wrapped(term.meaning(), TEXT_MUTED, Font.PLAIN)));
        return leftAligned(row);
    }

    /** A panel that tracks the viewport's width, so wrapped text never scrolls sideways. */
    private static final class ScrollablePanel extends JPanel implements Scrollable {
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
