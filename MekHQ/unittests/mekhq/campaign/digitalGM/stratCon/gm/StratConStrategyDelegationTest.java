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
package mekhq.campaign.digitalGM.stratCon.gm;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType.AUXILIARY;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import megamek.common.rolls.TargetRoll;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.DigitalGMRegistry;
import mekhq.campaign.digitalGM.IForceDeploymentStrategy;
import mekhq.campaign.digitalGM.IReinforcementStrategy;
import mekhq.campaign.digitalGM.ISectorGenerationStrategy;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConContractInitializer;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.ImprovedStratConSectorGeneration;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.LatitudeBand;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.LegacyStratConSectorGeneration;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.PlanetProfile;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConRoadPlacer;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConSectorGenerator;
import mekhq.campaign.digitalGM.stratCon.sectorGeneration.StratConTerrainPlacer;
import mekhq.campaign.digitalGM.stratCon.strategy.NoOpFacilityStrategy;
import mekhq.campaign.digitalGM.stratCon.strategy.StratConFacilityStrategy;
import mekhq.campaign.digitalGM.stratCon.strategy.StratConForceDeploymentStrategy;
import mekhq.campaign.digitalGM.stratCon.strategy.StratConMapGenerationStrategy;
import mekhq.campaign.digitalGM.stratCon.strategy.StratConOpForDeploymentStrategy;
import mekhq.campaign.digitalGM.stratCon.strategy.StratConOpForGenerationStrategy;
import mekhq.campaign.digitalGM.stratCon.strategy.StratConReinforcementStrategy;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.AtBDynamicScenarioFactory;
import mekhq.campaign.mission.scenarios.AtBScenario;
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
        AbstractContract contract = mock(AbstractContract.class);
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
        AbstractContract contract = mock(AbstractContract.class);
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
        AbstractContract contract = mock(AbstractContract.class);
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
        AbstractContract contract = mock(AbstractContract.class);
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
        AbstractContract contract = mock(AbstractContract.class);
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

    // region sector generation

    /** @return campaign options with only the alternate-sector-terrain switch set. */
    private static CampaignOptions optionsWithAlternateTerrain(boolean useAlternateTerrain) {
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.USE_STRAT_CON_ALTERNATE_SECTOR_TERRAIN)).thenReturn(useAlternateTerrain);
        return options;
    }

    private static StratConTrackState landTrack(int width, int height, String terrainType) {
        StratConTrackState track = new StratConTrackState();
        track.setWidth(width);
        track.setHeight(height);
        track.setTemperature(25);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                track.setTerrainTile(new StratConCoords(x, y), terrainType);
            }
        }
        return track;
    }

    @Test
    void sectorGenerationStrategyFollowsTheAlternateTerrainOption() {
        // With no GM registered the facade falls back to the default StratCon GM, which is where the improved/legacy
        // branch lives; the campaign option keeps its meaning while the branch is a proper strategy seam.
        try (MockedStatic<DigitalGMRegistry> registry = mockStatic(DigitalGMRegistry.class)) {
            registry.when(() -> DigitalGMRegistry.getActiveGM(org.mockito.ArgumentMatchers.any()))
                  .thenReturn(Optional.empty());

            assertInstanceOf(ImprovedStratConSectorGeneration.class,
                  StratConGMs.sectorGeneration(optionsWithAlternateTerrain(true)),
                  "the alternate-terrain option should select the improved sector generation");
            assertInstanceOf(LegacyStratConSectorGeneration.class,
                  StratConGMs.sectorGeneration(optionsWithAlternateTerrain(false)),
                  "the alternate-terrain option being off should select the legacy sector generation");
        }
    }

    @Test
    void sectorGenerationStrategyIsStableAcrossLookups() {
        try (MockedStatic<DigitalGMRegistry> registry = mockStatic(DigitalGMRegistry.class)) {
            registry.when(() -> DigitalGMRegistry.getActiveGM(org.mockito.ArgumentMatchers.any()))
                  .thenReturn(Optional.empty());
            CampaignOptions options = optionsWithAlternateTerrain(true);

            assertSame(StratConGMs.sectorGeneration(options),
                  StratConGMs.sectorGeneration(options),
                  "repeated lookups should hand back the same strategy instance, not a fresh one each call");
        }
    }

    @Test
    void legacySectorGenerationDelegatesToTheStripePlacer() {
        StratConTrackState track = mock(StratConTrackState.class);
        ISectorGenerationStrategy strategy = new LegacyStratConSectorGeneration();

        try (MockedStatic<StratConTerrainPlacer> placer = mockStatic(StratConTerrainPlacer.class)) {
            strategy.initializeTrack(track, PlanetProfile.neutral(25), LatitudeBand.EQUATORIAL, true);

            placer.verify(() -> StratConTerrainPlacer.InitializeTrackTerrain(track));
        }
    }

    @Test
    void legacySectorGenerationClearsTheTrackBeforeRegenerating() {
        StratConTrackState track = mock(StratConTrackState.class);
        ISectorGenerationStrategy strategy = new LegacyStratConSectorGeneration();

        try (MockedStatic<StratConTerrainPlacer> placer = mockStatic(StratConTerrainPlacer.class)) {
            strategy.regenerateTrack(track, PlanetProfile.neutral(25), LatitudeBand.EQUATORIAL, true);

            verify(track).clearForRegeneration();
            placer.verify(() -> StratConTerrainPlacer.InitializeTrackTerrain(track));
        }
    }

    @Test
    void legacySectorGenerationConnectFacilitiesToRoadsIsANoOp() {
        // The legacy placer builds no road network at all, so facility "road connection" must place nothing rather
        // than half-building a network the rest of the legacy sector does not have.
        StratConTrackState track = landTrack(10, 10, "Plains");
        track.addCity(new StratConCoords(1, 1));
        track.addCity(new StratConCoords(8, 8));
        ISectorGenerationStrategy strategy = new LegacyStratConSectorGeneration();

        strategy.connectFacilitiesToRoads(track, List.of(new StratConCoords(4, 4), new StratConCoords(5, 5)));

        assertTrue(track.getRoads().isEmpty(), "the legacy generator must never place roads");
        assertTrue(track.getRoadExits().isEmpty(), "the legacy generator must never place road exits");
    }

    @Test
    void legacySectorGenerationTouchesNoRoadPlacerAtAll() {
        StratConTrackState track = mock(StratConTrackState.class);
        ISectorGenerationStrategy strategy = new LegacyStratConSectorGeneration();

        try (MockedStatic<StratConRoadPlacer> roads = mockStatic(StratConRoadPlacer.class)) {
            strategy.connectFacilitiesToRoads(track, List.of(new StratConCoords(2, 2)));

            roads.verifyNoInteractions();
        }
    }

    @Test
    void improvedSectorGenerationDelegatesToTheGeneratorPipeline() {
        StratConTrackState track = mock(StratConTrackState.class);
        PlanetProfile profile = PlanetProfile.neutral(25);
        ISectorGenerationStrategy strategy = new ImprovedStratConSectorGeneration();

        try (MockedStatic<StratConSectorGenerator> generator = mockStatic(StratConSectorGenerator.class)) {
            strategy.initializeTrack(track, profile, LatitudeBand.NORTH_POLAR, false);

            generator.verify(() -> StratConSectorGenerator.generate(track, profile, LatitudeBand.NORTH_POLAR, false));
        }
    }

    @Test
    void improvedSectorGenerationClearsTheTrackBeforeRegenerating() {
        StratConTrackState track = mock(StratConTrackState.class);
        PlanetProfile profile = PlanetProfile.neutral(25);
        ISectorGenerationStrategy strategy = new ImprovedStratConSectorGeneration();

        try (MockedStatic<StratConSectorGenerator> generator = mockStatic(StratConSectorGenerator.class)) {
            strategy.regenerateTrack(track, profile, LatitudeBand.EQUATORIAL, true);

            verify(track).clearForRegeneration();
            generator.verify(() -> StratConSectorGenerator.generate(track, profile, LatitudeBand.EQUATORIAL, true));
        }
    }

    @Test
    void improvedSectorGenerationRegenerateReplacesTheWholeTrackTerrain() {
        // End-to-end on a real track: every hex of the stale terrain is gone and every hex is filled again, so the GM
        // "Regenerate Sector" tool can never leave a half-old, half-new sector behind.
        StratConTrackState track = landTrack(16, 16, "Volcano");
        track.addCity(new StratConCoords(3, 3));
        new ImprovedStratConSectorGeneration().regenerateTrack(track,
              PlanetProfile.neutral(25),
              LatitudeBand.EQUATORIAL,
              true);

        int volcanicHexes = 0;
        for (int x = 0; x < track.getWidth(); x++) {
            for (int y = 0; y < track.getHeight(); y++) {
                StratConCoords coords = new StratConCoords(x, y);
                assertFalse(track.getTerrainTile(coords).isEmpty(), "hex " + x + "," + y + " was left empty");
                if ("Volcano".equals(track.getTerrainTile(coords))) {
                    volcanicHexes++;
                }
            }
        }

        assertNotEquals(track.getWidth() * track.getHeight(),
              volcanicHexes,
              "the pre-existing terrain survived regeneration untouched");
    }

    @Test
    void improvedSectorGenerationConnectsFacilitiesThroughTheRoadPlacer() {
        StratConTrackState track = mock(StratConTrackState.class);
        List<StratConCoords> facilities = List.of(new StratConCoords(2, 2), new StratConCoords(6, 6));
        ISectorGenerationStrategy strategy = new ImprovedStratConSectorGeneration();

        try (MockedStatic<StratConRoadPlacer> roads = mockStatic(StratConRoadPlacer.class)) {
            strategy.connectFacilitiesToRoads(track, facilities);

            roads.verify(() -> StratConRoadPlacer.recalculateRoads(track, facilities));
        }
    }

    // endregion sector generation
}
