package mekhq.campaign.stratcon;

import megamek.common.Compute;
import megamek.common.Coords;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.stratcon.StratconFacility.FacilityType;
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
            retVal.addTrack(InitializeTrackState(retVal, oddLanceCount));
        }
        
        for(int x = 0; x < contract.getRequiredLances() / NUM_LANCES_PER_TRACK; x++) {
            retVal.addTrack(InitializeTrackState(retVal, NUM_LANCES_PER_TRACK));
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
}
