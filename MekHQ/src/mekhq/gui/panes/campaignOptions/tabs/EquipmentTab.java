package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.campaignOptions.CampaignOptions;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class EquipmentTab {
    JFrame frame;
    String name;

    //start Tech Limits Tab
    private JCheckBox limitByYearBox;
    private JCheckBox disallowExtinctStuffBox;
    private JCheckBox allowClanPurchasesBox;
    private JCheckBox allowISPurchasesBox;
    private JCheckBox allowCanonOnlyBox;
    private JCheckBox allowCanonRefitOnlyBox;
    private JLabel lblChoiceTechLevel;
    private MMComboBox<String> choiceTechLevel;
    private JCheckBox variableTechLevelBox;
    private JCheckBox useAmmoByTypeBox;
    //end Tech Limits Tab

    //start Random Assignment Tables Tab
    //end Random Assignment Tables Tab

    //start Rulesets Tab
    //end Rulesets Tab

    public EquipmentTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    /**
     * Calls the initialization methods for the different tabs.
     */
    void initialize() {
        initializeTechLimitsTab();
        initializeRandomAssignmentTablesTab();
    }

    /**
     * Initializes the components of the TechLimitsTab.
     * This panel contains various controls for setting technological limits.
     */
    private void initializeTechLimitsTab() {
        limitByYearBox = new JCheckBox();
        disallowExtinctStuffBox = new JCheckBox();
        allowClanPurchasesBox = new JCheckBox();
        allowISPurchasesBox = new JCheckBox();
        allowCanonOnlyBox = new JCheckBox();
        allowCanonRefitOnlyBox = new JCheckBox();
        lblChoiceTechLevel = new JLabel();
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());
        variableTechLevelBox = new JCheckBox();
        useAmmoByTypeBox = new JCheckBox();
    }

    /**
     * Initializes the components of the RandomAssignmentTablesTab.
     * This panel would typically contain controls related to random assignment tables.
     */
    private void initializeRandomAssignmentTablesTab() {
    }

    /**
     * Creates a {@link JPanel} representing the tech limits tab.
     * This method constructs various components including checkboxes, labels, and combo boxes
     * to customize the tech limit settings.
     *
     * @return a {@link JPanel} containing the technical limits tab with all its configured components
     */
    JPanel createTechLimitsTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("TechLimitsTab",
            getImageDirectory() + "logo_clan_cloud_cobra.png", false,
            "", true);

        // Limit Parts/Units by Year
        limitByYearBox = createCheckBox("LimitByYearBox", null);

        // Disallow Extinct Units/Parts
        disallowExtinctStuffBox = createCheckBox("DisallowExtinctStuffBox", null);

        // Allow Clan/Inner Sphere Purchases
        allowClanPurchasesBox = createCheckBox("AllowClanPurchasesBox", null);
        allowISPurchasesBox = createCheckBox("AllowISPurchasesBox", null);

        // Canon Purchases/Refits
        allowCanonOnlyBox = createCheckBox("AllowCanonOnlyBox", null);
        allowCanonRefitOnlyBox = createCheckBox("AllowCanonRefitOnlyBox", null);

        // Maximum Tech Level
        lblChoiceTechLevel = createLabel("ChoiceTechLevel", null);
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());

        // Variable Tech Level
        variableTechLevelBox = createCheckBox("VariableTechLevelBox", null);

        // Ammo by Type
        useAmmoByTypeBox = createCheckBox("UseAmmoByTypeBox", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("TechLimitsTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(limitByYearBox)
                .addComponent(disallowExtinctStuffBox)
                .addComponent(allowClanPurchasesBox)
                .addComponent(allowISPurchasesBox)
                .addComponent(allowCanonOnlyBox)
                .addComponent(allowCanonRefitOnlyBox)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblChoiceTechLevel)
                    .addComponent(choiceTechLevel))
                .addComponent(variableTechLevelBox)
                .addComponent(useAmmoByTypeBox));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addComponent(limitByYearBox)
                .addComponent(disallowExtinctStuffBox)
                .addComponent(allowClanPurchasesBox)
                .addComponent(allowISPurchasesBox)
                .addComponent(allowCanonOnlyBox)
                .addComponent(allowCanonRefitOnlyBox)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblChoiceTechLevel)
                    .addComponent(choiceTechLevel)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(variableTechLevelBox)
                .addComponent(useAmmoByTypeBox));

        // Create Parent Panel and return
        return createParentPanel(panel, "TechLimitsTab");
    }

    /**
     * @return a {@link DefaultComboBoxModel} containing options for maximum technology levels.
     */
    private static DefaultComboBoxModel<String> getMaximumTechLevelOptions() {
        DefaultComboBoxModel<String> maximumTechLevelModel = new DefaultComboBoxModel<>();

        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_INTRO));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_STANDARD));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_ADVANCED));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_EXPERIMENTAL));
        maximumTechLevelModel.addElement(CampaignOptions.getTechLevelName(CampaignOptions.TECH_UNOFFICIAL));

        return maximumTechLevelModel;
    }
}
