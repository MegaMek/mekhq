/*
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
package mekhq.gui.preferences;

import mekhq.preferences.PreferenceElement;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

public class JScrollPanePreference extends PreferenceElement implements PropertyChangeListener {
    //region Variable Declarations
    private final WeakReference<JScrollPane> weakReference;
    private int horizontalValue;
    private int verticalValue;
    //endregion Variable Declarations

    //region Constructors
    public JScrollPanePreference(final JScrollPane scrollPane) {
        super(scrollPane.getName());
        setHorizontalValue((scrollPane.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
                ? 0 : scrollPane.getHorizontalScrollBar().getValue());
        setVerticalValue((scrollPane.getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER)
                ? 0 : scrollPane.getVerticalScrollBar().getValue());
        weakReference = new WeakReference<>(scrollPane);
        scrollPane.addPropertyChangeListener(this);
    }
    //endregion Constructors

    //region Getters/Setters
    public WeakReference<JScrollPane> getWeakReference() {
        return weakReference;
    }

    public int getHorizontalValue() {
        return horizontalValue;
    }

    public void setHorizontalValue(final int horizontalValue) {
        this.horizontalValue = horizontalValue;
    }

    public int getVerticalValue() {
        return verticalValue;
    }

    public void setVerticalValue(final int verticalValue) {
        this.verticalValue = verticalValue;
    }
    //endregion Getters/Setters

    //region PreferenceElement
    @Override
    protected String getValue() {
        return String.format("%d|%d", getHorizontalValue(), getVerticalValue());
    }

    @Override
    protected void initialize(final String value) {
        // TODO : Java 11 : Swap to isBlank
        assert (value != null) && !value.trim().isEmpty();

        final JScrollPane element = getWeakReference().get();
        if (element != null) {
            final String[] parts = value.split("\\|", -1);
            setHorizontalValue(Integer.parseInt(parts[0]));
            setVerticalValue(Integer.parseInt(parts[1]));

            if (element.getHorizontalScrollBarPolicy() != JScrollPane.HORIZONTAL_SCROLLBAR_NEVER) {
                element.getHorizontalScrollBar().setValue(getHorizontalValue());
            }

            if (element.getVerticalScrollBarPolicy() != JScrollPane.VERTICAL_SCROLLBAR_NEVER) {
                element.getVerticalScrollBar().setValue(getVerticalValue());
            }
        }
    }

    @Override
    protected void dispose() {
        final JScrollPane element = getWeakReference().get();
        if (element != null) {
            element.removePropertyChangeListener(this);
            getWeakReference().clear();
        }
    }
    //endregion PreferenceElement

    //region PropertyChangeListener
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        final JScrollPane element = getWeakReference().get();
        if (element != null) {
            setHorizontalValue((element.getHorizontalScrollBarPolicy() == JScrollPane.HORIZONTAL_SCROLLBAR_NEVER)
                    ? 0 : element.getHorizontalScrollBar().getValue());
            setVerticalValue((element.getVerticalScrollBarPolicy() == JScrollPane.VERTICAL_SCROLLBAR_NEVER)
                    ? 0 : element.getVerticalScrollBar().getValue());
        }
    }
    //endregion PropertyChangeListener
}
