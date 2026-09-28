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
package mekhq.campaign.digitalGM.stratCon.pointOfInterest;

import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.CONV_FIGHTER;
import static megamek.common.units.UnitType.DROPSHIP;
import static megamek.common.units.UnitType.INFANTRY;
import static megamek.common.units.UnitType.MEK;
import static megamek.common.units.UnitType.TANK;
import static megamek.common.units.UnitType.VTOL;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.DATA_CACHE;
import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.finances.enums.TransactionType;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the data cache point of interest: which formations can recover one, securing it when no scenario breaks
 * out, winning or losing the scenario fought over it, the combat bonus paid for each one secured, its objective, and
 * its rolled lifespan.
 *
 * <p>The contested path - a scenario breaking out and being placed over the cache - builds a full scenario from a
 * template, so it is not covered here; its outcome is, through {@link StratConPointOfInterestRules#processScenarioEnded}
 * and the daily step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConDataCacheBehaviorTest {
    private static final String TYPE_ID = "UnitTestDataCache";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords CACHE_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest dataCache;
    private StratConStrategicObjective objective;
    // the active contract of the campaign built by deploymentCampaign
    private AbstractContract contract;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(DATA_CACHE.getBehaviorId());
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(true);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        dataCache = new StratConPointOfInterest(TYPE_ID, CACHE_COORDS);
        assertTrue(track.addPointOfInterest(dataCache), "test setup: the data cache should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, dataCache);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract whose map holds the test sector, and one player formation of the given
     * primary unit type.
     */
    private Campaign deploymentCampaign(int primaryUnitType, ContractMoraleLevel moraleLevel,
          boolean isScenarioRuledOut) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        if (isScenarioRuledOut) {
            // Odds no roll can meet, so no scenario can break out.
            track.setScenarioOdds(-100);
        }
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.setContractsUseSpecialMechanics(true);
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.ESPIONAGE);
        when(contract.getMoraleLevel()).thenReturn(moraleLevel);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private static Campaign dailyCampaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, CACHE_COORDS, FORMATION_ID, campaign);
    }

    private void assertSecured() {
        assertNull(track.getPointOfInterest(dataCache.getId()), "a secured cache leaves the map");
        assertEquals(PointOfInterestStatus.RESOLVED, dataCache.getStatus());
        assertTrue(objective.isObjectiveCompleted(track));
        assertFalse(objective.isObjectiveFailed(track));
    }

    private void assertLost() {
        assertNull(track.getPointOfInterest(dataCache.getId()), "a lost cache leaves the map");
        assertFalse(objective.isObjectiveCompleted(track));
        assertTrue(objective.isObjectiveFailed(track));
    }

    private void assertUntouched() {
        assertSame(dataCache, track.getPointOfInterest(dataCache.getId()));
        assertTrue(dataCache.isActive());
        assertFalse(dataCache.hasLinkedScenario());
        assertFalse(objective.isObjectiveResolved(track));
    }

    // Registration

    @Test
    void theDataCacheBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConContestedPointOfInterestBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(DATA_CACHE.getBehaviorId()));
        assertInstanceOf(StratConContestedPointOfInterestBehavior.class, dataCache.getBehavior());
    }

    @Test
    void theObjectiveIsDescribedAsRetrievingTheCache() {
        String description = dataCache.getBehavior().getObjectiveDescription(dataCache, track);

        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    // Deployment

    @ParameterizedTest
    @ValueSource(ints = { MEK, TANK, INFANTRY, VTOL, CONV_FIGHTER, AEROSPACE_FIGHTER, DROPSHIP })
    void anyFormationSecuresTheCacheWhenNoScenarioBreaksOut(int primaryUnitType) {
        // No scenario can break out.
        Campaign campaign = deploymentCampaign(primaryUnitType, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertSecured();
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void aRoutedEnemyCannotContestTheCache() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.ROUTED, false);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertSecured();
    }

    @Test
    void aCacheOutsideAnyActiveContractIsLeftAlone() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        when(campaign.getActiveContracts()).thenReturn(List.of());

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertUntouched();
    }

    @Test
    void aFormationJoiningTheBattleOverTheCacheDoesNotRollAgain() {
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(dataCache, track.getPointOfInterest(dataCache.getId()));
        assertTrue(dataCache.isActive());
        assertEquals(SCENARIO_ID, dataCache.getLinkedScenarioId(), "still waiting on the same battle");
    }

    @Test
    void aSecuredCacheIsNotAskedAboutLaterDeployments() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        deploy(campaign);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign), "nothing is left on the hex");
    }

    // The scenario fought over the cache

    @Test
    void linkingAScenarioRecordsItsBackingScenario() {
        StratConScenario scenario = new StratConScenario();
        scenario.setBackingScenarioID(SCENARIO_ID);

        StratConPointOfInterestRules.linkScenario(dataCache, scenario);

        assertTrue(dataCache.hasLinkedScenario());
        assertEquals(SCENARIO_ID, dataCache.getLinkedScenarioId());
    }

    @Test
    void winningTheScenarioSecuresTheCache() {
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = dailyCampaign();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertSecured();
        assertFalse(dataCache.hasLinkedScenario());
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void losingTheScenarioLosesTheCache() {
        // A draw is not an overall victory, so it reaches the cache as a loss too.
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = dailyCampaign();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertLost();
        assertFalse(dataCache.hasLinkedScenario());
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void anotherScenarioEndingLeavesTheCacheAlone() {
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = dailyCampaign();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID + 1, true, campaign);

        assertSame(dataCache, track.getPointOfInterest(dataCache.getId()));
        assertTrue(dataCache.isActive());
        assertEquals(SCENARIO_ID, dataCache.getLinkedScenarioId());
        verify(campaign, never()).addReport(any(), anyString());
    }

    @Test
    void aScenarioThatLeavesTheMapUnplayedLosesTheCache() {
        // Linked to a scenario the sector no longer holds - ignored, or removed by a GM.
        dataCache.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertLost();
    }

    // Combat bonus

    private Money giveContractCombatPay() {
        Money combatPay = Money.of(25000);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              combatPay));
        return combatPay;
    }

    @Test
    void securingACacheWithoutAFightPaysTheCombatBonus() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        Money combatPay = giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(combatPay), anyString());
        verify(campaign).addReport(eq(BATTLE), anyString());
    }

    @Test
    void winningTheScenarioOverTheCachePaysTheCombatBonus() {
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        Money combatPay = giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(combatPay), anyString());
    }

    @Test
    void losingTheCachePaysNothing() {
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void aCacheThatExpiresPaysNothing() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();
        dataCache.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);

        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void aContractWithoutCombatPayPaysNothing() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        assertSecured();
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    // Expiry

    @Test
    void anUnrecoveredCacheExpiresAndIsLost() {
        dataCache.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertEquals(PointOfInterestStatus.EXPIRED, dataCache.getStatus());
        assertLost();
    }

    @Test
    void aCacheWaitingOnItsScenarioDoesNotExpire() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(CACHE_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        dataCache.setLinkedScenarioId(SCENARIO_ID);
        dataCache.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(dataCache, track.getPointOfInterest(dataCache.getId()));
        assertTrue(dataCache.isActive());
        assertEquals(SCENARIO_ID, dataCache.getLinkedScenarioId());
        assertFalse(objective.isObjectiveResolved(track));
    }

    // Lifespan

    @Test
    void aCacheLastsOneToSixDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);
        boolean[] rolled = new boolean[7];

        for (int attempt = 0; attempt < 600; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, CACHE_COORDS, TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 6), "rolled a lifespan of " + lifespanDays);
            rolled[lifespanDays] = true;
        }

        for (int lifespanDays = 1; lifespanDays <= 6; lifespanDays++) {
            assertTrue(rolled[lifespanDays], "a lifespan of " + lifespanDays + " should come up in 600 rolls");
        }
    }

    @Test
    void theLifespanDieAddsToAFixedLifespan() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setLifespanDays(10);
        definition.setLifespanDieSides(6);

        for (int attempt = 0; attempt < 100; attempt++) {
            int lifespanDays = definition.rollLifespanDays();
            assertTrue((lifespanDays >= 11) && (lifespanDays <= 16), "rolled a lifespan of " + lifespanDays);
        }
    }

    @Test
    void withoutALifespanDieTheLifespanIsFixed() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setLifespanDays(4);

        assertEquals(4, definition.rollLifespanDays());

        definition.setLifespanDays(0);
        assertNull(StratConPointOfInterest.fromDefinition(definition, CACHE_COORDS, TODAY).getExpiryDate(),
              "no lifespan and no die means it never expires");
    }
}
