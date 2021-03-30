package mekhq.campaign.personnel.ranks;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;

public class RankValidator {
    public RankValidator() {

    }

    public boolean validate(final @Nullable RankSystem rankSystem) {
        // Null is never a valid rank system, but this catches some default returns whose errors are
        // caught during the loading process. This MUST be the first check and CANNOT be removed.
        if (rankSystem == null) {
            return false;
        }

        // If the code is a duplicate, we've got a duplicate key error
        if (Ranks.getRankSystems().containsKey(rankSystem.getRankSystemCode())) {
            if (rankSystem.getType().isUserData()) {
                MekHQ.getLogger().error("Duplicate Rank System Code: " + rankSystem.getRankSystemCode()
                        + ". Current " + Ranks.getRankSystems().get(rankSystem.getRankSystemCode()).getRankSystemName()
                        + " is duplicated by userData Rank System " + rankSystem.getRankSystemName());
            } else {
                MekHQ.getLogger().error("Duplicate Rank System Code: " + rankSystem.getRankSystemCode()
                        + ". Current " + Ranks.getRankSystems().get(rankSystem.getRankSystemCode()).getRankSystemName()
                        + " is duplicated by " + rankSystem.getRankSystemName());
            }
            return false;
        }

        // Default System Validation has passed successfully
        if (rankSystem.getType().isDefault()) {
            return true;
        }

        // Now for the more computationally intensive processing.


        // Validation has passed successfully
        return true;
    }
}
