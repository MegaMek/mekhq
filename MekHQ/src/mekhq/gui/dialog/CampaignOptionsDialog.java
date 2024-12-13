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
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignPreset;
import mekhq.gui.FileDialogs;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.panes.CampaignOptionsPane;
import mekhq.gui.panes.campaignOptions.SelectPresetDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

import static mekhq.gui.panes.campaignOptions.SelectPresetDialog.PRESET_SELECTION_CANCELLED;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com) (Original Version, now largely CampaignOptionsPane)
 * @author Justin 'Windchild' Bowen (Current Version)
 */
public class CampaignOptionsDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private final Campaign campaign;
    private final boolean startup;
    private final MekHQ application;
    private CampaignOptionsPane campaignOptionsPane;
    //endregion Variable Declarations

    //region Constructors
    public CampaignOptionsDialog(final JFrame frame, final Campaign campaign, final boolean startup) {
        super(frame, true, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog",
                        MekHQ.getMHQOptions().getLocale()),
                "CampaignOptionsDialog", "CampaignOptionsDialog.title");
        this.campaign = campaign;
        this.startup = startup;
        this.application = null;
        initialize();
    }

    /**
     * Allows dialog to be constructed with an owner being another dialog
     */
    public CampaignOptionsDialog(final JDialog owner, final JFrame frame, final Campaign campaign,
                                 final boolean startup, @Nullable final MekHQ application) {
        super(owner, frame, true, ResourceBundle.getBundle("mekhq.resources.CampaignOptionsDialog",
                        MekHQ.getMHQOptions().getLocale()),
                "CampaignOptionsDialog", "CampaignOptionsDialog.title");
        this.campaign = campaign;
        this.startup = startup;
        this.application = application;
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
                this::okButtonPerformedShowStratConNotice));

        panel.add(new MMButton("btnSavePreset", resources, "btnSavePreset.text",
                "btnSavePreset.toolTipText", evt -> btnSaveActionPerformed()));

        panel.add(new MMButton("btnLoadPreset", resources, "btnLoadPreset.text",
                "btnLoadPreset.toolTipText", evt -> {
            final SelectPresetDialog presetSelectionDialog =
                new SelectPresetDialog(getFrame(), true, false);
            if (presetSelectionDialog.getReturnState() != PRESET_SELECTION_CANCELLED) {
                applyPreset(presetSelectionDialog.getSelectedPreset());
            }
        }));

        panel.add(new MMButton("btnCancel", resources, "Cancel.text", "Cancel.toolTipText",
                evt -> {
            if (application != null) {
                application.exit(false);
            } else {
                cancelActionPerformed(evt);
            }}));

        return panel;
    }

    /**
     * Performs the action when the Ok button is clicked, which includes invoking the
     * {@link #okButtonActionPerformed(ActionEvent)} method and then the {@link #showStratConNotice()} method.
     *
     * @param e the ActionEvent triggering this method
     */
    private void okButtonPerformedShowStratConNotice(ActionEvent e) {
        okButtonActionPerformed(e);
        showStratConNotice();
    }

    /**
     * Displays a promo introducing users to StratCon.
     * This method shows the promo only when the Campaign Options pane is closed
     * and the current day is the first day of the campaign.
     */
    private void showStratConNotice() {
        // we don't store whether this dialog has previously appeared,
        // instead we just have it appear only when Campaign Options is closed,
        // the current day is the first day of the campaign, and StratCon is enabled
        if (!campaign.getCampaignOptions().isUseStratCon() || !campaign.getLocalDate().equals(campaign.getCampaignStartDate())) {
            return;
        }

        ImageIcon imageIcon = new ImageIcon("data/images/stratcon/stratConPromo.png");
        JLabel imageLabel = new JLabel(imageIcon);
        JPanel imagePanel = new JPanel(new GridBagLayout());
        imagePanel.add(imageLabel);

        String title = resources.getString("stratConPromo.title");

        String message = resources.getString("stratConPromo.message");
        JLabel messageLabel = new JLabel(message);
        JPanel messagePanel = new JPanel(new GridBagLayout());
        messagePanel.add(messageLabel);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(imagePanel);
        panel.add(messagePanel);

        Object[] options = {
                resources.getString("stratConPromo.button")
        };

        JOptionPane.showOptionDialog(null, panel, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
    }

    @Override
    protected void finalizeInitialization() throws Exception {
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
