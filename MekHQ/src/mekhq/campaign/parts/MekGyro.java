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

    private void computeCost () {
        double c = 0;
        if (getType() == Mech.GYRO_XL) {
            c = 750000 * (int) Math.ceil(getWalkMP() * getTonnage() / 100f) * 0.5;
        } else if (getType() == Mech.GYRO_COMPACT) {
            c = 400000 * (int) Math.ceil(getWalkMP() * getTonnage() / 100f) * 1.5;
        } else if (getType() == Mech.GYRO_HEAVY_DUTY) {
            c = 500000 * (int) Math.ceil(getWalkMP() * getTonnage() / 100f) * 2;
        } else {
            c = 300000 * (int) Math.ceil(getWalkMP() * getTonnage() / 100f);
        }
        this.cost = (int) Math.round(c);
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        // TODO Is walk mp important for gyro compatibility ?
        // walk mp is used in cost calculation
        return (task instanceof MekGyroReplacement 
                && ((MekGyroReplacement)task).getUnit().getEntity().getGyroType() == type
                && tonnage == ((MekGyroReplacement)task).getUnit().getEntity().getWeight());
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
    public String getSaveString () {
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
}
