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
import static mekhq.gui.dialog.factionStanding.manualMissionDialogs.SimulateMissionDialog.handleFactionRegardUpdates;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.FactionHints;
import mekhq.campaign.universe.Factions;
import mekhq.gui.dialog.factionStanding.manualMissionDialogs.ManualMissionDialog;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Stores and manages the standing values between factions in the Faction Standings system.
 *
 * <p>A {@link FactionStandings} object tracks the current Regard values for all relevant factions using faction codes
 * as keys and numeric Regard as values. Values may be positive (good reputation), negative (bad reputation), or zero
 * (neutral). This class provides functionality to initialize standings according to relationships, adjust and degrade
 * Regard values, serialize data to XML, and reconstruct state from XML.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class FactionStandings {
    private static final MMLogger LOGGER = MMLogger.create(FactionStandings.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionStandings";

    /**
     * This value defines the upper limit of Regard a campaign can achieve with a faction.
     */
    static final double MAXIMUM_REGARD = 60.0;

    /**
     * A constant representing the minimum regard a campaign can have with a faction.
     */
    static final double MINIMUM_REGARD = -60.0;

    /**
     * The base regard value for all factions.
     */
    static final double DEFAULT_REGARD = 0.0;

    /**
     * The amount by which regard degrades over time.
     */
    static final double DEFAULT_REGARD_DEGRADATION = 0.25;

    /**
     * The starting regard for the campaign's faction
     */
    static final double STARTING_REGARD_SAME_FACTION = FactionStandingLevel.STANDING_LEVEL_5.getMaximumRegard() / 2;

    /**
     * The starting regard for factions that are allies of the campaign faction.
     */
    static final double STARTING_REGARD_ALLIED_FACTION = STARTING_REGARD_SAME_FACTION / 2;

    /**
     * The starting regard for factions that are at war with the campaign faction.
     */
    static final double STARTING_REGARD_ENEMY_FACTION_AT_WAR = FactionStandingLevel.STANDING_LEVEL_3.getMinimumRegard() /
                                                                     2;

    /**
     * The starting regard for factions that are rivals of the campaign faction.
     */
    static final double STARTING_REGARD_ENEMY_FACTION_RIVAL = STARTING_REGARD_ENEMY_FACTION_AT_WAR / 2;

    /**
     * The climate regard adjustment for the campaign's faction
     */
    static final double CLIMATE_REGARD_SAME_FACTION = DEFAULT_REGARD_DEGRADATION * 50;

    /**
     * The climate regard adjustment for factions that are allies of the campaign faction.
     */
    static final double CLIMATE_REGARD_ALLIED_FACTION = DEFAULT_REGARD_DEGRADATION * 25;

    /**
     * The climate regard adjustment for factions that are at war with the campaign faction.
     */
    static final double CLIMATE_REGARD_ENEMY_FACTION_AT_WAR = -CLIMATE_REGARD_SAME_FACTION;

    /**
     * The climate regard adjustment for factions that are rivals of the campaign faction.
     */
    static final double CLIMATE_REGARD_ENEMY_FACTION_RIVAL = -CLIMATE_REGARD_ALLIED_FACTION;

    /**
     * Regard increase for successfully completing a contract for the employer.
     */
    static final double REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER = DEFAULT_REGARD_DEGRADATION * 5;

    /**
     * Regard increase for successfully completing a contract for factions allied with the employer.
     */
    static final double REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY = DEFAULT_REGARD_DEGRADATION * 2;

    /**
     * Regard increase for completing a 'partial success' contract for the employer.
     */
    static final double REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER / 3;

    /**
     * Regard increase for completing a 'partial success' contract for factions allied with the employer.
     */
    static final double REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY / 3;

    /**
     * Regard penalty for failing a contract for the employer.
     */
    static final double REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER = -REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;

    /**
     * Regard penalty for completing a 'partial success' contract for factions allied with the employer.
     */
    static final double REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY = -REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY;

    /**
     * Regard penalty for breaching a contract (employer).
     */
    static final double REGARD_DELTA_CONTRACT_BREACH_EMPLOYER = -(REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER * 2);

    /**
     * Regard penalty for breaching a contract (employer's allies).
     */
    static final double REGARD_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY = -(REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY * 2);

    /**
     * Regard decrease when accepting a contract against a non-Clan enemy.
     */
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL = -REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;

    /**
     * Regard decrease when accepting a contract against a Clan enemy.
     */
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY / 2;

    /**
     * Regard decrease when accepting a contract for non-Clan factions allied with the enemy.
     */
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL = -(REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL /
                                                                                 2);

    /**
     * Regard decrease when accepting a contract for Clan factions allied with the enemy.
     */
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN / 2;

    /**
     * Regard penalty for refusing a batchall.
     */
    static final double REGARD_DELTA_REFUSE_BATCHALL = REGARD_DELTA_CONTRACT_BREACH_EMPLOYER * 2;

    /**
     * Regard penalty for refusing a batchall.
     */
    static final double REGARD_DELTA_EXECUTING_PRISONER = -0.1;

    /**
     * A mapping of faction names to their respective standing levels.
     *
     * <p>This variable is used to store and track the Regard score of factions the campaign has interacted with.</p>
     *
     * <p><b>Key:</b></p> A {@link String} representing the shortname of the faction (aka Faction Code).
     * <p><b>Value:</b></p> A {@link Double} representing the campaign's Regard with that faction.
     */
    private Map<String, Double> factionRegard = new HashMap<>();

    /**
     * A mapping of faction names to their respective standing levels.
     *
     * <p>This variable is used to store and track the temporary Regard modifier from factions at war or allied.</p>
     *
     * <p><b>Key:</b></p> A {@link String} representing the shortname of the faction (aka Faction Code).
     * <p><b>Value:</b></p> A {@link Double} representing the campaign's Regard with that faction.
     */
    private Map<String, Double> climateRegard = new HashMap<>();

    /**
     * Constructs an empty standings map. No initial relationships or regard values are set.
     *
     * <p><b>Usage:</b> this does not populate the 'standing' map with any values. That has to be handled
     * separately.</p>
     *
     * <p>If we're starting a new campaign, we should follow up object construction with a call to
     * {@link #initializeStartingRegardValues(Faction, LocalDate)} and
     * {@link #updateClimateRegard(Faction, LocalDate)}</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionStandings() {
    }

    /**
     * @return the maximum regard the campaign can have with a faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getMaximumRegard() {
        return MAXIMUM_REGARD;
    }

    /**
     * @return the minimum regard the campaign can have with a faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getMinimumRegard() {
        return MINIMUM_REGARD;
    }

    /**
     * Initializes faction regard standings at the start of a campaign or when performing a full reset.
     *
     * <p>This method sets up initial Regard values for all factions relative to the given campaign faction on a
     * specified date. Direct allies, direct enemies, and secondary relationships (such as allies of enemies) are
     * each assigned distinct starting regard values, determined by the configuration in {@link FactionStandingLevel}.</p>
     *
     * <p>The process is performed in two passes:</p>
     * <ul>
     *     <li><b>First pass:</b> Identifies and assigns Regard to the campaign faction itself, direct allies, and
     *     direct enemies. Allies receive a positive Regard boost, enemies receive a negative one, and the campaign
     *     faction starts with a high positive Regard.</li>
     *     <li><b>Second pass:</b> Assigns intermediate regard values to factions indirectly related to the campaign
     *     faction (such as allies of enemies), while skipping those already processed in the first pass.</li>
     * </ul>
     *
     * <p>This method is intended primarily for initializing a new campaign or completely resetting the standings.
     * To maintain player progress, it should not be used for incremental changes during an active campaign.</p>
     *
     * @param campaignFaction the main faction from which all relationships are evaluated
     * @param today the current campaign date, used to determine relationships between factions
     * @return a list of formatted report strings describing each regard value that was set during initialization;
     *         one entry per modified faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> initializeStartingRegardValues(final Faction campaignFaction, final LocalDate today) {
        List<String> regardChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        String report;
        for (Faction otherFaction : allFactions) {
            if (!otherFaction.validIn(gameYear)) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();

            if (otherFaction.isAggregate()) {
                continue;
            }

            if (otherFaction.equals(campaignFaction)) {
                report = changeRegardForFaction(otherFactionCode, STARTING_REGARD_SAME_FACTION, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
                continue;
            }

            if (factionHints.isAlliedWith(campaignFaction, otherFaction, today)) {
                report = changeRegardForFaction(otherFactionCode, STARTING_REGARD_ALLIED_FACTION, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                    continue;
                }
            }

            if (factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                report = changeRegardForFaction(otherFactionCode, STARTING_REGARD_ENEMY_FACTION_AT_WAR, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                    continue;
                }
            }

            if (factionHints.isRivalOf(campaignFaction, otherFaction, today)) {
                report = changeRegardForFaction(otherFactionCode, STARTING_REGARD_ENEMY_FACTION_RIVAL, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
            }

            if (campaignFaction.isMercenary()) {
                double mercenaryRelationsModifier = MercenaryRelations.getMercenaryRelationsModifier(otherFaction,
                      today);

                if (mercenaryRelationsModifier != DEFAULT_REGARD) {
                    report = changeRegardForFaction(otherFactionCode, mercenaryRelationsModifier, gameYear);
                    if (!report.isBlank()) {
                        regardChangeReports.add(report);
                    }
                }
            }
        }

        return regardChangeReports;
    }

    /**
     * Determines if the specified faction is considered "untracked."
     *
     * <p>A faction is untracked if it represents an aggregate of independent 'factions', rather than a faction we can
     * track. For example, "PIR" (pirates) is used to abstractly represent all pirates, but individual pirate groups
     * are not tracked. As there is no unified body to gain or loss Regard with, we choose not to track Standing with
     * that faction.</p>
     *
     * <p><b>Note:</b> We're calling out the specific faction codes and not the tags to ensure that we're not
     * accidentally filtering out factions that we might want to track.</p>
     *
     * @param factionCode the {@link Faction} to check
     *
     * @return {@code true} if the faction is untracked; {@code false} otherwise
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public static boolean isUntrackedFaction(final String factionCode) {
        final List<String> untrackedFactionTags = Arrays.asList("MERC",
              "PIR",
              "RON",
              "REB",
              "IND",
              "ABN",
              "UND",
              "NONE",
              "CLAN",
              "DIS");

        return untrackedFactionTags.contains(factionCode);
    }

    /**
     * Replaces the current map of faction standings with the provided map.
     *
     * <p>Existing contents are discarded. After this call, only the entries in the given map remain.</p>
     *
     * @param factionRegard the new map of faction codes to regard values
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFactionRegard(Map<String, Double> factionRegard) {
        this.factionRegard = factionRegard;
    }


    /**
     * Replaces the current map of faction standings with the provided map.
     *
     * <p>Existing contents are discarded. After this call, only the entries in the given map remain.</p>
     *
     * @param climateRegard the new map of faction codes to regard values
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setClimateRegard(Map<String, Double> climateRegard) {
        this.climateRegard = climateRegard;
    }

    /**
     * Retrieves all current faction standings.
     *
     * @return a {@link Map} containing all faction codes mapped to their current regard values.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Map<String, Double> getAllFactionStandings() {
        return factionRegard;
    }

    /**
     * Retrieves all current faction standings based on climate climate.
     *
     * @return a {@link Map} containing all faction codes mapped to their current regard values.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public Map<String, Double> getAllClimateRegard() {
        return climateRegard;
    }

    /**
     * Retrieves the current regard value for the specified faction.
     *
     * @param factionCode a unique code identifying the faction
     * @param includeCurrentClimate whether to include temporary modifiers from the current climate climate
     *
     * @return the regard value for the faction, or 0 if none is present
     *
     * @author Illiani
     * @since 0.50.07
     */
    public double getRegardForFaction(final String factionCode, final boolean includeCurrentClimate) {
        double regard = factionRegard.getOrDefault(factionCode, DEFAULT_REGARD);

        if (includeCurrentClimate) {
            regard += climateRegard.getOrDefault(factionCode, DEFAULT_REGARD);
        }

        return clamp(regard, MINIMUM_REGARD, MAXIMUM_REGARD);
    }

    /**
     * Sets the regard value for the specified faction, directly assigning (or overwriting) the value. If the faction
     * code does not already exist, a new entry is created.
     *
     * <p>The regard value is automatically clamped between {@code MINIMUM_REGARD} and {@code MAXIMUM_REGARD}.</p>
     *
     * <p>If {@code includeReport} is {@code true}, a report string describing the change is returned. Otherwise, an
     * empty string is returned.</p>
     *
     * @param factionCode a unique code identifying the faction
     * @param newRegard the regard (standing) value to assign
     * @param gameYear  the current in-game year for reporting purposes
     * @param includeReport  if {@code true}, no report string is returned; if {@code false}, a report of the change is generated
     * @return a report string describing the change if {@code includeReport} is {@code true}; otherwise, an empty
     * string
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String setRegardForFaction(final String factionCode, final double newRegard, final int gameYear,
          final boolean includeReport) {
        double regardValue = clamp(newRegard, MINIMUM_REGARD, MAXIMUM_REGARD);
        double currentRegard = getRegardForFaction(factionCode, false);

        factionRegard.put(factionCode, regardValue);

        if (includeReport) {
            return getRegardChangedReport(-currentRegard, gameYear, factionCode, regardValue, currentRegard);
        }

        return "";
    }

    /**
     * Adjusts the regard value for a specified faction by a given amount and generates a detailed report of any change.
     *
     * <p>Retrieves the current regard of the specified faction and alters it by {@code delta}. If the faction does not
     * exist in the standings, it is initialized with the specified delta value. The method determines if this
     * adjustment causes the faction to cross a standing milestone, as defined in {@link FactionStandingLevel}. If a
     * milestone transition occurs, the report includes a message highlighting this change. The generated report uses
     * color formatting to indicate the direction of change (increase or decrease) and displays the faction’s full
     * name for the current game year.</p>
     *
     * <p>If {@code delta} is zero, the method leaves regard and milestones unchanged and returns an empty string.</p>
     *
     * @param factionCode unique identifier for the faction whose regard should be adjusted
     * @param delta the amount to increment or decrement the faction's regard (can be positive or negative)
     * @param gameYear the current in-game year, affecting how faction names are displayed in reports
     * @return a formatted {@link String} describing the regard change and any milestone transition, or an empty string
     * if {@code delta} is zero
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String changeRegardForFaction(final String factionCode, final double delta, final int gameYear) {
        if (delta == 0) {
            LOGGER.debug("A change of 0 Regard requested for {}. Shortcutting the method.", factionCode);
            return "";
        }

        double originalRegard = getRegardForFaction(factionCode, false);
        double newRegard = clamp(originalRegard + delta, MINIMUM_REGARD, MAXIMUM_REGARD);

        factionRegard.put(factionCode, newRegard);

        return getRegardChangedReport(delta, gameYear, factionCode, newRegard, originalRegard);
    }

    /**
     * Updates the internal map representing the "climate regard"—an attitude or relationship level—between the
     * specified campaign faction and all other factions for the given date.
     *
     * <p>The method iterates over all factions and assigns a regard value based on alliances, wars, rivalry, and
     * whether the faction is untracked or invalid for the specified year.</p>
     *
     * <p>Existing climateRegard entries are removed.</p>
     *
     * <p>After updating, this method generates and returns an HTML-formatted report summarizing the new climate
     * regard standings for all relevant factions.</p>
     *
     * @param campaignFaction the {@link Faction} representing the campaign's primary faction
     * @param today           the {@link LocalDate} to use for validating factions and determining relationships
     *
     * @return an HTML-formatted {@link String} report of faction climate regard changes
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String updateClimateRegard(final Faction campaignFaction, final LocalDate today) {
        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();
        boolean isPirate = campaignFaction.isPirate();

        // Clear any existing climate regard entries
        climateRegard.clear();

        for (Faction otherFaction : allFactions) {
            if (!otherFaction.validIn(today.getYear())) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();

            if (otherFaction.isAggregate()) {
                continue;
            }

            if (otherFaction.equals(campaignFaction)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_SAME_FACTION);
                continue;
            }

            if ((isPirate && otherFaction.isPirate()) ||
                      factionHints.isAlliedWith(campaignFaction, otherFaction, today)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_ALLIED_FACTION);
                continue;
            }

            if ((isPirate && !otherFaction.isPirate()) ||
                      factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_ENEMY_FACTION_AT_WAR);
                continue;
            }

            if (factionHints.isRivalOf(campaignFaction, otherFaction, today)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_ENEMY_FACTION_RIVAL);
            }

            if (campaignFaction.isMercenary()) {
                double mercenaryRelationsModifier = MercenaryRelations.getMercenaryRelationsModifier(otherFaction,
                      today);

                if (mercenaryRelationsModifier != DEFAULT_REGARD) {
                    climateRegard.put(otherFactionCode, mercenaryRelationsModifier);
                }
            }
        }

        // If we're not handling any climate modifiers, return an empty string
        if (climateRegard.isEmpty()) {
            return "";
        }

        return buildClimateReport(campaignFaction.isPirate(), today).toString();
    }

    /**
     * Builds an HTML-formatted report summarizing the current "climate regard" standings between the campaign faction
     * and all other tracked factions for the specified date.
     *
     * <p>The report includes each faction's name and its corresponding regard value, color-coded to indicate
     * positive or negative standing.</p>
     *
     * <p>If any entries exist, an introductory line is inserted at the beginning.</p>
     *
     * @param campaignIsPirate whether the faction is a pirate faction
     * @param today the {@link LocalDate} used for retrieving year-specific faction names
     *
     * @return a {@link StringBuilder} containing the formatted climate regard report
     *
     * @author Illiani
     * @since 0.50.07
     */
    private StringBuilder buildClimateReport(boolean campaignIsPirate, LocalDate today) {
        StringBuilder report = new StringBuilder();
        String factionName;
        double regard;
        String reportFormat = "<br>- %s: <span color='%s'><b>%s</b>" + CLOSING_SPAN_TAG;

        List<String> sortedFactionCodes = new ArrayList<>(climateRegard.keySet());
        Collections.sort(sortedFactionCodes);
        for (String factionCode : sortedFactionCodes) {
            regard = climateRegard.get(factionCode);

            // We don't report negative Regard for pirates because they're always negative.
            if (campaignIsPirate && regard < 0) {
                continue;
            }

            Faction faction = Factions.getInstance().getFaction(factionCode);
            if (faction == null) {
                LOGGER.warn("Faction {} is missing from the Factions collection. Skipping.",
                      climateRegard.get(factionCode));
                continue;
            }

            factionName = faction.getFullName(today.getYear());
            String color = regard >= 0 ? getPositiveColor() : getNegativeColor();

            report.append(String.format(reportFormat, factionName, color, regard));
        }

        if (!report.isEmpty()) {
            String reportKey = campaignIsPirate ?
                                     "factionStandings.change.report.climate.pirate" :
                                     "factionStandings.change.report.climate";

            report.insert(0,
                  getFormattedTextAt(RESOURCE_BUNDLE,
                        reportKey,
                        spanOpeningWithCustomColor(getWarningColor()),
                        CLOSING_SPAN_TAG,
                        spanOpeningWithCustomColor(getNegativeColor()),
                        CLIMATE_REGARD_ENEMY_FACTION_AT_WAR,
                        CLOSING_SPAN_TAG));
        }
        return report;
    }

    /**
     * Builds a formatted report string describing changes to a faction's regard and any milestone transitions.
     *
     * <p>This method generates detailed feedback about a Regard value adjustment for a faction, including whether a
     * milestone ({@link FactionStandingLevel}) has changed as a result. The report text uses color formatting to
     * visually indicate the change's nature (positive, negative, or neutral); includes the faction's name for the
     * current game year; the direction and magnitude of the regard change; and a message about the milestone
     * status—whether a new milestone was reached or the faction remains within the same milestone.</p>
     *
     * <p>If the relevant faction is not present, a default faction is used for milestone reporting.</p>
     *
     * <p>An additional prefix may be applied to the faction name.</p>
     *
     * @param delta        the amount of Regard gained or lost
     * @param gameYear     the current in-game year, used to render the appropriate faction name
     * @param factionCode  unique identifier for the faction whose Regard should be adjusted
     * @param newRegard      the Regard value after the delta is applied
     * @param originalRegard the Regard value before the delta is applied
     *
     * @return a formatted {@link String} describing the regard change, direction, and any milestone transition
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String getRegardChangedReport(final double delta, final int gameYear, final String factionCode,
          final double newRegard, final double originalRegard) {
        Faction relevantFaction = Factions.getInstance().getFaction(factionCode);
        FactionStandingLevel originalMilestone = FactionStandingUtilities.calculateFactionStandingLevel(originalRegard);
        FactionStandingLevel newMilestone = FactionStandingUtilities.calculateFactionStandingLevel(newRegard);

        String reportingColor;
        String milestoneChangeReport;
        if (originalMilestone != newMilestone) {
            if (relevantFaction == null) {
                relevantFaction = Factions.getInstance().getDefaultFaction();
            }

            if (newMilestone.getStandingLevel() > originalMilestone.getStandingLevel()) {
                reportingColor = getPositiveColor();
            } else {
                reportingColor = getNegativeColor();
            }

            milestoneChangeReport = getFormattedTextAt(RESOURCE_BUNDLE,
                  "factionStandings.change.report.milestone.new",
                  spanOpeningWithCustomColor(reportingColor),
                  newMilestone.getLabel(relevantFaction),
                  CLOSING_SPAN_TAG);
        } else {
            reportingColor = getWarningColor();

            milestoneChangeReport = getFormattedTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.milestone.same",
                  spanOpeningWithCustomColor(reportingColor),
                  newMilestone.getLabel(relevantFaction),
                  CLOSING_SPAN_TAG);
        }

        // Build final report
        String deltaDirection;
        if (newRegard > originalRegard) {
            reportingColor = getPositiveColor();
            deltaDirection = getTextAt(RESOURCE_BUNDLE, "factionStandings.change.increased");
        } else {
            reportingColor = getNegativeColor();
            deltaDirection = getTextAt(RESOURCE_BUNDLE, "factionStandings.change.decreased");
        }

        String factionName = relevantFaction.getFullName(gameYear);
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
    public void resetAllFactionStandings() {
        factionRegard.clear();
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
        factionRegard.remove(factionCode);
    }

    /**
     * Gradually reduces all non-zero faction regard values toward zero by a fixed increment, simulating regard decay over time.
     *
     * <p>For each faction with a non-zero regard value, the method decrements positive values and increments negative
     * values by a fixed amount. This step-wise adjustment continues regard's progression toward zero, with Regard being
     * set to exactly zero if it otherwise crosses zero, thus preventing overshooting. For each adjustment, a report
     * string is generated if an actual change occurs.</p>
     *
     * <p>This method is typically called annually to model the natural decline of relationships or reputation over
     * time.</p>
     *
     * @param gameYear the current in-game year, used for proper display of faction names in reports
     * @return a list of formatted report strings describing each regard change made during this process; one entry per
     * modified faction, or an empty list if no changes occurred
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processRegardDegradation(final int gameYear) {
        List<String> regardChangeReports = new ArrayList<>();
        LOGGER.info("Processing regard decay for {} factions.", factionRegard.size());
        for (String factionCode : new HashSet<>(factionRegard.keySet())) {
            Faction faction = Factions.getInstance().getFaction(factionCode);
            if (faction == null) {
                LOGGER.info("Faction {} is missing from the Factions collection. Skipping.", factionCode);
                continue;
            }

            if (isNotValidForTracking(faction, gameYear)) {
                continue;
            }

            double currentRegard = factionRegard.get(factionCode);

            if (currentRegard != DEFAULT_REGARD) {
                double delta = currentRegard > DEFAULT_REGARD ?
                                     -DEFAULT_REGARD_DEGRADATION :
                                     DEFAULT_REGARD_DEGRADATION;
                String report = changeRegardForFaction(factionCode, delta, gameYear);
                double newRegard = getRegardForFaction(factionCode, false);

                if ((currentRegard > DEFAULT_REGARD && newRegard < DEFAULT_REGARD) ||
                          (currentRegard < DEFAULT_REGARD && newRegard > DEFAULT_REGARD)) {
                    setRegardForFaction(factionCode, DEFAULT_REGARD, gameYear, false);
                }

                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
            }
        }

        return regardChangeReports;
    }

    /**
     * Processes the acceptance of a contract against a specified enemy faction and applies regard changes to all relevant
     * factions.
     *
     * <p>This method iterates through all factions in the game, adjusting their regard values based on relationships to
     * the specified enemy faction and the contract's context. Regard deltas are applied for the enemy faction itself, as
     * well as any factions allied with the enemy. The changes vary depending on whether the factions are clans or
     * non-clans, and different regard penalties are applied accordingly.</p>
     *
     * <p>For each application of a regard delta, a report string is generated and included in the result list if it is
     * not blank.</p>
     *
     * @param enemyFaction The {@link Faction} representing the enemy against whom the contract is accepted.
     * @param today        The {@link LocalDate} representing the game date on which the contract is accepted.
     *
     * @return A {@link List} of {@link String} objects summarizing any regard changes that occurred as a result of
     *       accepting the contract.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processContractAccept(@Nullable final Faction enemyFaction, final LocalDate today) {
        // If we're missing the relevant faction, alert the player and abort
        if (enemyFaction == null) {
            String report = getMissingFactionReport();

            return List.of(report);
        }

        List<String> regardChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        String report;
        double regardDelta;
        for (Faction otherFaction : allFactions) {
            if (!otherFaction.validIn(gameYear)) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();

            if (otherFaction.isAggregate()) {
                continue;
            }

            if (otherFaction.equals(enemyFaction)) {
                if (otherFaction.isClan()) {
                    regardDelta = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN;
                } else {

                    regardDelta = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL;
                }

                report = changeRegardForFaction(otherFactionCode, regardDelta, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
                continue;
            }

            if (factionHints.isAlliedWith(enemyFaction, otherFaction, today)) {
                if (otherFaction.isClan()) {
                    regardDelta = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN;
                } else {

                    regardDelta = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL;
                }

                report = changeRegardForFaction(otherFactionCode, regardDelta, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
            }
        }

        return regardChangeReports;
    }

    /**
     * Processes the outcome of a contract upon its completion and updates regard standings accordingly.
     *
     * <p>Depending on the mission status (success, partial, failure, or breach), this method determines the appropriate
     * regard delta for the employer faction and its allies.</p>
     *
     * <p>If the employer faction is missing, a report is generated and returned accordingly. This report informs the
     * player that they need to manually apply the Standing change via the Standing Report GUI.</p>
     *
     * <p>Regard changes are applied to the employer and all allied factions, and corresponding report strings are
     * returned for each regard change applied.</p>
     *
     * @param employerFaction The {@link Faction} that employed the contract, or {@code null} if unavailable.
     * @param today           The {@link LocalDate} representing the date of contract completion.
     * @param missionStatus   The {@link MissionStatus} of the contract upon completion.
     * @return A {@link List} of strings summarizing any regard changes or messages relating to missing factions.
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processContractCompletion(@Nullable final Faction employerFaction, final LocalDate today,
                                                  final MissionStatus missionStatus) {
        // If the mission is still active, there is nothing to process, so abort
        if (missionStatus == MissionStatus.ACTIVE) {
            return new ArrayList<>();
        }

        double regardDeltaEmployer = 0.0;
        double regardDeltaEmployerAlly = 0.0;
        switch (missionStatus) {
            case SUCCESS -> {
                regardDeltaEmployer = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;
                regardDeltaEmployerAlly = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY;
            }
            case PARTIAL -> {
                regardDeltaEmployer = REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER;
                regardDeltaEmployerAlly = REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY;
            }
            case FAILED -> {
                regardDeltaEmployer = REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER;
                regardDeltaEmployerAlly = REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY;
            }
            case BREACH -> {
                regardDeltaEmployer = REGARD_DELTA_CONTRACT_BREACH_EMPLOYER;
                regardDeltaEmployerAlly = REGARD_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY;
            }
        }

        // If there is no change to make, we exit early so as not to process faction data needlessly
        if ((regardDeltaEmployer + regardDeltaEmployerAlly) == 0.0) {
            return new ArrayList<>();
        }

        List<String> regardChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints();

        String report;
        for (Faction otherFaction : allFactions) {
            String otherFactionCode = otherFaction.getShortName();

            if (isNotValidForTracking(otherFaction, gameYear)) {
                continue;
            }

            if (otherFaction.equals(employerFaction)) {
                report = changeRegardForFaction(otherFactionCode, regardDeltaEmployer, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
                continue;
            }

            if (factionHints.isAlliedWith(employerFaction, otherFaction, today)) {
                report = changeRegardForFaction(otherFactionCode, regardDeltaEmployerAlly, gameYear);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
            }
        }

        return regardChangeReports;
    }

    /**
     * Determines whether a given faction should be excluded from tracking for regard (including climate regard, based
     * on its validity in the specified game year or if it is considered "untracked."
     *
     * @param otherFaction the {@link Faction} to evaluate
     * @param gameYear the year for which validity should be checked
     * @return {@code true} if the faction is either invalid in the specified year or is untracked; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    private boolean isNotValidForTracking(Faction otherFaction, int gameYear) {
        if (!otherFaction.validIn(gameYear)) {
            return true;
        }

        return otherFaction.isAggregate();
    }

    /**
     * Generates a report message indicating that the relevant faction (employer or enemy) is missing.
     *
     * <p>The message varies depending on whether the context is contract completion or acceptance.</p>
     *
     * @return A {@link String} representing the formatted report message for the missing faction scenario.
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getMissingFactionReport() {
        return getFormattedTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.missingFaction",
              spanOpeningWithCustomColor(getWarningColor()),
              CLOSING_SPAN_TAG);
    }

    /**
     * Processes the penalty for refusing a batchall against a specific Clan faction.
     *
     * <p>This method applies a regard penalty to the given clan faction code for the specified year and generates a regard
     * change report if applicable.</p>
     *
     * <p>This method is included as a shortcut to allow developers to call Batchall refusal changes without needing to
     * worry about setting up bespoke methods any time this could occur.</p>
     *
     * @param clanFactionCode The code representing the clan faction being penalized.
     * @param gameYear        The year in which the batchall was refused.
     * @return A {@link List} of regard change report strings relating to the refusal.
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processRefusedBatchall(final String clanFactionCode, final int gameYear) {
        List<String> regardChangeReports = new ArrayList<>();

        String report = changeRegardForFaction(clanFactionCode, REGARD_DELTA_REFUSE_BATCHALL, gameYear);

        if (!report.isBlank()) {
            regardChangeReports.add(report);
        }

        return regardChangeReports;
    }

    /**
     * Applies regard changes when the player executes prisoners of war.
     *
     * <p>For each victim in the specified list, the method identifies their origin faction and increments a regard penalty
     * for that faction, unless the faction is untracked. If multiple prisoners originate from the same faction, their
     * penalties are accumulated.</p>
     *
     * <p>After processing all victims, the method applies the total regard change for each affected faction for the
     * specified game year and collects any resulting regard change reports.</p>
     *
     * @param victims  the list of {@link Person} prisoners executed by the player
     * @param gameYear the year in which the executions and regard changes occur
     * @return a {@link List} of non-blank regard change report strings for each affected faction
     */
    public List<String> executePrisonersOfWar(final List<Person> victims, final int gameYear) {
        Map<String, Double> affectedFactions = new HashMap<>();

        for (Person victim : victims) {
            Faction originFaction = victim.getOriginFaction();
            String factionCode = originFaction.getShortName();

            if (originFaction.isAggregate()) {
                continue;
            }

            affectedFactions.merge(factionCode, REGARD_DELTA_EXECUTING_PRISONER, Double::sum);
        }

        List<String> regardChangeReports = new ArrayList<>();
        for (Map.Entry<String, Double> entry : affectedFactions.entrySet()) {
            String report = changeRegardForFaction(entry.getKey(), entry.getValue(), gameYear);
            if (!report.isBlank()) {
                regardChangeReports.add(report);
            }
        }

        return regardChangeReports;
    }

    /**
     * Writes all faction standings as XML out to the specified {@link PrintWriter}.
     *
     * <p>The output includes each faction code and its current regard value as a separate tag, indented for
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
        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionRegard");
        for (String factionCode : factionRegard.keySet()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, factionCode, factionRegard.get(factionCode).toString());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionRegard");

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "climateRegard");
        for (String factionCode : climateRegard.keySet()) {
            MHQXMLUtility.writeSimpleXMLTag(writer, indent, factionCode, climateRegard.get(factionCode).toString());
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "climateRegard");
    }

    /**
     * Creates a new {@code FactionStandings} instance by parsing standing values from an XML node.
     *
     * <p>This method reads child elements of the provided XML node, looking for a "standings" element.</p>
     *
     * <p>For each faction code found as a subelement, it extracts the faction's regard value from the element's text
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
        try {
            for (int i = 0; i < childNodes.getLength(); i++) {
                Node childNode = childNodes.item(i);
                String nodeName = childNode.getNodeName();

                if (nodeName.equalsIgnoreCase("factionRegard")) {
                    standings.setFactionRegard(processRegardNode(childNode, nodeName));
                } else if (nodeName.equalsIgnoreCase("climateRegard")) {
                    standings.setClimateRegard(processRegardNode(childNode, nodeName));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse FactionStandings: ", ex);
        }

        return standings;
    }

    /**
     * Parses a node containing faction regard information and returns a mapping of faction codes to their
     * corresponding regard values as doubles.
     *
     * <p>Each child element node is processed for its name and text value. If the value cannot be parsed as a
     * double, a default is used and the error is logged with the provided label.</p>
     *
     * @param childNode the XML {@link Node} containing child entries representing faction regards
     * @param logLabel a label to help identify log messages during parsing errors
     * @return a {@link Map} of faction codes to parsed regard values
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static Map<String, Double> processRegardNode(Node childNode, String logLabel) {
        NodeList factionEntries = childNode.getChildNodes();
        Map<String, Double> regard = new HashMap<>();
        for (int i = 0; i < factionEntries.getLength(); i++) {
            Node node = factionEntries.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                try {
                    regard.put(node.getNodeName(),
                          MathUtility.parseDouble(node.getTextContent(), FactionStandings.DEFAULT_REGARD));
                } catch (Exception ex) {
                    LOGGER.error("Could not parse {} {}: ", logLabel, node.getNodeName(), ex);
                }
            }
        }

        return regard;
    }

    public List<String> updateCampaignForPastMissions(List<Mission> missions, ImageIcon campaignIcon,
          Faction campaignFaction, LocalDate today) {
        List<String> reports = new ArrayList<>();

        resetAllFactionStandings();

        initializeStartingRegardValues(campaignFaction, today);

        sortMissionsBasedOnStartDateAndClass(missions);

        Map<Integer, List<Mission>> missionsByYear = new HashMap<>();
        int currentYear = today.getYear();

        for (Mission mission : missions) {
            int missionYear = currentYear;
            if (mission instanceof Contract contract) {
                missionYear = contract.getStartDate().getYear();
            }

            missionsByYear.computeIfAbsent(missionYear, y -> new ArrayList<>()).add(mission);
        }

        List<Integer> sortedYears = new ArrayList<>(missionsByYear.keySet());
        Collections.sort(sortedYears);

        for (int year : sortedYears) {
            List<Mission> missionsForYear = missionsByYear.get(year);
            for (Mission mission : missionsForYear) {
                MissionStatus missionStatus = mission.getStatus();

                if (mission instanceof AtBContract atbContract) {
                    reports.addAll(processContractAccept(atbContract.getEnemy(), today));

                    if (missionStatus != MissionStatus.ACTIVE) {
                        reports.addAll(processContractCompletion(atbContract.getEmployerFaction(),
                              today,
                              missionStatus));
                    }
                } else {
                    // Non-AtB missions have their Standings updated when the contract concludes
                    if (missionStatus != MissionStatus.ACTIVE) {
                        ManualMissionDialog dialog = new ManualMissionDialog(null,
                              campaignIcon,
                              campaignFaction,
                              today,
                              missionStatus,
                              mission.getName());

                        Faction employerChoice = dialog.getEmployerChoice();
                        Faction enemyChoice = dialog.getEnemyChoice();
                        MissionStatus statusChoice = dialog.getStatusChoice();

                        reports.addAll(handleFactionRegardUpdates(employerChoice,
                              enemyChoice,
                              statusChoice,
                              today,
                              this));
                    }
                }
            }

            // At the end of each processed year, simulate degradation (unless we're on the current year)
            if (year != currentYear) {
                reports.addAll(processRegardDegradation(year));
            }
        }

        return reports;
    }

    private static void sortMissionsBasedOnStartDateAndClass(List<Mission> missions) {
        missions.sort((mission1, mission2) -> {
            boolean m1IsContract = mission1 instanceof Contract;
            boolean m2IsContract = mission2 instanceof Contract;

            if (m1IsContract && m2IsContract) {
                return ((Contract) mission1).getStartDate().compareTo(((Contract) mission2).getStartDate());
            } else if (m1IsContract) {
                return -1; // mission1 comes before mission2
            } else if (m2IsContract) {
                return 1; // mission1 comes after mission2
            } else {
                return 0; // both are non-Contract, maintain relative order
            }
        });
    }
}
