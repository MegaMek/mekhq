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
package mekhq.campaign.unit.actions;

import java.util.Objects;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.parts.PartChangedEvent;
import mekhq.campaign.events.units.UnitChangedEvent;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;

/**
 * Unloads the {@code AmmoType} for an {@code AmmoBin} on a {@code Unit}, it will not change the {@code AmmoType}.
 */
public record UnloadAmmoTypeAction(AmmoBin ammoBin) implements IUnitAction {

    /**
     * Initializes a new instance of the {@code UnloadAmmoTypeAction} class.
     *
     * @param ammoBin The {@code AmmoBin} to unload.
     */
    public UnloadAmmoTypeAction(AmmoBin ammoBin) {
        this.ammoBin = Objects.requireNonNull(ammoBin);
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (!Objects.equals(ammoBin.getUnit(), unit)) {
            return;
        }
        ammoBin.unload();
        MekHQ.triggerEvent(new PartChangedEvent(ammoBin));
        MekHQ.triggerEvent(new UnitChangedEvent(unit));

    }
}
