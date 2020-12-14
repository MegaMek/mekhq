/*
 * Copyright (C) 2020 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.unit.actions;

import java.util.Objects;

import megamek.common.AmmoType;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PartChangedEvent;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.equipment.AmmoBin;
import mekhq.campaign.unit.Unit;

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
