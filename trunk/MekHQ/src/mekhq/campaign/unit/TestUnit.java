/*
 * TestUnit.java
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

package mekhq.campaign.unit;

import megamek.common.Entity;
import mekhq.campaign.Campaign;

/**
 * This extension to units is for units that are not affiliated with the campaign and 
 * so methods applied to them should not be allowed to affect the campaign structure.
 * 
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class TestUnit extends Unit  {
	
	public TestUnit() {
        super(null, null);       
    }
    
    public TestUnit(Entity en, Campaign c) {
        super(en, c);
        initializeParts(false);
        runDiagnostic();
    }
    
    @Override
    public void initializeParts(boolean addParts) {
    	//always return false
    	super.initializeParts(false);
    }
	
}