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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.unit.actions;

import java.util.HashMap;
import java.util.Map;

import megamek.common.equipment.AmmoMounted;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.unit.Unit;

/**
 * Checks for additional ammo bins and adds the appropriate part.
 * <p>
 * Large craft can combine all the ammo of a single type into a single bin. Switching the munition type of one or more
 * tons of ammo can require the addition of an ammo bin and can change the ammo bin capacity.
 */
public class AdjustLargeCraftAmmoAction implements IUnitAction {

    @Override
    public void execute(Campaign campaign, Unit unit) {
        if (!unit.getEntity().usesWeaponBays()) {
            return;
        }

        Map<Integer, LargeCraftAmmoBin> ammoParts = new HashMap<>();
        for (Part part : unit.getParts()) {
            if (part instanceof LargeCraftAmmoBin ammoBin) {
                ammoParts.put(ammoBin.getEquipmentNum(), ammoBin);
            }
        }

        for (AmmoMounted ammoMounted : unit.getEntity().getAmmo()) {
            int eqNum = unit.getEntity().getEquipmentNum(ammoMounted);
            LargeCraftAmmoBin part = ammoParts.get(eqNum);
            if (null == part) {
                part = new LargeCraftAmmoBin((int) unit.getEntity().getWeight(), ammoMounted.getType(), eqNum,
                      ammoMounted.getOriginalShots() - ammoMounted.getBaseShotsLeft(), ammoMounted.getSize(), campaign);

                // Add the part to the unit
                unit.addPart(part);

                // Add the part to the bay (NOTE: must be on a unit first)
                part.setBay(unit.getEntity().getBayByAmmo(ammoMounted));

                // Add the part to the Campaign
                campaign.getQuartermaster().addPart(part, 0, false);
            } else {
                // Reset the AmmoType
                part.changeMunition(ammoMounted.getType());
            }
        }
    }
}
