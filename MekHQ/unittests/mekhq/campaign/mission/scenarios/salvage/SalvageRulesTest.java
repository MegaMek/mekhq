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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import megamek.common.units.Entity;
import megamek.common.units.Mek;
import megamek.common.units.Tank;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import mekhq.gui.dialog.camOpsSalvage.SalvageRecoveryConsole;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Covers the behavior of each salvage system (see {@link AbstractSalvage}): the questions callers ask of it, which
 * units may salvage, the settlement it creates, and how it resolves a scenario's salvage.
 */
class SalvageRulesTest {
    private static final AbstractSalvage LEGACY = new LegacySalvage();
    private static final AbstractSalvage STRICT = new CamOpsStrictSalvage();
    private static final AbstractSalvage REVISED = new CamOpsRevisedSalvage();
    private static final AbstractSalvage CHAOS = new ChaosCampaignSalvage();
    private static final AbstractSalvage MEKHQ = new MekHQSalvage();

    private static Formation formationOfType(FormationType formationType) {
        Formation formation = mock(Formation.class);
        when(formation.getFormationType()).thenReturn(formationType);
        return formation;
    }

    private static AbstractContract contract() {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getSalvageRightsMultiplier()).thenReturn(0.5);
        return contract;
    }

    @Nested
    class SalvageSystems {
        @Test
        void eachSystemIsBackedByItsOwnRules() {
            assertInstanceOf(LegacySalvage.class, SalvageSystem.LEGACY.getSalvage());
            assertInstanceOf(CamOpsStrictSalvage.class, SalvageSystem.CAM_OPS_STRICT.getSalvage());
            assertInstanceOf(CamOpsRevisedSalvage.class, SalvageSystem.CAM_OPS_REVISED.getSalvage());
            assertInstanceOf(ChaosCampaignSalvage.class, SalvageSystem.CHAOS_CAMPAIGN.getSalvage());
            assertInstanceOf(MekHQSalvage.class, SalvageSystem.MEKHQ.getSalvage());
        }

        @ParameterizedTest
        @EnumSource(SalvageSystem.class)
        void rulesAreShared(SalvageSystem salvageSystem) {
            assertSame(salvageSystem.getSalvage(), salvageSystem.getSalvage());
        }

        @ParameterizedTest
        @EnumSource(SalvageSystem.class)
        void lookupNameRoundTrips(SalvageSystem salvageSystem) {
            assertEquals(salvageSystem, SalvageSystem.fromLookupName(salvageSystem.getLookupName()));
        }

        @Test
        void unknownOrMissingLookupNameFallsBackToLegacy() {
            assertEquals(SalvageSystem.LEGACY, SalvageSystem.fromLookupName("NOT_A_SYSTEM"));
            assertEquals(SalvageSystem.LEGACY, SalvageSystem.fromLookupName(""));
            assertEquals(SalvageSystem.LEGACY, SalvageSystem.fromLookupName(null));
        }

        @ParameterizedTest
        @EnumSource(SalvageSystem.class)
        void labelAndTooltipAreLocalized(SalvageSystem salvageSystem) {
            // A missing resource key comes back wrapped in '!'
            assertFalse(salvageSystem.getLabel().isBlank());
            assertFalse(salvageSystem.getLabel().startsWith("!"), salvageSystem.getLabel());
            assertFalse(salvageSystem.getTooltip().isBlank());
            assertFalse(salvageSystem.getTooltip().startsWith("!"), salvageSystem.getTooltip());
            assertEquals(salvageSystem.getLabel(), salvageSystem.toString());
        }
    }

    @Nested
    class Capabilities {
        @ParameterizedTest(name = "{0}")
        @CsvSource({
              // system,        operations, wizard, combat, multiple, purchases
              "LEGACY,          false,      true,   false,  false,    false",
              "CAM_OPS_STRICT,  true,       false,  false,  false,    false",
              "CAM_OPS_REVISED, true,       false,  true,   true,     false",
              "CHAOS_CAMPAIGN,  false,      false,  false,  false,    true",
              "MEKHQ,           true,       false,  true,   true,     true"
        })
        void systemCapabilities(SalvageSystem salvageSystem, boolean isUseSalvageOperations,
              boolean isClaimedInWizard, boolean isCombatAllowed, boolean isMultipleAllowed, boolean isPurchases) {
            AbstractSalvage rules = salvageSystem.getSalvage();

            assertEquals(isUseSalvageOperations, rules.isUseSalvageOperations(), "salvage operations");
            assertEquals(isClaimedInWizard, rules.isSalvageClaimedInResolveWizard(), "claimed in wizard");
            assertEquals(isCombatAllowed, rules.isSalvageFormationCombatAllowed(), "salvage formation combat");
            assertEquals(isMultipleAllowed, rules.isMultipleSalvagePerUnitAllowed(), "multiple salvage per unit");
            assertEquals(isPurchases, rules.createSettlement(contract()).isKeptSalvageBought(), "purchases");
        }

        @Test
        void onlyTheLegacySystemClaimsSalvageInTheWizard() {
            for (SalvageSystem salvageSystem : SalvageSystem.values()) {
                assertEquals(salvageSystem == SalvageSystem.LEGACY,
                      salvageSystem.getSalvage().isSalvageClaimedInResolveWizard(), salvageSystem.name());
            }
        }

        @ParameterizedTest
        @EnumSource(FormationType.class)
        void onlySalvageFormationsMayFightAndSalvage(FormationType formationType) {
            Formation formation = formationOfType(formationType);
            boolean isSalvage = formationType == FormationType.SALVAGE;

            assertFalse(STRICT.canSalvageAfterFighting(formation));
            assertFalse(LEGACY.canSalvageAfterFighting(formation));
            assertFalse(CHAOS.canSalvageAfterFighting(formation));
            assertEquals(isSalvage, REVISED.canSalvageAfterFighting(formation));
            assertEquals(isSalvage, MEKHQ.canSalvageAfterFighting(formation));
        }
    }

    @Nested
    class UnitRules {
        private static Unit handlessMek(boolean hasBadLeg) {
            Mek mek = mock(Mek.class);
            when(mek.canPerformGroundSalvageOperations()).thenReturn(false);
            when(mek.atLeastOneBadLeg()).thenReturn(hasBadLeg);
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(mek);
            when(unit.canSalvage(anyBoolean())).thenReturn(false);
            when(unit.getCargoCapacityForSalvage()).thenReturn(10.0);
            when(unit.isFullyCrewed()).thenReturn(true);
            when(unit.isRepairable()).thenReturn(true);
            return unit;
        }

        @Test
        void mekHqInheritsRevisedCargoSalvageForHandlessMeks() {
            assertTrue(MEKHQ.canSalvage(handlessMek(false), false));
            assertTrue(MEKHQ.isAvailableForSalvage(handlessMek(false), false));
        }

        @Test
        void mekHqInheritsRevisedMobilityRequirement() {
            assertFalse(MEKHQ.isAvailableForSalvage(handlessMek(true), false));
        }

        @Test
        void strictNeverLetsHandlessMeksSalvage() {
            assertFalse(STRICT.canSalvage(handlessMek(false), false));
        }

        @Test
        void handlessNonMekIsNotGivenTheMekExemption() {
            Unit unit = mock(Unit.class);
            when(unit.getEntity()).thenReturn(mock(Tank.class));
            when(unit.canSalvage(anyBoolean())).thenReturn(false);
            when(unit.getCargoCapacityForSalvage()).thenReturn(10.0);
            when(unit.isFullyCrewed()).thenReturn(true);

            assertFalse(REVISED.canSalvage(unit, false));
        }

        @Test
        void revisedStillChecksMobilityOfNonMeks() {
            Tank tank = mock(Tank.class);
            when(tank.isPermanentlyImmobilized(false)).thenReturn(true);

            assertTrue(REVISED.isImmobilized(tank));
            assertFalse(REVISED.isImmobilized(mock(Entity.class)));
        }

        @Test
        void strictNeverTreatsMeksAsImmobilized() {
            Mek mek = mock(Mek.class);
            when(mek.atLeastOneBadLeg()).thenReturn(true);
            when(mek.isPermanentlyImmobilized(false)).thenReturn(true);

            assertFalse(STRICT.isImmobilized(mek));
        }
    }

    @Nested
    class Settlements {
        @Test
        void campaignOperationsSystemsCapSalvage() {
            assertInstanceOf(CappedSalvageSettlement.class, LEGACY.createSettlement(contract()));
            assertInstanceOf(CappedSalvageSettlement.class, STRICT.createSettlement(contract()));
            assertInstanceOf(CappedSalvageSettlement.class, REVISED.createSettlement(contract()));
        }

        @Test
        void purchaseSystemsSellSalvage() {
            assertInstanceOf(PurchaseSalvageSettlement.class, CHAOS.createSettlement(contract()));
            assertInstanceOf(PurchaseSalvageSettlement.class, MEKHQ.createSettlement(contract()));
        }
    }

    @Nested
    class Resolution {
        private final List<TestUnit> claimed = List.of(mock(TestUnit.class));
        private final List<TestUnit> sold = List.of(mock(TestUnit.class));
        private final List<TestUnit> unclaimed = List.of(mock(TestUnit.class));

        private static Campaign campaign(boolean isUseRiskySalvage) {
            CampaignOptions options = new CampaignOptions();
            options.set(CampaignOption.IS_USE_RISKY_SALVAGE, isUseRiskySalvage);
            Campaign campaign = mock(Campaign.class);
            when(campaign.getCampaignOptions()).thenReturn(options);
            return campaign;
        }

        private static Scenario scenario(boolean hasFormations, boolean hasTechs) {
            Scenario scenario = mock(Scenario.class);
            when(scenario.getSalvageFormations()).thenReturn(hasFormations ? List.of(1) : List.of());
            when(scenario.getSalvageTechs()).thenReturn(hasTechs ? List.of(UUID.randomUUID()) : List.of());
            return scenario;
        }

        private static MockedConstruction<SalvageRecoveryConsole> mockPicker() {
            return mockConstruction(SalvageRecoveryConsole.class, (picker, context) -> {
                when(picker.getUsedSalvageTime()).thenReturn(90);
                when(picker.getCountOfSalvageUnits()).thenReturn(3);
            });
        }

        @Test
        void legacySettlesTheWizardsChoicesDirectly() {
            Campaign campaign = campaign(false);
            AbstractContract contract = contract();
            Scenario scenario = scenario(false, false);

            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class);
                  MockedConstruction<SalvageRecoveryConsole> pickers = mockPicker()) {
                LEGACY.resolveScenarioSalvage(campaign, contract, scenario, true, claimed, sold, unclaimed);

                utilities.verify(() -> CamOpsSalvageUtilities.resolveSalvage(same(campaign), same(contract),
                      same(scenario), any(CappedSalvageSettlement.class), same(claimed), same(sold),
                      same(unclaimed)));
                assertTrue(pickers.constructed().isEmpty());
            }
        }

        @Test
        void legacySettlesEvenWithoutBattlefieldControl() {
            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class)) {
                LEGACY.resolveScenarioSalvage(campaign(false), contract(), scenario(false, false), false, claimed,
                      sold, unclaimed);

                utilities.verify(() -> CamOpsSalvageUtilities.resolveSalvage(any(), any(), any(), any(), anyList(),
                      anyList(), anyList()));
            }
        }

        @ParameterizedTest(name = "control={0}, formations={1}, techs={2}")
        @CsvSource({ "false, true, true", "true, false, true", "true, true, false", "false, false, false" })
        void salvageOperationsNeedControlTeamsAndTechs(boolean hasControl, boolean hasFormations,
              boolean hasTechs) {
            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class);
                  MockedConstruction<SalvageRecoveryConsole> pickers = mockPicker()) {
                STRICT.resolveScenarioSalvage(campaign(true), contract(), scenario(hasFormations, hasTechs),
                      hasControl, claimed, sold, unclaimed);

                assertTrue(pickers.constructed().isEmpty());
                utilities.verifyNoInteractions();
            }
        }

        @ParameterizedTest
        @EnumSource(value = SalvageSystem.class, names = { "CAM_OPS_STRICT", "CAM_OPS_REVISED", "MEKHQ" })
        void salvageTeamsRecoverWrecksAndSpendTheirTime(SalvageSystem salvageSystem) {
            Campaign campaign = campaign(false);
            Scenario scenario = scenario(true, true);

            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class);
                  MockedConstruction<SalvageRecoveryConsole> pickers = mockPicker()) {
                salvageSystem.getSalvage()
                      .resolveScenarioSalvage(campaign, contract(), scenario, true, claimed, sold, unclaimed);

                assertEquals(1, pickers.constructed().size());
                List<UUID> techs = scenario.getSalvageTechs();
                utilities.verify(() -> CamOpsSalvageUtilities.depleteTechMinutes(same(campaign), eq(techs), eq(90)));
                utilities.verify(() -> CamOpsSalvageUtilities.performRiskySalvageChecks(any(), anyList(), anyInt()),
                      never());
                utilities.verify(() -> CamOpsSalvageUtilities.resolveSalvage(any(), any(), any(), any(), anyList(),
                      anyList(), anyList()), never());
            }
        }

        @Test
        void riskySalvageRollsForEachRecoveredWreck() {
            Campaign campaign = campaign(true);
            Scenario scenario = scenario(true, true);

            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class);
                  MockedConstruction<SalvageRecoveryConsole> pickers = mockPicker()) {
                STRICT.resolveScenarioSalvage(campaign, contract(), scenario, true, claimed, sold, unclaimed);

                List<UUID> techs = scenario.getSalvageTechs();
                utilities.verify(() -> CamOpsSalvageUtilities.performRiskySalvageChecks(same(campaign), eq(techs),
                      eq(3)));
            }
        }

        @Test
        void chaosCampaignRecoversEverythingWithBattlefieldControl() {
            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class);
                  MockedConstruction<SalvageRecoveryConsole> pickers = mockPicker()) {
                // No salvage teams are needed
                CHAOS.resolveScenarioSalvage(campaign(true), contract(), scenario(false, false), true, claimed,
                      sold, unclaimed);

                assertEquals(1, pickers.constructed().size());
                utilities.verifyNoInteractions();
            }
        }

        @Test
        void chaosCampaignRecoversNothingWithoutBattlefieldControl() {
            try (MockedStatic<CamOpsSalvageUtilities> utilities = mockStatic(CamOpsSalvageUtilities.class);
                  MockedConstruction<SalvageRecoveryConsole> pickers = mockPicker()) {
                CHAOS.resolveScenarioSalvage(campaign(true), contract(), scenario(true, true), false, claimed,
                      sold, unclaimed);

                assertTrue(pickers.constructed().isEmpty());
                utilities.verifyNoInteractions();
            }
        }
    }
}
