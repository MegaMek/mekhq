package mekhq.campaign.log;

import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class AwardLogEntryController{
    private ResourceBundle logEntriesResourceMap;

    public AwardLogEntryController(ResourceBundle logEntriesResourceMap) {
        this.logEntriesResourceMap = logEntriesResourceMap;
    }

    public void logAward(Person person, Date date, Award award){
        String message = logEntriesResourceMap.getString("awarded.text");
        person.addLogEntry(new AwardLogEntry(date, MessageFormat.format(message, award.getName(), award.getDescription())));
    }

    public void logRemovedAward(Person person, Date date, Award award){
        String message = logEntriesResourceMap.getString("removedAward.text");
        person.addLogEntry(new AwardLogEntry(date, MessageFormat.format(message, award.getName())));
    }
}
