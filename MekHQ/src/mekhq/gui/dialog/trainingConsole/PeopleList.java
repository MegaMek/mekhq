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
package mekhq.gui.dialog.trainingConsole;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.*;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.swing.*;
import javax.swing.border.Border;

import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudCheckBox;
import mekhq.gui.baseComponents.hud.HudLinkList;
import mekhq.gui.dialog.trainingConsole.TrainingConsoleState.Sort;
import mekhq.gui.dialog.trainingConsole.TrainingRoster.PersonEntry;

/**
 * The Training Console's people list: a search field, a count line, sortable column headers, and one row per person
 * with five columns: name, role, formation, experience and available XP.
 *
 * <p>The list keeps no selection of its own. A click or Space on a row is reported to the console, which keeps the
 * selection and redraws. Only the rows on screen are drawn, so the list holds thousands of people without slowing
 * down.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
final class PeopleList extends JPanel {
    private static final String RESOURCE_BUNDLE = TrainingConsole.RESOURCE_BUNDLE;
    private static final int ROW_HEIGHT = 32;
    /** How the columns share the width: name, role, formation, experience, XP. */
    private static final double[] COLUMN_WEIGHTS = { 0.3, 0.19, 0.3, 0.13, 0.08 };

    /** Reports a click or Space on a row. */
    @FunctionalInterface
    interface RowAction {
        /**
         * @param entry   the row
         * @param index   its position
         * @param isRange {@code true} for a shift-click, which selects every row from the last one clicked
         */
        void act(PersonEntry entry, int index, boolean isRange);
    }

    private static final class RowModel extends AbstractListModel<PersonEntry> {
        private List<PersonEntry> rows = List.of();

        void setRows(List<PersonEntry> rows) {
            int oldSize = this.rows.size();
            this.rows = List.copyOf(rows);
            if (oldSize > 0) {
                fireIntervalRemoved(this, 0, oldSize - 1);
            }
            if (!this.rows.isEmpty()) {
                fireIntervalAdded(this, 0, this.rows.size() - 1);
            }
        }

        @Override
        public int getSize() {
            return rows.size();
        }

        @Override
        public PersonEntry getElementAt(int index) {
            return rows.get(index);
        }
    }

    private final RowModel model = new RowModel();
    private final HudLinkList<PersonEntry> list = new HudLinkList<>(model);
    private final JTextField search = new JTextField();
    private final JLabel count = new JLabel();
    private final JLabel selectAll;
    private final JPanel header = new JPanel(new GridBagLayout());
    private final Consumer<Sort> onSort;

    /**
     * @param onSearch      called with the search text whenever it changes
     * @param onRow         called when the player clicks a row or presses Space on it
     * @param onSelectShown called when the player selects everyone shown
     * @param onClear       called when the player clears the selection
     * @param onSort        called with a column when the player clicks its header
     * @param isSelected    whether a person is selected
     * @param pendingCount  how many purchases are staged for a person
     * @param availableXp   a person's XP once everything staged for them is paid for
     */
    PeopleList(Consumer<String> onSearch, RowAction onRow, Runnable onSelectShown, Runnable onClear,
          Consumer<Sort> onSort, Predicate<Person> isSelected, Function<Person, Integer> pendingCount,
          Function<Person, Integer> availableXp) {
        super(new BorderLayout());
        this.onSort = onSort;
        setOpaque(true);
        setBackground(SURFACE_DEEP);

        Hud.styleField(search);
        search.getAccessibleContext().setAccessibleName(getTextAt(RESOURCE_BUNDLE, "people.search"));
        ConsoleWidgets.onTextSettled(search, onSearch);
        JPanel searchRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
        searchRow.add(Hud.eyebrow(getTextAt(RESOURCE_BUNDLE, "people.search")), BorderLayout.WEST);
        searchRow.add(search, BorderLayout.CENTER);
        searchRow.setAlignmentX(LEFT_ALIGNMENT);

        count.setForeground(TEXT_FAINT);
        count.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        selectAll = Hud.link("", onSelectShown);
        JPanel links = Hud.transparentPanel(null);
        links.setLayout(new BoxLayout(links, BoxLayout.X_AXIS));
        links.add(selectAll);
        links.add(Box.createHorizontalStrut(scaleForGUI(14)));
        links.add(Hud.link(getTextAt(RESOURCE_BUNDLE, "people.clear"), onClear));
        JPanel countRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(10), 0));
        countRow.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), 0, 0, 0));
        countRow.add(count, BorderLayout.CENTER);
        countRow.add(links, BorderLayout.EAST);
        countRow.setAlignmentX(LEFT_ALIGNMENT);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));
        head.add(searchRow);
        head.add(countRow);

        header.setOpaque(true);
        header.setBackground(SURFACE_DEEP);
        header.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
              // The same left and right padding as a row: its 2-pixel selection bar, then 8 pixels, and 10 on the right
              BorderFactory.createEmptyBorder(scaleForGUI(6), scaleForGUI(10), scaleForGUI(6), scaleForGUI(10))));
        setSortShown(Sort.SELECTED, true);

        list.setCellRenderer(new PersonRowRenderer(isSelected, pendingCount, availableXp));
        list.setFixedCellHeight(scaleForGUI(ROW_HEIGHT));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) {
                    return;
                }
                int index = list.locationToIndex(event.getPoint());
                if ((index >= 0) && list.getCellBounds(index, index).contains(event.getPoint())) {
                    onRow.act(model.getElementAt(index), index, event.isShiftDown());
                }
            }
        });
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "toggleRow");
        list.getActionMap().put("toggleRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int index = list.getSelectedIndex();
                if (index >= 0) {
                    onRow.act(model.getElementAt(index), index, false);
                }
            }
        });
        // Ctrl+A (Cmd+A on a Mac) selects everyone shown, as it would in any list
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_A,
              java.awt.Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "selectShown");
        list.getActionMap().put("selectShown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                onSelectShown.run();
            }
        });

        JScrollPane scroll = new JScrollPane(list);
        Hud.styleScroll(scroll, SURFACE_DEEP, false);
        // As the scroll pane's column header, the headers are exactly as wide as the rows, scroll bar or not, so the
        // columns line up
        scroll.setColumnHeaderView(header);
        scroll.getColumnHeader().setBackground(SURFACE_DEEP);

        add(head, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /** Draws the column headers, marking the sorted one. */
    void setSortShown(Sort sort, boolean isAscending) {
        header.removeAll();
        // The checkbox column's header is exactly as wide as a row's selection box, so each header starts where its
        // column does; it sorts the selection to the top
        int boxWidth = new HudCheckBox("").getPreferredSize().width;
        JLabel selectedHeader = new JLabel((sort == Sort.SELECTED) ? (isAscending ? "\u25B2" : "\u25BC") : "\u2713");
        selectedHeader.setFont(hudFont(Font.BOLD, 0.72f, 0.0f));
        selectedHeader.setForeground((sort == Sort.SELECTED) ? ACCENT : TEXT_FAINT);
        selectedHeader.setHorizontalAlignment(SwingConstants.CENTER);
        selectedHeader.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        selectedHeader.setToolTipText(getTextAt(RESOURCE_BUNDLE, "people.column.SELECTED.toolTipText"));
        Dimension boxSize = new Dimension(boxWidth, selectedHeader.getPreferredSize().height);
        selectedHeader.setPreferredSize(boxSize);
        selectedHeader.setMinimumSize(boxSize);
        selectedHeader.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent event) {
                onSort.accept(Sort.SELECTED);
            }
        });
        header.add(selectedHeader, constraints(0, 0));
        Sort[] columns = { Sort.NAME, Sort.ROLE, Sort.FORMATION, Sort.EXPERIENCE, Sort.XP };
        for (int index = 0; index < columns.length; index++) {
            Sort column = columns[index];
            String text = getTextAt(RESOURCE_BUNDLE, "people.column." + column.name()).toUpperCase(Locale.ROOT);
            if (column == sort) {
                text += isAscending ? "  ▲" : "  ▼";
            }
            JLabel label = new JLabel(text);
            label.setFont(hudFont(Font.BOLD, 0.72f, 0.12f));
            label.setForeground((column == sort) ? ACCENT : TEXT_FAINT);
            label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            label.setToolTipText(getTextAt(RESOURCE_BUNDLE, "people.column.toolTipText"));
            fixWidth(label);
            label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent event) {
                    onSort.accept(column);
                }
            });
            header.add(label, constraints(index + 1, COLUMN_WEIGHTS[index]));
        }
        header.revalidate();
        header.repaint();
    }

    /** Replaces the rows, keeping the scroll position. */
    void setRows(List<PersonEntry> rows) {
        model.setRows(rows);
    }

    /** @return the rows shown, in order */
    List<PersonEntry> rows() {
        List<PersonEntry> rows = new ArrayList<>(model.getSize());
        for (int index = 0; index < model.getSize(); index++) {
            rows.add(model.getElementAt(index));
        }
        return rows;
    }

    /** Redraws the rows without changing them, after the selection or the plan has changed. */
    void redraw() {
        list.repaint();
    }

    void setCount(int shown, int selected, int hidden) {
        count.setText(hidden > 0 ?
                            getFormattedTextAt(RESOURCE_BUNDLE, "people.count.hidden", shown, selected, hidden) :
                            getFormattedTextAt(RESOURCE_BUNDLE, "people.count", shown, selected));
        selectAll.setText(getFormattedTextAt(RESOURCE_BUNDLE, "people.selectAll", shown));
    }

    private static GridBagConstraints constraints(int column, double weight) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = column;
        constraints.gridy = 0;
        constraints.weightx = weight;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 0, scaleForGUI(10));
        return constraints;
    }

    /**
     * Lets the grid share the width by weight alone, so a long name is shortened instead of widening its column. The
     * height comes from the font as well as the text, as a row's cells are still empty when the renderer is built.
     */
    private static void fixWidth(JComponent component) {
        int height = Math.max(component.getPreferredSize().height,
              component.getFontMetrics(component.getFont()).getHeight());
        component.setPreferredSize(new Dimension(0, height));
        component.setMinimumSize(new Dimension(0, height));
    }

    /** A person row: the selection box, then name, role, formation, experience and available XP. */
    private static final class PersonRowRenderer extends JPanel implements ListCellRenderer<PersonEntry> {
        private static final Font NAME_FONT = hudFont(Font.BOLD, 0.92f, 0.0f);
        private static final Font CELL_FONT = hudFont(Font.PLAIN, 0.86f, 0.0f);

        private final Predicate<Person> isSelected;
        private final Function<Person, Integer> pendingCount;
        private final Function<Person, Integer> availableXp;
        private final HudCheckBox checkBox = new HudCheckBox("");
        private final JLabel name = cell(NAME_FONT, TEXT);
        private final JLabel role = cell(CELL_FONT, TEXT_MUTED);
        private final JLabel formation = cell(CELL_FONT, TEXT_MUTED);
        private final JLabel experience = cell(CELL_FONT, TEXT);
        private final JLabel xp = cell(CELL_FONT, TEXT);
        /** The two row borders, made once rather than on every paint: a selected row's, and anyone else's. */
        private final Border selectedBorder = rowBorder(true);
        private final Border unselectedBorder = rowBorder(false);

        PersonRowRenderer(Predicate<Person> isSelected, Function<Person, Integer> pendingCount,
              Function<Person, Integer> availableXp) {
            super(new GridBagLayout());
            this.isSelected = isSelected;
            this.pendingCount = pendingCount;
            this.availableXp = availableXp;
            setOpaque(true);
            checkBox.setOpaque(false);
            add(checkBox, constraints(0, 0));
            JLabel[] cells = { name, role, formation, experience, xp };
            for (int index = 0; index < cells.length; index++) {
                fixWidth(cells[index]);
                add(cells[index], constraints(index + 1, COLUMN_WEIGHTS[index]));
            }
        }

        private static Border rowBorder(boolean selected) {
            return BorderFactory.createCompoundBorder(
                  BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, DIVIDER),
                  BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, scaleForGUI(2), 0, 0, selected ? ACCENT : SURFACE_DEEP),
                        BorderFactory.createEmptyBorder(0, scaleForGUI(8), 0, scaleForGUI(10))));
        }

        private static JLabel cell(Font font, Color color) {
            JLabel label = new JLabel();
            label.setFont(font);
            label.setForeground(color);
            return label;
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends PersonEntry> list, PersonEntry entry,
              int index, boolean selectedInList, boolean cellHasFocus) {
            Person person = entry.person();
            boolean selected = isSelected.test(person);
            setBackground(selected ? SURFACE : SURFACE_DEEP);
            setBorder(selected ? selectedBorder : unselectedBorder);
            checkBox.setSelected(selected);

            name.setText(entry.title());

            role.setText(entry.isPrisoner() ?
                               getFormattedTextAt(RESOURCE_BUNDLE, "people.prisoner", entry.roleName()) :
                               entry.roleName());
            formation.setText(entry.formationName().isEmpty() ? getTextAt(RESOURCE_BUNDLE, "people.noFormation") :
                                    entry.formationName());
            formation.setForeground(entry.formationName().isEmpty() ? TEXT_FAINT : TEXT_MUTED);
            formation.setToolTipText(entry.formationName().isEmpty() ? null : entry.formationName());

            int pending = pendingCount.apply(person);
            String level = entry.skillLevel().toString();
            if (pending > 0) {
                experience.setText(getFormattedTextAt(RESOURCE_BUNDLE, "people.pending", level, pending));
                experience.setForeground(AMBER);
            } else {
                experience.setText(level);
                experience.setForeground(ConsoleWidgets.levelColor(entry.skillLevel()));
            }

            // XP left once everything staged for them is paid for, amber when staging has spent some of it
            int available = availableXp.apply(person);
            xp.setText(TrainingConsole.formatCount(available));
            xp.setForeground((pending > 0) ? AMBER : ((available > 0) ? TEXT : TEXT_FAINT));
            xp.setToolTipText((pending > 0) ?
                                    getFormattedTextAt(RESOURCE_BUNDLE, "people.xp.staged", person.getXP()) : null);
            return this;
        }
    }
}
