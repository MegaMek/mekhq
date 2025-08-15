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

public class TimeAwards {
    private static final MMLogger logger = MMLogger.create(TimeAwards.class);

    /**
     * This function loops through Time Awards, checking whether the person is eligible to receive each type of award
     *
     * @param campaign the campaign to be processed
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where item == Time)
     */
    public static Map<Integer, List<Object>> TimeAwardsProcessor(Campaign campaign, UUID person, List<Award> awards) {
        int requiredYearsOfService;
        boolean isCumulative;
        long yearsOfService;

        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> eligibleAwardsBestable = new ArrayList<>();
        Award bestAward = new Award();

        for (Award award : awards) {
            try {
                requiredYearsOfService = award.getQty();
            } catch (Exception e) {
                logger.warn("Award {} from the {} set has an invalid qty value {}",
                      award.getName(), award.getSet(), award.getQty());
                continue;
            }

            try {
                isCumulative = award.isStackable();
            } catch (Exception e) {
                logger.warn("Award {} from the {} set has an invalid stackable value {}",
                      award.getName(), award.getSet(), award.getQty());
                continue;
            }

            if (award.canBeAwarded(campaign.getPerson(person))) {
                try {
                    yearsOfService = campaign.getPerson(person).getYearsInService(campaign);
                } catch (Exception e) {
                    logger.error("Unable to parse yearsOfService for {} while processing Award {} from the [{}] set.",
                          campaign.getPerson(person).getFullName(), award.getName(), award.getSet());
                    continue;
                }

                if (isCumulative) {
                    requiredYearsOfService *= campaign.getPerson(person).getAwardController().getNumberOfAwards(award)
                                                    + 1;
                }

                if (yearsOfService >= requiredYearsOfService) {
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
