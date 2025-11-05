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

import static java.lang.Math.max;
import static java.lang.Math.round;
import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import megamek.common.ui.FastJScrollPane;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.TransportCostCalculations;
import mekhq.gui.baseComponents.roundedComponents.RoundedLineBorder;

/**
 * Dialog to display a detailed summary of jump-related transport costs for a campaign in MekHQ.
 *
 * <p>This dialog presents the breakdown of costs for cargo, passengers, units, and DropShip hiring, based on the
 * provided {@link TransportCostCalculations}. All values are presented in a vertically arranged and scrollable window,
 * with appropriate borders and padding for clarity.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public class JumpCostsSummary extends JDialog {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.TransportCostCalculations";

    private static final String TITLE = getTextAt(RESOURCE_BUNDLE,
          "TransportCostCalculations.report.header.title");
    private static final int PADDING = scaleForGUI(10);

    private final TransportCostCalculations calculations;


    /**
     * Constructs a modal dialog to display a complete transport cost summary for the given calculations. The dialog
     * includes sections for overall costs, cargo and passengers, units, and DropShip hiring. All content is
     * automatically formatted and displayed in a scrollable pane.
     *
     * @param owner        the parent window for this dialog (can be null)
     * @param calculations the {@link TransportCostCalculations} used for summary calculations and display
     *
     * @author Illiani
     * @since 0.50.10
     */
    public JumpCostsSummary(Frame owner, TransportCostCalculations calculations) {
        super(owner, TITLE, true);
        this.calculations = calculations;

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.add(getTotalCost());
        leftPanel.add(getCargoAndPassengersSummary());
        leftPanel.add(getDropShipSummary());

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(getUnitsSummary());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightPanel, BorderLayout.EAST);

        JScrollPane scrollPane = new FastJScrollPane(mainPanel);
        scrollPane.setBorder(null);

        this.setLayout(new BorderLayout());
        this.add(scrollPane, BorderLayout.CENTER);
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        this.pack();
        this.setLocationRelativeTo(owner);
        this.setVisible(true);
    }


    /**
     * Creates and returns a summary panel displaying the total transport cost per month, week, and day.
     *
     * <p>Each cost value is retrieved from the {@link TransportCostCalculations} for a standard period, formatted
     * using localized resource strings, and shown as a label in a vertically arranged panel. The summary panel includes
     * a titled border for context and is aligned to the left.</p>
     *
     * @return a {@link JPanel} containing formatted labels for per-month, per-week, and per-day costs
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JPanel getTotalCost() {
        Money costPerMonth = calculations.calculateJumpCostForEntireJourney(30, 0);
        String perMonthText = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.totalCost.month",
              costPerMonth.toAmountString());
        JLabel lblPerMonth = new JLabel(perMonthText);

        Money costPerWeek = calculations.calculateJumpCostForEntireJourney(7, 0);
        String perWeekText = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.totalCost.week",
              costPerWeek.toAmountString());
        JLabel lblPerWeek = new JLabel(perWeekText);

        Money costPerDay = calculations.calculateJumpCostForEntireJourney(1, 0);
        String perDayText = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.totalCost.day",
              costPerDay.toAmountString());
        JLabel lblPerDay = new JLabel(perDayText);


        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        String title = getTextAt(RESOURCE_BUNDLE, "TransportCostCalculations.report.header.all");
        summary.setBorder(RoundedLineBorder.createRoundedLineBorder(title));
        summary.add(lblPerMonth);
        summary.add(lblPerWeek);
        summary.add(lblPerDay);
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);

        return summary;
    }

    /**
     * Creates a summary panel with information about cargo and passenger requirements and costs. The returned panel
     * uses a custom rounded titled border.
     *
     * @return a {@link JPanel} summarizing cargo and passenger transport needs and costs
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JPanel getCargoAndPassengersSummary() {
        int requiredCargoSpace = (int) round(calculations.getAdditionalCargoSpaceRequired());
        String cargoSpaceLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.requiredSpace", requiredCargoSpace);
        JLabel lblCargo = new JLabel(cargoSpaceLabel);

        int cargoCost = (int) round(calculations.getCargoBayCost());
        String cargoCostLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.cost", cargoCost);
        JLabel lblCargoCost = new JLabel(cargoCostLabel);

        int requiredPassengerBays = calculations.getAdditionalPassengerBaysRequired();
        String passengersLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.passengerBays", requiredPassengerBays);
        JLabel lblPassengers = new JLabel(passengersLabel);

        int passengerCost = (int) round(calculations.getAdditionalPassengerBaysCost());
        String passengersCostLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.cost", passengerCost);
        JLabel lblPassengerCost = new JLabel(passengersCostLabel);

        JPanel cargoSummary = new JPanel();
        cargoSummary.setLayout(new BoxLayout(cargoSummary, BoxLayout.Y_AXIS));
        String title = getTextAt(RESOURCE_BUNDLE, "TransportCostCalculations.report.header.cargo");
        cargoSummary.setBorder(RoundedLineBorder.createRoundedLineBorder(title));
        cargoSummary.add(lblCargo);
        cargoSummary.add(lblCargoCost);
        cargoSummary.add(Box.createVerticalStrut(PADDING));
        cargoSummary.add(lblPassengers);
        cargoSummary.add(lblPassengerCost);
        cargoSummary.setAlignmentX(Component.LEFT_ALIGNMENT);

        return cargoSummary;
    }

    /**
     * Creates a summary panel listing all relevant unit categories (e.g., small craft, vehicles, infantry) with their
     * corresponding required space and transport costs. Only categories with values greater than zero are included.
     * Each entry is displayed as a pair of vertically stacked labels with spacing.
     *
     * @return a {@link JPanel} summarizing all movable units and their respective costs
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JPanel getUnitsSummary() {
        JPanel cargoSummary = new JPanel();
        cargoSummary.setLayout(new BoxLayout(cargoSummary, BoxLayout.Y_AXIS));
        String title = getTextAt(RESOURCE_BUNDLE, "TransportCostCalculations.report.header.units");
        cargoSummary.setBorder(RoundedLineBorder.createRoundedLineBorder(title));
        cargoSummary.setAlignmentX(Component.LEFT_ALIGNMENT);

        int requiredSmallCraftSpace = calculations.getAdditionalSmallCraftBaysRequired();
        if (requiredSmallCraftSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.smallCraft",
                  requiredSmallCraftSpace,
                  cargoSummary,
                  calculations.getAdditionalSmallCraftBaysCost());
        }

        int requiredASFSpace = calculations.getAdditionalASFBaysRequired();
        if (requiredASFSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.asf",
                  requiredASFSpace,
                  cargoSummary,
                  calculations.getAdditionalASFBaysCost());
        }

        int requiredMekSpace = calculations.getAdditionalMekBaysRequired();
        if (requiredMekSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.mek",
                  requiredMekSpace,
                  cargoSummary,
                  calculations.getAdditionalMekBaysCost());
        }

        int requiredSuperHeavyVehicleSpace = calculations.getAdditionalSuperHeavyVehicleBaysRequired();
        if (requiredSuperHeavyVehicleSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.superHeavyVehicle",
                  requiredSuperHeavyVehicleSpace,
                  cargoSummary,
                  calculations.getAdditionalSuperHeavyVehicleBaysCost());
        }

        int requiredHeavyVehicleSpace = calculations.getAdditionalHeavyVehicleBaysRequired();
        if (requiredHeavyVehicleSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.heavyVehicle",
                  requiredHeavyVehicleSpace,
                  cargoSummary,
                  calculations.getAdditionalHeavyVehicleBaysCost());
        }

        int requiredLightVehicleSpace = calculations.getAdditionalLightVehicleBaysRequired();
        if (requiredLightVehicleSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.lightVehicle",
                  requiredLightVehicleSpace,
                  cargoSummary,
                  calculations.getAdditionalLightVehicleBaysCost());
        }

        int requiredProtoMekSpace = calculations.getAdditionalProtoMekBaysRequired();
        if (requiredProtoMekSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.protoMek",
                  requiredProtoMekSpace,
                  cargoSummary,
                  calculations.getAdditionalProtoMekBaysCost());
        }

        int requiredBattleArmorSpace = calculations.getAdditionalBattleArmorBaysRequired();
        if (requiredBattleArmorSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.battleArmor",
                  requiredBattleArmorSpace,
                  cargoSummary,
                  calculations.getAdditionalBattleArmorBaysCost());
        }

        int requiredInfantrySpace = calculations.getAdditionalInfantryBaysRequired();
        if (requiredInfantrySpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.infantry",
                  requiredInfantrySpace,
                  cargoSummary,
                  calculations.getAdditionalInfantryBaysCost());
        }

        int requiredOtherUnitSpace = calculations.getOtherUnitCount();
        if (requiredOtherUnitSpace > 0) {
            createSummaryEntry("TransportCostCalculations.report.entry.other",
                  requiredOtherUnitSpace,
                  cargoSummary,
                  calculations.getAdditionalOtherUnitBaysCost());
        }

        return cargoSummary;
    }

    /**
     * Utility method to add a formatted label and its cost, with vertical padding, to the given panel.
     *
     * @param key          the resource key for the label text
     * @param requiredBays the amount to be displayed in the primary label
     * @param panel        the JPanel to which the labels should be added
     * @param cost         the numeric value to be shown in the cost label
     *
     * @author Illiani
     * @since 0.50.10
     */
    private void createSummaryEntry(String key, int requiredBays, JPanel panel, double cost) {
        String label = getFormattedTextAt(RESOURCE_BUNDLE,
              key, requiredBays);
        JLabel lblUnit = new JLabel(label);
        panel.add(lblUnit);

        cost = (int) round(cost);
        String costLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.cost", cost);
        JLabel lblUnitCost = new JLabel(costLabel);

        panel.add(lblUnitCost);
        panel.add(Box.createVerticalStrut(PADDING));
    }

    /**
     * Creates a summary panel showing DropShip-related requirements and total costs, such as additional bays, required
     * DropShips, collars, and their associated costs. Each value is formatted and added in a vertically stacked layout
     * with a titled border.
     *
     * @return a {@link JPanel} presenting DropShip hiring and jump collar information
     *
     * @author Illiani
     * @since 0.50.10
     */
    private JPanel getDropShipSummary() {
        JPanel summary = new JPanel();
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        String title = getTextAt(RESOURCE_BUNDLE, "TransportCostCalculations.report.header.dropShipHiring");
        summary.setBorder(RoundedLineBorder.createRoundedLineBorder(title));
        summary.setAlignmentX(Component.LEFT_ALIGNMENT);

        int totalAdditionalBaysRequired = calculations.getTotalAdditionalBaysRequired();
        String bayLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.totalBays", totalAdditionalBaysRequired);
        JLabel lblBays = new JLabel(bayLabel);
        summary.add(lblBays);

        int requiredCargoDropShips = calculations.getRequiredCargoDropShips();
        int additionalDropShips = max(0, calculations.getAdditionalDropShipsRequired() - requiredCargoDropShips);
        String dropShipLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.regularDropShips", additionalDropShips);
        JLabel lblDropShips = new JLabel(dropShipLabel);
        summary.add(lblDropShips);

        String cargoDropShipLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.cargoDropShips", requiredCargoDropShips);
        JLabel lblCargoDropShips = new JLabel(cargoDropShipLabel);
        summary.add(lblCargoDropShips);

        int jumpShipCollars = calculations.getAdditionalCollarsRequired();
        String jumpShipLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.collars", jumpShipCollars);
        JLabel lblJumpShips = new JLabel(jumpShipLabel);
        summary.add(lblJumpShips);

        int jumpShipCost = (int) round(calculations.getDockingCollarCost());
        int perJumpCost = (int) round(calculations.getJumpShipsRequired());
        String jumpShipCostLabel = getFormattedTextAt(RESOURCE_BUNDLE,
              "TransportCostCalculations.report.entry.cost.plusJump", jumpShipCost, perJumpCost);
        JLabel lblJumpShipsCost = new JLabel(jumpShipCostLabel);
        summary.add(lblJumpShipsCost);

        return summary;
    }
}
