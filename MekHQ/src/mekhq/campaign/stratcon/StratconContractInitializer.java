package mekhq.campaign.stratcon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import megamek.common.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.ScenarioTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.mission.atb.AtBScenarioModifier;
import mekhq.campaign.stratcon.StratconContractDefinition.ObjectiveParameters;
import mekhq.campaign.stratcon.StratconContractDefinition.StrategicObjectiveType;

public class StratconContractInitializer {
    public static final int NUM_LANCES_PER_TRACK = 3;
    
    public static void InitializeCampaignState(AtBContract contract, Campaign campaign, StratconContractDefinition contractDefinition) {
        StratconCampaignState campaignState = new StratconCampaignState(contract);
        
        // go through each objective in the contract definition.
        // make counts of allied/hostile objective facilities and objective scenarios
        Map<StrategicObjectiveType, Integer> objectiveCounts;
        
        /*for(ObjectiveParameters objectiveParams : contractDefinition.getObjectiveParameters()) {
            if(objectiveParams.objectiveCount == StratconContractDefinition.COUNT_SCALED) {
                objectiveCounts.compute(objectiveParams.objectiveType, 
                        (k, v) -> v == null ? contract.getRequiredLances() : v + contract.getRequiredLances());
            } else {
                objectiveCounts.compute(objectiveParams.objectiveType, 
                        (k, v) -> v == null ? objectiveParams.objectiveCount : v + objectiveParams.objectiveCount);
            }
        }*/
        
        /*int numTracks = contract.getRequiredLances() / NUM_LANCES_PER_TRACK;
        //int objectives
        
        
        for(int x = 0; x < contract.getRequiredLances() / NUM_LANCES_PER_TRACK; x++) {
            int splitNumObjectives = (int) Math.ceil(contractDefinition.getObjectiveCount() / 3);
            
            
            StratconTrackState track = InitializeTrackState(campaign, campaignState, 
                    NUM_LANCES_PER_TRACK, contractDefinition.getAlliedFacilityCount(), contractDefinition.getHostileFacilityCount(),
                    contractDefinition.getO);
            track.setDisplayableName(String.format("Track %d", x));
            campaignState.addTrack(track);
        }
        
        // a campaign will have X tracks going at a time, where
        // X = # required lances / 3, rounded up. The last track will have fewer required lances.
        int oddLanceCount = contract.getRequiredLances() % NUM_LANCES_PER_TRACK;
        if(oddLanceCount > 0) {
            StratconTrackState track = InitializeTrackState(campaignState, oddLanceCount);
            track.setDisplayableName(String.format("Track %d", campaignState.getTracks().size()));
            campaignState.addTrack(track);
        }*/
        
        contract.setStratconCampaignState(campaignState);
    }
    
    public static StratconTrackState initializeTrackState(Campaign campaign, StratconCampaignState campaignState, 
            int numLances, int numAlliedFacilities, int numHostileFacilities, int numObjectiveScenarios, 
            int numAlliedObjectiveFacilities, int numHostileObjectiveFacilities, 
            List<String> objectiveScenarios, List<String> objectiveModifiers) {
        // to initialize a track, 
        // 1. we set the # of required lances
        // 2. set the track size to a total of numlances * 28 hexes, a rectangle that is wider than it is taller
        //      the idea being to create a roughly rectangular playing field that,
        //      if one deploys a scout lance each week to a different spot, can be more or less fully covered
        // 3. set up numlances facilities (?) in random spots on the track
        // 4. set up scenarios
        
        StratconTrackState retVal = new StratconTrackState();
        retVal.setRequiredLanceCount(numLances);
        
        // set width and height
        int numHexes = numLances * 28;
        int height = (int) Math.floor(Math.sqrt(numHexes));
        int width = numHexes / height;
        retVal.setWidth(width);
        retVal.setHeight(height);
        
        // plop down objective scenarios first
        initializeObjectiveScenarios(campaign, campaignState.getContract(), retVal, 
                numObjectiveScenarios, objectiveScenarios, objectiveModifiers);
        
        // plop down objective facilities second
        initializeTrackFacilities(retVal, numAlliedObjectiveFacilities, ForceAlignment.Allied, null, true);
        initializeTrackFacilities(retVal, numHostileObjectiveFacilities, ForceAlignment.Allied, null, true);
        
        // plop down non-objective facilities last
        initializeTrackFacilities(retVal, numAlliedFacilities, ForceAlignment.Allied, null, false);
        initializeTrackFacilities(retVal, numHostileFacilities, ForceAlignment.Opposing, null, false);
        
        return retVal;
    }
    
    /**
     * Worker function that takes a trackstate and plops down the given number of facilities owned by the given faction
     * Avoids places with existing facilities and scenarios, capable of taking facility sub set and setting strategic objective flag.
     */
    private static void initializeTrackFacilities(StratconTrackState trackState, int numFacilities, ForceAlignment owner, 
            List<String> facilitySubset, boolean strategicObjective) {
        for (int fCount = 0; fCount < numFacilities; fCount++) {
            StratconFacility sf =
                    facilitySubset == null ? 
                    StratconFacilityFactory.getRandomFacility() :
                    StratconFacilityFactory.getFacilityByName(facilitySubset.get(Compute.randomInt(facilitySubset.size())));
            sf.setOwner(owner);
            sf.setStrategicObjective(strategicObjective);
            
            int x = Compute.randomInt(trackState.getWidth());
            int y = Compute.randomInt(trackState.getHeight());
            StratconCoords coords = new StratconCoords(x, y);
            
            // make sure we don't put the facility down on top of anything else
            while ((trackState.getFacility(coords) != null) ||
                    (trackState.getScenario(coords) != null)) {
                x = Compute.randomInt(trackState.getWidth());
                y = Compute.randomInt(trackState.getHeight());
                coords = new StratconCoords(x, y);
            }
            
            trackState.addFacility(coords, sf);
            if (sf.getOwner() == ForceAlignment.Allied) {
                trackState.getRevealedCoords().add(coords);
            }
        }
    }
    
    /**
     * Worker function that takes a trackstate and plops down the given number of facilities owned by the given faction
     */
    private static void initializeObjectiveScenarios(Campaign campaign, AtBContract contract, StratconTrackState trackState, 
            int numScenarios, List<String> objectiveScenarios, List<String> objectiveModifiers) {
        // pick scenario from subset
        // place it on the map somewhere nothing else has been placed yet
        // if it's a facility scenario, place the facility
        // run generateScenario() to apply all the necessary mods
        // apply objective mods (?)
        
        
        for (int sCount = 0; sCount < numScenarios; sCount++) {
            // pick
            ScenarioTemplate template = StratconScenarioFactory.getSpecificScenario(
                    objectiveScenarios.get(Compute.randomInt(objectiveScenarios.size())));
            
            // plonk
            int x = Compute.randomInt(trackState.getWidth());
            int y = Compute.randomInt(trackState.getHeight());
            StratconCoords coords = new StratconCoords(x, y);
            
            // make sure we don't put the facility down on top of anything else
            while ((trackState.getFacility(coords) != null) ||
                    (trackState.getScenario(coords) != null)) {
                x = Compute.randomInt(trackState.getWidth());
                y = Compute.randomInt(trackState.getHeight());
                coords = new StratconCoords(x, y);
            }
            
            // facility
            if(template.isFacilityScenario()) {
                StratconFacility facility = template.isHostileFacility() ?
                        StratconFacilityFactory.getRandomHostileFacility() : StratconFacilityFactory.getRandomAlliedFacility();
                trackState.addFacility(coords, facility);
            }
            
            // create scenario - don't assign a force yet
            StratconScenario scenario = StratconRulesManager.generateScenario(campaign, contract, trackState, Force.FORCE_NONE, coords, template);
            
            // clear dates, because we don't want the scenario disappearing on us
            scenario.setDeploymentDate(null);
            scenario.setActionDate(null);
            scenario.setReturnDate(null);
            
            // apply objective mods
            for(String modifier : objectiveModifiers) {
                scenario.getBackingScenario().addScenarioModifier(AtBScenarioModifier.getScenarioModifier(modifier));
            }
            
            trackState.addScenario(scenario);
        }
    }
    
    public static int calculateNumFacilities(StratconContractDefinition contractDefinition, StratconCampaignState campaignState) {
        return 0;
    }
    
    
}
