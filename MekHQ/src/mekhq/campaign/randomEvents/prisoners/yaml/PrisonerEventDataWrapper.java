package mekhq.campaign.randomEvents.prisoners.yaml;

import mekhq.campaign.randomEvents.prisoners.records.PrisonerEventData;

import java.util.List;

/**
 * A wrapper class for managing a list of {@link PrisonerEventData}.
 * This class provides getter and setter methods to access and modify the list
 * of prisoner events.
 */
public class PrisonerEventDataWrapper {
    private List<PrisonerEventData> events;

    /**
     * @return a {@link List} of {@link PrisonerEventData} objects representing
     * the prisoner events.
     */
    public List<PrisonerEventData> getEvents() {
        return events;
    }

    /**
     * Sets the list of {@link PrisonerEventData} for this wrapper.
     *
     * @param events a {@link List} of {@link PrisonerEventData} objects to be
     *               associated with this wrapper.
     */
    public void setEvents(List<PrisonerEventData> events) {
        this.events = events;
    }
}
