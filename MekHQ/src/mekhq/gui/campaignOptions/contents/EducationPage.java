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
 * The {@code EducationPage} class builds and manages the Education leaf page of the Biography section of the Campaign
 * Options dialog. It owns the widgets for the education module - curriculum XP rates, academy sets, XP and skill
 * bonuses, dropout chances, and academy accidents - and synchronises them with a shared {@link BiographyOptionsModel}.
 *
 * <p>This view is a sub-component of {@link BiographyPages}: the model snapshot and the overall load/apply lifecycle
 * still live on {@code BiographyPages}, while this class is responsible only for constructing the Education panel and
 * copying its values to and from the model. The page is built lazily; until {@link #createPanel(BiographyOptionsModel)}
 * is called, {@link #readFromModel(BiographyOptionsModel)} and {@link #writeToModel(BiographyOptionsModel)} are
 * no-ops.</p>
 */
class EducationPage {
    private static final int LABEL_COLUMN_WIDTH = CampaignOptionsFormPanel.DEFAULT_LABEL_WIDTH;
    // Wider than the default control column because the Biography combo boxes need the extra room.
    private static final int CONTROL_COLUMN_WIDTH = 240;

    private CampaignOptionsHeaderPanel educationHeader;
    private JCheckBox chkUseEducationModule;
    private JLabel lblCurriculumXpRate;
    private JSpinner spnCurriculumXpRate;
    private JLabel lblMaximumJumpCount;
    private JSpinner spnMaximumJumpCount;
    private JCheckBox chkUseReeducationCamps;
    private JCheckBox chkEnableOverrideRequirements;
    private JCheckBox chkShowIneligibleAcademies;
    private JLabel lblEntranceExamBaseTargetNumber;
    private JSpinner spnEntranceExamBaseTargetNumber;
    private JPanel pnlEnableStandardSets;
    private JCheckBox chkEnableLocalAcademies;
    private JCheckBox chkEnablePrestigiousAcademies;
    private JCheckBox chkEnableUnitEducation;
    private JPanel pnlXpAndSkillBonuses;
    private JCheckBox chkEnableBonuses;
    private JLabel lblFacultyXpMultiplier;
    private JSpinner spnFacultyXpMultiplier;
    private JPanel pnlDropoutChance;
    private JLabel lblAdultDropoutChance;
    private JSpinner spnAdultDropoutChance;
    private JLabel lblChildrenDropoutChance;
    private JSpinner spnChildrenDropoutChance;
    private JPanel pnlAccidentsAndEvents;
    private JCheckBox chkAllAges;
    private JLabel lblMilitaryAcademyAccidents;
    private JSpinner spnMilitaryAcademyAccidents;

    private boolean created;

    /**
     * Builds the Education page, populates its controls from the supplied model, and returns the assembled panel.
     *
     * @param model the shared biography options model to populate the freshly built controls from
     *
     * @return a {@link JPanel} representing the Education Page
     */
    @Nonnull JPanel createPanel(@Nullable BiographyOptionsModel model) {
        // Header
        String imageAddress = getImageDirectory() + "logo_taurian_concordat.png";
        educationHeader = new CampaignOptionsHeaderPanel("EducationPage", imageAddress);

        // Contents
        chkUseEducationModule = new CampaignOptionsCheckBox("UseEducationModule");
        chkUseEducationModule.addMouseListener(createTipPanelUpdater("UseEducationModule"));

        lblCurriculumXpRate = new CampaignOptionsLabel("CurriculumXpRate");
        lblCurriculumXpRate.addMouseListener(createTipPanelUpdater("CurriculumXpRate"));
        spnCurriculumXpRate = new CampaignOptionsSpinner("CurriculumXpRate", 3, 1, 10, 1);
        spnCurriculumXpRate.addMouseListener(createTipPanelUpdater("CurriculumXpRate"));

        lblMaximumJumpCount = new CampaignOptionsLabel("MaximumJumpCount");
        lblMaximumJumpCount.addMouseListener(createTipPanelUpdater("MaximumJumpCount"));
        spnMaximumJumpCount = new CampaignOptionsSpinner("MaximumJumpCount", 5, 1, 200, 1);
        spnMaximumJumpCount.addMouseListener(createTipPanelUpdater("MaximumJumpCount"));

        chkUseReeducationCamps = new CampaignOptionsCheckBox("UseReeducationCamps");
        chkUseReeducationCamps.addMouseListener(createTipPanelUpdater("UseReeducationCamps"));

        chkEnableOverrideRequirements = new CampaignOptionsCheckBox("EnableOverrideRequirements",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        chkEnableOverrideRequirements.addMouseListener(createTipPanelUpdater("EnableOverrideRequirements"));

        chkShowIneligibleAcademies = new CampaignOptionsCheckBox("ShowIneligibleAcademies");
        chkShowIneligibleAcademies.addMouseListener(createTipPanelUpdater("ShowIneligibleAcademies"));

        lblEntranceExamBaseTargetNumber = new CampaignOptionsLabel("EntranceExamBaseTargetNumber",
                getMetadata(LEGACY_RULE_BEFORE_METADATA, CampaignOptionFlag.IMPORTANT));
        lblEntranceExamBaseTargetNumber.addMouseListener(createTipPanelUpdater("EntranceExamBaseTargetNumber"));
        spnEntranceExamBaseTargetNumber = new CampaignOptionsSpinner("EntranceExamBaseTargetNumber", 14, 0, 20, 1);
        spnEntranceExamBaseTargetNumber.addMouseListener(createTipPanelUpdater("EntranceExamBaseTargetNumber"));

        JPanel educationOptionsPanel = createEducationOptionsPanel();
        pnlEnableStandardSets = createEnableStandardSetsPanel();
        pnlXpAndSkillBonuses = createXpAndSkillBonusesPanel();
        pnlDropoutChance = createDropoutChancePanel();
        pnlAccidentsAndEvents = createAccidentsAndEventsPanel();
        JPanel panel = CampaignOptionsPagePanel.builder("EducationPage", "EducationPage", imageAddress)
            .header(educationHeader)
            .quote("educationPage")
            .section("lblEducationPage.text", "lblEducationPage.summary", educationOptionsPanel)
            .section("lblEnableStandardSetsPanel.text", "lblEnableStandardSetsPanel.summary", pnlEnableStandardSets)
            .section("lblXpAndSkillBonusesPanel.text", "lblXpAndSkillBonusesPanel.summary", pnlXpAndSkillBonuses)
            .section("lblDropoutChancePanel.text", "lblDropoutChancePanel.summary", pnlDropoutChance)
            .section("lblAccidentsAndEventsPanel.text", "lblAccidentsAndEventsPanel.summary", pnlAccidentsAndEvents)
            .build();

        created = true;
        readFromModel(model);

        return panel;
    }

    private @Nonnull JPanel createEducationOptionsPanel() {
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("EducationOptionsPanel",
                LABEL_COLUMN_WIDTH,
                CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkUseEducationModule);
        panel.addRow(lblCurriculumXpRate, spnCurriculumXpRate);
        panel.addRow(lblMaximumJumpCount, spnMaximumJumpCount);
        panel.addCheckBoxGrid(2,
            chkUseReeducationCamps,
            chkEnableOverrideRequirements,
            chkShowIneligibleAcademies);
        panel.addRow(lblEntranceExamBaseTargetNumber, spnEntranceExamBaseTargetNumber);

        return panel;
    }

    /**
     * Creates a panel for enabling different education-related academy sets.
     * <p>
     * This includes options to toggle various academy types:
     * <p>
     * <li>Local academies.</li>
     * <li>Prestigious academies.</li>
     * <li>Unit-based education academies.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the Enable Standard Sets UI components.
     */
    private @Nonnull JPanel createEnableStandardSetsPanel() {
        chkEnableLocalAcademies = new CampaignOptionsCheckBox("EnableLocalAcademies");
        chkEnableLocalAcademies.addMouseListener(createTipPanelUpdater("EnableLocalAcademies"));
        chkEnablePrestigiousAcademies = new CampaignOptionsCheckBox("EnablePrestigiousAcademies");
        chkEnablePrestigiousAcademies.addMouseListener(createTipPanelUpdater("EnablePrestigiousAcademies"));
        chkEnableUnitEducation = new CampaignOptionsCheckBox("EnableUnitEducation");
        chkEnableUnitEducation.addMouseListener(createTipPanelUpdater("EnableUnitEducation"));

        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("EnableStandardSetsPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBoxGrid(2,
                chkEnableLocalAcademies,
                chkEnablePrestigiousAcademies,
                chkEnableUnitEducation);

        return panel;
    }

    /**
     * Creates a panel for configuring experience gain and skill bonuses.
     * <p>
     * This includes:
     * <p>
     * <li>Option to enable or disable bonuses.</li>
     * <li>Setting the faculty XP multiplier.</li>
     * </p>
     *
     * @return A {@code JPanel} for managing XP rates and skill bonuses.
     */
    private @Nonnull JPanel createXpAndSkillBonusesPanel() {
        // Contents
        chkEnableBonuses = new CampaignOptionsCheckBox("EnableBonuses");
        chkEnableBonuses.addMouseListener(createTipPanelUpdater("EnableBonuses"));

        lblFacultyXpMultiplier = new CampaignOptionsLabel("FacultyXpMultiplier");
        lblFacultyXpMultiplier.addMouseListener(createTipPanelUpdater("FacultyXpMultiplier"));
        spnFacultyXpMultiplier = new CampaignOptionsSpinner("FacultyXpMultiplier", 1.00, 0.00, 10.00, 0.01);
        spnFacultyXpMultiplier.addMouseListener(createTipPanelUpdater("FacultyXpMultiplier"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("XpAndSkillBonusesPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkEnableBonuses);
        panel.addRow(lblFacultyXpMultiplier, spnFacultyXpMultiplier);

        return panel;
    }

    /**
     * Creates a panel for configuring dropout chances for academies.
     * <p>
     * This includes:
     * <p>
     * <li>Setting the dropout chance for adults.</li>
     * <li>Setting the dropout chance for children.</li>
     * </p>
     *
     * @return A {@code JPanel} for managing dropout change settings.
     */
    private @Nonnull JPanel createDropoutChancePanel() {
        // Contents
        lblAdultDropoutChance = new CampaignOptionsLabel("AdultDropoutChance");
        lblAdultDropoutChance.addMouseListener(createTipPanelUpdater("AdultDropoutChance"));
        spnAdultDropoutChance = new CampaignOptionsSpinner("AdultDropoutChance", 1000, 0, 100000, 1);
        spnAdultDropoutChance.addMouseListener(createTipPanelUpdater("AdultDropoutChance"));

        lblChildrenDropoutChance = new CampaignOptionsLabel("ChildrenDropoutChance");
        lblChildrenDropoutChance.addMouseListener(createTipPanelUpdater("ChildrenDropoutChance"));
        spnChildrenDropoutChance = new CampaignOptionsSpinner("ChildrenDropoutChance", 10000, 0, 100000, 1);
        spnChildrenDropoutChance.addMouseListener(createTipPanelUpdater("ChildrenDropoutChance"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("DropoutChancePanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addRow(lblAdultDropoutChance, spnAdultDropoutChance);
        panel.addRow(lblChildrenDropoutChance, spnChildrenDropoutChance);

        return panel;
    }

    /**
     * Creates a panel for configuring accidents and events related to military
     * academies.
     * <p>
     * This includes:
     * <p>
     * <li>Toggling settings for all-age accidents.</li>
     * <li>Configuring the frequency of military academy accidents.</li>
     * </p>
     *
     * @return A {@code JPanel} containing the accidents and events configuration
     *         UI.
     */
    private @Nonnull JPanel createAccidentsAndEventsPanel() {
        // Contents
        chkAllAges = new CampaignOptionsCheckBox("AllAges");
        chkAllAges.addMouseListener(createTipPanelUpdater("AllAges"));

        lblMilitaryAcademyAccidents = new CampaignOptionsLabel("MilitaryAcademyAccidents");
        lblMilitaryAcademyAccidents.addMouseListener(createTipPanelUpdater("MilitaryAcademyAccidents"));
        spnMilitaryAcademyAccidents = new CampaignOptionsSpinner("MilitaryAcademyAccidents", 10000, 0, 100000, 1);
        spnMilitaryAcademyAccidents.addMouseListener(createTipPanelUpdater("MilitaryAcademyAccidents"));

        // Layout the Panel
        final CampaignOptionsFormPanel panel = new CampaignOptionsFormPanel("AccidentsAndEventsPanel",
            LABEL_COLUMN_WIDTH,
            CONTROL_COLUMN_WIDTH);
        panel.addCheckBox(chkAllAges);
        panel.addRow(lblMilitaryAcademyAccidents, spnMilitaryAcademyAccidents);

        return panel;
    }

    /**
     * Copies education values from the shared model into this page's controls. This is a no-op until the page has been
     * built.
     *
     * @param model the shared biography options model to read values from
     */
    void readFromModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        chkUseEducationModule.setSelected(model.useEducationModule);
        spnCurriculumXpRate.setValue(model.curriculumXpRate);
        spnMaximumJumpCount.setValue(model.maximumJumpCount);
        chkUseReeducationCamps.setSelected(model.useReeducationCamps);
        chkEnableOverrideRequirements.setSelected(model.enableOverrideRequirements);
        chkShowIneligibleAcademies.setSelected(model.enableShowIneligibleAcademies);
        spnEntranceExamBaseTargetNumber.setValue(model.entranceExamBaseTargetNumber);
        chkEnableLocalAcademies.setSelected(model.enableLocalAcademies);
        chkEnablePrestigiousAcademies.setSelected(model.enablePrestigiousAcademies);
        chkEnableUnitEducation.setSelected(model.enableUnitEducation);
        chkEnableBonuses.setSelected(model.enableBonuses);
        spnFacultyXpMultiplier.setValue(model.facultyXpRate);
        spnAdultDropoutChance.setValue(model.adultDropoutChance);
        spnChildrenDropoutChance.setValue(model.childrenDropoutChance);
        chkAllAges.setSelected(model.allAges);
        spnMilitaryAcademyAccidents.setValue(model.militaryAcademyAccidents);
    }

    /**
     * Copies education values from this page's controls into the shared model. This is a no-op until the page has been
     * built.
     *
     * @param model the shared biography options model to write values into
     */
    void writeToModel(@Nullable BiographyOptionsModel model) {
        if (!created || model == null) {
            return;
        }

        model.useEducationModule = chkUseEducationModule.isSelected();
        model.curriculumXpRate = (int) spnCurriculumXpRate.getValue();
        model.maximumJumpCount = (int) spnMaximumJumpCount.getValue();
        model.useReeducationCamps = chkUseReeducationCamps.isSelected();
        model.enableOverrideRequirements = chkEnableOverrideRequirements.isSelected();
        model.enableShowIneligibleAcademies = chkShowIneligibleAcademies.isSelected();
        model.entranceExamBaseTargetNumber = (int) spnEntranceExamBaseTargetNumber.getValue();
        model.enableLocalAcademies = chkEnableLocalAcademies.isSelected();
        model.enablePrestigiousAcademies = chkEnablePrestigiousAcademies.isSelected();
        model.enableUnitEducation = chkEnableUnitEducation.isSelected();
        model.enableBonuses = chkEnableBonuses.isSelected();
        model.facultyXpRate = (double) spnFacultyXpMultiplier.getValue();
        model.adultDropoutChance = (int) spnAdultDropoutChance.getValue();
        model.childrenDropoutChance = (int) spnChildrenDropoutChance.getValue();
        model.allAges = chkAllAges.isSelected();
        model.militaryAcademyAccidents = (int) spnMilitaryAcademyAccidents.getValue();
    }
}
