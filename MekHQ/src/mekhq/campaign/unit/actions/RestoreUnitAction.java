/*
 * Copyright (c) 2020 The Megamek Team. All rights reserved.
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

import java.util.*;

import megamek.common.*;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.UnitChangedEvent;
import mekhq.campaign.parts.*;
import mekhq.campaign.parts.equipment.*;
import mekhq.campaign.unit.Unit;

/**
 * Restores a unit to an undamaged state.
 */
public class RestoreUnitAction implements IUnitAction {

    @Override
    public void execute(Campaign campaign, Unit unit) {
        unit.setSalvage(false);

        boolean needsCheck = true;
        while (unit.isAvailable() && needsCheck) {
            needsCheck = false;
            List<Part> parts = new ArrayList<>(unit.getParts());
            for (Part part : parts) {
                if (part instanceof MissingPart) {
                    //Make sure we restore both left and right thrusters
                    if (part instanceof MissingThrusters) {
                        if (((Aero) unit.getEntity()).getLeftThrustHits() > 0) {
                            ((MissingThrusters) part).setLeftThrusters(true);
                        }
                    }
                    // We magically acquire a replacement part, then fix the missing one.
                    campaign.getQuartermaster().addPart(((MissingPart) part).getNewPart(), 0);
                    part.fix();
                    part.resetTimeSpent();
                    part.resetOvertime();
                    part.setTech(null);
                    part.cancelReservation();
                    part.remove(false);
                    needsCheck = true;
                } else {
                    if (part.needsFixing()) {
                        needsCheck = true;
                        part.fix();
                    } else {
                        part.resetRepairSettings();
                    }
                    part.resetTimeSpent();
                    part.resetOvertime();
                    part.setTech(null);
                    part.cancelReservation();
                }

                // replace damaged armor and reload ammo bins after fixing their respective locations
                if (part instanceof Armor) {
                    final Armor armor = (Armor) part;
                    armor.setAmount(armor.getTotalAmount());
                } else if (part instanceof AmmoBin) {
                    final AmmoBin ammoBin = (AmmoBin) part;

                    // we magically find the ammo we need, then load the bin
                    // we only want to get the amount of ammo the bin actually needs
                    if (ammoBin.getShotsNeeded() > 0) {
                        ammoBin.setShotsNeeded(0);
                        ammoBin.updateConditionFromPart();
                    }
                }

            }

            // TODO: Make this less painful. We just want to fix hips and shoulders.
            Entity entity = unit.getEntity();
            if (entity instanceof Mech) {
                for (int loc : new int[] {
                    Mech.LOC_CLEG, Mech.LOC_LLEG, Mech.LOC_RLEG, Mech.LOC_LARM, Mech.LOC_RARM}) {
                    int numberOfCriticals = entity.getNumberOfCriticals(loc);
                    for (int crit = 0; crit < numberOfCriticals; ++ crit) {
                        CriticalSlot slot = entity.getCritical(loc, crit);
                        if (null != slot) {
                            slot.setHit(false);
                            slot.setDestroyed(false);
                        }
                    }
                }
            }
        }

        MekHQ.triggerEvent(new UnitChangedEvent(unit));
    }
}
