/*
 * MekInternalRepair.java
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

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekInternalRepair extends InternalRepair {
	private static final long serialVersionUID = -4275717871512159638L;
	double percent;

	public MekInternalRepair() {
		this(null, 0, 0);
	}
   
    public MekInternalRepair(Unit unit, int i, double pct) {
        super(unit, i);
        this.time = 90;
        this.difficulty = -1;
        this.percent = pct;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (percent > 0.75) {
            this.time = 270;
            this.difficulty = 2;
        } else if (percent > 0.5) {
            this.time = 180;
            this.difficulty = 1;
        } else if (percent > 0.25) {
            this.time = 135;
            this.difficulty = 0;
        }

        super.reCalc();
    }

    @Override
    public String getDetails() {
        String perString = Integer.toString((int)Math.floor(percent * 100)) + "% damage";
        return unit.getEntity().getLocationName(loc) + ", " + perString;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof MekInternalRepair
                && ((MekInternalRepair)task).getUnitId() == this.getUnitId()
                && ((MekInternalRepair)task).getLoc() == this.getLoc());
    }
    
    
    @Override
    public boolean canScrap() {
        return loc != Mech.LOC_CT;
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<percent>"
				+percent
				+"</percent>");
		
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("percent")) {
				percent = Double.parseDouble(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
