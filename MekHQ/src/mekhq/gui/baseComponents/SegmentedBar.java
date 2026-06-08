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
package mekhq.gui.baseComponents;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;

/**
 * A compact, reusable gauge that draws a row of equally-sized colored segments.
 *
 * <p>
 * Each segment carries its own fill {@link Color} and an optional tooltip shown
 * when the mouse hovers over that
 * segment. A subset of leading segments can be marked as "filled" (drawn in
 * full color) while the remainder are drawn
 * as neutral, hueless empty slots; this lets the component act either as a
 * static colored scale (all segments filled)
 * or as a level/progress indicator (filled up to the current value). Rendering
 * is intentionally flat: segments are
 * solid rounded rectangles with no borders or bevels. An optional thin outline
 * can highlight the last filled segment to
 * call out the current level.
 * </p>
 *
 * <p>
 * The component is value-agnostic. Callers describe what to show by supplying a
 * list of {@link Segment Segments};
 * helper factory methods are provided to build those segments from a color
 * gradient. This makes the same control usable
 * for very different concepts (morale, reputation, readiness, ...) simply by
 * changing the segments and tooltips.
 * </p>
 *
 * @author The MegaMek Team
 */
public class SegmentedBar extends JComponent {
    /**
     * A single segment of the bar.
     *
     * @param color   the fill color used when the segment is active
     * @param tooltip the tooltip to show when hovering this segment, or
     *                {@code null} for no tooltip
     */
    public record Segment(Color color, @Nullable String tooltip) {
    }

    private static final int DEFAULT_GAP = 2;
    private static final int DEFAULT_ARC = 4;
    /**
     * Vertical gap between the segment row and the active label drawn beneath it.
     */
    private static final int DEFAULT_LABEL_GAP = 3;
    /** Alpha applied to the neutral fill of inactive ("empty slot") segments. */
    private static final int INACTIVE_FILL_ALPHA = 28;
    /** Alpha applied to the outline that highlights the last filled segment. */
    private static final int MARKER_ALPHA = 210;

    private List<Segment> segments = List.of();
    private int filledCount = -1;
    private boolean showEdgeMarker = true;
    private String activeLabel;

    /**
     * Creates an empty bar. Use {@link #setSegments(List)} to populate it.
     */
    public SegmentedBar() {
        setOpaque(false);
        // Register so that per-segment tooltips returned from
        // getToolTipText(MouseEvent) are displayed.
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    /**
     * Sets the segments to display, left to right.
     *
     * @param segments the segments to draw; {@code null} is treated as an empty
     *                 list
     */
    public void setSegments(final @Nullable List<Segment> segments) {
        this.segments = (segments == null) ? List.of() : List.copyOf(segments);
        revalidate();
        repaint();
    }

    /**
     * Sets how many leading segments are drawn in full color. Segments at or beyond
     * this count are drawn as neutral
     * empty slots.
     *
     * @param filledCount the number of active segments, or a negative value to
     *                    treat every segment as filled
     */
    public void setFilledCount(final int filledCount) {
        this.filledCount = filledCount;
        repaint();
    }

    /**
     * Controls whether a thin outline highlights the last filled segment to
     * emphasise the current level.
     *
     * @param showEdgeMarker {@code true} to draw the highlight (the default),
     *                       {@code false} to hide it
     */
    public void setShowEdgeMarker(final boolean showEdgeMarker) {
        this.showEdgeMarker = showEdgeMarker;
        repaint();
    }

    /**
     * Sets an optional text label drawn, centered, beneath the last filled
     * ("active") segment. The label may be wider
     * than the segment it labels; it is centered on that segment's center and only
     * nudged inward to stay within the
     * component's bounds. Passing {@code null} or a blank string hides the label
     * and reclaims its vertical space.
     *
     * @param activeLabel the text to draw under the active segment, or {@code null}
     *                    for none
     */
    public void setActiveLabel(final @Nullable String activeLabel) {
        this.activeLabel = activeLabel;
        revalidate();
        repaint();
    }

    /**
     * @return the effective number of filled segments, clamped to the segment count
     */
    protected int effectiveFilledCount() {
        return (filledCount < 0) ? segments.size() : Math.min(filledCount, segments.size());
    }

    @Override
    public Dimension getPreferredSize() {
        final Dimension base = UIUtil.scaleForGUI(140, 14);
        return new Dimension(base.width, base.height + labelAreaHeight());
    }

    @Override
    public Dimension getMinimumSize() {
        final Dimension base = UIUtil.scaleForGUI(84, 10);
        return new Dimension(base.width, base.height + labelAreaHeight());
    }

    private int gap() {
        return Math.max(UIUtil.scaleForGUI(DEFAULT_GAP), 1);
    }

    private int arc() {
        return Math.max(UIUtil.scaleForGUI(DEFAULT_ARC), 2);
    }

    private int labelGap() {
        return Math.max(UIUtil.scaleForGUI(DEFAULT_LABEL_GAP), 1);
    }

    /**
     * @return the vertical space reserved beneath the segment row for the active
     *         label, or {@code 0} when no label is
     *         set
     */
    private int labelAreaHeight() {
        if ((activeLabel == null) || activeLabel.isBlank()) {
            return 0;
        }
        return labelGap() + getFontMetrics(getFont()).getHeight();
    }

    /**
     * @return the height available for the colored segment row, excluding any
     *         active-label area
     */
    private int barHeight() {
        return Math.max(getHeight() - labelAreaHeight(), 1);
    }

    /**
     * Computes the pixel bounds of the segment at the given index. Both painting
     * and tooltip hit-testing use this so
     * they stay in sync.
     *
     * @param index the zero-based segment index
     *
     * @return the bounds of the segment, or {@code null} if the index is out of
     *         range
     */
    protected @Nullable Rectangle segmentBounds(final int index) {
        final int count = segments.size();
        if ((count == 0) || (index < 0) || (index >= count)) {
            return null;
        }
        final int gap = gap();
        final int totalGap = gap * (count - 1);
        final int segmentWidth = Math.max((getWidth() - totalGap) / count, 1);
        final int usedWidth = segmentWidth * count + totalGap;
        final int xStart = Math.max((getWidth() - usedWidth) / 2, 0);
        final int x = xStart + index * (segmentWidth + gap);
        return new Rectangle(x, 0, segmentWidth, barHeight());
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (segments.isEmpty()) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int arc = arc();
            final int filled = effectiveFilledCount();
            final Color neutral = neutralColor();

            for (int i = 0; i < segments.size(); i++) {
                final Rectangle bounds = segmentBounds(i);
                if (bounds == null) {
                    continue;
                }

                if (i < filled) {
                    g2.setColor(segments.get(i).color());
                } else {
                    g2.setColor(new Color(neutral.getRed(), neutral.getGreen(), neutral.getBlue(),
                            INACTIVE_FILL_ALPHA));
                }
                g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc, arc);
            }

            if (showEdgeMarker && (filled > 0)) {
                final Rectangle bounds = segmentBounds(filled - 1);
                if (bounds != null) {
                    g2.setColor(new Color(neutral.getRed(), neutral.getGreen(), neutral.getBlue(), MARKER_ALPHA));
                    g2.drawRoundRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1, arc, arc);
                }
            }

            paintActiveLabel(g2, filled);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Draws the active label, if set, centered beneath the last filled segment.
     *
     * @param g2     the graphics context to paint on
     * @param filled the effective number of filled segments
     */
    private void paintActiveLabel(final Graphics2D g2, final int filled) {
        if ((activeLabel == null) || activeLabel.isBlank() || (filled <= 0)) {
            return;
        }
        final Rectangle active = segmentBounds(filled - 1);
        if (active == null) {
            return;
        }
        g2.setColor(neutralColor());
        final FontMetrics metrics = g2.getFontMetrics(getFont());
        final int textWidth = metrics.stringWidth(activeLabel);
        final int centerX = active.x + (active.width / 2);
        int textX = centerX - (textWidth / 2);
        // Keep the (potentially wider) label within the component bounds.
        textX = Math.max(0, Math.min(textX, getWidth() - textWidth));
        final int textY = active.height + labelGap() + metrics.getAscent();
        g2.drawString(activeLabel, textX, textY);
    }

    @Override
    public @Nullable String getToolTipText(final MouseEvent event) {
        for (int i = 0; i < segments.size(); i++) {
            final Rectangle bounds = segmentBounds(i);
            if ((bounds != null) && bounds.contains(event.getPoint())) {
                final String tooltip = segments.get(i).tooltip();
                if (tooltip != null) {
                    return tooltip;
                }
            }
        }
        return super.getToolTipText(event);
    }

    /**
     * Returns a theme-aware neutral color used for empty slots and the edge marker.
     * Derived from the label foreground
     * color so it reads correctly on both light and dark themes.
     *
     * @return the neutral color
     */
    private static Color neutralColor() {
        final Color foreground = UIManager.getColor("Label.foreground");
        return (foreground == null) ? Color.GRAY : foreground;
    }

    /**
     * Builds a list of segments whose colors are interpolated across the given
     * gradient anchors. The number of segments
     * equals the number of tooltips; anchor colors are spread evenly across that
     * range.
     *
     * @param anchors  two or more colors to interpolate across, from first segment
     *                 to last
     * @param tooltips one tooltip per segment (its length determines the number of
     *                 segments)
     *
     * @return the generated segments
     */
    public static List<Segment> gradientSegments(final Color[] anchors, final String[] tooltips) {
        final int count = tooltips.length;
        final List<Segment> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            result.add(new Segment(interpolatedColor(anchors, count, i), tooltips[i]));
        }
        return result;
    }

    /**
     * Convenience overload that interpolates between a single start and end color.
     *
     * @param start    the color of the first segment
     * @param end      the color of the last segment
     * @param tooltips one tooltip per segment (its length determines the number of
     *                 segments)
     *
     * @return the generated segments
     */
    public static List<Segment> gradientSegments(final Color start, final Color end, final String[] tooltips) {
        return gradientSegments(new Color[] { start, end }, tooltips);
    }

    /**
     * Computes a single interpolated color for a segment index within a gradient.
     *
     * @param anchors two or more colors to interpolate across
     * @param count   the total number of segments
     * @param index   the zero-based index of the segment to color
     *
     * @return the interpolated color for the segment
     */
    public static Color interpolatedColor(final Color[] anchors, final int count, final int index) {
        if ((count <= 1) || (anchors.length == 1)) {
            return anchors[0];
        }

        final float fraction = (float) index / (count - 1);
        final float scaled = fraction * (anchors.length - 1);
        final int lower = (int) Math.floor(scaled);
        final int upper = Math.min(lower + 1, anchors.length - 1);
        return interpolate(anchors[lower], anchors[upper], scaled - lower);
    }

    private static Color interpolate(final Color from, final Color to, final float t) {
        final int red = Math.round(from.getRed() + (to.getRed() - from.getRed()) * t);
        final int green = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * t);
        final int blue = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * t);
        return new Color(red, green, blue);
    }
}
