package mekhq.campaign.log;

import mekhq.campaign.LogEntry;

import java.util.Date;

public class PersonalLogEntry extends LogEntry {
    public PersonalLogEntry(Date date, String desc){
        super(date, desc, LogEntryType.PERSONAL);
    }
}
