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
import static megamek.common.units.UnitType.TANK;
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
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConStrategicObjective;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest.PointOfInterestStatus;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractFinanceData;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the pirate captain: a captain with no scenario having slipped away, the fight with one being an Essential
 * scenario (whose own objective and combat bonus stand in for the captain's), the captain itself paying nothing and
 * being no objective, which formations can run one down, its visibility, and its rolled lifespan.
 *
 * <p>Placing the Decapitation Strike builds a full scenario from a template, so it is not covered here; making that
 * scenario Essential is, through {@link StratConPirateCaptainBehavior#makeEssential}, and its outcome is, through
 * {@link StratConPointOfInterestRules#processScenarioEnded} and the daily step.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPirateCaptainBehaviorTest {
    private static final String TYPE_ID = "UnitTestPirateCaptain";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords CAPTAIN_COORDS = new StratConCoords(1, 1);

    private StratConTrackState track;
    private StratConPointOfInterest captain;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConPirateCaptainBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        captain = new StratConPointOfInterest(TYPE_ID, CAPTAIN_COORDS);
        assertTrue(track.addPointOfInterest(captain), "test setup: the captain should be placed");
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign holding one active contract - with combat pay - whose map holds the test sector, and one player
     * formation of the given primary unit type. With Essential Scenarios Only on, no scenario can break out, so every
     * captain has slipped away.
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

    private static Campaign dailyCampaign() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        return campaign;
    }

    private PointOfInterestDeploymentOutcome deploy(Campaign campaign) {
        return StratConPointOfInterestRules.processFormationDeployment(track, CAPTAIN_COORDS, FORMATION_ID, campaign);
    }

    /** @return the single general report the campaign received, checked to come from a real resource key */
    private static String generalReport(Campaign campaign) {
        ArgumentCaptor<String> reportCaptor = ArgumentCaptor.forClass(String.class);
        verify(campaign).addReport(eq(GENERAL), reportCaptor.capture());
        String report = reportCaptor.getValue();
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
        return report;
    }

    // Registration and scenario

    @Test
    void thePirateCaptainBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConPirateCaptainBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConPirateCaptainBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConPirateCaptainBehavior.class, captain.getBehavior());
    }

    @Test
    void theCaptainIsFoughtInADecapitationStrike() {
        assertEquals("Decapitation Strike.json", new StratConPirateCaptainBehavior().getScenarioTemplateName());
    }

    @Test
    void theCaptainHasItsOwnText() {
        String description = captain.getBehavior().getObjectiveDescription(captain, track);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    // Running one down

    @ParameterizedTest
    @ValueSource(ints = { MEK, TANK })
    void aCaptainWithNoScenarioHasSlippedAway(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);
        Finances finances = campaign.getPlayerForce().getFinances();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO, deploy(campaign));

        assertNull(track.getPointOfInterest(captain.getId()), "a captain who slipped away leaves the map");
        assertTrue(track.getStrategicObjectives().isEmpty(), "nothing is failed");
        generalReport(campaign);
        verify(finances, never()).credit(any(), any(), any(), anyString());
    }

    @Test
    void aFormationThatIsNotOnTheGroundCannotRunACaptainDown() {
        Campaign campaign = deploymentCampaign(AEROSPACE_FIGHTER);

        assertEquals(PointOfInterestDeploymentOutcome.NO_EFFECT, deploy(campaign));
        assertSame(captain, track.getPointOfInterest(captain.getId()));
        assertTrue(captain.isActive());
    }

    // The fight with the captain

    @Test
    void theFightWithACaptainIsAnEssentialScenarioWithItsOwnObjective() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(CAPTAIN_COORDS);
        scenario.setTurningPoint(true);

        StratConPirateCaptainBehavior.makeEssential(scenario, track);

        assertTrue(scenario.isStrategicObjective(), "an Essential scenario, so a win pays the combat bonus");
        assertFalse(scenario.isTurningPoint());
        StratConStrategicObjective objective = track.getObjectivesByCoords().get(CAPTAIN_COORDS);
        assertNotNull(objective);
        assertEquals(StrategicObjectiveType.SpecificScenarioVictory, objective.getObjectiveType());
        assertEquals(1, objective.getDesiredObjectiveCount());
        assertFalse(objective.isObjectiveCompleted(track));
    }

    @Test
    void bringingACaptainDownPaysNothingOfItsOwn() {
        // The Essential scenario pays the combat bonus when it resolves; the captain itself adds nothing.
        captain.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

        assertNull(track.getPointOfInterest(captain.getId()));
        assertEquals(PointOfInterestStatus.RESOLVED, captain.getStatus());
        verify(finances, never()).credit(any(), any(), any(), anyString());
        generalReport(campaign);
    }

    @Test
    void losingTheFightLetsTheCaptainEscape() {
        captain.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);

        StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, false, campaign);

        assertNull(track.getPointOfInterest(captain.getId()), "an escaped captain leaves the map");
        generalReport(campaign);
    }

    // On the map

    @Test
    void aCaptainIsVisibleWithoutBeingScouted() {
        assertFalse(captain.isRevealed());
        assertTrue(captain.isVisibleToPlayer(track));
    }

    @Test
    void aCaptainNotRunDownInTimeMovesOn() {
        captain.setExpiryDate(TODAY);

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertNull(track.getPointOfInterest(captain.getId()));
        assertEquals(PointOfInterestStatus.EXPIRED, captain.getStatus());
    }

    @Test
    void aCaptainWaitingOnItsFightDoesNotMoveOn() {
        StratConScenario scenario = new StratConScenario();
        scenario.setCoords(CAPTAIN_COORDS);
        scenario.setBackingScenarioID(SCENARIO_ID);
        track.addScenario(scenario);
        captain.setLinkedScenarioId(SCENARIO_ID);
        captain.setExpiryDate(TODAY.minusDays(2));

        StratConPointOfInterestRules.processNewDay(track, dailyCampaign());

        assertSame(captain, track.getPointOfInterest(captain.getId()));
        assertTrue(captain.isActive());
    }

    @Test
    void aCaptainLastsOneToSixDays() {
        StratConPointOfInterestDefinition definition = StratConPointOfInterestDefinitions.getDefinition(TYPE_ID);

        for (int attempt = 0; attempt < 100; attempt++) {
            StratConPointOfInterest placed = StratConPointOfInterest.fromDefinition(definition, CAPTAIN_COORDS, TODAY);
            assertNotNull(placed.getExpiryDate());
            int lifespanDays = (int) (placed.getExpiryDate().toEpochDay() - TODAY.toEpochDay());
            assertTrue((lifespanDays >= 1) && (lifespanDays <= 6), "rolled a lifespan of " + lifespanDays);
        }
    }
}
