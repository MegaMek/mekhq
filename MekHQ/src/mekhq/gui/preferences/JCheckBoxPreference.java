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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.lang.ref.WeakReference;

public class JCheckBoxPreference extends PreferenceElement implements ChangeListener {
    private final WeakReference<JCheckBox>  weakRef;
    private String name;
    private boolean value;

    public JCheckBoxPreference(JCheckBox checkBox){
        super(checkBox.getName());

        this.value = checkBox.isSelected();
        this.weakRef = new WeakReference<>(checkBox);
        checkBox.addChangeListener(this);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JCheckBox element = weakRef.get();
        if (element != null) {
            this.value = element.isSelected();
        }
    }

    @Override
    protected String getValue() {
        return Boolean.toString(this.value);
    }

    @Override
    protected void initialize(String value) {
        assert value != null && value.trim().length() > 0;

        JCheckBox element = weakRef.get();
        if (element != null) {
            element.setSelected(Boolean.parseBoolean(value));
        }
    }

    @Override
    protected void clean() {
        JCheckBox element = weakRef.get();
        if (element != null) {
            element.removeChangeListener(this);
            weakRef.clear();
        }
    }
}
