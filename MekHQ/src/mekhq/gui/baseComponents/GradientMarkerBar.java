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
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.UIManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;

/**
 * A compact, reusable gauge that draws a continuous horizontal track and places one or more labeled markers on it.
 *
 * <p>
 * Unlike {@link SegmentedBar}, which fills a fixed number of discrete blocks, this component represents an
 * <em>unbounded</em>, possibly signed value as a position along a track. A smooth color gradient is painted across a
 * caller-defined value range (the "gradient range"); values that fall outside that range are represented by optional
 * flat-colored "overshoot caps" at each end. This makes the control suitable for quantities that have a meaningful
 * working range but no hard minimum or maximum (for example a contract score that climbs toward a victory threshold but
 * may go negative or overshoot).
 * </p>
 *
 * <p>
 * Markers are drawn as thin vertical handles that poke slightly above and below the track so they stay visible against
 * any gradient color behind them. Each {@link Marker} carries a value, an optional text label, a color, and a
 * {@link MarkerStyle}. A marker whose value lies outside the gradient range is pinned to the center of the
 * corresponding overshoot cap (or clamped to the track end when that cap is disabled), so the marker never leaves the
 * track; callers are expected to show the exact value as text elsewhere.
 * </p>
 *
 * <p>
 * The component is value-agnostic: callers describe what to show by setting the gradient range, the gradient colors,
 * the optional caps, and the markers. This keeps the same control usable for very different concepts (contract score,
 * faction standing, ...).
 * </p>
 *
 * @author The MegaMek Team
 */
public class GradientMarkerBar extends JComponent {
    /**
     * How a {@link Marker} is drawn.
     *
     * <ul>
     *     <li>{@link #SOLID} - a wider, filled handle used for the primary value (for example the current score).</li>
     *     <li>{@link #TICK} - a thinner reference line used for a secondary value (for example a goal threshold).</li>
     * </ul>
     */
    public enum MarkerStyle {
        SOLID, TICK
    }

    /**
     * A single labeled point on the track.
     *
     * @param value      the value the marker represents, in the same units as the gradient range; values outside the
     *                   gradient range are pinned to the nearest overshoot cap
     * @param label      an optional short label drawn next to the marker, or {@code null} for no label
     * @param color      the color used to draw the marker handle
     * @param style      whether the marker is drawn as a {@link MarkerStyle#SOLID} handle or a {@link MarkerStyle#TICK}
     *                   reference line
     * @param labelAbove {@code true} to draw the label above the track, {@code false} to draw it below; ignored when
     *                   {@code label} is {@code null} or blank
     */
    public record Marker(double value, @Nullable String label, @Nonnull Color color, @Nonnull MarkerStyle style,
                         boolean labelAbove) {
    }

    private static final int PADDING = 6;
    private static final int TRACK_HEIGHT = 8;
    /** How far a marker handle extends beyond the track, top and bottom, so it stays visible over the gradient. */
    private static final int MARKER_OVERHANG = 4;
    /** Fixed pixel width of each overshoot cap, when enabled. */
    private static final int CAP_WIDTH = 12;
    private static final int SOLID_MARKER_WIDTH = 4;
    private static final int TICK_MARKER_WIDTH = 2;
    private static final int TRACK_ARC = 6;
    /** Vertical gap between a marker label and the track. */
    private static final int LABEL_GAP = 2;
    /** Alpha applied to the contrasting outline drawn around markers. */
    private static final int OUTLINE_ALPHA = 160;
    /** Luminance threshold (0-255) above which a marker is considered "light" and gets a dark outline. */
    private static final double LIGHT_LUMINANCE_THRESHOLD = 140.0;

    private double gradientStart = 0.0;
    private double gradientEnd = 1.0;
    private Color[] gradient = {};
    private @Nullable Color belowCapColor;
    private @Nullable Color aboveCapColor;
    private List<Marker> markers = List.of();

    /**
     * Creates an empty bar. Configure it with {@link #setGradientRange(double, double)}, {@link #setGradient(Color[])},
     * {@link #setOvershootCaps(Color, Color)}, and {@link #setMarkers(List)}.
     */
    public GradientMarkerBar() {
        setOpaque(false);
    }

    /**
     * Sets the value range spanned by the color gradient. Values at or below {@code start} map to the left end of the
     * gradient and values at or above {@code end} map to the right end; values outside the range are represented by the
     * overshoot caps (see {@link #setOvershootCaps(Color, Color)}).
     *
     * @param start the value mapped to the left end of the gradient
     * @param end   the value mapped to the right end of the gradient; should be greater than {@code start} (a
     *              non-positive span disables value-to-position mapping and pins markers to the left)
     */
    public void setGradientRange(final double start, final double end) {
        this.gradientStart = start;
        this.gradientEnd = end;
        repaint();
    }

    /**
     * Sets the colors interpolated across the gradient range, left to right. A {@code null} or empty array draws no
     * gradient; a single color fills the gradient range solidly.
     *
     * @param anchors the gradient colors from left to right, or {@code null} for none
     */
    public void setGradient(final @Nullable Color[] anchors) {
        this.gradient = (anchors == null) ? new Color[] {} : anchors.clone();
        repaint();
    }

    /**
     * Sets the flat fill colors for the overshoot caps drawn beyond each end of the gradient range. A {@code null}
     * color disables that cap, in which case markers beyond that end clamp to the track end instead of pinning to a
     * cap.
     *
     * @param belowColor the fill for the cap below the gradient range, or {@code null} for none
     * @param aboveColor the fill for the cap above the gradient range, or {@code null} for none
     */
    public void setOvershootCaps(final @Nullable Color belowColor, final @Nullable Color aboveColor) {
        this.belowCapColor = belowColor;
        this.aboveCapColor = aboveColor;
        revalidate();
        repaint();
    }

    /**
     * Sets the markers to draw on the track. {@code null} is treated as an empty list.
     *
     * @param markers the markers to draw, or {@code null} for none
     */
    public void setMarkers(final @Nullable List<Marker> markers) {
        this.markers = (markers == null) ? List.of() : List.copyOf(markers);
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        final Dimension base = UIUtil.scaleForGUI(140, TRACK_HEIGHT + 2 * MARKER_OVERHANG);
        return new Dimension(base.width, base.height + labelAreaHeight(true) + labelAreaHeight(false));
    }

    @Override
    public Dimension getMinimumSize() {
        final Dimension base = UIUtil.scaleForGUI(84, TRACK_HEIGHT + 2 * MARKER_OVERHANG);
        return new Dimension(base.width, base.height + labelAreaHeight(true) + labelAreaHeight(false));
    }

    private int padding() {
        return Math.max(UIUtil.scaleForGUI(PADDING), 1);
    }

    private int capWidth() {
        return Math.max(UIUtil.scaleForGUI(CAP_WIDTH), 1);
    }

    private int trackHeight() {
        return Math.max(UIUtil.scaleForGUI(TRACK_HEIGHT), 2);
    }

    private int markerOverhang() {
        return Math.max(UIUtil.scaleForGUI(MARKER_OVERHANG), 1);
    }

    private boolean hasBelowCap() {
        return belowCapColor != null;
    }

    private boolean hasAboveCap() {
        return aboveCapColor != null;
    }

    /**
     * @param above {@code true} for the area above the track, {@code false} for below
     *
     * @return the vertical space reserved for marker labels on the requested side, or {@code 0} if no marker has a
     *       label on that side
     */
    private int labelAreaHeight(final boolean above) {
        boolean any = false;
        for (final Marker marker : markers) {
            if ((marker.label() != null) && !marker.label().isBlank() && (marker.labelAbove() == above)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return 0;
        }
        return Math.max(UIUtil.scaleForGUI(LABEL_GAP), 1) + getFontMetrics(getFont()).getHeight();
    }

    /** @return the left edge of the gradient zone in pixels (after any below-cap). */
    private int gradientLeft() {
        return padding() + (hasBelowCap() ? capWidth() : 0);
    }

    /** @return the right edge of the gradient zone in pixels (before any above-cap). */
    private int gradientRight() {
        return Math.max(getWidth() - padding() - (hasAboveCap() ? capWidth() : 0), gradientLeft() + 1);
    }

    /**
     * Maps a value to its x-coordinate on the track. Values within the gradient range (inclusive of both ends) map
     * linearly across the gradient zone; values strictly below the range pin to the center of the below-cap (or the
     * gradient's left edge when that cap is disabled); values strictly above the range pin to the center of the
     * above-cap (or the gradient's right edge when that cap is disabled).
     *
     * @param value the value to map
     *
     * @return the x-coordinate, in pixels, for the value
     */
    int valueToX(final double value) {
        if (getWidth() <= 0) {
            return 0;
        }
        final int gradLeft = gradientLeft();
        final int gradRight = gradientRight();
        final double range = gradientEnd - gradientStart;
        if (range <= 0) {
            return gradLeft;
        }
        if (value < gradientStart) {
            return hasBelowCap() ? padding() + capWidth() / 2 : gradLeft;
        }
        if (value > gradientEnd) {
            return hasAboveCap() ? getWidth() - padding() - capWidth() / 2 : gradRight;
        }
        final double fraction = (value - gradientStart) / range;
        return (int) Math.round(gradLeft + fraction * (gradRight - gradLeft));
    }

    @Override
    protected void paintComponent(final Graphics g) {
        super.paintComponent(g);
        if (getWidth() <= 0) {
            return;
        }

        final Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            final int trackHeight = trackHeight();
            final int overhang = markerOverhang();
            final int aboveLabel = labelAreaHeight(true);
            final int trackTop = aboveLabel + overhang;
            final int trackLeft = padding();
            final int trackRight = getWidth() - padding();
            final int trackWidth = Math.max(trackRight - trackLeft, 1);
            final int arc = Math.max(UIUtil.scaleForGUI(TRACK_ARC), 2);

            paintTrack(g2, trackLeft, trackTop, trackWidth, trackHeight, arc);
            paintMarkers(g2, trackTop, trackHeight, overhang);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Paints the track: the optional overshoot caps and the gradient between them, clipped to a single rounded
     * rectangle so the outer corners are rounded.
     */
    private void paintTrack(final @Nonnull Graphics2D g2, final int trackLeft, final int trackTop, final int trackWidth,
          final int trackHeight, final int arc) {
        final Shape clip = new RoundRectangle2D.Double(trackLeft, trackTop, trackWidth, trackHeight, arc, arc);
        final Shape oldClip = g2.getClip();
        g2.clip(clip);

        final int gradLeft = gradientLeft();
        final int gradRight = gradientRight();

        // Background fill so the track is visible even before/without a gradient.
        g2.setColor(emptyTrackColor());
        g2.fillRect(trackLeft, trackTop, trackWidth, trackHeight);

        if (hasBelowCap()) {
            g2.setColor(belowCapColor);
            g2.fillRect(trackLeft, trackTop, gradLeft - trackLeft, trackHeight);
        }
        if (hasAboveCap()) {
            g2.setColor(aboveCapColor);
            g2.fillRect(gradRight, trackTop, (trackLeft + trackWidth) - gradRight, trackHeight);
        }

        paintGradient(g2, gradLeft, trackTop, gradRight - gradLeft, trackHeight);

        g2.setClip(oldClip);
    }

    /**
     * Paints the color gradient across the gradient zone. With no anchors nothing is drawn; with a single anchor the
     * zone is filled solidly; with two or more anchors a {@link LinearGradientPaint} is used.
     */
    private void paintGradient(final @Nonnull Graphics2D g2, final int x, final int y, final int width,
          final int height) {
        if ((gradient.length == 0) || (width <= 0)) {
            return;
        }
        if (gradient.length == 1) {
            g2.setColor(gradient[0]);
            g2.fillRect(x, y, width, height);
            return;
        }
        final float[] fractions = gradientFractions(gradient.length);
        final LinearGradientPaint paint = new LinearGradientPaint(x, y, x + width, y, fractions, gradient);
        g2.setPaint(paint);
        g2.fillRect(x, y, width, height);
    }

    /**
     * Paints every marker as a vertical handle (with a contrasting outline) plus its optional label.
     */
    private void paintMarkers(final @Nonnull Graphics2D g2, final int trackTop, final int trackHeight,
          final int overhang) {
        final int markerTop = trackTop - overhang;
        final int markerHeight = trackHeight + 2 * overhang;
        for (final Marker marker : markers) {
            final int width = (marker.style() == MarkerStyle.SOLID) ? Math.max(UIUtil.scaleForGUI(SOLID_MARKER_WIDTH), 2)
                  : Math.max(UIUtil.scaleForGUI(TICK_MARKER_WIDTH), 1);
            final int centerX = valueToX(marker.value());
            int x = centerX - width / 2;
            // Keep the handle fully within the component bounds.
            x = Math.max(0, Math.min(x, getWidth() - width));
            final int arc = Math.max(width, 2);

            // Contrasting outline for visibility against the gradient.
            g2.setColor(contrastOutline(marker.color()));
            g2.fillRoundRect(x - 1, markerTop - 1, width + 2, markerHeight + 2, arc, arc);
            g2.setColor(marker.color());
            g2.fillRoundRect(x, markerTop, width, markerHeight, arc, arc);

            paintMarkerLabel(g2, marker, centerX, markerTop, markerHeight);
        }
    }

    /**
     * Draws a marker's label, if any, centered on the marker and clamped within the component bounds.
     */
    private void paintMarkerLabel(final @Nonnull Graphics2D g2, final @Nonnull Marker marker, final int centerX,
          final int markerTop, final int markerHeight) {
        final String label = marker.label();
        if ((label == null) || label.isBlank()) {
            return;
        }
        g2.setColor(marker.color());
        final FontMetrics metrics = g2.getFontMetrics(getFont());
        final int textWidth = metrics.stringWidth(label);
        int textX = centerX - (textWidth / 2);
        textX = Math.max(0, Math.min(textX, getWidth() - textWidth));
        final int gap = Math.max(UIUtil.scaleForGUI(LABEL_GAP), 1);
        final int textY = marker.labelAbove() ? markerTop - gap - metrics.getDescent()
              : markerTop + markerHeight + gap + metrics.getAscent();
        g2.drawString(label, textX, textY);
    }

    /**
     * @return evenly spaced gradient fractions in {@code [0, 1]} for the given number of anchor colors
     */
    static float[] gradientFractions(final int anchorCount) {
        final float[] fractions = new float[anchorCount];
        for (int i = 0; i < anchorCount; i++) {
            fractions[i] = (float) i / (anchorCount - 1);
        }
        return fractions;
    }

    /**
     * @return a contrasting outline color (dark for light markers, light for dark markers) so markers read against the
     *       gradient
     */
    private static @Nonnull Color contrastOutline(final @Nonnull Color color) {
        final double luminance = 0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue();
        return (luminance > LIGHT_LUMINANCE_THRESHOLD) ? new Color(0, 0, 0, OUTLINE_ALPHA)
              : new Color(255, 255, 255, OUTLINE_ALPHA);
    }

    /**
     * @return a theme-aware, faint fill used for the bare track so it reads on both light and dark themes
     */
    private static @Nonnull Color emptyTrackColor() {
        final Color foreground = UIManager.getColor("Label.foreground");
        final Color base = (foreground == null) ? Color.GRAY : foreground;
        return new Color(base.getRed(), base.getGreen(), base.getBlue(), 28);
    }
}
