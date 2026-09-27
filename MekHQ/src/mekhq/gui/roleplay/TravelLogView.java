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
package mekhq.gui.roleplay;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.gui.roleplay.AskPage.column;
import static mekhq.gui.roleplay.OracleConsole.RESOURCE_BUNDLE;
import static mekhq.gui.roleplay.OracleConsole.formatDate;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import megamek.common.annotations.Nullable;
import mekhq.campaign.roleplay.JournalEntry;
import mekhq.campaign.roleplay.JournalEntryType;
import mekhq.campaign.roleplay.Roleplay;
import mekhq.campaign.roleplay.TravelLog;
import mekhq.campaign.roleplay.TravelLog.ContractInfo;
import mekhq.campaign.roleplay.TravelLog.Observation;
import mekhq.campaign.roleplay.TravelLog.Snapshot;
import mekhq.campaign.roleplay.TravelLog.SystemSummary;
import mekhq.campaign.roleplay.TravelRecord;
import mekhq.campaign.roleplay.TravelRecord.Kind;
import mekhq.campaign.roleplay.TravelRecord.Party;
import mekhq.campaign.roleplay.TravelSnapshots;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudSegmentedControl;
import mekhq.gui.baseComponents.hud.HudSegmentedControl.Segment;

/**
 * The Journal page's travel log: a sortable table of every system the company has been to, with what happened in
 * each, and a timeline of every departure and arrival that can be narrowed to the main force, a base or a cast
 * member.
 */
class TravelLogView {
    private enum Mode {
        SYSTEMS,
        TIMELINE
    }

    /** A timeline filter choice: everything, the main force, one base, or one cast member. */
    private record Who(@Nullable Party party, @Nullable UUID id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private final OracleConsole console;
    private final JPanel root = new JPanel(new BorderLayout());
    private final CardLayout cards = new CardLayout();
    private final JPanel cardHolder = new JPanel(cards);
    private final HudSegmentedControl<Mode> modeControl;
    private final JComboBox<Who> whoFilter = new JComboBox<>();
    private final JPanel filterRow;
    private final JLabel intro = Hud.hint("");
    private boolean updating;

    private final SystemModel systemModel = new SystemModel();
    private final JTable systemTable = new JTable(systemModel);
    private final RowModel detailModel = new RowModel();
    private final JTable detailTable = new JTable(detailModel);
    private final JLabel detailTitle = new JLabel();
    private final JLabel detailSub = Hud.hint("");
    private final JLabel detailEmpty = Hud.notice("");
    private final RowModel timelineModel = new RowModel();
    private final JTable timelineTable = new JTable(timelineModel);

    private List<ContractInfo> contracts = List.of();
    private String selectedSystem;

    TravelLogView(final OracleConsole console) {
        this.console = console;
        root.setOpaque(false);

        modeControl = new HudSegmentedControl<>(this::setMode);
        modeControl.setColumns(Mode.values().length);
        List<Segment<Mode>> segments = new ArrayList<>();
        for (Mode mode : Mode.values()) {
            segments.add(new Segment<>(mode, text("TravelLog.mode." + mode.name()), "", true));
        }
        modeControl.setSegments(segments);

        Hud.styleComboBox(whoFilter);
        whoFilter.setToolTipText(text("TravelLog.who.toolTipText"));
        whoFilter.addActionListener(event -> {
            if (!updating) {
                refreshTimeline();
            }
        });
        filterRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        filterRow.add(Hud.hint(text("TravelLog.who")), BorderLayout.WEST);
        filterRow.add(whoFilter, BorderLayout.CENTER);

        JPanel head = column();
        head.setBorder(BorderFactory.createEmptyBorder(0, 0, scaleForGUI(10), 0));
        JPanel modeRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(12), 0));
        JPanel modeHolder = Hud.transparentPanel(new BorderLayout());
        modeHolder.add(modeControl, BorderLayout.WEST);
        modeRow.add(modeHolder, BorderLayout.WEST);
        modeRow.add(filterRow, BorderLayout.EAST);
        head.add(leftAligned(modeRow));
        head.add(Box.createVerticalStrut(scaleForGUI(6)));
        head.add(leftAligned(intro));

        // By system: the table on the left, the chosen system's history on the right.
        styleSortable(systemTable, true);
        systemTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && !updating) {
                int row = systemTable.getSelectedRow();
                selectedSystem = (row < 0) ? null
                                       : systemModel.rows.get(systemTable.convertRowIndexToModel(row)).systemId();
                refreshDetail();
            }
        });
        styleSortable(detailTable, false);
        // Every row is in the chosen system, so the system column would only repeat it.
        detailTable.removeColumn(detailTable.getColumnModel().getColumn(3));
        detailTitle.setForeground(TEXT);
        detailTitle.setFont(hudFont(java.awt.Font.BOLD, 1.15f, 0.04f));
        JPanel detailHead = column();
        detailHead.add(leftAligned(detailTitle));
        detailHead.add(Box.createVerticalStrut(scaleForGUI(3)));
        detailHead.add(leftAligned(detailSub));
        detailHead.add(Box.createVerticalStrut(scaleForGUI(8)));
        detailHead.add(leftAligned(detailEmpty));
        JPanel detail = new JPanel(new BorderLayout());
        detail.setOpaque(true);
        detail.setBackground(SURFACE_DEEP);
        detail.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, scaleForGUI(1), 0, 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(12), scaleForGUI(14), scaleForGUI(12), scaleForGUI(14))));
        detail.add(detailHead, BorderLayout.NORTH);
        detail.add(tableScroll(detailTable), BorderLayout.CENTER);

        JScrollPane systemScroll = tableScroll(systemTable);
        systemScroll.setPreferredSize(new Dimension(scaleForGUI(620), 0));
        JPanel systems = new JPanel(new BorderLayout());
        systems.setOpaque(false);
        systems.add(systemScroll, BorderLayout.WEST);
        systems.add(detail, BorderLayout.CENTER);

        styleSortable(timelineTable, false);
        cardHolder.setOpaque(false);
        cardHolder.add(systems, Mode.SYSTEMS.name());
        cardHolder.add(tableScroll(timelineTable), Mode.TIMELINE.name());

        root.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(14), scaleForGUI(16), scaleForGUI(14),
              scaleForGUI(16)));
        root.add(head, BorderLayout.NORTH);
        root.add(cardHolder, BorderLayout.CENTER);

        modeControl.setSelected(Mode.SYSTEMS);
        setMode(Mode.SYSTEMS);
    }

    JComponent getComponent() {
        return root;
    }

    private void setMode(final Mode mode) {
        cards.show(cardHolder, mode.name());
        filterRow.setVisible(mode == Mode.TIMELINE);
        intro.setText(text("TravelLog.intro." + mode.name()));
    }

    // region Refresh

    void refresh() {
        TravelLog log = console.roleplay().getTravelLog();
        contracts = TravelSnapshots.contracts(console.campaign());
        LocalDate today = console.campaign().getLocalDate();
        Map<String, String> presentNow = describePresent(TravelSnapshots.capture(console.campaign()));

        updating = true;
        try {
            List<SystemRow> rows = new ArrayList<>();
            for (SystemSummary summary : log.summarize(contracts, today)) {
                rows.add(new SystemRow(summary, presentNow.getOrDefault(summary.systemId(), "")));
            }
            systemModel.setRows(rows);
            int restore = -1;
            for (int i = 0; i < rows.size(); i++) {
                if (rows.get(i).systemId().equals(selectedSystem)) {
                    restore = systemTable.convertRowIndexToView(i);
                }
            }
            if (restore >= 0) {
                systemTable.setRowSelectionInterval(restore, restore);
            } else {
                systemTable.clearSelection();
                selectedSystem = null;
            }
            refreshWhoChoices(log);
        } finally {
            updating = false;
        }
        refreshDetail();
        refreshTimeline();
    }

    private Map<String, String> describePresent(final Snapshot snapshot) {
        Map<String, List<String>> present = new LinkedHashMap<>();
        Observation force = snapshot.force();
        if (force != null && !force.travelling() && force.systemId() != null) {
            present.computeIfAbsent(force.systemId(), key -> new ArrayList<>()).add(text("TravelLog.mainForce"));
        }
        for (Observation base : snapshot.bases()) {
            if (base.systemId() != null) {
                present.computeIfAbsent(base.systemId(), key -> new ArrayList<>()).add(base.name());
            }
        }
        for (Observation member : snapshot.castAway()) {
            if (!member.travelling() && member.systemId() != null) {
                present.computeIfAbsent(member.systemId(), key -> new ArrayList<>()).add(member.name());
            }
        }
        Map<String, String> joined = new LinkedHashMap<>();
        present.forEach((system, names) -> joined.put(system, String.join(", ", names)));
        return joined;
    }

    private void refreshDetail() {
        TravelLog log = console.roleplay().getTravelLog();
        SystemRow row = systemModel.rows.stream()
                              .filter(candidate -> candidate.systemId().equals(selectedSystem))
                              .findFirst()
                              .orElse(null);
        detailTable.setVisible(row != null);
        if (row == null) {
            detailTitle.setText("");
            detailSub.setText("");
            detailEmpty.setText(text(systemModel.rows.isEmpty() ? "TravelLog.empty" : "TravelLog.pickSystem"));
            detailEmpty.setVisible(true);
            detailModel.setRows(List.of());
            return;
        }
        detailEmpty.setVisible(false);
        SystemSummary summary = row.summary();
        detailTitle.setText(summary.systemName().toUpperCase(Locale.ROOT));
        detailSub.setText(getFormattedTextAt(RESOURCE_BUNDLE, "TravelLog.detail.sub", summary.visits(),
              summary.daysPresent(), summary.contracts()));

        List<Row> rows = new ArrayList<>();
        for (TravelRecord record : log.getTimelineFor(summary.systemId(), contracts)) {
            rows.add(new Row(record.getDate(), who(record), describe(record), ""));
        }
        // Campaign events the chronicle wrote while the main force was here.
        List<LocalDate[]> stays = log.getStays(summary.systemId(), console.campaign().getLocalDate());
        for (JournalEntry entry : console.roleplay().getOracleLog()) {
            if (entry.getType() == JournalEntryType.CHRONICLE && during(entry.getDate(), stays)) {
                rows.add(new Row(entry.getDate(), text("TravelLog.campaign"), firstLine(entry.getText()), ""));
            }
        }
        rows.sort((a, b) -> b.date().compareTo(a.date()));
        detailModel.setRows(rows);
    }

    private static boolean during(final LocalDate date, final List<LocalDate[]> stays) {
        for (LocalDate[] stay : stays) {
            if (!date.isBefore(stay[0]) && !date.isAfter(stay[1])) {
                return true;
            }
        }
        return false;
    }

    private void refreshWhoChoices(final TravelLog log) {
        Who selected = (Who) whoFilter.getSelectedItem();
        DefaultComboBoxModel<Who> model = new DefaultComboBoxModel<>();
        model.addElement(new Who(null, null, text("TravelLog.who.all")));
        model.addElement(new Who(Party.MAIN_FORCE, null, text("TravelLog.mainForce")));
        Map<UUID, String> bases = new LinkedHashMap<>();
        Map<UUID, String> cast = new LinkedHashMap<>();
        for (TravelRecord record : log.getRecords()) {
            if (record.getParty() == Party.BASE && record.getPartyId() != null) {
                bases.put(record.getPartyId(), record.getPartyName());
            }
            for (UUID id : record.getCharacters()) {
                String name = console.roleplay().getCharacterName(id);
                cast.put(id, name == null ? record.getPartyName() : name);
            }
        }
        bases.forEach((id, name) -> model.addElement(new Who(Party.BASE, id,
              getFormattedTextAt(RESOURCE_BUNDLE, "TravelLog.base", name))));
        cast.forEach((id, name) -> model.addElement(new Who(Party.CAST, id, name)));
        whoFilter.setModel(model);
        for (int i = 0; i < model.getSize(); i++) {
            Who choice = model.getElementAt(i);
            if (selected != null && choice.party() == selected.party() && Objects.equals(choice.id(),
                  selected.id())) {
                whoFilter.setSelectedIndex(i);
            }
        }
    }

    private void refreshTimeline() {
        Who who = (Who) whoFilter.getSelectedItem();
        List<Row> rows = new ArrayList<>();
        for (TravelRecord record : console.roleplay().getTravelLog().getTimeline(contracts)) {
            if (who == null || who.party() == null || matches(record, who)) {
                rows.add(new Row(record.getDate(), who(record), describe(record),
                      record.getSystemName()));
            }
        }
        Collections.reverse(rows);
        timelineModel.setRows(rows);
    }

    private static boolean matches(final TravelRecord record, final Who who) {
        if (!record.isMovement()) {
            // Contracts belong to the main force.
            return who.party() == Party.MAIN_FORCE;
        }
        return record.concerns(who.party(), who.id());
    }

    // endregion Refresh

    // region Text

    private String who(final TravelRecord record) {
        if (!record.isMovement()) {
            return text("TravelLog.contract");
        }
        return switch (record.getParty()) {
            case MAIN_FORCE -> text("TravelLog.mainForce");
            case BASE -> getFormattedTextAt(RESOURCE_BUNDLE, "TravelLog.base", record.getPartyName());
            case CAST -> castNames(console.roleplay(), record);
        };
    }

    private static String castNames(final Roleplay roleplay, final TravelRecord record) {
        List<String> names = new ArrayList<>();
        for (UUID id : record.getCharacters()) {
            String name = roleplay.getCharacterName(id);
            if (name != null) {
                names.add(name);
            }
        }
        return names.isEmpty() ? record.getPartyName() : String.join(", ", names);
    }

    /**
     * @return a one-line description of a travel record, such as "Departed Galax for Tharkad"
     */
    static String describe(final TravelRecord record) {
        String other = record.getOtherSystemName();
        String key = "TravelLog.kind." + record.getKind().name();
        if ((record.getKind() == Kind.DEPARTED || record.getKind() == Kind.ARRIVED) && other == null) {
            key += ".noOther";
        }
        String text = getFormattedTextAt(RESOURCE_BUNDLE, key, record.getSystemName(),
              other == null ? "" : other, record.getPartyName());
        return record.isReconstructed() ? getFormattedTextAt(RESOURCE_BUNDLE, "TravelLog.reconstructed", text) : text;
    }

    private static String firstLine(final String text) {
        int newline = text.indexOf('\n');
        return (newline < 0) ? text : text.substring(0, newline);
    }

    private static String text(final String key) {
        return getTextAt(RESOURCE_BUNDLE, key);
    }

    // endregion Text

    // region Tables

    private static JScrollPane tableScroll(final JTable table) {
        JScrollPane scroll = new JScrollPane(table);
        Hud.styleScroll(scroll, SURFACE_DEEP, true);
        return scroll;
    }

    private static void styleSortable(final JTable table, final boolean selectable) {
        Hud.styleTable(table);
        table.setAutoCreateRowSorter(true);
        if (table.getRowSorter() instanceof TableRowSorter<?> sorter) {
            sorter.setSortsOnUpdates(true);
        }
        table.setRowSelectionAllowed(selectable);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFocusable(selectable);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(final JTable source, final Object value,
                  final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                Object shown = (value instanceof LocalDate date) ? formatDate(date) : value;
                super.getTableCellRendererComponent(source, shown, false, false, row, column);
                setOpaque(true);
                setBackground(isSelected ? SURFACE : SURFACE_DEEP);
                setForeground(isSelected || column == 0 ? TEXT : TEXT_MUTED);
                setHorizontalAlignment((value instanceof Number) ? RIGHT : LEFT);
                setBorder(BorderFactory.createEmptyBorder(0, scaleForGUI(10), 0, scaleForGUI(10)));
                return this;
            }
        });
        table.setDefaultRenderer(Number.class, table.getDefaultRenderer(Object.class));
        table.setDefaultRenderer(LocalDate.class, table.getDefaultRenderer(Object.class));
    }

    private record SystemRow(SystemSummary summary, String present) {
        String systemId() {
            return summary.systemId();
        }
    }

    private static final class SystemModel extends AbstractTableModel {
        private static final String[] COLUMNS = { "system", "first", "last", "visits", "days", "contracts",
                                                  "present" };
        private List<SystemRow> rows = List.of();

        void setRows(final List<SystemRow> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(final int column) {
            return text("TravelLog.column." + COLUMNS[column]);
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return switch (column) {
                case 1, 2 -> LocalDate.class;
                case 3, 5 -> Integer.class;
                case 4 -> Long.class;
                default -> String.class;
            };
        }

        @Override
        public Object getValueAt(final int row, final int column) {
            SystemSummary summary = rows.get(row).summary();
            return switch (column) {
                case 0 -> summary.systemName();
                case 1 -> summary.firstVisit();
                case 2 -> summary.lastVisit();
                case 3 -> summary.visits();
                case 4 -> summary.daysPresent();
                case 5 -> summary.contracts();
                default -> rows.get(row).present();
            };
        }
    }

    private record Row(LocalDate date, String who, String what, String system) {}

    private static final class RowModel extends AbstractTableModel {
        private static final String[] COLUMNS = { "date", "who", "what", "system" };
        private List<Row> rows = List.of();

        void setRows(final List<Row> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(final int column) {
            return text("TravelLog.column." + COLUMNS[column]);
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return column == 0 ? LocalDate.class : String.class;
        }

        @Override
        public Object getValueAt(final int row, final int column) {
            Row value = rows.get(row);
            return switch (column) {
                case 0 -> value.date();
                case 1 -> value.who();
                case 2 -> value.what();
                default -> value.system();
            };
        }
    }

    // endregion Tables
}
