/*
 * MissingEquipmentPart.java
 *
 * Copyright (c) 2009 Jay Lawson <jaylawson39 at yahoo.com>. All rights reserved.
 * Copyright (C) 2020 MegaMek team
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

package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;
import java.util.Objects;

import mekhq.MekHQ;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;

/**
 *
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MissingEquipmentPart extends MissingPart {
    private static final long serialVersionUID = 2892728320891712304L;

    //crap equipmenttype is not serialized!
    protected transient EquipmentType type;
    protected String typeName;
    protected int equipmentNum = -1;
    protected double equipTonnage;
    protected double size;

    public EquipmentType getType() {
        return type;
    }

    public int getEquipmentNum() {
        return equipmentNum;
    }

    public void setEquipmentNum(int num) {
        equipmentNum = num;
    }

    public MissingEquipmentPart() {
        this(0, null, -1, null, 0, 1.0, false);
    }

    public MissingEquipmentPart(int tonnage, EquipmentType et, int equipNum, double size, Campaign c, double eTonnage) {
        this(tonnage, et, equipNum, c, eTonnage, size, false);
    }

    public MissingEquipmentPart(int tonnage, EquipmentType et, int equipNum, Campaign c,
            double eTonnage, double size, boolean omniPodded) {
        // TODO Memorize all entity attributes needed to calculate cost
        // As it is a part bought with one entity can be used on another entity
        // on which it would have a different price (only tonnage is taken into
        // account for compatibility)
        super(tonnage, c);
        this.type = et;
        if (type != null) {
            this.name = type.getName(size);
            this.typeName = type.getInternalName();
        }
        this.equipmentNum = equipNum;
        this.equipTonnage = eTonnage;
        this.size = size;
        this.omniPodded = omniPodded;
    }

    @Override
    public MissingEquipmentPart clone() {
        return new MissingEquipmentPart(getUnitTonnage(), getType(), getEquipmentNum(), getCampaign(),
                getTonnage(), getSize(), isOmniPodded());
    }

    @Override
    public int getBaseTime() {
        return isOmniPodded() ? 30 : 120;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    /**
     * Restores the equipment from the name
     */
    public void restore() {
        if (typeName == null) {
            typeName = type.getName();
        } else {
            type = EquipmentType.get(typeName);
        }

        if (type == null) {
            MekHQ.getLogger().error("Mounted.restore: could not restore equipment type \"" + name + "\"");
        }
    }

    @Override
    public double getTonnage() {
        return equipTonnage;
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "typeName", type.getInternalName());
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipmentNum", equipmentNum);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "size", size);
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "equipTonnage", equipTonnage);
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x=0; x<nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("equipmentNum")) {
                equipmentNum = Integer.parseInt(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("typeName")) {
                typeName = wn2.getTextContent();
            } else if (wn2.getNodeName().equalsIgnoreCase("size")) {
                size = Double.parseDouble(wn2.getTextContent());
            } else if (wn2.getNodeName().equalsIgnoreCase("equipTonnage")) {
                equipTonnage = Double.parseDouble(wn2.getTextContent());
            }
        }
        restore();
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return type.getTechAdvancement();
    }

    @Override
    public void fix() {
        Part replacement = findReplacement(false);
        if (replacement != null) {
            Part actualReplacement = replacement.clone();
            unit.addPart(actualReplacement);
            
            campaign.getQuartermaster().addPart(actualReplacement, 0);
            replacement.decrementQuantity();
            
            ((EquipmentPart)actualReplacement).setEquipmentNum(equipmentNum);
            
            remove(false);

            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        //According to official answer, if sticker prices are different then
        //they are not acceptable substitutes, so we need to check for that as
        //well
        //http://bg.battletech.com/forums/strategic-operations/(answered)-can-a-lance-for-a-35-ton-mech-be-used-on-a-40-ton-mech-and-so-on/
        EquipmentPart newPart = getNewPart();

        // Don't replace with parts that don't match our expected type!
        if (!newPart.getClass().equals(part.getClass())) {
            return false;
        }

        EquipmentPart equipmentPart = (EquipmentPart) part;

        newPart.setEquipmentNum(getEquipmentNum());
        newPart.setUnit(unit); // CAW: find a way to do this without setting a unit
        return getType().equals(equipmentPart.getType())
                && (getTonnage() == equipmentPart.getTonnage())
                && (getSize() == equipmentPart.getSize())
                && Objects.equals(newPart.getStickerPrice(), equipmentPart.getStickerPrice());
    }

    protected @Nullable Mounted getMounted() {
        final Unit unit = getUnit();
        if ((unit != null) && (unit.getEntity() != null) && (getEquipmentNum() >= 0)) {
            final Mounted mounted = unit.getEntity().getEquipment(getEquipmentNum());
            if (mounted != null) {
                return mounted;
            }

            MekHQ.getLogger().warning("Missing valid equipment for " + getName() + " on unit " + getUnit().getName());
        }

        return null;
    }

    @Override
    public String checkFixable() {
        final Unit unit = getUnit();
        if ((unit != null) && (unit.isSalvage() || isTeamSalvaging())) {
            return null;
        }

        // The part is only fixable if the location is not destroyed.
        // be sure to check location and second location
        final Mounted m = getMounted();
        if ((unit != null) && (m != null)) {
            int loc = m.getLocation();
            if (unit.isLocationBreached(loc)) {
                return unit.getEntity().getLocationName(loc) + " is breached.";
            }
    
            if (unit.isLocationDestroyed(loc)) {
                return unit.getEntity().getLocationName(loc) + " is destroyed.";
            }

            if (m.isSplit()) {
                loc = m.getSecondLocation();
                if (unit.isLocationBreached(loc)) {
                    return unit.getEntity().getLocationName(loc) + " is breached.";
                }
                if (unit.isLocationDestroyed(loc)) {
                    return unit.getEntity().getLocationName(loc) + " is destroyed.";
                }
            }
        }

        return null;
    }

    @Override
    public boolean onBadHipOrShoulder() {
        final Unit unit = getUnit();
        final Mounted mounted = getMounted();
        if ((unit != null) && (mounted != null)) {
            return unit.hasBadHipOrShoulder(mounted.getLocation())
                    || (mounted.isSplit() && unit.hasBadHipOrShoulder(mounted.getSecondLocation()));
        }

        return false;
    }

    @Override
    public void setUnit(Unit u) {
        super.setUnit(u);
        if (unit != null) {
            equipTonnage = type.getTonnage(unit.getEntity(), getSize());
        }
    }

    @Override
    public EquipmentPart getNewPart() {
        EquipmentPart epart = new EquipmentPart(getUnitTonnage(), type, -1, size, omniPodded, campaign);
        epart.setEquipTonnage(equipTonnage);
        return epart;
    }

    @Override
    public int getLocation() {
        final Mounted mounted = getMounted();
        return (mounted != null) ? mounted.getLocation() : Entity.LOC_NONE;
    }

    public double getSize() {
        return size;
    }

    public boolean isRearFacing() {
        final Mounted mounted = getMounted();
        return (mounted != null) && mounted.isRearMounted();
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return (getEquipmentNum() == index) && (getLocation() == loc);
    }

    @Override
    public void updateConditionFromPart() {
        final Unit unit = getUnit();
        final Mounted mounted = getMounted();
        if ((unit != null) && (mounted != null)) {
            mounted.setHit(true);
            mounted.setDestroyed(true);
            mounted.setRepairable(false);
            unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum());
        }
    }

    @Override
    public boolean isOmniPoddable() {
        if (type.isOmniFixedOnly()) {
            return false;
        }
        if (type instanceof MiscType) {
            return type.hasFlag(MiscType.F_MECH_EQUIPMENT)
                    || type.hasFlag(MiscType.F_TANK_EQUIPMENT)
                    || type.hasFlag(MiscType.F_FIGHTER_EQUIPMENT);
        } else if (type instanceof WeaponType) {
            return (type.hasFlag(WeaponType.F_MECH_WEAPON)
                    || type.hasFlag(WeaponType.F_TANK_WEAPON)
                    || type.hasFlag(WeaponType.F_AERO_WEAPON))
                    && !((WeaponType)type).isCapital();
        }
        return true;
    }

    @Override
    public String getLocationName() {
        final Mounted mounted = getMounted();
        if ((mounted != null) && (mounted.getLocation() != Entity.LOC_NONE)) {
            return getUnit().getEntity().getLocationName(mounted.getLocation());
        }

        return null;
    }

    @Override
    public boolean isInLocation(String loc) {
        final Mounted mounted = getMounted();
        if (mounted == null) {
            return false;
        }

        int location = unit.getEntity().getLocationFromAbbr(loc);
        return (mounted.getLocation() == location)
                || (mounted.isSplit() && (mounted.getSecondLocation() == location));
    }
}
