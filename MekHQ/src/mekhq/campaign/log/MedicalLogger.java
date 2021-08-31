/*
 * Copyright (C) 2018 - The MegaMek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.log;

import megamek.common.util.EncodeControl;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.ResourceBundle;

/**
 * This class is responsible to control the logging of Medical Log Entries.
 * @author Miguel Azevedo
 */
public class MedicalLogger {
    private static ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries",
            new EncodeControl());

    public static MedicalLogEntry severedSpine(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("severedSpine.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                        GenderDescriptors.HIS_HER.getDescriptor(person.getGender()),
                        GenderDescriptors.HIM_HER.getDescriptor(person.getGender())));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry brokenRibPunctureDead(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("brokenRibPunctureDead.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                GenderDescriptors.HIS_HER.getDescriptor(person.getGender())));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry brokenRibPuncture(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("brokenRibPuncture.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                GenderDescriptors.HIS_HER.getDescriptor(person.getGender())));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry developedEncephalopathy(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("developedEncephalopathy.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry concussionWorsened(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("concussionWorsened.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry developedCerebralContusion(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("developedCerebralContusion.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry diedDueToBrainTrauma(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("diedDueToBrainTrauma.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry diedOfInternalBleeding(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("diedOfInternalBleeding.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry internalBleedingWorsened(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, logEntriesResourceMap.getString("internalBleedingWorsened.text"));
        person.addLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static void returnedWithInjuries(Person person, LocalDate date, Collection<Injury> newInjuries) {
        StringBuilder sb = new StringBuilder(logEntriesResourceMap.getString("returnedWithInjuries.text"));
        newInjuries.forEach((inj) -> sb.append("\n\t\t").append(inj.getFluff()));
        MedicalLogEntry entry = new MedicalLogEntry(date, sb.toString());
        person.addLogEntry(entry);
    }

    public static void docMadeAMistake(Person doctor, Person patient, Injury injury, LocalDate date) {
        String message = logEntriesResourceMap.getString("docMadeAMistake.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void docAmazingWork(Person doctor, Person patient, Injury injury, LocalDate date, int critTimeReduction) {
        String message = logEntriesResourceMap.getString("docAmazingWork.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName(), critTimeReduction));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void successfullyTreated(Person doctor, Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("successfullyTreated.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
                MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void injuryDidntHealProperly(Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("didntHealProperly.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void injuryHealed(Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("healed.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void injuryBecamePermanent(Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("becamePermanent.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void diedInInfirmary(Person person, LocalDate date) {
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("diedInInfirmary.text")));
    }

    public static void abductedFromInfirmary(Person person, LocalDate date) {
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("abductedFromInfirmary.text")));
    }

    public static void retiredAndTransferredFromInfirmary(Person person, LocalDate date) {
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("retiredAndTransferredFromInfirmary.text")));
    }

    public static void dismissedFromInfirmary(Person person, LocalDate date) {
        person.addLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("dismissedFromInfirmary.text")));
    }

    public static void deliveredBaby(Person patient, Person baby, LocalDate date) {
        String message = logEntriesResourceMap.getString("deliveredBaby.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message,
                GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender())));
        patient.addLogEntry(medicalLogEntry);
    }

    public static void hasConceived(Person patient, LocalDate date, String sizeString) {
        String message = logEntriesResourceMap.getString("hasConceived.text");

        if (sizeString != null) {
            message += " " + sizeString;
        }

        patient.addLogEntry(new MedicalLogEntry(date, message));
    }
}
