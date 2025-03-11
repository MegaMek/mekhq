/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.gui.dialog;

import megamek.client.ui.baseComponents.MMButton;
import megamek.client.ui.enums.ValidationState;
import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.OrganizationChangedEvent;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.autoAwards.AutoAwardsController;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationPersonTracker;
import mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator;
import mekhq.gui.baseComponents.AbstractMHQValidationButtonDialog;
import mekhq.gui.panels.CompanyGenerationOptionsPanel;
import mekhq.gui.utilities.JScrollPaneWithSpeed;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * This is currently just a temporary dialog over the CompanyGenerationOptionsPanel.
 * Wave 5 will be when this gets redone to be far nicer and more customizable.
 * @author Justin "Windchild" Bowen
 */
public class CompanyGenerationDialog extends AbstractMHQValidationButtonDialog {
    //region Variable Declarations
    private Campaign campaign;
    private CompanyGenerationOptions companyGenerationOptions;
    private CompanyGenerationOptionsPanel companyGenerationOptionsPanel;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(final JFrame frame, final Campaign campaign) {
        super(frame, "CompanyGenerationDialog", "CompanyGenerationDialog.title");
        setCampaign(campaign);
        setCompanyGenerationOptions(null);
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

    public @Nullable CompanyGenerationOptions getCompanyGenerationOptions() {
        return companyGenerationOptions;
    }

    public void setCompanyGenerationOptions(final @Nullable CompanyGenerationOptions companyGenerationOptions) {
        this.companyGenerationOptions = companyGenerationOptions;
    }

    public CompanyGenerationOptionsPanel getCompanyGenerationOptionsPanel() {
        return companyGenerationOptionsPanel;
    }

    public void setCompanyGenerationOptionsPanel(final CompanyGenerationOptionsPanel companyGenerationOptionsPanel) {
        this.companyGenerationOptionsPanel = companyGenerationOptionsPanel;
    }
    //endregion Getters/Setters

    //region Initialization
    @Override
    protected Container createCenterPane() {
        setCompanyGenerationOptionsPanel(new CompanyGenerationOptionsPanel(getFrame(), getCampaign(),
                getCompanyGenerationOptions()));
        return new JScrollPaneWithSpeed(getCompanyGenerationOptionsPanel());
    }

    @Override
    protected JPanel createButtonPanel() {
        final JPanel panel = new JPanel(new GridLayout(2, 3));

        setOkButton(new MMButton("btnGenerate", resources, "Generate.text",
                "CompanyGenerationDialog.btnGenerate.toolTipText", this::okButtonActionPerformed));
        panel.add(getOkButton());

        panel.add(new MMButton("btnApply", resources, "Apply.text",
                "CompanyGenerationDialog.btnApply.toolTipText", this::okButtonActionPerformed));

        panel.add(new MMButton("btnCancel", resources, "Cancel.text",
                "Cancel.toolTipText", this::cancelActionPerformed));

        panel.add(new MMButton("btnRestore", resources, "RestoreDefaults.text",
                "CompanyGenerationDialog.btnRestore.toolTipText",
                evt -> getCompanyGenerationOptionsPanel().setOptions()));

        panel.add(new MMButton("btnImport", resources, "Import.text",
                "CompanyGenerationDialog.btnImport.toolTipText",
                evt -> getCompanyGenerationOptionsPanel().importOptionsFromXML()));

        panel.add(new MMButton("btnExport", resources, "Export.text",
                "CompanyGenerationDialog.btnExport.toolTipText",
                evt -> getCompanyGenerationOptionsPanel().exportOptionsToXML()));

        return panel;
    }
    //endregion Initialization

    @Override
    protected void okAction() {
        final CompanyGenerationOptions options = getCompanyGenerationOptionsPanel().createOptionsFromPanel();
        final AbstractCompanyGenerator generator = options.getMethod().getGenerator(getCampaign(), options);

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

        MekHQ.triggerEvent(new OrganizationChangedEvent(getCampaign(), getCompanyGenerationOptionsPanel().getCampaign().getForces()));

        if (campaign.getCampaignOptions().isEnableAutoAwards()) {
            AutoAwardsController autoAwardsController = new AutoAwardsController();
            autoAwardsController.ManualController(campaign, false);
        }

        ReputationController reputationController = new ReputationController();
        reputationController.initializeReputation(campaign);
        campaign.setReputation(reputationController);
    }

    @Override
    protected ValidationState validateAction(final boolean display) {
        return getCompanyGenerationOptionsPanel().validateOptions(display);
    }
}
