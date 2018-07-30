package mekhq.campaign.mission;

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
}
