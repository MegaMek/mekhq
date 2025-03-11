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
 */
package mekhq.campaign.parts;

import megamek.common.IArmorState;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.VTOL;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Rotor extends TankLocation {
    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2460, 2470, 2510).setApproximate(true, false, false)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setTechRating(RATING_D).setAvailability(RATING_C, RATING_D, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    public Rotor() {
        this(0, null);
    }

    public Rotor(int tonnage, Campaign c) {
        super(VTOL.LOC_ROTOR, tonnage, c);
        this.name = "Rotor";
        this.damage = 0;
        this.unitTonnageMatters = true;
    }

    @Override
    public Rotor clone() {
        Rotor clone = new Rotor(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        clone.loc = this.loc;
        clone.damage = this.damage;
        clone.breached = this.breached;
        return clone;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof Rotor
                && getLoc() == ((Rotor) part).getLoc()
                && getUnitTonnage() == part.getUnitTonnage()
                && this.getDamage() == ((Rotor) part).getDamage()
                && part.getSkillMin() == this.getSkillMin();
    }

    @Override
    public void fix() {
        super.fix();
        if (damage > 0) {
            damage--;
        }
        if (null != unit && unit.getEntity() instanceof VTOL) {
            int currIsVal = unit.getEntity().getInternal(VTOL.LOC_ROTOR);
            int maxIsVal = unit.getEntity().getOInternal(VTOL.LOC_ROTOR);
            int repairedIsVal = Math.min(maxIsVal, currIsVal + 1);
            unit.getEntity().setInternal(repairedIsVal, VTOL.LOC_ROTOR);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingRotor(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof VTOL) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, VTOL.LOC_ROTOR);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0);
            ((VTOL) unit.getEntity()).resetMovementDamage();
            for (Part part : unit.getParts()) {
                if (part instanceof MotiveSystem) {
                    part.updateConditionFromEntity(false);
                }
            }
        }
        setUnit(null);
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 300;
        }
        return 120;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        return 2;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && damage > 0 && unit.getEntity() instanceof VTOL) {
            unit.getEntity().setInternal(unit.getEntity().getOInternal(VTOL.LOC_ROTOR) - damage, VTOL.LOC_ROTOR);
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (isSalvaging()) {
            //check for armor
            if (unit.getEntity().getArmorForReal(loc, false) > 0) {
                return "must salvage armor in this location first";
            }
        }
        return null;
    }

    @Override
    public String checkScrappable() {
        //check for armor
        if (unit.getEntity().getArmor(loc, false) != IArmorState.ARMOR_DESTROYED) {
            return "You must scrap armor in the rotor first";
        }
        return null;
    }

    @Override
    public boolean canNeverScrap() {
        return false;
    }

    @Override
    public double getTonnage() {
        return 0.1 * getUnitTonnage();
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(40000 * getTonnage());
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }
}
