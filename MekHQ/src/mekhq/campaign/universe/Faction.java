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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.universe;

import static megamek.common.Compute.randomInt;
import static mekhq.campaign.universe.enums.HonorRating.LIBERAL;
import static mekhq.campaign.universe.enums.HonorRating.OPPORTUNISTIC;
import static mekhq.campaign.universe.enums.HonorRating.STRICT;

import java.awt.Color;
import java.time.LocalDate;
import java.util.*;
import java.util.Map.Entry;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.enums.HonorRating;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author Jay Lawson (jaylawson39 at yahoo.com)
 */
public class Faction {
    private static final MMLogger logger = MMLogger.create(Faction.class);

    // region Variable Declarations
    public static final String DEFAULT_CODE = "???";

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
    private Set<Tag> tags;
    private int start; // Start year (inclusive)
    private int end; // End year (inclusive)
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
        tags = EnumSet.noneOf(Tag.class);
        start = 0;
        end = 9999;
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

    public void setAlternativeFactionCodes(final String... alternativeFactionCodes) {
        this.alternativeFactionCodes = alternativeFactionCodes;
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

    public boolean is(Tag tag) {
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
    public void setTags(Set<Tag> tags) {
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
        return is(Tag.PLAYABLE);
    }

    public boolean isMercenary() {
        return is(Tag.MERC);
    }

    public boolean isPirate() {
        return is(Tag.PIRATE);
    }

    public boolean isRebel() {
        return is(Tag.REBEL);
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
        return is(Tag.CLAN);
    }

    public boolean isInnerSphere() {
        return is(Tag.IS);
    }

    public boolean isPeriphery() {
        return is(Tag.PERIPHERY);
    }

    public boolean isDeepPeriphery() {
        return is(Tag.DEEP_PERIPHERY);
    }

    public boolean isIndependent() {
        return "IND".equals(getShortName());
    }

    // region Power Checks
    public boolean isSuperPower() {
        return is(Tag.SUPER);
    }

    public boolean isMajorOrSuperPower() {
        return isMajorPower() || isSuperPower();
    }

    public boolean isISMajorOrSuperPower() {
        return isInnerSphere() && isMajorOrSuperPower();
    }

    public boolean isMajorPower() {
        return is(Tag.MAJOR);
    }

    public boolean isMinorPower() {
        return is(Tag.MINOR);
    }

    public boolean isSmall() {
        return is(Tag.SMALL);
    }

    public boolean isNoble() {
        return is(Tag.NOBLE);
    }

    public boolean isPlanetaryGovt() {
        return is(Tag.PLANETARY_GOVERNMENT);
    }

    public boolean isCorporation() {
        return is(Tag.CORPORATION);
    }

    public boolean isInactive() {
        return is(Tag.INACTIVE);
    }

    public boolean isChaos() {
        return is(Tag.CHAOS);
    }

    public boolean isStingy() {
        return is(Tag.STINGY);
    }

    public boolean isGenerous() {
        return is(Tag.GENEROUS);
    }

    public boolean isControlling() {
        return is(Tag.CONTROLLING);
    }

    public boolean isLenient() {
        return is(Tag.LENIENT);
    }
    // endregion Power Checks
    // endregion Checks

    public static Faction getFactionFromXML(Node wn) throws DOMException {
        Faction retVal = new Faction();
        NodeList nl = wn.getChildNodes();

        for (int x = 0; x < nl.getLength(); x++) {
            Node wn2 = nl.item(x);
            try {
                if (wn2.getNodeName().equalsIgnoreCase("shortname")) {
                    retVal.shortName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("fullname")) {
                    retVal.fullName = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("altNamesByYear")) {
                    int year = Integer.parseInt(wn2.getAttributes().getNamedItem("year").getTextContent());
                    retVal.nameChanges.put(year, wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("altNames")) {
                    retVal.altNames = wn2.getTextContent().split(",", 0);
                } else if (wn2.getNodeName().equalsIgnoreCase("alternativeFactionCodes")) {
                    retVal.setAlternativeFactionCodes(wn2.getTextContent().trim().split(","));
                } else if (wn2.getNodeName().equalsIgnoreCase("startingPlanet")) {
                    retVal.startingPlanet = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("changePlanet")) {
                    retVal.planetChanges.put(
                            MHQXMLUtility.parseDate(wn2.getAttributes().getNamedItem("year").getTextContent().trim()),
                            wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("eraMods")) {
                    String[] values = wn2.getTextContent().split(",", -2);
                    int eraModCount = values.length;
                    retVal.eraMods = new int[eraModCount];
                    for (int i = 0; i < eraModCount; i++) {
                        retVal.eraMods[i] = Integer.parseInt(values[i]);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("nameGenerator")) {
                    retVal.nameGenerator = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("colorRGB")) {
                    String[] values = wn2.getTextContent().split(",");
                    if (values.length == 3) {
                        int colorRed = Integer.parseInt(values[0]);
                        int colorGreen = Integer.parseInt(values[1]);
                        int colorBlue = Integer.parseInt(values[2]);
                        retVal.color = new Color(colorRed, colorGreen, colorBlue);
                    }
                } else if (wn2.getNodeName().equalsIgnoreCase("currencyCode")) {
                    retVal.currencyCode = wn2.getTextContent();
                } else if (wn2.getNodeName().equalsIgnoreCase("layeredForceIconBackgroundCategory")) {
                    retVal.setLayeredForceIconBackgroundCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("layeredForceIconBackgroundFilename")) {
                    retVal.setLayeredForceIconBackgroundFilename(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("layeredForceIconLogoCategory")) {
                    retVal.setLayeredForceIconLogoCategory(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("layeredForceIconLogoFilename")) {
                    retVal.setLayeredForceIconLogoFilename(wn2.getTextContent().trim());
                } else if (wn2.getNodeName().equalsIgnoreCase("tags")) {
                    Arrays.stream(wn2.getTextContent().split(",")).map(tag -> tag.toUpperCase(Locale.ROOT))
                            .map(Tag::valueOf).forEach(tag -> retVal.tags.add(tag));
                } else if (wn2.getNodeName().equalsIgnoreCase("start")) {
                    retVal.start = Integer.parseInt(wn2.getTextContent());
                } else if (wn2.getNodeName().equalsIgnoreCase("end")) {
                    retVal.end = Integer.parseInt(wn2.getTextContent());
                }
            } catch (Exception e) {
                logger.error("", e);
            }
        }

        return retVal;
    }

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

    public enum Tag {
        /** Inner sphere */
        IS, PERIPHERY, DEEP_PERIPHERY, CLAN,
        /** A bunch of dirty pirates */
        PIRATE,
        /** Major mercenary bands */
        MERC,
        /** Major trading company */
        TRADER,
        /**
         * Super Power: the Terran Hegemony, the First Star League, and the Federated
         * Commonwealth. (CamOps p12)
         */
        SUPER,
        /**
         * Major Power: e.g. Inner Sphere Great Houses, Republic of the Sphere, Terran
         * Alliance,
         * Second Star League, Inner Sphere Clans. (CamOps p12)
         */
        MAJOR,
        /**
         * Faction is limited to a single star system, or potentially just a part of a
         * planet (CamOps p12)
         */
        MINOR,
        /** Independent world or Small State (CamOps p12) */
        SMALL,
        /** Faction is rebelling against the superior ("parent") faction */
        REBEL,
        /**
         * Faction isn't overtly acting on the political/military scale; think ComStar
         * before clan invasion
         */
        INACTIVE,
        /** Faction represents empty space */
        ABANDONED,
        /** Faction represents a lack of unified government */
        CHAOS,
        /** Faction is campaign-specific, generated on the fly */
        GENERATED,
        /** Faction is hidden from view */
        HIDDEN,
        /** Faction code is not intended to be for players */
        SPECIAL,
        /** Faction is meant to be played */
        PLAYABLE,
        /** Faction is an independent noble (Camops p. 39) */
        NOBLE,
        /** Faction is an independent planetary government (Camops p. 39) */
        PLANETARY_GOVERNMENT,
        /** Faction is an independent corporation (Camops p. 39) */
        CORPORATION,
        /** Faction is stingy and tends to pay less for contracts (Camops p. 42) */
        STINGY,
        /** Faction is generous and tends to pay more for contracts (Camops p. 42) */
        GENEROUS,
        /** Faction is controlling with mercenary command rights (Camops p. 42) */
        CONTROLLING,
        /** Faction is lenient with mercenary command rights (Camops p. 42) */
        LENIENT
    }

    /**
     * Calculates the honor rating for a given Clan.
     *
     * @param campaign    the ongoing campaign
     * @return the honor rating as an {@link HonorRating} enum
     */
    public HonorRating getHonorRating(Campaign campaign) {
        // Our research showed the post-Invasion shift in Clan doctrine to occur between 3053 and 3055
        boolean isPostInvasion = campaign.getLocalDate().getYear() >= 3053 + randomInt(2);

        // This is based on the table found on page 274 of Total Warfare
        // Any Clan not mentioned on that table is assumed to be Strict → Opportunistic
        return switch (shortName) {
            case "CCC", "CHH", "CIH", "CNC", "CSR" -> OPPORTUNISTIC;
            case "CCO", "CGS", "CSV" -> STRICT;
            case "CGB", "CWIE" -> {
                if (isPostInvasion) {
                    yield LIBERAL;
                } else {
                    yield STRICT;
                }
            }
            case "CDS" -> LIBERAL;
            case "CW" -> {
                if (isPostInvasion) {
                    yield LIBERAL;
                } else {
                    yield OPPORTUNISTIC;
                }
            }
            default -> {
                if (isPostInvasion) {
                    yield OPPORTUNISTIC;
                } else {
                    yield STRICT;
                }
            }
        };
    }

    /**
     * Determines whether this faction performs Batchalls based on its short name.
     *
     * <p>Batchalls are a tradition among specific factions - primarily various clans and related groups. This method
     * checks if the faction's short name matches any of the known batchall-performing factions.</p>
     *
     * @return {@code true} if the faction performs Batchalls; {@code false} otherwise
     */
    public boolean performsBatchalls() {
        List<String> batchallFactions = List.of("CBS",
              "CB",
              "CCC",
              "CCO",
              "CDS",
              "CFM",
              "CGB",
              "CGS",
              "CHH",
              "CIH",
              "CJF",
              "CMG",
              "CNC",
              "CSJ",
              "CSR",
              "CSA",
              "CSV",
              "CSL",
              "CWI",
              "CW",
              "CWE",
              "CWIE",
              "CEI",
              "RD",
              "RA",
              "CP",
              "AML",
              "CLAN");

        return batchallFactions.contains(shortName);
    }
}
