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
package mekhq.gui.dialog.trainingConsole;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import megamek.client.ui.WrapLayout;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.training.TrainingRefusal;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudButton;

/**
 * Small pieces the Training Console's pages share: the eligibility bar, its legend, a catalog card, tags and labels.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ConsoleWidgets {
    static final Color READY_COLOR = READY;
    static final Color SHORT_COLOR = AMBER;
    static final Color HELD_COLOR = ACCENT;
    static final Color BLOCKED_COLOR = new Color(58, 83, 97);
    /** How long a search field waits after the last keystroke before searching, so typing stays smooth. */
    private static final int SEARCH_DELAY_MILLIS = 150;

    private ConsoleWidgets() {
    }

    /**
     * One part of an eligibility bar.
     *
     * @param color its color
     * @param count how many people it stands for
     * @param label what it means, for the legend, such as "can improve now"
     */
    record Segment(Color color, int count, String label) {}

    /** A thin bar split by how many of the selection fall into each part. */
    static final class StackedBar extends JComponent {
        private final List<Segment> segments;

        StackedBar(List<Segment> segments) {
            this.segments = segments;
            setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(scaleForGUI(120), scaleForGUI(5));
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, scaleForGUI(5));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth();
                int height = getHeight();
                g2.setColor(SURFACE_HIGHLIGHT);
                g2.fillRoundRect(0, 0, width, height, height, height);
                int total = segments.stream().mapToInt(Segment::count).sum();
                if (total == 0) {
                    return;
                }
                int x = 0;
                int drawn = 0;
                for (Segment segment : segments) {
                    if (segment.count() <= 0) {
                        continue;
                    }
                    drawn += segment.count();
                    // Rounded from the running total, so the parts always fill the bar exactly
                    int end = (int) Math.round((double) drawn / total * width);
                    g2.setColor(segment.color());
                    g2.fillRect(x, 0, end - x, height);
                    x = end;
                }
            } finally {
                g2.dispose();
            }
        }
    }

    /** @return a legend for an eligibility bar: a colored square, a count and a meaning per part that isn't empty */
    static JComponent legend(List<Segment> segments) {
        JPanel legend = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(10), scaleForGUI(2)));
        for (Segment segment : segments) {
            if (segment.count() <= 0) {
                continue;
            }
            JPanel item = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(5), 0));
            JComponent swatch = new JComponent() {
                @Override
                protected void paintComponent(Graphics graphics) {
                    graphics.setColor(segment.color());
                    graphics.fillRect(0, 0, getWidth(), getHeight());
                }
            };
            swatch.setPreferredSize(new Dimension(scaleForGUI(8), scaleForGUI(8)));
            item.add(swatch);
            item.add(label(TrainingConsole.formatCount(segment.count()) + ' ' + segment.label(), 0.82f, TEXT_MUTED));
            legend.add(item);
        }
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        return legend;
    }

    static JLabel label(String text, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(hudFont(Font.PLAIN, size, 0.0f));
        label.setForeground(color);
        return label;
    }

    static JLabel bold(String text, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(hudFont(Font.BOLD, size, 0.0f));
        label.setForeground(color);
        return label;
    }

    /** @return a small outlined tag, such as "ROLE SKILL" */
    static JLabel tag(String text, Color color) {
        JLabel tag = new JLabel(text.toUpperCase(Locale.ROOT));
        tag.setFont(hudFont(Font.BOLD, 0.66f, 0.08f));
        tag.setForeground(color);
        tag.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(color.equals(TEXT_FAINT) ? BORDER : color.darker(), scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(1), scaleForGUI(4), scaleForGUI(1), scaleForGUI(4))));
        return tag;
    }

    /** @return a muted line of text that wraps to the width it is given */
    static JLabel wrapped(String text, Color color) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(hudFont(Font.PLAIN, 0.84f, 0.0f));
        label.setForeground(color);
        return label;
    }

    static JPanel column() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    /**
     * A column washed with a see-through tint. Swing assumes an opaque component covers every pixel it owns, so a
     * see-through background on one leaves whatever was last drawn there showing through, such as a button from
     * elsewhere in the dialog. This column stays non-opaque, letting its parents paint first, and lays the tint over
     * them itself.
     *
     * @param tint the see-through color to wash the column with
     *
     * @return the column
     */
    static JPanel tintedColumn(Color tint) {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                Graphics2D graphics2D = (Graphics2D) graphics.create();
                try {
                    graphics2D.setColor(tint);
                    graphics2D.fillRect(0, 0, getWidth(), getHeight());
                } finally {
                    graphics2D.dispose();
                }
            }
        };
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    static JPanel row(int gap) {
        JPanel panel = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(gap), scaleForGUI(3)));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }

    /**
     * Calls back with a text field's text every time it changes.
     *
     * @param field    the field
     * @param onChange called with the new text
     */
    static void onTextChange(JTextField field, Consumer<String> onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                onChange.accept(field.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                onChange.accept(field.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                onChange.accept(field.getText());
            }
        });
    }

    /**
     * Calls back with a search field's text once the player pauses typing, rather than on every keystroke, as each
     * search redraws a list that may hold thousands of people.
     *
     * @param field    the field
     * @param onSearch called with the text
     */
    static void onTextSettled(JTextField field, Consumer<String> onSearch) {
        Timer timer = new Timer(SEARCH_DELAY_MILLIS, event -> onSearch.accept(field.getText()));
        timer.setRepeats(false);
        onTextChange(field, text -> timer.restart());
    }

    /** @return XP as a cost, such as "120 XP", or as XP given back, such as "+40 XP back" */
    static String xpText(int xp) {
        return (xp < 0) ? getFormattedTextAt(TrainingConsole.RESOURCE_BUNDLE, "xp.gained", -xp) :
                     getFormattedTextAt(TrainingConsole.RESOURCE_BUNDLE, "xp.amount", xp);
    }

    /**
     * @param text      the button's text, shown in capitals
     * @param isPrimary {@code true} for a card's main action, {@code false} for a secondary one
     * @param isArmed   whether it can be used
     * @param onClick   what it does
     *
     * @return a compact console button
     */
    static HudButton button(String text, boolean isPrimary, boolean isArmed, Runnable onClick) {
        HudButton button = new HudButton(text.toUpperCase(Locale.ROOT), isPrimary, true);
        button.setArmed(isArmed);
        button.addActionListener(event -> onClick.run());
        return button;
    }

    /** @return a row holding one button on the left, as under a preview */
    static JComponent buttonRow(JComponent button) {
        JPanel row = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, 0, scaleForGUI(6)));
        row.add(button);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** @return a row with a muted label on the left and a control, such as a picker, filling the rest */
    static JComponent labeledRow(String label, JComponent control) {
        JPanel row = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        row.add(label(label, 0.84f, TEXT_MUTED), BorderLayout.WEST);
        row.add(control, BorderLayout.CENTER);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        return row;
    }

    /** @return a lightly tinted, outlined column for a preview of what a button will stage */
    static JPanel previewBox() {
        JPanel box = tintedColumn(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 14));
        box.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 90),
                    scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(10), scaleForGUI(10))));
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    /**
     * A small bar chart of how many of the selection hold each value, such as each skill level or attribute score.
     *
     * @param heading     the chart's heading
     * @param from        the first value shown
     * @param to          the last value shown
     * @param countAt     how many hold a value
     * @param isThreshold which values to mark, such as where a new experience level starts; {@code null} for none
     * @param footnote    a line under the chart, such as what the marks mean; {@code null} for none
     *
     * @return the chart with its heading
     */
    static JComponent spreadChart(String heading, int from, int to, IntUnaryOperator countAt,
          @Nullable IntPredicate isThreshold, @Nullable String footnote) {
        int columns = Math.max(1, (to - from) + 1);
        int[] counts = new int[columns];
        int most = 1;
        for (int column = 0; column < columns; column++) {
            counts[column] = countAt.applyAsInt(from + column);
            most = Math.max(most, counts[column]);
        }
        final int highest = most;
        Color barColor = new Color(ACCENT.getRed(), ACCENT.getGreen(), ACCENT.getBlue(), 140);
        Font font = label("", 0.7f, TEXT_FAINT).getFont();
        JComponent chart = new JComponent() {
            @Override
            protected void paintComponent(Graphics graphics) {
                int gap = scaleForGUI(3);
                int width = Math.max(1, (getWidth() - (gap * (columns - 1))) / columns);
                int barArea = getHeight() - scaleForGUI(12);
                graphics.setFont(font);
                for (int column = 0; column < columns; column++) {
                    int count = counts[column];
                    int x = column * (width + gap);
                    int height = (count == 0) ? 0 :
                                       Math.max(2, (int) Math.round((double) count / highest * (barArea - 2)));
                    graphics.setColor(barColor);
                    graphics.fillRect(x, barArea - height, width, height);
                    int value = from + column;
                    graphics.setColor(((isThreshold != null) && isThreshold.test(value)) ? AMBER : TEXT_FAINT);
                    String text = String.valueOf(value);
                    int textWidth = graphics.getFontMetrics().stringWidth(text);
                    graphics.drawString(text, x + ((width - textWidth) / 2), getHeight() - 1);
                }
            }
        };
        chart.setPreferredSize(new Dimension(scaleForGUI(300), scaleForGUI(56)));
        chart.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(56)));
        chart.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel holder = column();
        holder.add(leftAligned(Hud.eyebrow(heading)));
        holder.add(Box.createVerticalStrut(scaleForGUI(4)));
        holder.add(chart);
        if (footnote != null) {
            holder.add(leftAligned(label(footnote, 0.74f, TEXT_FAINT)));
        }
        return holder;
    }

    /** @return the money as the player reads it, or an empty string for zero */
    static String money(Money money) {
        return money.isZero() ? "" : money.toAmountAndSymbolString();
    }

    /**
     * A card in a page's catalog: a header the player clicks to open the card, a line of detail, an eligibility bar,
     * and the card's main action on the right. An open card shows its body underneath.
     */
    static final class CatalogCard extends JPanel {
        private final JPanel body = column();

        /**
         * @param title     the card's name
         * @param tags      tags after the name
         * @param meta      a line of detail under the name
         * @param bar       the eligibility bar, or {@code null}
         * @param actions   the main action and its cost, on the right
         * @param isOpen    whether to show the body
         * @param onToggle  called when the player clicks the header
         */
        CatalogCard(String title, List<JLabel> tags, String meta, @Nullable StackedBar bar, JComponent actions,
              boolean isOpen, @Nullable Runnable onToggle) {
            super(new BorderLayout());
            setOpaque(true);
            setBackground(isOpen ? SURFACE : SURFACE_DEEP);
            setBorder(BorderFactory.createLineBorder(isOpen ? BORDER_CYAN : BORDER, scaleForGUI(1)));
            setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel head = column();
            // One line: the name takes the width left beside the arrow, and a name too long for it ends in "..."
            JPanel nameLine = Hud.transparentPanel(new BorderLayout(scaleForGUI(5), 0));
            nameLine.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(5), 0, 0));
            if (onToggle != null) {
                // An arrow shows the card opens for more detail, and which way it is now
                nameLine.add(bold(isOpen ? "\u25BE" : "\u25B8", 0.95f, ACCENT), BorderLayout.WEST);
            }
            JLabel name = bold(title, 0.95f, TEXT);
            // Without this the label would ask for its whole width, and the card would grow rather than shorten it
            name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));
            name.setPreferredSize(new Dimension(0, name.getPreferredSize().height));
            nameLine.add(name, BorderLayout.CENTER);
            nameLine.setAlignmentX(Component.LEFT_ALIGNMENT);
            head.add(nameLine);
            if (!tags.isEmpty()) {
                // Tags have their own line under the name, so a long name never pushes them onto a ragged wrap
                JPanel tagLine = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(5), scaleForGUI(2)));
                tags.forEach(tagLine::add);
                tagLine.setAlignmentX(Component.LEFT_ALIGNMENT);
                head.add(tagLine);
            }
            if (!meta.isEmpty()) {
                JLabel metaLabel = label(meta, 0.8f, TEXT_FAINT);
                metaLabel.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(5), scaleForGUI(4), 0));
                head.add(leftAligned(metaLabel));
            }
            if (bar != null) {
                JPanel barHolder = Hud.transparentPanel(new BorderLayout());
                barHolder.setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(5), 0, 0));
                barHolder.add(bar, BorderLayout.CENTER);
                barHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
                head.add(barHolder);
            }
            JPanel top = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
            top.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(5), scaleForGUI(8),
                  scaleForGUI(10)));
            // Pinned to the top at its own height: a taller action column, such as a trait's two buttons, would
            // otherwise stretch the header and the eligibility bar in it
            JPanel headHolder = Hud.transparentPanel(new BorderLayout());
            headHolder.add(head, BorderLayout.NORTH);
            top.add(headHolder, BorderLayout.CENTER);
            JPanel actionHolder = Hud.transparentPanel(new BorderLayout());
            actionHolder.add(actions, BorderLayout.NORTH);
            top.add(actionHolder, BorderLayout.EAST);
            add(top, BorderLayout.NORTH);
            if (onToggle != null) {
                // The whole header strip opens the card: clicks on the name, tags and bar fall through to the header,
                // and clicks on the padding and the space around the action fall through to the strip. The action's
                // own button keeps its clicks. Nothing in the header has a tooltip, which would take its clicks.
                installToggle(top, onToggle);
                installToggle(head, onToggle);
            }

            body.setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(scaleForGUI(1), 0, 0, 0, DIVIDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(10), scaleForGUI(10),
                        scaleForGUI(10))));
            body.setVisible(isOpen);
            add(body, BorderLayout.CENTER);
        }

        private static void installToggle(Component component, Runnable onToggle) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            component.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (component.contains(event.getPoint())) {
                        onToggle.run();
                    }
                }
            });
        }

        /** Adds a part to the card's body, with a gap above it. */
        void addToBody(JComponent component) {
            if (body.getComponentCount() > 0) {
                body.add(Box.createVerticalStrut(scaleForGUI(8)));
            }
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
            body.add(component);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
        }
    }

    /**
     * @return a right-aligned column for a card's action: the button, then its cost lines
     */
    static JComponent actionColumn(JComponent button, String xpLine, String feeLine) {
        return actionColumn(button, xpLine, feeLine, null);
    }

    /**
     * @param reason why the button can't be used, shown under it, or {@code null} if it can be
     *
     * @return a right-aligned column for a card's action: the button, then its cost lines or why it is unavailable
     */
    static JComponent actionColumn(JComponent button, String xpLine, String feeLine, @Nullable String reason) {
        JPanel column = column();
        button.setAlignmentX(Component.RIGHT_ALIGNMENT);
        column.add(button);
        if (!xpLine.isEmpty()) {
            JLabel xp = label(xpLine, 0.78f, TEXT_MUTED);
            xp.setAlignmentX(Component.RIGHT_ALIGNMENT);
            column.add(Box.createVerticalStrut(scaleForGUI(3)));
            column.add(xp);
        }
        if (!feeLine.isEmpty()) {
            JLabel fee = label(feeLine, 0.78f, AMBER);
            fee.setAlignmentX(Component.RIGHT_ALIGNMENT);
            column.add(fee);
        }
        if (reason != null) {
            JLabel why = label(reason, 0.78f, TEXT_FAINT);
            why.setAlignmentX(Component.RIGHT_ALIGNMENT);
            column.add(Box.createVerticalStrut(scaleForGUI(3)));
            column.add(why);
        }
        return column;
    }

    /**
     * @return a button label with how many it applies to, such as "+1 level · 12", or the bare label when it applies
     *       to no one, so a disabled button doesn't read "· 0"
     */
    static String countedLabel(String plainKey, String countKey, int count) {
        return (count > 0) ? getFormattedTextAt(TrainingConsole.RESOURCE_BUNDLE, countKey, count) :
                     getTextAt(TrainingConsole.RESOURCE_BUNDLE, plainKey);
    }

    /**
     * The "can't" lines of a card: one line with everyone who can't and why, such as "5 can’t: 3 not enough XP, 2
     * already have it", then, when there is more than one reason, a line per reason. Each has a link that toggles the
     * selection to those people.
     *
     * @param reasons the people who can't, by why not, in any order; the lines put the most common reason first
     * @param toggle  toggles the selection to a group
     *
     * @return the lines, or none if everyone can
     */
    static List<JComponent> cannotLines(Map<TrainingRefusal, List<Person>> reasons, Consumer<List<Person>> toggle) {
        List<Map.Entry<TrainingRefusal, List<Person>>> sorted = reasons.entrySet().stream()
                                                                      .filter(entry -> !entry.getValue().isEmpty())
                                                                      .sorted(Comparator.comparingInt(
                                                                            entry -> -entry.getValue().size()))
                                                                      .toList();
        if (sorted.isEmpty()) {
            return List.of();
        }
        String link = getTextAt(TrainingConsole.RESOURCE_BUNDLE, "link.toggleThem");
        List<JComponent> lines = new ArrayList<>();
        if (sorted.size() == 1) {
            List<Person> people = sorted.get(0).getValue();
            lines.add(reasonLine(reason(people.size(), sorted.get(0).getKey()), link, () -> toggle.accept(people)));
            return lines;
        }
        List<Person> everyone = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        for (Map.Entry<TrainingRefusal, List<Person>> entry : sorted) {
            everyone.addAll(entry.getValue());
            parts.add(getFormattedTextAt(TrainingConsole.RESOURCE_BUNDLE, "line.cannot.part",
                  entry.getValue().size(), entry.getKey().getLabel()));
        }
        lines.add(reasonLine(getFormattedTextAt(TrainingConsole.RESOURCE_BUNDLE, "line.cannot", everyone.size(),
              String.join(", ", parts)), link, () -> toggle.accept(everyone)));
        for (Map.Entry<TrainingRefusal, List<Person>> entry : sorted) {
            List<Person> people = entry.getValue();
            lines.add(reasonLine(reason(people.size(), entry.getKey()), link, () -> toggle.accept(people)));
        }
        return lines;
    }

    /** @return why a purchase is unavailable, such as "4 can’t: not enough XP" */
    static String reason(int count, TrainingRefusal refusal) {
        return getFormattedTextAt(TrainingConsole.RESOURCE_BUNDLE, "reason.count", count, refusal.getLabel());
    }

    /** @return a line with text on the left and a link on the right, such as "12 short on XP · Toggle them" */
    static JComponent reasonLine(String text, @Nullable String linkText, @Nullable Runnable onLink) {
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        line.add(label(text, 0.84f, TEXT_MUTED), BorderLayout.CENTER);
        if ((linkText != null) && (onLink != null)) {
            line.add(Hud.link(linkText, onLink), BorderLayout.EAST);
        }
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    /** @return a colored box of fee lines, such as how many people reach Regular and what each pays */
    static JComponent feeBox(String title, String total, List<String[]> lines, @Nullable String warning) {
        JPanel box = tintedColumn(new Color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(), 18));
        box.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createLineBorder(new Color(AMBER.getRed(), AMBER.getGreen(), AMBER.getBlue(), 90),
                    scaleForGUI(1)),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(8), scaleForGUI(6), scaleForGUI(8))));
        box.add(pair(title, total, AMBER, true));
        for (String[] line : lines) {
            box.add(pair(line[0], line[1], TEXT_MUTED, false));
        }
        if (warning != null) {
            box.add(leftAligned(label(warning, 0.82f, DANGER)));
        }
        box.setAlignmentX(Component.LEFT_ALIGNMENT);
        return box;
    }

    /** @return a line with text on the left and a value on the right */
    static JComponent pair(String left, String right, Color color, boolean isBold) {
        JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        line.add(isBold ? bold(left, 0.84f, color) : label(left, 0.82f, color), BorderLayout.CENTER);
        line.add(isBold ? bold(right, 0.84f, color) : label(right, 0.82f, color), BorderLayout.EAST);
        line.setAlignmentX(Component.LEFT_ALIGNMENT);
        return line;
    }

    /** The colors a level reads in, matching the crew assignment dialog. */
    static Color levelColor(SkillLevel level) {
        return switch (level) {
            case LEGENDARY, HEROIC -> AMBER;
            case ELITE -> READY;
            case VETERAN -> ACCENT_BRIGHT;
            case REGULAR -> TEXT;
            case GREEN -> TEXT_MUTED;
            default -> INELIGIBLE;
        };
    }
}
