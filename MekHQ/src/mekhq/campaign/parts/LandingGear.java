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
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.TechRating;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.LandAirMek;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.missing.MissingLandingGear;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import org.w3c.dom.Node;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class LandingGear extends Part {
    public LandingGear() {
        this(0, null);
    }

    public LandingGear(int tonnage, Campaign c) {
        super(tonnage, c);
        this.name = "Landing Gear";
    }

    @Override
    public LandingGear clone() {
        LandingGear clone = new LandingGear(0, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                if (((Aero) unit.getEntity()).isGearHit()) {
                    hits = 1;
                } else {
                    hits = 0;
                }
            } else if (unit.getEntity() instanceof LandAirMek) {
                hits = unit.getHitCriticalSlots(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR);
            }
            if (checkForDestruction
                      && hits > priorHits
                      && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time;
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair times
            if (unit != null && unit.getEntity() instanceof Dropship) {
                time = 120;
            } else {
                time = 60;
            }
            if (isSalvaging()) {
                time *= 10;
            }
            return time;
        }
        if (isSalvaging()) {
            time = 1200;
        } else {
            time = 120;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 3;
        }
        return 2;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setGearHit(needsFixing());
        } else if (null != unit && unit.getEntity() instanceof LandAirMek) {
            if (hits == 0) {
                unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR);
            } else {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, hits);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setGearHit(false);
        } else if (null != unit && unit.getEntity() instanceof LandAirMek) {
            unit.repairSystem(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setGearHit(true);
            } else if (unit.getEntity() instanceof LandAirMek) {
                unit.damageSystem(CriticalSlot.TYPE_SYSTEM, LandAirMek.LAM_LANDING_GEAR, 3);
            }
            Part spare = campaign.getWarehouse().checkForExistingSparePart(this);
            if (!salvage) {
                campaign.getWarehouse().removePart(this);
            } else if (null != spare) {
                spare.changeQuantity(1);
                campaign.getWarehouse().removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.getQuartermaster().addPart(missing, 0, false);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingLandingGear(getUnitTonnage(), campaign);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public Money getStickerPrice() {
        return Money.of(10.0 * getUnitTonnage());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public TechRating getTechRating() {
        //go with conventional fighter avionics
        return TechRating.B;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof LandingGear;
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
    public boolean isRightTechType(String skillType) {
        if (unit != null && unit.getEntity() instanceof LandAirMek) {
            return skillType.equals(SkillTypeNew.S_TECH_MEK.name());
        }
        return (skillType.equals(SkillTypeNew.S_TECH_AERO.name()) ||
                      skillType.equals(SkillTypeNew.S_TECH_VESSEL.name()));
    }

    @Override
    public String getLocationName() {
        if (null != unit) {
            return unit.getEntity().getLocationName(unit.getEntity().getBodyLocation());
        }
        return null;
    }

    @Override
    public int getLocation() {
        if (null != unit) {
            return unit.getEntity().getBodyLocation();
        }
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TA_GENERIC;
    }
}
