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

import megamek.common.Entity;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.generators.companyGeneration.AbstractCompanyGenerator;
import mekhq.campaign.universe.generators.companyGeneration.CompanyGenerationOptions;
import mekhq.gui.enums.CompanyGenerationPanelType;
import mekhq.gui.view.CompanyGenerationOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CompanyGenerationDialog extends BaseButtonDialog {
    //region Variable Declarations
    private Campaign campaign;
    private CompanyGenerationPanelType currentPanelType;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CompanyGenerationDialog.title");
        setCampaign(campaign);
        setCurrentPanelType(CompanyGenerationPanelType.OPTIONS);
        initialize("CompanyGenerationDialog");
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(final Campaign campaign) {
        this.campaign = campaign;
    }

    public CompanyGenerationPanelType getCurrentPanelType() {
        return currentPanelType;
    }

    public void setCurrentPanelType(final CompanyGenerationPanelType currentPanelType) {
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
     * @return the center pane
     */
    @Override
    protected Container createCenterPane() {
        switch (getCurrentPanelType()) {
            case PERSONNEL:
            case UNITS:
            case UNIT:
            case SPARES:
            case CONTRACTS:
            case FINANCES:
            case OVERVIEW:
            case OPTIONS:
            default:
                return new JScrollPane(initializeCompanyGenerationOptionsPanel(getCampaign()));
        }
    }

    private JPanel initializeCompanyGenerationOptionsPanel(final Campaign campaign) {
        setCompanyGenerationOptionsPanel(new CompanyGenerationOptionsPanel(getFrame(), campaign));
        return getCompanyGenerationOptionsPanel();
    }

    @Override
    protected JPanel createButtonPanel() {
        switch (getCurrentPanelType()) {
            case PERSONNEL:
            case UNITS:
            case UNIT:
            case SPARES:
            case CONTRACTS:
            case FINANCES:
            case OVERVIEW:
            case OPTIONS:
            default:
                return initializeCompanyGenerationOptionsButtonPanel();
        }
    }

    private JPanel initializeCompanyGenerationOptionsButtonPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3));

        JButton cancelButton = new JButton(resources.getString("Cancel"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(this::cancelActionPerformed);
        panel.add(cancelButton);

        JButton btnExport = new JButton(resources.getString("Export"));
        btnExport.addActionListener(evt -> getCompanyGenerationOptionsPanel().exportOptionsToXML());
        panel.add(btnExport);

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
        panel.add(btnImport);

        JButton btnApply = new JButton(resources.getString("Apply"));
        /*
        btnApply.addActionListener(evt -> {
            getCompanyGenerationOptionsPanel().apply();
            MekHQ.triggerEvent(new OrganizationChangedEvent(getCompanyGenerationOptionsPanel().getCampaign().getForces()));
            setVisible(false);
        });
        */
        panel.add(btnApply);

        return panel;
    }
    //endregion Initialization

    @Override
    protected void okAction() {
        final CompanyGenerationOptions options = getCompanyGenerationOptionsPanel().createOptionsFromPanel();
        final AbstractCompanyGenerator generator = options.getType().getGenerator(getCampaign(), options);
        final List<Person> combatPersonnel = generator.generateCombatPersonnel(getCampaign());
        final List<Person> supportPersonnel = generator.generateSupportPersonnel(getCampaign());
        final List<Entity> entities = generator.generateUnits(getCampaign(), combatPersonnel);
        final List<Entity> mothballedEntities = generator.generateMothballedEntities(getCampaign(), entities);
        final Contract contract = null;
        generator.applyToCampaign(getCampaign(), combatPersonnel, supportPersonnel, entities, mothballedEntities, contract);
    }
}
