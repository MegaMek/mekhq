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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalHangar;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.Formation;
import mekhq.campaign.force.FormationType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.utilities.CombatRole;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.enums.HiringHallLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests the "Cap Contract Scale by Hiring Hall" anti-snowball option in
 * {@link AbstractContractGeneration#determineScale}, and that {@link AbstractContractGeneration#determineUncappedScale}
 * always reports the committed force's real Scale.
 */
class ContractScaleCapTest {
    /** Per-Scale Battle Value without the battlefield support point conversion */
    private static final int BATTLE_VALUE_PER_SCALE = 4_500;
    private static final int FORMATION_ID = 3;
    private static final LocalDate TODAY = LocalDate.of(3052, 6, 1);

    private Campaign campaign;
    private CampaignOptions campaignOptions;
    private PlayerForce playerForce;
    private LocalHangar hangar;
    private PlanetarySystem currentSystem;
    private AbstractContract contract;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        campaignOptions = new CampaignOptions();
        campaignOptions.set(CampaignOption.USE_CHAOS_SCALE_SUPPORT_POINT_CONVERSION, false);
        when(campaign.getCampaignOptions()).thenReturn(campaignOptions);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        playerForce = mock(PlayerForce.class);
        hangar = mock(LocalHangar.class);
        Formation formation = mock(Formation.class);
        when(formation.getCombatRoleInMemory()).thenReturn(CombatRole.MANEUVER);
        when(formation.isFormationType(FormationType.STANDARD)).thenReturn(true);
        when(playerForce.getFormation(FORMATION_ID)).thenReturn(formation);

        currentSystem = mock(PlanetarySystem.class);
        when(campaign.getCurrentSystem()).thenReturn(currentSystem);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.NONE);

        contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
    }

    private void committedForceOfScale(int scale) {
        Entity entity = mock(Entity.class);
        when(entity.calculateBattleValue(true, true)).thenReturn(BATTLE_VALUE_PER_SCALE * scale);
        Unit unit = mock(Unit.class);
        when(unit.getFormationId()).thenReturn(FORMATION_ID);
        when(unit.getEntity()).thenReturn(entity);
        List<Unit> units = List.of(unit);
        when(hangar.getUnits()).thenReturn(units);
    }

    private int determineScale() {
        return AbstractContractGeneration.determineScale(campaign, playerForce, hangar, contract);
    }

    @Test
    void noCapWhenTheOptionIsOff() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, false);
        committedForceOfScale(12);

        assertEquals(12, determineScale());
        verify(currentSystem, never()).getHiringHallLevel(TODAY);
    }

    @ParameterizedTest
    @CsvSource({ "STANDARD, 8", "MINOR, 6", "QUESTIONABLE, 4", "NONE, 2" })
    void largeForcesAreCappedByTheHiringHall(HiringHallLevel hiringHallLevel, int expectedScale) {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(hiringHallLevel);
        committedForceOfScale(12);

        assertEquals(expectedScale, determineScale());
    }

    @Test
    void aGreatHiringHallIsNeverCapped() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.GREAT);
        committedForceOfScale(40);

        assertEquals(40, determineScale());
    }

    @ParameterizedTest
    @EnumSource(HiringHallLevel.class)
    void smallForcesKeepTheirOwnScale(HiringHallLevel hiringHallLevel) {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(hiringHallLevel);
        committedForceOfScale(1);

        assertEquals(1, determineScale());
    }

    @Test
    void forceExactlyAtTheCapKeepsItsScale() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.MINOR);
        committedForceOfScale(6);

        assertEquals(6, determineScale());
    }

    @Test
    void emptyForceStaysAtScaleZero() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        List<Unit> noUnits = List.of();
        when(hangar.getUnits()).thenReturn(noUnits);

        assertEquals(0, determineScale());
    }

    /** In transit there is no system to broker the job, so there is nothing to cap by. */
    @Test
    void noCapWithoutACurrentSystem() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        when(campaign.getCurrentSystem()).thenReturn(null);
        committedForceOfScale(12);

        assertEquals(12, determineScale());
    }

    @Test
    void theHiringHallIsReadForToday() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        committedForceOfScale(3);

        determineScale();

        verify(currentSystem).getHiringHallLevel(TODAY);
    }

    @Test
    void uncappedScaleIgnoresTheCap() {
        campaignOptions.set(CampaignOption.CAP_CONTRACT_SCALE_BY_HIRING_HALL, true);
        when(currentSystem.getHiringHallLevel(TODAY)).thenReturn(HiringHallLevel.NONE);
        committedForceOfScale(12);

        assertEquals(12, AbstractContractGeneration.determineUncappedScale(campaign, playerForce, hangar, contract));
        assertEquals(2, determineScale());
    }
}
