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
 * Twelve percentage sliders bound to {@code CampaignOptions.getAutoLogistics*()} — heat sinks, Mek
 * head / non-repairable / other Mek locations, ammo, armor, actuators, jump jets, engines, weapons,
 * other. At generation time the same percentages drive both the starting spare inventory and the
 * ongoing campaign auto-logistics, so the player's starting-force setting persists as their long-term
 * stock policy.
 *
 * <p>Tab content is implemented in Phase A step 6 (or B, depending on whether the new behavior lands
 * with the structural migration or after). Currently a placeholder.</p>
 */
public class SparesTab {

    private final Campaign campaign;
    private CompanyGenerationOptions options;

    public SparesTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * Builds the Spares tab panel.
     *
     * @return the tab content; currently a placeholder label
     */
    public JPanel createTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("pnlSparesTab");
        panel.add(new JLabel(
                    "Spares tab — placeholder. Twelve auto-logistics sliders land in Phase B step 7.",
                    SwingConstants.CENTER),
              BorderLayout.CENTER);
        return panel;
    }

    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
    }

    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        // no-op until step 7
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
