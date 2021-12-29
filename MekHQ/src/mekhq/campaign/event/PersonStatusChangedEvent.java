package mekhq.campaign.event;

import mekhq.campaign.personnel.Person;

public class PersonStatusChangedEvent extends PersonChangedEvent {

    public PersonStatusChangedEvent(Person person) {
        super(person);
    }
}
