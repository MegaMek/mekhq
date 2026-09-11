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

import static mekhq.campaign.personnel.skills.SkillType.*;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * The catalog of CamOps specialized equipment kits MekHQ issues to technicians, and which {@code Tech/...} repair skills
 * each kit improves.
 *
 * <p>This mirrors {@link ArmorKitCatalog} for equipment kits: the kits are MegaMek {@code MiscType} equipment (defined in
 * {@code MiscType.createXxxRepairKit()}), referenced here by internal name, and grouped by the specialist skill they
 * assist. A technician who owns a kit gets a bonus to rolls made with the skills that kit covers (applied by the repair
 * target logic). Only the granular {@code Tech/...} skills are covered, so kits assist part repairs, not whole-unit
 * maintenance or refits, which use the global technician skills.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class EquipmentKitCatalog extends AbstractKitCatalog {
    public static final int REPAIR_KIT_ROLL_BONUS = 1;
    public static final int DELUXE_TOOLKIT_ROLL_BONUS = 1;

    // The specialized equipment kits the catalog reasons about, by their MegaMek internal name
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

    // General equipment kits that improve a non-technician skill (survival, navigation, medical, or administrative
    // work). Their bonuses are held in SKILL_BONUSES below.
    public static final String KIT_ADVANCED_FIELD = "Advanced Field Kit";
    public static final String KIT_BASIC_FIELD = "Basic Field Kit";
    public static final String KIT_COMPASS = "Compass";
    public static final String KIT_ELECTRONIC_COMPASS = "Electronic Compass";
    public static final String KIT_ADVANCED_MEDICAL = "Advanced Medical Kit";
    public static final String KIT_FIELD_SURGICAL = "Field Surgical Kit";
    public static final String KIT_MEDICAL = "Medical Kit";
    public static final String KIT_COMPAD = "Compad";
    public static final String KIT_NOTEPUTER = "Noteputer";
    public static final String KIT_PERSONAL_COMPUTER = "Personal Computer";
    public static final String KIT_POCKET_TRANSCRIBER = "Pocket Transcriber";
    public static final String KIT_TELESCAN = "Telescan";

    /**
     * Kit internal name -> the non-technician skills it improves and by how much. The medical kits' Surgery bonus is
     * one above the CamOps value (raised by +1, even where the book lists none) per campaign customization. These
     * bonuses are separate from the specialized-repair-kit repair bonus ({@link #REPAIR_KIT_ROLL_BONUS}).
     */
    private static final Map<String, Map<String, Integer>> SKILL_BONUSES = new LinkedHashMap<>();

    static {
        SKILL_BONUSES.put(KIT_ADVANCED_FIELD, Map.of(S_NAVIGATION, 1, S_SURVIVAL, 1));
        SKILL_BONUSES.put(KIT_BASIC_FIELD, Map.of(S_SURVIVAL, 1));
        SKILL_BONUSES.put(KIT_COMPASS, Map.of(S_NAVIGATION, 1));
        SKILL_BONUSES.put(KIT_ELECTRONIC_COMPASS, Map.of(S_NAVIGATION, 2));
        SKILL_BONUSES.put(KIT_ADVANCED_MEDICAL, Map.of(S_MEDTECH, 1, S_SURGERY, 1));
        SKILL_BONUSES.put(KIT_FIELD_SURGICAL, Map.of(S_MEDTECH, 2, S_SURGERY, 1));
        SKILL_BONUSES.put(KIT_MEDICAL, Map.of(S_MEDTECH, 1, S_SURGERY, 0));
        SKILL_BONUSES.put(KIT_COMPAD, Map.of(S_COMMUNICATIONS, 1));
        SKILL_BONUSES.put(KIT_NOTEPUTER, Map.of(S_ADMIN, 1, S_COMMUNICATIONS, 1));
        SKILL_BONUSES.put(KIT_PERSONAL_COMPUTER, Map.of(S_ADMIN, 1));
        SKILL_BONUSES.put(KIT_POCKET_TRANSCRIBER, Map.of(S_NEGOTIATION, 1));
        SKILL_BONUSES.put(KIT_TELESCAN, Map.of(S_APPRAISAL, 1));
    }

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

    /** Every technician-gear item the quartermaster can issue, in display order: the equipment kits, then general gear. */
    private static final Set<String> ISSUABLE_KITS = new LinkedHashSet<>();

    static {
        ISSUABLE_KITS.addAll(KIT_SKILLS.keySet());
        ISSUABLE_KITS.add(KIT_BASIC_TOOLKIT);
        ISSUABLE_KITS.add(KIT_DELUXE_TOOLKIT);
        ISSUABLE_KITS.add(KIT_DESCARTES_MK_XXI);
        ISSUABLE_KITS.add(KIT_DESCARTES_MK_XXV);
        ISSUABLE_KITS.addAll(SKILL_BONUSES.keySet());
    }

    /** The sentinel value for a per-profession default-kit campaign option meaning "issue no tool kit". */
    public static final String NO_DEFAULT_KIT = "";

    /**
     * The professions that receive a per-profession default equipment kit on recruitment: the four technician
     * professions (Astechs excluded), plus doctors and administrators for their medical and computer kits.
     */
    public enum KitProfession {MEK_TECH, MECHANIC, AERO_TEK, BA_TECH, ASTECH, DOCTOR, MEDIC, ADMIN}

    private EquipmentKitCatalog() {
    }

    /**
     * The choices for a per-profession default-kit dropdown: the "none" sentinel first, then every issuable tool kit by
     * internal name.
     *
     * @return the option values, "none" first
     */
    public static List<String> optionKitNames() {
        return optionKitNames(NO_DEFAULT_KIT, allKitNames());
    }

    /**
     * The technician professions this person qualifies for, by primary and secondary role. Used to decide which
     * per-profession default tool kits to issue on recruitment.
     *
     * @param person the person, or {@code null}
     *
     * @return the professions the person holds (possibly empty)
     */
    public static Set<KitProfession> professionsFor(@Nullable Person person) {
        Set<KitProfession> professions = EnumSet.noneOf(KitProfession.class);
        if (person == null) {
            return professions;
        }
        for (PersonnelRole role : List.of(person.getPrimaryRole(), person.getSecondaryRole())) {
            if (role.isMekTech()) {
                professions.add(KitProfession.MEK_TECH);
                continue;
            }
            if (role.isMechanic()) {
                professions.add(KitProfession.MECHANIC);
                continue;
            }
            if (role.isAeroTek()) {
                professions.add(KitProfession.AERO_TEK);
                continue;
            }
            if (role.isBATech()) {
                professions.add(KitProfession.BA_TECH);
                continue;
            }
            if (role.isDoctor()) {
                professions.add(KitProfession.DOCTOR);
                continue;
            }
            if (role.isAdministrator()) {
                professions.add(KitProfession.ADMIN);
                continue;
            }
            if (role.isMedic()) {
                professions.add(KitProfession.MEDIC);
                continue;
            }
            if (role.isAstech()) {
                professions.add(KitProfession.ASTECH);
                continue;
            }
        }
        return professions;
    }

    /**
     * The best bonus this person's owned equipment kits grant to a roll made with the given non-technician skill (for
     * example a medical kit boosting {@code MedTech}, or a computer boosting {@code Administration}). Bonuses are not
     * stacked - the single best applicable kit is used.
     *
     * @param person    the person, or {@code null}
     * @param skillName the skill being rolled
     *
     * @return the roll bonus (0 if the person owns no kit covering the skill)
     */
    public static int generalSkillBonus(@Nullable Person person, @Nullable String skillName) {
        if ((person == null) || (skillName == null)) {
            return 0;
        }
        int best = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : SKILL_BONUSES.entrySet()) {
            if (person.hasRepairKit(entry.getKey())) {
                best = Math.max(best, entry.getValue().getOrDefault(skillName, 0));
            }
        }
        return best;
    }

    /**
     * Every skill this person's owned tool kit improves, mapped to the modifier for each - the single source of truth
     * folded into {@code SkillModifierData} so a kit acts as a plain modifier to the skill (raising its effective value)
     * rather than a bespoke modifier on a particular roll. This covers:
     *
     * <ul>
     *     <li>a specialized repair kit: {@link #REPAIR_KIT_ROLL_BONUS} to each {@code Tech/...} skill it covers;</li>
     *     <li>the Deluxe Toolkit: {@link #DELUXE_TOOLKIT_ROLL_BONUS} to every technician skill (so it reaches both part
     *     repairs and, through the whole-unit global skills, maintenance and refits);</li>
     *     <li>a field/medical/computer kit: its {@link #SKILL_BONUSES} bonus to the non-technician skill(s) it aids.</li>
     * </ul>
     *
     * <p>Maintenance-only diagnostic bonuses (the Descartes scanners) are <em>not</em> here - they modify the
     * maintenance roll itself, not a skill, so they stay bespoke in {@link #maintenanceBonus(Person)}.</p>
     *
     * @param person the person, or {@code null}
     *
     * @return skill name -&gt; best kit modifier (never {@code null}; empty when nothing applies)
     */
    public static Map<String, Integer> kitSkillBonuses(@Nullable Person person) {
        if (person == null) {
            return Map.of();
        }
        Map<String, Integer> bonuses = new LinkedHashMap<>();

        // Specialized equipment kits: +REPAIR_KIT_ROLL_BONUS to each Tech skill they cover.
        for (Map.Entry<String, Set<String>> entry : KIT_SKILLS.entrySet()) {
            if (person.hasRepairKit(entry.getKey())) {
                for (String skill : entry.getValue()) {
                    bonuses.merge(skill, REPAIR_KIT_ROLL_BONUS, Math::max);
                }
            }
        }

        if (person.hasRepairKit(KIT_DELUXE_TOOLKIT)) {
            bonuses.merge(S_ASTECH, DELUXE_TOOLKIT_ROLL_BONUS, Math::max);
        }

        // Field/medical/computer kits: their non-technician skill bonuses.
        for (Map.Entry<String, Map<String, Integer>> entry : SKILL_BONUSES.entrySet()) {
            if (person.hasRepairKit(entry.getKey())) {
                for (Map.Entry<String, Integer> skillBonus : entry.getValue().entrySet()) {
                    bonuses.merge(skillBonus.getKey(), skillBonus.getValue(), Math::max);
                }
            }
        }
        return bonuses;
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
     * The equipment kits that improve a given skill.
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
     * The bespoke bonus a Descartes diagnostic scanner grants to a <em>maintenance</em> check (MK XXV grants +3, MK XXI
     * grants +2, for "diagnosing damage"). This modifies the maintenance roll itself rather than a skill, so unlike the
     * repair and Deluxe-Toolkit bonuses - which are skill modifiers folded into {@link #kitSkillBonuses(Person)} and so
     * reach maintenance automatically through the whole-unit skill's value - it must be applied by the maintenance logic
     * directly. The Descartes values sit one above their base CamOps figures so a Deluxe Toolkit's general +1 (already
     * in the skill value) does not invalidate them.
     *
     * @param person the technician, or {@code null}
     *
     * @return the Descartes maintenance-check bonus (0 if none applies)
     */
    public static int maintenanceBonus(@Nullable Person person) {
        if (person == null) {
            return 0;
        }
        if (person.hasRepairKit(KIT_DESCARTES_MK_XXV)) {
            return 2;
        } else if (person.hasRepairKit(KIT_DESCARTES_MK_XXI)) {
            return 1;
        }
        return 0;
    }

    /**
     * Whether this technician carries a general tool kit - a Basic Toolkit or the better Deluxe Toolkit - which is the
     * minimum kit required to perform a repair when the "Techs Need a Tool Kit" campaign option is enabled. Specialized
     * equipment kits augment a tool kit rather than replace it, so they do not satisfy this requirement on their own.
     *
     * @param person the technician, or {@code null}
     *
     * @return {@code true} if the technician carries at least a Basic Toolkit
     */
    public static boolean hasToolKit(@Nullable Person person) {
        return (person != null)
                     && (person.hasRepairKit(KIT_BASIC_TOOLKIT) || person.hasRepairKit(KIT_DELUXE_TOOLKIT));
    }
}
