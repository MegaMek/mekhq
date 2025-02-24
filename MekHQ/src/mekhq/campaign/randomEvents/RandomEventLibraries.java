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
package mekhq.campaign.randomEvents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;
import mekhq.campaign.randomEvents.prisoners.yaml.PrisonerEventDataWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A utility class that manages the loading and retrieval of random event data
 * from YAML files. Organizes the events into separate lists for later use.
 */
public class RandomEventLibraries {
    // file addresses
    /**
     * Directory path where event YAML files are located.
     */
    private final String DIRECTORY = "data/randomEvents/";

    /**
     * File extension for the YAML files.
     */
    private final String EXTENSION = ".yml";

    private final String PRISONER_EVENTS_MAJOR = Paths.get(DIRECTORY + "PrisonerMajorEventData" + EXTENSION).toString();
    private final String PRISONER_EVENTS_MINOR = Paths.get(DIRECTORY + "PrisonerMinorEventData" + EXTENSION).toString();

    // lists
    private List<PrisonerEventData> prisonerEventsMajor = new ArrayList<>();
    private List<PrisonerEventData> prisonerEventsMinor = new ArrayList<>();

    /**
     * Constructs a {@code RandomEventLibraries} object and initializes the event data
     * by loading it from the YAML files.
     */
    public RandomEventLibraries() {
        buildPrisonerEventData();
    }

    /**
     * Retrieves a list of prisoner events based on their severity (major or minor).
     *
     * @param isMajor {@code true} to retrieve major prisoner events, {@code false} to retrieve
     *                           minor prisoner events.
     * @return a {@link List} of {@link PrisonerEventData} corresponding to the specified event severity.
     */
    public List<PrisonerEventData> getPrisonerEvents(boolean isMajor) {
        if (isMajor) {
            return prisonerEventsMajor;
        } else {
            return prisonerEventsMinor;
        }
    }

    /**
     * Loads prisoner event data from predefined YAML files ({@code PRISONER_EVENTS_MAJOR} and
     * {@code PRISONER_EVENTS_MINOR}) and organizes the individual events into major and minor
     * event lists.
     * <p>
     * Uses Jackson for YAML deserialization via the {@link ObjectMapper}.
     * </p>
     */
    private void buildPrisonerEventData() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

        // List of file paths for major and minor events
        List<String> eventFiles = List.of(PRISONER_EVENTS_MAJOR, PRISONER_EVENTS_MINOR);

        for (String eventFile : eventFiles) {
            try {
                // Deserialize YAML into PrisonerEventDataWrapper
                PrisonerEventDataWrapper wrapper = objectMapper.readValue(
                    new File(eventFile),
                    PrisonerEventDataWrapper.class
                );

                // Access and sort individual events
                List<PrisonerEventData> events = wrapper.getEvents();

                for (PrisonerEventData event : events) {
                    if (eventFiles.indexOf(eventFile) == 0) {
                        prisonerEventsMajor.add(event);
                    } else {
                        prisonerEventsMinor.add(event);
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading prisoner event data from file: " + eventFile, e);
            }
        }
    }
}
