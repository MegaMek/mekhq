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

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.List;
import java.util.ResourceBundle;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import megamek.client.ui.util.UIUtil;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.enums.AtBMoraleLevel;
import mekhq.gui.baseComponents.SegmentedBar;

/**
 * A compact, segmented gauge that visualizes enemy {@link AtBMoraleLevel}.
 *
 * <p>
 * The bar has one segment per possible morale level, ordered from the morale scale's minimum to its maximum. The gauge
 * reads like the enemy's morale meter: the more segments that are lit, the higher the enemy's morale. The colors are
 * presented from the <em>player's</em> perspective: low enemy morale (a routed enemy) is green because it is favourable
 * for the player, climbing through orange and into red at the highest morale (a dangerous, fully-committed enemy).
 * Hovering a segment shows the name and description of that morale level.
 * </p>
 *
 * <p>
 * This wraps a private {@link SegmentedBar} (composition rather than inheritance) so that only the morale-specific API
 * is exposed; callers cannot reconfigure the underlying segments and break the "one segment per morale level"
 * invariant.
 * </p>
 *
 * @author The MegaMek Team
 */
public class MoraleBar extends JPanel {
    /**
     * Anchor colors for the morale gradient, ordered from the lowest enemy morale to the highest. Low enemy morale is
     * green (favourable for the player) and high enemy morale is red (dangerous for the player), so the gauge reads as
     * an enemy-strength meter from the player's perspective: a short green bar is good news, a long red bar is bad. The
     * colors are deliberately deep and well separated so that adjacent segments remain easy to tell apart.
     */
    private static final Color[] MORALE_GRADIENT = {
            new Color(0x12, 0x7C, 0x1E), // deep green - lowest enemy morale, most favourable for the player
            new Color(0x36, 0xB3, 0x2B), // green
            new Color(0x8C, 0xC6, 0x1A), // lime
            new Color(0xE8, 0xC4, 0x0A), // gold
            new Color(0xF2, 0x86, 0x00), // orange
            new Color(0xD2, 0x44, 0x10), // red-orange
            new Color(0xA8, 0x12, 0x12) // deep red - highest enemy morale, most dangerous for the player
    };

    /**
     * The wrapped gauge. Kept private so the segment-level API is not exposed to callers.
     */
    private final SegmentedBar bar = new SegmentedBar();

    /**
     * Creates a morale bar for the given morale level.
     *
     * @param moraleLevel the enemy morale level to display; may be {@code null}, in which case nothing is painted
     */
    public MoraleBar(final @Nullable AtBMoraleLevel moraleLevel) {
        this(moraleLevel, (moraleLevel == null) ? null : moraleLevel.toString());
    }

    /**
     * Creates a morale bar for the given morale level with a custom label drawn beneath the active segment.
     *
     * @param moraleLevel the enemy morale level to display; may be {@code null}, in which case nothing is painted
     * @param labelText   the text to show beneath the current level (for example a contract-specific name such as
     *                    "Peaceful"), or {@code null} to use the morale level's own name
     */
    public MoraleBar(final @Nullable AtBMoraleLevel moraleLevel, final @Nullable String labelText) {
        super(new BorderLayout());
        setOpaque(false);
        add(bar, BorderLayout.CENTER);
        setMoraleLevel(moraleLevel, labelText);
    }

    /**
     * Updates the displayed morale level and repaints the bar. The label beneath the active segment defaults to the
     * morale level's own name.
     *
     * @param moraleLevel the new enemy morale level to display, or {@code null} to clear the bar
     */
    public void setMoraleLevel(final @Nullable AtBMoraleLevel moraleLevel) {
        setMoraleLevel(moraleLevel, (moraleLevel == null) ? null : moraleLevel.toString());
    }

    /**
     * Updates the displayed morale level and the label drawn beneath the active segment, then repaints the bar.
     *
     * @param moraleLevel the new enemy morale level to display, or {@code null} to clear the bar
     * @param labelText   the text to show beneath the current level, or {@code null} for no label
     */
    public void setMoraleLevel(final @Nullable AtBMoraleLevel moraleLevel, final @Nullable String labelText) {
        if (moraleLevel == null) {
            bar.setSegments(List.of());
            bar.setFilledCount(0);
            bar.setActiveLabel(null);
            return;
        }

        final AtBMoraleLevel[] levels = AtBMoraleLevel.values();
        final String[] tooltips = new String[levels.length];
        for (int i = 0; i < levels.length; i++) {
            tooltips[i] = wordWrap(levels[i] + " \u2014 " + levels[i].getToolTipText());
        }

        bar.setSegments(SegmentedBar.gradientSegments(MORALE_GRADIENT, tooltips));
        bar.setFilledCount(moraleLevel.getLevel() - AtBMoraleLevel.MINIMUM_MORALE_LEVEL + 1);
        bar.setActiveLabel(labelText);
    }

    /**
     * Forwards the tooltip to the wrapped gauge so that the area around the segments (gaps and the label) shows this
     * fallback tooltip, while individual segments keep their own per-level tooltips.
     *
     * @param text the tooltip text, or {@code null} for none
     */
    @Override
    public void setToolTipText(final @Nullable String text) {
        bar.setToolTipText(text);
    }

    /**
     * Builds a self-contained panel wrapping a {@link MoraleBar}, suitable for embedding in dialogs such as the
     * immersive "Morale Update" notification. The bar is given generous horizontal padding so it reads as a centered
     * gauge rather than spanning the full dialog width. The label beneath the active segment honours the contract's
     * special "Peaceful" wording for routed garrison/retainer contracts, matching the briefing-room contract panel.
     *
     * @param contract the contract whose enemy morale should be displayed
     *
     * @return a transparent panel containing the configured morale bar
     */
    public static JPanel createDialogPanel(final AtBContract contract) {
        final JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        final int horizontalPadding = UIUtil.scaleForGUI(40);
        final int verticalPadding = UIUtil.scaleForGUI(6);
        panel.setBorder(BorderFactory.createEmptyBorder(verticalPadding, horizontalPadding, verticalPadding,
                horizontalPadding));

        final MoraleDisplay display = getMoraleDisplay(contract);
        final MoraleBar moraleBar = new MoraleBar(contract.getMoraleLevel(), display.label());
        moraleBar.setToolTipText(wordWrap(display.tooltip()));
        panel.add(moraleBar, BorderLayout.CENTER);
        return panel;
    }

    /**
     * The display text and tooltip used to describe a contract's enemy morale.
     *
     * @param label   the short morale name to show (e.g. a morale level name, or "Peaceful")
     * @param tooltip the descriptive tooltip for that morale state
     */
    public record MoraleDisplay(String label, String tooltip) {
    }

    /**
     * Computes the morale label and tooltip for a contract, applying the special "Peaceful" wording used for routed
     * garrison-duty and retainer contracts. This is the single source of truth shared by the briefing-room contract
     * panel and the morale dialog so the two never diverge.
     *
     * @param contract the contract whose enemy morale should be described
     *
     * @return the label and tooltip to display
     */
    public static MoraleDisplay getMoraleDisplay(final AtBContract contract) {
        final AtBMoraleLevel level = contract.getMoraleLevel();
        if ((contract.getContractType().isGarrisonDuty() || contract.getContractType().isRetainer()) &&
                level.isRouted()) {
            final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.ContractViewPanel",
                    MekHQ.getMHQOptions().getLocale());
            return new MoraleDisplay(resources.getString("txtGarrisonMoraleRouted.text"),
                    resources.getString("txtGarrisonMoraleRouted.tooltip"));
        }
        return new MoraleDisplay(level.toString(), level.getToolTipText());
    }
}

