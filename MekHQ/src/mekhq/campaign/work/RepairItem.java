/*
 * RepairItem.java
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

import mekhq.campaign.Unit;
import mekhq.campaign.team.SupportTeam;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public abstract class RepairItem extends UnitWorkItem {

    int hits;
    //the id of a corresponding salvage item that must be removed if this repair is mutated
    //into a replacement
    int salvageId = NONE;
    
    public RepairItem(Unit unit, int h) {
        super(unit);
        this.hits = h;
    }
    
    @Override
    public String getDetails() {
        return hits + " hit(s)";
    }
    
    public abstract WorkItem getReplacementTask();
    public abstract void doReplaceChanges();

    public final WorkItem replace () {
        doReplaceChanges();
        return getReplacementTask();
    }
    
    protected void removeSalvage() {
        WorkItem salvage = unit.campaign.getTask(salvageId);
        if(null != salvage) {
            unit.campaign.removeTask(salvage);
        }
    }
    
    public int getSalvageId() {
        return salvageId;
    }
    
    public void setSalvageId(int id) {
        this.salvageId = id;
    }
    
    @Override
    protected String maxSkillReached() {
        //I don't want to automatically mutate the repair into a replacement
        //because the part may still be functional (like a gyro with one hit) and 
        //the player may prefer to leave it in its damaged state rather than rip it out
        //the player can decide to scrap the component by using the context manu
        //unit.campaign.mutateTask(this, replace());
        return "<br><emph><b>Item cannot be repaired, it must be replaced instead.</b></emph>";
    }
    
    @Override
    public String getToolTip() {
        if(getSkillMin() > SupportTeam.EXP_ELITE) {
            return "<html> This task is impossible.<br>You can use the right-click menu to scrap this component.</html>";
        } 
        return super.getToolTip();
    }
    
}
