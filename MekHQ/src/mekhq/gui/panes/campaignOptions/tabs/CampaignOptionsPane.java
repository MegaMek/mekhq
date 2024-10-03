package mekhq.gui.panes.campaignOptions.tabs;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;

import javax.swing.*;
import java.util.Map;
import java.util.ResourceBundle;

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
        GeneralTab generalTab = new GeneralTab(campaign, getFrame(), "generalTab");
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("generalPanel.title")), generalTab.createGeneralTab());

        // Combat Readiness
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("combatReadinessParentTab.title")), createCombatReadinessParentTab());

        // Human Resources
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("humanResourcesParentTab.title")), createHumanResourcesParentTab());

        // Unit Development
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("unitDevelopmentParentTab.title")), createUnitDevelopmentParentTab());

        // Logistics and Maintenance
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("logisticsAndMaintenanceParentTab.title")), createLogisticsAndMaintenanceParentTab());

        // Strategic Operations
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("strategicOperationsParentTab.title")), createStrategicOperationsParentTab());
    }


    private JTabbedPane createCombatReadinessParentTab() {
        // Tech Limits
        // Rulesets
        // Random Assignment Tables

        // Parent Tab
        JTabbedPane combatReadinessParentTab = new JTabbedPane();

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

        logisticsAndMaintenanceParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("suppliesAndAcquisitionTab.title")), suppliesAndAcquisitionContentTabs);
        logisticsAndMaintenanceParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("repairAndMaintenanceContentTabs.title")), repairAndMaintenanceContentTabs);

        logisticsAndMaintenanceParentTab.addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("suppliesAndAcquisitionTab.title")), suppliesAndAcquisitionContentTabs);

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
