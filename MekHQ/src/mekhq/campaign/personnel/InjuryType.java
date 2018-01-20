/*
 * Copyright (C) 2016 MegaMek team
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.joda.time.DateTime;

import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;

/**
 * Flyweight design pattern implementation. InjuryType instances should be singletons and never
 * hold any data related to specific injuries. Use the {@link Injury} data for that, in particular
 * it's <tt>extraData</tt> data structure for generic type-safe data storage.
 */
@XmlJavaTypeAdapter(InjuryType.XMLAdapter.class)
public class InjuryType {
    /** Modifier tag to use for injuries */
    public static final String MODTAG_INJURY = "injury";

    // Registry methods
    private static final Map<String, InjuryType> REGISTRY = new HashMap<>();
    private static final Map<InjuryType, String> REV_REGISTRY = new HashMap<>();
    private static final Map<Integer, InjuryType> ID_REGISTRY = new HashMap<>();
    private static final Map<InjuryType, Integer> REV_ID_REGISTRY = new HashMap<>();

    public static InjuryType byKey(String key) {
        InjuryType result = REGISTRY.get(key);
        if(null == result) {
            try {
                result = ID_REGISTRY.get(Integer.valueOf(key));
            } catch(NumberFormatException nfex) {
                // Do nothing
            }
        }
        return result;
    }

    public static InjuryType byId(int id) {
        return ID_REGISTRY.get(Integer.valueOf(id));
    }

    public static void register(int id, String key, InjuryType injType) {
        Objects.requireNonNull(injType);
        if(id >= 0) {
            if(ID_REGISTRY.containsKey(Integer.valueOf(id))) {
                throw new IllegalArgumentException("Injury type ID " + id + " is already registered.");
            }
        }
        if(REGISTRY.containsKey(Objects.requireNonNull(key))) {
            throw new IllegalArgumentException("Injury type key \"" + key + "\" is already registered.");
        }
        if(key.isEmpty()) {
            throw new IllegalArgumentException("Injury type key can't be an empty string.");
        }
        if(REV_REGISTRY.containsKey(injType)) {
            throw new IllegalArgumentException("Injury type " + injType + " is already registered");
        }
        // All checks done
        if(id >= 0) {
            ID_REGISTRY.put(Integer.valueOf(id), injType);
            REV_ID_REGISTRY.put(injType, Integer.valueOf(id));
        }
        REGISTRY.put(key, injType);
        REV_REGISTRY.put(injType, key);
    }

    public static void register(String key, InjuryType injType) {
        register(-1, key, injType);
    }

    public static List<String> getAllKeys() {
        List<String> result = new ArrayList<>(REGISTRY.keySet());
        Collections.sort(result);
        return result;
    }

    public static List<InjuryType> getAllTypes() {
        List<InjuryType> result = new ArrayList<>(REGISTRY.values());
        Collections.sort(result, (it1, it2) -> it1.getKey().compareTo(it2.getKey()));
        return result;
    }

    /** Default injury type: reduction in hit points */
    public static InjuryType BAD_HEALTH = new InjuryType();
    static {
        BAD_HEALTH.recoveryTime = 7;
        BAD_HEALTH.fluffText = "Damaged health";
        BAD_HEALTH.maxSeverity = 5;
        BAD_HEALTH.allowedLocations = EnumSet.of(BodyLocation.GENERIC);
        register("bad_health", BAD_HEALTH);
    }

    /** Base recovery time in days */
    protected int recoveryTime = 0;
    protected boolean permanent = false;
    protected int maxSeverity = 1;
    protected String fluffText = "";
    protected String simpleName = "injured";
    protected InjuryLevel level = InjuryLevel.MINOR;
    protected Set<BodyLocation> allowedLocations = null;

    protected InjuryType() {
    }

    public final int getId() {
        return Utilities.nonNull(InjuryType.REV_ID_REGISTRY.get(this), Integer.valueOf(-1)).intValue();
    }

    public final String getKey() {
        return InjuryType.REV_REGISTRY.get(this);
    }

    public boolean isValidInLocation(BodyLocation loc) {
        if(null == allowedLocations) {
            allowedLocations = EnumSet.allOf(BodyLocation.class);
        }
        return allowedLocations.contains(loc);
    }

    /** Does having this injury mean the location is missing? (Amputation, genetic defect, ...) */
    public boolean impliesMissingLocation(BodyLocation loc) {
        return false;
    }

    /** Does having this injury in this location imply the character is dead? */
    public boolean impliesDead(BodyLocation loc) {
        return false;
    }

    public int getBaseRecoveryTime() {
        return recoveryTime;
    }

    public int getRecoveryTime(int severity) {
        return recoveryTime;
    }

    public int getRecoveryTime(Injury i) {
        return getRecoveryTime(i.getHits());
    }

    public boolean isPermanent() {
        return permanent;
    }

    public int getMaxSeverity() {
        return maxSeverity;
    }

    public boolean isHidden(Injury i) {
        return false;
    }

    public String getSimpleName() {
        return getSimpleName(1);
    }

    public String getSimpleName(int severity) {
        return simpleName;
    }

    public String getName(BodyLocation loc, int severity) {
        return Utilities.capitalize(fluffText);
    }

    public String getFluffText(BodyLocation loc, int severity, int gender) {
        return fluffText;
    }

    public InjuryLevel getLevel(Injury i) {
        return level;
    }

    public Injury newInjury(Campaign c, Person p, BodyLocation loc, int severity) {
        if(!isValidInLocation(loc)) {
            return null;
        }
        final int recoveryTime = getRecoveryTime(severity);
        final String fluff = getFluffText(loc, severity, p.getGender());
        Injury result = new Injury(recoveryTime, fluff, loc, this, severity, false);
        result.setVersion(Injury.VERSION);
        result.setStart(new DateTime(c.getCalendar()));
        return result;
    }

    public Collection<Modifier> getModifiers(Injury inj) {
        return Arrays.asList();
    }

    /**
     * Return a function which will generate a list of effects combat and similar stressful
     * situation while injured would have on the person in question given the random integer source.
     * Descriptions should be something like "50% chance of losing a leg" and similar.
     * <p>
     * Note that specific systems aren't required to use this generator. They are free to
     * implement their own.
     */
    public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
        return Arrays.asList();
    }

    // Standard actions generators

    protected GameEffect newResetRecoveryTimeAction(Injury i) {
        return new GameEffect(
            i.getFluff() + ": recovery timer reset",
            rnd -> i.setTime(i.getOriginalTime()));
    }

    // Helper classes and interfaces

    /** Why you no have this in java.util.function?!? */
    @FunctionalInterface
    public static interface ToBooleanFunction<T> {
        boolean applyAsBoolean(T value);
    }

    public static final class XMLAdapter extends XmlAdapter<String, InjuryType> {
        @Override
        public InjuryType unmarshal(String v) throws Exception {
            return (null == v) ? null : InjuryType.byKey(v);
        }

        @Override
        public String marshal(InjuryType v) throws Exception {
            return (null == v) ? null : v.getKey();
        }

    }
}
