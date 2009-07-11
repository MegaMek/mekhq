/*
 * Unit.java
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

import java.io.Serializable;
import java.util.ArrayList;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mech;
import mekhq.campaign.work.*;

/**
 * This is a wrapper class for entity, so that we can add some 
 * functionality to it
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class Unit implements Serializable {

    private Entity entity;
    
    public Unit(Entity en) {
        this.entity = en;
    }
    
    public Entity getEntity() {
        return entity;
    }
    
    public int getId() {
        return getEntity().getId();
    }
    
    //definitely need to refactor this but I can put it here now
    /**
     * Run a diagnostic on the given entity and build and ArrayList of WorkItems to return
     * TODO: this should really be a function in Entity, I should create a wrapper function
     *       for entity
     */
    public void runDiagnostic(Campaign campaign) {
        
        //check armor replacement
        for(int i = 0; i < entity.locations(); i++) {
            //TODO: get rear locations as well
            int diff = entity.getOArmor(i) - entity.getArmor(i);
            if(diff > 0) {
                campaign.addWork(new ArmorReplacement(this, i, diff, false));
            }
        }
        
        if(entity instanceof Mech) {
            Mech mech = (Mech)entity;
            
            //is the gyro destroyed?
            int gyroHits = entity.getHitCriticals(CriticalSlot.TYPE_SYSTEM, Mech.SYSTEM_GYRO, Mech.LOC_CT);
            if((gyroHits > 1 && entity.getGyroType() != Mech.GYRO_HEAVY_DUTY) 
                    || (gyroHits > 2 && entity.getGyroType() == Mech.GYRO_HEAVY_DUTY)) {
                campaign.addWork(new MekGyroReplacement(this));
            }  else if(gyroHits > 0) {
                campaign.addWork(new MekGyroRepair(this, gyroHits));
            }
        }
    }
    
}
