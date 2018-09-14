package mekhq.campaign.log;

import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.ResourceBundle;

public class MedicalLogEntryController {
    private ResourceBundle logEntriesResourceMap;

    public MedicalLogEntryController(ResourceBundle logEntriesResourceMap) {
        this.logEntriesResourceMap = logEntriesResourceMap;
    }

    public MedicalLogEntry logSeveredSpine(Person person, Date date){
        String message = logEntriesResourceMap.getString("severedSpine.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                        person.getGenderPronoun(person.PRONOUN_HISHER),
                        person.getGenderPronoun(Person.PRONOUN_HIMHER)));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry brokenRibPunctureDead(Person person, Date date){
        String message = logEntriesResourceMap.getString("brokenRibPunctureDead.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                person.getGenderPronoun(person.PRONOUN_HISHER)));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry brokenRibPuncture(Person person, Date date){
        String message = logEntriesResourceMap.getString("brokenRibPuncture.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                person.getGenderPronoun(person.PRONOUN_HISHER)));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry logDevelopedEncephalopatyh(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("developedEncephalopathy.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry logConcussionWorsned(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("concussionWorsened.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry logDevelopedCerbralContusion(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("developedCerebralContusion.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry logDiedDueToBrainTrauma(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("diedDueToBrainTrauma.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry logDiedOfInternalBleeding(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("diedOfInternalBleeding.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry logInternalBleedingWorsened(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("internalBleedingWorsened.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public void logReturnedWithInjuries(Person person, Date date, Collection<Injury> newInjuries){
        StringBuilder sb = new StringBuilder(logEntriesResourceMap.getString("returnedWithInjuries.text"));
        newInjuries.forEach((inj) -> sb.append("\n\t\t").append(inj.getFluff()));
        MedicalLogEntry entry = new MedicalLogEntry(date, sb.toString());
        person.addLogEntry(entry);
    }

    public void logDocMadeAMistake(Person doctor, Person patient, Injury injury, Date date){
        String message = logEntriesResourceMap.getString("docMadeAMistake.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logDocAmazingWork(Person doctor, Person patient, Injury injury, Date date, int critTimeReduction){
        String message = logEntriesResourceMap.getString("docAmazingWork.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName(), critTimeReduction));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logSuccessfullyTreated(Person doctor, Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("successfullyTreated.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logInjuryDidntHealProperly(Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("didntHealProperly.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logInjuryHealed(Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("healed.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logInjuryBecamePermanent(Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("becamePermanent.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logDiedInInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("diedInInfirmary.text")));
    }

    public void logAbductedFromInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("abductedFromInfirmary.text")));
    }

    public void logRetiredAndTransferedFromInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("retiredAndTransferedFromInfirmary.text")));
    }
    public void logDismissedFromInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("dismissedFromInfirmary.text")));
    }

    public void logDeliveredBaby(Person patient, Person baby, Date date){
        String message = logEntriesResourceMap.getString("deliveredBaby.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, (baby.getGender() == Person.G_MALE ? "boy!" : "girl!")));
        patient.addLogEntry(medicalLogEntry);
    }

    public void logHasConceived(Person patient, Date date, String sizeString){
        String message = logEntriesResourceMap.getString("hasConceived.text");

        if(null != sizeString)
            message += " " + sizeString;

        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, message);

        patient.addLogEntry(medicalLogEntry);
    }

    public void logDiedFromWounds(Person patient, Date date){
        String message = logEntriesResourceMap.getString("diedFromWounds.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, patient.getGenderPronoun(Person.PRONOUN_HISHER)));
        patient.addLogEntry(medicalLogEntry);
    }
}
