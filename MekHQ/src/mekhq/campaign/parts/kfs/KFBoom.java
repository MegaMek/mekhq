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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.parts.kfs;

import java.io.PrintWriter;

import megamek.common.SimpleTechLevel;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import megamek.common.enums.AvailabilityValue;
import megamek.common.enums.Faction;
import megamek.common.enums.TechBase;
import megamek.common.enums.TechRating;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.missing.MissingKFBoom;
import mekhq.campaign.parts.missing.MissingPart;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author MKerensky
 */
public class KFBoom extends Part {
    private static final MMLogger LOGGER = MMLogger.create(KFBoom.class);

    static final TechAdvancement TA_KF_BOOM = new TechAdvancement(TechBase.ALL)
                                                    .setAdvancement(2458, 2470, 2500).setPrototypeFactions(Faction.TH)
                                                    .setProductionFactions(Faction.TH).setTechRating(TechRating.C)
                                                    .setAvailability(AvailabilityValue.D,
                                                          AvailabilityValue.C,
                                                          AvailabilityValue.C,
                                                          AvailabilityValue.C)
                                                    .setStaticTechLevel(SimpleTechLevel.STANDARD);
    static final TechAdvancement TA_PROTOTYPE_KF_BOOM = new TechAdvancement(TechBase.ALL)
                                                              .setAdvancement(2458, 2470, 2500)
                                                              .setPrototypeFactions(Faction.TH)
                                                              .setProductionFactions(Faction.TH)
                                                              .setTechRating(TechRating.C)
                                                              .setAvailability(AvailabilityValue.F,
                                                                    AvailabilityValue.X,
                                                                    AvailabilityValue.X,
                                                                    AvailabilityValue.X)
                                                              .setStaticTechLevel(SimpleTechLevel.ADVANCED);

    private int boomType;

    public KFBoom() {
        this(0, null, Dropship.BOOM_STANDARD);
    }

    public KFBoom(int tonnage, Campaign c, int boomType) {
        super(tonnage, c);
        this.boomType = boomType;
        this.name = "DropShip K-F Boom";
        if (boomType == Dropship.BOOM_PROTOTYPE) {
            name += " (Prototype)";
        }
    }

    @Override
    public KFBoom clone() {
        KFBoom clone = new KFBoom(getUnitTonnage(), campaign, boomType);
        clone.copyBaseData(this);
        return clone;
    }

    public int getBoomType() {
        return boomType;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Dropship) {
            if (((Dropship) unit.getEntity()).isKFBoomDamaged()) {
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
            return 3600;
        }
        return 360;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return 0;
        }
        return -1;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageKFBoom(hits > 0);
        }
    }

    @Override
    public void fix() {
        super.fix();
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageKFBoom(false);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Dropship) {
            ((Dropship) unit.getEntity()).setDamageKFBoom(true);
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
        return new MissingKFBoom(getUnitTonnage(), campaign, boomType);
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
        if (boomType == Dropship.BOOM_STANDARD) {
            return Money.of(10000);
        } else if (boomType == Dropship.BOOM_PROTOTYPE) {
            return Money.of(1010000);
        } else {
            return Money.zero();
        }
    }

    @Override
    public double getTonnage() {
        return 0;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return (part instanceof KFBoom)
                     && (boomType == ((KFBoom) part).boomType);
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "boomType", boomType);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("boomType")) {
                    boomType = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception ex) {
                LOGGER.error("", ex);
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
        if (boomType != Dropship.BOOM_STANDARD) {
            return TA_PROTOTYPE_KF_BOOM;
        } else {
            return TA_KF_BOOM;
        }
    }
}
