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

import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.gui.enums.CompanyGenerationPanelType;
import mekhq.gui.view.CompanyGenerationOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

public class CompanyGenerationDialog extends BaseDialog {
    //region Variable Declarations
    private CompanyGenerationPanelType currentPanelType;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, ResourceBundle.getBundle("mekhq.resources.GUI", new EncodeControl()),
                "CompanyGenerationDialog.title", campaign);
    }
    //endregion Constructors

    //region Getters/Setters
    public CompanyGenerationPanelType getCurrentPanelType() {
        return currentPanelType;
    }

    public void setCurrentPanelType(CompanyGenerationPanelType currentPanelType) {
        this.currentPanelType = currentPanelType;
    }

    public CompanyGenerationOptionsPanel getCompanyGenerationOptionsPanel() {
        return companyGenerationOptionsPanel;
    }

    public void setCompanyGenerationOptionsPanel(final CompanyGenerationOptionsPanel companyGenerationOptionsPanel) {
        this.companyGenerationOptionsPanel = companyGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    /**
     * @param campaign the campaign with which to create the center pane
     * @return
     */
    @Override
    protected Container createCenterPane(final Campaign campaign) {
        switch (getCurrentPanelType()) {
            case OPTIONS:
            case PERSONNEL:
            case UNITS:
            case UNIT:
            case SPARES:
            case CONTRACTS:
            case FINANCES:
            case OVERVIEW:
            default:
                return new JScrollPane(initializeCompanyGenerationOptionsPanel(campaign));
        }
    }

    private JPanel initializeCompanyGenerationOptionsPanel(final Campaign campaign) {
        setCompanyGenerationOptionsPanel(new CompanyGenerationOptionsPanel(getFrame(), campaign));
        return getCompanyGenerationOptionsPanel();
    }

    @Override
    protected JPanel createButtonPanel() {
        switch (getCurrentPanelType()) {
            case OPTIONS:
            case PERSONNEL:
            case UNITS:
            case UNIT:
            case SPARES:
            case CONTRACTS:
            case FINANCES:
            case OVERVIEW:
            default:
                return initializeCompanyGenerationOptionsButtonPanel();
        }
    }

    private JPanel initializeCompanyGenerationOptionsButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3));

        JButton cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(this::cancelButtonActionPerformed);
        panel.add(cancelButton);

        JButton btnExport = new JButton(resources.getString("Export"));
        btnExport.addActionListener(evt -> getCompanyGenerationOptionsPanel().exportOptionsToXML());

        JButton okButton = new JButton(resources.getString("Generate"));
        okButton.setName("okButton");
        okButton.addActionListener(this::okButtonActionPerformed);
        panel.add(okButton);

        JButton btnRestore = new JButton(resources.getString("RestoreDefaults"));
        btnRestore.setName("btnRestore");
        btnRestore.addActionListener(evt -> getCompanyGenerationOptionsPanel().setOptions(
                MekHQ.getMekHQOptions().getDefaultCompanyGenerationType()));
        panel.add(btnRestore);

        JButton btnImport = new JButton(resources.getString("Import"));
        btnImport.addActionListener(evt -> getCompanyGenerationOptionsPanel().importOptionsFromXML());

        JButton btnApply = new JButton(resources.getString("Apply"));
        /*
        btnApply.addActionListener(evt -> {
            getCompanyGenerationOptionsPanel().apply();
            MekHQ.triggerEvent(new OrganizationChangedEvent(getCompanyGenerationOptionsPanel().getCampaign().getForces()));
            setVisible(false);
        });
        */

        return panel;
    }
    //endregion Initialization

    @Override
    protected void okAction() {

    }

    @Override
    protected void cancelAction() {

    }
}
