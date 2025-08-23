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
package mekhq.campaign.universe.factionStanding;

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.PlanetarySystem;


public class FactionStandingUtilities {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandingUtilities.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandingJudgments";

    private final static String DIALOG_KEY_AFFIX_INNER_SPHERE = "innerSphere";
    private final static String DIALOG_KEY_AFFIX_PERIPHERY = "periphery";
    private final static String DIALOG_KEY_AFFIX_CLAN = "clan";

    public final static String PIRACY_SUCCESS_INDEX_FACTION_CODE = "PSI";

    /**
     * List of personnel roles considered political in the context of censure effects.
     */
    final static List<PersonnelRole> POLITICAL_ROLES = List.of(
          PersonnelRole.MORALE_OFFICER,
          PersonnelRole.LOYALTY_MONITOR,
          PersonnelRole.LOYALTY_AUDITOR);

    /**
     * Determines the {@link FactionStandingLevel} corresponding to the given regard value.
     *
     * <p>Iterates through all defined standing levels and returns the one whose regard range (inclusive of the minimum
     * and maximum regard) contains the provided regard value.</p>
     *
     * <p>If the regard value does not fall within any defined standing level range, this method logs a warning and
     * returns {@link FactionStandingLevel#STANDING_LEVEL_4} as a default.</p>
     *
     * @param regard the regard value to evaluate
     *
     * @return the matching {@code FactionStandingLevel} for the given regard, or {@code STANDING_LEVEL_4} if no match
     *       is found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionStandingLevel calculateFactionStandingLevel(double regard) {
        for (FactionStandingLevel standingLevel : FactionStandingLevel.values()) {
            if (regard >= standingLevel.getMinimumRegard() && regard <= standingLevel.getMaximumRegard()) {
                return standingLevel;
            }
        }

        if (regard > FactionStandingLevel.STANDING_LEVEL_8.getMaximumRegard()) {
            return FactionStandingLevel.STANDING_LEVEL_8;
        }

        if (regard < FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard()) {
            return FactionStandingLevel.STANDING_LEVEL_0;
        }

        // I'm not expecting this to happen, given we already accept all values between Integer#MIN_VALUE and
        // Integer#MAX_VALUE. But if it somehow does, we'll just return STANDING_LEVEL_4 as a default.
        LOGGER.warn("Regard value {} is outside of the faction standing level range. Returning STANDING_LEVEL_4.",
              regard);

        return FactionStandingLevel.STANDING_LEVEL_4;
    }

    /**
     * Retrieves the current standing level based on the provided regard value.
     *
     * @param regard the regard value used to evaluate the faction standing level
     *
     * @return the corresponding standing level as an integer
     *
     * @author Illiani
     * @see FactionStandingLevel#getStandingLevel()
     * @since 0.50.07
     */
    public static int getStandingLevel(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getStandingLevel();
    }

    /**
     * Retrieves the negotiation modifier associated with the provided regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the negotiation modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getNegotiationModifier()
     * @since 0.50.07
     */
    public static int getNegotiationModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getNegotiationModifier();
    }

    /**
     * Returns the resupply weight modifier for the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the resupply weight modifier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getResupplyWeightModifier()
     * @since 0.50.07
     */
    public static double getResupplyWeightModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getResupplyWeightModifier();
    }

    /**
     * Determines if the command circuit access is available at the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return {@code true} if command circuit access is granted; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#hasCommandCircuitAccess()
     * @since 0.50.07
     */
    public static boolean hasCommandCircuitAccess(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.hasCommandCircuitAccess();
    }

    /**
     * Checks whether the specified regard value results in outlawed status.
     *
     * @param regard the regard value to evaluate
     *
     * @return {@code true} if outlawed; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#isOutlawed()
     * @since 0.50.07
     */
    public static boolean isOutlawed(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.isOutlawed();
    }

    /**
     * Checks if Batchalls are allowed for the provided regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return {@code true} if Batchall is allowed; {@code false} otherwise
     *
     * @author Illiani
     * @see FactionStandingLevel#isBatchallAllowed()
     * @since 0.50.07
     */
    public static boolean isBatchallAllowed(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.isBatchallAllowed();
    }

    /**
     * Returns the number of recruitment tickets granted for the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the number of recruitment tickets
     *
     * @author Illiani
     * @see FactionStandingLevel#getRecruitmentTickets()
     * @since 0.50.07
     */
    public static int getRecruitmentTickets(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getRecruitmentTickets();
    }

    /**
     * Returns the recruitment rolls modifier based on the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the recruitment rolls modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getRecruitmentRollsModifier()
     * @since 0.50.07
     */
    public static double getRecruitmentRollsModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getRecruitmentRollsModifier();
    }

    /**
     * Retrieves the barrack costs multiplier for the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the barrack costs multiplier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getBarrackCostsMultiplier()
     * @since 0.50.07
     */
    public static double getBarrackCostsMultiplier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getBarrackCostsMultiplier();
    }

    /**
     * Returns the unit market rarity modifier for the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the unit market rarity modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getUnitMarketRarityModifier()
     * @since 0.50.07
     */
    public static int getUnitMarketRarityModifier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getUnitMarketRarityModifier();
    }

    /**
     * Retrieves the contract pay multiplier corresponding to the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the contract pay multiplier as a double
     *
     * @author Illiani
     * @see FactionStandingLevel#getContractPayMultiplier()
     * @since 0.50.07
     */
    public static double getContractPayMultiplier(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getContractPayMultiplier();
    }

    /**
     * Returns the support point modifier applied at the start of a contract for the given regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the support point modifier for contract start
     *
     * @author Illiani
     * @see FactionStandingLevel#getSupportPointModifierContractStart()
     * @since 0.50.07
     */
    public static int getSupportPointModifierContractStart(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getSupportPointModifierContractStart();
    }

    /**
     * Returns the periodic support point modifier for the specified regard value.
     *
     * @param regard the regard value to evaluate
     *
     * @return the periodic support point modifier
     *
     * @author Illiani
     * @see FactionStandingLevel#getSupportPointModifierPeriodic()
     * @since 0.50.07
     */
    public static int getSupportPointModifierPeriodic(final double regard) {
        final FactionStandingLevel standing = calculateFactionStandingLevel(regard);

        return standing.getSupportPointModifierPeriodic();
    }

    /**
     * Determines whether command circuit access should be granted based on campaign settings, GM mode, current faction
     * standings, and a list of active contracts.
     *
     * <p>Access is immediately granted if both command circuit requirements are overridden and GM mode is
     * active. If not, and if faction standing is used as a criterion, the method evaluates the player's highest faction
     * regard across all active contracts, granting access if this level meets the threshold.</p>
     *
     * <p>If there are no active contracts, access is denied.</p>
     *
     * @param overridingCommandCircuitRequirements {@code true} if command circuit requirements are overridden
     * @param isGM                                 {@code true} if GM mode is enabled
     * @param useFactionStandingCommandCircuit     {@code true} if faction standing is used to determine access
     * @param factionStandings                     player faction standing data
     * @param activeContracts                      list of currently active contracts to evaluate for access
     *
     * @return {@code true} if command circuit access should be used; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static boolean isUseCommandCircuit(boolean overridingCommandCircuitRequirements, boolean isGM,
          boolean useFactionStandingCommandCircuit, FactionStandings factionStandings,
          List<AtBContract> activeContracts) {
        boolean useCommandCircuit = overridingCommandCircuitRequirements && isGM;

        if (useCommandCircuit) {
            return true;
        }

        if (activeContracts.isEmpty()) {
            return false;
        }

        double highestRegard = FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard();
        if (useFactionStandingCommandCircuit) {
            for (AtBContract contract : activeContracts) {
                double currentRegard = factionStandings.getRegardForFaction(contract.getEmployerCode(), true);
                if (currentRegard > highestRegard) {
                    highestRegard = currentRegard;
                }
            }
        }

        useCommandCircuit = hasCommandCircuitAccess(highestRegard);
        return useCommandCircuit;
    }

    /**
     * Determines whether a campaign force is allowed to enter the specified target planetary system, based on
     * population, ownership, outlaw status, contract relationships, and state of war.
     *
     * <p>The rules for entry are as follows:</p>
     * <ol>
     *   <li>If the target system is empty (population zero), entry is always permitted.</li>
     *   <li>If the target system is owned by any faction that is either an employer or a contract target, entry is
     *   allowed.</li>
     *   <li>If the player is outlawed in their current system, they may always exit to another system (unless {@code
     *   currentSystem} is {@code null}).</li>
     *   <li>If the player is outlawed in the target system, entry is denied.</li>
     *   <li>If the campaign faction is at war with all system factions, entry is denied.</li>
     *   <li>If none of the above conditions block entry, it is permitted.</li>
     * </ol>
     *
     * @param campaignFaction    the campaign's primary faction
     * @param factionStandings   the standings of the campaign with all factions
     * @param currentSystem      the planetary system currently occupied
     * @param targetSystem       the planetary system to test entry for
     * @param when               the date of attempted entry (population/ownership may change over time)
     * @param activeAtBContracts list of currently active contracts
     * @param factionHints       the details of the current factional relations
     *
     * @return {@code true} if entry to the target system is allowed; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static boolean canEnterTargetSystem(Faction campaignFaction, FactionStandings factionStandings,
          @Nullable PlanetarySystem currentSystem, PlanetarySystem targetSystem, LocalDate when,
          List<AtBContract> activeAtBContracts, FactionHints factionHints) {
        // Always allowed in empty systems
        if (targetSystem.getPopulation(when) == 0) {
            LOGGER.debug("Target system is empty, access granted");
            return true;
        }

        Set<Faction> systemFactions = targetSystem.getFactionSet(when);

        Set<Faction> contractEmployers = new HashSet<>();
        Set<Faction> contractTargets = new HashSet<>();
        for (AtBContract contract : activeAtBContracts) {
            contractEmployers.add(contract.getEmployerFaction());
            contractTargets.add(contract.getEnemy());
        }

        // Entry always allowed if the system is owned by any contract employer or target
        if (systemFactions.stream()
                  .anyMatch(systemFaction -> contractEmployers.contains(systemFaction)
                                                   || contractTargets.contains(systemFaction))) {
            LOGGER.debug("SystemFluff is owned by a contract employer or target, access granted");
            return true;
        }

        // Always allowed to leave if outlawed in the current system
        if (currentSystem != null) {
            if (isOutlawedInSystem(factionStandings, currentSystem, when)) {
                LOGGER.debug("Player is outlawed in current system, but always allowed to escape, access granted");
                return true;
            }
        }

        // Banned if outlawed in the target system
        if (isOutlawedInSystem(factionStandings, targetSystem, when)) {
            LOGGER.debug("Player is outlawed in target system, access denied");
            return false;
        }

        // Disallow if the campaign faction is at war with all system factions
        boolean allAtWarWithCampaign = systemFactions
                                             .stream()
                                             .allMatch(systemFaction -> factionHints.isAtWarWith(campaignFaction,
                                                   systemFaction,
                                                   when));
        if (allAtWarWithCampaign) {
            LOGGER.debug("Campaign faction is at war with all system factions, access denied");
            return false;
        }

        LOGGER.debug("Access granted");
        return true;
    }

    /**
     * Determines whether a faction is outlawed in the specified target system at a given date.
     *
     * <p>This method evaluates the highest standing ("regard") among all factions present in the target planetary
     * system on the specified date. It then checks whether the faction corresponding to the highest regard is
     * considered outlawed.</p>
     *
     * @param factionStandings the faction standings data to use for regard calculations
     * @param targetSystem     the planetary system in which to perform the check
     * @param when             the date for which to determine outlaw status
     *
     * @return {@code true} if the faction is outlawed in the target system at the given date; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static boolean isOutlawedInSystem(FactionStandings factionStandings, PlanetarySystem targetSystem,
          LocalDate when) {
        double highestRegard = FactionStandingLevel.STANDING_LEVEL_0.getMinimumRegard();
        for (Faction faction : targetSystem.getFactionSet(when)) {
            double currentRegard = factionStandings.getRegardForFaction(faction.getShortName(), true);
            if (currentRegard > highestRegard) {
                highestRegard = currentRegard;
            }
        }

        return isOutlawed(highestRegard);
    }

    /**
     * Checks whether the campaign is presently undertaking a mission for the specified faction.
     *
     * <p>This method verifies all the following conditions to determine mission status:</p>
     * <ul>
     *     <li>The campaign must currently be located on a planet.</li>
     *     <li>There must be at least one active AtB (Against the Bot) contract.</li>
     *     <li>At least one such AtB contract must have both an employer code matching the specified faction and a
     *     system matching the current location.</li>
     *     <li>Alternatively, the presence of any active mission also qualifies as being on a mission for the
     *     faction. This is to ensure compatibility with non-AtB campaigns.</li>
     * </ul>
     *
     * <p>Returns {@code true} if these checks indicate the campaign is actively on a mission or contract
     * corresponding to the specified faction.</p>
     *
     * @param isOnPlanet         whether the campaign is currently on a planet
     * @param activeAtBContracts list of all currently active AtB contracts
     * @param activeMissions     list of all currently active missions
     * @param factionCode        the code identifying the relevant faction
     * @param currentSystem      the planetary system in which the campaign is currently located
     * @param ignoreEmployer     whether the contract employer faction should be ignored
     *
     * @return {@code true} if the campaign is on a qualifying mission for the given faction; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static boolean isIsOnMission(boolean isOnPlanet, List<AtBContract> activeAtBContracts,
          List<Mission> activeMissions, String factionCode, PlanetarySystem currentSystem, boolean ignoreEmployer) {
        if (!isOnPlanet) {
            return false;
        }

        // Check if there are any active missions
        if (activeMissions.isEmpty()) {
            return false;
        }

        // Check if AtB contracts are not disabled and at least one matches the current system
        for (AtBContract contract : activeAtBContracts) {
            if (!ignoreEmployer) {
                if (!contract.getEmployerCode().equals(factionCode)) {
                    continue;
                }
            }

            if (contract.getSystem().equals(currentSystem)) {
                return true;
            }
        }

        if (!activeAtBContracts.isEmpty()) {
            return false;
        }

        // Check if there are any active missions
        return !activeMissions.isEmpty();
    }

    /**
     * Processes a mass change in loyalty for all relevant personnel, typically in response to a major positive or
     * negative censure outcome.
     *
     * @param campaign         the campaign instance
     * @param isMajor          whether this is a major change
     * @param isPositiveChange {@code true} for positive, {@code false} for negative shifts
     *
     * @author Illiani
     * @since 0.50.07
     */
    static void processMassLoyaltyChange(Campaign campaign, boolean isMajor, boolean isPositiveChange) {
        if (!campaign.getCampaignOptions().isUseLoyaltyModifiers()) {
            return;
        }

        LocalDate today = campaign.getLocalDate();
        for (Person person : campaign.getPersonnel()) {
            if (isExempt(person, today)) {
                continue;
            }

            person.performForcedDirectionLoyaltyChange(campaign, isPositiveChange, isMajor, false);
        }
    }

    /**
     * Determines if a person is exempt from certain censure actions on the given date.
     *
     * @param person the person to evaluate
     * @param today  the current date
     *
     * @return {@code true} if the person is exempt, {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    static boolean isExempt(Person person, LocalDate today) {
        if (person.getStatus().isDepartedUnit()) {
            return true;
        }

        if (person.isChild(today)) {
            return true;
        }

        if (!person.isEmployed()) {
            return true;
        }

        return person.getPrisonerStatus().isFreeOrBondsman();
    }

    /**
     * Returns the formatted full name of a {@link Faction} for the specified game year.
     *
     * <p>If the faction's name starts with the localized "clan" prefix, the method returns the full name as-is.
     * Otherwise, the localized "the" article is prefixed to the base name. This helps ensure proper grammatical usage
     * for varying factions based on localization and faction naming conventions.</p>
     *
     * @param faction  the {@link Faction} whose name should be formatted
     * @param gameYear the year for which the faction's full name is relevant
     *
     * @return the formatted faction name, including the appropriate localized prefix if necessary
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getFactionName(Faction faction, int gameYear) {
        final String CLAN = getTextAt(RESOURCE_BUNDLE, "FactionStandingUtilities.clan");
        final String THE = getTextAt(RESOURCE_BUNDLE, "FactionStandingUtilities.the");

        String baseName = faction.getFullName(gameYear);
        if (baseName.startsWith(CLAN)) {
            return baseName;
        }

        // Add additional conditionals here as necessary.

        return THE + ' ' + baseName;
    }

    /**
     * Constructs and returns the full display name for a given person.
     *
     * <p>The name is assembled in the following order: if present: rank title, given name, bloodname (if available),
     * or surname (if bloodname is not present). This attempts to produce a canonical or ceremonial name as used in
     * settings with titles and bloodnames.</p>
     *
     * @param person the {@link Person} whose full name should be generated
     *
     * @return the assembled full display name as a {@link String}
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getCharacterFullName(Person person) {
        String name = "";

        String title = person.getRankName();
        if (title != null && !title.isBlank()) {
            name = title;
        }

        String firstName = person.getGivenName();
        if (!name.isBlank()) {
            name += " " + firstName;
        } else {
            name = firstName;
        }

        String bloodname = person.getBloodname();
        if (bloodname != null && !bloodname.isBlank()) {
            name += " " + bloodname;
        }

        String surname = person.getSurname();
        if ((bloodname == null || bloodname.isBlank())
                  && (surname != null && !surname.isBlank())) {
            name += " " + surname;
        }

        return name;
    }

    /**
     * Generates a localized, in-character narrative text for a faction standing event, dynamically incorporating
     * identity and pronoun information for one or two characters, as well as campaign and faction context.
     *
     * <p>This method prepares a set of arguments including hyperlinked titles, given names, context-aware pronouns
     * (subject, object, possessive), and additional identifiers for the commander and (optionally) a secondary
     * individual. It also includes campaign-specific details like the campaign and faction name. The arguments are
     * formatted into a resource bundle string for display within campaign dialogs.</p>
     *
     * @param resourceBundle        the resource bundle address for localization
     * @param resourceBundleAddress the resource bundle reference key
     * @param commander             the main commander or subject of the censure event; may be {@code null} if no
     *                              commander is cited in the dialog.
     * @param secondCharacter       optional secondary character affected by the event; may be {@code null} if there
     *                              isn't a second character in the scene.
     * @param factionName           the name of any relevant faction
     * @param campaignName          the name of the current campaign
     * @param locationName          the name of the relevant system or planet; may be {@code null} if there isn't any
     *                              locational information in the scene.
     *
     * @return a formatted narrative {@link String} populated with character and context
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getInCharacterText(@Nullable String resourceBundle, String resourceBundleAddress,
          @Nullable Person commander, @Nullable Person secondCharacter, String factionName, String campaignName,
          String locationName, @Nullable Integer cashValue, String commanderAddress) {

        // We use fallback values so that we don't have to deal with null values
        final Gender FALLBACK_GENDER = Gender.MALE;
        final String FALLBACK_NAME = "";

        // COMMANDER pronoun/identity context
        final PronounData commanderPronounData = new PronounData(commander == null
                                                                       ? FALLBACK_GENDER
                                                                       : commander.getGender());
        // {0} full title
        final String commanderHyperlinkedFullTitle = commander == null
                                                           ? FALLBACK_NAME
                                                           : getCharacterFullName(commander);
        // {1} first name
        final String commanderFirstName = commander == null
                                                ? FALLBACK_NAME
                                                : commander.getGivenName();
        // {23} = full name, no title (out of numerical order because it was added much later)
        final String commanderFullName = commander == null
                                               ? FALLBACK_NAME
                                               : commander.getFullName();
        // {2} = He/She/They
        final String commanderHeSheTheyCapitalized = commanderPronounData.subjectPronoun();
        // {3} = he/she/they
        final String commanderHeSheTheyLowercase = commanderPronounData.subjectPronounLowerCase();
        // {4} = Him/Her/Them
        final String commanderHimHerThemCapitalized = commanderPronounData.objectPronoun();
        // {5} = him/her/them
        final String commanderHimHerThemLowercase = commanderPronounData.objectPronounLowerCase();
        // {6} = His/Her/Their
        final String commanderHisHerTheirCapitalized = commanderPronounData.possessivePronoun();
        // {7} = his/her/their
        final String commanderHisHerTheirLowercase = commanderPronounData.possessivePronounLowerCase();
        // {8} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)
        final int commanderPluralizer = commander == null ? 0 : commanderPronounData.pluralizer();

        // SECOND pronoun/identity context
        final PronounData secondPronounData = new PronounData(secondCharacter == null
                                                                    ? FALLBACK_GENDER
                                                                    : secondCharacter.getGender());
        // {9} full title
        final String secondHyperlinkedFullTitle = secondCharacter == null
                                                        ? FALLBACK_NAME
                                                        : getCharacterFullName(secondCharacter);
        // {10} first name
        final String secondFirstName = secondCharacter == null
                                             ? FALLBACK_NAME
                                             : secondCharacter.getGivenName();
        // {11} = He/She/They
        final String secondHeSheTheyCapitalized = secondPronounData.subjectPronoun();
        // {12} = he/she/they
        final String secondHeSheTheyLowercase = secondPronounData.subjectPronounLowerCase();
        // {13} = Him/Her/Them
        final String secondHimHerThemCapitalized = secondPronounData.objectPronoun();
        // {14} = him/her/them
        final String secondHimHerThemLowercase = secondPronounData.objectPronounLowerCase();
        // {15} = His/Her/Their
        final String secondHisHerTheirCapitalized = secondPronounData.possessivePronoun();
        // {16} = his/her/their
        final String secondHisHerTheirLowercase = secondPronounData.possessivePronounLowerCase();
        // {17} = Gender Neutral = 0, Otherwise 1 (used to determine whether to use a plural case)
        final int secondPluralizer = secondCharacter == null ? 0 : secondPronounData.pluralizer();

        // Miscellaneous campaign context
        // {18} = campaign name
        // {19} = faction name
        // {20} = location name
        if (locationName == null) {
            locationName = FALLBACK_NAME;
        }
        // {21} = cash value (in millions)
        if (cashValue == null) {
            cashValue = 0;
        }
        // {22} = commander address

        // Format and return the localized dialog text with the current context.
        return getFormattedTextAt(resourceBundle == null ? RESOURCE_BUNDLE : resourceBundle, resourceBundleAddress,
              commanderHyperlinkedFullTitle, commanderFirstName, commanderHeSheTheyCapitalized,
              commanderHeSheTheyLowercase, commanderHimHerThemCapitalized, commanderHimHerThemLowercase,
              commanderHisHerTheirCapitalized, commanderHisHerTheirLowercase, commanderPluralizer,
              secondHyperlinkedFullTitle, secondFirstName, secondHeSheTheyCapitalized, secondHeSheTheyLowercase,
              secondHimHerThemCapitalized, secondHimHerThemLowercase, secondHisHerTheirCapitalized,
              secondHisHerTheirLowercase, secondPluralizer, campaignName, factionName, locationName, cashValue,
              commanderAddress, commanderFullName);
    }

    /**
     * Generates a fallback dialog key by replacing the judging faction's code in the original dialog key with a
     * suitable default affix, based on the general type of the judging faction.
     *
     * <p>If the judging faction is a Clan, Periphery, or Inner Sphere, the corresponding constant affix is
     * substituted. This is useful for providing default or generic dialog text when no faction-specific version is
     * available.</p>
     *
     * @param dialogKey      the original dialog key, typically with a faction code suffix
     * @param judgingFaction the {@link Faction} being used to determine the fallback key
     *
     * @return a fallback dialog key string with the appropriate affix for the faction type
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static String getFallbackFactionKey(String dialogKey, Faction judgingFaction) {
        String affixKey;

        if (judgingFaction.isClan()) {
            affixKey = DIALOG_KEY_AFFIX_CLAN;
        } else if (judgingFaction.isPeriphery()) {
            affixKey = DIALOG_KEY_AFFIX_PERIPHERY;
        } else {
            affixKey = DIALOG_KEY_AFFIX_INNER_SPHERE;
        }

        String judgingFactionCode = judgingFaction.getShortName();

        return dialogKey.replace('.' + judgingFactionCode, '.' + affixKey);
    }
}
