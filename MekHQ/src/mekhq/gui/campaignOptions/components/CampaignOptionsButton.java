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
 * This class provides a custom {@link JButton} for campaign options.
 * The button name and tooltips are fetched from a resource bundle based on the provided name.
 */
public class CampaignOptionsButton extends JButton {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Creates a new {@link JButton}.
     * <p>
     * The name of the created {@link JButton} is {@code "btn" + name}
     * The resource bundle references for the created {@link JButton} are {@code "lbl" + name + ".text"}
     * and {@code "lbl" + name + ".tooltip"}.
     *
     * @param name the name of the button, used to generate the button's name and resource bundle references
     */
    public CampaignOptionsButton(String name) {
        this(name, null);
    }

    /**
     * Creates a new {@link JButton} with a custom tooltip wrap size.
     * <p>
     * The name of the created {@link JButton} is {@code "btn" + name}
     * The resource bundle references for the created {@link JButton} are {@code "lbl" + name + ".text"}
     * and {@code "lbl" + name + ".tooltip"}.
     *
     * @param name the name of the button, used for text and tooltip generation
     * @param customWrapSize the maximum number of characters for line wrapping in the tooltip,
     *                       or {@code null} if the default wrap size is to be used
     */
    public CampaignOptionsButton(String name, @Nullable Integer customWrapSize) {
        super(resources.getString("lbl" + name + ".text"));
        setToolTipText(wordWrap(resources.getString("lbl" + name + ".tooltip"),
            processWrapSize(customWrapSize)));
        setName("btn" + name);

        setFontScaling(this, false, 1);
    }
}
