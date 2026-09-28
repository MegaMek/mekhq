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
import static megamek.common.units.UnitType.DROPSHIP;
import static megamek.common.units.UnitType.INFANTRY;
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.VULNERABLE_INFRASTRUCTURE;
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
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
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
 * Tests for the vulnerable infrastructure point of interest: destroying it when no scenario breaks out (by any
 * formation, for the combat bonus), and a trap being spent - taken off the map with its objective removed, neither met
 * nor failed - whatever the result of the ambush it sprang.
 *
 * <p>The trap itself - an ambush being placed over the infrastructure - builds a full scenario from a template and
 * shows the ambush dialog, so it is not covered here; its outcome is, through
 * {@link StratConPointOfInterestRules#processScenarioEnded} and the daily step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConVulnerableInfrastructureBehaviorTest {
    private static final String TYPE_ID = "UnitTestVulnerableInfrastructure";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords TARGET_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest infrastructure;
    private StratConStrategicObjective objective;
    // the active contract of the campaign built by deploymentCampaign
    private AbstractContract contract;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(VULNERABLE_INFRASTRUCTURE.getBehaviorId());
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        infrastructure = new StratConPointOfInterest(TYPE_ID, TARGET_COORDS);
        assertTrue(track.addPointOfInterest(infrastructure), "test setup: the infrastructure should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, infrastructure);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract whose map holds the test sector, and one player formation of the given
     * primary unit type. With odds no roll can meet, no scenario can break out, so the infrastructure is never a
     * trap.
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
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.GUERRILLA_WARFARE);
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

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, TARGET_COORDS, FORMATION_ID, campaign);
    }

    private void assertDestroyed() {
        assertNull(track.getPointOfInterest(infrastructure.getId()), "destroyed infrastructure leaves the map");
        assertEquals(PointOfInterestStatus.RESOLVED, infrastructure.getStatus());
        assertTrue(track.getStrategicObjectives().contains(objective));
        assertTrue(objective.isObjectiveCompleted(track));
        assertFalse(objective.isObjectiveFailed(track));
    }

    private void assertTrapSpent() {
        assertNull(track.getPointOfInterest(infrastructure.getId()), "a spent trap leaves the map");
        assertFalse(track.getStrategicObjectives().contains(objective), "its objective is removed, not failed");
        assertFalse(infrastructure.hasLinkedScenario());
    }

    // Registration

    @Test
    void theVulnerableInfrastructureBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConAmbushPointOfInterestBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(VULNERABLE_INFRASTRUCTURE.getBehaviorId()));
        assertInstanceOf(StratConAmbushPointOfInterestBehavior.class, infrastructure.getBehavior());
    }

    @Test
    void theObjectiveIsDescribedAsDestroyingTheInfrastructure() {
        String description = infrastructure.getBehavior().getObjectiveDescription(infrastructure, track);

        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    // Destroying it

    @ParameterizedTest
    @ValueSource(ints = { MEK, INFANTRY, AEROSPACE_FIGHTER, DROPSHIP })
    void anyFormationDestroysTheInfrastructureWhenItIsNotATrap(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertDestroyed();
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void aRoutedEnemyCannotSpringATrap() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.ROUTED, false);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertDestroyed();
    }

    @Test
    void destroyingTheInfrastructurePaysTheCombatBonus() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        Money combatPay = giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(combatPay), anyString());
        verify(campaign).addReport(eq(BATTLE), anyString());
    }

    @Test
    void infrastructureOutsideAnyActiveContractIsLeftAlone() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);
        when(campaign.getActiveContracts()).thenReturn(List.of());

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(infrastructure, track.getPointOfInterest(infrastructure.getId()));
        assertTrue(infrastructure.isActive());
        assertFalse(objective.isObjectiveResolved(track));
    }

    @Test
    void aFormationJoiningASprungTrapDoesNotRollAgain() {
        infrastructure.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(infrastructure, track.getPointOfInterest(infrastructure.getId()));
        assertEquals(SCENARIO_ID, infrastructure.getLinkedScenarioId());
    }

    // A trap is spent whatever the ambush's result

    @Test
    void winningTheAmbushSpendsTheTrap() {
        infrastructure.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertTrapSpent();
        verify(finances, never()).credit(any(), any(), any(), anyString());
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void losingTheAmbushSpendsTheTrap() {
        infrastructure.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertTrapSpent();
    }

    @Test
    void anAmbushLeftUnplayedSpendsTheTrap() {
        // Linked to a scenario the sector no longer holds - ignored, or removed by a GM.
        infrastructure.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertTrapSpent();
    }

    @Test
    void spendingATrapLeavesTheSectorsOtherObjectivesAlone() {
        StratConStrategicObjective otherObjective = new StratConStrategicObjective();
        otherObjective.setObjectiveType(StrategicObjectiveType.AnyScenarioVictory);
        otherObjective.setDesiredObjectiveCount(2);
        track.addStrategicObjective(otherObjective);
        infrastructure.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false,
              deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false));

        assertEquals(List.of(otherObjective), track.getStrategicObjectives());
        assertFalse(otherObjective.isObjectiveFailed(track));
    }

    // Lifespan

    @Test
    void vulnerableInfrastructureNeverExpires() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);
        assertNull(StratConPointOfInterest.fromDefinition(definition, TARGET_COORDS, TODAY).getExpiryDate());

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY.plusYears(1));
        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertSame(infrastructure, track.getPointOfInterest(infrastructure.getId()));
        assertTrue(infrastructure.isActive());
    }
}
