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
package mekhq.gui.companyGeneration.components;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.FlatLafStyleBuilder.setFontScaling;
import static mekhq.gui.companyGeneration.components.CompanyGenerationUtilities.getCompanyGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

/**
 * A specialized {@link JSpinner} for the Company Generation dialog.
 *
 * <p>Tooltip text comes from the {@code mekhq.resources.CompanyGenerationDialog} bundle, indexed by
 * {@code "lbl" + name + ".tooltip"}. The spinner supports both integer and double-valued models;
 * the {@link Number}-typed constructor picks the right backing {@link SpinnerNumberModel} based on
 * the runtime type of the values passed in.</p>
 */
public class CompanyGenerationSpinner extends JSpinner {

    /**
     * Integer-valued spinner.
     *
     * @param name         the bundle-key suffix
     * @param defaultValue initial integer value
     * @param minimum      minimum integer value
     * @param maximum      maximum integer value
     * @param stepSize     integer increment per step
     */
    public CompanyGenerationSpinner(String name, int defaultValue, int minimum, int maximum, int stepSize) {
        this(name, (Number) defaultValue, minimum, maximum, stepSize);
    }

    /**
     * Double-valued spinner.
     *
     * @param name         the bundle-key suffix
     * @param defaultValue initial double value
     * @param minimum      minimum double value
     * @param maximum      maximum double value
     * @param stepSize     double increment per step
     */
    public CompanyGenerationSpinner(String name, double defaultValue, double minimum, double maximum,
          double stepSize) {
        this(name, (Number) defaultValue, minimum, maximum, stepSize);
    }

    private CompanyGenerationSpinner(String name, Number defaultValue, Number minimum, Number maximum,
          Number stepSize) {
        super(createSpinnerModel(defaultValue, minimum, maximum, stepSize));

        String tooltipText = getTextAt(getCompanyGenerationResourceBundle(), "lbl" + name + ".tooltip");
        if (!tooltipText.isEmpty()) {
            setToolTipText(wordWrap(tooltipText));
        }

        setName("spn" + name);
        setFontScaling(this, false, 1);
        JComponent editor = getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            JTextField textField = defaultEditor.getTextField();
            setFontScaling(textField, false, 1);
        }
    }

    /**
     * Builds an integer- or double-typed {@link SpinnerNumberModel} based on the runtime types of the
     * supplied values. Any of the four values being a {@link Double} forces double mode.
     */
    private static SpinnerNumberModel createSpinnerModel(Number defaultValue, Number minimum,
          Number maximum, Number stepSize) {
        boolean useDouble = defaultValue instanceof Double
              || minimum instanceof Double
              || maximum instanceof Double
              || stepSize instanceof Double;
        if (useDouble) {
            return new SpinnerNumberModel(defaultValue.doubleValue(), minimum.doubleValue(),
                  maximum.doubleValue(), stepSize.doubleValue());
        }
        return new SpinnerNumberModel(defaultValue.intValue(), minimum.intValue(), maximum.intValue(),
              stepSize.intValue());
    }
}
