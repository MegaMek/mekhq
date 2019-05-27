/*
 * MissingKFHeliumTank.java
 * 
 * Copyright (c) 2019 MegaMek Team
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import megamek.common.Jumpship;
import megamek.common.TechAdvancement;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;

/**
 *
 * @author MKerensky
 */
public class MissingKFHeliumTank extends MissingPart {
    
    /**
     * 
     */
    private static final long serialVersionUID = 3672916192478319355L;
    
    //Standard, primitive, compact, subcompact...
    private int coreType;
    
    public int getCoreType() {
        return coreType;
    }
	
	public MissingKFHeliumTank() {
	    this(0, Jumpship.DRIVE_CORE_STANDARD, null);
    }
    
    public MissingKFHeliumTank(int tonnage, int coreType, Campaign c) {
    	super(0, c);
    	this.coreType = coreType;
        this.name = "K-F Helium Tank";
    }
    
    @Override 
	public int getBaseTime() {
        //BattleSpace, p28, *10
        return 1800;
	}
	
	@Override
	public int getDifficulty() {
	    //BattleSpace, p28
		return 4;
	}
    
	@Override
	public String checkFixable() {
		return null;
	}

	@Override
	public Part getNewPart() {
		return new KFHeliumTank(getUnitTonnage(), coreType, campaign);
	}

	@Override
	public boolean isAcceptableReplacement(Part part, boolean refit) {
		return part instanceof KFHeliumTank && coreType == ((KFHeliumTank)part).getCoreType();
	}

	@Override
	public double getTonnage() {
		return 0;
	}

	@Override
	public void updateConditionFromPart() {
	    if(null != unit && unit.getEntity() instanceof Jumpship) {
            ((Jumpship)unit.getEntity()).setKFHeliumTankHit(true);
    }
	}
	
	@Override
	public void writeToXml(PrintWriter pw1, int indent) {
		writeToXmlBegin(pw1, indent);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<coreType>"
                +coreType
                +"</coreType>");
		writeToXmlEnd(pw1, indent);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);		
			if (wn2.getNodeName().equalsIgnoreCase("coreType")) {
                coreType = Integer.parseInt(wn2.getTextContent());
            } 
		}
	}

	@Override
	public String getLocationName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getLocation() {
		return Jumpship.LOC_HULL;
	}
	
	@Override
	public TechAdvancement getTechAdvancement() {
	    return KFHeliumTank.TA_HELIUM_TANK;
	}
	
}
