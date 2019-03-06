package mekhq.campaign.stratcon;

import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;

/**
 * This represents a facility in the StratCon context
 * @author NickAragua
 *
 */
public class StratconFacility {
    public enum FacilityType {
        MekBase,
        TankBase,
        AirBase,
        ArtilleryBase,
        SupplyDepot,
        DataCenter,
        IndustrialFacility,
        CommandCenter,
        EarlyWarningSystem,
        OrbitalDefense,
        BaseOfOperations
    }
    
    private ForceAlignment owner;
    private String displayableName;
    // we'll want to store a garrison here as well
    
    public ForceAlignment getOwner() {
        return owner;
    }
    
    public void setOwner(ForceAlignment owner) {
        this.owner = owner;
    }
    
    public String getDisplayableName() {
        return displayableName;
    }
    
    public void setDisplayableName(String displayableName) {
        this.displayableName = displayableName;
    }
}
