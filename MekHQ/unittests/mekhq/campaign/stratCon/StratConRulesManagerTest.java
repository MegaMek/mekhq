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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import mekhq.campaign.Campaign;
import mekhq.campaign.Hangar;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.force.CombatTeam;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.AtBDynamicScenario;
import mekhq.campaign.mission.enums.CombatRole;
import mekhq.campaign.mission.enums.ScenarioType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

/**
 * Tests for {@link StratConRulesManager}
 */
class StratConRulesManagerTest {

    /**
     * Creates the common mock infrastructure needed for deployForceToCoords tests.
     * processForceDeployment -> scanNeighboringCoords touches many objects.
     */
    private void setupProcessForceDeploymentMocks(Campaign campaign, CampaignOptions options,
          StratConTrackState track, StratConCoords coords, int forceID) {
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
     * Verifies that when a force deploys to coordinates containing an Official Challenge scenario,
     * the force is NOT auto-assigned to that scenario. This is a regression test for
     * <a href="https://github.com/MegaMek/mekhq/issues/8612">issue #8612</a>.
     */
    @Test
    void deployForceToCoords_officialChallenge_doesNotAutoAssignForce() {
        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(campaign.getCampaignOptions()).thenReturn(options);

        AtBContract contract = mock(AtBContract.class);
        StratConTrackState track = mock(StratConTrackState.class);
        StratConCoords coords = new StratConCoords(2, 3);
        int forceID = 1;

        // Create an Official Challenge scenario at the target coords
        StratConScenario challengeScenario = mock(StratConScenario.class);
        AtBDynamicScenario backingScenario = mock(AtBDynamicScenario.class);
        when(backingScenario.getStratConScenarioType()).thenReturn(ScenarioType.OFFICIAL_CHALLENGE);
        when(backingScenario.isFinalized()).thenReturn(true);
        when(backingScenario.isCloaked()).thenReturn(false);
        when(challengeScenario.getBackingScenario()).thenReturn(backingScenario);
        when(track.getScenario(coords)).thenReturn(challengeScenario);

        // Setup combat team
        CombatTeam combatTeam = mock(CombatTeam.class);
        CombatRole combatRole = mock(CombatRole.class);
        when(combatRole.isPatrol()).thenReturn(false);
        when(combatRole.isTraining()).thenReturn(false);
        when(combatTeam.getRole()).thenReturn(combatRole);
        var combatTeamsMap = new Hashtable<Integer, CombatTeam>();
        combatTeamsMap.put(forceID, combatTeam);
        when(campaign.getCombatTeamsAsMap()).thenReturn(combatTeamsMap);

        setupProcessForceDeploymentMocks(campaign, options, track, coords, forceID);

        // Act
        StratConRulesManager.deployForceToCoords(coords, forceID, campaign, contract, track, false);

        // Assert: force should NOT be added to Official Challenge scenario
        verify(challengeScenario, never()).addPrimaryForce(anyInt());
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

        setupProcessForceDeploymentMocks(campaign, options, track, coords, forceID);

        // Act
        StratConRulesManager.deployForceToCoords(coords, forceID, campaign, contract, track, false);

        // Assert: force SHOULD be added to the regular scenario
        verify(regularScenario).addPrimaryForce(forceID);
    }
}
