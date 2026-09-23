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
import static megamek.common.units.UnitType.MEK;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
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
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
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
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the assassination lead, which shares a potential lead's rules (see
 * {@link StratConPotentialLeadBehaviorTest}) but is never a dud and mostly turns up body doubles: these cover its
 * scenario, its text, the real target's fight, and a body double's.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConAssassinationLeadBehaviorTest {
    private static final String TYPE_ID = "UnitTestAssassinationLead";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords LEAD_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest lead;
    private StratConStrategicObjective objective;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConAssassinationLeadBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
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
     * A campaign holding one active contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(true);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              Money.of(25000)));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    /** @return the single general report the campaign received, checked to come from a real resource key */
    private static String generalReport(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        String report = reportCaptor.getValue();
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
        return report;
    }

    // What differs from a potential lead

    @Test
    void theAssassinationLeadBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConAssassinationLeadBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConAssassinationLeadBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConAssassinationLeadBehavior.class, lead.getBehavior());
    }

    @Test
    void aLeadThatPansOutIsFoughtAsAnAssassination() {
        StratConAssassinationLeadBehavior behavior = new StratConAssassinationLeadBehavior();

        assertEquals("Assassination.json", behavior.getScenarioTemplateName());
        assertFalse(new StratConPotentialLeadBehavior().getScenarioTemplateName()
                          .equals(behavior.getScenarioTemplateName()), "not a Mole Hunt");
    }

    @Test
    void theObjectiveHasItsOwnText() {
        String description = lead.getBehavior().getObjectiveDescription(lead, track);

        assertNotNull(description);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    // Each outcome still plays out, with its own text

    @Test
    void aFormationThatIsNotOnTheGroundCannotFollowUpALead() {
        Campaign campaign = deploymentCampaign(AEROSPACE_FIGHTER);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT,
              StratConPointOfInterestRules.processFormationDeployment(track, LEAD_COORDS, FORMATION_ID, campaign));
        assertSame(lead, track.getPointOfInterest(lead.getId()));
    }

    @Test
    void winningTheAssassinationMeetsTheObjectiveAndPaysTheCombatBonus() {
        markRealTarget();
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(Money.of(25000)), anyString());
        generalReport(campaign);
    }

    @Test
    void losingTheAssassinationFailsTheObjective() {
        markRealTarget();
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertTrue(objective.isObjectiveFailed(track));
        generalReport(campaign);
    }

    // Body doubles

    @Test
    void noLeadIsEverADud() {
        StratConAssassinationLeadBehavior behavior = new StratConAssassinationLeadBehavior();
        assertTrue(behavior.isContestCertain(lead), "a body double always fights");

        markRealTarget();
        assertTrue(behavior.isContestCertain(lead), "the real target always fights");
    }

    @Test
    void beatingABodyDoubleWithdrawsItsObjectiveButPaysTheCombatBonus() {
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertNull(track.getPointOfInterest(lead.getId()));
        assertFalse(track.getStrategicObjectives().contains(objective), "neither met nor failed");
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(Money.of(25000)), anyString());
        generalReport(campaign);
    }

    @Test
    void losingToABodyDoubleWithdrawsItsObjectiveAndPaysNothing() {
        lead.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertNull(track.getPointOfInterest(lead.getId()));
        assertFalse(track.getStrategicObjectives().contains(objective), "neither met nor failed");
        verify(finances, never()).credit(any(), any(), any(), anyString());
        generalReport(campaign);
    }

    private void markRealTarget() {
        lead.setStateValue(StratConAssassinationLeadBehavior.REAL_TARGET_STATE_KEY, "true");
    }
}
