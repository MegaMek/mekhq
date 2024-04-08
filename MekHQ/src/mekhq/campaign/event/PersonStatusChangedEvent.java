package mekhq.campaign.event;

import mekhq.campaign.personnel.Person;

/**
 * Triggered when a Person's status is changed
 */
public class PersonStatusChangedEvent extends PersonChangedEvent {

    public PersonStatusChangedEvent(Person person) {
        super(person);
    }
}
