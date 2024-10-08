package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.personnel.enums.AwardBonus;
import mekhq.campaign.personnel.enums.PrisonerCaptureStyle;
import mekhq.campaign.personnel.enums.PrisonerStatus;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;

import static megamek.client.ui.WrapLayout.wordWrap;
import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

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
    //end Salaries Tab

    /**
     * Represents a tab for repair and maintenance in an application.
     */
    private PersonnelTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

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

        lblAwardSetFilterList = new JLabel();
        txtAwardSetFilterList = new JTextArea();
        //end Awards Tab

        //start Medical Tab
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

    private JPanel createGeneralTab() {
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
        final JPanel panel = createStandardPanel("PersonnelCleanUpPanel", true, "");
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("PersonnelCleanUpPanel.title")));
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

    private JPanel createPersonnelLogsTab() {
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

    private JPanel createPersonnelInformationTab() {
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

    private JPanel createAdministratorsTab() {
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

    private JPanel createAwardsTab() {
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

        lblAwardTierSize = createLabel("", null);
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
            wordWrap(resources.getString("lblAwardSetFilterList.toolTipText")));
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
        final JPanel panel = createStandardPanel("AutoAwardsFilterPanel", true, "");
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("autoAwardsFilterPanel.title")));
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

        // Create Parent Panel and return
        return createParentPanel(panel, "AutoAwardsFilterPanel");
    }

    private JPanel createPrisonersAndDependentsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PrisonersAndDependentsTab",
            getImageDirectory() + "logo_clan_fire_mandrills.png",
            false, "", true);

        // Contents
        prisonerPanel = createPrisonersPanel();
        dependentsPanel = createDependentsPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("PrisonersAndDependentsTab", true, "");
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
        final JPanel panel = createStandardPanel("PrisonersPanel", true, "");
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("PrisonersPanel.title")));
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

        // Create Parent Panel and return
        return createParentPanel(panel, "PrisonersPanel");
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
        final JPanel panel = createStandardPanel("DependentsPanel", true, "");
        panel.setBorder(BorderFactory.createTitledBorder(resources.getString("DependentsPanel.title")));
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

        // Create Parent Panel and return
        return createParentPanel(panel, "DependentsPanel");
    }
}
