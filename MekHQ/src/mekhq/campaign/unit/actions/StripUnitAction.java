/*
 * StripUnitAction.java
 *
 * Copyright (C) 2018 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
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

package mekhq.campaign.unit.actions;

import mekhq.campaign.Campaign;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;

/**
 * Strips a unit of its parts and adds them to the campaign.
 */
public class StripUnitAction implements IUnitAction {

    /**
     * Strips a unit of its parts and adds them to the campaign.
     * @param campaign The campaign to add the parts to after removing them from the unit.
     * @param unit The unit to remove the parts from.
     */
    public void Execute(Campaign campaign, Unit unit) {
        unit.setSalvage(true);
        for (IPartWork partWork : unit.getSalvageableParts()) {
            partWork.succeed();
        }
    }
}
