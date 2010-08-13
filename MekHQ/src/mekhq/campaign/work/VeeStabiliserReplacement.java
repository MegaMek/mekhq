/*
 * VeeStabiliserReplacement.java
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

import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.Unit;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.VeeStabiliser;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
class VeeStabiliserReplacement extends ReplacementItem {
	private static final long serialVersionUID = 5283920941593252555L;
	private int loc;

	public VeeStabiliserReplacement() {
		this(null, 0);
	}
    
    public VeeStabiliserReplacement(Unit unit, int i) {
        super(unit);
        this.loc = i;
        this.time = 60;
        this.difficulty = 0;
        reCalc();
    }
    
    @Override
    public void reCalc() {
        if (unit == null)
        	return;
        
        this.name =  " Replace stabilizer (" + unit.getEntity().getLocationName(loc) + ")";

        super.reCalc();
    }

    @Override
    public void fix() {
        super.fix();
        //TODO: no method for setting the stabilizerhits to zero in Tank
    }
    
    public int getLoc() {
        return loc;
    }

    @Override
    public boolean sameAs(WorkItem task) {
        return (task instanceof VeeStabiliserReplacement
                && ((VeeStabiliserReplacement)task).getUnitId() == this.getUnitId()
                && ((VeeStabiliserReplacement)task).getLoc() == this.getLoc());
    }

    @Override
    public Part stratopsPartNeeded() {
        return new VeeStabiliser(false, (int) unit.getEntity().getWeight());
    }

    @Override
    public SalvageItem getSalvage() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<loc>"
				+loc
				+"</loc>");
		writeToXmlEnd(pw1, indent, id);
	}
	
	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("loc")) {
				loc = Integer.parseInt(wn2.getTextContent());
			}
		}
		
		super.loadFieldsFromXmlNode(wn);
	}
}
