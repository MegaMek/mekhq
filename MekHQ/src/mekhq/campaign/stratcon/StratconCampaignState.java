package mekhq.campaign.stratcon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import megamek.common.Coords;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;

public class StratconCampaignState {
    private Campaign campaign;
    private AtBContract contract;

    // these are all state variables that affect the current Stratcon Campaign
    private double globalOpforBVMultiplier;
    private int supportPoints;
    private int victoryPoints;
    private int strategicObjectivePoints;
    private List<StratconTrackState> tracks;

    public AtBContract getContract() {
        return contract;
    }

    public void setContract(AtBContract contract) {
        this.contract = contract;
    }

    public StratconCampaignState(Campaign campaign, AtBContract contract) {
        tracks = new ArrayList<>();
        this.campaign = campaign; 
        this.setContract(contract);
        
        StratconTrackState testTrack = new StratconTrackState();
        testTrack.setDisplayableName("Test Track");
        testTrack.setHeight(8);
        testTrack.setWidth(12);
        
        tracks.add(testTrack);
    }

    /**
     * The opfor BV multiplier. Intended to be additive.
     * @return The additive opfor BV multiplier.
     */
    public double getGlobalOpforBVMultiplier() {
        return globalOpforBVMultiplier;
    }
    
    public StratconTrackState getTrack(int index) {
        return tracks.get(index);
    }
    
    public void generateScenariosForTracks() {
        for(StratconTrackState track : tracks) {
            track.generateScenarios(campaign, getContract());
        }
    }
}