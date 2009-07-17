/*
 * PilotPerson.java
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

package mekhq.campaign.personnel;

import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Pilot;
import megamek.common.Protomech;
import megamek.common.Tank;

/**
 * A Person wrapper for pilots and vee crews
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PilotPerson extends Person {

    private static final int T_MECH = 0;
    private static final int T_VEE = 1;
    private static final int T_AERO = 2;
    private static final int T_PROTO = 3;
    private static final int T_NUM = 4;
    
    private Pilot pilot;
    private int type;
    
    public PilotPerson(Pilot p, int t) {
        super();
        this.pilot = p;
        this.type = t;
    }
    
    public int getType() {
        return type;
    }
    
    public String getTypeDesc() {
        switch(type) {
            case(T_MECH):
                return "Mechwarrior";
            case(T_VEE):
                return "Vehicle crew";
            case(T_AERO):
                return "Aero Pilot";
            case(T_PROTO):
                return "Proto Pilot";
            default:
                return "??";
        }
    }
    
    public static int getType(Entity en) {
        if(en instanceof Mech) {
            return T_MECH;
        }
        else if(en instanceof Protomech) {
            return T_PROTO;
        } 
        else if(en instanceof Aero) {
            return T_AERO;
        }
        else if(en instanceof Tank) {
            return T_VEE;
        }
        return -1;
    }
    
    public Pilot getPilot() {
        return pilot;
    }

    @Override
    public String getDesc() {
        String status = "";
        if(pilot.getHits() > 0) {
            status = " (" + pilot.getStatusDesc() + ")";
        }
        return pilot.getName() + " [" + pilot.getGunnery() + "/" + pilot.getPiloting() + " " + getTypeDesc() + "]" + status; 
    }
    
    
}
