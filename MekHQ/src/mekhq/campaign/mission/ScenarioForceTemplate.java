package mekhq.campaign.mission;

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
    public static final String[] FORCE_GENERATION_METHODS = { "BV Scaled", "Unit Count Scaled", "Fixed Unit Count" };
    public static final String[] DEPLOYMENT_ZONES = { "North", "Northeast", "East", "Southeast", "South", "Southwest", "West", "Northwest", "Edge", "Center", "Any" };
    public static final String[] BOT_DESTINATION_ZONES = { "None", "Short Edge", "North", "East", "South", "West" };
    public static final String[] UNIT_TYPES = { "Mek", "Vee", "Aero", "Conv. Fighter", "Gun Emplacement", "Infantry", "Battle Armor", "Naval" };
    
    private int forceAlignment;
    private int generationMethod;
    private double forceMultiplier;
    private List<Integer> deploymentZones;
    private int destinationZone;
    private double retreatThreshold;
    private List<Integer> allowedUnitTypes;
    private boolean canReinforceLinked;
    
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
        this.deploymentZones = deploymentZones;
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
}
