/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.compute.Compute.d6;
import static mekhq.campaign.market.personnelMarket.enums.PersonnelMarketStyle.MEKHQ;
import static mekhq.campaign.personnel.RecruitmentCosts.SALARY_MONTHS;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.personnelMarket.records.HiringCostBreakdown;
import mekhq.campaign.market.personnelMarket.records.PersonnelMarketEntry;
import mekhq.campaign.market.personnelMarket.yaml.PersonnelMarketLibraries;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.RecruitmentCosts;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import mekhq.campaign.universe.factionHints.FactionHints;
import mekhq.campaign.universe.factionStanding.FactionStandingLevel;
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
    public static final int ALTERNATE_ADVANCED_MEDICAL_RECRUITMENT_MULTIPLIER = 2;
    /** Months of salary a recruit costs with a Golden Hello */
    static final int GOLDEN_HELLO_SALARY_MONTHS = 24;

    private static final int DEFAULT_UNIT_REPUTATION_RECRUITMENT_CUTOFF = -25;
    private static final int CHAOS_UNIT_REPUTATION_RECRUITMENT_CUTOFF = -3;
    /** The largest system status multiplier: a Great hiring hall (3) on a major power's capital (+2) */
    private static final int MAXIMUM_SYSTEM_STATUS_MULTIPLIER = 5;
    /** Systems with fewer people than this give fewer recruitment rolls, in proportion to their population */
    private static final int LOW_POPULATION_RECRUITMENT_DIVIDER = 7_500_000;

    /**
     * Constructs a personnel market using the MekHQ classic ruleset.
     *
     * <p>Initializes data and behaviors for compatibility.</p>
     *
     * @author Illiani
     * @since 0.50.06
     */
    public PersonnelMarketMekHQ() {
        super();

        setAssociatedPersonnelMarketStyle(MEKHQ);

        PersonnelMarketLibraries personnelMarketLibraries = new PersonnelMarketLibraries();
        setClanMarketEntries(personnelMarketLibraries.getClanMarketMekHQ());
        setInnerSphereMarketEntries(personnelMarketLibraries.getInnerSphereMarketMekHQ());
    }

    /**
     * {@inheritDoc}
     *
     * <p>The MekHQ rules multiply the daily rolls by the system's status (up to
     * {@value #MAXIMUM_SYSTEM_STATUS_MULTIPLIER}), by two under Alternate Advanced Medical, and by the best Faction
     * Standing modifier when standings affect recruitment, before adding Connections.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public int getMaximumRecruitmentRolls() {
        int rolls = MAXIMUM_DAYS_IN_MONTH * MAXIMUM_SYSTEM_STATUS_MULTIPLIER;
        Campaign campaign = getCampaign();
        if (campaign != null) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            if (campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL)) {
                rolls *= ALTERNATE_ADVANCED_MEDICAL_RECRUITMENT_MULTIPLIER;
            }
            if (campaignOptions.isUseFactionStandingRecruitmentSafe()) {
                double bestModifier = 0;
                for (FactionStandingLevel level : FactionStandingLevel.values()) {
                    bestModifier = Math.max(bestModifier, level.getRecruitmentRollsModifier());
                }
                rolls = (int) round(rolls * bestModifier);
            }
        }
        return applyApplicantMultiplier(rolls + getMaximumConnectionsRolls());
    }

    /**
     * {@inheritDoc}
     *
     * @return {@value #LOW_POPULATION_RECRUITMENT_DIVIDER}
     */
    @Override
    public int getLowPopulationRecruitmentDivider() {
        return LOW_POPULATION_RECRUITMENT_DIVIDER;
    }

    /**
     * {@inheritDoc}
     *
     * <p>The cutoff depends on the {@link CampaignOption#USE_CHAOS_REPUTATION} campaign option, as Chaos reputation
     * runs on a much smaller scale.</p>
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    public int getUnitReputationRecruitmentCutoff() {
        Campaign campaign = getCampaign();
        boolean isUseChaosReputation = campaign != null
                                             && campaign.getCampaignOptions().get(CampaignOption.USE_CHAOS_REPUTATION);
        return isUseChaosReputation ? CHAOS_UNIT_REPUTATION_RECRUITMENT_CUTOFF :
                     DEFAULT_UNIT_REPUTATION_RECRUITMENT_CUTOFF;
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
        // Faction -> tenure weight (years the faction held a world here within living memory). Weighting recruits by
        // tenure makes long-standing rulers the dominant birth origin while recent or departed rulers still appear.
        Map<Faction, Integer> systemFactions = getCurrentSystem().getPopulationFactions(getToday());
        ArrayList<Faction> interestedFactions = new ArrayList<>();

        boolean filterOutLegalFactions = false;
        boolean isUseChaosReputation = getCampaign().getCampaignOptions().get(CampaignOption.USE_CHAOS_REPUTATION);
        if (getCampaign().getPlayerForce().getReputationRating(isUseChaosReputation) <
                  getUnitReputationRecruitmentCutoff()) {
            getLogger().debug(
                  "Only pirates & mercenaries will be considered for applicants, as the campaign's unit " +
                        "rating is below the cutoff.");
            filterOutLegalFactions = true;
        }

        if (getCampaign().getPlayerForce().isClanForce()) {
            if (!filterOutLegalFactions) {
                interestedFactions.add(getCampaign().getPlayerForce().getFaction());
            }

            return interestedFactions;
        }

        Factions factions = Factions.getInstance();
        Faction mercenaryFaction = factions.getFaction(MERCENARY_FACTION_CODE);
        Faction pirateFaction = factions.getFaction(PIRATE_FACTION_CODE);
        FactionStandings factionStandings = getCampaign().getPlayerForce().getFactionStandings();

        for (Map.Entry<Faction, Integer> systemFaction : systemFactions.entrySet()) {
            Faction faction = systemFaction.getKey();
            int tenureWeight = systemFaction.getValue();

            if (filterOutLegalFactions) {
                if (!faction.isPirate() && !faction.isMercenary()) {
                    continue;
                }
            }

            if (FactionHints.getInstance().isAtWarWith(getCampaignFaction(), faction, getToday())) {
                continue;
            }

            int factionStandingMultiplier = 1;
            if (getCampaign().getCampaignOptions().isUseFactionStandingRecruitmentSafe()) {
                double regard = factionStandings.getRegardForFaction(faction.getShortName(), true);
                factionStandingMultiplier = FactionStandingUtilities.getRecruitmentTickets(regard);
            }

            // Allies are three times as likely to join the campaign as non-allies
            if (getCampaignFaction().equals(faction)
                      || FactionHints.getInstance().isAlliedWith(getCampaignFaction(), faction, getToday())) {
                factionStandingMultiplier *= 3;
            }

            // Weight the applicant pool by how long the faction held a world here, on top of the standing multiplier.
            for (int i = 0; i < factionStandingMultiplier * tenureWeight; i++) {
                interestedFactions.add(faction);
            }
        }

        if (mercenaryFaction != null && !interestedFactions.contains(mercenaryFaction)) {
            interestedFactions.add(mercenaryFaction);
        }

        if (pirateFaction != null && !interestedFactions.contains(pirateFaction)) {
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
        AbstractLocation location = getCampaign().getPlayerForce().getForceDetachment().getCurrentLocation();
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

        if (getCampaign().getCampaignOptions()
                  .get(CampaignOption.PERSONNEL_MARKET_DISABLE_COMBAT_MANEUVERS_RESTRICTION)) {
            return "";
        }

        for (AbstractContract contract : getCampaign().getActiveContracts()) {
            if (!contract.getObjectiveType().isGarrisonType()) {
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
        boolean isShowOfInterestDisabled = getCampaign().getCampaignOptions()
                                                 .get(CampaignOption.PERSONNEL_MARKET_DISABLE_SHOW_OF_INTEREST);
        int averageSkillLevel = getCampaign().getPlayerForce()
                                      .getAverageSkillLevel(getCampaign().getCampaignOptions(),
                                            getCampaign().getLocalDate())
                                      .getExperienceLevel();

        calculateNumberOfRecruitmentRolls();

        // Applicants will only try to join the campaign if their experience level is below the average skill level
        // of the campaign minus 2 to a minimum of 2 (Green).
        averageSkillLevel = max(averageSkillLevel - (isOfferingGoldenHello() ? 1 : 2), 2);

        Map<PersonnelRole, PersonnelMarketEntry> unorderedMarketEntries = getCampaign().getPlayerForce().isClanForce() ?
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

            if (!isShowOfInterestDisabled && applicantSkill > averageSkillLevel) {
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

        // Dependents are civilians
        if (isCivilianApplicantsDisabled()) {
            return;
        }

        int dependentsCount = applyApplicantMultiplier(d6());

        for (int roll = 0; roll < dependentsCount; roll++) {
            Faction applicantOriginFaction = getRandomItem(getApplicantOriginFactions());
            Campaign campaign = getCampaign();
            Person applicant = campaign.getPlayerForce()
                                     .getHumanResources()
                                     .newDependent(campaign, Gender.RANDOMIZE, applicantOriginFaction, null);
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
        if (getCampaign().getCampaignOptions().get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL)) {
            // Alt Advanced Medical increases the impact of injuries. Therefore, players need to maintain a larger
            // roster of combat personnel. This multiplier doubles the number of recruits in the pool to account for
            // this.
            rolls *= ALTERNATE_ADVANCED_MEDICAL_RECRUITMENT_MULTIPLIER;
        }

        getLogger().debug("Rolls modified for location: {}", rolls);

        rolls = Math.clamp((int) round(rolls * getSystemPopulationRecruitmentMultiplier()), 1, rolls);
        getLogger().debug("Rolls modified for population: {}", rolls);

        CampaignOptions campaignOptions = getCampaign().getCampaignOptions();
        if (campaignOptions.isUseFactionStandingRecruitmentSafe()) {
            rolls = (int) round(rolls * getFactionStandingsRecruitmentModifier());
        }

        if (campaignOptions.get(CampaignOption.ALLOW_MONTHLY_CONNECTIONS)) {
            int additionalRecruits = performConnectionsRecruitsCheck();
            rolls += additionalRecruits;
        }

        setRecruitmentRolls(applyApplicantMultiplier(max(rolls, 0)));
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
        FactionStandings factionStandings = getCampaign().getPlayerForce().getFactionStandings();

        AbstractLocation location = getCampaign().getPlayerForce().getForceDetachment().getCurrentLocation();
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
        AbstractLocation location = getCampaign().getPlayerForce().getForceDetachment().getCurrentLocation();
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

    /**
     * Gets the cost of hiring an applicant: the recruitment cost Pay for Recruitment charges (see
     * {@link NewPersonnelMarket#getHiringCostBreakdown(Person)}), plus a Golden Hello if one was on offer when the
     * applicant arrived.
     *
     * <p>Personnel are hired without a rank, meaning they have a 0.5 salary multiplier. As a Golden Hello is 12
     * months' salary, it is charged as {@link #GOLDEN_HELLO_SALARY_MONTHS} months of the applicant's salary, less the
     * {@link RecruitmentCosts#SALARY_MONTHS} months a recruit normally costs. The player chose to offer it, so it is
     * charged even when Pay for Recruitment is off.</p>
     *
     * @param applicant the applicant
     *
     * @return the hiring cost, by part
     */
    @Override
    public HiringCostBreakdown getHiringCostBreakdown(Person applicant) {
        Campaign campaign = getCampaign();
        Money goldenHelloExtra = isWasOfferingGoldenHello()
                                       ? applicant.getSalary(campaign.getCampaignOptions(),
                                                     campaign.getPlayerForce().isClanForce(),
                                                     campaign.getLocalDate())
                                               .multipliedBy(GOLDEN_HELLO_SALARY_MONTHS - SALARY_MONTHS)
                                       : Money.zero();
        return getRecruitmentCostBreakdown(applicant, goldenHelloExtra);
    }
}
