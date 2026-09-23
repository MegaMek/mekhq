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

import static mekhq.campaign.enums.DailyReportType.BATTLE;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.PointOfInterestDeploymentOutcome;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinition;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestDefinitions;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConPointOfInterestRules;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConScheduledPointOfInterest;
import mekhq.campaign.digitalGM.stratCon.pointOfInterest.StratConTargetIntelligenceBehavior;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.scenarios.ScenarioForceTemplate.ForceAlignment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for target intelligence on Objective Raid contracts: which intelligence leads to a facility (settled at
 * contract start, one per point of scale at most), the intelligence never being an objective itself while the facility
 * it turns up is, the facility spawning, and ordinary intelligence proving worthless.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConTargetIntelligenceTest {
    private static final String TYPE_ID = "UnitTestTargetIntelligence";
    private static final LocalDate TODAY = LocalDate.of(3151, 1, 1);
    private static final StratConCoords INTEL_COORDS = new StratConCoords(0, 0);
    private static final int FORMATION_ID = 7;

    private StratConTrackState track;
    private StratConCampaignState campaignState;
    private AbstractContract contract;

    @BeforeAll
    static void loadStratConData() {
        StratConTestData.install();
    }

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConTargetIntelligenceBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        for (int x = 0; x < 5; x++) {
            for (int y = 0; y < 5; y++) {
                track.setTerrainTile(new StratConCoords(x, y), "Plains");
            }
        }

        campaignState = new StratConCampaignState();
        campaignState.addTrack(track);

        contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.OBJECTIVE_RAID);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        when(contract.getStartDate()).thenReturn(TODAY);
        when(contract.getLengthInMonths()).thenReturn(3);
        when(contract.getScale()).thenReturn(3);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign with real options - Essential Scenarios Only on, so no scenario can break out - holding the test
     * contract.
     */
    private Campaign campaign() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.ESSENTIAL_SCENARIOS_ONLY, true);

        Campaign campaign = mock(Campaign.class);
        when(campaign.getCampaignOptions()).thenReturn(options);
        when(campaign.getLocalDate()).thenReturn(TODAY);
        when(campaign.getActiveContracts()).thenReturn(List.of(contract));
        return campaign;
    }

    private StratConPointOfInterest placeIntelligence(boolean isFacilityLead) {
        StratConPointOfInterest intelligence = new StratConPointOfInterest(TYPE_ID, INTEL_COORDS);
        if (isFacilityLead) {
            intelligence.setStateValue(StratConTargetIntelligenceBehavior.FACILITY_LEAD_STATE_KEY, "true");
        }
        assertTrue(track.addPointOfInterest(intelligence), "test setup: the intelligence should be placed");
        return intelligence;
    }

    private static int countFacilityLeads(List<StratConScheduledPointOfInterest> scheduled) {
        int leads = 0;
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            if ("true".equals(pointOfInterest.getInitialState()
                                    .get(StratConTargetIntelligenceBehavior.FACILITY_LEAD_STATE_KEY))) {
                leads++;
            }
        }
        return leads;
    }

    // Scheduling

    @Test
    void anObjectiveRaidSchedulesTargetIntelligenceInPlaceOfItsEssentialScenarios() {
        assertEquals(StratConTargetIntelligenceBehavior.TYPE_ID,
              StratConContractInitializer.getSpecialPointOfInterestTypeId(contract, true));
        assertTrue(StratConContractInitializer.isReplacingEssentialScenarios(contract, true));
        assertFalse(StratConContractInitializer.isSpecialPointOfInterestObjective(
              StratConTargetIntelligenceBehavior.TYPE_ID), "the intelligence is not itself an objective");
    }

    @Test
    void onlyOnePieceOfIntelligencePerPointOfScaleLeadsToAFacility() {
        // Three tracks on the three-month table schedule three per roll, and scale 3 makes three rolls: nine in all.
        when(contract.getTrackCount()).thenReturn(3);
        StratConCampaignState scheduleState = new StratConCampaignState();

        StratConContractInitializer.scheduleSpecialPointsOfInterest(contract, scheduleState, true,
              StratConTargetIntelligenceBehavior.TYPE_ID);

        List<StratConScheduledPointOfInterest> scheduled = scheduleState.getScheduledPointsOfInterest();
        assertEquals(9, scheduled.size());
        assertEquals(3, countFacilityLeads(scheduled), "one lead per point of scale");
        for (StratConScheduledPointOfInterest pointOfInterest : scheduled) {
            assertFalse(pointOfInterest.isStrategicObjective());
        }
    }

    @Test
    void fewerPiecesOfIntelligenceThanTheScaleAllLead() {
        List<StratConScheduledPointOfInterest> scheduled = new ArrayList<>();
        for (int index = 0; index < 2; index++) {
            scheduled.add(new StratConScheduledPointOfInterest(TODAY, StratConTargetIntelligenceBehavior.TYPE_ID,
                  false));
        }

        AbstractContract largeContract = mock(AbstractContract.class);
        when(largeContract.getScale()).thenReturn(5);

        new StratConTargetIntelligenceBehavior().onScheduled(scheduled, largeContract);

        assertEquals(2, countFacilityLeads(scheduled));
    }

    // The facility

    @Test
    void aSpawnedFacilityIsAHostileRevealedObjectiveToDestroy() {
        StratConCoords coords = StratConContractInitializer.spawnObjectiveFacility(track, contract, campaign());

        assertNotNull(coords);
        StratConFacility facility = track.getFacility(coords);
        assertNotNull(facility);
        assertEquals(ForceAlignment.Opposing, facility.getOwner());
        assertTrue(facility.isStrategicObjective());
        assertTrue(facility.isVisible());
        assertTrue(track.getRevealedCoords().contains(coords));

        StratConStrategicObjective objective = track.getObjectivesByCoords().get(coords);
        assertNotNull(objective);
        assertEquals(StrategicObjectiveType.FacilityDestruction, objective.getObjectiveType());
        assertFalse(objective.isObjectiveCompleted(track));

        track.removeFacility(coords);
        assertTrue(objective.isObjectiveCompleted(track), "met once the facility is destroyed");
    }

    // Following it up

    @Test
    void aFacilityLeadTurnsUpAFacilityWithNoRoll() {
        StratConPointOfInterest intelligence = placeIntelligence(true);
        Campaign campaign = campaign();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
              StratConPointOfInterestRules.processFormationDeployment(track, INTEL_COORDS, FORMATION_ID, campaign));

        assertNull(track.getPointOfInterest(intelligence.getId()), "the intelligence is used up");
        assertEquals(1, track.getFacilities().size(), "one facility turned up");
        assertEquals(1, track.getStrategicObjectives().size(), "the facility is the objective, not the intelligence");
        verify(campaign).addReport(eq(BATTLE), anyString());
    }

    @Test
    void ordinaryIntelligenceProvesWorthless() {
        StratConPointOfInterest intelligence = placeIntelligence(false);
        Campaign campaign = campaign();

        assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
              StratConPointOfInterestRules.processFormationDeployment(track, INTEL_COORDS, FORMATION_ID, campaign));

        assertNull(track.getPointOfInterest(intelligence.getId()));
        assertTrue(track.getFacilities().isEmpty(), "no facility");
        assertTrue(track.getStrategicObjectives().isEmpty());
        verify(campaign).addReport(eq(GENERAL), anyString());
        verify(campaign, never()).addReport(eq(BATTLE), anyString());
    }

    @Test
    void theIntelligenceHasItsOwnText() {
        StratConPointOfInterest intelligence = placeIntelligence(false);

        String description = intelligence.getBehavior().getObjectiveDescription(intelligence, track);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }
}
