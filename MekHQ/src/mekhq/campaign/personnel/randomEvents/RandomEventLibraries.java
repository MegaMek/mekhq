package mekhq.campaign.personnel.randomEvents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import mekhq.campaign.personnel.prisoners.PrisonerEventData;
import mekhq.campaign.personnel.prisoners.yaml.PrisonerEventDataWrapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RandomEventLibraries {
    // file addresses
    private final String DIRECTORY = "data/randomEvents/";
    private final String EXTENSION = ".yml";
    private final String PRISONER_EVENTS_MAJOR = DIRECTORY + "PrisonerMajorEventData" + EXTENSION;
    private final String PRISONER_EVENTS_MINOR = DIRECTORY + "PrisonerMinorEventData" + EXTENSION;

    // lists
    private List<PrisonerEventData> prisonerEventsMajor = new ArrayList<>();
    private List<PrisonerEventData> prisonerEventsMinor = new ArrayList<>();

    public RandomEventLibraries() {
        buildPrisonerEventData();
    }

    public List<PrisonerEventData> getPrisonerEvents(boolean isMajor) {
        if (isMajor) {
            return prisonerEventsMajor;
        } else {
            return prisonerEventsMinor;
        }
    }

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
