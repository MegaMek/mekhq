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
 * The gauge reads from the player's perspective: a low or negative score is red, climbing through gold toward the
 * target. When the contract can be ended early, the gradient resolves to green at the target and the goal marker is
 * emphasised, signalling that reaching it lets the player declare victory. When the contract cannot be ended early, the
 * gradient deliberately stops short of green and the goal marker is muted: the score still climbs and still counts
 * toward the contract's final outcome, but reaching the target is a milestone rather than an exit.
 * </p>
 *
 * <p>
 * Because the score is unbounded and may be negative or exceed the requirement, values outside the {@code 0..required}
 * range are shown in flat darker overshoot caps at each end while the exact figures remain available as text and in the
 * tooltip.
 * </p>
 *
 * @author The MegaMek Team
 */
public class ContractScoreBar extends JPanel {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractViewPanel";

    /** Deep red for a low or negative score; shared with {@link MoraleBar}'s palette for visual consistency. */
    private static final Color DEEP_RED = new Color(0xA8, 0x12, 0x12);
    /** Gold for the mid range and for the upper end when the contract cannot be ended early. */
    private static final Color GOLD = new Color(0xE8, 0xC4, 0x0A);
    /** Deep green for a score at or beyond the target when the contract can be ended early. */
    private static final Color DEEP_GREEN = new Color(0x12, 0x7C, 0x1E);
    /** Alpha applied to the goal marker when the contract cannot be ended early, muting it to a reference line. */
    private static final int MUTED_ALPHA = 120;

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
        if (canEndEarly) {
            bar.setGradient(new Color[] { DEEP_RED, GOLD, DEEP_GREEN });
            bar.setOvershootCaps(DEEP_RED.darker(), DEEP_GREEN.darker());
        } else {
            // No green "finish line" cue: progress climbs from red to gold but never resolves to green, because
            // reaching the target does not let the player leave the contract.
            bar.setGradient(new Color[] { DEEP_RED, GOLD });
            bar.setOvershootCaps(DEEP_RED.darker(), GOLD.darker());
        }

        final Color markerColor = markerColor();
        final Color goalColor = canEndEarly ? markerColor : muted(markerColor);
        final List<Marker> markers = new ArrayList<>(2);
        // Goal threshold first, then the current-score marker so the latter is painted on top where they overlap.
        markers.add(new Marker(requiredScore, null, goalColor, MarkerStyle.TICK, false));
        markers.add(new Marker(currentScore, null, markerColor, MarkerStyle.SOLID, false));
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
