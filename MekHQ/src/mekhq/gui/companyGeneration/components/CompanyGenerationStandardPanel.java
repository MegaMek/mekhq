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

import static mekhq.gui.companyGeneration.components.CompanyGenerationUtilities.getCompanyGenerationResourceBundle;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import javax.swing.JPanel;

import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * A specialized {@link JPanel} used to group related controls inside Company Generation tabs.
 *
 * <p>Mirrors {@code CampaignOptionsStandardPanel}: an optional rounded titled border whose title text
 * comes from the {@code mekhq.resources.CompanyGenerationDialog} bundle. The layout itself is left to
 * the caller — pair this with {@code CampaignOptionsGridBagConstraints} the same way the Campaign
 * Options sub-panels do.</p>
 */
public class CompanyGenerationStandardPanel extends JPanel {

    /**
     * Borderless panel; just a named JPanel with the {@code pnl<name>} component name.
     */
    public CompanyGenerationStandardPanel(String name) {
        this(name, false, "");
    }

    /**
     * Optional untitled rounded border.
     *
     * @param name          the bundle-key suffix (used for the component name)
     * @param includeBorder when {@code true}, a rounded line border is applied
     */
    public CompanyGenerationStandardPanel(String name, boolean includeBorder) {
        this(name, includeBorder, "");
    }

    /**
     * Optional titled rounded border.
     *
     * @param name          the bundle-key suffix (used for the component name)
     * @param includeBorder when {@code true}, a rounded line border is applied
     * @param borderTitle   bundle-key suffix for the border title; final key is {@code lbl<borderTitle>.text}.
     *                      Empty string yields an untitled border (when {@code includeBorder} is true).
     */
    public CompanyGenerationStandardPanel(String name, boolean includeBorder, String borderTitle) {
        String resolvedTitle = borderTitle.isBlank()
              ? ""
              : getTextAt(getCompanyGenerationResourceBundle(), "lbl" + borderTitle + ".text");

        if (includeBorder) {
            if (resolvedTitle.isBlank()) {
                setBorder(RoundedLineBorder.createRoundedLineBorder());
            } else {
                setBorder(RoundedLineBorder.createRoundedLineBorder(resolvedTitle));
            }
        }

        setName("pnl" + name);
    }
}
