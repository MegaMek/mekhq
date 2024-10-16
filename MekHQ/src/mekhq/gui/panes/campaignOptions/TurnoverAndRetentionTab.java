package mekhq.gui.panes.campaignOptions;

import megamek.client.ui.baseComponents.MMComboBox;
import mekhq.campaign.personnel.enums.TurnoverFrequency;

import javax.swing.*;
import javax.swing.GroupLayout.Alignment;
import java.awt.*;

import static mekhq.gui.panes.campaignOptions.CampaignOptionsUtilities.*;

/**
 * This class represents a GUI configuration form with two main tabs: 'Turnover' and 'Fatigue'.
 * <p>
 * These tabs contain various controls for user input and selection including checkboxes, spinners
 * and combo boxes. These configurations cover a wide range of settings, categorized into panels that
 * include settings, modifiers, payouts, unit cohesion, administrative strain, and management skill
 * panels for the 'Turnover' tab, and fatigue configurations for the 'Fatigue' tab.
 * <p>
 * The class uses Swing, a Java GUI toolkit, to create these controls. A {@link JFrame} object is used
 * to contain the tab structure, which itself contains various {@link JPanel} instances to group related
 * settings.
 * <p>
 * Use of this class involves calling the constructor with a {@link JFrame} parent and the name of the
 * tab, and then adding the resulting object to a Swing container. The various public methods in the
 * class are then called in response to user interactions with the GUI.
 */
public class TurnoverAndRetentionTab {
    JFrame frame;
    String name;

    //start Turnover Tab
    private JCheckBox chkUseRandomRetirement;

    private JPanel pnlSettings;
    private JLabel lblTurnoverFixedTargetNumber;
    private JSpinner spnTurnoverFixedTargetNumber;
    private JLabel lblTurnoverFrequency;
    private MMComboBox<TurnoverFrequency> comboTurnoverFrequency;
    private JCheckBox chkUseContractCompletionRandomRetirement;
    private JCheckBox chkUseRandomFounderTurnover;
    private JCheckBox chkTrackOriginalUnit;
    private JCheckBox chkAeroRecruitsHaveUnits;
    private JCheckBox chkUseSubContractSoldiers;
    private JLabel lblServiceContractDuration;
    private JSpinner spnServiceContractDuration;
    private JLabel lblServiceContractModifier;
    private JSpinner spnServiceContractModifier;
    private JCheckBox chkPayBonusDefault;
    private JLabel lblPayBonusDefaultThreshold;
    private JSpinner spnPayBonusDefaultThreshold;

    private JPanel pnlModifiers;
    private JCheckBox chkUseCustomRetirementModifiers;
    private JCheckBox chkUseFatigueModifiers;
    private JCheckBox chkUseSkillModifiers;
    private JCheckBox chkUseAgeModifiers;
    private JCheckBox chkUseUnitRatingModifiers;
    private JCheckBox chkUseFactionModifiers;
    private JCheckBox chkUseMissionStatusModifiers;
    private JCheckBox chkUseHostileTerritoryModifiers;
    private JCheckBox chkUseFamilyModifiers;
    private JCheckBox chkUseLoyaltyModifiers;
    private JCheckBox chkUseHideLoyalty;

    private JPanel pnlPayout;
    private JLabel lblPayoutRateOfficer;
    private JSpinner spnPayoutRateOfficer;
    private JLabel lblPayoutRateEnlisted;
    private JSpinner spnPayoutRateEnlisted;
    private JLabel lblPayoutRetirementMultiplier;
    private JSpinner spnPayoutRetirementMultiplier;
    private JCheckBox chkUsePayoutServiceBonus;
    private JLabel lblPayoutServiceBonusRate;
    private JSpinner spnPayoutServiceBonusRate;

    private JPanel pnlUnitCohesion;

    private JPanel pnlAdministrativeStrainWrapper;
    private JCheckBox chkUseAdministrativeStrain;

    private JPanel pnlAdministrativeStrain;
    private JLabel lblAdministrativeCapacity;
    private JSpinner spnAdministrativeCapacity;
    private JLabel lblMultiCrewStrainDivider;
    private JSpinner spnMultiCrewStrainDivider;

    private JPanel pnlManagementSkillWrapper;
    private JCheckBox chkUseManagementSkill;

    private JPanel pnlManagementSkill;
    private JCheckBox chkUseCommanderLeadershipOnly;
    private JLabel lblManagementSkillPenalty;
    private JSpinner spnManagementSkillPenalty;
    //end Turnover Tab

    //start Fatigue Tab
    private JCheckBox chkUseFatigue;
    private JLabel lblFatigueRate;
    private JSpinner spnFatigueRate;
    private JCheckBox chkUseInjuryFatigue;
    private JLabel lblFieldKitchenCapacity;
    private JSpinner spnFieldKitchenCapacity;
    private JCheckBox chkFieldKitchenIgnoreNonCombatants;
    private JLabel lblFatigueLeaveThreshold;
    private JSpinner spnFatigueLeaveThreshold;
    //end Fatigue Tab

    /**
     * Constructs and configures a {@link TurnoverAndRetentionTab} instance.
     * This constructor receives a {@link JFrame} and a name string, assigns them to the instance
     * variables 'frame' and 'name', and then calls the initilize method to initialize the components
     * of the tabs.
     *
     * @param frame the {@link JFrame} to be assigned to this tab's frame
     * @param name the name to be assigned to this tab's name
     */
    TurnoverAndRetentionTab(JFrame frame, String name) {
        this.frame = frame;
        this.name = name;

        initialize();
    }

    /**
     * Initializes all tab components.
     */
    private void initialize() {
        initializeTurnoverTab();
        initializeFatigueTab();
    }

    /**
     * Initializes the components for the FatigueTab panel.
     * This panel contains settings related to fatigue mechanics in the game.
     */
    private void initializeFatigueTab() {
        chkUseFatigue = new JCheckBox();
        lblFatigueRate = new JLabel();
        spnFatigueRate = new JSpinner();
        chkUseInjuryFatigue = new JCheckBox();
        lblFieldKitchenCapacity = new JLabel();
        spnFieldKitchenCapacity = new JSpinner();
        chkFieldKitchenIgnoreNonCombatants = new JCheckBox();
        lblFatigueLeaveThreshold = new JLabel();
        spnFatigueLeaveThreshold = new JSpinner();
    }

    /**
     * Initializes the components for the TurnoverTab panel.
     * This panel contains various settings influencing unit turnover in the game.
     */
    private void initializeTurnoverTab() {
        chkUseRandomRetirement = new JCheckBox();

        pnlSettings = new JPanel();
        lblTurnoverFixedTargetNumber = new JLabel();
        spnTurnoverFixedTargetNumber = new JSpinner();
        lblTurnoverFrequency = new JLabel();
        comboTurnoverFrequency = new MMComboBox<>("comboTurnoverFrequency", TurnoverFrequency.values());
        chkUseContractCompletionRandomRetirement = new JCheckBox();
        chkUseRandomFounderTurnover = new JCheckBox();
        chkTrackOriginalUnit = new JCheckBox();
        chkAeroRecruitsHaveUnits = new JCheckBox();
        chkUseSubContractSoldiers = new JCheckBox();
        lblServiceContractDuration = new JLabel();
        spnServiceContractDuration = new JSpinner();
        lblServiceContractModifier = new JLabel();
        spnServiceContractModifier = new JSpinner();
        chkPayBonusDefault = new JCheckBox();
        lblPayBonusDefaultThreshold = new JLabel();
        spnPayBonusDefaultThreshold = new JSpinner();

        pnlModifiers = new JPanel();
        chkUseCustomRetirementModifiers = new JCheckBox();
        chkUseFatigueModifiers = new JCheckBox();
        chkUseSkillModifiers = new JCheckBox();
        chkUseAgeModifiers = new JCheckBox();
        chkUseUnitRatingModifiers = new JCheckBox();
        chkUseFactionModifiers = new JCheckBox();
        chkUseMissionStatusModifiers = new JCheckBox();
        chkUseHostileTerritoryModifiers = new JCheckBox();
        chkUseFamilyModifiers = new JCheckBox();
        chkUseLoyaltyModifiers = new JCheckBox();
        chkUseHideLoyalty = new JCheckBox();

        pnlPayout = new JPanel();
        lblPayoutRateOfficer = new JLabel();
        spnPayoutRateOfficer = new JSpinner();
        lblPayoutRateEnlisted = new JLabel();
        spnPayoutRateEnlisted = new JSpinner();
        lblPayoutRetirementMultiplier = new JLabel();
        spnPayoutRetirementMultiplier = new JSpinner();
        chkUsePayoutServiceBonus = new JCheckBox();
        lblPayoutServiceBonusRate = new JLabel();
        spnPayoutServiceBonusRate = new JSpinner();

        pnlUnitCohesion = new JPanel();

        pnlAdministrativeStrainWrapper = new JPanel();
        chkUseAdministrativeStrain = new JCheckBox();

        pnlAdministrativeStrain = new JPanel();
        lblAdministrativeCapacity = new JLabel();
        spnAdministrativeCapacity = new JSpinner();
        lblMultiCrewStrainDivider = new JLabel();
        spnMultiCrewStrainDivider = new JSpinner();

        pnlManagementSkillWrapper = new JPanel();
        chkUseManagementSkill = new JCheckBox();

        pnlManagementSkill = new JPanel();
        chkUseCommanderLeadershipOnly = new JCheckBox();
        lblManagementSkillPenalty = new JLabel();
        spnManagementSkillPenalty = new JSpinner();
    }

    /**
     * Constructs and configures a panel for the Turnover tab.
     * This tab contains a header panel, a checkbox for activating random retirement, settings,
     * modifiers, payout, and unit cohesion panels.
     * <p>
     * The layout of the tab panel is organized with a {@link GroupLayout} for clean vertical and
     * horizontal alignment.
     *
     * @return panel the configured {@link JPanel} for the Turnover tab
     */
    JPanel createTurnoverTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("TurnoverTab",
            getImageDirectory() + "logo_clan_jade_falcon.png",
            true);

        // Contents
        chkUseRandomRetirement = new CampaignOptionsCheckBox("UseRandomRetirement");
        pnlSettings = createSettingsPanel();
        pnlModifiers = createModifiersPanel();
        pnlPayout = createPayoutsPanel();
        pnlUnitCohesion = createUnitCohesionPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TurnoverTab", true);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseRandomRetirement)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlSettings)
                    .addComponent(pnlModifiers)
                    .addComponent(pnlPayout))
                .addComponent(pnlUnitCohesion));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addComponent(chkUseRandomRetirement)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlSettings)
                        .addComponent(pnlModifiers)
                        .addComponent(pnlPayout)
                        )
                    .addComponent(pnlUnitCohesion)));

        // Create Parent Panel and return
        return createParentPanel(panel, "TurnoverTab");
    }

    /**
     * Constructs and configures a settings panel for controlling various turnover settings.
     * This panel allows for configuration of turnover fixed target number, turnover frequency,
     * contract completion, random retirement, random founder turnover, tracking of original unit,
     * aero recruits having units, subcontracted soldiers, service contract duration and modifier,
     * default payment bonus and payment bonus threshold.
     * <p>
     * Each of these settings is either controlled by a checkbox or a spinner with a specific range.
     * The turnover frequency selection is handled by a combo box.
     * The layout of the panel is organized with a {@link GroupLayout}, ensuring organized vertical
     * and horizontal alignment.
     *
     * @return panel the configured {@link JPanel} with various turnover settings
     */
    private JPanel createSettingsPanel() {
        // Contents
        lblTurnoverFixedTargetNumber = new CampaignOptionsLabel("TurnoverFixedTargetNumber");
        spnTurnoverFixedTargetNumber = new CampaignOptionsSpinner("TurnoverFixedTargetNumber",
            3, 0, 10, 1);

        lblTurnoverFrequency = new CampaignOptionsLabel("TurnoverFrequency");
        comboTurnoverFrequency.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                          final int index, final boolean isSelected,
                                                          final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof TurnoverFrequency) {
                    list.setToolTipText(((TurnoverFrequency) value).getToolTipText());
                }
                return this;
            }
        });

        chkUseContractCompletionRandomRetirement = new CampaignOptionsCheckBox(
            "UseContractCompletionRandomRetirement");

        chkUseRandomFounderTurnover = new CampaignOptionsCheckBox("UseRandomFounderTurnover");

        chkTrackOriginalUnit = new CampaignOptionsCheckBox("TrackOriginalUnit");

        chkAeroRecruitsHaveUnits = new CampaignOptionsCheckBox("AeroRecruitsHaveUnits");

        chkUseSubContractSoldiers = new CampaignOptionsCheckBox("UseSubContractSoldiers");

        lblServiceContractDuration = new CampaignOptionsLabel("ServiceContractDuration");
        spnServiceContractDuration = new CampaignOptionsSpinner("ServiceContractDuration",
            36, 0, 120, 1);

        lblServiceContractModifier = new CampaignOptionsLabel("ServiceContractModifier");
        spnServiceContractModifier = new CampaignOptionsSpinner("ServiceContractModifier",
            3, 0, 10, 1);

        chkPayBonusDefault = new CampaignOptionsCheckBox("PayBonusDefault");

        lblPayBonusDefaultThreshold = new CampaignOptionsLabel("PayBonusDefaultThreshold");
        spnPayBonusDefaultThreshold = new CampaignOptionsSpinner("PayBonusDefaultThreshold",
            3, 0, 12, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("SettingsPanel", true,
            "SettingsPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblTurnoverFixedTargetNumber)
                    .addComponent(spnTurnoverFixedTargetNumber))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblTurnoverFrequency)
                    .addComponent(comboTurnoverFrequency))
                .addComponent(chkUseContractCompletionRandomRetirement)
                .addComponent(chkUseRandomFounderTurnover)
                .addComponent(chkTrackOriginalUnit)
                .addComponent(chkAeroRecruitsHaveUnits)
                .addComponent(chkUseSubContractSoldiers)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblServiceContractDuration)
                    .addComponent(spnServiceContractDuration))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblServiceContractModifier)
                    .addComponent(spnServiceContractModifier))
                .addComponent(chkPayBonusDefault)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPayBonusDefaultThreshold)
                    .addComponent(spnPayBonusDefaultThreshold)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTurnoverFixedTargetNumber)
                        .addComponent(spnTurnoverFixedTargetNumber)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblTurnoverFrequency)
                        .addComponent(comboTurnoverFrequency)
                        )
                    .addComponent(chkUseContractCompletionRandomRetirement)
                    .addComponent(chkUseRandomFounderTurnover)
                    .addComponent(chkTrackOriginalUnit)
                    .addComponent(chkAeroRecruitsHaveUnits)
                    .addComponent(chkUseSubContractSoldiers)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblServiceContractDuration)
                        .addComponent(spnServiceContractDuration)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblServiceContractModifier)
                        .addComponent(spnServiceContractModifier)
                        )
                    .addComponent(chkPayBonusDefault)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblPayBonusDefaultThreshold)
                        .addComponent(spnPayBonusDefaultThreshold)
                        )));

        return panel;
    }

    /**
     * Constructs and configures a settings panel for controlling various status modifiers.
     * This panel offers checkboxes for toggling the use of custom retirement modifiers, fatigue modifiers,
     * skill modifiers, age modifiers, unit rating modifiers, faction modifiers, mission status modifiers,
     * hostile territory modifiers, family modifiers, loyalty modifiers, and the hiding of loyalty.
     * <p>
     * Each of these settings is represented by a checkbox, allowing for them to be easily toggled
     * on or off. The layout of the panel is organized with a {@link GroupLayout} for clean vertical
     * and horizontal alignment.
     *
     * @return panel the configured {@link JPanel} with modifier settings
     */
    private JPanel createModifiersPanel() {
        // Contents
        chkUseCustomRetirementModifiers = new CampaignOptionsCheckBox("UseCustomRetirementModifiers");
        chkUseFatigueModifiers = new CampaignOptionsCheckBox("UseFatigueModifiers");
        chkUseSkillModifiers = new CampaignOptionsCheckBox("UseSkillModifiers");
        chkUseAgeModifiers = new CampaignOptionsCheckBox("UseAgeModifiers");
        chkUseUnitRatingModifiers = new CampaignOptionsCheckBox("UseUnitRatingModifiers");
        chkUseFactionModifiers = new CampaignOptionsCheckBox("UseFactionModifiers");
        chkUseMissionStatusModifiers = new CampaignOptionsCheckBox("UseMissionStatusModifiers");
        chkUseHostileTerritoryModifiers = new CampaignOptionsCheckBox("UseHostileTerritoryModifiers");
        chkUseFamilyModifiers = new CampaignOptionsCheckBox("UseFamilyModifiers");
        chkUseLoyaltyModifiers = new CampaignOptionsCheckBox("UseLoyaltyModifiers");
        chkUseHideLoyalty = new CampaignOptionsCheckBox("UseHideLoyalty");


        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("TurnoverModifiersPanel", true,
            "ModifiersPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseCustomRetirementModifiers)
                .addComponent(chkUseFatigueModifiers)
                .addComponent(chkUseSkillModifiers)
                .addComponent(chkUseAgeModifiers)
                .addComponent(chkUseUnitRatingModifiers)
                .addComponent(chkUseFactionModifiers)
                .addComponent(chkUseMissionStatusModifiers)
                .addComponent(chkUseHostileTerritoryModifiers)
                .addComponent(chkUseFamilyModifiers)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(chkUseLoyaltyModifiers)
                    .addComponent(chkUseHideLoyalty)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseCustomRetirementModifiers)
                    .addComponent(chkUseFatigueModifiers)
                    .addComponent(chkUseSkillModifiers)
                    .addComponent(chkUseAgeModifiers)
                    .addComponent(chkUseUnitRatingModifiers)
                    .addComponent(chkUseFactionModifiers)
                    .addComponent(chkUseMissionStatusModifiers)
                    .addComponent(chkUseHostileTerritoryModifiers)
                    .addComponent(chkUseFamilyModifiers)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(chkUseLoyaltyModifiers)
                        .addComponent(chkUseHideLoyalty)
                        )));

        return panel;
    }

    /**
     * Constructs and configures a settings panel for controlling payouts.
     * This panel allows for configuration of officer and enlisted payout rates, as well as retirement
     * multipliers and service bonus (if enabled).
     * <p>
     * Each configurable setting is controlled by a spinner with a different acceptable range.
     * The layout of the panel is organized with a {@link GroupLayout}, ensuring organized vertical
     * and horizontal alignment.
     *
     * @return panel the configured {@link JPanel} with payout settings
     */
    private JPanel createPayoutsPanel() {
        // Contents
        lblPayoutRateOfficer = new CampaignOptionsLabel("PayoutRateOfficer");
        spnPayoutRateOfficer = new CampaignOptionsSpinner("PayoutRateOfficer",
            3, 0, 12, 1);

        lblPayoutRateEnlisted = new CampaignOptionsLabel("PayoutRateEnlisted");
        spnPayoutRateEnlisted = new CampaignOptionsSpinner("PayoutRateEnlisted",
            3, 0, 12, 1);

        lblPayoutRetirementMultiplier = new CampaignOptionsLabel("PayoutRetirementMultiplier");
        spnPayoutRetirementMultiplier = new CampaignOptionsSpinner("PayoutRetirementMultiplier",
            24, 1, 120, 1);

        chkUsePayoutServiceBonus = new CampaignOptionsCheckBox("UsePayoutServiceBonus");

        lblPayoutServiceBonusRate = new CampaignOptionsLabel("PayoutServiceBonusRate");
        spnPayoutServiceBonusRate = new CampaignOptionsSpinner("PayoutServiceBonusRate",
            10, 1, 100, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("PayoutsPanel", true,
            "PayoutsPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPayoutRateOfficer)
                    .addComponent(spnPayoutRateOfficer))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPayoutRateEnlisted)
                    .addComponent(spnPayoutRateEnlisted))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPayoutRetirementMultiplier)
                    .addComponent(spnPayoutRetirementMultiplier))
                .addComponent(chkUsePayoutServiceBonus)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblPayoutServiceBonusRate)
                    .addComponent(spnPayoutServiceBonusRate)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblPayoutRateOfficer)
                        .addComponent(spnPayoutRateOfficer)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblPayoutRateEnlisted)
                        .addComponent(spnPayoutRateEnlisted)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblPayoutRetirementMultiplier)
                        .addComponent(spnPayoutRetirementMultiplier)
                        )
                    .addComponent(chkUsePayoutServiceBonus)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblPayoutServiceBonusRate)
                        .addComponent(spnPayoutServiceBonusRate)
                        )));

        return panel;
    }

    /**
     * Constructs and configures a settings panel for controlling unit cohesion.
     * This panel contains check boxes which toggle usage of administrative strain and management skill,
     * along with their associated settings panels.
     * <p>
     * The layout of the panel is organized with a {@link GroupLayout}, placing the components in a
     * sequential order for vertical alignment and in a parallel formation for horizontal alignment.
     *
     * @return panel the configured {@link JPanel} with unit cohesion settings
     */
    private JPanel createUnitCohesionPanel() {
        // Contents
        pnlAdministrativeStrainWrapper = createAdministrativeStrainWrapperPanel();
        pnlManagementSkillWrapper = createManagementSkillWrapperPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitCohesionPanel", true,
            "UnitCohesionPanel");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(pnlAdministrativeStrainWrapper)
                    .addComponent(pnlManagementSkillWrapper)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(pnlAdministrativeStrainWrapper)
                        .addComponent(pnlManagementSkillWrapper)
                        )));

        return panel;
    }

    /**
     * Constructs and configures an administrative strain wrapper {@link JPanel} with a checkbox and
     * associated settings panel.
     * <p>
     * The layout of the panel is organized in a {@link GroupLayout}, placing the checkbox and the
     * settings panel sequentially for vertical alignment and in parallel for horizontal alignment.
     *
     * @return {@link JPanel} The newly created and configured JPanel containing administrative strain
     * settings
     */
    private JPanel createAdministrativeStrainWrapperPanel() {
        // Contents
        chkUseAdministrativeStrain = new CampaignOptionsCheckBox("UseAdministrativeStrain");
        pnlAdministrativeStrain = createAdministrativeStrainPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministrativeStrainPanel", false);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseAdministrativeStrain)
                .addComponent(pnlAdministrativeStrain));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseAdministrativeStrain)
                    .addComponent(pnlAdministrativeStrain)));

        return panel;
    }

    /**
     * Creates a settings panel for controlling administrative strain parameters.
     * This panel includes settings for administrative capacity and multi-crew strain divider.
     * <p>
     * The administrative capacity is a customizable setting, allowing selection between 1 and 30.
     * The multi-crew strain divider is also a customizable setting, allowing selection between 1 and 25.
     * <p>
     * The panel uses a {@link GroupLayout} for layout management, organizing the components in sequential
     * and parallel groups for vertical and horizontal alignment respectively.
     *
     * @return panel the configured {@link JPanel} with administrative strain settings
     */
    private JPanel createAdministrativeStrainPanel() {
        // Contents
        lblAdministrativeCapacity = new CampaignOptionsLabel("AdministrativeCapacity");
        spnAdministrativeCapacity = new CampaignOptionsSpinner("AdministrativeCapacity",
            10, 1, 30, 1);

        lblMultiCrewStrainDivider = new CampaignOptionsLabel("MultiCrewStrainDivider");
        spnMultiCrewStrainDivider = new CampaignOptionsSpinner("MultiCrewStrainDivider",
            5, 1, 25, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("AdministrativeStrain", true,
            "AdministrativeStrain");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblAdministrativeCapacity)
                    .addComponent(spnAdministrativeCapacity))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblMultiCrewStrainDivider)
                    .addComponent(spnMultiCrewStrainDivider)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblAdministrativeCapacity)
                        .addComponent(spnAdministrativeCapacity)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblMultiCrewStrainDivider)
                        .addComponent(spnMultiCrewStrainDivider)
                        )));

        return panel;
    }

    /**
     * Creates and configures a settings panel for controlling management skills.
     * <p>
     * This panel contains a checkbox for toggling management skill usage, along with an associated
     * settings panel.
     * <p>
     * The layout of the panel is organized with a {@link GroupLayout}, placing the components in a
     * sequential order for vertical alignment and in a parallel formation for horizontal alignment.
     *
     * @return {@link JPanel} The constructed and configured jPanel containing the management skill settings
     */
    private JPanel createManagementSkillWrapperPanel() {
        // Contents
        chkUseManagementSkill = new CampaignOptionsCheckBox("UseManagementSkill");
        pnlManagementSkill = createManagementSkillPanel();

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("UnitCohesionPanel", false);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseManagementSkill)
                .addComponent(pnlManagementSkill));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseManagementSkill)
                    .addComponent(pnlManagementSkill)));

        return panel;
    }

    /**
     * Creates a panel for management skill settings.
     * This panel consists of a checkbox to enable/disable use of commander leadership only and
     * a spinner to control the management skill penalty.
     * <p>
     * The panel uses a {@link GroupLayout} for layout management, organizing its components
     * in a sequential group for vertical alignment and a parallel group for horizontal alignment.
     * <p>
     * The management skill penalty is a customizable setting, with the possibility
     * to choose a value between -10 and 10.
     *
     * @return panel the configured {@link JPanel} with management skill settings
     */
    private JPanel createManagementSkillPanel() {
        // Contents
        chkUseCommanderLeadershipOnly = new CampaignOptionsCheckBox("UseCommanderLeadershipOnly");

        lblManagementSkillPenalty = new CampaignOptionsLabel("ManagementSkillPenalty");
        spnManagementSkillPenalty = new CampaignOptionsSpinner("ManagementSkillPenalty",
            0, -10, 10, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("ManagementSkill", true,
            "ManagementSkill");
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(chkUseCommanderLeadershipOnly)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblManagementSkillPenalty)
                    .addComponent(spnManagementSkillPenalty)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(chkUseCommanderLeadershipOnly)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblManagementSkillPenalty)
                        .addComponent(spnManagementSkillPenalty)
                        )));

        return panel;
    }

    /**
     * Constructs and configures a panel for the Fatigue tab.
     * This tab contains a header panel, a checkbox for activating fatigue,
     * a spinner for adjusting the fatigue rate, a checkbox for toggling fatigue from injury,
     * a spinner for adjusting field kitchen capacity, a checkbox for making field kitchens ignore
     * non-combatants and a spinner for adjusting the fatigue leave threshold.
     * <p>
     * The layout of the tab panel is organized with a {@link GroupLayout}, ensuring organized vertical
     * and horizontal alignment.
     *
     * @return panel the configured {@link JPanel} for the Fatigue tab
     */
    JPanel createFatigueTab() {
        // Header
        JPanel headerPanel = new CampaignOptionsHeaderPanel("FatigueTab",
            getImageDirectory() + "logo_free_rasalhague_republic.png",
            true);

        // Contents
        chkUseFatigue = new CampaignOptionsCheckBox("UseFatigue");

        lblFatigueRate = new CampaignOptionsLabel("FatigueRate");
        spnFatigueRate = new CampaignOptionsSpinner("FatigueRate",
            1, 1, 10, 1);

        chkUseInjuryFatigue = new CampaignOptionsCheckBox("UseInjuryFatigue");

        lblFieldKitchenCapacity = new CampaignOptionsLabel("FieldKitchenCapacity");
        spnFieldKitchenCapacity = new CampaignOptionsSpinner("FieldKitchenCapacity",
            150, 0, 450, 1);

        chkFieldKitchenIgnoreNonCombatants = new CampaignOptionsCheckBox("FieldKitchenIgnoreNonCombatants");

        lblFatigueLeaveThreshold = new CampaignOptionsLabel("FatigueLeaveThreshold");
        spnFatigueLeaveThreshold = new CampaignOptionsSpinner("FatigueLeaveThreshold",
            13, 0, 17, 1);

        // Layout the Panel
        final JPanel panel = new CampaignOptionsStandardPanel("FatigueTab", true);
        final GroupLayout layout = createGroupLayout(panel);
        panel.setLayout(layout);

        layout.setVerticalGroup(
            layout.createSequentialGroup()
                .addComponent(headerPanel)
                .addComponent(chkUseFatigue)
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFatigueRate)
                    .addComponent(spnFatigueRate)
                    .addComponent(chkUseInjuryFatigue))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFieldKitchenCapacity)
                    .addComponent(spnFieldKitchenCapacity)
                    .addComponent(chkFieldKitchenIgnoreNonCombatants))
                .addGroup(layout.createParallelGroup(Alignment.BASELINE)
                    .addComponent(lblFatigueLeaveThreshold)
                    .addComponent(spnFatigueLeaveThreshold)));

        layout.setHorizontalGroup(
            layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(Alignment.LEADING)
                    .addComponent(headerPanel, Alignment.CENTER)
                    .addComponent(chkUseFatigue)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFatigueRate)
                        .addComponent(spnFatigueRate)
                        .addComponent(chkUseInjuryFatigue)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFieldKitchenCapacity)
                        .addComponent(spnFieldKitchenCapacity)
                        .addComponent(chkFieldKitchenIgnoreNonCombatants)
                        )
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblFatigueLeaveThreshold)
                        .addComponent(spnFatigueLeaveThreshold)
                        )));

        // Create Parent Panel and return
        return createParentPanel(panel, "FatigueTab");
    }
}
