/*
 * Copyright (C) 2018-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.log;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.Collection;
import java.util.ResourceBundle;

import mekhq.MekHQ;
import mekhq.campaign.personnel.Injury;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;

/**
 * This class is responsible to control the logging of Medical Log Entries.
 *
 * @author Miguel Azevedo
 */
public class MedicalLogger {
    private static final ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries",
          MekHQ.getMHQOptions().getLocale());

    public static void inoculation(Person person, LocalDate date, String planetName) {
        String message = String.format(logEntriesResourceMap.getString("inoculation.text"), planetName);
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, planetName));
        person.addMedicalLogEntry(medicalLogEntry);
    }

    public static MedicalLogEntry severedSpine(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("severedSpine.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message,
                    GenderDescriptors.HIS_HER_THEIR.getDescriptor(person.getGender()),
                    GenderDescriptors.HIM_HER_THEM.getDescriptor(person.getGender())));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry brokenRibPunctureDead(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("brokenRibPunctureDead.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, GenderDescriptors.HIS_HER_THEIR.getDescriptor(person.getGender())));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry brokenRibPuncture(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("brokenRibPuncture.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, GenderDescriptors.HIS_HER_THEIR.getDescriptor(person.getGender())));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry developedEncephalopathy(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              logEntriesResourceMap.getString("developedEncephalopathy.text"));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry concussionWorsened(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              logEntriesResourceMap.getString("concussionWorsened.text"));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry developedCerebralContusion(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              logEntriesResourceMap.getString("developedCerebralContusion.text"));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry diedDueToBrainTrauma(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              logEntriesResourceMap.getString("diedDueToBrainTrauma.text"));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry diedOfInternalBleeding(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              logEntriesResourceMap.getString("diedOfInternalBleeding.text"));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static MedicalLogEntry internalBleedingWorsened(Person person, LocalDate date) {
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              logEntriesResourceMap.getString("internalBleedingWorsened.text"));
        person.addMedicalLogEntry(medicalLogEntry);
        return medicalLogEntry;
    }

    public static void returnedWithInjuries(Person person, LocalDate date, Collection<Injury> newInjuries) {
        StringBuilder sb = new StringBuilder(logEntriesResourceMap.getString("returnedWithInjuries.text"));
        newInjuries.forEach((inj) -> sb.append("\n\t\t").append(inj.getFluff()));
        MedicalLogEntry entry = new MedicalLogEntry(date, sb.toString());
        person.addMedicalLogEntry(entry);
    }

    public static void docMadeAMistake(Person doctor, Person patient, Injury injury, LocalDate date) {
        String message = logEntriesResourceMap.getString("docMadeAMistake.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void docAmazingWork(Person doctor, Person patient, Injury injury, LocalDate date,
          int critTimeReduction) {
        String message = logEntriesResourceMap.getString("docAmazingWork.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, doctor.getFullTitle(), injury.getName(), critTimeReduction));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void successfullyTreated(Person doctor, Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("successfullyTreated.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, doctor.getFullTitle(), injury.getName()));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void injuryDidntHealProperly(Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("didntHealProperly.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void injuryHealed(Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("healed.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void injuryBecamePermanent(Person patient, LocalDate date, Injury injury) {
        String message = logEntriesResourceMap.getString("becamePermanent.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date, MessageFormat.format(message, injury.getName()));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void diedInInfirmary(Person person, LocalDate date) {
        person.addMedicalLogEntry(new MedicalLogEntry(date, logEntriesResourceMap.getString("diedInInfirmary.text")));
    }

    public static void abductedFromInfirmary(Person person, LocalDate date) {
        person.addMedicalLogEntry(new MedicalLogEntry(date,
              logEntriesResourceMap.getString("abductedFromInfirmary.text")));
    }

    public static void retiredAndTransferredFromInfirmary(Person person, LocalDate date) {
        person.addMedicalLogEntry(new MedicalLogEntry(date,
              logEntriesResourceMap.getString("retiredAndTransferredFromInfirmary.text")));
    }

    public static void dismissedFromInfirmary(Person person, LocalDate date) {
        person.addMedicalLogEntry(new MedicalLogEntry(date,
              logEntriesResourceMap.getString("dismissedFromInfirmary.text")));
    }

    public static void deliveredBaby(Person patient, Person baby, LocalDate date) {
        String message = logEntriesResourceMap.getString("deliveredBaby.text");
        MedicalLogEntry medicalLogEntry = new MedicalLogEntry(date,
              MessageFormat.format(message, GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender())));
        patient.addMedicalLogEntry(medicalLogEntry);
    }

    public static void hasConceived(Person patient, LocalDate date, String sizeString) {
        String message = logEntriesResourceMap.getString("hasConceived.text");

        if (!sizeString.isBlank()) {
            message += ' ' + sizeString;
        }

        patient.addMedicalLogEntry(new MedicalLogEntry(date, message));
    }
}
