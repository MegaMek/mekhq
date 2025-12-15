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

import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MEK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import megamek.common.units.UnitType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillModifierData;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

class AutomatedTechAssignmentsTest {
    @Test
    void getTechLevel_returnsExpNoneWhenSkillMissing() {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        Person person = mock(Person.class);
        when(person.getSkill("SOME_SKILL")).thenReturn(null);

        assertEquals(EXP_NONE, techAssignments.getTechLevel(person, "SOME_SKILL"));
    }

    @Test
    void getTechLevel_returnsSkillTotalSkillLevelWhenSkillPresent() {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        Person person = mock(Person.class);
        Skill skill = mock(Skill.class);
        SkillModifierData modData = mock(SkillModifierData.class);

        when(person.getSkill("SOME_SKILL")).thenReturn(skill);
        when(person.getSkillModifierData()).thenReturn(modData);
        when(skill.getTotalSkillLevel(modData)).thenReturn(42);

        assertEquals(42, techAssignments.getTechLevel(person, "SOME_SKILL"));
    }

    @Test
    void constructor_bucketsUnitsByUnitType_andSkipsNullEntityAndAlreadyAssignedTech() throws Exception {
        // Units with null tech and valid entities
        Unit mekUnit = unitWithUnitTypeAndBV(UnitType.MEK, 10);
        Unit protoMekUnit = unitWithUnitTypeAndBV(UnitType.PROTOMEK, 20);
        Unit handheldUnit = unitWithUnitTypeAndBV(UnitType.HANDHELD_WEAPON, 30);

        Unit tankUnit = unitWithUnitTypeAndBV(UnitType.TANK, 40);
        Unit vtolUnit = unitWithUnitTypeAndBV(UnitType.VTOL, 50);
        Unit navalUnit = unitWithUnitTypeAndBV(UnitType.NAVAL, 60);

        Unit baUnit = unitWithUnitTypeAndBV(UnitType.BATTLE_ARMOR, 70);

        Unit convFighterUnit = unitWithUnitTypeAndBV(UnitType.CONV_FIGHTER, 80);
        Unit asfUnit = unitWithUnitTypeAndBV(UnitType.AEROSPACE_FIGHTER, 90);
        Unit smallCraftUnit = unitWithUnitTypeAndBV(UnitType.SMALL_CRAFT, 100);

        // Skipped: null entity
        Unit nullEntityUnit = mock(Unit.class);
        when(nullEntityUnit.getTech()).thenReturn(null);
        when(nullEntityUnit.getEntity()).thenReturn(null);

        // Skipped: already has a tech
        Unit alreadyTeched = unitWithUnitTypeAndBV(UnitType.MEK, 999);
        when(alreadyTeched.getTech()).thenReturn(mock(Person.class)); // not null => excluded

        // Unsupported/self-crewed bucket (explicitly ignored by switch)
        Unit dropshipUnit = unitWithUnitTypeAndBV(UnitType.DROPSHIP, 1234);

        List<Unit> units = List.of(
              mekUnit, protoMekUnit, handheldUnit,
              tankUnit, vtolUnit, navalUnit,
              baUnit,
              convFighterUnit, asfUnit, smallCraftUnit,
              nullEntityUnit,
              alreadyTeched,
              dropshipUnit
        );

        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), units);

        List<Unit> unmaintainedMeks = getField(techAssignments, "unmaintainedMeks");
        List<Unit> unmaintainedVehicle = getField(techAssignments, "unmaintainedVehicle");
        List<Unit> unmaintainedBattleArmor = getField(techAssignments, "unmaintainedBattleArmor");
        List<Unit> unmaintainedAero = getField(techAssignments, "unmaintainedAero");

        // Correct bucketing
        assertEquals(List.of(handheldUnit, protoMekUnit, mekUnit), unmaintainedMeks,
              "Meks bucket should contain MEK/PROTOMEK/HANDHELD_WEAPON, sorted by BV desc");
        assertEquals(List.of(navalUnit, vtolUnit, tankUnit), unmaintainedVehicle,
              "Vehicle bucket should contain TANK/VTOL/NAVAL, sorted by BV desc");
        assertEquals(List.of(baUnit), unmaintainedBattleArmor,
              "BA bucket should contain BATTLE_ARMOR");
        assertEquals(List.of(smallCraftUnit, asfUnit, convFighterUnit), unmaintainedAero,
              "Aero bucket should contain CONV_FIGHTER/ASF/SMALL_CRAFT, sorted by BV desc");
    }

    @Test
    void sortByBattleValue_sortsNullEntityLast() throws Exception {
        Unit bv10 = unitWithUnitTypeAndBV(UnitType.MEK, 10);
        Unit bv30 = unitWithUnitTypeAndBV(UnitType.MEK, 30);

        Unit nullEntity = mock(Unit.class);
        when(nullEntity.getEntity()).thenReturn(null);

        List<Unit> list = new ArrayList<>(List.of(bv10, nullEntity, bv30));

        invokeStaticSortByBattleValue(list);

        assertEquals(List.of(bv30, bv10, nullEntity), list);
    }

    @Test
    void constructor_whenNoUnmaintainedUnits_initializesEmptyTechLists() throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(mock(Person.class)), List.of());

        List<Person> techMeks = getField(techAssignments, "techMeks");
        List<Person> techAero = getField(techAssignments, "techAero");
        List<Person> techBattleArmor = getField(techAssignments, "techBattleArmor");
        List<Person> techMechanic = getField(techAssignments, "techMechanic");

        assertEquals(List.of(), techMeks);
        assertEquals(List.of(), techAero);
        assertEquals(List.of(), techBattleArmor);
        assertEquals(List.of(), techMechanic);
    }

    @Test
    void arrangeTechsIntoBuckets_respectsTwoRoleLimit_andOnlyAddsRolesWithUnmaintainedUnits() throws Exception {
        // Ensure there are unmaintained units for MEK + AERO + BA + VEHICLE
        Unit mekUnit = unitWithUnitTypeAndBV(UnitType.MEK, 1);
        Unit aeroUnit = unitWithUnitTypeAndBV(UnitType.AEROSPACE_FIGHTER, 1);
        Unit baUnit = unitWithUnitTypeAndBV(UnitType.BATTLE_ARMOR, 1);
        Unit vehicleUnit = unitWithUnitTypeAndBV(UnitType.TANK, 1);

        Person mekAero = mock(Person.class);
        when(mekAero.isTechMek()).thenReturn(true);
        when(mekAero.isTechAero()).thenReturn(true);
        when(mekAero.isTechBA()).thenReturn(true); // would be 3rd role, should be blocked
        when(mekAero.isTechMechanic()).thenReturn(true); // would be 3rd/4th role, should be blocked

        Person baMech = mock(Person.class);
        when(baMech.isTechMek()).thenReturn(false);
        when(baMech.isTechAero()).thenReturn(false);
        when(baMech.isTechBA()).thenReturn(true);
        when(baMech.isTechMechanic()).thenReturn(true); // BA + mechanic should both be allowed (2 roles)

        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(mekAero, baMech),
              List.of(mekUnit, aeroUnit, baUnit, vehicleUnit));

        List<Person> techMeks = getField(techAssignments, "techMeks");
        List<Person> techAero = getField(techAssignments, "techAero");
        List<Person> techBattleArmor = getField(techAssignments, "techBattleArmor");
        List<Person> techMechanic = getField(techAssignments, "techMechanic");

        // mekAero goes into MEK and AERO, but not BA or mechanic due to 2-role cap
        // baMech goes into BA and mechanic (2-role cap reached)
        // Note: order can be affected by HashSet iteration in arrangeTechsIntoBuckets, so assert by contains/count.
        assertEquals(1, techMeks.stream().filter(p -> p == mekAero).count());
        assertEquals(1, techAero.stream().filter(p -> p == mekAero).count());
        assertEquals(0, techBattleArmor.stream().filter(p -> p == mekAero).count());
        assertEquals(0, techMechanic.stream().filter(p -> p == mekAero).count());

        assertEquals(1, techBattleArmor.stream().filter(p -> p == baMech).count());
        assertEquals(1, techMechanic.stream().filter(p -> p == baMech).count());
    }

    @Test
    void sortTechList_ordersByAssignedCountAsc_thenTechLevelDesc() throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        Person aPerson = mock(Person.class);
        Person bPerson = mock(Person.class);
        Person cPerson = mock(Person.class);

        when(aPerson.getTechUnits()).thenReturn(new ArrayList<>(List.of(mock(Unit.class)))); // size=1
        when(bPerson.getTechUnits()).thenReturn(new ArrayList<>(List.of(mock(Unit.class)))); // size=1
        when(cPerson.getTechUnits()).thenReturn(new ArrayList<>()); // size=0

        // Tie-breaker on skill level: aPerson higher than bPerson
        stubTechLevel(techAssignments, aPerson, S_TECH_MEK, 5);
        stubTechLevel(techAssignments, bPerson, S_TECH_MEK, 2);
        stubTechLevel(techAssignments, cPerson, S_TECH_MEK, 99); // irrelevant because size=0 should come first

        List<Person> list = new ArrayList<>(List.of(aPerson, bPerson, cPerson));
        invokeSortTechList(techAssignments, list, S_TECH_MEK);

        assertEquals(List.of(cPerson, aPerson, bPerson), list,
              "Expected: smallest assigned count first; for ties, highest tech level first");
    }

    @Test
    void assignUnmaintainedUnitsTechs_assignsUnitsToLeastLoadedTech_andKeepsTechListInBestFirstOrder()
          throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        // Tech 1: 0 units, tech level 3
        Person tech1 = mock(Person.class);
        List<Unit> t1Units = new ArrayList<>();
        when(tech1.getTechUnits()).thenReturn(t1Units);
        stubTechLevel(techAssignments, tech1, S_TECH_MEK, 3);

        // Tech 2: 0 units, tech level 9 (should win tie on first assignment)
        Person tech2 = mock(Person.class);
        List<Unit> t2Units = new ArrayList<>();
        when(tech2.getTechUnits()).thenReturn(t2Units);
        stubTechLevel(techAssignments, tech2, S_TECH_MEK, 9);

        // Units to assign
        Unit unit1 = unitWithUnitTypeAndBV(UnitType.MEK, 10);
        Unit unit2 = unitWithUnitTypeAndBV(UnitType.MEK, 20);
        Unit unit3 = unitWithUnitTypeAndBV(UnitType.MEK, 30);

        // Make setTech mutate the tech's unit list so getTechUnits().size() changes as expected.
        wireSetTechToUpdateTechUnits(unit1);
        wireSetTechToUpdateTechUnits(unit2);
        wireSetTechToUpdateTechUnits(unit3);

        List<Person> techs = new ArrayList<>(List.of(tech1, tech2));
        List<Unit> unmaintained = new ArrayList<>(List.of(unit1, unit2, unit3));

        invokeAssignUnmaintainedUnitsTechs(techAssignments, techs, unmaintained, S_TECH_MEK);

        // Assignment expectation:
        // unit1 -> tech2 (tie on size 0, higher skill wins)
        // now tech2 has 1, tech1 has 0
        // unit2 -> tech1 (least loaded)
        // now both have 1
        // unit3 -> tech2 again (tie on size 1, higher skill wins)
        // now tech2 has 2 (at capacity)
        assertSame(tech2, unit1.getTech());
        assertSame(tech1, unit2.getTech());
        assertSame(tech2, unit3.getTech());

        // Tech list should reflect the remaining eligible techs in "best first" order.
        // After assignments: tech1 has 1, tech2 has 2 and is at capacity, so tech2 is not re-queued and is absent.
        assertEquals(List.of(tech1), techs);
    }

    @Test
    void assignUnmaintainedUnitsTechs_isNoOpWhenTechsOrUnitsEmpty() throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        // Empty techs
        List<Person> techs = new ArrayList<>();
        List<Unit> units = new ArrayList<>(List.of(unitWithUnitTypeAndBV(UnitType.MEK, 1)));

        invokeAssignUnmaintainedUnitsTechs(techAssignments, techs, units, S_TECH_MEK);
        assertEquals(0, units.stream().filter(u -> u.getTech() != null).count());

        // Empty units
        Person tech = mock(Person.class);
        when(tech.getTechUnits()).thenReturn(new ArrayList<>());
        stubTechLevel(techAssignments, tech, S_TECH_MEK, 1);

        List<Person> techs2 = new ArrayList<>(List.of(tech));
        List<Unit> units2 = new ArrayList<>();

        invokeAssignUnmaintainedUnitsTechs(techAssignments, techs2, units2, S_TECH_MEK);
        assertEquals(List.of(tech), techs2, "Tech list should remain unchanged when there are no units");
    }

    private static Unit unitWithUnitTypeAndBV(int unitType, int battleValue) {
        Entity entity = mock(Entity.class);
        when(entity.getUnitType()).thenReturn(unitType);
        when(entity.calculateBattleValue()).thenReturn(battleValue);

        Unit unit = mock(Unit.class);
        when(unit.getTech()).thenReturn(null);
        when(unit.getEntity()).thenReturn(entity);

        return unit;
    }

    private static void wireSetTechToUpdateTechUnits(Unit unit) {
        doAnswer(invocation -> {
            Person tech = invocation.getArgument(0, Person.class);
            List<Unit> assigned = tech.getTechUnits();
            if (assigned != null) {
                assigned.add(unit);
            }
            // Also make unit.getTech() reflect the assignment
            when(unit.getTech()).thenReturn(tech);
            return null;
        }).when(unit).setTech(any(Person.class));
    }

    private static void stubTechLevel(AutomatedTechAssignments techAssignments, Person person, String skillType,
          int level) {
        Skill skill = mock(Skill.class);
        SkillModifierData modData = mock(SkillModifierData.class);

        when(person.getSkill(eq(skillType))).thenReturn(skill);
        when(person.getSkillModifierData()).thenReturn(modData);
        when(skill.getTotalSkillLevel(modData)).thenReturn(level);
    }

    private static void invokeAssignUnmaintainedUnitsTechs(AutomatedTechAssignments techAssignments,
          List<Person> techs, List<Unit> units, String skillType) throws Exception {
        Method assignUnmaintainedUnitsTechs = AutomatedTechAssignments.class.getDeclaredMethod(
              "assignUnmaintainedUnitsTechs", List.class, List.class, String.class);
        assignUnmaintainedUnitsTechs.setAccessible(true);
        assignUnmaintainedUnitsTechs.invoke(techAssignments, techs, units, skillType);
    }

    private static void invokeSortTechList(AutomatedTechAssignments techAssignments, List<Person> techs,
          String skillType)
          throws Exception {
        Method sortTechList = AutomatedTechAssignments.class.getDeclaredMethod("sortTechList",
              List.class,
              String.class);
        sortTechList.setAccessible(true);
        sortTechList.invoke(techAssignments, techs, skillType);
    }

    private static void invokeStaticSortByBattleValue(List<Unit> units) throws Exception {
        Method sortByBattleValue = AutomatedTechAssignments.class.getDeclaredMethod("sortByBattleValue", List.class);
        sortByBattleValue.setAccessible(true);
        sortByBattleValue.invoke(null, units);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String fieldName) throws Exception {
        Field declaredField = target.getClass().getDeclaredField(fieldName);
        declaredField.setAccessible(true);
        return (T) declaredField.get(target);
    }

    @Test
    void assignUnmaintainedUnitsTechs_doesNotAssignMoreThanTwoUnitsToAnyTech_evenAcrossMultipleAssignments()
          throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        // Tech 1 starts with 1 assigned unit already, high skill, so they would win ties if eligible.
        Person tech1 = mock(Person.class);
        List<Unit> t1Units = new ArrayList<>(List.of(mock(Unit.class))); // size=1
        when(tech1.getTechUnits()).thenReturn(t1Units);
        stubTechLevel(techAssignments, tech1, S_TECH_MEK, 10);

        // Tech 2 starts with 0 units, lower skill.
        Person tech2 = mock(Person.class);
        List<Unit> t2Units = new ArrayList<>(); // size=0
        when(tech2.getTechUnits()).thenReturn(t2Units);
        stubTechLevel(techAssignments, tech2, S_TECH_MEK, 1);

        Unit unit1 = unitWithUnitTypeAndBV(UnitType.MEK, 10);
        Unit unit2 = unitWithUnitTypeAndBV(UnitType.MEK, 20);
        Unit unit3 = unitWithUnitTypeAndBV(UnitType.MEK, 30);

        wireSetTechToUpdateTechUnits(unit1);
        wireSetTechToUpdateTechUnits(unit2);
        wireSetTechToUpdateTechUnits(unit3);

        List<Person> techs = new ArrayList<>(List.of(tech1, tech2));
        List<Unit> unmaintained = new ArrayList<>(List.of(unit1, unit2, unit3));

        invokeAssignUnmaintainedUnitsTechs(techAssignments, techs, unmaintained, S_TECH_MEK);

        // Expected:
        // unit1 -> tech2 (least loaded: 0 vs. 1)
        // unit2 -> tech1 (tie on load 1, tech1 higher skill; then tech1 reaches cap=2)
        // unit3 -> tech2 (tech1 is capped and must be skipped)
        assertSame(tech2, unit1.getTech());
        assertSame(tech1, unit2.getTech());
        assertSame(tech2, unit3.getTech());

        // Tech1 must have exactly one new assignment (from 1 -> 2) and must not receive more.
        assertEquals(2, t1Units.size());
        // Both techs hit cap=2, so neither should remain in the returned eligible-tech list.
        assertEquals(List.of(), techs);
        // No unit should be unassigned in this scenario.
        assertEquals(0, techAssignments.getReports().size());
    }

    @Test
    void assignUnmaintainedUnitsTechs_skipsTechsAlreadyAtCapacity() throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        // Capped tech: already has 2 units, should never receive any new unit.
        Person cappedTech = mock(Person.class);
        List<Unit> cappedUnits = new ArrayList<>(List.of(mock(Unit.class), mock(Unit.class))); // size=2
        when(cappedTech.getTechUnits()).thenReturn(cappedUnits);
        stubTechLevel(techAssignments, cappedTech, S_TECH_MEK, 999);

        // Eligible tech: 0 units.
        Person eligibleTech = mock(Person.class);
        List<Unit> eligibleUnits = new ArrayList<>();
        when(eligibleTech.getTechUnits()).thenReturn(eligibleUnits);
        stubTechLevel(techAssignments, eligibleTech, S_TECH_MEK, 1);

        Unit unit1 = unitWithUnitTypeAndBV(UnitType.MEK, 10);
        Unit unit2 = unitWithUnitTypeAndBV(UnitType.MEK, 20);

        wireSetTechToUpdateTechUnits(unit1);
        wireSetTechToUpdateTechUnits(unit2);

        List<Person> techs = new ArrayList<>(List.of(cappedTech, eligibleTech));
        List<Unit> unmaintained = new ArrayList<>(List.of(unit1, unit2));

        invokeAssignUnmaintainedUnitsTechs(techAssignments, techs, unmaintained, S_TECH_MEK);

        assertSame(eligibleTech, unit1.getTech());
        assertSame(eligibleTech, unit2.getTech());
        assertEquals(2, eligibleUnits.size());

        // The capped tech must not change.
        assertEquals(2, cappedUnits.size());
        assertEquals(0, techAssignments.getReports().size());
    }

    @Test
    void assignUnmaintainedUnitsTechs_reportsUnitsThatCannotBeAssignedWhenAllTechsReachCapacity() throws Exception {
        AutomatedTechAssignments techAssignments = new AutomatedTechAssignments(List.of(), List.of());

        // One tech can take at most 2 units; provide 3 units => last one must be reported unassignable.
        Person tech = mock(Person.class);
        List<Unit> techUnits = new ArrayList<>();
        when(tech.getTechUnits()).thenReturn(techUnits);
        stubTechLevel(techAssignments, tech, S_TECH_MEK, 5);

        Unit unit1 = unitWithUnitTypeAndBV(UnitType.MEK, 10);
        Unit unit2 = unitWithUnitTypeAndBV(UnitType.MEK, 20);
        Unit unit3 = unitWithUnitTypeAndBV(UnitType.MEK, 30);

        wireSetTechToUpdateTechUnits(unit1);
        wireSetTechToUpdateTechUnits(unit2);
        wireSetTechToUpdateTechUnits(unit3);

        List<Person> techs = new ArrayList<>(List.of(tech));
        List<Unit> unmaintained = new ArrayList<>(List.of(unit1, unit2, unit3));

        invokeAssignUnmaintainedUnitsTechs(techAssignments, techs, unmaintained, S_TECH_MEK);

        assertSame(tech, unit1.getTech());
        assertSame(tech, unit2.getTech());
        assertNull(unit3.getTech(), "Expected the 3rd unit to remain unassigned due to the 2-unit tech capacity cap");

        assertEquals(2, techUnits.size());
        assertEquals(1, techAssignments.getReports().size(),
              "Expected one 'unable to assign' report when eligible techs run out");
    }
}
