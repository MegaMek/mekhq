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

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.enums.ValidationState;
import mekhq.campaign.CampaignPreset;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.displayWrappers.FactionDisplay;

import javax.swing.*;
import java.awt.*;

public class CampaignPresetCustomizationDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private final CampaignPreset preset;

    private JTextField txtTitle;
    private JTextField txtDescription;

    //region Startup
    private JCheckBox chkSpecifyDate;
    private JCheckBox chkSpecifyFaction;
    private MMComboBox<FactionDisplay> comboFaction;
    private JCheckBox chkSpecifyPlanet;
    private JCheckBox chkSpecifyRankSystem;
    private JSpinner spnContractCount;
    private JCheckBox chkSpecifyGameOptions;
    //endregion Startup

    //endregion Variable Declarations

    //region Constructors
    public CampaignPresetCustomizationDialog(final JFrame frame, final CampaignPreset preset) {
        super(frame, "CampaignPresetCustomizationDialog", "CampaignPresetCustomizationDialog.title");
        this.preset = preset;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public CampaignPreset getPreset() {
        return preset;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {

        //region Startup
        //endregion Startup

        //region Continuous
        //endregion Continuous

        return null;
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected ValidationState validateAction(final boolean display) {
        return null;
    }
    //endregion Button Actions

    public void updatePreset(final CampaignPreset preset) {
        if (!getState().isSuccess()) {
            validateButtonActionPerformed(null);
        }

        if (getState().isSuccess() || getState().isWarning()) {
            // TODO : Update the preset based on the dialog
        }
    }
}
