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
package mekhq.campaign.unit.actions;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.SpacecraftCoolingSystem;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.IPartWork;

/**
 * Strips a unit of its parts and adds them to the campaign.
 */
public class StripUnitAction implements IUnitAction {

    /**
     * Strips a unit of its parts and adds them to the campaign.
     *
     * @param campaign The campaign to add the parts to after removing them from the unit.
     * @param unit     The unit to remove the parts from.
     */
    @Override
    public void execute(Campaign campaign, Unit unit) {
        unit.setSalvage(true);
        for (IPartWork partWork : unit.getSalvageableParts()) {
            if (partWork instanceof SpacecraftCoolingSystem) {
                //Pull all available sinks out of the system
                int removableHeatSinks = ((SpacecraftCoolingSystem) partWork).getRemoveableSinks();
                while (removableHeatSinks > 0) {
                    partWork.succeed();
                    removableHeatSinks--;
                }
            } else {
                partWork.succeed();
            }
        }
    }
}
