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

import megamek.common.compute.Compute;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class DropshipDockingCollar extends Part {
    private static final MMLogger logger = MMLogger.create(DropshipDockingCollar.class);

    static final TechAdvancement TA_BOOM = new TechAdvancement(TechBase.ALL)
                                                 .setAdvancement(2458, 2470, 2500)
                                                 .setPrototypeFactions(Faction.TH)
                                                 .setProductionFactions(Faction.TH)
                                                 .setTechRating(TechRating.C)
                                                 .setAvailability(AvailabilityValue.C,
                                                       AvailabilityValue.C,
                                                       AvailabilityValue.C,
                                                       AvailabilityValue.C)
                                                 .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_NO_BOOM = new TechAdvancement(TechBase.ALL)
                                                    .setAdvancement(2304, 2350, 2364, 2520)
                                                    .setPrototypeFactions(Faction.TA)
                                                    .setProductionFactions(Faction.TH)
                                                    .setTechRating(TechRating.B)
                                                    .setAvailability(AvailabilityValue.C,
                                                          AvailabilityValue.X,
                                                          AvailabilityValue.X,
                                                          AvailabilityValue.X)
                                                    .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private int collarType;

    public DropshipDockingCollar() {
        this(0, null, Dropship.COLLAR_STANDARD);
    }

    public DropshipDockingCollar(int tonnage, Campaign c, int collarType) {
        super(tonnage, c);
        this.collarType = collarType;
        this.name = "DropShip Docking Collar";
        if (collarType == Dropship.COLLAR_NO_BOOM) {
            name += " (No Boom)";
        } else if (collarType == Dropship.COLLAR_PROTOTYPE) {
            name += " (Prototype)";
        }
    }

    @Override
    public DropshipDockingCollar clone() {
        DropshipDockingCollar clone = new DropshipDockingCollar(getUnitTonnage(), campaign, collarType);
        clone.copyBaseData(this);
        return clone;
    }

    public int getCollarType() {
        return collarType;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Dropship) {
            if (((Dropship) unit.getEntity()).isDockCollarDamaged()) {
                hits = 1;
            } else {
                hits = 0;
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
        if (isSalvaging()) {
            return 2880;
        }
        return 120;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -2;
        }
        return 3;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageDockCollar(hits > 0);
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageDockCollar(false);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageDockCollar(true);
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
        return new MissingDropshipDockingCollar(getUnitTonnage(), campaign, collarType);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public boolean needsFixing() {
        return (hits > 0);
    }

    @Override
    public Money getStickerPrice() {
        if (collarType == Dropship.COLLAR_STANDARD) {
            return Money.of(10000);
        } else {
            return Money.of(1010000);
        }
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof DropshipDockingCollar)
                     && (collarType == ((DropshipDockingCollar) part).collarType);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collarType", collarType);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("collarType")) {
                    collarType = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_VESSEL);
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
        if (collarType != Dropship.COLLAR_NO_BOOM) {
            return TA_BOOM;
        } else {
            return TA_NO_BOOM;
        }
    }
}
