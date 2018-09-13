package mekhq.campaign.mission;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

public class ScenarioForceTemplate {
    // A scenario force template is a way to describe a particular force that gets generated when creating a DymanicScenario
    // It contains the following characteristics
    // 1) Force Alignment - whether the force is on the player's team, the opfor team or a third team
    // 2) Force Generation Method - By player force BV, by player force size or fixed size
    // 3) Force Size Multiplier - The multiplier for the cap used by the force generation method
    // 4) Deployment Zone Subset - This is a list of deployment zones from which one will be randomly picked for actual deployment
    // 5) Retreat threshold - The force will switch to retreat mode when this percentage (or fixed number) of its units are out of action
    // 6) Allowed unit types - This is a set of unit types of which the force may consist
    
    public static final String[] FORCE_ALIGNMENTS = { "Player", "Allied", "Opposing", "Third" };
    public static final String[] FORCE_GENERATION_METHODS = { "Player Deployed", "BV Scaled", "Unit Count Scaled", "Fixed Unit Count" };
    public static final String[] FORCE_DEPLOYMENT_SYNC_TYPES = { "None", "Same Edge", "Same Arc", "Opposite Edge", "Opposite Arc" };
    public static final String[] DEPLOYMENT_ZONES = { "North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest", "Edge", "Narrow Edge", "Center", "Any" };
    public static final String[] BOT_DESTINATION_ZONES = { "None", "Opposite Deployment Edge", "North", "East", "South", "West", "Random" };
    public static final String[] UNIT_TYPES = { "Mek", "ATB Mixed Mek/Vee", "Vee", "Aero", "Conv. Fighter", "Gun Emplacement", "Infantry", "Battle Armor", "Naval", "Civilian" };
    
    public enum ForceAlignment {
        Player,
        Allied,
        Opposing,
        Third
    }
    
    public enum ForceGenerationMethod {
        PlayerDeployed,
        BVScaled,
        UnitCountScaled,
        FixedUnitCount
    }
    
    public enum SynchronizedDeploymentType {
        None,
        SameEdge,
        SameArc,
        OppositeEdge,
        OppositeArc
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
    private double retreatThreshold;
    
    /**
     * The unit types that may be generated for this force. 
     */
    private List<Integer> allowedUnitTypes;
    
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
    
    //TODO: 
    // Introduce possibility to deploy opposite/same as a given force group
    // Probably set deployment zones to "opposite"
    // and introduce "deploymentForceGroup" (a drop down with all force groups present so far)
    
    /**
     * Blank constructor for deserialization purposes.
     */
    public ScenarioForceTemplate() {
        
    }
    
    public ScenarioForceTemplate(int forceAlignment, int generationMethod, double forceMultiplier, List<Integer> deploymentZones,
            int destinationZone, double retreatThreshold, List<Integer> allowedUnitTypes) {
        this.forceAlignment = forceAlignment;
        this.generationMethod = generationMethod;
        this.forceMultiplier = forceMultiplier;
        this.deploymentZones = deploymentZones == null ? new ArrayList<>() : deploymentZones;
        this.destinationZone = destinationZone;
        this.retreatThreshold = retreatThreshold;
        this.allowedUnitTypes = allowedUnitTypes;
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
    
    public double getRetreatThreshold() {
        return retreatThreshold;
    }
    
    @XmlElementWrapper(name="allowedUnitTypes")
    @XmlElement(name="allowedUnitType")
    public List<Integer> getAllowedUnitTypes() {
        return allowedUnitTypes;
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
    
    public void setRetreatThreshold(double retreatThreshold) {
        this.retreatThreshold = retreatThreshold;
    }
    
    public void setAllowedUnitTypes(List<Integer> allowedUnitTypes) {
        this.allowedUnitTypes = allowedUnitTypes;
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
}
