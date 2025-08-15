/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.work;

import megamek.common.TargetRoll;
import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;

public interface IWork {
    boolean needsFixing();

    /**
     * @return the base difficulty of this work unit
     */
    int getDifficulty();

    TargetRoll getAllMods(Person p);

    String succeed();

    String fail(int rating);

    /**
     * @return the team assigned to this work unit, or <code>null</code> if nobody is working on it
     */
    @Nullable
    Person getTech();

    /**
     * @return the current work time modifier set for this work unit; only override if the work unit supports more than
     *       the default, constant work time
     */
    default WorkTime getMode() {
        return WorkTime.NORMAL;
    }
}
