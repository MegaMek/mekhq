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

import java.io.File;
import java.util.Enumeration;
import megamek.common.Aero;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Pilot;
import megamek.common.Protomech;
import megamek.common.Tank;
import megamek.common.options.IOption;
import megamek.common.options.IOptionGroup;
import mekhq.campaign.Campaign;
import mekhq.campaign.Unit;
import mekhq.campaign.work.HealPilot;

/**
 * A Person wrapper for pilots and vee crews
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class PilotPerson extends Person {

    public static final int T_MECH = 0;
    public static final int T_VEE = 1;
    public static final int T_AERO = 2;
    public static final int T_PROTO = 3;
    public static final int T_NUM = 4;
    
    private Pilot pilot;
    private int type;
    private Unit unit;
    
    public PilotPerson(Pilot p, int t) {
        super();
        this.pilot = p;
        this.type = t;
        this.portraitCategory = pilot.getPortraitCategory();
        this.portraitFile = pilot.getPortraitFileName();
    }
    
    public PilotPerson(Pilot p, int t, Unit u) {
        this(p,t);
        this.unit = u;
    }
    
    public int getType() {
        return type;
    }
    
    public static String getTypeDesc(int type) {
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
    
    public String getTypeDesc() {
        return getTypeDesc(type);
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
    
    public boolean canPilot(Entity en) {
        if(en instanceof Mech && type == T_MECH) {
            return true;
        }
        else if(en instanceof Protomech && type == T_PROTO) {
            return true;
        } 
        else if(en instanceof Aero && type == T_AERO) {
            return true;
        }
        else if(en instanceof Tank && type == T_VEE) {
            return true;
        }
        return false;
    }
    
    public Pilot getPilot() {
        return pilot;
    }
    
    public void setPilot(Pilot p) {
        this.pilot = p;
    }

    @Override
    public String getDesc() {
        String care = "";
        String status = "";
        if(pilot.getHits() > 0) {
            status = " (" + pilot.getStatusDesc() + ")";
        }
        return care + pilot.getName() + " [" + pilot.getGunnery() + "/" + pilot.getPiloting() + " " + getTypeDesc() + "]" + status; 
    }
    
    @Override
    public String getDescHTML() {
        String toReturn = "<html><font size='2'><b>" + pilot.getName() + "</b><br>";
        toReturn += getTypeDesc() + " (" + pilot.getGunnery() + "/" + pilot.getPiloting() + ")<br>";
        toReturn += pilot.getStatusDesc() + getAssignedDoctorString();
        if(isDeployed()) {
            toReturn += " (DEPLOYED)";
        }
        toReturn += "</font></html>";
        return toReturn;
    }
    
    public boolean isAssigned() {
        return null != unit;
    }
    
    public Unit getAssignedUnit() {
        return unit;
    }
    
    public void setAssignedUnit(Unit u) {
        this.unit = u;
    }
    
    @Override
    public void runDiagnostic(Campaign campaign) {
        if(pilot.getHits() > 0) {
            setTask(new HealPilot(this));
            campaign.addWork(getTask());
            
        }
    }
    
    /**
     * heal one hit on the pilot/crew
     */
    @Override
    public void heal() {
        if(needsHealing()) {
            getPilot().setHits(getPilot().getHits() - 1);
        }
        if(!needsHealing() && null != task) {
            task.complete();
        }
    }

    @Override
    public boolean needsHealing() {
        return (getPilot().getHits() > 0);
    }

    @Override
    public String getDossier() {
        File file = new File(".");
        String path = file.getAbsolutePath();
        
        path = path.replace(".", "");
        String category = getPortraitCategory();
        String image = getPortraitFileName();
        if(category.equals(Pilot.ROOT_PORTRAIT)) {
            category = "";
        }
        String toReturn = "<html><b>" + pilot.getName() + "</b><br>";
        toReturn += "<i>" + pilot.getNickname() + "</i><br><br>";
        if(!image.equals(Pilot.PORTRAIT_NONE)) {
            toReturn += "<img src=\"file://" + path + "data/images/portraits/" + category + image + "\"/>";
        }
        toReturn += "<table>";
        toReturn += "<tr><td><b>Gunnery:</b></td><td>" + pilot.getGunnery() + "</td></tr>";
        toReturn += "<tr><td><b>Piloting:</b></td><td>" + pilot.getPiloting() + "</td></tr>";
        toReturn += "<tr><td><b>Iniative Bonus:</b></td><td>" + pilot.getInitBonus() + "</td></tr>";
        toReturn += "<tr><td><b>Commander Bonus:</b></td><td>" + pilot.getCommandBonus() + "</td></tr>";
        toReturn += "</table>";
     
        for (Enumeration<IOptionGroup> advGroups = pilot.getOptions().getGroups(); advGroups.hasMoreElements();) {
            IOptionGroup advGroup = advGroups.nextElement();
            if(pilot.countOptions(advGroup.getKey()) > 0) {
                toReturn += "<p><b><u>" + advGroup.getDisplayableName() + "</u></b><br>";    
                for (Enumeration<IOption> advs = advGroup.getOptions(); advs.hasMoreElements();) {
                    IOption adv = advs.nextElement();
                    if(adv.booleanValue()) {
                        toReturn += "  " + adv.getDisplayableNameWithValue() + "<br>";
                    }
                }
            }
        }
        
        if(null != getBiography() && getBiography().length() > 0) {
            toReturn += "<br><br><b>Biography</b><br>";
            toReturn += getBiography();
        }
        toReturn += "</html>";
        
        
        return toReturn;
    }
    
    @Override
    public void setPortraitCategory(String s) {
        super.setPortraitCategory(s);
        pilot.setPortraitCategory(s);
        
    }

    @Override
    public void setPortraitFileName(String s) {
        super.setPortraitFileName(s);
        pilot.setPortraitFileName(s);
    }
}
