/*
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.parts.equipment;

import java.io.PrintWriter;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.MiscType;
import megamek.common.Mounted;
import megamek.common.TechAdvancement;
import megamek.common.WeaponType;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.MissingPart;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingEquipmentPart extends MissingPart {
    private static final MMLogger logger = MMLogger.create(MissingEquipmentPart.class);

    // crap equipmenttype is not serialized!
    protected transient EquipmentType type;
    protected String typeName;
    protected int equipmentNum;
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

    public MissingEquipmentPart(int tonnage, EquipmentType et, int equipNum, Campaign c, double eTonnage, double size,
          boolean omniPodded) {
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
        return new MissingEquipmentPart(getUnitTonnage(),
              getType(),
              getEquipmentNum(),
              getCampaign(),
              getTonnage(),
              getSize(),
              isOmniPodded());
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
            logger.error("Mounted.restore: could not restore equipment type \"" + name + "\"");
        }
    }

    @Override
    public double getTonnage() {
        return equipTonnage;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "size", size);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipTonnage", equipTonnage);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
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

            ((EquipmentPart) actualReplacement).setEquipmentNum(equipmentNum);

            remove(false);

            actualReplacement.updateConditionFromPart();
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        EquipmentPart newPart = getNewPart();

        // Don't replace with parts that don't match our expected type!
        if (newPart.getClass() != part.getClass()) {
            return false;
        }

        EquipmentPart equipmentPart = (EquipmentPart) part;

        newPart.setEquipmentNum(getEquipmentNum());
        newPart.setUnit(unit); // CAW: find a way to do this without setting a unit

        return checkAttributesMatch(equipmentPart, newPart);
    }

    /**
     * Compares the attributes of two {@link EquipmentPart} objects to determine if they are equivalent. The comparison
     * checks several attributes, including type, tonnage, size, omni-podded status, sticker price, and unit tonnage
     * when applicable.
     *
     * <p><b>Attributes Checked:</b></p>
     * <ul>
     *   <li><b>Type:</b> Verifies that the equipment types for both parts are identical.</li>
     *   <li><b>Tonnage:</b> Ensures that the total tonnage of both parts matches.</li>
     *   <li><b>Size:</b> Confirms that the physical sizes of the two parts are the same.</li>
     *   <li><b>Omni-Podded:</b> Checks whether both parts share the same omni-podded attribute.</li>
     *   <li><b>Sticker Price:</b> Compares the monetary sticker price of both parts.</li>
     *   <li><b>Unit Tonnage:</b> For parts where sticker price does not match and sticker price returns 0 (for
     *   example, {@link MissingPart} objects) we compare unit tonnage as a fallback.</li>
     * </ul>
     *
     * @param equipmentPart the {@link EquipmentPart} used as the baseline for the comparison.
     * @param newPart       the {@link EquipmentPart} whose attributes are being compared to the baseline part.
     *
     * @return {@code true} if all compared attributes (type, tonnage, size, omni-podded status, sticker price, and unit
     *       tonnage where applicable) match; otherwise, {@code false}.
     */
    private boolean checkAttributesMatch(EquipmentPart equipmentPart, EquipmentPart newPart) {
        boolean typeMatches = getType().equals(equipmentPart.getType());
        boolean tonnageMatches = newPart.getTonnage() == equipmentPart.getTonnage();
        boolean sizeMatches = newPart.getSize() == equipmentPart.getSize();
        boolean omniPoddedMatches = newPart.isOmniPodded() == equipmentPart.isOmniPodded();

        Money newPartPrice = newPart.getStickerPrice();
        Money equipmentPartPrice = equipmentPart.getStickerPrice();
        boolean stickerPriceOrTonnageMatches = newPartPrice.equals(equipmentPartPrice);

        // According to official answer, if target unit tonnage differs, the item cannot be attached to a unit, even
        // if the equipment weight matches. The example in the below thread is a Mek Lance, the weight of which is
        // based on the unit it's mounted on. So while a 35-ton and 40-ton Mek would both use a 2-ton lance, they
        // are not interchangeable. Originally we handled this by comparing sticker price, however, missing parts have
        // a fixed sticker price of 0 C-Bills, which meant users were completely unable to replace those parts if
        // they were ever completely destroyed or removed. Now we compare unit tonnage.
        // https://bg.battletech.com/forums/index.php/topic,14741.msg340122.html#msg340122
        EquipmentType equipmentType = equipmentPart.getType();
        if (equipmentPartPrice.isZero() && !stickerPriceOrTonnageMatches && equipmentType instanceof MiscType) {
            if (((MiscType) equipmentType).isCostVariable()) {
                stickerPriceOrTonnageMatches = newPart.getUnitTonnage() == equipmentPart.getUnitTonnage();
            }
        }

        return typeMatches && tonnageMatches && sizeMatches && omniPoddedMatches && stickerPriceOrTonnageMatches;
    }

    protected @Nullable Mounted<?> getMounted() {
        final Unit unit = getUnit();
        if ((unit != null) && (unit.getEntity() != null) && (getEquipmentNum() >= 0)) {
            final Mounted<?> mounted = unit.getEntity().getEquipment(getEquipmentNum());
            if (mounted != null) {
                return mounted;
            }

            logger.warn("Missing valid equipment for " + getName() + " on unit " + getUnit().getName());
        }

        return null;
    }

    @Override
    public @Nullable String checkFixable() {
        final Unit unit = getUnit();
        if ((unit != null) && (unit.isSalvage() || isTeamSalvaging())) {
            return null;
        }

        // The part is only fixable if the location is not destroyed.
        // be sure to check location and second location
        final Mounted<?> m = getMounted();
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
        final Mounted<?> mounted = getMounted();
        if ((unit != null) && (mounted != null)) {
            return unit.hasBadHipOrShoulder(mounted.getLocation()) ||
                         (mounted.isSplit() && unit.hasBadHipOrShoulder(mounted.getSecondLocation()));
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
        final Mounted<?> mounted = getMounted();
        return (mounted != null) ? mounted.getLocation() : Entity.LOC_NONE;
    }

    public double getSize() {
        return size;
    }

    public boolean isRearFacing() {
        final Mounted<?> mounted = getMounted();
        return (mounted != null) && mounted.isRearMounted();
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return (getEquipmentNum() == index) && (getLocation() == loc);
    }

    @Override
    public void updateConditionFromPart() {
        final Unit unit = getUnit();
        final Mounted<?> mounted = getMounted();
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
            return type.hasFlag(MiscType.F_MEK_EQUIPMENT) ||
                         type.hasFlag(MiscType.F_TANK_EQUIPMENT) ||
                         type.hasFlag(MiscType.F_FIGHTER_EQUIPMENT);
        } else if (type instanceof WeaponType) {
            return (type.hasFlag(WeaponType.F_MEK_WEAPON) ||
                          type.hasFlag(WeaponType.F_TANK_WEAPON) ||
                          type.hasFlag(WeaponType.F_AERO_WEAPON)) && !((WeaponType) type).isCapital();
        }
        return true;
    }

    @Override
    public String getLocationName() {
        final Mounted<?> mounted = getMounted();
        if ((mounted != null) && (mounted.getLocation() != Entity.LOC_NONE)) {
            return getUnit().getEntity().getLocationName(mounted.getLocation());
        }

        return null;
    }

    @Override
    public boolean isInLocation(String loc) {
        final Mounted<?> mounted = getMounted();
        if (mounted == null) {
            return false;
        }

        int location = unit.getEntity().getLocationFromAbbr(loc);
        return (mounted.getLocation() == location) || (mounted.isSplit() && (mounted.getSecondLocation() == location));
    }
}
