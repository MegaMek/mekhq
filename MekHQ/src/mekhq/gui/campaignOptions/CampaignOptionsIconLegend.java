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
 * of The Topps Company Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.campaignOptions;

import static megamek.client.ui.util.FontHandler.symbolIcon;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.UIUtil;

/**
 * Reference key explaining the marker icons that appear on Campaign Options labels (custom system, documented,
 * important, recommended, and the "added since" version badges).
 *
 * <p>The markers are drawn as real Swing icon-and-text rows rather than an HTML glyph table, so the Google Material
 * Symbol icons render through the proven {@link megamek.client.ui.util.FontHandler#symbolIcon} path and stay crisp on
 * HiDPI displays. The glyph code points, colors, and descriptions are read from the same resource keys the inline
 * badges use, so the legend always matches the badges shown on the options.</p>
 */
public class CampaignOptionsIconLegend extends JPanel {
    private static final int ICON_SIZE = UIUtil.scaleForGUI(16);

    /**
     * One legend row: a Material Symbols glyph {@code codePoint}, an optional {@code color} tint (null uses the label
     * foreground), and the resource key of the row's description text. Build rows with {@link #flagEntry} and
     * {@link #versionBadgeEntries()}.
     *
     * @param codePoint      the Material Symbols code point of the row's glyph
     * @param color          the glyph tint, or null to use the label foreground
     * @param descriptionKey the resource key (in the Campaign Options bundle) of the row's description text
     */
    public record Entry(int codePoint, @Nullable Color color, String descriptionKey) {}

    /**
     * Creates a legend listing {@code entries}, two per row in the given order. Each caller supplies the markers it
     * uses (see {@link #flagEntry} and {@link #versionBadgeEntries()}), so this component stays independent of any one
     * dialog's set.
     *
     * @param entries the legend rows to show, in order
     */
    public CampaignOptionsIconLegend(List<Entry> entries) {
        super(new GridBagLayout());
        setName("campaignOptionsIconLegend");
        setOpaque(false);

        for (int index = 0; index < entries.size(); index++) {
            Entry entry = entries.get(index);
            addEntry(index / 2, index % 2, entry.codePoint(), entry.color(), entry.descriptionKey());
        }
    }

    /**
     * Builds the legend row explaining an option flag marker (uncoloured, matching the inline flag badges).
     *
     * @param flag the flag whose marker to explain
     *
     * @return the legend entry for {@code flag}
     */
    public static Entry flagEntry(CampaignOptionFlag flag) {
        return new Entry(flagSymbol(flag.name()), null, legendDescriptionKey(flag));
    }

    /**
     * Builds the "added since" version-badge rows - the development and milestone stars - in display order. Include
     * these for any dialog whose options carry a version in their metadata, so the coloured stars are explained.
     *
     * @return the development and milestone legend entries
     */
    public static List<Entry> versionBadgeEntries() {
        return List.of(new Entry(badgeSymbol("development"), badgeColor("development"), "legend.development"),
              new Entry(badgeSymbol("milestone"), badgeColor("milestone"), "legend.milestone"));
    }

    /**
     * Creates an "Icons Legend" footer button whose popup lists {@code entries}. Clicking it opens the legend in a
     * popup above the button, so every dialog that shows option badges explains them the same way.
     *
     * @param entries the legend rows to show
     *
     * @return the configured legend button
     */
    public static JButton createLegendButton(List<Entry> entries) {
        JButton legendButton = baseLegendButton();
        legendButton.addActionListener(
              evt -> showLegendPopupAbove(legendButton, new CampaignOptionsIconLegend(entries)));
        return legendButton;
    }

    private static JButton baseLegendButton() {
        JButton legendButton = new JButton(getTextAt(getCampaignOptionsResourceBundle(), "lblIconsLegend.text"));
        legendButton.setName("btnIconsLegend");
        legendButton.setToolTipText(getTextAt(getCampaignOptionsResourceBundle(), "lblIconsLegend.tooltip"));
        legendButton.setIcon(symbolIcon(0xE88E, legendButton.getFont().getSize(), legendButton.getForeground()));
        return legendButton;
    }

    /**
     * Shows {@code legend} in a popup anchored above {@code anchor}, overlaying the content above the footer (opening
     * downward would run past the bottom of the dialog).
     *
     * @param anchor the footer button the popup opens above
     * @param legend the legend content to show
     */
    private static void showLegendPopupAbove(JButton anchor, CampaignOptionsIconLegend legend) {
        legend.setBorder(BorderFactory.createEmptyBorder(UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(8)));

        JPopupMenu legendPopup = new JPopupMenu();
        legendPopup.setName("campaignOptionsLegendPopup");
        legendPopup.setLayout(new BorderLayout());
        legendPopup.add(legend, BorderLayout.CENTER);

        Dimension popupSize = legendPopup.getPreferredSize();
        // The button is in the footer, so open the popup above it (negative y) to overlay the content above.
        legendPopup.show(anchor, 0, -popupSize.height);
    }

    private void addEntry(int row, int column, int codePoint, @Nullable Color color, String descriptionKey) {
        Color iconColor = (color != null) ? color : UIManager.getColor("Label.foreground");

        JLabel label = new JLabel("<html>" + getTextAt(getCampaignOptionsResourceBundle(), descriptionKey) + "</html>");
        label.setIcon(symbolIcon(codePoint, ICON_SIZE, iconColor));
        label.setIconTextGap(UIUtil.scaleForGUI(6));

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = row;
        constraints.weightx = 0.5;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(UIUtil.scaleForGUI(3),
              UIUtil.scaleForGUI(8),
              UIUtil.scaleForGUI(3),
              UIUtil.scaleForGUI(8));
        add(label, constraints);
    }

    /** Maps an option flag to its legend description resource key. */
    private static String legendDescriptionKey(CampaignOptionFlag flag) {
        return switch (flag) {
            case CUSTOM_SYSTEM -> "legend.customSystem";
            case DOCUMENTED -> "legend.documented";
            case IMPORTANT -> "legend.important";
            case RECOMMENDED -> "legend.recommended";
            case UNIMPLEMENTED -> "legend.unimplemented";
        };
    }

    private static int flagSymbol(String key) {
        return getTextAt(getCampaignOptionsResourceBundle(), "flag." + key + ".symbol").codePointAt(0);
    }

    private static int badgeSymbol(String key) {
        return getTextAt(getCampaignOptionsResourceBundle(), "badge." + key + ".symbol").codePointAt(0);
    }

    private static Color badgeColor(String key) {
        return Color.decode(getTextAt(getCampaignOptionsResourceBundle(), "badge." + key + ".color"));
    }
}
