package mekhq.campaign.log;

import mekhq.campaign.LogEntry;

import java.util.Date;

public class AwardLogEntry extends LogEntry {
    public AwardLogEntry(Date date, String desc) {
        super(date, desc, LogEntryType.AWARD);
    }
}
