/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.dialog;

import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignPreset;
import mekhq.gui.baseComponents.AbstractMHQDialog;
import mekhq.gui.panels.CampaignPresetPanel;

import javax.swing.*;
import java.awt.*;

public class CampaignPresetManagementDialog extends AbstractMHQDialog {
    //region Variable Declarations
    private final Campaign campaign;
    //endregion Variable Declarations

    //region Constructors
    public CampaignPresetManagementDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CampaignPresetManagementDialog", "CampaignPresetManagementDialog.title");
        this.campaign = campaign;
        initialize();
    }
    //endregion Constructors

    //region Getters
    public Campaign getCampaign() {
        return campaign;
    }
    //endregion Getters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        final JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.setName("campaignPresetManagementPanel");

        for (final CampaignPreset preset : CampaignPreset.getCampaignPresets(true)) {
            panel.add(new CampaignPresetPanel(getFrame(), getCampaign(), preset));
        }

        final JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setName("campaignPresetManagementPane");
        return scrollPane;
    }
    //endregion Initialization
}
