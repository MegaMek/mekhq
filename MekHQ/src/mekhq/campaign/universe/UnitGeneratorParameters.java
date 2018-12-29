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
    public String faction;
    public int unitType;
    public int weightClass;
    public int year;
    public int quality;
    public Collection<EntityMovementMode> movementModes;
    public Predicate<MechSummary> filter;
    public Collection<MissionRole> missionRoles;
    
    public UnitGeneratorParameters() {
        movementModes = new ArrayList<>();
        missionRoles = new ArrayList<>();
    }
    
    /**
     * Translate the contents of this data structure into a megamek.client.ratgenerator.Parameters object
     * @return
     */
    public Parameters getRATGeneratorParameters() {
        FactionRecord fRec = Faction.getFactionRecordOrFallback(faction);
        String rating = RATGeneratorConnector.getFactionSpecificRating(fRec, quality);
        List<Integer> weightClasses = new ArrayList<>();
        
        if(weightClass != AtBDynamicScenarioFactory.UNIT_WEIGHT_UNSPECIFIED) {
            weightClasses.add(weightClass);
        }
        
        Parameters params = new Parameters(fRec, unitType, year, rating, weightClasses, ModelRecord.NETWORK_NONE,
                movementModes, missionRoles, 2, fRec);
        
        return params;
    }
}
