/*
 * MekGyro.java
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

import megamek.common.Mech;
import mekhq.campaign.MekHqXmlUtil;
import mekhq.campaign.work.MekGyroReplacement;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekGyro extends Part {
	private static final long serialVersionUID = 3420475726506139139L;
	protected int type;
    protected int walkMP;

    public MekGyro() {
    	this(false, 0, 0, 0);
    	reCalc();
    }
    
	@Override
   public void reCalc() {
    	// Do nothing.
    }
    
    public int getType() {
        return type;
    }

    public int getWalkMP() {
        return walkMP;
    }
    
    public MekGyro(boolean salvage, int tonnage, int type, int walkMP) {
        super(salvage, tonnage);
        this.type = type;
        this.name = Mech.getGyroTypeString(type) + " (" + tonnage + ")";
        this.walkMP = walkMP;
        computeCost();
    }

    public static int getGyroBaseTonnage(int walkMP, int unitTonnage) {
    	return (int) Math.ceil(walkMP * unitTonnage / 100f);
    }
    
    private int getGyroBaseTonnage() {
    	return MekGyro.getGyroBaseTonnage(getWalkMP(), getTonnage());
    }
    
    public static double getGyroTonnage(double gyroBaseTonnage, int gyroType) {
        if (gyroType == Mech.GYRO_XL) {
            return gyroBaseTonnage * 0.5;
        } else if (gyroType == Mech.GYRO_COMPACT) {
        	return gyroBaseTonnage * 1.5;
        } else if (gyroType == Mech.GYRO_HEAVY_DUTY) {
        	return gyroBaseTonnage * 2;
        }
    	
        return gyroBaseTonnage;
    }
    
    public static double getGyroTonnage(int walkMP, int unitTonnage, int gyroType) {
    	return MekGyro.getGyroTonnage(MekGyro.getGyroBaseTonnage(walkMP, unitTonnage), gyroType);
    }
    
    public double getGyroTonnage() {
    	return MekGyro.getGyroTonnage(getGyroBaseTonnage(), getType());
    }
    
    private void computeCost() {
        double c = 0;
        
        if (getType() == Mech.GYRO_XL) {
            c = 750000 * getGyroTonnage();
        } else if (getType() == Mech.GYRO_COMPACT) {
            c = 400000 * getGyroTonnage();
        } else if (getType() == Mech.GYRO_HEAVY_DUTY) {
            c = 500000 * getGyroTonnage();
        } else {
            c = 300000 * getGyroTonnage();
        }
        
        this.cost = (long) Math.round(c);
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
    	// Gyro compatibility is based on type and gyro tonnage, not unit tonnage...
    	// But gyro tonnage isn't story, only unit tonnage and unit MP.
    	// Unit tonnage and unit walk MP are only relevant in calculating Gyro tonnage...
    	// But with type it's enough to calculate it from.
    	double unitGyroTonnage = MekGyro.getGyroTonnage((int) Math.ceil(((MekGyroReplacement)task).getUnit().getEntity().getEngine().getRating() / 100f), ((MekGyroReplacement)task).getUnit().getEntity().getGyroType());
    	
        return (task instanceof MekGyroReplacement 
                && ((MekGyroReplacement)task).getUnit().getEntity().getGyroType() == type
                && getGyroTonnage() == unitGyroTonnage);
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof MekGyro
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && getType() == ((MekGyro) part).getType()
                && getTonnage() == ((MekGyro) part).getTonnage();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_MEK_GYRO;
    }

    @Override
    public String getSaveString() {
        return getName() + ";" + getTonnage() + ";" + getType() + ";" + getWalkMP();
    }

	@Override
	public void writeToXml(PrintWriter pw1, int indent, int id) {
		writeToXmlBegin(pw1, indent, id);
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<type>"
				+type
				+"</type>");
		pw1.println(MekHqXmlUtil.indentStr(indent+1)
				+"<walkMP>"
				+walkMP
				+"</walkMP>");
		writeToXmlEnd(pw1, indent, id);
	}

	@Override
	protected void loadFieldsFromXmlNode(Node wn) {
		NodeList nl = wn.getChildNodes();
		
		for (int x=0; x<nl.getLength(); x++) {
			Node wn2 = nl.item(x);
			
			if (wn2.getNodeName().equalsIgnoreCase("type")) {
				type = Integer.parseInt(wn2.getTextContent());
			} else if (wn2.getNodeName().equalsIgnoreCase("walkMP")) {
				walkMP = Integer.parseInt(wn2.getTextContent());
			} 
		}
	}
}
