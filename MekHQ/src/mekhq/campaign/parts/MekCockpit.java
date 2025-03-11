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

import megamek.common.Compute;
import megamek.common.CriticalSlot;
import megamek.common.Entity;
import megamek.common.Mek;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.SkillType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MekCockpit extends Part {
    private static final MMLogger logger = MMLogger.create(MekCockpit.class);

    private int type;
    private boolean isClan;

    public MekCockpit() {
        this(0, Mek.COCKPIT_STANDARD, false, null);
    }

    public MekCockpit(int tonnage, int t, boolean isClan, Campaign c) {
        super(tonnage, c);
        this.type = t;
        this.name = Mek.getCockpitDisplayString(type);
        this.isClan = isClan;
    }

    @Override
    public MekCockpit clone() {
        MekCockpit clone = new MekCockpit(getUnitTonnage(), type, isClan, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public double getTonnage() {
        switch (type) {
            case Mek.COCKPIT_SMALL:
                return 2.0;
            case Mek.COCKPIT_TORSO_MOUNTED:
            case Mek.COCKPIT_DUAL:
            case Mek.COCKPIT_SUPERHEAVY:
            case Mek.COCKPIT_SUPERHEAVY_INDUSTRIAL:
            case Mek.COCKPIT_TRIPOD:
            case Mek.COCKPIT_TRIPOD_INDUSTRIAL:
            case Mek.COCKPIT_INTERFACE:
            case Mek.COCKPIT_QUADVEE:
                return 4.0;
            case Mek.COCKPIT_PRIMITIVE:
            case Mek.COCKPIT_PRIMITIVE_INDUSTRIAL:
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD:
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL:
            case Mek.COCKPIT_SMALL_COMMAND_CONSOLE:
                return 5.0;
            case Mek.COCKPIT_COMMAND_CONSOLE:
                return 6.0;
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                return 7.0;
            case Mek.COCKPIT_STANDARD:
            case Mek.COCKPIT_INDUSTRIAL:
            case Mek.COCKPIT_VRRP:
            default:
                return 3.0;
        }
    }

    @Override
    public Money getStickerPrice() {
        switch (type) {
            case Mek.COCKPIT_COMMAND_CONSOLE:
                // 500000 for command console + 200000 for primary cockpit
                return Money.of(700000);
            case Mek.COCKPIT_SMALL:
                return Money.of(175000);
            case Mek.COCKPIT_TORSO_MOUNTED:
                return Money.of(750000);
            case Mek.COCKPIT_INDUSTRIAL:
            case Mek.COCKPIT_PRIMITIVE_INDUSTRIAL:
                return Money.of(100000);
            case Mek.COCKPIT_DUAL:
                return Money.of(40000);
            case Mek.COCKPIT_VRRP:
                return Money.of(1250000);
            case Mek.COCKPIT_QUADVEE:
                return Money.of(375000);
            case Mek.COCKPIT_SUPERHEAVY:
            case Mek.COCKPIT_TRIPOD_INDUSTRIAL:
                return Money.of(300000);
            case Mek.COCKPIT_TRIPOD:
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD_INDUSTRIAL:
                return Money.of(400000);
            case Mek.COCKPIT_SUPERHEAVY_COMMAND_CONSOLE:
                return Money.of(800000);
            case Mek.COCKPIT_SUPERHEAVY_TRIPOD:
                return Money.of(500000);
            case Mek.COCKPIT_SMALL_COMMAND_CONSOLE:
                return Money.of(675000);
            case Mek.COCKPIT_STANDARD:
            default:
                return Money.of(200000);
        }
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof MekCockpit && ((MekCockpit) part).getType() == type;
    }

    public int getType() {
        return type;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", type);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("type")) {
                    type = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT);
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingMekCockpit(getUnitTonnage(), type, isClan, campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT);
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
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit) {
            Entity entity = unit.getEntity();
            for (int i = 0; i < entity.locations(); i++) {
                if (entity.getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i) > 0) {
                    // check for missing equipment as well
                    if (!unit.isSystemMissing(Mek.SYSTEM_COCKPIT, i)) {
                        hits = entity.getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i);
                        break;
                    } else {
                        remove(false);
                        return;
                    }
                }
            }
            if (checkForDestruction && hits > priorHits
                    && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 300;
        }
        // TODO: These are made up values until the errata establish them
        return 200;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        // TODO: These are made up values until the errata establish them
        return 3;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, hits);
            }
        }
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
            if (unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i) > 0) {
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
    public boolean isMountedOnDestroyedLocation() {
        if (null == unit) {
            return false;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_COCKPIT, i) > 0
                    && unit.isLocationDestroyed(i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MEK);
    }

    @Override
    public String getLocationName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getLocation() {
        if (type == Mek.COCKPIT_TORSO_MOUNTED) {
            return Mek.LOC_CT;
        } else {
            return Mek.LOC_HEAD;
        }
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return Mek.getCockpitTechAdvancement(type);
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
