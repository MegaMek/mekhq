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
package mekhq.campaign.parts.protomeks;

import java.io.PrintWriter;

import megamek.common.CriticalSlot;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechAdvancement.AdvancementPhase;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.equipment.IArmorState;
import megamek.common.equipment.Mounted;
import megamek.common.interfaces.ILocationExposureStatus;
import megamek.common.rolls.TargetRoll;
import megamek.common.units.ProtoMek;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.parts.missing.MissingProtoMekLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.work.WorkTime;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ProtoMekLocation extends Part {
    private static final MMLogger LOGGER = MMLogger.create(ProtoMekLocation.class);

    public static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TechBase.CLAN)
                                                                 .setClanAdvancement(3055, 3060, 3060)
                                                                 .setClanApproximate(AdvancementPhase.PROTOTYPE)
                                                                 .setPrototypeFactions(Faction.CSJ)
                                                                 .setProductionFactions(Faction.CSJ)
                                                                 .setTechRating(TechRating.D)
                                                                 .setAvailability(AvailabilityValue.X,
                                                                       AvailabilityValue.X,
                                                                       AvailabilityValue.D,
                                                                       AvailabilityValue.D)
                                                                 .setStaticTechLevel(SimpleTechLevel.STANDARD);

    // some of these aren't used but may be later for advanced designs (i.e. WoR)
    protected int loc;
    protected int structureType;
    protected boolean booster;
    double percent;
    boolean breached;
    boolean blownOff;
    boolean forQuad;

    public ProtoMekLocation() {
        this(0, 0, 0, false, false, null);
    }

    public ProtoMekLocation(int loc, int tonnage, int structureType, boolean hasBooster, boolean quad, Campaign c) {
        super(tonnage, c);
        this.loc = loc;
        this.structureType = structureType;
        this.booster = hasBooster;
        this.percent = 1.0;
        this.forQuad = quad;
        this.breached = false;
        this.unitTonnageMatters = true;
        this.name = "ProtoMek Location";
        switch (loc) {
            case ProtoMek.LOC_HEAD:
                this.name = "ProtoMek Head";
                break;
            case ProtoMek.LOC_TORSO:
                this.name = "ProtoMek Torso";
                break;
            case ProtoMek.LOC_LEFT_ARM:
                this.name = "ProtoMek Left Arm";
                break;
            case ProtoMek.LOC_RIGHT_ARM:
                this.name = "ProtoMek Right Arm";
                break;
            case ProtoMek.LOC_LEG:
                this.name = "ProtoMek Legs";
                if (forQuad) {
                    this.name = "ProtoMek Legs (Quad)";
                }
                break;
            case ProtoMek.LOC_MAIN_GUN:
                this.name = "ProtoMek Main Gun";
                break;
        }
        if (booster) {
            this.name += " (Myomer Booster)";
        }
    }

    @Override
    public ProtoMekLocation clone() {
        ProtoMekLocation clone = new ProtoMekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
        clone.copyBaseData(this);
        clone.percent = this.percent;
        clone.breached = this.breached;
        clone.blownOff = this.blownOff;
        return clone;
    }

    public int getLoc() {
        return loc;
    }

    public boolean hasBooster() {
        return booster;
    }

    public int getStructureType() {
        return structureType;
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public Money getStickerPrice() {
        double nLocation = 7.0;
        if (null != unit) {
            nLocation = unit.getEntity().locations();
        }
        double totalStructureCost = 2400 * getUnitTonnage();
        if (booster) {
            if (null != unit) {
                totalStructureCost += Math.round(unit.getEntity().getEngine().getRating() *
                                                       1000 *
                                                       unit.getEntity().getWeight() *
                                                       0.025f);
            } else {
                // FIXME: different costs by engine rating and weight, use a fake rating
                totalStructureCost += Math.round(75000 * getUnitTonnage() * 0.025f);
            }
        }
        double cost = totalStructureCost / nLocation;
        if (loc == ProtoMek.LOC_TORSO) {
            cost += 575000;
        }
        return Money.of(cost);
    }

    public boolean forQuad() {
        return forQuad;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof ProtoMekLocation &&
                     getLoc() == ((ProtoMekLocation) part).getLoc() &&
                     getUnitTonnage() == part.getUnitTonnage() &&
                     hasBooster() == ((ProtoMekLocation) part).hasBooster() &&
                     (!isLegs() || forQuad == ((ProtoMekLocation) part).forQuad);
        // && getStructureType() == ((ProtomekLocation) part).getStructureType();
    }

    private boolean isLegs() {
        return loc == ProtoMek.LOC_LEG;
    }

    @Override
    public boolean isSameStatus(Part part) {
        return super.isSameStatus(part) && this.getPercent() == ((ProtoMekLocation) part).getPercent();
    }

    public double getPercent() {
        return percent;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "loc", loc);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "structureType", structureType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "booster", booster);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percent", percent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forQuad", forQuad);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("percent")) {
                    percent = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("booster")) {
                    booster = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("forQuad")) {
                    forQuad = Boolean.parseBoolean(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("breached")) {
                    breached = Boolean.parseBoolean(wn2.getTextContent().trim());
                }
            } catch (Exception e) {
                LOGGER.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (isBlownOff()) {
            blownOff = false;
            if (null != unit) {
                unit.getEntity().setLocationBlownOff(loc, false);
                for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                    CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                    // ignore empty & non-hittable slots
                    if (slot == null) {
                        continue;
                    }
                    slot.setMissing(false);
                    Mounted<?> m = slot.getMount();
                    if (null != m) {
                        m.setMissing(false);
                    }
                }
            }
        } else if (isBreached()) {
            breached = false;
            if (null != unit) {
                unit.getEntity().setLocationStatus(loc, ILocationExposureStatus.NORMAL, true);
                for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
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
            percent = 1.0;
            if (null != unit) {
                unit.getEntity().setInternal(unit.getEntity().getOInternal(loc), loc);
            }
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingProtoMekLocation(loc, getUnitTonnage(), structureType, booster, forQuad, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        blownOff = false;
        if (null != unit) {
            unit.getEntity().setInternal(IArmorState.ARMOR_DESTROYED, loc);
            unit.getEntity().setLocationBlownOff(loc, false);
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            if (loc != ProtoMek.LOC_TORSO) {
                Part missing = getMissingPart();
                unit.addPart(missing);
                campaign.getQuartermaster().addPart(missing, 0, false);
            }
            // According to StratOps, this always destroys all equipment in that location as
            // well
            for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                final CriticalSlot cs = unit.getEntity().getCritical(loc, i);
                if (null == cs || !cs.isEverHittable()) {
                    continue;
                }
                cs.setHit(true);
                cs.setDestroyed(true);
                cs.setRepairable(false);
                Mounted<?> m = cs.getMount();
                if (null != m) {
                    m.setHit(true);
                    m.setDestroyed(true);
                    m.setRepairable(false);
                }
            }
            for (Mounted<?> m : unit.getEntity().getEquipment()) {
                if (m.getLocation() == loc || m.getSecondLocation() == loc) {
                    m.setHit(true);
                    m.setDestroyed(true);
                    m.setRepairable(false);
                }
            }
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if (null != unit) {
            blownOff = unit.getEntity().isLocationBlownOff(loc);
            breached = unit.isLocationBreached(loc);
            percent = ((double) unit.getEntity().getInternalForReal(loc)) /
                            ((double) unit.getEntity().getOInternal(loc));
            if (percent <= 0.0) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            if (blownOff) {
                return 0;
            }
            return 240;
        }
        if (blownOff) {
            return 200;
        }
        if (breached) {
            return 60;
        }
        if (percent < 0.25) {
            return 270;
        } else if (percent < 0.5) {
            return 180;
        } else if (percent < 0.75) {
            return 135;
        }
        return 90;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            if (isBlownOff()) {
                return 0;
            }
            return 3;
        }
        if (blownOff) {
            return 1;
        }
        if (breached) {
            return 0;
        }
        if (percent < 0.25) {
            return 2;
        } else if (percent < 0.5) {
            return 1;
        } else if (percent < 0.75) {
            return 0;
        }
        return -1;
    }

    public boolean isBreached() {
        return breached;
    }

    public boolean isBlownOff() {
        return blownOff;
    }

    @Override
    public boolean needsFixing() {
        return percent < 1.0 || breached || blownOff;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        String toReturn = "";
        if (null != unit) {
            toReturn = unit.getEntity().getLocationName(loc);
            if (includeRepairDetails) {
                if (isBlownOff()) {
                    toReturn += " (Blown Off)";
                } else if (isBreached()) {
                    toReturn += " (Breached)";
                } else {
                    toReturn += " (" + Math.round(100 * percent) + "%)";
                }
            }
            return toReturn;
        }
        toReturn += getUnitTonnage() + " tons";
        if (includeRepairDetails) {
            toReturn += " (" + Math.round(100 * percent) + "%)";
        }
        return toReturn;
    }

    private int getAppropriateSystemIndex() {
        return switch (loc) {
            case ProtoMek.LOC_LEG -> ProtoMek.SYSTEM_LEG_CRIT;
            case ProtoMek.LOC_LEFT_ARM, ProtoMek.LOC_RIGHT_ARM -> ProtoMek.SYSTEM_ARM_CRIT;
            case ProtoMek.LOC_HEAD -> ProtoMek.SYSTEM_HEAD_CRIT;
            case ProtoMek.LOC_TORSO -> ProtoMek.SYSTEM_TORSO_CRIT;
            default -> -1;
        };
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            unit.getEntity().setInternal((int) Math.round(percent * unit.getEntity().getOInternal(loc)), loc);
            // if all the system crits are marked off on the entity in this location, then
            // we need to
            // fix one of them, because the last crit on protomeks is always location
            // destruction
            int systemIndex = getAppropriateSystemIndex();
            if (loc != -1 && unit.getEntity().getGoodCriticalSlots(CriticalSlot.TYPE_SYSTEM, systemIndex, loc) <= 0) {
                // Because the last crit for protomeks is always location destruction we need to
                // clear the first system crit we find
                for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                    CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                    if ((slot != null) && slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                        slot.setDestroyed(false);
                        slot.setHit(false);
                        slot.setRepairable(true);
                        slot.setMissing(false);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        if (isSalvaging()) {
            // check for armor
            if (unit.getEntity().getArmorForReal(loc, false) > 0 ||
                      (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmorForReal(loc, true) > 0)) {
                return "must salvage armor in this location first";
            }
            // you can only salvage a location that has nothing left on it
            int systemRepairable = 0;
            for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
                CriticalSlot slot = unit.getEntity().getCritical(loc, i);
                // ignore empty & non-hittable slots
                if ((slot == null) || !slot.isEverHittable()) {
                    continue;
                }
                // we don't care about the final critical hit to the system
                // in locations because that just represents the location destruction
                if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                    if (slot.isRepairable()) {
                        if (systemRepairable > 0) {
                            return "Repairable parts in " +
                                         unit.getEntity().getLocationName(loc) +
                                         " must be salvaged or scrapped first.";
                        } else {
                            systemRepairable++;
                        }
                    }
                } else if (slot.isRepairable()) {
                    return "Repairable parts in " +
                                 unit.getEntity().getLocationName(loc) +
                                 " must be salvaged or scrapped first.";
                }
            }
            // protomeks only have system stuff in the crits, so we need to also
            // check for mounted equipment separately
            for (Mounted<?> m : unit.getEntity().getEquipment()) {
                if (m.isRepairable() && (m.getLocation() == loc || m.getSecondLocation() == loc)) {
                    return "Repairable parts in " +
                                 unit.getEntity().getLocationName(loc) +
                                 " must be salvaged or scrapped first." +
                                 m.getName();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isSalvaging() {
        // Can't salvage a center torso
        return (loc != ProtoMek.LOC_TORSO) && super.isSalvaging();
    }

    @Override
    public @Nullable String checkScrappable() {
        // Can't scrap a center torso
        if (loc == ProtoMek.LOC_TORSO) {
            return "ProtoMek Torsos cannot be scrapped";
        }
        // Check for armor
        if (unit.getEntity().getArmor(loc, false) > 0 ||
                  (unit.getEntity().hasRearArmor(loc) && unit.getEntity().getArmor(loc, true) > 0)) {
            return "You must first remove the armor from this location before you scrap it";
        }
        // you can only salvage a location that has nothing left on it
        int systemRepairable = 0;
        for (int i = 0; i < unit.getEntity().getNumberOfCriticalSlots(loc); i++) {
            CriticalSlot slot = unit.getEntity().getCritical(loc, i);
            // ignore empty & non-hittable slots
            if ((slot == null) || !slot.isEverHittable()) {
                continue;
            }
            // we don't care about the final critical hit to the system
            // in locations because that just represents the location destruction
            if (slot.getType() == CriticalSlot.TYPE_SYSTEM) {
                if (slot.isRepairable()) {
                    if (systemRepairable > 0) {
                        return "Repairable parts in " +
                                     unit.getEntity().getLocationName(loc) +
                                     " must be salvaged or scrapped first.";
                    } else {
                        systemRepairable++;
                    }
                }
            } else if (slot.isRepairable()) {
                return "Repairable parts in " +
                             unit.getEntity().getLocationName(loc) +
                             " must be salvaged or scrapped first.";
            }
        }
        // ProtoMeks only have system stuff in the crits, so we need to also check for
        // mounted
        // equipment separately
        for (Mounted<?> m : unit.getEntity().getEquipment()) {
            if (m.isRepairable() && (m.getLocation() == loc || m.getSecondLocation() == loc)) {
                return "Repairable parts in " +
                             unit.getEntity().getLocationName(loc) +
                             " must be salvaged or scrapped first." +
                             m.getName();
            }
        }
        return null;
    }

    @Override
    public TargetRoll getAllMods(Person tech) {
        if (isBreached() && !isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "fixing breach");
        }
        if (isBlownOff() && isSalvaging()) {
            return new TargetRoll(TargetRoll.AUTOMATIC_SUCCESS, "salvaging blown-off location");
        }
        return super.getAllMods(tech);
    }

    @Override
    public String getDesc() {
        if ((!isBreached() && !isBlownOff()) || isSalvaging()) {
            return super.getDesc();
        }
        StringBuilder toReturn = new StringBuilder();
        toReturn.append("<html><b>").append(isBlownOff() ? "Re-attach " : "Seal ").append(getName());
        if (isUnitTonnageMatters()) {
            toReturn.append(" (").append(getUnitTonnage()).append(" ton)");
        }
        if (!getCampaign().getCampaignOptions().isDestroyByMargin()) {
            toReturn.append(" - ")
                  .append(ReportingUtilities.messageSurroundedBySpanWithColor(SkillType.getExperienceLevelColor(
                        getSkillMin()), SkillType.getExperienceLevelName(getSkillMin()) + "+"));
        }
        toReturn.append("</b><br/>").append(getDetails()).append("<br/>");

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
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MEK);
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

    @Override
    public PartRepairType getRepairPartType() {
        return PartRepairType.MEK_LOCATION;
    }
}
