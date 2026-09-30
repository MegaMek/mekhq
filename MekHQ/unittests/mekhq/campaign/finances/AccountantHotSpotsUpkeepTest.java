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
package mekhq.campaign.finances;

import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests the Hot Spots finance rules in {@link Accountant}: the monthly upkeep of 500 support points per Scale of the
 * whole TO&amp;E (optionally escalating), its replacement of maintenance and overhead, and the planetary maintenance
 * reduction.
 */
class AccountantHotSpotsUpkeepTest {
    /** Per-Scale Battle Value without the battlefield support point conversion */
    private static final int BATTLE_VALUE_PER_SCALE = 4_500;
    /** Per-Scale Battle Value with 32 BSP x 500 BV folded in */
    private static final int CONVERTED_BATTLE_VALUE_PER_SCALE = 20_500;
    private static final int SUPPORT_POINTS_TO_C_BILLS = 10_000;
    private static final int FORMATION_ID = 1;
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private PlayerForce playerForce;
    private LocalHangar hangar;
    private List<Unit> units;
    private Accountant accountant;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, true);
        campaignOptions.set(CampaignOption.ESCALATING_HOT_SPOTS_UPKEEP, false);
        campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, true);
        campaignOptions.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        playerForce = campaign.getPlayerForce();
        hangar = mock(LocalHangar.class);
        units = new ArrayList<>();
        when(hangar.getUnits()).thenAnswer(invocation -> new ArrayList<>(units));
        when(playerForce.getHangar()).thenReturn(hangar);
        Formation formation = mock(Formation.class);
        when(playerForce.getFormation(FORMATION_ID)).thenReturn(formation);

        accountant = new Accountant(campaign);
    }

    private void tableOfOrganizationWithBattleValue(int battleValue) {
        Entity entity = mock(Entity.class);
        when(entity.calculateBattleValue(true, true)).thenReturn(battleValue);
        Unit unit = mock(Unit.class);
        when(unit.getFormationId()).thenReturn(FORMATION_ID);
        when(unit.getEntity()).thenReturn(entity);
        units.add(unit);
    }

    private void tableOfOrganizationOfScale(int scale) {
        tableOfOrganizationWithBattleValue(BATTLE_VALUE_PER_SCALE * scale);
    }

    private static Money supportPoints(int supportPoints) {
        return Money.of(supportPoints * SUPPORT_POINTS_TO_C_BILLS);
    }

    @Nested
    class Upkeep {
        @Test
        void noUpkeepWhenTheOptionIsOff() {
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
            tableOfOrganizationOfScale(5);

            assertEquals(Money.zero(), accountant.getHotSpotsUpkeepCosts());
        }

        /** Working out Scale means a BV calculation for every unit, so it must be skipped when upkeep is off. */
        @Test
        void scaleIsNotWorkedOutWhenTheOptionIsOff() {
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);

            accountant.getHotSpotsUpkeepCosts();

            verify(hangar, never()).getUnits();
        }

        @ParameterizedTest
        @CsvSource({ "1", "3", "12" })
        void upkeepIsFiveHundredSupportPointsPerScale(int scale) {
            tableOfOrganizationOfScale(scale);

            assertEquals(supportPoints(500 * scale), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void anEmptyTableOfOrganizationCostsNothing() {
            assertEquals(Money.zero(), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void aPartScaleIsChargedAsAWholeScale() {
            tableOfOrganizationWithBattleValue(BATTLE_VALUE_PER_SCALE + 1);

            assertEquals(supportPoints(1_000), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void upkeepStaysInSupportPointsWithoutTheConversion() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);
            tableOfOrganizationOfScale(3);

            assertEquals(Money.of(1_500), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void scaleConversionOptionChangesTheScaleCharged() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, true);
            tableOfOrganizationWithBattleValue(CONVERTED_BATTLE_VALUE_PER_SCALE * 2);

            assertEquals(supportPoints(1_000), accountant.getHotSpotsUpkeepCosts());
        }

        /** Upkeep is for the whole TO&amp;E, whether or not the force is on contract. */
        @Test
        void upkeepIsChargedOnAndOffContract() {
            tableOfOrganizationOfScale(2);
            Money offContract = accountant.getHotSpotsUpkeepCosts();

            AbstractContract contract = mock(AbstractContract.class);
            when(campaign.getActiveContracts()).thenReturn(List.of(contract));
            when(campaign.hasActiveContract()).thenReturn(true);

            assertEquals(offContract, accountant.getHotSpotsUpkeepCosts());
        }
    }

    @Nested
    class EscalatingUpkeep {
        @BeforeEach
        void escalate() {
            campaignOptions.set(CampaignOption.ESCALATING_HOT_SPOTS_UPKEEP, true);
        }

        @ParameterizedTest
        @CsvSource({ "1, 500", "4, 2000", "5, 2625", "7, 4025", "10, 6500", "24, 24000" })
        void upkeepRisesFivePercentPerScaleAboveFour(int scale, int expectedSupportPoints) {
            tableOfOrganizationOfScale(scale);

            assertEquals(supportPoints(expectedSupportPoints), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void escalationDoesNothingWithoutUpkeep() {
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
            tableOfOrganizationOfScale(10);

            assertEquals(Money.zero(), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void escalatedUpkeepStaysInSupportPointsWithoutTheConversion() {
            campaignOptions.set(CampaignOption.USE_CHAOS_SUPPORT_POINT_CONVERSION, false);
            tableOfOrganizationOfScale(10);

            assertEquals(Money.of(6_500), accountant.getHotSpotsUpkeepCosts());
        }

        @Test
        void escalatedUpkeepNeverFallsBelowFlatUpkeep() {
            for (int scale = 0; scale <= 30; scale++) {
                units.clear();
                tableOfOrganizationOfScale(scale);
                when(campaign.getLocalDate()).thenReturn(TODAY.plusDays(scale));

                Money escalated = accountant.getHotSpotsUpkeepCosts();
                campaignOptions.set(CampaignOption.ESCALATING_HOT_SPOTS_UPKEEP, false);
                Money flat = accountant.getHotSpotsUpkeepCosts();
                campaignOptions.set(CampaignOption.ESCALATING_HOT_SPOTS_UPKEEP, true);

                assertEquals(false, escalated.isLessThan(flat), "Scale " + scale);
            }
        }
    }

    /** Upkeep replaces maintenance and overhead, whatever their own options say. */
    @Nested
    class ReplacedCosts {
        @BeforeEach
        void setUpCosts() {
            Unit unit = mock(Unit.class);
            when(unit.requiresMaintenance()).thenReturn(true);
            when(unit.getTech()).thenReturn(mock(Person.class));
            when(unit.getMaintenanceCost()).thenReturn(Money.of(100));
            List<Unit> allUnits = List.of(unit);
            when(campaign.getAllUnits()).thenReturn(allUnits);

            ForceHumanResources humanResources = mock(ForceHumanResources.class);
            when(playerForce.getHumanResources()).thenReturn(humanResources);
            Person person = mock(Person.class);
            when(person.getPrimaryRole()).thenReturn(MEKWARRIOR);
            when(person.getSalary(campaignOptions, false, TODAY)).thenReturn(Money.of(1_000));
            List<Person> salaried = List.of(person);
            when(humanResources.getSalaryEligiblePersonnel()).thenReturn(salaried);
            when(playerForce.isClanForce()).thenReturn(false);

            campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
            campaignOptions.set(CampaignOption.PAY_FOR_OVERHEAD, true);
        }

        @Test
        void maintenanceIsChargedWithoutUpkeep() {
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);

            assertEquals(Money.of(100), accountant.getMaintenanceCosts());
        }

        @Test
        void maintenanceIsNotChargedWithUpkeep() {
            assertEquals(Money.zero(), accountant.getMaintenanceCosts());
        }

        @Test
        void overheadIsChargedWithoutUpkeep() {
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);

            assertEquals(Money.of(50), accountant.getOverheadExpenses());
        }

        @Test
        void overheadIsNotChargedWithUpkeep() {
            assertEquals(Money.zero(), accountant.getOverheadExpenses());
        }
    }

    @Nested
    class PlanetaryMaintenanceReduction {
        private PlanetarySystem currentSystem;

        @BeforeEach
        void setUpPlanet() {
            campaignOptions.set(CampaignOption.PAY_FOR_HOT_SPOTS_UPKEEP, false);
            campaignOptions.set(CampaignOption.PAY_FOR_MAINTAIN, true);
            campaignOptions.set(CampaignOption.USE_PLANETARY_COST_REDUCTIONS, true);
            campaignOptions.set(CampaignOption.PLANETARY_COST_REDUCTIONS_FOR_MAINTENANCE, true);

            Unit unit = mock(Unit.class);
            when(unit.requiresMaintenance()).thenReturn(true);
            when(unit.getTech()).thenReturn(mock(Person.class));
            when(unit.getMaintenanceCost()).thenReturn(Money.of(1_000));
            when(unit.getWeeklyMaintenanceCost()).thenReturn(Money.of(200));
            List<Unit> allUnits = List.of(unit);
            when(campaign.getAllUnits()).thenReturn(allUnits);
            List<AbstractContract> noContracts = List.of();
            when(campaign.getActiveContracts()).thenReturn(noContracts);

            AbstractLocation location = mock(AbstractLocation.class);
            currentSystem = mock(PlanetarySystem.class);
            when(playerForce.getForceDetachment().getCurrentLocation()).thenReturn(location);
            when(location.isOnPlanet()).thenReturn(true);
            when(location.getCurrentSystem()).thenReturn(currentSystem);
            when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.STANDARD);
        }

        @Test
        void maintenanceIsReducedOffContract() {
            assertEquals(Money.of(500), accountant.getMaintenanceCosts());
            assertEquals(Money.of(100), accountant.getWeeklyMaintenanceCosts());
        }

        @Test
        void maintenanceIsNotReducedOnContract() {
            AbstractContract contract = mock(AbstractContract.class);
            List<AbstractContract> activeContracts = List.of(contract);
            when(campaign.getActiveContracts()).thenReturn(activeContracts);

            assertEquals(Money.of(1_000), accountant.getMaintenanceCosts());
            assertEquals(Money.of(200), accountant.getWeeklyMaintenanceCosts());
        }

        @Test
        void maintenanceIsNotReducedAtAPoorHiringHall() {
            when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.QUESTIONABLE);

            assertEquals(Money.of(1_000), accountant.getMaintenanceCosts());
        }

        @Test
        void maintenanceIsNotReducedWithoutTheMaintenanceOption() {
            campaignOptions.set(CampaignOption.PLANETARY_COST_REDUCTIONS_FOR_MAINTENANCE, false);

            assertEquals(Money.of(1_000), accountant.getMaintenanceCosts());
        }
    }
}
