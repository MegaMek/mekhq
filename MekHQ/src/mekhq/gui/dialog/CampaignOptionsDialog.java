/*
 * Copyright (c) 2009-2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
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

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.enums.DialogResult;
import megamek.client.ui.enums.ValidationState;
import megamek.common.annotations.Nullable;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignPreset;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.panes.CampaignOptionsPane;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com> (Original Version, now largely CampaignOptionsPane)
 * @author Justin 'Windchild' Bowen (Current Version)
 */
public class CampaignOptionsDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private final Campaign campaign;
    private final boolean startup;
    private CampaignOptionsPane campaignOptionsPane;
    //endregion Variable Declarations

    //region Constructors
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog",
                        MekHQ.getMekHQOptions().getLocale(), new EncodeControl()),
                "CampaignOptionsDialog", "CampaignOptionsDialog.title");
        this.campaign = campaign;
        this.startup = startup;
        initialize();
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public boolean isStartup() {
        return startup;
    }

    public CampaignOptionsPane getCampaignOptionsPane() {
        return campaignOptionsPane;
    }

    public void setCampaignOptionsPane(final CampaignOptionsPane campaignOptionsPane) {
        this.campaignOptionsPane = campaignOptionsPane;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setCampaignOptionsPane(new CampaignOptionsPane(getFrame(), getCampaign(), isStartup()));
        return getCampaignOptionsPane();
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(1, 0));

        panel.add(new MMButton("btnOkay", resources, "Confirm.text", null,
                this::okButtonActionPerformed));

        panel.add(new MMButton("btnSavePreset", resources, "btnSavePreset.text",
                "btnSavePreset.toolTipText", evt -> btnSaveActionPerformed()));

        panel.add(new MMButton("btnLoadPreset", resources, "btnLoadPreset.text",
                "btnLoadPreset.toolTipText", evt -> {
            final CampaignPresetSelectionDialog presetSelectionDialog = new CampaignPresetSelectionDialog(getFrame());
            if (presetSelectionDialog.showDialog().isConfirmed()) {
                applyPreset(presetSelectionDialog.getSelectedPreset());
            }
        }));

        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
                this::cancelActionPerformed));

        return panel;
    }

    @Override
    protected void finalizeInitialization() {
        getCampaignOptionsPane().setOptions(getCampaign().getCampaignOptions(),
                getCampaign().getRandomSkillPreferences());
        super.finalizeInitialization();
    }
    //endregion Initialization

    //region Button Actions
    @Override
    protected void okAction() {
        getCampaignOptionsPane().updateOptions();
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        return getCampaignOptionsPane().validateOptions(display);
    }

    private void btnSaveActionPerformed() {
        if (validateAction(true).isFailure()) {
            return;
        }
        getCampaignOptionsPane().updateOptions();
        setResult(DialogResult.CONFIRMED);

        final CreateCampaignPresetDialog createCampaignPresetDialog
                = new CreateCampaignPresetDialog(getFrame(), getCampaign(), null);
        if (!createCampaignPresetDialog.showDialog().isConfirmed()) {
            setVisible(false);
            return;
        }
        final CampaignPreset preset = createCampaignPresetDialog.getPreset();
        if (preset == null) {
            setVisible(false);
            return;
        }
        preset.writeToFile(getFrame(),
                FileDialogs.saveCampaignPreset(getFrame(), preset).orElse(null));
        setVisible(false);
    }
    //endregion Button Actions

    public void applyPreset(final @Nullable CampaignPreset preset) {
        getCampaignOptionsPane().applyPreset(preset);
    }
}
