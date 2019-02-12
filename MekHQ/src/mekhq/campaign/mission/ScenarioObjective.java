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
    
    private ObjectivePriority priority;
    private String description;
    
    public enum ObjectiveVerb {
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
    }

    public ObjectivePriority getPriority() {
        return priority;
    }

    public void setPriority(ObjectivePriority priority) {
        this.priority = priority;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
    // an objective should have the following characteristics:
    // 1) A priority level. This is used to determine if the objective should be used to determine victory/defeat
    // 2-6 should be a separate struct, in a list
    // 2) An objective verb. This describes what the player must *do* (e.g. destroy, protect)
    // 3) An objective noun. This describes the objective's subject (e.g. a specific force, a location, all allied forces)
    // 4) A target number. How much stuff must be done for the objective to count as completed.
    // 4a) Whether 4) is a fixed number or a percentage. 
    // 5) A time limit. The objective must be either completed within the time limit or prevented from failing within the time limit,
    //      depending on the objective verb
    // 6) A text description 
    // 7) Scenario Effects (a list) 
    //      One of the following:
    //      Victory
    //      Battlefield Control (can salvage, etc)
    //      Spawn scenario (select template) (
    //      Unlock scenario (select template)
    //
    
    // For example: Primary - Destroy enemy force, 50%, 10 turns
    //              Secondary - Reach Location AlliedForces 50% 
}