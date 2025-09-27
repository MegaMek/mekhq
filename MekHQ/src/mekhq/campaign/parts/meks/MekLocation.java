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
package mekhq.campaign.parts.meks;

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.IntStream;

import megamek.codeUtilities.MathUtility;
import megamek.common.CriticalSlot;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.MiscType;
import megamek.common.equipment.Mounted;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.Entity;
import megamek.common.units.EntityMovementMode;
import megamek.common.units.LandAirMek;
import megamek.common.units.Mek;
import megamek.common.verifier.Ceil;
import megamek.common.verifier.Structure;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingAvionics;
import mekhq.campaign.parts.missing.MissingLandingGear;
import mekhq.campaign.parts.missing.MissingMekLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MekLocation extends Part {
    private static final MMLogger LOGGER = MMLogger.create(MekLocation.class);

    protected int loc;
    protected int structureType;
    protected boolean clan; // Only need for Endo-Steel
    protected boolean tsm;
    double percent;
    boolean breached;
    boolean blownOff;
    boolean forQuad;

    // system components for head
    protected boolean sensors;
    protected boolean lifeSupport;

    public MekLocation() {
        this(0, 0, 0, false, false, false, false, false, null);
        this.unitTonnageMatters = true;
    }

    @Override
    public MekLocation clone() {
        MekLocation clone = new MekLocation(loc,
              getUnitTonnage(),
              structureType,
              clan,
              tsm,
              forQuad,
              sensors,
              lifeSupport,
              campaign);
        clone.copyBaseData(this);
        clone.percent = this.percent;
        clone.breached = this.breached;
        clone.blownOff = this.blownOff;
        return clone;
    }

    public int getLoc() {
        return loc;
    }

    public boolean isTsm() {
        return tsm;
    }

    public int getStructureType() {
        return structureType;
    }

    public void setClan(boolean clan) {
        this.clan = clan;
    }

    public MekLocation(int loc, int tonnage, int structureType, boolean clan, boolean hasTSM, boolean quad,
          boolean sensors, boolean lifeSupport, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.clan = clan;
        this.tsm = hasTSM;
        this.percent = 1.0;
        this.forQuad = quad;
        this.sensors = sensors;
        this.lifeSupport = lifeSupport;
        this.breached = false;
        this.unitTonnageMatters = true;
        // TODO : need to account for internal structure and myomer types
        // crap, no static report for location names?
        this.name = "Mek Location";
        switch (loc) {
            case Mek.LOC_HEAD:
                this.name = "Mek Head";
                break;
            case Mek.LOC_CENTER_TORSO:
                this.name = "Mek Center Torso";
                break;
            case Mek.LOC_LEFT_TORSO:
                this.name = "Mek Left Torso";
                break;
            case Mek.LOC_RIGHT_TORSO:
                this.name = "Mek Right Torso";
                break;
            case Mek.LOC_LEFT_ARM:
                this.name = forQuad ? "Mek Front Left Leg" : "Mek Left Arm";
                break;
            case Mek.LOC_RIGHT_ARM:
                this.name = forQuad ? "Mek Front Right Leg" : "Mek Right Arm";
                break;
            case Mek.LOC_LEFT_LEG:
                this.name = forQuad ? "Mek Rear Left Leg" : "Mek Left Leg";
                break;
            case Mek.LOC_RIGHT_LEG:
                this.name = forQuad ? "Mek Rear Right Leg" : "Mek Right Leg";
                break;
            case Mek.LOC_CENTER_LEG:
                this.name = "Mek Center Leg";
                break;
        }

        if (EquipmentType.T_STRUCTURE_ENDO_STEEL == structureType) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType, clan) + ')';
        } else if (structureType != EquipmentType.T_STRUCTURE_STANDARD) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType) + ')';
        }

        if (tsm) {
            this.name += " (TSM)";
        }
    }

    @Override
    public double getTonnage() {
        // use MegaMek's implementation of internal structure weight calculation
        // assume rounding to nearest half-ton
        // superheavy flag is set if weight is more than 100
        // assume movement mode is biped or tripod (technically meks can have other
        // movement modes but
        // that doesn't affect structure weight); currently impossible to tell whether a
        // "loose" left leg is for a biped or tripod.
        EntityMovementMode movementMode = (getLoc() == Mek.LOC_CENTER_LEG) ?
                                                EntityMovementMode.TRIPOD :
                                                EntityMovementMode.BIPED;

        double tonnage = Structure.getWeightStructure(structureType,
              getUnitTonnage(),
              Ceil.HALF_TON,
              (getUnitTonnage() > 100),
              movementMode);

        // determine the weight of the location;
        // if it's a "strange" location, then the rest of this is pointless.
        switch (loc) {
            case Mek.LOC_HEAD:
                tonnage *= 0.05;
                break;
            case Mek.LOC_CENTER_TORSO:
                tonnage *= 0.25;
                break;
            case Mek.LOC_LEFT_TORSO:
            case Mek.LOC_RIGHT_TORSO:
                tonnage *= 0.15;
                break;
            case Mek.LOC_LEFT_ARM:
            case Mek.LOC_RIGHT_ARM:
            case Mek.LOC_LEFT_LEG:
            case Mek.LOC_RIGHT_LEG:
            case Mek.LOC_CENTER_LEG:
                tonnage *= 0.1;
                break;
            default:
                return 0;
        }

        return tonnage;
    }

    @Override
    public Money getStickerPrice() {
        double totalStructureCost = EquipmentType.getStructureCost(getStructureType()) * getUnitTonnage();
        int muscleCost = isTsm() ? 16000 : 2000;
        double totalMuscleCost = muscleCost * getUnitTonnage();
        double cost = 0.1 * (totalStructureCost + totalMuscleCost);

        if (loc == Mek.LOC_HEAD) {
            if (sensors) {
                cost += 2000 * getUnitTonnage();
            }

            if (lifeSupport) {
                cost += 50000;
            }
        }
        return Money.of(cost);
    }

    private boolean isArm() {
        return (loc == Mek.LOC_RIGHT_ARM) || (loc == Mek.LOC_LEFT_ARM);
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isSamePartType(Part part) {
        if (!(part instanceof MekLocation other)) {
            return false;
        }

        return (getLoc() == other.getLoc()) &&
                     (getUnitTonnage() == other.getUnitTonnage()) &&
                     (isTsm() == other.isTsm()) &&
                     (getStructureType() == other.getStructureType()) &&
                     ((getStructureType() != EquipmentType.T_STRUCTURE_ENDO_STEEL) || (isClan() == other.isClan())) &&
                     (!isArm() || forQuad() == other.forQuad())
                     // Sensors and life support only matter if we're comparing two parts in the
                     // warehouse.
                     &&
                     ((getUnit() != null) ||
                            (other.getUnit() != null) ||
                            (hasSensors() == other.hasSensors() && hasLifeSupport() == other.hasLifeSupport()));
    }

    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) && (this.getPercent() == ((MekLocation) part).getPercent());
    }

    public double getPercent() {
        return percent;
    }

    /**
     * Sets the percent armor remaining.
     *
     * @param percent The percent armor remaining, expressed as a fraction.
     */
    public void setPercent(double percent) {
        this.percent = MathUtility.clamp(percent, 0.0, 1.0);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "structureType", structureType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "clan", clan);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "tsm", tsm);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percent", percent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forQuad", forQuad);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "sensors", sensors);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lifeSupport", lifeSupport);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("structureType")) {
                    structureType = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("clan")) {
                    clan = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
                    percent = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("tsm")) {
                    tsm = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
                    forQuad = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("sensors")) {
                    sensors = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("lifeSupport")) {
                    lifeSupport = Boolean.parseBoolean(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("breached")) {
                    breached = Boolean.parseBoolean(wn2.getTextContent());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();

        final Unit unit = getUnit();
        if ((unit != null) && isBlownOff()) {
            setBlownOff(false);

            unit.getEntity().setLocationBlownOff(loc, false);
            for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty slots
                if (slot == null) {
                    continue;
                }

                slot.setMissing(false);
                Mounted<?> m = slot.getMount();
                if (null != m) {
                    m.setMissing(false);
                }
            }
        } else if ((unit != null) && isBreached()) {
            setBreached(false);

            unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
            for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty slots
                if (slot == null) {
                    continue;
                }

                slot.setBreached(false);
                Mounted<?> m = slot.getMount();
                if (null != m) {
                    m.setBreached(false);
                }
            }
        } else {
            setPercent(1.0);
            if (unit != null) {
                unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
            }
        }
    }

    @Override
    public MissingMekLocation getMissingPart() {
        return new MissingMekLocation(loc, getUnitTonnage(), structureType, clan, tsm, forQuad, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        setBlownOff(false);
        setBreached(false);

        final Unit unit = getUnit();
        if (unit != null) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, getLoc());
            unit.getEntity().setLocationBlownOff(getLoc(), false);
            unit.getEntity().setLocationStatus(getLoc(), ILocationExposureStatus.NORMAL, true);

            // If this is a head. check for life support and sensors
            if (getLoc() == Mek.LOC_HEAD) {
                removeHeadComponents();
            }

            unit.removePart(this);
            setUnit(null);

            if (salvage) {
                // Return this part to the warehouse as a spare
                getCampaign().getWarehouse().addPart(this);
            } else {
                // Remove this part from the campaign
                getCampaign().getWarehouse().removePart(this);
            }

            if (getLoc() != Mek.LOC_CENTER_TORSO) {
                Part missing = Objects.requireNonNull(getMissingPart());

                unit.addPart(missing);
                campaign.getQuartermaster().addPart(missing, 0, false);

                missing.updateConditionFromEntity(false);
            }
        }
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (getUnit() != null) {
            setBlownOff(getUnit().getEntity().isLocationBlownOff(getLoc()));
            setBreached(getUnit().isLocationBreached(getLoc()));
            setPercent(getUnit().getEntity().getInternalForReal(getLoc()) /
                             ((double) getUnit().getEntity().getOInternal(getLoc())));
            if (getPercent() <= 0.0) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        // StratOps p191 / p183-185
        if (isSalvaging()) {
            if (isBlownOff()) {
                // it takes no time to salvage a blown off limb... because it isn't there.
                return 0;
            } else {
                return getRepairOrSalvageTime();
            }
        }

        // StratOps p183 Master Repair Table
        if (isBlownOff()) {
            if (loc == Mek.LOC_HEAD) {
                // 200 minutes for blown off head
                return 200;
            } else {
                // 180 minutes for a blown off limb
                return 180;
            }
        }

        if (isBreached()) {
            // StratOps p177 - 'Mek Hull Breach
            // "Once this time is spent [fixing the breach], the components
            // in the damaged location work normally. Other damage suffered
            // by that location prior to and after the breach must be
            // repaired normally."
            return 60;
        }

        return getRepairOrSalvageTime();
    }

    /**
     * Gets the time (in minutes) to repair or salvage this location.
     *
     * @return The time (in minutes) to repair or salvage this location.
     */
    private int getRepairOrSalvageTime() {
        // StratOps p184 Master Repair Table
        // NOTE : MissingMekLocation handles destroyed locations
        final double percent = getPercent();
        if (percent < 0.25) {
            return 270;
        } else if (percent < 0.5) {
            return 180;
        } else if (percent < 0.75) {
            return 135;
        } else {
            // < 25% damage
            return 90;
        }
    }

    @Override
    public int getDifficulty() {
        // StratOps p191 / p183-185
        if (isSalvaging()) {
            if (isBlownOff()) {
                // The difficulty in removing a location which does not exist is
                // philosophical...
                return 0;
            } else {
                // ... otherwise use the difficulty table
                return getRepairOrSalvageDifficulty();
            }
        }

        // StratOps p183 Master Repair Table
        if (isBlownOff()) {
            if (loc == Mek.LOC_HEAD) {
                // +2 for re-attaching a head
                return 2;
            } else {
                // +1 for re-attaching anything else
                return 1;
            }
        }

        if (isBreached()) {
            // 'Mek hull breach is +0
            return 0;
        }

        return getRepairOrSalvageDifficulty();
    }

    /**
     * Gets the repair or salvage difficulty for this location.
     *
     * @return The difficulty modifier for repair or salvage of this location.
     */
    private int getRepairOrSalvageDifficulty() {
        // StrapOps p184 Master Repair Table
        // NOTE : MissingMekLocation handles destroyed locations
        final double percent = getPercent();
        if (percent < 0.25) {
            return 2;
        } else if (percent < 0.5) {
            return 1;
        } else if (percent < 0.75) {
            return 0;
        } else {
            // < 25% damage
            return -1;
        }
    }

    /**
     * Gets a value indicating whether this location is breached.
     */
    public boolean isBreached() {
        return (getUnit() != null) && breached;
    }

    /**
     * Sets a value indicating whether the location is breached.
     *
     * @param breached A value indicating whether the location is breached.
     */
    public void setBreached(boolean breached) {
        this.breached = breached;
    }

    /**
     * Gets a value indicating whether this location is blown off.
     */
    public boolean isBlownOff() {
        return (getUnit() != null) && blownOff;
    }

    /**
     * Sets a value indicating whether the location is blown off.
     *
     * @param blownOff A value indicating whether the location is blown off.
     */
    public void setBlownOff(boolean blownOff) {
        this.blownOff = blownOff;
    }

    @Override
    public boolean needsFixing() {
        return (getPercent() < 1.0) ||
                     isBreached() ||
                     isBlownOff() ||
                     ((getUnit() != null) && getUnit().hasBadHipOrShoulder(getLoc()));
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        StringBuilder toReturn = new StringBuilder();

        if (null != getUnit()) {
            toReturn.append(Objects.requireNonNull(getUnit()).getEntity().getLocationName(loc)).append(", ");
        }

        toReturn.append(getUnitTonnage()).append(" tons");

        if (loc == Mek.LOC_HEAD) {
            StringJoiner components = new StringJoiner(", ");
            if (hasSensors()) {
                components.add("Sensors");
            }
            if (hasLifeSupport()) {
                components.add("Life Support");
            }
            if (components.length() > 0) {
                toReturn.append(" [").append(components).append(']');
            }
        }

        if (includeRepairDetails) {
            if (isBlownOff()) {
                toReturn.append(" (Blown Off)");
            } else if (isBreached()) {
                toReturn.append(" (Breached)");
            } else if (onBadHipOrShoulder()) {
                toReturn.append(" (Bad Hip/Shoulder)");
            } else if (getPercent() < 1.0) {
                toReturn.append(" (").append(Math.round(100 * getPercent())).append("%)");
                if (campaign.getCampaignOptions().isPayForRepairs()) {
                    toReturn.append(", ")
                          .append(getUndamagedValue().multipliedBy(0.2).toAmountAndSymbolString())
                          .append(" to repair");
                }
            }
        }

        return toReturn.toString();
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal((int) Math.round(getPercent() * unit.getEntity().getOInternal(loc)), loc);
            // TODO : we need to cycle through slots and remove crits on non-hittable ones
            // We shouldn't have to do this, these slots should not be hit in MM
            for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                if ((slot != null) && !slot.isEverHittable()) {
                    slot.setDestroyed(false);
                    slot.setHit(false);
                    slot.setRepairable(true);
                    slot.setMissing(false);
                    Mounted<?> m = slot.getMount();
                    if (m != null) {
                        m.setHit(false);
                        m.setDestroyed(false);
                        m.setMissing(false);
                        m.setRepairable(true);
                    }
                }
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (unit == null) {
            return null;
        }

        if (isBlownOff() && !isSalvaging()) {
            if ((loc == Mek.LOC_LEFT_ARM) && unit.isLocationDestroyed(Mek.LOC_LEFT_TORSO)) {
                return "must replace left torso first";
            } else if ((loc == Mek.LOC_RIGHT_ARM) && unit.isLocationDestroyed(Mek.LOC_RIGHT_TORSO)) {
                return "must replace right torso first";
            } else if (unit.isLocationDestroyed(Mek.LOC_CENTER_TORSO)) {
                // we shouldn't get here
                return "cannot repair part on destroyed unit";
            }
        } else if (isSalvaging()) {
            return checkSalvageable();
        } else if (!isBreached() && !isBlownOff()) {
            // check for damaged hips and shoulders
            if (onBadHipOrShoulder()) {
                return "You cannot repair a limb with a busted hip/shoulder. You must scrap and replace it instead.";
            }
        }

        return null;
    }

    /**
     * Gets a string indicating why the location is not salvageable, or {@code null} if the location can be salvaged.
     */
    public @Nullable String checkSalvageable() {
        if (!isSalvaging()) {
            return null;
        }

        // Don't allow salvaging of bad shoulder/hip limbs
        if (onBadHipOrShoulder()) {
            return "You cannot salvage a limb with a busted hip/shoulder. You must scrap it instead.";
        }
        // Can't salvage torsos until arms and legs are gone
        String limbName = forQuad ? " front leg " : " arm ";
        if ((unit.getEntity() instanceof Mek) &&
                  (loc == Mek.LOC_RIGHT_TORSO) &&
                  !unit.getEntity().isLocationBad(Mek.LOC_RIGHT_ARM)) {
            return "must salvage/scrap right" + limbName + "first";
        } else if ((unit.getEntity() instanceof Mek) &&
                         (loc == Mek.LOC_LEFT_TORSO) &&
                         !unit.getEntity().isLocationBad(Mek.LOC_LEFT_ARM)) {
            return "must salvage/scrap left" + limbName + "first";
        }
        // Check for armor
        if ((unit.getEntity().getArmorForReal(loc, false) > 0) ||
                  (unit.getEntity().hasRearArmor(loc) && (unit.getEntity().getArmorForReal(loc, true) > 0))) {
            return "must salvage armor in this location first";
        }

        // You can only salvage a location that has nothing left on it
        Set<Integer> equipmentSeen = new HashSet<>();
        StringJoiner partsToSalvageOrScrap = new StringJoiner(", ");
        for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // Ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }

            // Certain other specific crits need to be left out
            // ( there must be a better way to do this!)
            if ((slot.getType() == CriticalSlot.TYPE_SYSTEM) &&
                      IntStream.of(Mek.ACTUATOR_HIP, Mek.ACTUATOR_SHOULDER, Mek.SYSTEM_LIFE_SUPPORT, Mek.SYSTEM_SENSORS)
                            .anyMatch(j -> (slot.getIndex() == j))) {
                continue;
            }

            if (unit.getEntity() instanceof LandAirMek) {
                // Skip Landing Gear if already gone
                if (slot.getIndex() == LandAirMek.LAM_LANDING_GEAR) {
                    if (unit.findPart(p -> p instanceof MissingLandingGear) != null) {
                        continue;
                    } else {
                        partsToSalvageOrScrap.add(String.format("Landing Gear (%s)",
                              unit.getEntity().getLocationName(loc)));
                    }
                    // Skip Avionics if already gone
                } else if (slot.getIndex() == LandAirMek.LAM_AVIONICS) {
                    if (unit.findPart(p -> p instanceof MissingAvionics) != null) {
                        continue;
                    } else {
                        partsToSalvageOrScrap.add(String.format("Avionics (%s)",
                              unit.getEntity().getLocationName(loc)));
                    }
                }
            }

            if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if ((slot.getMount() != null) && !slot.getMount().isDestroyed()) {
                    EquipmentType equipmentType = slot.getMount().getType();
                    if (equipmentType.hasFlag(MiscType.F_NULL_SIG)) {
                        partsToSalvageOrScrap.add("Null-Signature System");
                    } else if (equipmentType.hasFlag(MiscType.F_VOID_SIG)) {
                        partsToSalvageOrScrap.add("Void-Signature System");
                    } else if (equipmentType.hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                        partsToSalvageOrScrap.add("Chameleon Shield");
                    }
                }
            }

            if (slot.isRepairable()) {
                String partName = "Repairable Part";

                // Try to get a more specific name
                int equipmentNum = unit.getEntity().getEquipmentNum(slot.getMount());
                if (equipmentNum >= 0) {
                    if (!equipmentSeen.add(equipmentNum)) {
                        // We have already marked this part as needing to be salvaged/scrapped
                        continue;
                    }

                    Part repairablePart = unit.findPart(p -> (p instanceof EquipmentPart) &&
                                                                   (((EquipmentPart) p).getEquipmentNum() ==
                                                                          equipmentNum));
                    if (repairablePart != null) {
                        partName = repairablePart.getName();
                    }
                }

                partsToSalvageOrScrap.add(String.format("%s (%s)", partName, unit.getEntity().getLocationName(loc)));
            }
        }

        if (partsToSalvageOrScrap.length() == 0) {
            return null;
        }

        return "The following parts must be salvaged or scrapped first: " + partsToSalvageOrScrap;
    }

    @Override
    public boolean isSalvaging() {
        // Can't salvage a center torso
        return (loc != Mek.LOC_CENTER_TORSO) && super.isSalvaging();
    }

    @Override
    public String checkScrappable() {
        // Can't scrap a center torso
        if (loc == Mek.LOC_CENTER_TORSO) {
            return "Mek Center Torso's cannot be scrapped";
        }
        // Only allow scrapping of locations with nothing on them, otherwise you will
        // get weirdness
        // where armor and actuators are still attached but everything else is scrapped
        // You can't salvage torsos until arms and legs are gone
        String limbName = forQuad ? " front leg " : " arm ";

        if ((unit.getEntity() instanceof Mek) &&
                  (loc == Mek.LOC_RIGHT_TORSO) &&
                  !unit.getEntity().isLocationBad(Mek.LOC_RIGHT_ARM)) {
            return "You must first remove the right " + limbName + " before you scrap the right torso";
        } else if ((unit.getEntity() instanceof Mek) &&
                         (loc == Mek.LOC_LEFT_TORSO) &&
                         !unit.getEntity().isLocationBad(Mek.LOC_LEFT_ARM)) {
            return "You must first remove the left " + limbName + " before you scrap the left torso";
        }
        // Check for armor
        if ((unit.getEntity().getArmorForReal(loc, false) > 0) ||
                  (unit.getEntity().hasRearArmor(loc) && (unit.getEntity().getArmorForReal(loc, true) > 0))) {
            return "You must first remove the armor from this location before you scrap it";
        }
        // You can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // Ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }

            // Certain other specific crits need to be left out
            // (there must be a better way to do this!)
            if ((slot.getType() == CriticalSlot.TYPE_SYSTEM) &&
                      (IntStream.of(Mek.SYSTEM_COCKPIT, Mek.ACTUATOR_HIP, Mek.ACTUATOR_SHOULDER)
                             .anyMatch(j -> slot.getIndex() == j))) {
                continue;
            }

            if (unit.getEntity() instanceof LandAirMek) {
                // Skip Landing Gear if already gone
                if (slot.getIndex() == LandAirMek.LAM_LANDING_GEAR) {
                    if (unit.findPart(p -> p instanceof MissingLandingGear) != null) {
                        continue;
                    } else {
                        return "Landing gear in " +
                                     unit.getEntity().getLocationName(loc) +
                                     " must be salvaged or scrapped first.";
                    }
                    // Skip Avionics if already gone
                } else if (slot.getIndex() == LandAirMek.LAM_AVIONICS) {
                    if (unit.findPart(p -> p instanceof MissingAvionics) != null) {
                        continue;
                    } else {
                        return "Avionics in " +
                                     unit.getEntity().getLocationName(loc) +
                                     " must be salvaged or scrapped first.";
                    }
                }
            }

            if (slot.isRepairable()) {
                return "You must first remove all equipment from this location before you scrap it";
            }
        }

        return null;
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        if (isBreached() && !isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
        } else if (isBlownOff() && isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "salvaging blown-off location");
        } else {
            return super.getAllMods(tech);
        }
    }

    @Override
    public String getDesc() {
        if ((!isBreached() && !isBlownOff())) {
            return super.getDesc();
        }
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("<html><b>")
              .append(isBlownOff() ? "Re-attach " : "Seal ")
              .append(getName())
              .append(" (")
              .append(getUnitTonnage())
              .append(" ton) - ")
              .append(ReportingUtilities.messageSurroundedBySpanWithColor(SkillType.getExperienceLevelColor(getSkillMin()),
                    SkillType.getExperienceLevelName(getSkillMin()) + "+"))
              .append("</b><br/>")
              .append(getDetails())
              .append("<br/>");

        if (getSkillMin() <= SkillType.EXP_LEGENDARY) {
            toReturn.append(getTimeLeft()).append(" minutes").append(null != getTech() ? " (scheduled)" : "");
            if (isBlownOff()) {
                toReturn.append(" <b>TN:</b> ")
                      .append(getAllMods(null).getValue() > -1 ? "+" : "")
                      .append(getAllMods(null).getValueAsString());
            }
            if (getMode() != WorkTime.NORMAL) {
                toReturn.append(" <i>").append(getCurrentModeName()).append("</i>");
            }
        }
        toReturn.append("</html>");
        return toReturn.toString();
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return (getUnit() != null) && getUnit().hasBadHipOrShoulder(getLoc());
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MEK);
    }

    public boolean hasSensors() {
        return sensors;
    }

    public void setSensors(boolean b) {
        sensors = b;
    }

    public boolean hasLifeSupport() {
        return lifeSupport;
    }

    public void setLifeSupport(boolean b) {
        lifeSupport = b;
    }

    private void removeHeadComponents() {
        MekSensor sensor = null;
        MekLifeSupport support = null;
        for (Part p : unit.getParts()) {
            if (null == sensor && p instanceof MekSensor) {
                sensor = (MekSensor) p;
            }
            if (null == support && p instanceof MekLifeSupport) {
                support = (MekLifeSupport) p;
            }
            if (null != sensor && null != support) {
                break;
            }
        }
        if (null != sensor) {
            sensor.remove(false);
            setSensors(true);
        }
        if (null != support) {
            support.remove(false);
            setLifeSupport(true);
        }
    }

    @Override
    public void doMaintenanceDamage(int d) {
        if ((getUnit() != null) && (d > 0)) {
            int points = getUnit().getEntity().getInternal(getLoc());
            getUnit().getEntity().setInternal(Math.max(points - d, 1), getLoc());
            updateConditionFromEntity(false);
        }
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return EquipmentType.getStructureTechAdvancement(structureType, clan);
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.MEK_LOCATION;
    }
}
