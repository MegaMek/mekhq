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
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.AbstractLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests {@link RepairCosts#getRepairCostMultiplier}: the Hot Spots: Draconis Reach Clan repair cost increase (pg 31)
 * and how it combines with the planetary cost reductions.
 */
class RepairCostsTest {
    private static final double DELTA = 1e-9;
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private PlanetarySystem currentSystem;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getActiveContracts()).thenReturn(List.of());

        AbstractLocation location = mock(AbstractLocation.class);
        currentSystem = mock(PlanetarySystem.class);
        when(campaign.getPlayerForce().getForceDetachment().getCurrentLocation()).thenReturn(location);
        when(location.isOnPlanet()).thenReturn(true);
        when(location.getCurrentSystem()).thenReturn(currentSystem);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.MINOR);
    }

    private static Unit unit(boolean isClan, boolean isMixedTech) {
        Entity entity = mock(Entity.class);
        when(entity.isClan()).thenReturn(isClan);
        when(entity.isMixedTech()).thenReturn(isMixedTech);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        return unit;
    }

    @Test
    void optionDefaultsToOff() {
        assertEquals(false, new CampaignOptions().get(CampaignOption.INCREASE_CLAN_REPAIR_COSTS));
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "true, false", "false, true", "true, true" })
    void noIncreaseWhenTheOptionIsOff(boolean isClan, boolean isMixedTech) {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, false);

        assertEquals(1.0, RepairCosts.getRepairCostMultiplier(campaign, unit(isClan, isMixedTech)), DELTA);
    }

    @Test
    void clanUnitsCostHalfAgainToRepair() {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);

        assertEquals(1.5, RepairCosts.getRepairCostMultiplier(campaign, unit(true, false)), DELTA);
    }

    @Test
    void mixedTechUnitsCostHalfAgainToRepair() {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);

        assertEquals(1.5, RepairCosts.getRepairCostMultiplier(campaign, unit(false, true)), DELTA);
    }

    /** A unit that is both Clan and mixed tech is still only increased once. */
    @Test
    void clanMixedTechUnitsAreOnlyIncreasedOnce() {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);

        assertEquals(1.5, RepairCosts.getRepairCostMultiplier(campaign, unit(true, true)), DELTA);
    }

    @Test
    void innerSphereUnitsAreUnaffected() {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);

        assertEquals(1.0, RepairCosts.getRepairCostMultiplier(campaign, unit(false, false)), DELTA);
    }

    /** Spare parts being repaired in the warehouse have no unit. */
    @Test
    void sparePartsAreUnaffected() {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);

        assertEquals(1.0, RepairCosts.getRepairCostMultiplier(campaign, null), DELTA);
    }

    @Test
    void unitsWithoutAnEntityAreUnaffected() {
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);
        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(null);

        assertEquals(1.0, RepairCosts.getRepairCostMultiplier(campaign, unit), DELTA);
    }

    @Test
    void planetaryReductionAppliesWithoutTheClanOption() {
        campaignOptions.set(CampaignOption.USE_PLANETARY_COST_REDUCTIONS, true);

        assertEquals(0.75, RepairCosts.getRepairCostMultiplier(campaign, unit(true, false)), DELTA);
    }

    @Test
    void clanIncreaseStacksMultiplicativelyWithThePlanetaryReduction() {
        campaignOptions.set(CampaignOption.USE_PLANETARY_COST_REDUCTIONS, true);
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);

        assertEquals(0.75 * 1.5, RepairCosts.getRepairCostMultiplier(campaign, unit(true, false)), DELTA);
        assertEquals(0.75, RepairCosts.getRepairCostMultiplier(campaign, unit(false, false)), DELTA);
        assertEquals(0.75, RepairCosts.getRepairCostMultiplier(campaign, null), DELTA);
    }

    @Test
    void clanIncreaseStillAppliesOnContractWhenThePlanetaryReductionDoesNot() {
        campaignOptions.set(CampaignOption.USE_PLANETARY_COST_REDUCTIONS, true);
        campaignOptions.set(CampaignOption.INCREASE_CLAN_REPAIR_COSTS, true);
        AbstractContract activeContract = mock(AbstractContract.class);
        when(campaign.getActiveContracts()).thenReturn(List.of(activeContract));

        assertEquals(1.5, RepairCosts.getRepairCostMultiplier(campaign, unit(false, true)), DELTA);
    }
}
