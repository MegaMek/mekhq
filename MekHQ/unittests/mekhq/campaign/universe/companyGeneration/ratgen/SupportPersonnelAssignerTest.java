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
package mekhq.campaign.universe.companyGeneration.ratgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
import mekhq.campaign.universe.enums.TechAssignmentSortFactor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SupportPersonnelAssignerTest {

    /**
     * Loggers silenced for the whole class. {@code RandomOriginOptions}'s constructor logs at
     * ERROR with a stack trace when the universe isn't loaded, which it never is in unit tests —
     * suppressing keeps {@code mekhq.log} clean of expected-noise stack traces.
     */
    private static final String[] SILENCED_LOGGERS = { "mekhq.campaign.RandomOriginOptions" };
    private static final Level[] PREVIOUS_LEVELS = new Level[SILENCED_LOGGERS.length];

    @BeforeAll
    static void silenceExpectedErrorLoggers() {
        for (int i = 0; i < SILENCED_LOGGERS.length; i++) {
            PREVIOUS_LEVELS[i] = LogManager.getLogger(SILENCED_LOGGERS[i]).getLevel();
            Configurator.setLevel(SILENCED_LOGGERS[i], Level.OFF);
        }
    }

    @AfterAll
    static void restoreLoggers() {
        for (int i = 0; i < SILENCED_LOGGERS.length; i++) {
            Configurator.setLevel(SILENCED_LOGGERS[i], PREVIOUS_LEVELS[i]);
        }
    }

    // ===== Gate =====

    @Test
    void assign_disabledByToggle_returnsZeroAndAssignsNothing() {
        Campaign campaign = newCampaign();
        Unit mek = mekUnit(60, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(mek));
        CompanyGenerationOptions options = baseOptions();
        options.setAssignTechsToUnits(false);

        int assigned = SupportPersonnelAssigner.assign(campaign, options,
              resultWithTechs(List.of(mekTech(SkillLevel.REGULAR, 8))));

        assertEquals(0, assigned);
        verify(mek, never()).setTech(any());
    }

    @Test
    void assign_nullArguments_returnZero() {
        assertEquals(0, SupportPersonnelAssigner.assign(null, null, null));
        assertEquals(0, SupportPersonnelAssigner.assign(mock(Campaign.class), null, null));
        assertEquals(0, SupportPersonnelAssigner.assign(mock(Campaign.class), baseOptions(), null));
    }

    @Test
    void assign_noUnits_returnsZero() {
        Campaign campaign = newCampaign();
        when(campaign.getActiveUnits()).thenReturn(Collections.emptyList());
        CompanyGenerationOptions options = baseOptions();

        int assigned = SupportPersonnelAssigner.assign(campaign, options,
              resultWithTechs(List.of(mekTech(SkillLevel.REGULAR, 8))));

        assertEquals(0, assigned);
    }

    @Test
    void assign_noTechs_returnsZero() {
        Campaign campaign = newCampaign();
        Unit mek = mekUnit(60, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(mek));
        CompanyGenerationOptions options = baseOptions();

        int assigned = SupportPersonnelAssigner.assign(campaign, options,
              resultWithTechs(Collections.emptyList()));

        assertEquals(0, assigned);
    }

    // ===== Single-tech basic assignment =====

    @Test
    void assign_oneMekOneTech_assignsTech() {
        Campaign campaign = newCampaign();
        Unit mek = mekUnit(60, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(mek));
        Person tech = mekTech(SkillLevel.REGULAR, 8);

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(), resultWithTechs(List.of(tech)));

        assertEquals(1, assigned);
        verify(mek).setTech(tech);
    }

    @Test
    void assign_skipsUnitsThatAlreadyHaveATech() {
        Campaign campaign = newCampaign();
        Unit mek = mekUnit(60, mock(Person.class));
        when(mek.getTech()).thenReturn(mock(Person.class));  // pre-assigned
        when(campaign.getActiveUnits()).thenReturn(List.of(mek));

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(mekTech(SkillLevel.REGULAR, 8))));

        assertEquals(0, assigned);
        verify(mek, never()).setTech(any());
    }

    @Test
    void assign_skipsMothballedUnits() {
        Campaign campaign = newCampaign();
        Unit mek = mekUnit(60, mock(Person.class));
        when(mek.isMothballed()).thenReturn(true);
        when(campaign.getActiveUnits()).thenReturn(List.of(mek));

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(mekTech(SkillLevel.REGULAR, 8))));

        assertEquals(0, assigned);
    }

    @Test
    void assign_skipsZeroMaintenanceUnits() {
        Campaign campaign = newCampaign();
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(mock(Entity.class));
        when(unit.getMaintenanceTime()).thenReturn(0);
        when(campaign.getActiveUnits()).thenReturn(List.of(unit));

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(mekTech(SkillLevel.REGULAR, 8))));

        assertEquals(0, assigned);
    }

    // ===== Per-type routing =====

    @Test
    void assign_routesMechanicToVehiclesNotMekTechs() {
        Campaign campaign = newCampaign();
        Unit tank = tankUnit(50, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(tank));
        Person mekTech = mekTech(SkillLevel.REGULAR, 8);
        Person mechanic = mechanic(SkillLevel.REGULAR, 8);

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(mekTech, mechanic)));

        assertEquals(1, assigned);
        verify(tank).setTech(mechanic);
        verify(tank, never()).setTech(mekTech);
    }

    @Test
    void assign_skipsUnitWithNoMatchingPool() {
        // A Mek with only a Mechanic available: no MEK_TECH in the pool, unit goes unassigned.
        Campaign campaign = newCampaign();
        Unit mek = mekUnit(60, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(mek));

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(mechanic(SkillLevel.REGULAR, 8))));

        assertEquals(0, assigned);
        verify(mek, never()).setTech(any());
    }

    // ===== Capacity cap =====

    @Test
    void assign_techRespects480MinuteDailyCap() {
        // One tech, three heavy Meks (75 min each = 225 total, well under 480).
        Campaign campaign = newCampaign();
        Unit a = mekUnit(75, mock(Person.class));
        Unit b = mekUnit(75, mock(Person.class));
        Unit c = mekUnit(75, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(a, b, c));
        Person tech = mekTech(SkillLevel.REGULAR, 8);

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(), resultWithTechs(List.of(tech)));

        assertEquals(3, assigned, "One Regular tech can take 6 heavy Meks before hitting 480/day cap");
    }

    @Test
    void assign_techAtCapWontTakeNewUnit() {
        // One tech, seven Assault Meks (90 min each). 5 fit (450), the 6th would push to 540.
        Campaign campaign = newCampaign();
        List<Unit> units = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            units.add(mekUnit(90, mock(Person.class)));
        }
        when(campaign.getActiveUnits()).thenReturn(units);

        // Stateful tech: track accumulated minutes so getMaintenanceTimeUsing returns the running
        // total after each setTech() call.
        Person tech = mekTech(SkillLevel.REGULAR, 8);
        int[] timeUsed = {0};
        lenient().when(tech.getMaintenanceTimeUsing()).thenAnswer(inv -> timeUsed[0]);
        for (Unit u : units) {
            lenient().doAnswer(inv -> { timeUsed[0] += 90; return null; }).when(u).setTech(tech);
        }

        int assigned = SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(tech)));

        // 5 × 90 = 450; one more would be 540 > 480 → 5 assigned, 2 skipped.
        assertEquals(5, assigned);
    }

    // ===== Sort grid =====

    @Test
    void assign_pilotRankDescending_officersGetTechsFirst() {
        Campaign campaign = newCampaign();
        // Two units, one tech. The pilot with the higher rank should win.
        Person highRank = mockPerson(SkillLevel.REGULAR, 30);
        Person lowRank = mockPerson(SkillLevel.REGULAR, 5);
        Unit officerMek = mekUnit(60, highRank);
        Unit gruntMek = mekUnit(60, lowRank);
        when(campaign.getActiveUnits()).thenReturn(List.of(gruntMek, officerMek));  // intentionally out-of-order

        CompanyGenerationOptions options = baseOptions();
        // Already PILOT_RANK descending by default, just being explicit
        options.setTechAssignmentPrimarySort(TechAssignmentSortFactor.PILOT_RANK);
        options.setTechAssignmentPrimaryDescending(true);

        Person tech = mekTech(SkillLevel.REGULAR, 8);
        // Make sure tech only fits one unit: 2 × 300 = 600 > 480 cap. Wire stateful setTech so the
        // tech's running maintenance-time actually grows after each assignment.
        when(officerMek.getMaintenanceTime()).thenReturn(300);
        when(gruntMek.getMaintenanceTime()).thenReturn(300);
        wireStatefulSetTech(officerMek, tech);
        wireStatefulSetTech(gruntMek, tech);

        SupportPersonnelAssigner.assign(campaign, options, resultWithTechs(List.of(tech)));

        verify(officerMek).setTech(tech);
        verify(gruntMek, never()).setTech(any());
    }

    @Test
    void assign_unitWeightDescending_heavierUnitsGoFirst() {
        Campaign campaign = newCampaign();
        Unit lightMek = mockMekUnit(45, /*weight class*/ 1);
        Unit assaultMek = mockMekUnit(90, /*weight class*/ 4);
        when(campaign.getActiveUnits()).thenReturn(List.of(lightMek, assaultMek));

        CompanyGenerationOptions options = baseOptions();
        options.setTechAssignmentPrimarySort(TechAssignmentSortFactor.UNIT_WEIGHT);
        options.setTechAssignmentPrimaryDescending(true);
        options.setTechAssignmentSecondarySort(TechAssignmentSortFactor.NONE);
        options.setTechAssignmentTertiarySort(TechAssignmentSortFactor.NONE);

        // Only one tech with enough capacity for one unit (2 × 300 = 600 > 480 cap).
        Person tech = mekTech(SkillLevel.REGULAR, 8);
        when(lightMek.getMaintenanceTime()).thenReturn(300);
        when(assaultMek.getMaintenanceTime()).thenReturn(300);
        wireStatefulSetTech(lightMek, tech);
        wireStatefulSetTech(assaultMek, tech);

        SupportPersonnelAssigner.assign(campaign, options, resultWithTechs(List.of(tech)));

        verify(assaultMek).setTech(tech);
        verify(lightMek, never()).setTech(any());
    }

    @Test
    void assign_pilotSkillAscending_picksLowestSkillFirst() {
        Campaign campaign = newCampaign();
        Person elitePilot = mockPerson(SkillLevel.ELITE, 8);
        Person greenPilot = mockPerson(SkillLevel.GREEN, 8);
        Unit eliteMek = mekUnit(60, elitePilot);
        Unit greenMek = mekUnit(60, greenPilot);
        when(campaign.getActiveUnits()).thenReturn(List.of(eliteMek, greenMek));

        CompanyGenerationOptions options = baseOptions();
        options.setTechAssignmentPrimarySort(TechAssignmentSortFactor.PILOT_SKILL);
        options.setTechAssignmentPrimaryDescending(false);  // ascending: low skill first
        options.setTechAssignmentSecondarySort(TechAssignmentSortFactor.NONE);
        options.setTechAssignmentTertiarySort(TechAssignmentSortFactor.NONE);

        Person tech = mekTech(SkillLevel.REGULAR, 8);
        when(eliteMek.getMaintenanceTime()).thenReturn(300);
        when(greenMek.getMaintenanceTime()).thenReturn(300);
        wireStatefulSetTech(eliteMek, tech);
        wireStatefulSetTech(greenMek, tech);

        SupportPersonnelAssigner.assign(campaign, options, resultWithTechs(List.of(tech)));

        verify(greenMek).setTech(tech);
        verify(eliteMek, never()).setTech(any());
    }

    @Test
    void assign_allSlotsNone_arbitraryOrder() {
        Campaign campaign = newCampaign();
        Unit a = mekUnit(60, mock(Person.class));
        Unit b = mekUnit(60, mock(Person.class));
        when(campaign.getActiveUnits()).thenReturn(List.of(a, b));

        CompanyGenerationOptions options = baseOptions();
        options.setTechAssignmentPrimarySort(TechAssignmentSortFactor.NONE);
        options.setTechAssignmentSecondarySort(TechAssignmentSortFactor.NONE);
        options.setTechAssignmentTertiarySort(TechAssignmentSortFactor.NONE);

        Person tech = mekTech(SkillLevel.REGULAR, 8);

        int assigned = SupportPersonnelAssigner.assign(campaign, options, resultWithTechs(List.of(tech)));

        // Both fit (60+60 = 120 < 480), and the order doesn't matter — just confirm both got tech.
        assertEquals(2, assigned);
        verify(a).setTech(tech);
        verify(b).setTech(tech);
    }

    // ===== Tech quality ordering =====

    @Test
    void assign_betterTechsGoToHigherPriorityUnits() {
        Campaign campaign = newCampaign();
        Person elitePilot = mockPerson(SkillLevel.ELITE, 30);
        Person greenPilot = mockPerson(SkillLevel.GREEN, 5);
        Unit officerMek = mekUnit(60, elitePilot);
        Unit gruntMek = mekUnit(60, greenPilot);
        when(campaign.getActiveUnits()).thenReturn(List.of(gruntMek, officerMek));

        Person eliteTech = mekTech(SkillLevel.ELITE, 12);
        Person greenTech = mekTech(SkillLevel.GREEN, 8);

        // Each unit is 250 min so a single tech can only take one (250 × 2 = 500 > 480 cap).
        when(officerMek.getMaintenanceTime()).thenReturn(250);
        when(gruntMek.getMaintenanceTime()).thenReturn(250);
        wireStatefulSetTech(officerMek, eliteTech);
        wireStatefulSetTech(officerMek, greenTech);
        wireStatefulSetTech(gruntMek, eliteTech);
        wireStatefulSetTech(gruntMek, greenTech);

        SupportPersonnelAssigner.assign(campaign, baseOptions(),
              resultWithTechs(List.of(greenTech, eliteTech)));

        verify(officerMek).setTech(eliteTech);
        verify(gruntMek).setTech(greenTech);
    }

    // ===== Helpers =====

    private static CompanyGenerationOptions baseOptions() {
        return new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
    }

    private Campaign newCampaign() {
        Campaign campaign = mock(Campaign.class);
        return campaign;
    }

    private static SupportPersonnelGenerator.Result resultWithTechs(List<Person> techs) {
        return new SupportPersonnelGenerator.Result(
              countOf(techs, PersonnelRole.MEK_TECH),
              countOf(techs, PersonnelRole.MECHANIC),
              countOf(techs, PersonnelRole.AERO_TEK),
              countOf(techs, PersonnelRole.BA_TECH),
              0, 0, 0, 0, 0, 0, 0,
              new ArrayList<>(techs));
    }

    private static int countOf(List<Person> techs, PersonnelRole role) {
        return (int) techs.stream().filter(p -> p.getPrimaryRole() == role).count();
    }

    private static Person mockPerson(SkillLevel skill, int rank) {
        Person p = mock(Person.class);
        lenient().when(p.getRankNumeric()).thenReturn(rank);
        lenient().when(p.getSkillLevel(any(Campaign.class), anyBoolean())).thenReturn(skill);
        return p;
    }

    private static Person mekTech(SkillLevel skill, int rank) {
        return statefulTech(skill, rank, PersonnelRole.MEK_TECH);
    }

    private static Person mechanic(SkillLevel skill, int rank) {
        return statefulTech(skill, rank, PersonnelRole.MECHANIC);
    }

    /**
     * Builds a tech mock whose {@code getMaintenanceTimeUsing()} starts at 0 and grows by the
     * maintenance time of every Unit passed to {@code setTech(this)}. Required for tests that
     * want capacity exhaustion to actually exhaust — the default Mockito stub would return 0
     * indefinitely and let one tech absorb every unit in the test.
     */
    private static Person statefulTech(SkillLevel skill, int rank, PersonnelRole role) {
        Person p = mockPerson(skill, rank);
        lenient().when(p.getPrimaryRole()).thenReturn(role);
        int[] timeUsed = {0};
        lenient().when(p.getMaintenanceTimeUsing()).thenAnswer(inv -> timeUsed[0]);
        TECH_USAGE_TRACKER.put(p, timeUsed);
        return p;
    }

    /** Shared usage tracker so {@link #wireStatefulSetTech} can find the counter for a given tech. */
    private static final java.util.IdentityHashMap<Person, int[]> TECH_USAGE_TRACKER = new java.util.IdentityHashMap<>();

    /**
     * Wires a unit's {@code setTech(tech)} call to add the unit's maintenance time onto the tech's
     * accumulator so subsequent {@code getMaintenanceTimeUsing()} calls return the running total.
     * Call this for every unit-tech pairing that needs capacity-cap behavior under test.
     */
    private static void wireStatefulSetTech(Unit unit, Person tech) {
        int[] counter = TECH_USAGE_TRACKER.get(tech);
        if (counter == null) {
            return;
        }
        lenient().doAnswer(inv -> {
            counter[0] += unit.getMaintenanceTime();
            return null;
        }).when(unit).setTech(tech);
    }

    private static Unit mekUnit(int maintenanceMinutes, Person commander) {
        return unitWith(e -> when(e.isMek()).thenReturn(true), maintenanceMinutes, commander, /*weight*/2);
    }

    private static Unit mockMekUnit(int maintenanceMinutes, int weightClass) {
        return unitWith(e -> when(e.isMek()).thenReturn(true), maintenanceMinutes, mock(Person.class), weightClass);
    }

    private static Unit tankUnit(int maintenanceMinutes, Person commander) {
        return unitWith(e -> when(e.isVehicle()).thenReturn(true), maintenanceMinutes, commander, /*weight*/2);
    }

    private static Unit unitWith(Consumer<Entity> entityConfig, int maintenanceMinutes,
          Person commander, int weightClass) {
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        entityConfig.accept(entity);
        lenient().when(entity.getWeightClass()).thenReturn(weightClass);
        lenient().when(unit.getEntity()).thenReturn(entity);
        lenient().when(unit.getMaintenanceTime()).thenReturn(maintenanceMinutes);
        lenient().when(unit.isMothballed()).thenReturn(false);
        lenient().when(unit.getTech()).thenReturn(null);
        lenient().when(unit.getCommander()).thenReturn(commander);
        return unit;
    }
}
