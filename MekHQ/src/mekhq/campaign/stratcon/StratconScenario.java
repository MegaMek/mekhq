package mekhq.campaign.stratcon;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.mission.atb.AtBScenarioModifier.EventTiming;

/**
 * Class that handles scenario metadata and interaction at the StratCon level
 * @author NickAragua
 *
 */
public class StratconScenario implements IStratconDisplayable {
    public enum ScenarioState {
        NONEXISTENT,
        UNRESOLVED,
        PRIMARY_FORCES_COMMITTED,
        COMPLETED,
        IGNORED,
        DEFEATED;
    }
    
    private final static Map<ScenarioState, String> scenarioStateNames;
    
    static {
        scenarioStateNames = new HashMap<>();
        scenarioStateNames.put(ScenarioState.NONEXISTENT, "Shouldn't be seen");
        scenarioStateNames.put(ScenarioState.UNRESOLVED, "Unresolved");
        scenarioStateNames.put(ScenarioState.PRIMARY_FORCES_COMMITTED, "Primary forces committed");
        scenarioStateNames.put(ScenarioState.COMPLETED, "Victory");
        scenarioStateNames.put(ScenarioState.IGNORED, "Ignored");
        scenarioStateNames.put(ScenarioState.DEFEATED, "Defeat");
    }
    
    
    private String SCENARIO_MODIFIER_ALLIED_GROUND_UNITS = "PrimaryAlliesGround.xml";
    private String SCENARIO_MODIFIER_ALLIED_AIR_UNITS = "PrimaryAlliesAir.xml";
    private String SCENARIO_MODIFIER_LIAISON_GROUND = "LiaisonGround.xml";
    private String SCENARIO_MODIFIER_HOUSE_CO_GROUND = "HouseOfficerGround.xml";
    private String SCENARIO_MODIFIER_INTEGRATED_UNITS_GROUND = "IntegratedAlliesGround.xml";
    private String SCENARIO_MODIFIER_LIAISON_AIR = "LiaisonAir.xml";
    private String SCENARIO_MODIFIER_HOUSE_CO_AIR = "HouseOfficerAir.xml";
    private String SCENARIO_MODIFIER_INTEGRATED_UNITS_AIR = "IntegratedAlliesAir.xml";
    private String SCENARIO_MODIFIER_TRAINEES_AIR = "AlliedTraineesAir.xml";
    private String SCENARIO_MODIFIER_TRAINEES_GROUND = "AlliedTraineesGround.xml";
    
    private AtBDynamicScenario backingScenario;
    private ScenarioState currentState = ScenarioState.UNRESOLVED;
    private int requiredPlayerLances;
    private boolean requiredScenario;
    private Date deploymentDate;
    private Date actionDate;
    private Date returnDate;
    
    private Campaign currentCampaign;

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
        if(sourceTemplate != null) {
            initializeScenario(campaign, contract, sourceTemplate);
        }
    }
    
    public void initializeScenario(Campaign campaign, AtBContract contract, int unitType) {
        ScenarioTemplate sourceTemplate = StratconScenarioFactory.getRandomScenario(unitType);

        if(sourceTemplate != null) {
            initializeScenario(campaign, contract, sourceTemplate);
        }
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
        setCurrentState(ScenarioState.UNRESOLVED);
        
        // retain the current campaign for convenience
        currentCampaign = campaign;
    }

    public void addPrimaryForce(int forceID) {
        backingScenario.addForces(forceID);
    }
    
    public void addForces(Set<Integer> forceIDs) {
        for(int forceID : forceIDs) {
            if(!backingScenario.getForceIDs().contains(forceID)) {
                addPrimaryForce(forceID);
            }
        }
    }
    
    public void clearReinforcements() {
        for(int forceID : backingScenario.getForceIDs()) {
            if(!backingScenario.getPlayerForceTemplates().containsKey(forceID)) {
                backingScenario.removeForce(forceID);
            }
        }
    }

    public List<Integer> getAssignedForces() {
        return backingScenario.getForceIDs();
    }
    
    public List<Integer> getPrimaryPlayerForceIDs() {
        return backingScenario.getPrimaryPlayerForceIDs();
    }
    
    /**
     * This method triggers the performance of all operations that should occur
     * after the player has committed the primary forces to this scenario.
     * @param campaign
     * @param contract
     */
    public void commitPrimaryForces(Campaign campaign, AtBContract contract) {
        currentState = ScenarioState.PRIMARY_FORCES_COMMITTED;

        AtBDynamicScenarioFactory.finalizeScenario(backingScenario, contract, campaign);
        
        campaign.addScenario(backingScenario, contract);
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
     * Set the 'attached' units modifier for the current scenario (integrated, house, liaison),
     * and make sure we're not deploying ground units to an air scenario
     * @param contract The scenario's contract
     */
    public void setAttachedUnitsModifier(AtBContract contract) {
        boolean airBattle = (backingScenario.getTemplate().mapParameters.getMapLocation() == MapLocation.LowAtmosphere) ||
                (backingScenario.getTemplate().mapParameters.getMapLocation() == MapLocation.Space);
        
        // if we're on cadre duty, we're getting three trainees, period
        if(contract.getMissionType() == AtBContract.MT_CADREDUTY) {
            if(airBattle) {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_TRAINEES_AIR));                
            } else {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_TRAINEES_GROUND));
            }
            return;
        }
        
        // if we're under non-independent command rights, a supervisor may come along
        switch(contract.getCommandRights()) {
        case AtBContract.COM_INTEGRATED:
            if(airBattle) {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_INTEGRATED_UNITS_AIR));                
            } else {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_INTEGRATED_UNITS_GROUND));
            }
            break;
        case AtBContract.COM_HOUSE:
            if(airBattle) {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_HOUSE_CO_AIR));
            } else {
                backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_HOUSE_CO_GROUND));
            }            
            break;
        case AtBContract.COM_LIAISON:
            if(isRequiredScenario()) {
                if(airBattle) {
                    backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_LIAISON_AIR));
                } else {
                    backingScenario.addScenarioModifier(AtBScenarioModifier.getScenarioModifier(SCENARIO_MODIFIER_LIAISON_GROUND));
                } 
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
        return getInfo(true);
    }
    
    public String getInfo(boolean includeForces) {
        StringBuilder stateBuilder = new StringBuilder();
        stateBuilder.append("<html>");

        stateBuilder.append(backingScenario.getName());
        stateBuilder.append("<br/>");
        stateBuilder.append(backingScenario.getTemplate().shortBriefing);
        stateBuilder.append("<br/>");
        
        stateBuilder.append("Status: ");
        stateBuilder.append(scenarioStateNames.get(currentState));

        if(includeForces) {
            List<UUID> unitIDs = backingScenario.getForces(currentCampaign).getAllUnits();
            
            if(!unitIDs.isEmpty()) {
                stateBuilder.append("<br/><br/>Assigned Units:<br/>");
                
                for(UUID unitID : unitIDs) {
                    stateBuilder.append("&nbsp;&nbsp;");
                    stateBuilder.append(currentCampaign.getUnit(unitID).getName());
                    stateBuilder.append("<br/>");
                }
            }
        }        

        stateBuilder.append("</html>");
        return stateBuilder.toString();
    }

    public String getName() {
        return backingScenario.getName();
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
    
    public AtBDynamicScenario getBackingScenario() {
        return backingScenario;
    }

    public Date getDeploymentDate() {
        return deploymentDate;
    }

    public void setDeploymentDate(Date deploymentDate) {
        this.deploymentDate = deploymentDate;
    }

    public Date getActionDate() {
        return actionDate;
    }

    public void setActionDate(Date actionDate) {
        this.actionDate = actionDate;
        backingScenario.setDate(actionDate);
    }

    public Date getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }
}