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

import megamek.common.units.Aero;
import megamek.common.compute.Compute;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class AeroSensor extends Part {
    final static TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TechBase.ALL)
                                                          .setISAdvancement(DATE_ES, DATE_ES, DATE_ES)
                                                          .setTechRating(TechRating.C)
                                                          .setAvailability(AvailabilityValue.C,
                                                                AvailabilityValue.C,
                                                                AvailabilityValue.C,
                                                                AvailabilityValue.C)
                                                          .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private boolean largeCraft;

    public AeroSensor() {
        this(0, false, null);
    }

    public AeroSensor(int tonnage, boolean largeCraft, Campaign campaign) {
        super(tonnage, campaign);
        this.name = "Aerospace Sensors";
        this.largeCraft = largeCraft;
        this.unitTonnageMatters = !largeCraft;
    }

    @Override
    public AeroSensor clone() {
        AeroSensor clone = new AeroSensor(getUnitTonnage(), largeCraft, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Aero) {
            hits = ((Aero) unit.getEntity()).getSensorHits();
            if (checkForDestruction
                      && hits > priorHits
                      && (hits < 3 && !campaign.getCampaignOptions().isUseAeroSystemHits())
                      && Compute.d6(2) < campaign.getCampaignOptions().getDestroyPartTarget()) {
                remove(false);
            } else if (hits >= 3) {
                remove(false);
            }
        }
    }

    @Override
    public int getBaseTime() {
        int time = 0;
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair times
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 120;
            } else {
                time = 75;
            }
            if (hits == 1) {
                time *= 1;
            }
            if (hits == 2) {
                time *= 2;
            }
            if (isSalvaging()) {
                if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                    time = 1200;
                } else {
                    time = 260;
                }
            }
            return time;
        }
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 1200;
            } else {
                time = 260;
            }
        } else {
            time = 120;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if (campaign.getCampaignOptions().isUseAeroSystemHits()) {
            // Test of proposed errata for repair time and difficulty
            if (isSalvaging()) {
                return -2;
            }
            if (hits == 1) {
                return -1;
            }
            if (hits == 2) {
                return 0;
            }
        }
        if (isSalvaging()) {
            return -2;
        }
        return -1;
    }


    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setSensorHits(hits);
        }

    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setSensorHits(0);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setSensorHits(3);
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
    public MissingPart getMissingPart() {
        return new MissingAeroSensor(getUnitTonnage(), largeCraft, campaign);
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
        if (largeCraft) {
            return Money.of(80000);
        }
        return Money.of(2000 * getUnitTonnage());
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof AeroSensor && largeCraft == ((AeroSensor) part).isForSpaceCraft()
                     && (largeCraft || getUnitTonnage() == part.getUnitTonnage());
    }

    public boolean isForSpaceCraft() {
        return largeCraft;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "dropship", largeCraft);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("dropship")) {
                largeCraft = wn2.getTextContent().trim().equalsIgnoreCase("true");
            }
        }
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        return super.getDetails(includeRepairDetails) + (largeCraft ? " (spacecraft)" : "");
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_VESSEL));
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
        return TECH_ADVANCEMENT;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }
}
