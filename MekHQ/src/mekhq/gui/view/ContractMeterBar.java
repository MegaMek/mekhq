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
package mekhq.gui.view;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;
import mekhq.gui.baseComponents.GradientMarkerBar;
import mekhq.gui.baseComponents.GradientMarkerBar.Marker;
import mekhq.gui.baseComponents.GradientMarkerBar.MarkerStyle;

/**
 * A continuous gauge that visualizes a contract metric as a current value placed on a track.
 *
 * <p>
 * The component supports two visual languages. <b>Value meters</b> (victory points, support points, salvage) read from
 * the player's perspective: a low value is red, climbing through gold to green as it approaches an upper reference, so
 * "more is better" is conveyed by hue. A <b>progress</b> gauge (the contract timeline) instead uses a single neutral
 * track, because time has no good or bad direction; a value-style red-to-green gradient there would imply a judgement
 * that does not exist. In both cases the current value is a bold, accent-colored marker so the figure the player is
 * tracking stands out, and the figures themselves are carried in the title so the bar reads as the primary display.
 * </p>
 *
 * <p>
 * Because a value may be negative or exceed its reference, the track stretches to keep every marker in view and
 * proportionally placed, filling any stretched-in regions of a value meter with flat darker colors (dark red below
 * zero, dark green beyond the reference). Those regions appear only when the value is actually outside the
 * {@code 0..reference} range, so the bar stays uncluttered in the common case.
 * </p>
 *
 * <p>
 * Instances are created through the {@link #victoryPoints(int, int, boolean)}, {@link #supportPoints(int, int)},
 * {@link #salvage(int, int)}, and {@link #timeline(LocalDate, LocalDate, LocalDate, String, String, String)} factory
 * methods, which supply the appropriate title, tooltip, and styling for each metric.
 * </p>
 *
 * @author The MegaMek Team
 */
public class ContractMeterBar extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractViewPanel";

    /** Deep red for a low or negative score; shared with {@link MoraleBar}'s palette for visual consistency. */
    private static final Color DEEP_RED = new Color(0xA8, 0x12, 0x12);
    /** Gold for the mid range of the gradient. */
    private static final Color GOLD = new Color(0xE8, 0xC4, 0x0A);
    /** Green for a score at the target; the gradient resolves here. */
    private static final Color GREEN = new Color(0x36, 0xB3, 0x2B);
    /** Cool azure accent for the current-value marker, chosen to stand out against the warm red-gold-green gradient. */
    private static final Color CURRENT_MARKER_COLOR = new Color(0x29, 0xB6, 0xF6);
    /** Neutral track for the progress (timeline) gauge: a muted slate that reads as "progress, not performance". */
    private static final Color NEUTRAL_TRACK = new Color(0x6E, 0x76, 0x7E);

    /** The wrapped gauge. Kept private so the marker-level API is not exposed to callers. */
    private final GradientMarkerBar bar = new GradientMarkerBar();

    /**
     * Creates a contract meter. Private; use the {@code victoryPoints}, {@code supportPoints}, {@code salvage}, and
     * {@code timeline} factory methods, which supply the appropriate title, tooltip, gradient, and markers.
     *
     * @param title         the title shown, centered, directly above the bar
     * @param gradientStart the value mapped to the left end of the gradient
     * @param gradientEnd   the value mapped to the right end of the gradient
     * @param gradient      the gradient colors, left to right (a single color fills the track neutrally)
     * @param belowColor    the flat fill for values below the gradient range, or {@code null} for none
     * @param aboveColor    the flat fill for values above the gradient range, or {@code null} for none
     * @param markers       the markers to draw on the track
     * @param tooltip       the tooltip shown when hovering anywhere on the bar
     */
    private ContractMeterBar(final String title, final double gradientStart, final double gradientEnd,
          final @Nonnull Color[] gradient, final @Nullable Color belowColor, final @Nullable Color aboveColor,
          final @Nonnull List<Marker> markers, final String tooltip) {
        // The bar reserves a little headroom above its track for the current-value label; a small negative vertical
        // gap pulls the title down into that headroom so it sits close to the value without crowding it.
        super(new BorderLayout(0, -UIUtil.scaleForGUI(2)));
        setOpaque(false);
        add(buildTitle(title), BorderLayout.NORTH);
        add(bar, BorderLayout.CENTER);

        bar.setGradientRange(gradientStart, gradientEnd);
        bar.setGradient(gradient);
        bar.setOutOfGradientColors(belowColor, aboveColor);
        bar.setMarkers(markers);

        setToolTipText(wordWrap(tooltip));
    }

    /**
     * Builds a value meter: a red-to-green gradient spanning {@code 0..referenceValue}, with neutral {@code 0} and
     * reference ticks and a bold, accent-colored current-value marker. The track stretches (with flat dark-red /
     * dark-green fills) to keep a negative or overshooting current value in view.
     *
     * @param title          the title shown above the bar (typically carrying the figures)
     * @param currentValue   the current value, drawn as the bold accent marker (may be negative)
     * @param referenceValue the upper reference value (the green end and a reference tick)
     * @param tooltip        the tooltip shown when hovering anywhere on the bar
     *
     * @return the configured value meter
     */
    private static @Nonnull ContractMeterBar valueMeter(final String title, final int currentValue,
          final int referenceValue, final String tooltip) {
        final Color markerColor = markerColor();
        final List<Marker> markers = new ArrayList<>(3);
        // The zero baseline and the reference total are neutral thin reference ticks labelled below the track; the
        // current value is a bold, accent-colored solid marker labelled above it. Splitting the labels across the two
        // sides keeps the current value legible even when it sits right next to the reference (for example a salvage
        // value of 71% against a 70% maximum). Markers are painted in list order, so the current-value marker is added
        // last to sit on top where markers coincide.
        markers.add(new Marker(referenceValue, Integer.toString(referenceValue), markerColor, MarkerStyle.TICK,
              false));
        markers.add(new Marker(0, "0", markerColor, MarkerStyle.TICK, false));
        markers.add(new Marker(currentValue, Integer.toString(currentValue), CURRENT_MARKER_COLOR, MarkerStyle.SOLID,
              true, true));
        return new ContractMeterBar(title, 0, referenceValue, new Color[] { DEEP_RED, GOLD, GREEN },
              DEEP_RED.darker(), GREEN.darker(), markers, tooltip);
    }

    /**
     * Creates a gauge of a contract's accumulated victory points against the score required to declare victory.
     *
     * @param currentScore  the contract's current accumulated victory points (may be negative)
     * @param requiredScore the victory points required to declare victory; should be positive (callers should fall
     *                      back to a plain-text display when the requirement is not a positive number)
     * @param canEndEarly   {@code true} if reaching {@code requiredScore} lets the player declare victory early;
     *                      {@code false} if the contract must run its full term, in which case the title carries a
     *                      concise "full term" cue
     *
     * @return the configured gauge
     */
    public static @Nonnull ContractMeterBar victoryPoints(final int currentScore, final int requiredScore,
          final boolean canEndEarly) {
        final String tooltip = getFormattedTextAt(RESOURCE_BUNDLE,
              canEndEarly ? "contractScoreBar.tooltip.canEndEarly" : "contractScoreBar.tooltip.cannotEndEarly",
              currentScore, requiredScore);
        final String titleKey = canEndEarly ? "contractScoreBar.title.text"
              : "contractScoreBar.title.cannotEndEarly.text";
        return valueMeter(getTextAt(RESOURCE_BUNDLE, titleKey), currentScore, requiredScore, tooltip);
    }

    /**
     * Creates a gauge of a contract's current support points against the reserve it can negotiate up to.
     *
     * @param currentPoints the contract's current support points
     * @param maximumPoints the support-point reserve the contract can negotiate up to; should be positive (callers
     *                      should fall back to a plain-text display when it is not)
     *
     * @return the configured gauge
     */
    public static @Nonnull ContractMeterBar supportPoints(final int currentPoints, final int maximumPoints) {
        final String tooltip = getFormattedTextAt(RESOURCE_BUNDLE, "contractSupportPointsBar.tooltip", currentPoints,
              maximumPoints);
        return valueMeter(getTextAt(RESOURCE_BUNDLE, "contractSupportPointsBar.title.text"), currentPoints,
              maximumPoints, tooltip);
    }

    /**
     * Creates a gauge of the salvage percentage claimed so far against the negotiated maximum.
     *
     * @param currentPercent the salvage percentage claimed so far
     * @param maximumPercent the negotiated maximum salvage percentage; should be positive (callers should fall back to
     *                       a plain-text display for the no-salvage and salvage-exchange cases)
     *
     * @return the configured gauge
     */
    public static @Nonnull ContractMeterBar salvage(final int currentPercent, final int maximumPercent) {
        final String tooltip = getFormattedTextAt(RESOURCE_BUNDLE, "contractSalvageBar.tooltip", currentPercent,
              maximumPercent);
        return valueMeter(getTextAt(RESOURCE_BUNDLE, "contractSalvageBar.title.text"), currentPercent, maximumPercent,
              tooltip);
    }

    /**
     * Creates a neutral progress gauge of how far the current date has advanced between a contract's start and end.
     * Unlike the value meters, the track is a single neutral color: time has no good or bad direction, so a red-to-green
     * gradient would imply a judgement that does not exist. The start and end are unlabeled ticks (the dates are carried
     * in the title), and the current date is the bold accent marker.
     *
     * @param startDate   the contract start date (left end of the track)
     * @param endDate     the contract end date (right end of the track)
     * @param currentDate the current date, drawn as the bold accent marker
     * @param startLabel  the formatted start date, shown in the title
     * @param endLabel    the formatted end date, shown in the title
     * @param currentLabel the formatted current date, shown beneath the current-date marker
     *
     * @return the configured gauge
     */
    public static @Nonnull ContractMeterBar timeline(final @Nonnull LocalDate startDate,
          final @Nonnull LocalDate endDate, final @Nonnull LocalDate currentDate, final String startLabel,
          final String endLabel, final String currentLabel) {
        final double start = startDate.toEpochDay();
        final double end = endDate.toEpochDay();
        final double current = currentDate.toEpochDay();
        final List<Marker> markers = new ArrayList<>(1);
        // The track itself runs from start to end (the dates are carried in the title), so the only marker is the bold
        // "today" marker that slides along it; no separate start or end ticks are drawn.
        markers.add(new Marker(current, currentLabel, CURRENT_MARKER_COLOR, MarkerStyle.SOLID, true, true));
        final String title = getFormattedTextAt(RESOURCE_BUNDLE, "contractTimelineBar.title.text", startLabel,
              endLabel);
        final String tooltip = getFormattedTextAt(RESOURCE_BUNDLE, "contractTimelineBar.tooltip", startLabel, endLabel,
              currentLabel);
        return new ContractMeterBar(title, start, end, new Color[] { NEUTRAL_TRACK }, null, null, markers, tooltip);
    }

    /**
     * Forwards the tooltip to the wrapped gauge so hovering anywhere on the bar shows it.
     *
     * @param text the tooltip text, or {@code null} for none
     */
    @Override
    public void setToolTipText(final @Nullable String text) {
        super.setToolTipText(text);
        bar.setToolTipText(text);
    }

    /**
     * @return a theme-aware color for the markers, derived from the label foreground so they read on light and dark
     *       themes
     */
    private static @Nonnull Color markerColor() {
        final Color foreground = UIManager.getColor("Label.foreground");
        return (foreground == null) ? Color.WHITE : foreground;
    }

    /**
     * Builds the centered title drawn directly above the bar so the title and gauge read as a single unit.
     *
     * @param title the title text (may be HTML)
     *
     * @return the title label
     */
    private static @Nonnull JLabel buildTitle(final String title) {
        return new JLabel(title, SwingConstants.CENTER);
    }
}
