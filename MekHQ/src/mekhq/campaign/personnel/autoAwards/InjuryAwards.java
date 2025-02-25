/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */

package mekhq.campaign.personnel.autoAwards;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Award;

public class InjuryAwards {
    private static final MMLogger logger = MMLogger.create(InjuryAwards.class);

    /**
     * This function loops through Injury Awards, checking whether the person is
     * eligible to receive each type of award
     *
     * @param campaign    the campaign to be processed
     * @param person      the Person to check award eligibility for
     * @param awards      awards the awards to be processed (should only include
     *                    awards where item == Injury)
     * @param injuryCount the number of Hits sustained in the Scenario just
     *                    concluded
     */
    public static Map<Integer, List<Object>> InjuryAwardsProcessor(Campaign campaign, UUID person, List<Award> awards,
            int injuryCount) {
        int injuriesNeeded;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    injuriesNeeded = award.getQty();
                } catch (Exception e) {
                    logger.warn("Injury Award {} from the {} set has invalid range qty {}",
                            award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                if (injuryCount >= injuriesNeeded) {
                    eligibleAwardsBestable.add(award);
                }
            }
        }

        if (!eligibleAwardsBestable.isEmpty()) {
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                int rollingQty = 0;

                for (Award award : eligibleAwardsBestable) {
                    if (award.getQty() > rollingQty) {
                        rollingQty = award.getQty();
                        bestAward = award;
                    }
                }

                eligibleAwards.add(bestAward);
            } else {
                eligibleAwards.addAll(eligibleAwardsBestable);
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
