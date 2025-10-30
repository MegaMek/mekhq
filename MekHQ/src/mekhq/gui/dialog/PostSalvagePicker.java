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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

import megamek.common.units.Aero;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Dialog for selecting salvage units after a scenario is completed.
 *
 * <p>This class presents the player with a dialog showing all available salvage units and allows them to select
 * which units to claim. For contract missions, it enforces salvage percentage limits and dynamically updates the
 * salvage allocation between the player's unit and the employer.</p>
 *
 * <p>The dialog displays units in a three-column layout, sorted by sell value (highest to lowest), and automatically
 * pre-selects units up to the salvage percentage limit for contracts. Units that have been sold are marked with
 * "(Sold)" for reference.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class PostSalvagePicker {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CamOpsSalvage";

    private final static int PADDING = scaleForGUI(10);
    private final static Dimension DIMENSION = scaleForGUI(800, 600);

    /**
     * Creates a new post-salvage picker dialog and processes the selected salvage.
     *
     * <p>This constructor displays a dialog allowing the player to select which salvage units to claim, then
     * processes the selected units by adding them to the campaign, handling financial transactions, and updating
     * contract salvage tracking.</p>
     *
     * @param campaign        The current {@link Campaign} in which the scenario took place.
     * @param mission         The {@link Mission} associated with the scenario.
     * @param scenario        The {@link Scenario} that was just completed.
     * @param actualSalvage   The list of {@link TestUnit}s available as salvage that the player can claim.
     * @param soldSalvage     The list of {@link TestUnit}s that were sold instead of claimed.
     * @param leftoverSalvage The list of {@link TestUnit}s that were not claimed by the player and go to the employer.
     *
     * @author Illiani
     * @since 0.50.10
     */
    public PostSalvagePicker(Campaign campaign, Mission mission, Scenario scenario, List<TestUnit> actualSalvage,
          List<TestUnit> soldSalvage, List<TestUnit> leftoverSalvage) {
        List<TestUnit> allUnits = new ArrayList<>(actualSalvage);
        allUnits.addAll(soldSalvage);
        allUnits.sort(Comparator.comparing(TestUnit::getSellValue).reversed()); // Highest -> Lowest

        // If the player has 100% salvage, skip the salvage selection dialog entirely.
        boolean skipSalvage = mission instanceof Contract contract && contract.getSalvagePct() >= 100;

        List<TestUnit> selectedUnits;
        if (!skipSalvage) {
            selectedUnits = showSalvageDialog(mission, allUnits, actualSalvage, soldSalvage);
        } else {
            selectedUnits = allUnits;
        }

        // Process selected units
        if (selectedUnits != null) {
            resolveSalvage(campaign, mission, scenario, actualSalvage, soldSalvage, leftoverSalvage);
        }
    }

    /**
     * Displays a dialog for selecting salvage units and returns the player's selection.
     *
     * <p>The dialog presents all available salvage units in a three-column layout. For contract missions, it
     * enforces salvage percentage limits and dynamically updates labels showing how much salvage value is allocated to
     * the player versus the employer.</p>
     *
     * <p>Units are automatically pre-selected up to the salvage percentage limit for contracts. When the limit is
     * reached, unchecked checkboxes are disabled to prevent the player from exceeding their salvage allocation.</p>
     *
     * <p>The dialog cannot be closed via the window close button to prevent accidental cancellation of all salvage
     * selections.</p>
     *
     * @param mission       The {@link Mission} to determine if salvage percentage limits apply.
     * @param allUnits      The complete list of available salvage units (both claimed and sold).
     * @param actualSalvage The list of salvage units available to claim (modified by this method).
     * @param soldSalvage   The list of units that were sold (modified by this method).
     *
     * @return The list of selected units. Cancellation is prevented by the dialog's close operation.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private List<TestUnit> showSalvageDialog(Mission mission, List<TestUnit> allUnits, List<TestUnit> actualSalvage,
          List<TestUnit> soldSalvage) {
        JDialog dialog = new JDialog((Frame) null, getText("accessingTerminal.title"), true);
        dialog.setLayout(new BorderLayout());
        dialog.setSize(DIMENSION);
        dialog.setLocationRelativeTo(null);

        // We don't want the player to be able to accidentally cancel all of their salvage.
        dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        JPanel pnlTop = new JPanel();
        pnlTop.setLayout(new BoxLayout(pnlTop, BoxLayout.Y_AXIS));
        pnlTop.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));

        JTextArea txtTop = new JTextArea(
              getTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.introduction")
        );
        txtTop.setEditable(false);
        txtTop.setWrapStyleWord(true);
        txtTop.setLineWrap(true);
        txtTop.setBackground(dialog.getBackground());
        txtTop.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlTop.add(txtTop);

        pnlTop.add(Box.createVerticalStrut(PADDING));

        // Info labels (will be updated dynamically if Contract)
        JLabel lblSalvagePercent = new JLabel();
        lblSalvagePercent.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSalvagedByUnit = new JLabel();
        lblSalvagedByUnit.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSalvagedByEmployer = new JLabel();
        lblSalvagedByEmployer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlSalvageValues = new JPanel();
        pnlSalvageValues.setLayout(new BoxLayout(pnlSalvageValues, BoxLayout.Y_AXIS));
        pnlSalvageValues.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel pnlCheckBoxesOuter = new JPanel(new GridLayout(1, 2, PADDING, 0));
        JPanel pnlLeftColumn = new JPanel();
        pnlLeftColumn.setLayout(new BoxLayout(pnlLeftColumn, BoxLayout.Y_AXIS));
        JPanel pnlMiddleColumn = new JPanel();
        pnlMiddleColumn.setLayout(new BoxLayout(pnlMiddleColumn, BoxLayout.Y_AXIS));
        JPanel pnlRightColumn = new JPanel();
        pnlRightColumn.setLayout(new BoxLayout(pnlRightColumn, BoxLayout.Y_AXIS));

        Map<JCheckBox, TestUnit> checkboxMap = new LinkedHashMap<>();

        if (mission instanceof Contract contract) {
            int salvagePercentMax = contract.getSalvagePct();
            Money[] salvagedByUnit = { Money.zero() };
            Money[] salvagedByEmployer = { Money.zero() };
            Money[] totalSalvageValue = { Money.zero() };

            for (TestUnit unit : allUnits) {
                totalSalvageValue[0] = totalSalvageValue[0].plus(unit.getSellValue());
            }

            lblSalvagePercent.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.percent",
                  salvagePercentMax));
            lblSalvagedByUnit.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.salvagedUnit",
                  salvagedByUnit[0].toAmountString()));
            lblSalvagedByEmployer.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.salvagedEmployer",
                  salvagedByEmployer[0].toAmountString()));

            pnlSalvageValues.add(lblSalvagePercent);
            pnlSalvageValues.add(lblSalvagedByUnit);
            pnlSalvageValues.add(lblSalvagedByEmployer);
            pnlTop.add(pnlSalvageValues);

            Runnable updateLabelsAndCheckboxes = getUpdateLabelsAndCheckboxes(lblSalvagedByUnit, salvagedByUnit,
                  lblSalvagedByEmployer, salvagedByEmployer, totalSalvageValue, salvagePercentMax, checkboxMap);

            Money maxAllowedSalvage = totalSalvageValue[0].multipliedBy(salvagePercentMax).dividedBy(100);
            Money runningTotal = Money.zero();

            int index = 0;
            for (TestUnit unit : allUnits) {
                ResolveScenarioTracker.UnitStatus unitStatus = new ResolveScenarioTracker.UnitStatus(unit);
                String label = unitStatus.getDesc(true);
                if (soldSalvage.contains(unit)) {
                    label += " (" + getTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.sold") + ")";
                }

                JCheckBox checkbox = new JCheckBox(label);
                checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);

                // Determine if this unit should be initially selected
                Money unitValue = unit.getSellValue();
                Money potentialTotal = runningTotal.plus(unitValue);

                if (!potentialTotal.isGreaterThan(maxAllowedSalvage)) {
                    checkbox.setSelected(true);
                    salvagedByUnit[0] = salvagedByUnit[0].plus(unitValue);
                    runningTotal = potentialTotal;
                } else {
                    checkbox.setSelected(false);
                    salvagedByEmployer[0] = salvagedByEmployer[0].plus(unitValue);
                }

                checkboxMap.put(checkbox, unit);

                // Alternate between columns
                if (index % 3 == 0) {
                    pnlLeftColumn.add(checkbox);
                } else if (index % 3 == 1) {
                    pnlMiddleColumn.add(checkbox);
                } else {
                    pnlRightColumn.add(checkbox);
                }
                index++;

                checkbox.addItemListener(e -> updateSalvageValues(checkbox,
                      salvagedByEmployer,
                      unitValue,
                      salvagedByUnit,
                      updateLabelsAndCheckboxes));
            }

            salvagedByUnit[0] = salvagedByUnit[0].plus(contract.getSalvagedByUnit());
            salvagedByEmployer[0] = salvagedByEmployer[0].plus(contract.getSalvagedByEmployer());

            updateLabelsAndCheckboxes.run();
        } else {
            // Non-contract missions - simple checkboxes without listeners
            int index = 0;
            for (TestUnit unit : allUnits) {
                ResolveScenarioTracker.UnitStatus unitStatus = new ResolveScenarioTracker.UnitStatus(unit);
                String label = unitStatus.getDesc(true);
                if (soldSalvage.contains(unit)) {
                    label += " (" + getTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.sold") + ")";
                }

                JCheckBox checkbox = new JCheckBox(label);
                checkbox.setSelected(true);
                checkbox.setAlignmentX(Component.LEFT_ALIGNMENT);
                checkboxMap.put(checkbox, unit);

                // Alternate between columns
                if (index % 3 == 0) {
                    pnlLeftColumn.add(checkbox);
                } else if (index % 3 == 1) {
                    pnlMiddleColumn.add(checkbox);
                } else {
                    pnlRightColumn.add(checkbox);
                }
                index++;
            }
        }

        pnlCheckBoxesOuter.add(pnlLeftColumn);
        pnlCheckBoxesOuter.add(pnlMiddleColumn);
        pnlCheckBoxesOuter.add(pnlRightColumn);

        dialog.add(pnlTop, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(pnlCheckBoxesOuter);
        scrollPane.setBorder(RoundedLineBorder.createRoundedLineBorder());
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        dialog.add(scrollPane, BorderLayout.CENTER);

        JPanel pnlButton = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnConfirm = new JButton(getTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.confirm"));

        final boolean[] confirmed = { false };

        btnConfirm.addActionListener(e -> dialogConfirmed(allUnits,
              actualSalvage,
              soldSalvage,
              checkboxMap,
              confirmed,
              dialog));

        pnlButton.add(btnConfirm);
        dialog.add(pnlButton, BorderLayout.SOUTH);

        dialog.setVisible(true);

        return confirmed[0] ? allUnits : null;
    }

    /**
     * Updates the salvage value allocation when a checkbox is toggled.
     *
     * <p>When a checkbox is selected, the unit's value is moved from the employer's salvage total to the player's
     * unit total. When deselected, the value is moved back to the employer.</p>
     *
     * @param checkbox                  The checkbox that was toggled.
     * @param salvagedByEmployer        Array containing the employer's salvage total (modified by this method).
     * @param unitValue                 The sell value of the unit associated with the checkbox.
     * @param salvagedByUnit            Array containing the player unit's salvage total (modified by this method).
     * @param updateLabelsAndCheckboxes Runnable to update the UI labels and checkbox-enabled states.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void updateSalvageValues(JCheckBox checkbox, Money[] salvagedByEmployer, Money unitValue,
          Money[] salvagedByUnit, Runnable updateLabelsAndCheckboxes) {
        if (checkbox.isSelected()) {
            // Moving from employer to unit
            salvagedByEmployer[0] = salvagedByEmployer[0].minus(unitValue);
            salvagedByUnit[0] = salvagedByUnit[0].plus(unitValue);
        } else {
            // Moving from unit to employer
            salvagedByUnit[0] = salvagedByUnit[0].minus(unitValue);
            salvagedByEmployer[0] = salvagedByEmployer[0].plus(unitValue);
        }

        updateLabelsAndCheckboxes.run();
    }

    /**
     * Handles the confirmation of salvage selection when the OK button is clicked.
     *
     * <p>This method collects all unselected units, removes them from the relevant lists, marks the dialog as
     * confirmed, and closes the dialog.</p>
     *
     * @param allUnits      The complete list of all salvage units (modified by this method).
     * @param actualSalvage The list of salvage units to be claimed (modified by this method).
     * @param soldSalvage   The list of units that were sold (modified by this method).
     * @param checkboxMap   The map linking checkboxes to their associated units.
     * @param confirmed     Boolean array indicating whether the dialog was confirmed.
     * @param dialog        The dialog to be disposed of after confirmation.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static void dialogConfirmed(List<TestUnit> allUnits, List<TestUnit> actualSalvage,
          List<TestUnit> soldSalvage,
          Map<JCheckBox, TestUnit> checkboxMap, boolean[] confirmed, JDialog dialog) {
        List<TestUnit> unselectedUnits = new ArrayList<>();

        // Collect unselected units
        for (Map.Entry<JCheckBox, TestUnit> entry : checkboxMap.entrySet()) {
            if (!entry.getKey().isSelected()) {
                unselectedUnits.add(entry.getValue());
            }
        }

        // Remove unselected units from all three lists
        allUnits.removeAll(unselectedUnits);
        actualSalvage.removeIf(testUnit -> unselectedUnits.stream()
                                                 .anyMatch(unit -> unit.getId().equals(testUnit.getId())));
        soldSalvage.removeAll(unselectedUnits);

        confirmed[0] = true;
        dialog.dispose();
    }

    /**
     * Creates a runnable that updates salvage labels and checkbox enabled states.
     *
     * <p>This method returns a {@link Runnable} that recalculates the salvage percentage and updates the UI labels
     * to reflect current salvage allocations. It also enables or disables unchecked checkboxes based on whether the
     * salvage percentage limit has been reached.</p>
     *
     * @param lblSalvagedByUnit     The label displaying the player unit's salvage total.
     * @param salvagedByUnit        Array containing the player unit's salvage total.
     * @param lblSalvagedByEmployer The label displaying the employer's salvage total.
     * @param salvagedByEmployer    Array containing the employer's salvage total.
     * @param totalSalvageValue     Array containing the total value of all available salvage.
     * @param salvagePercentMax     The maximum salvage percentage allowed by the contract.
     * @param checkboxMap           The map of checkboxes to units for enabling/disabling based on limits.
     *
     * @return A {@link Runnable} that updates labels and checkbox states when executed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private static Runnable getUpdateLabelsAndCheckboxes(JLabel lblSalvagedByUnit, Money[] salvagedByUnit,
          JLabel lblSalvagedByEmployer, Money[] salvagedByEmployer, Money[] totalSalvageValue, int salvagePercentMax,
          Map<JCheckBox, TestUnit> checkboxMap) {
        return () -> {
            lblSalvagedByUnit.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.salvagedUnit",
                  salvagedByUnit[0].toAmountString()));
            lblSalvagedByEmployer.setText(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.salvagedEmployer",
                  salvagedByEmployer[0].toAmountString()));

            // Calculate current percentage
            double currentPercent = 0;
            if (totalSalvageValue[0].isPositive()) {
                currentPercent = salvagedByUnit[0].getAmount()
                                       .multiply(BigDecimal.valueOf(100))
                                       .divide(totalSalvageValue[0].getAmount(), RoundingMode.HALF_UP)
                                       .doubleValue();
            }

            boolean exceededLimit = currentPercent >= salvagePercentMax;

            // Enable/disable unchecked checkboxes based on percentage
            for (Map.Entry<JCheckBox, TestUnit> entry : checkboxMap.entrySet()) {
                if (!entry.getKey().isSelected()) {
                    entry.getKey().setEnabled(!exceededLimit);
                }
            }
        };
    }

    /**
     * Processes and finalizes salvage after player selection.
     *
     * <p>This method handles the actual processing of salvage units, including:</p>
     * <ul>
     *   <li>Adding claimed salvage units to the campaign</li>
     *   <li>Processing sold salvage units and crediting the account</li>
     *   <li>Handling salvage exchange for contracts</li>
     *   <li>Updating contract salvage tracking</li>
     *   <li>Setting repair locations for salvaged units</li>
     * </ul>
     *
     * @param campaign        The current {@link Campaign} to add salvage to.
     * @param mission         The {@link Mission} associated with the salvage.
     * @param scenario        The {@link Scenario} that generated the salvage.
     * @param actualSalvage   The list of units claimed by the player.
     * @param soldSalvage     The list of units that were sold instead of claimed.
     * @param leftoverSalvage The list of units going to the employer or unclaimed.
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void resolveSalvage(Campaign campaign, Mission mission, Scenario scenario, List<TestUnit> actualSalvage,
          List<TestUnit> soldSalvage, List<TestUnit> leftoverSalvage) {
        boolean isContract = mission instanceof Contract;

        // now let's take care of salvage
        for (TestUnit salvageUnit : actualSalvage) {
            ResolveScenarioTracker.UnitStatus salvageStatus = new ResolveScenarioTracker.UnitStatus(salvageUnit);
            if (salvageUnit.getEntity() instanceof Aero) {
                ((Aero) salvageUnit.getEntity()).setFuelTonnage(((Aero) salvageStatus.getBaseEntity()).getFuelTonnage());
            }
            campaign.clearGameData(salvageUnit.getEntity());
            campaign.addTestUnit(salvageUnit);
            // if this is a contract, add to the salvaged value
            if (isContract) {
                ((Contract) mission).addSalvageByUnit(salvageUnit.getSellValue());
            }
        }

        // And any ransomed salvaged units
        Money unitRansoms = Money.zero();
        if (!soldSalvage.isEmpty()) {
            for (TestUnit ransomedUnit : soldSalvage) {
                unitRansoms = unitRansoms.plus(ransomedUnit.getSellValue());
            }

            if (unitRansoms.isGreaterThan(Money.zero())) {
                campaign.getFinances()
                      .credit(TransactionType.SALVAGE,
                            campaign.getLocalDate(),
                            unitRansoms,
                            getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.unitSale", scenario.getName()));
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.unitSale.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));
                if (isContract) {
                    ((Contract) mission).addSalvageByUnit(unitRansoms);
                }
            }
        }

        if (isContract) {
            Money value = Money.zero();
            for (TestUnit salvageUnit : leftoverSalvage) {
                value = value.plus(salvageUnit.getSellValue());
            }
            if (((Contract) mission).isSalvageExchange()) {
                value = value.multipliedBy(((Contract) mission).getSalvagePct()).dividedBy(100);
                campaign.getFinances()
                      .credit(TransactionType.SALVAGE_EXCHANGE,
                            campaign.getLocalDate(),
                            value,
                            getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.exchange", scenario.getName()));
                campaign.addReport(getFormattedTextAt(RESOURCE_BUNDLE, "PostSalvagePicker.exchange.report",
                      unitRansoms.toAmountString(), scenario.getHyperlinkedName()));
            } else {
                ((Contract) mission).addSalvageByEmployer(value);
            }
        }

        for (TestUnit unit : actualSalvage) {
            unit.setSite(mission.getRepairLocation());
        }
    }
}
