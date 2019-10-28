/*
 * MekBuilder - unit design companion of MegaMek
 * Copyright (C) 2017 The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package mekhq.campaign.parts;

import megamek.common.*;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.GregorianCalendar;

/**
 * Place holder for an engine that has been destroyed or removed from a support vehicle
 */
public class MissingSVEngine extends MissingPart {

    // Part stores unit tonnage as an int. Support vees can come in fractions of a ton.
    private double actualTonnage;
    private int etype;
    private int techRating;
    private int movementFactor;
    private double baseEngineFactor;
    private FuelType fuelType;

    private TechAdvancement techAdvancement;

    public MissingSVEngine() {
        this(0.0, Engine.COMBUSTION_ENGINE, RATING_D, 1, 0.0, FuelType.PETROCHEMICALS, null);
    }

    public MissingSVEngine(double unitTonnage, int etype, int techRating, int movementFactor,
                           double baseEngineFactor, FuelType fuelType, Campaign campaign) {
        super((int) unitTonnage, campaign);
        this.actualTonnage = unitTonnage;
        this.etype = etype;
        this.techRating = techRating;
        this.movementFactor = movementFactor;
        this.baseEngineFactor = baseEngineFactor;
        this.fuelType = fuelType;

        techAdvancement = new Engine(10, etype, Engine.SUPPORT_VEE_ENGINE).getTechAdvancement();
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
        if (null != getUnit()) {
            return RoundWeight.standard(actualTonnage * baseEngineFactor * movementFactor
                    * Engine.getSVEngineFactor(etype, techRating), getUnit().getEntity());
        } else {
            return RoundWeight.nextKg(actualTonnage * baseEngineFactor * movementFactor
                    * Engine.getSVEngineFactor(etype, techRating));
        }
    }

    private static final String NODE_UNIT_TONNAGE = "actualUnitTonnage";
    private static final String NODE_ETYPE = "etype";
    private static final String NODE_TECH_RATING = "techRating";
    private static final String NODE_MOVEMENT_FACTOR = "movementFactor";
    private static final String NODE_BASE_ENGINE_FACTOR = "baseEngineFactor";
    private static final String NODE_FUEL_TYPE = "fuelType";
    @Override
    public void writeToXml(PrintWriter pw, int indent) {
        writeToXmlBegin(pw, indent);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_UNIT_TONNAGE, actualTonnage);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_ETYPE, etype);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_TECH_RATING, ITechnology.getRatingName(techRating));
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_MOVEMENT_FACTOR, movementFactor);
        MekHqXmlUtil.writeSimpleXmlTag(pw, indent + 1, NODE_BASE_ENGINE_FACTOR, baseEngineFactor);
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
                case NODE_UNIT_TONNAGE:
                    actualTonnage = Double.parseDouble(wn.getTextContent());
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
                case NODE_MOVEMENT_FACTOR:
                    movementFactor = Integer.parseInt(wn.getTextContent());
                    break;
                case NODE_BASE_ENGINE_FACTOR:
                    baseEngineFactor = Double.parseDouble(wn.getTextContent());
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
                && (etype == ((SVEnginePart) other).getEType())
                && (techRating == ((SVEnginePart) other).getTechRating())
                && (movementFactor == ((SVEnginePart) other).getMovementFactor())
                && (baseEngineFactor == ((SVEnginePart) other).getBaseEngineFactor())
                && ((etype != Engine.COMBUSTION_ENGINE) || (fuelType == ((SVEnginePart) other).getFuelType()));
    }

    @Override
    public String checkFixable() {
        // If the engine location is destroyed, the unit is destroyed
        return null;
    }

    @Override
    public Part getNewPart() {
        return new SVEnginePart(actualTonnage, etype, techRating, movementFactor,
                baseEngineFactor, fuelType, getCampaign());
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
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.ENGINE;
    }

}
