package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import megamek.common.Compute;
import megamek.common.Coords;
import megamek.common.UnitType;
import megamek.common.event.Subscribe;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.NewDayEvent;
import mekhq.campaign.force.Force;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;
import mekhq.campaign.personnel.SkillType;
import mekhq.campaign.stratcon.StratconFacility.FacilityType;
import mekhq.campaign.stratcon.StratconScenario.ScenarioState;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Planet;

/**
 * This class contains "rules" logic for the AtB-Stratcon state
 * @author NickAragua
 *
 */
public class StratconRulesManager {
    public static final int NUM_LANCES_PER_TRACK = 3;    
    
    public static StratconCampaignState InitializeCampaignState(AtBContract contract, Campaign campaign) {
        StratconCampaignState retVal = new StratconCampaignState(contract);
        
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
            
            retVal.addFacility(new StratconCoords(x, y), sf);
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
                
                StratconCoords scenarioCoords = new StratconCoords(x, y);
                
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
                // safety code to prevent attempts to generate random int with upper bound of 0 which is apparently illegal
                int deploymentDay = track.getDeploymentTime() < 7 ? Compute.randomInt(7 - track.getDeploymentTime()) : 0;
                int battleDay = deploymentDay + (track.getDeploymentTime() > 0 ? Compute.randomInt(track.getDeploymentTime()) : 0);
                int returnDay = deploymentDay + track.getDeploymentTime();
                
                GregorianCalendar deploymentDate = (GregorianCalendar) campaign.getCalendar().clone();
                deploymentDate.add(Calendar.DAY_OF_MONTH, deploymentDay);
                GregorianCalendar battleDate = (GregorianCalendar) campaign.getCalendar().clone();
                battleDate.add(Calendar.DAY_OF_MONTH, battleDay);
                GregorianCalendar returnDate = (GregorianCalendar) campaign.getCalendar().clone();
                returnDate.add(Calendar.DAY_OF_MONTH, returnDay);
                
                scenario.setDeploymentDate(deploymentDate.getTime());
                scenario.setActionDate(battleDate.getTime());
                scenario.setReturnDate(returnDate.getTime());
                
                track.getScenarios().put(scenarioCoords, scenario);
                generatedScenarios.add(scenario);
                campaign.addScenario(scenario.getBackingScenario(), contract);
                scenario.setBackingScenarioID(scenario.getBackingScenario().getId());
                
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
     * @return An informative string containing the reasons the user was nagged.
     */
    public static String nagUnresolvedContacts(Campaign campaign) {
        StringBuilder sb = new StringBuilder();
        
        // check every track attached to an active contract for unresolved scenarios
        // to which the player must deploy forces today
        for(Contract contract : campaign.getActiveContracts()) {
            if(contract instanceof AtBContract) {
                for(StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                    for(StratconScenario scenario : track.getScenarios().values()) {
                        if(scenario.getCurrentState() == ScenarioState.UNRESOLVED &&
                                campaign.getCalendar().getTime().equals(scenario.getDeploymentDate())) {
                            // "scenario name, track name"
                            sb.append(String.format("%s, %s\n", scenario.getName(), track.getDisplayableName()));
                        }
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Determine whether the user should be nagged about insufficient forces assigned to StratCon tracks
     * @param campaign Campaign to check.
     * @return An informative string containing the reasons the user was nagged.
     */
    public static String nagInsufficientTrackForces(Campaign campaign) {
        if(campaign.getCalendar().get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        
        // check every track attached to an active contract for unresolved scenarios
        // to which the player must deploy forces today
        for(Contract contract : campaign.getActiveContracts()) {
            if(contract instanceof AtBContract) {
                for(StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                    if(track.getAssignedForceIDs().size() < track.getRequiredLanceCount()) {
                        // "track name, x/y lances"
                        sb.append(String.format("%s, %d/%d lances\n", track.getDisplayableName(), 
                                track.getAssignedForceIDs().size(), track.getRequiredLanceCount()));
                    }
                }
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Determines whether the force in question has the same primary unit type as the force template.
     * @param force The force to check.
     * @param forceTemplate The force template to check.
     * @param campaign The working campaign.
     * @return Whether or not the unit types match.
     */
    public static boolean forceCompositionMatchesDeclaredUnitType(Force force, ScenarioForceTemplate forceTemplate, Campaign campaign) {
        int primaryUnitType = force.getPrimaryUnitType(campaign);
        
        // special cases are "ATB_MIX" and "ATB_AERO_MIX", which encompass multiple unit types
        if(forceTemplate.getAllowedUnitType() == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX) {
            // "AtB mix" is usually ground units, but air units can sub in
            return primaryUnitType == UnitType.MEK ||
                    primaryUnitType == UnitType.TANK ||
                    primaryUnitType == UnitType.INFANTRY ||
                    primaryUnitType == UnitType.BATTLE_ARMOR ||
                    primaryUnitType == UnitType.PROTOMEK || 
                    primaryUnitType == UnitType.VTOL ||
                    primaryUnitType == UnitType.AERO ||
                    primaryUnitType == UnitType.CONV_FIGHTER;
        } else if (forceTemplate.getAllowedUnitType() == ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_AERO_MIX) {
            return primaryUnitType == UnitType.AERO ||
                    primaryUnitType == UnitType.CONV_FIGHTER;
        } else {
            return primaryUnitType == forceTemplate.getAllowedUnitType();
        }
    }
    
    /**
     * This is a list of all force IDs for forces that can be deployed to a scenario in the given force template
     * a) have not been assigned to a track
     * b) are combat-capable
     * c) are not deployed to a scenario
     * @return
     */
    public static List<Integer> getAvailableForceIDs(ScenarioForceTemplate template, Campaign campaign) {
        List<Integer> retVal = new ArrayList<>();
        
        Set<Integer> forcesInTracks = new HashSet<>();
        for(Contract contract : campaign.getActiveContracts()) {
            if(contract instanceof AtBContract) {
                for(StratconTrackState track : ((AtBContract) contract).getStratconCampaignState().getTracks()) {
                    forcesInTracks.addAll(track.getAssignedForceIDs());
                }
            }
        }
        
        for(int key : campaign.getLances().keySet()) {
            Force force = campaign.getForce(key);
            if(force != null && 
                    !force.isDeployed() && 
                    (force.getScenarioId() <= 0) &&
                    !force.getUnits().isEmpty() &&
                    !forcesInTracks.contains(force.getId()) &&
                    forceCompositionMatchesDeclaredUnitType(force, template, campaign)) {
                retVal.add(force.getId());
            }
        }
        
        return retVal;
    }
    
    /**
     * Returns a list of individual units eligible for deployment in scenarios run by "Defend" lances
     * @param campaign
     * @return List of unit IDs.
     */
    public static List<UUID> getEligibleDefensiveUnits(Campaign campaign) {
        List<UUID> retVal = new ArrayList<>();
        
        for(Unit u : campaign.getUnits()) {
            // "defensive" units are infantry, battle armor and (Weisman help you) gun emplacements
            if(((u.getEntity().getUnitType() == UnitType.INFANTRY) || 
                    (u.getEntity().getUnitType() == UnitType.BATTLE_ARMOR) ||
                    (u.getEntity().getUnitType() == UnitType.GUN_EMPLACEMENT)) &&
                    (u.getScenarioId() <= 0)) {
                
                // this is a little inefficient, but probably there aren't too many active AtB contracts at a time
                for(Contract contract : campaign.getActiveContracts()) {
                    if((contract instanceof AtBContract) &&
                            ((AtBContract) contract).getStratconCampaignState().isForceDeployedHere(u.getForceId())) {
                        continue;
                    }
                }
                
                retVal.add(u.getId());
            }
        }
        
        return retVal;
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
