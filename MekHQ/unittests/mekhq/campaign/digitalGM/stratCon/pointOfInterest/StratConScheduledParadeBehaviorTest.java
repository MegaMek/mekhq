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
import static mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConConfiguredPointOfInterestType.VULNERABLE_INFRASTRUCTURE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
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
import mekhq.campaign.mission.scenarios.ScenarioTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the scheduled parade, which shares the ambush rules of vulnerable infrastructure (see
 * {@link StratConVulnerableInfrastructureBehaviorTest} for those in depth): these cover what differs - its Crowd
 * Control scenario, its text, and its visibility - and that each of its outcomes plays out.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConScheduledParadeBehaviorTest {
    private static final String TYPE_ID = "UnitTestScheduledParade";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords PARADE_COORDS = new StratConCoords(1, 1);
    private static final Money COMBAT_PAY = Money.of(25000);

    private StratConTrackState track;
    private StratConPointOfInterest parade;
    private StratConStrategicObjective objective;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConScheduledParadeBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        parade = new StratConPointOfInterest(TYPE_ID, PARADE_COORDS);
        assertTrue(track.addPointOfInterest(parade), "test setup: the parade should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, parade);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With odds no roll can meet, no scenario can break out, so the
     * parade is never disrupted.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        // Odds no roll can meet, so no scenario can break out.
        track.setScenarioOdds(-100);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        StratConCampaignState campaignState = new StratConCampaignState();
        campaignState.setContractsUseSpecialMechanics(true);
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.RETAINER);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              COMBAT_PAY));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    /** Checks the single general report the campaign received comes from a real resource key. */
    private static void assertGeneralReportIsValid(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        assertTrue(isResourceKeyValid(reportCaptor.getValue()), "missing resource key: " + reportCaptor.getValue());
    }

    // What differs

    @Test
    void theScheduledParadeBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConScheduledParadeBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConScheduledParadeBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConScheduledParadeBehavior.class, parade.getBehavior());
    }

    @Test
    void aDisruptedParadeIsFoughtAsCrowdControl() {
        assertEquals("Crowd Control.json", new StratConScheduledParadeBehavior().getScenarioTemplateName());
    }

    @Test
    void aParadeRiotIsNotAnAmbushJustAsCivilDisobediencesIsNot() {
        assertFalse(new StratConScheduledParadeBehavior().isScenarioAnAmbush(),
              "a parade riot is not a Crisis, so it carries Civil Disobedience's stakes");
        assertFalse(new StratConCivilDisobedienceBehavior().isScenarioAnAmbush());
    }

    @Test
    void theRiotTemplateIsReadFromItsFileRatherThanTheScenarioManifest() {
        // Crowd Control is kept out of the manifest so it is never a random scenario; looking it up there finds
        // nothing, and the riot would become a random scenario with no mobs.
        StratConScheduledParadeBehavior behavior = new StratConScheduledParadeBehavior();
        ScenarioTemplate template = behavior.loadScenarioTemplate(behavior.getScenarioTemplateName());

        assertNotNull(template, "Crowd Control is read from the scenario templates directory");
        assertEquals("Crowd Control", template.name);
        assertNotNull(new StratConCivilDisobedienceBehavior().loadScenarioTemplate(StratConRiots.SCENARIO_TEMPLATE),
              "civil disobedience riots read the same file");
    }

    @Test
    void aRiotCanBreakOutWhileTheEnemyIsRouted() {
        assertTrue(new StratConScheduledParadeBehavior().isScenarioPossibleWhileRouted(),
              "civil unrest does not answer to the enemy's morale");
        assertFalse(VULNERABLE_INFRASTRUCTURE.createBehavior().isScenarioPossibleWhileRouted(),
              "a routed enemy still mounts no ordinary ambushes");
    }

    @Test
    void ignoringARoutLeavesTheUsualOdds() {
        AbstractContract routedContract = mock(AbstractContract.class);
        when(routedContract.getMoraleLevel()).thenReturn(ContractMoraleLevel.ROUTED);
        track.setScenarioOdds(30);

        assertEquals(-1, StratConRulesManager.calculateScenarioOdds(track, routedContract, true),
              "a routed enemy normally mounts nothing");
        assertEquals(30, StratConRulesManager.calculateScenarioOdds(track, routedContract, true, true));
    }

    @Test
    void theObjectiveHasItsOwnText() {
        String description = parade.getBehavior().getObjectiveDescription(parade, track);

        assertNotNull(description);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    @Test
    void aParadeIsVisibleWithoutBeingScouted() {
        assertTrue(parade.isVisibleToPlayer(track));
    }

    // Each outcome, with its own text

    @ParameterizedTest
    @ValueSource(ints = { MEK, AEROSPACE_FIGHTER })
    void anyFormationHoldsTheParadeWhenItIsNotDisrupted(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);
        Finances finances = campaign.getPlayerForce().getFinances();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
              StratConPointOfInterestRules.processFormationDeployment(track, PARADE_COORDS, FORMATION_ID, campaign));

        assertNull(track.getPointOfInterest(parade.getId()));
        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(COMBAT_PAY), anyString());
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void aDisruptedParadeIsCalledOffWhateverTheResult() {
        parade.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertNull(track.getPointOfInterest(parade.getId()));
        assertFalse(track.getStrategicObjectives().contains(objective), "its objective is removed, not failed");
        verify(finances, never()).credit(any(), any(), any(), anyString());
        assertGeneralReportIsValid(campaign);
    }

    @Test
    void aParadeNotHeldInTimeIsMissed() {
        parade.setExpiryDate(TODAY);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertEquals(PointOfInterestStatus.EXPIRED, parade.getStatus());
        assertTrue(objective.isObjectiveFailed(track));
    }
}
