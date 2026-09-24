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

import megamek.common.compute.Compute;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTestDice;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
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
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the show of force: making it when no scenario breaks out (by any formation, lowering a Garrison Duty
 * contract's Escalation by 3d6 but paying no combat bonus - its contract keeps its Essential scenarios), its ambush
 * drawing a template suited to the ambushed unit and spending the show of force whatever the result, its visibility,
 * and its rolled lifespan. Shows of force are not strategic objectives: they only calm Escalation.
 *
 * <p>The ambush itself builds a full scenario from a template and shows the ambush dialog, so it is not covered here;
 * its outcome is, through {@link StratConPointOfInterestRules#processScenarioEnded} and the daily step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConShowOfForceBehaviorTest {
    private static final String TYPE_ID = "UnitTestShowOfForce";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords SHOW_COORDS = new StratConCoords(1, 1);
    private static final int STARTING_ESCALATION = 100;

    private StratConTrackState track;
    private StratConPointOfInterest show;
    // the StratCon state of the contract in the campaign built by deploymentCampaign
    private StratConCampaignState campaignState;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setDisplayableName("Show of Force");
        definition.setBehaviorId(StratConShowOfForceBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        show = new StratConPointOfInterest(TYPE_ID, SHOW_COORDS);
        assertTrue(track.addPointOfInterest(show), "test setup: the show of force should be placed");
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active Garrison Duty contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With odds no roll can meet, no scenario can break out, so there is
     * never an ambush.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        return deploymentCampaign(primaryUnitType, true);
    }

    private Campaign deploymentCampaign(int primaryUnitType, boolean isContractsUseSpecialMechanics) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        // Odds no roll can meet, so no scenario can break out.
        track.setScenarioOdds(-100);
        when(options.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)).thenReturn(isContractsUseSpecialMechanics);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        AbstractContract contract = mock(AbstractContract.class);
        campaignState = new StratConCampaignState();
        campaignState.setContractsUseSpecialMechanics(isContractsUseSpecialMechanics);
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.GARRISON_DUTY);
        when(contract.getScale()).thenReturn(1);
        campaignState.setEscalation(STARTING_ESCALATION);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getContractFinanceData()).thenReturn(new ContractFinanceData(Money.zero(),
              Money.zero(),
              Money.of(25000)));
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private static Campaign dailyCampaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, SHOW_COORDS, FORMATION_ID, campaign);
    }

    /** @return the single general report the campaign received, checked to come from a real resource key */
    private static String generalReport(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        String report = reportCaptor.getValue();
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
        return report;
    }

    private void assertSpent() {
        assertNull(track.getPointOfInterest(show.getId()), "a spent show of force leaves the map");
        assertFalse(show.hasLinkedScenario());
    }

    // Registration and scenario

    @Test
    void theShowOfForceBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConShowOfForceBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConShowOfForceBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConShowOfForceBehavior.class, show.getBehavior());
    }

    @Test
    void theAmbushDrawsATemplateSuitedToTheAmbushedUnit() {
        assertNull(new StratConShowOfForceBehavior().getScenarioTemplateName());
    }

    // Making it

    @ParameterizedTest
    @ValueSource(ints = { MEK, AEROSPACE_FIGHTER })
    void anyFormationMakesTheShowOfForceWhenThereIsNoAmbush(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));

        assertNull(track.getPointOfInterest(show.getId()), "a show of force made leaves the map");
        assertEquals(PointOfInterestStatus.RESOLVED, show.getStatus());
        generalReport(campaign);
    }

    @Test
    void makingAShowOfForcePaysNoCombatBonus() {
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        deploy(campaign);

        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    // Escalation

    @Test
    void makingAShowOfForceLowersEscalationByThreeDice() {
        Campaign campaign = deploymentCampaign(MEK);

        try (MockedStatic<Compute> dice = StratConTestDice.loadDice()) {
            deploy(campaign);
        }

        assertEquals(STARTING_ESCALATION - (3 * StratConTestDice.PIPS_PER_DIE), campaignState.getEscalation(),
              "-3d6 Escalation");
    }

    @Test
    void anAmbushedShowOfForceCalmsNothingOfItsOwn() {
        // Winning the ambush lowers Escalation as any won scenario does, when the scenario resolves; the show of force
        // itself calms nothing.
        show.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertEquals(STARTING_ESCALATION, campaignState.getEscalation());
    }

    @Test
    void withoutSpecialMechanicsAShowOfForceCalmsNothing() {
        Campaign campaign = deploymentCampaign(MEK, false);

        deploy(campaign);

        assertEquals(STARTING_ESCALATION, campaignState.getEscalation());
    }

    // An ambushed show of force is spent whatever the result

    @Test
    void winningTheAmbushStillSpendsTheShowOfForce() {
        show.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertSpent();
        generalReport(campaign);
    }

    @Test
    void losingTheAmbushSpendsTheShowOfForce() {
        show.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, deploymentCampaign(MEK));

        assertSpent();
    }

    @Test
    void anAmbushLeftUnplayedSpendsTheShowOfForce() {
        show.setLinkedScenarioId(SCENARIO_ID);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSpent();
    }

    // On the map

    @Test
    void aShowOfForceIsVisibleWithoutBeingScouted() {
        assertFalse(show.isRevealed());
        assertTrue(show.isVisibleToPlayer(track));
    }

    @Test
    void aShowOfForceNotMadeInTimeIsGone() {
        show.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertNull(track.getPointOfInterest(show.getId()));
        assertEquals(PointOfInterestStatus.EXPIRED, show.getStatus());
    }

    @Test
    void aShowOfForceWaitingOnItsAmbushDoesNotExpire() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(SHOW_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        show.setLinkedScenarioId(SCENARIO_ID);
        show.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(show, track.getPointOfInterest(show.getId()));
        assertTrue(show.isActive());
    }

    @Test
    void aShowOfForceLastsOneToSixDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);

        for (int attempt = 0; attempt < 100; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, SHOW_COORDS, TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 6), "rolled a lifespan of " + lifespanDays);
        }
    }
}
