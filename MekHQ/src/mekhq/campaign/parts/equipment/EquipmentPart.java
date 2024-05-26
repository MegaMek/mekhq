/*
 * EquipmentPart.java
 *
 * Copyright (c) 2009 Jay Lawson (jaylawson39 at yahoo.com). All rights reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts.equipment;

import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.WeaponMounted;
import megamek.common.weapons.bayweapons.BayWeapon;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * This part covers most of the equipment types in WeaponType, AmmoType, and
 * MiscType It can robustly handle all equipment with static weights and costs.
 * It can also handle equipment whose only variability in terms of cost is the
 * equipment tonnage itself. More complicated variable weight/cost equipment
 * needs to be subclassed. Some examples of equipment that needs to be
 * subclasses: - MASC (depends on engine rating) - AES (depends on location and
 * cost is by unit tonnage)
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class EquipmentPart extends Part {
    // crap EquipmentType is not serialized!
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

    public void setEquipmentNum(int n) {
        this.equipmentNum = n;
    }

    public EquipmentPart() {
        this(0, null, Entity.LOC_NONE, 1.0, false, null);
    }

    public EquipmentPart(int tonnage, EquipmentType et, int equipNum, double size, Campaign c) {
        this(tonnage, et, equipNum, size, false, c);
    }

    public EquipmentPart(int tonnage, EquipmentType et, int equipNum, double size, boolean omniPodded, Campaign c) {
        super(tonnage, omniPodded, c);
        this.type = et;
        if (null != type) {
            this.name = type.getName(size);
            this.typeName = type.getInternalName();
        }

        this.equipmentNum = equipNum;
        this.size = size;

        if (null != type) {
            try {
                equipTonnage = type.getTonnage(null, size);
            } catch (NullPointerException ex) {
                LogManager.getLogger().error("", ex);
            }
        }
    }

    @Override
    public void setUnit(Unit u) {
        super.setUnit(u);
        if ((u != null) && (type != null)) {
            equipTonnage = type.getTonnage(u.getEntity(), size);
        }
    }

    public void setEquipTonnage(double ton) {
        equipTonnage = ton;
    }

    @Override
    public EquipmentPart clone() {
        EquipmentPart clone = new EquipmentPart(getUnitTonnage(), type, equipmentNum, size, omniPodded, campaign);
        clone.copyBaseData(this);
        clone.setEquipTonnage(equipTonnage);
        return clone;
    }

    @Override
    public double getTonnage() {
        return equipTonnage;
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
            LogManager.getLogger().error("Mounted.restore: could not restore equipment type \"" + typeName + "\"");
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        // According to official answer, if sticker prices are different then
        // they are not acceptable substitutes, so we need to check for that as
        // well
        // http://bg.battletech.com/forums/strategic-operations/(answered)-can-a-lance-for-a-35-ton-mech-be-used-on-a-40-ton-mech-and-so-on/
        return (getClass() == part.getClass())
                && getType().equals(((EquipmentPart) part).getType())
                && getTonnage() == part.getTonnage() && getStickerPrice().equals(part.getStickerPrice())
                && getSize() == ((EquipmentPart) part).getSize()
                && isOmniPodded() == part.isOmniPodded();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "equipmentNum", equipmentNum);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "typeName", type.getInternalName());
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
    public int getTechRating() {
        return type.getTechRating();
    }

    @Override
    public void fix() {
        super.fix();

        final Mounted mounted = getMounted();
        if (mounted != null) {
            mounted.setHit(false);
            mounted.setMissing(false);
            mounted.setDestroyed(false);
            unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
        }

        checkWeaponBay(getUnit(), getType(), getEquipmentNum());
    }

    @Override
    public MissingEquipmentPart getMissingPart() {
        return new MissingEquipmentPart(getUnitTonnage(), type, equipmentNum, campaign, equipTonnage, size, omniPodded);
    }

    @Override
    public void remove(boolean salvage) {
        final int equipmentNum = getEquipmentNum();
        final Unit unit = getUnit();
        if (unit != null) {
            final Mounted mounted = getMounted();
            if (null != mounted) {
                mounted.setHit(true);
                mounted.setDestroyed(true);
                mounted.setRepairable(false);
                unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, equipmentNum);
            }

            MissingEquipmentPart missing = getMissingPart();
            if (null != missing) {
                unit.addPart(missing);
                campaign.getQuartermaster().addPart(missing, 0);
            }

            unit.removePart(this);
            setUnit(null);
            setEquipmentNum(-1);

            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else {
                // Now that we're a spare part, add us back into the campaign
                // to merge us with any other parts of the same type
                campaign.getQuartermaster().addPart(this, 0);
            }

            checkWeaponBay(unit, getType(), equipmentNum);
        }
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        final Unit unit = getUnit();
        final Mounted mounted = getMounted();
        if ((unit == null) || (mounted == null)) {
            return;
        }

        if (mounted.isMissing()) {
            remove(false);
            return;
        }

        int priorHits = getHits();

        int newHits = unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum(),
                mounted.getLocation());
        if (mounted.isSplit()) {
            newHits += unit.getEntity().getDamagedCriticals(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum(),
                    mounted.getSecondLocation());
        }

        setHits(newHits);

        omniPodded = mounted.isOmniPodMounted();

        if (checkForDestruction && (getHits() > priorHits)
                && (Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget())) {
            remove(false);
            return;
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return isOmniPodded() ? 30 : 120;
        }

        final int hits = getHits();
        if ((type instanceof MiscType) && type.hasFlag(MiscType.F_BOMB_BAY)) {
            // LAM bomb bays only take 60 minutes to repair.
            return (hits > 0) ? 60 : 0;
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
        // LAM bomb bays have a fixed -1 difficulty.
        if ((type instanceof MiscType) && type.hasFlag(MiscType.F_BOMB_BAY)) {
            return -1;
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
    public boolean needsFixing() {
        return hits > 0;
    }

    protected @Nullable Mounted getMounted() {
        final Unit unit = getUnit();
        if ((unit != null) && (unit.getEntity() != null) && (getEquipmentNum() >= 0)) {
            final Mounted mounted = unit.getEntity().getEquipment(getEquipmentNum());
            if (mounted != null) {
                return mounted;
            }

            LogManager.getLogger().warn("Missing valid equipment for " + getName() + " on unit " + getUnit().getName());
        }

        return null;
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
    public void updateConditionFromPart() {
        final Unit unit = getUnit();
        if (unit == null) {
            return;
        }

        final Mounted mounted = getMounted();
        if (mounted != null) {
            mounted.setMissing(false);
            if (getHits() > 0) {
                mounted.setDestroyed(true);
                mounted.setHit(true);
                mounted.setRepairable(true);
                unit.damageSystem(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum(), getHits());
            } else {
                mounted.setHit(false);
                mounted.setDestroyed(false);
                mounted.setRepairable(true);
                unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, getEquipmentNum());
            }

            setOmniPodded(mounted.isOmniPodMounted());
        }

        checkWeaponBay(unit, getType(), getEquipmentNum());
    }

    @Override
    public @Nullable String checkFixable() {
        if (isSalvaging()) {
            return null;
        }

        // The part is only fixable if the location is not destroyed.
        // be sure to check location and second location
        final Unit unit = getUnit();
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
    public boolean isMountedOnDestroyedLocation() {
        final Unit unit = getUnit();
        final Mounted mounted = getMounted();
        if ((unit != null) && (mounted != null)) {
            return unit.isLocationDestroyed(mounted.getLocation())
                    || (mounted.isSplit() && unit.isLocationDestroyed(mounted.getSecondLocation()));
        }

        return false;
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

    /**
     * Copied from megamek.common.Entity.getWeaponsAndEquipmentCost(StringBuffer
     * detail, boolean ignoreAmmo)
     *
     */
    @Override
    public Money getStickerPrice() {
        // Ok, we can't use the resolveVariableCost methods from MegaMek, because they
        // rely on entity which may be null if this is a spare part. So we use our
        // own resolveVariableCost method
        // TODO : we need a static method that returns whether this equipment type depends upon
        // - unit tonnage
        // - item tonnage
        // - engine
        // use that to determine how to add things to the parts store and to
        // determine whether what can be used as a replacement
        // why does all the proto ammo have no cost?
        Entity en;
        boolean isArmored = false;
        Money itemCost = Money.of(type.getRawCost());

        if (itemCost.getAmount().intValue() == EquipmentType.COST_VARIABLE) {
            itemCost = resolveVariableCost(isArmored);
        }

        if (unit != null) {
            en = unit.getEntity();
            Mounted mounted = unit.getEntity().getEquipment(equipmentNum);
            if (null != mounted) {
                isArmored = mounted.isArmored();
            }
            itemCost = Money.of(type.getCost(en, isArmored, getLocation(), getSize()));
        }

        if (isOmniPodded()) {
            itemCost = itemCost.multipliedBy(1.25);
        }

        if (isArmored) {
            // need a getCriticals command - but how does this work?
            // finalCost += 150000 * getCriticals(entity);
        }
        return itemCost;
    }

    private Money resolveVariableCost(boolean isArmored) {
        Money varCost = Money.zero();
        Entity en = null;
        if (getUnit() != null) {
            en = getUnit().getEntity();
        }
        if (en != null) {
            varCost = Money.of(type.getCost(en, isArmored, getLocation(), getSize()));
        } else if (type instanceof MiscType) {
            if (type.hasFlag(MiscType.F_DRONE_CARRIER_CONTROL) || type.hasFlag(MiscType.F_MASH)) {
                varCost = Money.of(10000 * getTonnage());
            } else if (type.hasFlag(MiscType.F_OFF_ROAD)) {
                varCost = Money.of(10 * getTonnage() * getTonnage());
            } else if (type.hasFlag(MiscType.F_FLOTATION_HULL) || type.hasFlag(MiscType.F_VACUUM_PROTECTION)
                    || type.hasFlag(MiscType.F_ENVIRONMENTAL_SEALING) || type.hasFlag(MiscType.F_OFF_ROAD)) {
                // ??
            } else if (type.hasFlag(MiscType.F_LIMITED_AMPHIBIOUS) || type.hasFlag((MiscType.F_FULLY_AMPHIBIOUS))) {
                varCost = Money.of(getTonnage() * 10000);
            } else if (type.hasFlag(MiscType.F_DUNE_BUGGY)) {
                varCost = Money.of(10 * getTonnage() * getTonnage());
            } else if (type.hasFlag(MiscType.F_MASC) && type.hasFlag(MiscType.F_BA_EQUIPMENT)) {
                // TODO: handle this one differently
                // costValue = entity.getRunMP() * 75000;
            } else if (type.hasFlag(MiscType.F_HEAD_TURRET) || type.hasFlag(MiscType.F_SHOULDER_TURRET)
                    || type.hasFlag(MiscType.F_QUAD_TURRET)) {
                varCost = Money.of(getTonnage() * 10000);
            } else if (type.hasFlag(MiscType.F_SPONSON_TURRET)) {
                varCost = Money.of(getTonnage() * 4000);
            } else if (type.hasFlag(MiscType.F_PINTLE_TURRET)) {
                varCost = Money.of(getTonnage() * 1000);
            } else if (type.hasFlag(MiscType.F_ARMORED_MOTIVE_SYSTEM)) {
                // TODO: handle this through motive system part
                varCost = Money.of(getTonnage() * 100000);
            } else if (type.hasFlag(MiscType.F_JET_BOOSTER)) {
                // TODO: Handle this one through subtyping
                // varCost = entity.getEngine().getRating() * 10000;
            } else if (type.hasFlag(MiscType.F_DRONE_OPERATING_SYSTEM)) {
                varCost = Money.of((getTonnage() * 10000) + 5000);
            } else if (type.hasFlag(MiscType.F_TARGCOMP)) {
                varCost = Money.of(getTonnage() * 10000);
            } else if (type.hasFlag(MiscType.F_CLUB)
                    && (type.hasSubType(MiscType.S_HATCHET) || type.hasSubType(MiscType.S_MACE_THB))) {
                varCost = Money.of(getTonnage() * 5000);
            } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_SWORD)) {
                varCost = Money.of(getTonnage() * 10000);
            } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
                varCost = Money.of((1 + getTonnage()) * 10000);
            } else if (type.hasFlag(MiscType.F_TRACKS)) {
                // TODO: Handle this through subtyping
                // varCost = (int) Math.ceil((500 * entity.getEngine().getRating() *
                // entity.getWeight()) / 75);
            } else if (type.hasFlag(MiscType.F_TALON)) {
                varCost = Money.of(getTonnage() * 300);
            } else if (type.hasFlag(MiscType.F_SPIKES)) {
                varCost = Money.of(getTonnage() * 50);
            } else if (type.hasFlag(MiscType.F_PARTIAL_WING)) {
                varCost = Money.of(getTonnage() * 50000);
            } else if (type.hasFlag(MiscType.F_ACTUATOR_ENHANCEMENT_SYSTEM)) {
                // TODO: subtype this one
                // int multiplier = entity.locationIsLeg(loc) ? 700 : 500;
                // costValue = (int) Math.ceil(entity.getWeight() * multiplier);
            } else if (type.hasFlag(MiscType.F_HAND_WEAPON) && (type.hasSubType(MiscType.S_CLAW))) {
                varCost = Money.of(getUnitTonnage() * 200);
            } else if (type.hasFlag(MiscType.F_CLUB) && (type.hasSubType(MiscType.S_LANCE))) {
                varCost = Money.of(getUnitTonnage() * 150);
            } else if (type.hasFlag(MiscType.F_NAVAL_C3)) {
                varCost = Money.of(getUnitTonnage() * 100000);
            }
        }
        if (varCost.isZero()) {
            // if we don't know what it is...
            LogManager.getLogger().debug("I don't know how much " + name + " costs.");
        }
        return varCost;
    }

    /*
     * The following static functions help the parts store determine how to handle
     * variable weight equipment. If the type returns true to hasVariableTonnage
     * then the parts store will use a for loop to create equipment of the given
     * tonnage using the other helper functions. Note that this should not be used
     * for subclassed equipment parts whose "uniqueness" depends on more than the
     * item tonnage
     */
    public static boolean hasVariableTonnage(EquipmentType type) {
        return (type instanceof MiscType)
                && (type.hasFlag(MiscType.F_TARGCOMP) || type.hasFlag(MiscType.F_CLUB)
                        || type.hasFlag(MiscType.F_TALON));
    }

    public static double getStartingTonnage(EquipmentType type) {
        return 1;
    }

    public static double getMaxTonnage(EquipmentType type) {
        if (type.hasFlag(MiscType.F_TALON) || (type.hasFlag(MiscType.F_CLUB)
                && (type.hasSubType(MiscType.S_HATCHET) || type.hasSubType(MiscType.S_MACE_THB)))) {
            return 7;
        } else if (type.hasFlag(MiscType.F_CLUB)
                && (type.hasSubType(MiscType.S_LANCE) || type.hasSubType(MiscType.S_SWORD))) {
            return 5;
        } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_MACE)) {
            return 10;
        } else if (type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE)) {
            return 5.5;
        } else if (type.hasFlag(MiscType.F_TARGCOMP)) {
            // direct fire weapon weight divided by 4 - what is reasonably the highest - 15 tons?
            return 15;
        }
        return 1;
    }

    public static double getTonnageIncrement(EquipmentType type) {
        if ((type.hasFlag(MiscType.F_CLUB) && type.hasSubType(MiscType.S_RETRACTABLE_BLADE))) {
            return 0.5;
        }
        return 1;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return (getEquipmentNum() == index) && (getLocation() == loc);
    }

    @Override
    public boolean isOmniPoddable() {
        if (type.isOmniFixedOnly()) {
            return false;
        }
        if (type instanceof MiscType) {
            return type.hasFlag(MiscType.F_MECH_EQUIPMENT) || type.hasFlag(MiscType.F_TANK_EQUIPMENT)
                    || type.hasFlag(MiscType.F_FIGHTER_EQUIPMENT);
        } else if (type instanceof WeaponType) {
            return (type.hasFlag(WeaponType.F_MECH_WEAPON) || type.hasFlag(WeaponType.F_TANK_WEAPON)
                    || type.hasFlag(WeaponType.F_AERO_WEAPON)) && !((WeaponType) type).isCapital();
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

    /**
     * This method will check for an existing weapon bay that this equipment belongs
     * to and if there is one it will check the status of that weapon bay based on
     * the equipment. If this equipment is functional, then it will clear any hits
     * from the bay. If not, then it will check all the other equipment in the bay
     * and if they are all damaged, then it will mark the bay as destroyed. This is
     * designed to be used only by the fix and remove methods contained here in
     * order to properly update weapon bay mounts on the entity
     */
    private static void checkWeaponBay(Unit unit, EquipmentType type, int equipmentNum) {
        if ((unit == null) || (unit.getEntity() == null)
                || !unit.getEntity().usesWeaponBays()
                || !(type instanceof WeaponType)) {
            return;
        }

        final WeaponMounted weapon = (WeaponMounted) unit.getEntity().getEquipment(equipmentNum);
        if (weapon == null) {
            return;
        }

        WeaponMounted weaponBay = null;
        for (WeaponMounted m : unit.getEntity().getWeaponBayList()) {
            if (m.getLocation() != weapon.getLocation()) {
                continue;
            }
            if ((m.getType() instanceof BayWeapon) && m.getBayWeapons().contains(weapon)) {
                weaponBay = m;
                break;
            }
        }

        if (weaponBay == null) {
            return;
        }

        int wBayIndex = unit.getEntity().getEquipmentNum(weaponBay);
        // ok we found the weapons bay, now lets check first to see if the current
        // weapon is fixed
        if (!weapon.isDestroyed()) {
            weaponBay.setHit(false);
            weaponBay.setMissing(false);
            weaponBay.setDestroyed(false);
            unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, wBayIndex);
            return;
        }

        // if we are still here then we need to check the other weapons, if any of them
        // are usable then we should do the same thing. Otherwise all weapons are destroyed
        // and we should mark the bay as unusuable.
        for (WeaponMounted m : weaponBay.getBayWeapons()) {
            if (!m.isDestroyed()) {
                weaponBay.setHit(false);
                weaponBay.setMissing(false);
                weaponBay.setDestroyed(false);
                unit.repairSystem(CriticalSlot.TYPE_EQUIPMENT, wBayIndex);
                return;
            }
        }

        weaponBay.setHit(true);
        weaponBay.setDestroyed(true);
        weaponBay.setRepairable(true);
        unit.destroySystem(CriticalSlot.TYPE_EQUIPMENT, wBayIndex);
    }
}
