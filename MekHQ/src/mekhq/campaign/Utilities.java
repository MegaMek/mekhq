/*
 * Utilities.java
 * 
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
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

package mekhq.campaign;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;
import megamek.common.Aero;
import megamek.common.AmmoType;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Protomech;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Utilities {

    public static int roll3d6() {      
        Vector<Integer> rolls = new Vector<Integer>();
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        rolls.add(Compute.d6());
        Collections.sort(rolls);
        return (rolls.elementAt(0) + rolls.elementAt(1));       
    }
    
    public static ArrayList<AmmoType> getMunitionsFor(Entity entity, AmmoType cur_atype) {
        ArrayList<AmmoType> atypes = new ArrayList<AmmoType>();
        for(AmmoType atype : AmmoType.getMunitionsFor(cur_atype.getAmmoType())) {
            //this is an abbreviated version of setupMunitions in the CustomMechDialog
            //TODO: clan/IS limitations?
            if ((entity instanceof Aero)
                        && !((atype.getAmmoType() == AmmoType.T_MML)
                                || (atype.getAmmoType() == AmmoType.T_ATM)
                                || (atype.getAmmoType() == AmmoType.T_NARC) 
                                || (atype.getAmmoType() == AmmoType.T_AC_LBX))) {
                continue;
            }

            // Only Protos can use Proto-specific ammo
            if (atype.hasFlag(AmmoType.F_PROTOMECH)
                            && !(entity instanceof Protomech)) {
                continue;
            }

            // When dealing with machine guns, Protos can only
            // use proto-specific machine gun ammo
            if ((entity instanceof Protomech)
                            && atype.hasFlag(AmmoType.F_MG)
                            && !atype.hasFlag(AmmoType.F_PROTOMECH)) {
                continue;
            }

            // Battle Armor ammo can't be selected at all.
            // All other ammo types need to match on rack size and tech.
            if ((atype.getRackSize() == cur_atype.getRackSize())
                            && (atype.hasFlag(AmmoType.F_BATTLEARMOR) == cur_atype.hasFlag(AmmoType.F_BATTLEARMOR))
                            && (atype.hasFlag(AmmoType.F_ENCUMBERING) == cur_atype.hasFlag(AmmoType.F_ENCUMBERING))
                            && (atype.getTonnage(entity) == cur_atype.getTonnage(entity))) {
                atypes.add(atype);
            }
        }
        return atypes;
    }
    
}
