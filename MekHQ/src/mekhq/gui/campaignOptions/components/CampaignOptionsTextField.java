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

import javax.swing.JTextField;

import megamek.common.annotations.Nullable;

/**
 * A specialized {@link JTextField} component designed for use in campaign options dialogs.
 * <p>
 * This text field fetches its tooltip text dynamically from a resource bundle based on the provided name. It also
 * supports a customizable tooltip wrap size while maintaining consistent UI scaling.
 */
public class CampaignOptionsTextField extends JTextField {
    /**
     * Constructs a {@link CampaignOptionsTextField} with a default tooltip wrap size.
     * <p>
     * The name of the text field is set to {@code "lbl" + name}, and its tooltip text is fetched using the key
     * {@code "lbl" + name + ".tooltip"} from the resource bundle. Tooltips are word-wrapped to a default width of 100
     * characters.
     *
     * @param name the base name used to generate the text field's name and tooltip text.
     */
    public CampaignOptionsTextField(String name) {
        this(name, null);
    }

    /**
     * Constructs a {@link CampaignOptionsTextField} with a customizable tooltip wrap size.
     * <p>
     * The name of the text field is set to {@code "lbl" + name}, and its tooltip text is fetched using the key
     * {@code "lbl" + name + ".tooltip"} from the resource bundle. Tooltips are word-wrapped to the specified width in
     * {@code customWrapSize}.
     *
     * @param name           the base name used to generate the text field's name and tooltip text.
     * @param customWrapSize the maximum number of characters (including spaces) per line in the tooltip text. If
     *                       {@code null}, a default wrap size of 100 characters is used.
     */
    public CampaignOptionsTextField(String name, @Nullable Integer customWrapSize) {
        super();

        // Set the tooltip text with word wrapping
        String tooltipText = getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip");
        if (!tooltipText.isEmpty()) {
            setToolTipText(wordWrap(tooltipText, processWrapSize(customWrapSize)));
        }

        // Set the component name
        setName("lbl" + name);

        // Apply UI font scaling
        setFontScaling(this, false, 1);
    }
}
