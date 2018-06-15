/*
 * Copyright (c) 2018  - The MegaMek Team
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
package mekhq.campaign.market;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import megamek.common.Compute;
import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.rating.IUnitRating;
import mekhq.module.api.PersonnelMarketMethod;

/**
 * Generation method for personnel market based on Field Manual: Mercenaries (Revised)
 * 
 * @author Neoancient
 *
 */
public class PersonnelMarketFMMr implements PersonnelMarketMethod {

    @Override
    public String getModuleName() {
        return "FM: Mercenaries Revised";
    }

    @Override
    public List<Person> generatePersonnelForDay(Campaign c) {
        if (c.getCalendar().get(Calendar.DAY_OF_MONTH) != 1) {
            return null;
        }
        List<Person> retVal = new ArrayList<>();
        int q = 0;
        long mft = PersonnelMarket.getUnitMainForceType(c);
        int mftMod = 0;
        if (mft == Entity.ETYPE_MECH || mft == Entity.ETYPE_TANK || mft == Entity.ETYPE_INFANTRY || mft == Entity.ETYPE_BATTLEARMOR) {
            mftMod = 1;
        }
        for (int i = Person.T_NONE + 1; i < Person.T_NUM; i++) {
            int roll = Compute.d6(2);
            // TODO: Modifiers for hiring hall, but first needs to track the hiring hall
            switch(c.getUnitRatingMod()) {
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
                Person p = c.newPerson(i);
                UUID id = UUID.randomUUID();
                p.setId(id);

                retVal.add(p);
            }
        }
        return retVal;
    }

    @Override
    public List<Person> removePersonnelForDay(Campaign c, List<Person> current) {
        if (c.getCalendar().get(Calendar.DAY_OF_MONTH) == 1) {
            return current;
        } else {
            return null;
        }
    }

}
