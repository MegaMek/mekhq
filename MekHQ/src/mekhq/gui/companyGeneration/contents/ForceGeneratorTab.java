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

import megamek.client.ui.dialogs.randomArmy.ForceGeneratorOptionsView;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;

/**
 * Wraps the embedded {@link ForceGeneratorOptionsView} from MegaMek. This is where the user actually
 * picks faction, echelon, unit type, weight class, rating, experience, augmentation, transport
 * percentages, and mission roles — the inputs that drive ratgen.
 *
 * <p>Tab content migrates from {@code CompanyGenerationOptionsPanel} in Phase A step 5. The view
 * itself is unchanged; this Tab class becomes the new home for its construction, button-hiding,
 * year-locking, and the {@code buildForceDescriptor()} accessor the dialog calls on OK.</p>
 */
public class ForceGeneratorTab {

    private final Campaign campaign;
    private CompanyGenerationOptions options;
    private ForceGeneratorOptionsView optionsView;

    public ForceGeneratorTab(Campaign campaign, CompanyGenerationOptions options) {
        this.campaign = campaign;
        this.options = options;
    }

    /**
     * Builds the Force Generator tab panel.
     *
     * @return the tab content; currently a placeholder label
     */
    public JPanel createTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setName("pnlForceGeneratorTab");
        panel.add(new JLabel(
                    "Force Generator tab — placeholder. Embedded ratgen view migrates in Phase A step 5.",
                    SwingConstants.CENTER),
              BorderLayout.CENTER);
        return panel;
    }

    /**
     * Returns the embedded {@link ForceGeneratorOptionsView}, or {@code null} until step 5 wires it.
     * The Company Generation dialog calls this on OK to read the user's selections.
     */
    public ForceGeneratorOptionsView getOptionsView() {
        return optionsView;
    }

    public void loadValuesFromOptions(CompanyGenerationOptions sourceOptions) {
        this.options = sourceOptions;
    }

    public void writeValuesToOptions(CompanyGenerationOptions targetOptions) {
        // no-op until step 5
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }
}
