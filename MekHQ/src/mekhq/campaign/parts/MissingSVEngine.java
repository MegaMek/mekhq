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
package mekhq.campaign.parts;

import java.io.PrintWriter;

import megamek.common.Aero;
import megamek.common.Engine;
import megamek.common.Entity;
import megamek.common.FuelType;
import megamek.common.Tank;
import megamek.common.TechAdvancement;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Placeholder for an engine that has been destroyed or removed from a support vehicle
 */
public class MissingSVEngine extends MissingPart {
    private double engineTonnage;
    private int etype;
    private TechRating techRating;
    private FuelType fuelType;

    private TechAdvancement techAdvancement;

    /**
     * Constructor used during campaign deserialization
     */

    public MissingSVEngine() {
        this(0, 0.0, Engine.COMBUSTION_ENGINE, TechRating.D, FuelType.PETROCHEMICALS, null);
    }

    /**
     * Creates a support vehicle engine part.
     *
     * @param unitTonnage   The mass of the unit it is installed on/intended for, in tons.
     * @param engineTonnage The mass of the engine
     * @param etype         An {@link Engine} type constant
     * @param techRating    The engine's tech rating, {@code TechRating.A} through {@code TechRating.F}
     * @param fuelType      Needed to distinguish different types of internal combustion engines.
     * @param campaign      The campaign instance
     */
    public MissingSVEngine(int unitTonnage, double engineTonnage, int etype, TechRating techRating,
          FuelType fuelType, Campaign campaign) {
        super(unitTonnage, campaign);
        this.engineTonnage = engineTonnage;
        this.etype = etype;
        this.techRating = techRating;
        this.fuelType = fuelType;

        Engine engine = new Engine(10, etype, Engine.SUPPORT_VEE_ENGINE);
        techAdvancement = engine.getTechAdvancement();
        name = String.format("%s (%s) Engine", engine.getEngineName(), techRating.getName());
    }

    /**
     * @return The weight of the engine
     */
    public double getEngineTonnage() {
        return engineTonnage;
    }

    /**
     * @return The {@link Engine} type flag
     */
    public int getEType() {
        return etype;
    }

    @Override
    public TechRating getTechRating() {
        return techRating;
    }

    /**
     * Internal combustion engines differ by the type of fuel they are designed for.
     *
     * @return The type of fuel used by the engine.
     */
    public FuelType getFuelType() {
        return fuelType;
    }

    @Override
    public int getBaseTime() {
        return 360;
    }

    @Override
    public int getDifficulty() {
        return -1;
    }

    @Override
    public double getTonnage() {
        return engineTonnage;
    }

    private static final String NODE_ENGINE_TONNAGE = "engineTonnage";
    private static final String NODE_ETYPE = "etype";
    private static final String NODE_TECH_RATING = "techRating";
    private static final String NODE_FUEL_TYPE = "fuelType";

    @Override
    public void writeToXML(final PrintWriter pw, int indent) {
        indent = writeToXMLBegin(pw, indent);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, NODE_ENGINE_TONNAGE, engineTonnage);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, NODE_ETYPE, etype);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, NODE_TECH_RATING, techRating.getName());
        if (etype == Engine.COMBUSTION_ENGINE) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, NODE_FUEL_TYPE, fuelType.name());
        }
        writeToXMLEnd(pw, indent);
    }

    @Override
    public void loadFieldsFromXmlNode(Node node) {
        NodeList nl = node.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn = nl.item(x);
            switch (wn.getNodeName()) {
                case NODE_ENGINE_TONNAGE:
                    engineTonnage = Double.parseDouble(wn.getTextContent());
                    break;
                case NODE_ETYPE:
                    etype = Integer.parseInt(wn.getTextContent());
                    break;
                case NODE_TECH_RATING:
                    techRating = TechRating.fromName(wn.getTextContent());
                    break;
                case NODE_FUEL_TYPE:
                    fuelType = FuelType.valueOf(wn.getTextContent());
                    break;
            }
        }
    }

    @Override
    public boolean isAcceptableReplacement(Part other, boolean refit) {
        return other instanceof SVEnginePart
                     && (engineTonnage == ((SVEnginePart) other).getEngineTonnage())
                     && (etype == ((SVEnginePart) other).getEType())
                     && (techRating == other.getTechRating())
                     && ((etype != Engine.COMBUSTION_ENGINE) || (fuelType == ((SVEnginePart) other).getFuelType()));
    }

    @Override
    public @Nullable String checkFixable() {
        // If the engine location is destroyed, the unit is destroyed
        return null;
    }

    @Override
    public Part getNewPart() {
        return new SVEnginePart(getUnitTonnage(), engineTonnage, etype, techRating,
              fuelType, getCampaign());
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (unit.getEntity() instanceof Tank) {
                ((Tank) unit.getEntity()).engineHit();
            } else if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(((Aero) unit.getEntity()).getMaxEngineHits());
            }
        }
    }

    @Override
    public String getAcquisitionName() {
        return getPartName() + ",  " + getTonnage() + " tons";
    }

    @Override
    public String getLocationName() {
        return null;
    }

    @Override
    public int getLocation() {
        return Entity.LOC_NONE;
    }

    @Override
    public TechAdvancement getTechAdvancement() {
        return techAdvancement;
    }

    @Override
    public boolean isInLocation(String loc) {
        return false;
    }

    @Override
    public PartRepairType getMRMSOptionType() {
        return PartRepairType.ENGINE;
    }

}
