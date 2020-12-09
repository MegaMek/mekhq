/*
 * Copyright (c) 2020 Megamek Team. All rights reserved.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.AmmoType;
import megamek.common.Mounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.unit.Unit;

/**
 * Checks for additional ammo bins and adds the appropriate part.
 *
 * Large craft can combine all the ammo of a single type into a single bin. Switching the munition type
 * of one or more tons of ammo can require the addition of an ammo bin and can change the ammo bin
 * capacity.
 */
public class AdjustLargeCraftAmmoAction implements IUnitAction {

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (!unit.getEntity().usesWeaponBays()) {
            return;
        }

        Map<Integer, LargeCraftAmmoBin> ammoParts = new HashMap<>();
        for (Part p : unit.getParts()) {
            if (p instanceof LargeCraftAmmoBin) {
                LargeCraftAmmoBin ammoBin = (LargeCraftAmmoBin) p;
                ammoParts.put(ammoBin.getEquipmentNum(), ammoBin);
            }
        }

        List<Part> toAdd = new ArrayList<>();
        for (Mounted m : unit.getEntity().getAmmo()) {
            assert(m.getType() instanceof AmmoType);

            int eqNum = unit.getEntity().getEquipmentNum(m);
            LargeCraftAmmoBin part = ammoParts.get(eqNum);
            if (null == part) {
                part = new LargeCraftAmmoBin((int) unit.getEntity().getWeight(), (AmmoType) m.getType(), eqNum,
                        m.getOriginalShots() - m.getBaseShotsLeft(), m.getSize(), campaign);
                part.setBay(unit.getEntity().getBayByAmmo(m));
                toAdd.add(part);
            } else {
                // Reset the AmmoType
                part.changeMunition((AmmoType) m.getType());
            }
        }

        for (Part p : toAdd) {
            unit.addPart(p);
            campaign.getQuartermaster().addPart(p, 0);
        }
    }
}
