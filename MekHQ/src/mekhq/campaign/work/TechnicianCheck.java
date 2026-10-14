/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

import megamek.common.rolls.TargetRoll;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.skills.ActionCheck;

/**
 * Decides whether a Technician Skill Check for a repair, replacement, salvage or refit succeeded.
 *
 * <p>Campaign Operations (Extra Time, p. 208) says a result of 2 on the dice is always a failure, however low the
 * target number has been brought. Without this, an Elite tech taking extra time on easy work reaches a target of 2 or
 * less and can never fail. A target that needs no roll at all, such as reloading ammunition, still succeeds.</p>
 */
public final class TechnicianCheck {
    private static final MMLogger LOGGER = MMLogger.create(TechnicianCheck.class);

    private TechnicianCheck() {}

    /**
     * @param roll   the dice result
     * @param target the check's target number
     *
     * @return {@code true} if the roll succeeds against the target
     */
    public static boolean isSuccess(int roll, TargetRoll target) {
        int targetNumber = target.getValue();
        if (targetNumber == TargetRoll.AUTOMATIC_SUCCESS) {
            return true;
        }
        if (target.cannotSucceed()) {
            return false;
        }
        if (roll <= ActionCheck.ALWAYS_FAILING_ROLL) {
            if (roll >= targetNumber) {
                LOGGER.debug("[TechnicianCheck] a roll of {} fails although it meets target {}", roll, targetNumber);
            }
            return false;
        }
        return roll >= targetNumber;
    }
}
