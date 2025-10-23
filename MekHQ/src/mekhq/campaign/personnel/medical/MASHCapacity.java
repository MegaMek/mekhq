package mekhq.campaign.personnel.medical;

import java.util.List;

import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import mekhq.campaign.unit.Unit;

public class MASHCapacity {
    public static int checkMASHCapacity(List<Unit> units, int capacityPerTheatre) {
        int mashTheatreCount = 0;

        for (Unit unit : units) {
            if ((unit.isDeployed())
                      || (unit.isDamaged())
                      || (unit.getCrewState().isUncrewed())
                      || (unit.getCrewState().isPartiallyCrewed())) {
                continue;
            }

            for (MiscMounted item : unit.getEntity().getMisc()) {
                if (item.getType().hasFlag(MiscType.F_MASH)) {
                    mashTheatreCount++;
                }
            }
        }

        return mashTheatreCount * capacityPerTheatre;
    }

    public static boolean areMASHTheatresWithinCapacity(int mashTheatreCapacity, int mashTheatreUsage) {
        return mashTheatreCapacity >= mashTheatreUsage;
    }
}
