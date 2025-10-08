/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
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
    private static final MMLogger LOGGER = MMLogger.create(InjuryAwards.class);

    /**
     * This function loops through Injury Awards, checking whether the person is eligible to receive each type of award
     *
     * @param campaign    the campaign to be processed
     * @param person      the Person to check award eligibility for
     * @param awards      awards the awards to be processed (should only include awards where item == Injury)
     * @param injuryCount the number of Hits sustained in the Scenario just concluded
     */
    public static Map<Integer, List<Object>> InjuryAwardsProcessor(Campaign campaign, UUID person, List<Award> awards,
          int injuryCount) {
        int injuriesNeeded;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> bestEligibleAwards = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    injuriesNeeded = award.getQty();
                } catch (Exception e) {
                    LOGGER.warn("Injury Award {} from the {} set has invalid range qty {}",
                          award.getName(), award.getSet(), award.getQty());
                    continue;
                }

                if (injuryCount >= injuriesNeeded) {
                    bestEligibleAwards.add(award);
                }
            }
        }

        if (!bestEligibleAwards.isEmpty()) {
            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
                int rollingQty = 0;

                for (Award award : bestEligibleAwards) {
                    if (award.getQty() > rollingQty) {
                        rollingQty = award.getQty();
                        bestAward = award;
                    }
                }

                eligibleAwards.add(bestAward);
            } else {
                eligibleAwards.addAll(bestEligibleAwards);
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
