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
package mekhq.gui.dialog.crewAssignment;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static mekhq.gui.baseComponents.hud.HudStyle.BORDER;
import static mekhq.gui.baseComponents.hud.HudStyle.SURFACE_DEEP;
import static mekhq.gui.baseComponents.hud.HudStyle.TEXT_FAINT;
import static mekhq.gui.baseComponents.hud.HudStyle.hudFont;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import jakarta.annotation.Nullable;
import mekhq.gui.baseComponents.hud.Hud;
import mekhq.gui.baseComponents.hud.HudLinkList;
import mekhq.gui.baseComponents.hud.HudStyle;

/**
 * One list page of the {@link CrewAssignmentDialog}: an optional search field, a row of filter chips, a count line,
 * and the list itself.
 *
 * <p>The page holds no selection of its own. A click or Space on a row is reported to the dialog, which keeps the
 * selection in {@link CrewAssignmentState} and redraws. The list's own selection only marks the row the keyboard is
 * on. Only the rows on screen are drawn, so a page holds thousands of rows without slowing down.</p>
 *
 * @param <T> the type of row
 *
 * @author Illiani
 * @since 0.51.01
 */
final class CrewListPage<T> extends JPanel {
    /** Reports a click or Space on a row. */
    @FunctionalInterface
    interface RowAction<T> {
        /**
         * @param item    the row's item
         * @param index   the row's position in the list
         * @param isRange {@code true} for a shift-click, which selects every row from the last one clicked
         */
        void act(T item, int index, boolean isRange);
    }

    /** A list model that replaces all its rows at once. */
    private static final class RowModel<T> extends AbstractListModel<T> {
        private List<T> rows = List.of();

        void setRows(List<T> rows) {
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
        public T getElementAt(int index) {
            return rows.get(index);
        }
    }

    private final RowModel<T> model = new RowModel<>();
    private final HudLinkList<T> list = new HudLinkList<>(model);
    private final JPanel chips = Hud.transparentPanel(new FlowLayout(FlowLayout.LEFT, scaleForGUI(6), 0));
    private final JLabel count = new JLabel();
    private final @Nullable JTextField search;

    /**
     * @param searchLabel the search field's caption, or {@code null} for a page without one
     * @param onSearch    called with the search text whenever it changes; ignored without a search field
     * @param renderer    draws each row
     * @param rowHeight   the height of every row, before GUI scaling
     * @param onRow       called when the player clicks a row or presses Space on it
     */
    CrewListPage(@Nullable String searchLabel, Consumer<String> onSearch, ListCellRenderer<T> renderer,
          int rowHeight, RowAction<T> onRow) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(SURFACE_DEEP);

        JPanel head = new JPanel();
        head.setOpaque(false);
        head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
        head.setBorder(BorderFactory.createCompoundBorder(
              BorderFactory.createMatteBorder(0, 0, scaleForGUI(1), 0, BORDER),
              BorderFactory.createEmptyBorder(scaleForGUI(10), scaleForGUI(12), scaleForGUI(8), scaleForGUI(12))));

        if (searchLabel != null) {
            search = new JTextField();
            Hud.styleField(search);
            search.getAccessibleContext().setAccessibleName(searchLabel);
            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent event) {
                    onSearch.accept(search.getText());
                }

                @Override
                public void removeUpdate(DocumentEvent event) {
                    onSearch.accept(search.getText());
                }

                @Override
                public void changedUpdate(DocumentEvent event) {
                    onSearch.accept(search.getText());
                }
            });
            JPanel searchRow = Hud.transparentPanel(new BorderLayout(scaleForGUI(8), 0));
            searchRow.add(Hud.eyebrow(searchLabel), BorderLayout.WEST);
            searchRow.add(search, BorderLayout.CENTER);
            searchRow.setAlignmentX(LEFT_ALIGNMENT);
            head.add(searchRow);
            head.add(Box.createVerticalStrut(scaleForGUI(8)));
        } else {
            search = null;
        }

        chips.setAlignmentX(LEFT_ALIGNMENT);
        head.add(chips);
        count.setForeground(TEXT_FAINT);
        count.setFont(hudFont(Font.PLAIN, 0.82f, 0.0f));
        count.setBorder(BorderFactory.createEmptyBorder(scaleForGUI(6), 0, 0, 0));
        count.setAlignmentX(LEFT_ALIGNMENT);
        head.add(count);

        list.setCellRenderer(renderer);
        list.setFixedCellHeight(scaleForGUI(rowHeight));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setBackground(SURFACE_DEEP);
        list.setOpaque(true);
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

        JScrollPane scroll = new JScrollPane(list);
        Hud.styleScroll(scroll, HudStyle.SURFACE_DEEP, false);

        add(head, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    /** Replaces the rows, keeping the scroll position. */
    void setRows(List<T> rows) {
        model.setRows(rows);
    }

    /** Replaces what draws each row, for example after the plan the rows are drawn from has been replaced. */
    void setRenderer(ListCellRenderer<T> renderer) {
        list.setCellRenderer(renderer);
    }

    /** @return the rows shown, in order */
    List<T> rows() {
        List<T> rows = new ArrayList<>(model.getSize());
        for (int index = 0; index < model.getSize(); index++) {
            rows.add(model.getElementAt(index));
        }
        return rows;
    }

    /** Scrolls to a row, if it is shown, and puts the keyboard on it. */
    void reveal(T item) {
        int index = rows().indexOf(item);
        if (index >= 0) {
            list.setSelectedIndex(index);
            list.ensureIndexIsVisible(index);
        }
    }

    /** Redraws the rows without changing them, after a selection or the plan has changed. */
    void redraw() {
        list.repaint();
    }

    void setLinkHandler(HudLinkList.LinkHandler<T> handler) {
        list.setLinkHandler(handler);
    }

    /** Replaces the chips above the list. */
    void setChips(List<? extends JComponent> components) {
        chips.removeAll();
        components.forEach(chips::add);
        chips.setVisible(!components.isEmpty());
        chips.revalidate();
        chips.repaint();
    }

    void setCount(String text) {
        count.setText(text);
    }

    /** Puts text in the search field without the player typing it, for example to clear it. */
    void setSearchText(String text) {
        if ((search != null) && !search.getText().equals(text)) {
            search.setText(text);
        }
    }
}
