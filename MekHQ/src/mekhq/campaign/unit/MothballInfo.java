package mekhq.campaign.unit;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import megamek.common.Dropship;
import megamek.common.Infantry;
import megamek.common.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;

/**
 * This class is used to store information about a particular unit that is
 * lost when a unit is mothballed, so that it may be restored to as close to
 * its prior state as possible when the unit is reactivated.
 * @author NickAragua
 *
 */
public class MothballInfo {
    private UUID techID;
    private int forceID;
    private List<UUID> driverIDs;
    private List<UUID> gunnerIDs;
    private List<UUID> vesselCrewIDs;
    private UUID techOfficerID;
    private UUID navigatorID;
    
    /**
     * Creates a set of mothball info for a given unit
     * @param unit The unit to work with
     */
    public MothballInfo(Unit unit) {
        techID = unit.getTechId();
        forceID = unit.getForceId();
        driverIDs = (List<UUID>) unit.getDriverIDs().clone();
        gunnerIDs = (List<UUID>) unit.getGunnerIDs().clone();
        vesselCrewIDs = (List<UUID>) unit.getVesselCrewIDs().clone();
        techOfficerID = unit.getTechOfficerID();
        navigatorID = unit.getNavigatorID();
    }
    
    /**
     * Restore a unit's pilot, assigned tech and force, to the best of our ability
     * @param unit The unit to restore
     * @param campaign The campaign in which this is happening
     */
    public void restorePreMothballInfo(Unit unit, Campaign campaign) 
    {
        Person tech = campaign.getPerson(techID);
        if(tech != null) {
            unit.setTech(tech);
        }
        
        for(UUID driverID : driverIDs) {
            Person driver = campaign.getPerson(driverID);
            
            if(driver != null) {
                unit.addDriver(driver);
            }
        }
        
        for(UUID gunnerID : gunnerIDs) {
            Person gunner = campaign.getPerson(gunnerID);
            
            if(gunner != null) {
                unit.addGunner(gunner);
            }
        }
        
        for(UUID vesselCrewID : vesselCrewIDs) {
            Person vesselCrew = campaign.getPerson(vesselCrewID);
            
            if(vesselCrew != null) {
                unit.addVesselCrew(vesselCrew);
            }
        }
        
        Person techOfficer = campaign.getPerson(techOfficerID);
        if(techOfficer != null) {
            unit.setTechOfficer(techOfficer);
        }
        
        Person navigator = campaign.getPerson(navigatorID);
        if(navigator != null) {
            unit.setNavigator(navigator);
        }
        
        if(campaign.getForce(forceID) != null) {
            campaign.addUnitToForce(unit, forceID);
        }
    }
}
