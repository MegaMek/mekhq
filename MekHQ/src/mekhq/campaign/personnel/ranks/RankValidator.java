/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.ranks;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.personnel.Person;

import java.util.Collection;

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

        // Now for the more computationally intensive processing

        // Validation has passed successfully
        return true;
    }

    public void migratePersonnelRanks(final RankSystem oldRankSystem, final RankSystem newRankSystem,
                                      final Collection<Person> personnel) {

    }
}
