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
package mekhq.gui.companyGeneration.contents;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Catch-all for post-generation rules and miscellaneous knobs: finances (starting cash, credits,
 * debits), contracts (auto-generate starting contract), starting simulation (retirees, dependents,
 * dead-time months), surprises (mystery box, special events), and unit extras (mothballed counts,
 * customizations).
 *
 * <p>Tab content migrates over from {@code CompanyGenerationOptionsPanel} in Phase A step 6.
 * Currently a placeholder.</p>
 */
public class OtherTab {

    private final Campaign campaign;
    private CompanyGenerationOptions options;

    public OtherTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * Builds the Other tab panel.
     *
     * @return the tab content; currently a placeholder label
     */
    public JPanel createTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("pnlOtherTab");
        panel.add(new JLabel(
                    "Other tab — placeholder. Content migrates in Phase A step 6.",
                    SwingConstants.CENTER),
              BorderLayout.CENTER);
        return panel;
    }

    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
    }

    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        // no-op until step 6
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
