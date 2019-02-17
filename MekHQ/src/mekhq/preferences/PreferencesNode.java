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

package mekhq.preferences;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a group of @link PreferenceElement that are part of the same
 * @link Class.
 *
 * This class is not thread-safe.
 */
public class PreferencesNode {
    private final Class node;
    private final Map<String, PreferenceElement> elements;
    private Map<String, String> initialValues;

    private boolean initialized = false;
    private boolean finalized = false;

    PreferencesNode(Class node) {
        assert node != null;

        this.node = node;
        this.initialValues = new HashMap<>();
        this.elements = new HashMap<>();
    }

    /**
     * Adds a new element to be managed by this node.
     * If there are initial vales set for this node,
     * we will try to set an initial value for this element.
     * @param element element to manage.
     */
    public void manage(PreferenceElement element) {
        PreferenceElement actual = this.elements.get(element.getName());
        if (actual != null) {
            this.initialValues.put(actual.getName(), actual.getValue());
            actual.dispose();
        }

        this.elements.put(element.getName(), element);

        if (this.initialValues.containsKey(element.getName())) {
            element.initialize(this.initialValues.get(element.getName()));
        }
    }

    /**
     * The class which preferences we are storing in this node.
     * @return the class stored in this node.
     */
    String getNodeName() {
        return this.node.getName();
    }

    /**
     * Sets the initial values for elements managed for this node.
     * This method should only be called once.
     * @param initialValues initial values for the elements.
     */
    void initialize(Map<String, String> initialValues) {
        assert initialValues != null;
        assert !initialized;

        this.initialized = true;
        this.initialValues = initialValues;
    }

    /**
     * Gets the values of all the elements managed by this node.
     * This method should only be called once.
     * @return
     */
    Map<String, String> getFinalValues() {
        assert !finalized;

        finalized = true;
        Map<String, String> finalValues = new HashMap<>(this.elements.size());

        // Use the values we had stored from initialization
        for(Map.Entry<String, String> initialValue : this.initialValues.entrySet()) {
            finalValues.put(initialValue.getKey(), initialValue.getValue());
        }

        // Overwrite the initial values with values generated during this
        // session
        for(PreferenceElement wrapper : this.elements.values()) {
            finalValues.put(wrapper.getName(), wrapper.getValue());
        }

        return finalValues;
    }
}
