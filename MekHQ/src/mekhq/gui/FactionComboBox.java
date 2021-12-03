/*
 * FactionComboBox.java
 *
 * Copyright (c) 2014 - Carl Spain. All Rights Reserved.
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.gui;

import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.SortedComboBoxModel;

import javax.swing.*;
import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Combo box for choosing a faction by full name that accounts for the fact that full names are not
 * always unique within the faction's era.
 *
 * @author Neoancient
 */
public class FactionComboBox extends JComboBox<Map.Entry<String, String>> {
    public FactionComboBox() {
        setModel(new SortedComboBoxModel<>(Entry.comparingByValue()));
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public  Component getListCellRendererComponent(final JList<?> list, final Object value,
                                                           final int index, final boolean isSelected,
                                                           final boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value != null) {
                    setText((String) ((Map.Entry<?, ?>) value).getValue());
                }
                return this;
            }
        });
    }

    public void addFactionEntries(Collection<String> list, int year) {
        HashMap<String, String> map = new HashMap<>();
        HashSet<String> collisions = new HashSet<>();
        for (String key : list) {
            String fullName = Factions.getInstance().getFaction(key).getFullName(year);
            if (map.containsValue(fullName)) {
                collisions.add(fullName);
            }
            map.put(key, fullName);
        }
        for (String key : map.keySet()) {
            if (collisions.contains(map.get(key))) {
                map.put(key, map.get(key) + " (" + key + ")");
            }
        }
        for (Map.Entry<String, String> entry : map.entrySet()) {
            addItem(entry);
        }
    }

    public @Nullable String getSelectedItemKey() {
        if (getSelectedItem() == null) {
            return null;
        }
        return (String) ((Map.Entry<?, ?>) getSelectedItem()).getKey();
    }

    public void setSelectedItemByKey(final @Nullable String key) {
        if (key == null) {
            return;
        }

        for (int i = 0; i < getModel().getSize(); i++) {
            if (getModel().getElementAt(i).getKey().equals(key)) {
                setSelectedIndex(i);
                return;
            }
        }
    }
}
