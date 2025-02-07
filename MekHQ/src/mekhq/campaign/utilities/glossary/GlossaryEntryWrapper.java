/*
 * Copyright (c) 2025 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities.glossary;

import java.util.Map;

/**
 * The {@code GlossaryEntryWrapper} class is a container used to model the YAML structure
 * for glossary entries. It acts as an intermediary for parsing the glossary YAML file and
 * mapping its terms to specific glossary entries.
 *
 * <p>
 *     The YAML file is expected to define a map-like structure where the keys are glossary term
 *     identifiers (Strings) and the values are instances of {@link GlossaryEntry}.
 * </p>
 */
class GlossaryEntryWrapper {
    private Map<String, GlossaryEntry> entries;

    /**
     * Gets the map of glossary terms with their corresponding {@link GlossaryEntry} objects.
     *
     * @return A {@link Map} containing all glossary terms and their metadata.
     */
    public Map<String, GlossaryEntry> getEntries() {
        return entries;
    }

    /**
     * Sets the map of glossary terms with their corresponding {@link GlossaryEntry} records.
     *
     * <p>
     *     This method is primarily used during deserialization when Jackson maps the YAML data to
     *     this object. It allows assigning the parsed entries to the internal map.
     * </p>
     *
     * @param entries A {@link Map} containing glossary terms and their {@link GlossaryEntry} objects.
     */
    public void setEntries(Map<String, GlossaryEntry> entries) {
        this.entries = entries;
    }
}
