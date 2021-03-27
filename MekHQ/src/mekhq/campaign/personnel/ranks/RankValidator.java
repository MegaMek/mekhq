package mekhq.campaign.personnel.ranks;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;

public class RankValidator {

    public boolean validate(final @Nullable RankSystem rankSystem, final boolean userData) {
        if (rankSystem == null) {
            return false; // null is never a valid rank system
        }

        if (Ranks.getRankSystems().containsKey(rankSystem.getRankSystemCode())) {
            if (userData) {
                MekHQ.getLogger().error("Duplicate Rank System Code: " + rankSystem.getRankSystemCode()
                        + ". Current " + Ranks.getRankSystems().get(rankSystem.getRankSystemCode()).getRankSystemName()
                        + " is duplicated by userData Rank System " + rankSystem.getRankSystemName());
            } else {
                MekHQ.getLogger().error("Duplicate Rank System Code: " + rankSystem.getRankSystemCode()
                        + ". Current " + Ranks.getRankSystems().get(rankSystem.getRankSystemCode()).getRankSystemName()
                        + " is duplicated by " + rankSystem.getRankSystemName());
            }
            continue;
        }
    }
}
