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
import javax.swing.JPanel;
import javax.swing.UIManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.GradientMarkerBar;
import mekhq.gui.baseComponents.GradientMarkerBar.Marker;
import mekhq.gui.baseComponents.GradientMarkerBar.MarkerStyle;

/**
 * A continuous gauge that visualizes an AtB contract's accumulated victory points ("CVP") against the score required to
 * declare victory.
 *
 * <p>
 * The gauge reads from the player's perspective: a low or negative score is red, climbing through gold to green as it
 * approaches the required total. Because the score is unbounded and may be negative or exceed the requirement, values
 * outside the {@code 0..required} range are shown in flat darker overshoot caps (dark red below zero, dark green above
 * the target). Those caps are only drawn when the score is actually outside the range, so the bar stays uncluttered in
 * the common case. The exact figures remain available as marker labels and in the tooltip.
 * </p>
 *
 * <p>
 * Whether the player may declare victory early upon reaching the target is conveyed by the prominence of the goal marker
 * (muted when the contract cannot be ended early) and by the accompanying caption, rather than by the bar's colors.
 * </p>
 *
 * @author The MegaMek Team
 */
public class ContractScoreBar extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractViewPanel";

    /** Deep red for a low or negative score; shared with {@link MoraleBar}'s palette for visual consistency. */
    private static final Color DEEP_RED = new Color(0xA8, 0x12, 0x12);
    /** Gold for the mid range of the gradient. */
    private static final Color GOLD = new Color(0xE8, 0xC4, 0x0A);
    /** Green for a score at the target; the gradient resolves here. */
    private static final Color GREEN = new Color(0x36, 0xB3, 0x2B);
    /** Alpha applied to the goal marker when the contract cannot be ended early, muting it to a reference line. */
    private static final int MUTED_ALPHA = 160;

    /** The wrapped gauge. Kept private so the marker-level API is not exposed to callers. */
    private final GradientMarkerBar bar = new GradientMarkerBar();

    /**
     * Creates a contract-score gauge.
     *
     * @param currentScore  the contract's current accumulated victory points (may be negative)
     * @param requiredScore the victory points required to declare victory; should be positive (callers should fall
     *                      back to a plain-text display when the requirement is not a positive number)
     * @param canEndEarly   {@code true} if reaching {@code requiredScore} lets the player declare victory early;
     *                      {@code false} if the contract must run its full term
     */
    public ContractScoreBar(final int currentScore, final int requiredScore, final boolean canEndEarly) {
        super(new BorderLayout());
        setOpaque(false);
        add(bar, BorderLayout.CENTER);

        bar.setGradientRange(0, requiredScore);
        // Red climbs through gold to green as the score approaches the required total. The gradient reaches green
        // regardless of whether the contract can be ended early; that distinction is carried by the goal marker's
        // prominence and the accompanying "(No Early Contract End)" caption instead.
        bar.setGradient(new Color[] { DEEP_RED, GOLD, GREEN });
        // The track stretches to fit the current score, keeping it proportional even when out of range. The flat
        // out-of-gradient fills (dark red below zero, dark green beyond the target) therefore appear only when the
        // score actually falls outside the 0..required range, so the bar stays uncluttered in the common case.
        bar.setOutOfGradientColors(DEEP_RED.darker(), GREEN.darker());

        final Color markerColor = markerColor();
        final Color goalColor = canEndEarly ? markerColor : muted(markerColor);
        final List<Marker> markers = new ArrayList<>(2);
        // Goal threshold first, then the current-score marker so the latter is painted on top where they overlap. The
        // goal value is labelled above the track and the current value below, so both stay legible even when the two
        // markers sit close together (for example once the score reaches or overshoots the target).
        markers.add(new Marker(requiredScore, Integer.toString(requiredScore), goalColor, MarkerStyle.TICK, true));
        markers.add(new Marker(currentScore, Integer.toString(currentScore), markerColor, MarkerStyle.SOLID, false));
        bar.setMarkers(markers);

        setToolTipText(wordWrap(getFormattedTextAt(RESOURCE_BUNDLE,
              canEndEarly ? "contractScoreBar.tooltip.canEndEarly" : "contractScoreBar.tooltip.cannotEndEarly",
              currentScore, requiredScore)));
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
     * @return the given color with reduced opacity, used to de-emphasise the goal marker for contracts that cannot be
     *       ended early
     */
    private static @Nonnull Color muted(final @Nonnull Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), MUTED_ALPHA);
    }
}
