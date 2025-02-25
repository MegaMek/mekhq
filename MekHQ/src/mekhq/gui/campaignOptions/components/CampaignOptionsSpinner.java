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
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.swing.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;

/**
 * A specialized {@link JSpinner} component for use in campaign options dialogs.
 * <p>
 * This spinner is highly configurable, supporting both integer and double values,
 * customizable tooltips (with word wrapping), and dynamic resource loading for names
 * and tooltips. The spinner also applies consistent styling for consistent
 * appearance in the UI.
 */
public class CampaignOptionsSpinner extends JSpinner {

    /**
     * The path to the resource bundle containing text and tooltip information for the spinner.
     */
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";

    /**
     * The {@link ResourceBundle} used to load localized tooltip text for the spinner.
     */
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Creates a {@link CampaignOptionsSpinner} with fully configurable numeric values and tooltip settings.
     * <p>
     * The spinner uses a {@link SpinnerNumberModel} that supports both integer and double values.
     * The tooltip is fetched from a resource bundle using the key {@code "lbl" + name + ".tooltip"} and can be
     * wrapped to a specified line length using {@code customWrapSize}. If {@code noTooltip} is set to {@code true},
     * the spinner will not have a tooltip.
     *
     * @param name           the name of the spinner, used to construct its resource bundle keys and internal name
     * @param customWrapSize the maximum number of characters per line for the tooltip (or 100 by default if {@code null})
     * @param defaultValue   the default value of the spinner (integer or double)
     * @param minimum        the minimum value for the spinner (integer or double)
     * @param maximum        the maximum value for the spinner (integer or double)
     * @param stepSize       the step value for incrementing or decrementing the spinner (integer or double)
     * @param noTooltip      if {@code true}, the spinner will not have a tooltip
     */
    public CampaignOptionsSpinner(String name, @Nullable Integer customWrapSize,
                                  Number defaultValue, Number minimum,
                                  Number maximum, Number stepSize, boolean noTooltip) {
        super(createSpinnerModel(defaultValue, minimum, maximum, stepSize));

        if (!noTooltip) {
            setToolTipText(wordWrap(getTooltipText(name), processWrapSize(customWrapSize)));
        }

        configureSpinner(name);
    }

    /**
     * Creates a {@link CampaignOptionsSpinner} for integer values with default tooltip settings.
     * <p>
     * The spinner will be initialized with an integer-based {@link SpinnerNumberModel} using the
     * provided minimum, maximum, step size, and default value.
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
     * The spinner will be initialized with a double-based {@link SpinnerNumberModel} using the
     * provided minimum, maximum, step size, and default value.
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
     * Creates a {@link SpinnerNumberModel} for the spinner by detecting whether
     * integer or double-based values should be used.
     *
     * @param defaultValue the default value (integer or double)
     * @param minimum      the minimum value (integer or double)
     * @param maximum      the maximum value (integer or double)
     * @param stepSize     the step size (integer or double)
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

    /**
     * Fetches the tooltip text for the spinner from the resource bundle.
     * If the resource key is not found, an empty string is returned as the tooltip.
     *
     * @param name the name of the spinner, used to construct the resource bundle key for the tooltip
     * @return the tooltip text for the spinner, or an empty string if the key does not exist
     */
    private String getTooltipText(String name) {
        try {
            return resources.getString("lbl" + name + ".tooltip");
        } catch (MissingResourceException e) {
            // Return an empty string if the resource is missing
            return "";
        }
    }
}
