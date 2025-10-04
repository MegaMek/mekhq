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

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Aero;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.campaign.parts.missing.MissingAeroLifeSupport;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.skills.enums.SkillTypeNew;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class AeroLifeSupport extends Part {
    private Money cost;
    private boolean fighter;

    public static final TechAdvancement TECH_ADVANCEMENT = new TechAdvancement(TechBase.ALL)
                                                                 .setAdvancement(DATE_ES, DATE_ES, DATE_ES)
                                                                 .setTechRating(TechRating.C)
                                                                 .setAvailability(AvailabilityValue.C,
                                                                       AvailabilityValue.C,
                                                                       AvailabilityValue.C,
                                                                       AvailabilityValue.C)
                                                                 .setStaticTechLevel(SimpleTechLevel.STANDARD);

    public AeroLifeSupport() {
        this(0, Money.zero(), false, null);
    }

    public AeroLifeSupport(int tonnage, Money cost, boolean f, Campaign c) {
        super(tonnage, c);
        this.cost = cost;
        this.name = "Fighter Life Support";
        this.fighter = f;
        if (!fighter) {
            this.name = "Spacecraft Life Support";
        }
    }

    @Override
    public AeroLifeSupport clone() {
        AeroLifeSupport clone = new AeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (((Aero) unit.getEntity()).hasLifeSupport()) {
                hits = 0;
            } else {
                hits = 1;
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
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                if (isSalvaging()) {
                    time = 1200;
                } else {
                    time = 120;
                }
            } else {
                if (isSalvaging()) {
                    time = 180;
                } else {
                    time = 60;
                }
            }
            return time;
        }
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                time = 6720;
            } else {
                time = 180;
            }
        } else {
            time = 120;
        }
        return time;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            if (null != unit && (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof Jumpship)) {
                return 0;
            } else {
                return -1;
            }
        }
        return 1;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setLifeSupport(hits <= 0);
        }

    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setLifeSupport(true);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setLifeSupport(false);
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
        return new MissingAeroLifeSupport(getUnitTonnage(), cost, fighter, campaign);
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
        return cost;
    }

    public void calculateCost() {
        if (fighter) {
            cost = Money.of(50000);
        }
        if (null != unit) {
            cost = Money.of(5000.0 * (unit.getEntity().getNCrew() + unit.getEntity().getNPassenger()));
        }
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    public boolean isForFighter() {
        return fighter;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof AeroLifeSupport && fighter == ((AeroLifeSupport) part).isForFighter()
                     && (getStickerPrice().equals(part.getStickerPrice()));
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "fighter", fighter);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "cost", cost);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            if (wn2.getNodeName().equalsIgnoreCase("fighter")) {
                fighter = wn2.getTextContent().trim().equalsIgnoreCase("true");
            } else if (wn2.getNodeName().equalsIgnoreCase("cost")) {
                cost = Money.fromXmlString(wn2.getTextContent().trim());
            }
        }
    }

    @Override
    public boolean isRightTechType(String skillType) {
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
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ELECTRONICS;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return TECH_ADVANCEMENT;
    }
}
