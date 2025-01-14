/*
 * Copyright (c) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.campaignOptions.contents;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.annotations.Nullable;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Skills;
import mekhq.campaign.personnel.enums.*;
import mekhq.gui.campaignOptions.components.*;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JSpinner.DefaultEditor;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.campaignOptions.CampaignOptionsUtilities.*;

/**
 * Handles the Personnel tab of campaign options
 */
public class PersonnelTab {
    private static final String RESOURCE_PACKAGE = "mekhq/resources/CampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final CampaignOptions campaignOptions;

    //start General Tab
    private JPanel pnlPersonnelGeneralOptions;
    private JCheckBox chkUseTactics;
    private JCheckBox chkUseInitiativeBonus;
    private JCheckBox chkUseToughness;
    private JCheckBox chkUseRandomToughness;
    private JCheckBox chkUseArtillery;
    private JCheckBox chkUseAbilities;
    private JCheckBox chkUseEdge;
    private JCheckBox chkUseSupportEdge;
    private JCheckBox chkUseImplants;
    private JCheckBox chkUseAlternativeQualityAveraging;

    private JPanel pnlPersonnelCleanup;
    private JCheckBox chkUsePersonnelRemoval;
    private JCheckBox chkUseRemovalExemptCemetery;
    private JCheckBox chkUseRemovalExemptRetirees;

    private JPanel pnlAdministrators;
    private JCheckBox chkAdminsHaveNegotiation;
    private JCheckBox chkAdminExperienceLevelIncludeNegotiation;
    private JCheckBox chkAdminsHaveScrounge;
    private JCheckBox chkAdminExperienceLevelIncludeScrounge;
    //end General Tab

    //start Personnel Logs Tab
    private JCheckBox chkUseTransfers;
    private JCheckBox chkUseExtendedTOEForceName;
    private JCheckBox chkPersonnelLogSkillGain;
    private JCheckBox chkPersonnelLogAbilityGain;
    private JCheckBox chkPersonnelLogEdgeGain;
    private JCheckBox chkDisplayPersonnelLog;
    private JCheckBox chkDisplayScenarioLog;
    private JCheckBox chkDisplayKillRecord;
    //end Personnel Logs Tab

    //start Personnel Information Tab
    private JCheckBox chkUseTimeInService;
    private JLabel lblTimeInServiceDisplayFormat;
    private MMComboBox<TimeInDisplayFormat> comboTimeInServiceDisplayFormat;
    private JCheckBox chkUseTimeInRank;
    private JLabel lblTimeInRankDisplayFormat;
    private MMComboBox<TimeInDisplayFormat> comboTimeInRankDisplayFormat;
    private JCheckBox chkTrackTotalEarnings;
    private JCheckBox chkTrackTotalXPEarnings;
    private JCheckBox chkShowOriginFaction;
    //end Personnel Information Tab

    //start Awards Tab
    private JPanel pnlAwardsGeneralOptions;
    private JLabel lblAwardBonusStyle;
    private MMComboBox<AwardBonus> comboAwardBonusStyle;
    private JLabel lblAwardTierSize;
    private JSpinner spnAwardTierSize;
    private JCheckBox chkEnableAutoAwards;
    private JCheckBox chkIssuePosthumousAwards;
    private JCheckBox chkIssueBestAwardOnly;
    private JCheckBox chkIgnoreStandardSet;

    private JPanel pnlAutoAwardsFilter;
    private JCheckBox chkEnableContractAwards;
    private JCheckBox chkEnableFactionHunterAwards;
    private JCheckBox chkEnableInjuryAwards;
    private JCheckBox chkEnableIndividualKillAwards;
    private JCheckBox chkEnableFormationKillAwards;
    private JCheckBox chkEnableRankAwards;
    private JCheckBox chkEnableScenarioAwards;
    private JCheckBox chkEnableSkillAwards;
    private JCheckBox chkEnableTheatreOfWarAwards;
    private JCheckBox chkEnableTimeAwards;
    private JCheckBox chkEnableTrainingAwards;
    private JCheckBox chkEnableMiscAwards;
    private JTextArea txtAwardSetFilterList;
    //end Awards Tab

    //start Medical Tab
    private JCheckBox chkUseAdvancedMedical;
    private JLabel lblHealWaitingPeriod;
    private JSpinner spnHealWaitingPeriod;
    private JLabel lblNaturalHealWaitingPeriod;
    private JSpinner spnNaturalHealWaitingPeriod;
    private JLabel lblMinimumHitsForVehicles;
    private JSpinner spnMinimumHitsForVehicles;
    private JCheckBox chkUseRandomHitsForVehicles;
    private JCheckBox chkUseTougherHealing;
    private JLabel lblMaximumPatients;
    private JSpinner spnMaximumPatients;
    //end Medical Tab

    //start Prisoners & Dependents Tab
    private JPanel prisonerPanel;
    private JLabel lblPrisonerCaptureStyle;
    private MMComboBox<PrisonerCaptureStyle> comboPrisonerCaptureStyle;
    private JLabel lblPrisonerStatus;
    private MMComboBox<PrisonerStatus> comboPrisonerStatus;
    private JCheckBox chkPrisonerBabyStatus;
    private JCheckBox chkAtBPrisonerDefection;
    private JCheckBox chkAtBPrisonerRansom;

    private JPanel dependentsPanel;
    private JCheckBox chkUseRandomDependentAddition;
    private JCheckBox chkUseRandomDependentRemoval;
    //end Prisoners & Dependents Tab

    //start Salaries Tab
    private JCheckBox chkDisableSecondaryRoleSalary;

    private JPanel pnlSalaryMultipliersPanel;
    private JLabel lblAntiMekSalary;
    private JSpinner spnAntiMekSalary;
    private JLabel lblSpecialistInfantrySalary;
    private JSpinner spnSpecialistInfantrySalary;

    private JPanel pnlSalaryExperienceMultipliersPanel;
    private Map<SkillLevel, JLabel> lblSalaryExperienceMultipliers;
    private Map<SkillLevel, JSpinner> spnSalaryExperienceMultipliers;

    private JPanel pnlSalaryBaseSalaryPanel;
    private JLabel[] lblBaseSalary;
    private JSpinner[] spnBaseSalary;
    //end Salaries Tab

    /**
     * Represents a tab for repair and maintenance in an application.
     */
    public PersonnelTab(CampaignOptions campaignOptions) {
        this.campaignOptions = campaignOptions;

        initialize();
    }

    /**
     * Initializes the components of all tabs within the frame.
     */
    private void initialize() {
        initializeGeneralTab();
        initializePersonnelLogsTab();
        initializePersonnelInformationTab();
        initializeAwardsTab();
        initializeMedicalTab();
        initializePrisonersAndDependentsTab();
        initializeSalariesTab();
    }

    /**
     * Initializes the components of the SalariesTab.
     * The panel contains settings related to personnel salaries.
     */
    private void initializeSalariesTab() {
        chkDisableSecondaryRoleSalary = new JCheckBox();

        pnlSalaryMultipliersPanel = new JPanel();

        lblAntiMekSalary = new JLabel();
        spnAntiMekSalary = new JSpinner();

        lblSpecialistInfantrySalary = new JLabel();
        spnSpecialistInfantrySalary = new JSpinner();

        pnlSalaryExperienceMultipliersPanel = new JPanel();
        lblSalaryExperienceMultipliers = new HashMap<>();
        spnSalaryExperienceMultipliers = new HashMap<>();

        pnlSalaryBaseSalaryPanel = new JPanel();
        lblBaseSalary = new JLabel[29];
        spnBaseSalary = new JSpinner[29];
    }

    /**
     * Initializes the components of the PrisonersAndDependentsTab.
     * The panel contains settings related to prisoners and dependents.
     */
    private void initializePrisonersAndDependentsTab() {
        prisonerPanel = new JPanel();
        lblPrisonerCaptureStyle = new JLabel();
        comboPrisonerCaptureStyle = new MMComboBox<>("comboPrisonerCaptureStyle",
            PrisonerCaptureStyle.values());

        lblPrisonerStatus = new JLabel();
        comboPrisonerStatus = new MMComboBox<>("comboPrisonerStatus",
            getPrisonerStatusOptions());

        chkPrisonerBabyStatus = new JCheckBox();
        chkAtBPrisonerDefection = new JCheckBox();
        chkAtBPrisonerRansom = new JCheckBox();

        dependentsPanel = new JPanel();
        chkUseRandomDependentAddition = new JCheckBox();
        chkUseRandomDependentRemoval = new JCheckBox();
    }

    /**
     * Initializes the components of the MedicalTab.
     * The panel contains settings related to medical recovery and checks.
     */
    private void initializeMedicalTab() {
        chkUseAdvancedMedical = new JCheckBox();

        lblHealWaitingPeriod = new JLabel();
        spnHealWaitingPeriod = new JSpinner();

        lblNaturalHealWaitingPeriod = new JLabel();
        spnNaturalHealWaitingPeriod = new JSpinner();

        lblMinimumHitsForVehicles = new JLabel();
        spnMinimumHitsForVehicles = new JSpinner();

        chkUseRandomHitsForVehicles = new JCheckBox();
        chkUseTougherHealing = new JCheckBox();

        lblMaximumPatients = new JLabel();
        spnMaximumPatients = new JSpinner();
    }

    /**
     * Initializes the components of the AwardsTab.
     * The panel contains settings related to award allocation.
     */
    private void initializeAwardsTab() {
        pnlAwardsGeneralOptions = new JPanel();
        lblAwardBonusStyle = new JLabel();
        comboAwardBonusStyle = new MMComboBox<>("comboAwardBonusStyle", AwardBonus.values());

        lblAwardTierSize = new JLabel();
        spnAwardTierSize = new JSpinner();
        chkEnableAutoAwards = new JCheckBox();
        chkIssuePosthumousAwards = new JCheckBox();
        chkIssueBestAwardOnly = new JCheckBox();
        chkIgnoreStandardSet = new JCheckBox();
        chkEnableContractAwards = new JCheckBox();
        chkEnableFactionHunterAwards = new JCheckBox();
        chkEnableInjuryAwards = new JCheckBox();
        chkEnableIndividualKillAwards = new JCheckBox();
        chkEnableFormationKillAwards = new JCheckBox();
        chkEnableRankAwards = new JCheckBox();
        chkEnableScenarioAwards = new JCheckBox();
        chkEnableSkillAwards = new JCheckBox();
        chkEnableTheatreOfWarAwards = new JCheckBox();
        chkEnableTimeAwards = new JCheckBox();
        chkEnableTrainingAwards = new JCheckBox();
        chkEnableMiscAwards = new JCheckBox();

        pnlAutoAwardsFilter = new JPanel();
        txtAwardSetFilterList = new JTextArea();
    }

    /**
     * Initializes the components of the PersonnelInformationTab.
     * The panel contains settings related to personnel information display.
     */
    private void initializePersonnelInformationTab() {
        chkUseTimeInService = new JCheckBox();

        lblTimeInServiceDisplayFormat = new JLabel();
        comboTimeInServiceDisplayFormat = new MMComboBox<>("comboTimeInServiceDisplayFormat",
            TimeInDisplayFormat.values());

        chkUseTimeInRank = new JCheckBox();

        lblTimeInRankDisplayFormat = new JLabel();
        comboTimeInRankDisplayFormat = new MMComboBox<>("comboTimeInRankDisplayFormat",
            TimeInDisplayFormat.values());

        chkTrackTotalEarnings = new JCheckBox();
        chkTrackTotalXPEarnings = new JCheckBox();
        chkShowOriginFaction = new JCheckBox();
    }

    /**
     * Initializes components of the PersonnelLogsTab.
     * Panel consists of settings related to personnel log keeping.
     */
    private void initializePersonnelLogsTab() {
        chkUseTransfers = new JCheckBox();
        chkUseExtendedTOEForceName = new JCheckBox();
        chkPersonnelLogSkillGain = new JCheckBox();
        chkPersonnelLogAbilityGain = new JCheckBox();
        chkPersonnelLogEdgeGain = new JCheckBox();
        chkDisplayPersonnelLog = new JCheckBox();
        chkDisplayScenarioLog = new JCheckBox();
        chkDisplayKillRecord = new JCheckBox();
    }

    /**
     * Initializes components of the GeneralTab.
     * The panel contains general settings.
     */
    private void initializeGeneralTab() {
        pnlPersonnelGeneralOptions = new JPanel();
        chkUseTactics = new JCheckBox();
        chkUseInitiativeBonus = new JCheckBox();
        chkUseToughness = new JCheckBox();
        chkUseRandomToughness = new JCheckBox();
        chkUseArtillery = new JCheckBox();
        chkUseAbilities = new JCheckBox();
        chkUseEdge = new JCheckBox();
        chkUseSupportEdge = new JCheckBox();
        chkUseImplants = new JCheckBox();
        chkUseAlternativeQualityAveraging = new JCheckBox();

        pnlPersonnelCleanup = new JPanel();
        chkUsePersonnelRemoval = new JCheckBox();
        chkUseRemovalExemptCemetery = new JCheckBox();
        chkUseRemovalExemptRetirees = new JCheckBox();

        pnlAdministrators = new JPanel();
        chkAdminsHaveNegotiation = new JCheckBox();
        chkAdminExperienceLevelIncludeNegotiation = new JCheckBox();
        chkAdminsHaveScrounge = new JCheckBox();
        chkAdminExperienceLevelIncludeScrounge = new JCheckBox();
    }

    /**
     * @return a {@link DefaultComboBoxModel} containing all {@link PrisonerStatus} options except
     * {@code PrisonerStatus.FREE}
     */
    private DefaultComboBoxModel<PrisonerStatus> getPrisonerStatusOptions() {
        final DefaultComboBoxModel<PrisonerStatus> prisonerStatusModel = new DefaultComboBoxModel<>(
            PrisonerStatus.values());
        // we don't want this as a standard use case for prisoners
        prisonerStatusModel.removeElement(PrisonerStatus.FREE);

        return prisonerStatusModel;
    }

    public JPanel createGeneralTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PersonnelGeneralTab",
            getImageDirectory() + "logo_clan_wolverine.png");

        // Contents
        pnlPersonnelGeneralOptions = createGeneralOptionsPanel();
        pnlPersonnelCleanup = createPersonnelCleanUpPanel();
        pnlAdministrators = createAdministratorsPanel();

        // Layout the Panels
        final JPanel panelRight = new CampaignOptionsStandardPanel("RightPanel");
        GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridwidth = 1;
        layoutRight.gridx = 0;
        layoutRight.gridy = 0;
        panelRight.add(pnlPersonnelCleanup, layoutRight);

        layoutRight.gridy++;
        panelRight.add(pnlAdministrators, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("PersonnelGeneralTab", true);
        GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(pnlPersonnelGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "PersonnelGeneralTab");
    }

    private JPanel createGeneralOptionsPanel() {
        // Contents
        chkUseTactics = new CampaignOptionsCheckBox("UseTactics");
        chkUseInitiativeBonus = new CampaignOptionsCheckBox("UseInitiativeBonus");
        chkUseToughness = new CampaignOptionsCheckBox("UseToughness");
        chkUseRandomToughness = new CampaignOptionsCheckBox("UseRandomToughness");
        chkUseArtillery = new CampaignOptionsCheckBox("UseArtillery");
        chkUseAbilities = new CampaignOptionsCheckBox("UseAbilities");
        chkUseEdge = new CampaignOptionsCheckBox("UseEdge");
        chkUseSupportEdge = new CampaignOptionsCheckBox("UseSupportEdge");
        chkUseImplants = new CampaignOptionsCheckBox("UseImplants");
        chkUseAlternativeQualityAveraging = new CampaignOptionsCheckBox("UseAlternativeQualityAveraging");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelGeneralTab");
        GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUseTactics, layout);

        layout.gridy++;
        panel.add(chkUseInitiativeBonus, layout);

        layout.gridy++;
        panel.add(chkUseToughness, layout);

        layout.gridy++;
        panel.add(chkUseRandomToughness, layout);

        layout.gridy++;
        panel.add(chkUseArtillery, layout);

        layout.gridy++;
        panel.add(chkUseAbilities, layout);

        layout.gridy++;
        panel.add(chkUseEdge, layout);

        layout.gridy++;
        panel.add(chkUseSupportEdge, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseImplants, layout);

        layout.gridy++;
        panel.add(chkUseImplants, layout);

        layout.gridy++;
        panel.add(chkUseAlternativeQualityAveraging, layout);

        return panel;
    }

    /**
     * Creates a panel for personnel cleanup settings.
     * <p>
     * This method creates checkboxes for personnel cleanup options such as personnel removal, exempt
     * cemetery personnel, and exempt retirees.
     *
     * @return a {@link JPanel} containing the personnel cleanup checkboxes
     */
    private JPanel createPersonnelCleanUpPanel() {
        // Contents
        chkUsePersonnelRemoval = new CampaignOptionsCheckBox("UsePersonnelRemoval");
        chkUseRemovalExemptCemetery = new CampaignOptionsCheckBox("UseRemovalExemptCemetery");
        chkUseRemovalExemptRetirees = new CampaignOptionsCheckBox("UseRemovalExemptRetirees");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelCleanUpPanel", true,
            "PersonnelCleanUpPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel, null, GridBagConstraints.NONE);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkUsePersonnelRemoval, layout);
        layout.gridx++;
        panel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(35)));

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseRemovalExemptCemetery, layout);
        layout.gridx++;
        panel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(35)));

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkUseRemovalExemptRetirees, layout);
        layout.gridx++;
        panel.add(Box.createHorizontalStrut(UIUtil.scaleForGUI(35)));

        return panel;
    }

    /**
     * Creates a panel for the Administrators Tab in the application.
     * <p>
     * This method constructs the header panel and checkbox components for administrator settings
     * including negotiation and scrounging options.
     *
     * @return a {@link JPanel} representing the Administrators Tab panel
     */
    private JPanel createAdministratorsPanel() {
        // Contents
        chkAdminsHaveNegotiation = new CampaignOptionsCheckBox("AdminsHaveNegotiation");
        chkAdminExperienceLevelIncludeNegotiation = new CampaignOptionsCheckBox("AdminExperienceLevelIncludeNegotiation");
        chkAdminsHaveScrounge = new CampaignOptionsCheckBox("AdminsHaveScrounge");
        chkAdminExperienceLevelIncludeScrounge = new CampaignOptionsCheckBox("AdminExperienceLevelIncludeScrounge");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministratorsPanel", true,
            "AdministratorsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkAdminsHaveNegotiation, layout);

        layout.gridy++;
        panel.add(chkAdminExperienceLevelIncludeNegotiation, layout);

        layout.gridy++;
        panel.add(chkAdminsHaveScrounge, layout);

        layout.gridy++;
        panel.add(chkAdminExperienceLevelIncludeScrounge, layout);

        return panel;
    }

    /**
     * Creates the Awards Tab panel with various components like labels, checkboxes, and filter options.
     *
     * @return the {@link JPanel} representing the Awards Tab panel
     */
    public JPanel createAwardsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("AwardsTab",
            getImageDirectory() + "logo_outworld_alliance.png");

        // Contents
        pnlAwardsGeneralOptions = createAwardsGeneralOptionsPanel();
        pnlAutoAwardsFilter = createAutoAwardsFilterPanel();

        txtAwardSetFilterList = new JTextArea(10, 60);
        txtAwardSetFilterList.setLineWrap(true);
        txtAwardSetFilterList.setWrapStyleWord(true);
        txtAwardSetFilterList.setToolTipText(
            wordWrap(resources.getString("lblAwardSetFilterList.tooltip")));
        txtAwardSetFilterList.setName("txtAwardSetFilterList");
        txtAwardSetFilterList.setText("");
        JScrollPane scrollAwardSetFilterList = new JScrollPane(txtAwardSetFilterList);
        scrollAwardSetFilterList.setHorizontalScrollBarPolicy(
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollAwardSetFilterList.setVerticalScrollBarPolicy(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Layout the Panel
        final JPanel panelRight = new CampaignOptionsStandardPanel("AwardsTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridy = 0;
        layoutRight.gridwidth = 1;
        layoutRight.gridy++;
        panelRight.add(pnlAutoAwardsFilter, layoutRight);

        final JPanel panelBottom = new CampaignOptionsStandardPanel("AwardsTabBottom", true,
            "AwardsTabBottom");
        final GridBagConstraints layoutBottom = new CampaignOptionsGridBagConstraints(panelBottom,
            null, GridBagConstraints.HORIZONTAL);

        layoutBottom.gridx = 0;
        layoutBottom.gridy++;
        panelBottom.add(txtAwardSetFilterList, layoutBottom);

        final JPanel panelParent = new CampaignOptionsStandardPanel("AwardsTabRight", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(pnlAwardsGeneralOptions, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 2;
        panelParent.add(panelBottom, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "AwardsTab");
    }

    JPanel createAwardsGeneralOptionsPanel() {
        // Contents
        lblAwardBonusStyle = new CampaignOptionsLabel("AwardBonusStyle");
        comboAwardBonusStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof AwardBonus) {
                    list.setToolTipText(((AwardBonus) value).getToolTipText());
                }
                return this;
            }
        });

        lblAwardTierSize = new CampaignOptionsLabel("AwardTierSize");
        spnAwardTierSize = new CampaignOptionsSpinner("AwardTierSize",
            5, 1, 100, 1);

        chkEnableAutoAwards = new CampaignOptionsCheckBox("EnableAutoAwards");

        chkIssuePosthumousAwards = new CampaignOptionsCheckBox("IssuePosthumousAwards");

        chkIssueBestAwardOnly = new CampaignOptionsCheckBox("IssueBestAwardOnly");

        chkIgnoreStandardSet = new CampaignOptionsCheckBox("IgnoreStandardSet");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AwardsGeneralOptionsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(lblAwardBonusStyle, layout);
        layout.gridx++;
        panel.add(comboAwardBonusStyle, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblAwardTierSize, layout);
        layout.gridx++;
        panel.add(spnAwardTierSize, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkEnableAutoAwards, layout);

        layout.gridy++;
        panel.add(chkIssuePosthumousAwards, layout);

        layout.gridy++;
        panel.add(chkIssueBestAwardOnly, layout);

        layout.gridy++;
        panel.add(chkIgnoreStandardSet, layout);

        return panel;
    }

    /**
     * Creates a panel with checkboxes for various types of autoAwards award filter options.
     * <p>
     * This method creates checkboxes for different types of awards filters such as contract awards,
     * faction hunter awards, injury awards, individual kill awards, etc.
     *
     * @return a {@link JPanel} containing checkboxes for various types of autoAwards award filter
     * options
     */
    private JPanel createAutoAwardsFilterPanel() {
        // Contents
        chkEnableContractAwards = new CampaignOptionsCheckBox("EnableContractAwards");
        chkEnableFactionHunterAwards = new CampaignOptionsCheckBox("EnableFactionHunterAwards");
        chkEnableInjuryAwards = new CampaignOptionsCheckBox("EnableInjuryAwards");
        chkEnableIndividualKillAwards = new CampaignOptionsCheckBox("EnableIndividualKillAwards");
        chkEnableFormationKillAwards = new CampaignOptionsCheckBox("EnableFormationKillAwards");
        chkEnableRankAwards = new CampaignOptionsCheckBox("EnableRankAwards");
        chkEnableScenarioAwards = new CampaignOptionsCheckBox("EnableScenarioAwards");
        chkEnableSkillAwards = new CampaignOptionsCheckBox("EnableSkillAwards");
        chkEnableTheatreOfWarAwards = new CampaignOptionsCheckBox("EnableTheatreOfWarAwards");
        chkEnableTimeAwards = new CampaignOptionsCheckBox("EnableTimeAwards");
        chkEnableTrainingAwards = new CampaignOptionsCheckBox("EnableTrainingAwards");
        chkEnableMiscAwards = new CampaignOptionsCheckBox("EnableMiscAwards");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AutoAwardsFilterPanel", true, "AutoAwardsFilterPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridx = 0;
        layout.gridy = 0;
        layout.gridwidth = 1;
        panel.add(chkEnableContractAwards, layout);
        layout.gridx++;
        panel.add(chkEnableFactionHunterAwards, layout);
        layout.gridx++;
        panel.add(chkEnableInjuryAwards, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkEnableIndividualKillAwards, layout);
        layout.gridx++;
        panel.add(chkEnableFormationKillAwards, layout);
        layout.gridx++;
        panel.add(chkEnableRankAwards, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkEnableScenarioAwards, layout);
        layout.gridx++;
        panel.add(chkEnableSkillAwards, layout);
        layout.gridx++;
        panel.add(chkEnableTheatreOfWarAwards, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(chkEnableTimeAwards, layout);
        layout.gridx++;
        panel.add(chkEnableTrainingAwards, layout);
        layout.gridx++;
        panel.add(chkEnableMiscAwards, layout);

        return panel;
    }

    /**
     * Creates a panel for the Medical Tab in the application.
     *
     * @return a {@link JPanel} representing the Medical Tab containing settings for medical options.
     */
    public JPanel createMedicalTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("MedicalTab",
            getImageDirectory() + "logo_duchy_of_tamarind_abbey.png");

        // Contents
        chkUseAdvancedMedical = new CampaignOptionsCheckBox("UseAdvancedMedical");

        lblHealWaitingPeriod = new CampaignOptionsLabel("HealWaitingPeriod");
        spnHealWaitingPeriod = new CampaignOptionsSpinner("HealWaitingPeriod",
            1, 1, 30, 1);

        lblNaturalHealWaitingPeriod = new CampaignOptionsLabel("NaturalHealWaitingPeriod");
        spnNaturalHealWaitingPeriod = new CampaignOptionsSpinner("NaturalHealWaitingPeriod",
            1, 1, 365, 1);

        lblMinimumHitsForVehicles = new CampaignOptionsLabel("MinimumHitsForVehicles");
        spnMinimumHitsForVehicles = new CampaignOptionsSpinner("MinimumHitsForVehicles",
            1, 1, 5, 1);

        chkUseRandomHitsForVehicles = new CampaignOptionsCheckBox("UseRandomHitsForVehicles");

        chkUseTougherHealing = new CampaignOptionsCheckBox("UseTougherHealing");

        lblMaximumPatients = new CampaignOptionsLabel("MaximumPatients");
        spnMaximumPatients = new CampaignOptionsSpinner("MaximumPatients",
            25, 1, 100, 1);

        final JPanel panelLeft = new CampaignOptionsStandardPanel("MedicalTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridy = 0;
        layoutLeft.gridx = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(lblMaximumPatients, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaximumPatients, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblHealWaitingPeriod, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnHealWaitingPeriod, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblNaturalHealWaitingPeriod, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnNaturalHealWaitingPeriod, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseRandomHitsForVehicles, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblMinimumHitsForVehicles, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMinimumHitsForVehicles, layoutLeft);

        // Layout the Panels
        final JPanel panelRight = new CampaignOptionsStandardPanel("MedicalTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridy++;
        layoutRight.gridwidth = 1;
        panelRight.add(chkUseAdvancedMedical, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(chkUseTougherHealing, layoutRight);

        // Layout the Panels
        final JPanel panelParent = new CampaignOptionsStandardPanel("MedicalTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "MedicalTab");
    }

    /**
     * Creates a panel for the Personnel Information Tab in the application.
     *
     * @return a {@link JPanel} representing the Personnel Information Tab panel
     */
    public JPanel createPersonnelInformationTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PersonnelInformation",
            getImageDirectory() + "logo_rasalhague_dominion.png");

        // Contents
        chkUseTimeInService = new CampaignOptionsCheckBox("UseTimeInService");
        lblTimeInServiceDisplayFormat = new CampaignOptionsLabel("TimeInServiceDisplayFormat");
        chkUseTimeInRank = new CampaignOptionsCheckBox("UseTimeInRank");
        lblTimeInRankDisplayFormat = new CampaignOptionsLabel("TimeInRankDisplayFormat");
        chkTrackTotalEarnings = new CampaignOptionsCheckBox("TrackTotalEarnings");
        chkTrackTotalXPEarnings = new CampaignOptionsCheckBox("TrackTotalXPEarnings");
        chkShowOriginFaction = new CampaignOptionsCheckBox("ShowOriginFaction");

        JPanel pnlPersonnelLogs = createPersonnelLogsPanel();

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("PersonnelInformationLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(chkUseTimeInService, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(lblTimeInServiceDisplayFormat, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(comboTimeInServiceDisplayFormat, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkUseTimeInRank, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblTimeInRankDisplayFormat, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(comboTimeInRankDisplayFormat, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(chkTrackTotalEarnings, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkTrackTotalXPEarnings, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(chkShowOriginFaction, layoutLeft);

        final JPanel panelParent = new CampaignOptionsStandardPanel("PersonnelInformation", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(pnlPersonnelLogs, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "PersonnelInformation");
    }

    JPanel createPersonnelLogsPanel() {
        // Contents
        chkUseTransfers = new CampaignOptionsCheckBox("UseTransfers");
        chkUseExtendedTOEForceName = new CampaignOptionsCheckBox("UseExtendedTOEForceName");
        chkPersonnelLogSkillGain = new CampaignOptionsCheckBox("PersonnelLogSkillGain");
        chkPersonnelLogAbilityGain = new CampaignOptionsCheckBox("PersonnelLogAbilityGain");
        chkPersonnelLogEdgeGain = new CampaignOptionsCheckBox("PersonnelLogEdgeGain");
        chkDisplayPersonnelLog = new CampaignOptionsCheckBox("DisplayPersonnelLog");
        chkDisplayScenarioLog = new CampaignOptionsCheckBox("DisplayScenarioLog");
        chkDisplayKillRecord = new CampaignOptionsCheckBox("DisplayKillRecord");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PersonnelLogsPanel", true,
            "PersonnelLogsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 1;
        layout.gridx = 0;
        layout.gridy = 0;
        panel.add(chkUseTransfers, layout);

        layout.gridy++;
        panel.add(chkUseExtendedTOEForceName, layout);

        layout.gridy++;
        panel.add(chkPersonnelLogSkillGain, layout);

        layout.gridy++;
        panel.add(chkPersonnelLogAbilityGain, layout);

        layout.gridy++;
        panel.add(chkPersonnelLogEdgeGain, layout);

        layout.gridy++;
        panel.add(chkDisplayPersonnelLog, layout);

        layout.gridy++;
        panel.add(chkDisplayScenarioLog, layout);

        layout.gridy++;
        panel.add(chkDisplayKillRecord, layout);

        return panel;
    }

    /**
     * Creates a panel for configuring settings related to prisoners and dependents.
     * <p>
     * This method constructs a panel with different components such as header, prisoners panel, and
     * dependents panel.
     * The layout is set up with the header on top followed by the prisoners and dependents panels
     * side by side.
     *
     * @return a {@link JPanel} representing the prisoners and dependents configuration settings
     */
    public JPanel createPrisonersAndDependentsTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("PrisonersAndDependentsTab",
            getImageDirectory() + "logo_illyrian_palatinate.png");

        // Contents
        prisonerPanel = createPrisonersPanel();
        dependentsPanel = createDependentsPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PrisonersAndDependentsTab", true,
            "");
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panel);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panel.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panel.add(prisonerPanel, layoutParent);

        layoutParent.gridx++;
        panel.add(dependentsPanel, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panel, "PrisonersAndDependentsTab");
    }

    /**
     * Creates a panel for configuring settings related to prisoners in the application.
     * <p>
     * This method sets up various components such as prisoner capture style, status, and related checkboxes.
     *
     * @return a {@link JPanel} containing the prisoner configuration settings
     */
    private JPanel createPrisonersPanel() {
        // Contents
        lblPrisonerCaptureStyle = new CampaignOptionsLabel("PrisonerCaptureStyle");
        comboPrisonerCaptureStyle.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PrisonerCaptureStyle) {
                    list.setToolTipText(((PrisonerCaptureStyle) value).getToolTipText());
                }
                return this;
            }
        });

        lblPrisonerStatus = new CampaignOptionsLabel("PrisonerStatus");

        chkPrisonerBabyStatus = new CampaignOptionsCheckBox("PrisonerBabyStatus");
        chkAtBPrisonerDefection = new CampaignOptionsCheckBox("AtBPrisonerDefection");
        chkAtBPrisonerRansom = new CampaignOptionsCheckBox("AtBPrisonerRansom");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PrisonersPanel", true,
            "PrisonersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblPrisonerCaptureStyle, layout);
        layout.gridx++;
        panel.add(comboPrisonerCaptureStyle, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblPrisonerStatus, layout);
        layout.gridx++;
        panel.add(comboPrisonerStatus, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(chkPrisonerBabyStatus, layout);

        layout.gridy++;
        panel.add(chkAtBPrisonerDefection, layout);

        layout.gridy++;
        panel.add(chkAtBPrisonerRansom, layout);

        return panel;
    }

    /**
     * Creates a panel with checkboxes for setting dependent options.
     *
     * @return a {@link JPanel} containing checkboxes for setting dependent options
     */
    private JPanel createDependentsPanel() {
        // Contents
        chkUseRandomDependentAddition = new CampaignOptionsCheckBox("UseRandomDependentAddition");
        chkUseRandomDependentRemoval = new CampaignOptionsCheckBox("UseRandomDependentRemoval");

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("DependentsPanel", true, "DependentsPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(chkUseRandomDependentAddition, layout);

        layout.gridy++;
        panel.add(chkUseRandomDependentRemoval, layout);

        return panel;
    }

    /**
     * Creates a salary configuration tab for managing salary settings such as multipliers and base salaries.
     *
     * @return a {@link JPanel} representing the salary tab
     */
    public JPanel createSalariesTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("SalariesTab",
            getImageDirectory() + "logo_clan_coyote.png");

        // Contents
        chkDisableSecondaryRoleSalary = new CampaignOptionsCheckBox("DisableSecondaryRoleSalary");
        pnlSalaryMultipliersPanel = createSalaryMultipliersPanel();
        pnlSalaryExperienceMultipliersPanel = createExperienceMultipliersPanel();
        pnlSalaryBaseSalaryPanel = createBaseSalariesPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalariesTab", true);
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridwidth = 5;
        layout.gridy = 0;
        panel.add(headerPanel, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 1;
        panel.add(chkDisableSecondaryRoleSalary, layout);

        layout.gridy++;
        panel.add(pnlSalaryMultipliersPanel, layout);
        layout.gridx++;
        panel.add(pnlSalaryExperienceMultipliersPanel, layout);

        layout.gridx = 0;
        layout.gridy++;
        layout.gridwidth = 2;
        panel.add(pnlSalaryBaseSalaryPanel, layout);

        // Create Parent Panel and return
        return createParentPanel(panel, "SalariesTab");
    }

    /**
     * Creates a panel for configuring salary multipliers for different personnel roles.
     *
     * @return a {@link JPanel} containing the salary multiplier configuration panel
     */
    private JPanel createSalaryMultipliersPanel() {
        // Contents
        lblAntiMekSalary = new CampaignOptionsLabel("AntiMekSalary");
        spnAntiMekSalary = new CampaignOptionsSpinner("AntiMekSalary",
            0, 0, 100, 0.01);

        lblSpecialistInfantrySalary = new CampaignOptionsLabel("SpecialistInfantrySalary");
        spnSpecialistInfantrySalary = new CampaignOptionsSpinner("SpecialistInfantrySalary",
            0, 0, 100, 0.01);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SalaryMultipliersPanel", true,
            "SalaryMultipliersPanel");
        final GridBagConstraints layout = new CampaignOptionsGridBagConstraints(panel);

        layout.gridy = 0;
        layout.gridx = 0;
        layout.gridwidth = 1;
        panel.add(lblAntiMekSalary, layout);
        layout.gridx++;
        panel.add(spnAntiMekSalary, layout);

        layout.gridx = 0;
        layout.gridy++;
        panel.add(lblSpecialistInfantrySalary, layout);
        layout.gridx++;
        panel.add(spnSpecialistInfantrySalary, layout);

        return panel;
    }

    /**
     * Creates a panel for configuring experience multipliers for different skill levels.
     * <p>
     * This method dynamically generates labels and spinners for each skill level based on the values
     * in the {@link SkillLevel} enum.
     * </p>
     *
     * @return a {@link JPanel} containing the experience multipliers configuration panel
     */
    private JPanel createExperienceMultipliersPanel() {
        // Contents
        for (final SkillLevel skillLevel : Skills.SKILL_LEVELS) {
            final JLabel label = new CampaignOptionsLabel("SkillLevel" + skillLevel.toString(),
                null, true);
            label.setToolTipText(resources.getString("lblSkillLevelMultiplier.tooltip"));
            lblSalaryExperienceMultipliers.put(skillLevel, label);

            final JSpinner spinner = new CampaignOptionsSpinner("SkillLevel" + skillLevel,
                null, 0, 0, 100, 0.1, true);
            spinner.setToolTipText(resources.getString("lblSkillLevelMultiplier.tooltip"));
            spnSalaryExperienceMultipliers.put(skillLevel, spinner);

        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ExperienceMultipliersPanel",
            true, "ExperienceMultipliersPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        SkillLevel[] skillLevels = SkillLevel.values();
        int rows = 2;
        int columns = 4;

        SequentialGroup horizontalGroups = layout.createSequentialGroup();
        ParallelGroup[] verticalGroups = new ParallelGroup[rows];

        for (int j = 0; j < rows; j++) {
            verticalGroups[j] = layout.createParallelGroup(Alignment.BASELINE);
        }

        for (int i = 0; i < columns; i++) {
            ParallelGroup horizontalParallelGroup = layout.createParallelGroup();

            for (int j = 0; j < rows; j++) {
                int index = i * rows + j;

                SequentialGroup horizontalSequentialGroup = layout.createSequentialGroup();
                horizontalSequentialGroup.addComponent(lblSalaryExperienceMultipliers.get(skillLevels[index]));
                horizontalSequentialGroup.addComponent(spnSalaryExperienceMultipliers.get(skillLevels[index]));
                if (i != (columns - 1)) {
                    horizontalSequentialGroup.addGap(10);
                }

                horizontalParallelGroup.addGroup(horizontalSequentialGroup);
                verticalGroups[j].addComponent(lblSalaryExperienceMultipliers.get(skillLevels[index]));
                verticalGroups[j].addComponent(spnSalaryExperienceMultipliers.get(skillLevels[index]));
            }

            horizontalGroups.addGroup(horizontalParallelGroup);
        }

        layout.setHorizontalGroup(horizontalGroups);
        SequentialGroup verticalGroup = layout.createSequentialGroup();
        for (Group group: verticalGroups) {
            verticalGroup.addGroup(group);
        }
        layout.setVerticalGroup(verticalGroup);

        return panel;
    }

    /**
     * Creates a panel for configuring base salaries for different personnel roles.
     * <p>
     * This method dynamically generates labels and spinners for each personnel role
     * based on the values in the PersonnelRole enum.
     *
     * @return a {@link JPanel} containing the base salaries configuration panel
     */
    private JPanel createBaseSalariesPanel() {
        // Contents
        for (final PersonnelRole personnelRole : PersonnelRole.values()) {
            String componentName = personnelRole.toString().replaceAll(" ", "");

            // JLabel
            JLabel jLabel = new JLabel(personnelRole.toString());
            jLabel.setName("lbl" + componentName);

            Dimension labelSize = jLabel.getPreferredSize();
            jLabel.setMinimumSize(UIUtil.scaleForGUI(labelSize.width, labelSize.height));

            // JSpinner
            JSpinner jSpinner = new JSpinner();
            jSpinner.setModel(new SpinnerNumberModel(0.0, 0.0, 1000000, 10.0));
            jSpinner.setName("spn" + componentName);

            DefaultEditor editor = (DefaultEditor) jSpinner.getEditor();
            editor.getTextField().setHorizontalAlignment(JTextField.LEFT);

            Dimension spinnerSize = jSpinner.getPreferredSize();
            jSpinner.setMinimumSize(UIUtil.scaleForGUI(spinnerSize.width, spinnerSize.height));

            // Component Tracking Assignment
            lblBaseSalary[personnelRole.ordinal()] = jLabel;
            spnBaseSalary[personnelRole.ordinal()] = jSpinner;
        }

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("BaseSalariesPanel", true,
            "BaseSalariesPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        SequentialGroup mainHorizontalGroup = layout.createSequentialGroup();
        SequentialGroup mainVerticalGroup = layout.createSequentialGroup();

        int columns = 3;
        int rows = (int) Math.ceil((double) lblBaseSalary.length / columns);

        // Create an array to store ParallelGroups for each column
        ParallelGroup[] columnGroups = new ParallelGroup[columns];
        for (int i = 0; i < columns; i++) {
            columnGroups[i] = layout.createParallelGroup();
        }

        for (int j = 0; j < rows; j++) {
            ParallelGroup verticalGroup = layout.createParallelGroup(Alignment.BASELINE);

            for (int i = 0; i < columns; i++) {
                int index = i * rows + j;

                if (index < lblBaseSalary.length) {
                    // Create a SequentialGroup for the label and spinner
                    SequentialGroup horizontalSequentialGroup = layout.createSequentialGroup();

                    horizontalSequentialGroup.addComponent(lblBaseSalary[index]);
                    horizontalSequentialGroup.addComponent(spnBaseSalary[index]);
                    if (i != (columns - 1)) {
                        horizontalSequentialGroup.addGap(10);
                    }

                    // Add the SequentialGroup to the column's ParallelGroup
                    columnGroups[i].addGroup(horizontalSequentialGroup);

                    verticalGroup.addComponent(lblBaseSalary[index]);
                    verticalGroup.addComponent(spnBaseSalary[index]);
                }
            }
            mainVerticalGroup.addGroup(verticalGroup);
        }
        for (ParallelGroup columnGroup : columnGroups) {
            mainHorizontalGroup.addGroup(columnGroup);
        }

        layout.setHorizontalGroup(mainHorizontalGroup);
        layout.setVerticalGroup(mainVerticalGroup);

        return panel;
    }

    public void loadValuesFromCampaignOptions() {
        loadValuesFromCampaignOptions(null);
    }

    public void loadValuesFromCampaignOptions(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // General
        chkUseTactics.setSelected(options.isUseTaxes());
        chkUseInitiativeBonus.setSelected(options.isUseInitiativeBonus());
        chkUseToughness.setSelected(options.isUseToughness());
        chkUseRandomToughness.setSelected(options.isUseRandomToughness());
        chkUseArtillery.setSelected(options.isUseArtillery());
        chkUseAbilities.setSelected(options.isUseAbilities());
        chkUseEdge.setSelected(options.isUseEdge());
        chkUseSupportEdge.setSelected(options.isUseSupportEdge());
        chkUseImplants.setSelected(options.isUseImplants());
        chkUseAlternativeQualityAveraging.setSelected(options.isAlternativeQualityAveraging());
        chkUsePersonnelRemoval.setSelected(options.isUsePersonnelRemoval());
        chkUseRemovalExemptCemetery.setSelected(options.isUseRemovalExemptCemetery());
        chkUseRemovalExemptRetirees.setSelected(options.isUseRemovalExemptRetirees());
        chkAdminsHaveNegotiation.setSelected(options.isAdminsHaveNegotiation());
        chkAdminExperienceLevelIncludeNegotiation.setSelected(options.isAdminExperienceLevelIncludeNegotiation());
        chkAdminsHaveScrounge.setSelected(options.isAdminsHaveScrounge());
        chkAdminExperienceLevelIncludeScrounge.setSelected(options.isAdminExperienceLevelIncludeScrounge());

        // Personnel Log
        chkUseTransfers.setSelected(options.isUseTransfers());
        chkUseExtendedTOEForceName.setSelected(options.isUseExtendedTOEForceName());
        chkPersonnelLogSkillGain.setSelected(options.isPersonnelLogSkillGain());
        chkPersonnelLogAbilityGain.setSelected(options.isPersonnelLogAbilityGain());
        chkPersonnelLogEdgeGain.setSelected(options.isPersonnelLogEdgeGain());
        chkDisplayPersonnelLog.setSelected(options.isDisplayPersonnelLog());
        chkDisplayScenarioLog.setSelected(options.isDisplayScenarioLog());
        chkDisplayKillRecord.setSelected(options.isDisplayKillRecord());

        // Personnel Information
        chkUseTimeInService.setSelected(options.isUseTimeInService());
        comboTimeInServiceDisplayFormat.setSelectedItem(options.getTimeInServiceDisplayFormat());
        chkUseTimeInRank.setSelected(options.isUseTimeInRank());
        comboTimeInRankDisplayFormat.setSelectedItem(options.getTimeInRankDisplayFormat());
        chkTrackTotalEarnings.setSelected(options.isTrackTotalEarnings());
        chkTrackTotalXPEarnings.setSelected(options.isTrackTotalXPEarnings());
        chkShowOriginFaction.setSelected(options.isShowOriginFaction());

        // Awards
        comboAwardBonusStyle.setSelectedItem(options.getAwardBonusStyle());
        spnAwardTierSize.setValue(options.getAwardTierSize());
        chkEnableAutoAwards.setSelected(options.isEnableAutoAwards());
        chkIssuePosthumousAwards.setSelected(options.isIssuePosthumousAwards());
        chkIssueBestAwardOnly.setSelected(options.isIssueBestAwardOnly());
        chkIgnoreStandardSet.setSelected(options.isIgnoreStandardSet());
        chkEnableContractAwards.setSelected(options.isEnableContractAwards());
        chkEnableFactionHunterAwards.setSelected(options.isEnableFactionHunterAwards());
        chkEnableInjuryAwards.setSelected(options.isEnableInjuryAwards());
        chkEnableIndividualKillAwards.setSelected(options.isEnableIndividualKillAwards());
        chkEnableFormationKillAwards.setSelected(options.isEnableFormationKillAwards());
        chkEnableRankAwards.setSelected(options.isEnableRankAwards());
        chkEnableScenarioAwards.setSelected(options.isEnableScenarioAwards());
        chkEnableSkillAwards.setSelected(options.isEnableSkillAwards());
        chkEnableTheatreOfWarAwards.setSelected(options.isEnableTheatreOfWarAwards());
        chkEnableTimeAwards.setSelected(options.isEnableTimeAwards());
        chkEnableTrainingAwards.setSelected(options.isEnableTrainingAwards());
        chkEnableMiscAwards.setSelected(options.isEnableMiscAwards());
        txtAwardSetFilterList.setText(options.getAwardSetFilterList());

        // Medical
        chkUseAdvancedMedical.setSelected(options.isUseAdvancedMedical());
        spnHealWaitingPeriod.setValue(options.getHealingWaitingPeriod());
        spnNaturalHealWaitingPeriod.setValue(options.getNaturalHealingWaitingPeriod());
        spnMinimumHitsForVehicles.setValue(options.getMinimumHitsForVehicles());
        chkUseRandomHitsForVehicles.setSelected(options.isUseRandomHitsForVehicles());
        chkUseTougherHealing.setSelected(options.isTougherHealing());
        spnMaximumPatients.setValue(options.getMaximumPatients());

        // Prisoners and Dependents
        comboPrisonerCaptureStyle.setSelectedItem(options.getPrisonerCaptureStyle());
        comboPrisonerStatus.setSelectedItem(options.getDefaultPrisonerStatus());
        chkPrisonerBabyStatus.setSelected(options.isPrisonerBabyStatus());
        chkAtBPrisonerDefection.setSelected(options.isUseAtBPrisonerDefection());
        chkAtBPrisonerRansom.setSelected(options.isUseAtBPrisonerRansom());
        chkUseRandomDependentAddition.setSelected(options.isUseRandomDependentAddition());
        chkUseRandomDependentRemoval.setSelected(options.isUseRandomDependentRemoval());

        // Salaries
        chkDisableSecondaryRoleSalary.setSelected(options.isDisableSecondaryRoleSalary());
        spnAntiMekSalary.setValue(options.getSalaryAntiMekMultiplier());
        spnSpecialistInfantrySalary.setValue(options.getSalarySpecialistInfantryMultiplier());
        for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
            entry.getValue().setValue(options.getSalaryXPMultipliers().get(entry.getKey()));
        }
        for (int i = 0; i < spnBaseSalary.length; i++) {
            spnBaseSalary[i].setValue(options.getRoleBaseSalaries()[i].getAmount().doubleValue());
        }
    }

    public void applyCampaignOptionsToCampaign(@Nullable CampaignOptions presetCampaignOptions) {
        CampaignOptions options = presetCampaignOptions;
        if (presetCampaignOptions == null) {
            options = this.campaignOptions;
        }

        // General
        options.setUseTaxes(chkUseTactics.isSelected());
        options.setUseInitiativeBonus(chkUseInitiativeBonus.isSelected());
        options.setUseToughness(chkUseToughness.isSelected());
        options.setUseRandomToughness(chkUseRandomToughness.isSelected());
        options.setUseArtillery(chkUseArtillery.isSelected());
        options.setUseAbilities(chkUseAbilities.isSelected());
        options.setUseEdge(chkUseEdge.isSelected());
        options.setUseSupportEdge(chkUseSupportEdge.isSelected());
        options.setUseImplants(chkUseImplants.isSelected());
        options.setAlternativeQualityAveraging(chkUseAlternativeQualityAveraging.isSelected());
        options.setUsePersonnelRemoval(chkUsePersonnelRemoval.isSelected());
        options.setUseRemovalExemptCemetery(chkUseRemovalExemptCemetery.isSelected());
        options.setUseRemovalExemptRetirees(chkUseRemovalExemptRetirees.isSelected());
        options.setAdminsHaveNegotiation(chkAdminsHaveNegotiation.isSelected());
        options.setAdminExperienceLevelIncludeNegotiation(chkAdminExperienceLevelIncludeNegotiation.isSelected());
        options.setAdminsHaveScrounge(chkAdminsHaveScrounge.isSelected());
        options.setAdminExperienceLevelIncludeScrounge(chkAdminExperienceLevelIncludeScrounge.isSelected());

        // Personnel Log
        options.setUseTransfers(chkUseTransfers.isSelected());
        options.setUseExtendedTOEForceName(chkUseExtendedTOEForceName.isSelected());
        options.setPersonnelLogSkillGain(chkPersonnelLogSkillGain.isSelected());
        options.setPersonnelLogAbilityGain(chkPersonnelLogAbilityGain.isSelected());
        options.setPersonnelLogEdgeGain(chkPersonnelLogEdgeGain.isSelected());
        options.setDisplayPersonnelLog(chkDisplayPersonnelLog.isSelected());
        options.setDisplayScenarioLog(chkDisplayScenarioLog.isSelected());
        options.setDisplayKillRecord(chkDisplayKillRecord.isSelected());

        // Personnel Information
        options.setUseTimeInService(chkUseTimeInService.isSelected());
        options.setTimeInServiceDisplayFormat(comboTimeInServiceDisplayFormat.getSelectedItem());
        options.setUseTimeInRank(chkUseTimeInRank.isSelected());
        options.setTimeInRankDisplayFormat(comboTimeInRankDisplayFormat.getSelectedItem());
        options.setTrackTotalEarnings(chkTrackTotalEarnings.isSelected());
        options.setTrackTotalXPEarnings(chkTrackTotalXPEarnings.isSelected());
        options.setShowOriginFaction(chkShowOriginFaction.isSelected());

        // Awards
        options.setAwardBonusStyle(comboAwardBonusStyle.getSelectedItem());
        options.setAwardTierSize((int) spnAwardTierSize.getValue());
        options.setEnableAutoAwards(chkEnableAutoAwards.isSelected());
        options.setIssuePosthumousAwards(chkIssuePosthumousAwards.isSelected());
        options.setIssueBestAwardOnly(chkIssueBestAwardOnly.isSelected());
        options.setIgnoreStandardSet(chkIgnoreStandardSet.isSelected());
        options.setEnableContractAwards(chkEnableContractAwards.isSelected());
        options.setEnableFactionHunterAwards(chkEnableFactionHunterAwards.isSelected());
        options.setEnableInjuryAwards(chkEnableInjuryAwards.isSelected());
        options.setEnableIndividualKillAwards(chkEnableIndividualKillAwards.isSelected());
        options.setEnableFormationKillAwards(chkEnableFormationKillAwards.isSelected());
        options.setEnableRankAwards(chkEnableRankAwards.isSelected());
        options.setEnableScenarioAwards(chkEnableScenarioAwards.isSelected());
        options.setEnableSkillAwards(chkEnableSkillAwards.isSelected());
        options.setEnableTheatreOfWarAwards(chkEnableTheatreOfWarAwards.isSelected());
        options.setEnableTimeAwards(chkEnableTimeAwards.isSelected());
        options.setEnableTrainingAwards(chkEnableTrainingAwards.isSelected());
        options.setEnableMiscAwards(chkEnableMiscAwards.isSelected());
        options.setAwardSetFilterList(txtAwardSetFilterList.getText());

        // Medical
        options.setUseAdvancedMedical(chkUseAdvancedMedical.isSelected());
        options.setHealingWaitingPeriod((int) spnHealWaitingPeriod.getValue());
        options.setNaturalHealingWaitingPeriod((int) spnNaturalHealWaitingPeriod.getValue());
        options.setMinimumHitsForVehicles((int) spnMinimumHitsForVehicles.getValue());
        options.setUseRandomHitsForVehicles(chkUseRandomHitsForVehicles.isSelected());
        options.setTougherHealing(chkUseTougherHealing.isSelected());
        options.setMaximumPatients((int) spnMaximumPatients.getValue());

        // Prisoners and Dependents
        options.setPrisonerCaptureStyle(comboPrisonerCaptureStyle.getSelectedItem());
        options.setDefaultPrisonerStatus(comboPrisonerStatus.getSelectedItem());
        options.setPrisonerBabyStatus(chkPrisonerBabyStatus.isSelected());
        options.setUseAtBPrisonerDefection(chkAtBPrisonerDefection.isSelected());
        options.setUseAtBPrisonerRansom(chkAtBPrisonerRansom.isSelected());
        options.setUseRandomDependentAddition(chkUseRandomDependentAddition.isSelected());
        options.setUseRandomDependentRemoval(chkUseRandomDependentRemoval.isSelected());

        // Salaries
        options.setDisableSecondaryRoleSalary(chkDisableSecondaryRoleSalary.isSelected());
        options.setSalaryAntiMekMultiplier((double) spnAntiMekSalary.getValue());
        options.setSalarySpecialistInfantryMultiplier((double) spnSpecialistInfantrySalary.getValue());

        options.setDisableSecondaryRoleSalary(chkDisableSecondaryRoleSalary.isSelected());
        options.setSalaryAntiMekMultiplier((double) spnAntiMekSalary.getValue());
        options.setSalarySpecialistInfantryMultiplier((double) spnSpecialistInfantrySalary.getValue());

        for (final Entry<SkillLevel, JSpinner> entry : spnSalaryExperienceMultipliers.entrySet()) {
            options.getSalaryXPMultipliers().put(entry.getKey(),
                (Double) entry.getValue().getValue());
        }

        for (final PersonnelRole personnelRole : PersonnelRole.values()) {
            options.setRoleBaseSalary(personnelRole,
                (double) spnBaseSalary[personnelRole.ordinal()].getValue());
        }
    }
}
