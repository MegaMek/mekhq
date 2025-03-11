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

import megamek.common.*;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class AeroHeatSink extends Part {
    private int type;

    static final TechAdvancement TA_SINGLE = EquipmentType.get("Heat Sink").getTechAdvancement();
    static final TechAdvancement TA_IS_DOUBLE = EquipmentType.get("ISDoubleHeatSink").getTechAdvancement();
    static final TechAdvancement TA_CLAN_DOUBLE = EquipmentType.get("CLDoubleHeatSink").getTechAdvancement();

    // To differentiate Clan double heatsinks, which aren't defined in Aero
    public static final int CLAN_HEAT_DOUBLE = 2;

    public AeroHeatSink() {
        this(0, Aero.HEAT_SINGLE, false, null);
    }

    public AeroHeatSink(int tonnage, int type, boolean omniPodded, Campaign c) {
        super(tonnage, omniPodded, c);
        this.name = "Aero Heat Sink";
        this.type = type;
        if (type == CLAN_HEAT_DOUBLE) {
            this.name = "Aero Double Heat Sink (Clan)";
        }
        if (type == Aero.HEAT_DOUBLE) {
            this.name = "Aero Double Heat Sink";
        }
    }

    @Override
    public AeroHeatSink clone() {
        AeroHeatSink clone = new AeroHeatSink(getUnitTonnage(), type, omniPodded, campaign);
        clone.copyBaseData(this);
        return clone;
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        int priorHits = hits;
        if (null != unit && unit.getEntity() instanceof Aero && hits == 0) {
            // ok this is really ugly, but we don't track individual heat sinks, so I have no idea of
            // a better way to do it
            int hsDamage = ((Aero) unit.getEntity()).getHeatSinkHits();
            for (Part part : unit.getParts()) {
                if (hsDamage == 0) {
                    break;
                } else if ((part instanceof AeroHeatSink && part.getHits() > 0) || part instanceof MissingAeroHeatSink) {
                    hsDamage--;
                }
            }

            if (hsDamage > 0) {
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
        if (isOmniPodded()) {
            return 10;
        }
        //New SO errata 6-2019
        return 20;
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -2;
        }
        return -1;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (hits == 0) {
                Aero a = (Aero) unit.getEntity();
                a.setHeatSinks(Math.min(a.getHeatSinks()+1, a.getOHeatSinks()));
            }
        }
    }

    @Override
    public void fix() {
        boolean fixed = needsFixing();
        super.fix();
        if (fixed && (null != unit)
                && unit.getEntity() instanceof Aero) {
            ((Aero) unit.getEntity()).setHeatSinks(((Aero) unit.getEntity()).getHeatSinks() + 1);
        }
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit && unit.getEntity() instanceof Aero) {
            if (hits == 0) {
                ((Aero) unit.getEntity()).setHeatSinks(((Aero) unit.getEntity()).getHeatSinks() - 1);
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
        return new MissingAeroHeatSink(getUnitTonnage(), type, omniPodded, campaign);
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
        if (type == Aero.HEAT_DOUBLE) {
            return Money.of(isOmniPodded()? 7500 : 6000);
        } else {
            return Money.of(isOmniPodded()? 2500 : 2000);
        }
    }

    @Override
    public double getTonnage() {
        return 1;
    }

    @Override
    public int getTechRating() {
        if (type == CLAN_HEAT_DOUBLE) {
            return EquipmentType.RATING_F;
        } else if (type == Aero.HEAT_DOUBLE) {
            return EquipmentType.RATING_E;
        } else {
            return EquipmentType.RATING_C;
        }
    }

    @Override
    public int getTechLevel() {
        if (type == CLAN_HEAT_DOUBLE) {
            return TechConstants.T_ALL_CLAN;
        }
        return TechConstants.T_ALLOWED_ALL;
    }

    @Override
    public boolean isSamePartType(Part part) {
        return part instanceof AeroHeatSink && type == ((AeroHeatSink) part).getType()
                && isOmniPodded() == part.isOmniPodded();
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

            if (wn2.getNodeName().equalsIgnoreCase("type")) {
                type = Integer.parseInt(wn2.getTextContent());
                if (type == CLAN_HEAT_DOUBLE) {
                    this.name = "Clan Aero Double Heat Sink";
                }
                if (type == Aero.HEAT_DOUBLE) {
                    this.name = "Aero Double Heat Sink";
                }
            }
        }
    }

    @Override
    public boolean isRightTechType(String skillType) {
        return skillType.equals(SkillType.S_TECH_AERO);
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
        if (type == Aero.HEAT_SINGLE) {
            return TA_SINGLE;
        } else if (type == CLAN_HEAT_DOUBLE) {
            return TA_CLAN_DOUBLE;
        } else {
            return TA_IS_DOUBLE;
        }
    }

    @Override
    public boolean isOmniPoddable() {
        return true;
    }
}
