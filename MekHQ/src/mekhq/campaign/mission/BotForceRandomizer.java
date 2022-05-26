/*
 * Copyright (c) 2021 - The Megamek Team. All Rights Reserved.
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.mission;

import megamek.Version;
import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.client.generator.enums.SkillGeneratorType;
import megamek.client.generator.skillGenerators.AbstractSkillGenerator;
import megamek.client.generator.skillGenerators.TaharqaSkillGenerator;
import megamek.codeUtilities.StringUtility;
import megamek.common.*;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.enums.SkillLevel;
import mekhq.utilities.MHQXMLUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.IUnitGenerator;
import mekhq.campaign.universe.UnitGeneratorParameters;
import org.apache.commons.math3.distribution.GammaDistribution;
import org.apache.logging.log4j.LogManager;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.util.*;

/**
 * A class that can be used to generate a random force with some parameters. Provides a simpler approach
 * to opfor generation than AtBDynamicScenarioFactory. Intended for use by StoryArc but written generally
 * enough to be repurposed.
 *
 * Unlike AtBDynamicScenarioFactory, the methods here are not static, but depend on variables in an actual
 * BotForceRandomizer than can be added to a BotForce. If present, this randomizer will be used to generate
 * forces for the BotForce through the GameThread when a game is started.
 */
public class BotForceRandomizer {
    //region Variable declarations
    public static final int UNIT_WEIGHT_UNSPECIFIED = -1;

    private enum BalancingMethod {
        BV,
        WEIGHT_ADJ;

        @Override
        public String toString() {
            if (this == BV) {
                return "BV";
            } else if (this == WEIGHT_ADJ) {
                return "Adjusted Weight";
            }
            return super.toString();
        }
    }

    /** faction to draw from **/
    private String factionCode;

    /** skill level **/
    private SkillLevel skill;

    /** unit quality level **/
    private int quality;

    /** unit type **/
    private int unitType;

    /** lance size - this is the smallest increment in which random units will be generated and added **/
    private int lanceSize;

    /** focal weight class - if this is missing we use the mean weight class of the players unit **/
    private double focalWeightClass;

    /** force multiplier relative to player's deployed forces **/
    private double forceMultiplier;

    /** balancing method **/
    private BalancingMethod balancingMethod;

    /** convenience campaign pointer **/
    private Campaign campaign;

    /**
     * what percent of mek and aero forces should actually be conventional?
     * (tanks and conventional aircraft respectively)
     **/
    private int percentConventional;

    /**
     * percent chance that a mek "lance" will come with integrated battle armor units
     */
    private int baChance;
    //endregion Variable Declarations

    //region Constructors
    public BotForceRandomizer() {
        factionCode = "MERC";
        skill = SkillLevel.REGULAR;
        unitType = UnitType.MEK;
        forceMultiplier = 1.0;
        percentConventional = 0;
        baChance = 0;
        balancingMethod = BalancingMethod.WEIGHT_ADJ;
        lanceSize = 1;
    }
    //endregion Constructors

    //region Getters/Setters
    public String getFactionCode() {
        return factionCode;
    }

    public void setFactionCode(final String factionCode) {
        this.factionCode = factionCode;
    }
    //endregion Getters/Setters

    /**
     * This is the primary function that generates a force of entities from the given parameters. The
     * intent is that this function is called from GameThread when the game is started.
     * @param playerUnits A List of Units for the player's deployed force in the relevant scenario. This
     *                    is used to determine the total points allowed for this force.
     * @param botFixedEntities A List of The fixed Entities that might have also been declared in BotForce already.
     *                         This is used to calculate the starting points already used when generating the force.
     * @return A List of Entities that will be added to the game by GameThread.
     */
    public List<Entity> generateForce(List<Unit> playerUnits, List<Entity> botFixedEntities) {
        ArrayList<Entity> entityList = new ArrayList<>();

        double maxPoints = calculateMaxPoints(playerUnits);
        double currentPoints = calculateStartingPoints(botFixedEntities);
        if ((focalWeightClass < EntityWeightClass.WEIGHT_LIGHT) ||
                (focalWeightClass > EntityWeightClass.WEIGHT_ASSAULT)) {
            // if no focal weight class was provided or its outside of range then use the mean of the player units
            focalWeightClass = calculateMeanWeightClass(playerUnits);
        }

        // using a gamma distribution to get actual weight class for each lance. Each gamma
        // distribution is centered on the focal weight class and has some chance of going higher
        // or lower. The scale parameter of 0.4 produces a reasonable variance.
        GammaDistribution gamma = new GammaDistribution(focalWeightClass / 0.4, 0.4);

        int uType;
        List<Entity> lanceList;
        int weightClass;
        while (currentPoints < maxPoints) {

            weightClass = sampleWeightClass(gamma);

            // if the unit type is mek or aero, then roll to see if I get a conventional unit instead
            uType = unitType;
            if ((unitType == UnitType.MEK) && (percentConventional > 0)
                    && (Compute.randomInt(100) <= percentConventional)) {
                uType = UnitType.TANK;
            } else if ((unitType == UnitType.AERO) && (percentConventional > 0)
                    && (Compute.randomInt(100) <= percentConventional)) {
                uType = UnitType.CONV_FIGHTER;
            }

            lanceList = generateLance(lanceSize, uType, weightClass);
            for (Entity e : lanceList) {
                entityList.add(e);
                currentPoints += calculatePoints(e);
            }
        }

        return entityList;
    }

    /**
     * Generate a "lance" of entities based on the lanceSize variable. This is not really a lance but
     * the size of the increment in the number of entities that are part of this force. This can be set to
     * 1 to generate entities individually. The larger this number is the greater the chance of overshooting
     * the target number of points.
     * @param size an int giving the number of units to generate
     * @param uType The UnitType of generated units
     * @param weightClass an int giving the weight class of generated units. The function applies some randomness
     *                    to this, so some entities within the lance may be heavier or lighter.
     * @return A List of generated entities.
     */
    public List<Entity> generateLance(int size, int uType, int weightClass) {
        ArrayList<Entity> lanceList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Entity e = getEntity(uType, weightClass);
            if (null != e) {
                lanceList.add(e);
            }
        }

        // check for integrated BA support
        if ((unitType == UnitType.MEK) && (baChance > 0)
                && (Compute.randomInt(100) <= baChance)) {
            for (int i = 0; i < size; i++) {
                Entity e = getEntity(UnitType.BATTLE_ARMOR, UNIT_WEIGHT_UNSPECIFIED);
                if (null != e) {
                    lanceList.add(e);
                }
            }
        }

        return lanceList;
    }

    /**
     * Determines the most appropriate RAT and uses it to generate a random Entity. This
     * function borrows heavily from AtBDynamicScenarioFactory#getEntity
     *
     * @param uType    The UnitTableData constant for the type of unit to generate.
     * @param weightClass The weight class of the unit to generate
     * @return A new Entity with crew.
     */
    public Entity getEntity(int uType, int weightClass) {
        MechSummary ms;

        // allow some variation in actual weight class
        int weightRoll = Compute.randomInt(6);
        if ((weightRoll == 1) && (weightClass > EntityWeightClass.WEIGHT_LIGHT)) {
            weightClass -= 1;
        } else if ((weightRoll == 6) && (weightClass < EntityWeightClass.WEIGHT_ASSAULT)) {
            weightClass += 1;
        }

        UnitGeneratorParameters params = new UnitGeneratorParameters();
        params.setFaction(factionCode);
        params.setQuality(quality);
        params.setUnitType(uType);
        params.setWeightClass(weightClass);
        params.setYear(campaign.getGameYear());

        if (uType == UnitType.TANK) {
            // allow VTOLs too
            params.getMovementModes().addAll(IUnitGenerator.MIXED_TANK_VTOL);
        }

        ms = campaign.getUnitGenerator().generate(params);

        return createEntityWithCrew(ms);
    }

    /**
     * This creates the entity with a crew. Borrows heavily from AtBDynamicScenarioFactory#createEntityWithCrew
     *
     * @param ms Which entity to generate
     * @return A crewed entity
     */
    public @Nullable Entity createEntityWithCrew(MechSummary ms) {
        Entity en;
        try {
            en = new MechFileParser(ms.getSourceFile(), ms.getEntryName()).getEntity();
        } catch (Exception ex) {
            LogManager.getLogger().error("Unable to load entity: " + ms.getSourceFile() + ": " + ms.getEntryName(), ex);
            return null;
        }
        Faction faction = Factions.getInstance().getFaction(factionCode);

        en.setOwner(campaign.getPlayer());
        en.setGame(campaign.getGame());

        RandomNameGenerator rng = RandomNameGenerator.getInstance();
        rng.setChosenFaction(faction.getNameGenerator());
        Gender gender = RandomGenderGenerator.generate();
        String[] crewNameArray = rng.generateGivenNameSurnameSplit(gender, faction.isClan(), faction.getShortName());
        String crewName = crewNameArray[0];
        crewName += !StringUtility.isNullOrBlank(crewNameArray[1]) ?  " " + crewNameArray[1] : "";

        Map<Integer, Map<String, String>> extraData = new HashMap<>();
        Map<String, String> innerMap = new HashMap<>();
        innerMap.put(Crew.MAP_GIVEN_NAME, crewNameArray[0]);
        innerMap.put(Crew.MAP_SURNAME, crewNameArray[1]);

        final AbstractSkillGenerator skillGenerator = new TaharqaSkillGenerator();
        skillGenerator.setLevel(skill);
        if (faction.isClan()) {
            skillGenerator.setType(SkillGeneratorType.CLAN);
        }
        int[] skills = skillGenerator.generateRandomSkills(en);

        if (faction.isClan() && (Compute.d6(2) > (6 - skill.ordinal() + skills[0] + skills[1]))) {
            Phenotype phenotype = Phenotype.NONE;
            switch (en.getUnitType()) {
                case UnitType.MEK:
                    phenotype = Phenotype.MECHWARRIOR;
                    break;
                case UnitType.TANK:
                case UnitType.VTOL:
                    // The Vehicle Phenotype is unique to Clan Hell's Horses
                    if (faction.getShortName().equals("CHH")) {
                        phenotype = Phenotype.VEHICLE;
                    }
                    break;
                case UnitType.BATTLE_ARMOR:
                    phenotype = Phenotype.ELEMENTAL;
                    break;
                case UnitType.AERO:
                case UnitType.CONV_FIGHTER:
                    phenotype = Phenotype.AEROSPACE;
                    break;
                case UnitType.PROTOMEK:
                    phenotype = Phenotype.PROTOMECH;
                    break;
                case UnitType.SMALL_CRAFT:
                case UnitType.DROPSHIP:
                case UnitType.JUMPSHIP:
                case UnitType.WARSHIP:
                    // The Naval Phenotype is unique to Clan Snow Raven and the Raven Alliance
                    if (faction.getShortName().equals("CSR") || faction.getShortName().equals("RA")) {
                        phenotype = Phenotype.NAVAL;
                    }
                    break;
            }

            if (phenotype != Phenotype.NONE) {
                String bloodname = Bloodname.randomBloodname(faction.getShortName(), phenotype,
                        campaign.getGameYear()).getName();
                crewName += " " + bloodname;
                innerMap.put(Crew.MAP_BLOODNAME, bloodname);
                innerMap.put(Crew.MAP_PHENOTYPE, phenotype.name());
            }
        }

        extraData.put(0, innerMap);

        en.setCrew(new Crew(en.getCrew().getCrewType(), crewName, Compute.getFullCrewSize(en),
                skills[0], skills[1], gender, extraData));

        en.setExternalIdAsString(UUID.randomUUID().toString());
        return en;
    }

    /**
     * This function samples from the given gamma distribution to get a random weight class. Results are trimmed
     * to reasonable values and rounded to integers.
     * @param gamma The GammaDistribution from which a random value is drawn
     * @return and integer giving the sampled weight class
     */
    private int sampleWeightClass(GammaDistribution gamma) {
        int weightClass = (int) Math.round(gamma.sample());
        // clamp to weight limits
        return Math.max(EntityWeightClass.WEIGHT_LIGHT, Math.min(EntityWeightClass.WEIGHT_ASSAULT, weightClass));
    }

    /**
     * This function calculates the maximum "points" that the generated force should be. The term "points" is abstract
     * and can refer to different things depending on the selected BalancingMethod. The maximum points are defined by
     * a multiple of the player unit points.
     * @param playerUnits A List of Units from the player's units assigned to a given scenario
     * @return a double giving the targeted maximum points for the generated force.
     */
    private double calculateMaxPoints(List<Unit> playerUnits) {
        double maxPoints = 0;
        for (Unit u : playerUnits) {
            maxPoints += calculatePoints(u.getEntity());
        }

        maxPoints = (int) Math.ceil(maxPoints * forceMultiplier);
        return maxPoints;
    }

    /**
     * Calculates the starting points for this force already used up by fixed entities that are part of the BotForce.
     * The term "points" is abstract and can refer to different things depending on the selected BalancingMethod.
     * @param botEntities - A List of Entities, typically specified as fixed units in BotForce
     * @return a double giving the starting points already used by the fixed units in the BotForce
     */
    private double calculateStartingPoints(List<Entity> botEntities) {
        double startPoints = 0;
        for (Entity e : botEntities) {
            startPoints += calculatePoints(e);
        }

        return startPoints;
    }

    /**
     * This function calculates how many "points" a given entity counts for. The use of points is abstract and
     * will be determined differently depending on the provided BalancingMethod
     * @param e - an Entity
     * @return a double giving the points provided by this entity
     */
    private double calculatePoints(Entity e) {
        if (balancingMethod == BalancingMethod.BV) {
            return e.calculateBattleValue();
        } else if (balancingMethod == BalancingMethod.WEIGHT_ADJ) {
            return getAdjustedWeightPoints(e);
        }
        return e.getWeight();
    }

    /**
     * A static method calculating the adjusted weight of an entity for use in the WEIGHT_ADJ BalancingMethod.
     * Units get points by weight, but a multiplier is applied to these weights by unit type.
     * @param e an Entity
     * @return a double indicating the adjusted weight points of a unit.
     */
    private static double getAdjustedWeightPoints(Entity e) {
        double points = e.getWeight();

        double multiplier;
        switch (e.getUnitType()) {
            case UnitType.MEK:
            case UnitType.AERO:
            case UnitType.PROTOMEK:
                multiplier = 1.0;
                break;
            case UnitType.TANK:
            case UnitType.VTOL:
            case UnitType.NAVAL:
                multiplier = 0.6;
                break;
            case UnitType.CONV_FIGHTER:
                multiplier = 0.4;
                break;
            case UnitType.BATTLE_ARMOR:
                points = 10;
                multiplier = 1;
                break;
            case UnitType.INFANTRY:
                points = 0.5;
                multiplier = 1;
                break;
            case UnitType.GUN_EMPLACEMENT:
                multiplier = 0.2;
                break;
            case UnitType.DROPSHIP:
            case UnitType.JUMPSHIP:
            case UnitType.WARSHIP:
                multiplier = 0.1;
                break;
            default:
                multiplier = 0;
        }

        return points * multiplier;

    }

    /**
     * Calculates the mean weight class of a List of Units
     * @param playerUnits - A List of Units
     * @return a double indicating the mean weight class
     */
    private double calculateMeanWeightClass(List<Unit> playerUnits) {
        int sumWeightClass = 0;
        int nUnits = 0;
        for (Unit u : playerUnits) {
            sumWeightClass += u.getEntity().getWeightClass();
            nUnits += 1;
        }

        if ((nUnits == 0) || (sumWeightClass == 0)) {
            return EntityWeightClass.WEIGHT_MEDIUM;
        }

        return sumWeightClass / ((double) nUnits);
    }

    /**
     * This method returns a description of the random parameters of this object that will be shown in the
     * ScenarioViewPanel
     * @return a String giving the description.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(Factions.getInstance().getFaction(factionCode).getFullName(campaign.getGameYear()));
        sb.append(" ");
        sb.append(skill.toString());
        sb.append(" ");
        String typeDesc = UnitType.getTypeDisplayableName(unitType);
        if (percentConventional > 0) {
            typeDesc = typeDesc + " and Conventional";
        }
        sb.append(typeDesc);
        sb.append(" at x");
        sb.append(forceMultiplier);
        sb.append(" multiplier (");
        sb.append(balancingMethod.toString());
        sb.append(")");
        return sb.toString();
    }

    //region File I/O
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "botForceRandomizer");
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "factionCode", getFactionCode());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "quality", quality);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "skill", skill.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "unitType", unitType);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "lanceSize", lanceSize);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "focalWeightClass", focalWeightClass);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "forceMultiplier", forceMultiplier);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "balancingMethod", balancingMethod.name());
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "percentConventional", percentConventional);
        MHQXMLUtility.writeSimpleXMLTag(pw, indent, "baChance", baChance);
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "botForceRandomizer");
    }

    public static BotForceRandomizer generateInstanceFromXML(Node wn, Campaign c, Version version) {
        BotForceRandomizer retVal = new BotForceRandomizer();

        retVal.campaign = c;
        try {
            // Okay, now load Part-specific fields!
            NodeList nl = wn.getChildNodes();

            for (int x = 0; x < nl.getLength(); x++) {
                Node wn2 = nl.item(x);

                if (wn2.getNodeName().equalsIgnoreCase("factionCode")) {
                    retVal.setFactionCode(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("quality")) {
                    retVal.quality = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("unitType")) {
                    retVal.unitType = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("skill")) {
                    retVal.skill = SkillLevel.valueOf(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("lanceSize")) {
                    retVal.lanceSize = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("focalWeightClass")) {
                    retVal.focalWeightClass = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("forceMultiplier")) {
                    retVal.forceMultiplier = Double.parseDouble(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("percentConventional")) {
                    retVal.percentConventional = Integer.parseInt(wn2.getTextContent().trim());
                }  else if (wn2.getNodeName().equalsIgnoreCase("baChance")) {
                    retVal.baChance = Integer.parseInt(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("balancingMethod")) {
                    retVal.balancingMethod = BalancingMethod.valueOf(wn2.getTextContent().trim());
                }
            }
        }  catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }

        return retVal;
    }
    //endregion File I/O
}
