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
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.SkillType;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;

/**
 * Engine for a support vehicle. An identical support vehicle engine will have the same engine type,
 * unit tonnage, tech rating, and movement factor. The movement factor is the vehicle's (cruise/safe thrust)^2 + 4.
 * ICEs will also have the same fuel type.
 */
public class SVEnginePart extends Part {

    // Part stores unit tonnage as an int. Support vees can come in fractions of a ton.
    private double actualTonnage;
    private int etype;
    private int techRating;
    private int movementFactor;
    private double baseEngineFactor;
    private FuelType fuelType;

    private TechAdvancement techAdvancement;

    /**
     * Constructor used during campaign deserialization
     */
    @SuppressWarnings("unused")
    private SVEnginePart() {
        this(0.0, Engine.COMBUSTION_ENGINE, RATING_D, 1, 0.0, FuelType.PETROCHEMICALS, null);
    }

    /**
     * Creates a support vehicle engine part.
     *
     * @param unitTonnage      The mass of the unit it is installed on/intended for, in tons.
     * @param etype            An {@link Engine} type constant
     * @param techRating       The engine's tech rating, {@code RATING_A} through {@code RATING_F}
     * @param movementFactor   The movement factor of the unit this engine is installed on/intended for. The movement
     *                         factor is the square of the cruise/safe thrust squared, plus four. Rail support
     *                         vehicles subtract two from the cruise speed for use in this calculation. The rules
     *                         don't say what to do with a theoretical mp of 1, but presumably it would not be
     *                         larger than a unit with mp 2, despite the formula.
     * @param baseEngineFactor A factor based on the vehicle size class and motive type. See {@link megamek.common.Entity#getBaseEngineValue()}
     * @param fuelType         Needed to distinguish different types of internal combustion engines.
     * @param campaign         The campaign instance
     */
    public SVEnginePart(double unitTonnage, int etype, int techRating, int movementFactor, double baseEngineFactor,
                        FuelType fuelType, Campaign campaign) {
        super((int) unitTonnage, campaign);
        this.actualTonnage = unitTonnage;
        this.etype = etype;
        this.techRating = techRating;
        this.movementFactor = movementFactor;
        this.baseEngineFactor = baseEngineFactor;
        this.fuelType = fuelType;

        Engine engine = new Engine(10, etype, Engine.SUPPORT_VEE_ENGINE);
        techAdvancement = engine.getTechAdvancement();
        name = String.format("%s (%s) Engine", engine.getEngineName(), ITechnology.getRatingName(techRating));
    }

    /**
     * {@link Part#getUnitTonnage()} returns an {@code int}. Support vehicle weights don't necessarily
     * come in full tons.
     *
     * @return The weight of the support vehicle the engine belongs in.
     */
    public double getActualUnitTonnage() {
        return actualTonnage;
    }

    /**
     * @return The {@link Engine} type flag
     */
    public int getEType() {
        return etype;
    }

    /**
     * @return The engine tech rating (A-F)
     */
    public int getTechRating() {
        return techRating;
    }

    /**
     * @return The unit's movement factor, used to calculate engine weight.
     */
    public int getMovementFactor() {
        return movementFactor;
    }

    /**
     * The base engine factor is determined by motive type and size class.
     *
     * @return The base engine factor, used to calculate engine weight.
     * @see Entity#getBaseEngineValue()
     */
    public double getBaseEngineFactor() {
        return baseEngineFactor;
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
    public SVEnginePart clone() {
        SVEnginePart engine = new SVEnginePart(actualTonnage, etype, techRating, movementFactor,
                baseEngineFactor, fuelType, getCampaign());
        engine.copyBaseData(this);
        return engine;
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

    @Override
    public Money getStickerPrice() {
        return Money.of(5000 * getTonnage() * Engine.getSVCostMultiplier(etype));
    }

    @Override
    public boolean isSamePartType(Part other) {
        return other instanceof SVEnginePart
                && (etype == ((SVEnginePart) other).etype)
                && (techRating == ((SVEnginePart) other).techRating)
                && (movementFactor == ((SVEnginePart) other).movementFactor)
                && (baseEngineFactor == ((SVEnginePart) other).baseEngineFactor)
                && ((etype != Engine.COMBUSTION_ENGINE) || (fuelType == ((SVEnginePart) other).fuelType));
    }

    @Override
    public int getTechLevel() {
        return TechConstants.T_IS_TW_NON_BOX;
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
    public void fix() {
        super.fix();
        if (null != unit) {
            if (unit.getEntity() instanceof Tank) {
                ((Tank) unit.getEntity()).engineFix();
            } else if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(0);
            }
        }
    }

    @Override
    public MissingPart getMissingPart() {
        return new MissingSVEngine(actualTonnage, etype, techRating, movementFactor,
                baseEngineFactor, fuelType, getCampaign());
    }

    @Override
    public void remove(boolean salvage) {
        if (null != unit) {
            if (unit.getEntity() instanceof Tank) {
                ((Tank) unit.getEntity()).engineHit();
            } else if (unit.getEntity() instanceof Aero) {
                ((Aero) unit.getEntity()).setEngineHits(((Aero) unit.getEntity()).getMaxEngineHits());
            }
            Part spare = campaign.checkForExistingSparePart(this);
            if(!salvage) {
                campaign.removePart(this);
            } else if(null != spare) {
                spare.incrementQuantity();
                campaign.removePart(this);
            }
            unit.removePart(this);
            Part missing = getMissingPart();
            unit.addPart(missing);
            campaign.addPart(missing, 0);
        }
        setUnit(null);
        updateConditionFromEntity(false);
    }

    @Override
    public void updateConditionFromEntity(boolean checkForDestruction) {
        if(null != unit) {
            int engineHits = 0;
            int engineCrits = 0;
            if (unit.getEntity() instanceof Tank) {
                engineCrits = 2;
                if (((Tank) unit.getEntity()).isEngineHit()) {
                    engineHits = 1;
                }
            } else if (unit.getEntity() instanceof Aero) {
                engineHits = unit.getEntity().getEngineHits();
                engineCrits = 3;
            }
            if(engineHits >= engineCrits) {
                remove(false);
            } else {
                hits = Math.max(engineHits, 0);
            }
        }
    }

    @Override
    public int getBaseTime() {
        if (isSalvaging()) {
            return 360;
        }
        // 100 minutes per hit, to a maximum of 300 minutes
        return Math.min(300, hits * 100);
    }

    @Override
    public int getDifficulty() {
        if (isSalvaging()) {
            return -1;
        }
        if (hits == 1) {
            return -1;
        } else if (hits == 2) {
            return 0;
        } else if (hits > 2) {
            return 2;
        }
        return 0;
    }

    @Override
    public boolean needsFixing() {
        return hits > 0;
    }

    @Override
    public void updateConditionFromPart() {
        if (null != unit) {
            if (hits == 0) {
                if (unit.getEntity() instanceof Tank) {
                    ((Tank) unit.getEntity()).engineFix();
                } else if (unit.getEntity() instanceof Aero) {
                    ((Aero) unit.getEntity()).setEngineHits(0);
                }
            } else {
                if (unit.getEntity() instanceof Tank) {
                    ((Tank) unit.getEntity()).engineHit();
                } else if (unit.getEntity() instanceof Aero) {
                    ((Aero) unit.getEntity()).setEngineHits(hits);
                }
            }
        }
    }

    @Override
    public String checkFixable() {
        return null;
    }

    @Override
    public boolean isMountedOnDestroyedLocation() {
        return false;
    }

    @Override
    public boolean isRightTechType(String skillType) {
        if (null != getUnit()) {
            if (getUnit().getEntity() instanceof Aero) {
                return skillType.equals(SkillType.S_TECH_AERO);
            } else {
                return skillType.equals(SkillType.S_TECH_MECHANIC);
            }
        }
        // We're not tracking whether parts in the warehouse came from ground or fixed-wing/airships,
        // so let either tech repair it.
        return (skillType.equals(SkillType.S_TECH_AERO) || skillType.equals(SkillType.S_TECH_MECHANIC));
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
        return techAdvancement;
    }

    @Override
    public int getMassRepairOptionType() {
        return Part.REPAIR_PART_TYPE.ENGINE;
    }
}
