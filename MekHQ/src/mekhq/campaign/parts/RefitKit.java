/*
 * RefitKit.java
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

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.EquipmentType;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.Refit;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author natit
 */
public class RefitKit extends Part {
	private static final long serialVersionUID = 5712293563267818853L;
	protected String sourceName;
    protected String targetName;
    
    public RefitKit() {
    	this(false, 0, null, null, 0);
    }
    
    public RefitKit (boolean salvage, int tonnage, String sourceName, String targetName, long cost) {
        super(salvage, tonnage);
        this.sourceName = sourceName;
        this.targetName = targetName;
        this.cost = cost;
    }
    
    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof RefitKit
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && this.sourceName.equals(((RefitKit) part).sourceName)
                && this.targetName.equals(((RefitKit) part).targetName);
    }

    @Override
    public int getPartType() {
        return PART_TYPE_OTHER;
    }

    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        if (task instanceof Refit) {
            return task.getUnit().getEntity().getShortName().equals(this.sourceName)
                    && ((Refit) task).getTargetEntity().getShortName().equals(this.targetName);
        } else {
            return false;
        }
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<sourceName>"
				+sourceName
				+"</sourceName>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<targetName>"
				+targetName
				+"</targetName>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("sourceName")) {
				sourceName = wn2.getTextContent();
			} else if (wn2.getNodeName().equalsIgnoreCase("targetName")) {
				targetName = wn2.getTextContent();
			} 
		}
	}

	@Override
	public int getAvailability(int era) {
		return EquipmentType.RATING_A;
	}

	@Override
	public int getTechRating() {
		return EquipmentType.RATING_A;
	}

	@Override
	public void fix() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Part getMissingPart() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void remove(boolean salvage) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateConditionFromEntity() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean needsFixing() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void updateConditionFromPart() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String checkFixable() {
		// TODO Auto-generated method stub
		return null;
	}
}
