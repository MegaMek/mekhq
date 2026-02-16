/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2009-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe;

import static megamek.common.compute.Compute.randomInt;

import java.awt.Color;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import megamek.client.ratgenerator.FactionRecord;
import megamek.common.annotations.Nullable;
import megamek.common.universe.Faction2;
import megamek.common.universe.FactionLeaderData;
import megamek.common.universe.FactionTag;
import megamek.common.universe.HonorRating;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.ranks.Ranks;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Faction {

    // region Variable Declarations
    public static final String DEFAULT_CODE = "???";
    public static final String MERCENARY_FACTION_CODE = "MERC";
    public static final String PIRATE_FACTION_CODE = "PIR";
    public static final String COMSTAR_FACTION_CODE = "CS";

    private Faction2 faction2;

    private final String shortName;
    private final String fullName;
    private NavigableMap<Integer, String> nameChanges = new TreeMap<>();
    private String[] alternativeFactionCodes;
    private String startingPlanet;
    private final NavigableMap<LocalDate, String> planetChanges = new TreeMap<>();
    private String nameGenerator;
    private int[] eraMods;
    private Color color;
    private final String currencyCode = ""; // Currency of the faction, if any
    private String layeredFormationIconBackgroundCategory;
    private String layeredFormationIconBackgroundFilename;
    private String layeredFormationIconLogoCategory;
    private String layeredFormationIconLogoFilename;
    private Set<FactionTag> tags;
    private int start; // Start year (inclusive)
    private int end; // End year (inclusive)
    private String successor;
    private HonorRating preInvasionHonorRating;
    private HonorRating postInvasionHonorRating;

    // endregion Variable Declarations

    // region Constructors
    public Faction() {
        this(DEFAULT_CODE, "Unknown");
    }

    public Faction(final String shortName, final String fullName) {
        this.shortName = shortName;
        this.fullName = fullName;
        nameGenerator = "General";
        color = Color.LIGHT_GRAY;
        startingPlanet = "Terra";
        eraMods = null;
        setLayeredFormationIconBackgroundCategory("");
        setLayeredFormationIconBackgroundFilename(null);
        setLayeredFormationIconLogoCategory("");
        setLayeredFormationIconLogoFilename(null);
        tags = EnumSet.noneOf(FactionTag.class);
        start = 0;
        end = 9999;
    }

    public Faction(Faction2 faction2) {
        this(faction2.getKey(), faction2.getName());
        this.faction2 = faction2;
        startingPlanet = faction2.getCapital();
        nameGenerator = faction2.getNameGenerator();
        color = faction2.getColor();
        successor = faction2.getSuccessor();
        List<String> yamlTags = faction2.getTags().stream().map(Enum::toString).toList();
        tags = yamlTags.stream().map(FactionTag::valueOf).collect(Collectors.toSet());
        eraMods = faction2.getEraMods();
        if (faction2.getBackground() != null) {
            Path backgroundPath = Path.of(faction2.getBackground());
            if (backgroundPath.getParent() != null) {
                layeredFormationIconBackgroundCategory = backgroundPath.getParent().toString();
            }
            layeredFormationIconBackgroundFilename = backgroundPath.getFileName().toString();
        }
        if (faction2.getLogo() != null) {
            Path logoPath = Path.of(faction2.getLogo());
            if (logoPath.getParent() != null) {
                layeredFormationIconLogoCategory = logoPath.getParent().toString();
            }
            layeredFormationIconLogoFilename = logoPath.getFileName().toString();
        }
        alternativeFactionCodes = faction2.getFallBackFactions().toArray(new String[0]);
        nameChanges = faction2.getNameChanges();
        for (Entry<Integer, String> entry : faction2.getCapitalChanges().entrySet()) {
            planetChanges.put(LocalDate.ofYearDay(entry.getKey(), 1), entry.getValue());
        }
        List<FactionRecord.DateRange> active = faction2.getYearsActive();
        if (!active.isEmpty()) {
            start = Objects.requireNonNullElse(active.get(0).start, 0);
            end = Objects.requireNonNullElse(active.get(active.size() - 1).end, 9999);
        }
        HonorRating preInvasion = faction2.getPreInvasionHonorRating();
        HonorRating postInvasion = faction2.getPostInvasionHonorRating();
        if (isClan()) {
            preInvasionHonorRating = (preInvasion != HonorRating.NONE) ? preInvasion : HonorRating.STRICT;
            postInvasionHonorRating = (postInvasion != HonorRating.NONE) ? postInvasion : HonorRating.OPPORTUNISTIC;
        } else {
            preInvasionHonorRating = HonorRating.NONE;
            postInvasionHonorRating = HonorRating.NONE;
        }
    }
    // endregion Constructors

    public String getShortName() {
        return shortName;
    }

    public String getFullName(int year) {
        Entry<Integer, String> change = nameChanges.floorEntry(year);
        if (null == change) {
            return fullName;
        } else {
            return change.getValue();
        }
    }

    public @Nullable String[] getAlternativeFactionCodes() {
        return alternativeFactionCodes;
    }

    public Color getColor() {
        return color;
    }

    public String getNameGenerator() {
        return nameGenerator;
    }

    public @Nullable PlanetarySystem getStartingPlanet(final Campaign campaign, final LocalDate date) {
        return campaign.getSystemById(getStartingPlanet(date));
    }

    public String getStartingPlanet(final LocalDate date) {
        final Entry<LocalDate, String> change = planetChanges.floorEntry(date);
        return (change == null) ? startingPlanet : change.getValue();
    }

    public Optional<String> getCamosFolder(int year) {
        return Optional.ofNullable(faction2 != null ? faction2.getCamosFolder(year) : null);
    }

    public int getEraMod(int year) {
        if (eraMods == null) {
            return 0;
        } else {
            if (year < 2570) {
                // Era: Age of War
                return eraMods[0];
            } else if (year < 2598) {
                // Era: RW
                return eraMods[1];
            } else if (year < 2785) {
                // Era: Star League
                return eraMods[2];
            } else if (year < 2828) {
                // Era: 1st SW
                return eraMods[3];
            } else if (year < 2864) {
                // Era: 2nd SW
                return eraMods[4];
            } else if (year < 3028) {
                // Era: 3rd SW
                return eraMods[5];
            } else if (year < 3050) {
                // Era: 4th SW
                return eraMods[6];
            } else if (year < 3067) {
                // Era: Clan Invasion
                return eraMods[7];
            } else if (year < 3086) {
                // Era: Jihad
                return eraMods[8];
            } else if (year < 3151) {
                // Era: Dark Age
                return eraMods[9];
            } else {
                // Era: ilClan
                return eraMods[10];
            }
        }
    }

    public boolean is(FactionTag tag) {
        return tags.contains(tag);
    }

    /**
     * Updates the set of tags associated with the faction. Tags represent various attributes or characteristics that
     * describe the faction, such as its size, alignment, behavior, or role within the campaign universe.
     *
     * @param tags the set of tags to be assigned to the faction. Each tag represents a specific characteristic or
     *             quality of the faction, such as {@code PIRATE}, {@code SUPER}, {@code REBEL}, among others.
     */
    public void setTags(Set<FactionTag> tags) {
        this.tags = tags;
    }

    public boolean validIn(final LocalDate today) {
        return validIn(today.getYear());
    }

    public boolean validIn(int year) {
        return (year >= start) && (year <= end);
    }

    public boolean validBetween(int startYear, int endYear) {
        return (startYear <= end) && (endYear >= start);
    }

    public int getStartYear() {
        return start;
    }

    public int getEndYear() {
        return end;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getLayeredFormationIconBackgroundCategory() {
        return layeredFormationIconBackgroundCategory;
    }

    public void setLayeredFormationIconBackgroundCategory(final String layeredFormationIconBackgroundCategory) {
        this.layeredFormationIconBackgroundCategory = layeredFormationIconBackgroundCategory;
    }

    public @Nullable String getLayeredFormationIconBackgroundFilename() {
        return layeredFormationIconBackgroundFilename;
    }

    public void setLayeredFormationIconBackgroundFilename(final @Nullable String layeredFormationIconBackgroundFilename) {
        this.layeredFormationIconBackgroundFilename = layeredFormationIconBackgroundFilename;
    }

    public String getLayeredFormationIconLogoCategory() {
        return layeredFormationIconLogoCategory;
    }

    public void setLayeredFormationIconLogoCategory(final String layeredFormationIconLogoCategory) {
        this.layeredFormationIconLogoCategory = layeredFormationIconLogoCategory;
    }

    public @Nullable String getLayeredFormationIconLogoFilename() {
        return layeredFormationIconLogoFilename;
    }

    public void setLayeredFormationIconLogoFilename(final @Nullable String layeredFormationIconLogoFilename) {
        this.layeredFormationIconLogoFilename = layeredFormationIconLogoFilename;
    }

    // region Checks
    public boolean isPlayable() {
        return is(FactionTag.PLAYABLE);
    }

    public boolean isMercenary() {
        return is(FactionTag.MERC);
    }

    /**
     * Determines whether the faction is a mercenary organization based on its short name.
     *
     * <p>Currently this checks whether the faction is the Mercenary Guild, Mercenary Review Board, Mercenary Review
     * and Bonding Commission, or the Mercenary Bondy Authority.</p>
     *
     * @return {@code true} if the faction's short name matches any of the predefined mercenary organization identifiers
     *       ("MG", "MRB", "MRBC", "MBA"); {@code false} otherwise.
     */
    public boolean isMercenaryOrganization() {
        final String MERCENARY_GUILD = "MG";
        final String MERCENARY_REVIEW_BOARD = "MRB";
        final String MERCENARY_REVIEW_BONDING_COMMISSION = "MRBC";
        final String MERCENARY_BONDY_AUTHORITY = "MBA";

        return Objects.equals(shortName, MERCENARY_GUILD) ||
                     Objects.equals(shortName, MERCENARY_REVIEW_BOARD) ||
                     Objects.equals(shortName, MERCENARY_REVIEW_BONDING_COMMISSION) ||
                     Objects.equals(shortName, MERCENARY_BONDY_AUTHORITY);
    }

    /**
     * Returns the currently active mercenary organization for the given game year.
     *
     * <p>This method checks a prioritized list of known mercenary organizations and returns the first one found to
     * be valid for the specified year. The search order is:</p>
     *
     * <ol>
     *     <li>Mercenary Guild</li>
     *     <li>Mercenary Review Board</li>
     *     <li>Mercenary Review and Bonding Commission</li>
     *     <li>Mercenary Bondy Authority</li>
     * </ol>
     *
     * <p>If none of the organizations other than the Mercenary Guild are valid in the given year, the Mercenary
     * Guild is returned by default.</p>
     *
     * @param gameYear the year to check for an active organization
     *
     * @return the {@code Faction} object representing the active mercenary organization
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static Faction getActiveMercenaryOrganization(int gameYear) {
        final Factions factions = Factions.getInstance();
        final Faction MERCENARY_GUILD = factions.getFaction("MG");
        if (MERCENARY_GUILD.validIn(gameYear)) {
            return MERCENARY_GUILD;
        }

        final Faction MERCENARY_REVIEW_BOARD = factions.getFaction("MRB");
        if (MERCENARY_REVIEW_BOARD.validIn(gameYear)) {
            return MERCENARY_REVIEW_BOARD;
        }

        final Faction MERCENARY_REVIEW_BONDING_COMMISSION = factions.getFaction("MRBC");
        if (MERCENARY_REVIEW_BONDING_COMMISSION.validIn(gameYear)) {
            return MERCENARY_REVIEW_BONDING_COMMISSION;
        }

        final Faction MERCENARY_BONDY_AUTHORITY = factions.getFaction("MBA");
        if (MERCENARY_BONDY_AUTHORITY.validIn(gameYear)) {
            return MERCENARY_BONDY_AUTHORITY;
        }

        return MERCENARY_GUILD;
    }

    public boolean isPirate() {
        return is(FactionTag.PIRATE);
    }

    public boolean isRebel() {
        return is(FactionTag.REBEL);
    }

    public boolean isRebelOrPirate() {
        return isRebel() || isPirate();
    }

    public boolean isGovernment() {
        return !isClan() && (isComStar() || isISMajorOrSuperPower() || isMinorPower()
                                   || isPlanetaryGovt() || isIndependent());
    }

    public boolean isComStar() {
        return "CS".equals(getShortName());
    }

    public boolean isWoB() {
        return "WOB".equals(getShortName());
    }

    public boolean isComStarOrWoB() {
        return isComStar() || isWoB();
    }

    public boolean isMarianHegemony() {
        return "MH".equals(getShortName());
    }

    public boolean isClan() {
        return is(FactionTag.CLAN);
    }

    public boolean isInnerSphere() {
        return is(FactionTag.IS);
    }

    public boolean isPeriphery() {
        return is(FactionTag.PERIPHERY);
    }

    public boolean isDeepPeriphery() {
        return is(FactionTag.DEEP_PERIPHERY);
    }

    public boolean isIndependent() {
        return "IND".equals(getShortName());
    }

    // region Power Checks
    public boolean isSuperPower() {
        return is(FactionTag.SUPER);
    }

    public boolean isMajorOrSuperPower() {
        return isMajorPower() || isSuperPower();
    }

    public boolean isISMajorOrSuperPower() {
        return isInnerSphere() && isMajorOrSuperPower();
    }

    public boolean isMajorPower() {
        return is(FactionTag.MAJOR);
    }

    public boolean isMinorPower() {
        return is(FactionTag.MINOR);
    }

    public boolean isSmall() {
        return is(FactionTag.SMALL);
    }

    public boolean isNoble() {
        return is(FactionTag.NOBLE);
    }

    public boolean isPlanetaryGovt() {
        return is(FactionTag.PLANETARY_GOVERNMENT);
    }

    public boolean isCorporation() {
        return is(FactionTag.CORPORATION);
    }

    public boolean isInactive() {
        return is(FactionTag.INACTIVE);
    }

    public boolean isActive() {
        return !isInactive();
    }

    public boolean isChaos() {
        return is(FactionTag.CHAOS);
    }

    public boolean isStingy() {
        return is(FactionTag.STINGY);
    }

    public boolean isGenerous() {
        return is(FactionTag.GENEROUS);
    }

    public boolean isControlling() {
        return is(FactionTag.CONTROLLING);
    }

    public boolean isLenient() {
        return is(FactionTag.LENIENT);
    }
    // endregion Power Checks
    // endregion Checks

    /** @return Sorted list of faction names as one string */
    public static String getFactionNames(Collection<Faction> factions, int year) {
        if (null == factions) {
            return "-";
        }
        List<String> factionNames = new ArrayList<>(factions.size());
        for (Faction f : factions) {
            factionNames.add(f.getFullName(year));
        }
        Collections.sort(factionNames);
        return Utilities.combineString(factionNames, "/");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Faction other) {
            return (null != shortName) && (shortName.equals(other.shortName));
        }
        return false;
    }

    /**
     * Returns the honor rating for a given Clan. Our research showed the post-Invasion shift in Clan doctrine to occur
     * between 3053 and 3055. This is based on the table found on page 274 of Total Warfare. Any Clan not mentioned on
     * that table is assumed to be Strict (pre-invasion) -> Opportunistic (post-invasion).
     * <p>
     * Note that this method uses 3053 and 3054 randomly as a threshold to determine if the current year is
     * post-invasion. Thus, in those years, the result may vary between calls with otherwise equal parameters.
     *
     * @param campaign the ongoing campaign
     *
     * @return the honor rating as an {@link HonorRating} enum
     */
    public HonorRating getHonorRating(Campaign campaign) {
        boolean isPostInvasion = campaign.getLocalDate().getYear() >= 3053 + randomInt(2);
        return isPostInvasion ? postInvasionHonorRating : preInvasionHonorRating;
    }

    /**
     * Returns the size of the lowest formation type (e.g., lance). If this faction gives the size directly
     * (formationBaseSize) this value is returned. Otherwise, the fallback Factions are called recursively. When there
     * is no callback Faction, 5 is returned for a clan faction and 4 otherwise.
     * <p>
     * This means that the Word of Blake Faction will give a value of 6 and WoB subcommands do not have to give any
     * value as long as their fallback Faction is WoB.
     *
     * @return The size of a lance, point or analogous formation type
     */
    public int getFormationBaseSize() {
        return faction2.getFormationBaseSize();
    }

    /**
     * Returns the grouping multiplier for accumulated formations such as company, galaxy or level 3. If this faction
     * gives the value directly (formationGrouping) this value is returned. Otherwise, the fallback Factions are called
     * recursively. When there is no callback Faction, 5 is returned for a clan faction and 3 otherwise (3 lances form a
     * company, 3 companies form a battalion etc.)
     * <p>
     * This means that the Word of Blake Faction will give a value of 6 and WoB subcommands do not have to give any
     * value as long as their fallback Faction is WoB.
     *
     * @return How many formations form a formation of a higher type (e.g., lances in a company)
     */
    public int getFormationGrouping() {
        return faction2.getFormationGrouping();
    }

    /**
     * Retrieves the rank system identifier for this faction.
     *
     * <p>The method checks the `rankSystem` field; if it is set and not {@code -1}, its value is returned
     * directly.</p>
     *
     * <p>If the rank system is unspecified but there are fallback factions, the method iterates through each
     * fallback faction, returning the first available rank system found among them.</p>
     *
     * <p>If no fallback faction provides a rank system, the method returns a default value based on whether the
     * faction is a clan or not.</p>
     *
     * @return the rank system identifier for this faction, or a default value ({@code CLAN} for Clan factions,
     *       {@code SLDF} for non-Clan factions) if not specified.
     *
     * @author Illiani
     * @since 0.50.07
     */
    public String getRankSystemCode() {
        return faction2.getRankSystem();
    }

    /**
     * Returns the {@link RankSystem} object associated with this faction.
     *
     * <p>This uses the rank system code obtained from {@link #getRankSystemCode()} and looks up the corresponding
     * {@link RankSystem} instance using {@code Ranks.getRankSystemFromCode}. If a {@link RankSystem} cannot be found
     * for the return value of {@link #getRankSystemCode()}, the rank system associated with
     * {@link Ranks#DEFAULT_SYSTEM_CODE} will be returned.</p>
     *
     * @return the {@code RankSystem} for this faction, or {@code null} if not found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public RankSystem getRankSystem() {
        return Ranks.getRankSystemFromCode(getRankSystemCode());
    }

    /**
     * Determines whether this faction performs Batchalls.
     *
     * <p>Batchalls are a tradition among specific factions - primarily various clans and related groups. </p>
     *
     * @return {@code true} if the faction performs Batchalls; {@code false} otherwise
     */
    public boolean performsBatchalls() {
        return faction2.performsBatchalls();
    }

    /**
     * @return {@code true} if the faction is an aggregate of independent 'factions', rather than a singular
     *       organization.
     *
     *       <p>For example, "PIR" (pirates) is used to abstractly represent all pirates, not individual pirate
     *       groups.</p>
     *
     * @author Illiani
     * @since 0.50.07
     */
    public boolean isAggregate() {
        return faction2.isAggregate();
    }

    /**
     * Retrieves the faction leader in power during the specified year.
     *
     * @param year the year to check for a valid leader
     *
     * @return the {@link FactionLeaderData} for the leader valid in the given year, or {@code null} if none found
     *
     * @author Illiani
     * @since 0.50.07
     */
    public @Nullable FactionLeaderData getLeaderForYear(final int year) {
        return faction2.getFactionLeaderForYear(year);
    }
}
