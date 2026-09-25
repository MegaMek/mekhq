/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.gui.dialog.camOpsSalvage;

import static java.lang.Math.round;
import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getText;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.*;

import jakarta.annotation.Nullable;
import megamek.client.ui.dialogs.unitSelectorDialogs.EntityReadoutDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.units.Entity;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.salvage.AbstractSalvage;
import mekhq.campaign.mission.scenarios.salvage.CamOpsSalvageUtilities;
import mekhq.campaign.mission.scenarios.salvage.RecoveryMethod;
import mekhq.campaign.mission.scenarios.salvage.RecoveryStatus;
import mekhq.campaign.mission.scenarios.salvage.RecoveryTimeCalculations;
import mekhq.campaign.mission.scenarios.salvage.RecoveryTimeData;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoveryPlan;
import mekhq.campaign.mission.scenarios.salvage.SalvageRecoveryPlan.RemainingCapacity;
import mekhq.campaign.mission.scenarios.salvage.SalvageSettlement;
import mekhq.campaign.mission.scenarios.salvage.WreckRecovery;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.roundedComponents.RoundedJButton;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Dialog for managing salvage operations after a scenario is completed.
 *
 * <p>This class presents a dialog that allows players to assign salvage units to recover battlefield salvage.
 * Players can select which units from their salvage forces will be used to recover each piece of salvage, and the
 * dialog tracks recovery time, salvage allocation, and validates assignments.</p>
 *
 * <p>For contract missions, the dialog enforces salvage percentage limits and dynamically updates the salvage
 * allocation between the player's unit and the employer based on which salvage items are claimed.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Validates salvage assignments (cargo capacity, towage capacity, naval tug requirements)</li>
 *   <li>Tracks recovery time based on assigned techs</li>
 *   <li>Enforces contract salvage percentage limits</li>
 *   <li>Distinguishes between salvage for immediate sale vs. salvage to keep</li>
 *   <li>Prevents duplicate assignment of salvage units</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class SalvagePostScenarioPicker {
    private static final MMLogger LOGGER = MMLogger.create(SalvagePostScenarioPicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    private final static int PADDING = scaleForGUI(10);
    private final static Dimension DEFAULT_SIZE = scaleForGUI(1200, 600);

    private final static int UNKNOWN_UNIT_WEIGHT = -1;

    private final boolean isInSpace;
    private final AbstractSalvage salvageRules;
    /** How the salvage is divided between the player and their employer. */
    private final SalvageSettlement settlement;
    /** Which units recover which wrecks, and whether that works. */
    private final SalvageRecoveryPlan recoveryPlan;
    private int maximumSalvageTime = 0;
    private int usedSalvageTime = 0;
    private Money employerSalvageMoneyInitial = Money.zero();
    private Money employerSalvageMoneyCurrent = Money.zero();
    private Money unitSalvageMoneyInitial = Money.zero();
    private Money unitSalvageMoneyCurrent = Money.zero();
    private List<Unit> salvageUnits;
    private List<TestUnit> allUnits;
    private final List<TestUnit> keptSalvage = new ArrayList<>();
    private final List<TestUnit> soldSalvage = new ArrayList<>();
    private final List<TestUnit> employerSalvage = new ArrayList<>();
    private final Map<String, Unit> unitNameMap = new LinkedHashMap<>();
    private final Map<String, Integer> ownedVariantCounts = new HashMap<>();
    private final Map<String, Integer> ownedChassisCounts = new HashMap<>();
    private Map<UUID, RecoveryTimeData> recoveryTimeData;
    /** The player's funds when the dialog opened, used to check salvage purchases can be afforded. */
    private Money availableFunds = Money.zero();
    /** The value of the salvage the player has chosen to keep in this scenario. */
    private Money keptSalvageValue = Money.zero();
    /** The value of the salvage going to the employer in this scenario. */
    private Money employerSalvageValue = Money.zero();

    /**
     * Returns the total number of salvage units being tracked in this operation.
     *
     * <p>This includes all salvage currently categorized as:</p>
     *
     * <ul>
     *   <li><b>Kept salvage</b> — units the player has chosen to retain</li>
     *   <li><b>Sold salvage</b> — units marked for immediate sale</li>
     *   <li><b>Employer salvage</b> — units allocated to the employer</li>
     * </ul>
     *
     * <p>The total reflects the sum of these three lists and represents every salvage unit processed after a
     * scenario.</p>
     *
     * @return the total count of salvage units across kept, sold, and employer categories
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getCountOfSalvageUnits() {
        return keptSalvage.size() + soldSalvage.size() + employerSalvage.size();
    }

    /**
     * Returns the number of tech minutes spent recovering salvage.
     *
     * @return the minutes spent on recovery; {@code 0} if the salvage dialog was never shown
     *
     * @author Illiani
     * @since 0.51.01
     */
    public int getUsedSalvageTime() {
        return usedSalvageTime;
    }

    /**
     * Groups a salvage unit's combo boxes with their associated labels.
     *
     * <p>This class encapsulates all UI components related to assigning salvage forces to a single
     * salvage unit. It includes two combo boxes for selecting recovery units, labels for displaying the unit
     * information and validation status, and a flag to prevent recursive updates during combo box changes.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static class SalvageComboBoxGroup {
        final JButton unitButton;
        final JComboBox<String> comboBoxLeft;
        final JComboBox<String> comboBoxRight;
        final JLabel validationLabel;
        final JLabel unitLabel;
        final JCheckBox claimedSalvageForKeeps;
        final JCheckBox claimedSalvageForSale;
        final TestUnit targetUnit;
        /** The plan for recovering the wreck, which decides whether the assigned units can recover it. */
        final WreckRecovery recovery;
        boolean isUpdating = false;  // Flag to prevent recursive updates
        /** Lets the player choose to carry or drag the wreck, where both are possible; {@code null} if not offered. */
        @Nullable JComboBox<RecoveryMethod> recoveryMethodBox = null;

        /**
         * Creates a new salvage combo box group.
         *
         * @param unitButton             a button used to access field stripping
         * @param comboBoxLeft           first combo box for selecting a salvage unit
         * @param comboBoxRight          second combo box for selecting a salvage unit
         * @param validationLabel        label displaying validation status
         * @param unitLabel              label displaying the salvage unit name and value
         * @param claimedSalvageForKeeps {@code true} if the player claimed the salvage for keeps
         * @param claimedSalvageForSale  {@code true} if the player claimed the salvage for immediate sale
         * @param recovery               the plan for recovering the salvage unit
         *
         * @author Illiani
         * @since 0.50.10
         */
        SalvageComboBoxGroup(JButton unitButton, JComboBox<String> comboBoxLeft, JComboBox<String> comboBoxRight,
              JLabel validationLabel, JLabel unitLabel, JCheckBox claimedSalvageForKeeps,
              JCheckBox claimedSalvageForSale, WreckRecovery recovery) {
            this.unitButton = unitButton;
            this.comboBoxLeft = comboBoxLeft;
            this.comboBoxRight = comboBoxRight;
            this.validationLabel = validationLabel;
            this.unitLabel = unitLabel;
            this.claimedSalvageForKeeps = claimedSalvageForKeeps;
            this.claimedSalvageForSale = claimedSalvageForSale;
            this.targetUnit = recovery.getWreck();
            this.recovery = recovery;
        }
    }

    /**
     * Creates a new post-salvage picker dialog and processes the selected salvage.
     *
     * <p>This constructor displays a dialog allowing the player to select which salvage units to claim and which
     * salvage forces to assign to recover them. After the dialog is confirmed, it processes the selections and resolves
     * the salvage through the campaign.</p>
     *
     * <p>If the contract grants no salvage rights, the dialog is skipped and no salvage is processed.</p>
     *
     * @param campaign      the current {@link Campaign} in which the scenario took place
     * @param salvageRules  the rules of the campaign's salvage system
     * @param mission       the {@link AbstractContract} associated with the scenario
     * @param scenario      the {@link Scenario} that was just completed
     * @param actualSalvage the list of {@link TestUnit}s available as salvage that the player can claim
     * @param soldSalvage   the list of {@link TestUnit}s that are marked for immediate sale
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvagePostScenarioPicker(Campaign campaign, AbstractSalvage salvageRules, AbstractContract mission,
          Scenario scenario, List<TestUnit> actualSalvage, List<TestUnit> soldSalvage) {
        this.isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;
        this.salvageRules = salvageRules;
        this.settlement = salvageRules.createSettlement(mission);
        this.recoveryPlan = new SalvageRecoveryPlan(salvageRules, isInSpace);

        setAvailableTechTime(campaign, scenario);
        List<Integer> salvageFormations = setSalvageUnits(campaign, scenario);
        sanitizeOtherScenarioAssignments(campaign.getActiveScenarios(), scenario, scenario.getSalvageTechs(),
              salvageFormations);

        arrangeUnits(actualSalvage, soldSalvage);
        setRecoveryTimeDataMap(campaign, scenario);

        boolean playerGetsNoSalvage = !mission.canSalvage();
        if (playerGetsNoSalvage) {
            return; // There isn't going to be anything to process
        }

        employerSalvageMoneyInitial = mission.getSalvagedByEmployerValue();
        employerSalvageMoneyCurrent = employerSalvageMoneyInitial;
        unitSalvageMoneyInitial = mission.getSalvagedByUnitValue();
        unitSalvageMoneyCurrent = unitSalvageMoneyInitial;
        availableFunds = campaign.getPlayerForce().getFinances().getBalance();

        showSalvageDialog(campaign);

        // Process selected units
        CamOpsSalvageUtilities.resolveSalvage(campaign, mission, scenario, settlement, this.keptSalvage,
              this.soldSalvage, this.employerSalvage);
    }

    /**
     * If the player follows a very convoluted stream of steps, it's possible for them to assign the same force or tech
     * to multiple salvage operations on the same day. This method ensures this doesn't happen by removing the force/s
     * (and tech/s) from any other scenarios they have been assigned to.
     *
     * @param activeScenarios   a list of scenarios marked as 'current' (i.e., unresolved)
     * @param currentScenario   the current scenario, techs and forces won't be sanitized from this scenario
     * @param salvageTechs      a list of techs assigned to the salvage operation
     * @param salvageFormations a list of forces assigned to the salvage operation
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void sanitizeOtherScenarioAssignments(List<Scenario> activeScenarios, Scenario currentScenario,
          List<UUID> salvageTechs,
          List<Integer> salvageFormations) {
        for (Scenario activeScenario : activeScenarios) {
            if (activeScenario == currentScenario) {
                continue;
            }

            activeScenario.removeSalvageFormation(salvageFormations);
            activeScenario.removeSalvageTechs(salvageTechs);
        }
    }

    /**
     * Initializes the recovery time data map for all salvage units.
     *
     * <p>For each unit in the salvage list, calculates the recovery time based on the entity's characteristics, the
     * scenario conditions, and the planet's environment. Stores the results in a map keyed by unit ID for quick lookup
     * during validation and time tracking.</p>
     *
     * @param campaign the campaign containing the salvage operation
     * @param scenario the scenario from which salvage is being recovered
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void setRecoveryTimeDataMap(Campaign campaign, Scenario scenario) {
        recoveryTimeData = new HashMap<>();
        for (TestUnit unit : allUnits) {
            Entity entity = unit.getEntity();
            if (entity == null) {
                LOGGER.error("Entity for unit {} not found in campaign", unit.getId());
                continue;
            }

            RecoveryTimeData data = RecoveryTimeCalculations.calculateRecoveryTimeForEntity(entity.getDisplayName(),
                  entity.getRecoveryTime(), entity.isAero(), scenario, campaign.getPlayerForce()
                                                            .getForceDetachment()
                                                            .getCurrentLocation()
                                                            .getPlanet());
            recoveryTimeData.put(unit.getId(), data);
        }
    }

    /**
     * Populates the list of available salvage units from the scenario's assigned salvage forces.
     *
     * <p>Retrieves all forces assigned to salvage operations for this scenario and collects units from those forces
     * that are capable of salvaging in the current environment (ground or space).</p>
     *
     * <p>Units that fought in the scenario are excluded, unless the salvage system allows Salvage formations to fight
     * and then salvage, and the unit's formation is a Salvage formation.</p>
     *
     * @param campaign the campaign containing the salvage forces
     * @param scenario the scenario being resolved
     *
     * @return the ID numbers of the forces involved in the salvage operation
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<Integer> setSalvageUnits(Campaign campaign, Scenario scenario) {
        List<Integer> salvageFormations = new ArrayList<>();
        salvageUnits = new ArrayList<>();
        mekhq.campaign.LocalHangar hangar = campaign.getPlayerForce().getHangar();
        for (Integer forceId : scenario.getSalvageFormations()) {
            salvageFormations.add(forceId);

            Formation formation = campaign.getPlayerForce().getFormation(forceId);
            if (formation == null) {
                LOGGER.error("Force {} not found in campaign", forceId);
                continue;
            }

            // Units that fought can't also salvage, unless the salvage system lets Salvage formations do both
            boolean canCombatUnitsSalvage = salvageRules.canSalvageAfterFighting(formation);
            for (Unit unit : formation.getAllUnitsAsUnits(hangar, false)) {
                boolean didFightInScenario = unit.getScenarioId() == scenario.getId();
                if (didFightInScenario && !canCombatUnitsSalvage) {
                    continue;
                }

                if (salvageRules.isAvailableForSalvage(unit, isInSpace)) {
                    salvageUnits.add(unit);
                }
            }
        }

        return salvageFormations;
    }

    /**
     * Arranges salvage units in display order, sorted by sell value from highest to lowest.
     *
     * <p>Combines units marked for immediate sale with units to be kept, then sorts them by their sell value in
     * descending order so the most valuable salvage appears first.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void arrangeUnits(List<TestUnit> actualSalvage, List<TestUnit> soldSalvage) {
        allUnits = new ArrayList<>(actualSalvage);
        allUnits.addAll(soldSalvage);
        allUnits.sort(Comparator.comparing(TestUnit::getSellValue).reversed()); // Highest -> Lowest

    }


    /**
     * Calculates the total available tech time for salvage operations.
     *
     * <p>Sums the remaining minutes of all techs assigned to salvage operations for this scenario. This total is
     * used to validate that salvage assignments don't exceed available tech time.</p>
     *
     * @param campaign the campaign containing the assigned techs
     * @param scenario the scenario being resolved
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void setAvailableTechTime(Campaign campaign, Scenario scenario) {
        List<UUID> assignedTechIds = scenario.getSalvageTechs();
        for (UUID techId : assignedTechIds) {
            Person tech = campaign.getPlayerForce().getHumanResources().getPerson(techId);
            if (tech == null) {
                LOGGER.error("Salvage tech {} not found in campaign", techId);
                continue;
            }

            // I don't expect we'll have negative tech minutes, but you never know
            maximumSalvageTime += Math.max(0, tech.getMinutesLeft());
        }
    }

    /**
     * Displays the salvage selection dialog.
     *
     * <p>Creates and displays a modal dialog showing all available salvage with combo boxes to assign recovery units.
     * For contract missions, also displays salvage percentage information and enforces salvage limits. The dialog
     * validates all assignments and prevents confirmation if any assignments are invalid.</p>
     *
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void showSalvageDialog(Campaign campaign) {
        countOwnedUnits(campaign);

        JDialog dialog = new JDialog((Frame) null, getText("accessingTerminal.title"), true);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // We don't want the player to cancel out

        // Info panel at the top (only for contracts)
        JLabel employerSalvageLabel = null;
        JLabel salvagePercentLabel = null;
        JLabel unitSalvageLabel = null;
        JLabel availableTimeLabel = null;

        JPanel infoContainer = new JPanel(new BorderLayout());
        infoContainer.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        // Left column (existing info labels)
        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));

        // Unless salvage is capped, the salvage percent is the player's share, not a cap
        int salvagePercent = settlement.getPlayerSharePercent();
        if (settlement.isSalvageCapped()) {
            salvagePercentLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent.normal",
                  getCurrentPercentAsBigDecimal(),
                  salvagePercent));
        } else {
            salvagePercentLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent.exchange",
                  salvagePercent));
        }
        employerSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "SalvagePostScenarioPicker.employerSalvage", employerSalvageMoneyCurrent.toAmountString()));
        if (isExchangeRights()) {
            unitSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.unitSalvage", getExchangeUnitSalvage().toAmountString()));
        } else {
            unitSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.unitSalvage", unitSalvageMoneyCurrent.toAmountString()));
        }
        availableTimeLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "SalvagePostScenarioPicker.time", usedSalvageTime, maximumSalvageTime));
        // Without salvage operations there is no recovery time to track
        availableTimeLabel.setVisible(salvageRules.isUseSalvageOperations());
        if (settlement.isKeptSalvageBought()) {
            updatePurchaseLabels(employerSalvageLabel, unitSalvageLabel);
        }

        infoPanel.add(salvagePercentLabel);
        infoPanel.add(employerSalvageLabel);
        infoPanel.add(unitSalvageLabel);
        infoPanel.add(availableTimeLabel);

        // Right column (tutorial text)
        JEditorPane tutorialPane = new JEditorPane();
        tutorialPane.setContentType("text/html");
        tutorialPane.setEditable(false);
        tutorialPane.setOpaque(false);
        String tutorialText;
        if (salvageRules.isUseSalvageOperations()) {
            tutorialText = getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial");
            if (salvageRules.isMultipleSalvagePerUnitAllowed()) {
                tutorialText += getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial.sharedCapacity");
            }
        } else {
            tutorialText = getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial.automaticRecovery");
        }
        if (settlement.isKeptSalvageBought()) {
            tutorialText += getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial.purchases",
                  salvagePercent);
        }
        tutorialPane.setText(tutorialText);
        tutorialPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        int preferredWidth = scaleForGUI(700);
        tutorialPane.setSize(preferredWidth, Short.MAX_VALUE);
        Dimension preferredSize = tutorialPane.getPreferredSize();
        preferredSize.width = preferredWidth;
        tutorialPane.setPreferredSize(preferredSize);

        // Add both to container
        infoContainer.add(infoPanel, BorderLayout.CENTER);
        infoContainer.add(tutorialPane, BorderLayout.EAST);

        dialog.add(infoContainer, BorderLayout.NORTH);

        // Final references for use in lambdas
        final JLabel finalSalvagePercentLabel = salvagePercentLabel;
        final JLabel finalEmployerSalvageLabel = employerSalvageLabel;
        final JLabel finalUnitSalvageLabel = unitSalvageLabel;
        final JLabel finalAvailableTimeLabel = availableTimeLabel;

        // Main panel with single column
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(RoundedLineBorder.createRoundedLineBorder());

        // Track all combo boxes and their associated validation labels
        List<SalvageComboBoxGroup> salvageComboBoxGroups = new ArrayList<>();

        // Button panel (created early so we can reference it in listeners)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        RoundedJButton confirmButton = new RoundedJButton(getText("Confirm.text"));

        // Build the mapping and populate salvage unit options ONCE, outside the loop
        unitNameMap.clear();
        for (Unit salvageUnit : salvageUnits) {
            String base = CamOpsSalvageUtilities.getSalvageTooltip(List.of(salvageUnit), isInSpace, salvageRules);
            String key = base;
            int duplicate = 2;
            while (unitNameMap.containsKey(key)) {
                key = base + " [" + duplicate++ + "]";
            }
            unitNameMap.put(key, salvageUnit);
        }

        // We sort alphabetically for ease of use. Natural ordering keeps "#10" after "#9". The map itself is rebuilt
        // in sorted order, as every combo box is repopulated from it whenever a selection changes.
        List<String> names = new ArrayList<>(unitNameMap.keySet());
        names.sort(new NaturalOrderComparator());
        Map<String, Unit> unsortedUnitNameMap = new HashMap<>(unitNameMap);
        unitNameMap.clear();
        for (String name : names) {
            unitNameMap.put(name, unsortedUnitNameMap.get(name));
        }

        // Add all units to single column
        for (TestUnit unit : allUnits) {
            String unitName = unit.getName();
            Money sellValue = unit.getSellValue();

            // Create row panel with label, two combo boxes, and validation label
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(75)));

            int unitWeight = getUnitWeight(unit);
            String unitWeightString = unitWeight == UNKNOWN_UNIT_WEIGHT ? "?" : String.valueOf(unitWeight);

            JLabel unitLabel = new JLabel();
            Entity salvageEntity = unit.getEntity();
            int ownedVariantCount = 0;
            int ownedChassisCount = 0;
            if (salvageEntity != null) {
                ownedVariantCount = ownedVariantCounts.getOrDefault(getVariantKey(salvageEntity), 0);
                ownedChassisCount = ownedChassisCounts.getOrDefault(salvageEntity.getChassis(), 0);
            }
            unitLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.unitLabel.unit",
                  unitName, sellValue.toAmountString(), unitWeightString, ownedVariantCount, ownedChassisCount));

            RecoveryTimeData data = recoveryTimeData.get(unit.getId());
            if (data != null) {
                unitLabel.setToolTipText(wordWrap(data.getRecoveryTimeBreakdownString(false)));
            } else {
                LOGGER.error("No recovery time data found for unit {}", unit.getId());
            }

            JLabel validationLabel = new JLabel();

            JCheckBox claimedSalvageForKeeps = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.unitLabel.salvage"));
            claimedSalvageForKeeps.setEnabled(false);
            JCheckBox claimedSalvageForSale = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.unitLabel.sale"));
            claimedSalvageForSale.setEnabled(false);

            // Under salvage purchases, salvage is bought rather than claimed, and there's nothing to sell: salvage
            // that isn't bought is paid out as the player's share
            if (settlement.isKeptSalvageBought()) {
                claimedSalvageForSale.setVisible(false);
                Money unitPurchaseCost = settlement.getPurchaseCost(sellValue);
                claimedSalvageForKeeps.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.unitLabel.buy", unitPurchaseCost.toAmountString()));
                claimedSalvageForKeeps.setToolTipText(wordWrap(getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.unitLabel.buy.tooltip", unitPurchaseCost.toAmountString(),
                      settlement.getCashShare(sellValue).toAmountString())));
            }

            JComboBox<String> comboBox1 = new JComboBox<>();
            fixComboBoxWidth(comboBox1);
            comboBox1.addItem(null); // Allow empty selection
            JComboBox<String> comboBox2 = new JComboBox<>();
            fixComboBoxWidth(comboBox2);
            comboBox2.addItem(null); // Allow empty selection

            // Carriers can take on several wrecks, so show how much room each has left
            if (salvageRules.isMultipleSalvagePerUnitAllowed()) {
                comboBox1.setRenderer(createRemainingCapacityRenderer());
                comboBox2.setRenderer(createRemainingCapacityRenderer());
            }

            // Build the mapping and populate combo boxes
            for (String displayName : names) {
                comboBox1.addItem(displayName);
                comboBox2.addItem(displayName);
            }

            RoundedJButton viewButton = new RoundedJButton("\u24D8");
            viewButton.setFocusable(false);

            RoundedJButton fieldStripButton = new RoundedJButton("\u2692");
            fieldStripButton.setEnabled(false); // TODO remove this line when we're ready to implement field stripping
            fieldStripButton.setFocusable(false);
            fieldStripButton.setToolTipText(getTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.fieldStripButton.tooltip"));
            fieldStripButton.putClientProperty("unitId", unit);

            SalvageComboBoxGroup group = new SalvageComboBoxGroup(fieldStripButton,
                  comboBox1,
                  comboBox2,
                  validationLabel,
                  unitLabel,
                  claimedSalvageForKeeps,
                  claimedSalvageForSale,
                  recoveryPlan.addWreck(unit));
            salvageComboBoxGroups.add(group);

            // Where a carrier can take on several wrecks, the player chooses whether a unit carries or drags. There's
            // no dragging in space.
            if (recoveryPlan.isRecoveryMethodChoiceOffered()) {
                JComboBox<RecoveryMethod> recoveryMethodBox = new JComboBox<>();
                recoveryMethodBox.addItem(null);
                for (RecoveryMethod recoveryMethod : RecoveryMethod.values()) {
                    recoveryMethodBox.addItem(recoveryMethod);
                }
                recoveryMethodBox.setEnabled(false);
                recoveryMethodBox.setToolTipText(wordWrap(getTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.recoveryMethod.tooltip")));
                Dimension recoveryMethodSize = scaleForGUI(90, recoveryMethodBox.getPreferredSize().height);
                recoveryMethodBox.setPreferredSize(recoveryMethodSize);
                recoveryMethodBox.setMinimumSize(recoveryMethodSize);
                recoveryMethodBox.setMaximumSize(recoveryMethodSize);
                recoveryMethodBox.addActionListener(e -> performComboChangeAction(salvageComboBoxGroups,
                      group, finalSalvagePercentLabel, finalEmployerSalvageLabel, finalUnitSalvageLabel,
                      finalAvailableTimeLabel, confirmButton));
                group.recoveryMethodBox = recoveryMethodBox;
            }

            // These need to be after the above lines, as we're going to use 'group' in the listeners.
            comboBox1.addActionListener(e -> performComboChangeAction(salvageComboBoxGroups,
                  group, finalSalvagePercentLabel, finalEmployerSalvageLabel, finalUnitSalvageLabel,
                  finalAvailableTimeLabel, confirmButton));
            comboBox2.addActionListener(e -> performComboChangeAction(salvageComboBoxGroups,
                  group, finalSalvagePercentLabel, finalEmployerSalvageLabel, finalUnitSalvageLabel,
                  finalAvailableTimeLabel, confirmButton));
            // Keeps and Sale are mutually exclusive; whichever was just ticked wins
            claimedSalvageForKeeps.addActionListener(e -> {
                if (claimedSalvageForKeeps.isSelected()) {
                    claimedSalvageForSale.setSelected(false);
                }
                performComboChangeAction(salvageComboBoxGroups, group, finalSalvagePercentLabel,
                      finalEmployerSalvageLabel, finalUnitSalvageLabel, finalAvailableTimeLabel, confirmButton);
            });
            claimedSalvageForSale.addActionListener(e -> {
                if (claimedSalvageForSale.isSelected()) {
                    claimedSalvageForKeeps.setSelected(false);
                }
                performComboChangeAction(salvageComboBoxGroups, group, finalSalvagePercentLabel,
                      finalEmployerSalvageLabel, finalUnitSalvageLabel, finalAvailableTimeLabel, confirmButton);
            });
            viewButton.addActionListener(new ViewUnitListener(group.targetUnit));

            fieldStripButton.addActionListener(e -> fieldStrip(group));

            rowPanel.add(viewButton);
            rowPanel.add(fieldStripButton);
            rowPanel.add(claimedSalvageForKeeps);
            rowPanel.add(claimedSalvageForSale);
            rowPanel.add(unitLabel);
            // Without salvage operations, wrecks are recovered automatically, so there's no one to assign
            if (salvageRules.isUseSalvageOperations()) {
                rowPanel.add(comboBox1);
                rowPanel.add(comboBox2);
                if (group.recoveryMethodBox != null) {
                    rowPanel.add(group.recoveryMethodBox);
                }
            }
            rowPanel.add(validationLabel);

            column.add(rowPanel);
        }

        JScrollPane scrollPane = new JScrollPane(column);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);

        confirmButton.addActionListener(e -> confirmationAction(campaign, dialog));

        buttonPanel.add(confirmButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Initial state. Without salvage operations, every wreck starts out recovered.
        revalidateAllGroups(salvageComboBoxGroups);
        updateSalvageAllocation(salvageComboBoxGroups, finalSalvagePercentLabel, finalEmployerSalvageLabel,
              finalUnitSalvageLabel, finalAvailableTimeLabel);
        updateConfirmButtonState(salvageComboBoxGroups, confirmButton, finalUnitSalvageLabel, finalAvailableTimeLabel);

        dialog.setPreferredSize(DEFAULT_SIZE);
        dialog.setSize(DEFAULT_SIZE);
        dialog.setLocationRelativeTo(null);
        setPreferences(dialog); // Must be before setVisible
        dialog.setVisible(true);
    }

    /**
     * Counts the units the player already owns, by exact variant and by chassis, so each piece of salvage can show how
     * many of it the player already has.
     *
     * @param campaign the current campaign
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void countOwnedUnits(Campaign campaign) {
        ownedVariantCounts.clear();
        ownedChassisCounts.clear();
        for (Unit ownedUnit : campaign.getPlayerForce().getHangar().getUnits()) {
            Entity ownedEntity = ownedUnit.getEntity();
            if (ownedEntity == null) {
                continue;
            }

            ownedVariantCounts.merge(getVariantKey(ownedEntity), 1, Integer::sum);
            ownedChassisCounts.merge(ownedEntity.getChassis(), 1, Integer::sum);
        }
    }

    /**
     * Builds a key identifying an entity's exact variant (chassis and model).
     *
     * @param entity the entity to identify
     *
     * @return the variant key
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String getVariantKey(Entity entity) {
        return entity.getChassis() + ' ' + entity.getModel();
    }

    /**
     * Creates a renderer for the recovery unit drop-down boxes that shows, before each carrier's name, how much cargo
     * space or how many bays it has left.
     *
     * @return the renderer
     *
     * @author Illiani
     * @since 0.51.01
     */
    private ListCellRenderer<Object> createRemainingCapacityRenderer() {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String unitName) {
                    Unit unit = unitNameMap.get(unitName);
                    String prefix = (unit == null) ? null : getRemainingCapacityPrefix(unit);
                    if (prefix != null) {
                        setText(prefix + ' ' + unitName);
                    }
                }
                return this;
            }
        };
    }

    /**
     * Shows the player's cash share and purchase cost, in place of the salvage split, under salvage purchases.
     *
     * @param cashShareLabel    label showing the cash the player will be paid (can be null)
     * @param purchaseCostLabel label showing what the player will pay for the salvage they're buying (can be null)
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void updatePurchaseLabels(@Nullable JLabel cashShareLabel, @Nullable JLabel purchaseCostLabel) {
        if (cashShareLabel != null) {
            cashShareLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.cashShare",
                  settlement.getCashShare(employerSalvageValue).toAmountString()));
        }
        if (purchaseCostLabel != null) {
            purchaseCostLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.purchaseCost",
                  settlement.getPurchaseCost(keptSalvageValue).toAmountString(), availableFunds.toAmountString()));
        }
    }

    private static int getUnitWeight(TestUnit unit) {
        Entity entity = unit.getEntity();
        int unitWeight = UNKNOWN_UNIT_WEIGHT;
        if (entity != null) {
            unitWeight = (int) round(entity.getWeight());
        }

        return unitWeight;
    }

    private static void confirmationAction(Campaign campaign, JDialog dialog) {
        dialog.setVisible(false);
        ImmersiveDialogSimple confirmationDialog = new ImmersiveDialogSimple(campaign, null, null,
              getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.confirmation.text"),
              List.of(getText("Cancel.text"), getText("Confirm.text")), null, null, false);
        if (confirmationDialog.getDialogChoice() == 0) { // Cancelled
            dialog.setVisible(true);
            return;
        }

        dialog.dispose();
    }

    /**
     * This is not currently implemented. The purpose of this method is to allow future developers and easy access point
     * to implement field stripping, without needing to grok where it should fit in the class (and gui).
     */
    private void fieldStrip(SalvageComboBoxGroup group) {
        TestUnit targetUnit = group.targetUnit;
        // Example: open a JDialog with a list of parts that can be stripped and a way to pick a tech (that is
        // assigned to the scenario) to perform the task. We can probably ape the Repair tab.
    }

    /**
     * Performs all necessary updates when a combo box selection changes.
     *
     * <p>This consolidated method handles:</p>
     * <ul>
     *   <li>Revalidating every assignment, as rows may share a carrier's cargo space or bays</li>
     *   <li>Updating available options in all combo boxes to prevent invalid assignments</li>
     *   <li>Recalculating salvage allocation and time usage</li>
     *   <li>Updating the confirm button state</li>
     * </ul>
     *
     * @param salvageComboBoxGroups     list of all combo box groups
     * @param group                     the specific group that changed
     * @param finalEmployerSalvageLabel label displaying employer salvage value
     * @param finalUnitSalvageLabel     label displaying unit salvage value
     * @param finalAvailableTimeLabel   label displaying time usage
     * @param confirmButton             the dialog's confirm button
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void performComboChangeAction(List<SalvageComboBoxGroup> salvageComboBoxGroups,
          SalvageComboBoxGroup group, JLabel finalSalvagePercentLabel, JLabel finalEmployerSalvageLabel,
          JLabel finalUnitSalvageLabel, JLabel finalAvailableTimeLabel, JButton confirmButton) {
        // Prevent recursive calls
        if (group.isUpdating) {
            return;
        }

        try {
            group.isUpdating = true;
            // Rows can share a carrier's cargo space or bays, so a change to one row can affect any other
            revalidateAllGroups(salvageComboBoxGroups);
            updateComboBoxOptions(salvageComboBoxGroups);

            updateSalvageAllocation(salvageComboBoxGroups,
                  finalSalvagePercentLabel,
                  finalEmployerSalvageLabel,
                  finalUnitSalvageLabel,
                  finalAvailableTimeLabel);
            updateConfirmButtonState(salvageComboBoxGroups, confirmButton, finalUnitSalvageLabel,
                  finalAvailableTimeLabel);
        } finally {
            group.isUpdating = false;
        }
    }

    /**
     * Updates the confirm button enabled state based on validation rules.
     *
     * <p>The confirm button is disabled if:</p>
     * <ul>
     *   <li>Any salvage assignment has invalid validation (insufficient capacity, missing naval tug)</li>
     *   <li>For contracts: the player's salvage percentage exceeds the contract limit (not under salvage
     *   purchases)</li>
     *   <li>Under salvage purchases: the player can't afford the salvage they've chosen to buy</li>
     *   <li>The used minutes exceed the available minutes</li>
     * </ul>
     *
     * <p>When the salvage percentage is exceeded, the unit salvage label is also colored red.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     * @param confirmButton         the confirm button to enable/disable
     * @param unitSalvageLabel      label showing unit salvage value (colored red if limit exceeded)
     * @param salvageTimeLabel      label showing salvage time spent (colored red if limit exceeded)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateConfirmButtonState(List<SalvageComboBoxGroup> salvageComboBoxGroups, JButton confirmButton,
          JLabel unitSalvageLabel, JLabel salvageTimeLabel) {
        boolean shouldEnable = true;

        // Check for any assignments that can't recover their wreck
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            group.unitLabel.setForeground(null); // reset

            if (group.recovery.getStatus().isProblem()) {
                disableConfirmAndColorName(confirmButton, group.unitLabel);
                shouldEnable = false;
            }
        }

        // Check salvage percentage if this is a contract
        unitSalvageLabel.setForeground(null);
        BigDecimal currentPercent = getCurrentPercentAsBigDecimal();
        boolean isOverSalvageCap = currentPercent.compareTo(BigDecimal.valueOf(settlement.getPlayerSharePercent())) > 0;
        if (settlement.isSalvageCapped() && isOverSalvageCap) {
            disableConfirmAndColorName(confirmButton, unitSalvageLabel);
            // If we've gone over our %, we only block progression if the player is trying to salvage even more.
            shouldEnable &= unitSalvageMoneyCurrent.compareTo(unitSalvageMoneyInitial) <= 0;
        }

        // Salvage purchases must be affordable
        if (!settlement.canAfford(keptSalvageValue, availableFunds)) {
            disableConfirmAndColorName(confirmButton, unitSalvageLabel);
            shouldEnable = false;
        }

        // Time budget check (disable and, ideally, color the time label in updateSalvageAllocation)
        if (usedSalvageTime > maximumSalvageTime) {
            disableConfirmAndColorName(confirmButton, salvageTimeLabel);
            shouldEnable = false;
        } else {
            if (salvageTimeLabel != null) {
                salvageTimeLabel.setForeground(null);
            }
        }

        // All checks passed
        confirmButton.setEnabled(shouldEnable);
    }

    /**
     * Calculates the unit's running salvage total for a contract with salvage exchange rights.
     *
     * <p>This is the salvage value the unit had already earned this contract, plus the unit's cut of any salvage
     * recovered for the employer in this scenario.</p>
     *
     * @return the unit's running salvage total
     *
     * @author Illiani
     * @since 0.51.01
     */
    private Money getExchangeUnitSalvage() {
        Money employerSalvageThisScenario = employerSalvageMoneyCurrent.minus(employerSalvageMoneyInitial);
        return unitSalvageMoneyInitial.plus(settlement.getCashShare(employerSalvageThisScenario));
    }

    /**
     * Checks whether the contract has salvage exchange rights, under which all salvage goes to the employer and the
     * player is paid a share of its value.
     *
     * @return {@code true} if the player can't keep any salvage
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean isExchangeRights() {
        return !settlement.canKeepSalvage();
    }

    private BigDecimal getCurrentPercentAsBigDecimal() {
        Money totalSalvage = employerSalvageMoneyCurrent.plus(unitSalvageMoneyCurrent);
        BigDecimal currentPercent = BigDecimal.valueOf(0);
        if (totalSalvage.isPositive()) {
            // Calculate percentage: (unitSalvage / totalSalvage) * 100
            BigDecimal hundred = BigDecimal.valueOf(100);
            currentPercent = unitSalvageMoneyCurrent.getAmount()
                                   .multiply(hundred)
                                   .divide(totalSalvage.getAmount(), 4, RoundingMode.HALF_UP);
        }
        return currentPercent;
    }

    private static void disableConfirmAndColorName(JButton confirmButton, JLabel unitSalvageLabel) {
        if (unitSalvageLabel != null) {
            unitSalvageLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
        }
        confirmButton.setEnabled(false);
    }

    /**
     * Updates salvage allocation tracking based on which units have salvage teams assigned.
     *
     * <p>Recalculates:</p>
     * <ul>
     *   <li>Used salvage time: sum of recovery times for all assigned salvage</li>
     *   <li>Unit salvage value: sum of sell values for salvage with assigned recovery forces</li>
     *   <li>Employer salvage value: sum of sell values for salvage without assigned recovery forces</li>
     * </ul>
     *
     * <p>Updates the provided labels with the new values.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     * @param salvagePercentLabel   label showing employer-unit salvage percent (can be null)
     * @param employerSalvageLabel  label showing employer salvage value (can be null)
     * @param unitSalvageLabel      label showing unit salvage value (can be null)
     * @param availableTimeLabel    label showing time usage (can be null)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateSalvageAllocation(List<SalvageComboBoxGroup> salvageComboBoxGroups,
          @Nullable JLabel salvagePercentLabel, @Nullable JLabel employerSalvageLabel,
          @Nullable JLabel unitSalvageLabel, @Nullable JLabel availableTimeLabel) {
        usedSalvageTime = 0;
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            if (group.recovery.hasRecoveryUnits()) {
                RecoveryTimeData timeData = recoveryTimeData.get(group.targetUnit.getId());
                if (timeData != null) {
                    // Saturate rather than overflow, so an absurd recovery time can't wrap around to negative
                    usedSalvageTime = (int) Math.min(Integer.MAX_VALUE,
                          (long) usedSalvageTime + timeData.totalRecoveryTime());
                }
            }
        }

        Money tempEmployerSalvage = Money.zero();
        for (TestUnit employerUnit : employerSalvage) {
            tempEmployerSalvage = tempEmployerSalvage.plus(employerUnit.getSellValue());
        }

        Money tempUnitSalvage = Money.zero();
        for (TestUnit keptUnit : keptSalvage) {
            tempUnitSalvage = tempUnitSalvage.plus(keptUnit.getSellValue());
        }
        for (TestUnit soldUnit : soldSalvage) {
            tempUnitSalvage = tempUnitSalvage.plus(soldUnit.getSellValue());
        }

        // Update the actual tracking values
        employerSalvageMoneyCurrent = employerSalvageMoneyInitial.plus(tempEmployerSalvage);
        unitSalvageMoneyCurrent = unitSalvageMoneyInitial.plus(tempUnitSalvage);

        keptSalvageValue = Money.zero();
        for (TestUnit keptUnit : keptSalvage) {
            keptSalvageValue = keptSalvageValue.plus(keptUnit.getSellValue());
        }
        employerSalvageValue = tempEmployerSalvage;

        if (settlement.isKeptSalvageBought()) {
            updatePurchaseLabels(employerSalvageLabel, unitSalvageLabel);
            if (availableTimeLabel != null) {
                availableTimeLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.time", usedSalvageTime, maximumSalvageTime));
            }
            return;
        }

        // Update labels if they exist
        if (salvagePercentLabel != null && settlement.isSalvageCapped()) {
            salvagePercentLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent.normal", getCurrentPercentAsBigDecimal(),
                  settlement.getPlayerSharePercent()));
        }
        if (employerSalvageLabel != null) {
            employerSalvageLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.employerSalvage", employerSalvageMoneyCurrent.toAmountString()));
        }
        if (unitSalvageLabel != null) {
            String label;
            if (isExchangeRights()) {
                label = getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.unitSalvage", getExchangeUnitSalvage().toAmountString());
            } else {
                label = getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.unitSalvage", unitSalvageMoneyCurrent.toAmountString());
            }
            unitSalvageLabel.setText(label);
        }
        if (availableTimeLabel != null) {
            availableTimeLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.time", usedSalvageTime, maximumSalvageTime));
        }
    }

    /**
     * Revalidates every salvage assignment, then shows the result on each row.
     *
     * <p>The rows' selections are copied into the recovery plan, which works out whether each wreck can be recovered
     * (see {@link SalvageRecoveryPlan#revalidate()}). Each row's salvage is then moved to the matching kept, sold, or
     * employer list.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void revalidateAllGroups(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            group.recovery.setRecoveryUnits(getSelectedUnit(group.comboBoxLeft), getSelectedUnit(group.comboBoxRight));
            if (group.recoveryMethodBox != null) {
                group.recovery.setPreferredRecoveryMethod((RecoveryMethod) group.recoveryMethodBox.getSelectedItem());
            }
        }

        recoveryPlan.revalidate();

        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            showRecoveryStatus(group);
            syncRecoveryMethodBox(group);
            syncMembershipForGroup(group, group.recovery.isRecovered());
        }
    }

    /**
     * Shows a row's recovery status, and enables its claim checkboxes if the wreck will be recovered.
     *
     * @param group the combo box group to update
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void showRecoveryStatus(SalvageComboBoxGroup group) {
        RecoveryStatus status = group.recovery.getStatus();
        group.validationLabel.setText(status.getLabel());

        if (status.isRecovered()) {
            group.unitLabel.setForeground(null); // Reset to default color
            group.claimedSalvageForKeeps.setEnabled(settlement.canKeepSalvage());
            group.claimedSalvageForSale.setEnabled(settlement.canSellSalvage());
            return;
        }

        group.unitLabel.setForeground(status.isProblem() ? MekHQ.getMHQOptions().getFontColorNegative() : null);
        group.claimedSalvageForKeeps.setSelected(false);
        group.claimedSalvageForKeeps.setEnabled(false);
        group.claimedSalvageForSale.setSelected(false);
        group.claimedSalvageForSale.setEnabled(false);
    }

    /**
     * Updates a row's carry/drag selector to match the recovery method the plan settled on.
     *
     * <p>The selector is only enabled when a single unit is assigned and it could either carry or drag the wreck.
     * Otherwise it shows the only available method, or nothing.</p>
     *
     * @param group the combo box group to update
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static void syncRecoveryMethodBox(SalvageComboBoxGroup group) {
        JComboBox<RecoveryMethod> recoveryMethodBox = group.recoveryMethodBox;
        if (recoveryMethodBox == null) {
            return;
        }

        // Update without re-triggering the listeners
        ActionListener[] listeners = recoveryMethodBox.getActionListeners();
        for (ActionListener listener : listeners) {
            recoveryMethodBox.removeActionListener(listener);
        }
        try {
            recoveryMethodBox.setSelectedItem(group.recovery.getRecoveryMethod());
            recoveryMethodBox.setEnabled(group.recovery.isRecoveryMethodChoosable());
        } finally {
            for (ActionListener listener : listeners) {
                recoveryMethodBox.addActionListener(listener);
            }
        }
    }

    /**
     * Updates all combo boxes so each only offers units that could legally be assigned there.
     *
     * @param salvageComboBoxGroups list of all combo box groups in the dialog
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateComboBoxOptions(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            updateSingleComboBox(group, group.comboBoxLeft, group.comboBoxRight);
            updateSingleComboBox(group, group.comboBoxRight, group.comboBoxLeft);
        }
    }

    /**
     * Updates a single combo box with available unit options.
     *
     * <p>Repopulates the combo box with the current selection, plus every unit that could legally be assigned there
     * (see {@link SalvageRecoveryPlan#isOffered(WreckRecovery, Unit, Unit)}). Preserves the current selection after
     * updating.</p>
     *
     * @param group         the group the combo box belongs to
     * @param comboBox      the combo box to update
     * @param otherComboBox the other combo box in the same group
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateSingleComboBox(SalvageComboBoxGroup group, JComboBox<String> comboBox,
          JComboBox<String> otherComboBox) {
        String currentSelection = (String) comboBox.getSelectedItem();
        Unit otherSlotUnit = getSelectedUnit(otherComboBox);

        // Temporarily remove all action listeners to prevent recursive calls
        ActionListener[] listeners = comboBox.getActionListeners();
        for (ActionListener listener : listeners) {
            comboBox.removeActionListener(listener);
        }

        try {
            comboBox.removeAllItems();
            comboBox.addItem(null); // Allow empty selection

            for (Map.Entry<String, Unit> entry : unitNameMap.entrySet()) {
                String unitName = entry.getKey();
                if (unitName.equals(currentSelection) ||
                          recoveryPlan.isOffered(group.recovery, entry.getValue(), otherSlotUnit)) {
                    comboBox.addItem(unitName);
                }
            }

            // Restore the selection
            comboBox.setSelectedItem(currentSelection);
        } finally {
            // Re-add all action listeners
            for (ActionListener listener : listeners) {
                comboBox.addActionListener(listener);
            }
        }
    }

    /**
     * Builds the prefix shown before a carrier's name in the drop-down boxes, showing how much room it has left.
     *
     * @param carrier the unit to describe
     *
     * @return the prefix, or {@code null} if the unit isn't carrying anything
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable String getRemainingCapacityPrefix(Unit carrier) {
        RemainingCapacity remainingCapacity = recoveryPlan.getRemainingCapacity(carrier);
        if (remainingCapacity == null) {
            return null;
        }

        if (remainingCapacity.isBayCapacity()) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.freeBays",
                  remainingCapacity.freeBays());
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.freeCargo",
              remainingCapacity.freeCargoTons());
    }

    private @Nullable Unit getSelectedUnit(JComboBox<String> comboBox) {
        String unitName = (String) comboBox.getSelectedItem();
        return (unitName == null) ? null : unitNameMap.get(unitName);
    }

    /**
     * This override forces the preferences for this class to be tracked in MekHQ instead of MegaMek.
     */
    private void setPreferences(JDialog dialog) {
        try {
            PreferencesNode preferences = MekHQ.getMHQPreferences().forClass(SalvagePostScenarioPicker.class);
            dialog.setName("SalvagePostScenarioPicker");
            preferences.manage(new JWindowPreference(dialog));
        } catch (Exception ex) {
            LOGGER.error("Failed to set user preferences", ex);
        }
    }

    private void moveToList(TestUnit u, List<TestUnit> dest) {
        keptSalvage.remove(u);
        soldSalvage.remove(u);
        employerSalvage.remove(u);
        if (!dest.contains(u)) {
            dest.add(u);
        }
    }

    private void removeFromAll(TestUnit u) {
        keptSalvage.remove(u);
        soldSalvage.remove(u);
        employerSalvage.remove(u);
    }

    private void syncMembershipForGroup(SalvageComboBoxGroup group, boolean isValid) {
        final TestUnit targetUnit = group.targetUnit;

        final boolean sale = group.claimedSalvageForSale.isSelected();
        final boolean keeps = group.claimedSalvageForKeeps.isSelected();

        if (!isValid) {
            // Invalid assignment -> remove from all 3 lists
            removeFromAll(targetUnit);
            return;
        }

        // Valid assignment
        if (sale) {
            moveToList(targetUnit, soldSalvage);
        } else if (keeps) {
            moveToList(targetUnit, keptSalvage);
        } else {
            // Neither selected: valid -> goes to employer
            moveToList(targetUnit, employerSalvage);
        }
    }

    private record ViewUnitListener(TestUnit unit) implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent evt) {
            showUnit(unit);
        }

        private void showUnit(TestUnit unit) {
            new EntityReadoutDialog(null, true, unit.getEntity()).setVisible(true);
        }
    }

    /**
     * Fixes the width of a combo box to prevent resizing when items are added or selected.
     *
     * <p>This method sets the preferred, minimum, and maximum sizes of the combo box to a fixed width while
     * preserving the component's preferred height. This prevents the combo box from resizing dynamically based on its
     * content, providing a consistent user interface.</p>
     *
     * @param combo the {@link JComboBox} to fix the width of
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void fixComboBoxWidth(JComboBox<?> combo) {
        Dimension dimension = scaleForGUI(250, combo.getPreferredSize().height);
        combo.setPreferredSize(dimension);
        combo.setMinimumSize(dimension);
        combo.setMaximumSize(dimension);
    }
}
