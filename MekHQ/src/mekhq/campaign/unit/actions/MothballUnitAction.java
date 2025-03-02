/*
 * Copyright (C) 2019-2025 The MegaMek Team
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
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */
package mekhq.campaign.unit.actions;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Mothballs a unit.
 */
public class MothballUnitAction implements IUnitAction {
    private final Person tech;
    private final boolean isGM;

    /**
     * Initializes a new instance of the MothballUnitAction class.
     * @param tech The ID of the technician performing the work, or null
     *             if noone is needed to perform the work (self crewed or GM).
     * @param isGM A boolean value indicating whether or not GM mode should be used
     *             to complete the action.
     */
    public MothballUnitAction(@Nullable Person tech, boolean isGM) {
        this.tech = tech;
        this.isGM = isGM;
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (isGM) {
            unit.startMothballing(null, true);
        } else {
            if (!unit.isSelfCrewed() && (null == tech)) {
                return;
            }

            unit.startMothballing(tech);
        }
    }
}
