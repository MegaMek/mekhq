/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
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
 */

package mekhq.campaign.unit.actions;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Activates a unit.
 */
public class ActivateUnitAction implements IUnitAction {

    private final Person tech;
    private final boolean isGM;

    /**
     * Initializes a new instance of the ActivateUnitAction class.
     * @param tech The technician performing the work, or null
     *             if no one is needed to perform the work (self crewed or GM).
     * @param isGM A boolean value indicating whether or not GM mode should be used
     *             to complete the action.
     */
    public ActivateUnitAction(@Nullable Person tech, boolean isGM) {
        this.tech = tech;
        this.isGM = isGM;
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (isGM) {
            unit.startActivating(null, true);
        }
        else {
            if (!unit.isSelfCrewed() && (null == tech)) {
                return;
            }

            unit.startActivating(tech);
        }
    }
}
