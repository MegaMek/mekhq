package mekhq.campaign.log;

import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class PersonalLogEntryController {
    private ResourceBundle logEntriesResourceMap;

    public PersonalLogEntryController(ResourceBundle logEntriesResourceMap) {
        this.logEntriesResourceMap = logEntriesResourceMap;
    }

    public void logSpouseKia(Person spouse, Person person, Date date){
        String message = logEntriesResourceMap.getString("spouseKia.text");
        spouse.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, person.getName())));
    }

    public void logDivorcedFrom(Person person, Person spouse, Date date){
        String message = logEntriesResourceMap.getString("divorcedFrom.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spouse.getFullName())));
    }

    public void logMarriage(Person person, Person spouse, Date date){
        String message = logEntriesResourceMap.getString("marries.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spouse.getFullName())));
    }

    public void logGainedEdge(Person person, Date date){
        person.addLogEntry(new PersonalLogEntry(date, logEntriesResourceMap.getString("gainedEdge.text")));
    }

    public void logGained(Person person, Date date, String spa){
        String message = logEntriesResourceMap.getString("gained.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spa)));
    }
}
