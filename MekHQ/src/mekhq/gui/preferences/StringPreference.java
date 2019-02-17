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

import mekhq.gui.utilities.ObservableString;
import mekhq.preferences.PreferenceElement;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

public class StringPreference extends PreferenceElement implements PropertyChangeListener {
    private final WeakReference<ObservableString> weakRef;
    private String value;

    public StringPreference(ObservableString stringProperty) {
        super(stringProperty.getName());

        this.value = stringProperty.getValue();
        this.weakRef = new WeakReference<>(stringProperty);
        stringProperty.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        ObservableString element = weakRef.get();
        if (element != null) {
            this.value = element.getValue();
        }
    }

    @Override
    protected String getValue() {
        return this.value;
    }

    @Override
    protected void initialize(String value) {
        ObservableString element = weakRef.get();
        if (element != null) {
            this.value = value;
            element.setValue(this.value);
        }
    }

    @Override
    protected void dispose() {
        ObservableString element = weakRef.get();
        if (element != null) {
            element.removePropertyChangeListener(this);
            weakRef.clear();
        }
    }
}
