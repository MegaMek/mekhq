/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.commandGeneration.designer;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.commandGeneration.designer.DesignerUi.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCheckBox;

/**
 * The Command Designer's Logistics page: what the command owns and starts with.
 *
 * <p>The spare parts it is stocked with, which are the campaign's own AutoLogistics restock percentages, and the cash
 * it starts with, counted in months of the running costs the campaign charges. The lift the roll brings is set on the
 * Force page with the rest of the transport, and the starting simulation on the People page.</p>
 */
public final class LogisticsPage {
    private static final int RAIL_WIDTH = 300;
    private static final int WRAP_WIDTH = 280;
    /** The width the starting-loan warning wraps at. */
    private static final int LOAN_WARNING_WIDTH = 520;
    private static final int MAX_SPARES_PERCENT = 10000;
    /** Where a spares meter reads full: twice a full restock. */
    private static final int SPARES_METER_FULL = 200;
    private static final int MAX_MONTHS = 120;
    private static final int TRACK_MONTHS = 24;

    /**
     * The restock categories the Spares section offers, each paired with the campaign option it reads and writes.
     *
     * <p>This is the single list. The steppers are built from it, loaded from it and written back from it, so a new
     * autoLogistics category is added here once rather than in three places that can drift apart. Declared in the
     * order they appear on screen.</p>
     */
    enum SparesCategory {
        MEK_HEAD("SparesMekHead", CampaignOption.AUTO_LOGISTICS_MEK_HEAD),
        MEK_LOCATION("SparesMekLocation", CampaignOption.AUTO_LOGISTICS_MEK_LOCATION),
        NON_REPAIRABLE_LOCATION("SparesNonRepairableLocation", CampaignOption.AUTO_LOGISTICS_NON_REPAIRABLE_LOCATION),
        ARMOR("SparesArmor", CampaignOption.AUTO_LOGISTICS_ARMOR),
        EQUIPMENT_KIT("SparesEquipmentKit", CampaignOption.AUTO_LOGISTICS_EQUIPMENT_KIT),
        AMMUNITION("SparesAmmunition", CampaignOption.AUTO_LOGISTICS_AMMUNITION),
        BOMB("SparesBomb", CampaignOption.AUTO_LOGISTICS_BOMB),
        HEAT_SINK("SparesHeatSink", CampaignOption.AUTO_LOGISTICS_HEAT_SINK),
        WEAPONS("SparesWeapons", CampaignOption.AUTO_LOGISTICS_WEAPONS),
        ACTUATORS("SparesActuators", CampaignOption.AUTO_LOGISTICS_ACTUATORS),
        JUMP_JETS("SparesJumpJets", CampaignOption.AUTO_LOGISTICS_JUMP_JETS),
        HEAD_COMPONENTS("SparesHeadComponents", CampaignOption.AUTO_LOGISTICS_HEAD_COMPONENTS),
        ENGINES("SparesEngines", CampaignOption.AUTO_LOGISTICS_ENGINES),
        GYROS("SparesGyros", CampaignOption.AUTO_LOGISTICS_GYROS),
        OTHER("SparesOther", CampaignOption.AUTO_LOGISTICS_OTHER);

        private final String resourceKey;
        private final CampaignOption<Integer> campaignOption;

        SparesCategory(String resourceKey, CampaignOption<Integer> campaignOption) {
            this.resourceKey = resourceKey;
            this.campaignOption = campaignOption;
        }

        /** @return the bundle key for this category's label and tooltip */
        String getResourceKey() {
            return resourceKey;
        }

        /** @return the campaign option this category's stepper reads and writes */
        CampaignOption<Integer> getCampaignOption() {
            return campaignOption;
        }
    }

    private final Campaign campaign;
    private final Runnable onChanged;
    private final JPanel root = new JPanel(new BorderLayout());

    // Spares
    private final Map<SparesCategory, HudStepper> spares = new EnumMap<>(SparesCategory.class);
    private final Map<SparesCategory, Meter> sparesMeters = new EnumMap<>(SparesCategory.class);

    // Starting cash
    private final JLabel monthsBig = new JLabel();
    /** The months of running costs the starting cash is set to, chosen on the month track. */
    private int months;
    private final MonthTrack monthTrack = new MonthTrack();
    private final JPanel ledger = column();
    private final HudStepper minimumCash;
    private final HudCheckBox startingLoan = optionCheck("StartingLoan");
    /** Holds the starting-loan warning while the loan is ticked; empty otherwise. */
    private final JPanel loanWarning = column();

    // Lift and history

    /**
     * @param campaign  the campaign the command is built into
     * @param onChanged told when a choice here changes what the tiles or Review show
     */
    public LogisticsPage(Campaign campaign, Runnable onChanged) {
        this.campaign = campaign;
        this.onChanged = onChanged;

        minimumCash = new HudStepper(0, 1_000_000_000, 500_000, ForcePage::formatNumber, 110, value -> changed());
        minimumCash.setToolTipText(text("Designer.logistics.minimumCash.tooltip"));
        startingLoan.addActionListener(event -> changed());
        monthTrack.setToolTipText(text("Designer.logistics.months.tooltip"));
        monthsBig.setToolTipText(text("Designer.logistics.months.tooltip"));

        root.setOpaque(true);
        root.setBackground(GROUND);
        root.add(buildSpares(), BorderLayout.WEST);
        root.add(buildCash(), BorderLayout.CENTER);
    }

    public JComponent getComponent() {
        return root;
    }

    // region Layout

    private JComponent buildSpares() {
        JPanel rows = Hud.transparentPanel(new GridBagLayout());
        GridBagConstraints cell = new GridBagConstraints();
        cell.gridy = 0;
        cell.anchor = GridBagConstraints.WEST;
        cell.insets = new Insets(0, 0, scaleForGUI(4), scaleForGUI(8));
        for (SparesCategory category : SparesCategory.values()) {
            Meter meter = new Meter();
            sparesMeters.put(category, meter);
            HudStepper stepper = new HudStepper(0, MAX_SPARES_PERCENT, 10,
                  value -> formatted("Designer.percent", value), 58, value -> refreshSparesMeter(category));
            spares.put(category, stepper);
            String tooltip = text("lbl" + category.getResourceKey() + ".tooltip");
            JLabel label = muted(text("lbl" + category.getResourceKey() + ".text"));
            label.setToolTipText(tooltip);
            stepper.setToolTipText(tooltip);

            cell.gridx = 0;
            cell.weightx = 1.0;
            cell.fill = GridBagConstraints.HORIZONTAL;
            rows.add(label, cell);
            cell.gridx = 1;
            cell.weightx = 0.0;
            cell.fill = GridBagConstraints.NONE;
            rows.add(stepper, cell);
            cell.gridx = 2;
            cell.weightx = 0.6;
            cell.fill = GridBagConstraints.HORIZONTAL;
            rows.add(meter, cell);
            cell.gridy++;
        }
        JPanel content = stack(section(text("Designer.logistics.section.spares"),
              text("Designer.logistics.section.spares.aside"), rows,
              wrapped(text("Designer.logistics.spares.note"), WRAP_WIDTH)));
        padded(content, 12, 14, 18, 14);
        JComponent scroll = scroll(content, SURFACE_DEEP);
        scroll.setPreferredSize(new Dimension(scaleForGUI(RAIL_WIDTH + 60), scaleForGUI(400)));
        scroll.setBorder(BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER));
        return scroll;
    }

    private JComponent buildCash() {
        monthsBig.setForeground(ACCENT_BRIGHT);
        monthsBig.setFont(hudFont(Font.BOLD, 2.2f, 0.0f));
        JPanel monthsRow = new JPanel(new BorderLayout(scaleForGUI(16), 0)) {
            @Override
            protected void paintComponent(Graphics graphics) {
                graphics.setColor(SURFACE);
                graphics.fillRect(0, 0, getWidth(), getHeight());
                graphics.setColor(BORDER);
                graphics.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }

            // Follows the row's current height: the big month count is only filled in on the first refresh, so a
            // cap taken now would be too short and the column would take the difference out of the ledger below.
            @Override
            public Dimension getMaximumSize() {
                return new Dimension(Short.MAX_VALUE, getPreferredSize().height);
            }
        };
        monthsRow.setOpaque(false);
        monthsRow.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(12), scaleForGUI(12),
              scaleForGUI(12)));
        JPanel trackColumn = column();
        addRows(trackColumn, monthTrack);
        monthsRow.add(monthsBig, BorderLayout.WEST);
        monthsRow.add(trackColumn, BorderLayout.CENTER);

        JPanel content = stack(
              section(text("Designer.logistics.section.cash"), text("Designer.logistics.section.cash.aside"),
                    monthsRow, ledger, field(text("Designer.logistics.minimumCash"), minimumCash), startingLoan,
                    loanWarning));
        padded(content, 12, 16, 18, 16);
        return scroll(content, GROUND);
    }

    // endregion Layout

    // region Refresh

    private void changed() {
        // The designer's refresh, which runs once however many changes arrive, refreshes this page along with the
        // tiles. Refreshing here as well did the page's work twice for every click.
        onChanged.run();
    }

    private void refreshSparesMeter(SparesCategory category) {
        sparesMeters.get(category).setFill(spares.get(category).getValue() / (double) SPARES_METER_FULL, ACCENT);
    }

    /** Recomputes everything this page shows that depends on the rolled command or on other choices here. */
    public void refresh() {
        for (SparesCategory category : SparesCategory.values()) {
            refreshSparesMeter(category);
        }
        monthsBig.setText(formatted("Designer.logistics.months.big", months));
        monthTrack.setMonths(months);
        refreshLedger();
        refreshLoanWarning();
        root.revalidate();
        root.repaint();
    }

    /** Lists which running costs the cash is measured in, as this campaign charges them, and its floor. */
    private void refreshLedger() {
        ledger.removeAll();
        CampaignOptions options = campaign.getCampaignOptions();
        addRows(ledger,
              costRow("Salaries", options.get(CampaignOption.PAY_FOR_SALARIES)),
              costRow("Maintenance", options.isChargingMaintenance()),
              costRow("Overhead", options.isChargingOverhead()),
              costRow("Food", options.get(CampaignOption.PAY_FOR_FOOD)),
              costRow("Housing", options.get(CampaignOption.PAY_FOR_HOUSING)),
              costRow("HotSpots", options.get(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP)),
              ledgerRow(text("Designer.logistics.ledger.months"),
                    formatted("Designer.logistics.ledger.months.value", months), TEXT, true),
              ledgerRow(text("Designer.logistics.ledger.float"), ForcePage.formatNumber(minimumCash.getValue()),
                    TEXT, false));
        if (startingLoan.isSelected()) {
            addRows(ledger, ledgerRow(text("Designer.logistics.ledger.loan"),
                  text("Designer.logistics.ledger.loan.value"), AMBER, false));
        }
        addRows(ledger, ledgerRow(text("Designer.logistics.ledger.total"),
              text("Designer.logistics.ledger.total.months"), ACCENT_BRIGHT, true));
    }

    private void refreshLoanWarning() {
        loanWarning.removeAll();
        if (startingLoan.isSelected()) {
            addRows(loanWarning, note(text("Designer.logistics.loan.warning"), AMBER, LOAN_WARNING_WIDTH));
        }
    }

    private JComponent costRow(String key, boolean charged) {
        return ledgerRow(text("Designer.logistics.ledger." + key),
              text(charged ? "Designer.logistics.ledger.counted" : "Designer.logistics.ledger.notCharged"),
              charged ? READY : TEXT_FAINT, false);
    }

    private static JComponent ledgerRow(String label, String value, Color valueColor, boolean emphasized) {
        JPanel row = new JPanel(new BorderLayout(scaleForGUI(10), 0));
        row.setOpaque(true);
        row.setBackground(emphasized ? SURFACE : SURFACE_DEEP);
        row.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), scaleForGUI(1), scaleForGUI(1), BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(10), scaleForGUI(6), scaleForGUI(10))));
        JLabel left = new JLabel(label);
        left.setForeground(TEXT);
        left.setFont(hudFont(emphasized ? Font.BOLD : Font.PLAIN, 0.92f, 0.0f));
        JLabel right = new JLabel(value, JLabel.RIGHT);
        right.setForeground(valueColor);
        right.setFont(hudFont(Font.BOLD, 0.9f, 0.0f));
        row.add(left, BorderLayout.CENTER);
        row.add(right, BorderLayout.EAST);
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, row.getPreferredSize().height));
        return row;
    }

    // endregion Refresh

    // region Options

    /** @return the months of running costs the starting cash is set to */
    public int getStartingCashMonths() {
        return months;
    }

    public void loadValuesFromOptions(@Nullable CommandGenerationOptions options) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        for (SparesCategory category : SparesCategory.values()) {
            spares.get(category).setValue(campaignOptions.get(category.getCampaignOption()));
        }
        if (options != null) {
            months = Math.clamp(options.getStartingCashMonths(), 0, MAX_MONTHS);
            minimumCash.setValue((int) Math.clamp(options.getMinimumStartingCash(), 0, Integer.MAX_VALUE));
            startingLoan.setSelected(options.isStartingLoan());
        }
        refresh();
    }

    public void writeValuesToOptions(@Nullable CommandGenerationOptions options) {
        // The spares are campaign options, written by writeSparesToCampaignOptions when the command is built.
        // Writing them here too would make reading the page a side effect on the campaign.
        if (options == null) {
            return;
        }
        options.setStartingCashMonths(months);
        options.setMinimumStartingCash(minimumCash.getValue());
        options.setStartingLoan(startingLoan.isSelected());
    }

    /** Writes the spares percentages to the campaign's AutoLogistics options; called when the command is built. */
    public void writeSparesToCampaignOptions() {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        for (SparesCategory category : SparesCategory.values()) {
            campaignOptions.set(category.getCampaignOption(), spares.get(category).getValue());
        }
    }

    // endregion Options

    /** A row of month cells, the first months filled; clicking a cell sets the months to it. */
    private final class MonthTrack extends JComponent {
        private int filled;

        MonthTrack() {
            setPreferredSize(new Dimension(scaleForGUI(260), scaleForGUI(20)));
            setMaximumSize(new Dimension(Short.MAX_VALUE, scaleForGUI(20)));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    if (!isEnabled() || !contains(event.getPoint())) {
                        return;
                    }
                    int cell = Math.clamp(event.getX() * TRACK_MONTHS / Math.max(1, getWidth()), 0,
                          TRACK_MONTHS - 1);
                    months = cell + 1;
                    changed();
                }
            });
        }

        void setMonths(int count) {
            filled = count;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D canvas = (Graphics2D) graphics.create();
            try {
                int gap = scaleForGUI(3);
                int width = (getWidth() - (TRACK_MONTHS - 1) * gap) / TRACK_MONTHS;
                for (int index = 0; index < TRACK_MONTHS; index++) {
                    int x = index * (width + gap);
                    boolean isFilled = index < filled;
                    if (isFilled && isEnabled()) {
                        canvas.setPaint(new GradientPaint(0, 0, translucent(ACCENT, 115), 0, getHeight(),
                              translucent(ACCENT, 50)));
                    } else {
                        canvas.setColor(SURFACE_DEEP);
                    }
                    canvas.fillRect(x, 0, width, getHeight());
                    canvas.setColor(isFilled ? (isEnabled() ? ACCENT : BORDER) : BORDER);
                    canvas.drawRect(x, 0, width - 1, getHeight() - 1);
                }
            } finally {
                canvas.dispose();
            }
        }
    }
}
