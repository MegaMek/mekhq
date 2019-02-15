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
 * The root class for MekHQ user preferences system.
 */
public class MekHqPreferences {
    private final Map<Class, PreferencesNode> preferences;

    public MekHqPreferences() {
        this.preferences = new HashMap<>();
    }

    public void loadPreferences(String preferencesFile) {
        // load preferences from a file
    }

    public void savePreferences(String preferencesFile) {
        // save preferences to a file
        for(Map.Entry<Class, PreferencesNode> node : this.preferences.entrySet()) {
            // Write Class.getName();
            for (Map.Entry<String, String> element : node.getValue().getFinalValues().entrySet()) {
                // Write element.Name - element.Value pairs
            }
        }
    }

    public PreferencesNode forElement(Class element) {
        PreferencesNode node = this.preferences.getOrDefault(element, null);
        if (node == null) {
            node = new PreferencesNode(element);
            this.preferences.put(element, node);
        }

        return node;
    }
}
