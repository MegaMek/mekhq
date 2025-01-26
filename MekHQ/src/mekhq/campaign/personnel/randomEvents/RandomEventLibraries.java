package mekhq.campaign.personnel.randomEvents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mekhq.campaign.personnel.prisoners.PrisonerEventData;
import mekhq.campaign.personnel.prisoners.yaml.PrisonerEventDataWrapper;

import java.io.File;
import java.io.IOException;
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

    private final String PRISONER_EVENTS_MAJOR = DIRECTORY + "PrisonerMajorEventData" + EXTENSION;
    private final String PRISONER_EVENTS_MINOR = DIRECTORY + "PrisonerMinorEventData" + EXTENSION;

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
