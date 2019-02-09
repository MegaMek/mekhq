package mekhq.campaign.stratcon;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBDynamicScenario;

public class StratconScenario implements IStratconDisplayable {
    public enum ScenarioState {
        UNRESOLVED,
        PRIMARY_FORCES_COMMITTED,
        READY_TO_PLAY,
        COMPLETED,
        IGNORED,
        DEFEATED;
    }

    private AtBDynamicScenario backingScenario;
    private ScenarioState currentState;
    private StratconCampaignState campaignState;



    public void initializeScenario(Campaign campaign) {
        // scenario initialized from template - includes name, type, objectives, terrain/map, weather, lighting, gravity, atmo pressure
        // player assigns primary forces (list of ints that are force IDs)
        // roll events
        // generate allied primary forces (if any)
        // generate attached unit(s) (if any)
        // generate opposing forces (based on primary force BV)
        // make event adjustments to scenario forces
        // player assigns reinforcements (list of ints that are force IDs)
        // [at any point afterwards] assign external forces from other scenario
        backingScenario = new AtBDynamicScenario();
        //backingScenario.initBattleEnvironment(campaign);
    }

    public void commitPrimaryForces(List<Force> primaryForces) {
        //backingScenario.setPrimaryPlayerForces(primaryForces);
        currentState = ScenarioState.PRIMARY_FORCES_COMMITTED;

        // now that we have the primary forces, we need to:
        // a) roll events, immediately applying any that are relevant
        // b) determine if there are any allied primary forces
        // c) generate attached unit(s) (if any)
        // d) apply any event-related adjustments
        // e) place the scenario in the briefing room
        generateEvents();
        processPreForceGenerationEvents();
        generateAlliedForces();
        generateAttachedUnits();
        generateOpposingForces();
    }

    public void generateEvents() {

    }

    public void processPreForceGenerationEvents() {

    }

    public void generateAlliedForces() {

    }

    public void generateAttachedUnits() {

    }

    public void generateOpposingForces() {
        // calculate BV budget.
        // BV budget is defined as ((primary player force BV * difficulty multiplier) + primary allied/not attached force BV) 
        // * scenario multiplier
        // * campaign state multiplier

    }

    public void processPostForceGenerationEvents() {

    }

    public ScenarioState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(ScenarioState state) {
        currentState = state;
    }

    public String getInfo() {
        StringBuilder stateBuilder = new StringBuilder();
        stateBuilder.append("<html>");

        switch(currentState) {
        case UNRESOLVED:
            stateBuilder.append("Unresolved");
            break;
        case COMPLETED:
            stateBuilder.append("Completed");
            break;
        case IGNORED:
            stateBuilder.append("Ignored");
            break;
        case DEFEATED:
            stateBuilder.append("Defeated");
            break;
        default:
            stateBuilder.append("Unknown");
            break;
        }

        stateBuilder.append("</html>");
        return stateBuilder.toString();
    }
}