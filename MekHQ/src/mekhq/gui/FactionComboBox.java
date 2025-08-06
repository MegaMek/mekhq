/*
 * Copyright (c) 2014 - Carl Spain. All Rights Reserved.
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

import java.awt.Component;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;

import megamek.common.annotations.Nullable;
import mekhq.campaign.universe.Factions;
import mekhq.gui.baseComponents.SortedComboBoxModel;

/**
 * Combo box for choosing a faction by full name that accounts for the fact that full names are not always unique within
 * the faction's era.
 *
 * @author Neoancient
 */
public class FactionComboBox extends JComboBox<Map.Entry<String, String>> {
    public FactionComboBox() {
        setModel(new SortedComboBoxModel<>(Entry.comparingByValue()));
        setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(final JList<?> list, final Object value,
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
