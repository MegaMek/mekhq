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
 * Pre-generation rules: force-shape toggles (Command Lance, weight limits, force naming method),
 * the nine support-personnel percentage sliders, officer-selection flags, rank / callsign / founder
 * flags, and the random origin sub-panel.
 *
 * <p>Tab content migrates over from {@code CompanyGenerationOptionsPanel} in Phase A step 4.
 * Currently a placeholder so the new {@link mekhq.gui.companyGeneration.CompanyGenerationPane} can
 * mount cleanly while the rest of the migration proceeds.</p>
 */
public class SetupTab {

    private final Campaign campaign;
    private CompanyGenerationOptions options;

    public SetupTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * Builds the Setup tab panel.
     *
     * @return the tab content; currently a placeholder label
     */
    public JPanel createTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("pnlSetupTab");
        panel.add(new JLabel("Setup tab — placeholder. Content migrates in Phase A step 4.",
              SwingConstants.CENTER), BorderLayout.CENTER);
        return panel;
    }

    /**
     * Pushes values from the supplied options onto this tab's controls. Stub until the controls land
     * in step 4.
     */
    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
    }

    /**
     * Reads values back from this tab's controls into the supplied options. Stub until step 4.
     */
    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        // no-op until step 4
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
