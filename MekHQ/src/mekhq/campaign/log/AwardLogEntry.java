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

import java.time.LocalDate;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.personnel.Person;

/**
 * This class is a specific log entry related to awards.
 *
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
