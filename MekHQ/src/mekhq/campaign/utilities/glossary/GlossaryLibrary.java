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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The {@code GlossaryLibrary} class manages the loading and retrieval of glossary entries.
 * These entries are stored in YAML files and provide definitions or descriptions for various terms.
 *
 * <p>The default glossary file location is set to {@code data/universe/Glossary.yml}.</p>
 */
public class GlossaryLibrary {
    private final String DIRECTORY = "data/universe/";
    private final String EXTENSION = ".yml";
    private final String GLOSSARY_ADDRESS = DIRECTORY + "Glossary" + EXTENSION;

    private final Map<String, GlossaryEntry> glossaryEntries = new HashMap<>();

    /**
     * The command string used to indicate a glossary entry in other parts of the application.
     * This is primarily used for recognizing glossary calls in hyperlinks. See
     * {@link mekhq.gui.baseComponents.MHQDialogImmersive} for an example usage.
     */
    public static final String GLOSSARY_COMMAND_STRING = "GLOSSARY";

    /**
     * Constructs a new {@code GlossaryLibrary} and immediately loads glossary entries
     * from the default file path specified in {@link #GLOSSARY_ADDRESS}.
     *
     * <p>
     *     Any errors encountered during loading will result in a {@link RuntimeException}.
     * </p>
     */
    public GlossaryLibrary() {
        loadGlossaryEntries(GLOSSARY_ADDRESS);
    }

    /**
     * Loads a map of glossary entries from a specified YAML file.
     *
     * <p>
     *     The YAML file must follow the structure defined by the {@link GlossaryEntryWrapper}
     *     class, which wraps a map of glossary terms and their corresponding {@link GlossaryEntry}
     *     records.
     * </p>
     *
     * <p>
     *     If the file cannot be read or parsed, a {@link RuntimeException} is thrown with the
     *     error details.
     * </p>
     *
     * @param filePath The path to the YAML file containing the glossary data.
     * @throws RuntimeException if the file cannot be read or parsed.
     */
    public void loadGlossaryEntries(String filePath) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        try {
            GlossaryEntryWrapper wrapper = objectMapper.readValue(
                new File(filePath), GlossaryEntryWrapper.class
            );

            glossaryEntries.putAll(wrapper.getEntries());
        } catch (IOException e) {
            throw new RuntimeException("Error reading glossary entries from file: " + filePath, e);
        }
    }

    /**
     * Gets the map of all glossary entries that have been loaded.
     *
     * <p>
     *     The keys of the map represent glossary term identifiers, and the values represent the
     *     corresponding {@link GlossaryEntry} objects.
     * </p>
     *
     * @return A {@link Map} containing all glossary entries, or an empty map if no entries were
     * loaded.
     */
    public Map<String, GlossaryEntry> getGlossaryEntries() {
        return glossaryEntries;
    }
}
