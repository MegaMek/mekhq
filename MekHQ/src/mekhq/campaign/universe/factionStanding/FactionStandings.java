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

import static java.lang.Math.max;
import static megamek.codeUtilities.MathUtility.clamp;
import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static mekhq.campaign.universe.Faction.PIRATE_FACTION_CODE;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_0;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_6;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_8;
import static mekhq.campaign.universe.factionStanding.FactionStandingUtilities.PIRACY_SUCCESS_INDEX_FACTION_CODE;
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
import java.util.Objects;
import javax.swing.ImageIcon;

import megamek.codeUtilities.MathUtility;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.MHQConstants;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.factionHints.FactionHints;
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
     * This value defines the upper limit of Regard a campaign can achieve with the campaign's faction.
     */
    static final double MAXIMUM_SAME_FACTION_REGARD = STANDING_LEVEL_8.getMaximumRegard();

    /**
     * The maximum regard value a campaign can have with a faction other than the campaign's faction
     */
    static final double MAXIMUM_OTHER_FACTION_REGARD = STANDING_LEVEL_6.getMaximumRegard();

    /**
     * A constant representing the minimum regard a campaign can have with any faction.
     */
    static final double MINIMUM_REGARD = STANDING_LEVEL_0.getMinimumRegard();

    /**
     * The base regard value for all factions.
     */
    static final double DEFAULT_REGARD = 0.0;

    /**
     * The amount by which regard degrades over time.
     */
    static final double DEFAULT_REGARD_DEGRADATION = 0.375;

    /**
     * The starting regard for the campaign's faction
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double STARTING_REGARD_SAME_FACTION = FactionStandingLevel.STANDING_LEVEL_5.getMaximumRegard() / 2;

    /**
     * The starting regard for factions that are allies of the campaign faction.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double STARTING_REGARD_ALLIED_FACTION = STARTING_REGARD_SAME_FACTION / 2;

    /**
     * The starting regard for factions that are at war with the campaign faction.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double STARTING_REGARD_ENEMY_FACTION_AT_WAR = FactionStandingLevel.STANDING_LEVEL_3.getMinimumRegard() /
                                                                     2;
    /**
     * The starting regard for factions that are rivals of the campaign faction.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
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
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY = DEFAULT_REGARD_DEGRADATION * 2;

    /**
     * Regard increase for completing a 'partial success' contract for the employer.
     */
    static final double REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER / 3;

    /**
     * Regard increase for completing a 'partial success' contract for factions allied with the employer.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER_ALLY = REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY / 3;

    /**
     * Regard penalty for failing a contract for the employer.
     */
    static final double REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER = -REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;

    /**
     * Regard penalty for completing a 'partial success' contract for factions allied with the employer.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER_ALLY = -REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY;

    /**
     * Regard penalty for breaching a contract (employer).
     */
    static final double REGARD_DELTA_CONTRACT_BREACH_EMPLOYER = -(REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER * 3);

    /**
     * Regard penalty for breaching a contract (employer's allies).
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double REGARD_DELTA_CONTRACT_BREACH_EMPLOYER_ALLY = -(REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER_ALLY * 2);

    /**
     * Regard decrease when accepting a contract against a non-Clan enemy.
     */
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL = -REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;

    /**
     * Regard decrease when accepting a contract against a Clan enemy.
     */
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN = -(REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER / 2);

    /**
     * Regard decrease when accepting a contract for non-Clan factions allied with the enemy.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_NORMAL = -(REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL /
                                                                                 2);
    /**
     * Regard decrease when accepting a contract for Clan factions allied with the enemy.
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    static final double REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_ALLY_CLAN = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN / 2;

    /**
     * Regard penalty for refusing a batchall.
     */
    static final double REGARD_DELTA_REFUSE_BATCHALL = REGARD_DELTA_CONTRACT_BREACH_EMPLOYER * 2;

    /**
     * The multiplier applied to climate regard for pirate campaigns.
     */
    static final int PIRATE_CLIMATE_REGARD_ADJUSTMENT_NORMAL = 10;
    /**
     * The multiplier applied to the climate regard for pirate campaigns from Clan factions
     */
    static final int PIRATE_CLIMATE_REGARD_ADJUSTMENT_CLAN = 5;

    /**
     * Regard penalty for refusing a batchall.
     */
    static final double REGARD_DELTA_EXECUTING_PRISONER = -0.1;

    /**
     * How much we should divide contract duration by when determining Duration Multiplier
     */
    static final int CONTRACT_DURATION_LENGTH_DIVISOR = 6;

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
     * Holds information relating to faction judgment activities.
     *
     * <p>This variable is used to store and track any actions a faction may have taken for or against the
     * campaign.</p>
     */
    private FactionJudgment factionJudgment = new FactionJudgment();

    /**
     * Constructs an empty standings map. No initial relationships or regard values are set.
     *
     * <p><b>Usage:</b> this does not populate the 'standing' map with any values. That has to be handled
     * separately.</p>
     *
     * <p>If we're starting a new campaign, we should follow up object construction with a call to
     * {@link #updateClimateRegard(Faction, LocalDate, double, boolean)}</p>
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
    public static double getMaximumSameFactionRegard() {
        return MAXIMUM_SAME_FACTION_REGARD;
    }

    /**
     * @return the maximum regard the campaign can have with a faction other than the campaign's faction.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static double getMaximumOtherFactionRegard() {
        return MAXIMUM_OTHER_FACTION_REGARD;
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
     * specified date. Direct allies, direct enemies, and secondary relationships (such as allies of enemies) are each
     * assigned distinct starting regard values, determined by the configuration in {@link FactionStandingLevel}.</p>
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
     * @param today           the current campaign date, used to determine relationships between factions
     *
     * @return a list of formatted report strings describing each regard value that was set during initialization; one
     *       entry per modified faction
     *
     * @author Illiani
     * @since 0.50.07
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public List<String> initializeStartingRegardValues(final Faction campaignFaction, final LocalDate today) {
        List<String> regardChangeReports = new ArrayList<>();

        int gameYear = today.getYear();

        Collection<Faction> allFactions = Factions.getInstance().getFactions();
        FactionHints factionHints = FactionHints.defaultFactionHints(false);

        boolean isMercenary = campaignFaction.isMercenaryOrganization();
        boolean isPirate = campaignFaction.isPirate();

        String report;
        for (Faction otherFaction : allFactions) {
            if (!otherFaction.validIn(gameYear)) {
                continue;
            }

            String campaignFactionCode = campaignFaction.getShortName();
            String otherFactionCode = otherFaction.getShortName();

            if (otherFaction.isAggregate()) {
                continue;
            }

            if (otherFaction.equals(campaignFaction)) {
                report = changeRegardForFaction(campaignFactionCode, otherFactionCode, STARTING_REGARD_SAME_FACTION,
                      gameYear, 1.0);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
                continue;
            }

            if ((isPirate && otherFaction.isPirate())
                      || factionHints.isAlliedWith(campaignFaction, otherFaction, today)) {
                report = changeRegardForFaction(campaignFactionCode, otherFactionCode, STARTING_REGARD_ALLIED_FACTION,
                      gameYear, 1.0);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                    continue;
                }
            }

            if ((isPirate && !otherFaction.isPirate())
                      || factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                report = changeRegardForFaction(campaignFactionCode, otherFactionCode,
                      STARTING_REGARD_ENEMY_FACTION_AT_WAR, gameYear, 1.0);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                    continue;
                }
            }

            if (factionHints.isRivalOf(campaignFaction, otherFaction, today)) {
                report = changeRegardForFaction(campaignFactionCode, otherFactionCode,
                      STARTING_REGARD_ENEMY_FACTION_RIVAL, gameYear, 1.0);
                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                    continue;
                }
            }

            if (isMercenary) {
                double mercenaryRelationsModifier = MercenaryRelations.getMercenaryRelationsModifier(otherFaction,
                      today);

                if (mercenaryRelationsModifier != DEFAULT_REGARD) {
                    report = changeRegardForFaction(campaignFactionCode, otherFactionCode, mercenaryRelationsModifier,
                          gameYear, 1.0);
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
     * track. For example, "PIR" (pirates) is used to abstractly represent all pirates, but individual pirate groups are
     * not tracked. As there is no unified body to gain or loss Regard with, we choose not to track Standing with that
     * faction.</p>
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
     * Replaces the current {@link FactionJudgment} with the provided object.
     *
     * <p>Existing contents are discarded. After this call, only the entries in the given object remain.</p>
     *
     * @param factionJudgment the new {@link FactionJudgment} object
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void setFactionJudgment(FactionJudgment factionJudgment) {
        this.factionJudgment = factionJudgment;
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
     * Retrieves all current faction standings based on climate.
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
     * Returns the {@link FactionJudgment} instance associated with this object.
     *
     * <p>The {@link FactionJudgment} provides information and operations related to the judgement actions (censures)
     * imposed due to faction standing or rule violations.</p>
     *
     * @return the {@link FactionJudgment} for this instance
     *
     * @author Illiani
     * @since 0.50.07
     */
    public FactionJudgment getFactionJudgments() {
        return factionJudgment;
    }

    /**
     * Retrieves the current regard value for the specified faction.
     *
     * @param factionCode           a unique code identifying the faction
     * @param includeCurrentClimate whether to include temporary modifiers from the current climate
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

        return clamp(regard, MINIMUM_REGARD, MAXIMUM_SAME_FACTION_REGARD);
    }

    /**
     * Use {@link #setRegardForFaction(String, String, double, int, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String setRegardForFaction(final String factionCode, final double newRegard, final int gameYear,
          final boolean includeReport) {
        return setRegardForFaction(null, factionCode, newRegard, gameYear, includeReport);
    }

    /**
     * Sets the regard value for the specified faction, directly assigning or overwriting the value. If the faction code
     * does not already exist, a new entry is created.
     *
     * <p>The regard value is automatically clamped between {@code MINIMUM_REGARD} and the appropriate maximum:
     * {@code MAXIMUM_SAME_FACTION_REGARD} if setting regard for the campaign's own code or if
     * {@code campaignFactionCode} is {@code null}, or {@code MAXIMUM_OTHER_FACTION_REGARD} for other factions.</p>
     *
     * <p>If {@code includeReport} is {@code true}, a report string describing the change is generated and returned.
     * If {@code includeReport} is {@code false}, an empty string is returned.</p>
     *
     * @param campaignFactionCode the unique code identifying the campaign's main faction.
     * @param factionCode         a unique code identifying the faction whose regard value will be set
     * @param newRegard           the regard (standing) value to assign
     * @param gameYear            the current in-game year, for report generation purposes
     * @param includeReport       if {@code true}, a report string describing the change is generated and returned; if
     *                            {@code false}, an empty string is returned
     *
     * @return a report string describing the change if {@code includeReport} is {@code true}; otherwise, an empty
     *       string
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String setRegardForFaction(@Nullable String campaignFactionCode, String factionCode,
          final double newRegard, final int gameYear, final boolean includeReport) {
        double maximumRegard = Objects.equals(campaignFactionCode, factionCode) || campaignFactionCode == null
                                     ? MAXIMUM_SAME_FACTION_REGARD
                                     : MAXIMUM_OTHER_FACTION_REGARD;

        factionCode = convertSpecialFaction(factionCode, gameYear);

        double regardValue = clamp(newRegard, MINIMUM_REGARD, maximumRegard);
        double currentRegard = getRegardForFaction(factionCode, false);

        factionRegard.put(factionCode, regardValue);

        double change = regardValue - currentRegard;

        if (includeReport) {
            return getRegardChangedReport(change, gameYear, factionCode, regardValue, currentRegard);
        }

        return "";
    }

    /**
     * Converts certain special faction codes into their game-context equivalents for a given year.
     *
     * <p>If the provided faction code indicates "Pirates," it is converted to the piracy success index code. If the
     * code indicates "Mercenaries," it uses the {@link Faction#getActiveMercenaryOrganization(int)} method to determine
     * the active mercenary group for the specified year and uses its short name. All other faction codes are returned
     * unchanged.</p>
     *
     * @param factionCode the faction code to convert, such as "PIR" or "MERC"
     * @param gameYear    the year used for resolving year-dependent special factions
     *
     * @return the resolved faction code for the given context and year
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String convertSpecialFaction(String factionCode, int gameYear) {
        if (factionCode.equals(PIRATE_FACTION_CODE)) {
            factionCode = PIRACY_SUCCESS_INDEX_FACTION_CODE;
        } else if (factionCode.equals(MERCENARY_FACTION_CODE)) {
            Faction mercenaryOrganization = Faction.getActiveMercenaryOrganization(gameYear);
            factionCode = mercenaryOrganization.getShortName();
        }
        return factionCode;
    }

    /**
     * Use {@link #changeRegardForFaction(String, String, double, int, double)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String changeRegardForFaction(@Nullable String campaignFactionCode, final String factionCode,
          final double delta, final int gameYear) {
        return changeRegardForFaction(campaignFactionCode, factionCode, delta, gameYear, 1.0);
    }

    /**
     * Adjusts the regard value for a specified faction by a given amount and generates a detailed report of any
     * change.
     *
     * <p>Retrieves the current regard of the specified faction and alters it by {@code delta}. If the faction does not
     * exist in the standings, it is initialized with the specified delta value. The method determines if this
     * adjustment causes the faction to cross a standing milestone, as defined in {@link FactionStandingLevel}. If a
     * milestone transition occurs, the report includes a message highlighting this change. The generated report uses
     * color formatting to indicate the direction of change (increase or decrease) and displays the faction’s full name
     * for the current game year.</p>
     *
     * <p>If {@code delta} is zero, the method leaves regard and milestones unchanged and returns an empty string.</p>
     *
     * @param campaignFactionCode the unique code identifying the campaign's faction.
     * @param factionCode         unique identifier for the faction whose regard should be adjusted
     * @param delta               the amount to increment or decrement the faction's regard (can be positive or
     *                            negative)
     * @param gameYear            the current in-game year, affecting how faction names are displayed in reports
     * @param regardMultiplier    the multiplier set in campaign options.
     *
     * @return a formatted {@link String} describing the regard change and any milestone transition, or an empty string
     *       if {@code delta} is zero
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String changeRegardForFaction(@Nullable String campaignFactionCode, String factionCode, final double delta,
          final int gameYear, final double regardMultiplier) {
        if (delta == 0) {
            LOGGER.debug("A change of 0 Regard requested for {}. Shortcutting the method.", factionCode);
            return "";
        }

        factionCode = convertSpecialFaction(factionCode, gameYear);

        double adjustedDelta = delta * regardMultiplier;

        double originalRegard = getRegardForFaction(factionCode, false);

        double maximumRegard = Objects.equals(campaignFactionCode, factionCode) || campaignFactionCode == null
                                     ? MAXIMUM_SAME_FACTION_REGARD
                                     : MAXIMUM_OTHER_FACTION_REGARD;
        double newRegard = clamp(originalRegard + adjustedDelta, MINIMUM_REGARD, maximumRegard);

        factionRegard.put(factionCode, newRegard);

        return getRegardChangedReport(adjustedDelta, gameYear, factionCode, newRegard, originalRegard);
    }

    /**
     * Checks if the specified faction should receive a new or escalated censure based on its latest standing, and
     * applies the appropriate censure level if necessary.
     * <p>
     * This method computes the current regard value for the given faction and determines the corresponding standing
     * level. If the calculated standing level is at or below the threshold for censure, the faction's censure level
     * will be increased for the provided date. The updated censure level is then returned; if no change is needed,
     * {@code null} is returned.
     * </p>
     *
     * @param faction           the {@link Faction} object to check against
     * @param today             the date to use when recording a possible censure escalation
     * @param activeMissions    a list of the campaign's current active missions
     * @param campaignInTransit {@code true} if the campaign is currently in transit
     *
     * @return the new {@link FactionCensureLevel} if a censure change occurred, or {@code null} if there was no change
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable FactionCensureLevel checkForCensure(Faction faction, LocalDate today,
          List<Mission> activeMissions, boolean campaignInTransit) {
        if (faction.isAggregate()) {
            return null;
        }

        String factionCode = faction.getShortName();
        factionCode = convertSpecialFaction(factionCode, today.getYear());

        double regard = getRegardForFaction(factionCode, true);

        if (regard < FactionJudgment.THRESHOLD_FOR_CENSURE) {
            // This will return null if no change has taken place
            return factionJudgment.increaseCensureForFaction(faction, today, activeMissions, campaignInTransit);
        }

        return null;
    }

    /**
     * Processes all tracked faction censures to determine if any have expired as of the provided date, and
     * automatically degrades (reduces) the censure level for any faction whose censure has expired.
     * <p>
     * Iterates through all current censure entries, checking if each entry's expiration date has passed. If so, it
     * triggers a decrease in censure for the corresponding faction effective on the given day.
     * </p>
     *
     * @param today the date to use when checking for censure expiration and applying any degradation
     *
     * @author Illiani
     * @since 0.50.07
     */
    public void processCensureDegradation(final LocalDate today) {
        factionJudgment.processCensureDegradation(today);
    }

    /**
     * Checks if the specified faction should receive a new or escalated accolade based on its latest standing, and
     * applies the appropriate accolade level if necessary.
     *
     * <p>This method computes the current regard value for the given faction and determines the corresponding
     * standing level. If the calculated standing level is at or below the threshold for accolade, the faction's
     * accolade level will be increased for the provided date. The updated accolade level is then returned; if no change
     * is needed, {@code null} is returned.</p>
     *
     * @param faction           the {@link Faction} object to check against
     * @param today             the date to use when recording a possible accolade improvement
     * @param hasActiveContract {@code true} if the campaign has an active contract, otherwise {@code false}
     *
     * @return the new {@link FactionAccoladeLevel} if an accolade change occurred, or {@code null} if there was no
     *       change
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable FactionAccoladeLevel checkForAccolade(Faction faction, LocalDate today,
          boolean hasActiveContract) {
        if (faction.isAggregate()) {
            return null;
        }

        String factionCode = faction.getShortName();
        if (factionJudgment.factionHasCensure(factionCode)) {
            LOGGER.debug("Faction {} has a censure, so accolade improvement is impossible.", factionCode);
            return null;
        }

        double regard = getRegardForFaction(factionCode, true);
        FactionStandingLevel factionStanding = FactionStandingUtilities.calculateFactionStandingLevel(regard);

        if (factionStanding.getStandingLevel() >= FactionJudgment.THRESHOLD_FOR_ACCOLADE) {
            LOGGER.debug("Faction {} has sufficient standing for accolade improvement.", factionCode);
            // This will return null if no change has taken place
            return factionJudgment.increaseAccoladeForFaction(faction, today, factionStanding, hasActiveContract);
        }

        return null;
    }

    /** Use {@link #updateClimateRegard(Faction, LocalDate, double, boolean)} instead */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String updateClimateRegard(final Faction campaignFaction, final LocalDate today) {
        return updateClimateRegard(campaignFaction, today, 1.0, false);
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
     * @param campaignFaction            the {@link Faction} representing the campaign's primary faction
     * @param today                      the {@link LocalDate} to use for validating factions and determining
     *                                   relationships
     * @param regardMultiplier           the regard multiplier set in campaign options
     * @param enableVerboseClimateRegard {@code true} if the verbose climate regard campaign option is enabled
     *
     * @return an HTML-formatted {@link String} report of faction climate regard changes
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String updateClimateRegard(final Faction campaignFaction, final LocalDate today,
          final double regardMultiplier, final boolean enableVerboseClimateRegard) {
        return updateClimateRegard(campaignFaction, today, regardMultiplier, enableVerboseClimateRegard, false);
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
     * @param campaignFaction            the {@link Faction} representing the campaign's primary faction
     * @param today                      the {@link LocalDate} to use for validating factions and determining
     *                                   relationships
     * @param regardMultiplier           the regard multiplier set in campaign options
     * @param enableVerboseClimateRegard {@code true} if the verbose climate regard campaign option is enabled
     * @param useTestDirectory           {@code true} if called from within a Unit Test
     *
     * @return an HTML-formatted {@link String} report of faction climate regard changes
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String updateClimateRegard(final Faction campaignFaction, final LocalDate today,
          final double regardMultiplier, boolean enableVerboseClimateRegard, boolean useTestDirectory) {
        Collection<Faction> allFactions = Factions.getInstance().getActiveFactions(today);
        FactionHints factionHints = FactionHints.defaultFactionHints(useTestDirectory);
        boolean isPirate = campaignFaction.isPirate();

        // Clear any existing climate regard entries
        climateRegard.clear();

        for (Faction otherFaction : allFactions) {
            if (otherFaction.isAggregate()) {
                continue;
            }

            if (otherFaction.isMercenaryOrganization()) {
                continue;
            }

            String otherFactionCode = otherFaction.getShortName();
            if (otherFactionCode.equals(PIRACY_SUCCESS_INDEX_FACTION_CODE)) {
                continue;
            }

            if (otherFaction.equals(campaignFaction)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_SAME_FACTION * regardMultiplier);
                continue;
            }

            if (factionHints.isRivalOf(campaignFaction, otherFaction, today)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_ENEMY_FACTION_RIVAL * regardMultiplier);
            }

            if (campaignFaction.getShortName().equals(MERCENARY_FACTION_CODE)) {
                double mercenaryRelationsModifier = MercenaryRelations.getMercenaryRelationsModifier(otherFaction,
                      today);

                if (mercenaryRelationsModifier != DEFAULT_REGARD) {
                    climateRegard.put(otherFactionCode, mercenaryRelationsModifier * regardMultiplier);
                }
            }

            if (factionHints.isAtWarWith(campaignFaction, otherFaction, today)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_ENEMY_FACTION_AT_WAR * regardMultiplier);
            }

            if ((isPirate && otherFaction.isPirate()) ||
                      factionHints.isAlliedWith(campaignFaction, otherFaction, today)) {
                climateRegard.put(otherFactionCode, CLIMATE_REGARD_ALLIED_FACTION * regardMultiplier);
                continue;
            }

            if (isPirate) {
                if (otherFaction.isClan()) {
                    climateRegard.put(otherFactionCode,
                          (REGARD_DELTA_CONTRACT_BREACH_EMPLOYER *
                                 regardMultiplier *
                                 PIRATE_CLIMATE_REGARD_ADJUSTMENT_CLAN));
                } else {
                    climateRegard.put(otherFactionCode,
                          REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER *
                                regardMultiplier *
                                PIRATE_CLIMATE_REGARD_ADJUSTMENT_NORMAL);
                }
            }
        }

        // If we're not handling any climate modifiers, return an empty string
        if (climateRegard.isEmpty() || !enableVerboseClimateRegard) {
            return "";
        }

        return buildClimateReport(campaignFaction.isPirate(), campaignFaction.isClan(), today).toString();
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
     * @param campaignIsPirate whether the campaign faction is a pirate faction
     * @param campaignIsClan   whether the campaign faction is a Clan faction
     * @param today            the {@link LocalDate} used for retrieving year-specific faction names
     *
     * @return a {@link StringBuilder} containing the formatted climate regard report
     *
     * @author Illiani
     * @since 0.50.07
     */
    private StringBuilder buildClimateReport(boolean campaignIsPirate, boolean campaignIsClan, LocalDate today) {
        // We minus a day as otherwise this will return false if today is the first day of the First Wave
        boolean clanInvasionHasBegun = MHQConstants.CLAN_INVASION_FIRST_WAVE_BEGINS.minusDays(1).isBefore(today);

        StringBuilder report = new StringBuilder();
        String factionName;
        double regard;
        String reportFormat = "<br>- %s: <span color='%s'><b>%s</b>" + CLOSING_SPAN_TAG;

        List<String> sortedFactionCodes = new ArrayList<>(climateRegard.keySet());
        Collections.sort(sortedFactionCodes);
        for (String factionCode : sortedFactionCodes) {
            regard = climateRegard.get(factionCode);
            String rounded = String.format("%.2f", regard);

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

            // If the Clan Invasion First Wave hasn't occurred
            if (!clanInvasionHasBegun && (campaignIsClan != faction.isClan())) {
                continue;
            }

            factionName = FactionStandingUtilities.getFactionName(faction, today.getYear());
            String color = regard >= 0 ? getPositiveColor() : getNegativeColor();

            report.append(String.format(reportFormat, factionName, color, rounded));
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
                        REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER * 10,
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
     * @param delta          the amount of Regard gained or lost
     * @param gameYear       the current in-game year, used to render the appropriate faction name
     * @param factionCode    unique identifier for the faction whose Regard should be adjusted
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
        Factions factions = Factions.getInstance();
        Faction relevantFaction = factions.getFaction(factionCode);
        FactionStandingLevel originalMilestone = FactionStandingUtilities.calculateFactionStandingLevel(originalRegard);
        FactionStandingLevel newMilestone = FactionStandingUtilities.calculateFactionStandingLevel(newRegard);

        String reportingColor;
        String milestoneChangeReport;
        if (originalMilestone != newMilestone) {
            if (relevantFaction == null) {
                relevantFaction = factions.getDefaultFaction();
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

        String factionName = FactionStandingUtilities.getFactionName(relevantFaction, gameYear);
        if (!factionName.contains(getTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.clan.check"))) {
            factionName = getTextAt(RESOURCE_BUNDLE, "factionStandings.change.report.clan.prefix") + ' ' + factionName;
        }
        String deltaRounded = String.format("%.2f", delta);

        return getFormattedTextAt(RESOURCE_BUNDLE,
              "factionStandings.change.report", factionName, spanOpeningWithCustomColor(reportingColor),
              deltaDirection,
              CLOSING_SPAN_TAG, deltaRounded,
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
     * Use {@link #processRegardDegradation(String, int, double)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public List<String> processRegardDegradation(@Nullable String campaignFactionCode, final int gameYear) {
        return processRegardDegradation(campaignFactionCode, gameYear, 1.0);
    }

    /**
     * Gradually reduces all non-zero faction regard values toward zero by a fixed increment, simulating regard decay
     * over time.
     *
     * <p>For each faction with a non-zero regard value, the method decrements positive values and increments negative
     * values by a fixed amount. This step-wise adjustment continues regard's progression toward zero, with Regard being
     * set to exactly zero if it otherwise crosses zero, thus preventing overshooting. For each adjustment, a report
     * string is generated if an actual change occurs.</p>
     *
     * <p>This method is typically called annually to model the natural decline of relationships or reputation over
     * time.</p>
     *
     * @param campaignFactionCode the unique identifier for the current campaign faction
     * @param gameYear            the current in-game year, used for proper display of faction names in reports
     * @param regardMultiplier    the multiplier set in campaign options.
     *
     * @return a list of formatted report strings describing each regard change made during this process; one entry per
     *       modified faction, or an empty list if no changes occurred
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processRegardDegradation(@Nullable String campaignFactionCode, final int gameYear,
          final double regardMultiplier) {
        List<String> regardChangeReports = new ArrayList<>();
        LOGGER.info("Processing regard decay for {} factions.", factionRegard.size());
        for (String factionCode : new HashSet<>(factionRegard.keySet())) {
            Faction faction = Factions.getInstance().getFaction(factionCode);
            if (faction == null) {
                LOGGER.info("Faction {} is missing from the Factions collection. Skipping.", factionCode);
                continue;
            }

            if (isNotValidForTracking(faction, gameYear)) {
                factionRegard.remove(factionCode);
                continue;
            }

            double currentRegard = factionRegard.get(factionCode);

            if (currentRegard != DEFAULT_REGARD) {
                double delta = currentRegard > DEFAULT_REGARD ?
                                     -DEFAULT_REGARD_DEGRADATION :
                                     DEFAULT_REGARD_DEGRADATION;
                String report = changeRegardForFaction(campaignFactionCode,
                      factionCode,
                      delta,
                      gameYear,
                      regardMultiplier);
                double newRegard = getRegardForFaction(factionCode, false);

                if ((currentRegard > DEFAULT_REGARD && newRegard < DEFAULT_REGARD) ||
                          (currentRegard < DEFAULT_REGARD && newRegard > DEFAULT_REGARD)) {
                    setRegardForFaction(null, factionCode, DEFAULT_REGARD, gameYear, false);
                }

                if (!report.isBlank()) {
                    regardChangeReports.add(report);
                }
            }
        }

        return regardChangeReports;
    }

    /** Use {@link #processContractAccept(String, Faction, LocalDate, double, int)} instead */
    @Deprecated(since = "0.50.08", forRemoval = true)
    public @Nullable String processContractAccept(@Nullable final String campaignFactionCode,
          @Nullable final Faction enemyFaction, final LocalDate today) {
        return processContractAccept(campaignFactionCode, enemyFaction, today, 1.0, 1);
    }

    /**
     * Processes the acceptance of a contract against a specified enemy faction and applies the appropriate regard
     * changes for the campaign faction.
     *
     * <p>This method determines the correct regard penalty based on whether the enemy faction is classified as a
     * clan or not. The penalty is then applied to the regard value between the campaign's faction and the enemy
     * faction. If the enemy faction is null or is an aggregate (grouping rather than a true faction), the method takes
     * no action, and an appropriate report is returned or null.</p>
     *
     * @param campaignFactionCode the unique code for the campaign's faction
     * @param enemyFaction        the {@link Faction} representing the targeted enemy against whom the contract is
     *                            accepted
     * @param today               the current in-game date of contract acceptance
     * @param regardMultiplier    the regard multiplier assigned in campaign options
     * @param contractDuration    how many months the contract is estimated to last
     *
     * @return a summary {@link String} describing the regard changes applied, or the result from
     *       {@link #getMissingFactionReport()} if the enemy is missing, or {@code null} if the enemy faction is an
     *       aggregate
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable String processContractAccept(@Nullable final String campaignFactionCode,
          @Nullable final Faction enemyFaction, final LocalDate today, final double regardMultiplier,
          final int contractDuration) {
        // If we're missing the relevant faction, alert the player and abort
        if (enemyFaction == null) {
            return getMissingFactionReport();
        }

        int gameYear = today.getYear();

        if (enemyFaction.isAggregate()) {
            return null;
        }

        double regardDelta;
        if (enemyFaction.isClan()) {
            regardDelta = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_CLAN;
        } else {
            regardDelta = REGARD_DELTA_CONTRACT_ACCEPT_ENEMY_NORMAL;
        }

        double durationMultiplier = max((double) contractDuration / CONTRACT_DURATION_LENGTH_DIVISOR, 1.0);
        regardDelta *= durationMultiplier;

        return changeRegardForFaction(campaignFactionCode, enemyFaction.getShortName(), regardDelta, gameYear,
              regardMultiplier);
    }

    /** Use {@link #processContractCompletion(Faction, Faction, LocalDate, MissionStatus, double, int)} instead. */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public List<String> processContractCompletion(@Nullable final Faction campaignFaction,
          @Nullable final Faction employerFaction, final LocalDate today, final MissionStatus missionStatus) {
        return processContractCompletion(campaignFaction, employerFaction, today, missionStatus, 1.0, 1);
    }

    /**
     * Processes the outcome of a contract upon its completion and updates regard standings accordingly.
     *
     * <p>Depending on the mission status (success, partial, failure, or breach), this method determines the
     * appropriate regard delta for the employer faction and its allies.</p>
     *
     * <p>If the employer faction is missing, a report is generated and returned accordingly. This report informs the
     * player that they need to manually apply the Standing change via the Standing Report GUI.</p>
     *
     * <p>Regard changes are applied to the employer and all allied factions, and corresponding report strings are
     * returned for each regard change applied.</p>
     *
     * @param campaignFaction  The current campaign faction.
     * @param employerFaction  The {@link Faction} that employed the contract, or {@code null} if unavailable.
     * @param today            The {@link LocalDate} representing the date of contract completion.
     * @param missionStatus    The {@link MissionStatus} of the contract upon completion.
     * @param regardMultiplier The regard gain multiplier set in campaign options
     * @param contractDuration how many months the contract is estimated to last
     *
     * @return A {@link List} of strings summarizing any regard changes or messages relating to missing factions.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processContractCompletion(@Nullable final Faction campaignFaction,
          @Nullable final Faction employerFaction, final LocalDate today, final MissionStatus missionStatus,
          final double regardMultiplier, final int contractDuration) {
        // If the mission is still active, there is nothing to process, so abort
        if (missionStatus == MissionStatus.ACTIVE) {
            return new ArrayList<>();
        }

        double regardDeltaEmployer = getRegardDeltaEmployer(missionStatus, contractDuration);

        List<String> regardChangeReports = new ArrayList<>();

        String campaignFactionCode = campaignFaction.getShortName();
        int gameYear = today.getYear();

        String report;
        if (!employerFaction.isAggregate()) {
            report = changeRegardForFaction(campaignFactionCode,
                  employerFaction.getShortName(),
                  regardDeltaEmployer,
                  gameYear, regardMultiplier);
            if (!report.isBlank()) {
                regardChangeReports.add(report);
            }
        }

        if (campaignFactionCode.equals(MERCENARY_FACTION_CODE)) {
            report = processMercenaryOrganizationRegardUpdate(gameYear,
                  campaignFactionCode,
                  regardDeltaEmployer,
                  regardMultiplier);

            if (!report.isBlank()) {
                regardChangeReports.add(report);
            }
        } else if (campaignFactionCode.equals(PIRATE_FACTION_CODE)) {
            report = changeRegardForFaction(campaignFactionCode,
                  PIRACY_SUCCESS_INDEX_FACTION_CODE,
                  regardDeltaEmployer,
                  gameYear,
                  regardMultiplier);

            if (!report.isBlank()) {
                regardChangeReports.add(report);
            }
        }

        return regardChangeReports;
    }

    private static double getRegardDeltaEmployer(MissionStatus missionStatus, double contractDuration) {
        double regardDeltaEmployer = switch (missionStatus) {
            case SUCCESS -> REGARD_DELTA_CONTRACT_SUCCESS_EMPLOYER;
            case PARTIAL -> REGARD_DELTA_CONTRACT_PARTIAL_EMPLOYER;
            case FAILED -> REGARD_DELTA_CONTRACT_FAILURE_EMPLOYER;
            case BREACH -> REGARD_DELTA_CONTRACT_BREACH_EMPLOYER;
            default -> throw new IllegalStateException("Unexpected value: " + missionStatus);
        };

        double durationMultiplier = max(contractDuration / CONTRACT_DURATION_LENGTH_DIVISOR, 1.0);
        regardDeltaEmployer *= durationMultiplier;
        return regardDeltaEmployer;
    }

    /**
     * Updates the regard for relevant mercenary organizations upon the completion of a contract.
     *
     * <p>This method checks for the presence and temporal validity (for the given game year) of several distinct
     * mercenary authority factions, including:</p>
     *
     * <ul>
     *     <li>The Mercenary Guild (MG)</li>
     *     <li>The Mercenary Review Board (MRB)</li>
     *     <li>The Mercenary Review and Bonding Commission (MRBC)</li>
     *     <li>The Mercenary Bonding Authority (MBA)</li>
     * </ul>
     *
     * <p></p>It updates the regard value for the first valid authority found for the specified year and adjusts the
     * campaign's standing with that mercenary organization by the supplied delta value. If no valid organization is
     * found, no changes occur and warnings are logged as appropriate.</p>
     *
     * @param gameYear            the current game year, used to determine each organization's validity
     * @param campaignFactionCode the short code for the campaign's faction whose standing is to be updated
     * @param regardDeltaEmployer the amount to adjust the regard by (positive or negative)
     * @param regardMultiplier    the regard gain multiplier set in campaign options
     *
     * @return the updated report {@link String}, including any log or status messages from the regard update
     *
     * @author Illiani
     * @since 0.50.07
     */
    private String processMercenaryOrganizationRegardUpdate(int gameYear, String campaignFactionCode,
          double regardDeltaEmployer, double regardMultiplier) {
        final String MERCENARY_GUILD = "MG";
        final String MERCENARY_REVIEW_BOARD = "MRB";
        final String MERCENARY_REVIEW_AND_BONDING_COMMISSION = "MRBC";
        final String MERCENARY_BONDING_AUTHORITY = "MBA";

        Factions factions = Factions.getInstance();
        Faction mercenaryGuild = factions.getFaction(MERCENARY_GUILD);
        if (mercenaryGuild == null) {
            LOGGER.warn("Faction {} did not return valid faction data for Mercenary Guild. Skipping. This " +
                              "needs to be investigated. The most likely cause is that we have changed the faction " +
                              "code for this faction.",
                  MERCENARY_GUILD);
        }

        Faction mercenaryReviewBoard = factions.getFaction(MERCENARY_REVIEW_BOARD);
        if (mercenaryReviewBoard == null) {
            LOGGER.warn("Faction {} did not return valid faction data for Mercenary Review Board. Skipping." +
                              " This needs to be investigated. The most likely cause is that we have changed the " +
                              "faction code for this faction.",
                  MERCENARY_REVIEW_BOARD);
        }

        Faction mercenaryReviewBondingCommission = factions.getFaction(MERCENARY_REVIEW_AND_BONDING_COMMISSION);
        if (mercenaryReviewBondingCommission == null) {
            LOGGER.warn("Faction {} did not return valid faction data for Mercenary Review and Bonding " +
                              "Commission. Skipping. This needs to be investigated. The most likely cause is that we " +
                              "have changed the faction code for this faction.",
                  MERCENARY_REVIEW_AND_BONDING_COMMISSION);
        }

        Faction mercenaryBondingAssociation = factions.getFaction(MERCENARY_BONDING_AUTHORITY);
        if (mercenaryBondingAssociation == null) {
            LOGGER.warn("Faction {} did not return valid faction data for Mercenary Bonding Association. " +
                              "Skipping. This needs to be investigated. The most likely cause is that we have changed" +
                              " the faction code for this faction.",
                  MERCENARY_BONDING_AUTHORITY);
        }

        String mercenaryAuthority = "";
        if (mercenaryGuild != null && mercenaryGuild.validIn(gameYear)) {
            mercenaryAuthority = mercenaryGuild.getShortName();
        } else if (mercenaryReviewBoard != null && mercenaryReviewBoard.validIn(gameYear)) {
            mercenaryAuthority = mercenaryReviewBoard.getShortName();
        } else if (mercenaryReviewBondingCommission != null && mercenaryReviewBondingCommission.validIn(gameYear)) {
            mercenaryAuthority = mercenaryReviewBondingCommission.getShortName();
        } else if (mercenaryBondingAssociation != null && mercenaryBondingAssociation.validIn(gameYear)) {
            mercenaryAuthority = mercenaryBondingAssociation.getShortName();
        }

        String report = "";
        if (!mercenaryAuthority.isBlank()) {
            report = changeRegardForFaction(campaignFactionCode,
                  mercenaryAuthority,
                  regardDeltaEmployer,
                  gameYear,
                  regardMultiplier);
        }

        return report;
    }

    /**
     * Determines whether a given faction should be excluded from tracking for regard (including climate regard, based
     * on its validity in the specified game year or if it is considered "untracked."
     *
     * @param otherFaction the {@link Faction} to evaluate
     * @param gameYear     the year for which validity should be checked
     *
     * @return {@code true} if the faction is either invalid in the specified year or is untracked; {@code false}
     *       otherwise
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

    /** Use {@link #processRefusedBatchall(String, String, int, double)} instead */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public List<String> processRefusedBatchall(final String campaignFactionCode, final String clanFactionCode,
          final int gameYear) {
        return processRefusedBatchall(campaignFactionCode, clanFactionCode, gameYear, 1.0);
    }

    /**
     * Processes the penalty for refusing a batchall against a specific Clan faction.
     *
     * <p>This method applies a regard penalty to the given clan faction code for the specified year and generates a
     * regard change report if applicable.</p>
     *
     * <p>This method is included as a shortcut to allow developers to call Batchall refusal changes without needing to
     * worry about setting up bespoke methods any time this could occur.</p>
     *
     * @param campaignFactionCode The code representing the current campaign faction.
     * @param clanFactionCode     The code representing the clan faction being penalized.
     * @param gameYear            The year in which the batchall was refused.
     * @param regardMultiplier    The regard gain multiplier set in campaign options
     *
     * @return A {@link List} of regard change report strings relating to the refusal.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> processRefusedBatchall(final String campaignFactionCode, final String clanFactionCode,
          final int gameYear, final double regardMultiplier) {
        List<String> regardChangeReports = new ArrayList<>();

        String report = changeRegardForFaction(campaignFactionCode, clanFactionCode, REGARD_DELTA_REFUSE_BATCHALL,
              gameYear, regardMultiplier);

        if (!report.isBlank()) {
            regardChangeReports.add(report);
        }

        return regardChangeReports;
    }

    /**
     * Applies regard changes when the player executes prisoners of war.
     *
     * <p>For each victim in the specified list, the method identifies their origin faction and increments a regard
     * penalty for that faction, unless the faction is untracked. If multiple prisoners originate from the same faction,
     * their penalties are accumulated.</p>
     *
     * <p>After processing all victims, the method applies the total regard change for each affected faction for the
     * specified game year and collects any resulting regard change reports.</p>
     *
     * @param campaignFactionCode the unique identifier for the current campaign faction
     * @param victims             the list of {@link Person} prisoners executed by the player
     * @param gameYear            the year in which the executions and regard changes occur
     * @param regardMultiplier    the regard multiplier set in campaign options
     *
     * @return a {@link List} of non-blank regard change report strings for each affected faction
     */
    public List<String> executePrisonersOfWar(@Nullable String campaignFactionCode, final List<Person> victims,
          final int gameYear, final double regardMultiplier) {
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
            String report = changeRegardForFaction(campaignFactionCode, entry.getKey(), entry.getValue(), gameYear,
                  regardMultiplier);
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

        MHQXMLUtility.writeSimpleXMLOpenTag(writer, indent++, "factionJudgment");
        factionJudgment.writeFactionJudgmentToXML(writer, indent);
        MHQXMLUtility.writeSimpleXMLCloseTag(writer, --indent, "factionJudgment");
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
                } else if (nodeName.equalsIgnoreCase("factionJudgment")) {
                    standings.setFactionJudgment(FactionJudgment.generateInstanceFromXML(childNode));
                }
            }
        } catch (Exception ex) {
            LOGGER.error("Could not parse FactionStandings: ", ex);
        }

        return standings;
    }

    /**
     * Parses a node containing faction regard information and returns a mapping of faction codes to their corresponding
     * regard values as doubles.
     *
     * <p>Each child element node is processed for its name and text value. If the value cannot be parsed as a
     * double, a default is used and the error is logged with the provided label.</p>
     *
     * @param childNode the XML {@link Node} containing child entries representing faction regards
     * @param logLabel  a label to help identify log messages during parsing errors
     *
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

    /** Use {@link #updateCampaignForPastMissions(List, ImageIcon, Faction, LocalDate, double)} instead */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public List<String> updateCampaignForPastMissions(List<Mission> missions, ImageIcon campaignIcon,
          Faction campaignFaction, LocalDate today) {
        return updateCampaignForPastMissions(missions, campaignIcon, campaignFaction, today, 1.0);
    }

    /**
     * Updates the campaign status for a list of past missions, adjusting faction standings, applying campaign icon and
     * faction, and generating a report of changes.
     *
     * <p>The method resets all faction standings, initializes regard values, sorts missions by date and class,
     * groups them by year, and processes each mission to handle standing updates and degradation if needed.</p>
     *
     * @param missions         the list of missions (including {@code Contract} and {@code AtBContract} types) to
     *                         process
     * @param campaignIcon     the icon that represents the campaign visually
     * @param campaignFaction  the main faction for the campaign
     * @param today            the current in-campaign date
     * @param regardMultiplier the multiplier set in campaign options.
     *
     * @return a list of {@link String} objects representing reports or logs of actions performed during the update
     *
     * @author Illiani
     * @since 0.50.07
     */
    public List<String> updateCampaignForPastMissions(List<Mission> missions, ImageIcon campaignIcon,
          Faction campaignFaction, LocalDate today, double regardMultiplier) {
        List<String> reports = new ArrayList<>();

        resetAllFactionStandings();

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

        String campaignFactionCode = campaignFaction.getShortName();
        List<Integer> sortedYears = new ArrayList<>(missionsByYear.keySet());
        Collections.sort(sortedYears);
        for (int year : sortedYears) {
            List<Mission> missionsForYear = missionsByYear.get(year);
            for (Mission mission : missionsForYear) {
                MissionStatus missionStatus = mission.getStatus();

                if (mission instanceof AtBContract atbContract) {
                    int contractLength = atbContract.getLength();
                    String report = processContractAccept(campaignFactionCode,
                          atbContract.getEnemy(),
                          today,
                          regardMultiplier,
                          contractLength);
                    if (report != null) {
                        reports.add(report);
                    }

                    if (missionStatus != MissionStatus.ACTIVE) {
                        reports.addAll(processContractCompletion(campaignFaction, atbContract.getEmployerFaction(),
                              today, missionStatus, regardMultiplier, contractLength));
                    }
                } else {
                    // Non-AtB missions have their Standings updated when the contract concludes
                    if (missionStatus != MissionStatus.ACTIVE) {
                        ManualMissionDialog dialog = new ManualMissionDialog(null,
                              campaignIcon,
                              campaignFaction,
                              today,
                              missionStatus,
                              mission.getName(),
                              mission.getLength());

                        Faction employerChoice = dialog.getEmployerChoice();
                        Faction enemyChoice = dialog.getEnemyChoice();
                        MissionStatus statusChoice = dialog.getStatusChoice();
                        int contractLength = dialog.getDurationChoice();

                        reports.addAll(handleFactionRegardUpdates(campaignFaction, employerChoice,
                              enemyChoice, statusChoice, today, this, regardMultiplier, contractLength));
                    }
                }
            }

            // At the end of each processed year, simulate degradation (unless we're on the current year)
            if (year != currentYear) {
                reports.addAll(processRegardDegradation(campaignFactionCode, year, regardMultiplier));
            }
        }

        return reports;
    }

    /**
     * Sorts the provided list of missions, ordering contract missions before non-contract missions. If both missions
     * are contracts, they are ordered by their start dates.
     *
     * <p>This ensures contracts are prioritized chronologically, while other missions retain their relative order.</p>
     *
     * @param missions the list of missions to sort in-place
     */
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
