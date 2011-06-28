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

import megamek.common.Aero;
import megamek.common.BattleArmor;
import megamek.common.Mech;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.work.IPartWork;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
    	this(null, 0, T_MECH);
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
	
	public boolean isRightType(Unit unit) {
		if(null == unit) {
			return true;
		}
		if(unit.getEntity() instanceof Mech) {
			return type == T_MECH;
		} 
		else if(unit.getEntity() instanceof Tank) {
			return type == T_MECHANIC;
		}
		else if(unit.getEntity() instanceof Aero) {
			return type == T_AERO;
		}
		else if(unit.getEntity() instanceof BattleArmor) {
			return type == T_BA;
		}
		return false;
	}
}
