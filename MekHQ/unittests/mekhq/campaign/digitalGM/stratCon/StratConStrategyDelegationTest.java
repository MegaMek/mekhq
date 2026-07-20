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
package mekhq.campaign.digitalGM.stratCon;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType.AUXILIARY;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.IForceDeploymentStrategy;
import mekhq.campaign.digitalGM.IReinforcementStrategy;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.AtBScenario;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Verifies that the newly extracted StratCon strategies ({@link StratConForceDeploymentStrategy},
 * {@link StratConReinforcementStrategy}, and the facility ops on {@link StratConFacilityStrategy}) delegate to the
 * matching static entry points on {@link StratConRulesManager} with the same arguments &mdash; i.e. the extraction is a
 * pure seam and moved no behaviour. Also confirms the no-op facility strategy touches nothing and that
 * {@link AbstractStratConGM} exposes the StratCon implementations by default.
 *
 * @author Illiani
 */
class StratConStrategyDelegationTest {

    @Test
    void forceDeploymentStrategyDelegatesToRulesManager() {
        StratConCoords coords = mock(StratConCoords.class);
        Campaign campaign = mock(Campaign.class);
        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConScenario scenario = mock(StratConScenario.class);
        IForceDeploymentStrategy strategy = new StratConForceDeploymentStrategy();

        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            strategy.deployForceToCoords(coords, 5, campaign, contract, track, true);
            strategy.assignForceToScenario(coords, 5, campaign, contract, track, false);
            strategy.processForceDeployment(coords, 5, campaign, track, true);
            strategy.commitPrimaryForces(campaign, scenario, track);

            rules.verify(() -> StratConRulesManager.deployForceToCoords(coords, 5, campaign, contract, track, true));
            rules.verify(() -> StratConRulesManager.assignForceToScenario(coords, 5, campaign, contract, track, false));
            rules.verify(() -> StratConRulesManager.processForceDeployment(coords, 5, campaign, track, true));
            rules.verify(() -> StratConRulesManager.commitPrimaryForces(campaign, scenario, track));
        }
    }

    @Test
    void reinforcementStrategyDelegatesAndReturnsResults() {
        Campaign campaign = mock(Campaign.class);
        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        StratConScenario scenario = mock(StratConScenario.class);
        Formation formation = mock(Formation.class);
        Person liaison = mock(Person.class);
        TargetRoll targetRoll = mock(TargetRoll.class);
        IReinforcementStrategy strategy = new StratConReinforcementStrategy();

        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            rules.when(() -> StratConRulesManager.getReinforcementType(5, track, campaign, campaignState))
                  .thenReturn(AUXILIARY);
            rules.when(() -> StratConRulesManager.calculateReinforcementTargetNumber(liaison, contract, 4))
                  .thenReturn(targetRoll);
            rules.when(() -> StratConRulesManager.processReinforcementDeployment(formation, AUXILIARY, campaignState,
                  scenario, campaign, 8, false, false)).thenReturn(SUCCESS);
            rules.when(() -> StratConRulesManager.processReinforcementDeployment(formation, AUXILIARY, campaignState,
                  scenario, campaign, 8, false, true)).thenReturn(SUCCESS);

            assertSame(AUXILIARY, strategy.getReinforcementType(5, track, campaign, campaignState));
            assertSame(targetRoll, strategy.calculateReinforcementTargetNumber(liaison, contract, 4));
            assertSame(SUCCESS,
                  strategy.processReinforcementDeployment(formation, AUXILIARY, campaignState, scenario, campaign, 8,
                        false));
            assertSame(SUCCESS,
                  strategy.processReinforcementDeployment(formation, AUXILIARY, campaignState, scenario, campaign, 8,
                        false, true));
        }
    }

    @Test
    void facilityStrategyDelegatesAllOps() {
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        AtBScenario scenario = mock(AtBScenario.class);
        AtBContract contract = mock(AtBContract.class);
        StratConFacility facility = mock(StratConFacility.class);
        StratConFacilityStrategy strategy = new StratConFacilityStrategy();

        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            strategy.applyPeriodicEffects(track, campaignState, true);
            strategy.updateFacilityForScenario(scenario, contract, true, false);
            strategy.switchFacilityOwner(facility);

            rules.verify(() -> StratConRulesManager.processFacilityEffects(track, campaignState, true));
            rules.verify(() -> StratConRulesManager.updateFacilityForScenario(scenario, contract, true, false));
            rules.verify(() -> StratConRulesManager.switchFacilityOwner(facility));
        }
    }

    @Test
    void noOpFacilityStrategyTouchesNothing() {
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCampaignState campaignState = mock(StratConCampaignState.class);
        AtBScenario scenario = mock(AtBScenario.class);
        AtBContract contract = mock(AtBContract.class);
        StratConFacility facility = mock(StratConFacility.class);
        NoOpFacilityStrategy strategy = new NoOpFacilityStrategy();

        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            strategy.applyPeriodicEffects(track, campaignState, true);
            strategy.updateFacilityForScenario(scenario, contract, true, false);
            strategy.switchFacilityOwner(facility);

            rules.verifyNoInteractions();
        }
    }

    @Test
    void opForGenerationStrategyDelegatesToDynamicScenarioFactory() {
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        AtBContract contract = mock(AtBContract.class);
        Campaign campaign = mock(Campaign.class);
        StratConOpForGenerationStrategy strategy = new StratConOpForGenerationStrategy();

        try (MockedStatic<AtBDynamicScenarioFactory> factory = mockStatic(AtBDynamicScenarioFactory.class)) {
            strategy.generateOpFor(backingScenario, contract, campaign);

            factory.verify(() -> AtBDynamicScenarioFactory.finalizeScenario(backingScenario, contract, campaign));
        }
    }

    @Test
    void opForDeploymentStrategyDelegatesToContractInitializer() {
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = mock(StratConCoords.class);
        StratConOpForDeploymentStrategy strategy = new StratConOpForDeploymentStrategy();

        try (MockedStatic<StratConContractInitializer> initializer =
                   mockStatic(StratConContractInitializer.class)) {
            initializer.when(() -> StratConContractInitializer.getUnoccupiedCoords(track, true, false, true))
                  .thenReturn(coords);
            initializer.when(() -> StratConContractInitializer.getUnoccupiedCoords(track, false, false, false))
                  .thenReturn(coords);

            // explicit constraints pass through
            assertSame(coords, strategy.getUnoccupiedCoords(track, true, false, true));
            initializer.verify(() -> StratConContractInitializer.getUnoccupiedCoords(track, true, false, true));

            // convenience overload defaults to (false, false, false)
            assertSame(coords, strategy.getUnoccupiedCoords(track));
            initializer.verify(() -> StratConContractInitializer.getUnoccupiedCoords(track, false, false, false));
        }
    }

    @Test
    void mapGenerationStrategyDelegatesToRulesManager() {
        StratConTrackState track = mock(StratConTrackState.class);
        StratConScenario scenario = mock(StratConScenario.class);
        StratConMapGenerationStrategy strategy = new StratConMapGenerationStrategy();

        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            strategy.setScenarioTerrain(track, scenario, true);

            rules.verify(() -> StratConRulesManager.setScenarioParametersFromBiome(track, scenario, true));
        }
    }

    @Test
    void abstractStratConGmExposesStratConStrategiesByDefault() {
        StratConDigitalGM gm = new StratConDigitalGM();

        assertInstanceOf(StratConForceDeploymentStrategy.class, gm.getForceDeploymentStrategy());
        assertInstanceOf(StratConReinforcementStrategy.class, gm.getReinforcementStrategy());
        assertInstanceOf(StratConOpForGenerationStrategy.class, gm.getOpForGenerationStrategy());
        assertInstanceOf(StratConOpForDeploymentStrategy.class, gm.getOpForDeploymentStrategy());
        assertInstanceOf(StratConMapGenerationStrategy.class, gm.getMapGenerationStrategy());
    }
}
