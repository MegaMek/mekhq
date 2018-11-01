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

import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.util.Date;

/**
 * This class is a specific log entry related to awards.
 * @author Miguel Azevedo
 */
public class AwardLogEntry extends LogEntry {

    public AwardLogEntry(Date date, String desc){
        super(date, desc, LogEntryType.AWARD);
    }

    @Override
    public void onLogEntryEdited(Date originalDate, Date newDate, String originalDesc, String newDesc, Person person) {
        Award award = AwardLogger.getAwardFromLogEntry(person, originalDesc);
        award.replaceDate(originalDate, newDate);
    }
}
