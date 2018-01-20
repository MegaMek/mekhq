/*
 * ProtomechArmor.java
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

import megamek.common.TechAdvancement;
import mekhq.campaign.Campaign;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class ProtomekArmor extends Armor implements IAcquisitionWork {
    private static final long serialVersionUID = 5275226057484468868L;

    public ProtomekArmor() {
        this(0, 0, -1, false, null);
    }

    public ProtomekArmor(int tonnage, int points, int loc, boolean clan, Campaign c) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage, -1, points, loc, false, clan, c);
        this.name = "Protomech Armor";
    }

    @Override
    public ProtomekArmor clone() {
        ProtomekArmor clone = new ProtomekArmor(0, 0, amount, clan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        return 50 * amount/1000.0;
    }

    @Override
    public long getCurrentValue() {
        return amount * 625;
    }

    public double getTonnageNeeded() {
        double armorPerTon = 20;
        return amountNeeded / armorPerTon;
    }

    public long getValueNeeded() {
        return adjustCostsForCampaignOptions((long)(amountNeeded * 625));
    }

    @Override
    public long getStickerPrice() {
        //always in 5-ton increments
        return (long)(5 * 20 * 625);
    }

    @Override
    public long getBuyCost() {
        return getStickerPrice();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof ProtomekArmor
                && isClanTechBase() == part.isClanTechBase()
                && getRefitId() == part.getRefitId();
    }

    @Override
    protected boolean isClanTechBase() {
        return clan;
    }

    public double getArmorWeight(int points) {
        return points * 50/1000.0;
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new ProtomekArmor(0, (int)Math.round(5 * getArmorPointsPerTon()), -1, clan, campaign);
    }

	@Override
	public int getDifficulty() {
		return -2;
	}

    public double getArmorPointsPerTon() {
        return 20;
    }

    public Part getNewPart() {
        return new ProtomekArmor(0, (int)Math.round(5 * getArmorPointsPerTon()), -1, clan, campaign);
    }

    public int getAmountAvailable() {
        for(Part part : campaign.getSpareParts()) {
            if(part instanceof ProtomekArmor) {
                ProtomekArmor a = (ProtomekArmor)part;
                if(a.isClanTechBase() == clan && !a.isReservedForRefit() && a.isPresent()) {
                    return a.getAmount();
                }
            }
        }
        return 0;
    }

    public void changeAmountAvailable(int amount) {
        ProtomekArmor a = null;
        for(Part part : campaign.getSpareParts()) {
            if(part instanceof ProtomekArmor
                    && ((ProtomekArmor)part).isClanTechBase() == clan
                    && getRefitId() == part.getRefitId()
                    && part.isPresent()) {
                a = (ProtomekArmor)part;
                a.setAmount(a.getAmount() + amount);
                break;
            }
        }
        if(null != a && a.getAmount() <= 0) {
            campaign.removePart(a);
        } else if(null == a && amount > 0) {
            campaign.addPart(new ProtomekArmor(getUnitTonnage(), amount, -1, isClanTechBase(), campaign), 0);
        }
    }

    @Override
    public int getTechRating() {
        return RATING_F;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        // This is for standard armor, which does not currently have a separate entry in MM.
        // TODO: EDP armor (which does have an entry in MM)
        return ProtomekLocation.TECH_ADVANCEMENT;
    }

}
