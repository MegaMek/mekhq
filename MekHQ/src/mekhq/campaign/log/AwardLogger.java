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
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

/**
 * This class is responsible to control the logging of Award Log Entries.
 *
 * @author Miguel Azevedo
 */
public class AwardLogger {
    private static final transient ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle(
          "mekhq.resources.LogEntries",
          MekHQ.getMHQOptions().getLocale());

    public static void award(Person person, LocalDate date, Award award) {
        String message = logEntriesResourceMap.getString("awarded.text");
        person.addPersonalLogEntry(new AwardLogEntry(date,
              MessageFormat.format(message, award.getName(), award.getSet(), award.getDescription())));
    }

    public static void removedAward(Person person, LocalDate date, Award award) {
        String message = logEntriesResourceMap.getString("removedAward.text");
        person.addPersonalLogEntry(new AwardLogEntry(date,
              MessageFormat.format(message, award.getName(), award.getSet())));
    }

    /**
     * Finds the award corresponding to a log entry
     *
     * @param person       owner of the log entry
     * @param logEntryText text of the log entry
     *
     * @return award of the owner corresponding to the log entry text
     */
    public static @Nullable Award getAwardFromLogEntry(final @Nullable Person person, final String logEntryText) {
        if (person == null) {
            return null;
        }

        final String message = logEntriesResourceMap.getString("awarded.text");
        Pattern pattern = Pattern.compile(MessageFormat.format(message, "(.*)", "(.*)", "(.*)"));
        Matcher matcher = pattern.matcher(logEntryText);

        Award award = null;

        if (matcher.matches()) {
            award = person.getAwardController().getAward(matcher.group(2), matcher.group(1));
        } else {
            // In a first implementation, the award Set was not included in the log, so it is impossible to distinguish
            // awards with same name but in different set. So it assumes it is the first it finds, using the old message format.
            pattern = Pattern.compile("Awarded (.*): (.*)");
            matcher = pattern.matcher(logEntryText);
            if (matcher.matches()) {
                award = person.getAwardController().getFirstAwardIgnoringSet(matcher.group(1));
            }
        }
        return award;
    }
}
