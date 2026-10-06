/*
 * Copyright (C) 2018-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionHints;

import static mekhq.MHQConstants.FACTION_DIPLOMACY_DIRECTORY_PATH;
import static mekhq.MHQConstants.FACTION_HINTS_FILE;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Holds the faction diplomacy data (wars, alliances, rivalries, neutral factions, and contained factions) used to
 * compute the probabilities of conflicts between factions.
 *
 * <p>Data is loaded from the per-conflict YAML files in {@code data/universe/factionDiplomacy/} when that directory
 * exists, falling back to the legacy {@code data/universe/factionhints.xml} otherwise. The format is documented in
 * {@code docs/Faction Diplomacy Readme.md}.</p>
 *
 * @author Neoancient
 */
public class FactionHints {
    private static final MMLogger LOGGER = MMLogger.create(FactionHints.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.FactionHints";

    private static final String TEST_DIR = "testresources/" + FACTION_HINTS_FILE.replace("factionhints",
          "factionhints_test");
    private static final String TEST_DIPLOMACY_DIR = "testresources/"
          + FACTION_DIPLOMACY_DIRECTORY_PATH.replace("factionDiplomacy", "factionDiplomacy_test");

    /**
     * Field separator and entry-type labels used by {@link #diplomaticSignatures()} to build the normalized text
     * signatures that {@link #hasSameDiplomaticData(FactionHints)} compares.
     */
    private static final String SIGNATURE_SEPARATOR = "|";
    private static final String SIGNATURE_TYPE_WAR = "war";
    private static final String SIGNATURE_TYPE_ALLIANCE = "alliance";
    private static final String SIGNATURE_TYPE_RIVALRY = "rivalry";
    private static final String SIGNATURE_TYPE_NEUTRAL_EXCEPTION = "neutralException";
    private static final String SIGNATURE_TYPE_NEUTRAL = "neutral";
    private static final String SIGNATURE_TYPE_CONTAINED = "contained";

    private static volatile FactionHints instance;

    private boolean customLegacyDataDetected;

    private final Set<Faction> neutralFactions;

    private final Map<Faction, Map<Faction, List<FactionHint>>> wars;
    private final Map<Faction, Map<Faction, List<FactionHint>>> alliances;
    private final Map<Faction, Map<Faction, List<FactionHint>>> rivals;
    private final Map<Faction, Map<Faction, List<FactionHint>>> neutralExceptions;
    private final Map<Faction, Map<Faction, List<AltLocation>>> containedFactions;

    /**
     * Returns the singleton instance of {@link FactionHints}, initializing it if necessary.
     *
     * @return The singleton FactionHints instance, loaded from default data.
     */
    public static FactionHints getInstance() {
        return getOrInitializeInstance(false);
    }

    /**
     * For test purposes only. Loads the singleton using test directory instead of main data. Call this ONLY in tests,
     * never in production code.
     */
    public static void initializeTestInstance() {
        getOrInitializeInstance(true, true);
    }

    /**
     * Returns the singleton instance of {@link FactionHints}, initializing it if necessary.
     *
     * <p>This method ensures that the instance is fully constructed and loaded with data before being published to
     * other threads.</p>
     *
     * @param useTestDirectory whether to load data from the test directory (for testing only)
     *
     * @return the singleton {@link FactionHints} instance, loaded from the specified data source
     */
    private static FactionHints getOrInitializeInstance(boolean useTestDirectory) {
        return getOrInitializeInstance(useTestDirectory, false);
    }

    /**
     * Returns the singleton instance of {@link FactionHints}, initializing it if necessary.
     *
     * <p>This method ensures that the instance is fully constructed and loaded with data before being published to
     * other threads.</p>
     *
     * @param useTestDirectory whether to load data from the test directory (for testing only)
     * @param loadFreshData    whether to always load fresh data from the data source, even if it's already loaded
     *
     * @return the singleton {@link FactionHints} instance, loaded from the specified data source
     */
    private static FactionHints getOrInitializeInstance(boolean useTestDirectory, boolean loadFreshData) {
        boolean isLoadNewInstance = instance == null || loadFreshData;
        if (isLoadNewInstance) {
            synchronized (FactionHints.class) {
                // The use of tempHints here ensures that the instance is never seen by any other thread until it's
                // fully constructed and initialized, eliminating any risk of a race condition.
                FactionHints tempHints = new FactionHints();
                tempHints.loadData(useTestDirectory);
                instance = tempHints;
            }
        }
        return instance;
    }

    /**
     * Protected constructor that initializes empty data structures.
     */
    public FactionHints() {
        neutralFactions = new HashSet<>();
        wars = new HashMap<>();
        alliances = new HashMap<>();
        rivals = new HashMap<>();
        neutralExceptions = new HashMap<>();
        containedFactions = new HashMap<>();
    }

    public Map<Faction, Map<Faction, List<FactionHint>>> getWars() {
        return wars;
    }

    public Map<Faction, Map<Faction, List<FactionHint>>> getAlliances() {
        return alliances;
    }

    public Map<Faction, Map<Faction, List<FactionHint>>> getRivals() {
        return rivals;
    }

    public Map<Faction, Map<Faction, List<FactionHint>>> getNeutralExceptions() {
        return neutralExceptions;
    }

    /**
     * @deprecated use {@link #getInstance()} instead.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public static FactionHints defaultFactionHints() {
        return defaultFactionHints(false);
    }

    /**
     * @deprecated use {@link #getInstance()} instead.
     */
    @Deprecated(since = "0.50.10", forRemoval = true)
    public static FactionHints defaultFactionHints(boolean useTestDirectory) {
        FactionHints hints = new FactionHints();
        hints.loadData(useTestDirectory);
        return hints;
    }

    private void loadData(boolean useTestDirectory) {
        try {
            File diplomacyDirectory = new File(useTestDirectory ? TEST_DIPLOMACY_DIR
                                                     : FACTION_DIPLOMACY_DIRECTORY_PATH);
            boolean yamlDataLoaded = false;
            if (diplomacyDirectory.isDirectory()) {
                LOGGER.info("[FactionDiplomacy] Loading faction diplomacy data from {}",
                      diplomacyDirectory.getPath());
                yamlDataLoaded = FactionDiplomacyLoader.load(this, diplomacyDirectory);
            }
            if (yamlDataLoaded) {
                checkForCustomLegacyXml(new File(useTestDirectory ? TEST_DIR : FACTION_HINTS_FILE));
            } else {
                LOGGER.warn("[FactionDiplomacy] No faction diplomacy YAML data in {} - falling back to legacy XML {}",
                      diplomacyDirectory.getPath(), FACTION_HINTS_FILE);
                loadFactionHints(useTestDirectory);
            }
        } catch (DOMException e) {
            LOGGER.error("", e);
        }
    }

    /**
     * Accounts for non-existent factions that are used to indicate special status of the planet (undiscovered,
     * abandoned), as well as the synthetic placeholder faction {@link Factions#getFaction(String)} silently returns for
     * any faction code it doesn't recognize (stale/typo'd/retired ownership data), rather than {@code null}.
     *
     * @param f The input faction
     *
     * @return Whether the faction is not a true faction
     */
    public static boolean isEmptyFaction(Faction f) {
        List<String> codes = Arrays.asList(Faction.DEFAULT_CODE, "UND", "ABN", "NONE");
        return codes.contains(f.getShortName());
    }

    /**
     * @param f1 Faction One
     * @param f2 Faction Two
     *
     * @return Whether the factions are allies
     */
    public boolean isAlliedWith(Faction f1, Faction f2,
          LocalDate date) {
        return hintApplies(alliances, f1, f2, date);
    }

    /**
     * Checks whether two factions should be treated as allies because they share a common ally, rather than because of
     * a direct alliance record between the two. This covers member states of the same superpower that are each
     * individually recorded as allied with that superpower (but not with each other directly) &mdash; without this,
     * such member states would incorrectly be valid targets against each other.
     * <p>Only a single degree of separation is considered (a third faction directly allied with both {@code f1} and
     * {@code f2}); this does not recurse through chains of shared allies.</p>
     *
     * @param f1   Faction One
     * @param f2   Faction Two
     * @param date The campaign date
     *
     * @return {@code true} if some other faction is allied with both {@code f1} and {@code f2} on the given date
     */
    public boolean isAlliedThroughSharedAlly(Faction f1, Faction f2, LocalDate date) {
        Set<Faction> knownFactions = new HashSet<>(alliances.keySet());
        for (Map<Faction, List<FactionHint>> nested : alliances.values()) {
            knownFactions.addAll(nested.keySet());
        }

        for (Faction sharedAlly : knownFactions) {
            if (!sharedAlly.equals(f1) &&
                      !sharedAlly.equals(f2) &&
                      isAlliedWith(sharedAlly, f1, date) &&
                      isAlliedWith(sharedAlly, f2, date)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param f1 Faction One
     * @param f2 Faction Two
     *
     * @return Whether the factions are rivals
     */
    public boolean isRivalOf(Faction f1, Faction f2, LocalDate date) {
        return hintApplies(rivals, f1, f2, date);
    }

    /**
     * @param f1 Faction One
     * @param f2 Faction Two
     *
     * @return Whether the factions are at war on the given date
     */
    public boolean isAtWarWith(Faction f1, Faction f2, LocalDate date) {
        return hintApplies(wars, f1, f2, date);
    }

    /**
     *
     * @param f1   A faction
     * @param f2   Another faction
     * @param date The current campaign date
     *
     * @return The name of the current war the two factions are involved in, or {@code null} if they are not currently
     *       at war.
     */
    @Nullable
    public String getCurrentWar(Faction f1, Faction f2,
          LocalDate date) {
        if (wars.get(f1) != null && wars.get(f1).get(f2) != null) {
            for (FactionHint fh : wars.get(f1).get(f2)) {
                if (fh.isInDateRange(date)) {
                    return fh.toString();
                }
            }
        }
        if (wars.get(f2) != null && wars.get(f2).get(f1) != null) {
            for (FactionHint fh : wars.get(f2).get(f1)) {
                if (fh.isInDateRange(date)) {
                    return fh.toString();
                }
            }
        }
        return null;
    }

    /**
     * Indicates a faction is neutral (e.g. ComStar) or non-combatant and should not be chosen as an enemy unless at
     * war at the time. Neutral factions can still act as employers, but their offensive contract types are downgraded
     * to defensive ones against enemies they are not at war with.
     *
     * @param faction Any faction
     *
     * @return Whether the faction is considered neutral
     */
    public boolean isNeutral(Faction faction) {
        return neutralFactions.contains(faction);
    }

    /**
     * Indicates a faction is neutral toward a particular potential opponent. Factions that are generally non-combatant
     * may have certain factions that are exceptions (like pirates) or have particular periods where they go to war
     * despite their normal non-combative nature.
     *
     * @param faction  A potentially neutral faction
     * @param opponent A possible opponent
     * @param date     The campaign date
     *
     * @return true if the potential opponent should not be considered as an enemy
     */
    public boolean isNeutral(Faction faction, Faction opponent,
          LocalDate date) {
        return neutralFactions.contains(faction)
                     && !hintApplies(neutralExceptions, faction, opponent, date)
                     && !isAtWarWith(faction, opponent, date);
    }

    private boolean hintApplies(
          Map<Faction, Map<Faction, List<FactionHint>>> hints,
          Faction f1, Faction f2, LocalDate date) {
        if (hints.get(f1) != null && hints.get(f1).get(f2) != null) {
            for (FactionHint fh : hints.get(f1).get(f2)) {
                if (fh.isInDateRange(date)) {
                    return true;
                }
            }
        }
        if (hints.get(f2) != null && hints.get(f2).get(f1) != null) {
            for (FactionHint fh : hints.get(f2).get(f1)) {
                if (fh.isInDateRange(date)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Some factions are present within the borders of another and do not have any planets that are considered theirs,
     * but still participate in military action. This includes Clan Wolf-in-Exile, the abjured Nova Cats, and the other
     * Inner Sphere powers which operated in the Draconis Combine during Operation Bulldog.
     *
     * @param f    A potential host faction
     * @param date The campaign date
     *
     * @return A Set of all factions (if any) contained within the borders of the host faction.
     */
    public Set<Faction> getContainedFactions(Faction f,
          LocalDate date) {
        HashSet<Faction> retVal = new HashSet<>();
        if (containedFactions.get(f) != null) {
            for (Faction f2 : containedFactions.get(f).keySet()) {
                for (AltLocation l : containedFactions.get(f).get(f2)) {
                    if (l.isInDateRange(date)) {
                        retVal.add(f2);
                    }
                }
            }
        }
        return retVal;
    }

    /**
     * @param contained A faction that is potentially hosted within the borders of another, with no planets directly
     *                  controlled.
     * @param date      The campaign date.
     *
     * @return The faction that controls the planets where the contained faction is positioned, or {@code null} if the
     *       faction is not contained within another at the time. When the faction has several hosts (e.g. the Star
     *       League within each of its member states), an arbitrary one is returned; use
     *       {@link #getContainedFactionHosts(Faction, LocalDate)} to choose among them.
     */
    @Nullable
    public Faction getContainedFactionHost(Faction contained,
          LocalDate date) {
        List<Faction> hosts = getContainedFactionHosts(contained, date);
        return hosts.isEmpty() ? null : hosts.get(0);
    }

    /**
     * @param contained A faction that is potentially hosted within the borders of others, with no planets directly
     *                  controlled.
     * @param date      The campaign date.
     *
     * @return every faction hosting the contained faction on the given date, in no particular order &mdash; e.g. the
     *       Star League is hosted by the Terran Hegemony and each member state. Empty if the faction is not contained
     *       within another at the time.
     */
    public List<Faction> getContainedFactionHosts(Faction contained, LocalDate date) {
        List<Faction> hosts = new ArrayList<>();
        for (Faction f : containedFactions.keySet()) {
            List<AltLocation> locs = containedFactions.get(f).get(contained);
            if (null != locs) {
                for (AltLocation loc : locs) {
                    if (loc.isInDateRange(date)) {
                        hosts.add(f);
                        break;
                    }
                }
            }
        }
        return hosts;
    }

    /**
     * Designates the proportion of space a contained faction takes up within the borders of the host
     *
     * @param host      The host faction
     * @param contained The contained faction
     * @param date      The campaign date
     *
     * @return The ratio of space taken up by the contained faction to that of the host. A value of {@code 0.0} is the
     *       "no split" sentinel (used when the data omits the fraction): consumers then apportion the full border to
     *       both factions instead of splitting it.
     */
    public double getAltLocationFraction(Faction host,
          Faction contained, LocalDate date) {
        if (containedFactions.get(host) != null && containedFactions.get(host).get(contained) != null) {
            for (AltLocation l : containedFactions.get(host).get(contained)) {
                if (l.isInDateRange(date)) {
                    return l.getFraction();
                }
            }
        }
        return 0.0;
    }

    /**
     * Determines whether a faction that is contained within another can consider a third faction to be an opponent. A
     * contained faction is one that does not have any planets assigned to it but occupies space in another faction's
     * space, such as the exiled Clan Wolf or the abjured Clan Nova Cat. Normally these are treated the same way as the
     * containing faction, but in some cases the inner faction may have a reduced set of opponents, such as the Second
     * Star League force in the Draconis Combine during Operation Bulldog, which should only be considered opponents of
     * Clan Smoke Jaguar and not the DC neighbors.
     *
     * @param outer    The faction that controls the planets in the region.
     * @param inner    The faction that occupies planets within the outer faction's space.
     * @param opponent A potential opponent of the inner faction
     * @param date     The campaign date
     *
     * @return Whether {@code opponent} can be treated as an enemy of {@code inner}.
     */
    public boolean isContainedFactionOpponent(Faction outer,
          Faction inner, Faction opponent, LocalDate date) {
        if (containedFactions.get(outer) != null && containedFactions.get(outer).get(inner) != null) {
            for (AltLocation l : containedFactions.get(outer).get(inner)) {
                if (l.isInDateRange(date)) {
                    if (l.getOpponents().isEmpty()) {
                        return !inner.equals(opponent) || hintApplies(wars, inner, inner, date);
                    }

                    for (Faction f : l.getOpponents()) {
                        if (f.equals(opponent)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Adds an alliance
     *
     * @param allianceName The name of the alliance
     * @param start        The alliance start date
     * @param end          The alliance end date
     * @param parties      All the factions involved in the alliance
     */
    public void addAlliance(String allianceName, @Nullable LocalDate start, @Nullable LocalDate end,
          Faction... parties) {
        addFactionHint(alliances, allianceName, start, end, parties);
    }

    /**
     * Adds a war. All named factions are considered to be at war with each other. To add a war with multiple parties on
     * each side, add a war record for each combination.
     *
     * @param warName The name of the war
     * @param start   The war start date
     * @param end     The war end date
     * @param parties All the factions involved in the war.
     */
    public void addWar(String warName, @Nullable LocalDate start, @Nullable LocalDate end,
          Faction... parties) {
        addFactionHint(wars, warName, start, end, parties);
    }

    /**
     * Adds a rivalry
     *
     * @param rivalryName The name of the rivalry
     * @param start       The rivalry start date.
     * @param end         The rivalry end date
     * @param parties     All the factions involved in the rivalry
     */
    protected void addRivalry(String rivalryName, @Nullable LocalDate start, @Nullable LocalDate end,
          Faction... parties) {
        addFactionHint(rivals, rivalryName, start, end, parties);
    }

    /**
     * Adds exceptions to general neutrality for certain possible opponents
     *
     * @param start      The start date for the exception
     * @param end        The end date for the exception
     * @param faction    The generally neutral faction
     * @param exceptions The factions that should be considered exceptions to neutrality
     */
    public void addNeutralExceptions(String exceptionName, @Nullable LocalDate start,
          @Nullable LocalDate end, Faction faction,
          Faction... exceptions) {
        neutralExceptions.putIfAbsent(faction, new HashMap<>());
        for (Faction exception : exceptions) {
            neutralExceptions.get(faction).putIfAbsent(exception, new ArrayList<>());
            neutralExceptions.get(faction).get(exception).add(new FactionHint("", start, end));
        }
    }

    /**
     * Adds faction to list of non-combatants
     *
     */
    public void addNeutralFaction(Faction faction) {
        if (null != faction) {
            neutralFactions.add(faction);
        }
    }

    /**
     * Gives a faction a presence inside another faction without controlling any systems there.
     *
     * @param host      The faction that controls the space
     * @param contained The faction inside the other
     * @param start     The start date
     * @param end       The end date
     * @param ratio     The ratio of the size of the contained faction to that of the host
     */
    public void addContainedFaction(Faction host, Faction contained,
          LocalDate start,
          LocalDate end, double ratio) {
        addContainedFaction(host, contained, start, end, ratio, null);
    }

    /**
     * Gives a faction a presence inside another faction without controlling any systems there and gives it a restricted
     * list of opponents that can be attacked from there.
     *
     * @param host      The faction that controls the space
     * @param contained The faction inside the other
     * @param start     The start date
     * @param end       The end date
     * @param ratio     The ratio of the size of the contained faction to that of the host
     * @param opponents If non-null, all possible opponents based on the position within the other faction should be
     *                  restricted to this list.
     */
    public void addContainedFaction(Faction host, Faction contained,
          LocalDate start, LocalDate end,
          double ratio, @Nullable List<Faction> opponents) {
        containedFactions.putIfAbsent(host, new HashMap<>());
        containedFactions.get(host).putIfAbsent(contained, new ArrayList<>());
        containedFactions.get(host).get(contained).add(new AltLocation(start, end, ratio, opponents));
    }

    private void addFactionHint(
          Map<Faction, Map<Faction, List<FactionHint>>> hintMap,
          String name,
          LocalDate start, LocalDate end, Faction[] parties) {
        FactionHint hint = new FactionHint(name, start, end);
        for (int i = 0; i < parties.length - 1; i++) {
            for (int j = i + 1; j < parties.length; j++) {
                if ((null != parties[i]) && (null != parties[j])) {
                    hintMap.putIfAbsent(parties[i], new HashMap<>());
                    hintMap.get(parties[i]).putIfAbsent(parties[j], new ArrayList<>());
                    hintMap.get(parties[i]).get(parties[j]).add(hint);
                }
            }
        }
    }

    /**
     * @return {@code true} if a legacy {@code factionhints.xml} whose contents differ from the loaded faction
     *       diplomacy data was found. This usually means a player carried a customized XML file into a release that
     *       ships the {@code factionDiplomacy} YAML directory, so their customizations are not in effect.
     */
    public boolean isCustomLegacyDataDetected() {
        return customLegacyDataDetected;
    }

    /**
     * Detects a customized legacy {@code factionhints.xml} that is being ignored in favor of the YAML directory, and
     * politely tells the player how to migrate their custom wars. Does nothing when the legacy file is absent or when
     * its contents match the loaded YAML data (the stock file shipped alongside the directory).
     *
     * @param legacyXmlFile the legacy XML file location to check
     */
    private void checkForCustomLegacyXml(File legacyXmlFile) {
        if (!legacyXmlFile.isFile() || !legacyXmlDiffers(legacyXmlFile)) {
            return;
        }
        customLegacyDataDetected = true;
        LOGGER.warn("""
                    [FactionDiplomacy] {} differs from the shipped faction diplomacy data. \
                    That XML file is NO LONGER LOADED when the {} directory is present, so any custom wars or \
                    alliances in it are not in effect. To keep them, recreate them as a YAML file in the \
                    factionDiplomacy directory (the format is documented in docs/Faction Diplomacy Readme.md). \
                    If the XML file was not customized, it can simply be deleted.""",
              legacyXmlFile.getPath(), FACTION_DIPLOMACY_DIRECTORY_PATH);
        showCustomLegacyDataWarning(legacyXmlFile);
    }

    /**
     * Loads the given legacy XML into a scratch instance and compares its diplomatic content against this instance.
     *
     * @param legacyXmlFile the legacy {@code factionhints.xml} to compare
     *
     * @return {@code true} if the XML contains different diplomatic data than this instance
     */
    boolean legacyXmlDiffers(File legacyXmlFile) {
        FactionHints legacyHints = new FactionHints();
        try {
            legacyHints.loadFactionHintsFromXmlFile(legacyXmlFile);
        } catch (RuntimeException exception) {
            LOGGER.error(exception,
                  "[FactionDiplomacy] Could not parse " + legacyXmlFile.getPath() + " for comparison");
            return true;
        }
        return !hasSameDiplomaticData(legacyHints);
    }

    /**
     * @param other another instance to compare with
     *
     * @return {@code true} if both instances hold exactly the same wars, alliances, rivalries, neutral factions, and
     *       contained factions
     */
    boolean hasSameDiplomaticData(FactionHints other) {
        return diplomaticSignatures().equals(other.diplomaticSignatures());
    }

    /**
     * @return a sorted, normalized text signature of every diplomatic entry held by this instance, used to compare
     *       two data sources for semantic equality independent of file format or entry order
     */
    private List<String> diplomaticSignatures() {
        List<String> signatures = new ArrayList<>();
        appendHintSignatures(signatures, SIGNATURE_TYPE_WAR, wars);
        appendHintSignatures(signatures, SIGNATURE_TYPE_ALLIANCE, alliances);
        appendHintSignatures(signatures, SIGNATURE_TYPE_RIVALRY, rivals);
        appendHintSignatures(signatures, SIGNATURE_TYPE_NEUTRAL_EXCEPTION, neutralExceptions);
        for (Faction faction : neutralFactions) {
            signatures.add(SIGNATURE_TYPE_NEUTRAL + SIGNATURE_SEPARATOR + faction.getShortName());
        }
        for (Faction host : containedFactions.keySet()) {
            for (Faction contained : containedFactions.get(host).keySet()) {
                for (AltLocation location : containedFactions.get(host).get(contained)) {
                    List<String> opponentCodes = location.getOpponents().stream()
                                                       .map(Faction::getShortName)
                                                       .sorted()
                                                       .toList();
                    signatures.add(String.join(SIGNATURE_SEPARATOR, SIGNATURE_TYPE_CONTAINED,
                          host.getShortName(), contained.getShortName(),
                          String.valueOf(location.getStart()), String.valueOf(location.getEnd()),
                          String.valueOf(location.getFraction()), opponentCodes.toString()));
                }
            }
        }
        Collections.sort(signatures);
        return signatures;
    }

    private static void appendHintSignatures(List<String> signatures, String type,
          Map<Faction, Map<Faction, List<FactionHint>>> hints) {
        for (Faction firstFaction : hints.keySet()) {
            for (Faction secondFaction : hints.get(firstFaction).keySet()) {
                String firstCode = firstFaction.getShortName();
                String secondCode = secondFaction.getShortName();
                String pair = (firstCode.compareTo(secondCode) <= 0)
                                    ? firstCode + "," + secondCode
                                    : secondCode + "," + firstCode;
                for (FactionHint hint : hints.get(firstFaction).get(secondFaction)) {
                    signatures.add(String.join(SIGNATURE_SEPARATOR, type, pair, hint.getName(),
                          String.valueOf(hint.getStart()), String.valueOf(hint.getEnd())));
                }
            }
        }
    }

    /**
     * Shows the custom-data warning dialog unless running headless (tests, dedicated servers).
     *
     * @param legacyXmlFile the customized legacy XML file
     */
    private void showCustomLegacyDataWarning(File legacyXmlFile) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        String title = getTextAt(RESOURCE_BUNDLE, "FactionHints.customLegacyXml.title");
        String message = getFormattedTextAt(RESOURCE_BUNDLE, "FactionHints.customLegacyXml.message",
              legacyXmlFile.getPath(), FACTION_DIPLOMACY_DIRECTORY_PATH);
        SwingUtilities.invokeLater(() ->
              JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE));
    }

    /**
     * Legacy loader for the monolithic {@code factionhints.xml}. Only used when the {@code factionDiplomacy} YAML
     * directory is not present; see {@link FactionDiplomacyLoader} for the current format.
     */
    private void loadFactionHints(boolean useTestDirectory) throws DOMException {
        loadFactionHintsFromXmlFile(new File(useTestDirectory ? TEST_DIR : FACTION_HINTS_FILE));
    }

    /**
     * Parses the given legacy faction hints XML file into this instance.
     *
     * @param xmlFile the XML file to parse
     */
    private void loadFactionHintsFromXmlFile(File xmlFile) throws DOMException {
        LOGGER.info("Starting load of faction hint data from XML...");
        Document xmlDoc;

        try (InputStream is = new FileInputStream(xmlFile)) {
            DocumentBuilder db = MHQXMLUtility.newSafeDocumentBuilder();

            xmlDoc = db.parse(is);
        } catch (Exception e) {
            LOGGER.error("", e);
            return;
        }

        Element rootElement = xmlDoc.getDocumentElement();
        NodeList nl = rootElement.getChildNodes();
        rootElement.normalize();

        for (int i = 0; i < nl.getLength(); i++) {
            Node wn = nl.item(i);

            if (wn.getParentNode() != rootElement) {
                continue;
            }

            if (wn.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = wn.getNodeName();

                switch (nodeName) {
                    case "neutral" -> {
                        String fKey = wn.getAttributes().getNamedItem("faction").getTextContent().trim();
                        Faction f = Factions.getInstance()
                                          .getFaction(fKey);
                        if (f.getShortName().equalsIgnoreCase(Faction.DEFAULT_CODE)) {
                            LOGGER.error("Invalid faction code in factionhints.xml: {}", fKey);
                        } else {
                            neutralFactions.add(f);
                            addNeutralExceptions(f, wn);
                        }
                    }
                    case "rivals" -> setFactionHint(rivals, wn);
                    case "war" -> setFactionHint(wars, wn);
                    case "alliance" -> setFactionHint(alliances, wn);
                    case "location" -> {
                        LocalDate start = null;
                        LocalDate end = null;
                        double fraction = 0.0;
                        String outerCode = "";
                        String innerCode = "";
                        List<Faction> opponents = null;
                        if (wn.getAttributes().getNamedItem("start") != null) {
                            start = MHQXMLUtility
                                          .parseDate(wn.getAttributes().getNamedItem("start").getTextContent().trim());
                        }
                        if (wn.getAttributes().getNamedItem("end") != null) {
                            end = MHQXMLUtility.parseDate(wn.getAttributes()
                                                                .getNamedItem("end")
                                                                .getTextContent()
                                                                .trim());
                        }
                        for (int j = 0; j < wn.getChildNodes().getLength(); j++) {
                            try {
                                Node wn2 = wn.getChildNodes().item(j);
                                switch (wn2.getNodeName()) {
                                    case "outer":
                                        outerCode = wn2.getTextContent().trim();
                                        break;
                                    case "inner":
                                        innerCode = wn2.getTextContent().trim();
                                        break;
                                    case "fraction":
                                        fraction = Double.parseDouble(wn2.getTextContent().trim());
                                        break;
                                    case "opponents":
                                        opponents = new ArrayList<>();
                                        for (String fKey : wn2.getTextContent().trim().split(",")) {
                                            Faction f = Factions.getInstance()
                                                              .getFaction(fKey);
                                            if (!f.getShortName()
                                                       .equalsIgnoreCase(Faction.DEFAULT_CODE)) {
                                                opponents.add(f);
                                            }
                                        }
                                        break;
                                }
                            } catch (Exception e) {
                                LOGGER.error("", e);
                            }
                        }

                        final Faction outer = Factions.getInstance()
                                                    .getFaction(outerCode);
                        final Faction inner = Factions.getInstance()
                                                    .getFaction(innerCode);
                        if (outer.getShortName().equalsIgnoreCase(Faction.DEFAULT_CODE)
                                  ||
                                  inner.getShortName().equalsIgnoreCase(Faction.DEFAULT_CODE)) {
                            LOGGER.error("Invalid faction code in factionhints.xml: {}/{}", outerCode, innerCode);
                        } else {
                            addContainedFaction(outer, inner, start, end, fraction, opponents);
                        }
                    }
                }
            }
        }
    }

    private void setFactionHint(
          Map<Faction, Map<Faction, List<FactionHint>>> hint, Node node)
          throws DOMException {
        String name = "";
        LocalDate start = null;
        LocalDate end = null;
        if (node.getAttributes().getNamedItem("name") != null) {
            name = node.getAttributes().getNamedItem("name").getTextContent().trim();
        }
        if (node.getAttributes().getNamedItem("start") != null) {
            start = MHQXMLUtility.parseDate(node.getAttributes().getNamedItem("start").getTextContent().trim());
        }
        if (node.getAttributes().getNamedItem("end") != null) {
            end = MHQXMLUtility.parseDate(node.getAttributes().getNamedItem("end").getTextContent().trim());
        }
        for (int n = 0; n < node.getChildNodes().getLength(); n++) {
            Node wn = node.getChildNodes().item(n);
            if (wn.getNodeName().equals("parties")) {
                LocalDate localStart = start;
                LocalDate localEnd = end;
                if (wn.getAttributes().getNamedItem("start") != null) {
                    localStart = MHQXMLUtility
                                       .parseDate(wn.getAttributes().getNamedItem("start").getTextContent().trim());
                }
                if (wn.getAttributes().getNamedItem("end") != null) {
                    localEnd = MHQXMLUtility.parseDate(wn.getAttributes().getNamedItem("end").getTextContent().trim());
                }

                String[] factionKeys = wn.getTextContent().trim().split(",");
                Faction[] parties = new Faction[factionKeys.length];
                for (int i = 0; i < factionKeys.length; i++) {
                    final Faction faction = Factions.getInstance()
                                                  .getFaction(factionKeys[i]);
                    if (faction.getShortName().equalsIgnoreCase(Faction.DEFAULT_CODE)) {
                        LOGGER.error("Invalid faction code in factionhints.xml: {}", factionKeys[i]);
                        continue;
                    }
                    parties[i] = faction;
                }
                addFactionHint(hint, name, localStart, localEnd, parties);
            }
        }
    }

    private void addNeutralExceptions(Faction faction, Node node) throws DOMException {
        LocalDate end = null;
        if (node.getAttributes().getNamedItem("end") != null) {
            end = MHQXMLUtility.parseDate(node.getAttributes().getNamedItem("end").getTextContent().trim());
        }

        for (int n = 0; n < node.getChildNodes().getLength(); n++) {
            Node wn = node.getChildNodes().item(n);
            if (wn.getNodeName().equals("exceptions")) {
                LocalDate localStart = null;
                LocalDate localEnd = end;
                if (wn.getAttributes().getNamedItem("start") != null) {
                    localStart = MHQXMLUtility
                                       .parseDate(wn.getAttributes().getNamedItem("start").getTextContent().trim());
                }

                if (wn.getAttributes().getNamedItem("end") != null) {
                    localEnd = MHQXMLUtility.parseDate(wn.getAttributes().getNamedItem("end").getTextContent().trim());
                }

                String[] parties = wn.getTextContent().trim().split(",");

                for (String party : parties) {
                    final Faction f = Factions.getInstance()
                                            .getFaction(party);
                    if (f.getShortName().equalsIgnoreCase(Faction.DEFAULT_CODE)) {
                        LOGGER.error("Invalid faction code in factionhints.xml: {}", party);
                        continue;
                    }
                    addNeutralExceptions("", localStart, localEnd, faction, f);
                }
            }
        }
    }
}
