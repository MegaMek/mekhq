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
package mekhq.gui.campaignOptions.contents;

import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.LEGACY_RULE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.MILESTONE_BEFORE_METADATA;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.createTipPanelUpdater;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getImageDirectory;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.getMetadata;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import mekhq.gui.campaignOptions.CampaignOptionFlag;
import mekhq.gui.campaignOptions.components.CampaignOptionsCheckBox;
import mekhq.gui.campaignOptions.components.CampaignOptionsFormPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsHeaderPanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsLabel;
import mekhq.gui.campaignOptions.components.CampaignOptionsPagePanel;
import mekhq.gui.campaignOptions.components.CampaignOptionsSpinner;

/**
 * The {@code MedicalPage} class builds and manages the Medical leaf page of the Campaign Options dialog. It owns the
 * widgets for medical configuration - healing periods, advanced medical rule toggles, patient capacity, and MASH
 * theatre options - and synchronises them with a shared {@link PersonnelOptionsModel}.
 *
 * <p>This view is a sub-component of {@link PersonnelPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code PersonnelPages}, while this class is responsible only for constructing the Medical panel and
 * copying medical values to and from the model. The page is built lazily; until
 * {@link #createPanel(PersonnelOptionsModel)} is called, {@link #readFromModel(PersonnelOptionsModel)} and
 * {@link #writeToModel(PersonnelOptionsModel)} are no-ops.</p>
 */
class MedicalPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    private static final int CONTROL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_CONTROL_WIDTH;

    private CampaignOptionsHeaderPanel medicalHeader;
    private JCheckBox chkUseAdvancedMedical;
    private JLabel lblHealWaitingPeriod;
    private JSpinner spnHealWaitingPeriod;
    private JLabel lblNaturalHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JLabel lblMinimumHitsForVehicles;
    private JSpinner spnMinimumHitsForVehicles;
    private JCheckBox chkUseRandomHitsForVehicles;
    private JCheckBox chkUseTougherHealing;
    private JCheckBox chkUseAlternativeAdvancedMedical;
    private JCheckBox chkUseAlternativeAdvancedMedicalFewerPermanentInjuries;
    private JLabel lblAlternativeAdvancedMedicalHealingTimeMultiplier;
    private JSpinner spnAlternativeAdvancedMedicalHealingTimeMultiplier;
    private JCheckBox chkUseRandomDiseases;
    private JLabel lblMaximumPatients;
    private JSpinner spnMaximumPatients;
    private JCheckBox chkDoctorsUseAdministration;
    private JCheckBox chkUseUsefulMedics;
    private JCheckBox chkUseMASHTheatres;
    private JLabel lblMASHTheatreCapacity;
    private JSpinner spnMASHTheatreCapacity;

    private boolean created;

    /**
     * Builds the Medical page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared personnel options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Medical Page
     */
    @Nonnull
    JPanel createPanel(@Nullable PersonnelOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_duchy_of_tamarind_abbey.png";
        medicalHeader = new CampaignOptionsHeaderPanel("MedicalPage", imageAddress);

        // Contents
        chkUseAdvancedMedical = new CampaignOptionsCheckBox("UseAdvancedMedical",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                    CampaignOptionFlag.DOCUMENTED));
        chkUseAdvancedMedical.addMouseListener(createTipPanelUpdater("UseAdvancedMedical"));

        lblHealWaitingPeriod = new CampaignOptionsLabel("HealWaitingPeriod");
        lblHealWaitingPeriod.addMouseListener(createTipPanelUpdater("HealWaitingPeriod"));
        spnHealWaitingPeriod = new CampaignOptionsSpinner("HealWaitingPeriod", 1, 1, 30, 1);
        spnHealWaitingPeriod.addMouseListener(createTipPanelUpdater("HealWaitingPeriod"));

        lblNaturalHealWaitingPeriod = new CampaignOptionsLabel("NaturalHealWaitingPeriod");
        lblNaturalHealWaitingPeriod
              .addMouseListener(createTipPanelUpdater("NaturalHealWaitingPeriod"));
        spnNaturalHealWaitingPeriod = new CampaignOptionsSpinner("NaturalHealWaitingPeriod", 1, 1, 365, 1);
        spnNaturalHealWaitingPeriod
              .addMouseListener(createTipPanelUpdater("NaturalHealWaitingPeriod"));

        lblMinimumHitsForVehicles = new CampaignOptionsLabel("MinimumHitsForVehicles");
        lblMinimumHitsForVehicles.addMouseListener(createTipPanelUpdater("MinimumHitsForVehicles"));
        spnMinimumHitsForVehicles = new CampaignOptionsSpinner("MinimumHitsForVehicles", 1, 1, 5, 1);
        spnMinimumHitsForVehicles.addMouseListener(createTipPanelUpdater("MinimumHitsForVehicles"));

        chkUseRandomHitsForVehicles = new CampaignOptionsCheckBox("UseRandomHitsForVehicles");
        chkUseRandomHitsForVehicles
              .addMouseListener(createTipPanelUpdater("UseRandomHitsForVehicles"));

        chkUseTougherHealing = new CampaignOptionsCheckBox("UseTougherHealing",
              getMetadata(null, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseTougherHealing.addMouseListener(createTipPanelUpdater("UseTougherHealing"));

        chkUseAlternativeAdvancedMedical = new CampaignOptionsCheckBox("UseAlternativeAdvancedMedical",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                    CampaignOptionFlag.IMPORTANT));
        chkUseAlternativeAdvancedMedical.addMouseListener(createTipPanelUpdater("UseAlternativeAdvancedMedical"));

        chkUseAlternativeAdvancedMedicalFewerPermanentInjuries = new CampaignOptionsCheckBox(
              "UseAlternativeAdvancedMedicalFewerPermanentInjuries",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                    CampaignOptionFlag.IMPORTANT));
        chkUseAlternativeAdvancedMedicalFewerPermanentInjuries.addMouseListener(createTipPanelUpdater(
              "UseAlternativeAdvancedMedicalFewerPermanentInjuries"));

        lblAlternativeAdvancedMedicalHealingTimeMultiplier = new CampaignOptionsLabel(
              "AlternativeAdvancedMedicalHealingTimeMultiplier");
        lblAlternativeAdvancedMedicalHealingTimeMultiplier.addMouseListener(createTipPanelUpdater(
              "AlternativeAdvancedMedicalHealingTimeMultiplier"));
        spnAlternativeAdvancedMedicalHealingTimeMultiplier = new CampaignOptionsSpinner(
              "AlternativeAdvancedMedicalHealingTimeMultiplier",
              1.0,
              0.01,
              10,
              0.01);
        spnAlternativeAdvancedMedicalHealingTimeMultiplier.addMouseListener(createTipPanelUpdater(
              "AlternativeAdvancedMedicalHealingTimeMultiplier"));

        chkUseRandomDiseases = new CampaignOptionsCheckBox("UseRandomDiseases",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM,
                    CampaignOptionFlag.IMPORTANT));
        chkUseRandomDiseases.addMouseListener(createTipPanelUpdater("UseRandomDiseases"));

        lblMaximumPatients = new CampaignOptionsLabel("MaximumPatients");
        lblMaximumPatients.addMouseListener(createTipPanelUpdater("MaximumPatients"));
        spnMaximumPatients = new CampaignOptionsSpinner("MaximumPatients", 25, 1, 100, 1);
        spnMaximumPatients.addMouseListener(createTipPanelUpdater("MaximumPatients"));

        chkDoctorsUseAdministration = new CampaignOptionsCheckBox("DoctorsUseAdministration",
              getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkDoctorsUseAdministration
              .addMouseListener(createTipPanelUpdater("DoctorsUseAdministration"));

        chkUseUsefulMedics = new CampaignOptionsCheckBox("UseUsefulMedics",
              getMetadata(MILESTONE_BEFORE_METADATA, CampaignOptionFlag.CUSTOM_SYSTEM));
        chkUseUsefulMedics.addMouseListener(createTipPanelUpdater("UseUsefulMedics"));

        chkUseMASHTheatres = new CampaignOptionsCheckBox("UseMASHTheatres",
              getMetadata(MILESTONE_BEFORE_METADATA));
        chkUseMASHTheatres.addMouseListener(createTipPanelUpdater("UseMASHTheatres"));

        lblMASHTheatreCapacity = new CampaignOptionsLabel("MASHTheatreCapacity",
              getMetadata(MILESTONE_BEFORE_METADATA));
        lblMASHTheatreCapacity.addMouseListener(createTipPanelUpdater("MASHTheatreCapacity"));
        spnMASHTheatreCapacity = new CampaignOptionsSpinner("MASHTheatreCapacity", 25, 1, 100, 1);
        spnMASHTheatreCapacity.addMouseListener(createTipPanelUpdater("MASHTheatreCapacity"));

        // Layout the Panels
        final CampaignOptionsFormPanel medicalCapacityPanel = new CampaignOptionsFormPanel("MedicalCapacityPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        medicalCapacityPanel.addRow(lblMaximumPatients, spnMaximumPatients);
        medicalCapacityPanel.addCheckBoxGrid(2,
              chkDoctorsUseAdministration,
              chkUseUsefulMedics,
              chkUseMASHTheatres);
        medicalCapacityPanel.addRow(lblMASHTheatreCapacity, spnMASHTheatreCapacity);

        final CampaignOptionsFormPanel healingChecksPanel = new CampaignOptionsFormPanel("HealingChecksPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        healingChecksPanel.addRow(lblHealWaitingPeriod, spnHealWaitingPeriod);
        healingChecksPanel.addRow(lblNaturalHealWaitingPeriod, spnNaturalHealWaitingPeriod);
        healingChecksPanel.addCheckBox(chkUseRandomHitsForVehicles);
        healingChecksPanel.addRow(lblMinimumHitsForVehicles, spnMinimumHitsForVehicles);

        final CampaignOptionsFormPanel advancedMedicalRulesPanel = new CampaignOptionsFormPanel(
              "AdvancedMedicalRulesPanel",
              LABEL_COLUMN_WIDTH,
              CONTROL_COLUMN_WIDTH);
        advancedMedicalRulesPanel.addCheckBoxGrid(2,
              chkUseAdvancedMedical,
              chkUseTougherHealing,
              chkUseAlternativeAdvancedMedical,
              chkUseAlternativeAdvancedMedicalFewerPermanentInjuries,
              chkUseRandomDiseases);
        advancedMedicalRulesPanel.addRow(lblAlternativeAdvancedMedicalHealingTimeMultiplier,
              spnAlternativeAdvancedMedicalHealingTimeMultiplier);
        JPanel panel = CampaignOptionsPagePanel.builder("MedicalPage", "MedicalPage", imageAddress)
                             .header(medicalHeader)
                             .quote("medicalPage")
                             .section("lblMedicalCapacityPanel.text",
                                   "lblMedicalCapacityPanel.summary",
                                   medicalCapacityPanel)
                             .section("lblHealingChecksPanel.text", "lblHealingChecksPanel.summary", healingChecksPanel)
                             .section("lblAdvancedMedicalRulesPanel.text",
                                   "lblAdvancedMedicalRulesPanel.summary",
                                   advancedMedicalRulesPanel)
                             .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    /**
     * Copies medical values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared personnel options model to read values from
     */
    void readFromModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseAdvancedMedical.setSelected(model.useAdvancedMedical);
        spnHealWaitingPeriod.setValue(model.healingWaitingPeriod);
        spnNaturalHealWaitingPeriod.setValue(model.naturalHealingWaitingPeriod);
        spnMinimumHitsForVehicles.setValue(model.minimumHitsForVehicles);
        chkUseRandomHitsForVehicles.setSelected(model.useRandomHitsForVehicles);
        chkUseTougherHealing.setSelected(model.tougherHealing);
        chkUseAlternativeAdvancedMedical.setSelected(model.useAlternativeAdvancedMedical);
        chkUseAlternativeAdvancedMedicalFewerPermanentInjuries.setSelected(model.useAlternativeAdvancedMedicalFewerPermanentInjuries);
        spnAlternativeAdvancedMedicalHealingTimeMultiplier.setValue(model.alternativeAdvancedMedicalHealingTimeMultiplier);
        chkUseRandomDiseases.setSelected(model.useRandomDiseases);
        spnMaximumPatients.setValue(model.maximumPatients);
        chkDoctorsUseAdministration.setSelected(model.doctorsUseAdministration);
        chkUseUsefulMedics.setSelected(model.useUsefulMedics);
        chkUseMASHTheatres.setSelected(model.useMASHTheatres);
        spnMASHTheatreCapacity.setValue(model.mashTheatreCapacity);
    }

    /**
     * Copies medical values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared personnel options model to write values into
     */
    void writeToModel(@Nullable PersonnelOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useAdvancedMedical = chkUseAdvancedMedical.isSelected();
        model.healingWaitingPeriod = (int) spnHealWaitingPeriod.getValue();
        model.naturalHealingWaitingPeriod = (int) spnNaturalHealWaitingPeriod.getValue();
        model.minimumHitsForVehicles = (int) spnMinimumHitsForVehicles.getValue();
        model.useRandomHitsForVehicles = chkUseRandomHitsForVehicles.isSelected();
        model.tougherHealing = chkUseTougherHealing.isSelected();
        model.useAlternativeAdvancedMedical = chkUseAlternativeAdvancedMedical.isSelected();
        model.useAlternativeAdvancedMedicalFewerPermanentInjuries = chkUseAlternativeAdvancedMedicalFewerPermanentInjuries.isSelected();
        model.alternativeAdvancedMedicalHealingTimeMultiplier = (double) spnAlternativeAdvancedMedicalHealingTimeMultiplier.getValue();
        model.useRandomDiseases = chkUseRandomDiseases.isSelected();
        model.maximumPatients = (int) spnMaximumPatients.getValue();
        model.doctorsUseAdministration = chkDoctorsUseAdministration.isSelected();
        model.useUsefulMedics = chkUseUsefulMedics.isSelected();
        model.useMASHTheatres = chkUseMASHTheatres.isSelected();
        model.mashTheatreCapacity = (int) spnMASHTheatreCapacity.getValue();
    }
}
