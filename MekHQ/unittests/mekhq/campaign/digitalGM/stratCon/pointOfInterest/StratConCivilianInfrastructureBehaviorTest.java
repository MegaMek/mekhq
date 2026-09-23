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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Tests for civilian infrastructure, which shares the contested rules of the data cache (see
 * {@link StratConDataCacheBehaviorTest} for those in depth): these cover what differs - any formation can attack it,
 * its defenders spring an ambush suited to the attackers, it is hidden and never expires, and the Escalation it raises
 * - and that each of its outcomes plays out.
 *
 * <p>Destroying it without a fight raises Escalation by 3d6. Winning the ambush raises it by 2d6; the won scenario's
 * usual +1d6 for winning is added when the scenario resolves, not here. Losing adds nothing.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConCivilianInfrastructureBehaviorTest {
    private static final String TYPE_ID = "UnitTestCivilianInfrastructure";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords TARGET_COORDS = new StratConCoords(1, 1);
    private static final Money COMBAT_PAY = Money.of(25000);

    private StratConTrackState track;
    private StratConPointOfInterest infrastructure;
    private StratConStrategicObjective objective;
    // the StratCon state of the contract in the campaign built by deploymentCampaign
    private StratConCampaignState campaignState;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConCivilianInfrastructureBehavior.BEHAVIOR_ID);
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
     * A campaign with special mechanics on, holding one active Terrorism contract - with combat pay - whose map holds
     * the test sector, and one player formation of the given primary unit type. With Essential Scenarios Only on, no
     * scenario can break out, so there is never an ambush.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(true);
        when(options.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)).thenReturn(true);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.TERRORISM);
        when(contract.getScale()).thenReturn(1);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              COMBAT_PAY));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private void assertEscalationBetween(int minimum, int maximum) {
        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= minimum) && (escalation <= maximum),
              "Escalation should be " + minimum + " to " + maximum + ", got " + escalation);
    }

    /** Checks the single general report the campaign received comes from a real resource key. */
    private static void assertGeneralReportIsValid(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        assertTrue(isResourceKeyValid(reportCaptor.getValue()), "missing resource key: " + reportCaptor.getValue());
    }

    // What differs

    @Test
    void theCivilianInfrastructureBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConCivilianInfrastructureBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConCivilianInfrastructureBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConCivilianInfrastructureBehavior.class, infrastructure.getBehavior());
    }

    @Test
    void itsDefendersSpringAnAmbushSuitedToTheAttackers() {
        assertNull(new StratConCivilianInfrastructureBehavior().getScenarioTemplateName(),
              "no fixed template: the ambush is drawn for the attackers' unit type");
    }

    @Test
    void theObjectiveHasItsOwnText() {
        String description = infrastructure.getBehavior().getObjectiveDescription(infrastructure, track);

        assertNotNull(description);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    @Test
    void civilianInfrastructureIsHiddenUntilScoutedAndNeverExpires() {
        assertFalse(infrastructure.isVisibleToPlayer(track));

        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY.plusYears(1));
        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertSame(infrastructure, track.getPointOfInterest(infrastructure.getId()));
        assertTrue(infrastructure.isActive());
    }

    // Each outcome, with its Escalation

    @ParameterizedTest
    @ValueSource(ints = { MEK, AEROSPACE_FIGHTER })
    void anyFormationDestroysItWithoutAFightForTheBonusAndThreeDiceOfEscalation(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);
        Finances finances = campaign.getPlayerForce().getFinances();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
              StratConPointOfInterestRules.processFormationDeployment(track, TARGET_COORDS, FORMATION_ID, campaign));

        assertNull(track.getPointOfInterest(infrastructure.getId()));
        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(COMBAT_PAY), anyString());
        assertEscalationBetween(3, 18);
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void winningTheAmbushDestroysItAndRaisesEscalationByTwoDice() {
        infrastructure.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertTrue(objective.isObjectiveCompleted(track));
        assertEscalationBetween(2, 12);
    }

    @Test
    void losingTheAmbushFailsTheObjectiveAndRaisesNoEscalation() {
        infrastructure.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertTrue(objective.isObjectiveFailed(track));
        assertEquals(0, campaignState.getEscalation());
        assertGeneralReportIsValid(campaign);
    }
}
