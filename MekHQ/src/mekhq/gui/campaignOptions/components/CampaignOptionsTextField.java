/*
 * Copyright (c) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui.campaignOptions.components;

import megamek.common.annotations.Nullable;

import javax.swing.*;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

/**
 * A specialized {@link JTextField} component designed for use in campaign options dialogs.
 * <p>
 * This text field fetches its tooltip text dynamically from a resource bundle
 * based on the provided name. It also supports a customizable tooltip wrap size while
 * maintaining consistent UI scaling.
 */
public class CampaignOptionsTextField extends JTextField {

    /**
     * The path to the resource bundle that contains tooltip text for the text field.
     */
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";

    /**
     * The {@link ResourceBundle} used to fetch localized tooltip and other properties.
     */
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Constructs a {@link CampaignOptionsTextField} with a default tooltip wrap size.
     * <p>
     * The name of the text field is set to {@code "lbl" + name}, and its tooltip text is fetched
     * using the key {@code "lbl" + name + ".tooltip"} from the resource bundle. Tooltips are
     * word-wrapped to a default width of 100 characters.
     *
     * @param name the base name used to generate the text field's name and tooltip text.
     */
    public CampaignOptionsTextField(String name) {
        this(name, null);
    }

    /**
     * Constructs a {@link CampaignOptionsTextField} with a customizable tooltip wrap size.
     * <p>
     * The name of the text field is set to {@code "lbl" + name}, and its tooltip text is fetched
     * using the key {@code "lbl" + name + ".tooltip"} from the resource bundle. Tooltips are
     * word-wrapped to the specified width in {@code customWrapSize}.
     *
     * @param name           the base name used to generate the text field's name and tooltip text.
     * @param customWrapSize the maximum number of characters (including spaces) per line in the tooltip text.
     *                       If {@code null}, a default wrap size of 100 characters is used.
     */
    public CampaignOptionsTextField(String name, @Nullable Integer customWrapSize) {
        super();

        // Set the tooltip text with word wrapping
        setToolTipText(wordWrap(
            resources.getString("lbl" + name + ".tooltip"),
            processWrapSize(customWrapSize)
        ));

        // Set the component name
        setName("lbl" + name);

        // Apply UI font scaling
        setFontScaling(this, false, 1);
    }
}
