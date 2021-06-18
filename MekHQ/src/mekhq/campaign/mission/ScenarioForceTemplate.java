/*
 * Copyright (c) 2019 The Megamek Team. All rights reserved.
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

package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import org.w3c.dom.Node;

import megamek.common.Board;
import megamek.common.UnitType;
import megamek.common.annotations.Nullable;
import mekhq.MekHQ;

public class ScenarioForceTemplate implements Comparable<ScenarioForceTemplate> {
    // A scenario force template is a way to describe a particular force that gets generated when creating a DymanicScenario
    // It contains the following characteristics
    // 1) Force Alignment - whether the force is on the player's team, the opfor team or a third team
    // 2) Force Generation Method - By player force BV, by player force size or fixed size
    // 3) Force Size Multiplier - The multiplier for the cap used by the force generation method
    // 4) Deployment Zone Subset - This is a list of deployment zones from which one will be randomly picked for actual deployment
    // 5) Retreat threshold - The force will switch to retreat mode when this percentage (or fixed number) of its units are out of action
    // 6) Allowed unit types - This is a set of unit types of which the force may consist

    public static final String[] FORCE_ALIGNMENTS = { "Player", "Allied", "Opposing", "Third", "Planet Owner" };
    public static final String[] FORCE_GENERATION_METHODS = { "Player Supplied", "BV Scaled", "Unit Count Scaled", "Fixed Unit Count", "Player/Fixed Unit Count" };
    public static final String[] FORCE_DEPLOYMENT_SYNC_TYPES = { "None", "Same Edge", "Same Arc", "Opposite Edge", "Opposite Arc" };
    public static final String[] DEPLOYMENT_ZONES = { "Any", "Northwest", "North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Edge", "Center", "Narrow Edge" };
    public static final String[] BOT_DESTINATION_ZONES = { "North", "East", "South", "West", "Nearest", "None", "Opposite Deployment Edge", "Random" };
    public static final Map<Integer, String> SPECIAL_UNIT_TYPES;

    /**
     * Team IDs, indexed by FORCE_ALIGNMENT
     */
    public static final Map<Integer, Integer> TEAM_IDS;
    public static final Map<Integer, String> SPECIAL_ARRIVAL_TURNS;

    public static final int SPECIAL_UNIT_TYPE_ATB_AERO_MIX = -3;
    public static final int SPECIAL_UNIT_TYPE_ATB_MIX = -2;
    public static final int SPECIAL_UNIT_TYPE_ATB_CIVILIANS = -1;

    public static final int ARRIVAL_TURN_STAGGERED = -1;
    public static final int ARRIVAL_TURN_STAGGERED_BY_LANCE = -2;
    public static final int ARRIVAL_TURN_AS_REINFORCEMENTS = -3;

    public static final int DEPLOYMENT_ZONE_NARROW_EDGE = DEPLOYMENT_ZONES.length - 1;

    public static final int DESTINATION_EDGE_OPPOSITE_DEPLOYMENT = 6;
    public static final int DESTINATION_EDGE_RANDOM = 7;

    // this is used to indicate that a "fixed" size unit should deploy as a lance
    public static final int FIXED_UNIT_SIZE_LANCE = -1;

    public static final String PRIMARY_FORCE_TEMPLATE_ID = "Player";
    public static final String REINFORCEMENT_TEMPLATE_ID = "Reinforcements";

    /**
     * Which side a particular force will fight for
     */
    public enum ForceAlignment {
        /**
         * On this player's side, controlled by the player
         */
        Player,
        
        /**
         * Allied, bot-controlled
         */
        Allied,
        
        /**
         * Opposing, bot-controlled
         */
        Opposing,
        
        /**
         * Hostile to both allied and opposing, bot-controlled
         */
        Third,
        
        /**
         * Dynamically either allied, opposing or third, depending on who owns the current planet
         */
        PlanetOwner;

        /**
         * Get a force alignment value given an int
         */
        @Nullable
        public static ForceAlignment getForceAlignment(int ordinal) {
            for (ForceAlignment fe : values()) {
                if (fe.ordinal() == ordinal) {
                    return fe;
                }
            }
            return null;
        }
    }

    /**
     * What kind of mechanism to use to generate the force
     */
    public enum ForceGenerationMethod {
        /**
         * Assigned by player from TO&E
         */
        PlayerSupplied,
        
        /**
         * Scale using BV, based on the BV value of already generated units flagged as contributing towards BV
         */
        BVScaled,
        
        /*
         * Scale on the unit count, based on number of already generated units flagged as contributing towards unit count
         */
        UnitCountScaled,
        
        /**
         * What it says on the tin
         */
        FixedUnitCount,
        
        /**
         * Either assigned by player from TO&E or a minimum fixed number of units; TODO: currently unimplemented
         */
        PlayerOrFixedUnitCount
    }

    /**
     * How to determine deployment edge of this force based on deployment edge of a designated force
     */
    public enum SynchronizedDeploymentType {
        /**
         * Don't
         */
        None,
        
        /**
         * Same edge as the designated force
         */
        SameEdge,
        
        /**
         * Same or adjacent edge as the designated force (e.g. E = E, NE, SE)
         */
        SameArc,
        
        /**
         * Opposite edge from the designated force (ANY = ANY, CTR = EDGE, EDGE = CTR)
         */
        OppositeEdge,
        
        /**
         * Oppositee or adjacent edge as the designated force (e.g. W = E, NE, SE)
         */
        OppositeArc
    }

    static {
        SPECIAL_UNIT_TYPES = new HashMap<>();
        SPECIAL_UNIT_TYPES.put(SPECIAL_UNIT_TYPE_ATB_AERO_MIX, "AtB Aircraft Mix");
        SPECIAL_UNIT_TYPES.put(SPECIAL_UNIT_TYPE_ATB_CIVILIANS, "AtB Civilian Units");
        SPECIAL_UNIT_TYPES.put(SPECIAL_UNIT_TYPE_ATB_MIX, "Standard AtB Mix");

        SPECIAL_ARRIVAL_TURNS = new HashMap<>();
        SPECIAL_ARRIVAL_TURNS.put(ARRIVAL_TURN_STAGGERED, "Staggered");
        SPECIAL_ARRIVAL_TURNS.put(ARRIVAL_TURN_STAGGERED_BY_LANCE, "Staggered By Lance");
        SPECIAL_ARRIVAL_TURNS.put(ARRIVAL_TURN_AS_REINFORCEMENTS, "Reinforcements");

        TEAM_IDS = new HashMap<>();
        TEAM_IDS.put(ForceAlignment.Player.ordinal(), 1);
        TEAM_IDS.put(ForceAlignment.Allied.ordinal(), 1);
        TEAM_IDS.put(ForceAlignment.Opposing.ordinal(), 2);
        TEAM_IDS.put(ForceAlignment.Third.ordinal(), 3);
    }

    /**
     * The alignment of the force.
     * Player - the "force" will be added to whatever units the player deploys or *is* the player-controlled force
     * Allied - a bot-controlled force on the same team as the player
     * Opposing - a bot-controlled force on the opposite team from the player
     * Third - a bot-controlled force hostile to both the player and opposing bot
     */
    private int forceAlignment;

    /**
     * The mechanism used to generate the force.
     * Player Deployed - the player will deploy this force.
     * BV Scaled - the contents of this force are scaled based on the BV of player and allied forces
     * Unit Count Scaled - the contents of this force are scaled based on the number of player and allied forces
     * Fixed Unit Count - this force has a fixed number of units.
     */
    private int generationMethod;

    /**
     * This is used to multiply the BV budget or Unit count of the force if the generation method is scaled.
     */
    private double forceMultiplier;

    /**
     * The possible deployment zones for this force. "Narrow Edge" examines the board and picks one of the edges with the
     * lowest dimensions.
     */
    private List<Integer> deploymentZones;

    /**
     * The zone to which this force will attempt to move.
     */
    private int destinationZone;

    /**
     * This force will attempt to retreat after losing the specified percentage of units (by count or BV?)
     */
    private int retreatThreshold;

    /**
     * The unit types that may be generated for this force.
     */
    private int allowedUnitType;

    /**
     * Whether this force is allowed to reinforce linked scenarios (as described in the AtB Stratcon rules)
     */
    private boolean canReinforceLinked;

    /**
     * Whether this force contributes to the BV budget if the generation method is BV Scaled
     */
    private boolean contributesToBV;

    /**
     * Whether this force contributes to the unit count if the generation method is Unit Count Scaled.
     */
    private boolean contributesToUnitCount;

    /**
     * A short, unique name for the force.
     */
    private String forceName;

    /**
     * The identifier of a force with which this force is synchronized, for the purposes of sharing deployment zones
     * and retreat thresholds.
     */
    private String syncedForceName;

    /**
     * In the case where this force is synchronized with another,
     */
    private SynchronizedDeploymentType syncDeploymentType;

    /**
     * Whether or not this force shares a retreat threshold with the synced force.
     * If yes, then all synced forces will retreat once
     */
    private boolean syncRetreatThreshold;

    /**
     * The turn on which this force arrives. Staggered = -1, Staggered By Lance = -2
     */
    private int arrivalTurn;

    /**
     * Maximum weight class of this force.
     */
    private int maxWeightClass;

    /**
     * Minimum weight class of this force.
     */
    private int minWeightClass;

    /**
     * Whether or not this force contributes to scaling map size.
     */
    private boolean contributesToMapSize;

    /**
     * The materialized deployment zone after this template has been applied to a force.
     */
    private int actualDeploymentZone = Board.START_NONE;

    /**
     * How many units to generate, in case of a fixed unit count.
     */
    private int fixedUnitCount = 0;

    /**
     * The "generation bucket" to which this force template is assigned.
     * Forces within a particular "generation bucket" will be generated at the same time, taking into account
     * forces previously generated.
     */
    private int generationOrder = 0;

    /**
     * Whether or not to load any aerospace units generated by this force template with bombs.
     * May not actually result in bombs.
     */
    private boolean allowAeroBombs = false;

    /**
     * The altitude/elevation at which this unit starts.
     * For normally ground units, this indicates a "hot drop" (is it possible?)
     * For helos, it's the elevation
     * For aircraft it's actual altitude, with 0 being grounded.
     * Ignored in space.
     */
    private int startingAltitude;

    /**
     * Whether or not this force will be composed of artillery units.
     * For some unit types this may result in failure to generate a force, so use with caution.
     */
    private boolean useArtillery = false;

    /**
     * Whether or not this force will deploy artillery units off-board.
     */
    private boolean deployOffBoard = false;

    /**
     * A list of force IDs with which this force will be linked for objective purposes.
     * e.g. if there's an objective to destroy 50% of "Primary Opfor", this force will count towards that as well.
     */
    private List<String> objectiveLinkedForces;

    @Override
    public ScenarioForceTemplate clone() {
        return new ScenarioForceTemplate(this);
    }
    
    /**
     * Blank constructor for deserialization purposes.
     */
    public ScenarioForceTemplate() {
        deploymentZones = new ArrayList<>();
        objectiveLinkedForces = new ArrayList<>();
    }

    /**
     * Constructor given a set of individual parameters, useful for populating from individual UI elements
     */
    public ScenarioForceTemplate(int forceAlignment, int generationMethod, double forceMultiplier, List<Integer> deploymentZones,
            int destinationZone, int retreatThreshold, int allowedUnitType) {
        this.forceAlignment = forceAlignment;
        this.generationMethod = generationMethod;
        this.forceMultiplier = forceMultiplier;
        this.destinationZone = destinationZone;
        this.retreatThreshold = retreatThreshold;
        this.allowedUnitType = allowedUnitType;
        this.deploymentZones = deploymentZones == null ? new ArrayList<>() : new ArrayList<>(deploymentZones);
        this.objectiveLinkedForces = new ArrayList<>();
    }
    
    /**
     * Copy constructor
     */
    public ScenarioForceTemplate(ScenarioForceTemplate forceDefinition) {
        forceAlignment = forceDefinition.forceAlignment;
        generationMethod = forceDefinition.generationMethod;
        forceMultiplier = forceDefinition.forceMultiplier;
        deploymentZones = new ArrayList<>();
        
        for (int zone : forceDefinition.deploymentZones) {
            deploymentZones.add(zone);
        }
        
        destinationZone = forceDefinition.destinationZone;
        retreatThreshold = forceDefinition.retreatThreshold;
        allowedUnitType = forceDefinition.allowedUnitType;
        canReinforceLinked = forceDefinition.canReinforceLinked;
        contributesToBV = forceDefinition.contributesToBV;
        contributesToUnitCount = forceDefinition.contributesToUnitCount;
        forceName = forceDefinition.forceName;
        syncedForceName = forceDefinition.syncedForceName;
        syncDeploymentType = forceDefinition.syncDeploymentType;
        syncRetreatThreshold = forceDefinition.syncRetreatThreshold;
        arrivalTurn = forceDefinition.arrivalTurn;
        maxWeightClass = forceDefinition.maxWeightClass;
        minWeightClass = forceDefinition.minWeightClass;
        contributesToMapSize = forceDefinition.contributesToMapSize;
        actualDeploymentZone =  forceDefinition.actualDeploymentZone;
        fixedUnitCount = forceDefinition.fixedUnitCount;
        generationOrder = forceDefinition.generationOrder;
        allowAeroBombs = forceDefinition.allowAeroBombs;
        startingAltitude = forceDefinition.startingAltitude;
        useArtillery = forceDefinition.useArtillery;
        deployOffBoard = forceDefinition.deployOffBoard;
        objectiveLinkedForces = new ArrayList<String>();
        
        for (String force : forceDefinition.objectiveLinkedForces) {
            objectiveLinkedForces.add(force);
        }
    }

    public int getForceAlignment() {
        return forceAlignment;
    }

    public int getGenerationMethod() {
        return generationMethod;
    }

    public double getForceMultiplier() {
        return forceMultiplier;
    }

    @XmlElementWrapper(name="deploymentZones")
    @XmlElement(name="deploymentZone")
    public List<Integer> getDeploymentZones() {
        return deploymentZones;
    }

    public int getDestinationZone() {
        return destinationZone;
    }

    public int getRetreatThreshold() {
        return retreatThreshold;
    }

    public int getAllowedUnitType() {
        return allowedUnitType;
    }

    public boolean getCanReinforceLinked() {
        return canReinforceLinked;
    }

    public boolean getContributesToBV() {
        return contributesToBV;
    }

    public boolean getContributesToUnitCount() {
        return contributesToUnitCount;
    }

    public String getForceName() {
        return forceName;
    }

    public String getSyncedForceName() {
        return syncedForceName;
    }

    public SynchronizedDeploymentType getSyncDeploymentType() {
        return syncDeploymentType;
    }

    public boolean getSyncRetreatThreshold() {
        return syncRetreatThreshold;
    }

    public int getActualDeploymentZone() {
        return actualDeploymentZone;
    }

    public int getArrivalTurn() {
        return arrivalTurn;
    }

    public int getMaxWeightClass() {
        return maxWeightClass;
    }

    public boolean getContributesToMapSize() {
        return contributesToMapSize;
    }

    public int getGenerationOrder() {
        return generationOrder;
    }

    public int getFixedUnitCount() {
        return fixedUnitCount;
    }

    public boolean getAllowAeroBombs() {
        return allowAeroBombs;
    }

    public int getStartingAltitude() {
        return startingAltitude;
    }

    public boolean getUseArtillery() {
        return useArtillery;
    }

    public boolean getDeployOffboard() {
        return deployOffBoard;
    }

    public void setForceAlignment(int forceAlignment) {
        this.forceAlignment = forceAlignment;
    }

    public void setGenerationMethod(int generationMethod) {
        this.generationMethod = generationMethod;
    }

    public void setForceMultiplier(double forceMultiplier) {
        this.forceMultiplier = forceMultiplier;
    }

    public void setDeploymentZones(List<Integer> deploymentZones) {
        this.deploymentZones = deploymentZones;
    }

    public void setDestinationZone(int destinationZone) {
        this.destinationZone = destinationZone;
    }

    public void setRetreatThreshold(int retreatThreshold) {
        this.retreatThreshold = retreatThreshold;
    }

    public void setAllowedUnitType(int allowedUnitType) {
        this.allowedUnitType = allowedUnitType;
    }

    public void setCanReinforceLinked(boolean canReinforce) {
        this.canReinforceLinked = canReinforce;
    }

    public void setContributesToBV(boolean contributesToBV) {
        this.contributesToBV = contributesToBV;
    }

    public void setContributesToUnitCount(boolean contributesToUnitCount) {
        this.contributesToUnitCount = contributesToUnitCount;
    }

    public void setForceName(String forceName) {
        this.forceName = forceName;
    }

    public void setSyncedForceName(String syncedForceName) {
        this.syncedForceName = syncedForceName;
    }

    public void setSyncDeploymentType(SynchronizedDeploymentType syncDeploymentType) {
        this.syncDeploymentType = syncDeploymentType;
    }

    public void setActualDeploymentZone(int zone) {
        this.actualDeploymentZone = zone;
    }

    public void setArrivalTurn(int arrivalTurn) {
        this.arrivalTurn = arrivalTurn;
    }

    public void setMaxWeightClass(int maxWeightClass) {
        this.maxWeightClass = maxWeightClass;
    }

    public int getMinWeightClass() {
        return minWeightClass;
    }

    public void setMinWeightClass(int minWeightClass) {
        this.minWeightClass = minWeightClass;
    }

    public void setContributesToMapSize(boolean contributesToMapSize) {
        this.contributesToMapSize = contributesToMapSize;
    }

    public void setFixedUnitCount(int fixedUnitCount) {
        this.fixedUnitCount = fixedUnitCount;
    }

    public void setGenerationOrder(int generationOrder) {
        this.generationOrder = generationOrder;
    }

    public void setAllowAeroBombs(boolean allowAeroBombs) {
        this.allowAeroBombs = allowAeroBombs;
    }

    public void setUseArtillery(boolean useArtillery) {
        this.useArtillery = useArtillery;
    }

    public void setStartingAltitude(int startingAltitude) {
        this.startingAltitude = startingAltitude;
    }

    public void setDeployOffboard(boolean deployOffBoard) {
        this.deployOffBoard = deployOffBoard;
    }

    @XmlElementWrapper(name="objectiveLinkedForces")
    @XmlElement(name="objectiveLinkedForce")
    public List<String> getObjectiveLinkedForces() {
        return objectiveLinkedForces;
    }

    public void setObjectiveLinkedForces(List<String> objectiveLinkedForces) {
        this.objectiveLinkedForces = objectiveLinkedForces;
    }

    /**
     * Whether this force is to be player controlled and supplied units
     */
    public boolean isPlayerForce() {
        return getForceAlignment() == ForceAlignment.Player.ordinal() &&
               getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal();
    }

    /**
     * Whether this force is to be player controlled but externally-supplied units
     */
    public boolean isAlliedPlayerForce() {
        return getForceAlignment() == ForceAlignment.Player.ordinal() &&
                getGenerationMethod() != ForceGenerationMethod.PlayerSupplied.ordinal();
    }

    /**
     * Whether this force is bot-controlled and allied to the player
     * @return
     */
    public boolean isAlliedBotForce() {
        return getForceAlignment() == ForceAlignment.Allied.ordinal() &&
                getGenerationMethod() == ForceGenerationMethod.PlayerSupplied.ordinal();
    }

    /**
     * Whether this force is bot-controlled and hostile to the player
     */
    public boolean isEnemyBotForce() {
        return getForceAlignment() == ForceAlignment.Opposing.ordinal() ||
                getForceAlignment() == ForceAlignment.Third.ordinal();
    }

    /**
     * Convenience function that returns the displayable name of the selected unit type.
     * @return
     */
    public String getAllowedUnitTypeName() {
        if(getAllowedUnitType() >= UnitType.SIZE || getAllowedUnitType() < 0) {
            return SPECIAL_UNIT_TYPES.get(getAllowedUnitType());
        } else {
            return UnitType.getTypeDisplayableName(getAllowedUnitType());
        }
    }

    /**
     * Attempt to deserialize an instance of a ScenarioForceTemplate from the passed-in XML Node
     * @param xmlNode The source file
     * @return Possibly an instance of a ScenarioForceTemplate
     */
    public static ScenarioForceTemplate Deserialize(Node xmlNode) {
        ScenarioForceTemplate resultingTemplate = null;

        try {
            JAXBContext context = JAXBContext.newInstance(ScenarioForceTemplate.class);
            Unmarshaller um = context.createUnmarshaller();
            JAXBElement<ScenarioForceTemplate> templateElement = um.unmarshal(xmlNode, ScenarioForceTemplate.class);
            resultingTemplate = templateElement.getValue();
        } catch(Exception e) {
            MekHQ.getLogger().error(ScenarioTemplate.class, "Deserialize", "Error Deserializing Scenario Force Template", e);
        }

        return resultingTemplate;
    }

    @Override
    public int compareTo(ScenarioForceTemplate o) {
        if(this.forceAlignment > o.forceAlignment) {
            return 1;
        } else if(this.forceAlignment < o.forceAlignment) {
            return -1;
        } else {
            return this.forceName.charAt(0) > o.forceName.charAt(0) ? 1 : -1;
        }
    }

    /**
     * Convenient factory method to return a default "Reinforcements" force template
     * generally useful if the scenario does not have one specified
     */
    public static ScenarioForceTemplate getDefaultReinforcementsTemplate() {
        final List<Integer> reinforcementDeploymentZones = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9);
        ScenarioForceTemplate rft = new ScenarioForceTemplate();
        rft.setAllowedUnitType(SPECIAL_UNIT_TYPE_ATB_MIX);
        rft.setCanReinforceLinked(true);
        rft.setDeploymentZones(reinforcementDeploymentZones);
        rft.setArrivalTurn(ARRIVAL_TURN_AS_REINFORCEMENTS);
        rft.setForceAlignment(ForceAlignment.Player.ordinal());
        rft.setForceName(REINFORCEMENT_TEMPLATE_ID);
        rft.setGenerationMethod(ForceGenerationMethod.PlayerSupplied.ordinal());
        rft.setGenerationOrder(1);
        rft.setActualDeploymentZone(Board.START_NONE);
        rft.setSyncDeploymentType(SynchronizedDeploymentType.None);

        return rft;
    }
    
    @Override
    public String toString() {
        return getForceName();
    }
}
