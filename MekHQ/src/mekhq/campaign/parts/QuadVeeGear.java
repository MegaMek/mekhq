/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.parts;

import java.io.PrintWriter;

import org.w3c.dom.Node;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.QuadVee;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;

/**
 * Conversion gear for QuadVees
 *
 * @author Neoancient
 */
public class QuadVeeGear extends Part {
    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_CLAN)
            .setTechRating(RATING_F)
            .setAvailability(RATING_X, RATING_X, RATING_X, RATING_F)
            .setClanAdvancement(3130, 3135, DATE_NONE, DATE_NONE, DATE_NONE)
            .setClanApproximate(true).setPrototypeFactions(F_CHH)
            .setProductionFactions(F_CHH)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    public QuadVeeGear() {
        this(0, null);
    }

    public QuadVeeGear(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Conversion Gear";
    }

    @Override
    public QuadVeeGear clone() {
        QuadVeeGear clone = new QuadVeeGear(0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            hits = unit.getHitCriticals(CriticalSlot.TYPE_SYSTEM,
                    QuadVee.SYSTEM_CONVERSION_GEAR);
        }
    }

    @Override
    public int getBaseTime() {
        // Using value for 'Mek "weapons and other equipment"
        if (isSalvaging()) {
            return 120;
        }
        if (hits == 1) {
            return 100;
        } else if (hits == 2) {
            return 150;
        } else if (hits == 3) {
            return 200;
        } else if (hits > 3) {
            return 250;
        }
        return 0;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        if (hits == 1) {
            return -3;
        } else if (hits == 2) {
            return -2;
        } else if (hits == 3) {
            return 0;
        } else if (hits > 3) {
            return 2;
        }
        return 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, hits);
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.damageSystem(CriticalSlot.TYPE_SYSTEM, QuadVee.SYSTEM_CONVERSION_GEAR, 4);
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
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingQuadVeeGear(getUnitTonnage(), campaign);
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (isSalvaging()) {
            return null;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().locationIsLeg(i)) {
                if (unit.isLocationBreached(i)) {
                    return unit.getEntity().getLocationName(i) + " is breached.";
                }
                if (unit.isLocationDestroyed(i)) {
                    return unit.getEntity().getLocationName(i) + " is destroyed.";
                }
            }
        }
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        /*
         * The cost for conversion equipment is calculated as 10% of the total cost of
         * weapons/equipment
         * and structure. This is unworkable for the conversion gear sticker price,
         * since this
         * would make the cost of the conversion gear in OmniQuadVees vary with the
         * configuration.
         * We will use a general 10,000 * part tonnage and assume the remainder is part
         * of the
         * turret mechanism that is only destroyed if the center torso is destroyed.
         */
        return Money.of(getTonnage() * 10000);
    }

    @Override
    public double getTonnage() {
        return Math.ceil(unitTonnage * 0.1);
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof QuadVeeGear && part.unitTonnage == unitTonnage;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // nothing to load
    }

    @Override
    public String getLocationName() {
        return null;
    }

}
