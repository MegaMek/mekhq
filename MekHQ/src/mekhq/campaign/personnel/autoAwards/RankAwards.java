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
import mekhq.campaign.personnel.Person;

public class RankAwards {
    private static final MMLogger logger = MMLogger.create(RankAwards.class);

    // region Enum Declarations
    enum RankAwardsEnums {
        PROMOTION("Promotion"),
        INCLUSIVE("Inclusive"),
        EXCLUSIVE("Exclusive");

        private final String name;

        RankAwardsEnums(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
    // endRegion Enum Declarations

    /**
     * This function loops through Rank Awards, checking whether the person is
     * eligible to receive each type of award.
     *
     * @param campaign the current campaign
     * @param personId the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where
     *                 item == Kill)
     */
    public static Map<Integer, List<Object>> RankAwardsProcessor(Campaign campaign, UUID personId, List<Award> awards) {
        int requiredRankNumeric;
        boolean isEligible;

        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            try {
                requiredRankNumeric = award.getQty();
            } catch (Exception e) {
                logger.warn("Award {} from the {} set has an invalid qty value {}",
                        award.getName(),
                        award.getSet(),
                        award.getQty());
                continue;
            }

            boolean matchFound = false;

            // as there is only a max iteration of 3, there is no reason to use a stream
            // here
            for (RankAwardsEnums value : RankAwardsEnums.values()) {
                if (value.getName().equalsIgnoreCase(award.getRange())) {
                    matchFound = true;
                    break;
                }
            }

            if (!matchFound) {
                logger.warn("Award {} from the {} set has the invalid range {}",
                        award.getName(),
                        award.getSet(),
                        award.getRange());
            }

            Person person = campaign.getPerson(personId);

            isEligible = switch (award.getRange()) {
                case "Promotion" -> (person.getRankNumeric() == requiredRankNumeric)
                        && ((award.getSize() == null)
                                || (award.getSize().equalsIgnoreCase(person.getRankSystem().getCode())));
                case "Inclusive" -> person.getRankNumeric() >= requiredRankNumeric;
                case "Exclusive" -> {
                    if (((requiredRankNumeric <= 20) && (person.getRankNumeric() <= 20))
                            || ((requiredRankNumeric <= 30) && (person.getRankNumeric() <= 30))
                            || ((requiredRankNumeric >= 31) && (person.getRankNumeric() >= 31))) {
                        yield person.getRankNumeric() >= requiredRankNumeric;
                    } else {
                        yield false;
                    }
                }
                default -> false;
            };

            if (isEligible) {
                eligibleAwards.add(award);
            }
        }

        return AutoAwardsController.prepareAwardData(personId, eligibleAwards);
    }
}
