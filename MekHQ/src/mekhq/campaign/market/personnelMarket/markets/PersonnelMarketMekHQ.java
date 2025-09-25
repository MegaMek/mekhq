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
package mekhq.campaign.market.personnelMarket.markets;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.round;
import static megamek.codeUtilities.MathUtility.clamp;
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.CurrentLocation;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.market.personnelMarket.yaml.PersonnelMarketLibraries;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.rating.CamOpsReputation.ReputationController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.factionStanding.FactionStandingUtilities;
import mekhq.campaign.universe.factionStanding.FactionStandings;

/**
 * Implements MekHQ's custom personnel market system.
 *
 * <p>This market style provides the classic MekHQ logic for generating and selecting personnel applicants. It
 * introduces several classic-specific behaviors, such as using resource bundles for availability messaging, population
 * multipliers, and custom system status recruitment multipliers.</p>
 *
 * <ul>
 *     <li>Uses MekHQ’s custom market logic as a selectable option.</li>
 *     <li>Determines applicant origins and handles availability messages.</li>
 *     <li>Recruitment roll calculations incorporate system population and system status.</li>
 * </ul>
 *
 * <p><b>Extends:</b> {@link NewPersonnelMarket}</p>
 * <p><b>Associated Market Style:</b> {@link mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle#MEKHQ}</p>
 *
 * @author Illiani
 * @since 0.50.06
 */
public class PersonnelMarketMekHQ extends NewPersonnelMarket {
    /**
     * Constructs a personnel market using the MekHQ classic ruleset.
     *
     * <p>Initializes data and behaviors for compatibility.</p>
     *
     * @param campaign the parent campaign instance
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketMekHQ() {
        super();

        setAssociatedPersonnelMarketStyle(MEKHQ);

        setLowPopulationRecruitmentDivider(10000000);
        setUnitReputationRecruitmentCutoff(-25);

        PersonnelMarketLibraries personnelMarketLibraries = new PersonnelMarketLibraries();
        setClanMarketEntries(personnelMarketLibraries.getClanMarketMekHQ());
        setInnerSphereMarketEntries(personnelMarketLibraries.getInnerSphereMarketMekHQ());
    }


    /**
     * Determines the list of factions from which personnel applicants may originate, according to MekHQ rules and
     * campaign context.
     *
     * @return a list of possible applicant origin factions
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Override
    public ArrayList<Faction> getApplicantOriginFactions() {
        Set<Faction> systemFactions = getCurrentSystem().getFactionSet(getToday());
        ArrayList<Faction> interestedFactions = new ArrayList<>();

        boolean filterOutLegalFactions = false;
        if (getCampaign().getCampaignOptions().getUnitRatingMethod().isCampaignOperations()) {
            if (getCampaign().getReputation().getReputationRating() < getUnitReputationRecruitmentCutoff()) {
                getLogger().debug(
                      "Only pirates & mercenaries will be considered for applicants, as the campaign's unit " +
                            "rating is below the cutoff.");
                filterOutLegalFactions = true;
            }
        }

        if (getCampaign().isClanCampaign()) {
            if (!filterOutLegalFactions) {
                interestedFactions.add(getCampaign().getFaction());
            }

            return interestedFactions;
        }

        Factions factions = Factions.getInstance();
        Faction mercenaryFaction = factions.getFaction(MERCENARY_FACTION_CODE);
        Faction pirateFaction = factions.getFaction(PIRATE_FACTION_CODE);
        FactionStandings factionStandings = getCampaign().getFactionStandings();

        for (Faction faction : systemFactions) {
            if (filterOutLegalFactions) {
                if (!faction.isPirate() && !faction.isMercenary()) {
                    continue;
                }
            }

            if (FactionHints.defaultFactionHints().isAtWarWith(getCampaignFaction(), faction, getToday())) {
                continue;
            }

            int factionStandingMultiplier = 1;
            if (getCampaign().getCampaignOptions().isUseFactionStandingRecruitmentSafe()) {
                double regard = factionStandings.getRegardForFaction(faction.getShortName(), true);
                factionStandingMultiplier = FactionStandingUtilities.getRecruitmentTickets(regard);
            }

            // Allies are three times as likely to join the campaign as non-allies
            if (getCampaignFaction().equals(faction)
                      || FactionHints.defaultFactionHints().isAlliedWith(getCampaignFaction(), faction, getToday())) {
                factionStandingMultiplier *= 3;
            }

            for (int i = 0; i < factionStandingMultiplier; i++) {
                interestedFactions.add(faction);
            }
        }

        if (mercenaryFaction != null &&
                  !interestedFactions.isEmpty() &&
                  !interestedFactions.contains(mercenaryFaction)) {
            interestedFactions.add(mercenaryFaction);
        }

        if (pirateFaction != null &&
                  !interestedFactions.isEmpty() &&
                  !interestedFactions.contains(pirateFaction)) {
            interestedFactions.add(pirateFaction);
        }

        return interestedFactions;
    }


    /**
     * Returns a localized message pertaining to market availability modifiers.
     *
     * @return an availability message string
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Override
    public String getAvailabilityMessage() {
        CurrentLocation location = getCampaign().getLocation();
        String color;
        String closingBrace = CLOSING_SPAN_TAG;

        if (!location.isOnPlanet()) {
            color = MekHQ.getMHQOptions().getFontColorNegativeHexColor();

            return getFormattedTextAt(getResourceBundle(),
                  "hint.personnelMarket.inTransit",
                  spanOpeningWithCustomColor(color),
                  closingBrace);
        }

        if (getApplicantOriginFactions().isEmpty()) {
            color = MekHQ.getMHQOptions().getFontColorNegativeHexColor();

            return getFormattedTextAt(getResourceBundle(),
                  "hint.personnelMarket.noInterest",
                  spanOpeningWithCustomColor(color),
                  closingBrace);
        }

        for (AtBContract contract : getCampaign().getActiveAtBContracts()) {
            if (!contract.getContractType().isGarrisonType()) {
                color = MekHQ.getMHQOptions().getFontColorNegativeHexColor();

                return getFormattedTextAt(getResourceBundle(),
                      "hint.personnelMarket.onContract",
                      spanOpeningWithCustomColor(color),
                      closingBrace);
            }
        }

        return "";
    }


    /**
     * Generates market applicants for the current period, factoring in population multipliers and custom system
     * status.
     *
     * @author Illiani
     * @since 0.50.06
     */
    @Override
    public void generateApplicants() {
        ReputationController reputation = getCampaign().getReputation();
        int averageSkillLevel = reputation.getAverageSkillLevel().getExperienceLevel();

        calculateNumberOfRecruitmentRolls();

        // Applicants will only try to join the campaign if their experience level is below the average skill level
        // of the campaign minus 2 to a minimum of 2 (Green).
        averageSkillLevel = max(averageSkillLevel - (isOfferingGoldenHello() ? 1 : 2), 2);

        Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries = getCampaign().isClanCampaign() ?
                                                                                getClanMarketEntries() :
                                                                                getInnerSphereMarketEntries();
        unorderedMarketEntries = sanitizeMarketEntries(unorderedMarketEntries);
        List<PersonnelMarketEntry> orderedMarketEntries = getMarketEntriesAsList(unorderedMarketEntries);

        for (int recruitmentRoll = 0; recruitmentRoll < getRecruitmentRolls(); recruitmentRoll++) {
            Person applicant = generateSingleApplicant(unorderedMarketEntries, orderedMarketEntries);
            if (applicant == null) {
                continue;
            }

            int applicantSkill = applicant.getSkillLevel(getCampaign(), false).getExperienceLevel();

            if (applicantSkill > averageSkillLevel) {
                boolean notInterested = false;

                int difference = applicantSkill - averageSkillLevel;
                for (int i = 0; i < difference; i++) {
                    int interestRoll = Compute.randomInt(10);

                    if (interestRoll != 0) {
                        notInterested = true;
                        break;
                    }
                }

                if (notInterested) {
                    getLogger().debug("Applicant is too experienced for the campaign, skipping.");

                    rarePersonnel.remove(applicant.getId());
                    rareProfessions.remove(applicant.getPrimaryRole());
                    continue;
                }
            }

            addApplicant(applicant);
        }

        int dependentsCount = d6();

        for (int roll = 0; roll < dependentsCount; roll++) {
            Faction applicantOriginFaction = getRandomItem(getApplicantOriginFactions());
            Person applicant = getCampaign().newDependent(Gender.RANDOMIZE, applicantOriginFaction,
                  null);
            if (applicant == null) {
                continue;
            }

            addApplicant(applicant);
        }
    }


    /**
     * Calculates a recruitment multiplier based on current system population.
     *
     * @return recruitment multiplier as a double
     *
     * @author Illiani
     * @since 0.50.06
     */
    private double getSystemPopulationRecruitmentMultiplier() {
        long currentSystemPopulation = getCurrentSystem().getPopulation(getToday());
        double populationRatio = (double) currentSystemPopulation / getLowPopulationRecruitmentDivider();
        return min(populationRatio, 1.0);
    }

    /**
     * Calculates the number of recruitment rolls.
     *
     * @author Illiani
     * @since 0.50.06
     */
    private void calculateNumberOfRecruitmentRolls() {
        int lengthOfMonth = getToday().getMonth().length(getToday().isLeapYear());
        getLogger().debug("Base rolls: {}", lengthOfMonth);

        int rolls = lengthOfMonth * getSystemStatusRecruitmentMultiplier();
        getLogger().debug("Rolls modified for location: {}", rolls);

        rolls = clamp((int) round(rolls * getSystemPopulationRecruitmentMultiplier()), 1, rolls);
        getLogger().debug("Rolls modified for population: {}", rolls);

        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        if (campaignOptions.isUseFactionStandingRecruitmentSafe()) {
            rolls = (int) round(rolls * getFactionStandingsRecruitmentModifier());
        }

        if (campaignOptions.isAllowMonthlyConnections()) {
            int additionalRecruits = performConnectionsRecruitsCheck();
            rolls += additionalRecruits;
        }

        setRecruitmentRolls(max(rolls, 0));
    }

    /**
     * Calculates the recruitment modifier based on the campaign's faction standing with local factions.
     *
     * <p>For each faction present in the current planetary system, this method determines the recruitment
     * modifier using the campaign's regard value with that faction. The highest recruitment modifier found among the
     * factions is returned.
     *
     * @return the highest recruitment modifier from all local faction standings
     *
     * @author Illiani
     * @since 0.50.07
     */
    private double getFactionStandingsRecruitmentModifier() {
        FactionStandings factionStandings = getCampaign().getFactionStandings();

        CurrentLocation location = getCampaign().getLocation();
        PlanetarySystem currentSystem = location.getCurrentSystem();
        double multiplier = 0;

        for (Faction faction : currentSystem.getFactionSet(getToday())) {
            double regard = factionStandings.getRegardForFaction(faction.getShortName(), true);
            double currentModifier = FactionStandingUtilities.getRecruitmentRollsModifier(regard);

            if (currentModifier > multiplier) {
                multiplier = currentModifier;
            }
        }

        return multiplier;
    }


    /**
     * Calculates an additional recruitment multiplier based on system status.
     *
     * @return recruitment multiplier as an int
     *
     * @author Illiani
     * @since 0.50.06
     */
    public int getSystemStatusRecruitmentMultiplier() {
        CurrentLocation location = getCampaign().getLocation();
        PlanetarySystem currentSystem = location.getCurrentSystem();

        LocalDate today = getCampaign().getLocalDate();
        HiringHallLevel hiringHallLevel = currentSystem.getHiringHallLevel(today);

        int rolls = switch (hiringHallLevel) {
            case STANDARD -> 2;
            case GREAT -> 3;
            default -> 1;
        };

        getLogger().debug("Rolls based on hiring hall status: {}", rolls);

        boolean isCapital = false;
        boolean isMajorCapital = false;
        for (Faction faction : currentSystem.getFactionSet(today)) {
            if (currentSystem.equals(faction.getStartingPlanet(getCampaign(), today))) {
                isCapital = true;

                if (faction.isMajorOrSuperPower()) {
                    isMajorCapital = true;
                    break;
                }
            }
        }

        if (isCapital) {
            rolls++;
        }

        if (isMajorCapital) {
            rolls++;
        }

        getLogger().debug("Rolls including capital status: {}", rolls);

        return rolls;
    }
}
