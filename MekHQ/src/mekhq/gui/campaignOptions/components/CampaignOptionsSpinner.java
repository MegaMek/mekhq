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

public class CampaignOptionsSpinner extends JSpinner {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    /**
     * Creates a {@link JSpinner} object.
     * <p>
     * The name of the {@link JSpinner} will be {@code "spn" + name},
     * and it will use the {@code "lbl" + name + ".tooltip"} resource bundle item.
     *
     * @param name           a string representing the name of the object.
     * @param customWrapSize the maximum number of characters (including spaces) on each
     *                       line of the tooltip (or {@code 100}, if {@code null}).
     * @param defaultValue   The default value of the spinner (integer or double).
     * @param minimum        The minimum value of the spinner (integer or double).
     * @param maximum        The maximum value of the spinner (integer or double).
     * @param stepSize       The step size of the spinner (integer or double).
     * @param noTooltip      {@code true} if the component should be created without a tooltip.
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
     * Creates a {@link JSpinner} object with integer values.
     * <p>
     * This constructor assumes a default {@code null} for wrap size and tooltip enabled.
     *
     * @param name         a string representing the name of the object.
     * @param defaultValue The default value of the spinner (integer).
     * @param minimum      The minimum value of the spinner (integer).
     * @param maximum      The maximum value of the spinner (integer).
     * @param stepSize     The step size of the spinner (integer).
     */
    public CampaignOptionsSpinner(String name, int defaultValue, int minimum,
                                  int maximum, int stepSize) {
        this(name, null, defaultValue, minimum, maximum, stepSize, false);
    }

    /**
     * Creates a {@link JSpinner} object with double values.
     * <p>
     * This constructor assumes a default {@code null} for wrap size and tooltip enabled.
     *
     * @param name         a string representing the name of the object.
     * @param defaultValue The default value of the spinner (double).
     * @param minimum      The minimum value of the spinner (double).
     * @param maximum      The maximum value of the spinner (double).
     * @param stepSize     The step size of the spinner (double).
     */
    public CampaignOptionsSpinner(String name, double defaultValue, double minimum,
                                  double maximum, double stepSize) {
        this(name, null, defaultValue, minimum, maximum, stepSize, false);
    }

    /**
     * A helper method to create the appropriate {@link SpinnerNumberModel} based on numeric types (integer or double).
     *
     * @param defaultValue The default value (integer or double).
     * @param minimum      The minimum value (integer or double).
     * @param maximum      The maximum value (integer or double).
     * @param stepSize     The step size (integer or double).
     * @return A configured {@link SpinnerNumberModel}.
     */
    private static SpinnerNumberModel createSpinnerModel(Number defaultValue, Number minimum,
                                                         Number maximum, Number stepSize) {
        if (defaultValue instanceof Double || minimum instanceof Double ||
            maximum instanceof Double || stepSize instanceof Double) {
            // If any value is a double, use a double-based SpinnerNumberModel
            return new SpinnerNumberModel(
                defaultValue.doubleValue(), minimum.doubleValue(), maximum.doubleValue(), stepSize.doubleValue()
            );
        } else {
            // Otherwise, use an integer-based SpinnerNumberModel
            return new SpinnerNumberModel(
                defaultValue.intValue(), minimum.intValue(), maximum.intValue(), stepSize.intValue()
            );
        }
    }

    /**
     * A helper method to configure repeated spinner settings (name, tooltip, etc.).
     *
     * @param name The base name of the spinner.
     */
    private void configureSpinner(String name) {
        setName("spn" + name);
        setFontScaling(this, false, 1);

        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) this.getEditor();
        editor.getTextField().setHorizontalAlignment(JTextField.LEFT);
    }

    /**
     * A helper method to get the tooltip text based on the spinner's name. Falls back to an empty string
     * if the tooltip resource is missing.
     *
     * @param name Name of the spinner.
     * @return Tooltip text.
     */
    private String getTooltipText(String name) {
        try {
            return resources.getString("lbl" + name + ".tooltip");
        } catch (MissingResourceException e) {
            return ""; // Default to no tooltip if the resource is missing
        }
    }
}
