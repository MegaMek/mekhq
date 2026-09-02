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
package mekhq.campaign.personnel.quartermaster;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import megamek.common.annotations.Nullable;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.CrewArmorKitRules;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Sorts the armor kits MegaMek defines into the three groups MekHQ issues from, and answers which kits a crew may be
 * given this year.
 *
 * <p>MegaMek marks a kit only by its name and its survival flags, not by who is meant to wear it, so the grouping is
 * kept here as a name set: the MekWarrior kits go to MekWarriors, the aerospace pilot kit to aerospace crews, and
 * everything else — faction infantry kits, environment suits, the tanker's smock — is issued to everyone else.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ArmorKitCatalog {
    /**
     * The internal name of the kit a person wears when they have been issued nothing: civilian clothing that provides
     * no protection. This is the default every person starts in, what stripping a kit returns them to, and the one kit
     * the stores do not sell.
     */
    public static final String DEFAULT_ARMOR_KIT_NAME = "Clothing, Fatigues/Civilian/Non-Armored";

    // The armor kits the catalog reasons about, by their MegaMek internal name.
    public static final String KIT_MEKWARRIOR_BASIC = "MekWarrior Kit (Basic)";
    public static final String KIT_MEKWARRIOR_ADVANCED = "MekWarrior Kit (Advanced)";
    public static final String KIT_MEKWARRIOR_CLAN = "MekWarrior Kit (Clan)";
    public static final String KIT_AEROSPACE_PILOT = "Aerospace Fighter Pilot Kit";
    public static final String KIT_SNOWSUIT = "Snowsuit";
    public static final String KIT_HEAT_SUIT = "Heat Suit";
    public static final String KIT_TANKERS_SMOCK = "Tanker's Smock";
    public static final String KIT_ENVIRONMENT_SUIT_LIGHT = "Environment Suit, Light";
    public static final String KIT_FLAK_STANDARD = "Flak, Standard";

    private static final Set<String> MEKWARRIOR_KITS = Set.of(KIT_MEKWARRIOR_BASIC,
          KIT_MEKWARRIOR_ADVANCED,
          KIT_MEKWARRIOR_CLAN);

    private static final Set<String> AEROSPACE_KITS = Set.of(KIT_AEROSPACE_PILOT);

    private static final Set<String> VEHICLE_KITS = Set.of(KIT_SNOWSUIT,
          KIT_HEAT_SUIT,
          KIT_TANKERS_SMOCK,
          KIT_ENVIRONMENT_SUIT_LIGHT,
          KIT_FLAK_STANDARD);

    /** The group a kit belongs to, deciding which crews may be issued it. */
    public enum Category {
        MEKWARRIOR, AIRCRAFT, INFANTRY, SOLDIER
    }

    private ArmorKitCatalog() {
    }

    /**
     * The group a kit belongs to. Anything not named as a MekWarrior or aerospace kit is an infantry kit, the group
     * every other crew draws from.
     *
     * @param kitInternalName the internal name of the kit
     *
     * @return the kit's {@link Category}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Category categoryOf(@Nullable String kitInternalName) {
        if (MEKWARRIOR_KITS.contains(kitInternalName)) {
            return Category.MEKWARRIOR;
        }
        if (AEROSPACE_KITS.contains(kitInternalName)) {
            return Category.AIRCRAFT;
        }
        return Category.INFANTRY;
    }

    /**
     * Whether a kit is one of the three MekWarrior kits (Basic, Advanced, or Clan) — the kits a MekWarrior must wear to
     * meet the deployment requirement.
     *
     * @param kitInternalName the internal name of the kit, or {@code null}
     *
     * @return {@code true} if the kit is a MekWarrior kit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isMekWarriorKit(@Nullable String kitInternalName) {
        return MEKWARRIOR_KITS.contains(kitInternalName);
    }

    /** The fixed kit sets used by the campaign-options dropdowns. Soldiers have no dropdown, so an empty set. */
    private static Set<String> kitsFor(Category category) {
        return switch (category) {
            case MEKWARRIOR -> MEKWARRIOR_KITS;
            case AIRCRAFT -> AEROSPACE_KITS;
            case INFANTRY -> VEHICLE_KITS;
            case SOLDIER -> Set.of();
        };
    }

    /** Whether a kit belongs to a group's issuable list: fixed sets for the crews, every infantry kit for soldiers. */
    private static boolean isInCategory(String kitInternalName, Category category) {
        return switch (category) {
            case MEKWARRIOR -> MEKWARRIOR_KITS.contains(kitInternalName);
            case AIRCRAFT -> AEROSPACE_KITS.contains(kitInternalName);
            case INFANTRY -> VEHICLE_KITS.contains(kitInternalName);
            case SOLDIER -> !MEKWARRIOR_KITS.contains(kitInternalName) && !AEROSPACE_KITS.contains(kitInternalName);
        };
    }

    /**
     * The kit choices for a group's campaign-options default: coveralls (meaning "none") first, then that group's kits
     * by internal name, ungated by year. Used to populate the default-kit dropdowns.
     *
     * @param category the group whose kits are offered
     *
     * @return the kit internal names to choose from, coveralls first
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> optionKitNames(Category category) {
        List<String> names = new ArrayList<>();
        names.add(DEFAULT_ARMOR_KIT_NAME);
        List<String> categoryKits = new ArrayList<>(kitsFor(category));
        categoryKits.sort(String::compareTo);
        names.addAll(categoryKits);
        return names;
    }

    /**
     * Every kit internal name the catalog hardcodes and compares against — the union of the group name sets and the
     * no-protection default. Exposed for a regression test that asserts each still resolves to a real kit, so a rename
     * in MegaMek's equipment tables is caught rather than silently dropping a kit from its group.
     *
     * @return every referenced kit internal name
     *
     * @author Illiani
     * @since 0.51.01
     */
    static Set<String> allReferencedKitNames() {
        Set<String> names = new HashSet<>();
        names.add(DEFAULT_ARMOR_KIT_NAME);
        names.addAll(MEKWARRIOR_KITS);
        names.addAll(AEROSPACE_KITS);
        names.addAll(VEHICLE_KITS);
        return names;
    }

    public static List<EquipmentType> availableKits(Category category) {
        List<EquipmentType> result = new ArrayList<>();
        for (EquipmentType kit : CrewArmorKitRules.availableArmorKits()) {
            String internalName = kit.getInternalName();
            if (DEFAULT_ARMOR_KIT_NAME.equals(internalName)) {
                continue;
            }
            // Every kit in the group is listed regardless of the year; ones not available now carry an impossible
            // acquisition target so the player can still issue any they happen to hold in stores.
            if (isInCategory(internalName, category)) {
                result.add(kit);
            }
        }
        return result;
    }

    /**
     * Whether the player may buy and issue a kit for this person at all: a combat crew member who mans a unit that can
     * wear a kit — a Mek, aerospace, vehicle, or large-craft crew. Foot infantry and battle armor (whose armor is the
     * unit), ProtoMeks (no ejection), and support or civilian personnel are never issued a kit.
     *
     * @param person the person being considered, or {@code null}
     *
     * @return {@code true} if the player can buy and issue a kit for this person
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean canBeIssuedKit(@Nullable Person person) {
        if (person == null) {
            return false;
        }
        return isKitCrewRole(person.getPrimaryRole()) || isKitCrewRole(person.getSecondaryRole());
    }

    private static boolean isKitCrewRole(PersonnelRole role) {
        return role.isMekWarriorGrouping()
                     || role.isAerospaceGrouping()
                     || role.isConventionalAircraftPilot()
                     || role.isSoldier()
                     || role.isVehicleCrewGround()
                     || role.isVehicleCrewNaval()
                     || role.isVehicleCrewVTOL()
                     || role.isVesselPilot()
                     || role.isVesselGunner()
                     || role.isVesselCrew()
                     || role.isVesselNavigator();
    }

    /**
     * The kit group this person draws from: MekWarriors (and LAM pilots) wear MekWarrior kits, aerospace and
     * conventional fighter pilots wear aerospace kits, and everyone else — vehicle and large-craft crews — wears
     * infantry kits. A person who is both (a LAM pilot) is treated as a MekWarrior.
     *
     * @param person the person whose kit group is wanted
     *
     * @return the {@link Category} of kit this person may be issued
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Category categoryFor(Person person) {
        PersonnelRole primary = person.getPrimaryRole();
        PersonnelRole secondary = person.getSecondaryRole();

        if (primary.isMekWarriorGrouping() || secondary.isMekWarriorGrouping()) {
            return Category.MEKWARRIOR;
        }
        if (primary.isAerospaceGrouping()
                  || primary.isConventionalAircraftPilot()
                  || secondary.isAerospaceGrouping()
                  || secondary.isConventionalAircraftPilot()) {
            return Category.AIRCRAFT;
        }
        if (primary.isSoldier() || secondary.isSoldier()) {
            return Category.SOLDIER;
        }
        return Category.INFANTRY;
    }

    /**
     * Whether a kit issued to this unit's crew reaches the board. This is MegaMek's
     * {@link CrewArmorKitRules#canWearArmorKit} unchanged — Mek, vehicle, and aerospace crews, who might end up outside
     * their unit on foot.
     *
     * @param entity the unit whose crew is being configured, or {@code null}
     *
     * @return {@code true} if the crew of this unit wears its issued kit
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean canWearIssuedKit(@Nullable Entity entity) {
        return CrewArmorKitRules.canWearArmorKit(entity);
    }

    /**
     * The faction-appropriate kit to issue a generated (NPC) crew, by internal name, or {@code null} for none.
     *
     * <p>MekWarriors take the Clan kit when the faction is Clan and it exists in the year, falling back to the cooling
     * suit; Inner Sphere MekWarriors take the cooling suit, or the cooling vest while the suit is extinct. Tankers take
     * the smock, aircraft the aerospace kit. Anything unavailable in the year yields {@code null}.</p>
     *
     * @param entity      the generated unit whose crew is being kitted
     * @param clanFaction whether the generating faction is Clan
     * @param year        the campaign year
     *
     * @return the kit internal name, or {@code null} if none is available or the unit cannot wear one
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable String npcKitFor(@Nullable Entity entity, boolean clanFaction, int year) {
        if (!canWearIssuedKit(entity)) {
            return null;
        }
        if (entity instanceof Mek) {
            String advancedKit = availableOrNull(KIT_MEKWARRIOR_ADVANCED, year, false);
            if (advancedKit != null) {
                return advancedKit;
            }

            if (clanFaction) {
                String clanKit = availableOrNull(KIT_MEKWARRIOR_CLAN, year, true);
                return (clanKit != null) ? clanKit : availableOrNull(KIT_MEKWARRIOR_BASIC, year, true);
            }

            return availableOrNull(KIT_MEKWARRIOR_BASIC, year, false);
        }

        if (entity instanceof Tank) {
            return availableOrNull(KIT_TANKERS_SMOCK, year, clanFaction);
        }

        if (entity.isAero()) {
            return availableOrNull(KIT_AEROSPACE_PILOT, year, clanFaction);
        }
        return null;
    }

    private static @Nullable String availableOrNull(String kitInternalName, int year, boolean clan) {
        EquipmentType kit = EquipmentType.get(kitInternalName);
        return ((kit != null) && kit.isAvailableIn(year, clan, false)) ? kitInternalName : null;
    }
}
