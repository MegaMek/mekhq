package mekhq.gui.panes.campaignOptions.tabs;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;

import javax.swing.*;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Supplier;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.createSubTabs;

public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    private static final MMLogger logger = MMLogger.create(CampaignOptionsPane.class);
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final Campaign campaign;

    public CampaignOptionsPane(final JFrame frame, final Campaign campaign) {
        super(frame, resources, "campaignOptionsDialog");
        this.campaign = campaign;

        initialize();
    }

    @Override
    protected void initialize() {
        // General
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 5,
            resources.getString("generalPanel.title")), createGeneralTab());

        // Combat Readiness
        createTab("combatReadinessParentTab", this::createCombatReadinessParentTab);

        // Human Resources
        createTab("humanResourcesParentTab", this::createHumanResourcesParentTab);

        // Unit Development
        createTab("unitDevelopmentParentTab", this::createUnitDevelopmentParentTab);

        // Logistics and Maintenance
        createTab("logisticsAndMaintenanceParentTab", this::createLogisticsAndMaintenanceParentTab);

        // Strategic Operations
        createTab("strategicOperationsParentTab", this::createStrategicOperationsParentTab);
    }

    /**
     * Creates a tab and adds it to the TabbedPane.
     *
     * @param resourceName the name of the resource used to create the tab's title
     * @param tabCreator   a supplier that creates the TabbedPane for the tab
     */
    private void createTab(String resourceName, Supplier<JTabbedPane> tabCreator) {
        JTabbedPane createdTab = tabCreator.get();

        JScrollPane tabScrollPane = new JScrollPane(createdTab,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Increase vertical scroll speed
        int verticalScrollSpeed = 20;
        tabScrollPane.getVerticalScrollBar().setUnitIncrement(verticalScrollSpeed);

        // Increase horizontal scroll speed
        int horizontalScrollSpeed = 20;
        tabScrollPane.getHorizontalScrollBar().setUnitIncrement(horizontalScrollSpeed);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 5,
            resources.getString(resourceName + ".title")), tabScrollPane);
    }

    /**
     * Creates the General tab.
     *
     * @return a {@link JScrollPane} containing the General tab
     */
    private JScrollPane createGeneralTab() {
        GeneralTab generalTab = new GeneralTab(campaign, getFrame(), "generalTab");
        JPanel createdGeneralTab = generalTab.createGeneralTab();

        return new JScrollPane(createdGeneralTab,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private JTabbedPane createCombatReadinessParentTab() {
        // Tech Limits
        // Random Assignment Tables
        // Rulesets

        // Parent Tab
        JTabbedPane combatReadinessParentTab = new JTabbedPane();

        TechLimitsTab techLimitsTab = new TechLimitsTab(getFrame(),
            "techLimitsTab");

        combatReadinessParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("TechLimitsParentTab.title")), techLimitsTab.createTechLimitsTab());

        return combatReadinessParentTab;
    }

    private JTabbedPane createHumanResourcesParentTab() {
        //    Personnel
        //    Life Paths
        //    Turnover and Retention
        //    Name and Portrait Generation
        //    Rank Systems

        // Parent Tab
        JTabbedPane humanResourcesParentTab = new JTabbedPane();

        return humanResourcesParentTab;
    }

    private JTabbedPane createUnitDevelopmentParentTab() {
        //    Experience
        //    Skills
        //    Skill Randomization
        //    SPAs

        // Parent Tab
        JTabbedPane unitDevelopmentParentTab = new JTabbedPane();

        return unitDevelopmentParentTab;
    }

    /**
     * Creates the Logistics and Maintenance parent tab for the Campaign Options Pane.
     *
     * @return the created {@link JTabbedPane} representing the Logistics and Maintenance parent tab
     */
    private JTabbedPane createLogisticsAndMaintenanceParentTab() {
        // Parent Tab
        JTabbedPane logisticsAndMaintenanceParentTab = new JTabbedPane();

        // Repair and Maintenance
        RepairAndMaintenanceTab repairAndMaintenanceTab = new RepairAndMaintenanceTab(getFrame(),
            "repairAndMaintenanceTab");

        JTabbedPane repairAndMaintenanceContentTabs = createSubTabs(Map.of(
            "repairTab", repairAndMaintenanceTab.createRepairTab(),
            "maintenanceTab", repairAndMaintenanceTab.createMaintenanceTab()));

        logisticsAndMaintenanceParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("repairAndMaintenanceContentTabs.title")), repairAndMaintenanceContentTabs);

        // Supplies and Acquisition
        SuppliesAndAcquisitionTab suppliesAndAcquisitionTab = new SuppliesAndAcquisitionTab(getFrame(),
            "suppliesAndAcquisitionTab");

        JTabbedPane suppliesAndAcquisitionContentTabs = createSubTabs(Map.of(
            "acquisitionTab", suppliesAndAcquisitionTab.createAcquisitionTab(),
            "deliveryTab", suppliesAndAcquisitionTab.createDeliveryTab(),
            "planetaryAcquisitionTab", suppliesAndAcquisitionTab.createPlanetaryAcquisitionTab()));

        // Add tabs
        logisticsAndMaintenanceParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("suppliesAndAcquisitionTab.title")), suppliesAndAcquisitionContentTabs);
        logisticsAndMaintenanceParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("repairAndMaintenanceContentTabs.title")), repairAndMaintenanceContentTabs);

        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("logisticsAndMaintenanceParentTab.title")), logisticsAndMaintenanceParentTab);

        return logisticsAndMaintenanceParentTab;
    }

    private JTabbedPane createStrategicOperationsParentTab() {
        //    Finances
        //    Mercenary
        //    Markets

        // Parent Tab
        JTabbedPane strategicOperationsParentTab = new JTabbedPane();

        return strategicOperationsParentTab;
    }

    private void setOptions() {
        // TODO this is where we update the dialog based on current campaign settings.
    }

    private void updateOptions() {
        // TODO this is where we update campaign values based on the dialog values
    }
}
