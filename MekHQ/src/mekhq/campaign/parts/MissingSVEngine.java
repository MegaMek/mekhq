/*
 * Copyright (c) 2019 - The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.parts;

import megamek.common.*;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartRepairType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Place holder for an engine that has been destroyed or removed from a support vehicle
 */
public class MissingSVEngine extends MissingPart {
    private static final long serialVersionUID = -1388693773192376347L;

    private double engineTonnage;
    private int etype;
    private int techRating;
    private FuelType fuelType;

    private TechAdvancement techAdvancement;

    /**
     * Constructor used during campaign deserialization
     */
    @SuppressWarnings("unused")
    public MissingSVEngine() {
        this(0, 0.0, Engine.COMBUSTION_ENGINE, RATING_D, FuelType.PETROCHEMICALS, null);
    }

    /**
     * Creates a support vehicle engine part.
     *
     * @param unitTonnage      The mass of the unit it is installed on/intended for, in tons.
     * @param engineTonnage    The mass of the engine
     * @param etype            An {@link Engine} type constant
     * @param techRating       The engine's tech rating, {@code RATING_A} through {@code RATING_F}
     * @param fuelType         Needed to distinguish different types of internal combustion engines.
     * @param campaign         The campaign instance
     */
    public MissingSVEngine(int unitTonnage, double engineTonnage, int etype, int techRating,
                           FuelType fuelType, Campaign campaign) {
        super(unitTonnage, campaign);
        this.engineTonnage = engineTonnage;
        this.etype = etype;
        this.techRating = techRating;
        this.fuelType = fuelType;

        Engine engine = new Engine(10, etype, Engine.SUPPORT_VEE_ENGINE);
        techAdvancement = engine.getTechAdvancement();
        name = String.format("%s (%s) Engine", engine.getEngineName(), ITechnology.getRatingName(techRating));
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
    public int getTechRating() {
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
    public void writeToXml(PrintWriter pw, int indent) {
        writeToXmlBegin(pw, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_ENGINE_TONNAGE, engineTonnage);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_ETYPE, etype);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_TECH_RATING, ITechnology.getRatingName(techRating));
        if (etype == Engine.COMBUSTION_ENGINE) {
            MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_FUEL_TYPE, fuelType.name());
        }
        writeToXmlEnd(pw, indent);
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
                    for (int i = 0; i < ratingNames.length; i++) {
                        if (ratingNames[i].equals(wn.getTextContent())) {
                            techRating = i;
                            break;
                        }
                    }
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
    public String checkFixable() {
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
        if(null != unit) {
            if(unit.getEntity() instanceof Tank) {
                ((Tank) unit.getEntity()).engineHit();
            } else if(unit.getEntity() instanceof Aero) {
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
    public PartRepairType getMassRepairOptionType() {
        return PartRepairType.ENGINE;
    }

}
