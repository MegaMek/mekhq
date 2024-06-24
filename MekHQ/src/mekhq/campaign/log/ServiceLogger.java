/*
 * Copyright (c) 2018-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import org.apache.logging.log4j.LogManager;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * This class is responsible to control the logging of Service Log Entries.
 * @author Miguel Azevedo
 */
public class ServiceLogger {
    private static final transient ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries",
            MekHQ.getMHQOptions().getLocale());

    public static void retireDueToWounds(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("retiredDueToWounds.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message,
                GenderDescriptors.HIS_HER_THEIR.getDescriptor(person.getGender()))));
    }

    public static void madeBondsman(Person person, LocalDate date, String name, String rankEntry) {
        String message = logEntriesResourceMap.getString("madeBondsmanBy.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public static void madeBondsman(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("madeBondsman.text")));
    }

    public static void madePrisoner(Person person, LocalDate date, String name, String rankEntry) {
        String message = logEntriesResourceMap.getString("madePrisonerBy.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public static void madePrisoner(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("madePrisoner.text")));
    }

    public static void joined(Person person, LocalDate date, String name, String rankEntry) {
        String message = logEntriesResourceMap.getString("joined.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public static void freed(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("freed.text")));
    }

    public static void freed(Person person, LocalDate date, String name, String rankEntry) {
        String message = logEntriesResourceMap.getString("freedBy.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, name) + rankEntry));
    }

    public static void changedStatus(final Person person, final LocalDate date,
                                     final PersonnelStatus status) {
        person.addLogEntry(new ServiceLogEntry(date, status.getLogText()));
    }

    public static void eduEnrolled(Person person, LocalDate date, String institution) {
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(logEntriesResourceMap.getString("eduEnrolled.text"), institution)));
    }

    public static void eduGraduated(Person person, LocalDate date, String institution) {
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(logEntriesResourceMap.getString("eduGraduated.text"), institution)));
    }

    public static void eduGraduatedPlus(Person person, LocalDate date, String graduationType, String institution) {
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(logEntriesResourceMap.getString("eduGraduatedPlus.text"), graduationType, institution)));
    }

    public static void eduGraduatedMasters(Person person, LocalDate date, String institution) {
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(logEntriesResourceMap.getString("eduGraduatedMasters.text"), institution)));
    }

    public static void eduGraduatedDoctorate(Person person, LocalDate date, String institution) {
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(logEntriesResourceMap.getString("eduGraduatedDoctorate.text"), institution)));
    }

    public static void eduFailed(Person person, LocalDate date, String institution) {
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(logEntriesResourceMap.getString("eduFailed.text"), institution)));
    }

    public static void recoveredMia(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("recoveredMia.text")));
    }

    public static void recoveredPoW(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("recoveredPoW.text")));
    }

    public static void returnedFromLeave(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("returnedFromLeave.text")));
    }

    public static void returnedFromEducation(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("returnedFromEducation.text")));
    }

    public static void returnedFromAWOL(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("returnedFromAWOL.text")));
    }

    public static void returnedFromMissing(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("returnedFromMissing.text")));
    }

    public static void resurrected(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("resurrected.text")));
    }

    public static void rehired(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("rehired.text")));
    }

    public static void retired(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("retired.text")));
    }

    public static void resigned(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("resigned.text")));
    }

    public static void deserted(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("deserted.text")));
    }

    public static void defected(Person person, LocalDate date) {
        person.addLogEntry(new ServiceLogEntry(date, logEntriesResourceMap.getString("defected.text")));
    }

    public static void promotedTo(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("promotedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, person.getRankName())));
    }

    public static void demotedTo(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("demotedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, person.getRankName())));
    }

    public static void participatedInScenarioDuringMission(Person person, LocalDate date,
                                                           String scenarioName, String missionName) {
        String message = logEntriesResourceMap.getString("participatedInScenarioDuringMission.text");
        person.addScenarioLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, scenarioName, missionName)));
    }

    public static void capturedInScenarioDuringMission(Person person, LocalDate date,
                                                           String scenarioName, String missionName) {
        String message = logEntriesResourceMap.getString("capturedInScenarioDuringMission.text");
        person.addLogEntry(new ServiceLogEntry(date,
                MessageFormat.format(message, scenarioName, missionName)));
    }

    public static void gainedXpFromMedWork(Person doctor, LocalDate date, int taskXP) {
        String message = logEntriesResourceMap.getString("gainedXpFromMedWork.text");
        doctor.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, taskXP)));
    }

    public static void successfullyTreatedWithXp(Person doctor, Person patient, LocalDate date, int injuries, int xp) {
        String message = logEntriesResourceMap.getString("successfullyTreatedWithXp.text");
        doctor.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, patient, injuries, xp)));
    }

    public static void successfullyTreated(Person doctor, Person patient, LocalDate date, int injuries) {
        String message = logEntriesResourceMap.getString("successfullyTreatedForXInjuries.text");
        doctor.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, patient.getFullName(), injuries)));
    }

    public static void assignedTo(Person person, LocalDate date, String unitName) {
        String message = logEntriesResourceMap.getString("assignedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public static void reassignedTo(Person person, LocalDate date, String unitName) {
        String message = logEntriesResourceMap.getString("reassignedTo.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public static void removedFrom(Person person, LocalDate date, String unitName) {
        String message = logEntriesResourceMap.getString("removedFrom.text");
        person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public static void addedToTOEForce(Campaign campaign, Person person, LocalDate date, Force force) {
        if (force != null) {
            String message = logEntriesResourceMap.getString("addToTOEForce.text");
            person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message,
                    campaign.getCampaignOptions().isUseExtendedTOEForceName() ? force.getFullName() : force.getName())));
        }
    }

    public static void reassignedTOEForce(final Campaign campaign, final Person person,
                                          final LocalDate date, final @Nullable Force oldForce,
                                          final @Nullable Force newForce) {
        if ((oldForce == null) && (newForce == null)) {
            LogManager.getLogger().error(String.format("Cannot reassign %s on %s because both specified forces are null",
                    person.getFullTitle(), date));
            return;
        }

        if ((oldForce != null) && (newForce != null)) {
            String message = logEntriesResourceMap.getString("reassignedTOEForce.text");
            person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message,
                    campaign.getCampaignOptions().isUseExtendedTOEForceName() ? oldForce.getFullName() : oldForce.getName(),
                    campaign.getCampaignOptions().isUseExtendedTOEForceName() ? newForce.getFullName() : newForce.getName())));
        } else if (oldForce == null) {
            addedToTOEForce(campaign, person, date, newForce);
        } else {
            removedFromTOEForce(campaign, person, date, oldForce);
        }
    }

    public static void removedFromTOEForce(Campaign campaign, Person person, LocalDate date, Force force) {
        if (force != null) {
            String message = logEntriesResourceMap.getString("removedFromTOEForce.text");
            person.addLogEntry(new ServiceLogEntry(date, MessageFormat.format(message,
                    campaign.getCampaignOptions().isUseExtendedTOEForceName() ? force.getFullName() : force.getName())));
        }
    }

    public static void oprhaned(Person person, LocalDate date) {
        String message = logEntriesResourceMap.getString("removedFromTOEForce.text");

        person.addLogEntry(new ServiceLogEntry(date, message));
    }
}
