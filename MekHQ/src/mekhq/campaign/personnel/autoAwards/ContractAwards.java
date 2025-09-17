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

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;

public class ContractAwards {
    private static final MMLogger LOGGER = MMLogger.create(ContractAwards.class);

    /**
     * This function loops through Contract Awards, checking whether the person is eligible to receive each type of
     * award
     *
     * @param campaign the campaign to be processed
     * @param mission  the mission that just concluded
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where item == Kill)
     */
    public static Map<Integer, List<Object>> ContractAwardsProcessor(Campaign campaign, Mission mission,
          UUID person, List<Award> awards) {
        List<Award> eligibleAwards = new ArrayList<>();
        List<Award> bestEligibleAwards = new ArrayList<>();
        Award bestAward = new Award();

        long contractDuration = ChronoUnit.MONTHS.between(
              ((Contract) mission).getStartDate(),
              campaign.getLocalDate());

        // these entries should always be in lower case
        List<String> validTypes = Arrays.asList("months", "duty", "garrison duty", "cadre duty", "security duty",
              "riot duty", "planetary assault", "relief duty", "guerrilla warfare", "pirate hunting", "raid",
              "diversionary raid", "objective raid", "recon raid", "extraction raid");

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                if (award.getRange().equalsIgnoreCase("months")) {
                    try {
                        int requiredDuration = award.getQty();

                        if (contractDuration >= requiredDuration) {
                            bestEligibleAwards.add(award);
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Award {} from the {} set has an invalid qty value {}",
                              award.getName(), award.getSet(), award.getQty());
                    }
                } else if (validTypes.contains(award.getRange().toLowerCase())) {
                    switch (award.getRange().toLowerCase()) {
                        case "duty":
                            if (mission.getType().toLowerCase().contains("duty")) {
                                eligibleAwards.add(award);
                            }
                            break;
                        case "raid":
                            if (mission.getType().toLowerCase().contains("raid")) {
                                eligibleAwards.add(award);
                            }
                            break;
                        default:
                            if (mission.getType().equalsIgnoreCase(award.getRange())) {
                                eligibleAwards.add(award);
                            }
                    }
                } else {
                    LOGGER.warn("Award {} from the {} set has an invalid range value {}",
                          award.getName(), award.getSet(), award.getRange());
                }
            }
        }

        if (!bestEligibleAwards.isEmpty()) {
            int rollingQty = 0;

            if (campaign.getCampaignOptions().isIssueBestAwardOnly()) {
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
