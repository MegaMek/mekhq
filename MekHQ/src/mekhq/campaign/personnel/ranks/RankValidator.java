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
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;

import java.util.Collection;

public class RankValidator {
    public RankValidator() {

    }

    public boolean validate(final @Nullable RankSystem rankSystem, final boolean checkCode) {
        // Null is never a valid rank system, but this catches some default returns whose errors are
        // caught during the loading process. This MUST be the first check and CANNOT be removed.
        if (rankSystem == null) {
            return false;
        }

        // If the code is a duplicate, we've got a duplicate key error
        if (checkCode && Ranks.getRankSystems().containsKey(rankSystem.getRankSystemCode())) {
            if (rankSystem.getType().isUserData()) {
                MekHQ.getLogger().error("Duplicate Rank System Code: " + rankSystem.getRankSystemCode()
                        + ". Current " + Ranks.getRankSystems().get(rankSystem.getRankSystemCode()).toString()
                        + " is duplicated by userData Rank System " + rankSystem.toString());
            } else {
                MekHQ.getLogger().error("Duplicate Rank System Code: " + rankSystem.getRankSystemCode()
                        + ". Current " + Ranks.getRankSystems().get(rankSystem.getRankSystemCode()).toString()
                        + " is duplicated by " + rankSystem.toString());
            }
            return false;
        }

        // Default System Validation has passed successfully
        if (rankSystem.getType().isDefault()) {
            return true;
        }

        // Now for the more computationally intensive processing, the rank validation
        // First, let's check the size, as we currently require a size equal to the total number of
        // rank tiers
        if (rankSystem.getRanks().size() != Rank.RC_NUM) {
            MekHQ.getLogger().error(String.format("Illegal number of ranks of %d when %d is required",
                    rankSystem.getRanks().size(), Rank.RC_NUM));
            return false;
        }

        // Index 0 needs to be checked individually, as all systems MUST either be filled or indicate
        // an alternative system

        // The rest of the levels need to not be an infinite loop and valid

        // Validation has passed successfully
        return true;
    }

    public void checkAssignedRankSystems(final Campaign campaign) {
        // First, we need to ensure the campaign's rank system was refreshed. This can be done by
        // checking if the system is a campaign custom
        if (!campaign.getRankSystem().getType().isCampaign()) {
            // This ensures it properly changes, with fallback properly handled
            campaign.setRankSystemDirect(Ranks.getRankSystemFromCode(campaign.getRankSystem().getRankSystemCode()));
        }

        // Then, we need to fix any old rank system assignments for personnel
        for (final Person person : campaign.getPersonnel()) {

        }
    }

    public void changeCampaignRankSystem(final RankSystem oldRankSystem,
                                         final RankSystem newRankSystem,
                                         final Collection<Person> personnel) {
        // We need to swap over the previous rank system to the current one
        for (final Person person : personnel) {

        }

        // Then, we need to check the ranks for the personnel
        checkPersonnelRanks(personnel);
    }

    public void checkPersonnelRanks(final Collection<Person> personnel) {

    }

    public void checkPersonRank(final Person person) {

    }
}
