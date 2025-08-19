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
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.CriticalSlot;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.TechConstants;
import megamek.common.annotations.Nullable;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.ProtoMek;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillType;
import org.w3c.dom.Node;

/**
 * Legacy part that represents standard ProtoMek jump jets.
 *
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class ProtoMekJumpJet extends Part {
    static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TechBase.CLAN)
                                                          .setClanAdvancement(3055, 3060, 3060)
                                                          .setClanApproximate(true, false, false)
                                                          .setPrototypeFactions(Faction.CSJ)
                                                          .setProductionFactions(Faction.CSJ)
                                                          .setTechRating(TechRating.D)
                                                          .setAvailability(AvailabilityValue.X,
                                                                AvailabilityValue.X,
                                                                AvailabilityValue.C,
                                                                AvailabilityValue.C)
                                                          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    public ProtoMekJumpJet() {
        this(0, null);
    }

    @Override
    public ProtoMekJumpJet clone() {
        ProtoMekJumpJet clone = new ProtoMekJumpJet(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        return clone;
    }

    public ProtoMekJumpJet(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "ProtoMek Jump Jet";
        this.unitTonnageMatters = true;
    }

    @Override
    public double getTonnage() {
        if (getUnitTonnage() <= 5) {
            return 0.05;
        } else if (getUnitTonnage() <= 9) {
            return 0.1;
        } else {
            return 0.15;
        }
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(getUnitTonnage() * 400);
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof ProtoMekJumpJet
                     && getUnitTonnage() == part.getUnitTonnage();
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        writeToXMLEnd(pw, indent);
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit) {
            // repair depending upon how many others are still damaged
            int damageJJ = getOtherDamagedJumpJets();
            if (damageJJ == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
            } else if (damageJJ < (int) Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, 2);
            }
        }
    }

    @Override
    public TechBase getTechBase() {
        return TechBase.CLAN;
    }

    @Override
    public int getTechLevel() {
        return TechConstants.T_CLAN_TW;
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingProtoMekJumpJet(getUnitTonnage(), campaign);
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            int h = 1;
            int damageJJ = getOtherDamagedJumpJets() + 1;
            if (damageJJ >= (int) Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                h = 2;
            }
            unit.destroySystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, h);
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
        //FIXME: implement check for destruction
        if (null != unit) {
            hits = unit.getEntity()
                         .getDamagedCriticals(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
            if (hits > 2) {
                remove(false);
                return;
            }
            //only ever damage the first jump jet on the unit
            int damageJJ = 0;
            if (hits == 2) {
                damageJJ = (int) Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0);
            } else if (hits == 1) {
                damageJJ = 1;
            }
            damageJJ -= getOtherDamagedJumpJets();
            if (damageJJ > 0) {
                hits = 1;
            } else {
                hits = 0;
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 60;
        }
        return 90;
    }

    @Override
    public int getDifficulty() {
        return 0;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (null != unit) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO);
        }
        return getUnitTonnage() + " tons";
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            int damageJJ = getOtherDamagedJumpJets() + hits;
            if (damageJJ == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
            } else if (damageJJ < (int) Math.ceil(unit.getEntity().getOriginalJumpMP() / 2.0)) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, 1);
            } else {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO);
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, ProtoMek.SYSTEM_TORSOCRIT, ProtoMek.LOC_TORSO, 2);
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
        if (unit.isLocationBreached(ProtoMek.LOC_TORSO)) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO) + " is breached.";
        }
        if (isMountedOnDestroyedLocation()) {
            return unit.getEntity().getLocationName(ProtoMek.LOC_TORSO) + " is destroyed.";
        }
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return null != unit && unit.isLocationDestroyed(ProtoMek.LOC_TORSO);
    }

    @Override
    public boolean onBadHipOrShoulder() {
        return false;
    }

    @Override
    public boolean isPartForEquipmentNum(int index, int loc) {
        return false;//index == type && loc == location;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_MEK);
    }

    @Override
    public boolean isOmniPoddable() {
        return false;
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        // TODO Auto-generated method stub

    }

    private int getOtherDamagedJumpJets() {
        int damagedJJ = 0;
        if (null != unit) {
            for (Part p : unit.getParts()) {
                if (p.getId() == this.getId()) {
                    continue;
                }
                if (p instanceof MissingProtoMekJumpJet
                          || (p instanceof ProtoMekJumpJet && p.needsFixing())) {
                    damagedJJ++;
                }
            }
        }
        return damagedJJ;
    }

    @Override
    public String getLocationName() {
        return unit != null ? unit.getEntity().getLocationName(getLocation()) : null;
    }

    @Override
    public int getLocation() {
        return ProtoMek.LOC_TORSO;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }

}
