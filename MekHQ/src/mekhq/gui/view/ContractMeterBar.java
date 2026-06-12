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

import java.awt.BorderLayout;
import java.awt.Color;
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
 * A continuous gauge that visualizes a contract metric (such as victory points or support points) as a current value
 * measured against an upper reference value.
 *
 * <p>
 * The gauge reads from the player's perspective: a low or negative value is red, climbing through gold to green as it
 * approaches the reference total. It is the primary display for the value, so its markers carry the figures: the zero
 * baseline and the reference total are neutral reference ticks, while the current value is a thicker, accent-colored
 * marker so the figure the player is tracking stands out at a glance. Because the value is unbounded and may be
 * negative or exceed the reference, the track stretches to keep every marker in view and proportionally placed,
 * filling the stretched-in regions with flat darker colors (dark red below zero, dark green beyond the reference).
 * Those regions appear only when the value is actually outside the {@code 0..reference} range, so the bar stays
 * uncluttered in the common case.
 * </p>
 *
 * <p>
 * Instances are created through the {@link #victoryPoints(int, int, boolean)} and {@link #supportPoints(int, int)}
 * factory methods, which supply the appropriate title, tooltip, and styling for each metric. When reaching the
 * reference is not, by itself, an achievable goal (for example a contract that cannot be ended early), the reference
 * tick is muted; the explanation is carried by the bar's tooltip rather than by the bar's colors.
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

    /** The wrapped gauge. Kept private so the marker-level API is not exposed to callers. */
    private final GradientMarkerBar bar = new GradientMarkerBar();

    /**
     * Creates a contract meter. Private; use the {@link #victoryPoints(int, int, boolean)} and
     * {@link #supportPoints(int, int)} factory methods, which supply the appropriate title, tooltip, and styling.
     *
     * @param title          the title shown, centered, directly above the bar
     * @param currentValue   the current value, drawn as the prominent accent-colored marker (may be negative)
     * @param referenceValue the upper reference value (the green end of the gradient and a reference tick); should be
     *                       positive (callers fall back to a plain-text display when it is not)
     * @param tooltip        the tooltip shown when hovering anywhere on the bar
     */
    private ContractMeterBar(final String title, final int currentValue, final int referenceValue,
          final String tooltip) {
        // A small vertical gap separates the title from the bar so they read as a grouped unit without crowding.
        super(new BorderLayout(0, UIUtil.scaleForGUI(1)));
        setOpaque(false);
        add(buildTitle(title), BorderLayout.NORTH);
        add(bar, BorderLayout.CENTER);

        bar.setGradientRange(0, referenceValue);
        // Red climbs through gold to green as the value approaches the reference total.
        bar.setGradient(new Color[] { DEEP_RED, GOLD, GREEN });
        // The track stretches to fit the current value, keeping it proportional even when out of range. The flat
        // out-of-gradient fills (dark red below zero, dark green beyond the reference) therefore appear only when the
        // value actually falls outside the 0..reference range, so the bar stays uncluttered in the common case.
        bar.setOutOfGradientColors(DEEP_RED.darker(), GREEN.darker());

        final Color markerColor = markerColor();
        final List<Marker> markers = new ArrayList<>(3);
        // The bar is the primary display, so its markers carry the figures, all labelled below the track so the title
        // can sit flush above it. The zero baseline and the reference total are neutral thin reference ticks drawn in
        // the same color (a different color reads as a bug without explanation); the current value is a bold, thicker,
        // accent-colored solid marker, so the figure the player tracks stands out by weight, hue, and a bold label.
        // Markers are painted in list order, so the current-value marker is added last to sit on top where markers
        // coincide (for example a brand-new contract whose value is still zero).
        markers.add(new Marker(referenceValue, Integer.toString(referenceValue), markerColor, MarkerStyle.TICK,
              false));
        markers.add(new Marker(0, "0", markerColor, MarkerStyle.TICK, false));
        markers.add(new Marker(currentValue, Integer.toString(currentValue), CURRENT_MARKER_COLOR, MarkerStyle.SOLID,
              false, true));
        bar.setMarkers(markers);

        setToolTipText(wordWrap(tooltip));
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
        final String titleKey = canEndEarly ? "contractScoreBar.title.text" : "contractScoreBar.title.cannotEndEarly.text";
        return new ContractMeterBar(getFormattedTextAt(RESOURCE_BUNDLE, titleKey, currentScore, requiredScore),
              currentScore, requiredScore, tooltip);
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
        return new ContractMeterBar(getFormattedTextAt(RESOURCE_BUNDLE, "contractSupportPointsBar.title.text",
              currentPoints, maximumPoints), currentPoints, maximumPoints, tooltip);
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
