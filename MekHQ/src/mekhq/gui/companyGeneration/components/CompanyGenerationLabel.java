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

import javax.swing.JLabel;

import megamek.common.annotations.Nullable;

/**
 * A specialized {@link JLabel} for the Company Generation dialog.
 *
 * <p>Text and tooltip come from {@code mekhq.resources.CompanyGenerationDialog}, indexed by
 * {@code "lbl" + name + ".text"} and {@code "lbl" + name + ".tooltip"}. Pass {@code noTooltip = true}
 * to omit tooltip lookup entirely (useful for value-display labels whose text is set programmatically).</p>
 */
public class CompanyGenerationLabel extends JLabel {

    /**
     * Constructs a label with both text and tooltip pulled from the Company Generation bundle.
     *
     * @param name the bundle-key suffix
     */
    public CompanyGenerationLabel(String name) {
        this(name, null, false);
    }

    /**
     * Same as {@link #CompanyGenerationLabel(String)} but lets the caller suppress the tooltip lookup.
     *
     * @param name      the bundle-key suffix
     * @param noTooltip when {@code true}, no tooltip is set on the label
     */
    public CompanyGenerationLabel(String name, boolean noTooltip) {
        this(name, null, noTooltip);
    }

    /**
     * Full constructor.
     *
     * @param name           the bundle-key suffix
     * @param customWrapSize maximum tooltip line length, or {@code null} for the default
     * @param noTooltip      when {@code true}, no tooltip is set on the label
     */
    public CompanyGenerationLabel(String name, @Nullable Integer customWrapSize, boolean noTooltip) {
        super(String.format("<html>%s</html>",
              getTextAt(getCompanyGenerationResourceBundle(), "lbl" + name + ".text")));

        if (!noTooltip) {
            String tooltipText = getTextAt(getCompanyGenerationResourceBundle(), "lbl" + name + ".tooltip");
            if (!tooltipText.isEmpty()) {
                setToolTipText(wordWrap(tooltipText, processWrapSize(customWrapSize)));
            }
        }

        setName("lbl" + name);
        setFontScaling(this, false, 1);
    }
}
