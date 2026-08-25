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
package mekhq.campaign.universe.commandGeneration.ratgen;

import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.force.PlayerForce;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import megamek.common.enums.SkillLevel;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;
import testUtilities.MHQTestUtilities;
import mekhq.campaign.universe.commandGeneration.ratgen.SupportPersonnelGenerator.Result;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SupportPersonnelGeneratorTest {

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

    /**
     * No-op skill generator passed to the package-private overload so the production
     * {@code DefaultSkillGenerator} doesn't try to manipulate a deeply-mocked {@code Person}.
     * Tests verify that {@code newPerson} and {@code recruitPerson} were called the right number
     * of times; the actual skill-rolling implementation is exercised in
     * {@code DefaultSkillGeneratorTest} (not added here).
     */
    private AbstractSkillGenerator stubSkillGen;
    /** Wired by {@link #newCampaignWithUnits(List, int)}; personnel calls now land here, not on the campaign. */
    private ForceHumanResources humanResources;

    @BeforeEach
    void setUp() {
        stubSkillGen = mock(AbstractSkillGenerator.class);
    }

    // ===== Pure conversion =====

    @Test
    void toExperienceLevel_mapsAllSevenSkillTiersToSkillTypeConstants() {
        assertEquals(SkillType.EXP_ULTRA_GREEN, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.ULTRA_GREEN));
        assertEquals(SkillType.EXP_GREEN, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.GREEN));
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.REGULAR));
        assertEquals(SkillType.EXP_VETERAN, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.VETERAN));
        assertEquals(SkillType.EXP_ELITE, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.ELITE));
        assertEquals(SkillType.EXP_HEROIC, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.HEROIC));
        assertEquals(SkillType.EXP_LEGENDARY, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.LEGENDARY));
    }

    @Test
    void toExperienceLevel_nullAndNoneFallBackToRegular() {
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(null));
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.NONE));
    }

    @Test
    void rollRandomSkillLevel_staysWithinUltraGreenToLegendary() {
        // The roll is a 2d6 bell curve with rare escalation into Heroic/Legendary; over many rolls it
        // must never produce NONE or anything outside the Ultra-Green..Legendary band.
        for (int i = 0; i < 5000; i++) {
            SkillLevel level = SupportPersonnelGenerator.rollRandomSkillLevel();
            assertNotNull(level);
            assertTrue(level.getExperienceLevel() >= SkillLevel.ULTRA_GREEN.getExperienceLevel()
                          && level.getExperienceLevel() <= SkillLevel.LEGENDARY.getExperienceLevel(),
                  "rolled " + level + " out of range");
        }
    }

    @Test
    void experienceLevelFor_usesFixedLevelWhenConfigured() {
        // A configured (non-null) level always maps to that tier - no randomness.
        assertEquals(SkillType.EXP_VETERAN, SupportPersonnelGenerator.experienceLevelFor(SkillLevel.VETERAN));
        assertEquals(SkillType.EXP_LEGENDARY, SupportPersonnelGenerator.experienceLevelFor(SkillLevel.LEGENDARY));
    }

    // ===== Edge cases =====

    @Test
    void generate_nullArguments_returnsEmptyResult() {
        Result a = SupportPersonnelGenerator.generate(null, null);
        Result b = SupportPersonnelGenerator.generate(mock(Campaign.class), null);
        Result c = SupportPersonnelGenerator.generate(null, new CommandGenerationOptions());

        for (Result r : List.of(a, b, c)) {
            assertEquals(0, r.totalTechsGenerated());
            assertEquals(0, r.doctorsGenerated());
            assertEquals(0, r.totalAdministratorsGenerated());
            assertEquals(0, r.astechsAdded());
            assertEquals(0, r.medicsAdded());
            assertTrue(r.generatedPersons().isEmpty());
        }
    }

    @Test
    void generate_emptyForce_generatesNoOne() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 0);
        CommandGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.totalTechsGenerated());
        assertEquals(0, result.doctorsGenerated());
        assertEquals(0, result.astechsAdded());
        assertEquals(0, result.medicsAdded());
        verify(humanResources, never()).newPerson(eq(campaign), any());
        verify(humanResources, never()).increaseAsTechPool(eq(campaign), anyInt());
        verify(humanResources, never()).increaseMedicPool(eq(campaign), anyInt());
    }

    // ===== Tech roles =====

    @Test
    void generate_twelveMeks_atFullCoverage_createsTwelveMekTechs() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 12), 12);
        CommandGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(12, result.mekTechsGenerated());
        verify(humanResources, times(12)).newPerson(campaign, PersonnelRole.MEK_TECH);
    }

    @Test
    void generate_twelveMeks_at200Percent_createsTwentyFourMekTechs() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 12), 12);
        CommandGenerationOptions options = baseOptions();
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.MEK_TECH, 200);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(24, result.mekTechsGenerated());
    }

    @Test
    void generate_twelveMeks_atZeroCoverage_skipsMekTechs() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 12), 12);
        CommandGenerationOptions options = baseOptions();
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.MEK_TECH, 0);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.mekTechsGenerated());
        verify(humanResources, never()).newPerson(campaign, PersonnelRole.MEK_TECH);
    }

    @Test
    void generate_appliesSkillLevelFromOptions() {
        Campaign campaign = newCampaignWithUnits(List.of(mekUnit()), 1);
        CommandGenerationOptions options = baseOptions();
        options.getSupportPersonnelSkillLevels().put(PersonnelRole.MEK_TECH, SkillLevel.ELITE);

        // Pin the Mek Tech so the assertion below can name the person it is about. Only this role is
        // fixed to Elite; every other role stays on the "Random" picker and rolls its own level via
        // rollRandomSkillLevel(), which returns Elite on a 12. Counting EXP_ELITE calls across the whole
        // run therefore failed roughly one time in thirty-six per additional support hire, whenever an
        // unrelated person happened to roll a 12.
        Person mekTech = mock(Person.class);
        when(humanResources.newPerson(campaign, PersonnelRole.MEK_TECH)).thenReturn(mekTech);

        SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // We pass a stubbed AbstractSkillGenerator into the package-private overload, so verify it was
        // called for the generated Mek Tech (the role under test) with the converted experience level.
        // This proves the SkillLevel-to-EXP conversion is wired correctly.
        verify(humanResources, times(1)).newPerson(campaign, PersonnelRole.MEK_TECH);
        verify(stubSkillGen).generateSkills(campaign, mekTech, SkillType.EXP_ELITE);
    }

    @Test
    void generate_recruitsEveryGeneratedPerson() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 5), 5);
        CommandGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        verify(humanResources, atLeast(result.totalTechsGenerated()))
              .recruitPerson(eq(campaign), any(), eq(PrisonerStatus.FREE), anyBoolean(), anyBoolean(), anyBoolean());
        assertEquals(result.totalTechsGenerated() + result.doctorsGenerated() + result.totalAdministratorsGenerated(),
              result.generatedPersons().size(), "Pool-mode astechs/medics are NOT counted as Persons in the result");
    }

    // ===== Admin split =====

    @Test
    void generate_adminDemandSplitEquallyAcrossFourRoles() {
        // 400 personnel + 0 techs = 400. ceil(400/20) = 20 admins. Split / 4 = 5 per admin role.
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 400);
        CommandGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(5, result.administratorCommandGenerated());
        assertEquals(5, result.administratorLogisticsGenerated());
        assertEquals(5, result.administratorTransportGenerated());
        assertEquals(5, result.administratorHRGenerated());
        assertEquals(20, result.totalAdministratorsGenerated());
    }

    @Test
    void generate_adminPerRoleCoverage_appliedIndependently() {
        // 400 personnel → 20 admins → 5 per role. With Logistics at 200%, only Logistics scales.
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 400);
        CommandGenerationOptions options = baseOptions();
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.ADMINISTRATOR_LOGISTICS, 200);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(5, result.administratorCommandGenerated());
        assertEquals(10, result.administratorLogisticsGenerated(), "Logistics doubled per coverage");
        assertEquals(5, result.administratorTransportGenerated());
        assertEquals(5, result.administratorHRGenerated());
    }

    // ===== Astechs =====

    @Test
    void generate_astechsOff_skipsAstechCreation() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 4), 4);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateAstechs(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.astechsAdded());
        verify(humanResources, never()).increaseAsTechPool(eq(campaign), anyInt());
        verify(humanResources, never()).newPerson(campaign, PersonnelRole.ASTECH);
    }

    @Test
    void generate_astechsAsPool_callsIncreaseAsTechPool_atSixPerTech() {
        // 4 Meks → 4 Mek Techs → 4 × 6 = 24 astechs in the pool.
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 4), 4);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(24, result.astechsAdded());
        verify(humanResources).increaseAsTechPool(campaign, 24);
        verify(humanResources, never()).newPerson(campaign, PersonnelRole.ASTECH);
    }

    @Test
    void generate_astechsAsPersonnel_createsIndividualAstechPersons() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 2), 2);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // 2 Meks → 2 Mek Techs → 2 × 6 = 12 astech Persons.
        assertEquals(12, result.astechsAdded());
        verify(humanResources, times(12)).newPerson(campaign, PersonnelRole.ASTECH);
        verify(humanResources, never()).increaseAsTechPool(eq(campaign), anyInt());
    }

    @Test
    void generate_astechsSkippedWhenNoTechs() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 100);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.totalTechsGenerated());
        assertEquals(0, result.astechsAdded(), "No techs = no astechs");
        verify(humanResources, never()).increaseAsTechPool(eq(campaign), anyInt());
    }

    // ===== Medics =====

    @Test
    void generate_medicsAsPool_callsIncreaseMedicPool_atFourPerDoctor() {
        // 100 personnel + 0 techs = 100 → 4 doctors. 4 × 4 = 16 medics.
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 100);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateMedics(true);
        options.setMedicsAsPersonnel(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(4, result.doctorsGenerated());
        assertEquals(16, result.medicsAdded());
        verify(humanResources).increaseMedicPool(campaign, 16);
        verify(humanResources, never()).newPerson(campaign, PersonnelRole.MEDIC);
    }

    @Test
    void generate_medicsAsPersonnel_createsIndividualMedicPersons() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 50);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateMedics(true);
        options.setMedicsAsPersonnel(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // 50 personnel → 2 doctors → 2 × 4 = 8 medic Persons.
        assertEquals(2, result.doctorsGenerated());
        assertEquals(8, result.medicsAdded());
        verify(humanResources, times(8)).newPerson(campaign, PersonnelRole.MEDIC);
        verify(humanResources, never()).increaseMedicPool(eq(campaign), anyInt());
    }

    @Test
    void generate_medicsOff_skipsMedicCreation() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 100);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateMedics(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.medicsAdded());
        verify(humanResources, never()).increaseMedicPool(eq(campaign), anyInt());
        verify(humanResources, never()).newPerson(campaign, PersonnelRole.MEDIC);
    }

    // ===== Generated-persons list =====

    @Test
    void generate_personnelModeAstechsAndMedics_appearInGeneratedList() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 2), 24);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(true);
        options.setGenerateMedics(true);
        options.setMedicsAsPersonnel(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // 2 Meks → 2 Mek Techs → 12 astechs (Person mode). 24 + 2 techs = 26 → 2 doctors → 8 medics.
        int expected = result.totalTechsGenerated()
              + result.doctorsGenerated()
              + result.totalAdministratorsGenerated()
              + result.astechsAdded()
              + result.medicsAdded();
        assertEquals(expected, result.generatedPersons().size(),
              "Person-mode astechs and medics must appear in generatedPersons for Stage 7d to flag");
    }

    @Test
    void generate_poolModeAstechsAndMedics_doNotAppearInGeneratedList() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 2), 24);
        CommandGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(false);
        options.setGenerateMedics(true);
        options.setMedicsAsPersonnel(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        int expectedListSize = result.totalTechsGenerated()
              + result.doctorsGenerated()
              + result.totalAdministratorsGenerated();
        assertEquals(expectedListSize, result.generatedPersons().size(),
              "Pool-mode astechs/medics are anonymous pool counts, not Persons");
    }

    // ===== Helpers =====

    private static CommandGenerationOptions baseOptions() {
        CommandGenerationOptions options = new CommandGenerationOptions();
        // Route through the mocked Campaign.getFaction() instead of the real default
        // specifiedFaction (which would resolve through the Ranks singleton — not initialized in
        // unit-test context, and would NPE on getRankSystem). The rank-system-swap path is
        // covered by integration testing in the live MekHQ launch, not here.
        options.setUseSpecifiedFactionToAssignRanks(false);
        return options;
    }

    private Campaign newCampaignWithUnits(List<Unit> units, int personnelCount) {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);
        humanResources = mock(ForceHumanResources.class);
        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(playerForce.getHumanResources()).thenReturn(humanResources);
        when(campaign.getActiveUnits()).thenReturn(units == null ? Collections.emptyList() : units);

        List<Person> personnel = new ArrayList<>();
        for (int i = 0; i < personnelCount; i++) {
            personnel.add(mock(Person.class));
        }
        lenient().when(humanResources.getActivePersonnel(false, false)).thenReturn(personnel);

        CampaignOptions opts = new CampaignOptions();
        opts.set(CampaignOption.MAXIMUM_PATIENTS, 25);
        // These tests measure the tech/doctor counts the generator asks for. HR strain defaults on and
        // would top the roster up on its own, so it is switched off to leave the counts as generated.
        opts.set(CampaignOption.USE_HR_STRAIN, false);
        when(campaign.getCampaignOptions()).thenReturn(opts);

        Faction faction = mock(Faction.class);
        lenient().when(faction.isPirate()).thenReturn(false);
        lenient().when(faction.isMercenary()).thenReturn(false);
        lenient().when(faction.isClan()).thenReturn(false);
        lenient().when(faction.isComStarOrWoB()).thenReturn(false);
        when(playerForce.getFaction()).thenReturn(faction);

        RandomSkillPreferences skillPrefs = mock(RandomSkillPreferences.class);
        lenient().when(campaign.getRandomSkillPreferences()).thenReturn(skillPrefs);

        // Every newPerson(role) returns a fresh mocked Person so verify(...) can count calls.
        lenient().when(humanResources.newPerson(eq(campaign), any(PersonnelRole.class)))
              .thenAnswer(inv -> mock(Person.class));
        // Recruitment always succeeds in these tests.
        lenient().when(humanResources.recruitPerson(eq(campaign), any(Person.class), any(PrisonerStatus.class),
              anyBoolean(), anyBoolean(), anyBoolean()))
              .thenReturn(true);

        return campaign;
    }

    private static Unit unitWith(Consumer<Entity> entityConfig, int fullCrewSize) {
        Unit unit = mock(Unit.class);
        Entity entity = mock(Entity.class);
        entityConfig.accept(entity);
        lenient().when(unit.isMothballed()).thenReturn(false);
        lenient().when(unit.getEntity()).thenReturn(entity);
        lenient().when(unit.getFullCrewSize()).thenReturn(fullCrewSize);
        return unit;
    }

    private static Unit mekUnit() {
        return unitWith(e -> when(e.isMek()).thenReturn(true), 1);
    }

    private static <T> List<T> repeat(java.util.function.Supplier<T> supplier, int count) {
        List<T> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(supplier.get());
        }
        return out;
    }

    @Test
    void countActiveByRoleCountsOnlyMatchingPrimaryRole() {
        // Drives support-personnel reconciliation: generateRole subtracts this count from its target so
        // a re-run tops up only the shortfall instead of duplicating existing staff.
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
        Campaign campaign = MHQTestUtilities.getTestCampaign();
        recruitWithRole(campaign, PersonnelRole.MEK_TECH);
        recruitWithRole(campaign, PersonnelRole.MEK_TECH);
        recruitWithRole(campaign, PersonnelRole.DOCTOR);

        assertEquals(2, SupportPersonnelGenerator.countActiveByRole(campaign, PersonnelRole.MEK_TECH));
        assertEquals(1, SupportPersonnelGenerator.countActiveByRole(campaign, PersonnelRole.DOCTOR));
        assertEquals(0, SupportPersonnelGenerator.countActiveByRole(campaign, PersonnelRole.ADMINISTRATOR_HR));
    }

    private static void recruitWithRole(Campaign campaign, PersonnelRole role) {
        Person person = campaign.getPlayerForce().getHumanResources().newPerson(campaign, role);
        campaign.getPlayerForce().getHumanResources().recruitPerson(campaign, person, PrisonerStatus.FREE, true, true, true);
    }
}
