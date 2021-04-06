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
package mekhq.gui.displayWrappers;

import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;

import java.util.ArrayList;
import java.util.List;

public class RankDisplay {
    //region Variable Declarations
    private final int rankNumeric;
    private final String displayName;
    //endregion Variable Declarations

    //region Constructors
    public RankDisplay(final int rankNumeric, final String displayName) {
        this.rankNumeric = rankNumeric;
        this.displayName = displayName;
    }
    //endregion Constructors

    //region Getters
    public int getRankNumeric() {
        return rankNumeric;
    }
    //endregion Getters

    /**
     * This creates a list of all valid rank displays, which can then be added to a checkbox or used
     * to create menu items
     * @param rankSystem the rank system to get all valid rank display from
     * @param initialProfession the initial profession for the ranks
     * @return a list of all valid rank displays
     */
    public static List<RankDisplay> getRankDisplaysForSystem(final RankSystem rankSystem, final Profession initialProfession) {
        final List<RankDisplay> rankDisplays = new ArrayList<>();
        final Profession profession = initialProfession.getBaseProfession(rankSystem);
        for (int i = 0; i < rankSystem.getRanks().size(); i++) {
            final Rank rank = rankSystem.getRanks().get(i);
            if (!rank.isEmpty(profession)) {
                rankDisplays.add(new RankDisplay(i, rank.getName(profession.getProfessionFromBase(rankSystem, rank))));
            }
        }
        return rankDisplays;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
