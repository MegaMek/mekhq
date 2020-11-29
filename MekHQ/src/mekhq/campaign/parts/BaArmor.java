/*
 * BaArmor.java
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

import java.util.Objects;

import megamek.common.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class BaArmor extends Armor implements IAcquisitionWork {
    private static final long serialVersionUID = 5275226057484468868L;

    public static boolean canBeClan(int type) {
        return type == EquipmentType.T_ARMOR_BA_STANDARD || type == EquipmentType.T_ARMOR_BA_STEALTH_BASIC
                || type == EquipmentType.T_ARMOR_BA_STEALTH_IMP || type == EquipmentType.T_ARMOR_BA_STEALTH
                || type == EquipmentType.T_ARMOR_BA_FIRE_RESIST;
    }

    public static boolean canBeIs(int type) {
        return type != EquipmentType.T_ARMOR_BA_FIRE_RESIST;
    }


    public static double getPointsPerTon(int t, boolean isClan) {
        return 1.0/EquipmentType.getBaArmorWeightPerPoint(t, isClan);
    }

    public BaArmor() {
        this(0, 0, 0, -1, false, null);
    }

    public BaArmor(int tonnage, int points, int type, int loc, boolean clan, Campaign c) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage, type, points, loc, false, clan, c);
    }

    public BaArmor clone() {
        BaArmor clone = new BaArmor(0, amount, type, location, clan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        return EquipmentType.getBaArmorWeightPerPoint(type, clan) * amount;
    }

    public Money getPointCost() {
        switch(type) {
        case EquipmentType.T_ARMOR_BA_STANDARD_ADVANCED:
            return Money.of(12500);
        case EquipmentType.T_ARMOR_BA_MIMETIC:
        case EquipmentType.T_ARMOR_BA_STEALTH:
            return Money.of(15000);
        case EquipmentType.T_ARMOR_BA_STEALTH_BASIC:
            return Money.of(12000);
        case EquipmentType.T_ARMOR_BA_STEALTH_IMP:
            return Money.of(20000);
        case EquipmentType.T_ARMOR_BA_STEALTH_PROTOTYPE:
            return Money.of(50000);
        case EquipmentType.T_ARMOR_BA_FIRE_RESIST:
        case EquipmentType.T_ARMOR_BA_STANDARD_PROTOTYPE:
        case EquipmentType.T_ARMOR_BA_STANDARD:
        default:
            return Money.of(10000);
        }
    }

    private double getPointsPerTon() {
        return getPointsPerTon(type, clan);
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public Money getCurrentValue() {
        return getPointCost().multipliedBy(amount);
    }

    @Override
    public double getTonnageNeeded() {
        return amountNeeded / getPointsPerTon();
    }

    @Override
    public Money getValueNeeded() {
        return adjustCostsForCampaignOptions(getPointCost().multipliedBy(amountNeeded));
    }

    @Override
    public Money getStickerPrice() {
        //always in 5-ton increments
        return getPointCost().multipliedBy(5).multipliedBy(getPointsPerTon());
    }

    @Override
    public Money getBuyCost() {
        return getStickerPrice();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof BaArmor)
                && (isClanTechBase() == part.isClanTechBase())
                && Objects.equals(getRefitUnit(), part.getRefitUnit())
                && (((BaArmor) part).getType() == getType());
    }

    @Override
    public boolean isSameStatus(Part part) {
        return !hasParentPart() && !part.hasParentPart() && this.getDaysToArrival() == part.getDaysToArrival();
    }

    public double getArmorWeight(int points) {
        return points * 50/1000.0;
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new BaArmor(0, (int)Math.round(5 * getPointsPerTon()), type, -1, clan, campaign);
    }

    public Part getNewPart() {
        return new BaArmor(0, (int)Math.round(5 * getPointsPerTon()), type, -1, clan, campaign);
    }

    public int getAmountAvailable() {
        BaArmor a = (BaArmor) campaign.getWarehouse().findSparePart(part -> {
            return part instanceof BaArmor
                && part.isPresent()
                && !part.isReservedForRefit()
                && isClanTechBase() == part.isClanTechBase()
                && ((BaArmor)part).getType() == getType();
        });

        return a != null ? a.getAmount() : 0;
    }

    @Override
    public void changeAmountAvailable(int amount) {
        BaArmor a = (BaArmor) campaign.getWarehouse().findSparePart(part -> {
            return isSamePartType(part)
                && part.isPresent();
        });

        if(null != a) {
            a.setAmount(a.getAmount() + amount);
            if (a.getAmount() <= 0) {
                campaign.getWarehouse().removePart(a);
            }
        } else if(amount > 0) {
            campaign.getQuartermaster().addPart(new BaArmor(getUnitTonnage(), amount, type, -1, isClanTechBase(), campaign), 0);
        }
    }
}
