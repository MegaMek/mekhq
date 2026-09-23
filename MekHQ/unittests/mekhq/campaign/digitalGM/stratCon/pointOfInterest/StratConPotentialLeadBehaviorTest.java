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

import static megamek.common.units.UnitType.INFANTRY;
import static megamek.common.units.UnitType.MEK;
import static megamek.common.units.UnitType.TANK;
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
import mekhq.campaign.campaignOptions.CampaignOption;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the potential lead point of interest: which formations can follow one up, a dud being taken off the map
 * with its objective removed, winning or losing the Mole Hunt fought over one that pans out (and the combat bonus for a
 * win), its visibility, and its rolled lifespan.
 *
 * <p>A lead panning out - a Mole Hunt scenario being placed over it - builds a full scenario from a template, so it is
 * not covered here; its outcome is, through {@link StratConPointOfInterestRules#processScenarioEnded} and the daily
 * step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPotentialLeadBehaviorTest {
    private static final String TYPE_ID = "UnitTestPotentialLead";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords LEAD_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest lead;
    private StratConStrategicObjective objective;
    // the active contract of the campaign built by deploymentCampaign
    private AbstractContract contract;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConPotentialLeadBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        lead = new StratConPointOfInterest(TYPE_ID, LEAD_COORDS);
        assertTrue(track.addPointOfInterest(lead), "test setup: the lead should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, lead);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract whose map holds the test sector, and one player formation of the given
     * primary unit type. With Essential Scenarios Only on, no scenario can break out, so every lead is a dud.
     */
    private Campaign deploymentCampaign(int primaryUnitType, ContractMoraleLevel moraleLevel,
          boolean essentialScenariosOnly) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(essentialScenariosOnly);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
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
        return StratConPointOfInterestRules.processFormationDeployment(track, LEAD_COORDS, FORMATION_ID, campaign);
    }

    private void assertDud() {
        assertNull(track.getPointOfInterest(lead.getId()), "a dud leaves the map");
        assertFalse(track.getStrategicObjectives().contains(objective), "its objective is removed, not failed");
    }

    private void assertMoleCaught() {
        assertNull(track.getPointOfInterest(lead.getId()));
        assertEquals(PointOfInterestStatus.RESOLVED, lead.getStatus());
        assertTrue(objective.isObjectiveCompleted(track));
        assertFalse(objective.isObjectiveFailed(track));
    }

    private void assertMoleEscaped() {
        assertNull(track.getPointOfInterest(lead.getId()));
        assertTrue(track.getStrategicObjectives().contains(objective), "the objective stays, failed");
        assertFalse(objective.isObjectiveCompleted(track));
        assertTrue(objective.isObjectiveFailed(track));
    }

    // Registration

    @Test
    void thePotentialLeadBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConPotentialLeadBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConPotentialLeadBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConPotentialLeadBehavior.class, lead.getBehavior());
    }

    @Test
    void theObjectiveIsDescribedAsFollowingUpTheLead() {
        String description = lead.getBehavior().getObjectiveDescription(lead, track);

        assertNotNull(description);
        assertFalse(description.isBlank());
    }

    // Following it up

    @ParameterizedTest
    @ValueSource(ints = { MEK, TANK, INFANTRY })
    void aLeadWithNoScenarioIsADud(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType, ContractMoraleLevel.STALEMATE, true);
        giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertDud();
        verify(campaign).addReport(eq(GENERAL), anyString());
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void aRoutedEnemyLeavesOnlyDuds() {
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.ROUTED, false);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));
        assertDud();
    }

    @Test
    void aFormationJoiningTheMoleHuntDoesNotRollAgain() {
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, true);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(lead, track.getPointOfInterest(lead.getId()));
        assertEquals(SCENARIO_ID, lead.getLinkedScenarioId());
    }

    // The Mole Hunt fought over a lead that pans out

    @Test
    void winningTheMoleHuntMeetsTheObjectiveAndPaysTheCombatBonus() {
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        Money combatPay = giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertMoleCaught();
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(combatPay), anyString());
    }

    @Test
    void losingTheMoleHuntFailsTheObjective() {
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK, ContractMoraleLevel.STALEMATE, false);
        giveContractCombatPay();
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertMoleEscaped();
        verify(finances, never()).credit(any(), any(), any(), anyString());
        verify(campaign).addReport(eq(GENERAL), anyString());
    }

    @Test
    void aMoleHuntLeftUnplayedFailsTheObjective() {
        lead.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertMoleEscaped();
    }

    // On the map

    @Test
    void aLeadIsVisibleWithoutBeingScouted() {
        assertFalse(lead.isRevealed());
        assertTrue(lead.isVisibleToPlayer(track));
    }

    @Test
    void aLeadNotFollowedUpInTimeGoesCold() {
        lead.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertNull(track.getPointOfInterest(lead.getId()));
        assertEquals(PointOfInterestStatus.EXPIRED, lead.getStatus());
        assertTrue(objective.isObjectiveFailed(track));
    }

    @Test
    void aLeadWaitingOnItsMoleHuntDoesNotGoCold() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(LEAD_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        lead.setLinkedScenarioId(SCENARIO_ID);
        lead.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(lead, track.getPointOfInterest(lead.getId()));
        assertTrue(lead.isActive());
        assertFalse(objective.isObjectiveResolved(track));
    }

    @Test
    void aLeadLastsOneToSixDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);

        for (int attempt = 0; attempt < 100; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, LEAD_COORDS, TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 6), "rolled a lifespan of " + lifespanDays);
        }
    }
}
