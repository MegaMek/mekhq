/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.utilities;

import static mekhq.campaign.enums.DailyReportType.TECHNICAL;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_AERO;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_BA;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANIC;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.jspecify.annotations.NonNull;

/**
 * Automatically assigns available tech personnel to unmaintained units.
 *
 * <p>This utility performs four main steps:</p>
 * <ol>
 *     <li><b>Bucket units</b> into unmaintained categories (Meks, Aero, Battle Armor, Vehicles) based on
 *     {@link Entity#getUnitType()}.</li>
 *     <li><b>Sort each unit bucket</b> by {@link Entity#calculateBattleValue()} (highest to lowest), so more
 *     valuable units are assigned first.</li>
 *     <li><b>Bucket techs</b> into role-based lists (Mek techs, Aero techs, BA techs, Mechanics), including only
 *     roles that have at least one unmaintained unit to assign. A person may qualify for multiple roles, but
 *     assignment to more than two roles is prevented.</li>
 *     <li><b>Assign units to techs</b> by repeatedly selecting the “best” tech (least loaded first; if tied, highest
 *     skill level first) and assigning the next unit.</li>
 * </ol>
 *
 * <p><b>Sorting/selection rules</b></p>
 * <ul>
 *     <li><b>Unit ordering:</b> higher battle value units are assigned before lower battle value units.</li>
 *     <li><b>Tech ordering:</b> techs are ordered by {@code person.getTechUnits().size()} ascending (least loaded
 *     first). If two techs have the same assigned-unit count, the tie is broken by
 *     {@link #getTechLevel(Person, String)} descending (higher skill level first).</li>
 * </ul>
 *
 * <p><b>Notes</b></p>
 * <ul>
 *   <li>Units that already have a tech assigned ({@code unit.getTech() != null}) are ignored.</li>
 *   <li>Units with {@code null} entities are ignored.</li>
 * </ul>
 *
 * @author Illiani
 * @since 0.50.11
 */
public class AutomatedTechAssignments {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.AutomatedTechAssignments";

    private final List<Unit> unmaintainedMeks = new ArrayList<>();
    private final List<Unit> unmaintainedAero = new ArrayList<>();
    private final List<Unit> unmaintainedBattleArmor = new ArrayList<>();
    private final List<Unit> unmaintainedVehicle = new ArrayList<>();

    private List<Person> techMeks;
    private List<Person> techAero;
    private List<Person> techBattleArmor;
    private List<Person> techMechanic;

    final private List<String> reports = new ArrayList<>();

    public List<String> getReports() {
        return reports;
    }

    /**
     * Creates an assignment helper, performs unit bucketing/sorting, tech bucketing/sorting, and assigns techs to all
     * unmaintained units.
     *
     * @param techs all available personnel to consider for assignment
     * @param units all units that may require a tech assignment
     *
     * @author Illiani
     * @since 0.50.11
     */
    public AutomatedTechAssignments(List<Person> techs, Collection<Unit> units) {
        arrangeUnitsIntoBuckets(units);
        sortUnitBuckets();
        arrangeTechsIntoBuckets(techs);
        sortTechBuckets();

        assignUnmaintainedUnitsTechs(techMeks, unmaintainedMeks, S_TECH_MEK);
        assignUnmaintainedUnitsTechs(techAero, unmaintainedAero, S_TECH_AERO);
        assignUnmaintainedUnitsTechs(techBattleArmor, unmaintainedBattleArmor, S_TECH_BA);
        assignUnmaintainedUnitsTechs(techMechanic, unmaintainedVehicle, S_TECH_MECHANIC);
    }

    /**
     * Populates the role-based tech lists ({@link #techMeks}, {@link #techAero}, {@link #techBattleArmor},
     * {@link #techMechanic}) from the supplied tech pool.
     *
     * <p>Techs are only added to role lists that have corresponding unmaintained units to assign. If there are no
     * unmaintained units in any category, this method returns after initializing the lists as empty.</p>
     *
     * <p>A person may qualify for multiple roles, but no more than two roles are assigned per person.</p>
     *
     * @param techs the pool of tech personnel to bucket
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void arrangeTechsIntoBuckets(List<Person> techs) {
        techMeks = new ArrayList<>();
        techAero = new ArrayList<>();
        techBattleArmor = new ArrayList<>();
        techMechanic = new ArrayList<>();

        final boolean hasUnmaintainedMeks = !unmaintainedMeks.isEmpty();
        final boolean hasUnmaintainedAero = !unmaintainedAero.isEmpty();
        final boolean hasUnmaintainedBattleArmor = !unmaintainedBattleArmor.isEmpty();
        final boolean hasUnmaintainedVehicles = !unmaintainedVehicle.isEmpty();

        if (!hasUnmaintainedMeks && !hasUnmaintainedAero && !hasUnmaintainedBattleArmor && !hasUnmaintainedVehicles) {
            return;
        }

        final Set<Person> tempSetOfTechMeks = new HashSet<>();
        final Set<Person> tempSetOfTechAero = new HashSet<>();
        final Set<Person> tempSetOfTechBattleArmor = new HashSet<>();
        final Set<Person> tempSetOfTechMechanic = new HashSet<>();

        for (Person person : techs) {
            if (person.isNeverAssignMaintenanceAutomatically() || person.getTechUnits().size() >= 2) {
                continue;
            }

            int rolesAssigned = 0;

            if (hasUnmaintainedMeks && person.isTechMek()) {
                tempSetOfTechMeks.add(person);
                rolesAssigned++;
            }

            if (hasUnmaintainedAero && person.isTechAero()) {
                tempSetOfTechAero.add(person);
                rolesAssigned++;
            }

            if (rolesAssigned < 2 && hasUnmaintainedBattleArmor && person.isTechBA()) {
                tempSetOfTechBattleArmor.add(person);
                rolesAssigned++;
            }

            if (rolesAssigned < 2 && hasUnmaintainedVehicles && person.isTechMechanic()) {
                tempSetOfTechMechanic.add(person);
            }
        }

        techMeks.addAll(tempSetOfTechMeks);
        techAero.addAll(tempSetOfTechAero);
        techBattleArmor.addAll(tempSetOfTechBattleArmor);
        techMechanic.addAll(tempSetOfTechMechanic);
    }

    /**
     * Assigns each unit in {@code unmaintainedUnits} to a tech from {@code techs} using a load-balancing priority rule
     * and a per-tech capacity limit.
     *
     * <p>The “best” tech is selected by:</p>
     * <ol>
     *     <li>{@link #getTechLevel(Person, String)} highest -> lowest</li>
     *     <li>tie-breaker: fewest assigned tech units lowest -> highest</li>
     * </ol>
     *
     * <p><b>Capacity limit:</b> a tech may be assigned at most two units total. Techs that already have
     * {@code person.getTechUnits().size() >= 2} are skipped. After assigning a unit, a tech is only reinserted into
     * the priority queue if they are still under the limit.</p>
     *
     * <p>If {@code unmaintainedUnits} is {@code null} or empty, this method does nothing. If {@code techs} is
     * {@code null} or empty, each unit in {@code unmaintainedUnits} is reported as unassignable.</p>
     *
     * <p>If the method runs out of eligible techs while units remain, each remaining unit is reported as
     * unassignable.</p>
     *
     * @param techs             the tech candidates eligible for these units (modified in-place to reflect the final
     *                          priority-queue poll order of any remaining eligible techs)
     * @param unmaintainedUnits the units that need a tech assigned
     * @param techSkill         the skill type used to evaluate tech proficiency for tiebreaking
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void assignUnmaintainedUnitsTechs(List<Person> techs, List<Unit> unmaintainedUnits, String techSkill) {
        if (unmaintainedUnits == null || unmaintainedUnits.isEmpty()) {
            return;
        }

        // Report when there are units to assign but no eligible techs.
        if (techs == null || techs.isEmpty()) {
            for (Unit unit : unmaintainedUnits) {
                reports.add(getFormattedTextAt(
                      RESOURCE_BUNDLE,
                      "AutomatedTechAssignments.unableToAssign",
                      unit.getHyperlinkedName(),
                      techSkill
                ));
            }
            return;
        }

        PriorityQueue<Person> queue = getPersonPriorityQueue(techSkill);
        queue.addAll(techs);

        for (Unit unit : unmaintainedUnits) {
            Person tech = queue.poll();

            // Skip techs who are already at/over capacity (>= 2 units).
            while (tech != null && tech.getTechUnits().size() >= 2) {
                tech = queue.poll();
            }

            if (tech == null) {
                // No more eligible techs: report this unit as unassignable.
                reports.add(getFormattedTextAt(
                      RESOURCE_BUNDLE,
                      "AutomatedTechAssignments.unableToAssign",
                      unit.getHyperlinkedName(),
                      techSkill
                ));
                continue;
            }

            unit.setTech(tech);

            reports.add(getFormattedTextAt(
                  RESOURCE_BUNDLE,
                  "AutomatedTechAssignments.automaticallyAssigned",
                  tech.getHyperlinkedName(),
                  unit.getHyperlinkedName()
            ));

            // Only re-queue if still eligible after assignment.
            if (tech.getTechUnits().size() < 2) {
                queue.offer(tech);
            }
        }

        techs.clear();
        while (!queue.isEmpty()) {
            techs.add(queue.poll());
        }
    }

    /**
     * Creates a priority queue of {@link Person} instances ordered by suitability for assignment.
     *
     * <p>The queue is ordered so that the "best" tech is at the head:</p>
     * <ol>
     *     <li><b>Tie-breaker</b>: higher tech skill first, using {@link #getTechLevel(Person, String)} with the
     *     provided {@code techSkill}.</li>
     *     <li><b>Lowest current workload first</b>: {@code person.getTechUnits().size()} ascending (fewest assigned
     *     units).</li>
     * </ol>
     *
     * <p>This ordering is used to implement simple load-balancing when assigning units to techs: repeatedly polling
     * from the queue yields the least-busy tech, preferring more skilled techs when workloads are equal.</p>
     *
     * @param techSkill the skill identifier used to evaluate tech proficiency for tiebreaking
     *
     * @return a non-null, empty {@link PriorityQueue} configured with the appropriate ordering
     *
     * @author Illiani
     * @since 0.50.11
     */
    private @NonNull PriorityQueue<Person> getPersonPriorityQueue(String techSkill) {
        Comparator<Person> bestTechFirst = Comparator
                                                 .comparingInt((Person p) -> getTechLevel(p, techSkill))
                                                 .reversed() // highest tech level first
                                                 .thenComparingInt(p -> p.getTechUnits().size()); // smallest to largest

        return new PriorityQueue<>(bestTechFirst);
    }

    /**
     * Sorts all unit buckets by battle value, highest to lowest.
     *
     * <p>This delegates to {@link #sortByBattleValue(List)} for each bucket.</p>
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void sortUnitBuckets() {
        sortByBattleValue(unmaintainedMeks);
        sortByBattleValue(unmaintainedAero);
        sortByBattleValue(unmaintainedBattleArmor);
        sortByBattleValue(unmaintainedVehicle);
    }

    /**
     * Sorts all tech buckets using {@link #sortTechList(List, String)} and the appropriate skill type for each role.
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void sortTechBuckets() {
        sortTechList(techMeks, S_TECH_MEK);
        sortTechList(techAero, S_TECH_AERO);
        sortTechList(techBattleArmor, S_TECH_BA);
        sortTechList(techMechanic, S_TECH_MECHANIC);
    }

    /**
     * Sorts the provided tech list by:
     *
     * <ol>
     *     <li>{@link #getTechLevel(Person, String)} descending (highest proficiency first)</li>
     *     <li>{@code person.getTechUnits().size()} ascending (least loaded first)</li>
     * </ol>
     *
     * <p>If {@code techs} is {@code null} or empty, this method does nothing.</p>
     *
     * @param techs     the list of tech personnel to sort in-place
     * @param skillType the skill used to compute proficiency for tiebreaking
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void sortTechList(List<Person> techs, String skillType) {
        if (techs == null || techs.isEmpty()) {
            return;
        }

        techs.sort(
              Comparator
                    .comparingInt((Person tech) -> getTechLevel(tech, skillType))
                    .reversed() // highest tech level first
                    .thenComparingInt(tech -> tech.getTechUnits().size()) // smallest -> largest
        );
    }

    /**
     * Buckets units into the appropriate unmaintained category lists based on entity unit type.
     *
     * <p>Only units without an assigned tech ({@code unit.getTech() == null}) are considered. Units whose
     * {@link Unit#getEntity()} is {@code null} are ignored.</p>
     *
     * <p>Unit type mapping:</p>
     * <ul>
     *     <li>Meks: {@link UnitType#MEK}, {@link UnitType#PROTOMEK}, {@link UnitType#HANDHELD_WEAPON}</li>
     *     <li>Vehicles: {@link UnitType#TANK}, {@link UnitType#VTOL}, {@link UnitType#NAVAL}</li>
     *     <li>Battle Armor: {@link UnitType#BATTLE_ARMOR}</li>
     *     <li>Aero: {@link UnitType#CONV_FIGHTER}, {@link UnitType#AEROSPACE_FIGHTER}, {@link UnitType#SMALL_CRAFT}</li>
     *     <li>Ignored (unsupported/self-crewed): gun emplacements, buildings, stations, WarShips, JumpShips,
     *     DropShips, etc.</li>
     * </ul>
     *
     * @param units all units to inspect for bucketing
     *
     * @author Illiani
     * @since 0.50.11
     */
    private void arrangeUnitsIntoBuckets(Collection<Unit> units) {
        for (Unit unit : units) {
            if (unit.getTech() == null) {
                Entity entity = unit.getEntity();
                if (entity == null) {
                    continue;
                }

                int unitType = entity.getUnitType();
                switch (unitType) {
                    // Self-crewed or not yet supported
                    case UnitType.GUN_EMPLACEMENT, UnitType.ADVANCED_BUILDING, UnitType.MOBILE_STRUCTURE,
                         UnitType.SPACE_STATION, UnitType.WARSHIP, UnitType.JUMPSHIP, UnitType.DROPSHIP -> {}
                    // Everything else
                    case UnitType.MEK, UnitType.PROTOMEK, UnitType.HANDHELD_WEAPON -> unmaintainedMeks.add(unit);
                    case UnitType.TANK, UnitType.VTOL, UnitType.NAVAL -> unmaintainedVehicle.add(unit);
                    case UnitType.BATTLE_ARMOR -> unmaintainedBattleArmor.add(unit);
                    case UnitType.CONV_FIGHTER, UnitType.AEROSPACE_FIGHTER, UnitType.SMALL_CRAFT ->
                          unmaintainedAero.add(unit);
                }
            }
        }
    }

    /**
     * Sorts the provided unit list in-place by {@link Entity#calculateBattleValue()} descending.
     *
     * <p>Units with {@code null} entities are treated as having the smallest possible battle value and will appear
     * last in the sorted list.</p>
     *
     * @param units the unit list to sort in-place
     *
     * @author Illiani
     * @since 0.50.11
     */
    private static void sortByBattleValue(List<Unit> units) {
        units.sort(Comparator.comparingInt((Unit unit) -> {
                  Entity entity = unit.getEntity();
                  return (entity == null) ? Integer.MIN_VALUE : entity.calculateBattleValue();
              }
        ).reversed());
    }

    /**
     * Returns the tech’s effective skill level for the given skill type.
     *
     * <p>If the person has the skill, this returns {@link Skill#getTotalSkillLevel(SkillModifierData)} using the
     * person’s current {@link SkillModifierData}. If the person does not have the skill, {@link SkillType#EXP_NONE} is
     * returned.</p>
     *
     * @param person    the tech whose skill is being queried
     * @param skillType the skill identifier to fetch from the person
     *
     * @return the computed total skill level, or {@link SkillType#EXP_NONE} if the skill is missing
     *
     * @author Illiani
     * @since 0.50.11
     */
    public int getTechLevel(Person person, String skillType) {
        Skill skill = person.getSkill(skillType);
        if (skill != null) {
            SkillModifierData modifierData = person.getSkillModifierData();
            return skill.getTotalSkillLevel(modifierData);
        }

        return EXP_NONE;
    }

    /**
     * Performs an automatic assignment process for units that are currently unmaintained and records the outcome.
     *
     * <p>This method creates an {@link AutomatedTechAssignments} instance using the campaign's technicians and units.
     * Any generated report entries are appended to the campaign as {@code TECHNICAL} reports. If at least one report is
     * produced, an additional informational message explaining how to disable the feature is added first, followed by
     * each report line.</p>
     *
     * @param campaign the campaign whose technicians and units are evaluated and to which any resulting reports are
     *                 added; must not be {@code null}
     *
     * @author Illiani
     * @since 0.50.11
     */
    public static void handleTheAutomaticAssignmentOfUnmaintainedUnits(Campaign campaign) {
        AutomatedTechAssignments automatedAssignments = new AutomatedTechAssignments(campaign.getTechs(),
              campaign.getUnits());
        List<String> reports = automatedAssignments.getReports();
        if (!reports.isEmpty()) {
            String message = getTextAt("mekhq.resources.AutomatedTechAssignments",
                  "AutomatedTechAssignments.howToDisable");
            campaign.addReport(TECHNICAL, message);

            for (String report : reports) {
                campaign.addReport(TECHNICAL, report);
            }
        }
    }
}
