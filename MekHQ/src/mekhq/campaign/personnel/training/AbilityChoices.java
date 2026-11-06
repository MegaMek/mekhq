/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.training;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import jakarta.annotation.Nullable;
import megamek.common.equipment.WeaponMounted;
import megamek.common.equipment.WeaponType;
import megamek.common.options.OptionsConstants;
import megamek.common.units.Crew;
import megamek.common.units.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.unit.Unit;

/**
 * The values an ability that needs a choice can take, such as Weapon Specialist's weapon or Range Master's range
 * bracket, and the value that suits each person best when the player lets the console choose.
 *
 * <p>Only the weapon abilities choose for a person: from the weapons on the unit they serve in. Range brackets,
 * environments and unit types have no sensible default, so the player always picks those.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class AbilityChoices {
    private AbilityChoices() {
    }

    /**
     * @param ability the ability
     *
     * @return {@code true} if gaining the ability needs a chosen value
     */
    public static boolean needsChoice(SpecialAbility ability) {
        String name = ability.getName();
        if (isWeaponChoice(name) || name.equals(OptionsConstants.GUNNERY_SPECIALIST)
                  || name.equals(OptionsConstants.GUNNERY_RANGE_MASTER)
                  || name.equals(OptionsConstants.MISC_ENV_SPECIALIST)
                  || name.equals(OptionsConstants.MISC_HUMAN_TRO)) {
            return true;
        }
        return !genericChoices(ability).isEmpty();
    }

    /**
     * @param ability the ability
     *
     * @return {@code true} if the console can choose the value for each person, from their unit
     */
    public static boolean canChooseForEachPerson(SpecialAbility ability) {
        String name = ability.getName();
        return isWeaponChoice(name) || name.equals(OptionsConstants.GUNNERY_SPECIALIST);
    }

    /**
     * The values a player can choose for everyone at once. Weapon abilities list every weapon on the units the given
     * people serve in.
     *
     * @param ability the ability
     * @param people  the people the choice is for
     *
     * @return the values, in display order
     */
    public static List<String> sharedChoices(SpecialAbility ability, Iterable<Person> people) {
        String name = ability.getName();
        if (isWeaponChoice(name)) {
            TreeSet<String> weapons = new TreeSet<>();
            for (Person person : people) {
                weapons.addAll(weaponsOf(person, name.equals(OptionsConstants.GUNNERY_SANDBLASTER)).keySet());
            }
            return new ArrayList<>(weapons);
        }
        return switch (name) {
            case OptionsConstants.GUNNERY_SPECIALIST ->
                  List.of(Crew.SPECIAL_ENERGY, Crew.SPECIAL_BALLISTIC, Crew.SPECIAL_MISSILE);
            case OptionsConstants.GUNNERY_RANGE_MASTER ->
                  List.of(Crew.RANGEMASTER_MEDIUM, Crew.RANGEMASTER_LONG, Crew.RANGEMASTER_EXTREME);
            case OptionsConstants.MISC_ENV_SPECIALIST -> List.of(Crew.ENVIRONMENT_SPECIALIST_FOG,
                  Crew.ENVIRONMENT_SPECIALIST_LIGHT, Crew.ENVIRONMENT_SPECIALIST_RAIN,
                  Crew.ENVIRONMENT_SPECIALIST_SNOW, Crew.ENVIRONMENT_SPECIALIST_WIND);
            case OptionsConstants.MISC_HUMAN_TRO ->
                  List.of(Crew.HUMAN_TRO_MEK, Crew.HUMAN_TRO_AERO, Crew.HUMAN_TRO_VEE, Crew.HUMAN_TRO_BA);
            default -> genericChoices(ability);
        };
    }

    /**
     * Chooses the value that suits a person best: for Weapon Specialist and Sandblaster, the weapon their unit mounts
     * most of; for Specialist, the weapon class most of their unit's weapons belong to.
     *
     * @param ability the ability
     * @param person  the person
     *
     * @return the value, or {@code null} if nothing suits them, such as Sandblaster for someone whose unit has no
     *       cluster weapon
     */
    public static @Nullable String choiceFor(SpecialAbility ability, Person person) {
        if (!canChooseForEachPerson(ability)) {
            return null;
        }
        String name = ability.getName();
        Map<String, Integer> counts = name.equals(OptionsConstants.GUNNERY_SPECIALIST) ?
                                            weaponClassesOf(person) :
                                            weaponsOf(person, name.equals(OptionsConstants.GUNNERY_SANDBLASTER));
        String most = null;
        int mostCount = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > mostCount) {
                mostCount = entry.getValue();
                most = entry.getKey();
            }
        }
        return most;
    }

    /**
     * @param ability the ability
     * @param person  the person
     * @param choice  a value
     *
     * @return {@code true} if the value can be given to this person: any listed value, except that a weapon must be
     *       on their unit
     */
    public static boolean isValidFor(SpecialAbility ability, Person person, String choice) {
        String name = ability.getName();
        if (isWeaponChoice(name)) {
            return weaponsOf(person, name.equals(OptionsConstants.GUNNERY_SANDBLASTER)).containsKey(choice);
        }
        return sharedChoices(ability, List.of(person)).contains(choice);
    }

    private static boolean isWeaponChoice(String name) {
        return name.equals(OptionsConstants.GUNNERY_WEAPON_SPECIALIST)
                     || name.equals(OptionsConstants.GUNNERY_SANDBLASTER);
    }

    private static List<String> genericChoices(SpecialAbility ability) {
        List<String> choices = new ArrayList<>();
        for (String choice : ability.getChoiceValues()) {
            if (!choice.equalsIgnoreCase("none")) {
                choices.add(choice);
            }
        }
        return choices;
    }

    /** @return each weapon on the person's unit, by name, with how many are mounted */
    private static Map<String, Integer> weaponsOf(Person person, boolean isClusterOnly) {
        Map<String, Integer> weapons = new HashMap<>();
        Unit unit = person.getUnit();
        Entity entity = (unit == null) ? null : unit.getEntity();
        if (entity == null) {
            return weapons;
        }
        for (WeaponMounted weapon : entity.getWeaponList()) {
            WeaponType type = weapon.getType();
            if (type == null) {
                continue;
            }
            if (isClusterOnly && !SpecialAbility.isWeaponEligibleForSPA(type, person.getPrimaryRole(), true)) {
                continue;
            }
            weapons.merge(weapon.getName(), 1, Integer::sum);
        }
        return weapons;
    }

    /** @return each weapon class on the person's unit, with how many of its weapons belong to it */
    private static Map<String, Integer> weaponClassesOf(Person person) {
        Map<String, Integer> classes = new HashMap<>();
        Unit unit = person.getUnit();
        Entity entity = (unit == null) ? null : unit.getEntity();
        if (entity == null) {
            return classes;
        }
        for (WeaponMounted weapon : entity.getWeaponList()) {
            WeaponType type = weapon.getType();
            if (type == null) {
                continue;
            }
            if (type.hasFlag(WeaponType.F_ENERGY)) {
                classes.merge(Crew.SPECIAL_ENERGY, 1, Integer::sum);
            } else if (type.hasFlag(WeaponType.F_MISSILE)) {
                classes.merge(Crew.SPECIAL_MISSILE, 1, Integer::sum);
            } else if (type.hasFlag(WeaponType.F_BALLISTIC)) {
                classes.merge(Crew.SPECIAL_BALLISTIC, 1, Integer::sum);
            }
        }
        return classes;
    }
}
