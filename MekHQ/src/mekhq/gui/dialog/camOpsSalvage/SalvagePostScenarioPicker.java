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
import java.awt.Color;
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

import megamek.client.ui.dialogs.unitSelectorDialogs.EntityReadoutDialog;
import megamek.client.ui.preferences.JWindowPreference;
import megamek.client.ui.preferences.PreferencesNode;
import megamek.common.annotations.Nullable;
import megamek.common.bays.ASFBay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.SmallCraft;
import megamek.common.util.sorter.NaturalOrderComparator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBScenario;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.mission.scenarios.camOpsSalvage.CamOpsSalvageUtilities;
import mekhq.campaign.mission.scenarios.camOpsSalvage.RecoveryTimeCalculations;
import mekhq.campaign.mission.scenarios.camOpsSalvage.RecoveryTimeData;
import mekhq.campaign.mission.scenarios.salvage.AbstractSalvage;
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
    /** Allowance for floating-point error when summing cargo tonnages. */
    private final static double CAPACITY_TOLERANCE = 0.0001;

    private final boolean isInSpace;
    private final AbstractSalvage salvageRules;
    private int maximumSalvageTime = 0;
    private int usedSalvageTime = 0;
    private int salvagePercent = 100;
    private double salvageRightsMultiplier = 1.0;
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
    private boolean isExchangeRights = false;

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
        boolean isUpdating = false;  // Flag to prevent recursive updates
        boolean isValid = false;
        /** What this wreck takes up in its carrier's shared cargo space or bays, or {@code null} if not shared. */
        @Nullable CarryLoad carryLoad = null;
        /** Lets the player choose to carry or drag the wreck, where both are possible; {@code null} if not offered. */
        @Nullable JComboBox<RecoveryMethod> recoveryMethodBox = null;
        /** The unit the current recovery method choice was made for, so the choice resets when the unit changes. */
        @Nullable Unit recoveryMethodUnit = null;

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
         * @param targetUnit             the salvage unit being assigned recovery forces
         *
         * @author Illiani
         * @since 0.50.10
         */
        SalvageComboBoxGroup(JButton unitButton, JComboBox<String> comboBoxLeft, JComboBox<String> comboBoxRight,
              JLabel validationLabel, JLabel unitLabel, JCheckBox claimedSalvageForKeeps,
              JCheckBox claimedSalvageForSale, TestUnit targetUnit) {
            this.unitButton = unitButton;
            this.comboBoxLeft = comboBoxLeft;
            this.comboBoxRight = comboBoxRight;
            this.validationLabel = validationLabel;
            this.unitLabel = unitLabel;
            this.claimedSalvageForKeeps = claimedSalvageForKeeps;
            this.claimedSalvageForSale = claimedSalvageForSale;
            this.targetUnit = targetUnit;
        }
    }

    /**
     * The share of a recovery unit's cargo space or bays taken up by a wreck it carries.
     *
     * <p>Only used where the salvage system lets a carrying unit recover several wrecks (see
     * {@link AbstractSalvage#isMultipleSalvagePerUnitAllowed()}).</p>
     *
     * @param carrier      the unit carrying the wreck
     * @param cargoTons    the cargo space the wreck takes up, in tons ({@code 0} if carried in a bay)
     * @param isBayLoad    {@code true} if the wreck is a fighter or small craft carried in a bay
     * @param isSmallCraft {@code true} if the wreck is a small craft, which needs a small craft bay
     *
     * @author Illiani
     * @since 0.51.01
     */
    private record CarryLoad(Unit carrier, double cargoTons, boolean isBayLoad, boolean isSmallCraft) {}

    /**
     * How a single recovery unit brings in a wreck on the ground, where it could do either.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private enum RecoveryMethod {
        /** Carried in the unit's cargo space, which can be shared with other wrecks. */
        CARRY,
        /** Dragged, which commits the unit to this wreck alone. */
        DRAG;

        @Override
        public String toString() {
            return getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.recoveryMethod." + name());
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
     * @param mission       the {@link AbstractContract} associated with the scenario
     * @param scenario      the {@link Scenario} that was just completed
     * @param actualSalvage the list of {@link TestUnit}s available as salvage that the player can claim
     * @param soldSalvage   the list of {@link TestUnit}s that are marked for immediate sale
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvagePostScenarioPicker(Campaign campaign, AbstractContract mission, Scenario scenario,
          List<TestUnit> actualSalvage, List<TestUnit> soldSalvage) {
        this.isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;
        this.salvageRules = campaign.getCampaignOptions().get(CampaignOption.SALVAGE_SYSTEM).getSalvage();

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

        salvageRightsMultiplier = mission.getSalvageRightsMultiplier();
        salvagePercent = (int) round(salvageRightsMultiplier * 100);
        employerSalvageMoneyInitial = mission.getSalvagedByEmployerValue();
        employerSalvageMoneyCurrent = employerSalvageMoneyInitial;
        unitSalvageMoneyInitial = mission.getSalvagedByUnitValue();
        unitSalvageMoneyCurrent = unitSalvageMoneyInitial;
        isExchangeRights = mission.isSalvageExchange();

        showSalvageDialog(campaign);

        // Process selected units
        CamOpsSalvageUtilities.resolveSalvage(campaign, mission, scenario, this.keptSalvage, this.soldSalvage,
              this.employerSalvage);
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
            boolean canCombatUnitsSalvage = salvageRules.isSalvageFormationCombatAllowed() &&
                                                  formation.getFormationType().isSalvage();
            for (Unit unit : formation.getAllUnitsAsUnits(hangar, false)) {
                boolean didFightInScenario = unit.getScenarioId() == scenario.getId();
                if (didFightInScenario && !canCombatUnitsSalvage) {
                    continue;
                }

                if (CamOpsSalvageUtilities.isAvailableForSalvage(unit, isInSpace, salvageRules)) {
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

        if (isExchangeRights) {
            salvagePercentLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent.exchange",
                  salvagePercent));
        } else {
            salvagePercentLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent.normal",
                  getCurrentPercentAsBigDecimal(),
                  salvagePercent));
        }
        employerSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "SalvagePostScenarioPicker.employerSalvage", employerSalvageMoneyCurrent.toAmountString()));
        if (isExchangeRights) {
            unitSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.unitSalvage", getExchangeUnitSalvage().toAmountString()));
        } else {
            unitSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.unitSalvage", unitSalvageMoneyCurrent.toAmountString()));
        }
        availableTimeLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
              "SalvagePostScenarioPicker.time", usedSalvageTime, maximumSalvageTime));

        infoPanel.add(salvagePercentLabel);
        infoPanel.add(employerSalvageLabel);
        infoPanel.add(unitSalvageLabel);
        infoPanel.add(availableTimeLabel);

        // Right column (tutorial text)
        JEditorPane tutorialPane = new JEditorPane();
        tutorialPane.setContentType("text/html");
        tutorialPane.setEditable(false);
        tutorialPane.setOpaque(false);
        String tutorialText = getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial");
        if (salvageRules.isMultipleSalvagePerUnitAllowed()) {
            tutorialText += getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial.sharedCapacity");
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

            JComboBox<String> comboBox1 = new JComboBox<>();
            fixComboBoxWidth(comboBox1);
            comboBox1.addItem(null); // Allow empty selection
            JComboBox<String> comboBox2 = new JComboBox<>();
            fixComboBoxWidth(comboBox2);
            comboBox2.addItem(null); // Allow empty selection

            // Carriers can take on several wrecks, so show how much room each has left
            if (salvageRules.isMultipleSalvagePerUnitAllowed()) {
                comboBox1.setRenderer(createRemainingCapacityRenderer(salvageComboBoxGroups));
                comboBox2.setRenderer(createRemainingCapacityRenderer(salvageComboBoxGroups));
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
                  unit);
            salvageComboBoxGroups.add(group);

            // Where a carrier can take on several wrecks, the player chooses whether a unit carries or drags. There's
            // no dragging in space.
            if (salvageRules.isMultipleSalvagePerUnitAllowed() && !isInSpace) {
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
            rowPanel.add(comboBox1);
            rowPanel.add(comboBox2);
            if (group.recoveryMethodBox != null) {
                rowPanel.add(group.recoveryMethodBox);
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

        // Initial button state check
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
     * @param salvageComboBoxGroups list of all combo box groups
     *
     * @return the renderer
     *
     * @author Illiani
     * @since 0.51.01
     */
    private ListCellRenderer<Object> createRemainingCapacityRenderer(
          List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        return new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                  boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String unitName) {
                    Unit unit = unitNameMap.get(unitName);
                    String prefix = (unit == null) ? null : getRemainingCapacityPrefix(salvageComboBoxGroups, unit);
                    if (prefix != null) {
                        setText(prefix + ' ' + unitName);
                    }
                }
                return this;
            }
        };
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
     *   <li>For contracts: the player's salvage percentage exceeds the contract limit</li>
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

        // Check for any invalid units
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            group.unitLabel.setForeground(null); // reset

            String unitName1 = (String) group.comboBoxLeft.getSelectedItem();
            String unitName2 = (String) group.comboBoxRight.getSelectedItem();

            // If units are assigned, check validation state
            if ((unitName1 != null || unitName2 != null) && !group.isValid) {
                disableConfirmAndColorName(confirmButton, group.unitLabel);
                shouldEnable = false;
            }
        }

        // Check salvage percentage if this is a contract
        unitSalvageLabel.setForeground(null);
        BigDecimal currentPercent = getCurrentPercentAsBigDecimal();
        if (currentPercent.compareTo(BigDecimal.valueOf(salvagePercent)) > 0 && !isExchangeRights) {
            disableConfirmAndColorName(confirmButton, unitSalvageLabel);
            // If we've gone over our %, we only block progression if the player is trying to salvage even more.
            shouldEnable &= unitSalvageMoneyCurrent.compareTo(unitSalvageMoneyInitial) <= 0;
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
        return unitSalvageMoneyInitial.plus(employerSalvageThisScenario.multipliedBy(salvageRightsMultiplier));
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
            String unitName1 = (String) group.comboBoxLeft.getSelectedItem();
            String unitName2 = (String) group.comboBoxRight.getSelectedItem();
            boolean hasAssignedUnits = (unitName1 != null) || (unitName2 != null);

            if (hasAssignedUnits) {
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

        // Update labels if they exist
        if (salvagePercentLabel != null && !isExchangeRights) {
            salvagePercentLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent.normal", getCurrentPercentAsBigDecimal(), salvagePercent));
        }
        if (employerSalvageLabel != null) {
            employerSalvageLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.employerSalvage", employerSalvageMoneyCurrent.toAmountString()));
        }
        if (unitSalvageLabel != null) {
            String label;
            if (isExchangeRights) {
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
     * Revalidates every salvage assignment, then applies the limits of any cargo space or bays shared between rows.
     *
     * <p>Each row is first validated on its own (see {@link #updateValidation(SalvageComboBoxGroup, Map)}). Where the
     * salvage system lets a carrying unit recover several wrecks, each row's share of its carrier's cargo space or
     * bays is then worked out, and rows that overload a carrier, or that use a unit committed elsewhere, are
     * invalidated. Finally, every row's salvage is moved to the matching kept, sold, or employer list.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void revalidateAllGroups(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            updateValidation(group, unitNameMap);
            syncRecoveryMethodBox(group);
            group.carryLoad = getSharedCarryLoad(group);
        }

        if (salvageRules.isMultipleSalvagePerUnitAllowed()) {
            applySharedCapacityLimits(salvageComboBoxGroups);
            labelRecoveryMethods(salvageComboBoxGroups);
        }

        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            syncMembershipForGroup(group, hasAssignedUnits(group) && group.isValid);
        }
    }

    /**
     * Works out a row's share of its carrier's cargo space or bays.
     *
     * <p>A wreck only takes a share when a single unit is assigned, and that unit carries it rather than dragging or
     * tugging it. Carried wrecks go in the carrier's cargo space, except fighters and small craft in space, which go
     * in its bays.</p>
     *
     * @param group the combo box group to check
     *
     * @return the wreck's share of its carrier's capacity, or {@code null} if it doesn't share its carrier
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable CarryLoad getSharedCarryLoad(SalvageComboBoxGroup group) {
        if (!salvageRules.isMultipleSalvagePerUnitAllowed() || !group.isValid) {
            return null;
        }

        Unit unitLeft = getSelectedUnit(group.comboBoxLeft);
        Unit unitRight = getSelectedUnit(group.comboBoxRight);
        if ((unitLeft == null) == (unitRight == null)) {
            return null; // Nothing assigned, or a two-unit team, which is committed to this wreck
        }

        Entity targetEntity = group.targetUnit.getEntity();
        if (targetEntity == null) {
            return null;
        }

        Unit carrier = (unitLeft != null) ? unitLeft : unitRight;
        if (isInSpace) {
            if (isLargeVessel(targetEntity)) {
                return null; // Tugged, which commits the carrier
            }
            if (isSmallVessel(targetEntity)) {
                return new CarryLoad(carrier, 0.0, true, isSmallCraft(targetEntity));
            }
        }

        double targetWeight = targetEntity.getWeight();
        boolean canCarry = getCargoCapacity(carrier) >= targetWeight;
        boolean isDragChosen = getChosenRecoveryMethod(group) == RecoveryMethod.DRAG;
        if (canCarry && !isDragChosen) {
            return new CarryLoad(carrier, targetWeight, false, false);
        }

        return null; // Dragged, which commits the carrier
    }

    /**
     * Updates a row's carry/drag selector to match its current assignment.
     *
     * <p>The selector is only enabled when a single unit is assigned and it could either carry or drag the wreck.
     * Otherwise it shows the only available method, or nothing. When the assigned unit changes, the choice resets to
     * carrying, as that leaves the unit free to take on more salvage.</p>
     *
     * @param group the combo box group to update
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void syncRecoveryMethodBox(SalvageComboBoxGroup group) {
        JComboBox<RecoveryMethod> recoveryMethodBox = group.recoveryMethodBox;
        if (recoveryMethodBox == null) {
            return;
        }

        List<Unit> selectedUnits = getSelectedUnits(group);
        Unit singleUnit = (selectedUnits.size() == 1) ? selectedUnits.getFirst() : null;
        Entity targetEntity = group.targetUnit.getEntity();

        boolean isChoosable = false;
        RecoveryMethod recoveryMethod = null;
        if ((singleUnit != null) && group.isValid && (targetEntity != null)) {
            double targetWeight = targetEntity.getWeight();
            boolean canCarry = getCargoCapacity(singleUnit) >= targetWeight;
            boolean canDrag = getTowCapacity(singleUnit) >= targetWeight;
            isChoosable = canCarry && canDrag;

            if (isChoosable) {
                boolean isSameUnit = singleUnit == group.recoveryMethodUnit;
                RecoveryMethod previousChoice = (RecoveryMethod) recoveryMethodBox.getSelectedItem();
                recoveryMethod = (isSameUnit && (previousChoice != null)) ? previousChoice : RecoveryMethod.CARRY;
            } else if (canCarry) {
                recoveryMethod = RecoveryMethod.CARRY;
            } else if (canDrag) {
                recoveryMethod = RecoveryMethod.DRAG;
            }
        }
        group.recoveryMethodUnit = singleUnit;

        // Update without re-triggering the listeners
        ActionListener[] listeners = recoveryMethodBox.getActionListeners();
        for (ActionListener listener : listeners) {
            recoveryMethodBox.removeActionListener(listener);
        }
        try {
            recoveryMethodBox.setSelectedItem(recoveryMethod);
            recoveryMethodBox.setEnabled(isChoosable);
        } finally {
            for (ActionListener listener : listeners) {
                recoveryMethodBox.addActionListener(listener);
            }
        }
    }

    private static @Nullable RecoveryMethod getChosenRecoveryMethod(SalvageComboBoxGroup group) {
        return (group.recoveryMethodBox == null) ? null : (RecoveryMethod) group.recoveryMethodBox.getSelectedItem();
    }

    /**
     * Invalidates rows that overload a carrier's cargo space or bays, or that use a unit committed elsewhere.
     *
     * <p>A unit either carries salvage (sharing its capacity between wrecks) or is committed to a single wreck,
     * dragging or tugging it alone or as part of a two-unit team. It can never do both.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void applySharedCapacityLimits(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        Color negativeColor = MekHQ.getMHQOptions().getFontColorNegative();
        String inUseLabel = getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.unitInUse");
        List<SalvageComboBoxGroup> invalidatedGroups = new ArrayList<>();

        // A committed unit can't be used anywhere else
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            if (!group.isValid || (group.carryLoad != null)) {
                continue;
            }
            for (Unit unit : getSelectedUnits(group)) {
                for (SalvageComboBoxGroup otherGroup : getGroupsUsing(salvageComboBoxGroups, unit, group)) {
                    invalidatedGroups.add(group);
                    invalidatedGroups.add(otherGroup);
                }
            }
        }

        // Carriers can't take on more than fits
        Map<Unit, List<SalvageComboBoxGroup>> groupsByCarrier = getGroupsByCarrier(salvageComboBoxGroups);
        for (Map.Entry<Unit, List<SalvageComboBoxGroup>> entry : groupsByCarrier.entrySet()) {
            Unit carrier = entry.getKey();
            List<SalvageComboBoxGroup> carriedGroups = entry.getValue();

            List<SalvageComboBoxGroup> cargoGroups = new ArrayList<>();
            List<SalvageComboBoxGroup> bayGroups = new ArrayList<>();
            double cargoTonsUsed = 0.0;
            int smallCraftCarried = 0;
            for (SalvageComboBoxGroup group : carriedGroups) {
                if (group.carryLoad.isBayLoad()) {
                    bayGroups.add(group);
                    if (group.carryLoad.isSmallCraft()) {
                        smallCraftCarried++;
                    }
                } else {
                    cargoGroups.add(group);
                    cargoTonsUsed += group.carryLoad.cargoTons();
                }
            }

            if (cargoTonsUsed > getCargoCapacity(carrier) + CAPACITY_TOLERANCE) {
                for (SalvageComboBoxGroup group : cargoGroups) {
                    invalidate(group, getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.cargoFull"),
                          negativeColor);
                    group.carryLoad = null;
                }
            }

            if (!hasBaysFor(carrier, bayGroups.size(), smallCraftCarried)) {
                for (SalvageComboBoxGroup group : bayGroups) {
                    invalidate(group, getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noFreeBay"),
                          negativeColor);
                    group.carryLoad = null;
                }
            }
        }

        for (SalvageComboBoxGroup group : invalidatedGroups) {
            invalidate(group, inUseLabel, negativeColor);
            group.carryLoad = null;
        }
    }

    /**
     * Labels each valid row with how its wreck is recovered: carried in cargo, carried in a bay, or dragged.
     *
     * @param salvageComboBoxGroups list of all combo box groups
     *
     * @author Illiani
     * @since 0.51.01
     */
    private void labelRecoveryMethods(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            if (!group.isValid) {
                continue;
            }

            String validationKey;
            if (group.carryLoad != null) {
                validationKey = group.carryLoad.isBayLoad() ?
                                      "SalvagePostScenarioPicker.validation.valid.bay" :
                                      "SalvagePostScenarioPicker.validation.valid.cargo";
            } else {
                validationKey = "SalvagePostScenarioPicker.validation.valid.committed";
            }
            group.validationLabel.setText(getTextAt(RESOURCE_BUNDLE, validationKey));
        }
    }

    /**
     * Groups the rows that share a carrier's capacity by their carrier.
     *
     * @param salvageComboBoxGroups list of all combo box groups
     *
     * @return the rows carried by each carrier
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static Map<Unit, List<SalvageComboBoxGroup>> getGroupsByCarrier(
          List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        Map<Unit, List<SalvageComboBoxGroup>> groupsByCarrier = new LinkedHashMap<>();
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            if (group.carryLoad != null) {
                groupsByCarrier.computeIfAbsent(group.carryLoad.carrier(), carrier -> new ArrayList<>()).add(group);
            }
        }
        return groupsByCarrier;
    }

    /**
     * Checks whether a carrier has enough suitable bays with working doors for the fighters and small craft assigned
     * to it. Fighters fit in fighter or small craft bays; small craft only fit in small craft bays.
     *
     * @param carrier           the carrying unit
     * @param vesselsCarried    the total number of fighters and small craft assigned to the carrier
     * @param smallCraftCarried how many of those are small craft
     *
     * @return {@code true} if every assigned fighter and small craft has a bay
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean hasBaysFor(Unit carrier, int vesselsCarried, int smallCraftCarried) {
        if (vesselsCarried == 0) {
            return true;
        }

        Entity carrierEntity = carrier.getEntity();
        if (carrierEntity == null) {
            return false;
        }

        int smallCraftBays = CamOpsSalvageUtilities.countBaysWithWorkingDoors(carrierEntity, SmallCraftBay.class);
        int fighterBays = CamOpsSalvageUtilities.countBaysWithWorkingDoors(carrierEntity, ASFBay.class);
        return (smallCraftCarried <= smallCraftBays) && (vesselsCarried <= smallCraftBays + fighterBays);
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
            updateSingleComboBox(salvageComboBoxGroups, group, group.comboBoxLeft, group.comboBoxRight);
            updateSingleComboBox(salvageComboBoxGroups, group, group.comboBoxRight, group.comboBoxLeft);
        }
    }

    /**
     * Updates a single combo box with available unit options.
     *
     * <p>Repopulates the combo box with the current selection, plus every unit that could legally be assigned there
     * (see {@link #isOfferedInComboBox(List, SalvageComboBoxGroup, JComboBox, String, Unit)}). Preserves the current
     * selection after updating.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     * @param group                 the group the combo box belongs to
     * @param comboBox              the combo box to update
     * @param otherComboBox         the other combo box in the same group
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateSingleComboBox(List<SalvageComboBoxGroup> salvageComboBoxGroups, SalvageComboBoxGroup group,
          JComboBox<String> comboBox, JComboBox<String> otherComboBox) {
        String currentSelection = (String) comboBox.getSelectedItem();

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
                          isOfferedInComboBox(salvageComboBoxGroups, group, otherComboBox, unitName,
                                entry.getValue())) {
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
     * Checks whether a unit could be assigned in a combo box.
     *
     * <p>A unit not assigned anywhere else is always offered. Where the salvage system lets a carrying unit recover
     * several wrecks, a unit that is only carrying salvage elsewhere is also offered, provided it would carry this
     * wreck alone and still has room for it.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups
     * @param group                 the group the combo box belongs to
     * @param otherComboBox         the other combo box in the same group
     * @param unitName              the unit's display name
     * @param unit                  the unit
     *
     * @return {@code true} if the unit should be offered
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean isOfferedInComboBox(List<SalvageComboBoxGroup> salvageComboBoxGroups, SalvageComboBoxGroup group,
          JComboBox<String> otherComboBox, String unitName, Unit unit) {
        if (unitName.equals(otherComboBox.getSelectedItem())) {
            return false; // The same unit can't fill both slots
        }

        List<SalvageComboBoxGroup> otherUses = getGroupsUsing(salvageComboBoxGroups, unit, group);
        if (otherUses.isEmpty()) {
            return true;
        }

        if (!salvageRules.isMultipleSalvagePerUnitAllowed() || (otherComboBox.getSelectedItem() != null)) {
            return false; // Two-unit teams are committed to a single wreck
        }

        for (SalvageComboBoxGroup otherUse : otherUses) {
            if (otherUse.carryLoad == null) {
                return false; // Committed elsewhere
            }
        }

        return canCarryAdditionally(unit, group.targetUnit, otherUses);
    }

    /**
     * Checks whether a carrier has room for another wreck on top of those it already carries.
     *
     * @param carrier        the carrying unit
     * @param target         the additional wreck
     * @param carriedGroups  the rows the carrier already carries
     *
     * @return {@code true} if the additional wreck would fit
     *
     * @author Illiani
     * @since 0.51.01
     */
    private boolean canCarryAdditionally(Unit carrier, TestUnit target, List<SalvageComboBoxGroup> carriedGroups) {
        Entity targetEntity = target.getEntity();
        if (targetEntity == null) {
            return false;
        }

        if (isInSpace && isLargeVessel(targetEntity)) {
            return false; // Tugging commits the carrier
        }

        if (isInSpace && isSmallVessel(targetEntity)) {
            int vesselsCarried = 1;
            int smallCraftCarried = isSmallCraft(targetEntity) ? 1 : 0;
            for (SalvageComboBoxGroup carriedGroup : carriedGroups) {
                if (carriedGroup.carryLoad.isBayLoad()) {
                    vesselsCarried++;
                    if (carriedGroup.carryLoad.isSmallCraft()) {
                        smallCraftCarried++;
                    }
                }
            }
            return hasBaysFor(carrier, vesselsCarried, smallCraftCarried);
        }

        double cargoTonsUsed = targetEntity.getWeight();
        for (SalvageComboBoxGroup carriedGroup : carriedGroups) {
            cargoTonsUsed += carriedGroup.carryLoad.cargoTons();
        }
        return cargoTonsUsed <= getCargoCapacity(carrier) + CAPACITY_TOLERANCE;
    }

    /**
     * Builds the prefix shown before a carrier's name in the drop-down boxes, showing how much room it has left.
     *
     * @param salvageComboBoxGroups list of all combo box groups
     * @param carrier               the unit to describe
     *
     * @return the prefix, or {@code null} if the unit isn't carrying anything
     *
     * @author Illiani
     * @since 0.51.01
     */
    private @Nullable String getRemainingCapacityPrefix(List<SalvageComboBoxGroup> salvageComboBoxGroups,
          Unit carrier) {
        double cargoTonsUsed = 0.0;
        int vesselsCarried = 0;
        boolean isCarrying = false;
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            if ((group.carryLoad != null) && (group.carryLoad.carrier() == carrier)) {
                isCarrying = true;
                if (group.carryLoad.isBayLoad()) {
                    vesselsCarried++;
                } else {
                    cargoTonsUsed += group.carryLoad.cargoTons();
                }
            }
        }

        if (!isCarrying) {
            return null;
        }

        if (vesselsCarried > 0) {
            Entity carrierEntity = carrier.getEntity();
            int bays = (carrierEntity == null) ? 0 :
                             CamOpsSalvageUtilities.countBaysWithWorkingDoors(carrierEntity, SmallCraftBay.class) +
                                   CamOpsSalvageUtilities.countBaysWithWorkingDoors(carrierEntity, ASFBay.class);
            return getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.freeBays",
                  Math.max(0, bays - vesselsCarried));
        }

        double freeCargoTons = Math.max(0.0, getCargoCapacity(carrier) - cargoTonsUsed);
        return getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.freeCargo", freeCargoTons);
    }

    /**
     * Finds the other rows that have a unit assigned.
     *
     * @param salvageComboBoxGroups list of all combo box groups
     * @param unit                  the unit to look for
     * @param excludedGroup         a row to ignore, usually the one being checked
     *
     * @return every other row with the unit assigned in either slot
     *
     * @author Illiani
     * @since 0.51.01
     */
    private List<SalvageComboBoxGroup> getGroupsUsing(List<SalvageComboBoxGroup> salvageComboBoxGroups, Unit unit,
          SalvageComboBoxGroup excludedGroup) {
        List<SalvageComboBoxGroup> groupsUsing = new ArrayList<>();
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            if ((group != excludedGroup) && getSelectedUnits(group).contains(unit)) {
                groupsUsing.add(group);
            }
        }
        return groupsUsing;
    }

    private List<Unit> getSelectedUnits(SalvageComboBoxGroup group) {
        List<Unit> selectedUnits = new ArrayList<>();
        Unit unitLeft = getSelectedUnit(group.comboBoxLeft);
        if (unitLeft != null) {
            selectedUnits.add(unitLeft);
        }
        Unit unitRight = getSelectedUnit(group.comboBoxRight);
        if (unitRight != null) {
            selectedUnits.add(unitRight);
        }
        return selectedUnits;
    }

    private @Nullable Unit getSelectedUnit(JComboBox<String> comboBox) {
        String unitName = (String) comboBox.getSelectedItem();
        return (unitName == null) ? null : unitNameMap.get(unitName);
    }

    private static boolean hasAssignedUnits(SalvageComboBoxGroup group) {
        return (group.comboBoxLeft.getSelectedItem() != null) || (group.comboBoxRight.getSelectedItem() != null);
    }

    /** Jumpship includes WarShips. */
    private static boolean isLargeVessel(Entity entity) {
        return entity instanceof Dropship || entity instanceof Jumpship;
    }

    /** Dropship extends SmallCraft, so large vessels must be excluded. */
    private static boolean isSmallVessel(Entity entity) {
        return !isLargeVessel(entity) && (entity instanceof SmallCraft || entity instanceof AeroSpaceFighter);
    }

    private static boolean isSmallCraft(Entity entity) {
        return !isLargeVessel(entity) && (entity instanceof SmallCraft);
    }

    /**
     * Validates the salvage assignment for a combo box group and updates its validation label.
     *
     * <p>Checks the following requirements:</p>
     * <ul>
     *   <li>Space salvage:
     *     <ul>
     *       <li>For large vessels (Dropship or Jumpship): at least one assigned unit must have a naval tug</li>
     *       <li>For small vessels (Small craft or Aerospace Fighters): at least one assigned unit must have a
     *       SC or ASF bay</li>
     *       <li>For anything else: at least one assigned unit's cargo capacity must meet or exceed the salvage
     *       unit's weight</li>
     *     </ul>
     *   </li>
     *   <li>Ground salvage:
     *     <ul>
     *       <li>Either: one assigned unit's cargo capacity must meet or exceed the salvage unit's weight</li>
     *       <li>Or: the combined tow capacity of the assigned units must meet or exceed the salvage unit's
     *       weight</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>Updates the validation label with the result and colors the unit label red if validation fails.</p>
     *
     * @param group       the combo box group to validate
     * @param unitNameMap mapping from display names to Unit objects
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateValidation(SalvageComboBoxGroup group, Map<String, Unit> unitNameMap) {
        String unitNameLeft = (String) group.comboBoxLeft.getSelectedItem();
        String unitNameRight = (String) group.comboBoxRight.getSelectedItem();

        Unit salvageUnitLeft = unitNameLeft != null ? unitNameMap.get(unitNameLeft) : null;
        Unit salvageUnitRight = unitNameRight != null ? unitNameMap.get(unitNameRight) : null;
        TestUnit targetUnit = group.targetUnit;

        // If no units selected, clear validation and reset color
        if (salvageUnitLeft == null && salvageUnitRight == null) {
            invalidate(group, "", null);
            return;
        }

        Entity targetEntity = targetUnit.getEntity();
        double targetWeight = 0.0;
        boolean isLargeVessel = false;
        boolean isSmallVessel = false;
        if (targetEntity != null) {
            targetWeight = targetEntity.getWeight();
            // Jumpship includes WarShips
            isLargeVessel = targetEntity instanceof Dropship || targetEntity instanceof Jumpship;
            isSmallVessel = targetEntity instanceof SmallCraft || targetEntity instanceof AeroSpaceFighter;
        }

        Entity unitLeftEntity = salvageUnitLeft != null ? salvageUnitLeft.getEntity() : null;
        Entity unitRightEntity = salvageUnitRight != null ? salvageUnitRight.getEntity() : null;
        if (isInSpace) {
            // Dropship extends SmallCraft, so the large vessel check must come first
            if (isLargeVessel) {
                if (!checkForNavalTug(group, unitLeftEntity, unitRightEntity)) {
                    return;
                }
            } else if (isSmallVessel) {
                if (!checkForVesselWithSuitableBayEquipment(group, unitLeftEntity, unitRightEntity)) {
                    return;
                }
            } else if (!isCargoCapacitySufficient(salvageUnitLeft, salvageUnitRight, targetWeight)) {
                invalidate(group,
                      getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noCapacity.cargo"),
                      MekHQ.getMHQOptions().getFontColorNegative());
                return;
            }
        } else if (!checkForGroundCapacity(group, salvageUnitLeft, salvageUnitRight, targetWeight)) {
            return;
        }

        validate(group);
    }

    private static double getCargoCapacity(@Nullable Unit unit) {
        return unit == null ? 0.0 : unit.getCargoCapacityForSalvage();
    }

    private static double getTowCapacity(@Nullable Unit unit) {
        return unit == null ? 0.0 : CamOpsSalvageUtilities.getTowCapacity(unit);
    }

    /**
     * Checks whether either assigned unit can carry the salvage in its cargo space. Cargo capacity is not combined, as
     * a single wreck can't be split between two units.
     *
     * @param unitLeft     the first assigned unit, or {@code null}
     * @param unitRight    the second assigned unit, or {@code null}
     * @param targetWeight the weight of the salvage
     *
     * @return {@code true} if either unit has enough cargo capacity
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean isCargoCapacitySufficient(@Nullable Unit unitLeft, @Nullable Unit unitRight,
          double targetWeight) {
        return Math.max(getCargoCapacity(unitLeft), getCargoCapacity(unitRight)) >= targetWeight;
    }

    /**
     * Checks whether the assigned units can recover the salvage during ground operations, invalidating the group if
     * they can't.
     *
     * <p>The salvage can be recovered if either unit can carry it in its cargo space, or if the combined tow
     * capacity of both units is enough to drag it.</p>
     *
     * @param group        the combo box group being validated
     * @param unitLeft     the first assigned unit, or {@code null}
     * @param unitRight    the second assigned unit, or {@code null}
     * @param targetWeight the weight of the salvage
     *
     * @return {@code true} if the salvage can be recovered
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static boolean checkForGroundCapacity(SalvageComboBoxGroup group, @Nullable Unit unitLeft,
          @Nullable Unit unitRight, double targetWeight) {
        if (isCargoCapacitySufficient(unitLeft, unitRight, targetWeight)) {
            return true;
        }

        double combinedTowCapacity = getTowCapacity(unitLeft) + getTowCapacity(unitRight);
        if (combinedTowCapacity >= targetWeight) {
            return true;
        }

        // Report whichever option came closest
        double bestCargoCapacity = Math.max(getCargoCapacity(unitLeft), getCargoCapacity(unitRight));
        String validationKey = combinedTowCapacity >= bestCargoCapacity ?
                                     "SalvagePostScenarioPicker.validation.noCapacity.tow" :
                                     "SalvagePostScenarioPicker.validation.noCapacity.cargo";
        invalidate(group, getTextAt(RESOURCE_BUNDLE, validationKey), MekHQ.getMHQOptions().getFontColorNegative());
        return false;
    }

    private static boolean checkForNavalTug(SalvageComboBoxGroup group, Entity unitLeftEntity, Entity unitRightEntity) {
        boolean hasNavalTugLeft = null != unitLeftEntity && CamOpsSalvageUtilities.hasNavalTug(unitLeftEntity);
        boolean hasNavalTugRight = null != unitRightEntity && CamOpsSalvageUtilities.hasNavalTug(unitRightEntity);
        boolean hasNavalTug = hasNavalTugLeft || hasNavalTugRight;

        if (!hasNavalTug) {
            invalidate(group, getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noTug"),
                  MekHQ.getMHQOptions().getFontColorNegative());
            return false;
        }

        return true;
    }

    private static boolean checkForVesselWithSuitableBayEquipment(SalvageComboBoxGroup group, Entity unitLeftEntity,
          Entity unitRightEntity) {
        boolean isSuitableVesselLeft = null != unitLeftEntity &&
                                             CamOpsSalvageUtilities.hasSuitableBayEquipment(unitLeftEntity);
        boolean isSuitableVesselRight = null != unitRightEntity &&
                                              CamOpsSalvageUtilities.hasSuitableBayEquipment(unitRightEntity);
        boolean isSuitableVessel = isSuitableVesselLeft || isSuitableVesselRight;

        if (!isSuitableVessel) {
            invalidate(group, getTextAt(RESOURCE_BUNDLE,
                        "SalvagePostScenarioPicker.validation.noVesselWithSuitableBayEquipment"),
                  MekHQ.getMHQOptions().getFontColorNegative());
            return false;
        }

        return true;
    }

    private void validate(SalvageComboBoxGroup group) {
        group.isValid = true;
        group.validationLabel.setText(getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.valid"));
        group.unitLabel.setForeground(null); // Reset to default color
        if (!isExchangeRights) { // For exchange rights we keep everything disabled
            group.claimedSalvageForKeeps.setEnabled(true);
            group.claimedSalvageForSale.setEnabled(true);
        }
    }

    private static void invalidate(SalvageComboBoxGroup group, String label, Color FontColorNegative) {
        group.isValid = false;
        group.validationLabel.setText(label);
        group.unitLabel.setForeground(FontColorNegative);
        group.claimedSalvageForKeeps.setSelected(false);
        group.claimedSalvageForKeeps.setEnabled(false);
        group.claimedSalvageForSale.setSelected(false);
        group.claimedSalvageForSale.setEnabled(false);
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
