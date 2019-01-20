package mekhq.campaign.universe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import megamek.client.ratgenerator.FactionRecord;
import megamek.client.ratgenerator.MissionRole;
import megamek.client.ratgenerator.ModelRecord;
import megamek.client.ratgenerator.UnitTable.Parameters;
import megamek.common.EntityMovementMode;
import megamek.common.MechSummary;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;

/**
 * Data structure that contains parameters relevant to unit generation via the IUnitGenerator interface
 * and is capable of translating itself to megamek.client.ratgenerator.parameters
 * @author NickAragua
 *
 */
public class UnitGeneratorParameters {
    private String faction;
    private int unitType;
    private int weightClass;
    private int year;
    private int quality;
    private Collection<EntityMovementMode> movementModes;
    private Predicate<MechSummary> filter;
    private Collection<MissionRole> missionRoles;
    
    public UnitGeneratorParameters() {
        movementModes = new ArrayList<>();
        setMissionRoles(new ArrayList<>());
    }
    
    /**
     * Translate the contents of this data structure into a megamek.client.ratgenerator.Parameters object
     * @return
     */
    public Parameters getRATGeneratorParameters() {
        FactionRecord fRec = Faction.getFactionRecordOrFallback(getFaction());
        String rating = RATGeneratorConnector.getFactionSpecificRating(fRec, getQuality());
        List<Integer> weightClasses = new ArrayList<>();
        
        if(getWeightClass() != AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED) {
            weightClasses.add(getWeightClass());
        }
        
        Parameters params = new Parameters(fRec, getUnitType(), getYear(), rating, weightClasses, ModelRecord.NETWORK_NONE,
                getMovementModes(), getMissionRoles(), 2, fRec);
        
        return params;
    }

    public String getFaction() {
        return faction;
    }

    public void setFaction(String faction) {
        this.faction = faction;
    }

    public int getUnitType() {
        return unitType;
    }

    public void setUnitType(int unitType) {
        this.unitType = unitType;
    }

    public int getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(int weightClass) {
        this.weightClass = weightClass;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }

    public Collection<EntityMovementMode> getMovementModes() {
        return movementModes;
    }
    
    public void setMovementModes(Collection<EntityMovementMode> movementModes) {
        this.movementModes = movementModes;
    }
    
    public void clearMovementModes() {
        movementModes.clear();
    }

    public Collection<MissionRole> getMissionRoles() {
        return missionRoles;
    }

    public void setMissionRoles(Collection<MissionRole> missionRoles) {
        this.missionRoles = missionRoles;
    }
    
    public void clearMissionRoles() {
        missionRoles.clear();
    }
    
    public void addMissionRole(MissionRole role) {
        missionRoles.add(role);
    }

    public Predicate<MechSummary> getFilter() {
        return filter;
    }

    public void setFilter(Predicate<MechSummary> filter) {
        this.filter = filter;
    }
}
