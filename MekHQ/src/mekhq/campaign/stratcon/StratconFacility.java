package mekhq.campaign.stratcon;

import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.mission.ScenarioForceTemplate.ForceAlignment;

/**
 * This represents a facility in the StratCon context
 * @author NickAragua
 *
 */
public class StratconFacility implements Cloneable {
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
    private List<String> sharedModifiers = new ArrayList<>();
    private List<String> localModifiers = new ArrayList<>();
    // we'll want to store a garrison here as well eventually
    
    public static StratconFacility createTestFacility() {
        StratconFacility test = new StratconFacility();
        test.displayableName = "test facility";
        test.facilityType = FacilityType.TankBase;
        test.sharedModifiers.add("AlliedTankGarrison.xml");
        test.localModifiers.add("AlliedTankGarrison.xml");
        test.localModifiers.add("AlliedTankGarrison.xml");
        test.owner = ForceAlignment.Opposing;
        return test;
    }
    
    public Object clone() {
        StratconFacility clone = new StratconFacility();
        clone.owner = owner;
        clone.displayableName = displayableName;
        clone.facilityType = facilityType;
        clone.visible = visible;
        clone.sharedModifiers = new ArrayList<>(sharedModifiers);
        clone.localModifiers = new ArrayList<>(localModifiers);
        return clone;
    }
    
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

    /**
     * This is a list of scenario modifier IDs that affect scenarios in the same track as this facility.
     */
    public List<String> getSharedModifiers() {
        return sharedModifiers;
    }

    public void setSharedModifiers(List<String> sharedModifiers) {
        this.sharedModifiers = sharedModifiers;
    }

    /**
     * This is a list of scenario modifier IDs that affect scenarios involving this facility directly. 
     */
    public List<String> getLocalModifiers() {
        return localModifiers;
    }

    public void setLocalModifiers(List<String> localModifiers) {
        this.localModifiers = localModifiers;
    }
}
