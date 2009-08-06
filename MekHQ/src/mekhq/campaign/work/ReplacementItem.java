/*
 * ReplacementItem.java
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

package mekhq.campaign.work;

import megamek.common.TargetRoll;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.team.SupportTeam;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class ReplacementItem extends UnitWorkItem {

    protected Part part;
    
    public ReplacementItem(Unit unit) {
        super(unit);
    }
    
    public Part getPart() {
        return part;
    }
    
    public void setPart(Part part) {
        this.part = part;
    }
    
    public boolean hasPart() {
        return null != part;
    }
    
    /**
     * uses the part and if this depletes the part, returns true
     * @return
     */
    public void useUpPart() {
        if(hasPart()) {
            unit.campaign.removePart(part);
            this.part = null;
        }
    }
    
    @Override
    public void fix() {
        unit.campaign.addWork(getSalvage());
        useUpPart();
    }
    
    @Override
    public TargetRoll getAllMods() {
        TargetRoll target = super.getAllMods();
        if(null != part && part.isSalvage()) {
            target.addModifier(1, "salvaged part");
        }
        return target;
    }
    
    @Override
    protected String maxSkillReached() {
        useUpPart();
        //reset the skill min counter back to green
        setSkillMin(SupportTeam.EXP_GREEN);
        return "<br><emph><b>Component destroyed!</b></emph>";
    }
    
    public abstract Part partNeeded();
    
    public abstract SalvageItem getSalvage();
}
