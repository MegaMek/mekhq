/*
 * Copyright (C) 2018 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.log;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * This class is responsible to control the logging of Medical Log Entries.
 * @author Miguel Azevedo
 */
public class MedicalLogger {
    private ResourceBundle logEntriesResourceMap;
    private static MedicalLogger medicalLogger;

    public MedicalLogger() {
        this.logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());
    }

    public static MedicalLogger getInstance(){
        if(null == medicalLogger){
            medicalLogger = new MedicalLogger();
        }
        return medicalLogger;
    }

    public MedicalLogEntry severedSpine(Person person, Date date){
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

    public MedicalLogEntry developedEncephalopatyh(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("developedEncephalopathy.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry concussionWorsned(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("concussionWorsened.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry developedCerbralContusion(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("developedCerebralContusion.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry diedDueToBrainTrauma(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("diedDueToBrainTrauma.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry diedOfInternalBleeding(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("diedOfInternalBleeding.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public MedicalLogEntry internalBleedingWorsened(Person person, Date date){
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("internalBleedingWorsened.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public void returnedWithInjuries(Person person, Date date, Collection<Injury> newInjuries){
        StringBuilder sb = new StringBuilder(logEntriesResourceMap.getString("returnedWithInjuries.text"));
        newInjuries.forEach((inj) -> sb.append("\n\t\t").append(inj.getFluff()));
        MedicalLogEntry entry = new MedicalLogEntry(date, sb.toString());
        person.addLogEntry(entry);
    }

    public void docMadeAMistake(Person doctor, Person patient, Injury injury, Date date){
        String message = logEntriesResourceMap.getString("docMadeAMistake.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void docAmazingWork(Person doctor, Person patient, Injury injury, Date date, int critTimeReduction){
        String message = logEntriesResourceMap.getString("docAmazingWork.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName(), critTimeReduction));
        patient.addLogEntry(medicalLogEntry);
    }

    public void successfullyTreated(Person doctor, Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("successfullyTreated.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void injuryDidntHealProperly(Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("didntHealProperly.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void injuryHealed(Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("healed.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void injuryBecamePermanent(Person patient, Date date, Injury injury){
        String message = logEntriesResourceMap.getString("becamePermanent.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public void diedInInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("diedInInfirmary.text")));
    }

    public void abductedFromInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("abductedFromInfirmary.text")));
    }

    public void retiredAndTransferedFromInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("retiredAndTransferedFromInfirmary.text")));
    }
    public void dismissedFromInfirmary(Person person, Date date){
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("dismissedFromInfirmary.text")));
    }

    public void deliveredBaby(Person patient, Person baby, Date date){
        String message = logEntriesResourceMap.getString("deliveredBaby.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, (baby.getGender() == Person.G_MALE ? "boy!" : "girl!")));
        patient.addLogEntry(medicalLogEntry);
    }

    public void hasConceived(Person patient, Date date, String sizeString){
        String message = logEntriesResourceMap.getString("hasConceived.text");

        if(null != sizeString)
            message += " " + sizeString;

        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, message);
        patient.addLogEntry(medicalLogEntry);
    }

    public void diedFromWounds(Person patient, Date date){
        String message = logEntriesResourceMap.getString("diedFromWounds.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, patient.getGenderPronoun(Person.PRONOUN_HISHER)));
        patient.addLogEntry(medicalLogEntry);
    }
}
