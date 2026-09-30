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
package mekhq.campaign.universe.factionStanding;

import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_1;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_2;
import static mekhq.campaign.universe.factionStanding.FactionStandingLevel.STANDING_LEVEL_3;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.enums.DailyReportType;
import mekhq.campaign.force.PlayerForce;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the faction-standing adjustments performed by {@link GoingRogue}.
 *
 * <p>These tests exercise the pure standing helpers used when a force goes rogue or resolves a Faction Standing
 * ultimatum. The wider {@code processGoingRogue} flow builds immersive dialogs in its constructors, so it is not
 * suitable for headless unit testing; the standing math, however, is fully deterministic and is covered here.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class GoingRogueTest {
    private static final String PLAYER_FACTION_CODE = "LA";
    private static final String OTHER_FACTION_CODE = "FS";
    private static final int GAME_YEAR = 3025;

    /**
     * Builds a campaign whose player force reports the supplied player faction and standings.
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static Campaign buildCampaign(Faction playerFaction, FactionStandings factionStandings) {
        Campaign campaign = mock(Campaign.class);
        PlayerForce playerForce = mock(PlayerForce.class);

        when(campaign.getPlayerForce()).thenReturn(playerForce);
        when(campaign.getGameYear()).thenReturn(GAME_YEAR);
        when(playerForce.getFaction()).thenReturn(playerFaction);
        when(playerForce.getFactionStandings()).thenReturn(factionStandings);

        return campaign;
    }

    @Test
    @DisplayName("processRegardBump raises regard to the next standing level's minimum")
    void testProcessRegardBumpRaisesRegardToNextLevelMinimum() {
        Faction playerFaction = mock(Faction.class);
        when(playerFaction.isAggregate()).thenReturn(false);
        when(playerFaction.getShortName()).thenReturn(PLAYER_FACTION_CODE);

        FactionStandings factionStandings = mock(FactionStandings.class);
        // -30.0 sits squarely inside STANDING_LEVEL_2, so the next level is STANDING_LEVEL_3.
        when(factionStandings.getRegardForFaction(PLAYER_FACTION_CODE, false)).thenReturn(-30.0);
        when(factionStandings.setRegardForFaction(anyString(), anyString(), eq(STANDING_LEVEL_3.getMinimumRegard()),
              eq(GAME_YEAR), eq(true))).thenReturn("REPORT");

        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processRegardBump(campaign);

        verify(factionStandings).setRegardForFaction(eq(PLAYER_FACTION_CODE), eq(PLAYER_FACTION_CODE),
              eq(STANDING_LEVEL_3.getMinimumRegard()), eq(GAME_YEAR), eq(true));
        verify(campaign).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("processRegardBump does nothing when regard already meets the next level's minimum")
    void testProcessRegardBumpNoOpWhenAlreadyAtThreshold() {
        Faction playerFaction = mock(Faction.class);
        when(playerFaction.isAggregate()).thenReturn(false);
        when(playerFaction.getShortName()).thenReturn(PLAYER_FACTION_CODE);

        FactionStandings factionStandings = mock(FactionStandings.class);
        // STANDING_LEVEL_2's upper bound (which matches STANDING_LEVEL_3's minimum) is classified as STANDING_LEVEL_2,
        // whose next-level target equals the current regard, so no bump should occur.
        when(factionStandings.getRegardForFaction(PLAYER_FACTION_CODE, false))
              .thenReturn(STANDING_LEVEL_2.getMaximumRegard());

        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processRegardBump(campaign);

        verify(factionStandings, never()).setRegardForFaction(anyString(), anyString(), anyDouble(), anyInt(), anyBoolean());
        verify(campaign, never()).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("processRegardBump does nothing when the faction is already at the maximum standing level")
    void testProcessRegardBumpNoOpAtMaximumStanding() {
        Faction playerFaction = mock(Faction.class);
        when(playerFaction.isAggregate()).thenReturn(false);
        when(playerFaction.getShortName()).thenReturn(PLAYER_FACTION_CODE);

        FactionStandings factionStandings = mock(FactionStandings.class);
        // 55.0 sits inside STANDING_LEVEL_8, so there is no higher level to advance to.
        when(factionStandings.getRegardForFaction(PLAYER_FACTION_CODE, false)).thenReturn(55.0);

        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processRegardBump(campaign);

        verify(factionStandings, never()).setRegardForFaction(anyString(), anyString(), anyDouble(), anyInt(), anyBoolean());
        verify(campaign, never()).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("processRegardBump ignores aggregate factions")
    void testProcessRegardBumpIgnoresAggregateFactions() {
        Faction playerFaction = mock(Faction.class);
        when(playerFaction.isAggregate()).thenReturn(true);

        FactionStandings factionStandings = mock(FactionStandings.class);
        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processRegardBump(campaign);

        verifyNoInteractions(factionStandings);
        verify(campaign, never()).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("processFactionStandingChangeForOldFaction lowers regard to the departing floor")
    void testProcessFactionStandingChangeForOldFactionLowersRegard() {
        Faction playerFaction = mock(Faction.class);
        when(playerFaction.getShortName()).thenReturn(PLAYER_FACTION_CODE);

        Faction oldFaction = mock(Faction.class);
        when(oldFaction.isAggregate()).thenReturn(false);
        when(oldFaction.getShortName()).thenReturn(OTHER_FACTION_CODE);

        FactionStandings factionStandings = mock(FactionStandings.class);
        // -30.0 is above the STANDING_LEVEL_1 floor, so it should be reduced down to that floor.
        when(factionStandings.getRegardForFaction(OTHER_FACTION_CODE, false)).thenReturn(-30.0);
        when(factionStandings.setRegardForFaction(anyString(), anyString(), eq(STANDING_LEVEL_1.getMinimumRegard()),
              eq(GAME_YEAR), eq(true))).thenReturn("REPORT");

        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processFactionStandingChangeForOldFaction(campaign, oldFaction);

        verify(factionStandings).setRegardForFaction(eq(PLAYER_FACTION_CODE), eq(OTHER_FACTION_CODE),
              eq(STANDING_LEVEL_1.getMinimumRegard()), eq(GAME_YEAR), eq(true));
        verify(campaign).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("processFactionStandingChangeForOldFaction leaves regard already at or below the floor untouched")
    void testProcessFactionStandingChangeForOldFactionNoOpWhenAlreadyLow() {
        Faction playerFaction = mock(Faction.class);
        when(playerFaction.getShortName()).thenReturn(PLAYER_FACTION_CODE);

        Faction oldFaction = mock(Faction.class);
        when(oldFaction.isAggregate()).thenReturn(false);
        when(oldFaction.getShortName()).thenReturn(OTHER_FACTION_CODE);

        FactionStandings factionStandings = mock(FactionStandings.class);
        // -55.0 is below the STANDING_LEVEL_1 floor, so there is nothing to reduce.
        when(factionStandings.getRegardForFaction(OTHER_FACTION_CODE, false)).thenReturn(-55.0);

        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processFactionStandingChangeForOldFaction(campaign, oldFaction);

        verify(factionStandings, never()).setRegardForFaction(anyString(), anyString(), anyDouble(), anyInt(), anyBoolean());
        verify(campaign, never()).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("processFactionStandingChangeForOldFaction ignores aggregate factions")
    void testProcessFactionStandingChangeForOldFactionIgnoresAggregateFactions() {
        Faction playerFaction = mock(Faction.class);
        Faction oldFaction = mock(Faction.class);
        when(oldFaction.isAggregate()).thenReturn(true);

        FactionStandings factionStandings = mock(FactionStandings.class);
        Campaign campaign = buildCampaign(playerFaction, factionStandings);

        GoingRogue.processFactionStandingChangeForOldFaction(campaign, oldFaction);

        verifyNoInteractions(factionStandings);
        verify(campaign, never()).addReport(eq(DailyReportType.POLITICS), anyString());
    }

    @Test
    @DisplayName("failsLoyaltyCheck: a roll below the target number of 6 fails")
    void testFailsLoyaltyCheckBaseTargetNumber() {
        assertTrue(GoingRogue.failsLoyaltyCheck(5, 0, 0));
        assertFalse(GoingRogue.failsLoyaltyCheck(6, 0, 0));
    }

    @Test
    @DisplayName("failsLoyaltyCheck: divisiveness raises or lowers the target number")
    void testFailsLoyaltyCheckAppliesDivisiveness() {
        // A divisive ultimatum (+2) makes a roll of 7 fail
        assertTrue(GoingRogue.failsLoyaltyCheck(7, 0, 2));
        assertFalse(GoingRogue.failsLoyaltyCheck(8, 0, 2));
        // A unifying ultimatum (-2) lets a roll of 4 pass
        assertFalse(GoingRogue.failsLoyaltyCheck(4, 0, -2));
        assertTrue(GoingRogue.failsLoyaltyCheck(3, 0, -2));
    }

    @Test
    @DisplayName("failsLoyaltyCheck: divisiveness stacks with the person's loyalty modifier")
    void testFailsLoyaltyCheckStacksWithLoyaltyModifier() {
        assertTrue(GoingRogue.failsLoyaltyCheck(8, 1, 2));
        assertFalse(GoingRogue.failsLoyaltyCheck(9, 1, 2));
        assertFalse(GoingRogue.failsLoyaltyCheck(6, -1, 1));
    }

    @Test
    @DisplayName("failsLoyaltyCheck: at the divisiveness limits, the best and worst rolls still behave sensibly")
    void testFailsLoyaltyCheckAtDivisivenessLimits() {
        // +6: target 12, so only a natural 12 passes for a neutral character
        assertTrue(GoingRogue.failsLoyaltyCheck(11, 0, FactionStandingUltimatumData.MAXIMUM_DIVISIVENESS));
        assertFalse(GoingRogue.failsLoyaltyCheck(12, 0, FactionStandingUltimatumData.MAXIMUM_DIVISIVENESS));
        // -6: target 0, so even a natural 2 passes for a neutral character
        assertFalse(GoingRogue.failsLoyaltyCheck(2, 0, FactionStandingUltimatumData.MINIMUM_DIVISIVENESS));
    }
}
