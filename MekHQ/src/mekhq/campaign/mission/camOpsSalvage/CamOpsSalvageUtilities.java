package mekhq.campaign.mission.camOpsSalvage;

import static megamek.common.equipment.MiscType.F_NAVAL_TUG_ADAPTOR;

import megamek.common.equipment.Mounted;
import megamek.common.units.Entity;

public class CamOpsSalvageUtilities {
    public static boolean hasNavalTug(Entity entity) {
        for (Mounted<?> mounted : entity.getMisc()) {
            if (mounted.getType().hasFlag(F_NAVAL_TUG_ADAPTOR)) {
                // isOperable doesn't check if the mounted location still exists, so we check for that first.
                if (!mounted.getEntity().isLocationBad(mounted.getLocation()) && (mounted.isOperable())) {
                    return true;
                }
            }
        }

        return false;
    }
}
