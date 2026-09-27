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

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;

import megamek.client.ui.comboBoxes.SearchableComboBox;
import megamek.common.annotations.Nullable;
import mekhq.gui.baseComponents.hud.Hud;

/**
 * A HUD-styled drop-down that can be searched by typing, for lists too long to scroll through: the company's people,
 * every skill, every thread. MegaMek's {@link SearchableComboBox} takes its entries when it is made, so giving the
 * picker new entries puts a new box in the same place; callers only ever see the picker.
 *
 * @param <E> the type of entry
 */
final class SearchablePicker<E> {
    private final String name;
    private final Function<E, String> display;
    private final Runnable onChange;
    private final JPanel slot = Hud.transparentPanel(new BorderLayout());
    private SearchableComboBox<E> combo;
    private boolean filling;

    /**
     * @param name     the component name, for tests and tooling
     * @param display  the text shown for, and searched in, each entry
     * @param onChange run when the player picks a different entry, but not when the entries are replaced
     */
    SearchablePicker(final String name, final Function<E, String> display, final Runnable onChange) {
        this.name = name;
        this.display = display;
        this.onChange = onChange;
        setItems(List.of(), null);
    }

    /** @return the component to add to a layout; it stays the same when the entries change */
    JPanel getComponent() {
        return slot;
    }

    /**
     * Replaces the entries and selects one, without reporting a change.
     *
     * @param items    the entries, in display order
     * @param selected the entry to select, or {@code null} for the first
     */
    void setItems(final List<E> items, final @Nullable E selected) {
        filling = true;
        try {
            SearchableComboBox<E> replacement = new SearchableComboBox<>(name, items, display);
            style(replacement);
            if (combo != null) {
                replacement.setToolTipText(combo.getToolTipText());
            }
            if (selected != null && items.contains(selected)) {
                replacement.setSelectedItem(selected);
            } else if (!items.isEmpty()) {
                replacement.setSelectedItem(items.get(0));
            }
            replacement.addActionListener(event -> {
                if (!filling) {
                    onChange.run();
                }
            });
            combo = replacement;
            slot.removeAll();
            slot.add(combo, BorderLayout.CENTER);
            slot.revalidate();
            slot.repaint();
        } finally {
            filling = false;
        }
    }

    /** @return every entry, in display order */
    List<E> getItems() {
        List<E> items = new ArrayList<>();
        for (int index = 0; index < combo.getItemCount(); index++) {
            items.add(combo.getItemAt(index));
        }
        return items;
    }

    /** @return the selected entry, or {@code null} if the list is empty */
    @Nullable E getSelected() {
        return combo.getSelectedItem();
    }

    /**
     * Selects an entry, reporting the change as if the player had picked it.
     *
     * @param item the entry to select; one that isn't listed is ignored
     */
    void setSelected(final @Nullable E item) {
        if (item != null && getItems().contains(item)) {
            combo.setSelectedItem(item);
        }
    }

    /** @param text the tooltip, kept when the entries are replaced */
    void setToolTipText(final String text) {
        combo.setToolTipText(text);
    }

    /** Colours and font for the HUD. The box keeps its own renderer, which shows each entry's display text. */
    private static void style(final SearchableComboBox<?> box) {
        box.setBackground(SURFACE_DEEP);
        box.setForeground(TEXT);
        box.setFont(hudFont(Font.PLAIN, 0.92f, 0.0f));
        box.setBorder(BorderFactory.createLineBorder(BORDER, scaleForGUI(1)));
        if (box.getEditor().getEditorComponent() instanceof JTextField field) {
            field.setBackground(SURFACE_DEEP);
            field.setForeground(TEXT);
            field.setCaretColor(ACCENT);
        }
    }
}
