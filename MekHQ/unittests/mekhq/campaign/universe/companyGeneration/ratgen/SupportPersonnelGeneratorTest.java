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
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.generator.AbstractSkillGenerator;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.randomEvents.prisoners.enums.PrisonerStatus;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.companyGeneration.ratgen.SupportPersonnelGenerator.Result;
import mekhq.campaign.universe.enums.CompanyGenerationMethod;
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

    @BeforeEach
    void setUp() {
        stubSkillGen = mock(AbstractSkillGenerator.class);
    }

    // ===== Pure conversion =====

    @Test
    void toExperienceLevel_mapsAllFiveSkillTiersToSkillTypeConstants() {
        assertEquals(SkillType.EXP_ULTRA_GREEN, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.ULTRA_GREEN));
        assertEquals(SkillType.EXP_GREEN, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.GREEN));
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.REGULAR));
        assertEquals(SkillType.EXP_VETERAN, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.VETERAN));
        assertEquals(SkillType.EXP_ELITE, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.ELITE));
    }

    @Test
    void toExperienceLevel_nullAndOutOfRangeFallBackToRegular() {
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(null));
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.NONE));
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.HEROIC));
        assertEquals(SkillType.EXP_REGULAR, SupportPersonnelGenerator.toExperienceLevel(SkillLevel.LEGENDARY));
    }

    // ===== Edge cases =====

    @Test
    void generate_nullArguments_returnsEmptyResult() {
        Result a = SupportPersonnelGenerator.generate(null, null);
        Result b = SupportPersonnelGenerator.generate(mock(Campaign.class), null);
        Result c = SupportPersonnelGenerator.generate(null, new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED));

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
        CompanyGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.totalTechsGenerated());
        assertEquals(0, result.doctorsGenerated());
        assertEquals(0, result.astechsAdded());
        assertEquals(0, result.medicsAdded());
        verify(campaign, never()).newPerson(any());
        verify(campaign, never()).increaseAsTechPool(anyInt());
        verify(campaign, never()).increaseMedicPool(anyInt());
    }

    // ===== Tech roles =====

    @Test
    void generate_twelveMeks_atFullCoverage_createsTwelveMekTechs() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 12), 12);
        CompanyGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(12, result.mekTechsGenerated());
        verify(campaign, times(12)).newPerson(PersonnelRole.MEK_TECH);
    }

    @Test
    void generate_twelveMeks_at200Percent_createsTwentyFourMekTechs() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 12), 12);
        CompanyGenerationOptions options = baseOptions();
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.MEK_TECH, 200);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(24, result.mekTechsGenerated());
    }

    @Test
    void generate_twelveMeks_atZeroCoverage_skipsMekTechs() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 12), 12);
        CompanyGenerationOptions options = baseOptions();
        options.getSupportPersonnelCoveragePercents().put(PersonnelRole.MEK_TECH, 0);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.mekTechsGenerated());
        verify(campaign, never()).newPerson(PersonnelRole.MEK_TECH);
    }

    @Test
    void generate_appliesSkillLevelFromOptions() {
        Campaign campaign = newCampaignWithUnits(List.of(mekUnit()), 1);
        CompanyGenerationOptions options = baseOptions();
        options.getSupportPersonnelSkillLevels().put(PersonnelRole.MEK_TECH, SkillLevel.ELITE);

        SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // We pass a stubbed AbstractSkillGenerator into the package-private overload, so verify
        // it was called once per generated Mek Tech (the role under test) with the converted
        // experience level. This proves the SkillLevel-to-EXP conversion is wired correctly.
        verify(campaign, times(1)).newPerson(PersonnelRole.MEK_TECH);
        verify(stubSkillGen, times(1)).generateSkills(eq(campaign), any(), eq(SkillType.EXP_ELITE));
    }

    @Test
    void generate_recruitsEveryGeneratedPerson() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 5), 5);
        CompanyGenerationOptions options = baseOptions();

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        verify(campaign, atLeast(result.totalTechsGenerated()))
              .recruitPerson(any(), eq(PrisonerStatus.FREE), anyBoolean(), anyBoolean());
        assertEquals(result.totalTechsGenerated() + result.doctorsGenerated() + result.totalAdministratorsGenerated(),
              result.generatedPersons().size(), "Pool-mode astechs/medics are NOT counted as Persons in the result");
    }

    // ===== Admin split =====

    @Test
    void generate_adminDemandSplitEquallyAcrossFourRoles() {
        // 400 personnel + 0 techs = 400. ceil(400/20) = 20 admins. Split / 4 = 5 per admin role.
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 400);
        CompanyGenerationOptions options = baseOptions();

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
        CompanyGenerationOptions options = baseOptions();
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
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateAstechs(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.astechsAdded());
        verify(campaign, never()).increaseAsTechPool(anyInt());
        verify(campaign, never()).newPerson(PersonnelRole.ASTECH);
    }

    @Test
    void generate_astechsAsPool_callsIncreaseAsTechPool_atSixPerTech() {
        // 4 Meks → 4 Mek Techs → 4 × 6 = 24 astechs in the pool.
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 4), 4);
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(24, result.astechsAdded());
        verify(campaign).increaseAsTechPool(24);
        verify(campaign, never()).newPerson(PersonnelRole.ASTECH);
    }

    @Test
    void generate_astechsAsPersonnel_createsIndividualAstechPersons() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 2), 2);
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);
        options.setAstechsAsPersonnel(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // 2 Meks → 2 Mek Techs → 2 × 6 = 12 astech Persons.
        assertEquals(12, result.astechsAdded());
        verify(campaign, times(12)).newPerson(PersonnelRole.ASTECH);
        verify(campaign, never()).increaseAsTechPool(anyInt());
    }

    @Test
    void generate_astechsSkippedWhenNoTechs() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 100);
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateAstechs(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.totalTechsGenerated());
        assertEquals(0, result.astechsAdded(), "No techs = no astechs");
        verify(campaign, never()).increaseAsTechPool(anyInt());
    }

    // ===== Medics =====

    @Test
    void generate_medicsAsPool_callsIncreaseMedicPool_atFourPerDoctor() {
        // 100 personnel + 0 techs = 100 → 4 doctors. 4 × 4 = 16 medics.
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 100);
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateMedics(true);
        options.setMedicsAsPersonnel(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(4, result.doctorsGenerated());
        assertEquals(16, result.medicsAdded());
        verify(campaign).increaseMedicPool(16);
        verify(campaign, never()).newPerson(PersonnelRole.MEDIC);
    }

    @Test
    void generate_medicsAsPersonnel_createsIndividualMedicPersons() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 50);
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateMedics(true);
        options.setMedicsAsPersonnel(true);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        // 50 personnel → 2 doctors → 2 × 4 = 8 medic Persons.
        assertEquals(2, result.doctorsGenerated());
        assertEquals(8, result.medicsAdded());
        verify(campaign, times(8)).newPerson(PersonnelRole.MEDIC);
        verify(campaign, never()).increaseMedicPool(anyInt());
    }

    @Test
    void generate_medicsOff_skipsMedicCreation() {
        Campaign campaign = newCampaignWithUnits(Collections.emptyList(), 100);
        CompanyGenerationOptions options = baseOptions();
        options.setGenerateMedics(false);

        Result result = SupportPersonnelGenerator.generate(campaign, options, stubSkillGen);

        assertEquals(0, result.medicsAdded());
        verify(campaign, never()).increaseMedicPool(anyInt());
        verify(campaign, never()).newPerson(PersonnelRole.MEDIC);
    }

    // ===== Generated-persons list =====

    @Test
    void generate_personnelModeAstechsAndMedics_appearInGeneratedList() {
        Campaign campaign = newCampaignWithUnits(repeat(SupportPersonnelGeneratorTest::mekUnit, 2), 24);
        CompanyGenerationOptions options = baseOptions();
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
        CompanyGenerationOptions options = baseOptions();
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

    private static CompanyGenerationOptions baseOptions() {
        CompanyGenerationOptions options = new CompanyGenerationOptions(CompanyGenerationMethod.RULESET_BASED);
        // Route through the mocked Campaign.getFaction() instead of the real default
        // specifiedFaction (which would resolve through the Ranks singleton — not initialized in
        // unit-test context, and would NPE on getRankSystem). The rank-system-swap path is
        // covered by integration testing in the live MekHQ launch, not here.
        options.setUseSpecifiedFactionToAssignRanks(false);
        return options;
    }

    private Campaign newCampaignWithUnits(List<Unit> units, int personnelCount) {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getActiveUnits()).thenReturn(units == null ? Collections.emptyList() : units);

        List<Person> personnel = new ArrayList<>();
        for (int i = 0; i < personnelCount; i++) {
            personnel.add(mock(Person.class));
        }
        lenient().when(campaign.getActivePersonnel(false, false)).thenReturn(personnel);

        CampaignOptions opts = mock(CampaignOptions.class);
        lenient().when(opts.getMaximumPatients()).thenReturn(25);
        when(campaign.getCampaignOptions()).thenReturn(opts);

        Faction faction = mock(Faction.class);
        lenient().when(faction.isPirate()).thenReturn(false);
        lenient().when(faction.isMercenary()).thenReturn(false);
        lenient().when(faction.isClan()).thenReturn(false);
        lenient().when(faction.isComStarOrWoB()).thenReturn(false);
        when(campaign.getFaction()).thenReturn(faction);

        RandomSkillPreferences skillPrefs = mock(RandomSkillPreferences.class);
        lenient().when(campaign.getRandomSkillPreferences()).thenReturn(skillPrefs);

        // Every newPerson(role) returns a fresh mocked Person so verify(...) can count calls.
        lenient().when(campaign.newPerson(any(PersonnelRole.class)))
              .thenAnswer(inv -> mock(Person.class));
        // Recruitment always succeeds in these tests.
        lenient().when(campaign.recruitPerson(any(Person.class), any(PrisonerStatus.class), anyBoolean(), anyBoolean()))
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
}
