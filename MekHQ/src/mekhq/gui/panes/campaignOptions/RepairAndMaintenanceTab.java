package mekhq.gui.panes.campaignOptions;

import javax.swing.*;
import java.awt.*;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

/**
 * This class represents the tab for repair and maintenance settings.
 */
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
     * Initializes the repair and maintenance tab by creating and initializing various UI components.
     */
    void initialize() {
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

    /**
     * Creates the repair tab panel.
     *
     * @return the created repair tab panel as a {@link JPanel}
     */
    JPanel createRepairTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("RepairTab",
            getImageDirectory() + "logo_aurigan_coalition.png", true);

        // Era Mods
            useEraModsCheckBox = new CampaignOptionsCheckBox("UseEraModsCheckBox");

        // Tech Placement
        assignedTechFirstCheckBox = new CampaignOptionsCheckBox("AssignedTechFirstCheckBox");
        resetToFirstTechCheckBox = new CampaignOptionsCheckBox("ResetToFirstTechCheckBox");

        // Use Quirks
        useQuirksBox = new CampaignOptionsCheckBox("UseQuirksBox");

        // Aero System Damage
        useAeroSystemHitsBox = new CampaignOptionsCheckBox("UseAeroSystemHitsBox");

        // Damage by Margin
        useDamageMargin = new CampaignOptionsCheckBox("UseDamageMargin");
        useDamageMargin.addActionListener(evt -> spnDamageMargin.setEnabled(useDamageMargin.isSelected()));

        lblDamageMargin = new CampaignOptionsLabel("DamageMargin");
        spnDamageMargin = new CampaignOptionsSpinner("DamageMargin",
            1, 1, 20, 1);

        // Equipment Survival
        lblDestroyPartTarget = new CampaignOptionsLabel("DestroyPartTarget");
        spnDestroyPartTarget = new CampaignOptionsSpinner("DestroyPartTarget",
            2, 2, 13, 1);

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("repairTabLeft");
        final GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(useEraModsCheckBox, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(assignedTechFirstCheckBox, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(resetToFirstTechCheckBox, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(useQuirksBox, layoutLeft);

        final JPanel panelRight = new CampaignOptionsStandardPanel("RepairTabRight", true,
            "RepairTabRight");
        final GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutRight.gridx = 0;
        layoutRight.gridy = 0;
        layoutRight.gridwidth = 2;
        panelRight.add(useAeroSystemHitsBox, layoutRight);

        layoutRight.gridy++;
        panelRight.add(useDamageMargin, layoutRight);

        layoutRight.gridy++;
        layoutRight.gridwidth = 1;
        panelRight.add(lblDamageMargin, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnDamageMargin, layoutRight);

        layoutRight.gridx = 0;
        layoutRight.gridy++;
        panelRight.add(lblDestroyPartTarget, layoutRight);
        layoutRight.gridx++;
        panelRight.add(spnDestroyPartTarget, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("RepairTab", true);
        final GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridx = 0;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);

        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "repairTab");
    }

    /**
     * Creates the maintenance tab panel.
     *
     * @return The created maintenance tab panel as a {@link JPanel}.
     */
    JPanel createMaintenanceTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("MaintenanceTab",
            getImageDirectory() + "logo_clan_blood_spirit.png", true);

        // Contents
        checkMaintenance = new CampaignOptionsCheckBox("CheckMaintenance");

        lblMaintenanceDays = new CampaignOptionsLabel("MaintenanceDays");
        spnMaintenanceDays = new CampaignOptionsSpinner("MaintenanceDays",
            7, 1, 365, 1);

        lblMaintenanceBonus = new CampaignOptionsLabel("MaintenanceBonus");
        spnMaintenanceBonus = new CampaignOptionsSpinner("MaintenanceBonus",
            0, -13, 13, 1);

        lblDefaultMaintenanceTime = new CampaignOptionsLabel("DefaultMaintenanceTime");
        spnDefaultMaintenanceTime = new CampaignOptionsSpinner("DefaultMaintenanceTime",
            1, 1, 4, 1);

        useQualityMaintenance = new CampaignOptionsCheckBox("UseQualityMaintenance");

        reverseQualityNames = new CampaignOptionsCheckBox("ReverseQualityNames");

        chkUseRandomUnitQualities = new CampaignOptionsCheckBox("UseRandomUnitQualities");

        chkUsePlanetaryModifiers = new CampaignOptionsCheckBox("UsePlanetaryModifiers");

        useUnofficialMaintenance = new CampaignOptionsCheckBox("UseUnofficialMaintenance");

        logMaintenance = new CampaignOptionsCheckBox("LogMaintenance");

        // Layout the Panel
        final JPanel panelLeft = new CampaignOptionsStandardPanel("repairTabLeft");
        GridBagConstraints layoutLeft = new CampaignOptionsGridBagConstraints(panelLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelLeft.add(checkMaintenance, layoutLeft);

        layoutLeft.gridy++;
        panelLeft.add(lblMaintenanceDays, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaintenanceDays, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblMaintenanceBonus, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnMaintenanceBonus, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        panelLeft.add(lblDefaultMaintenanceTime, layoutLeft);
        layoutLeft.gridx++;
        panelLeft.add(spnDefaultMaintenanceTime, layoutLeft);

        layoutLeft.gridx = 0;
        layoutLeft.gridy++;
        layoutLeft.gridwidth = 2;
        panelLeft.add(logMaintenance, layoutLeft);

        final JPanel panelRight = new CampaignOptionsStandardPanel("repairTabRight", true);
        GridBagConstraints layoutRight = new CampaignOptionsGridBagConstraints(panelRight);

        layoutLeft.gridx = 0;
        layoutLeft.gridy = 0;
        layoutLeft.gridwidth = 1;
        panelRight.add(useQualityMaintenance, layoutRight);

        layoutRight.gridy++;
        panelRight.add(reverseQualityNames, layoutRight);

        layoutRight.gridy++;
        panelRight.add(chkUseRandomUnitQualities, layoutRight);

        layoutRight.gridy++;
        panelRight.add(chkUseRandomUnitQualities, layoutRight);

        layoutRight.gridy++;
        panelRight.add(chkUsePlanetaryModifiers, layoutRight);

        layoutRight.gridy++;
        panelRight.add(useUnofficialMaintenance, layoutRight);

        final JPanel panelParent = new CampaignOptionsStandardPanel("repairTab", true);
        GridBagConstraints layoutParent = new CampaignOptionsGridBagConstraints(panelParent);

        layoutParent.gridwidth = 5;
        layoutParent.gridy = 0;
        panelParent.add(headerPanel, layoutParent);

        layoutParent.gridx = 0;
        layoutParent.gridy++;
        layoutParent.gridwidth = 1;
        panelParent.add(panelLeft, layoutParent);
        layoutParent.gridx++;
        panelParent.add(panelRight, layoutParent);

        // Create Parent Panel and return
        return createParentPanel(panelParent, "maintenanceTab");
    }

    private void recreateFinancesPanel(boolean isReversingQualityNames) {
        // TODO handle this
    }
}
