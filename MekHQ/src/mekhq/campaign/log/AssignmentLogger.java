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

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.personnel.Person;

/**
 * This class is responsible to control the logging of unit assignment Log Entries.
 *
 * @author Miguel Azevedo
 */
public class AssignmentLogger {
    private static final MMLogger logger = MMLogger.create(AssignmentLogger.class);

    private static final ResourceBundle logEntriesResourceMap = ResourceBundle.getBundle("mekhq.resources.LogEntries",
          MekHQ.getMHQOptions().getLocale());

    public static void assignedTo(Person person, LocalDate date, String unitName) {
        String message = logEntriesResourceMap.getString("assignedTo.text");
        person.addPersonalLogEntry(new AssignmentLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public static void reassignedTo(Person person, LocalDate date, String unitName) {
        String message = logEntriesResourceMap.getString("reassignedTo.text");
        person.addPersonalLogEntry(new AssignmentLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public static void removedFrom(Person person, LocalDate date, String unitName) {
        String message = logEntriesResourceMap.getString("removedFrom.text");
        person.addPersonalLogEntry(new AssignmentLogEntry(date, MessageFormat.format(message, unitName)));
    }

    public static void addedToTOEForce(Campaign campaign, Person person, LocalDate date, Force force) {
        if (force != null) {
            String message = logEntriesResourceMap.getString("addToTOEForce.text");
            person.addPersonalLogEntry(new AssignmentLogEntry(date,
                  MessageFormat.format(message,
                        campaign.getCampaignOptions().isUseExtendedTOEForceName() ?
                              force.getFullName() :
                              force.getName())));
        }
    }

    public static void reassignedTOEForce(final Campaign campaign, final Person person, final LocalDate date,
          final @Nullable Force oldForce, final @Nullable Force newForce) {
        if ((oldForce == null) && (newForce == null)) {
            logger.error("Cannot reassign {} on {} because both specified forces are null",
                  person.getFullTitle(),
                  date);
            return;
        }

        if ((oldForce != null) && (newForce != null)) {
            String message = logEntriesResourceMap.getString("reassignedTOEForce.text");
            person.addPersonalLogEntry(new AssignmentLogEntry(date,
                  MessageFormat.format(message,
                        campaign.getCampaignOptions().isUseExtendedTOEForceName() ?
                              oldForce.getFullName() :
                              oldForce.getName(),
                        campaign.getCampaignOptions().isUseExtendedTOEForceName() ?
                              newForce.getFullName() :
                              newForce.getName())));
        } else if (oldForce == null) {
            addedToTOEForce(campaign, person, date, newForce);
        } else {
            removedFromTOEForce(campaign, person, date, oldForce);
        }
    }

    public static void removedFromTOEForce(Campaign campaign, Person person, LocalDate date, Force force) {
        if (force != null) {
            String message = logEntriesResourceMap.getString("removedFromTOEForce.text");
            person.addPersonalLogEntry(new AssignmentLogEntry(date,
                  MessageFormat.format(message,
                        campaign.getCampaignOptions().isUseExtendedTOEForceName() ?
                              force.getFullName() :
                              force.getName())));
        }
    }
}
