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
import megamek.common.util.StringUtil;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;

import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * This class is responsible to control the logging of Personal Log Entries.
 * @author Miguel Azevedo
 */
public class PersonalLogger {
    private static ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());

    public static void spouseKia(Person spouse, Person person, Date date) {
        String message = logEntriesResourceMap.getString("spouseKia.text");
        spouse.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, person.getFullName())));
    }

    public static void divorcedFrom(Person person, Person spouse, Date date) {
        String message = logEntriesResourceMap.getString("divorcedFrom.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spouse.getFullName())));
    }

    public static void marriage(Person person, Person spouse, Date date) {
        String message = logEntriesResourceMap.getString("marries.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spouse.getFullName())));
    }

    public static void marriageNameChange(Person person, Person spouse, Date date) {
        String message = logEntriesResourceMap.getString("marriageNameChange.text");

        message = MessageFormat.format(message,
                GenderDescriptors.HIS_HER.getDescriptor(person.getGender()),
                (!StringUtil.isNullOrEmpty(person.getMaidenName())) ? person.getMaidenName()
                        : logEntriesResourceMap.getString("marriageNameChange.emptyMaidenName.text"),
                person.getSurname(), spouse.getFullName());

        person.addLogEntry(new PersonalLogEntry(date, message));
    }

    public static void gainedEdge(Person person, Date date) {
        person.addLogEntry(new PersonalLogEntry(date, logEntriesResourceMap.getString("gainedEdge.text")));
    }

    public static void gained(Person person, Date date, String spa) {
        String message = logEntriesResourceMap.getString("gained.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spa)));
    }

    public static void spouseConceived(Person person, String spouseName, Date date, String sizeString) {
        String message = MessageFormat.format(logEntriesResourceMap.getString("spouseConceived.text"), spouseName);

        if(sizeString != null) {
            message += " " + sizeString;
        }

        person.addLogEntry(new PersonalLogEntry(date, message));
    }

    //this is called to log the child being born on the father's personal log
    public static void ourChildBorn(Person person, Person baby, String spouseName, Date date) {
        person.addLogEntry(new PersonalLogEntry(date,
                MessageFormat.format(logEntriesResourceMap.getString("ourChildBorn.text"),
                        spouseName, GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender()))));
    }
}
