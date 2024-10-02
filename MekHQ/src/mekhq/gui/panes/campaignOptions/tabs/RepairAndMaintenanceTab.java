package mekhq.gui.panes.campaignOptions.tabs;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class RepairAndMaintenanceTab {
    JFrame frame;
    String name;

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

    /**
     * Represents a tab for repair and maintenance in an application.
     */
    public RepairAndMaintenanceTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    /**
     * Creates the repair tab panel.
     *
     * @return the created repair tab panel as a {@link JPanel}
     */
    public JPanel createRepairTab() {
        // Header
        JPanel imagePanel = createHeaderPanel("RepairTab",
            getImageDirectory() + "logo_aurigan_coalition.png", false,
            "", true);

        // Era Mods
        useEraModsCheckBox = createCheckBox("UseEraModsCheckBox", null);

        // Tech Placement
        assignedTechFirstCheckBox = createCheckBox("AssignedTechFirstCheckBox", null);
        resetToFirstTechCheckBox = createCheckBox("ResetToFirstTechCheckBox", null);

        // Use Quirks
        useQuirksBox = createCheckBox("UseQuirksBox", null);

        // Aero System Damage
        useAeroSystemHitsBox = createCheckBox("UseAeroSystemHitsBox", null);

        // Damage by Margin
        useDamageMargin = createCheckBox("UseDamageMargin", null);
        useDamageMargin.addActionListener(evt -> spnDamageMargin.setEnabled(useDamageMargin.isSelected()));

        lblDamageMargin = createLabel("DamageMargin", null);
        spnDamageMargin = createSpinner("DamageMargin", null,
            1, 1, 20, 1);

        // Equipment Survival
        lblDestroyPartTarget = createLabel("DestroyPartTarget", null);
        spnDestroyPartTarget = createSpinner("DestroyPartTarget", null,
            2, 2, 13, 1);
        JLabel lblDestroyPartTargetPost = new JLabel("<html><b>+</b></html>");

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
                    .addComponent(lblDamageMargin)
                    .addComponent(spnDamageMargin))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDestroyPartTarget)
                    .addComponent(spnDestroyPartTarget)
                    .addComponent(lblDestroyPartTargetPost)));

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
                    .addComponent(lblDamageMargin)
                    .addComponent(spnDamageMargin)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblDestroyPartTarget)
                    .addComponent(spnDestroyPartTarget)
                    .addComponent(lblDestroyPartTargetPost)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE)));

        // Create Parent Panel and return
        return createParentPanel(panel, "repairTab", 500);
    }

    /**
     * Creates the maintenance tab panel.
     *
     * @return The created maintenance tab panel as a {@link JPanel}.
     */
    public JPanel createMaintenanceTab() {
        // Promotional Image
        JPanel imagePanel = createHeaderPanel("MaintenanceTab",
            getImageDirectory() + "logo_clan_blood_spirit.png",
            false, "", true);

        // Check Maintenance
        checkMaintenance = createCheckBox("CheckMaintenance", null);

        // Maintenance Cycle Duration
        lblMaintenanceDays = createLabel("MaintenanceDays", null);
        spnDamageMargin = createSpinner("MaintenanceDays", null,
            7, 1, 365, 1);

        // Maintenance Bonus
        lblMaintenanceBonus = createLabel("MaintenanceBonus", null);
        spnMaintenanceBonus = createSpinner("MaintenanceBonus", null,
            0, -13, 13, 1);

        // Default Maintenance Time
        lblDefaultMaintenanceTime = createLabel("DefaultMaintenanceTime", null);
        spnDefaultMaintenanceTime = createSpinner("DefaultMaintenanceTime", null,
            1, 1, 4, 1);

        // Use Quality Modifiers
        useQualityMaintenance = createCheckBox("UseQualityMaintenance", null);

        // Reverse Quality names
        reverseQualityNames = createCheckBox("ReverseQualityNames", null);

        // Use Random Unit Qualities
        chkUseRandomUnitQualities = createCheckBox("UseRandomUnitQualities", null);

        // Use Planetary Modifiers
        chkUsePlanetaryModifiers = createCheckBox("UsePlanetaryModifiers", null);

        // Only Damage F-Rated Equipment
        useUnofficialMaintenance = createCheckBox("UseUnofficialMaintenance", null);

        // Report Maintenance checks to Log
        logMaintenance = createCheckBox("LogMaintenance", null);

        // Layout the Panel
        final JPanel panel = createStandardPanel("repairTab", true, "");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(imagePanel)
                .addComponent(checkMaintenance)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMaintenanceDays)
                    .addComponent(spnMaintenanceDays))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMaintenanceBonus)
                    .addComponent(spnMaintenanceBonus))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblDefaultMaintenanceTime)
                    .addComponent(spnDefaultMaintenanceTime))
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
                    .addComponent(lblMaintenanceDays)
                    .addComponent(spnMaintenanceDays)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblMaintenanceBonus)
                    .addComponent(spnMaintenanceBonus)
                    .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblDefaultMaintenanceTime)
                    .addComponent(spnDefaultMaintenanceTime)
                .addContainerGap(Short.MAX_VALUE, Short.MAX_VALUE))
                .addComponent(useQualityMaintenance)
                .addComponent(reverseQualityNames)
                .addComponent(chkUseRandomUnitQualities)
                .addComponent(chkUsePlanetaryModifiers)
                .addComponent(useUnofficialMaintenance)
                .addComponent(logMaintenance));

        // Create Parent Panel and return
        return createParentPanel(panel, "maintenanceTab", 500);
    }

    private void recreateFinancesPanel(boolean isReversingQualityNames) {
        // TODO handle this
    }

    /**
     * Initializes the repair and maintenance tab by creating and initializing various UI components.
     */
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
