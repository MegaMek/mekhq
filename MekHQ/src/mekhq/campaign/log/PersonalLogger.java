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
import megamek.common.util.StringUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.GenderDescriptors;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * This class is responsible to control the logging of Personal Log Entries.
 * @author Miguel Azevedo
 */
public class PersonalLogger {
    private static ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());

    public static void spouseKia(Person spouse, Person person, LocalDate date) {
        String message = resources.getString("spouseKia.text");
        spouse.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, person.getFullName())));
    }

    public static void divorcedFrom(Person person, Person spouse, LocalDate date) {
        String message = resources.getString("divorcedFrom.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spouse.getFullName())));
    }

    public static void marriage(Person person, Person spouse, LocalDate date) {
        String message = resources.getString("marries.text");
        person.addLogEntry(new PersonalLogEntry(date, MessageFormat.format(message, spouse.getFullName())));
    }

    public static void marriageNameChange(Person person, Person spouse, LocalDate date) {
        String message = resources.getString("marriageNameChange.text");

        message = MessageFormat.format(message,
                GenderDescriptors.HIS_HER.getDescriptor(person.getGender()),
                (!StringUtil.isNullOrEmpty(person.getMaidenName())) ? person.getMaidenName()
                        : resources.getString("marriageNameChange.emptyMaidenName.text"),
                person.getSurname(), spouse.getFullName());

        person.addLogEntry(new PersonalLogEntry(date, message));
    }

    public static void improvedSkill(final Campaign campaign, final Person person,
                                     final LocalDate date, final String skill, final String value) {
        if (campaign.getCampaignOptions().isPersonnelLogSkillGain()) {
            person.addLogEntry(new PersonalLogEntry(date,
                    MessageFormat.format(resources.getString("improvedSkill.text"), skill, value)));
        }
    }

    public static void gainedSPA(final Campaign campaign, final Person person, final LocalDate date,
                                 final String spa) {
        if (campaign.getCampaignOptions().isPersonnelLogAbilityGain()) {
            person.addLogEntry(new PersonalLogEntry(date,
                    MessageFormat.format(resources.getString("gained.text"), spa)));
        }
    }

    public static void gainedEdge(final Campaign campaign, final Person person, final LocalDate date) {
        if (campaign.getCampaignOptions().isPersonnelLogEdgeGain()) {
            person.addLogEntry(new PersonalLogEntry(date,
                    MessageFormat.format(resources.getString("gainedEdge.text"), person.getEdge())));
        }
    }

    public static void changedEdge(final Campaign campaign, final Person person, final LocalDate date) {
        if (campaign.getCampaignOptions().isPersonnelLogEdgeGain()) {
            person.addLogEntry(new PersonalLogEntry(date,
                    MessageFormat.format(resources.getString("changedEdge.text"), person.getEdge())));
        }
    }

    public static void spouseConceived(Person person, String spouseName, LocalDate date, String sizeString) {
        String message = MessageFormat.format(resources.getString("spouseConceived.text"), spouseName);

        if (sizeString != null) {
            message += " " + sizeString;
        }

        person.addLogEntry(new PersonalLogEntry(date, message));
    }

    //this is called to log the child being born on the father's personal log
    public static void ourChildBorn(Person person, Person baby, String spouseName, LocalDate date) {
        person.addLogEntry(new PersonalLogEntry(date,
                MessageFormat.format(resources.getString("ourChildBorn.text"),
                        spouseName, GenderDescriptors.BOY_GIRL.getDescriptor(baby.getGender()))));
    }
}
