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
import java.util.stream.IntStream;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

public class TheatreOfWarAwards {
    private static final MMLogger logger = MMLogger.create(TheatreOfWarAwards.class);

    /**
     * This function loops through Theatre of War Awards, checking whether the person is eligible to receive each type
     * of award
     *
     * @param campaign the campaign to be processed
     * @param mission  the mission just completed
     * @param person   the person to check award eligibility for
     * @param awards   the awards to be processed (should only include awards where item == TheatreOfWar)
     */
    public static Map<Integer, List<Object>> TheatreOfWarAwardsProcessor(Campaign campaign, Mission mission,
          UUID person, List<Award> awards) {
        boolean isEligible;
        List<Award> eligibleAwards = new ArrayList<>();

        // if the mission isn't an instance of 'AtBContract' we won't have the information we need,
        // so abort processing.
        if (!(mission instanceof AtBContract)) {
            return AutoAwardsController.prepareAwardData(person, eligibleAwards);
        }

        String employer = ((AtBContract) mission).getEmployerCode();

        int contractStartYear = ((Contract) mission).getStartDate().getYear();
        int currentYear = campaign.getGameYear();

        for (Award award : awards) {
            List<String> attackers = new ArrayList<>();
            List<String> defenders = new ArrayList<>();

            List<String> wartime = List.of(award.getSize()
                                                 .replaceAll("\\s", "")
                                                 .split(","));

            if (wartime.size() != 2) {
                logger.warn("Award {} from the {} set has invalid start/end date {}",
                      award.getName(), award.getSet(), award.getSize());
                continue;
            }

            List<String> belligerents = List.of(award.getRange().split(","));

            if (!belligerents.isEmpty()) {
                if (belligerents.size() > 1) {
                    for (String belligerent : belligerents) {
                        if (belligerent.replaceAll("[()]", "").contains("1")) {
                            attackers.add(belligerent.replaceAll("[^. A-Za-z]", ""));
                        } else if (belligerent.replaceAll("[()]", "").contains("2")) {
                            defenders.add(belligerent.replaceAll("[^. A-Za-z]", ""));
                        }
                    }

                    if ((attackers.isEmpty()) || (defenders.isEmpty())) {
                        logger.warn("Award {} from the {} set has incorrectly formated belligerents {}",
                              award.getName(), award.getSet(), award.getRange());
                        continue;
                    }
                }
            } else {
                logger.warn("Award {} from the {} set has no belligerents",
                      award.getName(), award.getSet());
                continue;
            }

            if (award.canBeAwarded(campaign.getPerson(person))) {
                if (isDuringWartime(wartime, contractStartYear, currentYear)) {
                    isEligible = true;
                } else {
                    continue;
                }

                if (belligerents.size() == 1) {
                    if (!processFaction(employer, belligerents.get(0))) {
                        continue;
                    }
                } else if (campaign.getCampaignOptions().isUseAtB()) {
                    String enemy = ((AtBContract) mission).getEnemyCode();

                    if (hasLoyalty(employer, attackers)) {
                        isEligible = hasLoyalty(enemy, defenders);
                    } else if (hasLoyalty(employer, defenders)) {
                        isEligible = hasLoyalty(enemy, attackers);
                    } else {
                        continue;
                    }
                }

                if (isEligible) {
                    eligibleAwards.add(award);
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }

    /**
     * Streams through years covered by Contract, returns true if at least one is during wartime
     *
     * @param wartime           a list with two entries, war start year and war end year (can be identical)
     * @param contractStartYear the contract's start yet
     * @param currentYear       the current campaign year
     */
    private static boolean isDuringWartime(List<String> wartime, int contractStartYear, int currentYear) {
        int contractLength = currentYear - contractStartYear;

        try {
            return IntStream.rangeClosed(0, contractLength).map(year -> contractStartYear + year)
                         .anyMatch(checkYear -> (checkYear >= Integer.parseInt(wartime.get(0)))
                                                      && (checkYear <= Integer.parseInt(wartime.get(1))));
        } catch (Exception e) {
            logger.error("Failed to parse isDuringWartime. Returning false.");
            return false;
        }
    }

    /**
     * Streams through the contents of factions and returns true if any match missionFaction
     *
     * @param missionFaction a single faction (either employer or enemy)
     * @param factions       a list of factions (either a list of attackers, or of defenders)
     */
    private static boolean hasLoyalty(String missionFaction, List<String> factions) {
        return factions.stream().anyMatch(faction -> processFaction(missionFaction, faction));
    }

    /**
     * Checks whether missionFaction matches the requirements of belligerent
     *
     * @param missionFaction a single faction (either employer or enemy)
     * @param belligerent    the faction, or super-faction, to be matched against
     */
    static boolean processFaction(String missionFaction, String belligerent) {
        Faction faction = Factions.getInstance().getFaction(missionFaction);

        missionFaction = missionFaction.toLowerCase().replaceAll("\\s", "");
        belligerent = belligerent.toLowerCase().replaceAll("\\s", "");

        return switch (belligerent) {
            case "majorpowers" -> faction.isMajorOrSuperPower();
            case "innersphere" -> faction.isInnerSphere();
            case "clans" -> faction.isClan();
            case "periphery" -> faction.isPeriphery();
            case "pirate" -> faction.isPirate();
            case "mercenary" -> faction.isMercenary();
            case "independent" -> faction.isIndependent();
            case "deepperiphery" -> faction.isDeepPeriphery();
            case "comstar" -> faction.isComStar();
            case "wob" -> faction.isWoB();
            case "comstarorwob" -> faction.isComStarOrWoB();
            default -> missionFaction.equals(belligerent);
        };
    }
}
