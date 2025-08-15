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

import megamek.common.Aero;
import megamek.common.ConvFighter;
import megamek.common.Dropship;
import megamek.common.Entity;
import megamek.common.SimpleTechLevel;
import megamek.common.SmallCraft;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class StructuralIntegrity extends Part {
    private static final MMLogger logger = MMLogger.create(StructuralIntegrity.class);

    // Slight variations for ASFs, CFs, and SC/DS
    static final TechAdvancement TA_ASF = new TechAdvancement(TechBase.ALL)
                                                .setAdvancement(2200, 2470, 2490)
                                                .setApproximate(true, false, false)
                                                .setPrototypeFactions(Faction.TA)
                                                .setProductionFactions(Faction.TH)
                                                .setTechRating(TechRating.C)
                                                .setAvailability(AvailabilityValue.C,
                                                      AvailabilityValue.D,
                                                      AvailabilityValue.D,
                                                      AvailabilityValue.C)
                                                .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_CF = new TechAdvancement(TechBase.ALL)
                                               .setAdvancement(DATE_PS, 2470, 2490)
                                               .setProductionFactions(Faction.TH)
                                               .setTechRating(TechRating.C)
                                               .setAvailability(AvailabilityValue.C,
                                                     AvailabilityValue.C,
                                                     AvailabilityValue.C,
                                                     AvailabilityValue.C)
                                               .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_DS = new TechAdvancement(TechBase.ALL)
                                               .setAdvancement(2200, 2470, 2490)
                                               .setApproximate(true, false, false)
                                               .setPrototypeFactions(Faction.TA)
                                               .setProductionFactions(Faction.TH)
                                               .setTechRating(TechRating.C)
                                               .setAvailability(AvailabilityValue.D,
                                                     AvailabilityValue.D,
                                                     AvailabilityValue.D,
                                                     AvailabilityValue.D)
                                               .setStaticTechLevel(SimpleTechLevel.STANDARD);

    private int pointsNeeded;

    public StructuralIntegrity() {
        this(0, null);
    }

    public StructuralIntegrity(int entityWeight, Campaign c) {
        super(entityWeight, c);
        pointsNeeded = 0;
        this.name = "Structural Integrity";
    }

    @Override
    public StructuralIntegrity clone() {
        StructuralIntegrity clone = new StructuralIntegrity(getUnitTonnage(), campaign);
        clone.copyBaseData(this);
        clone.pointsNeeded = this.pointsNeeded;
        return clone;
    }

    @Override
    public Money getStickerPrice() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (unit.getEntity() instanceof Dropship || unit.getEntity() instanceof SmallCraft) {
                return Money.of(((Aero) unit.getEntity()).getOSI() * 100000);
            } else if (unit.getEntity() instanceof ConvFighter) {
                return Money.of(((Aero) unit.getEntity()).getOSI() * 4000);
            } else {
                return Money.of(((Aero) unit.getEntity()).getOSI() * 50000);
            }
        }
        return Money.zero();
    }

    @Override
    public double getTonnage() {
        // not important I suppose
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof StructuralIntegrity) &&
                     (getUnitTonnage() == part.getUnitTonnage());
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("pointsNeeded")) {
                    pointsNeeded = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "pointsNeeded", pointsNeeded);
        writeToXMLEnd(pw, indent);
    }

    @Override
    public @Nullable String checkFixable() {
        return null;
    }

    @Override
    public String getDetails() {
        return getDetails(true);
    }

    @Override
    public String getDetails(boolean includeRepairDetails) {
        if (null != unit) {
            return pointsNeeded + " points destroyed";
        }
        return "SI not on unit? Wazz up with dat?";
    }

    @Override
    public void fix() {
        super.fix();
        pointsNeeded = 0;
        if (null != unit && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setSI(((Aero) unit.getEntity()).getOSI());
        }
    }

    @Override
    public @Nullable MissingPart getMissingPart() {
        // You can't replace this part, so return null
        return null;
    }

    @Override
    public String checkScrappable() {
        return "Structural Integrity cannot be scrapped";
    }

    @Override
    public boolean canNeverScrap() {
        return true;
    }

    @Override
    public boolean isSalvaging() {
        return false;
    }

    @Override
    public void remove(boolean salvage) {
        // You can't remove this part, so don't do anything
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if ((unit != null) && (unit.getEntity() instanceof Aero)) {
            pointsNeeded = ((Aero) unit.getEntity()).getOSI() - ((Aero) unit.getEntity()).getSI();
        }
    }

    @Override
    public int getBaseTime() {
        return 600 * pointsNeeded;
    }

    @Override
    public int getDifficulty() {
        return 1;
    }

    @Override
    public void updateConditionFromPart() {
        ((Aero) unit.getEntity()).setSI(((Aero) unit.getEntity()).getOSI() - pointsNeeded);
    }

    @Override
    public boolean needsFixing() {
        return pointsNeeded > 0;
    }

    @Override
    public void doMaintenanceDamage(int d) {
        int points = ((Aero) unit.getEntity()).getSI();
        points = Math.max(points - d, 1);
        ((Aero) unit.getEntity()).setSI(points);
        updateConditionFromEntity(false);
    }

    @Override
    public @Nullable String getLocationName() {
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
        if (null == getUnit()) {
            return TA_GENERIC;
        } else if (getUnit().getEntity().hasETypeFlag(Entity.ETYPE_SMALL_CRAFT)) {
            return TA_DS;
        } else if (getUnit().getEntity().hasETypeFlag(Entity.ETYPE_CONV_FIGHTER)) {
            return TA_CF;
        } else {
            return TA_ASF;
        }
    }
}
