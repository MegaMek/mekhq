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
    private FacilityType facilityType;
    private boolean visible;
    // we'll want to store a garrison here as well eventually
    
    public ForceAlignment getOwner() {
        return owner;
    }
    
    public void setOwner(ForceAlignment owner) {
        this.owner = owner;
    }
    
    public String getDisplayableName() {
        return String.format("%s: %s", displayableName, facilityType.toString());
    }
    
    public void setDisplayableName(String displayableName) {
        this.displayableName = displayableName;
    }

    public FacilityType getFacilityType() {
        return facilityType;
    }

    public void setFacilityType(FacilityType facilityType) {
        this.facilityType = facilityType;
    }

    public boolean getVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    public boolean isVisible() {
        return (owner == ForceAlignment.Allied) || visible;
    }
}
