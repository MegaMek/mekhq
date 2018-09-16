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
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.text.MessageFormat;
import java.util.Date;
import java.util.ResourceBundle;

/**
 * This class is responsible to control the logging of Award Log Entries.
 * @author Miguel Azevedo
 */
public class AwardLogger {
    private ResourceBundle logEntriesResourceMap;
    private static AwardLogger awardLogger;

    public AwardLogger() {
        this.logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries", new EncodeControl());
    }
    public static AwardLogger getInstance(){
        if(null == awardLogger){
            awardLogger = new AwardLogger();
        }
        return awardLogger;
    }

    public void award(Person person, Date date, Award award){
        String message = logEntriesResourceMap.getString("awarded.text");
        person.addLogEntry(new AwardLogEntry(date, MessageFormat.format(message, award.getName(), award.getDescription())));
    }

    public void removedAward(Person person, Date date, Award award){
        String message = logEntriesResourceMap.getString("removedAward.text");
        person.addLogEntry(new AwardLogEntry(date, MessageFormat.format(message, award.getName())));
    }
}
