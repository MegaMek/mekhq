/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2013-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
        ProtoMekArmor clone = new ProtoMekArmor(0, getType(), getAmount(), getLocation(), clan, campaign);
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
        return points * 50 / 1000.0;
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
        return campaign.getWarehouse()
                     .streamSpareParts().filter(this::isSameProtoMekArmor)
                     .mapToInt(part -> ((ProtoMekArmor) part).getAmount())
                     .sum();
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
        // type entry so we can just use the base protomek advancement
        return ProtoMek.TA_STANDARD_PROTOMEK;
    }

    @Override
    protected int changeAmountAvailableSingle(int amount) {
        ProtoMekArmor armor = (ProtoMekArmor) campaign.getWarehouse()
                                                    .findSparePart(part -> isSamePartType(part) && part.isPresent());

        if (null != armor) {
            int amountRemaining = armor.getAmount() + amount;
            armor.setAmount(amountRemaining);
            if (armor.getAmount() <= 0) {
                campaign.getWarehouse().removePart(armor);
                return Math.min(0, amountRemaining);
            }
        } else if (amount > 0) {
            campaign.getQuartermaster()
                  .addPart(new ProtoMekArmor(getUnitTonnage(), type, amount, -1, isClanTechBase(), campaign), 0, false);
        }
        return 0;
    }

    /**
     * Not sure how true this title is, it was used in {@link ProtoMekArmor#getAmountAvailable}
     *
     * @param part is this part the same
     *
     * @return true if the two parts are the same, at least as far as {@link ProtoMekArmor#getAmountAvailable} is
     *       concerned
     */
    private boolean isSameProtoMekArmor(Part part) {
        return (part instanceof ProtoMekArmor protoMekArmor) &&
                     protoMekArmor.isPresent() &&
                     !protoMekArmor.isReservedForRefit() &&
                     isClanTechBase() == protoMekArmor.isClanTechBase() &&
                     (getType() == (protoMekArmor).getType());
    }
}
