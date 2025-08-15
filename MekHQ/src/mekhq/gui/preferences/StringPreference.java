/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui.preferences;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

import megamek.client.ui.preferences.PreferenceElement;
import mekhq.gui.utilities.ObservableString;

public class StringPreference extends PreferenceElement implements PropertyChangeListener {
    private final WeakReference<ObservableString> weakRef;
    private String value;

    public StringPreference(ObservableString stringProperty) throws Exception {
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
