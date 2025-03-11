/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.unit.actions;

import megamek.common.AmmoType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;

import java.util.Objects;

/**
 * Swaps the {@code AmmoType} for an {@code AmmoBin} on a {@code Unit}.
 */
public class SwapAmmoTypeAction implements IUnitAction {

    private final AmmoBin ammoBin;
    private final AmmoType ammoType;

    /**
     * Initializes a new instance of the {@code SwapAmmoTypeAction} class.
     * @param ammoBin The {@code AmmoBin} to swap ammo.
     * @param ammoType The new {@code AmmoType} to use in the {@code ammoBin}.
     */
    public SwapAmmoTypeAction(AmmoBin ammoBin, AmmoType ammoType) {
        this.ammoBin = Objects.requireNonNull(ammoBin);
        this.ammoType = Objects.requireNonNull(ammoType);
    }

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (!Objects.equals(ammoBin.getUnit(), unit)) {
            return;
        }

        ammoBin.changeMunition(ammoType);
        MekHQ.triggerEvent(new PartChangedEvent(ammoBin));
        MekHQ.triggerEvent(new UnitChangedEvent(unit));
    }
}
