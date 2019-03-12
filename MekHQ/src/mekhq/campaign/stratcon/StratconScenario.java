package mekhq.campaign.stratcon;

import java.util.List;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;

public class StratconScenario implements IStratconDisplayable {
    public enum ScenarioState {
        UNRESOLVED,
        PRIMARY_FORCES_COMMITTED,
        COMPLETED,
        IGNORED,
        DEFEATED;
    }
    
    private String SCENARIO_MODIFIER_ALLIED_GROUND_UNITS = "";
    private String SCENARIO_MODIFIER_ALLIED_AIR_UNITS = "";
    private String SCENARIO_MODIFIER_LIAISON = "";
    private String SCENARIO_MODIFIER_HOUSE_CO = "";
    private String SCENARIO_MODIFIER_INTEGRATED_UNITS = "";

    private AtBDynamicScenario backingScenario;
    private ScenarioState currentState;
    private int requiredPlayerLances;
    private boolean requiredScenario;

    public void initializeScenario(Campaign campaign, AtBContract contract, MapLocation location) {
        // scenario initialized from template - includes name, type, objectives, terrain/map, weather, lighting, gravity, atmo pressure
        // player assigns primary forces (list of ints that are force IDs)
        // roll events
        // generate allied primary forces (if any)
        // generate attached unit(s) (if any)
        // generate opposing forces (based on primary force BV)
        // make event adjustments to scenario forces
        // player assigns reinforcements (list of ints that are force IDs)
        // [at any point afterwards] assign external forces from other scenario
        ScenarioTemplate sourceTemplate = StratconScenarioFactory.getRandomScenario(location);
        initializeScenario(campaign, contract, sourceTemplate);
    }
    
    public void initializeScenario(Campaign campaign, AtBContract contract, int unitType) {
        ScenarioTemplate sourceTemplate = StratconScenarioFactory.getRandomScenario(unitType);
        initializeScenario(campaign, contract, sourceTemplate);
    }
    
    private void initializeScenario(Campaign campaign, AtBContract contract, ScenarioTemplate template) {
        backingScenario = AtBDynamicScenarioFactory.initializeScenarioFromTemplate(template, contract, campaign);
        
        // do an appropriate allied force if the contract calls for it
        // do any attached or integrated units
        setAlliedForceModifier(contract);
        setAttachedUnitsModifier(contract);
        
        if((contract.getCommandRights() == AtBContract.COM_HOUSE) ||
                (contract.getCommandRights() == AtBContract.COM_INTEGRATED)) {
            requiredScenario = true;
        }
        
        AtBDynamicScenarioFactory.setScenarioModifiers(backingScenario);
        AtBDynamicScenarioFactory.applyScenarioModifiers(backingScenario, campaign, EventTiming.PreForceGeneration);
    }

    public void addPrimaryForce(int forceID) {
        backingScenario.addForces(forceID);
    }
    
    public void commitPrimaryForces(Campaign campaign, AtBContract contract) {
        //backingScenario.setPrimaryPlayerForces(primaryForces);
        currentState = ScenarioState.PRIMARY_FORCES_COMMITTED;

        // now that we have the primary forces, we need to:
        // a) roll events, immediately applying any that are relevant
        // b) determine if there are any allied primary forces
        // c) generate attached unit(s) (if any)
        // d) apply any event-related adjustments
        // e) place the scenario in the briefing room
        generateEvents();
        AtBDynamicScenarioFactory.finalizeScenario(backingScenario, contract, campaign);
    }

    public void generateEvents() {

    }

    public void processPreForceGenerationEvents() {

    }

    /**
     * Set up the appropriate primary allied force modifier, if any 
     * @param contract The scenario's contract.
     */
    private void setAlliedForceModifier(AtBContract contract) {
        int alliedUnitOdds = 0;
        
        // first, we determine the odds of having an allied unit present
        if(contract.getMissionType() == AtBContract.MT_RIOTDUTY) {
            alliedUnitOdds = 50;
        } else {
            switch(contract.getCommandRights()) {
            case AtBContract.COM_INTEGRATED:
                alliedUnitOdds = 50;
                break;
            case AtBContract.COM_HOUSE:
                alliedUnitOdds = 30;
                break;
            case AtBContract.COM_LIAISON:
                alliedUnitOdds = 10;
                break;
            }
        }
        
        // if an allied unit is present, then we want to make sure that it's ground units
        // for ground battles
        if(Compute.randomInt(100) <= alliedUnitOdds) {
            if((backingScenario.getTemplate().mapParameters.getMapLocation() == MapLocation.LowAtmosphere) ||
               (backingScenario.getTemplate().mapParameters.getMapLocation() == MapLocation.Space)) {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_ALLIED_AIR_UNITS));
            } else {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_ALLIED_GROUND_UNITS));
            }
        }
    }

    /**
     * Set the 'attached' units modifier for the current scenario (integraed, house, liaison)
     * @param contract The scenario's contract
     */
    public void setAttachedUnitsModifier(AtBContract contract) {
        switch(contract.getCommandRights()) {
        case AtBContract.COM_INTEGRATED:
            backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_INTEGRATED_UNITS));
            break;
        case AtBContract.COM_HOUSE:
            backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_HOUSE_CO));
            break;
        case AtBContract.COM_LIAISON:
            if(isRequiredScenario()) {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_LIAISON));
            }
            break;
        }
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

    
    public int getRequiredPlayerLances() {
        return requiredPlayerLances;
    }
    

    public void setRequiredPlayerLances(int requiredPlayerLances) {
        this.requiredPlayerLances = requiredPlayerLances;
    }
    
    public void incrementRequiredPlayerLances() {
        requiredPlayerLances++;
    }

    public boolean isRequiredScenario() {
        return requiredScenario;
    }

    public void setRequiredScenario(boolean requiredScenario) {
        this.requiredScenario = requiredScenario;
    }
}