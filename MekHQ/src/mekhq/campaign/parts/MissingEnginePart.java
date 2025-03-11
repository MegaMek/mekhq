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

import megamek.common.Aero;
import megamek.common.CriticalSlot;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.EntityMovementMode;
import megamek.common.Mek;
import megamek.common.ProtoMek;
import megamek.common.Tank;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import megamek.common.verifier.TestEntity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class MissingEnginePart extends MissingPart {
    private static final MMLogger logger = MMLogger.create(MissingEnginePart.class);

    protected Engine engine;
    protected boolean forHover;

    public MissingEnginePart() {
        this(0, null, null, false);
    }

    public MissingEnginePart(int tonnage, Engine e, Campaign c, boolean hover) {
        super(tonnage, c);
        this.engine = e;
        this.forHover = hover;
        if (null != engine) {
            this.name = engine.getEngineName() + " Engine";
        }
        this.engine = e;
    }

    @Override
    public int getBaseTime() {
        return 360;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    public Engine getEngine() {
        return engine;
    }

    @Override
    public double getTonnage() {
        double weight = Engine.ENGINE_RATINGS[(int) Math.ceil(engine.getRating() / 5.0)];
        switch (engine.getEngineType()) {
            case Engine.COMBUSTION_ENGINE:
                weight *= 2.0f;
                break;
            case Engine.NORMAL_ENGINE:
                break;
            case Engine.XL_ENGINE:
                weight *= 0.5f;
                break;
            case Engine.LIGHT_ENGINE:
                weight *= 0.75f;
                break;
            case Engine.XXL_ENGINE:
                weight /= 3f;
                break;
            case Engine.COMPACT_ENGINE:
                weight *= 1.5f;
                break;
            case Engine.FISSION:
                weight *= 1.75;
                weight = Math.max(5, weight);
                break;
            case Engine.FUEL_CELL:
                weight *= 1.2;
                break;
            case Engine.NONE:
                return 0;
        }
        weight = TestEntity.ceilMaxHalf(weight, TestEntity.Ceil.HALFTON);
        if (engine.hasFlag(Engine.TANK_ENGINE) && engine.isFusion()) {
            weight *= 1.5f;
        }
        double toReturn = TestEntity.ceilMaxHalf(weight, TestEntity.Ceil.HALFTON);
        if (forHover) {
            return Math.max(TestEntity.ceilMaxHalf(getUnitTonnage() / 5.0, TestEntity.Ceil.HALFTON), toReturn);
        }
        return toReturn;
    }

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineType", engine.getEngineType());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineRating", engine.getRating());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "engineFlags", engine.getFlags());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forHover", forHover);
        writeToXMLEnd(pw, indent);
    }

    @Override
    protected void loadFieldsFromXmlNode(Node wn) {
        NodeList nl = wn.getChildNodes();
        int engineType = -1;
        int engineRating = -1;
        int engineFlags = 0;

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);

            try {
                if (wn2.getNodeName().equalsIgnoreCase("engineType")) {
                    engineType = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("engineRating")) {
                    engineRating = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("engineFlags")) {
                    engineFlags = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        engine = new Engine(engineRating, engineType, engineFlags);
        this.name = engine.getEngineName() + " Engine";
    }

    @Override
    public boolean isAcceptableReplacement(Part part, boolean refit) {
        int year = campaign.getGameYear();
        if (part instanceof EnginePart) {
            Engine eng = ((EnginePart) part).getEngine();
            if (null != eng) {
                return (getEngine().getEngineType() == eng.getEngineType())
                        && (getEngine().getRating() == eng.getRating())
                        && (getEngine().getTechType(year) == eng.getTechType(year))
                        && (getUnitTonnage() == part.getUnitTonnage())
                        && (getTonnage() == part.getTonnage());
            }
        }
        return false;
    }

    public void fixTankFlag(boolean hover) {
        int flags = engine.getFlags();
        if (!engine.hasFlag(Engine.TANK_ENGINE)) {
            flags |= Engine.TANK_ENGINE;
        }
        engine = new Engine(engine.getRating(), engine.getEngineType(), flags);
        this.name = engine.getEngineName() + " Engine";
        this.forHover = hover;
    }

    public void fixClanFlag() {
        int flags = engine.getFlags();
        if (!engine.hasFlag(Engine.CLAN_ENGINE)) {
            flags |= Engine.CLAN_ENGINE;
        }
        engine = new Engine(engine.getRating(), engine.getEngineType(), flags);
        this.name = engine.getEngineName() + " Engine";
    }

    @Override
    public @Nullable String checkFixable() {
        if (null == unit) {
            return null;
        }
        for (int i = 0; i < unit.getEntity().locations(); i++) {
            if (unit.getEntity().getNumberOfCriticals(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE, i) > 0
                    && unit.isLocationDestroyed(i)) {
                return unit.getEntity().getLocationName(i) + " is destroyed.";
            }
        }
        return null;
    }

    @Override
    public Part getNewPart() {
        boolean useHover = null != unit && unit.getEntity().getMovementMode() == EntityMovementMode.HOVER
                && unit.getEntity() instanceof Tank;
        return new EnginePart(getUnitTonnage(),
                new Engine(engine.getRating(), engine.getEngineType(), engine.getFlags()), campaign, useHover);
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (unit.getEntity() instanceof Mek) {
                unit.destroySystem(CriticalSlot.TYPE_SYSTEM, Mek.SYSTEM_ENGINE);
            } else if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(((Aero) unit.getEntity()).getMaxEngineHits());
            } else if (unit.getEntity() instanceof Tank) {
                ((Tank) unit.getEntity()).engineHit();
            } else if (unit.getEntity() instanceof ProtoMek) {
                ((ProtoMek) unit.getEntity()).setEngineHit(true);
            }
        }
    }

    @Override
    public String getAcquisitionName() {
        return getPartName() + ",  " + getTonnage() + " tons";
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
        return engine.getTechAdvancement();
    }

    @Override
    public boolean isInLocation(String loc) {
        if (null == unit || null == unit.getEntity()) {
            return false;
        }
        if (unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_CT) {
            return true;
        }
        boolean needsSideTorso = false;
        switch (getEngine().getEngineType()) {
            case Engine.XL_ENGINE:
            case Engine.LIGHT_ENGINE:
            case Engine.XXL_ENGINE:
                needsSideTorso = true;
                break;
            default:
                break;
        }
        return needsSideTorso
                && ((unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_LT)
                        || (unit.getEntity().getLocationFromAbbr(loc) == Mek.LOC_RT));
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ENGINE;
    }
}
