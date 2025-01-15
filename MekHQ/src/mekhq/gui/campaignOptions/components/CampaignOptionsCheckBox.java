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
import mekhq.gui.campaignOptions.CampaignOptionsUtilities;

import javax.swing.*;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

/**
 * A specialized {@link JCheckBox} used in the campaign options dialog.
 * <p>
 * This check box's text and tooltip are dynamically loaded from a resource bundle
 * based on a given name. The tooltip can optionally include word wrapping
 * with a configurable wrap size.
 * <p>
 * The checkbox also supports font scaling adjustments.
 */
public class CampaignOptionsCheckBox extends JCheckBox {
    /**
     * The path to the resource bundle containing text and tooltip information
     * for this component.
     */
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";

    /**
     * The {@link ResourceBundle} used to load localized strings for checkbox text and tooltips.
     */
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Constructs a new instance of {@link CampaignOptionsCheckBox} with the specified name.
     * <p>
     * The name is used to determine the checkbox's visible text and tooltip, as well
     * as to generate its unique internal name.
     * <p>
     * The text is located in the resource bundle key {@code "lbl" + name + ".text"}.
     * The tooltip is located in the resource bundle key {@code "lbl" + name + ".tooltip"}.
     * <p>
     * A default wrap size is used for the tooltip text if {@link CampaignOptionsUtilities#processWrapSize}
     * is not overridden.
     *
     * @param name the name used to fetch the checkbox's text and tooltip, and to set its name
     */
    public CampaignOptionsCheckBox(String name) {
        this(name, null);
    }

    /**
     * Constructs a new instance of {@link CampaignOptionsCheckBox} with the specified
     * name and a custom tooltip wrap size.
     * <p>
     * The name is used to determine the checkbox's visible text and tooltip, as well
     * as to generate its unique internal name. The text and tooltip are fetched
     * from the resource bundle, located at keys {@code "lbl" + name + ".text"}
     * and {@code "lbl" + name + ".tooltip"} respectively.
     * <p>
     * If a custom wrap size is provided, the tooltip text will be word-wrapped
     * accordingly. If {@code customWrapSize} is {@code null}, a default wrap size is used.
     *
     * @param name           the name used to fetch the checkbox's text and tooltip, and to set its name
     * @param customWrapSize the maximum number of characters per tooltip line,
     *                       or {@code null} for the default wrap size
     */
    public CampaignOptionsCheckBox(String name, @Nullable Integer customWrapSize) {
        // Sets the checkbox's text from the resource bundle, wrapped in HTML tags
        super(String.format("<html>%s</html>", resources.getString("lbl" + name + ".text")));

        // Sets the checkbox's internal name
        setName("chk" + name);

        // Sets the checkbox's tooltip, applying word wrapping based on customWrapSize
        setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
                processWrapSize(customWrapSize)));

        // Applies font scaling with default scaling disabled
        setFontScaling(this, false, 1);
    }
}
