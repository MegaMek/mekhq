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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;

import megamek.common.bays.ASFBay;
import megamek.common.bays.Bay;
import megamek.common.bays.CargoBay;
import megamek.common.bays.SmallCraftBay;
import megamek.common.compute.Compute;
import megamek.common.equipment.MiscMounted;
import megamek.common.equipment.MiscType;
import megamek.common.icons.Camouflage;
import megamek.common.units.Aero;
import megamek.common.units.AeroSpaceFighter;
import megamek.common.units.Dropship;
import megamek.common.units.Entity;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.ResolveScenarioTracker;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.utilities.ContractSettlement;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.InjurySPAUtility;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.unit.TestUnit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/**
 * Covers the parts of {@link CamOpsSalvageUtilities} not covered by {@code CamOpsSalvageUtilitiesTest}: equipment
 * checks, the salvage tooltip, settling salvage, delivery timing, deploying salvage teams, and salvage accidents.
 */
class CamOpsSalvageResolutionTest {
    private static final LocalDate TODAY = LocalDate.of(3025, 6, 1);
    private static final double DELTA = 0.0001;

    private static Money moneyOf(double amount) {
        return argThat(money -> (money != null) && (money.compareTo(Money.of(amount)) == 0));
    }

    private static MiscMounted navalTug(Entity entity, boolean isLocationBad, boolean isOperable) {
        MiscType type = mock(MiscType.class);
        when(type.hasFlag(MiscType.F_NAVAL_TUG_ADAPTOR)).thenReturn(true);
        MiscMounted mounted = mock(MiscMounted.class);
        when(mounted.getType()).thenReturn(type);
        when(mounted.getEntity()).thenReturn(entity);
        when(mounted.getLocation()).thenReturn(1);
        when(mounted.isOperable()).thenReturn(isOperable);
        when(entity.isLocationBad(1)).thenReturn(isLocationBad);
        return mounted;
    }

    private static Vector<Bay> bays(Bay... bays) {
        return new Vector<>(List.of(bays));
    }

    @Nested
    class NavalTug {
        @ParameterizedTest(name = "locationBad={0}, operable={1}")
        @CsvSource({ "false, true, true", "true, true, false", "false, false, false", "true, false, false" })
        void onlyAWorkingTugInAnIntactLocationCounts(boolean isLocationBad, boolean isOperable, boolean expected) {
            Dropship dropship = mock(Dropship.class);
            List<MiscMounted> misc = List.of(navalTug(dropship, isLocationBad, isOperable));
            when(dropship.getMisc()).thenReturn(misc);

            assertEquals(expected, CamOpsSalvageUtilities.hasNavalTug(dropship));
        }

        @Test
        void otherEquipmentIsNotATug() {
            Dropship dropship = mock(Dropship.class);
            MiscMounted mounted = mock(MiscMounted.class);
            when(mounted.getType()).thenReturn(mock(MiscType.class));
            when(dropship.getMisc()).thenReturn(List.of(mounted));

            assertFalse(CamOpsSalvageUtilities.hasNavalTug(dropship));
        }

        @Test
        void anyWorkingTugIsEnough() {
            Dropship dropship = mock(Dropship.class);
            MiscType type = mock(MiscType.class);
            when(type.hasFlag(MiscType.F_NAVAL_TUG_ADAPTOR)).thenReturn(true);
            MiscMounted broken = mock(MiscMounted.class);
            when(broken.getType()).thenReturn(type);
            when(broken.getEntity()).thenReturn(dropship);
            when(broken.getLocation()).thenReturn(0);
            when(broken.isOperable()).thenReturn(false);
            MiscMounted working = navalTug(dropship, false, true);
            when(dropship.getMisc()).thenReturn(List.of(broken, working));

            assertTrue(CamOpsSalvageUtilities.hasNavalTug(dropship));
        }

        @Test
        void noEquipmentMeansNoTug() {
            assertFalse(CamOpsSalvageUtilities.hasNavalTug(mock(Dropship.class)));
        }
    }

    @Nested
    class Bays {
        private static Unit carrier(double trackedFighterSlots, double trackedSmallCraftSlots, Bay... bays) {
            Dropship dropship = mock(Dropship.class);
            when(dropship.getTransportBays()).thenReturn(bays(bays));
            Unit carrier = mock(Unit.class);
            when(carrier.getEntity()).thenReturn(dropship);
            when(carrier.getCurrentASFCapacity()).thenReturn(trackedFighterSlots);
            when(carrier.getCurrentSmallCraftCapacity()).thenReturn(trackedSmallCraftSlots);
            return carrier;
        }

        @Test
        void slotsInBaysOfTheRequestedTypeWithWorkingDoorsAreCounted() {
            Unit carrier = carrier(100, 100, new ASFBay(2, 1, 1), new ASFBay(2, 0, 2), new ASFBay(3, 2, 3),
                  new SmallCraftBay(2, 1, 4), new CargoBay(10, 1, 5));

            assertEquals(5, CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier));
            assertEquals(2, CamOpsSalvageUtilities.getFreeSmallCraftBaySlots(carrier));
        }

        @Test
        void doorsDoNotLimitSlots() {
            // A Vengeance's fighter bay: 18 slots behind 4 doors
            Unit carrier = carrier(18, 0, new ASFBay(18, 4, 1));

            assertEquals(18, CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier));
        }

        @Test
        void bayWhoseDoorsWereDestroyedHasNoFreeSlots() {
            ASFBay bay = new ASFBay(2, 1, 1);
            bay.setCurrentDoors(0);

            assertEquals(0, CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier(2, 0, bay)));
        }

        @Test
        void slotsTakenByAssignedFightersAreNotFree() {
            assertEquals(1, CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier(1, 0, new ASFBay(18, 4, 1))));
        }

        @Test
        void carrierWithoutAnEntityHasNoSlots() {
            Unit carrier = mock(Unit.class);
            when(carrier.getCurrentASFCapacity()).thenReturn(10.0);

            assertEquals(0, CamOpsSalvageUtilities.getFreeFighterBaySlots(carrier));
        }
    }

    @Nested
    class ResolveSalvage {
        private final Campaign campaign = mockCampaign();
        private final Finances finances = campaign.getPlayerForce().getFinances();
        private final CampaignOptions options = new CampaignOptions();
        private final AbstractContract contract = mock(AbstractContract.class);
        private final Scenario scenario = mock(Scenario.class);
        private final List<Unit> addedUnits = new ArrayList<>();
        // Salvage delivered on the day it is resolved is present; salvage still in transit is not
        private boolean isAddedUnitPresent = true;

        ResolveSalvage() {
            when(campaign.getCampaignOptions()).thenReturn(options);
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.ASSASSINATION);
            when(scenario.getId()).thenReturn(7);
            when(scenario.getName()).thenReturn("Raid");
            when(scenario.getHyperlinkedName()).thenReturn("<a>Raid</a>");
            when(campaign.addTestUnit(any(), anyInt())).thenAnswer(invocation -> {
                TestUnit testUnit = invocation.getArgument(0);
                Entity entity = testUnit.getEntity();
                Unit unit = mock(Unit.class);
                when(unit.getEntity()).thenReturn(entity);
                when(unit.isPresent()).thenReturn(isAddedUnitPresent);
                addedUnits.add(unit);
                return unit;
            });
        }

        private static TestUnit wreck(double sellValue, Camouflage camouflage) {
            Entity entity = mock(Mek.class);
            when(entity.getCamouflage()).thenReturn(camouflage);
            TestUnit wreck = mock(TestUnit.class);
            when(wreck.getEntity()).thenReturn(entity);
            when(wreck.getSellValue()).thenReturn(Money.of(sellValue));
            return wreck;
        }

        private static TestUnit wreck(double sellValue) {
            return wreck(sellValue, new Camouflage());
        }

        private void resolve(SalvageSettlement settlement, List<TestUnit> kept, List<TestUnit> sold,
              List<TestUnit> employer) {
            CamOpsSalvageUtilities.resolveSalvage(campaign, contract, scenario, settlement, kept, sold, employer);
        }

        @Test
        void keptSalvageJoinsTheCampaignAtTheContractRepairSite() {
            TestUnit wreck = wreck(1000);

            resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());

            verify(campaign).clearGameData(wreck.getEntity());
            verify(campaign).addTestUnit(same(wreck), eq(0));
            // The campaign keeps a new unit, not the test unit, so that's the one that must be at the repair site
            verify(addedUnits.getFirst()).setSite(Unit.SITE_FIELD_WORKSHOP);
            verify(contract).changeSalvagedByUnitValue(moneyOf(1000));
        }

        @Test
        void keptSalvageStillInTransitIsLeftForItsArrivalToPlace() {
            isAddedUnitPresent = false;
            TestUnit wreck = wreck(1000);

            resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());

            // A unit in transit is given the best repair site when it arrives (LocationNewDayUtil), not here
            verify(addedUnits.getFirst(), never()).setSite(anyInt());
        }

        @Test
        void enemyCamouflageIsDroppedByDefault() {
            Camouflage enemyCamouflage = new Camouflage("Clans", "Wolf.png");
            TestUnit wreck = wreck(1000, enemyCamouflage);

            resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());

            verify(wreck.getEntity(), never()).setCamouflage(any());
        }

        @Test
        void enemyCamouflageIsKeptWhenEnabled() {
            options.set(CampaignOption.IS_KEEP_ENEMY_CAMOUFLAGE_ON_SALVAGE, true);
            Camouflage enemyCamouflage = new Camouflage("Clans", "Wolf.png");
            TestUnit wreck = wreck(1000, enemyCamouflage);

            resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());

            verify(wreck.getEntity()).setCamouflage(argThat(camouflage -> "Clans".equals(camouflage.getCategory()) &&
                                                                               "Wolf.png".equals(
                                                                                     camouflage.getFilename())));
        }

        @Test
        void defaultCamouflageIsNeverKept() {
            options.set(CampaignOption.IS_KEEP_ENEMY_CAMOUFLAGE_ON_SALVAGE, true);
            TestUnit wreck = wreck(1000, new Camouflage());

            resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());

            verify(wreck.getEntity(), never()).setCamouflage(any());
        }

        @Test
        void aerospaceSalvageIsRefueledFromAFreshCopy() {
            Aero aero = mock(AeroSpaceFighter.class);
            when(aero.getCamouflage()).thenReturn(new Camouflage());
            TestUnit wreck = mock(TestUnit.class);
            when(wreck.getEntity()).thenReturn(aero);
            when(wreck.getSellValue()).thenReturn(Money.of(1000));
            Aero freshCopy = mock(AeroSpaceFighter.class);
            when(freshCopy.getFuelTonnage()).thenReturn(5.0);

            try (MockedConstruction<ResolveScenarioTracker.UnitStatus> ignored =
                       mockConstruction(ResolveScenarioTracker.UnitStatus.class,
                             (status, context) -> when(status.getBaseEntity()).thenReturn(freshCopy))) {
                resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());
            }

            verify(aero).setFuelTonnage(5.0);
        }

        @Test
        void aerospaceSalvageWithoutAFreshCopyKeepsItsFuel() {
            Aero aero = mock(AeroSpaceFighter.class);
            when(aero.getCamouflage()).thenReturn(new Camouflage());
            TestUnit wreck = mock(TestUnit.class);
            when(wreck.getEntity()).thenReturn(aero);
            when(wreck.getSellValue()).thenReturn(Money.of(1000));

            // The unit's file couldn't be found, so there's nothing to refuel from
            try (MockedConstruction<ResolveScenarioTracker.UnitStatus> ignored =
                       mockConstruction(ResolveScenarioTracker.UnitStatus.class)) {
                resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());
            }

            verify(aero, never()).setFuelTonnage(anyDouble());
            verify(campaign).addTestUnit(same(wreck), eq(0));
        }

        @Test
        void nonAerospaceSalvageNeverBuildsAUnitStatus() {
            try (MockedConstruction<ResolveScenarioTracker.UnitStatus> statuses =
                       mockConstruction(ResolveScenarioTracker.UnitStatus.class)) {
                resolve(new CappedSalvageSettlement(0.5), List.of(wreck(1000)), List.of(), List.of());

                assertTrue(statuses.constructed().isEmpty());
            }
        }

        @Test
        void worthlessSoldSalvageIsNotPaid() {
            resolve(new CappedSalvageSettlement(0.5), List.of(), List.of(wreck(0)), List.of());

            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void soldSalvageIsPaidOut() {
            resolve(new CappedSalvageSettlement(0.5), List.of(), List.of(wreck(1000), wreck(500)), List.of());

            verify(finances).credit(eq(TransactionType.SALVAGE), eq(TODAY), moneyOf(1500), anyString());
            verify(contract).changeSalvagedByUnitValue(moneyOf(1500));
            verify(campaign, never()).addTestUnit(any(), anyInt());
        }

        @Test
        void cappedSalvageLeavesTheEmployersShareWithTheEmployer() {
            resolve(new CappedSalvageSettlement(0.5), List.of(), List.of(), List.of(wreck(1000)));

            verify(contract).changeSalvagedByEmployerValue(moneyOf(1000));
            verify(finances, never()).credit(any(), any(), any(), anyString());
        }

        @Test
        void exchangePaysTheShareOfEmployerSalvage() {
            resolve(new ExchangeSalvageSettlement(0.25), List.of(), List.of(), List.of(wreck(1000)));

            verify(finances).credit(eq(TransactionType.SALVAGE_EXCHANGE), eq(TODAY), moneyOf(250), anyString());
            verify(contract).changeSalvagedByUnitValue(moneyOf(250));
            verify(contract).changeSalvagedByEmployerValue(moneyOf(750));
        }

        @Test
        void purchasesChargeForKeptSalvageAndPayForTheRest() {
            resolve(new PurchaseSalvageSettlement(0.25), List.of(wreck(1000)), List.of(), List.of(wreck(400)));

            verify(finances).debit(eq(TransactionType.UNIT_PURCHASE), eq(TODAY), moneyOf(750), anyString());
            verify(finances).credit(eq(TransactionType.SALVAGE), eq(TODAY), moneyOf(100), anyString());
            // The kept wreck counts 250 to the player and the 750 paid for it to the employer
            verify(contract).changeSalvagedByUnitValue(moneyOf(250));
            verify(contract).changeSalvagedByUnitValue(moneyOf(100));
            verify(contract).changeSalvagedByEmployerValue(moneyOf(1050));
        }

        @ParameterizedTest(name = "{0}% salvage rights")
        @CsvSource({ "30", "7", "50", "100" })
        void boughtSalvageLeavesNoOverageAtContractEnd(int salvagePercent) {
            double playerShare = salvagePercent / 100.0;
            Money[] salvagedByUnit = { Money.zero() };
            Money[] salvagedByEmployer = { Money.zero() };
            doAnswer(invocation -> salvagedByUnit[0] = salvagedByUnit[0].plus((Money) invocation.getArgument(0)))
                  .when(contract).changeSalvagedByUnitValue(any());
            doAnswer(invocation -> salvagedByEmployer[0] =
                                         salvagedByEmployer[0].plus((Money) invocation.getArgument(0)))
                  .when(contract).changeSalvagedByEmployerValue(any());
            when(contract.getSalvagedByUnitValue()).thenAnswer(invocation -> salvagedByUnit[0]);
            when(contract.getSalvagedByEmployerValue()).thenAnswer(invocation -> salvagedByEmployer[0]);
            when(contract.getSalvageRightsMultiplier()).thenReturn(playerShare);

            // e.g. at 30%, buying a 10M wreck costs 7M: 3M is the player's, 7M the employer's
            resolve(new PurchaseSalvageSettlement(playerShare), List.of(wreck(10_000_000)), List.of(), List.of());

            assertEquals(0, ContractSettlement.salvageOverage(contract).compareTo(Money.zero()));
            assertEquals(0, salvagedByUnit[0].compareTo(Money.of(10_000_000).multipliedBy(playerShare)));
        }

        @Test
        void nothingToSettleChangesNothing() {
            resolve(new PurchaseSalvageSettlement(0.25), List.of(), List.of(), List.of());

            verify(finances, never()).debit(any(), any(), any(), anyString());
            verify(finances, never()).credit(any(), any(), any(), anyString());
            verify(contract).changeSalvagedByEmployerValue(moneyOf(0));
        }

        @Nested
        class DeliveryTime {
            private final StratConCampaignState state = mock(StratConCampaignState.class);
            private final StratConTrackState track = mock(StratConTrackState.class);
            private final StratConScenario stratConScenario = mock(StratConScenario.class);
            private final Map<Integer, LocalDate> returnDates = new HashMap<>();

            DeliveryTime() {
                when(contract.getStratConCampaignState()).thenReturn(state);
                when(state.getTracks()).thenReturn(List.of(track));
                when(track.getBackingScenariosMap()).thenReturn(Map.of(7, stratConScenario));
                when(track.getAssignedForceReturnDates()).thenReturn(returnDates);
                when(track.getDeploymentTime()).thenReturn(11);
            }

            private int deliveryTime() {
                TestUnit wreck = wreck(1000);
                resolve(new CappedSalvageSettlement(0.5), List.of(wreck), List.of(), List.of());
                ArgumentCaptor<Integer> days = ArgumentCaptor.forClass(Integer.class);
                verify(campaign).addTestUnit(same(wreck), days.capture());
                return days.getValue();
            }

            @Test
            void salvageArrivesWithTheLastSalvageTeam() {
                when(scenario.getSalvageFormations()).thenReturn(List.of(1, 2, 3));
                returnDates.put(1, TODAY.plusDays(4));
                returnDates.put(2, TODAY.plusDays(9));
                returnDates.put(3, TODAY.plusDays(2)); // Earlier teams don't delay the salvage
                returnDates.put(4, TODAY.plusDays(30)); // Not a salvage team for this scenario

                assertEquals(9, deliveryTime());
            }

            @Test
            void withoutTeamReturnDatesSalvageArrivesWithTheScenario() {
                when(scenario.getSalvageFormations()).thenReturn(List.of(1));
                when(stratConScenario.getReturnDate()).thenReturn(TODAY.plusDays(6));

                assertEquals(6, deliveryTime());
            }

            @Test
            void withoutAnyReturnDateSalvageTakesTheTracksDeploymentTime() {
                when(scenario.getSalvageFormations()).thenReturn(List.of());

                assertEquals(11, deliveryTime());
            }

            @Test
            void overdueSalvageArrivesImmediately() {
                when(scenario.getSalvageFormations()).thenReturn(List.of(1));
                returnDates.put(1, TODAY.minusDays(3));

                assertEquals(0, deliveryTime());
            }

            @Test
            void scenarioOffTheTracksArrivesImmediately() {
                when(track.getBackingScenariosMap()).thenReturn(Map.of());

                assertEquals(0, deliveryTime());
            }

            @Test
            void contractWithoutStratConArrivesImmediately() {
                when(contract.getStratConCampaignState()).thenReturn(null);

                assertEquals(0, deliveryTime());
            }
        }
    }

    @Nested
    class DeploySalvageTeams {
        private final Campaign campaign = mockCampaign();
        private final AbstractContract contract = mock(AbstractContract.class);
        private final Scenario scenario = mock(Scenario.class);
        private final StratConCampaignState state = mock(StratConCampaignState.class);
        private final StratConTrackState track = mock(StratConTrackState.class);
        private final StratConCoords coords = new StratConCoords(2, 3);

        DeploySalvageTeams() {
            UUID missionId = UUID.randomUUID();
            when(scenario.getMissionId()).thenReturn(missionId);
            when(scenario.getId()).thenReturn(7);
            when(campaign.getContract(missionId)).thenReturn(contract);
            when(campaign.getLocalDate()).thenReturn(TODAY);
            when(contract.getStratConCampaignState()).thenReturn(state);
            when(state.getTracks()).thenReturn(List.of(track));
            StratConScenario stratConScenario = mock(StratConScenario.class);
            when(stratConScenario.getBackingScenarioID()).thenReturn(7);
            when(track.getScenarios()).thenReturn(Map.of(coords, stratConScenario));
        }

        @Test
        void salvageTeamsDeployToTheScenariosCoordinates() {
            when(scenario.getSalvageFormations()).thenReturn(List.of(1, 2));
            when(campaign.getPlayerForce().getFormation(1)).thenReturn(mock(Formation.class));
            when(campaign.getPlayerForce().getFormation(2)).thenReturn(mock(Formation.class));

            CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario);

            verify(track).assignForce(1, coords, TODAY, false);
            verify(track).assignForce(2, coords, TODAY, false);
        }

        @Test
        void missingFormationsAreSkipped() {
            when(scenario.getSalvageFormations()).thenReturn(List.of(1, 2));
            when(campaign.getPlayerForce().getFormation(1)).thenReturn(mock(Formation.class));
            when(campaign.getPlayerForce().getFormation(2)).thenReturn(null);

            CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario);

            verify(track).assignForce(1, coords, TODAY, false);
            verify(track, never()).assignForce(eq(2), any(), any(), anyBoolean());
        }

        @Test
        void scenarioOffTheTracksDeploysNobody() {
            when(scenario.getId()).thenReturn(8);
            when(scenario.getSalvageFormations()).thenReturn(List.of(1));
            when(campaign.getPlayerForce().getFormation(1)).thenReturn(mock(Formation.class));

            CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario);

            verify(track, never()).assignForce(anyInt(), any(), any(), anyBoolean());
        }

        @Test
        void contractWithoutStratConDeploysNobody() {
            when(contract.getStratConCampaignState()).thenReturn(null);
            when(scenario.getSalvageFormations()).thenReturn(List.of(1));

            CamOpsSalvageUtilities.deploySalvageTeams(campaign, scenario);

            verify(track, never()).assignForce(anyInt(), any(), any(), anyBoolean());
        }
    }

    @Nested
    class RiskySalvage {
        private final Campaign campaign = mockCampaign();
        private final CampaignOptions options = new CampaignOptions();

        RiskySalvage() {
            options.set(CampaignOption.USE_ADVANCED_MEDICAL, false);
            options.set(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL, false);
            options.set(CampaignOption.USE_EDGE, false);
            when(campaign.getCampaignOptions()).thenReturn(options);
            when(campaign.getLocalDate()).thenReturn(TODAY);
        }

        private Person tech(int currentHits) {
            Person tech = mock(Person.class);
            UUID id = UUID.randomUUID();
            when(tech.getId()).thenReturn(id);
            when(tech.getHits()).thenReturn(currentHits);
            when(tech.getTotalInjurySeverity()).thenReturn(currentHits);
            when(tech.getOptions()).thenReturn(mock(PersonnelOptions.class));
            when(campaign.getPlayerForce().getHumanResources().getPerson(id)).thenReturn(tech);
            return tech;
        }

        /**
         * Rolls snake eyes on every 2d6 roll, 3 on every 1d6 roll, and always picks the first tech.
         */
        private static MockedStatic<Compute> snakeEyes(int twoDiceRoll) {
            MockedStatic<Compute> compute = mockStatic(Compute.class);
            compute.when(() -> Compute.d6(2)).thenReturn(twoDiceRoll);
            compute.when(() -> Compute.d6(1)).thenReturn(3);
            compute.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            return compute;
        }

        private static MockedStatic<InjurySPAUtility> noSpaAdjustment() {
            MockedStatic<InjurySPAUtility> injurySpaUtility = mockStatic(InjurySPAUtility.class);
            injurySpaUtility.when(() -> InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(any(), anyBoolean(),
                  anyInt(), anyInt())).thenAnswer(invocation -> invocation.getArgument(3));
            return injurySpaUtility;
        }

        @Test
        void noTechsMeansNoAccidents() {
            try (MockedStatic<Compute> compute = snakeEyes(2)) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(), 5);

                compute.verifyNoInteractions();
            }
        }

        @Test
        void accidentsOnlyHappenOnSnakeEyes() {
            Person tech = tech(0);

            try (MockedStatic<Compute> compute = snakeEyes(3);
                  MockedStatic<InjurySPAUtility> ignored = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 4);

                compute.verify(() -> Compute.d6(2), times(4));
                verify(tech, never()).setHits(anyInt());
                verify(campaign, never()).addReport(any(DailyReportType.class), anyString());
            }
        }

        @Test
        void eachSnakeEyesInjuresATech() {
            Person tech = tech(1);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(tech).setHits(4);
                verify(tech, never()).changeStatus(any(), any(), any());
                verify(campaign).addReport(eq(DailyReportType.MEDICAL), anyString());
            }
        }

        @Test
        void accidentReportNamesTheInjuredTechAndTheirHits() {
            Person tech = tech(1);
            // Stub both name forms so the test checks that the tech is named, not which name form the report uses
            when(tech.getHyperlinkedName()).thenReturn("Kai Allard");
            when(tech.getHyperlinkedFullTitle()).thenReturn("Kai Allard");
            ArgumentCaptor<String> report = ArgumentCaptor.forClass(String.class);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(campaign).addReport(eq(DailyReportType.MEDICAL), report.capture());
            }

            String reportText = report.getValue();
            assertTrue(reportText.contains("Kai Allard took 3 hits."), reportText);
            assertFalse(reportText.contains("killed"), reportText);
        }

        @Test
        void accidentReportNamesTheKilledTech() {
            Person tech = tech(6);
            when(tech.getHyperlinkedFullTitle()).thenReturn("Tech Kai Allard");
            ArgumentCaptor<String> report = ArgumentCaptor.forClass(String.class);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(campaign).addReport(eq(DailyReportType.MEDICAL), report.capture());
            }

            String reportText = report.getValue();
            assertTrue(reportText.contains("Tech Kai Allard took 3 hits and was"), reportText);
            assertTrue(reportText.contains("killed"), reportText);
        }

        @Test
        void techWithFatalInjuriesDiesOnlyOnce() {
            Person tech = tech(6);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 3);

                // The tech is removed from the pool once dead, so later accidents can't find anyone to hurt
                verify(tech, times(1)).changeStatus(campaign, TODAY, PersonnelStatus.ACCIDENTAL);
                verify(tech, times(1)).setHits(anyInt());
            }
        }

        @Test
        void edgeCanAvoidAnAccident() {
            options.set(CampaignOption.USE_EDGE, true);
            Person tech = tech(0);
            when(tech.getCurrentEdge()).thenReturn(1);
            when(tech.getOptions().booleanOption(PersonnelOptions.EDGE_SALVAGE_ACCIDENTS)).thenReturn(true);

            try (MockedStatic<Compute> compute = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored = noSpaAdjustment()) {
                // The accident roll is snake eyes, but the reroll isn't
                compute.when(() -> Compute.d6(2)).thenReturn(2, 7);

                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(tech).spendEdge();
                verify(tech, never()).setHits(anyInt());
                verify(campaign).addReport(eq(DailyReportType.PERSONNEL), anyString());
            }
        }

        @Test
        void failedEdgeRerollStillInjures() {
            options.set(CampaignOption.USE_EDGE, true);
            Person tech = tech(0);
            when(tech.getCurrentEdge()).thenReturn(1);
            when(tech.getOptions().booleanOption(PersonnelOptions.EDGE_SALVAGE_ACCIDENTS)).thenReturn(true);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(tech).spendEdge();
                verify(tech).setHits(3);
            }
        }

        @Test
        void edgeIsNotSpentWithoutTheSalvageAccidentOption() {
            options.set(CampaignOption.USE_EDGE, true);
            Person tech = tech(0);
            when(tech.getCurrentEdge()).thenReturn(1);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(tech, never()).spendEdge();
                verify(tech).setHits(3);
            }
        }

        @Test
        void advancedMedicalResolvesAccidentsAsCombatDamage() {
            options.set(CampaignOption.USE_ADVANCED_MEDICAL, true);
            Person tech = tech(0);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment();
                  MockedStatic<InjuryUtil> injuryUtil =
                        mockStatic(InjuryUtil.class)) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                injuryUtil.verify(() -> InjuryUtil
                                              .resolveCombatDamage(campaign, tech, 3));
                verify(tech, never()).setHits(anyInt());
            }
        }

        @Test
        void techWithoutEdgeCantReroll() {
            options.set(CampaignOption.USE_EDGE, true);
            Person tech = tech(0);
            when(tech.getCurrentEdge()).thenReturn(0);
            when(tech.getOptions().booleanOption(PersonnelOptions.EDGE_SALVAGE_ACCIDENTS)).thenReturn(true);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(tech.getId()), 1);

                verify(tech, never()).spendEdge();
                verify(tech).setHits(3);
            }
        }

        @Test
        void missingTechsAreIgnored() {
            UUID missingTechId = UUID.randomUUID();
            when(campaign.getPlayerForce().getHumanResources().getPerson(missingTechId)).thenReturn(null);

            try (MockedStatic<Compute> ignored = snakeEyes(2);
                  MockedStatic<InjurySPAUtility> ignored2 = noSpaAdjustment()) {
                CamOpsSalvageUtilities.performRiskySalvageChecks(campaign, List.of(missingTechId), 2);

                verify(campaign, never()).addReport(any(DailyReportType.class), anyString());
            }
        }
    }
}
