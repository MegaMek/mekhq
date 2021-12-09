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

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

import java.time.LocalDate;

/**
 * This class is a specific log entry related to awards.
 * @author Miguel Azevedo
 */
public class AwardLogEntry extends LogEntry {
    public AwardLogEntry(LocalDate date, String desc) {
        super(date, desc, LogEntryType.AWARD);
    }

    @Override
    public void onLogEntryEdited(final LocalDate originalDate, final LocalDate newDate,
                                 final String originalDesc, final String newDesc,
                                 final @Nullable Person person) {
        final Award award = AwardLogger.getAwardFromLogEntry(person, originalDesc);
        if (award != null) {
            award.replaceDate(originalDate, newDate);
        }
    }
}
