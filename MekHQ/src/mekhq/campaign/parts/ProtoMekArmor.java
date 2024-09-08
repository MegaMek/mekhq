/*
 * ProtoMekArmor.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.util.Objects;

import megamek.common.EquipmentType;
import megamek.common.ProtoMek;
import megamek.common.TechAdvancement;
import megamek.common.equipment.ArmorType;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.work.IAcquisitionWork;

/**
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ProtoMekArmor extends Armor {
    public ProtoMekArmor() {
        this(0, EquipmentType.T_ARMOR_STANDARD_PROTOMEK, 0, -1, false, null);
    }

    public ProtoMekArmor(int tonnage, int type, int points, int loc, boolean clan, Campaign c) {
        // Amount is used for armor quantity, not tonnage
        super(tonnage, type, points, loc, false, clan, c);
        this.name = "ProtoMek Armor";
    }

    @Override
    public ProtoMekArmor clone() {
        ProtoMekArmor clone = new ProtoMekArmor(0, type, 0, amount, clan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        return ArmorType.of(type, true).getWeightPerPoint() * amount;
    }

    @Override
    public Money getActualValue() {
        return adjustCostsForCampaignOptions(
                Money.of(amount * ArmorType.of(type, true).getWeightPerPoint()));
    }

    @Override
    public double getTonnageNeeded() {
        return amountNeeded / ArmorType.of(type, true).getWeightPerPoint();
    }

    @Override
    public Money getValueNeeded() {
        return adjustCostsForCampaignOptions(
                Money.of(amountNeeded * ArmorType.of(type, clan).getCost()));
    }

    @Override
    public Money getStickerPrice() {
        // always in 5-ton increments
        return Money.of(5.0 / ArmorType.of(type, true).getWeightPerPoint() * getArmorPointsPerTon()
                * ArmorType.of(type, clan).getCost());
    }

    @Override
    public Money getBuyCost() {
        return getActualValue();
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (getClass() == part.getClass())
                && getType() == ((ProtoMekArmor) part).getType()
                && isClanTechBase() == part.isClanTechBase()
                && Objects.equals(getRefitUnit(), part.getRefitUnit());
    }

    @Override
    protected boolean isClanTechBase() {
        return clan;
    }

    @Override
    public double getArmorWeight(int points) {
        return points * 50/1000.0;
    }

    @Override
    public IAcquisitionWork getAcquisitionWork() {
        return new ProtoMekArmor(0, type, (int) Math.round(5.0 * getArmorPointsPerTon()),
                -1, clan, campaign);
    }

    @Override
    public int getDifficulty() {
        return -2;
    }

    @Override
    public double getArmorPointsPerTon() {
        return 1.0 / ArmorType.of(type, true).getWeightPerPoint();
    }

    @Override
    public Part getNewPart() {
        return new ProtoMekArmor(0, type, (int) Math.round(5 * getArmorPointsPerTon()),
                -1, clan, campaign);
    }

    @Override
    public int getAmountAvailable() {
        ProtoMekArmor a = (ProtoMekArmor) campaign.getWarehouse().findSparePart(part ->
                (part instanceof ProtoMekArmor)
                        && part.isPresent()
                        && !part.isReservedForRefit()
                        && isClanTechBase() == part.isClanTechBase()
                        && (getType() == ((ProtoMekArmor) part).getType()));

        return a != null ? a.getAmount() : 0;
    }

    @Override
    public void changeAmountAvailable(int amount) {
        ProtoMekArmor a = (ProtoMekArmor) campaign.getWarehouse().findSparePart(part ->
                isSamePartType(part) && part.isPresent());

        if (null != a) {
            a.setAmount(a.getAmount() + amount);
            if (a.getAmount() <= 0) {
                campaign.getWarehouse().removePart(a);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster().addPart(new ProtoMekArmor(getUnitTonnage(), type, amount, -1, isClanTechBase(), campaign), 0);
        }
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        if (type != EquipmentType.T_ARMOR_STANDARD_PROTOMEK) {
            final EquipmentType eq = EquipmentType.get(EquipmentType.getArmorTypeName(type, clan));
            if (null != eq) {
                return eq.getTechAdvancement();
            }
        }
        // Standard ProtoMek armor is not the same as Standard armor, but does not have an associated
        // type entry so we can just use the base protomech advancement
        return ProtoMek.TA_STANDARD_PROTOMECH;
    }
}
