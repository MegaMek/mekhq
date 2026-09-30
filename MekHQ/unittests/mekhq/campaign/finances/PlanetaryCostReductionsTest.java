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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the Hot Spots: Draconis Reach off-contract planetary cost reductions (pg 69) in
 * {@link PlanetaryCostReductions}, across every combination of the three options, contract status, and location.
 */
class PlanetaryCostReductionsTest {
    private static final double DELTA = 1e-9;
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private AbstractLocation location;
    private PlanetarySystem currentSystem;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        location = mock(AbstractLocation.class);
        currentSystem = mock(PlanetarySystem.class);

        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getActiveContracts()).thenReturn(List.of());
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(location);
        when(location.isOnPlanet()).thenReturn(true);
        when(location.getCurrentSystem()).thenReturn(currentSystem);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.STANDARD);
    }

    private void setOptions(boolean useReductions, boolean onContract, boolean forMaintenance) {
        campaignOptions.set(CampaignOption.USE_PLANETARY_COST_REDUCTIONS, useReductions);
        campaignOptions.set(CampaignOption.PLANETARY_COST_REDUCTIONS_ON_CONTRACT, onContract);
        campaignOptions.set(CampaignOption.PLANETARY_COST_REDUCTIONS_FOR_MAINTENANCE, forMaintenance);
    }

    private void setOnContract(boolean isOnContract) {
        List<AbstractContract> activeContracts = isOnContract ? List.of(mock(AbstractContract.class)) : List.of();
        when(campaign.getActiveContracts()).thenReturn(activeContracts);
    }

    @Test
    void allOptionsDefaultToOff() {
        CampaignOptions defaults = new CampaignOptions();
        assertEquals(false, defaults.get(CampaignOption.USE_PLANETARY_COST_REDUCTIONS));
        assertEquals(false, defaults.get(CampaignOption.PLANETARY_COST_REDUCTIONS_ON_CONTRACT));
        assertEquals(false, defaults.get(CampaignOption.PLANETARY_COST_REDUCTIONS_FOR_MAINTENANCE));
    }

    @Nested
    class RepairAndRefit {
        @Test
        void noReductionWhenTheOptionIsOff() {
            setOptions(false, true, true);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        /** Sub-options do nothing without the main option, even with every one ticked. */
        @Test
        void subOptionsAloneDoNothing() {
            setOptions(false, true, true);
            setOnContract(false);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
            assertEquals(1.0, PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }

        @ParameterizedTest
        @EnumSource(HiringHallLevel.class)
        void offContractOnAPlanetUsesTheHiringHallMultiplier(HiringHallLevel hiringHallLevel) {
            setOptions(true, false, false);
            when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(hiringHallLevel);

            assertEquals(hiringHallLevel.getPlanetaryCostMultiplier(),
                  PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void noReductionOnContractByDefault() {
            setOptions(true, false, false);
            setOnContract(true);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void onContractOptionExtendsTheReductionToContracts() {
            setOptions(true, true, false);
            setOnContract(true);
            when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.MINOR);

            assertEquals(0.75, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void maintenanceOptionDoesNotAffectRepairs() {
            setOptions(true, false, true);
            setOnContract(true);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        /**
         * The cached {@link Campaign#hasActiveContract()} flag is only refreshed daily under StratCon, so the helper
         * must look at the contracts themselves.
         */
        @Test
        void contractStatusIsReadFromTheContractsNotTheCachedFlag() {
            setOptions(true, false, false);
            setOnContract(true);
            when(campaign.hasActiveContract()).thenReturn(false);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void noReductionInTransit() {
            setOptions(true, true, true);
            when(location.isOnPlanet()).thenReturn(false);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void noReductionWithoutALocation() {
            setOptions(true, true, true);
            when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(null);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void noReductionWithoutASystem() {
            setOptions(true, true, true);
            when(location.getCurrentSystem()).thenReturn(null);

            assertEquals(1.0, PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign), DELTA);
        }

        @Test
        void theHiringHallIsLookedUpForToday() {
            setOptions(true, false, false);

            PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign);

            verify(currentSystem).getHiringHallLevel(TODAY);
        }

        @Test
        void locationIsNotLookedUpWhenTheOptionIsOff() {
            setOptions(false, false, false);

            PlanetaryCostReductions.getRepairAndRefitMultiplier(campaign);

            verify(location, never()).isOnPlanet();
        }
    }

    @Nested
    class Maintenance {
        @Test
        void noReductionWithoutTheMaintenanceOption() {
            setOptions(true, true, false);

            assertEquals(1.0, PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }

        @Test
        void noReductionWithOnlyTheMaintenanceOption() {
            setOptions(false, false, true);

            assertEquals(1.0, PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }

        @ParameterizedTest
        @EnumSource(HiringHallLevel.class)
        void offContractMaintenanceUsesTheHiringHallMultiplier(HiringHallLevel hiringHallLevel) {
            setOptions(true, false, true);
            when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(hiringHallLevel);

            assertEquals(hiringHallLevel.getPlanetaryCostMultiplier(),
                  PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }

        /** Maintenance is never reduced on contract, even when repairs and refits are. */
        @ParameterizedTest
        @CsvSource({ "true", "false" })
        void maintenanceIsNeverReducedOnContract(boolean reductionsApplyOnContract) {
            setOptions(true, reductionsApplyOnContract, true);
            setOnContract(true);
            when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.GREAT);

            assertEquals(1.0, PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }

        @Test
        void noMaintenanceReductionInTransit() {
            setOptions(true, false, true);
            when(location.isOnPlanet()).thenReturn(false);

            assertEquals(1.0, PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }

        @Test
        void noMaintenanceReductionWithoutASystem() {
            setOptions(true, false, true);
            when(location.getCurrentSystem()).thenReturn(null);

            assertEquals(1.0, PlanetaryCostReductions.getMaintenanceMultiplier(campaign), DELTA);
        }
    }
}
