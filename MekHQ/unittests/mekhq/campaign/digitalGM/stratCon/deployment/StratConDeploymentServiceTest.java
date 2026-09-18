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
package mekhq.campaign.digitalGM.stratCon.deployment;

import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementEligibilityType.AUXILIARY;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.DELAYED;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.FAILED;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.INSTANT;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.INTERCEPTED;
import static mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager;
import mekhq.campaign.digitalGM.stratCon.StratConRulesManager.ReinforcementResultsType;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.force.Formation;
import mekhq.campaign.mission.scenarios.AtBDynamicScenario;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for the campaign-state side of {@link StratConDeploymentService}: how a reinforcement roll's outcome is
 * recorded on the scenario, which units land in the delayed and instant arrival lists, how loose auxiliary and utility
 * units are added, and the guard that keeps a staged formation's own units from being committed a second time.
 */
class StratConDeploymentServiceTest {
    private static final String TEMPLATE_ALPHA = "Reinforcements Alpha";
    private static final String TEMPLATE_BRAVO = "Reinforcements Bravo";
    private static final int FORMATION_ID = 11;
    private static final int OTHER_FORMATION_ID = 12;
    private static final int TARGET_NUMBER = 7;

    private Campaign campaign;
    private StratConCampaignState campaignState;
    private StratConTrackState track;
    private StratConScenario scenario;
    private AtBDynamicScenario backingScenario;

    @BeforeEach
    void setUp() {
        campaign = mock(Campaign.class);
        campaignState = mock(StratConCampaignState.class);
        track = mock(StratConTrackState.class);
        scenario = mock(StratConScenario.class);
        backingScenario = new AtBDynamicScenario();
        when(scenario.getBackingScenario()).thenReturn(backingScenario);
    }

    // region commitReinforcementForces

    @Test
    void failedRollRecordsTheForceAndAddsNothing() {
        Formation formation = formationWithUnits(FORMATION_ID, UUID.randomUUID());

        List<Formation> committed = commit(singleTemplate(formation), FAILED, false, false);

        assertEquals(List.of(formation), committed, "a failed force is still reported back to the caller");
        verify(scenario).addFailedReinforcements(FORMATION_ID);
        verify(scenario, never()).addForce(any(), any(), any());
        assertTrue(backingScenario.getFriendlyDelayedReinforcements().isEmpty());
        assertTrue(backingScenario.getFriendlyInstantReinforcements().isEmpty());
    }

    @Test
    void interceptedRollCountsAsAFailure() {
        Formation formation = formationWithUnits(FORMATION_ID, UUID.randomUUID());

        commit(singleTemplate(formation), INTERCEPTED, false, false);

        verify(scenario).addFailedReinforcements(FORMATION_ID);
        verify(scenario, never()).addForce(any(), any(), any());
        assertTrue(backingScenario.getFriendlyDelayedReinforcements().isEmpty());
        assertTrue(backingScenario.getFriendlyInstantReinforcements().isEmpty());
    }

    @Test
    void delayedRollAddsTheForceAndQueuesItsUnitsAsDelayed() {
        UUID firstUnitId = UUID.randomUUID();
        UUID secondUnitId = UUID.randomUUID();
        Formation formation = formationWithUnits(FORMATION_ID, firstUnitId, secondUnitId);

        commit(singleTemplate(formation), DELAYED, false, false);

        verify(scenario).addForce(formation, TEMPLATE_ALPHA, campaign);
        verify(scenario, never()).addFailedReinforcements(anyInt());
        assertEquals(List.of(firstUnitId, secondUnitId), backingScenario.getFriendlyDelayedReinforcements());
        assertTrue(backingScenario.getFriendlyInstantReinforcements().isEmpty());
    }

    @Test
    void instantRollAddsTheForceAndQueuesItsUnitsAsInstant() {
        UUID unitId = UUID.randomUUID();
        Formation formation = formationWithUnits(FORMATION_ID, unitId);

        commit(singleTemplate(formation), INSTANT, false, false);

        verify(scenario).addForce(formation, TEMPLATE_ALPHA, campaign);
        assertEquals(List.of(unitId), backingScenario.getFriendlyInstantReinforcements());
        assertTrue(backingScenario.getFriendlyDelayedReinforcements().isEmpty());
    }

    @Test
    void successfulRollAddsTheForceWithoutAnArrivalQueue() {
        Formation formation = formationWithUnits(FORMATION_ID, UUID.randomUUID());

        commit(singleTemplate(formation), SUCCESS, false, false);

        verify(scenario).addForce(formation, TEMPLATE_ALPHA, campaign);
        verify(scenario, never()).addFailedReinforcements(anyInt());
        assertTrue(backingScenario.getFriendlyDelayedReinforcements().isEmpty());
        assertTrue(backingScenario.getFriendlyInstantReinforcements().isEmpty());
    }

    @Test
    void carriersThatStayHomeAreLeftOutOfTheArrivalQueue() {
        UUID fightingUnitId = UUID.randomUUID();
        UUID carrierUnitId = UUID.randomUUID();
        Formation formation = formationWithUnits(FORMATION_ID, fightingUnitId, carrierUnitId);
        Unit carrier = carrierUnit(carrierUnitId);
        when(campaign.getUnit(carrierUnitId)).thenReturn(carrier);

        commit(singleTemplate(formation), DELAYED, false, false);

        assertEquals(List.of(fightingUnitId), backingScenario.getFriendlyDelayedReinforcements());
    }

    @Test
    void unknownUnitsAreLeftOutOfTheArrivalQueue() {
        UUID knownUnitId = UUID.randomUUID();
        UUID missingUnitId = UUID.randomUUID();
        Formation formation = formationWithUnits(FORMATION_ID, knownUnitId, missingUnitId);
        when(campaign.getUnit(missingUnitId)).thenReturn(null);

        commit(singleTemplate(formation), INSTANT, false, false);

        assertEquals(List.of(knownUnitId), backingScenario.getFriendlyInstantReinforcements());
    }

    @Test
    void eachForceIsAddedUnderItsOwnTemplateInCommitOrder() {
        Formation alpha = formationWithUnits(FORMATION_ID, UUID.randomUUID());
        Formation bravo = formationWithUnits(OTHER_FORMATION_ID, UUID.randomUUID());
        Map<String, List<Formation>> forcesByTemplate = new LinkedHashMap<>();
        forcesByTemplate.put(TEMPLATE_ALPHA, List.of(alpha));
        forcesByTemplate.put(TEMPLATE_BRAVO, List.of(bravo));

        List<Formation> committed = commit(forcesByTemplate, SUCCESS, false, false);

        assertEquals(List.of(alpha, bravo), committed);
        verify(scenario).addForce(alpha, TEMPLATE_ALPHA, campaign);
        verify(scenario).addForce(bravo, TEMPLATE_BRAVO, campaign);
    }

    @Test
    void gmAndInstantFlagsReachTheReinforcementRoll() {
        UUID unitId = UUID.randomUUID();
        Formation formation = formationWithUnits(FORMATION_ID, unitId);

        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            rules.when(() -> StratConRulesManager.getReinforcementType(FORMATION_ID, track, campaign, campaignState))
                  .thenReturn(AUXILIARY);
            rules.when(() -> StratConRulesManager.processReinforcementDeployment(formation, AUXILIARY, campaignState,
                  scenario, campaign, TARGET_NUMBER, true, true)).thenReturn(INSTANT);

            StratConDeploymentService.commitReinforcementForces(campaign, campaignState, track, scenario,
                  singleTemplate(formation), TARGET_NUMBER, true, true);

            rules.verify(() -> StratConRulesManager.processReinforcementDeployment(formation, AUXILIARY,
                  campaignState, scenario, campaign, TARGET_NUMBER, true, true));
        }

        assertEquals(List.of(unitId), backingScenario.getFriendlyInstantReinforcements());
    }

    // endregion

    // region loose units

    @Test
    void auxiliaryUnitsArriveInstantlyAndSpendLeadership() {
        UUID unitId = UUID.randomUUID();
        Unit unit = unitWithId(unitId);

        StratConDeploymentService.addAuxiliaryUnits(scenario, List.of(unit));

        verify(scenario).addUnit(unit, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, true);
        assertEquals(List.of(unitId), backingScenario.getFriendlyInstantReinforcements());
    }

    @Test
    void utilityUnitsArriveInstantlyWithoutSpendingLeadership() {
        UUID unitId = UUID.randomUUID();
        Unit unit = unitWithId(unitId);

        StratConDeploymentService.addUtilityUnits(scenario, List.of(unit));

        verify(scenario).addUnit(unit, ScenarioForceTemplate.PRIMARY_FORCE_TEMPLATE_ID, false);
        assertEquals(List.of(unitId), backingScenario.getFriendlyInstantReinforcements());
    }

    // endregion

    // region excludeUnitsOfFormations

    @Test
    void unitsInsideAStagedFormationAreExcludedInOrder() {
        Unit looseUnit = unitWithId(UUID.randomUUID());
        UUID lanceMemberId = UUID.randomUUID();
        Unit lanceMember = unitWithId(lanceMemberId);
        Unit otherLooseUnit = unitWithId(UUID.randomUUID());
        Formation stagedLance = formationWithUnits(FORMATION_ID, lanceMemberId);

        List<Unit> remaining = StratConDeploymentService.excludeUnitsOfFormations(
              List.of(looseUnit, lanceMember, otherLooseUnit), List.of(stagedLance));

        assertEquals(List.of(looseUnit, otherLooseUnit), remaining);
    }

    @Test
    void nothingIsExcludedWhenNoFormationIsStaged() {
        Unit firstUnit = unitWithId(UUID.randomUUID());
        Unit secondUnit = unitWithId(UUID.randomUUID());

        List<Unit> remaining = StratConDeploymentService.excludeUnitsOfFormations(List.of(firstUnit, secondUnit),
              List.of());

        assertEquals(List.of(firstUnit, secondUnit), remaining);
    }

    @Test
    void unitIdsOfFormationsIncludeSupportSubFormations() {
        UUID supportUnitId = UUID.randomUUID();
        Formation formation = mock(Formation.class);
        when(formation.getAllUnits(false)).thenReturn(new Vector<>(List.of(supportUnitId)));
        when(formation.getAllUnits(true)).thenReturn(new Vector<>());

        Set<UUID> unitIds = StratConDeploymentService.unitIdsOfFormations(List.of(formation));

        assertEquals(Set.of(supportUnitId), unitIds,
              "the whole formation tree deploys together, so support sub-formations count too");
    }

    // endregion

    // region helpers

    private static Map<String, List<Formation>> singleTemplate(Formation formation) {
        return Map.of(TEMPLATE_ALPHA, List.of(formation));
    }

    /** Runs the commit with every reinforcement roll forced to the given result. */
    private List<Formation> commit(Map<String, List<Formation>> forcesByTemplate, ReinforcementResultsType result,
          boolean isGMReinforcement, boolean isInstantlyDeployed) {
        try (MockedStatic<StratConRulesManager> rules = mockStatic(StratConRulesManager.class)) {
            rules.when(() -> StratConRulesManager.getReinforcementType(anyInt(), eq(track), eq(campaign),
                  eq(campaignState))).thenReturn(AUXILIARY);
            rules.when(() -> StratConRulesManager.processReinforcementDeployment(any(), eq(AUXILIARY),
                  eq(campaignState), eq(scenario), eq(campaign), anyInt(), anyBoolean(), anyBoolean()))
                  .thenReturn(result);

            return StratConDeploymentService.commitReinforcementForces(campaign, campaignState, track, scenario,
                  forcesByTemplate, TARGET_NUMBER, isGMReinforcement, isInstantlyDeployed);
        }
    }

    /** A formation holding the given units, each of which the campaign resolves to a plain (non-carrier) unit. */
    private Formation formationWithUnits(int formationId, UUID... unitIds) {
        Formation formation = mock(Formation.class);
        when(formation.getId()).thenReturn(formationId);
        when(formation.getAllUnits(anyBoolean())).thenReturn(new Vector<>(List.of(unitIds)));
        for (UUID unitId : unitIds) {
            // Build the unit before stubbing the campaign, so the two stubbings do not nest.
            Unit unit = unitWithId(unitId);
            when(campaign.getUnit(unitId)).thenReturn(unit);
        }
        return formation;
    }

    private static Unit unitWithId(UUID unitId) {
        Unit unit = mock(Unit.class);
        when(unit.getId()).thenReturn(unitId);
        return unit;
    }

    private static Unit carrierUnit(UUID unitId) {
        Unit unit = unitWithId(unitId);
        when(unit.isCarrier()).thenReturn(true);
        return unit;
    }

    // endregion
}
