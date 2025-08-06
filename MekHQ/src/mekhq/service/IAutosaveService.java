/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.service;

import mekhq.campaign.Campaign;

/**
 * Handles the possible auto-save situations in MekHQ.
 */
public interface IAutosaveService {
    /**
     * Handles auto-saving when the day of the campaign advances.
     *
     * @param campaign Campaign to save
     */
    void requestDayAdvanceAutosave(Campaign campaign);

    /**
     * Handles auto-saving before a scenario starts.
     *
     * @param campaign Campaign to save
     */
    void requestBeforeScenarioAutosave(Campaign campaign);

    /**
     * Handles auto-saving before a mission or contract ends.
     *
     * @param campaign Campaign to save
     */
    void requestBeforeMissionEndAutosave(Campaign campaign);
}
