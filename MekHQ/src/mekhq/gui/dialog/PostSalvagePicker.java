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
package mekhq.gui.dialog;

import static megamek.client.ui.WrapLayout.wordWrap;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getText;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
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
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

public class PostSalvagePicker {
    private static final MMLogger LOGGER = MMLogger.create(PostSalvagePicker.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    private final static int PADDING = scaleForGUI(10);

    private final boolean isInSpace;
    private int maximumSalvageTime = 0;
    private int usedSalvageTime = 0;
    private int salvagePercent = 100;
    private Money employerSalvageMoney = Money.zero();
    private Money unitSalvageMoney = Money.zero();
    private List<Unit> salvageUnits;
    private List<TestUnit> allUnits;
    private final List<TestUnit> actualSalvage;
    private final List<TestUnit> soldSalvage;
    private final List<TestUnit> employerSalvage = new ArrayList<>();
    private final Map<String, Unit> unitNameMap = new HashMap<>();
    private Map<UUID, RecoveryTimeData> recoveryTimeData;

    public PostSalvagePicker(Campaign campaign, Mission mission, Scenario scenario, List<TestUnit> actualSalvage,
          List<TestUnit> soldSalvage) {
        this.actualSalvage = actualSalvage;
        this.soldSalvage = soldSalvage;
        this.isInSpace = scenario.getBoardType() == AtBScenario.T_SPACE;

        setSalvageUnits(campaign, scenario);
        setAvailableTechTime(campaign, scenario);
        arrangeUnits();
        setRecoveryTimeDataMap(campaign, scenario);

        // If the player has 100% (or 0%) salvage, skip the salvage selection dialog entirely.
        boolean playerGetsAllSalvage = mission instanceof Contract contract && contract.getSalvagePct() >= 100;
        boolean playerGetsNoSalvage = mission instanceof Contract contract && contract.getSalvagePct() <= 0;

        boolean isContract = mission instanceof Contract;
        if (!playerGetsAllSalvage && !playerGetsNoSalvage) {
            if (isContract) {
                salvagePercent = ((Contract) mission).getSalvagePct();
                employerSalvageMoney = ((Contract) mission).getSalvagedByEmployer();
                unitSalvageMoney = ((Contract) mission).getSalvagedByUnit();
            }

            List<ComboBoxGroup> selectedGroups = showSalvageDialog(isContract);
            if (selectedGroups != null) {
                processSalvageAssignments(selectedGroups);
            }
        } else if (playerGetsNoSalvage) {
            actualSalvage.clear();
            soldSalvage.clear();
            allUnits.clear();
        }

        // Process selected units
        CamOpsSalvageUtilities.resolveSalvage(campaign, mission, scenario, actualSalvage, soldSalvage,
              employerSalvage);
    }

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

    private void setSalvageUnits(Campaign campaign, Scenario scenario) {
        salvageUnits = new ArrayList<>();
        Hangar hangar = campaign.getHangar();
        for (Integer forceId : scenario.getSalvageForces()) {
            Force force = campaign.getForce(forceId);
            if (force == null) {
                LOGGER.error("Force {} not found in campaign", forceId);
                continue;
            }

            for (Unit unit : force.getAllUnitsAsUnits(hangar, false)) {
                if (unit.canSalvage(isInSpace)) {
                    salvageUnits.add(unit);
                }
            }
        }
    }

    private void arrangeUnits() {
        allUnits = new ArrayList<>(actualSalvage);
        allUnits.addAll(soldSalvage);
        allUnits.sort(Comparator.comparing(TestUnit::getSellValue).reversed()); // Highest -> Lowest
    }

    private void setAvailableTechTime(Campaign campaign, Scenario scenario) {
        List<UUID> assignedTechIds = scenario.getSalvageTechs();
        for (UUID techId : assignedTechIds) {
            Person tech = campaign.getPerson(techId);
            if (tech == null) {
                LOGGER.error("Salvage tech {} not found in campaign", techId);
                continue;
            }

            maximumSalvageTime += tech.getMinutesLeft();
        }
    }

    private void processSalvageAssignments(List<ComboBoxGroup> comboBoxGroups) {
        List<TestUnit> unitsToMoveToEmployer = new ArrayList<>();

        for (ComboBoxGroup group : comboBoxGroups) {
            String unitName1 = (String) group.comboBox1.getSelectedItem();
            String unitName2 = (String) group.comboBox2.getSelectedItem();

            // If no units are assigned to salvage this unit
            if (unitName1 == null && unitName2 == null) {
                unitsToMoveToEmployer.add(group.targetUnit);
            }
        }

        // Move units from actualSalvage to employerSalvage
        for (TestUnit unit : unitsToMoveToEmployer) {
            if (actualSalvage.contains(unit)) {
                actualSalvage.remove(unit);
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

    private List<ComboBoxGroup> showSalvageDialog(boolean isContract) {
        JDialog dialog = new JDialog((Frame) null, getText("accessingTerminal.title"), true);
        dialog.setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE); // We don't want the player to cancel out

        // Info panel at the top (only for contracts)
        JLabel employerSalvageLabel = null;
        JLabel unitSalvageLabel = null;
        JLabel availableTimeLabel = null;

        if (isContract) {
            JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

            JLabel salvagePercentLabel = new JLabel("Salvage Percent: " + salvagePercent + "%");
            employerSalvageLabel = new JLabel("Employer Salvage: " +
                                                    employerSalvageMoney.toAmountString() +
                                                    " C-Bills");
            unitSalvageLabel = new JLabel("Unit Salvage: " + unitSalvageMoney.toAmountString() + " C-Bills");
            availableTimeLabel = new JLabel("Available Time: " +
                                                  usedSalvageTime +
                                                  " / " +
                                                  maximumSalvageTime +
                                                  " minutes");

            infoPanel.add(salvagePercentLabel);
            infoPanel.add(employerSalvageLabel);
            infoPanel.add(unitSalvageLabel);
            infoPanel.add(availableTimeLabel);

            dialog.add(infoPanel, BorderLayout.NORTH);
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
        List<ComboBoxGroup> comboBoxGroups = new ArrayList<>();

        // Button panel (created early so we can reference it in listeners)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton confirmButton = new JButton(getText("Confirm.text"));

        final boolean[] confirmed = { false };
        final ResultHolder resultHolder = new ResultHolder();

        // Add all units to single column
        for (TestUnit unit : allUnits) {
            String unitName = unit.getName();
            Money sellValue = unit.getSellValue();

            // Create row panel with label, two combo boxes, and validation label
            JPanel rowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
            rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
            rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

            JLabel unitLabel = new JLabel();
            if (soldSalvage.contains(unit)) {
                unitLabel.setText(unitName + " - " + sellValue.toAmountString() + "C-Bills <b><s>C</s><b>");
            } else {
                unitLabel.setText(unitName + " - " + sellValue.toAmountString() + "C-Bills <b>\u267B</b>");
            }

            RecoveryTimeData data = recoveryTimeData.get(unit.getId());
            if (data != null) {
                unitLabel.setToolTipText(wordWrap(data.getRecoveryTimeBreakdownString(false)));
            } else {
                LOGGER.error("No recovery time data found for unit {}", unit.getId());
            }

            JLabel validationLabel = new JLabel();

            JComboBox<String> comboBox1 = new JComboBox<>();
            JComboBox<String> comboBox2 = new JComboBox<>();
            comboBox1.addItem(null); // Allow empty selection
            comboBox2.addItem(null); // Allow empty selection

            // Build the mapping and populate combo boxes
            unitNameMap.clear();
            for (Unit salvageUnit : salvageUnits) {
                String displayName = CamOpsSalvageUtilities.getSalvageTooltip(List.of(salvageUnit), isInSpace);
                unitNameMap.put(displayName, salvageUnit);
                comboBox1.addItem(displayName);
                comboBox2.addItem(displayName);
            }

            ComboBoxGroup group = new ComboBoxGroup(comboBox1, comboBox2, validationLabel, unitLabel, unit);
            comboBoxGroups.add(group);

            // These need to be after the above lines, as we're going to use 'group' in the listeners.
            comboBox1.addActionListener(e -> performComboChangeAction(isContract, comboBoxGroups, group,
                  finalEmployerSalvageLabel, finalUnitSalvageLabel, finalAvailableTimeLabel, confirmButton));
            comboBox2.addActionListener(e -> performComboChangeAction(isContract, comboBoxGroups, group,
                  finalEmployerSalvageLabel, finalUnitSalvageLabel, finalAvailableTimeLabel, confirmButton));

            rowPanel.add(unitLabel);
            rowPanel.add(comboBox1);
            rowPanel.add(comboBox2);
            rowPanel.add(validationLabel);

            column.add(rowPanel);
        }

        JScrollPane scrollPane = new JScrollPane(column);
        scrollPane.setBorder(null);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        dialog.add(mainPanel, BorderLayout.CENTER);

        confirmButton.addActionListener(e -> {
            confirmed[0] = true;
            resultHolder.groups = comboBoxGroups;
            dialog.dispose();
        });

        buttonPanel.add(confirmButton);

        dialog.add(buttonPanel, BorderLayout.SOUTH);

        // Initial button state check
        updateConfirmButtonState(comboBoxGroups, confirmButton, finalUnitSalvageLabel, isContract);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);

        return confirmed[0] ? resultHolder.groups : null;
    }

    static class ResultHolder {
        List<ComboBoxGroup> groups = null;
    }

    private void performComboChangeAction(boolean isContract, List<ComboBoxGroup> comboBoxGroups, ComboBoxGroup group,
          JLabel finalEmployerSalvageLabel, JLabel finalUnitSalvageLabel, JLabel finalAvailableTimeLabel,
          JButton confirmButton) {
        updateComboBoxOptions(comboBoxGroups, unitNameMap);
        updateValidation(group, unitNameMap);
        updateSalvageAllocation(comboBoxGroups,
              unitNameMap,
              finalEmployerSalvageLabel,
              finalUnitSalvageLabel,
              finalAvailableTimeLabel);
        updateConfirmButtonState(comboBoxGroups, confirmButton, finalUnitSalvageLabel, isContract);
    }

    /**
     * Updates the confirm button enabled state based on validation rules.
     *
     * @param comboBoxGroups list of all combo box groups
     * @param confirmButton  the confirm button to enable/disable
     * @param isContract     whether this is a contract mission
     */
    private void updateConfirmButtonState(List<ComboBoxGroup> comboBoxGroups, JButton confirmButton,
          JLabel unitSalvageLabel, boolean isContract) {
        // Check for any invalid units
        for (ComboBoxGroup group : comboBoxGroups) {
            String unitName1 = (String) group.comboBox1.getSelectedItem();
            String unitName2 = (String) group.comboBox2.getSelectedItem();

            // If units are assigned, check validation state
            if ((unitName1 != null || unitName2 != null) && !isValidationValid(group)) {
                confirmButton.setEnabled(false);
                return;
            }
        }

        // Check salvage percentage if this is a contract
        if (isContract) {
            Money totalSalvage = employerSalvageMoney.plus(unitSalvageMoney);

            if (totalSalvage.isPositive()) {
                // Calculate percentage: (unitSalvage / totalSalvage) * 100
                double currentPercent = unitSalvageMoney.getAmount().doubleValue() /
                                              totalSalvage.getAmount().doubleValue() * 100.0;

                if (currentPercent > salvagePercent) {
                    confirmButton.setEnabled(false);
                    if (unitSalvageLabel != null) {
                        unitSalvageLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
                    }
                    return;
                }
            }
        }

        // Reset color if salvage percent is valid
        if (unitSalvageLabel != null) {
            unitSalvageLabel.setForeground(null);
        }

        // All checks passed
        confirmButton.setEnabled(true);
    }

    /**
     * Checks if a combo box group has valid selections.
     *
     * @param group the combo box group to check
     *
     * @return true if valid or no units assigned, false if invalid
     */
    private boolean isValidationValid(ComboBoxGroup group) {
        String validationText = group.validationLabel.getText();
        return validationText.isEmpty() || validationText.equals("Valid");
    }

    /**
     * Updates salvage allocation tracking based on which units have salvage teams assigned.
     *
     * @param comboBoxGroups       list of all combo box groups
     * @param employerSalvageLabel label showing employer salvage value (can be null)
     * @param unitSalvageLabel     label showing unit salvage value (can be null)
     * @param availableTimeLabel   label showing time usage (can be null)
     */
    private void updateSalvageAllocation(List<ComboBoxGroup> comboBoxGroups, Map<String, Unit> unitNameMap,
          JLabel employerSalvageLabel, JLabel unitSalvageLabel, JLabel availableTimeLabel) {
        // Reset tracking values
        usedSalvageTime = 0;
        Money tempEmployerSalvage = Money.zero();
        Money tempUnitSalvage = Money.zero();

        for (ComboBoxGroup group : comboBoxGroups) {
            String unitName1 = (String) group.comboBox1.getSelectedItem();
            String unitName2 = (String) group.comboBox2.getSelectedItem();

            Unit unit1 = unitName1 != null ? unitNameMap.get(unitName1) : null;
            Unit unit2 = unitName2 != null ? unitNameMap.get(unitName2) : null;
            TestUnit targetUnit = group.targetUnit;

            boolean hasAssignedUnits = (unit1 != null) || (unit2 != null);

            if (hasAssignedUnits) {
                // Add recovery time
                RecoveryTimeData timeData = recoveryTimeData.get(targetUnit.getId());
                if (timeData != null) {
                    usedSalvageTime += timeData.totalRecoveryTime();
                }

                // Add to unit salvage
                tempUnitSalvage = tempUnitSalvage.plus(targetUnit.getSellValue());
            } else {
                // Add to employer salvage
                tempEmployerSalvage = tempEmployerSalvage.plus(targetUnit.getSellValue());
            }
        }

        // Update the actual tracking values
        employerSalvageMoney = tempEmployerSalvage;
        unitSalvageMoney = tempUnitSalvage;

        // Update labels if they exist
        if (employerSalvageLabel != null) {
            employerSalvageLabel.setText("Employer Salvage: " + employerSalvageMoney.toAmountString() + " C-Bills");
        }
        if (unitSalvageLabel != null) {
            unitSalvageLabel.setText("Unit Salvage: " + unitSalvageMoney.toAmountString() + " C-Bills");
        }
        if (availableTimeLabel != null) {
            availableTimeLabel.setText("Available Time: " + usedSalvageTime + " / " + maximumSalvageTime + " minutes");
        }
    }

    private record ComboBoxGroup(JComboBox<String> comboBox1, JComboBox<String> comboBox2, JLabel validationLabel,
          JLabel unitLabel, TestUnit targetUnit) {}

    private void updateComboBoxOptions(List<ComboBoxGroup> comboBoxGroups, Map<String, Unit> unitNameMap) {
        // Collect all currently selected unit names
        List<String> selectedUnitNames = new ArrayList<>();
        for (ComboBoxGroup group : comboBoxGroups) {
            String selected1 = (String) group.comboBox1.getSelectedItem();
            String selected2 = (String) group.comboBox2.getSelectedItem();
            if (selected1 != null) {
                selectedUnitNames.add(selected1);
            }
            if (selected2 != null) {
                selectedUnitNames.add(selected2);
            }
        }

        // Update each combo box
        for (ComboBoxGroup group : comboBoxGroups) {
            updateSingleComboBox(group.comboBox1, selectedUnitNames, unitNameMap);
            updateSingleComboBox(group.comboBox2, selectedUnitNames, unitNameMap);
        }
    }

    private void updateSingleComboBox(JComboBox<String> comboBox, List<String> selectedUnitNames,
          Map<String, Unit> unitNameMap) {
        String currentSelection = (String) comboBox.getSelectedItem();
        comboBox.removeAllItems();
        comboBox.addItem(null); // Allow empty selection

        for (String unitName : unitNameMap.keySet()) {
            // Add unit if it's not selected elsewhere, or if it's the current selection
            if (!selectedUnitNames.contains(unitName) || unitName.equals(currentSelection)) {
                comboBox.addItem(unitName);
            }
        }

        // Restore the selection
        comboBox.setSelectedItem(currentSelection);
    }

    private void updateValidation(ComboBoxGroup group, Map<String, Unit> unitNameMap) {
        String unitName1 = (String) group.comboBox1.getSelectedItem();
        String unitName2 = (String) group.comboBox2.getSelectedItem();

        Unit unit1 = unitName1 != null ? unitNameMap.get(unitName1) : null;
        Unit unit2 = unitName2 != null ? unitNameMap.get(unitName2) : null;
        TestUnit targetUnit = group.targetUnit;

        // If no units selected, clear validation and reset color
        if (unit1 == null && unit2 == null) {
            group.validationLabel.setText("");
            group.unitLabel.setForeground(null); // Reset to default color
            return;
        }

        Entity targetEntity = targetUnit.getEntity();
        double targetWeight = 0.0;
        boolean isLargeVessel = false;
        if (targetEntity != null) {
            targetWeight = targetEntity.getWeight();
            isLargeVessel = targetEntity instanceof Dropship || targetEntity instanceof Jumpship;
        }

        // Check for naval tug requirement
        Entity unit1Entity = unit1 != null ? unit1.getEntity() : null;
        Entity unit2Entity = unit2 != null ? unit2.getEntity() : null;
        if (isInSpace && isLargeVessel) {
            boolean hasNavalTug = (unit1Entity != null && CamOpsSalvageUtilities.hasNavalTug(unit1Entity)) ||
                                        (unit2Entity != null && CamOpsSalvageUtilities.hasNavalTug(unit2Entity));
            if (!hasNavalTug) {
                group.validationLabel.setText("No Naval Tug");
                group.unitLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
                return;
            }
        }

        // Check cargo and towage capacity
        double cargo1 = unit1 != null ? unit1.getCargoCapacity() : 0;
        double cargo2 = unit2 != null ? unit2.getCargoCapacity() : 0;
        double weight1 = unit1Entity != null ? unit1Entity.getWeight() : 0;
        double weight2 = unit2 != null ? unit2Entity.getWeight() : 0;

        boolean hasCargoCapacity = cargo1 >= targetWeight || cargo2 >= targetWeight;
        boolean hasTowageCapacity = (weight1 + weight2) >= targetWeight;

        if (!hasCargoCapacity && !hasTowageCapacity) {
            group.validationLabel.setText("Insufficient Towage or Cargo");
            group.unitLabel.setForeground(MekHQ.getMHQOptions().getFontColorNegative());
            return;
        }

        group.validationLabel.setText("Valid");
        group.unitLabel.setForeground(null);  // Reset to default color
    }
}
