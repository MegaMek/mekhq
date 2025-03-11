/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
import megamek.common.DockingCollar;
import megamek.common.Entity;
import megamek.common.Jumpship;
import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.SkillType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author MKerensky
 */
public class JumpshipDockingCollar extends Part {
    private static final MMLogger logger = MMLogger.create(JumpshipDockingCollar.class);

    static final TechAdvancement TA_BOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2458, 2470, 2500)
            .setPrototypeFactions(F_TH)
            .setProductionFactions(F_TH)
            .setTechRating(RATING_C)
            .setAvailability(RATING_C, RATING_C, RATING_C, RATING_C)
            .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_NO_BOOM = new TechAdvancement(TECH_BASE_ALL)
            .setAdvancement(2304, 2350, 2364, 2520)
            .setPrototypeFactions(F_TA)
            .setProductionFactions(F_TH)
            .setTechRating(RATING_B)
            .setAvailability(RATING_C, RATING_X, RATING_X, RATING_X)
            .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private int collarType;
    private int collarNumber;

    public JumpshipDockingCollar() {
        this(0, 0, null, Jumpship.COLLAR_STANDARD);
    }

    public JumpshipDockingCollar(int tonnage, int collarNumber, Campaign c, int collarType) {
        super(tonnage, c);
        this.collarNumber = collarNumber;
        this.collarType = collarType;
        this.name = "JumpShip Docking Collar";
        if (collarType == Jumpship.COLLAR_NO_BOOM) {
            name += " (Pre Boom)";
        }
    }

    public int getCollarNumber() {
        return collarNumber;
    }

    @Override
    public JumpshipDockingCollar clone() {
        JumpshipDockingCollar clone = new JumpshipDockingCollar(0, collarNumber, campaign, collarType);
        clone.copyBaseData(this);
        return clone;
    }

    public int getCollarType() {
        return collarType;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            DockingCollar collar = unit.getEntity().getCollarById(collarNumber);
            if (collar != null && collar.isDamaged()) {
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
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            DockingCollar collar = unit.getEntity().getCollarById(collarNumber);
            if (collar != null) {
                collar.setDamaged(hits > 0);
            }
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Jumpship) {
            DockingCollar collar = unit.getEntity().getCollarById(collarNumber);
            if (collar != null) {
                collar.setDamaged(false);
            }
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (unit.getEntity() instanceof Jumpship) {
            DockingCollar collar = unit.getEntity().getCollarById(collarNumber);
            if (collar != null) {
                collar.setDamaged(true);
            }
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
        return new MissingJumpshipDockingCollar(0, collarNumber, campaign, collarType);
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
        if (collarType == Jumpship.COLLAR_STANDARD) {
            return Money.of(100000);
        } else {
            return Money.of(500000);
        }
    }

    @Override
    public double getTonnage() {
        return 1000;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof JumpshipDockingCollar)
                && (collarType == ((JumpshipDockingCollar) part).collarType);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collarType", collarType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "collarNumber", collarNumber);
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
                } else if (wn2.getNodeName().equalsIgnoreCase("collarNumber")) {
                    collarNumber = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
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
        if (collarType != Jumpship.COLLAR_NO_BOOM) {
            return TA_BOOM;
        } else {
            return TA_NO_BOOM;
        }
    }
}
