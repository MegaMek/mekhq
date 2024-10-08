package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.common.enums.SkillLevel;
import mekhq.campaign.personnel.Skills;
import mekhq.campaign.personnel.enums.*;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.LayoutStyle.ComponentPlacement;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

/**
 * Handles the Personnel tab of campaign options
 */
public class PersonnelTab {
    JFrame frame;
    String name;

    //start General Tab
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

    private JPanel personnelCleanUpPanel;
    private JCheckBox chkUsePersonnelRemoval;
    private JCheckBox chkUseRemovalExemptCemetery;
    private JCheckBox chkUseRemovalExemptRetirees;
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

    //start Administrators Tab
    private JCheckBox chkAdminsHaveNegotiation;
    private JCheckBox chkAdminExperienceLevelIncludeNegotiation;
    private JCheckBox chkAdminsHaveScrounge;
    private JCheckBox chkAdminExperienceLevelIncludeScrounge;
    //end Administrators Tab

    //start Awards Tab
    private JLabel lblAwardBonusStyle;
    private MMComboBox<AwardBonus> comboAwardBonusStyle;
    private JLabel lblAwardTierSize;
    private JSpinner spnAwardTierSize;
    private JCheckBox chkEnableAutoAwards;
    private JCheckBox chkIssuePosthumousAwards;
    private JCheckBox chkIssueBestAwardOnly;
    private JCheckBox chkIgnoreStandardSet;

    private JPanel autoAwardsFilterPanel;
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
    private JLabel lblAwardSetFilterList;
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
    PersonnelTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }


    /**
     * This method initializes the various checkboxes, labels, spinners, combo boxes, panels, and
     * other GUI components for different tabs.
     * It sets up the components for the General Tab, Personnel Logs Tab, Personnel Information Tab,
     * Administrators Tab, Awards Tab, Medical Tab, Prisoners and Dependents Tab, and the Salaries Tab.
     */
    protected void initialize() {
        //start General Tab
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

        personnelCleanUpPanel = new JPanel();
        chkUsePersonnelRemoval = new JCheckBox();
        chkUseRemovalExemptCemetery = new JCheckBox();
        chkUseRemovalExemptRetirees = new JCheckBox();
        //end General Tab

        //start Personnel Logs Tab
        chkUseTransfers = new JCheckBox();
        chkUseExtendedTOEForceName = new JCheckBox();
        chkPersonnelLogSkillGain = new JCheckBox();
        chkPersonnelLogAbilityGain = new JCheckBox();
        chkPersonnelLogEdgeGain = new JCheckBox();
        chkDisplayPersonnelLog = new JCheckBox();
        chkDisplayScenarioLog = new JCheckBox();
        chkDisplayKillRecord = new JCheckBox();
        //end Personnel Logs Tab

        //start Personnel Information Tab
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
        //end Personnel Information Tab

        //start Administrators Tab
        chkAdminsHaveNegotiation = new JCheckBox();
        chkAdminExperienceLevelIncludeNegotiation = new JCheckBox();
        chkAdminsHaveScrounge = new JCheckBox();
        chkAdminExperienceLevelIncludeScrounge = new JCheckBox();
        //end Administrators Tab

        //start Awards Tab
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

        autoAwardsFilterPanel = new JPanel();
        lblAwardSetFilterList = new JLabel();
        txtAwardSetFilterList = new JTextArea();
        //end Awards Tab

        //start Medical Tab
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
        //end Medical Tab

        //start Prisoners & Dependents Tab
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
        //end Prisoners & Dependents Tab

        //start Salaries Tab
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
        //end Salaries Tab
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

    JPanel createGeneralTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("GeneralTab",
            getImageDirectory() + "logo_circinus_federation.png",
            false, "", true);

        // Contents
        chkUseTactics = createCheckBox("UseTactics", null);
        chkUseInitiativeBonus = createCheckBox("UseInitiativeBonus", null);
        chkUseToughness = createCheckBox("UseToughness", null);
        chkUseRandomToughness = createCheckBox("UseRandomToughness", null);
        chkUseArtillery = createCheckBox("UseArtillery", null);
        chkUseAbilities = createCheckBox("UseAbilities", null);
        chkUseEdge = createCheckBox("UseEdge", null);
        chkUseSupportEdge = createCheckBox("UseSupportEdge", null);
        chkUseImplants = createCheckBox("UseImplants", null);
        chkUseAlternativeQualityAveraging = createCheckBox("UseAlternativeQualityAveraging", null);

        personnelCleanUpPanel = createPersonnelCleanUpPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("GeneralTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseTactics)
                .addComponent(chkUseInitiativeBonus)
                .addComponent(chkUseToughness)
                .addComponent(chkUseRandomToughness)
                .addComponent(chkUseArtillery)
                .addComponent(chkUseAbilities)
                .addComponent(chkUseEdge)
                .addComponent(chkUseSupportEdge)
                .addComponent(chkUseImplants)
                .addComponent(chkUseAlternativeQualityAveraging)
                .addComponent(personnelCleanUpPanel));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(chkUseTactics)
                .addComponent(chkUseInitiativeBonus)
                .addComponent(chkUseToughness)
                .addComponent(chkUseRandomToughness)
                .addComponent(chkUseArtillery)
                .addComponent(chkUseAbilities)
                .addComponent(chkUseEdge)
                .addComponent(chkUseSupportEdge)
                .addComponent(chkUseImplants)
                .addComponent(chkUseAlternativeQualityAveraging)
                .addComponent(personnelCleanUpPanel));

        // Create Parent Panel and return
        return createParentPanel(panel, "GeneralTab");
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
        chkUsePersonnelRemoval = createCheckBox("UsePersonnelRemoval", null);
        chkUseRemovalExemptCemetery = createCheckBox("UseRemovalExemptCemetery", null);
        chkUseRemovalExemptRetirees = createCheckBox("UseRemovalExemptRetirees", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PersonnelCleanUpPanel", true,
            "PersonnelCleanUpPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUsePersonnelRemoval)
                .addComponent(chkUseRemovalExemptCemetery)
                .addComponent(chkUseRemovalExemptRetirees));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkUsePersonnelRemoval)
                .addComponent(chkUseRemovalExemptCemetery)
                .addComponent(chkUseRemovalExemptRetirees));

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelCleanUpPanel");
    }

    /**
     * Creates a tab for managing personnel logs.
     *
     * @return a {@link JPanel} representing the Personnel Logs Tab panel
     */
    JPanel createPersonnelLogsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PersonnelLogsTab",
            getImageDirectory() + "logo_clan_coyote.png",
            false, "", true);

        // Contents
        chkUseTransfers = createCheckBox("UseTransfers", null);
        chkUseExtendedTOEForceName = createCheckBox("UseExtendedTOEForceName", null);
        chkPersonnelLogSkillGain = createCheckBox("PersonnelLogSkillGain", null);
        chkPersonnelLogAbilityGain = createCheckBox("PersonnelLogAbilityGain", null);
        chkPersonnelLogEdgeGain = createCheckBox("PersonnelLogEdgeGain", null);
        chkDisplayPersonnelLog = createCheckBox("DisplayPersonnelLog", null);
        chkDisplayScenarioLog = createCheckBox("DisplayScenarioLog", null);
        chkDisplayKillRecord = createCheckBox("DisplayKillRecord", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PersonnelLogsTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseTransfers)
                .addComponent(chkUseExtendedTOEForceName)
                .addComponent(chkPersonnelLogSkillGain)
                .addComponent(chkPersonnelLogAbilityGain)
                .addComponent(chkPersonnelLogEdgeGain)
                .addComponent(chkDisplayPersonnelLog)
                .addComponent(chkDisplayScenarioLog)
                .addComponent(chkDisplayKillRecord));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(chkUseTransfers)
                .addComponent(chkUseExtendedTOEForceName)
                .addComponent(chkPersonnelLogSkillGain)
                .addComponent(chkPersonnelLogAbilityGain)
                .addComponent(chkPersonnelLogEdgeGain)
                .addComponent(chkDisplayPersonnelLog)
                .addComponent(chkDisplayScenarioLog)
                .addComponent(chkDisplayKillRecord));

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelLogsTab");
    }

    /**
     * Creates a panel for the Personnel Information Tab in the application.
     *
     * @return a {@link JPanel} representing the Personnel Information Tab panel
     */
    JPanel createPersonnelInformationTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PersonnelInformation",
            getImageDirectory() + "logo_comstar.png",
            false, "", true);

        // Contents
        chkUseTimeInService = createCheckBox("UseTimeInService", null);
        lblTimeInServiceDisplayFormat = createLabel("TimeInServiceDisplayFormat", null);
        chkUseTimeInRank = createCheckBox("UseTimeInRank", null);
        lblTimeInRankDisplayFormat = createLabel("TimeInRankDisplayFormat", null);
        chkTrackTotalEarnings = createCheckBox("TrackTotalEarnings", null);
        chkTrackTotalXPEarnings = createCheckBox("TrackTotalXPEarnings", null);
        chkShowOriginFaction = createCheckBox("ShowOriginFaction", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PersonnelInformation", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseTimeInService)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblTimeInServiceDisplayFormat)
                    .addComponent(comboTimeInServiceDisplayFormat))
                .addComponent(chkUseTimeInRank)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblTimeInRankDisplayFormat)
                    .addComponent(comboTimeInRankDisplayFormat))
                .addComponent(chkTrackTotalEarnings)
                .addComponent(chkTrackTotalXPEarnings)
                .addComponent(chkShowOriginFaction));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(chkUseTimeInService)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblTimeInServiceDisplayFormat)
                    .addComponent(comboTimeInServiceDisplayFormat)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(chkUseTimeInRank)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblTimeInRankDisplayFormat)
                    .addComponent(comboTimeInRankDisplayFormat)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(chkTrackTotalEarnings)
                .addComponent(chkTrackTotalXPEarnings)
                .addComponent(chkShowOriginFaction));

        // Create Parent Panel and return
        return createParentPanel(panel, "PersonnelInformation");
    }

    /**
     * Creates a panel for the Administrators Tab in the application.
     * <p>
     * This method constructs the header panel and checkbox components for administrator settings
     * including negotiation and scrounging options.
     *
     * @return a {@link JPanel} representing the Administrators Tab panel
     */
    JPanel createAdministratorsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("AdministratorsTab",
            getImageDirectory() + "logo_clan_diamond_sharks.png",
            false, "", true);

        // Contents
        chkAdminsHaveNegotiation = createCheckBox("AdminsHaveNegotiation", null);
        chkAdminExperienceLevelIncludeNegotiation = createCheckBox("AdminExperienceLevelIncludeNegotiation", null);
        chkAdminsHaveScrounge = createCheckBox("AdminsHaveScrounge", null);
        chkAdminExperienceLevelIncludeScrounge = createCheckBox("AdminExperienceLevelIncludeScrounge", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("AdministratorsTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkAdminsHaveNegotiation)
                .addComponent(chkAdminExperienceLevelIncludeNegotiation)
                .addComponent(chkAdminsHaveScrounge)
                .addComponent(chkAdminExperienceLevelIncludeScrounge));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(chkAdminsHaveNegotiation)
                .addComponent(chkAdminExperienceLevelIncludeNegotiation)
                .addComponent(chkAdminsHaveScrounge)
                .addComponent(chkAdminExperienceLevelIncludeScrounge));

        // Create Parent Panel and return
        return createParentPanel(panel, "AdministratorsTab");
    }

    /**
     * Creates the Awards Tab panel with various components like labels, checkboxes, and filter options.
     *
     * @return the {@link JPanel} representing the Awards Tab panel
     */
    JPanel createAwardsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("AwardsTab",
            getImageDirectory() + "logo_draconis_combine.png",
            false, "", true);

        // Contents
        lblAwardBonusStyle = createLabel("AwardBonusStyle", null);
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

        lblAwardTierSize = createLabel("AwardTierSize", null);
        spnAwardTierSize = createSpinner("AwardTierSize", null,
            5, 1, 100, 1);

        chkEnableAutoAwards = createCheckBox("EnableAutoAwards", null);

        chkIssuePosthumousAwards = createCheckBox("IssuePosthumousAwards", null);

        chkIssueBestAwardOnly = createCheckBox("IssueBestAwardOnly", null);

        chkIgnoreStandardSet = createCheckBox("IgnoreStandardSet", null);

        autoAwardsFilterPanel = createAutoAwardsFilterPanel();

        lblAwardSetFilterList = createLabel("AwardSetFilterList", null);
        txtAwardSetFilterList = new JTextArea(5, 20);
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
        final JPanel panel = createStandardPanel("AwardsTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAwardBonusStyle)
                    .addComponent(comboAwardBonusStyle))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAwardTierSize)
                    .addComponent(spnAwardTierSize))
                .addComponent(chkEnableAutoAwards)
                .addComponent(chkIssuePosthumousAwards)
                .addComponent(chkIssueBestAwardOnly)
                .addComponent(chkIgnoreStandardSet)
                .addComponent(autoAwardsFilterPanel)
                .addComponent(lblAwardSetFilterList)
                .addComponent(txtAwardSetFilterList));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAwardBonusStyle)
                    .addComponent(comboAwardBonusStyle)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAwardTierSize)
                    .addComponent(spnAwardTierSize)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(chkEnableAutoAwards)
                .addComponent(chkIssuePosthumousAwards)
                .addComponent(chkIssueBestAwardOnly)
                .addComponent(chkIgnoreStandardSet)
                .addComponent(autoAwardsFilterPanel)
                .addComponent(lblAwardSetFilterList)
                .addComponent(txtAwardSetFilterList));

        // Create Parent Panel and return
        return createParentPanel(panel, "AwardsTab");
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
        chkEnableContractAwards = createCheckBox("EnableContractAwards", null);
        chkEnableFactionHunterAwards = createCheckBox("EnableFactionHunterAwards", null);
        chkEnableInjuryAwards = createCheckBox("EnableInjuryAwards", null);
        chkEnableIndividualKillAwards = createCheckBox("EnableIndividualKillAwards", null);
        chkEnableFormationKillAwards = createCheckBox("EnableFormationKillAwards", null);
        chkEnableRankAwards = createCheckBox("EnableRankAwards", null);
        chkEnableScenarioAwards = createCheckBox("EnableScenarioAwards", null);
        chkEnableSkillAwards = createCheckBox("EnableSkillAwards", null);
        chkEnableTheatreOfWarAwards = createCheckBox("EnableTheatreOfWarAwards", null);
        chkEnableTimeAwards = createCheckBox("EnableTimeAwards", null);
        chkEnableTrainingAwards = createCheckBox("EnableTrainingAwards", null);
        chkEnableMiscAwards = createCheckBox("EnableMiscAwards", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("AutoAwardsFilterPanel", true, "AutoAwardsFilterPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createParallelGroup(Alignment.BASELINE)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(chkEnableContractAwards)
                    .addComponent(chkEnableFactionHunterAwards)
                    .addComponent(chkEnableInjuryAwards)
                    .addComponent(chkEnableIndividualKillAwards))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(chkEnableRankAwards)
                    .addComponent(chkEnableScenarioAwards)
                    .addComponent(chkEnableSkillAwards)
                    .addComponent(chkEnableFormationKillAwards))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(chkEnableTheatreOfWarAwards)
                    .addComponent(chkEnableTimeAwards)
                    .addComponent(chkEnableTrainingAwards)
                    .addComponent(chkEnableMiscAwards)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup()
                    .addComponent(chkEnableContractAwards)
                    .addComponent(chkEnableFactionHunterAwards)
                    .addComponent(chkEnableInjuryAwards)
                    .addComponent(chkEnableIndividualKillAwards))
                .addGroup(layout.createParallelGroup()
                    .addComponent(chkEnableFormationKillAwards)
                    .addComponent(chkEnableRankAwards)
                    .addComponent(chkEnableScenarioAwards)
                    .addComponent(chkEnableSkillAwards))
                .addGroup(layout.createParallelGroup()
                    .addComponent(chkEnableTheatreOfWarAwards)
                    .addComponent(chkEnableTimeAwards)
                    .addComponent(chkEnableTrainingAwards)
                    .addComponent(chkEnableMiscAwards)));

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
    JPanel createPrisonersAndDependentsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PrisonersAndDependentsTab",
            getImageDirectory() + "logo_clan_fire_mandrills.png",
            false, "", true);

        // Contents
        prisonerPanel = createPrisonersPanel();
        dependentsPanel = createDependentsPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("PrisonersAndDependentsTab", true,
            "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(prisonerPanel)
                    .addComponent(dependentsPanel)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(prisonerPanel)
                    .addComponent(dependentsPanel)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

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
        lblPrisonerCaptureStyle = createLabel("PrisonerCaptureStyle", null);
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

        lblPrisonerStatus = createLabel("PrisonerStatus", null);

        chkPrisonerBabyStatus = createCheckBox("PrisonerBabyStatus", null);
        chkAtBPrisonerDefection = createCheckBox("AtBPrisonerDefection", null);
        chkAtBPrisonerRansom = createCheckBox("AtBPrisonerRansom", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("PrisonersPanel", true, "PrisonersPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPrisonerCaptureStyle)
                    .addComponent(comboPrisonerCaptureStyle))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPrisonerStatus)
                    .addComponent(comboPrisonerStatus))
                .addComponent(chkPrisonerBabyStatus)
                .addComponent(chkAtBPrisonerDefection)
                .addComponent(chkAtBPrisonerRansom));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPrisonerCaptureStyle)
                    .addComponent(comboPrisonerCaptureStyle)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPrisonerStatus)
                    .addComponent(comboPrisonerStatus)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(chkPrisonerBabyStatus)
                .addComponent(chkAtBPrisonerDefection)
                .addComponent(chkAtBPrisonerRansom));

        return panel;
    }

    /**
     * Creates a panel with checkboxes for setting dependent options.
     *
     * @return a {@link JPanel} containing checkboxes for setting dependent options
     */
    private JPanel createDependentsPanel() {
        // Contents
        chkUseRandomDependentAddition = createCheckBox("UseRandomDependentAddition", null);
        chkUseRandomDependentRemoval = createCheckBox("UseRandomDependentRemoval", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("DependentsPanel", true, "DependentsPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseRandomDependentAddition)
                .addComponent(chkUseRandomDependentRemoval));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(chkUseRandomDependentAddition)
                .addComponent(chkUseRandomDependentRemoval));

        return panel;
    }

    /**
     * Creates a panel for the Medical Tab in the application.
     *
     * @return a {@link JPanel} representing the Medical Tab containing settings for medical options.
     */
    JPanel createMedicalTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("MedicalTab",
            getImageDirectory() + "logo_duchy_of_tamarind_abbey.png",
            false, "", true);

        // Contents
        chkUseAdvancedMedical = createCheckBox("UseAdvancedMedical", null);

        lblHealWaitingPeriod = createLabel("HealWaitingPeriod", null);
        spnHealWaitingPeriod = createSpinner("HealWaitingPeriod", null,
            1, 1, 30, 1);

        lblNaturalHealWaitingPeriod = createLabel("NaturalHealWaitingPeriod", null);
        spnNaturalHealWaitingPeriod = createSpinner("NaturalHealWaitingPeriod", null,
            1, 1, 365, 1);

        lblMinimumHitsForVehicles = createLabel("MinimumHitsForVehicles", null);
        spnMinimumHitsForVehicles = createSpinner("MinimumHitsForVehicles", null,
            1, 1, 5, 1);

        chkUseRandomHitsForVehicles = createCheckBox("UseRandomHitsForVehicles", null);

        chkUseTougherHealing = createCheckBox("UseTougherHealing", null);

        lblMaximumPatients = createLabel("MaximumPatients", null);
        spnMaximumPatients = createSpinner("MaximumPatients", null,
            25, 1, 100, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("MedicalTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseAdvancedMedical)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblHealWaitingPeriod)
                    .addComponent(spnHealWaitingPeriod))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblNaturalHealWaitingPeriod)
                    .addComponent(spnNaturalHealWaitingPeriod))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMinimumHitsForVehicles)
                    .addComponent(spnMinimumHitsForVehicles))
                .addComponent(chkUseRandomHitsForVehicles)
                .addComponent(chkUseTougherHealing)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMaximumPatients)
                    .addComponent(spnMaximumPatients)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(chkUseAdvancedMedical)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblHealWaitingPeriod)
                    .addComponent(spnHealWaitingPeriod)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblNaturalHealWaitingPeriod)
                    .addComponent(spnNaturalHealWaitingPeriod)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblMinimumHitsForVehicles)
                    .addComponent(spnMinimumHitsForVehicles)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(chkUseRandomHitsForVehicles)
                .addComponent(chkUseTougherHealing)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblMaximumPatients)
                    .addComponent(spnMaximumPatients)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        // Create Parent Panel and return
        return createParentPanel(panel, "MedicalTab");
    }

    /**
     * Creates a salary configuration tab for managing salary settings such as multipliers and base salaries.
     *
     * @return a {@link JPanel} representing the salary tab
     */
    JPanel createSalariesTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("SalariesTab",
            getImageDirectory() + "logo_clan_ghost_bear.png",
            false, "", true);

        // Contents
        chkDisableSecondaryRoleSalary = createCheckBox("DisableSecondaryRoleSalary", null);
        pnlSalaryMultipliersPanel = createSalaryMultipliersPanel();
        pnlSalaryExperienceMultipliersPanel = createExperienceMultipliersPanel();
        pnlSalaryBaseSalaryPanel = createBaseSalariesPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("SalariesTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkDisableSecondaryRoleSalary)
                .addComponent(pnlSalaryMultipliersPanel)
                .addComponent(pnlSalaryExperienceMultipliersPanel)
                .addComponent(pnlSalaryBaseSalaryPanel));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(chkDisableSecondaryRoleSalary)
                .addComponent(pnlSalaryMultipliersPanel)
                .addComponent(pnlSalaryExperienceMultipliersPanel)
                .addComponent(pnlSalaryBaseSalaryPanel));

        // Create Parent Panel and return
        return createParentPanel(panel, "SalariesTab");
    }

    /**
     * Creates a panel for configuring salary multipliers for different personnel roles.
     *
     * @return a {@link JPanel} containing the salary multipliers configuration panel
     */
    private JPanel createSalaryMultipliersPanel() {
        // Contents
        lblAntiMekSalary = createLabel("AntiMekSalary", null);
        spnAntiMekSalary = createSpinner("AntiMekSalary", null,
            0, 0, 100, 0.05);

        lblSpecialistInfantrySalary = createLabel("SpecialistInfantrySalary", null);
        spnSpecialistInfantrySalary = createSpinner("SpecialistInfantrySalary", null,
            0, 0, 100, 0.05);

        // Layout the Panel
        final JPanel panel = createStandardPanel("SalaryMultipliersPanel", true,
            "SalaryMultipliersPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAntiMekSalary)
                    .addComponent(spnAntiMekSalary)
                    .addComponent(lblSpecialistInfantrySalary)
                    .addComponent(spnSpecialistInfantrySalary)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAntiMekSalary)
                    .addComponent(spnAntiMekSalary)
                    .addComponent(lblSpecialistInfantrySalary)
                    .addComponent(spnSpecialistInfantrySalary)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

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
            final JLabel label = createLabel("SkillLevel" + skillLevel.toString(), null);
            lblSalaryExperienceMultipliers.put(skillLevel, label);

            final JSpinner spinner = createSpinner("SkillLevel" + skillLevel, null,
                0, 0, 10, 0.05);
            spnSalaryExperienceMultipliers.put(skillLevel, spinner);

        }

        // Layout the Panel
        final JPanel panel = createStandardPanel("ExperienceMultipliersPanel", true,
            "ExperienceMultipliersPanel");
        final GroupLayout layout = createStandardLayout(panel);
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
                horizontalSequentialGroup.addPreferredGap(ComponentPlacement.RELATED, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
                horizontalSequentialGroup.addComponent(spnSalaryExperienceMultipliers.get(skillLevels[index]));
                if (i != (columns - 1)) {
                    horizontalSequentialGroup.addGap(200);
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
            String componentName = "BaseSalary" + personnelRole.toString();
            componentName = componentName.replaceAll(" ", "");

            final JLabel label = createLabel(componentName, null);

            final JSpinner spinner = createSpinner(componentName, null,
                0.0, 0.0, 1000000, 10.0);

            // Component Tracking Assignment
            lblBaseSalary[personnelRole.ordinal()] = label;
            spnBaseSalary[personnelRole.ordinal()] = spinner;
        }

        // Layout the Panel
        final JPanel panel = createStandardPanel("BaseSalariesPanel", true,
            "BaseSalariesPanel");
        final GroupLayout layout = createStandardLayout(panel);
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
                    horizontalSequentialGroup.addPreferredGap(ComponentPlacement.RELATED, Short.MAX_VALUE, Short.MAX_VALUE);
                    horizontalSequentialGroup.addComponent(spnBaseSalary[index]);
                    if (i != (columns - 1)) {
                        horizontalSequentialGroup.addGap(100);
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
}
