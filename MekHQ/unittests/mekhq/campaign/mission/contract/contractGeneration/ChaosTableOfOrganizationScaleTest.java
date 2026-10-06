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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests the whole-TO&amp;E Scale used by Hot Spots upkeep:
 * {@link ChaosContractDeterminationScale#generateScaleForTableOfOrganization} and its per-day cache,
 * {@link ChaosContractDeterminationScale#getScaleForTableOfOrganization}.
 *
 * <p>Unlike the contract Scale, every unit in a formation counts whatever the formation's type or role; only units
 * outside the TO&amp;E and mothballed units are left out.</p>
 */
class ChaosTableOfOrganizationScaleTest {
    /** Per-Scale Battle Value without the battlefield support point conversion */
    private static final int BATTLE_VALUE_PER_SCALE = 4_500;
    /** Per-Scale Battle Value with 32 BSP x 500 BV folded in */
    private static final int CONVERTED_BATTLE_VALUE_PER_SCALE = 20_500;

    private static final int FORMATION_ID = 7;
    private static final int OTHER_FORMATION_ID = 8;
    private static final int UNASSIGNED_FORMATION_ID = -1;

    private PlayerForce playerForce;
    private LocalHangar hangar;
    private List<Unit> units;

    @BeforeEach
    void setUp() {
        playerForce = mock(PlayerForce.class);
        hangar = mock(LocalHangar.class);
        units = new ArrayList<>();
        when(hangar.getUnits()).thenAnswer(invocation -> new ArrayList<>(units));

        Formation reserveSupport = mock(Formation.class);
        when(reserveSupport.getCombatRoleInMemory()).thenReturn(CombatRole.RESERVE);
        when(reserveSupport.isFormationType(FormationType.STANDARD)).thenReturn(false);
        when(playerForce.getFormation(FORMATION_ID)).thenReturn(reserveSupport);

        Formation maneuver = mock(Formation.class);
        when(maneuver.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
        when(maneuver.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(playerForce.getFormation(OTHER_FORMATION_ID)).thenReturn(maneuver);

        when(playerForce.getFormation(UNASSIGNED_FORMATION_ID)).thenReturn(null);
    }

    private Unit addUnit(int formationId, boolean isMothballed, Integer battleValue) {
        Unit unit = mock(Unit.class);
        when(unit.getFormationId()).thenReturn(formationId);
        when(unit.isMothballed()).thenReturn(isMothballed);
        if (battleValue == null) {
            when(unit.getEntity()).thenReturn(null);
        } else {
            Entity entity = mock(Entity.class);
            when(entity.calculateBattleValue(true, true)).thenReturn(battleValue);
            when(unit.getEntity()).thenReturn(entity);
        }
        units.add(unit);
        return unit;
    }

    private int scale(boolean convertSupportPoints) {
        return ChaosContractDeterminationScale.generateScaleForTableOfOrganization(playerForce, hangar,
              convertSupportPoints);
    }

    @Nested
    class GenerateScaleForTableOfOrganization {
        @Test
        void anEmptyHangarHasNoScale() {
            assertEquals(0, scale(false));
            assertEquals(0, scale(true));
        }

        @Test
        void battleValueIsDividedByTheUnconvertedPerScaleFigure() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 3);

            assertEquals(3, scale(false));
        }

        @Test
        void battleValueIsDividedByTheConvertedPerScaleFigure() {
            addUnit(FORMATION_ID, false, CONVERTED_BATTLE_VALUE_PER_SCALE * 2);

            assertEquals(2, scale(true));
        }

        @Test
        void partScalesRoundUp() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE + 1);

            assertEquals(2, scale(false));
        }

        @Test
        void anyBattleValueIsAtLeastScaleOne() {
            addUnit(FORMATION_ID, false, 1);

            assertEquals(1, scale(false));
        }

        @Test
        void battleValueIsSummedAcrossFormationsBeforeRounding() {
            // Each alone would round up to 1; together they are exactly 1
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE / 2);
            addUnit(OTHER_FORMATION_ID, false, BATTLE_VALUE_PER_SCALE / 2);

            assertEquals(1, scale(false));
        }

        /** Upkeep covers the whole TO&amp;E, so reserve and non-standard formations count, unlike contract Scale. */
        @Test
        void reserveAndNonStandardFormationsCount() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE);

            assertEquals(1, scale(false));
        }

        @Test
        void mothballedUnitsDoNotCount() {
            Unit mothballed = addUnit(FORMATION_ID, true, BATTLE_VALUE_PER_SCALE * 10);
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE);

            assertEquals(1, scale(false));
            verify(mothballed, times(0)).getEntity();
        }

        @Test
        void unitsOutsideTheTableOfOrganizationDoNotCount() {
            addUnit(UNASSIGNED_FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 10);
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE);

            assertEquals(1, scale(false));
        }

        @Test
        void unitsWithoutAnEntityAddNothing() {
            addUnit(FORMATION_ID, false, null);

            assertEquals(0, scale(false));
        }

        /** Scale ignores pilot skill: BV is asked for with C3 and skill ignored, like contract Scale. */
        @Test
        void battleValueIgnoresSkillAndC3() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE);
            Entity entity = units.get(0).getEntity();

            scale(false);

            verify(entity).calculateBattleValue(true, true);
        }
    }

    @Nested
    class Cache {
        private final LocalDate today = LocalDate.of(3052, 6, 1);
        private Campaign campaign;
        private CampaignOptions campaignOptions;

        @BeforeEach
        void setUpCampaign() {
            campaign = campaignFor(playerForce, today);
            campaignOptions = campaign.getCampaignOptions();
            when(playerForce.getHangar()).thenReturn(hangar);
        }

        private Campaign campaignFor(PlayerForce force, LocalDate date) {
            Campaign newCampaign = mockCampaign();
            CampaignOptions newOptions = new CampaignOptions();
            newOptions.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, false);
            when(newCampaign.getCampaignOptions()).thenReturn(newOptions);
            when(newCampaign.getPlayerForce()).thenReturn(force);
            when(newCampaign.getLocalDate()).thenReturn(date);
            return newCampaign;
        }

        @Test
        void matchesTheUncachedScale() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 4);

            assertEquals(scale(false), ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
        }

        @Test
        void repeatCallsTheSameDayReuseTheResult() {
            Unit unit = addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 2);
            Entity entity = unit.getEntity();

            assertEquals(2, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
            assertEquals(2, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
            assertEquals(2, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));

            verify(entity, times(1)).calculateBattleValue(true, true);
        }

        @Test
        void aNewDayRecalculates() {
            Unit unit = addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 2);
            Entity entity = unit.getEntity();
            ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign);

            when(entity.calculateBattleValue(true, true)).thenReturn(BATTLE_VALUE_PER_SCALE * 5);
            when(campaign.getLocalDate()).thenReturn(today.plusDays(1));

            assertEquals(5, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
        }

        @Test
        void buyingOrSellingAUnitRecalculates() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 2);
            assertEquals(2, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));

            Unit bought = addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE * 3);
            assertEquals(5, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));

            units.remove(bought);
            assertEquals(2, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
        }

        @Test
        void changingTheConversionOptionRecalculates() {
            addUnit(FORMATION_ID, false, CONVERTED_BATTLE_VALUE_PER_SCALE);
            // 20,500 / 4,500 rounds up to 5
            assertEquals(5, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));

            campaignOptions.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, true);

            assertEquals(1, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
        }

        /** Loading another campaign on the same day with the same unit count must not reuse this one's Scale. */
        @Test
        void anotherCampaignIsNeverServedThisCampaignsScale() {
            addUnit(FORMATION_ID, false, BATTLE_VALUE_PER_SCALE);
            assertEquals(1, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));

            PlayerForce otherForce = mock(PlayerForce.class);
            LocalHangar otherHangar = mock(LocalHangar.class);
            Unit otherUnit = mock(Unit.class);
            Entity otherEntity = mock(Entity.class);
            when(otherEntity.calculateBattleValue(true, true)).thenReturn(BATTLE_VALUE_PER_SCALE * 9);
            when(otherUnit.getEntity()).thenReturn(otherEntity);
            when(otherUnit.getFormationId()).thenReturn(FORMATION_ID);
            Formation otherFormation = mock(Formation.class);
            when(otherForce.getFormation(FORMATION_ID)).thenReturn(otherFormation);
            List<Unit> otherUnits = List.of(otherUnit);
            when(otherHangar.getUnits()).thenReturn(otherUnits);
            when(otherForce.getHangar()).thenReturn(otherHangar);
            Campaign otherCampaign = campaignFor(otherForce, today);

            assertEquals(9, ChaosContractDeterminationScale.getScaleForTableOfOrganization(otherCampaign));
            assertEquals(1, ChaosContractDeterminationScale.getScaleForTableOfOrganization(campaign));
        }
    }
}
