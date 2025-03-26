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
 */
package mekhq.campaign.market;

import java.util.ArrayList;
import java.util.List;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.IUnitRating;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Generation method for personnel market based on Field Manual: Mercenaries (Revised)
 *
 * @author Neoancient
 */
public class PersonnelMarketFMMr implements PersonnelMarketMethod {

    @Override
    public String getModuleName() {
        return "FM: Mercenaries Revised";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        if (c.getLocalDate().getDayOfMonth() != 1) {
            return null;
        }
        List<Person> retVal = new ArrayList<>();
        int q = 0;
        long mft = PersonnelMarket.getUnitMainForceType(c);
        int mftMod = 0;
        if (mft == Entity.ETYPE_MEK ||
                  mft == Entity.ETYPE_TANK ||
                  mft == Entity.ETYPE_INFANTRY ||
                  mft == Entity.ETYPE_BATTLEARMOR) {
            mftMod = 1;
        }
        for (PersonnelRole role : PersonnelRole.getMarketableRoles()) {
            int roll = Compute.d6(2);
            // TODO: Modifiers for hiring hall, but first needs to track the hiring hall
            switch (c.getAtBUnitRatingMod()) {
                case IUnitRating.DRAGOON_A:
                case IUnitRating.DRAGOON_ASTAR:
                    roll += 3;
                    break;
                case IUnitRating.DRAGOON_B:
                    roll += 2;
                    break;
                case IUnitRating.DRAGOON_C:
                    roll += 1;
                    break;
                case IUnitRating.DRAGOON_D:
                    roll -= 1;
                    break;
                case IUnitRating.DRAGOON_F:
                    roll -= 2;
                    break;
            }
            roll += mftMod;
            roll = Math.max(roll, 0);
            if (roll < 4) {
                q = 0;
            } else if (roll < 6) {
                q = 1;
            } else if (roll < 9) {
                q = 2;
            } else if (roll < 11) {
                q = 3;
            } else if (roll < 14) {
                q = 4;
            } else if (roll < 16) {
                q = 5;
            } else {
                q = 6;
            }
            for (int j = 0; j < q; j++) {
                retVal.add(c.newPerson(role));
            }
        }
        return retVal;
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        if (c.getLocalDate().getDayOfMonth() == 1) {
            return current;
        } else {
            return null;
        }
    }
}
