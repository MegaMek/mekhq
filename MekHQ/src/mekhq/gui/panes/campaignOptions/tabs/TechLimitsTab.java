package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.campaignOptions.CampaignOptions;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class TechLimitsTab {
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

    public TechLimitsTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
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
                .addComponent(allowCanonRefitOnlyBox)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblChoiceTechLevel)
                    .addComponent(choiceTechLevel))
                .addComponent(variableTechLevelBox)
                .addComponent(useAmmoByTypeBox));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel)
                .addComponent(limitByYearBox)
                .addComponent(disallowExtinctStuffBox)
                .addComponent(allowClanPurchasesBox)
                .addComponent(allowCanonRefitOnlyBox)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblChoiceTechLevel)
                    .addComponent(choiceTechLevel)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(variableTechLevelBox)
                .addComponent(useAmmoByTypeBox));

        // Create Parent Panel and return
        JPanel parentPanel = createParentPanel(panel, "TechLimitsTab");

        // Create a panel for the quote
        JPanel quotePanel = new JPanel();
        JLabel quote = new JLabel(String.format("<html><i><center>%s</i></center></html>",
            resources.getString("TechLimitsTab.border")));
        quotePanel.add(parentPanel);
        quotePanel.add(quote);

        // Reorganize mainPanel to include quotePanel at bottom
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setName("TechLimitsTab");
        contentPanel.add(parentPanel, BorderLayout.CENTER);
        contentPanel.add(quotePanel, BorderLayout.SOUTH);

        // Create a wrapper panel for its easy alignment controls
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        wrapperPanel.add(contentPanel, gbc);

        return wrapperPanel;
    }

    protected void initialize() {
        // Tech Limits Tab
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

        // Random Assignment Tables Tab

        // Rulesets Tab
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
