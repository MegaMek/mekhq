package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconFacility.FacilityType;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.universe.Planet;

/**
 * This class contains "rules" logic for the AtB-Stratcon state
 * @author NickAragua
 *
 */
public class StratconRulesManager {
    public static final int NUM_LANCES_PER_TRACK = 3;    
    
    public static StratconCampaignState InitializeCampaignState(AtBContract contract, Campaign campaign) {
        StratconCampaignState retVal = new StratconCampaignState(campaign, contract);
        
        // a campaign will have X tracks going at a time, where
        // X = # required lances / 3, rounded up. The last track will have fewer required lances.
        int oddLanceCount = contract.getRequiredLances() % NUM_LANCES_PER_TRACK;
        if(oddLanceCount > 0) {
            StratconTrackState track = InitializeTrackState(retVal, oddLanceCount);
            track.setDisplayableName("Odd Track");
            retVal.addTrack(track);
        }
        
        for(int x = 0; x < contract.getRequiredLances() / NUM_LANCES_PER_TRACK; x++) {
            StratconTrackState track = InitializeTrackState(retVal, NUM_LANCES_PER_TRACK);
            track.setDisplayableName(String.format("Track %d", x));
            retVal.addTrack(track);
        }
        
        return retVal;
    }
    
    public static StratconTrackState InitializeTrackState(StratconCampaignState campaignState, int numLances) {
        // to initialize a track, 
        // 1. we set the # of required lances
        // 2. set the track size to a total of numlances * 28 hexes, a rectangle that is wider than it is taller
        //      the idea being to create a roughly rectangular playing field that,
        //      if one deploys a scout lance each week to a different spot, can be more or less fully covered
        // 3. set up numlances facilities in random spots on the track
        
        StratconTrackState retVal = new StratconTrackState();
        retVal.setRequiredLanceCount(numLances);
        
        // set width and height
        int numHexes = numLances * 28;
        int height = (int) Math.floor(Math.sqrt(numHexes));
        int width = numHexes / height;
        retVal.setWidth(width);
        retVal.setHeight(height);
        
        // generate the facilities
        for(int fCount = 0; fCount < numLances; fCount++) {
            StratconFacility sf = new StratconFacility();
            sf.setOwner(ForceAlignment.Opposing);
            
            int fIndex = Compute.randomInt(StratconFacility.FacilityType.values().length);
            sf.setFacilityType(FacilityType.values()[fIndex]);
            
            int x = Compute.randomInt(width);
            int y = Compute.randomInt(height);
            
            sf.setDisplayableName(String.format("Facility %d,%d", x, y));
            
            retVal.addFacility(new Coords(x, y), sf);
        }
        
        return retVal;
    }

    public static void generateScenariosForTrack(Campaign campaign, AtBContract contract, StratconTrackState track) {
        List<StratconScenario> generatedScenarios = new ArrayList<>();
        boolean autoAssignLances = contract.getCommandRights() == AtBContract.COM_INTEGRATED;
        
        //StratconScenarioFactory.reloadScenarios();
        
        // create scenario for each force if we roll higher than the track's scenario odds
        // if we already have a scenario in the given coords, let's make it a larger battle instead
        // otherwise, generate an appropriate scenario for the unit type of the force being examined
        for(int forceID : track.getAssignedForceIDs()) {
            if(Compute.randomInt(100) > track.getScenarioOdds()) {
                // get coordinates
                int x = Compute.randomInt(track.getWidth());
                int y = Compute.randomInt(track.getHeight());                
                
                Coords scenarioCoords = new Coords(x, y);
                
                if(track.getScenarios().containsKey(scenarioCoords)) {
                    track.getScenarios().get(scenarioCoords).incrementRequiredPlayerLances();
                    // if under integrated command, automatically assign the lance to the scenario
                    if(autoAssignLances) {
                        track.getScenarios().get(scenarioCoords).addPrimaryForce(forceID);
                    }
                    continue;
                }
                
                StratconScenario scenario = new StratconScenario();
                scenario.initializeScenario(campaign, contract, campaign.getForce(forceID).getPrimaryUnitType(campaign));
                
                // set up deployment day, battle day, return day here
                
                track.getScenarios().put(scenarioCoords, scenario);
                generatedScenarios.add(scenario);
                
                // if under integrated command, automatically assign the lance to the scenario
                if(autoAssignLances) {
                    scenario.addPrimaryForce(forceID);
                }
            }
        }
        
        // if under integrated command, automatically assign the lance to the scenario
        if(autoAssignLances) {
            for(StratconScenario scenario : generatedScenarios) {
                scenario.commitPrimaryForces(campaign, contract);
            }
        }
        
        // if under liaison command, pick a random scenario from the ones generated
        // to set as required and attach liaison
        if(contract.getCommandRights() == AtBContract.COM_HOUSE) {
            int scenarioIndex = Compute.randomInt(generatedScenarios.size() - 1);
            generatedScenarios.get(scenarioIndex).setRequiredScenario(true);
            generatedScenarios.get(scenarioIndex).setAttachedUnitsModifier(contract);
        }
    }
    
    /**
     * Determine whether the user should be nagged about unresolved scenarios on AtB Stratcon tracks.
     * @param campaign Campaign to check.
     * @return To nag or not to nag.
     */
    public static boolean nagUnresolvedContacts(Campaign campaign) {
        // check every track attached to an active contract for unresolved scenarios
        // to which the player must deploy forces today
        for(Contract contract : campaign.getActiveContracts()) {
            if(contract instanceof AtBContract) {
                for(StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                    for(StratconScenario scenario : track.getScenarios().values()) {
                        //scenario.getCurrentState()
                        if(scenario.getCurrentState() == ScenarioState.UNRESOLVED &&
                                campaign.getCalendar().getTime().equals(scenario.getDeploymentDate())) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }
    
    public StratconRulesManager() {
        MekHQ.registerHandler(this);
    }
    
    @Subscribe
    public void handleNewDay(NewDayEvent ev) {
        if(ev.getCampaign().getCalendar().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
            // run scenario generation routine for every track attached to an active contract
            for(Contract contract : ev.getCampaign().getActiveContracts()) {
                if(contract instanceof AtBContract) {
                    for(StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                        generateScenariosForTrack(ev.getCampaign(), (AtBContract) contract, track);
                    }
                }
            }
        }
    }
    
    public void shutdown() {
        MekHQ.unregisterHandler(this);
    }
}
