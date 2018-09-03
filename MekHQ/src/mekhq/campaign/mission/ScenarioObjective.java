package mekhq.campaign.mission;

/**
 * Contains metadata used to describe a scenario objective
 * @author NickAragua
 *
 */
public class ScenarioObjective {
    public enum ObjectivePriority {
        Primary,
        Secondary
    };
    
    public ObjectivePriority priority;
    public String description;
    
    /*public enum ObjectiveVerb {
        Destroy,
        Capture,
        Protect,
        Extract,
        Tag,
        Scan,
        Engage,
        Reach
    }
    
    public enum UnitObjectives {
        SpecificForce,
        OpposingForces,
        AlliedForces
    }*/
    
    // an objective should have the following characteristics:
    // 1) A priority level. This is used to determine if the objective should be used to determine victory/defeat
    // 2) An objective group ID. Optional. This is used to create "complex" objectives where more than one task must be accomplished
    // 3) An objective verb. This describes what the player must *do* (e.g. destroy, protect)
    // 4) An objective noun. This describes the objective's subject (e.g. a specific force, a location, all allied forces)
    // 5) A target number. How much stuff must be done for the objective to count as completed.
    // 5a) Whether 5) is a fixed number or a percentage. 
    // 6) A time limit. The objective must be either completed within the time limit or prevented from failing within the time limit,
    //      depending on the objective verb
    
    // For example: Primary - Destroy enemy force, 50%, 10 turns
    //              Secondary - Reach Location AlliedForces 50% 
}