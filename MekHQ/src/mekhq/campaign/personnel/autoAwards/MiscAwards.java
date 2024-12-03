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

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Award;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.RandomFactionGenerator;

import java.time.LocalDate;
import java.util.*;

public class MiscAwards {

    /**
     * This function processes miscellaneous awards for a given person in a
     * campaign.
     * It checks the eligibility of the person for each type of award and returns a
     * map of eligible awards grouped by their respective IDs.
     *
     * @param campaign               the current campaign
     * @param mission                the mission just completed (null if no mission
     *                               was completed)
     * @param person                 the person to check award eligibility for
     * @param awards                 the awards to be processed (should only include
     *                               awards where item == Kill)
     * @param missionWasSuccessful   true if the completed mission was successful,
     *                               false otherwise
     * @param isCivilianHelp         true if the completed scenario was AtB Scenario
     *                               CIVILIANHELP
     * @param killCount              the number of kills (null if not applicable)
     * @param injuryCount            the number of injuries (null if not applicable)
     * @param supportPersonOfTheYear a UUID identifying the candidate for Support
     *                               Person of the Year awards
     * @return a map of eligible awards grouped by their respective IDs
     */
    public static Map<Integer, List<Object>> MiscAwardsProcessor(Campaign campaign, @Nullable Mission mission,
            UUID person, List<Award> awards, Boolean missionWasSuccessful, boolean isCivilianHelp,
            @Nullable Integer killCount, @Nullable Integer injuryCount, @Nullable UUID supportPersonOfTheYear) {
        List<Award> eligibleAwards = new ArrayList<>();

        for (Award award : awards) {
            switch (award.getRange().replaceAll("\\s", "").toLowerCase()) {
                case "missionaccomplished" -> {
                    if (missionWasSuccessful && MissionAccomplishedAward(campaign, award, person)) {
                        eligibleAwards.add(award);
                    }
                }
                case "houseworldnowar" -> {
                    if (HouseWorldWar(campaign, mission, award, person, false)) {
                        eligibleAwards.add(award);
                    }
                }
                case "houseworldyeswar" -> {
                    if (HouseWorldWar(campaign, mission, award, person, true)) {
                        eligibleAwards.add(award);
                    }
                }
                case "periphery" -> {
                    if (Periphery(campaign, mission, award, person)) {
                        eligibleAwards.add(award);
                    }
                }
                case "medalofhonor" -> {
                    if (MedalOfHonor(campaign, award, person, killCount, injuryCount)) {
                        eligibleAwards.add(award);
                    }
                }
                case "ceremonialduty" -> {
                    if (CeremonialDuty(campaign, award, person, mission)) {
                        eligibleAwards.add(award);
                    }
                }
                case "prisonerofwar" -> {
                    if (mission != null && prisonerOfWar(campaign, award, person)) {
                        eligibleAwards.add(award);
                    }
                }
                case "drillinstructor" -> {
                    if (drillInstructor(campaign, award, person)) {
                        eligibleAwards.add(award);
                    }
                }
                case "civilianhelp" -> {
                    if ((isCivilianHelp) && (award.canBeAwarded(campaign.getPerson(person)))) {
                        eligibleAwards.add(award);
                    }
                }
                case "supportpersonoftheyear" -> {
                    if ((supportPersonOfTheYear != null)
                            && (supportPersonOfTheYear(campaign, award, person, supportPersonOfTheYear))) {
                        eligibleAwards.add(award);
                    }
                }
                default -> {
                }
            }
        }

        return AutoAwardsController.prepareAwardData(person, eligibleAwards);
    }

    /**
     * This function checks whether Mission Accomplished awards can be awarded to
     * Person
     *
     * @param campaign the current campaign
     * @param award    the award to be processed
     * @param person   the person to check award eligibility for
     */
    private static boolean MissionAccomplishedAward(Campaign campaign, Award award, UUID person) {
        return award.canBeAwarded(campaign.getPerson(person));
    }

    /**
     * This function checks whether House World War/No War awards can be awarded to
     * Person
     *
     * @param campaign the current campaign
     * @param mission  the Mission just completed
     * @param award    the award to be processed
     * @param person   the person to check award eligibility for
     * @param isYesWar true if this is a Yes War Award
     */
    private static boolean HouseWorldWar(Campaign campaign, @Nullable Mission mission, Award award, UUID person,
            boolean isYesWar) {
        if (award.canBeAwarded(campaign.getPerson(person))) {
            if (mission != null) {
                if (mission instanceof AtBContract) {
                    PlanetarySystem system = campaign.getSystemById(mission.getSystemId());
                    LocalDate date = campaign.getLocalDate();
                    Faction enemyFaction = ((AtBContract) mission).getEnemy();

                    for (Faction faction : system.getFactionSet(campaign.getLocalDate())) {
                        if (faction.isISMajorOrSuperPower()) {
                            boolean isAtWar = RandomFactionGenerator.getInstance().getFactionHints()
                                    .isAtWarWith(enemyFaction, faction, date);

                            if ((isAtWar) && (isYesWar)) {
                                return true;
                            } else if ((!isAtWar) && (!isYesWar)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * This method checks whether Periphery awards can be awarded to a person.
     *
     * @param campaign the current campaign
     * @param mission  the mission just completed (nullable)
     * @param award    the award to be processed
     * @param person   the person to check award eligibility for
     * @return true if the person is eligible for the award, false otherwise
     */
    private static boolean Periphery(Campaign campaign, @Nullable Mission mission, Award award, UUID person) {
        if (award.canBeAwarded(campaign.getPerson(person))) {
            if (mission != null) {
                try {
                    PlanetarySystem system = campaign.getSystemById(mission.getSystemId());

                    return system.getFactionSet(campaign.getLocalDate()).stream().anyMatch(Faction::isPeriphery);
                } catch (Exception e) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Determines if a person is eligible for the Medal of Honor award based on the
     * campaign, award, person, kill count, and injury count.
     *
     * @param campaign    the campaign in which the person is participating
     * @param award       the award being considered
     * @param person      the unique identifier of the person
     * @param killCount   the number of kills achieved by the person (nullable)
     * @param injuryCount the number of injuries suffered by the person (nullable)
     * @return true if the person is eligible for the Medal of Honor award, false
     *         otherwise
     */
    private static boolean MedalOfHonor(Campaign campaign, Award award, UUID person, @Nullable Integer killCount,
            @Nullable Integer injuryCount) {
        if (award.canBeAwarded(campaign.getPerson(person))) {
            if ((killCount != null) && (injuryCount != null)) {
                return (killCount >= Integer.parseInt(award.getSize())) && (injuryCount >= award.getQty());
            }
        }
        return false;
    }

    /**
     * Checks if the given person is eligible for the ceremonial-duty award.
     *
     * @param campaign the campaign the person is participating in
     * @param award    the award to be checked
     * @param person   the UUID of the person to be checked
     * @param mission  the mission the person is assigned to (nullable)
     * @return true if the person is eligible for the award, false otherwise
     */
    private static boolean CeremonialDuty(Campaign campaign, Award award, UUID person, @Nullable Mission mission) {
        if (award.canBeAwarded(campaign.getPerson(person))) {
            if (mission != null) {
                if (mission instanceof Contract) {
                    PlanetarySystem capitalSystem = Factions.getInstance()
                            .getFactionFromFullNameAndYear(((Contract) mission).getEmployer(),
                                    campaign.getGameYear())
                            .getStartingPlanet(campaign, campaign.getLocalDate());
                    try {
                        if (campaign.getCurrentSystem().equals(capitalSystem)) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if a person is a prisoner of war in a given campaign and is eligible
     * to receive an award.
     *
     * @param campaign the campaign in which the person is participating
     * @param award    the award to be given
     * @param person   the unique identifier of the person to check
     * @return true if the person is a prisoner of war and is eligible to receive
     *         the award, false otherwise
     */
    private static boolean prisonerOfWar(Campaign campaign, Award award, UUID person) {
        if (award.canBeAwarded(campaign.getPerson(person))) {
            return campaign.getPerson(person).getStatus().isPoW();
        }

        return false;
    }

    /**
     * Checks if the given person is a training lance leader and is eligible for the
     * given award.
     *
     * @param campaign the campaign object representing the current campaign
     * @param award    the award object representing the award to be checked
     * @param person   the UUID of the person to be checked
     * @return true if the person is a training lance leader and is eligible for the
     *         award, false otherwise
     */
    private static boolean drillInstructor(Campaign campaign, Award award, UUID person) {
        if (award.canBeAwarded(campaign.getPerson(person))) {
            return campaign.getAllStrategicFormations().stream()
                    .anyMatch(lance -> (lance.getRole().isTraining()) && (lance.getCommanderId().equals(person)));
        }

        return false;
    }

    /**
     * Determines if a person is eligible for the Support Person of the Year award
     * in a given campaign.
     *
     * @param campaign               The campaign in which the award is being
     *                               considered.
     * @param award                  The Support Person of the Year award being
     *                               considered.
     * @param person                 The UUID of the person being evaluated for the
     *                               award.
     * @param supportPersonOfTheYear The UUID of the person chosen as the Support
     *                               Person of the Year.
     * @return true if the person is eligible for the award, false otherwise.
     */
    private static boolean supportPersonOfTheYear(Campaign campaign, Award award, UUID person,
            UUID supportPersonOfTheYear) {
        if (supportPersonOfTheYear.equals(person)) {
            if (award.canBeAwarded(campaign.getPerson(person))) {
                return true;
            } else {
                final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.AutoAwardsDialog",
                        MekHQ.getMHQOptions().getLocale());

                campaign.addReport(String.format(resources.getString("supportPersonOfTheYear.tex"),
                        campaign.getPerson(person).getHyperlinkedFullTitle(),
                        award.getName(),
                        award.getSet()));

                return false;
            }
        }

        return false;
    }
}
