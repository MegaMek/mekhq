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

import java.io.PrintWriter;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.Mounted;
import megamek.common.SimpleTechLevel;
import megamek.common.Tank;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class TankLocation extends Part {
    private static final MMLogger logger = MMLogger.create(TankLocation.class);

    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2460, 2470, 2510).setApproximate(true, false, false)
            .setPrototypeFactions(F_TH).setProductionFactions(F_TH)
            .setTechRating(RATING_D).setAvailability(RATING_A, RATING_A, RATING_A, RATING_A)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);

    protected int loc;
    protected int damage;
    protected boolean breached;

    public TankLocation() {
        this(0, 0, null);
    }

    @Override
    public TankLocation clone() {
        TankLocation clone = new TankLocation(loc, getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        clone.loc = this.loc;
        clone.damage = this.damage;
        clone.breached = this.breached;
        return clone;
    }

    public int getLoc() {
        return loc;
    }

    public TankLocation(int loc, int tonnage, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.damage = 0;
        this.breached = false;
        this.name = "Tank Location";
        switch (loc) {
            case Tank.LOC_FRONT:
                this.name = "Vehicle Front";
                break;
            case Tank.LOC_LEFT:
                this.name = "Vehicle Left Side";
                break;
            case Tank.LOC_RIGHT:
                this.name = "Vehicle Right Side";
                break;
            case Tank.LOC_REAR:
                this.name = "Vehicle Rear";
                break;
        }
        computeCost();
    }

    protected void computeCost() {
        // TODO: implement
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof TankLocation
                && getLoc() == ((TankLocation) part).getLoc()
                && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) && this.getDamage() == ((TankLocation) part).getDamage();
    }

    public int getDamage() {
        return damage;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "damage", damage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "breached", breached);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("loc")) {
                    loc = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("damage")) {
                    damage = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("breached")) {
                    breached = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (isBreached()) {
            breached = false;
            if (null != unit) {
                unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
                for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                    CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                    // ignore empty & non-hittable slots
                    if (slot == null) {
                        continue;
                    }
                    slot.setBreached(false);
                    Mounted<?> m = slot.getMount();
                    if (null != m) {
                        m.setBreached(false);
                    }
                }
            }
        } else {
            damage = 0;
            if (null != unit) {
                unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
            }
        }
    }

    @Override
    public @Nullable MissingPart getMissingPart() {
        // Can't replace locations
        return null;
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.incrementQuantity();
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            if (IArmorState.ARMOR_DESTROYED == unit.getEntity().getInternal(loc)) {
                remove(false);
            } else {
                int originalInternal = unit.getEntity().getOInternal(loc);
                int internal = unit.getEntity().getInternal(loc);
                damage = originalInternal - Math.min(originalInternal, Math.max(internal, 0));
                if (unit.isLocationBreached(loc)) {
                    breached = true;
                }
            }
        }
    }

    @Override
    public int getBaseTime() {
        return 60;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    public boolean isBreached() {
        return breached;
    }

    @Override
    public boolean needsFixing() {
        return damage > 0 || breached;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringBuilder toReturn = new StringBuilder();

        toReturn.append(super.getDetails(includeRepairDetails));

        if (includeRepairDetails) {
            if (isBreached()) {
                toReturn.append(", Breached");
            } else if (damage > 0) {
                toReturn.append(", ")
                    .append(damage)
                    .append(damage == 1 ? " point" : " points")
                    .append(" of damage");
            }
        }

        return toReturn.toString();
    }

    @Override
    public void updateConditionFromPart() {
        unit.getEntity().setInternal(unit.getEntity().getOInternal(loc) - damage, loc);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public String checkScrappable() {
        return "Vehicle locations cannot be scrapped";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
    }

    @Override
    public double getTonnage() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        // TODO Auto-generated method stub
        return Money.zero();
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        if (isBreached() && !isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
        }
        return super.getAllMods(tech);
    }

    @Override
    public String getDesc() {
        if (!isBreached() || isSalvaging()) {
            return super.getDesc();
        }
        String toReturn = "<html><font";
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        toReturn += "<b>Seal " + getName() + "</b><br/>";
        toReturn += getDetails() + "<br/>";
        toReturn += "" + getTimeLeft() + " minutes" + scheduled;
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECHANIC);
    }

    @Override
    public void doMaintenanceDamage(int d) {
        int points = unit.getEntity().getInternal(loc);
        points = Math.max(points - d, 1);
        unit.getEntity().setInternal(points, loc);
        updateConditionFromEntity(false);
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(loc) : null;
    }

    @Override
    public int getLocation() {
        return loc;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }
}
