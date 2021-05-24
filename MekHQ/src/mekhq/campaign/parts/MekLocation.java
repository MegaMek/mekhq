/*
 * MekLocation.java
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import java.io.PrintWriter;
import java.util.StringJoiner;

import megamek.common.MiscType;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.EquipmentType;
import megamek.common.IArmorState;
import megamek.common.ILocationExposureStatus;
import megamek.common.LandAirMech;
import megamek.common.Mech;
import megamek.common.Mounted;
import megamek.common.TargetRoll;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.work.WorkTime;

/**
 * @author Jay Lawson <jaylawson39 at yahoo.com>
 */
public class MekLocation extends Part {
    private static final long serialVersionUID = -122291037522319765L;
    protected int loc;
    protected int structureType;
    protected boolean clan; // Only need for Endo-Steel
    protected boolean tsm;
    double percent;
    boolean breached;
    boolean blownOff;
    boolean forQuad;

    //system components for head
    protected boolean sensors;
    protected boolean lifeSupport;

    public MekLocation() {
        this(0, 0, 0, false, false, false, false, false, null);
    }

    @Override
    public MekLocation clone() {
        MekLocation clone = new MekLocation(loc, getUnitTonnage(), structureType, clan,
                tsm, forQuad, sensors, lifeSupport, campaign);
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

    public MekLocation(int loc, int tonnage, int structureType, boolean clan,
            boolean hasTSM, boolean quad, boolean sensors, boolean lifeSupport, Campaign c) {
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
        //TODO: need to account for internal structure and myomer types
        //crap, no static report for location names?
        this.name = "Mech Location";
        switch(loc) {
        case(Mech.LOC_HEAD):
            this.name = "Mech Head";
            break;
        case(Mech.LOC_CT):
            this.name = "Mech Center Torso";
            break;
        case(Mech.LOC_LT):
            this.name = "Mech Left Torso";
            break;
        case(Mech.LOC_RT):
            this.name = "Mech Right Torso";
            break;
        case(Mech.LOC_LARM):
            this.name = "Mech Left Arm";
            if (forQuad) {
                this.name = "Mech Front Left Leg";
            }
            break;
        case(Mech.LOC_RARM):
            this.name = "Mech Right Arm";
            if (forQuad) {
                this.name = "Mech Front Right Leg";
            }
            break;
        case(Mech.LOC_LLEG):
            this.name = "Mech Left Leg";
            if (forQuad) {
                this.name = "Mech Rear Left Leg";
            }
            break;
        case(Mech.LOC_RLEG):
            this.name = "Mech Right Leg";
            if (forQuad) {
                this.name = "Mech Rear Right Leg";
            }
            break;
        }
        if (EquipmentType.T_STRUCTURE_ENDO_STEEL == structureType) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType, isClan()) + ")";
        } else if (structureType != EquipmentType.T_STRUCTURE_STANDARD) {
            this.name += " (" + EquipmentType.getStructureTypeName(structureType) + ")";
        }
        if (tsm) {
            this.name += " (TSM)";
        }
    }

    @Override
    public double getTonnage() {
        //TODO: how much should this weigh?
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        double totalStructureCost = EquipmentType.getStructureCost(getStructureType()) * getUnitTonnage();
        int muscCost = isTsm() ? 16000 : 2000;
        double totalMuscleCost = muscCost * getUnitTonnage();
        double cost = 0.1 * (totalStructureCost + totalMuscleCost);

        if (loc == Mech.LOC_HEAD) {
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
        return loc == Mech.LOC_RARM || loc == Mech.LOC_LARM;
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isSamePartType(Part part) {
        if (!(part instanceof MekLocation)) {
            return false;
        }

        MekLocation other = (MekLocation) part;
        return (getLoc() == other.getLoc())
                && (getUnitTonnage() == other.getUnitTonnage())
                && (isTsm() == other.isTsm())
                && (getStructureType() == other.getStructureType())
                && ((getStructureType() != EquipmentType.T_STRUCTURE_ENDO_STEEL)
                        || (isClan() == other.isClan()))
                && (!isArm() || forQuad() == other.forQuad())
                // Sensors and life support only matter if we're comparing two parts in the warehouse.
                && ((getUnit() != null) || (other.getUnit() != null)
                        || (hasSensors() == other.hasSensors()
                                && hasLifeSupport() == other.hasLifeSupport()));
    }

    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) && this.getPercent() == ((MekLocation) part).getPercent();
    }

    public double getPercent() {
        return percent;
    }

    /**
     * Sets the percent armor remaining.
     * @param percent The percent armor remaining, expressed as a fraction.
     */
    protected void setPercent(double percent) {
        this.percent = Math.max(0.0, Math.min(percent, 1.0));
    }

    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        writeToXmlBegin(pw1, indent);
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<loc>"
                +loc
                +"</loc>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<structureType>"
                +structureType
                +"</structureType>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<clan>"
                +clan
                +"</clan>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<tsm>"
                +tsm
                +"</tsm>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<percent>"
                +percent
                +"</percent>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<forQuad>"
                +forQuad
                +"</forQuad>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<sensors>"
                +sensors
                +"</sensors>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<lifeSupport>"
                +lifeSupport
                +"</lifeSupport>");
        pw1.println(MekHqXmlUtil.indentStr(indent+1)
                +"<breached>"
                +breached
                +"</breached>");
        writeToXmlEnd(pw1, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

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
        }
    }

    @Override
    public void fix() {
        super.fix();

        final Unit unit = getUnit();
        if ((unit != null) && isBlownOff()) {
            setBlownOff(false);

            unit.getEntity().setLocationBlownOff(loc, false);
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty slots
                if (slot == null) {
                    continue;
                }

                slot.setMissing(false);
                Mounted m = slot.getMount();
                if (null != m) {
                    m.setMissing(false);
                }
            }
        } else if ((unit != null) && isBreached()) {
            setBreached(false);

            unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty slots
                if (slot == null) {
                    continue;
                }

                slot.setBreached(false);
                Mounted m = slot.getMount();
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
            if (getLoc() == Mech.LOC_HEAD) {
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

            if (getLoc() != Mech.LOC_CT) {
                Part missing = getMissingPart();
                assert(missing != null);

                unit.addPart(missing);
                campaign.getQuartermaster().addPart(missing, 0);

                missing.updateConditionFromEntity(false);
            }
        }
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (getUnit() != null) {
            setBlownOff(getUnit().getEntity().isLocationBlownOff(getLoc()));
            setBreached(getUnit().isLocationBreached(getLoc()));
            setPercent(getUnit().getEntity().getInternalForReal(getLoc()) / ((double) getUnit().getEntity().getOInternal(getLoc())));
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
                // it takes no time to salvage a blown off limb...
                // ...because it isn't there.
                return 0;
            } else {
                return getRepairOrSalvageTime();
            }
        }

        // StratOps p183 Master Repair Table
        if (isBlownOff()) {
            if (loc == Mech.LOC_HEAD) {
                // 200 minutes for blown off head
                return 200;
            } else {
                // 180 minutes for a blown off limb
                return 180;
            }
        }

        if (isBreached()) {
            // StratOps p177 - 'Mech Hull Breach
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
     * @return The time (in minutes) to repair or salvage this location.
     */
    private int getRepairOrSalvageTime() {
        // StratOps p184 Master Repair Table
        // NOTE: MissingMekLocation handles destroyed locations
        final double percent = getPercent();
        if (percent < 0.25) {
            return 270;
        } else if (percent < 0.5) {
            return 180;
        } else if (percent < 0.75) {
            return 135;
        } else {
            // <25% damage
            return 90;
        }
    }

    @Override
    public int getDifficulty() {
        // StratOps p191 / p183-185
        if (isSalvaging()) {
            if (isBlownOff()) {
                // The difficulty in removing a location which
                // does not exist is philosophical...
                return 0;
            } else {
                // ...otherwise use the difficulty table
                return getRepairOrSalvageDifficulty();
            }
        }

        // StratOps p183 Master Repair Table
        if (isBlownOff()) {
            if (loc == Mech.LOC_HEAD) {
                // +2 for re-attaching a head
                return 2;
            } else {
                // +1 for re-attaching anything else
                return 1;
            }
        }

        if (isBreached()) {
            // 'Mech hull breach is +0
            return 0;
        }

        return getRepairOrSalvageDifficulty();
    }

    /**
     * Gets the repair or salvage difficulty for this location.
     * @return The difficulty modifier for repair or salvage of this location.
     */
    private int getRepairOrSalvageDifficulty() {
        // StrapOps p184 Master Repair Table
        // NOTE: MissingMekLocation handles destroyed locations
        final double percent = getPercent();
        if (percent < 0.25) {
            return 2;
        } else if (percent < 0.5) {
            return 1;
        } else if (percent < 0.75) {
            return 0;
        } else {
            // <25% damage
            return -1;
        }
    }

    /**
     * Gets a value indicating whether or not this location is breached.
     */
    public boolean isBreached() {
        return (getUnit() != null) && breached;
    }

    /**
     * Sets a value indicating whether or not the location is breached.
     * @param breached A value indicating whether or not the location is breached.
     */
    protected void setBreached(boolean breached) {
        this.breached = breached;
    }

    /**
     * Gets a value indicating whether or not this location is blown off.
     */
    public boolean isBlownOff() {
        return (getUnit() != null) && blownOff;
    }

    /**
     * Sets a value indicating whether or not the location is blown off.
     * @param blownOff A value indicating whether or not the location is blown off.
     */
    protected void setBlownOff(boolean blownOff) {
        this.blownOff = blownOff;
    }

    @Override
    public boolean needsFixing() {
        return (getPercent() < 1.0) || isBreached() || isBlownOff()
                || ((getUnit() != null) && getUnit().hasBadHipOrShoulder(getLoc()));
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (getUnit() != null) {
            return getDetailsOnUnit(includeRepairDetails);
        }

        String toReturn = getUnitTonnage() + " tons";
        if (includeRepairDetails) {
            toReturn += " (" + Math.round(100 * getPercent()) + "%)";
        }

        if (loc == Mech.LOC_HEAD) {
            StringJoiner components = new StringJoiner(", ");
            if (hasSensors()) {
                components.add("Sensors");
            }
            if (hasLifeSupport()) {
                components.add("Life Support");
            }
            if (components.length() > 0) {
                toReturn += " [" + components.toString() + "]";
            }
        }

        return toReturn;
    }

    private String getDetailsOnUnit(boolean includeRepairDetails) {
        assert(getUnit() != null);

        String toReturn = getUnit().getEntity().getLocationName(loc);
        if (includeRepairDetails) {
            if (isBlownOff()) {
                toReturn += " (Blown Off)";
            } else if (isBreached()) {
                toReturn += " (Breached)";
            } else if (onBadHipOrShoulder()) {
                toReturn += " (Bad Hip/Shoulder)";
            } else {
                toReturn += " (" + Math.round(100 * getPercent()) + "%)";
            }
        }

        return toReturn;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal((int) Math.round(getPercent() * unit.getEntity().getOInternal(loc)), loc);
            //TODO: we need to cycle through slots and remove crits on non-hittable ones
            //We shouldn't have to do this, these slots should not be hit in MM
            for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                if ((slot != null) && !slot.isEverHittable()) {
                    slot.setDestroyed(false);
                    slot.setHit(false);
                    slot.setRepairable(true);
                    slot.setMissing(false);
                    Mounted m = slot.getMount();
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
    public String checkFixable() {
        if (unit == null) {
            return null;
        }

        if (isBlownOff() && !isSalvaging()) {
            if (loc == Mech.LOC_LARM && unit.isLocationDestroyed(Mech.LOC_LT)) {
                return "must replace left torso first";
            } else if (loc == Mech.LOC_RARM && unit.isLocationDestroyed(Mech.LOC_RT)) {
                return "must replace right torso first";
            } else if (unit.isLocationDestroyed(Mech.LOC_CT)) {
                // we shouldnt get here
                return "cannot repair part on destroyed unit";
            }
        } else if (isSalvaging()) {
            return checkSalvagable();
        } else if (!isBreached() && !isBlownOff()) {
            // check for damaged hips and shoulders
            if (onBadHipOrShoulder()) {
                return "You cannot repair a limb with a busted hip/shoulder. You must scrap and replace it instead.";
            }
        }

        return null;
    }

    /**
     * Gets a string indicating why the location is not salvagable, or {@code null} if
     * the location can be salvaged.
     */
    public @Nullable String checkSalvagable() {
        if (!isSalvaging()) {
            return null;
        }

        //dont allow salvaging of bad shoulder/hip limbs
        if (onBadHipOrShoulder()) {
            return "You cannot salvage a limb with a busted hip/shoulder. You must scrap it instead.";
        }
        //cant salvage torsos until arms and legs are gone
        String limbName = forQuad ? " front leg " : " arm ";
        if (unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && !unit.getEntity().isLocationBad(Mech.LOC_RARM)) {
            return "must salvage/scrap right" + limbName + "first";
        }
        if (unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && !unit.getEntity().isLocationBad(Mech.LOC_LARM)) {
            return "must salvage/scrap left" + limbName + "first";
        }
        //check for armor
        if (unit.getEntity().getArmorForReal(loc, false) > 0
                || (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmorForReal(loc, true) > 0 )) {
            return "must salvage armor in this location first";
        }
        //you can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }

            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
            if ((slot.getType() == CriticalSlot.TYPE_SYSTEM)
                    && ((slot.getIndex() == Mech.ACTUATOR_HIP)
                        || (slot.getIndex() == Mech.ACTUATOR_SHOULDER)
                        || (slot.getIndex() == Mech.SYSTEM_LIFE_SUPPORT)
                        || (slot.getIndex() == Mech.SYSTEM_SENSORS))) {
                continue;
            }

            if (unit.getEntity() instanceof LandAirMech) {
                // Skip Landing Gear if already gone
                if (slot.getIndex() == LandAirMech.LAM_LANDING_GEAR) {
                    if (unit.findPart(p -> p instanceof MissingLandingGear) != null) {
                        continue;
                    } else {
                        return "Landing gear in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
                    }
                // Skip Avionics if already gone
                } else if (slot.getIndex() == LandAirMech.LAM_AVIONICS) {
                    if (unit.findPart(p -> p instanceof MissingAvionics) != null) {
                        continue;
                    } else {
                        return "Avionics in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
                    }
                }
            }

            if (slot.getType() == CriticalSlot.TYPE_EQUIPMENT) {
                if ((slot.getMount() != null) && !slot.getMount().isDestroyed()) {
                    EquipmentType equipmentType = slot.getMount().getType();
                    if (equipmentType.hasFlag(MiscType.F_NULLSIG)) {
                            return "Null-Signature System must be salvaged or scrapped first.";
                    } else if (equipmentType.hasFlag(MiscType.F_VOIDSIG)) {
                        return "Void-Signature System must be salvaged or scrapped first.";
                    } else if (equipmentType.hasFlag(MiscType.F_CHAMELEON_SHIELD)) {
                        return "Chameleon shield must be salvaged or scrapped first.";
                    }
                }
            }

            if (slot.isRepairable()) {
                return "Repairable parts in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
            }
        }

        return null;
    }

    @Override
    public boolean isSalvaging() {
        //cant salvage a center torso
        if (loc ==  Mech.LOC_CT) {
            return false;
        }
        return super.isSalvaging();
    }

    @Override
    public String checkScrappable() {
        //cant scrap a center torso
        if (loc ==  Mech.LOC_CT) {
            return "Mech Center Torso's cannot be scrapped";
        }
        //only allow scrapping of locations with nothing on them
        //otherwise you will get weirdness where armor and actuators are
        //still attached but everything else is scrapped
        //cant salvage torsos until arms and legs are gone
        String limbName = " arm ";
        if (forQuad) {
            limbName = " front leg ";
        }
        if (unit.getEntity() instanceof Mech && loc == Mech.LOC_RT && !unit.getEntity().isLocationBad(Mech.LOC_RARM)) {
            return "You must first remove the right " + limbName + " before you scrap the right torso";
        }
        if (unit.getEntity() instanceof Mech && loc == Mech.LOC_LT && !unit.getEntity().isLocationBad(Mech.LOC_LARM)) {
            return "You must first remove the left " + limbName + " before you scrap the left torso";
        }
        //check for armor
        if (unit.getEntity().getArmorForReal(loc, false) > 0
                || (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmorForReal(loc, true) > 0 )) {
            return "You must first remove the armor from this location before you scrap it";
        }
        //you can only salvage a location that has nothing left on it
        for (int i = 0; i < unit.getEntity().getNumberOfCriticals(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }

            //certain other specific crits need to be left out (uggh, must be a better way to do this!)
            if (slot.getType() == CriticalSlot.TYPE_SYSTEM
                    && (slot.getIndex() == Mech.SYSTEM_COCKPIT
                          || slot.getIndex() == Mech.ACTUATOR_HIP
                          || slot.getIndex() == Mech.ACTUATOR_SHOULDER)) {
                continue;
            }

            if (unit.getEntity() instanceof LandAirMech) {
                // Skip Landing Gear if already gone
                if (slot.getIndex() == LandAirMech.LAM_LANDING_GEAR) {
                    if (unit.findPart(p -> p instanceof MissingLandingGear) != null) {
                        continue;
                    } else {
                        return "Landing gear in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
                    }
                // Skip Avionics if already gone
                } else if (slot.getIndex() == LandAirMech.LAM_AVIONICS) {
                    if (unit.findPart(p -> p instanceof MissingAvionics) != null) {
                        continue;
                    } else {
                        return "Avionics in " + unit.getEntity().getLocationName(loc) + " must be salvaged or scrapped first.";
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
        if ((!isBreached() && !isBlownOff()) || isSalvaging()) {
            return super.getDesc();
        }
        String toReturn = "<html><font size='2'";
        String scheduled = "";
        if (getTech() != null) {
            scheduled = " (scheduled) ";
        }

        toReturn += ">";
        if (isBlownOff()) {
            toReturn += "<b>Re-attach " + getName() + "</b><br/>";
        } else {
            toReturn += "<b>Seal " + getName() + "</b><br/>";
        }
        toReturn += getDetails() + "<br/>";
        if (getSkillMin() > SkillType.EXP_ELITE) {
            toReturn += "<font color='red'>Impossible</font>";
        } else {
            toReturn += "" + getTimeLeft() + " minutes" + scheduled;
            if (isBlownOff()) {
                String bonus = getAllMods(null).getValueAsString();
                if (getAllMods(null).getValue() > -1) {
                    bonus = "+" + bonus;
                }
                bonus = "(" + bonus + ")";
                if (!getCampaign().getCampaignOptions().isDestroyByMargin()) {
                    toReturn += ", " + SkillType.getExperienceLevelName(getSkillMin());
                }
                toReturn += " " + bonus;
                if (getMode() != WorkTime.NORMAL) {
                    toReturn += "<br/><i>" + getCurrentModeName() + "</i>";
                }
            }
        }
        toReturn += "</font></html>";
        return toReturn;
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return (getUnit() != null) && getUnit().hasBadHipOrShoulder(getLoc());
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MECH);
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
                sensor = (MekSensor)p;
            }
            if (null == support && p instanceof MekLifeSupport) {
                support = (MekLifeSupport)p;
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
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.GENERAL_LOCATION;
    }

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.MEK_LOCATION;
    }
}
