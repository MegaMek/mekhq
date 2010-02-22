/*
 * MekSensor.java
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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import megamek.common.TechConstants;
import mekhq.campaign.work.ReplacementItem;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class GenericSparePart extends Part {

    public static int UNITARY_COST = 1;

    protected int tech;
    protected int amount;

    public GenericSparePart(int tech, int amount) {
        super(false, 0);
        this.tech = tech;
        this.amount = amount;
        
        this.name = "Spare Part " + TechConstants.getLevelName(tech);
        computeCost();
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
        computeCost();
    }

    @Override
    public int getTech () {
        return this.tech;
    }

    protected void computeCost () {
        this.cost = GenericSparePart.UNITARY_COST * getAmount();
    }
    
    @Override
    public boolean canBeUsedBy(ReplacementItem task) {
        return (task instanceof ReplacementItem
                && ((ReplacementItem) task).partNeeded().getTech() == getTech());
    }

    @Override
    public boolean isSamePartTypeAndStatus (Part part) {
        return part instanceof GenericSparePart
                && getName().equals(part.getName())
                && getStatus().equals(part.getStatus())
                && ((GenericSparePart) part).getTech() == getTech();
    }

    @Override
    public int getPartType() {
        return PART_TYPE_GENERIC_SPARE_PART;
    }

    public static char getAvailability () {
        return 'A';
    }

    @Override
    public String getSaveString () {
        return getName() + ";" + getTonnage() + ";" + getTech() + ";" + getAmount();
    }

    @Override
    public String getDesc() {
        return super.getDesc();
    }

    @Override
    public String getCostString () {
        NumberFormat numberFormat = DecimalFormat.getIntegerInstance();
        String text = numberFormat.format(getAmount()) + " " + (getAmount()!=0?"Parts":"Part");
        return text;
    }
    
}
