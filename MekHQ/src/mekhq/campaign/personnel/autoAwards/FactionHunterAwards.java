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
import java.util.Objects;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.universe.Faction;

public class FactionHunterAwards {
    /**
     * This function loops through Faction Hunter Awards, checking whether the person is eligible to receive each type
     * of award
     *
     * @param campaign the campaign to be processed
     * @param mission  the mission just completed
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where item == TheatreOfWar)
     */
    public static Map<Integer, List<Object>> FactionHunterAwardsProcessor(Campaign campaign, Mission mission,
          UUID person, List<Award> awards) {
        boolean isEligible = false;
        List<Award> eligibleAwards = new ArrayList<>();

        Faction missionFaction = ((AtBContract) mission).getEnemy();

        for (Award award : awards) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                List<String> targetFactions = List.of(award.getRange().split(","));

                if (!targetFactions.isEmpty()) {
                    // returns true if missionFaction matches the requirements of the listed
                    // targetFactions
                    for (String awardFaction : targetFactions) {
                        // does the awardFaction equal missionFaction? if so, break the loop
                        if ((Objects.equals(awardFaction, missionFaction.getShortName()))) {
                            isEligible = true;
                            break;
                        }

                        // does awardFaction match one of the special super-factions?
                        switch (awardFaction.toLowerCase()) {
                            case "major powers":
                                if (missionFaction.isMajorOrSuperPower()) {
                                    isEligible = true;
                                }
                                break;
                            case "inner sphere":
                                if (missionFaction.isInnerSphere()) {
                                    isEligible = true;
                                }
                                break;
                            case "clans":
                                if (missionFaction.isClan()) {
                                    isEligible = true;
                                }
                                break;
                            case "periphery":
                                if (missionFaction.isPeriphery()) {
                                    isEligible = true;
                                }
                                break;
                            case "pirate":
                                if (missionFaction.isPirate()) {
                                    isEligible = true;
                                }
                                break;
                            case "mercenary":
                                if (missionFaction.isMercenary()) {
                                    isEligible = true;
                                }
                                break;
                            case "independent":
                                if (missionFaction.isIndependent()) {
                                    isEligible = true;
                                }
                                break;
                            case "deep periphery":
                                if (missionFaction.isDeepPeriphery()) {
                                    isEligible = true;
                                }
                                break;
                            case "wob":
                                if (missionFaction.isWoB()) {
                                    isEligible = true;
                                }
                                break;
                            case "comstar or wob":
                                if (missionFaction.isComStarOrWoB()) {
                                    isEligible = true;
                                }
                                break;
                            default:
                                break;
                        }

                        // once we have one positive, there is no need to continue cycling through
                        // factions
                        if (isEligible) {
                            break;
                        }
                    }

                    if (isEligible) {
                        eligibleAwards.add(award);
                    }
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }
}
