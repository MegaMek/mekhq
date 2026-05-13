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
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.processWrapSize;
import static mekhq.gui.companyGeneration.components.CompanyGenerationUtilities.getCompanyGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.JCheckBox;

import megamek.common.annotations.Nullable;

/**
 * A specialized {@link JCheckBox} for the Company Generation dialog.
 *
 * <p>Text and tooltip come from the {@code mekhq.resources.CompanyGenerationDialog} bundle, indexed
 * by {@code "lbl" + name + ".text"} and {@code "lbl" + name + ".tooltip"}. The wrap-size and font-scale
 * helpers are reused from the Campaign Options package — only the bundle is package-local.</p>
 */
public class CompanyGenerationCheckBox extends JCheckBox {

    /**
     * Constructs a checkbox whose text and tooltip are read from the Company Generation bundle.
     *
     * @param name the bundle-key suffix; final keys are {@code lbl<name>.text} / {@code lbl<name>.tooltip}
     */
    public CompanyGenerationCheckBox(String name) {
        this(name, null);
    }

    /**
     * Same as {@link #CompanyGenerationCheckBox(String)} but lets the caller specify the tooltip's
     * word-wrap column count.
     *
     * @param name           the bundle-key suffix
     * @param customWrapSize maximum tooltip line length, or {@code null} for the default
     */
    public CompanyGenerationCheckBox(String name, @Nullable Integer customWrapSize) {
        super(String.format("<html>%s</html>",
              getTextAt(getCompanyGenerationResourceBundle(), "lbl" + name + ".text")));

        setName("chk" + name);

        String tooltipText = getTextAt(getCompanyGenerationResourceBundle(), "lbl" + name + ".tooltip");
        if (!tooltipText.isEmpty()) {
            setToolTipText(wordWrap(tooltipText, processWrapSize(customWrapSize)));
        }

        setFontScaling(this, false, 1);
    }
}
