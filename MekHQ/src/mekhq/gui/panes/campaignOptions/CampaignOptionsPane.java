package mekhq.gui.panes.campaignOptions;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;
import mekhq.gui.panes.campaignOptions.tabs.GeneralTab;
import mekhq.gui.panes.campaignOptions.tabs.RepairAndMaintenanceTab;

import javax.swing.*;
import java.util.Map;
import java.util.ResourceBundle;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.createSubTabs;

public class CampaignOptionsPane extends AbstractMHQTabbedPane {
    private static final MMLogger logger = MMLogger.create(CampaignOptionsPane.class);
    private static final String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE);

    private final Campaign campaign;
    final static int WIDTH_MULTIPLIER = 3; // This seems to be the sweet spot

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

        // Repair and Maintenance
        RepairAndMaintenanceTab repairAndMaintenanceTab = new RepairAndMaintenanceTab(getFrame(),
            "repairAndMaintenanceTab");
        JTabbedPane repairAndMaintenanceContentTabs = createSubTabs(Map.of(
            "repairTab", repairAndMaintenanceTab.createRepairTab(),
            "maintenanceTab", repairAndMaintenanceTab.createMaintenanceTab()));
        addTab(String.format("<html><font size=%s><b>%s</b></font></html>", 4,
            resources.getString("RepairAndMaintenancePanel.title")), repairAndMaintenanceContentTabs);
    }

    private void setOptions() {
        // TODO this is where we update the dialog based on current campaign settings.
    }

    private void updateOptions() {
        // TODO this is where we update campaign values based on the dialog values
    }
}
