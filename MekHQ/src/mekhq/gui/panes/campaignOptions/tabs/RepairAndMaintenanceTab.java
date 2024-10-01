package mekhq.gui.panes.campaignOptions.tabs;

import mekhq.MekHQ;
import mekhq.gui.baseComponents.AbstractMHQScrollablePanel;
import mekhq.gui.baseComponents.AbstractMHQTabbedPane;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

public class RepairAndMaintenanceTab extends AbstractMHQTabbedPane {
    // region Variable Declarations
    private static String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

    //start Repair Tab
    private JCheckBox useEraModsCheckBox;
    private JCheckBox assignedTechFirstCheckBox;
    private JCheckBox resetToFirstTechCheckBox;
    private JCheckBox useQuirksBox;
    private JCheckBox useAeroSystemHitsBox;
    private JCheckBox useDamageMargin;
    private JLabel lblDamageMargin;
    private JSpinner spnDamageMargin;
    private JLabel lblDestroyPartTarget;
    private JSpinner spnDestroyPartTarget;
    //end Repair Tab

    //start Maintenance Tab
    private JCheckBox checkMaintenance;
    private JLabel lblMaintenanceDays;
    private JSpinner spnMaintenanceDays;
    private JLabel lblMaintenanceBonus;
    private JSpinner spnMaintenanceBonus;
    private JLabel lblDefaultMaintenanceTime;
    private JSpinner spnDefaultMaintenanceTime;
    private JCheckBox useQualityMaintenance;
    private JCheckBox reverseQualityNames;
    private JCheckBox chkUseRandomUnitQualities;
    private JCheckBox chkUsePlanetaryModifiers;
    private JCheckBox useUnofficialMaintenance;
    private JCheckBox logMaintenance;
    //end Maintenance Tab

    public RepairAndMaintenanceTab(JFrame frame, String name) {
        super(frame, name);

        initialize();
    }

    /**
     * Creates the general tab.
     *
     * @return the created general tab as an {@link AbstractMHQScrollablePanel}
     */
    public JTabbedPane createRepairAndMaintenanceTab() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel repairPanel = createRepairTab();
        tabbedPane.addTab("Repair", repairPanel);

        JPanel maintenancePanel = createMaintenanceTab();
        tabbedPane.addTab("Maintenance", maintenancePanel);

        return tabbedPane;
    }

    /**
     * Creates the repair tab panel.
     *
     * @return the created repair tab panel as a {@link JPanel}
     */
    public JPanel createRepairTab() {
        // Header
        JPanel imagePanel = createHeaderPanel("RepairTab",
            "data/images/universe/factions/logo_aurigan_coalition.png", false,
            "", true);

        // Era Mods
        useEraModsCheckBox = createCheckBox("useEraModsCheckBox", null);

        // Tech Placement
        assignedTechFirstCheckBox = createCheckBox("assignedTechFirstCheckBox", null);
        resetToFirstTechCheckBox = createCheckBox("resetToFirstTechCheckBox", null);

        // Use Quirks
        useQuirksBox = createCheckBox("useQuirksBox", null);

        // Aero System Damage
        useAeroSystemHitsBox = createCheckBox("useAeroSystemHitsBox", null);

        // Damage by Margin
        useDamageMargin = createCheckBox("useDamageMargin", null);
        useDamageMargin.addActionListener(evt -> spnDamageMargin.setEnabled(useDamageMargin.isSelected()));
        Map<JLabel, JSpinner> damageMarginFields = createLabeledSpinner("DamageMargin",
            null, 1, 1, 20, 1);
        for (Entry<JLabel, JSpinner> entry : damageMarginFields.entrySet()) {
            lblDamageMargin = entry.getKey();
            spnDamageMargin = entry.getValue();
        }

        // Equipment Survival
        Map<JLabel, JSpinner> DestroyPartTargetFields = createLabeledSpinner("DestroyPartTarget",
            null, 2, 2, 13, 1);
        for (Entry<JLabel, JSpinner> entry : DestroyPartTargetFields.entrySet()) {
            lblDestroyPartTarget = entry.getKey();
            spnDestroyPartTarget = entry.getValue();
        }

        // Layout the Panel
        final JPanel panel = createStandardPanel("repairTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(imagePanel)
                .addComponent(useEraModsCheckBox)
                .addComponent(assignedTechFirstCheckBox)
                .addComponent(resetToFirstTechCheckBox)
                .addComponent(useQuirksBox)
                .addComponent(useAeroSystemHitsBox)
                .addComponent(useDamageMargin)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnDamageMargin)
                    .addComponent(lblDamageMargin, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnDestroyPartTarget)
                    .addComponent(lblDestroyPartTarget, Alignment.LEADING)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(imagePanel)
                .addComponent(useEraModsCheckBox)
                .addComponent(assignedTechFirstCheckBox)
                .addComponent(resetToFirstTechCheckBox)
                .addComponent(useQuirksBox)
                .addComponent(useAeroSystemHitsBox)
                .addComponent(useDamageMargin)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(spnDamageMargin)
                    .addComponent(lblDamageMargin))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(spnDestroyPartTarget)
                    .addComponent(lblDestroyPartTarget)));

        // Create Parent Panel and return
        return createParentPanel(panel, "repairTab", 500, 500);
    }

    /**
     * Creates the maintenance tab panel.
     *
     * @return The created maintenance tab panel as a {@link JPanel}.
     */
    public JPanel createMaintenanceTab() {
        // Promotional Image
        JPanel imagePanel = createHeaderPanel("MaintenanceTab",
            "data/images/universe/factions/logo_clan_blood_spirit.png",
            false, "", true);

        // Check Maintenance
        checkMaintenance = createCheckBox("checkMaintenance", null);

        // Maintenance Cycle Duration
        Map<JLabel, JSpinner> maintenanceDaysFields = createLabeledSpinner("MaintenanceDays",
            null, 7, 1, 365, 1);
        for (Entry<JLabel, JSpinner> entry : maintenanceDaysFields.entrySet()) {
            lblMaintenanceDays = entry.getKey();
            spnMaintenanceDays = entry.getValue();
        }

        // Maintenance Bonus
        Map<JLabel, JSpinner> maintenanceBonusFields = createLabeledSpinner("MaintenanceBonus",
            null, 0, -13, 13, 1);
        for (Entry<JLabel, JSpinner> entry : maintenanceBonusFields.entrySet()) {
            lblMaintenanceBonus = entry.getKey();
            spnMaintenanceBonus = entry.getValue();
        }

        // Default Maintenance Time
        Map<JLabel, JSpinner> defaultMaintenanceTimeFields = createLabeledSpinner("DefaultMaintenanceTime",
            null, 1, 1, 4, 1);
        for (Entry<JLabel, JSpinner> entry : defaultMaintenanceTimeFields.entrySet()) {
            lblDefaultMaintenanceTime = entry.getKey();
            spnDefaultMaintenanceTime = entry.getValue();
        }

        // Use Quality Modifiers
        useQualityMaintenance = createCheckBox("useQualityMaintenance", null);

        // Reverse Quality names
        reverseQualityNames = createCheckBox("reverseQualityNames", null);

        // Use Random Unit Qualities
        chkUseRandomUnitQualities = createCheckBox("chkUseRandomUnitQualities", null);

        // Use Planetary Modifiers
        chkUsePlanetaryModifiers = createCheckBox("chkUsePlanetaryModifiers", null);

        // Only Damage F-Rated Equipment
        useUnofficialMaintenance = createCheckBox("useUnofficialMaintenance", null);

        // Report Maintenance checks to Log
        logMaintenance = createCheckBox("logMaintenance", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("repairTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(imagePanel)
                .addComponent(checkMaintenance)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnMaintenanceDays)
                    .addComponent(lblMaintenanceDays, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnMaintenanceBonus)
                    .addComponent(lblMaintenanceBonus, Alignment.LEADING))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnDefaultMaintenanceTime)
                    .addComponent(lblDefaultMaintenanceTime, Alignment.LEADING))
                .addComponent(useQualityMaintenance)
                .addComponent(reverseQualityNames)
                .addComponent(chkUseRandomUnitQualities)
                .addComponent(chkUsePlanetaryModifiers)
                .addComponent(useUnofficialMaintenance)
                .addComponent(logMaintenance));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(imagePanel)
                .addComponent(checkMaintenance)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(spnMaintenanceDays)
                    .addComponent(lblMaintenanceDays))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(spnMaintenanceBonus)
                    .addComponent(lblMaintenanceBonus))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(spnDefaultMaintenanceTime)
                    .addComponent(lblDefaultMaintenanceTime))
                .addComponent(useQualityMaintenance)
                .addComponent(reverseQualityNames)
                .addComponent(chkUseRandomUnitQualities)
                .addComponent(chkUsePlanetaryModifiers)
                .addComponent(useUnofficialMaintenance)
                .addComponent(logMaintenance));

        // Create Parent Panel and return
        return createParentPanel(panel, "maintenanceTab", 500, 500);
    }

    private void recreateFinancesPanel(boolean isReversingQualityNames) {
        // TODO handle this
    }

    @Override
    protected void initialize() {
        initializeRepairTab();
        initializeMaintenanceTab();
    }

    /**
     * Initializes the repair tab by creating and initializing various UI components.
     */
    private void initializeRepairTab() {
        useEraModsCheckBox = new JCheckBox();

        assignedTechFirstCheckBox = new JCheckBox();

        resetToFirstTechCheckBox = new JCheckBox();

        useQuirksBox = new JCheckBox();

        useAeroSystemHitsBox = new JCheckBox();

        useDamageMargin = new JCheckBox();
        lblDamageMargin = new JLabel();
        spnDamageMargin = new JSpinner();

        lblDestroyPartTarget = new JLabel();
        spnDestroyPartTarget = new JSpinner();
    }

    /**
     * Initializes the maintenance tab by creating and initializing various UI components.
     */
    private void initializeMaintenanceTab() {
        checkMaintenance = new JCheckBox();

        lblMaintenanceDays = new JLabel();
        spnMaintenanceDays = new JSpinner();

        lblMaintenanceBonus = new JLabel();
        spnMaintenanceBonus = new JSpinner();

        lblDefaultMaintenanceTime = new JLabel();
        spnDefaultMaintenanceTime = new JSpinner();

        useQualityMaintenance = new JCheckBox();

        reverseQualityNames = new JCheckBox();

        chkUseRandomUnitQualities = new JCheckBox();

        chkUsePlanetaryModifiers = new JCheckBox();

        useUnofficialMaintenance = new JCheckBox();

        logMaintenance = new JCheckBox();
    }
}
