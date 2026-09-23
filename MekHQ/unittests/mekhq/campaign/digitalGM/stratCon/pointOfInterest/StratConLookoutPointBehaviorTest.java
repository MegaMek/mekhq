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
import static megamek.common.units.UnitType.INFANTRY;
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.LOOKOUT_POINT;
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
 * Tests for the lookout point: observing from one when no scenario breaks out (by any formation, for the combat
 * bonus), an ambushed one being compromised - taken off the map with its objective removed, neither met nor failed -
 * whatever the result of the ambush, its visibility, and its rolled lifespan.
 *
 * <p>The ambush itself builds a full scenario from a template and shows the ambush dialog, so it is not covered here;
 * its outcome is, through {@link StratConPointOfInterestRules#processScenarioEnded} and the daily step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConLookoutPointBehaviorTest {
    private static final String TYPE_ID = "UnitTestLookoutPoint";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords LOOKOUT_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest lookoutPoint;
    private StratConStrategicObjective objective;
    // the active contract of the campaign built by deploymentCampaign
    private AbstractContract contract;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(LOOKOUT_POINT.getBehaviorId());
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        lookoutPoint = new StratConPointOfInterest(TYPE_ID, LOOKOUT_COORDS);
        assertTrue(track.addPointOfInterest(lookoutPoint), "test setup: the lookout point should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, lookoutPoint);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract whose map holds the test sector, and one player formation of the given
     * primary unit type. With odds no roll can meet, no scenario can break out, so there is never an ambush.
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
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.OBSERVATION_RAID);
        when(contract.getMoraleLevel()).thenReturn(moraleLevel);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private Money giveContractCombatPay() {
        Money combatPay = Money.of(25000);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              combatPay));
        return combatPay;
    }

    private static Campaign dailyCampaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, LOOKOUT_COORDS, FORMATION_ID, campaign);
    }

    private void assertObserved() {
        assertNull(track.getPointOfInterest(lookoutPoint.getId()), "a used lookout point leaves the map");
        assertEquals(PointOfInterestStatus.RESOLVED, lookoutPoint.getStatus());
        assertTrue(objective.isObjectiveCompleted(track));
        assertFalse(objective.isObjectiveFailed(track));
    }

    private void assertCompromised() {
        assertNull(track.getPointOfInterest(lookoutPoint.getId()), "a compromised lookout point leaves the map");
        assertFalse(track.getStrategicObjectives().contains(objective), "its objective is removed, not failed");
        assertFalse(lookoutPoint.hasLinkedScenario());
    }

    // Registration

    @Test
    void theLookoutPointBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConAmbushPointOfInterestBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(LOOKOUT_POINT.getBehaviorId()));
        assertInstanceOf(StratConAmbushPointOfInterestBehavior.class, lookoutPoint.getBehavior());
    }

    @Test
    void theObjectiveIsDescribedAsObservingEnemyMovements() {
        String description = lookoutPoint.getBehavior().getObjectiveDescription(lookoutPoint, track);

        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    // Observing

    @ParameterizedTest
    @ValueSource(ints = { MEK, INFANTRY, AEROSPACE_FIGHTER })
    void anyFormationObservesWhenThereIsNoAmbush(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertObserved();
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void observingPaysTheCombatBonus() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        Money combatPay = giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(combatPay), anyString());
    }

    @Test
    void aRoutedEnemyCannotAmbushTheLookoutPoint() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.ROUTED, false);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertObserved();
    }

    @Test
    void aFormationJoiningTheAmbushDoesNotRollAgain() {
        lookoutPoint.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(lookoutPoint, track.getPointOfInterest(lookoutPoint.getId()));
        assertEquals(SCENARIO_ID, lookoutPoint.getLinkedScenarioId());
    }

    // An ambushed lookout point is compromised whatever the result

    @Test
    void winningTheAmbushStillCompromisesTheLookoutPoint() {
        lookoutPoint.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertCompromised();
        verify(finances, never()).credit(any(), any(), any(), anyString());
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void losingTheAmbushCompromisesTheLookoutPoint() {
        lookoutPoint.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false,
              deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false));

        assertCompromised();
    }

    @Test
    void anAmbushLeftUnplayedCompromisesTheLookoutPoint() {
        lookoutPoint.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertCompromised();
    }

    // On the map

    @Test
    void aLookoutPointIsVisibleWithoutBeingScouted() {
        assertFalse(lookoutPoint.isRevealed());
        assertTrue(lookoutPoint.isVisibleToPlayer(track));
    }

    @Test
    void aLookoutPointNotUsedInTimeExpires() {
        lookoutPoint.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertNull(track.getPointOfInterest(lookoutPoint.getId()));
        assertEquals(PointOfInterestStatus.EXPIRED, lookoutPoint.getStatus());
        assertTrue(objective.isObjectiveFailed(track));
    }

    @Test
    void aLookoutPointWaitingOnItsAmbushDoesNotExpire() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(LOOKOUT_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        lookoutPoint.setLinkedScenarioId(SCENARIO_ID);
        lookoutPoint.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(lookoutPoint, track.getPointOfInterest(lookoutPoint.getId()));
        assertTrue(lookoutPoint.isActive());
    }

    @Test
    void aLookoutPointLastsOneToSixDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);

        for (int attempt = 0; attempt < 100; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, LOOKOUT_COORDS,
                  TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 6), "rolled a lifespan of " + lifespanDays);
        }
    }
}
