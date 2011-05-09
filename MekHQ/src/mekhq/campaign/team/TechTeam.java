/*
 * TechTeam.java
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

package mekhq.campaign.team;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Compute;
import megamek.common.Entity;
import megamek.common.Mech;
import megamek.common.Tank;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Utilities;
import mekhq.campaign.work.UnitWorkItem;
import mekhq.campaign.work.WorkItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class TechTeam extends SupportTeam {
	private static final long serialVersionUID = -4446125046165470664L;
	public static final int T_MECH = 0;
    public static final int T_MECHANIC = 1;
    public static final int T_AERO = 2;
    public static final int T_BA = 3;
    public static final int T_NUM = 4;
    
    private int type;
    
    public TechTeam() {
    	this(null, EXP_GREEN, T_MECH);
    }
    
    public TechTeam(String name, int rating, int type) {
        super(name, rating);
        this.type = type;
        this.fullSize = 7;
        this.currentSize = 7;
        reCalc();
    }
    
    @Override
    public void reCalc() {
    	// Do nothing.
    }
    
    @Override
    public String getTypeDesc() {
        return getTypeDesc(type);
    }
    
    public static String getTypeDesc(int type) {
        switch(type) {
            case T_MECH:
                return "Mech Tech";
            case T_MECHANIC:
                return "Mechanic";
            case T_AERO:
                return "Aero Tech";
            case T_BA:
                return "BA Tech";
        }
        
        return "?? Tech";
    }
    
    public int getType() {
    	return type;
    }
    
    public void setType(int t) {
    	this.type = t;
    }
    
    @Override
    public int getSkillBase(int effectiveRating) {
        int base = 11;
         switch(effectiveRating) {
           case SupportTeam.EXP_GREEN:
               base = 9;
               break;
           case SupportTeam.EXP_REGULAR:
               base = 7;
               break;
           case SupportTeam.EXP_VETERAN:
               base = 6;
               break;
           case SupportTeam.EXP_ELITE:
               base = 5;
               break;
       }
       return base;
    }
   
   @Override
   public boolean canDo(WorkItem task) {
        return true; 

   } 
    
   @Override
   public int makeRoll(WorkItem task) {
       if(task instanceof UnitWorkItem && isRightType(((UnitWorkItem)task).getUnit().getEntity())) {
           return Compute.d6(2);
       } else {
           return Utilities.roll3d6();
       }
   }
   
   public boolean isRightType(Entity en) {
       if((type == T_MECH && !(en instanceof Mech)) 
               || (type == T_MECHANIC && !(en instanceof Tank))
               || (type == T_AERO && !(en instanceof Aero))
               || (type == T_BA && !(en instanceof BattleArmor))) {
           return false;
       }               
       return true;
   }
   
   @Override
   public String getDescHTML() {
        String toReturn = "<html><font size='2'><b>" + getName() + "</b><br/>";
        toReturn += getRatingName() + " " + getTypeDesc() + "<br/>";
        toReturn += getMinutesLeft() + " minutes left";
        if(campaign.isOvertimeAllowed()) {
            toReturn += " + (" + getOvertimeLeft() + " overtime)";
        }
        toReturn += "</font></html>";
        return toReturn;
   }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			}
		}
	}
}
