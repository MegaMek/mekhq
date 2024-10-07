package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.personnel.enums.TimeInDisplayFormat;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

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
    //end Awards Tab

    //start Administrators Tab
    //end Administrators Tab

    //start Medical Tab
    //end Medical Tab

    //start Prisoners & Dependents Tab
    //end Prisoners & Dependents Tab

    //start Salaries Tab
    //end Salaries Tab

    /**
     * Represents a tab for repair and maintenance in an application.
     */
    public PersonnelTab(JFrame frame, String name) {
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

        //start Awards Tab
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
        //end Awards Tab

        //start Personnel Information Tab
        //end Personnel Information Tab

        //start Administrators Tab
        //end Administrators Tab

        //start Medical Tab
        //end Medical Tab

        //start Prisoners & Dependents Tab
        //end Prisoners & Dependents Tab

        //start Salaries Tab
        //end Salaries Tab
    }

    public JPanel createGeneralTab() {
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
                .addComponent(chkUseAlternativeQualityAveraging));

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
                .addComponent(chkUseAlternativeQualityAveraging));

        // Create Parent Panel and return
        return createParentPanel(panel, "GeneralTab");
    }

    public JPanel createPersonnelLogsTab() {
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

    public JPanel createPersonnelInformationTab() {
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
}
