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
package mekhq.gui.campaignOptions;

import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.Component;

import megamek.client.ui.comboBoxes.FontComboBox;
import megamek.client.ui.displayWrappers.FontDisplay;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;

/**
 * The Fonts page of the MekHQ Client Options dialog: a single section with the handwriting font used by the medical
 * view dialog. Owns its control and copies it back into the shared {@link MHQOptionsModel} in {@link #writeToModel()}.
 */
class MHQFontsPage extends MHQOptionsPage {
    private FontComboBox comboMedicalViewDialogHandwritingFont;

    MHQFontsPage(MHQOptionsModel model) {
        super(model);
    }

    @Override
    Component createPage() {
        CampaignOptionsLabel label = new CampaignOptionsLabel(RESOURCE_BUNDLE, "lblMedicalViewDialogHandwritingFont");

        comboMedicalViewDialogHandwritingFont = new FontComboBox("comboMedicalViewDialogHandwritingFont");
        comboMedicalViewDialogHandwritingFont.setToolTipText(
              getTextAt(RESOURCE_BUNDLE, "lblMedicalViewDialogHandwritingFont.toolTipText"));
        comboMedicalViewDialogHandwritingFont.setSelectedItem(new FontDisplay(model.medicalViewDialogHandwritingFont));

        CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("MHQFontsContent", FORM_LABEL_WIDTH,
              FORM_CONTROL_WIDTH);
        panel.addRow(label, comboMedicalViewDialogHandwritingFont);
        Component page = buildMHQPage("MHQFontsPage", "lblMHQFontsSection.text", "lblMHQFontsSection.summary", panel);
        created = true;
        return page;
    }

    @Override
    void writeToModel() {
        if (!created) {
            return;
        }
        model.medicalViewDialogHandwritingFont = comboMedicalViewDialogHandwritingFont.getFont().getFamily();
    }
}
