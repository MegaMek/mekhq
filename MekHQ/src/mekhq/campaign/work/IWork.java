/*
 * IWork.java
 * 
 * Copyright (C) 2016 MegaMek team
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
package mekhq.campaign.work;

import java.util.UUID;

import megamek.common.TargetRoll;
import mekhq.campaign.personnel.Person;

public interface IWork {
    boolean needsFixing();
    
    /** @return the base difficulty of this work unit */
    int getDifficulty();
    
    TargetRoll getAllMods(Person p);
    
    String succeed();
    
    String fail(int rating);
    
    /** @return the team UUID assigned to this work unit, or <tt>null</tt> if nobody is working on it */
    UUID getTeamId();

    /**
     * @return the current work time modifier set for this work unit; only override if the work
     * unit supports more than the default, constant work time
     */
    default WorkTime getMode() {
        return WorkTime.NORMAL;
    }
}