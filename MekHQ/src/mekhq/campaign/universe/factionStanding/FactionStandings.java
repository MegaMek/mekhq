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

import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionStanding.enums.FactionStandingLevel;
import mekhq.utilities.MHQXMLUtility;
import mekhq.utilities.ReportingUtilities;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stores and manages the standing values between factions in the Faction Standings system.
 *
 * <p>A {@link FactionStandings} object tracks the current fame values for all relevant factions using faction codes
 * as keys and numeric fame as values. Values may be positive (good reputation), negative (bad reputation), or zero
 * (neutral). This class provides functionality to initialize standings according to relationships, adjust and degrade
 * fame values, serialize data to XML, and reconstruct state from XML.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandings {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandings.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    /**
     * This value defines the upper limit of fame a campaign can achieve with a faction.
     */
    static final double MAXIMUM_FAME = 120.0;

    /**
     * A constant representing the minimum fame a campaign can have with a faction.
     */
    static final double MINIMUM_FAME = -120.0;

    /**
     * The base fame value for all factions.
     */
    static final double DEFAULT_FAME = 0.0;

    /**
     * The amount by which fame degrades over time.
     */
    static final double DEFAULT_FAME_DEGRADATION = 0.25;

    /**
     * The starting fame for the campaign's faction
     */
    static final double STARTING_FAME_SAME_FACTION = DEFAULT_FAME + 25;

    /**
     * The starting fame for factions that are allies of the campaign faction.
     */
    static final double STARTING_FAME_ALLIED_FACTION = STARTING_FAME_SAME_FACTION / 2;

    /**
     * The starting fame for factions that are rivals of the campaign faction.
     */
    static final double STARTING_FAME_ENEMY_FACTION_RIVAL = DEFAULT_FAME - 25;

    /**
     * The starting fame for factions that are at war with the campaign faction.
     */
    static final double STARTING_FAME_ENEMY_FACTION_AT_WAR = DEFAULT_FAME - 40;

    /**
     * Fame increase for successfully completing a contract for the employer.
     */
    static final double FAME_DELTA_CONTRACT_SUCCESS_EMPLOYER = 5.0;

    /**
     * Fame increase for successfully completing a contract for factions allied with the employer.
     */
    static final double FAME_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY = 1.0;

    /**
     * Fame increase for completing a 'partial success' contract for the employer.
     */
    static final double FAME_DELTA_CONTRACT_PARTIAL_EMPLOYER = 1.0;

    /**
     * Fame increase for completing a 'partial success' contract for factions allied with the employer.
     */
    static final double FAME_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY = 0.2;

    /**
     * Fame penalty for failing a contract for the employer.
     */
    static final double FAME_DELTA_CONTRACT_FAILURE_EMPLOYER = -1.0;

    /**
     * Fame penalty for completing a 'partial success' contract for factions allied with the employer.
     */
    static final double FAME_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY = -0.2;

    /**
     * Fame penalty for breaching a contract (employer).
     */
    static final double FAME_DELTA_CONTRACT_BREACH_EMPLOYER = -10.0;

    /**
     * Fame penalty for breaching a contract (employer's allies).
     */
    static final double FAME_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY = -2;

    /**
     * Fame decrease when accepting a contract against a non-Clan enemy.
     */
    static final double FAME_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL = -5.0;

    /**
     * Fame decrease when accepting a contract against a Clan enemy.
     */
    static final double FAME_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN = -2.5;

    /**
     * Fame decrease when accepting a contract for non-Clan factions allied with the enemy.
     */
    static final double FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL = -1.0;

    /**
     * Fame decrease when accepting a contract for Clan factions allied with the enemy.
     */
    static final double FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN = -0.2;

    /**
     * Fame penalty for refusing a batchall.
     */
    static final double FAME_DELTA_REFUSE_BATCHALL = -5;

    /**
     * Fame penalty for refusing a batchall.
     */
    static final double FAME_DELTA_EXECUTING_PRISONER = -0.1;

    /**
     * A mapping of faction names to their respective standing levels.
     *
     * <p>This variable is used to store and track the Fame score of factions the campaign has interacted with.</p>
     *
     * <p><b>Key:</b></p> A {@link String} representing the shortname of the faction (aka Faction Code).
     * <p><b>Value:</b></p> A {@link Double} representing the campaign's Fame with that faction.
     */
    private Map<String, Double> factionStandings;

    /**
     * Constructs an empty standings map. No initial relationships or fame values are set.
     *
     * <p><b>Usage:</b> this does not populate the 'standing' map with any values. That has to be handled
     * separately.</p>
     *
     * <p>If we're starting a new campaign, we should follow up object construction with a call to
     * {@link #initializeStartingFameValues(Faction, LocalDate)}</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandings() {
        this.factionStandings = new HashMap<>();
    }

    /**
     * @return the maximum fame the campaign can have with a faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getMaximumFame() {
        return MAXIMUM_FAME;
    }

    /**
     * @return the minimum fame the campaign can have with a faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getMinimumFame() {
        return MINIMUM_FAME;
    }

    /**
     * Initializes faction fame standings at the start of a campaign or when performing a full reset.
     *
     * <p>This method sets up initial Fame values for all factions relative to the given campaign faction on a
     * specified date. Direct allies, direct enemies, and secondary relationships (such as allies of enemies) are
     * each assigned distinct starting fame values, determined by the configuration in {@link FactionStandingLevel}.</p>
     *
     * <p>The process is performed in two passes:</p>
     * <ul>
     *     <li><b>First pass:</b> Identifies and assigns Fame to the campaign faction itself, direct allies, and
     *     direct enemies. Allies receive a positive Fame boost, enemies receive a negative one, and the campaign
     *     faction starts with a high positive Fame.</li>
     *     <li><b>Second pass:</b> Assigns intermediate fame values to factions indirectly related to the campaign
     *     faction (such as allies of enemies), while skipping those already processed in the first pass.</li>
     * </ul>
     *
     * <p>This method is intended primarily for initializing a new campaign or completely resetting the standings.
     * To maintain player progress, it should not be used for incremental changes during an active campaign.</p>
     *
     * @param campaignFaction the main faction from which all relationships are evaluated
     * @param today the current campaign date, used to determine relationships between factions
     * @return a list of formatted report strings describing each fame value that was set during initialization;
     *         one entry per modified faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> initializeStartingFameValues(final Faction campaignFaction, final LocalDate today) {
        List<String> fameChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        String report;
        for (Faction otherFaction : allFactions) {
            if (isUntrackedFaction(otherFaction)) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();
            if (otherFaction.equals(campaignFaction)) {
                report = changeFameForFaction(otherFactionCode, STARTING_FAME_SAME_FACTION, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
                continue;
            }

            if (factionHints.isAlliedWith(campaignFaction, otherFaction, today)) {
                report = changeFameForFaction(otherFactionCode, STARTING_FAME_ALLIED_FACTION, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                    continue;
                }
            }

            if (factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                report = changeFameForFaction(otherFactionCode, STARTING_FAME_ENEMY_FACTION_AT_WAR, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
            }

            if (factionHints.isRivalOf(campaignFaction, otherFaction, today)) {
                report = changeFameForFaction(otherFactionCode, STARTING_FAME_ENEMY_FACTION_RIVAL, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
            }
        }

        return fameChangeReports;
    }

    /**
     * Determines if the specified faction is considered "untracked."
     *
     * <p>A faction is untracked if it is classified as a mercenary, rebel or pirate, corporation, or independent.</p>
     *
     * @param otherFaction the {@link Faction} to check
     *
     * @return {@code true} if the faction is mercenary, rebel or pirate, corporation, or independent; {@code false}
     *       otherwise
     */
    public static boolean isUntrackedFaction(Faction otherFaction) {
        return otherFaction.isMercenary() ||
                     otherFaction.isRebelOrPirate() ||
                     otherFaction.isCorporation() ||
                     otherFaction.isIndependent();
    }

    /**
     * Replaces the current map of faction standings with the provided map.
     *
     * <p>Existing contents are discarded. After this call, only the entries in the given map remain.</p>
     *
     * @param factionStandings the new map of faction codes to fame values
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFactionStandings(Map<String, Double> factionStandings) {
        this.factionStandings = factionStandings;
    }

    /**
     * Retrieves all current faction standings.
     *
     * @return a {@link Map} containing all faction codes mapped to their current fame values.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Map<String, Double> getAllFactionStandings() {
        return factionStandings;
    }

    /**
     * Retrieves the current fame value for the specified faction.
     *
     * @param factionCode a unique code identifying the faction
     *
     * @return the fame value for the faction, or 0 if none is present
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getFameForFaction(final String factionCode) {
        return factionStandings.getOrDefault(factionCode, DEFAULT_FAME);
    }

    /**
     * Sets the fame value for the specified faction, directly assigning (or overwriting) the value.
     *
     * <p>If the faction code does not already exist, a new entry is created.</p>
     *
     * @param factionCode a unique code identifying the faction
     * @param fame        the fame (standing) value to assign
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFameForFaction(final String factionCode, final double fame) {
        double fameValue = clamp(fame, MINIMUM_FAME, MAXIMUM_FAME);
        factionStandings.put(factionCode, fameValue);
    }

    /**
     * Adjusts the fame value for a specified faction by a given amount and generates a detailed report of any change.
     *
     * <p>Retrieves the current fame of the specified faction and alters it by {@code delta}. If the faction does not
     * exist in the standings, it is initialized with the specified delta value. The method determines if this
     * adjustment causes the faction to cross a standing milestone, as defined in {@link FactionStandingLevel}. If a
     * milestone transition occurs, the report includes a message highlighting this change. The generated report uses
     * color formatting to indicate the direction of change (increase or decrease) and displays the faction’s full
     * name for the current game year.</p>
     *
     * <p>If {@code delta} is zero, the method leaves fame and milestones unchanged and returns an empty string.</p>
     *
     * @param factionCode unique identifier for the faction whose fame should be adjusted
     * @param delta the amount to increment or decrement the faction's fame (can be positive or negative)
     * @param gameYear the current in-game year, affecting how faction names are displayed in reports
     * @return a formatted {@link String} describing the fame change and any milestone transition, or an empty string
     * if {@code delta} is zero
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String changeFameForFaction(final String factionCode, final double delta, final int gameYear) {
        if (delta == 0) {
            LOGGER.debug("A change of 0 Fame requested for {}. Shortcutting the method.", factionCode);
            return "";
        }

        double originalFame = getFameForFaction(factionCode);
        double newFame = clamp(originalFame + delta, MINIMUM_FAME, MAXIMUM_FAME);

        factionStandings.put(factionCode, newFame);

        return getFameChangedReport(delta, gameYear, factionCode, newFame, originalFame);
    }

    /**
     * Builds a formatted report string describing changes to a faction's fame and any milestone transitions.
     *
     * <p>This method generates detailed feedback about a Fame value adjustment for a faction, including whether a
     * milestone ({@link FactionStandingLevel}) has changed as a result. The report text uses color formatting to
     * visually indicate the change's nature (positive, negative, or neutral); includes the faction's name for the
     * current game year; the direction and magnitude of the fame change; and a message about the milestone
     * status—whether a new milestone was reached or the faction remains within the same milestone.</p>
     *
     * <p>If the relevant faction is not present, a default faction is used for milestone reporting.</p>
     *
     * <p>An additional prefix may be applied to the faction name.</p>
     *
     * @param delta        the amount of Fame gained or lost
     * @param gameYear     the current in-game year, used to render the appropriate faction name
     * @param factionCode  unique identifier for the faction whose Fame should be adjusted
     * @param newFame      the Fame value after the delta is applied
     * @param originalFame the Fame value before the delta is applied
     *
     * @return a formatted {@link String} describing the fame change, direction, and any milestone transition
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getFameChangedReport(final double delta, final int gameYear, final String factionCode, final double newFame,
                                        final double originalFame) {
        Faction relevantFaction = Factions.getInstance().getFaction(factionCode);
        FactionStandingLevel originalMilestone = FactionStandingUtilities.calculateFactionStandingLevel(originalFame);
        FactionStandingLevel newMilestone = FactionStandingUtilities.calculateFactionStandingLevel(newFame);

        String reportingColor;
        String milestoneChangeReport;
        if (originalMilestone != newMilestone) {
            if (relevantFaction == null) {
                relevantFaction = Factions.getInstance().getDefaultFaction();
            }

            if (newMilestone.getStandingLevel() > originalMilestone.getStandingLevel()) {
                reportingColor = ReportingUtilities.getPositiveColor();
            } else {
                reportingColor = ReportingUtilities.getNegativeColor();
            }

            milestoneChangeReport = getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandings.change.report.milestone.new",
                  spanOpeningWithCustomColor(reportingColor),
                  newMilestone.getLabel(relevantFaction),
                  CLOSING_SPAN_TAG);
        } else {
            reportingColor = ReportingUtilities.getWarningColor();

            milestoneChangeReport = getFormattedTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.milestone.same",
                  spanOpeningWithCustomColor(reportingColor),
                  newMilestone.getLabel(relevantFaction),
                  CLOSING_SPAN_TAG);
        }

        // Build final report
        String deltaDirection;
        if (newFame > originalFame) {
            reportingColor = ReportingUtilities.getPositiveColor();
            deltaDirection = getTextAt(RESOURCE_BUNDLE, "factionStandings.change.increased");
        } else {
            reportingColor = ReportingUtilities.getNegativeColor();
            deltaDirection = getTextAt(RESOURCE_BUNDLE, "factionStandings.change.decreased");
        }

        String factionName = relevantFaction == null ?
                                   getTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.unknownFaction") :
                                   relevantFaction.getFullName(gameYear);
        if (!factionName.contains(getTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.clan.check"))) {
            factionName = getTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.clan.prefix") + ' ' + factionName;
        }

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "factionStandings.change.report", factionName, spanOpeningWithCustomColor(reportingColor),
              deltaDirection,
              CLOSING_SPAN_TAG, delta,
              milestoneChangeReport);
    }

    /**
     * Clears all faction standings, removing all records.
     *
     * <p>After this call, the 'standings' map is empty and no reputations are stored.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void wipeAllFactionStandings() {
        factionStandings.clear();
    }

    /**
     * Removes the standing entry for a single faction only.
     *
     * @param factionCode the code of the faction to remove
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void resetFactionStanding(final String factionCode) {
        factionStandings.remove(factionCode);
    }

    /**
     * Gradually reduces all non-zero faction fame values toward zero by a fixed increment, simulating fame decay over time.
     *
     * <p>For each faction with a non-zero fame value, the method decrements positive values and increments negative
     * values by a fixed amount. This step-wise adjustment continues fame's progression toward zero, with fame being
     * set to exactly zero if it otherwise crosses zero, thus preventing overshooting. For each adjustment, a report
     * string is generated if an actual change occurs.</p>
     *
     * <p>This method is typically called annually to model the natural decline of relationships or reputation over
     * time.</p>
     *
     * @param gameYear the current in-game year, used for proper display of faction names in reports
     * @return a list of formatted report strings describing each fame change made during this process; one entry per
     * modified faction, or an empty list if no changes occurred
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processFameDegradation(final int gameYear) {
        List<String> fameChangeReports = new ArrayList<>();
        LOGGER.info("Processing fame decay for {} factions.", factionStandings.size());
        for (String factionCode : new HashSet<>(factionStandings.keySet())) {
            double currentFame = factionStandings.get(factionCode);

            if (currentFame != DEFAULT_FAME) {
                double delta = currentFame > DEFAULT_FAME ? -DEFAULT_FAME_DEGRADATION : DEFAULT_FAME_DEGRADATION;
                String report = changeFameForFaction(factionCode, delta, gameYear);
                double newFame = getFameForFaction(factionCode);

                if ((currentFame > DEFAULT_FAME && newFame < DEFAULT_FAME) ||
                          (currentFame < DEFAULT_FAME && newFame > DEFAULT_FAME)) {
                    setFameForFaction(factionCode, DEFAULT_FAME);
                }

                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
            }
        }

        return fameChangeReports;
    }

    /**
     * Processes the acceptance of a contract against a specified enemy faction and applies fame changes to all relevant
     * factions.
     *
     * <p>This method iterates through all factions in the game, adjusting their fame values based on relationships to
     * the specified enemy faction and the contract's context. Fame deltas are applied for the enemy faction itself, as
     * well as any factions allied with the enemy. The changes vary depending on whether the factions are clans or
     * non-clans, and different fame penalties are applied accordingly.</p>
     *
     * <p>For each application of a fame delta, a report string is generated and included in the result list if it is
     * not blank.</p>
     *
     * @param enemyFaction The {@link Faction} representing the enemy against whom the contract is accepted.
     * @param today        The {@link LocalDate} representing the game date on which the contract is accepted.
     *
     * @return A {@link List} of {@link String} objects summarizing any fame changes that occurred as a result of
     *       accepting the contract.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processContractAccept(@Nullable final Faction enemyFaction, final LocalDate today) {
        // If we're missing the relevant faction, alert the player and abort
        if (enemyFaction == null) {
            String report = getMissingFactionReport(false);

            return List.of(report);
        }

        List<String> fameChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        String report;
        double fameDelta;
        for (Faction otherFaction : allFactions) {
            if (isUntrackedFaction(otherFaction)) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();
            if (otherFaction.equals(enemyFaction)) {
                if (otherFaction.isClan()) {
                    fameDelta = FAME_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN;
                } else {

                    fameDelta = FAME_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL;
                }

                report = changeFameForFaction(otherFactionCode, fameDelta, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
                continue;
            }

            if (factionHints.isAlliedWith(enemyFaction, otherFaction, today)) {
                if (otherFaction.isClan()) {
                    fameDelta = FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN;
                } else {

                    fameDelta = FAME_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL;
                }

                report = changeFameForFaction(otherFactionCode, fameDelta, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
            }
        }

        return fameChangeReports;
    }

    /**
     * Processes the outcome of a contract upon its completion and updates fame standings accordingly.
     *
     * <p>Depending on the mission status (success, partial, failure, or breach), this method determines the appropriate
     * fame delta for the employer faction and its allies.</p>
     *
     * <p>If the employer faction is missing, a report is generated and returned accordingly. This report informs the
     * player that they need to manually apply the Standing change via the Standing Report GUI.</p>
     *
     * <p>Fame changes are applied to the employer and all allied factions, and corresponding report strings are
     * returned for each fame change applied.</p>
     *
     * @param employerFaction The {@link Faction} that employed the contract, or {@code null} if unavailable.
     * @param today           The {@link LocalDate} representing the date of contract completion.
     * @param missionStatus   The {@link MissionStatus} of the contract upon completion.
     * @return A {@link List} of strings summarizing any fame changes or messages relating to missing factions.
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processContractCompletion(@Nullable final Faction employerFaction, final LocalDate today,
                                                  final MissionStatus missionStatus) {
        // If the mission is still active, there is nothing to process, so abort
        if (missionStatus == MissionStatus.ACTIVE) {
            return new ArrayList<>();
        }

        // If we're missing the relevant faction, alert the player and abort
        if (employerFaction == null) {
            String report = getMissingFactionReport(true);

            return List.of(report);
        }

        double fameDeltaEmployer = 0.0;
        double fameDeltaEmployerAlly = 0.0;
        switch (missionStatus) {
            case SUCCESS -> {
                fameDeltaEmployer = FAME_DELTA_CONTRACT_SUCCESS_EMPLOYER;
                fameDeltaEmployerAlly = FAME_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY;
            }
            case PARTIAL -> {
                fameDeltaEmployer = FAME_DELTA_CONTRACT_PARTIAL_EMPLOYER;
                fameDeltaEmployerAlly = FAME_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY;
            }
            case FAILED -> {
                fameDeltaEmployer = FAME_DELTA_CONTRACT_FAILURE_EMPLOYER;
                fameDeltaEmployerAlly = FAME_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY;
            }
            case BREACH -> {
                fameDeltaEmployer = FAME_DELTA_CONTRACT_BREACH_EMPLOYER;
                fameDeltaEmployerAlly = FAME_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY;
            }
        }

        // If there is no change to make, we exit early so as not to process faction data needlessly
        if ((fameDeltaEmployer + fameDeltaEmployerAlly) == 0.0) {
            return new ArrayList<>();
        }

        List<String> fameChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        String report;
        for (Faction otherFaction : allFactions) {
            if (isUntrackedFaction(otherFaction)) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();
            if (otherFaction.equals(employerFaction)) {
                report = changeFameForFaction(otherFactionCode, fameDeltaEmployer, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
                continue;
            }

            if (factionHints.isAlliedWith(employerFaction, otherFaction, today)) {
                report = changeFameForFaction(otherFactionCode, fameDeltaEmployerAlly, gameYear);
                if (!report.isBlank()) {
                    fameChangeReports.add(report);
                }
            }
        }

        return fameChangeReports;
    }

    /**
     * Generates a report message indicating that the relevant faction (employer or enemy) is missing.
     *
     * <p>The message varies depending on whether the context is contract completion or acceptance.</p>
     *
     * @param isContractCompletion {@code true} if the report is for contract completion, {@code false} for contract acceptance.
     * @return A {@link String} representing the formatted report message for the missing faction scenario.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getMissingFactionReport(final boolean isContractCompletion) {
        String contractStatus = isContractCompletion ? "completion" : "acceptance";
        String relevantFaction = isContractCompletion ? "employer" : "enemy";

        return getFormattedTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.missingFaction",
                spanOpeningWithCustomColor(ReportingUtilities.getWarningColor()), CLOSING_SPAN_TAG,
                getTextAt(RESOURCE_BUNDLE, "factionStandings.change." + contractStatus),
                getTextAt(RESOURCE_BUNDLE, "factionStandings.change." + relevantFaction));
    }

    /**
     * Processes the penalty for refusing a batchall against a specific Clan faction.
     *
     * <p>This method applies a fame penalty to the given clan faction code for the specified year and generates a fame
     * change report if applicable.</p>
     *
     * <p>This method is included as a shortcut to allow developers to call Batchall refusal changes without needing to
     * worry about setting up bespoke methods any time this could occur.</p>
     *
     * @param clanFactionCode The code representing the clan faction being penalized.
     * @param gameYear        The year in which the batchall was refused.
     * @return A {@link List} of fame change report strings relating to the refusal.
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processRefusedBatchall(final String clanFactionCode, final int gameYear) {
        List<String> fameChangeReports = new ArrayList<>();

        String report = changeFameForFaction(clanFactionCode, FAME_DELTA_REFUSE_BATCHALL, gameYear);

        if (!report.isBlank()) {
            fameChangeReports.add(report);
        }

        return fameChangeReports;
    }

    /**
     * Applies fame changes when the player executes prisoners of war.
     *
     * <p>For each victim in the specified list, the method identifies their origin faction and increments a fame penalty
     * for that faction, unless the faction is untracked. If multiple prisoners originate from the same faction, their
     * penalties are accumulated.</p>
     *
     * <p>After processing all victims, the method applies the total fame change for each affected faction for the
     * specified game year, and collects any resulting fame change reports.</p>
     *
     * @param victims  the list of {@link Person} prisoners executed by the player
     * @param gameYear the year in which the executions and fame changes occur
     * @return a {@link List} of non-blank fame change report strings for each affected faction
     */
    public List<String> executePrisonersOfWar(final List<Person> victims, final int gameYear) {
        Map<String, Double> affectedFactions = new HashMap<>();

        for (Person victim : victims) {
            Faction originFaction = victim.getOriginFaction();
            if (isUntrackedFaction(originFaction)) {
                continue;
            }

            String factionCode = originFaction.getShortName();
            affectedFactions.merge(factionCode, FAME_DELTA_EXECUTING_PRISONER, Double::sum);
        }

        List<String> fameChangeReports = new ArrayList<>();
        for (Map.Entry<String, Double> entry : affectedFactions.entrySet()) {
            String report = changeFameForFaction(entry.getKey(), entry.getValue(), gameYear);
            if (!report.isBlank()) {
                fameChangeReports.add(report);
            }
        }

        return fameChangeReports;
    }

    /**
     * Writes all faction standings as XML out to the specified {@link PrintWriter}.
     *
     * <p>The output includes each faction code and its current fame value as a separate tag, indented for
     * readability within a parent {@code standings} element.</p>
     *
     * <p>This is primarily used for saving campaign data.</p>
     *
     * @param writer the writer to output XML to
     * @param indent the indentation level for formatting
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void writeFactionStandingsToXML(final PrintWriter writer, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "standings");
        for (String factionCode : factionStandings.keySet()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, factionCode, factionStandings.get(factionCode).toString());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "standings");
    }

    /**
     * Creates a new {@code FactionStandings} instance by parsing standing values from an XML node.
     *
     * <p>This method reads child elements of the provided XML node, looking for a "standings" element.</p>
     *
     * <p>For each faction code found as a subelement, it extracts the faction's fame value from the element's text
     * content. Parsed standing values are collected into a map, and then applied to a new {@link FactionStandings}
     * instance.</p>
     *
     * <p>If any parsing errors occur for individual entries, an error is logged and the process continues.</p>
     *
     * <p>If the entire node cannot be parsed, an error is logged and an (empty) {@link FactionStandings} is still
     * returned.</p>
     *
     * @param parentNode the XML node containing faction standings data, typically as a parent "standings" element
     *
     * @return a new {@link FactionStandings} instance populated with parsed standing values from the XML, or empty if
     *       nothing could be read
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static FactionStandings generateInstanceFromXML(final Node parentNode) {
        NodeList childNodes = parentNode.getChildNodes();

        FactionStandings standings = new FactionStandings();

        Map<String, Double> factionStandings = new HashMap<>();
        try {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                String nodeName = childNode.getNodeName();

                if (nodeName.equalsIgnoreCase("standings")) {
                    NodeList factionEntries = childNode.getChildNodes();

                    for (int factionEntry = 0; factionEntry < factionEntries.getLength(); factionEntry++) {
                        Node node = factionEntries.item(factionEntry);

                        if (node.getNodeType() == Node.ELEMENT_NODE) {
                            try {
                                factionStandings.put(node.getNodeName(),
                                      MathUtility.parseDouble(node.getTextContent(), DEFAULT_FAME));
                            } catch (Exception ex) {
                                LOGGER.error("Could not parse {}: ", node.getNodeName(), ex);
                            }
                        }
                    }
                }
            }

            standings.setFactionStandings(factionStandings);
        } catch (Exception ex) {
            LOGGER.error("Could not parse FactionStandings: ", ex);
        }

        return standings;
    }
}
