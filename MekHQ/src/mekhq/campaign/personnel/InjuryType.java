/*
 * Copyright (C) 2016-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel;

import static mekhq.campaign.personnel.PersonnelOptions.MUTATION_EXCEPTIONAL_IMMUNE_SYSTEM;

import java.util.*;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import megamek.codeUtilities.ObjectUtility;
import megamek.common.enums.Gender;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.GameEffect;
import mekhq.campaign.personnel.InjuryType.XMLAdapter;
import mekhq.campaign.personnel.enums.InjuryLevel;

/**
 * Flyweight design pattern implementation. InjuryType instances should be singletons and never hold any data related to
 * specific injuries. Use the {@link Injury} data for that, in particular it's <code>extraData</code> data structure for
 * generic type-safe data storage.
 */
@XmlJavaTypeAdapter(value = XMLAdapter.class)
public class InjuryType {
    /** Modifier tag to use for injuries */
    public static final String MOD_TAG_INJURY = "injury";

    // Registry methods
    private static final Map<String, InjuryType> REGISTRY = new HashMap<>();
    private static final Map<InjuryType, String> REV_REGISTRY = new HashMap<>();
    private static final Map<Integer, InjuryType> ID_REGISTRY = new HashMap<>();
    private static final Map<InjuryType, Integer> REV_ID_REGISTRY = new HashMap<>();

    public static InjuryType byKey(String key) {
        InjuryType result = REGISTRY.get(key);
        if (null == result) {
            try {
                result = ID_REGISTRY.get(Integer.valueOf(key));
            } catch (NumberFormatException ignored) {

            }
        }
        return result;
    }

    public static InjuryType byId(int id) {
        return ID_REGISTRY.get(id);
    }

    public static void register(int id, String key, InjuryType injType) {
        Objects.requireNonNull(injType);
        if (id >= 0) {
            if (ID_REGISTRY.containsKey(id)) {
                throw new IllegalArgumentException("Injury type ID " + id + " is already registered.");
            }
        }
        if (REGISTRY.containsKey(Objects.requireNonNull(key))) {
            throw new IllegalArgumentException("Injury type key \"" + key + "\" is already registered.");
        }
        if (key.isEmpty()) {
            throw new IllegalArgumentException("Injury type key can't be an empty string.");
        }
        if (REV_REGISTRY.containsKey(injType)) {
            throw new IllegalArgumentException("Injury type " + injType.getSimpleName() + " is already registered");
        }
        // All checks done
        if (id >= 0) {
            ID_REGISTRY.put(id, injType);
            REV_ID_REGISTRY.put(injType, id);
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
        result.sort(Comparator.comparing(InjuryType::getKey));
        return result;
    }

    /** Default injury type: reduction in hit points */
    public static InjuryType BAD_HEALTH = new InjuryType();

    static {
        BAD_HEALTH.recoveryTime = 7;
        BAD_HEALTH.fluffText = "Damaged health";
        BAD_HEALTH.maxSeverity = 5;
        BAD_HEALTH.allowedLocations = EnumSet.of(mekhq.campaign.personnel.medical.BodyLocation.GENERIC);
        register("bad_health", BAD_HEALTH);
    }

    /** Base recovery time in days */
    protected int recoveryTime = 0;
    protected boolean permanent = false;
    protected int maxSeverity = 1;
    protected String fluffText = "";
    protected String simpleName = "injured";
    protected InjuryLevel level = InjuryLevel.MINOR;
    protected Set<mekhq.campaign.personnel.medical.BodyLocation> allowedLocations = null;

    protected InjuryType() {

    }

    public final int getId() {
        return ObjectUtility.nonNull(InjuryType.REV_ID_REGISTRY.get(this), -1);
    }

    public final String getKey() {
        return InjuryType.REV_REGISTRY.get(this);
    }

    public boolean isValidInLocation(mekhq.campaign.personnel.medical.BodyLocation loc) {
        if (null == allowedLocations) {
            allowedLocations = EnumSet.allOf(mekhq.campaign.personnel.medical.BodyLocation.class);
        }
        return allowedLocations.contains(loc);
    }

    /** Does having this injury mean the location is missing? (Amputation, genetic defect, ...) */
    public boolean impliesMissingLocation() {
        return false;
    }

    /** Does having this injury in this location imply the character is dead? */
    public boolean impliesDead(mekhq.campaign.personnel.medical.BodyLocation loc) {
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

    public String getName(mekhq.campaign.personnel.medical.BodyLocation loc, int severity) {
        return Utilities.capitalize(fluffText);
    }

    public String getFluffText(mekhq.campaign.personnel.medical.BodyLocation loc, int severity, Gender gender) {
        return fluffText;
    }

    public InjuryLevel getLevel(Injury i) {
        return level;
    }

    public Injury newInjury(Campaign campaign, Person person,
          mekhq.campaign.personnel.medical.BodyLocation bodyLocation, int severity) {
        if (!isValidInLocation(bodyLocation)) {
            return null;
        }

        int recoveryTime = getRecoveryTime(severity);
        if (person.getOptions().booleanOption(MUTATION_EXCEPTIONAL_IMMUNE_SYSTEM)) {
            recoveryTime = recoveryTime / 2;
        }

        final String fluff = getFluffText(bodyLocation, severity, person.getGender());
        Injury result = new Injury(recoveryTime, fluff, bodyLocation, this, severity, campaign.getLocalDate(), false);
        result.setVersion(Injury.VERSION);
        return result;
    }

    public Collection<Modifier> getModifiers(Injury inj) {
        return Collections.emptyList();
    }

    /**
     * Return a function which will generate a list of effects combat and similar stressful situation while injured
     * would have on the person in question given the random integer source. Descriptions should be something like "50%
     * chance of losing a leg" and similar.
     * <p>
     * Note that specific systems aren't required to use this generator. They are free to implement their own.
     */
    public List<GameEffect> genStressEffect(Campaign c, Person p, Injury i, int hits) {
        return Collections.emptyList();
    }

    // Standard actions generators

    protected GameEffect newResetRecoveryTimeAction(Injury i) {
        return new GameEffect(i.getFluff() + ": recovery timer reset", rnd -> i.setTime(i.getOriginalTime()));
    }

    // Helper classes and interfaces
    public static final class XMLAdapter extends XmlAdapter<String, InjuryType> {
        @Override
        public InjuryType unmarshal(String v) {
            return (null == v) ? null : InjuryType.byKey(v);
        }

        @Override
        public String marshal(InjuryType v) {
            return (null == v) ? null : v.getKey();
        }

    }
}
