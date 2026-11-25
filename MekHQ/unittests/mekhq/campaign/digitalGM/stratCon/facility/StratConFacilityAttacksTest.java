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
package mekhq.campaign.digitalGM.stratCon.facility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTestData;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityAttacks.FacilityAttackType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the enemy's attacks on facilities held by the player or their employer (see
 * {@link StratConFacilityAttacks}): how many come and when, which kind each is, and when none come at all.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConFacilityAttacksTest {
    private static final StratConCoords FACILITY_COORDS = new StratConCoords(3, 3);
    private static final LocalDate TODAY = LocalDate.of(3050, 1, 3);
    private static final int LENGTH_IN_MONTHS = 3;

    private StratConCampaignState campaignState;
    private StratConTrackState track;
    private Campaign campaign;
    private CampaignOptions options;
    private AbstractContract contract;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        contract = mock(AbstractContract.class);
        when(contract.getScale()).thenReturn(2);
        when(contract.getExactScale()).thenReturn(2.0);
        when(contract.getLengthInMonths()).thenReturn(LENGTH_IN_MONTHS);
        when(contract.getStartDate()).thenReturn(TODAY);
        when(contract.getEndingDate()).thenReturn(TODAY.plusMonths(LENGTH_IN_MONTHS));
        when(contract.getName()).thenReturn("Test Contract");

        campaignState = new StratConCampaignState(contract);
        track = new StratConTrackState();
        track.setWidth(8);
        track.setHeight(8);
        campaignState.addTrack(track);

        campaign = mock(Campaign.class, RETURNS_DEEP_STUBS);
        options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_SUPPLY_LINES)).thenReturn(false);
        when(options.isUseStratConMaplessMode()).thenReturn(false);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getGUI()).thenReturn(null);
    }

    private void placeFacility(ForceAlignment owner) {
        track.addFacility(FACILITY_COORDS, StratConTestData.facility(owner, FacilityType.MekBase));
    }

    // Scheduling

    @Test
    void oneComesEachMonthPerPointOfScaleWithinTheContract() {
        StratConFacilityAttacks.scheduleFacilityAttacks(contract, campaignState, TODAY);

        List<LocalDate> attackDates = campaignState.getFacilityAttackDates();
        assertEquals(2 * LENGTH_IN_MONTHS, attackDates.size());
        for (LocalDate attackDate : attackDates) {
            assertFalse(attackDate.isBefore(TODAY));
            assertTrue(attackDate.isBefore(TODAY.plusMonths(LENGTH_IN_MONTHS)));
        }
        assertEquals(TODAY.plusMonths(LENGTH_IN_MONTHS), campaignState.getFacilityAttacksScheduledUntil());
    }

    @Test
    void theCountIsRoundedFromTheExactScale() {
        when(contract.getExactScale()).thenReturn(1.5);

        StratConFacilityAttacks.scheduleFacilityAttacks(contract, campaignState, TODAY);

        // 1.5 rounds to 2 a month
        assertEquals(2 * LENGTH_IN_MONTHS, campaignState.getFacilityAttackDates().size());
    }

    @Test
    void aContractNeverScheduledIsScheduledOnItsNextDay() {
        StratConFacilityAttacks.processFacilityAttacks(campaign, contract, campaignState);

        assertEquals(TODAY.plusMonths(LENGTH_IN_MONTHS), campaignState.getFacilityAttacksScheduledUntil());
        for (LocalDate attackDate : campaignState.getFacilityAttackDates()) {
            assertTrue(attackDate.isAfter(TODAY), "those due today were already taken");
        }
    }

    // When none come

    @Test
    void noAttackComesWithoutAFacilityOnThePlayersSide() {
        placeFacility(ForceAlignment.Opposing);

        try (MockedStatic<StratConRulesManager> rulesManager = mockStatic(StratConRulesManager.class)) {
            assertNull(StratConFacilityAttacks.launchFacilityAttack(campaign, contract, campaignState));

            rulesManager.verify(() -> StratConRulesManager.startCounterattackScenario(any(), any(), any(), any()),
                  never());
        }
    }

    @Test
    void attacksDueWhileTheContractIsPeacefulAreDropped() {
        placeFacility(ForceAlignment.Allied);
        when(contract.isPeaceful()).thenReturn(true);
        campaignState.setFacilityAttacksScheduledUntil(TODAY.plusDays(10));
        campaignState.getFacilityAttackDates().add(TODAY);

        try (MockedStatic<StratConRulesManager> rulesManager = mockStatic(StratConRulesManager.class)) {
            StratConFacilityAttacks.processFacilityAttacks(campaign, contract, campaignState);

            rulesManager.verify(() -> StratConRulesManager.startCounterattackScenario(any(), any(), any(), any()),
                  never());
        }
        assertTrue(campaignState.getFacilityAttackDates().isEmpty());
    }

    // Which kind of attack

    @Test
    void anInterdictionOrCounterattackIsPossibleWithASupplyRoute() {
        assertEquals(List.of(FacilityAttackType.SUPPLY_INTERDICTION, FacilityAttackType.COUNTERATTACK),
              StratConFacilityAttacks.getPossibleAttackTypes(true));
    }

    @Test
    void withoutASupplyRouteOnlyACounterattackIsPossible() {
        assertEquals(List.of(FacilityAttackType.COUNTERATTACK), StratConFacilityAttacks.getPossibleAttackTypes(false));
    }

    @Test
    void aCounterattackIsTwiceAsLikelyAsAnInterdiction() {
        try (MockedStatic<Compute> dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.randomInt(3)).thenReturn(0, 1, 2);

            assertEquals(FacilityAttackType.SUPPLY_INTERDICTION, StratConFacilityAttacks.pickAttackType(true));
            assertEquals(FacilityAttackType.COUNTERATTACK, StratConFacilityAttacks.pickAttackType(true));
            assertEquals(FacilityAttackType.COUNTERATTACK, StratConFacilityAttacks.pickAttackType(true));
        }
    }

    @Test
    void aCounterattackIsFoughtTheDayItComes() {
        placeFacility(ForceAlignment.Player);
        StratConScenario scenario = mock(StratConScenario.class);

        try (MockedStatic<StratConRulesManager> rulesManager = mockStatic(StratConRulesManager.class);
              MockedStatic<Compute> dice = mockStatic(Compute.class, CALLS_REAL_METHODS)) {
            dice.when(() -> Compute.randomInt(anyInt())).thenReturn(0);
            rulesManager.when(() -> StratConRulesManager.startCounterattackScenario(any(), any(), any(), any()))
                  .thenReturn(scenario);

            assertEquals(FacilityAttackType.COUNTERATTACK,
                  StratConFacilityAttacks.launchFacilityAttack(campaign, contract, campaignState));

            rulesManager.verify(() -> StratConRulesManager.startCounterattackScenario(eq(campaign),
                  eq(contract),
                  eq(track),
                  eq(FACILITY_COORDS)));
        }
    }
}
