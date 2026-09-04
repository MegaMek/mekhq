/*
 * Copyright (c) 2011 - Jay Lawson (jaylawson39 at yahoo.com). All Rights Reserved.
 * Copyright (C) 2011-2026 The MegaMek Team. All Rights Reserved.
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

import static mekhq.campaign.universe.Faction.ABANDONED_FACTION_CODE;
import static mekhq.campaign.universe.Faction.DISPUTED_FACTION_CODE;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import megamek.codeUtilities.ObjectUtility;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.personnel.education.Academy;
import mekhq.campaign.personnel.education.AcademyFactory;
import mekhq.campaign.universe.enums.CapitalType;
import mekhq.campaign.universe.enums.HPGRating;
import mekhq.campaign.universe.enums.HiringHallLevel;

/**
 * This is a PlanetarySystem object that will contain information about the system as well as an ArrayList of the Planet
 * objects that make up the system
 *
 * @author Taharqa
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(converter = PlanetarySystem.PlanetarySystemPostLoader.class)
public class PlanetarySystem {
    private static final double COMMAND_CIRCUIT_RECHARGE_TIME_HOURS = 10;

    // --- Sophistication Rating Enum ---
    public enum PlanetarySophistication {
        ADVANCED(0, "Advanced"),
        A(1, "A"),
        B(2, "B"),
        C(3, "C"),
        D(4, "D"),
        F(5, "F"),
        REGRESSED(6, "Regressed");

        private final int index;
        private final String name;
        private static final Map<Integer, PlanetarySophistication> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, PlanetarySophistication> NAME_LOOKUP = new HashMap<>();

        static {
            for (PlanetarySophistication tr : values()) {
                INDEX_LOOKUP.put(tr.index, tr);
                NAME_LOOKUP.put(tr.name, tr);
            }
        }

        PlanetarySophistication(int idx, String name) {
            this.index = idx;
            this.name = name;
        }

        public int getIndex() {return index;}

        public String getName() {return name;}

        public static PlanetarySophistication fromIndex(int idx) {
            PlanetarySophistication tr = INDEX_LOOKUP.get(idx);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetarySophistication index: " + idx);}
            return tr;
        }

        public static PlanetarySophistication fromName(String name) {
            PlanetarySophistication tr = NAME_LOOKUP.get(name);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetarySophistication name: " + name);}
            return tr;
        }

        public boolean isBetterOrEqualThan(PlanetarySophistication other) {
            return index <= other.index;
        }

        public boolean isBetterThan(PlanetarySophistication other) {
            return index < other.index;
        }
    }

    // --- Planetary Rating Enum ---
    public enum PlanetaryRating {
        A(0, "A"),
        B(1, "B"),
        C(2, "C"),
        D(3, "D"),
        F(4, "F");

        private final int index;
        private final String name;
        private static final Map<Integer, PlanetaryRating> INDEX_LOOKUP = new HashMap<>();
        private static final Map<String, PlanetaryRating> NAME_LOOKUP = new HashMap<>();

        static {
            for (PlanetaryRating tr : values()) {
                INDEX_LOOKUP.put(tr.index, tr);
                NAME_LOOKUP.put(tr.name, tr);
            }
        }

        PlanetaryRating(int idx, String name) {
            this.index = idx;
            this.name = name;
        }

        public int getIndex() {return index;}

        public String getName() {return name;}

        public static PlanetaryRating fromIndex(int idx) {
            PlanetaryRating tr = INDEX_LOOKUP.get(idx);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetaryRating index: " + idx);}
            return tr;
        }

        public static PlanetaryRating fromName(String name) {
            PlanetaryRating tr = NAME_LOOKUP.get(name);
            if (tr == null) {throw new IllegalArgumentException("Invalid PlanetaryRating name: " + name);}
            return tr;
        }
    }


    @JsonProperty("xcood")
    private Double x;
    @JsonProperty("ycood")
    private Double y;

    // Base data
    @JsonProperty("id")
    private String id;
    @JsonProperty("sucsId")
    @JsonDeserialize(using = SucsIdDeserializer.class)
    private Integer sucsId;
    private String name;

    // Star data (to be factored out)
    @JsonProperty("spectralType")
    private SourceableValue<StarType> star;

    /**
     * {@code true} for synthetic systems loaded from {@code mm-data/data/universe/planetary_systems/connector_systems/}
     * — these are jump-path routing helpers (DPR, HWY, LTR, FDR, ER, HL prefixes) with no inhabitants, no faction
     * history, and no canonical lore. They share the loader and registry with real canon systems and need to be
     * filtered out of any UI that asks the user to pick a real system (e.g. the origin-system picker in
     * {@code CustomizePersonDialog} — see issue #8934).
     *
     * <p>Set by {@link Systems#parsePlanetarySystemFiles} based on the loading directory. {@code @JsonIgnore}
     * keeps it out of any future YAML/JSON serialization round-trip (the YAML schema has no equivalent field
     * and the value is reconstructed from the load path) — defensive against schema drift.</p>
     */
    @JsonIgnore
    private boolean connector = false;

    // tree map of planets sorted by system position
    private TreeMap<Integer, Planet> planets;

    // for reading in because lists are easier
    private List<Planet> planetList;

    // the location of the primary planet for this system
    @JsonProperty("primarySlot")
    private SourceableValue<Integer> primarySlot;

    /**
     * a hash to keep track of dynamic planet changes
     * <p>
     * sorted map of [date of change: change information]
     * <p>
     * Package-private so that Planets can access it
     */
    TreeMap<LocalDate, PlanetarySystemEvent> events;

    // For import only; lists are easier than maps in YAML.
    private List<PlanetarySystemEvent> eventList;

    public PlanetarySystem() {

    }

    public PlanetarySystem(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Integer getSucsId() {
        return sucsId;
    }

    public static class SucsIdDeserializer extends JsonDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) {
                return null;
            }
            if (token == JsonToken.VALUE_NUMBER_INT) {
                return parser.getIntValue();
            }
            if (token == JsonToken.VALUE_STRING) {
                String text = parser.getText().trim();
                if (text.isEmpty() || ".na".equalsIgnoreCase(text)) {
                    return null;
                }
                try {
                    return Integer.valueOf(text);
                } catch (NumberFormatException ex) {
                    throw JsonMappingException.from(parser, "Cannot deserialize sucsId from '" + text
                          + "'; expected integer or .na", ex);
                }
            }
            return (Integer) context.handleUnexpectedToken(Integer.class, parser);
        }
    }

    /**
     * @return {@code true} if this system was loaded from the {@code connector_systems/} subdirectory — a
     *       synthetic jump-path routing helper with no inhabitants. UI code that asks the user to pick a
     *       real system should filter these out. See {@link #connector} for full context.
     */
    @JsonIgnore
    public boolean isConnector() {
        return connector;
    }

    /** Marks this system as a synthetic connector (used by {@link Systems} during YAML loading). */
    @JsonIgnore
    public void setConnector(boolean connector) {
        this.connector = connector;
    }

    public Double getX() {
        return x;
    }

    public Double getY() {
        return y;
    }

    public String getName(LocalDate when) {
        // if no primary slot was explicitly defined, then just return the id
        if (getSourcedPrimarySlot() == null && id != null) {
            return id;
        }

        if (null != getPrimaryPlanet()) {
            return getPrimaryPlanet().getName(when);
        }

        return "Unknown";
    }

    public List<String> getFactions(LocalDate when) {
        List<String> factions = new ArrayList<>();
        for (Planet planet : planets.values()) {
            List<String> f = planet.getFactions(when);
            if (null != f) {
                factions.addAll(f);
            }
        }
        return factions;
    }

    public Set<Faction> getFactionSet(LocalDate when) {
        Set<Faction> factions = new HashSet<>();
        for (Planet planet : planets.values()) {
            Set<Faction> f = planet.getFactionSet(when);
            if (null != f) {
                factions.addAll(f);
            }
        }
        // ignore cases where abandoned (ABN) is given in addition to real factions
        if (factions.size() > 1) {
            factions.remove(Factions.getInstance().getFaction("ABN"));
        }
        return factions;
    }

    /**
     * The look-back window, in years, over which prior rulers still count toward a world's living population. A native
     * up to this old could have been born under a faction that has since lost the world, so those factions remain a
     * plausible birth origin.
     */
    private static final int POPULATION_WINDOW_YEARS = 40;

    /** Average days per year (accounting for leap years), for converting a tenure span into whole-year weights. */
    private static final double DAYS_PER_YEAR = 365.25;

    /**
     * Returns the factions a person native to this system could plausibly have been born under, each weighted by how
     * long it ruled within the last {@link #POPULATION_WINDOW_YEARS} years.
     *
     * <p>For every planet, this measures how much of the window each real faction held the world and credits it that
     * many years of tenure. A faction that has held the world for the whole window weighs
     * {@link #POPULATION_WINDOW_YEARS}; one that ruled only briefly weighs proportionally less (minimum 1). Tenure is
     * summed across the system's planets, so a faction credited on several worlds accumulates their spans.</p>
     *
     * <p>Contested stretches are handled specially. {@link #getFactionSet(LocalDate)} reports a disputed world's owner
     * as the aggregate {@code DIS} ("Disputed") pseudo-faction, which is not a real polity a person can belong to. The
     * duration of each dispute is instead split evenly between the two sides fighting over it: the owner recorded
     * immediately before the dispute and the one who eventually resolves it (which may lie in the future for a dispute
     * still raging at {@code when}). This keeps both combatants in the pool, weighted by how long the world has been
     * fought over.</p>
     *
     * <p>The {@code DIS} marker is never returned, and {@code ABN} (Abandoned) is dropped when other owners are
     * present.</p>
     *
     * @param when the date to evaluate ownership at
     *
     * @return a map of birth-eligible factions to their tenure weight in whole years; may be empty if a disputed world
     *       has no recorded owner before or after the dispute
     */
    public Map<Faction, Integer> getPopulationFactions(LocalDate when) {
        Map<Faction, Double> tenureYears = new HashMap<>();
        for (Planet planet : planets.values()) {
            accumulatePopulationTenure(tenureYears, planet, when);
        }

        Map<Faction, Integer> weights = new HashMap<>();
        for (Map.Entry<Faction, Double> entry : tenureYears.entrySet()) {
            // Any faction that ruled at all is worth at least one ticket; longer and multi-world tenure weighs more.
            int weight = (int) Math.max(1, Math.round(entry.getValue()));
            weights.put(entry.getKey(), weight);
        }

        // Abandoned (ABN) is a pseudo-faction; it must never be treated as a valid birth origin.
        weights.remove(Factions.getInstance().getFaction(ABANDONED_FACTION_CODE));
        return weights;
    }

    /**
     * Accumulates, into {@code tenureYears}, how long each real faction held {@code planet} within the last
     * {@link #POPULATION_WINDOW_YEARS} years before {@code when}.
     *
     * <p>Walks the ownership timeline, crediting each owner the length of its reign clipped to the window. Future
     * ownership (after {@code when}) is not part of the current population and is skipped, except that an ongoing
     * dispute's eventual resolver is credited its share of the contested span (see {@link #creditContestants}).</p>
     *
     * @param tenureYears the running per-faction tenure total, in years
     * @param planet      the planet whose ownership timeline is walked
     * @param when        the date the population is evaluated at
     */
    private static void accumulatePopulationTenure(Map<Faction, Double> tenureYears, Planet planet, LocalDate when) {
        List<Planet.PlanetaryEvent> events = planet.getEvents();
        if (events == null) {
            return;
        }
        LocalDate windowStart = when.minusYears(POPULATION_WINDOW_YEARS);

        // Ownership snapshots (events that set a faction) in chronological order, across the whole timeline.
        List<Planet.PlanetaryEvent> ownership = new ArrayList<>();
        for (Planet.PlanetaryEvent event : events) {
            if (event.date != null && event.faction != null && event.faction.getValue() != null) {
                ownership.add(event);
            }
        }

        for (int i = 0; i < ownership.size(); i++) {
            Planet.PlanetaryEvent event = ownership.get(i);
            if (!event.date.isBefore(when)) {
                break; // this reign begins at or after the evaluation date — not part of the current population
            }
            // This owner holds from its own date until the next ownership change (or the evaluation date).
            LocalDate segmentEnd = (i + 1 < ownership.size()) ? ownership.get(i + 1).date : when;
            if (segmentEnd.isAfter(when)) {
                segmentEnd = when;
            }
            // Clip the reign to the living-population window.
            LocalDate segmentStart = event.date.isBefore(windowStart) ? windowStart : event.date;
            if (!segmentStart.isBefore(segmentEnd)) {
                continue; // reign falls entirely outside the window
            }
            double years = ChronoUnit.DAYS.between(segmentStart, segmentEnd) / DAYS_PER_YEAR;

            if (isDisputedOnly(event.faction.getValue())) {
                creditContestants(tenureYears, ownership, i, years);
            } else {
                creditCodes(tenureYears, event.faction.getValue(), years);
            }
        }
    }

    /**
     * Credits {@code years} of tenure to each real faction named in {@code codes} (the Disputed marker and unknown
     * codes are skipped). Co-owners each receive the full span rather than a split.
     */
    private static void creditCodes(Map<Faction, Double> tenureYears, List<String> codes, double years) {
        Set<Faction> factions = new HashSet<>();
        addFactionsFromCodes(factions, codes);
        for (Faction faction : factions) {
            tenureYears.merge(faction, years, Double::sum);
        }
    }

    /**
     * Splits a contested span evenly between the two sides fighting over the world: the last real owner recorded before
     * the dispute at {@code disputeIndex} and the first real owner recorded after it (which may lie in the future for a
     * dispute still ongoing at the evaluation date). If only one side is recorded, it takes the whole span.
     *
     * @param tenureYears  the running per-faction tenure total, in years
     * @param ownership    the planet's chronological ownership snapshots
     * @param disputeIndex the index of the disputed snapshot within {@code ownership}
     * @param years        the length of the contested span, in years
     */
    private static void creditContestants(Map<Faction, Double> tenureYears, List<Planet.PlanetaryEvent> ownership,
          int disputeIndex, double years) {
        Set<Faction> contestants = new HashSet<>();
        for (int j = disputeIndex - 1; j >= 0; j--) {
            List<String> codes = ownership.get(j).faction.getValue();
            if (!isDisputedOnly(codes)) {
                addFactionsFromCodes(contestants, codes);
                break;
            }
        }
        for (int j = disputeIndex + 1; j < ownership.size(); j++) {
            List<String> codes = ownership.get(j).faction.getValue();
            if (!isDisputedOnly(codes)) {
                addFactionsFromCodes(contestants, codes);
                break;
            }
        }
        if (contestants.isEmpty()) {
            return;
        }
        double share = years / contestants.size();
        for (Faction faction : contestants) {
            tenureYears.merge(faction, share, Double::sum);
        }
    }

    /**
     * @return {@code true} if {@code codes} is non-empty and every code is the Disputed marker, i.e. the world is
     *       actively contested with no real owner recorded at the queried date
     */
    private static boolean isDisputedOnly(List<String> codes) {
        if (codes == null || codes.isEmpty()) {
            return false;
        }
        for (String code : codes) {
            if (!DISPUTED_FACTION_CODE.equals(code)) {
                return false;
            }
        }
        return true;
    }

    /** Resolves each faction code to a {@link Faction} and adds the non-null, non-disputed results to {@code sink}. */
    private static void addFactionsFromCodes(Set<Faction> sink, List<String> codes) {
        for (String code : codes) {
            if (DISPUTED_FACTION_CODE.equals(code)) {
                continue;
            }
            Faction faction = Factions.getInstance().getFaction(code);
            if (faction != null) {
                sink.add(faction);
            }
        }
    }

    public long getPopulation(LocalDate when) {
        long pop = 0L;
        for (Planet planet : planets.values()) {
            planet.getPopulation(when);
            pop += planet.getPopulation(when);
        }
        return pop;
    }

    /** highest socio-industrial ratings among all planets in-system for the map **/
    public SocioIndustrialData getSocioIndustrial(LocalDate when) {
        PlanetarySophistication tech = PlanetarySophistication.REGRESSED;
        PlanetaryRating industry = PlanetaryRating.F;
        PlanetaryRating rawMaterials = PlanetaryRating.F;
        PlanetaryRating output = PlanetaryRating.F;
        PlanetaryRating agriculture = PlanetaryRating.F;

        for (Planet planet : planets.values()) {
            SocioIndustrialData sic = planet.getSocioIndustrial(when);
            if (null != sic) {
                if (sic.tech.getIndex() < tech.getIndex()) {
                    tech = sic.tech;
                }
                if (sic.industry.getIndex() < industry.getIndex()) {
                    industry = sic.industry;
                }
                if (sic.rawMaterials.getIndex() < rawMaterials.getIndex()) {
                    rawMaterials = sic.rawMaterials;
                }
                if (sic.output.getIndex() < output.getIndex()) {
                    output = sic.output;
                }
                if (sic.agriculture.getIndex() < agriculture.getIndex()) {
                    agriculture = sic.agriculture;
                }
            }
        }
        return new SocioIndustrialData(tech, industry, rawMaterials, output, agriculture);
    }

    /**
     * @param when the date to check
     *
     * @return the most significant administrative capital status among the planets of this system (national outranks
     *       regional outranks district), or {@link CapitalType#NONE} if no planet is a capital
     */
    public CapitalType getCapitalType(LocalDate when) {
        CapitalType mostSignificant = CapitalType.NONE;
        for (Planet planet : planets.values()) {
            CapitalType planetCapitalType = planet.getCapitalType(when);
            if (planetCapitalType.significance() > mostSignificant.significance()) {
                mostSignificant = planetCapitalType;
            }
        }
        return mostSignificant;
    }

    /** @return the highest HPG rating among planets **/
    public HPGRating getHPG(LocalDate when) {
        HPGRating rating = HPGRating.X;
        for (Planet planet : planets.values()) {
            if ((null != planet.getHPG(when)) && (planet.getHPG(when).compareTo(rating) > 0)) {
                rating = planet.getHPG(when);
            }
        }
        return rating;
    }

    /** @return the highest Hiring Hall rating among planets **/
    public HiringHallLevel getHiringHallLevel(LocalDate when) {
        HiringHallLevel level = HiringHallLevel.NONE;
        for (Planet planet : planets.values()) {
            if ((null != planet.getHiringHallLevel(when)) && (planet.getHiringHallLevel(when).compareTo(level) > 0)) {
                level = planet.getHiringHallLevel(when);
            }
        }
        return level;
    }

    /** @return true if a hiring hall is present in the system **/
    public boolean isHiringHall(LocalDate when) {
        return !getHiringHallLevel(when).isNone();
    }

    /**
     * @return short name if set, else full name, else "unnamed"
     */
    /**
     * @param when the date to name the system as of, since ownership and name can change over time
     *
     * @return the system's name as a report hyperlink that focuses the interstellar map on it
     */
    public String getHyperlinkedName(LocalDate when) {
        return String.format("<a href='SYSTEM:%s'>%s</a>", getId(), getName(when));
    }

    public String getPrintableName(LocalDate when) {
        final String system = getName(when);
        return (system == null) ? "Unknown System" : system;
    }

    /**
     * @return the distance to a point in space in light years
     */
    public double getDistanceTo(double x, double y) {
        return Math.sqrt(Math.pow(x - this.x, 2) + Math.pow(y - this.y, 2));
    }

    /**
     * @return the distance to another system in light years (0 if both are in the same system)
     */
    public double getDistanceTo(PlanetarySystem anotherSystem) {
        return Math.sqrt(Math.pow(x - anotherSystem.x, 2) + Math.pow(y - anotherSystem.y, 2));
    }

    public Boolean isNadirCharge(LocalDate when) {
        return (null != getSourcedNadirCharge(when) && getSourcedNadirCharge(when).getValue());
    }

    public SourceableValue<Boolean> getSourcedNadirCharge(LocalDate when) {
        return getEventData(when, null, e -> e.nadirCharge);
    }

    public boolean isZenithCharge(LocalDate when) {
        return (null != getSourcedZenithCharge(when) && getSourcedZenithCharge(when).getValue());
    }

    public SourceableValue<Boolean> getSourcedZenithCharge(LocalDate when) {
        return getEventData(when, null, e -> e.zenithCharge);
    }

    public int getNumberRechargeStations(LocalDate when) {
        return (isNadirCharge(when) ? 1 : 0) + (isZenithCharge(when) ? 1 : 0);
    }

    public String getRechargeStationsText(LocalDate when) {
        boolean nadir = isNadirCharge(when);
        boolean zenith = isZenithCharge(when);
        if (nadir && zenith) {
            return "Zenith, Nadir";
        } else if (zenith) {
            return "Zenith";
        } else if (nadir) {
            return "Nadir";
        } else {
            return "None";
        }
    }

    /**
     * Use {@link #getRechargeTime(LocalDate, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public double getRechargeTime(LocalDate when) {
        return getRechargeTime(when, false);
    }

    /**
     * Calculates the recharge time for a jump ship in hours, based on a given date and whether command circuits are
     * used.
     *
     * <p>When recharging at a zenith or nadir jump point, the method returns the minimum of either the command
     * circuit recharge time (if command circuits are enabled) or 176 hours, and the standard solar recharge time. For
     * all other locations, only the solar recharge time is considered, unless using command circuits, in which case
     * their recharge time is also taken into account.</p>
     *
     * @param when                 the date for which the recharge time should be determined
     * @param isUseCommandCircuits {@code true} if command circuits are being used, possibly reducing recharge time at
     *                             specific locations
     *
     * @return the calculated recharge time in hours
     */
    public double getRechargeTime(LocalDate when, boolean isUseCommandCircuits) {
        if (isZenithCharge(when) || isNadirCharge(when)) {
            // The 176 value comes from pg. 87-88 and 138 of StratOps
            return Math.min(isUseCommandCircuits ? COMMAND_CIRCUIT_RECHARGE_TIME_HOURS : 176.0, getSolarRechargeTime());
        } else {
            return Math.min(isUseCommandCircuits ? COMMAND_CIRCUIT_RECHARGE_TIME_HOURS : Double.MAX_VALUE,
                  getSolarRechargeTime());
        }
    }

    public double getSolarRechargeTime() {
        return getStar().getSolarRechargeTime();
    }

    /**
     * Use {@link #getRechargeTimeText(LocalDate, boolean)} instead
     */
    @Deprecated(since = "0.50.07", forRemoval = true)
    public String getRechargeTimeText(LocalDate when) {
        return getRechargeTimeText(when, false);
    }

    /**
     * Returns a human-readable description of the recharge time for a jump ship based on the specified date and whether
     * command circuits are used.
     *
     * <p>If the recharge is not possible (i.e., the computed recharge time is infinite), returns a message
     * indicating impossibility; otherwise, returns the recharge time formatted as hours.</p>
     *
     * @param when                the date to evaluate recharge conditions
     * @param isUseCommandCircuit {@code true} if command circuits are in use, which may affect recharge time
     *
     * @return a string describing the recharge time or indicating if recharging is impossible
     */
    public String getRechargeTimeText(LocalDate when, boolean isUseCommandCircuit) {
        double time = getRechargeTime(when, isUseCommandCircuit);
        if (Double.isInfinite(time)) {
            return "recharging impossible";
        } else {
            return String.format("%.0f hours", time);
        }
    }

    public double getStarDistanceToJumpPoint() {
        if (null == star) {
            // 40 is close to the midpoint value across all star types
            return StarUtil.getDistanceToJumpPoint(40);
        }
        return getStar().getDistanceToJumpPoint();
    }

    /**
     * @return the average travel time from low orbit to the jump point at 1g, in Terran days for a given planetary
     *       position
     */
    public double getTimeToJumpPoint(double acceleration) {
        return getTimeToJumpPoint(acceleration, getPrimaryPlanetPosition());
    }

    /**
     * @return the average travel time from low orbit to the jump point at 1g, in Terran days for a given planetary
     *       position
     */
    public double getTimeToJumpPoint(double acceleration, int sysPos) {
        return planets.get(sysPos).getTimeToJumpPoint(acceleration);
    }

    public StarType getStar() {
        SourceableValue<StarType> sourcedStar = getSourcedStar();
        return sourcedStar == null ? null : sourcedStar.getValue();
    }

    public SourceableValue<StarType> getSourcedStar() {
        return star;
    }

    public SourceableValue<Integer> getSourcedPrimarySlot() {return primarySlot;}

    /**
     * @return the planet object identified by the primary slot. If no primary slot is given, then this function will
     *       return the first planet
     */
    public Planet getPrimaryPlanet() {
        return planets.get(getPrimaryPlanetPosition());
    }

    public int getPrimaryPlanetPosition() {
        if (null == getSourcedPrimarySlot()) {
            // if no primary slot (i.e., an uninhabited system) then return the first planet
            return 1;
        }
        return getSourcedPrimarySlot().getValue();
    }

    public Planet getPlanet(int pos) {
        return planets.get(pos);
    }

    public Planet getPlanetById(String id) {
        for (Planet p : planets.values()) {
            if (p.getId().equals(id)) {
                return p;
            }
        }
        return null;
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public Set<Integer> getPlanetPositions() {
        return planets.keySet();
    }

    public Collection<Planet> getPlanets() {
        return planets.values();
    }

    public String getIcon() {
        return getStar().getIcon();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if ((null == object) || (getClass() != object.getClass())) {
            return false;
        }
        final PlanetarySystem other = (PlanetarySystem) object;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Deprecated(since = "0.51.0", forRemoval = true)
    public PlanetarySystemEvent getEvent(LocalDate when) {
        if ((null == when) || (null == events)) {
            return null;
        }
        return events.get(when);
    }

    protected <T> T getEventData(LocalDate when, T defaultValue, EventGetter<T> getter) {
        if ((null == when) || (null == events) || (null == getter)) {
            return defaultValue;
        }
        T result = defaultValue;
        for (LocalDate date : events.navigableKeySet()) {
            if (date.isAfter(when)) {
                break;
            }
            result = ObjectUtility.nonNull(getter.get(events.get(date)), result);
        }
        return result;
    }

    public List<PlanetarySystemEvent> getEvents() {
        if (null == events) {
            return null;
        }
        return new ArrayList<>(events.values());
    }

    @JsonGetter("planet")
    private List<Planet> getPlanetListForSerialization() {
        return ((planets == null) || planets.isEmpty()) ? null : new ArrayList<>(planets.values());
    }

    @JsonSetter("planet")
    private void setPlanetList(List<Planet> planetList) {
        this.planetList = planetList;
    }

    @JsonGetter("event")
    private List<PlanetarySystemEvent> getEventListForSerialization() {
        return ((events == null) || events.isEmpty()) ? null : new ArrayList<>(events.values());
    }

    @JsonSetter("event")
    private void setEventList(List<PlanetarySystemEvent> eventList) {
        this.eventList = eventList;
    }

    /**
     * Insert or replace a system-level event keyed by date. GM-only planetary editor; gameplay code should not call
     * this.
     *
     * @param event the event to insert; must have a non-null date
     * @throws IllegalArgumentException if the event or its date is null
     */
    public void putEvent(PlanetarySystemEvent event) {
        if ((event == null) || (event.date == null)) {
            throw new IllegalArgumentException("Planetary system events must have a date");
        }
        if (events == null) {
            events = new TreeMap<>();
        }
        events.put(event.date, event);
    }

    /**
     * Remove the system-level event for the given date if present. GM-only planetary editor; gameplay code should not
     * call this.
     *
     * @param when the date of the event to remove
     * @return {@code true} if an event was removed
     */
    public boolean removeEvent(LocalDate when) {
        if ((when == null) || (events == null)) {
            return false;
        }
        return events.remove(when) != null;
    }

    /**
     * Replace the system-level event timeline with the provided collection. GM-only planetary editor; gameplay code
     * should not call this.
     *
     * @param updatedEvents events to install (may be null or empty to clear)
     */
    public void replaceEvents(Collection<PlanetarySystemEvent> updatedEvents) {
        events = new TreeMap<>();
        if (updatedEvents != null) {
            for (PlanetarySystemEvent event : updatedEvents) {
                if ((event != null) && (event.date != null)) {
                    events.put(event.date, event);
                }
            }
        }
        if (events.isEmpty()) {
            events = null;
        }
    }

    protected interface EventGetter<T> {
        T get(PlanetarySystemEvent e);
    }

    /** A class representing some event, possibly changing planetary information */
    public static final class PlanetarySystemEvent {

        @JsonProperty("date")
        public LocalDate date;
        @JsonProperty("nadirCharge")
        public SourceableValue<Boolean> nadirCharge;
        @JsonProperty("zenithCharge")
        public SourceableValue<Boolean> zenithCharge;
        // Events marked as "custom" are saved to scenario files and loaded from there
        public transient boolean custom = false;

        /**
         * @return <code>true</code> if the event doesn't contain any change
         */
        public boolean isEmpty() {
            return (null == nadirCharge) && (null == zenithCharge);
        }
    }

    /**
     * Retrieves a list of filtered academies based on the given campaign.
     *
     * @param campaign The campaign for filtering the academies.
     *
     * @return A list of filtered academies based on the campaign.
     */
    public List<Academy> getFilteredAcademies(Campaign campaign) {
        final LocalDate currentDate = campaign.getLocalDate();
        AcademyFactory academyFactory = AcademyFactory.getInstance();

        List<String> excludedSets = List.of("Local Academies", "Unit Education");

        return academyFactory.getAllSetNames().stream()
                     .filter(setName -> !excludedSets.contains(setName) // Excluding certain setNames
                                              && (!setName.equalsIgnoreCase("Prestigious Academies")
                                                        ||
                                                        campaign.getCampaignOptions()
                                                              .get(CampaignOption.ENABLE_PRESTIGIOUS_ACADEMIES))) // Additional
                     // condition for
                     // "Prestigious
                     // Academies"
                     .flatMap(setName -> getFilteredAcademiesForSet(currentDate, setName).stream())
                     .toList();
    }

    /**
     * Retrieves a list of filtered academies for a given set and current date.
     *
     * @param currentDate The current date to filter the academies.
     * @param setName     The set name to filter the academies.
     *
     * @return A list of filtered academies for the given set and current date.
     */
    private List<Academy> getFilteredAcademiesForSet(LocalDate currentDate, String setName) {
        return AcademyFactory.getInstance().getAllAcademiesForSet(setName).stream()
                     .filter(academy -> academy.getLocationSystems().contains(this.getId())
                                              && !academy.isLocal()
                                              && !academy.isHomeSchool()
                                              && !academy.getName().contains("(Officer)")
                                              && currentDate.getYear() >= academy.getConstructionYear()
                                              && currentDate.getYear() < academy.getClosureYear()
                                              && currentDate.getYear() < academy.getDestructionYear())
                     .sorted()
                     .toList();
    }

    /**
     * Retrieves a string representation of the prestigious academies available in the system.
     *
     * @return A string representation of the prestigious academies in the system.
     */
    public String getAcademiesForSystem(List<Academy> filteredAcademies) {
        StringBuilder academyString = new StringBuilder();

        for (Academy academy : filteredAcademies) { // there are not enough entries to justify a Stream
            academyString.append("<b>").append(academy.getName()).append("</b><br>")
                  .append(academy.getDescription()).append("<br><br>");
        }

        return academyString.toString();
    }

    /** This class allows for some additional code on a planetary system after it is loaded by Jackson **/
    public static class PlanetarySystemPostLoader extends StdConverter<PlanetarySystem, PlanetarySystem> {

        @Override
        public PlanetarySystem convert(PlanetarySystem planetarySystem) {
            if (null == planetarySystem.id) {
                planetarySystem.id = planetarySystem.name;
            }

            // fill up planets
            planetarySystem.planets = new TreeMap<>();
            if (null != planetarySystem.planetList) {
                for (Planet planet : planetarySystem.planetList) {
                    planet.setParentSystem(planetarySystem);
                    if (!planetarySystem.planets.containsKey(planet.getSystemPosition())) {
                        planetarySystem.planets.put(planet.getSystemPosition(), planet);
                    }
                }
                planetarySystem.planetList.clear();
            }
            planetarySystem.planetList = null;
            // Fill up events
            planetarySystem.events = new TreeMap<>();
            if (null != planetarySystem.eventList) {
                for (PlanetarySystemEvent systemEvent : planetarySystem.eventList) {
                    if ((null != systemEvent) && (null != systemEvent.date)) {
                        planetarySystem.events.put(systemEvent.date, systemEvent);
                    }
                }
                planetarySystem.eventList.clear();
            }
            planetarySystem.eventList = null;

            return planetarySystem;
        }
    }
}
