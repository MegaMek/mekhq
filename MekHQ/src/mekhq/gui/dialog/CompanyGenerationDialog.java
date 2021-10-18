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
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator;
import mekhq.campaign.universe.generators.companyGenerators.CompanyGenerationOptions;
import mekhq.campaign.universe.generators.companyGenerators.CompanyGenerationPersonTracker;
import mekhq.gui.baseComponents.AbstractMHQButtonDialog;
import mekhq.gui.enums.CompanyGenerationPanelType;
import mekhq.gui.panels.CompanyGenerationOptionsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class CompanyGenerationDialog extends AbstractMHQButtonDialog {
    //region Variable Declarations
    private Campaign campaign;
    private CompanyGenerationPanelType currentPanelType;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CompanyGenerationDialog", "CompanyGenerationDialog.title");
        setCampaign(campaign);
        setCurrentPanelType(CompanyGenerationPanelType.OPTIONS);
        initialize();
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

        JButton cancelButton = new JButton(resources.getString("Cancel.text"));
        cancelButton.setName("cancelButton");
        cancelButton.addActionListener(this::cancelActionPerformed);
        panel.add(cancelButton);

        JButton btnExport = new JButton(resources.getString("Export.text"));
        btnExport.addActionListener(evt -> getCompanyGenerationOptionsPanel().exportOptionsToXML());
        panel.add(btnExport);

        JButton okButton = new JButton(resources.getString("Generate.text"));
        okButton.setName("okButton");
        okButton.addActionListener(this::okButtonActionPerformed);
        panel.add(okButton);

        JButton btnRestore = new JButton(resources.getString("RestoreDefaults.text"));
        btnRestore.setName("btnRestore");
        btnRestore.addActionListener(evt -> getCompanyGenerationOptionsPanel().setOptions(
                MekHQ.getMekHQOptions().getDefaultCompanyGenerationMethod()));
        panel.add(btnRestore);

        JButton btnImport = new JButton(resources.getString("Import.text"));
        btnImport.addActionListener(evt -> getCompanyGenerationOptionsPanel().importOptionsFromXML());
        panel.add(btnImport);

        JButton btnApply = new JButton(resources.getString("Apply.text"));
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
        final AbstractCompanyGenerator generator = options.getMethod().getGenerator(getCampaign(), options);
        generator.applyPhaseZeroToCampaign(getCampaign());

        final List<CompanyGenerationPersonTracker> trackers = generator.generatePersonnel(getCampaign());
        generator.generateUnitGenerationParameters(trackers);
        generator.generateEntities(getCampaign(), trackers);
        final List<Unit> units = generator.applyPhaseOneToCampaign(getCampaign(), trackers);

        final List<Entity> mothballedEntities = generator.generateMothballedEntities(getCampaign(), trackers);
        final List<Part> parts = generator.generateSpareParts(units);
        final List<Armor> armour = generator.generateArmour(units);
        final List<AmmoStorage> ammunition = generator.generateAmmunition(getCampaign(), units);
        units.addAll(generator.applyPhaseTwoToCampaign(getCampaign(), mothballedEntities, parts, armour, ammunition));

        final Contract contract = null;
        generator.applyPhaseThreeToCampaign(getCampaign(), trackers, units, parts, armour, ammunition, contract);
    }
}
