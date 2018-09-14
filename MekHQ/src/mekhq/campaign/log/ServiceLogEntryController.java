package mekhq.campaign.log;

import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;

public class ServiceLogEntryController {

    private ResourceBundle logEntriesResourceMap;

    public ServiceLogEntryController(ResourceBundle logEntriesResourceMap) {
        this.logEntriesResourceMap = logEntriesResourceMap;
    }

    public void logRetireDueToWounds(Person person, Date date){
        String message = logEntriesResourceMap.getString("retiredDueToWounds.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, person.getGenderPronoun(person.PRONOUN_HISHER))));
    }

    public void logMadeBondsman(Person person, Date date, String name, String rankEntry){
        String message = logEntriesResourceMap.getString("madeBondsmanBy.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public void logMadeBondsman(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("madeBondsman.text")));
    }

    public void logMadePrisoner(Person person, Date date, String name, String rankEntry){
        String message = logEntriesResourceMap.getString("madePrisonerBy.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public void logMadePrisoner(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("madePrisoner.text")));
    }

    public void logJoined(Person person, Date date, String name, String rankEntry){
        String message = logEntriesResourceMap.getString("joined.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public void logFreed(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("freed.text")));
    }

    public void logKia(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("kia.text")));
    }

    public void logMia(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("mia.text")));
    }

    public void logRetired(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("retired.text")));
    }

    public void logRecoveredMia(Person person, Date date){
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("recoveredMia.text")));
    }

    public void logPromotedTo(Person person, Date date){
        String message = logEntriesResourceMap.getString("promotedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, person.getRankName())));
    }

    public void logDemotedTo(Person person, Date date){
        String message = logEntriesResourceMap.getString("demotedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, person.getRankName())));
    }

    public void logParticipatedInMission(Person person, Date date, String scenarioName, String missionName){
        String message = logEntriesResourceMap.getString("participatedInMission.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, scenarioName, missionName)));
    }

    public void logGainedXpFromMedWork(Person doctor, Date date, int taskXP){
        String message = logEntriesResourceMap.getString("gainedXpFromMedWork.text");
        doctor.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, taskXP)));
    }

    public void logSuccessfullyTreatedWithXp(Person doctor, Person patient, Date date, int injuries, int xp){
        String message = logEntriesResourceMap.getString("successfullyTreatedWithXp.text");
        doctor.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, patient, injuries, xp)));
    }

    public void logSuccessfullyTreated(Person doctor, Person patient, Date date, int injuries){
        String message = logEntriesResourceMap.getString("successfullyTreatedForXInjuries.text");
        doctor.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, patient.getName(), injuries)));
    }

    public void logAssignedTo(Person person, Date date, String unitName){
        String message = logEntriesResourceMap.getString("assignedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public void logReassignedTo(Person person, Date date, String unitName){
        String message = logEntriesResourceMap.getString("reassignedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public void logRemovedFrom(Person person, Date date, String unitName){
        String message = logEntriesResourceMap.getString("removedFrom.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, unitName)));
    }
}
