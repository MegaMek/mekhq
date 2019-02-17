/*
 * Copyright (c) 2019 The MegaMek Team. All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.ref.WeakReference;

public class JComboBoxPreference extends PreferenceElement implements ItemListener {
    private final WeakReference<JComboBox> weakRef;
    private int selectedIndex;

    public JComboBoxPreference(JComboBox comboBox) {
        super(comboBox.getName());

        this.selectedIndex = comboBox.getSelectedIndex();
        this.weakRef = new WeakReference<>(comboBox);
        comboBox.addItemListener(this);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            JComboBox element = weakRef.get();
            if (element != null) {
                this.selectedIndex = element.getSelectedIndex();
            }
        }
    }

    @Override
    protected String getValue() {
        return Integer.toString(this.selectedIndex);
    }

    @Override
    protected void initialize(String value) {
        assert value != null && value.trim().length() > 0;

        JComboBox element = weakRef.get();
        if (element != null) {
            int index = Integer.parseInt(value);
            if (index >= 0 && index < element.getItemCount()) {
                this.selectedIndex = index;
                element.setSelectedIndex(this.selectedIndex);
            }
        }
    }

    @Override
    protected void dispose() {
        JComboBox element = weakRef.get();
        if (element != null) {
            element.removeItemListener(this);
            weakRef.clear();
        }
    }
}
