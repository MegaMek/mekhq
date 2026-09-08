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

import static mekhq.campaign.personnel.skills.SkillType.S_TECH_AERONAUTICS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_CYBERNETICS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_ELECTRONIC;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_JETS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANICAL;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MYOMER;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_NUCLEAR;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_WEAPONS;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;

/**
 * The catalog of CamOps specialized repair kits MekHQ issues to technicians, and which {@code Tech/...} repair skills
 * each kit improves.
 *
 * <p>This mirrors {@link ArmorKitCatalog} for repair kits: the kits are MegaMek {@code MiscType} equipment (defined in
 * {@code MiscType.createXxxRepairKit()}), referenced here by internal name, and grouped by the specialist skill they
 * assist. A technician who owns a kit gets a bonus to rolls made with the skills that kit covers (applied by the repair
 * target logic). Only the granular {@code Tech/...} skills are covered, so kits assist part repairs, not whole-unit
 * maintenance or refits, which use the global technician skills.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class RepairKitCatalog {
    /**
     * The bonus a matching specialized repair kit grants to a covered repair roll (a reduction to the target number).
     * Set to +2 so a specialist kit remains worthwhile alongside a Deluxe Toolkit, which grants +1 to every technician
     * roll.
     */
    public static final int REPAIR_KIT_ROLL_BONUS = 2;

    /** The bonus a Deluxe Toolkit grants to any technician roll (repairs and maintenance). */
    public static final int DELUXE_TOOLKIT_ROLL_BONUS = 1;

    // The specialized repair kits the catalog reasons about, by their MegaMek internal name
    // (see MiscType.createXxxRepairKit()).
    public static final String KIT_AEROSPACE = "Aerospace Repair Kit";
    public static final String KIT_BIONIC_MAINTENANCE = "Bionic Maintenance Kit";
    public static final String KIT_CUTTING_JOINING = "Cutting/Joining Kit";
    public static final String KIT_ELECTRONICS = "Electronics Repair Kit";
    public static final String KIT_FISSION_FUSION = "Fission/Fusion Repair Kit";
    public static final String KIT_MYOMER_ACTUATOR = "Myomer/Actuator Repair Kit";
    public static final String KIT_VEHICLE = "Vehicle Repair Kit";
    public static final String KIT_WEAPON = "Weapon Repair Kit";

    // General technician gear (not skill-specific). Toolkits improve every technician roll; the Descartes diagnostic
    // scanners improve maintenance ("diagnosing damage") checks. Basic Toolkit is issuable but has no roll bonus.
    public static final String KIT_BASIC_TOOLKIT = "Basic Toolkit";
    public static final String KIT_DELUXE_TOOLKIT = "Deluxe Toolkit";
    public static final String KIT_DESCARTES_MK_XXI = "Descartes MK XXI";
    public static final String KIT_DESCARTES_MK_XXV = "Descartes MK XXV";

    /** Kit internal name -> the {@code Tech/...} skills it improves. Insertion-ordered for stable display. */
    private static final Map<String, Set<String>> KIT_SKILLS = new LinkedHashMap<>();

    static {
        KIT_SKILLS.put(KIT_AEROSPACE, Set.of(S_TECH_AERONAUTICS, S_TECH_JETS));
        KIT_SKILLS.put(KIT_BIONIC_MAINTENANCE, Set.of(S_TECH_CYBERNETICS, S_TECH_MYOMER));
        KIT_SKILLS.put(KIT_CUTTING_JOINING, Set.of(S_TECH_MECHANICAL));
        KIT_SKILLS.put(KIT_ELECTRONICS, Set.of(S_TECH_ELECTRONIC));
        KIT_SKILLS.put(KIT_FISSION_FUSION, Set.of(S_TECH_NUCLEAR, S_TECH_ELECTRONIC));
        KIT_SKILLS.put(KIT_MYOMER_ACTUATOR, Set.of(S_TECH_MYOMER));
        KIT_SKILLS.put(KIT_VEHICLE, Set.of(S_TECH_MECHANICAL));
        KIT_SKILLS.put(KIT_WEAPON, Set.of(S_TECH_WEAPONS));
    }

    /** Every technician-gear item the quartermaster can issue, in display order: the repair kits, then general gear. */
    private static final Set<String> ISSUABLE_KITS = new LinkedHashSet<>();

    static {
        ISSUABLE_KITS.addAll(KIT_SKILLS.keySet());
        ISSUABLE_KITS.add(KIT_BASIC_TOOLKIT);
        ISSUABLE_KITS.add(KIT_DELUXE_TOOLKIT);
        ISSUABLE_KITS.add(KIT_DESCARTES_MK_XXI);
        ISSUABLE_KITS.add(KIT_DESCARTES_MK_XXV);
    }

    private RepairKitCatalog() {
    }

    /**
     * @return every technician-gear internal name the quartermaster can issue, in display order.
     */
    public static Set<String> allKitNames() {
        return ISSUABLE_KITS;
    }

    /**
     * @param kitInternalName a repair-kit internal name
     *
     * @return the {@code Tech/...} skills that kit improves (empty if the kit is unknown)
     */
    public static Set<String> skillsBoostedBy(@Nullable String kitInternalName) {
        return KIT_SKILLS.getOrDefault(kitInternalName, Set.of());
    }

    /**
     * @param skillName a {@code Tech/...} skill name
     *
     * @return {@code true} if any repair kit improves rolls made with this skill
     */
    public static boolean isKitBoostedSkill(@Nullable String skillName) {
        for (Set<String> skills : KIT_SKILLS.values()) {
            if (skills.contains(skillName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The repair kits that improve a given skill.
     *
     * @param skillName a {@code Tech/...} skill name
     *
     * @return the internal names of the kits that improve rolls made with this skill
     */
    public static Set<String> kitsBoostingSkill(@Nullable String skillName) {
        Set<String> result = new LinkedHashSet<>();
        for (Map.Entry<String, Set<String>> entry : KIT_SKILLS.entrySet()) {
            if (entry.getValue().contains(skillName)) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Whether the player may buy and issue tool kits for this person. Any character may be issued a tool kit; whether a
     * kit actually helps depends on the work they do (its bonus applies only to the repair rolls it covers), but
     * ownership itself is unrestricted.
     *
     * @param person the person being considered, or {@code null}
     *
     * @return {@code true} if tool kits may be issued to this person
     */
    public static boolean canBeIssuedKit(@Nullable Person person) {
        return person != null;
    }

    /**
     * Whether the given person owns a repair kit that improves rolls made with the given skill.
     *
     * @param person    the technician, or {@code null}
     * @param skillName the {@code Tech/...} skill being rolled
     *
     * @return {@code true} if the person owns at least one kit covering the skill
     */
    public static boolean ownsKitBoostingSkill(@Nullable Person person, @Nullable String skillName) {
        if ((person == null) || (skillName == null)) {
            return false;
        }
        for (String kitName : kitsBoostingSkill(skillName)) {
            if (person.hasRepairKit(kitName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The bonus this technician's owned kits grant to a <em>repair</em> roll made with the given skill: the best of a
     * matching specialized repair kit ({@link #REPAIR_KIT_ROLL_BONUS}) and a Deluxe Toolkit
     * ({@link #DELUXE_TOOLKIT_ROLL_BONUS}). Bonuses are not stacked - the single best applicable is used - so a Deluxe
     * Toolkit never adds on top of a matching specialist kit.
     *
     * @param person    the technician, or {@code null}
     * @param skillName the {@code Tech/...} skill being rolled
     *
     * @return the roll bonus (0 if none applies)
     */
    public static int repairBonus(@Nullable Person person, @Nullable String skillName) {
        if (person == null) {
            return 0;
        }
        int bonus = 0;
        if (ownsKitBoostingSkill(person, skillName)) {
            bonus = REPAIR_KIT_ROLL_BONUS;
        }
        if (person.hasRepairKit(KIT_DELUXE_TOOLKIT)) {
            bonus = Math.max(bonus, DELUXE_TOOLKIT_ROLL_BONUS);
        }
        return bonus;
    }

    /**
     * The bonus this technician's owned kits grant to a <em>maintenance</em> check: the best of a Descartes diagnostic
     * scanner (MK XXV grants +3, MK XXI grants +2, for "diagnosing damage") and a Deluxe Toolkit (+1 to any technician
     * roll). The Descartes bonuses are set one above their base CamOps values so a Deluxe Toolkit's general +1 does not
     * invalidate them. Bonuses are not stacked - the single best applicable is used.
     *
     * @param person the technician, or {@code null}
     *
     * @return the maintenance-check bonus (0 if none applies)
     */
    public static int maintenanceBonus(@Nullable Person person) {
        if (person == null) {
            return 0;
        }
        int bonus = 0;
        if (person.hasRepairKit(KIT_DESCARTES_MK_XXV)) {
            bonus = 3;
        } else if (person.hasRepairKit(KIT_DESCARTES_MK_XXI)) {
            bonus = 2;
        }
        if (person.hasRepairKit(KIT_DELUXE_TOOLKIT)) {
            bonus = Math.max(bonus, DELUXE_TOOLKIT_ROLL_BONUS);
        }
        return bonus;
    }
}
