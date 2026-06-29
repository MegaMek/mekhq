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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

    public CampaignOptionsIconLegend() {
        super(new GridBagLayout());
        setName("campaignOptionsIconLegend");
        setOpaque(false);

        addEntry(0, 0, flagSymbol("CUSTOM_SYSTEM"), null, "legend.customSystem");
        addEntry(0, 1, flagSymbol("DOCUMENTED"), null, "legend.documented");
        addEntry(1, 0, flagSymbol("IMPORTANT"), null, "legend.important");
        addEntry(1, 1, flagSymbol("RECOMMENDED"), null, "legend.recommended");
        addEntry(2, 0, badgeSymbol("development"), badgeColor("development"), "legend.development");
        addEntry(2, 1, badgeSymbol("milestone"), badgeColor("milestone"), "legend.milestone");
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
