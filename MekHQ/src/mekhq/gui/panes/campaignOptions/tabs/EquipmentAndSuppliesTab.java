package mekhq.gui.panes.campaignOptions.tabs;

import megamek.client.ui.baseComponents.MMComboBox;
import megamek.client.ui.swing.util.UIUtil;
import megamek.common.EquipmentType;
import mekhq.MekHQ;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.enums.PlanetaryAcquisitionFactionLimit;
import mekhq.campaign.personnel.SkillType;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;
import java.util.ResourceBundle;

import static mekhq.gui.panes.campaignOptions.tabs.CampaignOptionsUtilities.*;

public class EquipmentAndSuppliesTab {
    // region Variable Declarations
    private static String RESOURCE_PACKAGE = "mekhq/resources/NEWCampaignOptionsDialog";
    private static final ResourceBundle resources = ResourceBundle.getBundle(RESOURCE_PACKAGE,
        MekHQ.getMHQOptions().getLocale());

    JFrame frame;
    String name;

    //start Acquisition Tab
    private JLabel lblChoiceAcquireSkill;
    private MMComboBox<String> choiceAcquireSkill;
    private JCheckBox chkSupportStaffOnly;
    private JLabel lblAcquireClanPenalty;
    private JSpinner spnAcquireClanPenalty;
    private JLabel lblAcquireIsPenalty;
    private JSpinner spnAcquireIsPenalty;
    private JLabel lblAcquireWaitingPeriod;
    private JSpinner spnAcquireWaitingPeriod;
    private JLabel lblMaxAcquisitions;
    private JSpinner spnMaxAcquisitions;
    //end Acquisition Tab

    //start Delivery Tab
    private JLabel lblNDiceTransitTime;
    private JSpinner spnNDiceTransitTime;
    private JLabel lblConstantTransitTime;
    private JSpinner spnConstantTransitTime;
    private JLabel lblAcquireMosBonus;
    private JSpinner spnAcquireMosBonus;
    private JLabel lblAcquireMinimum;
    private JSpinner spnAcquireMinimum;
    private MMComboBox<String> choiceTransitTimeUnits;
    private MMComboBox<String> choiceAcquireMosUnits;
    private MMComboBox<String> choiceAcquireMinimumUnit;
    private static final int TRANSIT_UNIT_DAY = 0;
    private static final int TRANSIT_UNIT_WEEK = 1;
    private static final int TRANSIT_UNIT_MONTH = 2;
    private static final int TRANSIT_UNIT_NUM = 3;
    //end Delivery Tab

    //start Planetary Acquisition Tab
    private JCheckBox usePlanetaryAcquisitions;
    private JLabel lblMaxJumpPlanetaryAcquisitions;
    private JSpinner spnMaxJumpPlanetaryAcquisitions;
    private JLabel lblPlanetaryAcquisitionsFactionLimits;
    private MMComboBox<PlanetaryAcquisitionFactionLimit> comboPlanetaryAcquisitionsFactionLimits;
    private JCheckBox disallowClanPartsFromIS;
    private JCheckBox disallowPlanetaryAcquisitionClanCrossover;
    private JLabel lblPenaltyClanPartsFromIS;
    private JSpinner spnPenaltyClanPartsFromIS;
    private JCheckBox usePlanetaryAcquisitionsVerbose;

    private JPanel pnlTechModifiers;
    private JLabel[] lblPlanetAcquireTechBonus;
    private JSpinner[] spnPlanetAcquireTechBonus;

    private JPanel pnlIndustryModifiers;
    private JLabel[] lblPlanetAcquireIndustryBonus;
    private JSpinner[] spnPlanetAcquireIndustryBonus;

    private JPanel pnlOutputModifiers;
    private JLabel[] lblPlanetAcquireOutputBonus;
    private JSpinner[] spnPlanetAcquireOutputBonus;
    //end Planetary Acquisition Tab

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

    public EquipmentAndSuppliesTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    /**
     * Calls the initialization methods for all the tab panels.
     */
    void initialize() {
        initializeAcquisitionTab();
        initializeDelivery();
        initializePlanetaryAcquisitionsTab();
    }

    /**
     * Initializes the components of the ProcreationTab.
     * The panel contains settings related to character procreation in the simulation.
     */
    private void initializePlanetaryAcquisitionsTab() {
        // Options
        usePlanetaryAcquisitions = new JCheckBox();

        lblMaxJumpPlanetaryAcquisitions = new JLabel();
        spnMaxJumpPlanetaryAcquisitions = new JSpinner();

        lblPlanetaryAcquisitionsFactionLimits = new JLabel();
        comboPlanetaryAcquisitionsFactionLimits = new MMComboBox<>("comboPlanetaryAcquisitionsFactionLimits",
            PlanetaryAcquisitionFactionLimit.values());

        disallowPlanetaryAcquisitionClanCrossover = new JCheckBox();

        disallowClanPartsFromIS = new JCheckBox();

        lblPenaltyClanPartsFromIS = new JLabel();
        spnPenaltyClanPartsFromIS = new JSpinner();

        usePlanetaryAcquisitionsVerbose = new JCheckBox();

        // Modifiers
        pnlTechModifiers = new JPanel();
        lblPlanetAcquireTechBonus = new JLabel[6];
        spnPlanetAcquireTechBonus = new JSpinner[6];

        pnlIndustryModifiers = new JPanel();
        lblPlanetAcquireIndustryBonus = new JLabel[6];
        spnPlanetAcquireIndustryBonus = new JSpinner[6];

        pnlOutputModifiers = new JPanel();
        lblPlanetAcquireOutputBonus = new JLabel[6];
        spnPlanetAcquireOutputBonus = new JSpinner[6];
    }

    /**
     * Initializes the components of the DivorceTab.
     * The panel contains settings related to divorce mechanics within the simulation.
     */
    private void initializeDelivery() {
        lblNDiceTransitTime = new JLabel();
        spnNDiceTransitTime = new JSpinner();
        lblConstantTransitTime = new JLabel();
        spnConstantTransitTime = new JSpinner();
        choiceTransitTimeUnits = new MMComboBox<>("choiceTransitTimeUnits", getTransitUnitOptions());

        lblAcquireMosBonus = new JLabel();
        spnAcquireMosBonus = new JSpinner();
        choiceAcquireMosUnits = new MMComboBox<>("choiceAcquireMosUnits", getTransitUnitOptions());

        lblAcquireMinimum = new JLabel();
        spnAcquireMinimum = new JSpinner();
        choiceAcquireMinimumUnit = new MMComboBox<>("choiceAcquireMinimumUnit", getTransitUnitOptions());
    }

    /**
     * Initializes the components of the MarriageTab.
     * The panel contains various settings related to marriage mechanics within the simulation.
     */
    private void initializeAcquisitionTab() {
        lblChoiceAcquireSkill = new JLabel();
        choiceAcquireSkill = new MMComboBox<>("choiceAcquireSkill", buildAcquireSkillComboOptions());

        chkSupportStaffOnly = new JCheckBox();

        lblAcquireClanPenalty = new JLabel();
        spnAcquireClanPenalty = new JSpinner();

        lblAcquireIsPenalty = new JLabel();
        spnAcquireIsPenalty = new JSpinner();

        lblAcquireWaitingPeriod = new JLabel();
        spnAcquireWaitingPeriod = new JSpinner();

        lblMaxAcquisitions = new JLabel();
        spnMaxAcquisitions = new JSpinner();
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
     * Creates the acquisition tab panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    JPanel createAcquisitionTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("AcquisitionTab",
            getImageDirectory() + "logo_calderon_protectorate.png", true);

        // Acquisitions skill
        lblChoiceAcquireSkill = createLabel("ChoiceAcquireSkill");

        // Support personnel only
        chkSupportStaffOnly = createCheckBox("SupportStaffOnly");

        // Clan Acquisition penalty
        lblAcquireClanPenalty = createLabel("AcquireClanPenalty");
        spnAcquireClanPenalty = createSpinner("AcquireClanPenalty",
            0, 0, 13, 1);

        // IS Acquisition penalty
        lblAcquireIsPenalty = createLabel("AcquireISPenalty");
        spnAcquireIsPenalty = createSpinner("AcquireISPenalty",
            0, 0, 13, 1);

        // Waiting Period
        lblAcquireWaitingPeriod = createLabel("AcquireWaitingPeriod");
        spnAcquireWaitingPeriod = createSpinner("AcquireWaitingPeriod",
            1, 1, 365, 1);

        // Maximum Acquisitions
        lblMaxAcquisitions = createLabel("MaxAcquisitions");
        spnMaxAcquisitions = createSpinner("MaxAcquisitions",
            0,0, 100, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("acquisitionTab", true);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblChoiceAcquireSkill)
                    .addComponent(choiceAcquireSkill))
                .addComponent(chkSupportStaffOnly)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAcquireClanPenalty)
                    .addComponent(spnAcquireClanPenalty))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAcquireIsPenalty)
                    .addComponent(spnAcquireIsPenalty))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAcquireWaitingPeriod)
                    .addComponent(spnAcquireWaitingPeriod))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMaxAcquisitions)
                    .addComponent(spnMaxAcquisitions)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblChoiceAcquireSkill)
                    .addComponent(choiceAcquireSkill))
                .addComponent(chkSupportStaffOnly)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAcquireClanPenalty)
                    .addComponent(spnAcquireClanPenalty))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAcquireIsPenalty)
                    .addComponent(spnAcquireIsPenalty))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAcquireWaitingPeriod)
                    .addComponent(spnAcquireWaitingPeriod))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblMaxAcquisitions)
                    .addComponent(spnMaxAcquisitions)));

        // Create Parent Panel and return
        return createParentPanel(panel, "acquisitionsTab");
    }

    JPanel createPlanetaryAcquisitionTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("PlanetaryAcquisitionTab",
            getImageDirectory() + "logo_capellan_confederation.png", true);

        // Sub-Panels
        JPanel options = createOptionsPanel();
        JPanel modifiers = createModifiersPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("PlanetaryAcquisitionTab", true);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(options)
                    .addComponent(modifiers)));


        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(options)
                    .addComponent(modifiers)));

        // Create Parent Panel and return
        return createParentPanel(panel, "PlanetaryAcquisitionTab");
    }


    /**
     * Creates the planetary acquisition options panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    private JPanel createOptionsPanel() {
        // Use Planetary Acquisitions
        usePlanetaryAcquisitions = createCheckBox("UsePlanetaryAcquisitions");

        // Max Jump Distance
        lblMaxJumpPlanetaryAcquisitions = createLabel("MaxJumpPlanetaryAcquisitions");
        spnMaxJumpPlanetaryAcquisitions = createSpinner("MaxJumpPlanetaryAcquisitions",
            2, 0, 5, 1);

        // Faction Limits
        lblPlanetaryAcquisitionsFactionLimits = createLabel("PlanetaryAcquisitionsFactionLimits");

        // Disallow Resource Sharing (Inner Sphere)
        disallowPlanetaryAcquisitionClanCrossover = createCheckBox("DisallowPlanetaryAcquisitionClanCrossover");

        // Disallow Resource Sharing (Clans)
        disallowClanPartsFromIS = createCheckBox("DisallowClanPartsFromIS");

        // Acquisition Penalty
        lblPenaltyClanPartsFromIS = createLabel("PenaltyClanPartsFromIS");
        spnPenaltyClanPartsFromIS = createSpinner("PenaltyClanPartsFromIS",
            0, 0, 12, 1);

        // Verbose Reporting
        usePlanetaryAcquisitionsVerbose = createCheckBox("UsePlanetaryAcquisitionsVerbose");

        // Layout the Panel
        final JPanel panel = new JPanel();
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(usePlanetaryAcquisitions)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnMaxJumpPlanetaryAcquisitions)
                    .addComponent(lblMaxJumpPlanetaryAcquisitions))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetaryAcquisitionsFactionLimits)
                    .addComponent(comboPlanetaryAcquisitionsFactionLimits))
                .addComponent(disallowPlanetaryAcquisitionClanCrossover)
                .addComponent(disallowClanPartsFromIS)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(spnPenaltyClanPartsFromIS)
                    .addComponent(lblPenaltyClanPartsFromIS))
                .addComponent(usePlanetaryAcquisitionsVerbose));


        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(usePlanetaryAcquisitions)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblMaxJumpPlanetaryAcquisitions)
                    .addComponent(spnMaxJumpPlanetaryAcquisitions))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetaryAcquisitionsFactionLimits)
                    .addComponent(comboPlanetaryAcquisitionsFactionLimits))
                .addComponent(disallowPlanetaryAcquisitionClanCrossover)
                .addComponent(disallowClanPartsFromIS)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPenaltyClanPartsFromIS)
                    .addComponent(spnPenaltyClanPartsFromIS))
                .addComponent(usePlanetaryAcquisitionsVerbose));

        return panel;
    }

    /**
     * Creates the planetary acquisition modifiers panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    private JPanel createModifiersPanel() {
        // Modifier Spinners
        for (int i = EquipmentType.RATING_A; i <= EquipmentType.RATING_F; i++) {
            String modifierLabel = getModifierLabel(i);

            lblPlanetAcquireTechBonus[i] = new JLabel(String.format("<html>%s</html>",
                modifierLabel));
            spnPlanetAcquireTechBonus[i] = new JSpinner(new SpinnerNumberModel(
                0, -12, 12, 1));
            setSpinnerWidth(spnPlanetAcquireTechBonus[i]);

            lblPlanetAcquireIndustryBonus[i] = new JLabel(String.format("<html>%s</html>",
                modifierLabel));
            spnPlanetAcquireIndustryBonus[i] = new JSpinner(new SpinnerNumberModel(
                0, -12, 12, 1));
            setSpinnerWidth(spnPlanetAcquireIndustryBonus[i]);

            lblPlanetAcquireOutputBonus[i] = new JLabel(String.format("<html>%s</html>",
                modifierLabel));
            spnPlanetAcquireOutputBonus[i] = new JSpinner(new SpinnerNumberModel(
                0, -12, 12, 1));
            setSpinnerWidth(spnPlanetAcquireOutputBonus[i]);
        }

        // Panels
        pnlIndustryModifiers = createIndustryModifiersPanel();
        pnlTechModifiers = createTechModifiersPanel();
        pnlOutputModifiers = createOutputModifiersPanel();

        // Layout the Panel
        final JPanel panel = createStandardPanel("PlanetaryAcquisitionTabModifiers",
            true, "ModifiersPanel");
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlIndustryModifiers)
                    .addComponent(pnlTechModifiers)
                    .addComponent(pnlOutputModifiers)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(pnlIndustryModifiers)
                    .addComponent(pnlTechModifiers)
                    .addComponent(pnlOutputModifiers)));

        return panel;
    }

    /**
     * Creates the planetary acquisition modifiers panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    private JPanel createTechModifiersPanel() {
        JLabel techLabel = createLabel("TechLabel");
        techLabel.setName(String.format("<html><center>%s</center></html", techLabel.getText()));

        // Layout the Panel
        final JPanel panel = createStandardPanel("createTechModifiersPanel", false);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(techLabel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireTechBonus[0])
                    .addComponent(spnPlanetAcquireTechBonus[0]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireTechBonus[1])
                    .addComponent(spnPlanetAcquireTechBonus[1]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireTechBonus[2])
                    .addComponent(spnPlanetAcquireTechBonus[2]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireTechBonus[3])
                    .addComponent(spnPlanetAcquireTechBonus[3]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireTechBonus[4])
                    .addComponent(spnPlanetAcquireTechBonus[4]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireTechBonus[5])
                    .addComponent(spnPlanetAcquireTechBonus[5])));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(techLabel)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireTechBonus[0])
                    .addComponent(spnPlanetAcquireTechBonus[0]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireTechBonus[1])
                    .addComponent(spnPlanetAcquireTechBonus[1]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireTechBonus[2])
                    .addComponent(spnPlanetAcquireTechBonus[2]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireTechBonus[3])
                    .addComponent(spnPlanetAcquireTechBonus[3]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireTechBonus[4])
                    .addComponent(spnPlanetAcquireTechBonus[4]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireTechBonus[5])
                    .addComponent(spnPlanetAcquireTechBonus[5])));

        return panel;
    }

    /**
     * Creates the planetary acquisition modifiers panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    private JPanel createIndustryModifiersPanel() {
        JLabel industryLabel = createLabel("IndustryLabel");
        industryLabel.setName(String.format("<html><center>%s</center></html", industryLabel.getText()));

        // Layout the Panel
        final JPanel panel = createStandardPanel("IndustryModifiersPanel", false);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(industryLabel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireIndustryBonus[0])
                    .addComponent(spnPlanetAcquireIndustryBonus[0]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireIndustryBonus[1])
                    .addComponent(spnPlanetAcquireIndustryBonus[1]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireIndustryBonus[2])
                    .addComponent(spnPlanetAcquireIndustryBonus[2]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireIndustryBonus[3])
                    .addComponent(spnPlanetAcquireIndustryBonus[3]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireIndustryBonus[4])
                    .addComponent(spnPlanetAcquireIndustryBonus[4]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireIndustryBonus[5])
                    .addComponent(spnPlanetAcquireIndustryBonus[5])));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(industryLabel)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireIndustryBonus[0])
                    .addComponent(spnPlanetAcquireIndustryBonus[0]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireIndustryBonus[1])
                    .addComponent(spnPlanetAcquireIndustryBonus[1]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireIndustryBonus[2])
                    .addComponent(spnPlanetAcquireIndustryBonus[2]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireIndustryBonus[3])
                    .addComponent(spnPlanetAcquireIndustryBonus[3]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireIndustryBonus[4])
                    .addComponent(spnPlanetAcquireIndustryBonus[4]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireIndustryBonus[5])
                    .addComponent(spnPlanetAcquireIndustryBonus[5])));

        return panel;
    }

    private JPanel createOutputModifiersPanel() {
        JLabel outputLabel = createLabel("OutputLabel");
        outputLabel.setName(String.format("<html><center>%s</center></html", outputLabel.getText()));

        // Layout the Panel
        final JPanel panel = createStandardPanel("OutputModifiersPanel", false);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(outputLabel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireOutputBonus[0])
                    .addComponent(spnPlanetAcquireOutputBonus[0]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireOutputBonus[1])
                    .addComponent(spnPlanetAcquireOutputBonus[1]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireOutputBonus[2])
                    .addComponent(spnPlanetAcquireOutputBonus[2]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireOutputBonus[3])
                    .addComponent(spnPlanetAcquireOutputBonus[3]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireOutputBonus[4])
                    .addComponent(spnPlanetAcquireOutputBonus[4]))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPlanetAcquireOutputBonus[5])
                    .addComponent(spnPlanetAcquireOutputBonus[5])));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(outputLabel)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireOutputBonus[0])
                    .addComponent(spnPlanetAcquireOutputBonus[0]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireOutputBonus[1])
                    .addComponent(spnPlanetAcquireOutputBonus[1]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireOutputBonus[2])
                    .addComponent(spnPlanetAcquireOutputBonus[2]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireOutputBonus[3])
                    .addComponent(spnPlanetAcquireOutputBonus[3]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireOutputBonus[4])
                    .addComponent(spnPlanetAcquireOutputBonus[4]))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblPlanetAcquireOutputBonus[5])
                    .addComponent(spnPlanetAcquireOutputBonus[5])));

        return panel;
    }

    /**
     * Sets the width of a {@link JSpinner} component based on the width of the text that would be
     * displayed in it.
     *
     * @param spinner the {@link JSpinner} to set the width for
     */
    private void setSpinnerWidth(JSpinner spinner) {
        Dimension size = spinner.getPreferredSize();
        spinner.setMinimumSize(UIUtil.scaleForGUI(size.width, size.height));
    }

    /**
     * Retrieves the label for a given quality modifier.
     *
     * @param quality The quality modifier represented by an integer value.
     * @return The label corresponding to the quality modifier.
     */
    private String getModifierLabel(int quality) {
        return switch (quality) {
            case EquipmentType.RATING_A -> "A";
            case EquipmentType.RATING_B -> "B";
            case EquipmentType.RATING_C -> "C";
            case EquipmentType.RATING_D -> "D";
            case EquipmentType.RATING_E -> "E";
            case EquipmentType.RATING_F -> "F";
            default -> "ERROR";
        };
    }

    /**
     * Creates the delivery tab panel.
     *
     * @return the created tab panel as a {@link JPanel}
     */
    JPanel createDeliveryTab() {
        // Header
        JPanel headerPanel = createHeaderPanel("DeliveryTab",
            getImageDirectory() + "logo_clan_burrock.png", true);

        // Delivery Time
        lblNDiceTransitTime = createLabel("NDiceTransitTime");
        spnNDiceTransitTime = createSpinner("NDiceTransitTime", 0,
            0, 365, 1);

        lblConstantTransitTime = createLabel("ConstantTransitTime");
        spnConstantTransitTime = createSpinner("ConstantTransitTime",
            0, 0, 365, 1);

        // Margin of Success Reductions
        lblAcquireMosBonus = createLabel("AcquireMosBonus");
        spnAcquireMosBonus = createSpinner("AcquireMosBonus",
            null, 0, 0, 365, 1);

        // Minimum Transit Time
        lblAcquireMinimum = createLabel("AcquireMinimum");
        spnAcquireMinimum = createSpinner("AcquireMinimum",
            null, 0, 0, 365, 1);

        // Layout the Panel
        final JPanel panel = createStandardPanel("deliveryTab", true);
        final GroupLayout layout = createStandardLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblNDiceTransitTime)
                    .addComponent(spnNDiceTransitTime)
                    .addComponent(lblConstantTransitTime)
                    .addComponent(spnConstantTransitTime)
                    .addComponent(choiceTransitTimeUnits))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAcquireMosBonus)
                    .addComponent(spnAcquireMosBonus)
                    .addComponent(choiceAcquireMosUnits))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAcquireMinimum)
                    .addComponent(spnAcquireMinimum)
                    .addComponent(choiceAcquireMinimumUnit)));

        layout.setHorizontalGroup(
            layout.createParallelGroup(Alignment.LEADING)
                .addComponent(headerPanel, Alignment.CENTER)
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblNDiceTransitTime)
                    .addComponent(spnNDiceTransitTime)
                    .addComponent(lblConstantTransitTime)
                    .addComponent(spnConstantTransitTime)
                    .addComponent(choiceTransitTimeUnits))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAcquireMosBonus)
                    .addComponent(spnAcquireMosBonus)
                    .addComponent(choiceAcquireMosUnits))
                .addGroup(layout.createSequentialGroup()
                    .addComponent(lblAcquireMinimum)
                    .addComponent(spnAcquireMinimum)
                    .addComponent(choiceAcquireMinimumUnit)));

        // Create Parent Panel and return
        return createParentPanel(panel, "deliveryTab");
    }

    /**
     * Retrieves the transit unit options as a {@link DefaultComboBoxModel}.
     *
     * @return The {@link DefaultComboBoxModel} containing the transit unit options with labels fetched
     * from {@code getTransitUnitName()}.
     */
    private static DefaultComboBoxModel<String> getTransitUnitOptions() {
        DefaultComboBoxModel<String> transitUnitModel = new DefaultComboBoxModel<>();

        for (int i = 0; i < TRANSIT_UNIT_NUM; i++) {
            transitUnitModel.addElement(getTransitUnitName(i));
        }
        return transitUnitModel;
    }

    /**
     * Returns the name of the transit unit based on the given unit value.
     *
     * @param unit the unit value representing the transit unit
     * @return the name of the transit unit as a {@link String}
     */
    private static String getTransitUnitName(final int unit) {
        return switch (unit) {
            case TRANSIT_UNIT_DAY -> resources.getString("transitUnitNamesDays.text");
            case TRANSIT_UNIT_WEEK -> resources.getString("transitUnitNamesWeeks.text");
            case TRANSIT_UNIT_MONTH -> resources.getString("transitUnitNamesMonths.text");
            default -> "ERROR";
        };
    }

    /**
     * Builds the options for the acquisition skill combo box.
     *
     * @return the default combo box model containing the acquisition skill options
     */
    private static DefaultComboBoxModel<String> buildAcquireSkillComboOptions() {
        DefaultComboBoxModel<String> acquireSkillModel = new DefaultComboBoxModel<>();

        acquireSkillModel.addElement(CampaignOptions.S_TECH);
        acquireSkillModel.addElement(SkillType.S_ADMIN);
        acquireSkillModel.addElement(SkillType.S_SCROUNGE);
        acquireSkillModel.addElement(SkillType.S_NEG);
        acquireSkillModel.addElement(CampaignOptions.S_AUTO);

        return acquireSkillModel;
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
            getImageDirectory() + "logo_clan_cloud_cobra.png", true);

        // Limit Parts/Units by Year
        limitByYearBox = createCheckBox("LimitByYearBox");

        // Disallow Extinct Units/Parts
        disallowExtinctStuffBox = createCheckBox("DisallowExtinctStuffBox");

        // Allow Clan/Inner Sphere Purchases
        allowClanPurchasesBox = createCheckBox("AllowClanPurchasesBox");
        allowISPurchasesBox = createCheckBox("AllowISPurchasesBox");

        // Canon Purchases/Refits
        allowCanonOnlyBox = createCheckBox("AllowCanonOnlyBox");
        allowCanonRefitOnlyBox = createCheckBox("AllowCanonRefitOnlyBox");

        // Maximum Tech Level
        lblChoiceTechLevel = createLabel("ChoiceTechLevel");
        choiceTechLevel = new MMComboBox<>("choiceTechLevel", getMaximumTechLevelOptions());

        // Variable Tech Level
        variableTechLevelBox = createCheckBox("VariableTechLevelBox");

        // Ammo by Type
        useAmmoByTypeBox = createCheckBox("UseAmmoByTypeBox");

        // Layout the Panel
        final JPanel panel = createStandardPanel("TechLimitsTab", true);
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
                    .addComponent(choiceTechLevel))
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
