/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.components;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getCampaignOptionsResourceBundle;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.JLabel;

import megamek.common.annotations.Nullable;

/**
 * A specialized {@link JLabel} component designed for use in campaign options dialogs.
 * <p>
 * The label's text and tooltip are dynamically loaded from a resource bundle based on the provided name. Tooltip text
 * can also be configured for word wrapping, and an option exists to create the label without a tooltip.
 */
public class CampaignOptionsLabel extends JLabel {
    /**
     * Constructs a {@link CampaignOptionsLabel} with the specified name.
     * <p>
     * The label's text is retrieved using the resource key {@code "lbl" + name + ".text"}. The tooltip is retrieved
     * using the resource key {@code "lbl" + name + ".tooltip"}. If the keys do not exist in the resource bundle, an
     * exception will be thrown.
     * <p>
     * A tooltip is included by default, and a default line wrapping size is used for the tooltip text.
     *
     * @param name the base name of the label, used to construct resource bundle keys
     */
    public CampaignOptionsLabel(String name) {
        this(name, null, false);
    }

    /**
     * Constructs a {@link CampaignOptionsLabel} with the specified name, optional custom tooltip wrap size, and
     * optional tooltip exclusion.
     * <p>
     * The label's text is retrieved using the resource key {@code "lbl" + name + ".text"}. If {@code noTooltip} is
     * {@code false}, the tooltip is retrieved using the resource key {@code "lbl" + name + ".tooltip"} and word-wrapped
     * based on the provided {@code customWrapSize}, defaulting to 100 characters if {@code customWrapSize} is
     * {@code null}. If {@code noTooltip} is {@code true}, no tooltip is set for the label.
     * <p>
     * If the resource keys do not exist in the resource bundle, an exception will be thrown.
     *
     * @param name           the base name of the label, used to construct resource bundle keys
     * @param customWrapSize the maximum number of characters per line in the tooltip, or {@code null} to use the
     *                       default value of 100
     * @param noTooltip      if {@code true}, the label is created without a tooltip
     */
    public CampaignOptionsLabel(String name, @Nullable Integer customWrapSize, boolean noTooltip) {
        // Set the label's text using the resource bundle with HTML formatting for better rendering
        super(String.format("<html>%s</html>", getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".text")));

        // Configure the tooltip if not excluded
        if (!noTooltip) {
            setToolTipText(wordWrap(getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip"),
                  processWrapSize(customWrapSize)));
        }

        // Set the internal name of the label
        setName("lbl" + name);

        // Apply font scaling
        setFontScaling(this, false, 1);
    }
}
