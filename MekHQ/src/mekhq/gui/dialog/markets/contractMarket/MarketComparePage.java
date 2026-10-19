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
package mekhq.gui.dialog.markets.contractMarket;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.dialog.markets.contractMarket.ChaosContractMarketDialog.RESOURCE_BUNDLE;
import static mekhq.gui.dialog.markets.contractMarket.ContractMarketFormatting.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import jakarta.annotation.Nullable;
import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic;
import mekhq.campaign.mission.contract.contractData.ObfuscatableIntel;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudChip;
import mekhq.gui.baseComponents.hud.HudStyle;
import mekhq.gui.utilities.WrapLayout;

/**
 * The market's Compare page: the offers side by side, one column each, so players can weigh them without flicking
 * between dossiers. Rows are grouped into money, danger, terms and the job. In each row where higher (or lower) is
 * clearly better, the strongest value is marked BEST, and money rows carry a bar scaled across the offers so the gaps
 * read at a glance. Chips choose which offers are compared, and "Only rows that differ" hides what they share.
 *
 * <p>The offer names stay in view while scrolling down and the row labels while scrolling across: the names are the
 * table's header and the labels a second table in the scroll pane's row header.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class MarketComparePage {
    private static final String TABLE_CARD = "table";
    private static final String NOTICE_CARD = "notice";
    private static final int LABEL_COLUMN_WIDTH = 190;
    private static final int OFFER_COLUMN_MIN_WIDTH = 200;
    private static final int OFFER_COLUMN_WIDTH = 230;
    private static final int SECTION_ROW_HEIGHT = 24;
    private static final int ROW_HEIGHT = 26;
    private static final int BAR_ROW_HEIGHT = 34;
    /** A faint cyan wash over the deep surface, marking the rows most players decide by. */
    private static final Color KEY_ROW = blend(SURFACE_DEEP, ACCENT, 0.05f);

    /** Which way a row's numbers improve, if either does. Package-private, with the rows, for unit tests. */
    enum Better {
        HIGHER, LOWER, NEITHER
    }

    /**
     * One cell of the comparison.
     *
     * @param text      the text shown; may be HTML
     * @param key       plain text that is equal across offers exactly when the values are, for "Only rows that differ"
     * @param color     the text colour, or {@code null} for the standard colour
     * @param isBest    {@code true} if this is the strongest value in its row
     * @param bar       the bar's length as a share of the row's largest value, or a negative number for no bar
     * @param barColor  the bar's colour
     * @param pips      the difficulty to draw as pips, or {@code 0} for none
     * @param toolTip   a tooltip, or {@code null}
     */
    record Cell(String text, String key, @Nullable Color color, boolean isBest, double bar,
          @Nullable Color barColor, int pips, @Nullable String toolTip) {}

    /**
     * One row of the comparison: a section heading, or a labelled row of cells, one per compared offer.
     */
    record Row(String label, @Nullable String toolTip, boolean isSection, boolean isKey, List<Cell> cells) {
        boolean isTall() {
            return cells.stream().anyMatch(cell -> cell.bar() >= 0 || cell.pips() > 0);
        }
    }

    private final ChaosContractMarketDialog market;
    private final JPanel root = new JPanel(new BorderLayout());
    /** One chip per offer, wrapping onto further lines so every offer can be turned on or off. */
    private final JPanel chipRow = Hud.transparentPanel(new WrapLayout(FlowLayout.LEFT, scaleForGUI(4),
          scaleForGUI(3)));
    private final HudCheckBox differencesOnly;
    private final CardLayout bodyLayout = new CardLayout();
    private final JPanel body = new JPanel(bodyLayout);
    private final JLabel notice = Hud.notice("");
    private final JLabel cornerLabel = HudStyle.keyLabel("");

    private final CompareModel model = new CompareModel();
    private final LabelModel labelModel = new LabelModel();
    private final JTable table;
    private final JTable labels;

    private List<AbstractContract> columns = List.of();
    private List<Row> rows = List.of();

    MarketComparePage(ChaosContractMarketDialog market) {
        this.market = market;
        root.setOpaque(true);
        root.setBackground(GROUND);

        differencesOnly = new HudCheckBox(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.differencesOnly"));
        differencesOnly.setToolTipText(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.differencesOnly.tooltip"));
        differencesOnly.addActionListener(event -> refresh());

        JLabel hint = Hud.hint(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.hint"));
        JPanel controls = Hud.transparentPanel(null);
        controls.setLayout(new BoxLayout(controls, BoxLayout.X_AXIS));
        controls.add(differencesOnly);
        controls.add(Box.createHorizontalStrut(scaleForGUI(18)));
        controls.add(hint);

        JPanel bar = new JPanel(new BorderLayout(0, scaleForGUI(4)));
        bar.setOpaque(true);
        bar.setBackground(GROUND);
        bar.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(14), scaleForGUI(8), scaleForGUI(14))));
        bar.add(chipRow, BorderLayout.CENTER);
        bar.add(controls, BorderLayout.SOUTH);

        table = new JTable(model) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                // Fill the view when the offers fit, and scroll sideways instead of squeezing them when they do not.
                return (getParent() != null) && (getPreferredSize().width < getParent().getWidth());
            }
        };
        Hud.styleTable(table);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.setShowVerticalLines(true);
        table.setIntercellSpacing(new Dimension(scaleForGUI(1), scaleForGUI(1)));
        table.setDefaultRenderer(Object.class, new CellRenderer());

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setResizingAllowed(false);
        header.setDefaultRenderer(new ColumnHeaderRenderer());
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                int column = header.columnAtPoint(event.getPoint());
                if (column >= 0 && column < columns.size()) {
                    market.select(columns.get(column));
                }
            }
        });

        labels = new JTable(labelModel);
        Hud.styleTable(labels);
        labels.setDefaultRenderer(Object.class, new LabelRenderer());
        labels.setPreferredScrollableViewportSize(new Dimension(scaleForGUI(LABEL_COLUMN_WIDTH), 0));

        JScrollPane scroll = new JScrollPane(table);
        Hud.styleScroll(scroll, SURFACE_DEEP, false);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setRowHeaderView(labels);
        scroll.getRowHeader().setBackground(SURFACE_DEEP);
        JPanel corner = new JPanel(new BorderLayout());
        corner.setOpaque(true);
        corner.setBackground(GROUND);
        corner.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), scaleForGUI(1), BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(8), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));
        cornerLabel.setVerticalAlignment(SwingConstants.TOP);
        corner.add(cornerLabel, BorderLayout.NORTH);
        scroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, corner);

        notice.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(28), scaleForGUI(16), scaleForGUI(28),
              scaleForGUI(16)));
        JPanel noticeHolder = new JPanel(new BorderLayout());
        noticeHolder.setOpaque(true);
        noticeHolder.setBackground(SURFACE_DEEP);
        noticeHolder.add(notice, BorderLayout.NORTH);

        body.add(scroll, TABLE_CARD);
        body.add(noticeHolder, NOTICE_CARD);

        root.add(bar, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
    }

    JComponent getComponent() {
        return root;
    }

    /** Rebuilds the chips and the comparison from the market's state. */
    void refresh() {
        List<AbstractContract> offers = market.sortedContracts();
        rebuildChips(offers);

        columns = market.comparedContracts();
        if (offers.size() < 2 || columns.size() < 2) {
            notice.setText(getTextAt(RESOURCE_BUNDLE, offers.size() < 2
                                                            ? "compare.contractMarket.needTwo"
                                                            : "compare.contractMarket.pickTwo"));
            bodyLayout.show(body, NOTICE_CARD);
            return;
        }

        rows = buildRows(columns);
        if (differencesOnly.isSelected()) {
            rows = onlyDifferences(rows);
        }
        cornerLabel.setText(getFormattedTextAt(RESOURCE_BUNDLE, "compare.contractMarket.offers", columns.size())
                                  .toUpperCase(Locale.ROOT));

        model.fireTableStructureChanged();
        labelModel.fireTableStructureChanged();
        // A structure change discards per-row heights and column widths, so they are set again afterwards.
        for (int index = 0; index < rows.size(); index++) {
            Row row = rows.get(index);
            int height = scaleForGUI(row.isSection() ? SECTION_ROW_HEIGHT : row.isTall() ? BAR_ROW_HEIGHT : ROW_HEIGHT);
            table.setRowHeight(index, height);
            labels.setRowHeight(index, height);
        }
        for (int index = 0; index < table.getColumnCount(); index++) {
            TableColumn column = table.getColumnModel().getColumn(index);
            column.setMinWidth(scaleForGUI(OFFER_COLUMN_MIN_WIDTH));
            column.setPreferredWidth(scaleForGUI(OFFER_COLUMN_WIDTH));
        }
        table.getTableHeader().repaint();
        bodyLayout.show(body, TABLE_CARD);
    }

    private void rebuildChips(List<AbstractContract> offers) {
        chipRow.removeAll();
        chipRow.add(HudStyle.keyLabel(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.including")
                                            .toUpperCase(Locale.ROOT)));
        for (AbstractContract contract : offers) {
            HudChip chip = new HudChip(marketName(contract),
                  () -> market.setCompared(contract, !market.isCompared(contract)));
            chip.setActive(market.isCompared(contract));
            chip.setToolTipText(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.chip.tooltip"));
            chipRow.add(chip);
        }
        chipRow.revalidate();
        chipRow.repaint();
    }

    // region Rows

    private List<Row> buildRows(List<AbstractContract> offers) {
        List<Row> built = new ArrayList<>();

        section(built, "compare.contractMarket.section.money");
        moneyRow(built, "compare.contractMarket.row.netPerMonth", true, offers,
              contract -> market.estimate(contract).getNetPerMonth(), Better.HIGHER, true);
        moneyRow(built, "dossier.contractMarket.profit.net", false, offers,
              contract -> market.estimate(contract).estimatedNet(), Better.HIGHER, true);
        moneyRow(built, "dossier.contractMarket.profit.netWithCombatPay", false, offers,
              contract -> market.estimate(contract).getNetWithCombatPay(), Better.HIGHER, true);
        moneyRow(built, "dossier.contractMarket.metric.total", false, offers, AbstractContract::getTotalPay,
              Better.HIGHER, false);
        moneyRow(built, "compare.contractMarket.row.monthly", false, offers, AbstractContract::getMonthlyPayOut,
              Better.HIGHER, false);
        moneyRow(built, "dossier.contractMarket.metric.transport", false, offers,
              AbstractContract::getTransportPayment, Better.HIGHER, false);
        moneyRow(built, "compare.contractMarket.row.costs", false, offers,
              contract -> market.estimate(contract).operatingCosts(), Better.LOWER, false);
        moneyRow(built, "compare.contractMarket.row.combatPay", false, offers,
              contract -> market.estimate(contract).potentialCombatPay(), Better.HIGHER, false);

        section(built, "compare.contractMarket.section.danger");
        built.add(assessmentRow(offers));
        textRow(built, "dossier.contractMarket.intel.threat", offers,
              contract -> intel(contract, ObfuscatableIntel.THREAT, threat(contract)));
        textRow(built, "dossier.contractMarket.intel.opposition", offers,
              contract -> intel(contract, ObfuscatableIntel.OPPOSITION,
                    contract.getEnemyDisplayNameIncludingFaction(market.currentDate().getYear())));
        textRow(built, "dossier.contractMarket.intel.alliedCommand", offers,
              contract -> intel(contract, ObfuscatableIntel.ALLIED_COMMAND, alliedCommand(contract)));
        textRow(built, "dossier.contractMarket.intel.morale", offers,
              contract -> intel(contract, ObfuscatableIntel.MORALE, contract.getMoraleLevel().toString()));
        rankedRow(built, "dossier.contractMarket.intel.intensity", false, offers,
              contract -> String.valueOf(contract.getTrackCount()), contract -> (double) contract.getTrackCount(),
              Better.NEITHER);

        section(built, "compare.contractMarket.section.terms");
        // Command rights run from least freedom (Integrated) to most (Independent), so a later right is better.
        rankedRow(built, "dossier.contractMarket.terms.command", false, offers,
              contract -> contract.getCommandRights().toString(),
              contract -> (double) contract.getCommandRights().ordinal(), Better.HIGHER);
        // Exchange salvage has no share to rank, so it is left out of the best-value comparison.
        rankedRow(built, "dossier.contractMarket.terms.salvage", false, offers, ContractMarketFormatting::salvage,
              contract -> contract.isSalvageExchange() ? null : (double) asPercent(
                    contract.getSalvageRightsMultiplier()), Better.HIGHER);
        rankedRow(built, "dossier.contractMarket.terms.support", false, offers, ContractMarketFormatting::support,
              contract -> (double) asPercent(contract.getSupportMultiplier()), Better.HIGHER);
        rankedRow(built, "dossier.contractMarket.terms.transport", false, offers, ContractMarketFormatting::transport,
              contract -> (double) asPercent(contract.getTransportMultiplier()), Better.HIGHER);

        section(built, "compare.contractMarket.section.job");
        textRow(built, "compare.contractMarket.row.employer", offers, AbstractContract::getEmployerMarketDisplayName);
        textRow(built, "compare.contractMarket.row.objective", offers,
              contract -> objective(contract, market.isContractTypeHidden()));
        textRow(built, "compare.contractMarket.row.destination", offers,
              contract -> destination(contract, market.currentDate()));
        rankedRow(built, "dossier.contractMarket.terms.transit", false, offers,
              contract -> getFormattedTextAt(RESOURCE_BUNDLE, "value.contractMarket.days",
                    market.estimate(contract).transitDays()),
              contract -> (double) market.estimate(contract).transitDays(), Better.LOWER);
        rankedRow(built, "dossier.contractMarket.terms.term", false, offers,
              contract -> getFormattedTextAt(RESOURCE_BUNDLE, "card.contractMarket.months",
                    contract.getLengthInMonths()),
              contract -> (double) contract.getLengthInMonths(), Better.NEITHER);
        DecimalFormat months = new DecimalFormat("0.0");
        rankedRow(built, "compare.contractMarket.row.timeAway", false, offers,
              contract -> getFormattedTextAt(RESOURCE_BUNDLE, "compare.contractMarket.monthsAway",
                    months.format(market.estimate(contract).getEngagementMonths())),
              contract -> market.estimate(contract).getEngagementMonths(), Better.NEITHER);
        built.add(characteristicsRow(offers));
        built.add(badgesRow(offers));
        return built;
    }

    private static void section(List<Row> rows, String labelKey) {
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, labelKey), null, true, false, List.of()));
    }

    /**
     * Adds a row of plain text, such as the opposition. Text rows are never ranked.
     */
    private static void textRow(List<Row> rows, String labelKey, List<AbstractContract> offers,
          Function<AbstractContract, String> text) {
        List<Cell> cells = new ArrayList<>();
        for (AbstractContract contract : offers) {
            String value = text.apply(contract);
            cells.add(new Cell(MarketHud.escape(value), value, null, false, -1, null, 0, value));
        }
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, labelKey), rowToolTip(labelKey), false, false, cells));
    }

    /**
     * Adds a row whose values can be ranked, marking the best one when the values differ.
     *
     * @param number the value to rank by, or {@code null} for an offer that cannot be ranked on this row
     */
    private static void rankedRow(List<Row> rows, String labelKey, boolean isKey, List<AbstractContract> offers,
          Function<AbstractContract, String> text, Function<AbstractContract, Double> number, Better better) {
        List<Double> numbers = offers.stream().map(number).toList();
        Double best = bestValue(numbers, better);
        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            String value = text.apply(offers.get(index));
            boolean isBest = best != null && best.equals(numbers.get(index));
            cells.add(new Cell(MarketHud.escape(value), value, isBest ? READY : null, isBest, -1, null, 0, null));
        }
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, labelKey), rowToolTip(labelKey), false, isKey, cells));
    }

    /**
     * Adds a money row: compact figures, the best one marked, losses in red, and (for the profit rows) a bar scaled to
     * the row's largest figure so the gaps between offers read at a glance.
     *
     * @param signed {@code true} for figures that can be a loss, shown with their sign
     */
    private static void moneyRow(List<Row> rows, String labelKey, boolean isKey, List<AbstractContract> offers,
          Function<AbstractContract, Money> amount, Better better, boolean signed) {
        List<Money> amounts = offers.stream().map(amount).toList();
        List<Double> numbers = amounts.stream().map(money -> money.getAmount().doubleValue()).toList();
        Double best = bestValue(numbers, better);
        double largest = numbers.stream().mapToDouble(Math::abs).max().orElse(0);

        List<Cell> cells = new ArrayList<>();
        for (int index = 0; index < offers.size(); index++) {
            Money money = amounts.get(index);
            double value = numbers.get(index);
            boolean isBest = best != null && best == value;
            Color color = money.isNegative() ? DANGER : (isBest ? READY : null);
            Color barColor = money.isNegative() ? DANGER : (isBest ? READY : translucent(ACCENT, 110));
            String text = signed ? signedCompactMoney(money) : compactMoney(money);
            double bar = largest > 0 ? Math.abs(value) / largest : 0;
            cells.add(new Cell(text, text, color, isBest, bar, barColor, 0, money.toAmountAndSymbolString()));
        }
        rows.add(new Row(getTextAt(RESOURCE_BUNDLE, labelKey), rowToolTip(labelKey), false, isKey, cells));
    }

    /** The assessment: its word and score in the severity colour, over the difficulty pips. Never ranked. */
    private static Row assessmentRow(List<AbstractContract> offers) {
        List<Cell> cells = new ArrayList<>();
        for (AbstractContract contract : offers) {
            int difficulty = assessmentDifficulty(contract);
            String text = getFormattedTextAt(RESOURCE_BUNDLE, "compare.contractMarket.assessment",
                  assessmentLabel(contract), difficulty);
            cells.add(new Cell(MarketHud.escape(text), text, severityColor(difficulty), false, -1, null, difficulty,
                  null));
        }
        String labelKey = "dossier.contractMarket.intel.assessment";
        return new Row(getTextAt(RESOURCE_BUNDLE, labelKey), rowToolTip(labelKey), false, true, cells);
    }

    /** Each offer's characteristics, named in green, amber or red by whether they help or hurt. */
    private static Row characteristicsRow(List<AbstractContract> offers) {
        List<Cell> cells = new ArrayList<>();
        for (AbstractContract contract : offers) {
            List<ContractCharacteristic> ordered = new ArrayList<>(contract.getCharacteristics());
            ordered.sort(Comparator.comparing(ContractCharacteristic::getName));
            List<String> names = new ArrayList<>();
            List<String> descriptions = new ArrayList<>();
            List<String> plain = new ArrayList<>();
            for (ContractCharacteristic characteristic : ordered) {
                Color color = switch (characteristic.getPolarity()) {
                    case BENEFICIAL -> READY;
                    case ADVERSE -> DANGER;
                    case NEUTRAL -> AMBER;
                };
                names.add("<span style='color:" + hex(color) + "'>" + MarketHud.escape(characteristic.getName())
                                + "</span>");
                descriptions.add("<b>" + MarketHud.escape(characteristic.getName()) + "</b>: "
                                       + MarketHud.escape(characteristic.getToolTipText()));
                plain.add(characteristic.getName());
            }
            cells.add(listCell(names, descriptions, plain));
        }
        return new Row(getTextAt(RESOURCE_BUNDLE, "dossier.contractMarket.section.characteristics"), null, false,
              false, cells);
    }

    /** Each offer's badges: batchall, special type and special rules. */
    private static Row badgesRow(List<AbstractContract> offers) {
        List<Cell> cells = new ArrayList<>();
        for (AbstractContract contract : offers) {
            List<String> names = new ArrayList<>();
            List<String> descriptions = new ArrayList<>();
            List<String> plain = new ArrayList<>();
            for (MarketBadge badge : ContractMarketFormatting.badges(contract)) {
                names.add("<span style='color:" + hex(badge.color()) + "'>" + MarketHud.escape(badge.text())
                                + "</span>");
                descriptions.add("<b>" + MarketHud.escape(badge.text()) + "</b>: " + MarketHud.escape(badge.toolTip()));
                plain.add(badge.text());
            }
            cells.add(listCell(names, descriptions, plain));
        }
        return new Row(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.row.specialRules"), null, false, false,
              cells);
    }

    private static Cell listCell(List<String> names, List<String> descriptions, List<String> plain) {
        if (names.isEmpty()) {
            String none = getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.none");
            return new Cell(MarketHud.escape(none), "", TEXT_FAINT, false, -1, null, 0, null);
        }
        String separator = MarketHud.escape(getTextAt(RESOURCE_BUNDLE, "value.contractMarket.listSeparator"));
        String toolTip = "<html><div style='width:" + scaleForGUI(280) + "px'>" + String.join("<br>", descriptions)
                               + "</div></html>";
        return new Cell(String.join(separator, names), String.join(",", plain), null, false, -1, null, 0, toolTip);
    }

    /**
     * @return the best of the values that can be ranked, or {@code null} when the row has no better direction, fewer
     *       than two rankable values, or values that are all the same
     */
    // Package-private for unit tests
    static @Nullable Double bestValue(List<Double> numbers, Better better) {
        List<Double> rankable = numbers.stream().filter(Objects::nonNull).toList();
        if (better == Better.NEITHER || rankable.size() < 2
                  || rankable.stream().allMatch(value -> value.equals(rankable.getFirst()))) {
            return null;
        }
        return better == Better.HIGHER
                     ? rankable.stream().max(Double::compare).orElseThrow()
                     : rankable.stream().min(Double::compare).orElseThrow();
    }

    /**
     * Drops the rows whose values are the same for every compared offer, then any section left with no rows.
     */
    // Package-private for unit tests
    static List<Row> onlyDifferences(List<Row> all) {
        List<Row> kept = new ArrayList<>();
        Row pendingSection = null;
        for (Row row : all) {
            if (row.isSection()) {
                pendingSection = row;
                continue;
            }
            String first = row.cells().getFirst().key();
            if (row.cells().stream().allMatch(cell -> cell.key().equals(first))) {
                continue;
            }
            if (pendingSection != null) {
                kept.add(pendingSection);
                pendingSection = null;
            }
            kept.add(row);
        }
        return kept;
    }

    private static @Nullable String rowToolTip(String labelKey) {
        String toolTip = getTextAt(RESOURCE_BUNDLE, labelKey.replace("dossier.contractMarket.",
              "compare.contractMarket.tooltip.").replace("compare.contractMarket.row.",
              "compare.contractMarket.tooltip."));
        return isResourceKeyValid(toolTip)
                     ? MarketHud.wrapToolTip(toolTip) : null;
    }

    // endregion Rows

    // region Table models

    private final class CompareModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return columns.size();
        }

        @Override
        public String getColumnName(int column) {
            return marketName(columns.get(column));
        }

        @Override
        public Object getValueAt(int row, int column) {
            Row tableRow = rows.get(row);
            return tableRow.isSection() ? null : tableRow.cells().get(column);
        }
    }

    private final class LabelModel extends AbstractTableModel {
        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return rows.get(row);
        }
    }

    // endregion Table models

    // region Renderers

    /** Draws one cell: its value, a BEST mark when it leads its row, and a scaled bar or the difficulty pips. */
    private final class CellRenderer extends JPanel implements TableCellRenderer {
        private final JLabel value = new JLabel();
        private final JLabel best = new JLabel();
        private final Bar bar = new Bar();
        private final MarketHud.PipMeter pips = new MarketHud.PipMeter(0, ACCENT);
        private final JPanel south = Hud.transparentPanel(new BorderLayout());

        private CellRenderer() {
            super(new BorderLayout());
            setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), scaleForGUI(12), scaleForGUI(4),
                  scaleForGUI(12)));
            value.setFont(hudFont(Font.PLAIN, 0.9f, 0.0f));
            best.setText(getTextAt(RESOURCE_BUNDLE, "compare.contractMarket.best"));
            best.setForeground(READY);
            best.setFont(hudFont(Font.BOLD, 0.66f, 0.12f));
            JPanel line = Hud.transparentPanel(new BorderLayout(scaleForGUI(6), 0));
            line.add(value, BorderLayout.CENTER);
            line.add(best, BorderLayout.EAST);
            south.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(3), 0, 0, 0));
            add(line, BorderLayout.CENTER);
            add(south, BorderLayout.SOUTH);
        }

        @Override
        public Component getTableCellRendererComponent(JTable source, Object object, boolean isSelected,
              boolean hasFocus, int row, int column) {
            Row tableRow = rows.get(row);
            boolean isChosen = column < columns.size() && columns.get(column) == market.selected();
            if (tableRow.isSection()) {
                setBackground(GROUND);
                value.setText("");
                best.setVisible(false);
                south.removeAll();
                setToolTipText(null);
                return this;
            }

            Cell cell = (Cell) object;
            setBackground(isChosen ? SURFACE : tableRow.isKey() ? KEY_ROW : SURFACE_DEEP);
            value.setText("<html>" + cell.text() + "</html>");
            value.setForeground(cell.color() == null ? TEXT : cell.color());
            value.setFont(hudFont(cell.isBest() || tableRow.isKey() ? Font.BOLD : Font.PLAIN, 0.9f, 0.0f));
            best.setVisible(cell.isBest());

            south.removeAll();
            if (cell.pips() > 0) {
                pips.set(cell.pips(), cell.color() == null ? ACCENT : cell.color());
                south.add(pips, BorderLayout.WEST);
            } else if (cell.bar() >= 0) {
                bar.set(cell.bar(), cell.barColor());
                south.add(bar, BorderLayout.CENTER);
            }
            setToolTipText(cell.toolTip());
            return this;
        }
    }

    /** Draws a row label in the frozen first column, or a section heading in the HUD's tracked caps. */
    private static final class LabelRenderer extends JLabel implements TableCellRenderer {
        private LabelRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable source, Object object, boolean isSelected,
              boolean hasFocus, int row, int column) {
            Row tableRow = (Row) object;
            if (tableRow.isSection()) {
                setText(tableRow.label().toUpperCase(Locale.ROOT));
                setForeground(TEXT_FAINT);
                setFont(hudFont(Font.BOLD, 0.7f, 0.16f));
                setBackground(GROUND);
                setVerticalAlignment(SwingConstants.BOTTOM);
            } else {
                setText(tableRow.label());
                setForeground(tableRow.isKey() ? TEXT : TEXT_MUTED);
                setFont(hudFont(tableRow.isKey() ? Font.BOLD : Font.PLAIN, 0.88f, 0.0f));
                setBackground(tableRow.isKey() ? KEY_ROW : SURFACE_DEEP);
                setVerticalAlignment(SwingConstants.CENTER);
            }
            setToolTipText(tableRow.toolTip());
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, 0, scaleForGUI(1), BORDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(2), scaleForGUI(12), scaleForGUI(3), scaleForGUI(10))));
            return this;
        }
    }

    /**
     * Draws an offer's column heading: the difficulty ring and name, then employer, mission type and destination, and
     * whether the offer is the one selected for Accept. The selected offer's heading is raised with an accent rule.
     */
    private final class ColumnHeaderRenderer extends JPanel implements TableCellRenderer {
        private final JLabel name = new JLabel();
        private final JLabel sub = new JLabel();
        private final JLabel pick = new JLabel();

        private ColumnHeaderRenderer() {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            name.setFont(hudFont(Font.BOLD, 0.95f, 0.02f));
            name.setForeground(TEXT);
            name.setIconTextGap(scaleForGUI(7));
            sub.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
            sub.setForeground(TEXT_MUTED);
            sub.setIconTextGap(scaleForGUI(5));
            pick.setFont(hudFont(Font.BOLD, 0.64f, 0.14f));
            add(name);
            add(Box.createVerticalStrut(scaleForGUI(3)));
            add(sub);
            add(Box.createVerticalStrut(scaleForGUI(3)));
            add(pick);
        }

        @Override
        public Component getTableCellRendererComponent(JTable source, Object object, boolean isSelected,
              boolean hasFocus, int row, int column) {
            AbstractContract contract = columns.get(column);
            boolean isChosen = contract == market.selected();

            name.setText(marketName(contract));
            name.setIcon(new MarketHud.RingIcon(severityColor(assessmentDifficulty(contract))));
            List<String> parts = new ArrayList<>();
            parts.add(contract.getEmployerMarketDisplayName());
            if (!market.isContractTypeHidden()) {
                parts.add(objective(contract, false));
            }
            parts.add(destination(contract, market.currentDate()));
            sub.setText(String.join(getTextAt(RESOURCE_BUNDLE, "value.contractMarket.separator"), parts));
            sub.setIcon(new MarketHud.SwatchIcon(contract.getEmployerColor().getColour()));
            pick.setText(getTextAt(RESOURCE_BUNDLE, isChosen
                                                          ? "compare.contractMarket.picked"
                                                          : "compare.contractMarket.pick").toUpperCase(Locale.ROOT));
            pick.setForeground(isChosen ? ACCENT_BRIGHT : TEXT_FAINT);

            setOpaque(true);
            setBackground(isChosen ? SURFACE : GROUND);
            setBorder(BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(scaleForGUI(isChosen ? 2 : 0), 0, scaleForGUI(1), scaleForGUI(1),
                        isChosen ? ACCENT : BORDER),
                  BorderFactory.createEmptyBorder(scaleForGUI(isChosen ? 6 : 8), scaleForGUI(12), scaleForGUI(8),
                        scaleForGUI(12))));
            setToolTipText(ChaosContractMarketDialog.difficultyToolTip(contract));
            return this;
        }
    }

    /** A thin bar filled to a share of its width, as the money rows draw each offer against the row's largest. */
    private static final class Bar extends JComponent {
        private double share;
        private Color color = ACCENT;

        private Bar() {
            setPreferredSize(new Dimension(scaleForGUI(100), scaleForGUI(3)));
        }

        private void set(double share, Color color) {
            this.share = Math.clamp(share, 0.0, 1.0);
            this.color = color;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            graphics.setColor(SURFACE_HIGHLIGHT);
            graphics.fillRect(0, 0, getWidth(), getHeight());
            graphics.setColor(color);
            graphics.fillRect(0, 0, (int) Math.round(getWidth() * share), getHeight());
        }
    }

    // endregion Renderers

    private static Color blend(Color base, Color tint, float amount) {
        return new Color(Math.round(base.getRed() + (tint.getRed() - base.getRed()) * amount),
              Math.round(base.getGreen() + (tint.getGreen() - base.getGreen()) * amount),
              Math.round(base.getBlue() + (tint.getBlue() - base.getBlue()) * amount));
    }
}
