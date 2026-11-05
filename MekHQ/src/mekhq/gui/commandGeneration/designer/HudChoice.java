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
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;

/**
 * One choice among a list that changes as other selections change, such as the sizes a faction fields. A short list is
 * shown as a segmented control, so every option is visible at once; a long one as a HUD drop-down. Segments that
 * would not fit side by side in the width the control is given wrap onto further rows, and a list whose labels are
 * too long for even two to a row is shown as a drop-down rather than cut off. When they wrap, a leading Random entry
 * takes a row of its own across the full width, so the real choices sit together beneath it.
 *
 * <p>Values may be {@code null}, which pickers use for their Random or combined entry. Setting the choices or the
 * selection never tells the listener; only the player's choices do.</p>
 *
 * @param <T> the type of value
 */
public final class HudChoice<T> extends JPanel {

    /**
     * One entry.
     *
     * @param value the value, or {@code null} for a Random or combined entry
     * @param label the text shown for it
     * @param sub   a short line under the label when shown as a segment, or {@code null}
     */
    public record Choice<T>(@Nullable T value, String label, @Nullable String sub) {
        public Choice(@Nullable T value, String label) {
            this(value, label, null);
        }
    }

    /** A drop-down entry; its text is what the HUD renderer shows. */
    private record Item(int index, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Segment title font and padding, as {@link HudSegmentedControl}'s stand-alone style draws them. */
    private static final float SEGMENT_FONT_SCALE = 0.78f;
    private static final float SEGMENT_TRACKING = 0.1f;
    private static final int SEGMENT_PADDING = 16;
    /** The default width the control is laid out in, before GUI scaling. */
    private static final int DEFAULT_WIDTH = 260;

    private final int maximumSegments;
    private final int availableWidth;
    private final Consumer<T> onChoose;
    private final List<Choice<T>> choices = new ArrayList<>();
    /** The leading Random entry, on a row of its own when the rest wrap; empty and hidden otherwise. */
    private final HudSegmentedControl<Integer> lead;
    private final HudSegmentedControl<Integer> segments;
    private final JPanel segmentStack = new JPanel(new BorderLayout());
    private boolean leadOnOwnRow;
    /** Whether the first entry is a Random entry even though its value is not {@code null}. */
    private boolean firstIsRandom;
    private final JComboBox<Item> dropDown = new JComboBox<>();
    private boolean filling;
    private boolean showingSegments;

    /**
     * @param maximumSegments the most entries shown as segments; longer lists use a drop-down
     * @param onChoose        told the value the player chooses
     */
    public HudChoice(int maximumSegments, Consumer<T> onChoose) {
        this(maximumSegments, DEFAULT_WIDTH, onChoose);
    }

    /**
     * @param maximumSegments the most entries shown as segments; longer lists use a drop-down
     * @param availableWidth  the width the control is laid out in, before GUI scaling, which decides how many
     *                        segments fit in a row
     * @param onChoose        told the value the player chooses
     */
    public HudChoice(int maximumSegments, int availableWidth, Consumer<T> onChoose) {
        super(new BorderLayout());
        this.maximumSegments = maximumSegments;
        this.availableWidth = availableWidth;
        this.onChoose = onChoose;
        setOpaque(false);
        lead = new HudSegmentedControl<>(index -> {
            if (!filling) {
                chosen(index);
            }
        });
        segments = new HudSegmentedControl<>(index -> {
            if (!filling) {
                chosen(index);
            }
        });
        segmentStack.setOpaque(false);
        segmentStack.add(lead, BorderLayout.NORTH);
        segmentStack.add(segments, BorderLayout.CENTER);
        Hud.styleComboBox(dropDown);
        dropDown.setRenderer(Hud.comboRenderer());
        dropDown.addActionListener(event -> {
            if (!filling && (dropDown.getSelectedItem() instanceof Item item)) {
                chosen(item.index());
            }
        });
    }

    private void chosen(int index) {
        if ((index >= 0) && (index < choices.size())) {
            selectSegment(index);
            onChoose.accept(choices.get(index).value());
        }
    }

    /** Selects a segment in whichever of the two rows holds it, clearing the other. */
    private void selectSegment(int index) {
        boolean inLead = leadOnOwnRow && (index == 0);
        lead.setSelected(inLead ? 0 : null);
        segments.setSelected((!inLead && (index >= 0)) ? index : null);
    }

    /**
     * Replaces the entries and selects one, without telling the listener.
     *
     * @param newChoices the entries, in order
     * @param selected   the value to select; one that is not offered leaves nothing selected
     */
    public void setChoices(List<Choice<T>> newChoices, @Nullable T selected) {
        filling = true;
        try {
            choices.clear();
            choices.addAll(newChoices);
            int selectedIndex = indexOf(selected);
            int perRow = segmentsPerRow(choices);
            boolean useSegments = !choices.isEmpty() && (choices.size() <= maximumSegments) && (perRow > 0);
            if (useSegments) {
                // Once the segments wrap, a leading Random entry reads better on a row of its own above the real
                // choices than as one cell among them.
                boolean wraps = perRow < choices.size();
                leadOnOwnRow = wraps && (choices.size() > 2) && (firstIsRandom || (choices.get(0).value() == null));
                int first = leadOnOwnRow ? 1 : 0;
                int restPerRow = leadOnOwnRow ? segmentsPerRow(choices.subList(1, choices.size())) : perRow;
                if (restPerRow <= 0) {
                    restPerRow = perRow;
                }
                List<Segment<Integer>> leadList = new ArrayList<>();
                if (leadOnOwnRow) {
                    Choice<T> choice = choices.get(0);
                    leadList.add(new Segment<>(0, choice.label(), choice.sub(), true));
                }
                lead.setSegments(leadList);
                lead.setVisible(leadOnOwnRow);
                int restCount = choices.size() - first;
                segments.setColumns((restPerRow >= restCount) ? 0 : restPerRow);
                List<Segment<Integer>> list = new ArrayList<>();
                for (int index = first; index < choices.size(); index++) {
                    Choice<T> choice = choices.get(index);
                    list.add(new Segment<>(index, choice.label(), choice.sub(), true));
                }
                segments.setSegments(list);
                // Stacked under the lead row, the rest share its bottom edge rather than drawing a second one.
                segments.setBorder(leadOnOwnRow
                      ? BorderFactory.createMatteBorder(0, scaleForGUI(1), scaleForGUI(1), scaleForGUI(1), BORDER)
                      : BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
                selectSegment(selectedIndex);
            } else {
                leadOnOwnRow = false;
                dropDown.removeAllItems();
                for (int index = 0; index < choices.size(); index++) {
                    dropDown.addItem(new Item(index, choices.get(index).label()));
                }
                dropDown.setSelectedIndex(selectedIndex);
            }
            if (useSegments != showingSegments || getComponentCount() == 0) {
                removeAll();
                add(useSegments ? segmentStack : dropDown, BorderLayout.CENTER);
                showingSegments = useSegments;
            }
            setMaximumSize(new Dimension(Short.MAX_VALUE, getPreferredSize().height));
            revalidate();
            repaint();
        } finally {
            filling = false;
        }
    }

    /**
     * How many segments to put in a row so that every label fits: as many as the width takes, then balanced across
     * the rows so the last row is not left with one. A list that would need more than one segment per row but cannot
     * fit two is better as a drop-down.
     *
     * @return the segments per row, or {@code 0} to use a drop-down instead
     */
    private int segmentsPerRow(List<Choice<T>> entries) {
        if (entries.isEmpty()) {
            return 0;
        }
        FontMetrics titleMetrics = getFontMetrics(hudFont(Font.BOLD, SEGMENT_FONT_SCALE, SEGMENT_TRACKING));
        FontMetrics subMetrics = getFontMetrics(hudFont(Font.PLAIN, 0.74f, 0.0f));
        int widest = 0;
        for (Choice<T> choice : entries) {
            // Letter tracking widens the title by about a tenth of the font size per character, which the plain
            // metrics do not count.
            String title = choice.label().toUpperCase(Locale.ROOT);
            int titleWidth = titleMetrics.stringWidth(title)
                  + Math.round(title.length() * titleMetrics.getFont().getSize2D() * SEGMENT_TRACKING);
            int subWidth = (choice.sub() == null) ? 0 : subMetrics.stringWidth(choice.sub());
            widest = Math.max(widest, Math.max(titleWidth, subWidth));
        }
        int cellWidth = widest + scaleForGUI(SEGMENT_PADDING);
        int fit = Math.max(1, scaleForGUI(availableWidth) / Math.max(1, cellWidth));
        if (fit >= entries.size()) {
            return entries.size();
        }
        if (fit < 2) {
            return (entries.size() <= 1) ? 1 : 0;
        }
        int rows = (entries.size() + fit - 1) / fit;
        return (entries.size() + rows - 1) / rows;
    }

    /**
     * Marks the first entry as a Random entry, for a list whose Random entry carries a value of its own rather than
     * {@code null}, so it takes a row of its own when the segments wrap. Takes effect at the next
     * {@link #setChoices}.
     *
     * @param firstIsRandom {@code true} if the first entry is a Random entry
     */
    public void setFirstIsRandom(boolean firstIsRandom) {
        this.firstIsRandom = firstIsRandom;
    }

    /** Selects a value without telling the listener; one that is not offered leaves nothing selected. */
    public void setSelected(@Nullable T value) {
        filling = true;
        try {
            int index = indexOf(value);
            if (showingSegments) {
                selectSegment(index);
            } else {
                dropDown.setSelectedIndex(index);
            }
        } finally {
            filling = false;
        }
    }

    /** @return the selected value, or {@code null} for a Random entry or nothing selected */
    public @Nullable T getSelected() {
        int index;
        if (!showingSegments) {
            index = dropDown.getSelectedIndex();
        } else if (leadOnOwnRow && (lead.getSelected() != null)) {
            index = 0;
        } else {
            index = Objects.requireNonNullElse(segments.getSelected(), -1);
        }
        return ((index >= 0) && (index < choices.size())) ? choices.get(index).value() : null;
    }

    /** @return the number of entries offered */
    public int getChoiceCount() {
        return choices.size();
    }

    private int indexOf(@Nullable T value) {
        for (int index = 0; index < choices.size(); index++) {
            if (Objects.equals(choices.get(index).value(), value)) {
                return index;
            }
        }
        return -1;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        dropDown.setEnabled(enabled);
        segments.setEnabled(enabled);
        lead.setEnabled(enabled);
        for (int index = 0; index < choices.size(); index++) {
            segments.setSegmentEnabled(index, enabled);
            lead.setSegmentEnabled(index, enabled);
        }
    }
}
