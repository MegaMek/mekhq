package mekhq.campaign.stratcon;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.Coords;
import mekhq.campaign.Campaign;
import mekhq.campaign.force.Force;
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
    
    public List<StratconTrackState> getTracks() {
        return tracks;
    }
    
    public void addTrack(StratconTrackState track) {
        tracks.add(track);
    }
    
    public void generateScenariosForTracks() {
        for(StratconTrackState track : tracks) {
            track.generateScenarios(campaign, getContract());
        }
    }
    
    /**
     * This is a list of all force IDs for forces that 
     * a) have not been assigned to a track
     * b) are combat-capable
     * c) are not deployed to a scenario
     * @return
     */
    public List<Integer> getAvailableForceIDs() {
        List<Integer> retVal = new ArrayList<>();
        
        Set<Integer> forcesInTracks = new HashSet<>();
        for(StratconTrackState track : tracks) {
            forcesInTracks.addAll(track.getAssignedForceIDs());
        }
        
        for(int key : campaign.getLances().keySet()) {
            Force force = campaign.getForce(key);
            if(force != null && 
                    !force.isDeployed() && 
                    !force.getUnits().isEmpty() &&
                    !forcesInTracks.contains(force.getId())) {
                retVal.add(force.getId());
            }
        }
        
        return retVal;
    }
}