/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.personnelMarket;

import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.Compute.randomInt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;

public class NewPersonnelMarket {
    private static final MMLogger logger = MMLogger.create(NewPersonnelMarket.class);

    static int RARE_PROFESSION_WEIGHT = 10;

    final private Campaign campaign;
    final private Map<PersonnelRole, PersonnelMarketEntry> marketEntries;
    final private int gameYear;
    private List<Faction> applicantOriginFactions = new ArrayList<>();

    boolean systemHasNoPopulation;
    boolean noInterestedApplicants;
    boolean hasRarePersonnel;
    int recruitmentRolls;
    private List<Person> availablePersonnel = new ArrayList<>();

    public NewPersonnelMarket(Campaign campaign, Map<PersonnelRole, PersonnelMarketEntry> marketEntries,
          int lengthOfMonth) {
        this.campaign = campaign;
        this.marketEntries = marketEntries;

        LocalDate today = campaign.getLocalDate();
        this.gameYear = today.getYear();

        PlanetarySystem currentSystem = campaign.getLocation().getCurrentSystem();
        if (currentSystem.getPopulation(today) == 0) {
            logger.info("No population on the current system, so no applicants will be generated.");
            systemHasNoPopulation = true;
            return;
        }

        getApplicantOriginFactions(campaign.getFaction(), campaign.getLocation().getCurrentSystem(), today);
        if (applicantOriginFactions.isEmpty()) {
            logger.info("No applicants are interested from the available factions.");
            noInterestedApplicants = true;
            return;
        }

        generateApplicants(lengthOfMonth);
        if (availablePersonnel.isEmpty()) {
            logger.info("No applicants were generated.");
        } else {
            logger.info("Generated {} applicants for the campaign.", availablePersonnel.size());
        }
    }

    public List<Person> getAvailablePersonnel() {
        return availablePersonnel;
    }

    public boolean hasRarePersonnel() {
        return hasRarePersonnel;
    }

    public boolean systemHasNoPopulation() {
        return systemHasNoPopulation;
    }

    public boolean noInterestedApplicants() {
        return noInterestedApplicants;
    }

    public int getRecruitmentRolls() {
        return recruitmentRolls;
    }

    private void getApplicantOriginFactions(Faction campaignFaction, PlanetarySystem currentSystem, LocalDate today) {
        Set<Faction> systemFactions = currentSystem.getFactionSet(today);

        for (Faction faction : systemFactions) {
            if (FactionHints.defaultFactionHints().isAtWarWith(campaignFaction, faction, today)) {
                continue;
            }

            // Allies are three times as likely to join the campaign as non-allies
            if (FactionHints.defaultFactionHints().isAlliedWith(campaignFaction, faction, today)) {
                applicantOriginFactions.add(faction);
                applicantOriginFactions.add(faction);
            }
            applicantOriginFactions.add(faction);
        }
    }

    private void generateApplicants(int lengthOfMonth) {
        ReputationController reputation = campaign.getReputation();
        int averageSkillLevel = reputation.getAverageSkillLevel().getExperienceLevel();

        recruitmentRolls = lengthOfMonth * getRollsBasedOnLocation();

        // Applicants will only try to join the campaign if their experience level is below the average skill level
        // of the campaign minus 2 to a minimum of 2 (Green).
        averageSkillLevel = Math.max(averageSkillLevel - (campaign.isOfferingGoldenHello() ? 1 : 2), 2);

        for (int roll = 0; roll < recruitmentRolls; roll++) {
            PersonnelMarketEntry entry = pickEntry();
            if (entry == null) {
                logger.info("No personnel market entries found");
                return;
            }

            // If the entry's introduction year is after the current game year, pick the fallback profession
            // Keep going until we find an entry that is before the current game year.
            int remainingIterations = 3;
            PersonnelMarketEntry originalEntry = entry;
            while (entry != null && entry.introductionYear() > gameYear) {
                PersonnelRole fallbackProfession = entry.fallbackProfession();
                entry = marketEntries.get(fallbackProfession);
                remainingIterations--;
                if (remainingIterations <= 0) {
                    entry = null;
                }
            }

            if (entry == null) {
                logger.info("Could not find a suitable fallback profession for {} game year {}",
                      originalEntry.profession(),
                      gameYear);
                return;
            }

            // If we have a valid entry, we now need to generate the applicant
            String applicantOriginFaction = getRandomItem(applicantOriginFactions).getShortName();
            Person applicant = campaign.newPerson(entry.profession(), applicantOriginFaction, Gender.RANDOMIZE);
            if (applicant == null) {
                logger.info("Could not create person for {} game year {} from faction {}",
                      originalEntry.profession(),
                      gameYear,
                      applicantOriginFaction);
                return;
            }

            int applicantSkill = applicant.getSkillLevel(campaign, false).getExperienceLevel();

            if (applicantSkill > averageSkillLevel) {
                logger.debug("Applicant is too experienced for the campaign, skipping.");
                continue;
            }

            availablePersonnel.add(applicant);
            if (!hasRarePersonnel && entry.weight() <= RARE_PROFESSION_WEIGHT) {
                hasRarePersonnel = true;
            }
        }
    }

    private @Nullable PersonnelMarketEntry pickEntry() {
        int totalWeight = marketEntries.values().stream().mapToInt(PersonnelMarketEntry::weight).sum();
        if (totalWeight <= 0) {
            return null;
        }

        int roll = randomInt(totalWeight) + 1; // 1-based, so every entry with positive weight has a chance
        int cumulative = 0;
        for (PersonnelMarketEntry entry : marketEntries.values()) {
            cumulative += entry.weight();
            if (roll <= cumulative) {
                return entry;
            }
        }
        return null; // Should never hit here if weights > 0
    }

    public int getRollsBasedOnLocation() {
        CurrentLocation location = campaign.getLocation();
        PlanetarySystem currentSystem = location.getCurrentSystem();

        LocalDate today = campaign.getLocalDate();
        HiringHallLevel hiringHallLevel = currentSystem.getHiringHallLevel(today);

        int rolls = switch (hiringHallLevel) {
            case STANDARD -> 2;
            case GREAT -> 3;
            default -> 1;
        };

        logger.debug("Rolls based on hiring hall status: {}", rolls);

        for (Faction faction : currentSystem.getFactionSet(today)) {
            if (currentSystem.equals(faction.getStartingPlanet(campaign, today))) {
                if (faction.isMajorOrSuperPower()) {
                    rolls++;
                    break;
                }
            }
        }

        logger.debug("Rolls including capital status: {}", rolls);

        return rolls;
    }
}
