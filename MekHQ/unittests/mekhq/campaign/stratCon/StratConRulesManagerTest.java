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
package mekhq.campaign.stratCon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import mekhq.campaign.CurrentLocation;
import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.ScenarioForceTemplate;
import mekhq.campaign.mission.ScenarioMapParameters.MapLocation;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.Atmosphere;
import mekhq.campaign.universe.Planet;

import megamek.common.options.OptionsConstants;
import megamek.common.units.Entity;
import megamek.common.units.UnitType;

/**
 * Tests for {@link StratConRulesManager}
 */
class StratConRulesManagerTest {

    /**
     * Creates the common mock infrastructure needed for deployForceToCoords tests.
     * processForceDeployment -> scanNeighboringCoords touches many objects.
     */
    private void setupProcessForceDeploymentMocks(Campaign campaign, CampaignOptions options,
          StratConTrackState track, int forceID) {
        // scanNeighboringCoords needs revealed coords set
        when(track.getRevealedCoords()).thenReturn(new HashSet<>());
        when(track.getScanRangeIncrease()).thenReturn(0);

        // increaseFatigue needs Formation -> Units -> Crew
        Formation formation = mock(Formation.class);
        when(campaign.getFormation(forceID)).thenReturn(formation);

        UUID unitId = UUID.randomUUID();
        Vector<UUID> unitIds = new Vector<>();
        unitIds.add(unitId);
        when(formation.getAllUnits(false)).thenReturn(unitIds);

        Unit unit = mock(Unit.class);
        when(campaign.getUnit(unitId)).thenReturn(unit);
        when(unit.getCrew()).thenReturn(List.of(mock(Person.class)));

        // CampaignOptions needed by scanNeighboringCoords
        when(options.isUseFatigue()).thenReturn(false);
        when(options.getFatigueRate()).thenReturn(0);

        // processForceDeployment needs LocalDate and Hangar
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 15));
        when(campaign.getHangar()).thenReturn(mock(Hangar.class));

        // Track setup for processForceDeployment
        when(track.getAssignedCoordForces()).thenReturn(new HashMap<>());
    }

    /**
     * Verifies that coordinate deployment does not auto-assign Official Challenge scenarios, while explicit player
     * assignment still commits the selected force.
     *
     * <p>Regression coverage for
     * <a href="https://github.com/MegaMek/mekhq/issues/8612">issue #8612</a> and
     * <a href="https://github.com/MegaMek/mekhq/issues/8867">issue #8867</a>.
     */
    @Test
    void officialChallenge_deployToCoordsDoesNotAutoAssign_assignForceToScenarioCommits() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);

        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = new StratConCoords(2, 3);
        int forceID = 1;

        StratConScenario challengeScenario = mock(StratConScenario.class);
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getStratConScenarioType()).thenReturn(ScenarioType.OFFICIAL_CHALLENGE);
        when(backingScenario.isFinalized()).thenReturn(true);
        when(backingScenario.isCloaked()).thenReturn(false);
        when(backingScenario.getForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(challengeScenario.getBackingScenario()).thenReturn(backingScenario);
        when(challengeScenario.getPrimaryForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(challengeScenario.getPlayerTemplateForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(track.getScenario(coords)).thenReturn(challengeScenario);

        CombatTeam combatTeam = mock(CombatTeam.class);
        CombatRole combatRole = mock(CombatRole.class);
        when(combatRole.isPatrol()).thenReturn(false);
        when(combatRole.isTraining()).thenReturn(false);
        when(combatTeam.getRole()).thenReturn(combatRole);
        var combatTeamsMap = new Hashtable<Integer, CombatTeam>();
        combatTeamsMap.put(forceID, combatTeam);
        when(campaign.getCombatTeamsAsMap()).thenReturn(combatTeamsMap);

        setupProcessForceDeploymentMocks(campaign, options, track, forceID);

        StratConRulesManager.deployForceToCoords(coords, forceID, campaign, contract, track, false);

        verify(challengeScenario, never()).addPrimaryForce(anyInt());

        StratConRulesManager.assignForceToScenario(coords, forceID, campaign, contract, track, false);

        verify(challengeScenario).addPrimaryForce(forceID);
        verify(challengeScenario).commitPrimaryForces();
    }

    /**
     * Verifies that when a force deploys to coordinates containing a non-challenge scenario
     * (e.g., a fixed objective), the force IS auto-assigned as before.
     */
    @Test
    void deployForceToCoords_nonChallengeScenario_autoAssignsForce() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);

        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = new StratConCoords(2, 3);
        int forceID = 1;

        // Create a regular scenario at the target coords
        StratConScenario regularScenario = mock(StratConScenario.class);
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getStratConScenarioType()).thenReturn(ScenarioType.NONE);
        when(backingScenario.isFinalized()).thenReturn(true);
        when(backingScenario.isCloaked()).thenReturn(false);
        when(regularScenario.getBackingScenario()).thenReturn(backingScenario);
        when(regularScenario.getPrimaryForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(regularScenario.getPlayerTemplateForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(track.getScenario(coords)).thenReturn(regularScenario);

        // Setup combat team
        CombatTeam combatTeam = mock(CombatTeam.class);
        CombatRole combatRole = mock(CombatRole.class);
        when(combatRole.isPatrol()).thenReturn(false);
        when(combatRole.isTraining()).thenReturn(false);
        when(combatTeam.getRole()).thenReturn(combatRole);
        var combatTeamsMap = new Hashtable<Integer, CombatTeam>();
        combatTeamsMap.put(forceID, combatTeam);
        when(campaign.getCombatTeamsAsMap()).thenReturn(combatTeamsMap);

        setupProcessForceDeploymentMocks(campaign, options, track, forceID);

        // Act
        StratConRulesManager.deployForceToCoords(coords, forceID, campaign, contract, track, false);

        // Assert: force SHOULD be added to the regular scenario
        verify(regularScenario).addPrimaryForce(forceID);
    }

    /**
     * Verifies that explicit player assignment to a non-challenge scenario commits the selected force.
     */
    @Test
    void assignForceToScenario_nonChallengeScenario_commitsSelectedForce() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);

        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = new StratConCoords(2, 3);
        int forceID = 1;

        StratConScenario regularScenario = mock(StratConScenario.class);
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getStratConScenarioType()).thenReturn(ScenarioType.NONE);
        when(backingScenario.isFinalized()).thenReturn(true);
        when(backingScenario.isCloaked()).thenReturn(false);
        when(backingScenario.getForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(regularScenario.getBackingScenario()).thenReturn(backingScenario);
        when(regularScenario.getPrimaryForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(regularScenario.getPlayerTemplateForceIDs()).thenReturn(new java.util.ArrayList<>());
        when(track.getScenario(coords)).thenReturn(regularScenario);

        CombatTeam combatTeam = mock(CombatTeam.class);
        CombatRole combatRole = mock(CombatRole.class);
        when(combatRole.isPatrol()).thenReturn(false);
        when(combatTeam.getRole()).thenReturn(combatRole);
        var combatTeamsMap = new Hashtable<Integer, CombatTeam>();
        combatTeamsMap.put(forceID, combatTeam);
        when(campaign.getCombatTeamsAsMap()).thenReturn(combatTeamsMap);

        setupProcessForceDeploymentMocks(campaign, options, track, forceID);

        StratConRulesManager.assignForceToScenario(coords, forceID, campaign, contract, track, false);

        verify(regularScenario).addPrimaryForce(forceID);
        verify(regularScenario).commitPrimaryForces();
    }

    /**
     * Verifies that when an Official Challenge scenario spawns on a hex that already has a deployed
     * force (the {@code generateScenarioForExistingForces} path), the scenario does NOT override
     * force auto-assignment. This means {@code finalizeBackingScenario} (called with
     * {@code autoAssignLances=false} in {@code generateDailyScenariosForTrack}) will remove the
     * forces from the backing scenario and set the scenario to UNRESOLVED, preventing
     * auto-assignment.
     *
     * <p>Regression test for
     * <a href="https://github.com/MegaMek/mekhq/issues/8612">issue #8612</a>
     * — spawn-on-existing-force path.
     */
    @Test
    void generateScenarioForExistingForces_officialChallenge_doesNotOverrideAutoAssignment() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.isUseStratConMaplessMode()).thenReturn(false);

        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = new StratConCoords(2, 3);

        // Create a mock scenario whose backing scenario is an Official Challenge
        StratConScenario mockScenario = mock(StratConScenario.class);
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getStratConScenarioType()).thenReturn(ScenarioType.OFFICIAL_CHALLENGE);
        when(mockScenario.getBackingScenario()).thenReturn(backingScenario);

        Set<Integer> forceIDs = new LinkedHashSet<>(List.of(42));

        try (MockedStatic<StratConRulesManager> mockedManager =
                   Mockito.mockStatic(StratConRulesManager.class, CALLS_REAL_METHODS)) {
            // Mock setupScenario to return our controlled Official Challenge scenario
            mockedManager.when(() -> StratConRulesManager.setupScenario(
                  any(), any(), any(), any(), any(), any(), anyBoolean(), any()
            )).thenReturn(mockScenario);

            // Act
            StratConScenario result = StratConRulesManager.generateScenarioForExistingForces(
                  coords, forceIDs, contract, campaign, track, null, null);

            // Assert
            assertNotNull(result);
            // overrideForceAutoAssignment must be false for Official Challenge,
            // so finalizeBackingScenario will remove formations instead of committing them
            verify(mockScenario).setOverrideForceAutoAssignment(false);
        }
    }

    /**
     * Verifies that when a non-challenge scenario spawns on a hex with an existing force,
     * the scenario DOES override force auto-assignment (so forces are committed as usual).
     */
    @Test
    void generateScenarioForExistingForces_nonChallenge_overridesAutoAssignment() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(options.isUseStratConMaplessMode()).thenReturn(false);

        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = new StratConCoords(2, 3);

        // Create a mock scenario whose backing scenario is NOT an Official Challenge
        StratConScenario mockScenario = mock(StratConScenario.class);
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getStratConScenarioType()).thenReturn(ScenarioType.NONE);
        when(mockScenario.getBackingScenario()).thenReturn(backingScenario);

        Set<Integer> forceIDs = new LinkedHashSet<>(List.of(42));

        try (MockedStatic<StratConRulesManager> mockedManager =
                   Mockito.mockStatic(StratConRulesManager.class, CALLS_REAL_METHODS)) {
            mockedManager.when(() -> StratConRulesManager.setupScenario(
                  any(), any(), any(), any(), any(), any(), anyBoolean(), any()
            )).thenReturn(mockScenario);

            // Act
            StratConScenario result = StratConRulesManager.generateScenarioForExistingForces(
                  coords, forceIDs, contract, campaign, track, null, null);

            // Assert
            assertNotNull(result);
            // overrideForceAutoAssignment must be true for non-challenge scenarios,
            // so finalizeBackingScenario will commit forces as normal
            verify(mockScenario).setOverrideForceAutoAssignment(true);
        }
    }

    // -- isValidUnitForScenario tests --

    /**
     * Creates common mock infrastructure for isValidUnitForScenario tests.
     *
     * @param unitType the unit type to return from the entity
     * @param hasUnstreamlinedQuirk whether the entity has the unstreamlined quirk
     * @param atmosphere the planet's atmosphere (null to simulate missing planet data)
     * @param isUseDropShips whether campaign options allow player dropships
     * @param allowedUnitType the allowed unit type on the scenario force template (-2 for ATB_MIX)
     *
     * @return an Object array: [Unit, ScenarioForceTemplate, Campaign]
     */
    private Object[] setupIsValidUnitMocks(int unitType, boolean hasUnstreamlinedQuirk,
          Atmosphere atmosphere, boolean isUseDropShips, int allowedUnitType) {
        Entity entity = mock(Entity.class);
        when(entity.getUnitType()).thenReturn(unitType);
        when(entity.hasQuirk(OptionsConstants.QUIRK_NEG_UNSTREAMLINED)).thenReturn(hasUnstreamlinedQuirk);
        when(entity.doomedOnGround()).thenReturn(false);
        when(entity.doomedInAtmosphere()).thenReturn(false);
        when(entity.doomedInSpace()).thenReturn(false);

        Unit unit = mock(Unit.class);
        when(unit.getEntity()).thenReturn(entity);
        when(unit.isAvailable()).thenReturn(true);
        when(unit.isFunctional()).thenReturn(true);

        ScenarioForceTemplate template = mock(ScenarioForceTemplate.class);
        when(template.getAllowedUnitType()).thenReturn(allowedUnitType);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseDropShips()).thenReturn(isUseDropShips);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(LocalDate.of(3025, 1, 15));

        CurrentLocation location = mock(CurrentLocation.class);
        when(campaign.getLocation()).thenReturn(location);

        if (atmosphere != null) {
            Planet planet = mock(Planet.class);
            when(planet.getAtmosphere(any())).thenReturn(atmosphere);
            when(location.getPlanet()).thenReturn(planet);
        } else {
            when(location.getPlanet()).thenReturn(null);
        }

        return new Object[] { unit, template, campaign };
    }

    @Test
    void isValidUnitForScenario_normalDropshipOnGround_allowed() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, false,
              Atmosphere.BREATHABLE, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.AllGroundTerrain);

        assertTrue(result);
    }

    @Test
    void isValidUnitForScenario_unstreamlinedDropshipOnGroundWithAtmosphere_rejected() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, true,
              Atmosphere.BREATHABLE, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.AllGroundTerrain);

        assertFalse(result);
    }

    @Test
    void isValidUnitForScenario_unstreamlinedDropshipOnGroundWithVacuum_allowed() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, true,
              Atmosphere.NONE, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.AllGroundTerrain);

        assertTrue(result);
    }

    @Test
    void isValidUnitForScenario_unstreamlinedDropshipInSpace_allowed() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, true,
              Atmosphere.BREATHABLE, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.Space);

        assertTrue(result);
    }

    @Test
    void isValidUnitForScenario_unstreamlinedDropshipOnLowAtmosphereWithAtmosphere_rejected() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, true,
              Atmosphere.BREATHABLE, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.LowAtmosphere);

        assertFalse(result);
    }

    @Test
    void isValidUnitForScenario_unstreamlinedDropshipNullPlanet_rejected() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, true,
              null, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.AllGroundTerrain);

        assertFalse(result);
    }

    @Test
    void isValidUnitForScenario_dropshipWhenDropShipsDisabled_rejected() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.DROPSHIP, false,
              Atmosphere.BREATHABLE, false, UnitType.DROPSHIP);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.AllGroundTerrain);

        assertFalse(result);
    }

    @Test
    void isValidUnitForScenario_doomedOnGround_rejected() {
        Object[] mocks = setupIsValidUnitMocks(UnitType.JUMPSHIP, false,
              Atmosphere.BREATHABLE, true, ScenarioForceTemplate.SPECIAL_UNIT_TYPE_ATB_MIX);
        Entity entity = ((Unit) mocks[0]).getEntity();
        when(entity.doomedOnGround()).thenReturn(true);

        boolean result = StratConRulesManager.isValidUnitForScenario(
              (Unit) mocks[0], (ScenarioForceTemplate) mocks[1],
              (Campaign) mocks[2], MapLocation.AllGroundTerrain);

        assertFalse(result);
    }
}
