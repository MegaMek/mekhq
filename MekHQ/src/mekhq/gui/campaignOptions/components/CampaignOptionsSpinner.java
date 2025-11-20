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
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import megamek.common.annotations.Nullable;

/**
 * A specialized {@link JSpinner} component for use in campaign options dialogs.
 * <p>
 * This spinner is highly configurable, supporting both integer and double values, customizable tooltips (with word
 * wrapping), and dynamic resource loading for names and tooltips. The spinner also applies consistent styling for
 * consistent appearance in the UI.
 */
public class CampaignOptionsSpinner extends JSpinner {
    /**
     * Creates a {@link CampaignOptionsSpinner} with fully configurable numeric values and tooltip settings.
     * <p>
     * The spinner uses a {@link SpinnerNumberModel} that supports both integer and double values. The tooltip is
     * fetched from a resource bundle using the key {@code "lbl" + name + ".tooltip"} and can be wrapped to a specified
     * line length using {@code customWrapSize}. If {@code noTooltip} is set to {@code true}, the spinner will not have
     * a tooltip.
     *
     * @param name           the name of the spinner, used to construct its resource bundle keys and internal name
     * @param customWrapSize the maximum number of characters per line for the tooltip (or 100 by default if
     *                       {@code null})
     * @param defaultValue   the default value of the spinner (integer or double)
     * @param minimum        the minimum value for the spinner (integer or double)
     * @param maximum        the maximum value for the spinner (integer or double)
     * @param stepSize       the step value for incrementing or decrementing the spinner (integer or double)
     * @param noTooltip      if {@code true}, the spinner will not have a tooltip
     */
    public CampaignOptionsSpinner(String name, @Nullable Integer customWrapSize,
          Number defaultValue, Number minimum, Number maximum, Number stepSize, boolean noTooltip) {
        super(createSpinnerModel(defaultValue, minimum, maximum, stepSize));

        if (!noTooltip) {
            String tooltipText = getTextAt(getCampaignOptionsResourceBundle(), "lbl" + name + ".tooltip");
            tooltipText += getFormattedTextAt(getCampaignOptionsResourceBundle(), "lblMinimum.text", minimum);
            tooltipText += getFormattedTextAt(getCampaignOptionsResourceBundle(), "lblMaximum.text", maximum);
            tooltipText += getFormattedTextAt(getCampaignOptionsResourceBundle(), "lblDefault.text", defaultValue);

            setToolTipText(wordWrap(tooltipText));
        }

        configureSpinner(name);
    }

    /**
     * Creates a {@link CampaignOptionsSpinner} for integer values with default tooltip settings.
     * <p>
     * The spinner will be initialized with an integer-based {@link SpinnerNumberModel} using the provided minimum,
     * maximum, step size, and default value.
     *
     * @param name         the name of the spinner, used to construct its resource bundle keys and internal name
     * @param defaultValue the default value of the spinner (integer)
     * @param minimum      the minimum value for the spinner (integer)
     * @param maximum      the maximum value for the spinner (integer)
     * @param stepSize     the step value for incrementing or decrementing the spinner (integer)
     */
    public CampaignOptionsSpinner(String name, int defaultValue, int minimum,
          int maximum, int stepSize) {
        this(name, null, defaultValue, minimum, maximum, stepSize, false);
    }

    /**
     * Creates a {@link CampaignOptionsSpinner} for double values with default tooltip settings.
     * <p>
     * The spinner will be initialized with a double-based {@link SpinnerNumberModel} using the provided minimum,
     * maximum, step size, and default value.
     *
     * @param name         the name of the spinner, used to construct its resource bundle keys and internal name
     * @param defaultValue the default value of the spinner (double)
     * @param minimum      the minimum value for the spinner (double)
     * @param maximum      the maximum value for the spinner (double)
     * @param stepSize     the step value for incrementing or decrementing the spinner (double)
     */
    public CampaignOptionsSpinner(String name, double defaultValue, double minimum,
          double maximum, double stepSize) {
        this(name, null, defaultValue, minimum, maximum, stepSize, false);
    }

    /**
     * Creates a {@link SpinnerNumberModel} for the spinner by detecting whether integer or double-based values should
     * be used.
     *
     * @param defaultValue the default value (integer or double)
     * @param minimum      the minimum value (integer or double)
     * @param maximum      the maximum value (integer or double)
     * @param stepSize     the step size (integer or double)
     *
     * @return a configured {@link SpinnerNumberModel} for the spinner
     */
    private static SpinnerNumberModel createSpinnerModel(Number defaultValue, Number minimum,
          Number maximum, Number stepSize) {
        if (defaultValue instanceof Double || minimum instanceof Double ||
                  maximum instanceof Double || stepSize instanceof Double) {
            // Use a double-based model for floating-point precision
            return new SpinnerNumberModel(
                  defaultValue.doubleValue(), minimum.doubleValue(), maximum.doubleValue(), stepSize.doubleValue()
            );
        } else {
            // Use an integer-based model for whole numbers
            return new SpinnerNumberModel(
                  defaultValue.intValue(), minimum.intValue(), maximum.intValue(), stepSize.intValue()
            );
        }
    }

    /**
     * Configures the spinner's common settings, including name, font scaling, and text alignment.
     *
     * @param name the base name of the spinner, used to set its internal name
     */
    private void configureSpinner(String name) {
        setName("spn" + name);
        setFontScaling(this, false, 1);

        // Align text in the spinner editor to the left
        DefaultEditor editor = (DefaultEditor) this.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
    }
}
