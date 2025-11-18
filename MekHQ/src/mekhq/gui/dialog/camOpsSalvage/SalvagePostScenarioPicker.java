/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.units.Tank;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.enums.CampaignTransportType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.mission.camOpsSalvage.CamOpsSalvageUtilities;
import mekhq.campaign.mission.camOpsSalvage.RecoveryTimeCalculations;
import mekhq.campaign.mission.camOpsSalvage.RecoveryTimeData;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.ITransportAssignment;
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

    private final boolean isInSpace;
    private int maximumSalvageTime = 0;
    private int usedSalvageTime = 0;
    private int salvagePercent = 100;
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
     * Helper class to hold the result of the salvage dialog.
     *
     * <p>Used to work around Java's restrictions on generic arrays by providing a mutable container for the dialog
     * result that can be accessed from lambda expressions.</p>
     *
     * @author Illiani
     * @since 0.50.10
     */
    static class ResultHolder {
        List<SalvageComboBoxGroup> groups = null;
    }

    /**
     * Creates a new post-salvage picker dialog and processes the selected salvage.
     *
     * <p>This constructor displays a dialog allowing the player to select which salvage units to claim and which
     * salvage forces to assign to recover them. After the dialog is confirmed, it processes the selections and resolves
     * the salvage through the campaign.</p>
     *
     * <p>If the mission is a contract with 100% or 0% salvage rights, the dialog is skipped entirely and salvage is
     * automatically allocated.</p>
     *
     * @param campaign      the current {@link Campaign} in which the scenario took place
     * @param mission       the {@link Mission} associated with the scenario
     * @param scenario      the {@link Scenario} that was just completed
     * @param actualSalvage the list of {@link TestUnit}s available as salvage that the player can claim
     * @param soldSalvage   the list of {@link TestUnit}s that are marked for immediate sale
     *
     * @author Illiani
     * @since 0.50.10
     */
    public SalvagePostScenarioPicker(Campaign campaign, Mission mission, Scenario scenario,
          List<TestUnit> actualSalvage, List<TestUnit> soldSalvage) {
        this.isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;

        setAvailableTechTime(campaign, scenario);
        List<Integer> salvageForces = setSalvageUnits(campaign, scenario);
        sanitizeOtherScenarioAssignments(campaign.getActiveScenarios(), scenario.getSalvageTechs(), salvageForces);

        arrangeUnits(actualSalvage, soldSalvage);
        setRecoveryTimeDataMap(campaign, scenario);

        boolean isContract = mission instanceof Contract;
        boolean playerGetsNoSalvage = isContract && ((Contract) mission).getSalvagePct() <= 0;
        if (playerGetsNoSalvage) {
            return; // There isn't going to be anything to process
        }

        if (isContract) {
            salvagePercent = ((Contract) mission).getSalvagePct();
            employerSalvageMoneyInitial = ((Contract) mission).getSalvagedByEmployer();
            employerSalvageMoneyCurrent = employerSalvageMoneyInitial;
            unitSalvageMoneyInitial = ((Contract) mission).getSalvagedByUnit();
            unitSalvageMoneyCurrent = unitSalvageMoneyInitial;
            isExchangeRights = ((Contract) mission).isSalvageExchange();
        }

        List<SalvageComboBoxGroup> selectedGroups = showSalvageDialog(campaign, isContract);
        if (selectedGroups != null) {
            processSalvageAssignments(selectedGroups);
        }

        // Process selected units
        CamOpsSalvageUtilities.resolveSalvage(campaign, mission, scenario, this.keptSalvage, this.soldSalvage,
              this.employerSalvage);
    }

    /**
     * If the player follows a very convoluted stream of steps, it's possible for them to assign the same force or tech
     * to multiple salvage operations on the same day. This method ensures this doesn't happen by removing the force/s
     * (and tech/s) from any other scenarios they have been assigned to.
     *
     * @param activeScenarios a list of scenarios marked as 'current' (i.e., unresolved)
     * @param salvageTechs    a list of techs assigned to the salvage operation
     * @param salvageForces   a list of forces assigned to the salvage operation
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void sanitizeOtherScenarioAssignments(List<Scenario> activeScenarios, List<UUID> salvageTechs,
          List<Integer> salvageForces) {
        for (Scenario activeScenario : activeScenarios) {
            activeScenario.getSalvageForces().removeAll(salvageForces);
            activeScenario.getSalvageTechs().removeAll(salvageTechs);
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
                  entity.getRecoveryTime(), scenario, campaign.getLocation().getPlanet());
            recoveryTimeData.put(unit.getId(), data);
        }
    }

    /**
     * Populates the list of available salvage units from the scenario's assigned salvage forces.
     *
     * <p>Retrieves all forces assigned to salvage operations for this scenario and collects units from those forces
     * that are capable of salvaging in the current environment (ground or space).</p>
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
        List<Integer> salvageForces = new ArrayList<>();
        salvageUnits = new ArrayList<>();
        Hangar hangar = campaign.getHangar();
        for (Integer forceId : scenario.getSalvageForces()) {
            salvageForces.add(forceId);

            Force force = campaign.getForce(forceId);
            if (force == null) {
                LOGGER.error("Force {} not found in campaign", forceId);
                continue;
            }

            for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
                Entity entity = unit.getEntity();
                if (entity == null) {
                    continue;
                }

                if (entity instanceof Tank tank && tank.isTrailer()) {
                    ITransportAssignment transportAssignment = unit.getTransportAssignment(CampaignTransportType.TOW_TRANSPORT);
                    if (transportAssignment == null || !transportAssignment.hasTransport()) {
                        continue; // If nothing is towing the trailer, it can't reach the salvage operation
                    }
                }

                if (unit.isFullyCrewed() && unit.canSalvage(isInSpace)) {
                    salvageUnits.add(unit);
                }
            }
        }

        return salvageForces;
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
            Person tech = campaign.getPerson(techId);
            if (tech == null) {
                LOGGER.error("Salvage tech {} not found in campaign", techId);
                continue;
            }

            // I don't expect we'll have negative tech minutes, but you never know
            maximumSalvageTime += Math.max(0, tech.getMinutesLeft());
        }
    }

    /**
     * Processes salvage assignments after the dialog is confirmed.
     *
     * <p>Reviews all combo box groups to determine which salvage units had recovery forces assigned. Units without
     * assigned recovery forces are moved from the player's salvage lists to the employer's salvage list.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups from the dialog
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void processSalvageAssignments(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        List<TestUnit> unitsToMoveToEmployer = new ArrayList<>();

        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            String unitName1 = (String) group.comboBoxLeft.getSelectedItem();
            String unitName2 = (String) group.comboBoxRight.getSelectedItem();

            // Check if player has claimed this salvage
            boolean hasClaimed = group.claimedSalvageForKeeps.isSelected() ||
                                       group.claimedSalvageForSale.isSelected();

            // If no units are assigned to salvage this unit AND player hasn't claimed it
            if (unitName1 == null && unitName2 == null && !hasClaimed) {
                unitsToMoveToEmployer.add(group.targetUnit);
            }
        }

        JCheckBox claimedSalvageForKeeps = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
              "SalvagePostScenarioPicker.unitLabel.salvage"));
        claimedSalvageForKeeps.setEnabled(false);
        JCheckBox claimedSalvageForSale = new JCheckBox(getTextAt(RESOURCE_BUNDLE,
              "SalvagePostScenarioPicker.unitLabel.sale"));
        claimedSalvageForSale.setEnabled(false);

        // Move units from actualSalvage to employerSalvage
        for (TestUnit unit : unitsToMoveToEmployer) {
            if (keptSalvage.contains(unit)) {
                keptSalvage.remove(unit);
                employerSalvage.add(unit);
            }
        }

        // Move units from soldSalvage to employerSalvage
        for (TestUnit unit : unitsToMoveToEmployer) {
            if (soldSalvage.contains(unit)) {
                soldSalvage.remove(unit);
                employerSalvage.add(unit);
            }
        }
    }

    /**
     * Displays the salvage selection dialog and returns the user's selections.
     *
     * <p>Creates and displays a modal dialog showing all available salvage with combo boxes to assign recovery units.
     * For contract missions, also displays salvage percentage information and enforces salvage limits. The dialog
     * validates all assignments and prevents confirmation if any assignments are invalid.</p>
     *
     * @param campaign   the current campaign
     * @param isContract whether this is a contract mission (affects displayed information and validation)
     *
     * @return list of combo box groups with user selections, or null if the dialog was canceled
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<SalvageComboBoxGroup> showSalvageDialog(Campaign campaign, boolean isContract) {
        JDialog dialog = new JDialog((Frame) null, getText("accessingTerminal.title"), true);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // We don't want the player to cancel out

        // Info panel at the top (only for contracts)
        JLabel employerSalvageLabel = null;
        JLabel unitSalvageLabel = null;
        JLabel availableTimeLabel = null;

        if (isContract) {
            JPanel infoContainer = new JPanel(new BorderLayout());
            infoContainer.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

            // Left column (existing info labels)
            JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));

            JLabel salvagePercentLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.salvagePercent", salvagePercent));
            employerSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.employerSalvage", employerSalvageMoneyCurrent.toAmountString()));
            if (isExchangeRights) {
                Money actualFunds = employerSalvageMoneyCurrent.dividedBy(100).multipliedBy(salvagePercent);
                unitSalvageLabel = new JLabel(getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.unitSalvage", actualFunds.toAmountString()));
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
            tutorialPane.setText(getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.tutorial"));
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
        }

        // Final references for use in lambdas
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

        final boolean[] confirmed = { false };
        final ResultHolder resultHolder = new ResultHolder();

        // Build the mapping and populate salvage unit options ONCE, outside the loop
        unitNameMap.clear();
        for (Unit salvageUnit : salvageUnits) {
            String base = CamOpsSalvageUtilities.getSalvageTooltip(List.of(salvageUnit), false);
            String key = base;
            int duplicate = 2;
            while (unitNameMap.containsKey(key)) {
                key = base + " [" + duplicate++ + "]";
            }
            unitNameMap.put(key, salvageUnit);
        }

        // We sort alphabetically for ease of use
        List<String> names = new ArrayList<>(unitNameMap.keySet());
        names.sort(String.CASE_INSENSITIVE_ORDER);

        // Add all units to single column
        for (TestUnit unit : allUnits) {
            String unitName = unit.getName();
            Money sellValue = unit.getSellValue();

            // Create row panel with label, two combo boxes, and validation label
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, scaleForGUI(60)));

            JLabel unitLabel = new JLabel();
            unitLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.unitLabel.unit",
                  unitName, sellValue.toAmountString()));

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
            JComboBox<String> comboBox2 = new JComboBox<>();
            comboBox1.addItem(null); // Allow empty selection
            comboBox2.addItem(null); // Allow empty selection

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

            // These need to be after the above lines, as we're going to use 'group' in the listeners.
            comboBox1.addActionListener(e -> performComboChangeAction(isContract, salvageComboBoxGroups,
                  group, finalEmployerSalvageLabel, finalUnitSalvageLabel, finalAvailableTimeLabel, confirmButton));
            comboBox2.addActionListener(e -> performComboChangeAction(isContract, salvageComboBoxGroups,
                  group, finalEmployerSalvageLabel, finalUnitSalvageLabel, finalAvailableTimeLabel, confirmButton));
            claimedSalvageForKeeps.addActionListener(e -> performComboChangeAction(isContract,
                  salvageComboBoxGroups, group, finalEmployerSalvageLabel, finalUnitSalvageLabel,
                  finalAvailableTimeLabel, confirmButton));
            claimedSalvageForSale.addActionListener(e -> performComboChangeAction(isContract,
                  salvageComboBoxGroups, group, finalEmployerSalvageLabel, finalUnitSalvageLabel,
                  finalAvailableTimeLabel, confirmButton));
            viewButton.addActionListener(new ViewUnitListener(group.targetUnit));

            fieldStripButton.addActionListener(e -> fieldStrip(group));

            rowPanel.add(viewButton);
            rowPanel.add(fieldStripButton);
            rowPanel.add(claimedSalvageForKeeps);
            rowPanel.add(claimedSalvageForSale);
            rowPanel.add(unitLabel);
            rowPanel.add(comboBox1);
            rowPanel.add(comboBox2);
            rowPanel.add(validationLabel);

            column.add(rowPanel);
        }

        JScrollPane scrollPane = new JScrollPane(column);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);

        confirmButton.addActionListener(e -> confirmationAction(campaign,
              dialog,
              confirmed,
              resultHolder,
              salvageComboBoxGroups));

        buttonPanel.add(confirmButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Initial button state check
        updateConfirmButtonState(salvageComboBoxGroups, confirmButton, finalUnitSalvageLabel, finalAvailableTimeLabel,
              isContract);

        dialog.setPreferredSize(DEFAULT_SIZE);
        dialog.setSize(DEFAULT_SIZE);
        dialog.setLocationRelativeTo(null);
        setPreferences(dialog); // Must be before setVisible
        dialog.setVisible(true);

        return confirmed[0] ? resultHolder.groups : null;
    }

    private static void confirmationAction(Campaign campaign, JDialog dialog, boolean[] confirmed,
          ResultHolder resultHolder,
          List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        dialog.setVisible(false);
        ImmersiveDialogSimple confirmationDialog = new ImmersiveDialogSimple(campaign, null, null,
              getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.confirmation.text"),
              List.of(getText("Cancel.text"), getText("Confirm.text")), null, null, false);
        if (confirmationDialog.getDialogChoice() == 0) { // Cancelled
            dialog.setVisible(true);
            return;
        }

        confirmed[0] = true;
        resultHolder.groups = salvageComboBoxGroups;
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
     *   <li>Updating available options in all combo boxes to prevent duplicate assignments</li>
     *   <li>Validating the changed assignment</li>
     *   <li>Recalculating salvage allocation and time usage</li>
     *   <li>Updating the confirm button state</li>
     * </ul>
     *
     * @param isContract                whether this is a contract mission
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
    private void performComboChangeAction(boolean isContract, List<SalvageComboBoxGroup> salvageComboBoxGroups,
          SalvageComboBoxGroup group, JLabel finalEmployerSalvageLabel, JLabel finalUnitSalvageLabel,
          JLabel finalAvailableTimeLabel, JButton confirmButton) {
        // Prevent recursive calls
        if (group.isUpdating) {
            return;
        }

        try {
            group.isUpdating = true;
            updateComboBoxOptions(salvageComboBoxGroups, unitNameMap);
            updateValidation(group, unitNameMap);

            String unitName1 = (String) group.comboBoxLeft.getSelectedItem();
            String unitName2 = (String) group.comboBoxRight.getSelectedItem();
            boolean hasAssignedUnits = (unitName1 != null) || (unitName2 != null);
            boolean isValid = hasAssignedUnits && isValidationValid(group);
            syncMembershipForGroup(group, isValid);

            updateSalvageAllocation(salvageComboBoxGroups,
                  finalEmployerSalvageLabel,
                  finalUnitSalvageLabel,
                  finalAvailableTimeLabel);
            updateConfirmButtonState(salvageComboBoxGroups, confirmButton, finalUnitSalvageLabel,
                  finalAvailableTimeLabel, isContract);
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
     * @param isContract            whether this is a contract mission
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateConfirmButtonState(List<SalvageComboBoxGroup> salvageComboBoxGroups, JButton confirmButton,
          JLabel unitSalvageLabel, JLabel salvageTimeLabel, boolean isContract) {
        boolean shouldEnable = true;

        // Check for any invalid units
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            group.unitLabel.setForeground(null); // reset

            String unitName1 = (String) group.comboBoxLeft.getSelectedItem();
            String unitName2 = (String) group.comboBoxRight.getSelectedItem();

            // If units are assigned, check validation state
            if ((unitName1 != null || unitName2 != null) && !isValidationValid(group)) {
                disableConfirmAndColorName(confirmButton, group.unitLabel);
                shouldEnable = false;
            }
        }

        // Check salvage percentage if this is a contract
        if (isContract) {
            unitSalvageLabel.setForeground(null);
            Money totalSalvage = employerSalvageMoneyCurrent.plus(unitSalvageMoneyCurrent);

            if (totalSalvage.isPositive()) {
                // Calculate percentage: (unitSalvage / totalSalvage) * 100
                BigDecimal hundred = BigDecimal.valueOf(100);
                BigDecimal currentPercent = unitSalvageMoneyCurrent.getAmount()
                                                  .multiply(hundred)
                                                  .divide(totalSalvage.getAmount(), 4, RoundingMode.HALF_UP);

                if (currentPercent.compareTo(BigDecimal.valueOf(salvagePercent)) > 0 && !isExchangeRights) {
                    disableConfirmAndColorName(confirmButton, unitSalvageLabel);

                    // If we've gone over our %, we only block progression if the player is trying to salvage even more.
                    shouldEnable = !unitSalvageMoneyCurrent.equals(unitSalvageMoneyInitial);
                }
            }
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

    private static void disableConfirmAndColorName(JButton confirmButton, JLabel unitSalvageLabel) {
        if (unitSalvageLabel != null) {
            unitSalvageLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
        }
        confirmButton.setEnabled(false);
    }

    /**
     * Checks if a combo box group has valid salvage assignments.
     *
     * @param group the combo box group to check
     *
     * @return {@code true} if the group's validation is empty (no units assigned) or shows "Valid"
     *
     * @author Illiani
     * @since 0.50.10
     */
    private boolean isValidationValid(SalvageComboBoxGroup group) {
        String validationText = group.validationLabel.getText();
        return validationText.isEmpty() ||
                     validationText.equals(getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.valid"));
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
     * @param employerSalvageLabel  label showing employer salvage value (can be null)
     * @param unitSalvageLabel      label showing unit salvage value (can be null)
     * @param availableTimeLabel    label showing time usage (can be null)
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateSalvageAllocation(List<SalvageComboBoxGroup> salvageComboBoxGroups,
          JLabel employerSalvageLabel, JLabel unitSalvageLabel, JLabel availableTimeLabel) {
        usedSalvageTime = 0;
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            String unitName1 = (String) group.comboBoxLeft.getSelectedItem();
            String unitName2 = (String) group.comboBoxRight.getSelectedItem();
            boolean hasAssignedUnits = (unitName1 != null) || (unitName2 != null);

            if (hasAssignedUnits) {
                RecoveryTimeData timeData = recoveryTimeData.get(group.targetUnit.getId());
                if (timeData != null) {
                    usedSalvageTime += timeData.totalRecoveryTime();
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
        if (employerSalvageLabel != null) {
            employerSalvageLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE,
                  "SalvagePostScenarioPicker.employerSalvage", employerSalvageMoneyCurrent.toAmountString()));
        }
        if (unitSalvageLabel != null) {
            String label;
            if (isExchangeRights) {
                Money actualFunds = employerSalvageMoneyCurrent.dividedBy(100).multipliedBy(salvagePercent);
                label = getFormattedTextAt(RESOURCE_BUNDLE,
                      "SalvagePostScenarioPicker.unitSalvage", actualFunds.toAmountString());
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
     * Updates all combo boxes to ensure each salvage unit can only be selected once.
     *
     * <p>Collects all currently selected units across all combo boxes, then updates each combo box to only show
     * units that are either not selected elsewhere or are the current selection in that specific combo box.</p>
     *
     * @param salvageComboBoxGroups list of all combo box groups in the dialog
     * @param unitNameMap           mapping from display names to Unit objects
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateComboBoxOptions(List<SalvageComboBoxGroup> salvageComboBoxGroups,
          Map<String, Unit> unitNameMap) {
        // Collect all currently selected unit names
        List<String> selectedUnitNames = getSelectedUnitNames(salvageComboBoxGroups);

        // Update each combo box
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            updateSingleComboBox(group.comboBoxLeft, selectedUnitNames, unitNameMap);
            updateSingleComboBox(group.comboBoxRight, selectedUnitNames, unitNameMap);
        }
    }

    private static List<String> getSelectedUnitNames(List<SalvageComboBoxGroup> salvageComboBoxGroups) {
        List<String> selectedUnitNames = new ArrayList<>();
        for (SalvageComboBoxGroup group : salvageComboBoxGroups) {
            String selectedLeft = (String) group.comboBoxLeft.getSelectedItem();
            String selectedRight = (String) group.comboBoxRight.getSelectedItem();
            if (selectedLeft != null) {
                selectedUnitNames.add(selectedLeft);
            }
            if (selectedRight != null) {
                selectedUnitNames.add(selectedRight);
            }
        }
        return selectedUnitNames;
    }

    /**
     * Updates a single combo box with available unit options.
     *
     * <p>Repopulates the combo box with units that are either not selected in other combo boxes or are the current
     * selection in this combo box. Preserves the current selection after updating.</p>
     *
     * @param comboBox          the combo box to update
     * @param selectedUnitNames list of unit names currently selected across all combo boxes
     * @param unitNameMap       mapping from display names to Unit objects
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void updateSingleComboBox(JComboBox<String> comboBox, List<String> selectedUnitNames,
          Map<String, Unit> unitNameMap) {
        String currentSelection = (String) comboBox.getSelectedItem();

        // Temporarily remove all action listeners to prevent recursive calls
        ActionListener[] listeners = comboBox.getActionListeners();
        for (ActionListener listener : listeners) {
            comboBox.removeActionListener(listener);
        }

        try {
            comboBox.removeAllItems();
            comboBox.addItem(null); // Allow empty selection

            for (String unitName : unitNameMap.keySet()) {
                // Only add if it's the current selection OR it's not selected anywhere else
                if (unitName.equals(currentSelection) || !selectedUnitNames.contains(unitName)) {
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
     * Validates the salvage assignment for a combo box group and updates its validation label.
     *
     * <p>Checks the following requirements:</p>
     * <ul>
     *   <li>For large vessels in space: at least one assigned unit must have a naval tug</li>
     *   <li>Either: one assigned unit's cargo capacity must meet or exceed the salvage unit's weight</li>
     *   <li>Or: the combined weight of both assigned units must meet or exceed the salvage unit's weight</li>
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
        if (targetEntity != null) {
            targetWeight = targetEntity.getWeight();
            // Jumpship includes WarShips
            isLargeVessel = targetEntity instanceof Dropship || targetEntity instanceof Jumpship;
        }

        // Check for naval tug requirement
        Entity unitLeftEntity = salvageUnitLeft != null ? salvageUnitLeft.getEntity() : null;
        Entity unitRightEntity = salvageUnitRight != null ? salvageUnitRight.getEntity() : null;
        if (isInSpace && isLargeVessel) {
            if (!checkForNavalTug(group, unitLeftEntity, unitRightEntity)) {
                return;
            }
        }

        boolean isTwoUnitsSelected = salvageUnitLeft != null && salvageUnitRight != null;

        // If two units selected, must use towing
        if (isTwoUnitsSelected) {
            double weightLeft = getTowCapacity(unitLeftEntity);
            double weightRight = getTowCapacity(unitRightEntity);

            boolean hasTowageCapacity = (weightLeft + weightRight) >= targetWeight;
            if (!hasTowageCapacity) {
                invalidate(group,
                      getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noCapacity.tow"),
                      MekHQ.getMHQOptions().getFontColorNegative());
                return;
            }
        } else if (salvageUnitLeft != null) {
            if (!useTowageOrCargo(group, unitLeftEntity, salvageUnitLeft, targetWeight)) {
                return;
            }
        } else {
            if (!useTowageOrCargo(group, unitRightEntity, salvageUnitRight, targetWeight)) {
                return;
            }
        }

        validate(group);
    }

    private static double getTowCapacity(Entity selectedEntity, Unit selectedUnit) {
        if (selectedEntity == null) {
            return 0.0;
        } else if (selectedEntity instanceof Tank tank && tank.isTrailer()) {
            return 0.0;
        } else {
            double currentTowWeight = selectedUnit.getTotalWeightOfUnitsAssignedToBeTransported(
                  CampaignTransportType.TOW_TRANSPORT,
                  mekhq.campaign.unit.TransporterType.TANK_TRAILER_HITCH);
            return Math.max(0.0, selectedEntity.getWeight() - currentTowWeight);
        }
    }

    private boolean useTowageOrCargo(SalvageComboBoxGroup group, Entity entity, Unit unit, double targetWeight) {
        double unitWeight = entity.getWeight();
        double cargoCapacity = unit.getCargoCapacityForSalvage();
        boolean useTowage = entity.getWeight() >= cargoCapacity;
        if (useTowage) {
            boolean hasTowageCapacity = unitWeight >= targetWeight;
            if (!hasTowageCapacity) {
                invalidate(group,
                      getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noCapacity.tow"),
                      MekHQ.getMHQOptions().getFontColorNegative());
                return false;
            }
        } else {
            boolean hasCargoCapacity = cargoCapacity >= targetWeight;
            if (!hasCargoCapacity) {
                invalidate(group,
                      getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noCapacity.cargo"),
                      MekHQ.getMHQOptions().getFontColorNegative());
                return false;
            }
        }
        return true;
    }

    private static boolean checkForNavalTug(SalvageComboBoxGroup group, Entity unitLeftEntity, Entity unitRightEntity) {
        boolean hasNavalTugLeft = unitLeftEntity == null || CamOpsSalvageUtilities.hasNavalTug(unitLeftEntity);
        boolean hasNavalTugRight = unitRightEntity == null || CamOpsSalvageUtilities.hasNavalTug(unitRightEntity);
        boolean hasNavalTug = hasNavalTugLeft && hasNavalTugRight;

        if (!hasNavalTug) {
            invalidate(group, getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.noTug"),
                  MekHQ.getMHQOptions().getFontColorNegative());
            return false;
        }

        return true;
    }

    private void validate(SalvageComboBoxGroup group) {
        group.validationLabel.setText(getTextAt(RESOURCE_BUNDLE, "SalvagePostScenarioPicker.validation.valid"));
        group.unitLabel.setForeground(null); // Reset to default color
        if (!isExchangeRights) { // For exchange rights we keep everything disabled
            group.claimedSalvageForKeeps.setEnabled(true);
            group.claimedSalvageForSale.setEnabled(true);
        }
    }

    private static void invalidate(SalvageComboBoxGroup group, String label, Color FontColorNegative) {
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

        // Enforce mutual exclusivity (don’t allow both)
        if (group.claimedSalvageForSale.isSelected() && group.claimedSalvageForKeeps.isSelected()) {
            // Prefer the most recently toggled? We can keep simple: if sale is checked, uncheck keeps.
            group.claimedSalvageForKeeps.setSelected(false);
        }

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
}
