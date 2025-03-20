/*
 * Copyright (c) 2009 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2009-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.universe;

import megamek.common.annotations.Nullable;
import megamek.common.universe.Faction2;
import megamek.common.universe.FactionTag;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import megamek.common.universe.HonorRating;

import java.awt.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static megamek.common.Compute.randomInt;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Faction {

    // region Variable Declarations
    public static final String DEFAULT_CODE = "???";

    private Faction2 faction2;

    private String shortName;
    private String fullName;
    private NavigableMap<Integer, String> nameChanges = new TreeMap<>();
    private String[] altNames;
    private String[] alternativeFactionCodes;
    private String startingPlanet;
    private NavigableMap<LocalDate, String> planetChanges = new TreeMap<>();
    private String nameGenerator;
    private int[] eraMods;
    private Color color;
    private String currencyCode = ""; // Currency of the faction, if any
    private String layeredForceIconBackgroundCategory;
    private String layeredForceIconBackgroundFilename;
    private String layeredForceIconLogoCategory;
    private String layeredForceIconLogoFilename;
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
        setLayeredForceIconBackgroundCategory("");
        setLayeredForceIconBackgroundFilename(null);
        setLayeredForceIconLogoCategory("");
        setLayeredForceIconLogoFilename(null);
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
            if (backgroundPath.getParent()!=null) {
                layeredForceIconBackgroundCategory = backgroundPath.getParent().toString();
            }
            layeredForceIconBackgroundFilename = backgroundPath.getFileName().toString();
        }
        if (faction2.getLogo() != null) {
            Path logoPath = Path.of(faction2.getLogo());
            if (logoPath.getParent()!=null) {
                layeredForceIconLogoCategory = logoPath.getParent().toString();
            }
            layeredForceIconLogoFilename = logoPath.getFileName().toString();
        }
        alternativeFactionCodes = faction2.getFallBackFactions().toArray(new String[0]);
        nameChanges = faction2.getNameChanges();
        for (Entry<Integer, String> entry : faction2.getCapitalChanges().entrySet()) {
            planetChanges.put(LocalDate.ofYearDay(entry.getKey(), 1), entry.getValue());
        }
        if (!faction2.getYearsActive().isEmpty()) {
            Integer startYear = faction2.getYearsActive().get(0).start;
            start = startYear == null ? 0 : startYear;
            Integer endYear = faction2.getYearsActive().get(0).end;
            start = endYear == null ? 0 : endYear;
        }
        HonorRating preInvasion = faction2.getPreInvasionHonorRating();
        preInvasionHonorRating = (preInvasion != null) ? preInvasion : HonorRating.STRICT;
        HonorRating postInvasion = faction2.getPostInvasionHonorRating();
        postInvasionHonorRating = (postInvasion != null) ? postInvasion : HonorRating.OPPORTUNISTIC;
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
     * Updates the set of tags associated with the faction. Tags represent various
     * attributes or characteristics that describe the faction, such as its size,
     * alignment, behavior, or role within the campaign universe.
     *
     * @param tags the set of tags to be assigned to the faction. Each tag represents
     *             a specific characteristic or quality of the faction, such as
     *             {@code PIRATE}, {@code SUPER}, {@code REBEL}, among others.
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

    public String getLayeredForceIconBackgroundCategory() {
        return layeredForceIconBackgroundCategory;
    }

    public void setLayeredForceIconBackgroundCategory(final String layeredForceIconBackgroundCategory) {
        this.layeredForceIconBackgroundCategory = layeredForceIconBackgroundCategory;
    }

    public @Nullable String getLayeredForceIconBackgroundFilename() {
        return layeredForceIconBackgroundFilename;
    }

    public void setLayeredForceIconBackgroundFilename(final @Nullable String layeredForceIconBackgroundFilename) {
        this.layeredForceIconBackgroundFilename = layeredForceIconBackgroundFilename;
    }

    public String getLayeredForceIconLogoCategory() {
        return layeredForceIconLogoCategory;
    }

    public void setLayeredForceIconLogoCategory(final String layeredForceIconLogoCategory) {
        this.layeredForceIconLogoCategory = layeredForceIconLogoCategory;
    }

    public @Nullable String getLayeredForceIconLogoFilename() {
        return layeredForceIconLogoFilename;
    }

    public void setLayeredForceIconLogoFilename(final @Nullable String layeredForceIconLogoFilename) {
        this.layeredForceIconLogoFilename = layeredForceIconLogoFilename;
    }

    // region Checks
    public boolean isPlayable() {
        return is(FactionTag.PLAYABLE);
    }

    public boolean isMercenary() {
        return is(FactionTag.MERC);
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
     * @return the honor rating as an {@link HonorRating} enum
     */
    public HonorRating getHonorRating(Campaign campaign) {
        boolean isPostInvasion = campaign.getLocalDate().getYear() >= 3053 + randomInt(2);
        return isPostInvasion ? postInvasionHonorRating : preInvasionHonorRating;
    }

    /**
     * Returns the size of the lowest formation type (e.g., lance). If this faction gives the size directly
     * (formationBaseSize:) this value is returned. Otherwise the fallback Factions are called recursively. When there
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
     * gives the value directly (formationGrouping:) this value is returned. Otherwise the fallback Factions are called
     * recursively. When there is no callback Faction, 5 is returned for a clan faction and 3 otherwise (3 lances form a
     * company, 3 companies form a battalion etc)
     * <p>
     * This means that the Word of Blake Faction will give a value of 6 and WoB subcommands do not have to give any
     * value as long as their fallback Faction is WoB.
     *
     * @return How many formations form a formation of a higher type (e.g., lances in a company)
     */
    public int getFormationGrouping() {
        return faction2.getFormationGrouping();
    }
}
