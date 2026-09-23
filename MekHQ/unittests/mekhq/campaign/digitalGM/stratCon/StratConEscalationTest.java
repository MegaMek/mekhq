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

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractDefinition.StrategicObjectiveType;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractMoraleLevel;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for Escalation: which contracts track it, its maximum, what raises it and by how much, a Garrison Duty
 * contract's Escalation starting at its maximum and only falling, its part in the morale check, and the Diversionary
 * Raid's Escalation objective.
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConEscalationTest {
    private static final int SCALE = 2;

    private StratConCampaignState campaignState;

    @BeforeEach
    void setUp() {
        campaignState = new StratConCampaignState();
        StratConTrackState track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);
        campaignState.addTrack(track);
    }

    /**
     * A campaign whose test contract was accepted with "Contracts Use Special Mechanics" on or off, as fixed on its
     * StratCon state.
     */
    private Campaign campaign(boolean isContractsUseSpecialMechanics) {
        return campaign(isContractsUseSpecialMechanics, false);
    }

    private Campaign campaign(boolean isContractsUseSpecialMechanics, boolean isMapless) {
        campaignState.setContractsUseSpecialMechanics(isContractsUseSpecialMechanics);

        Campaign campaign = mock(Campaign.class);
        CampaignOptions options = mock(CampaignOptions.class);
        when(options.isUseStratConMaplessMode()).thenReturn(isMapless);
        when(campaign.getCampaignOptions()).thenReturn(options);
        return campaign;
    }

    private AbstractContract contract(ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        when(contract.getScale()).thenReturn(SCALE);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getMoraleLevel()).thenReturn(ContractMoraleLevel.STALEMATE);
        return contract;
    }

    private StratConStrategicObjective escalationObjective() {
        for (StratConStrategicObjective objective : campaignState.getTrack(0).getStrategicObjectives()) {
            if (objective.getObjectiveType() == StrategicObjectiveType.Escalation) {
                return objective;
            }
        }
        return null;
    }

    // Which contracts track Escalation

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class,
          names = { "DIVERSIONARY_RAID", "EXTRACTION_RAID", "OBJECTIVE_RAID", "OBSERVATION_RAID", "RECON_RAID",
                    "GUERRILLA_WARFARE", "ASSASSINATION", "SABOTAGE", "TERRORISM", "PIRATE_RAID", "GARRISON_DUTY" })
    void raidsGuerrillaOperationsSabotageTerrorismPirateRaidsAndGarrisonsTrackEscalation(ContractObjectiveType objectiveType) {
        assertTrue(StratConEscalation.isEscalationContract(contract(objectiveType)));
        assertTrue(StratConEscalation.isEscalationUsed(campaign(true), contract(objectiveType)));
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, names = { "PIRATE_HUNTING", "ESPIONAGE", "UNDEFINED" })
    void otherContractsDoNotTrackEscalation(ContractObjectiveType objectiveType) {
        assertFalse(StratConEscalation.isEscalationContract(contract(objectiveType)));
    }

    @Test
    void escalationBelongsToTheSpecialMechanicsOption() {
        assertFalse(StratConEscalation.isEscalationUsed(campaign(false),
              contract(ContractObjectiveType.DIVERSIONARY_RAID)));
    }

    @Test
    void escalationFollowsTheOptionAsItWasWhenTheContractWasAccepted() {
        // The campaign's option is never read: only the contract's own record of it counts, so changing the option
        // mid-contract changes nothing.
        Campaign campaign = campaign(true);
        AbstractContract contract = contract(ContractObjectiveType.DIVERSIONARY_RAID);
        assertTrue(StratConEscalation.isEscalationUsed(campaign, contract));

        campaignState.setContractsUseSpecialMechanics(false);
        assertFalse(StratConEscalation.isEscalationUsed(campaign, contract));
    }

    @Test
    void escalationIsNeverUsedInMaplessPlay() {
        assertFalse(StratConEscalation.isEscalationUsed(campaign(true, true),
              contract(ContractObjectiveType.DIVERSIONARY_RAID)));
    }

    @Test
    void aContractWithoutAStratConStateOrObjectiveTypeDoesNotTrackEscalation() {
        AbstractContract withoutState = contract(ContractObjectiveType.DIVERSIONARY_RAID);
        when(withoutState.getStratConCampaignState()).thenReturn(null);

        assertFalse(StratConEscalation.isEscalationUsed(campaign(true), withoutState));
        assertFalse(StratConEscalation.isEscalationUsed(campaign(true), null));
        assertFalse(StratConEscalation.isEscalationContract(contract(null)));
    }

    // Its range

    @Test
    void escalationRunsToOneHundredPerPointOfScale() {
        assertEquals(100 * SCALE, StratConEscalation.getMaximumEscalation(contract(ContractObjectiveType.RECON_RAID)));
        assertEquals(50 * SCALE,
              StratConEscalation.getDiversionaryRaidTarget(contract(ContractObjectiveType.DIVERSIONARY_RAID)));
    }

    @Test
    void escalationIsCappedAtItsMaximum() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);

        StratConEscalation.increaseEscalation(campaign(true), contract, 1000);

        assertEquals(100 * SCALE, campaignState.getEscalation());
    }

    @Test
    void escalationOnlyEverRises() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        campaignState.setEscalation(10);

        StratConEscalation.increaseEscalation(campaign(true), contract, -5);
        StratConEscalation.increaseEscalation(campaign(true), contract, 0);

        assertEquals(10, campaignState.getEscalation());
    }

    @Test
    void escalationDoesNotRiseOnContractsThatDoNotTrackIt() {
        StratConEscalation.increaseEscalation(campaign(false), contract(ContractObjectiveType.RECON_RAID), 5);
        StratConEscalation.increaseEscalation(campaign(true), contract(ContractObjectiveType.ESPIONAGE), 5);

        assertEquals(0, campaignState.getEscalation());
    }

    // What raises it

    @Test
    void deployingToAnEmptyHexRaisesEscalationByOne() {
        StratConEscalation.onEmptyHexDeployment(campaign(true), contract(ContractObjectiveType.RECON_RAID));

        assertEquals(1, campaignState.getEscalation());
    }

    @Test
    void resolvedScenariosRollTheirDice() {
        assertEquals(0, StratConEscalation.getScenarioEscalationDice(false, false), "a lost scenario adds nothing");
        assertEquals(1, StratConEscalation.getScenarioEscalationDice(true, false), "a won scenario adds 1d6");
        assertEquals(2, StratConEscalation.getScenarioEscalationDice(false, true), "a lost facility fight adds 2d6");
        assertEquals(3, StratConEscalation.getScenarioEscalationDice(true, true),
              "a won facility fight adds 3d6, in place of the 1d6 for winning");
    }

    @Test
    void aWonFacilityFightRaisesEscalationByThreeToEighteen() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);

        StratConEscalation.onScenarioCompleted(campaign(true), contract, true, true);

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 3) && (escalation <= 18), "3d6 Escalation, got " + escalation);
    }

    @Test
    void aLostScenarioAwayFromAFacilityRaisesNoEscalation() {
        StratConEscalation.onScenarioCompleted(campaign(true), contract(ContractObjectiveType.RECON_RAID), false,
              false);

        assertEquals(0, campaignState.getEscalation());
    }

    @Test
    void strikingAHighProfileTargetRaisesEscalationByThreeToEighteen() {
        StratConEscalation.onHighProfileTargetStruck(campaign(true), contract(ContractObjectiveType.DIVERSIONARY_RAID));

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 3) && (escalation <= 18), "3d6 Escalation, got " + escalation);
    }

    // The morale check

    @Test
    void aRollNoHigherThanEscalationRaisesMorale() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        campaignState.setEscalation(40);

        String report = StratConEscalation.applyMoraleRoll(contract, 40);

        verify(contract).changeMorale(ContractMoraleLevel.ADVANCING);
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
    }

    @Test
    void aRollAboveEscalationLeavesMoraleAlone() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);
        campaignState.setEscalation(40);

        String report = StratConEscalation.applyMoraleRoll(contract, 41);

        verify(contract, never()).changeMorale(any(ContractMoraleLevel.class));
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
    }

    @Test
    void noEscalationNeverRaisesMorale() {
        AbstractContract contract = contract(ContractObjectiveType.RECON_RAID);

        StratConEscalation.applyMoraleRoll(contract, 1);

        verify(contract, never()).changeMorale(any(ContractMoraleLevel.class));
    }

    @Test
    void moraleRisesOneLevelAndNoFurtherThanOverwhelming() {
        assertEquals(ContractMoraleLevel.CRITICAL, StratConEscalation.getRaisedMoraleLevel(ContractMoraleLevel.ROUTED));
        assertEquals(ContractMoraleLevel.ADVANCING,
              StratConEscalation.getRaisedMoraleLevel(ContractMoraleLevel.STALEMATE));
        assertEquals(ContractMoraleLevel.OVERWHELMING,
              StratConEscalation.getRaisedMoraleLevel(ContractMoraleLevel.OVERWHELMING));
    }

    @Test
    void contractsThatDoNotTrackEscalationRollNothing() {
        assertNull(StratConEscalation.rollForMorale(campaign(false), contract(ContractObjectiveType.RECON_RAID)));
    }

    // Garrison Duty: Escalation starts at its maximum and only falls

    @Test
    void onlyGarrisonDutyDeescalates() {
        assertTrue(StratConEscalation.isDeescalatingContract(contract(ContractObjectiveType.GARRISON_DUTY)));
        assertFalse(StratConEscalation.isDeescalatingContract(contract(ContractObjectiveType.RECON_RAID)));
        assertFalse(StratConEscalation.isDeescalatingContract(contract(null)));
    }

    @Test
    void aGarrisonStartsAtMaximumEscalation() {
        StratConEscalation.startEscalation(contract(ContractObjectiveType.GARRISON_DUTY), campaignState);

        assertEquals(100 * SCALE, campaignState.getEscalation());
    }

    @Test
    void otherContractsStartAtNoEscalation() {
        StratConEscalation.startEscalation(contract(ContractObjectiveType.RECON_RAID), campaignState);

        assertEquals(0, campaignState.getEscalation());
    }

    @Test
    void nothingRaisesAGarrisonsEscalation() {
        AbstractContract contract = contract(ContractObjectiveType.GARRISON_DUTY);
        campaignState.setEscalation(50);

        StratConEscalation.increaseEscalation(campaign(true), contract, 20);
        StratConEscalation.onHighProfileTargetStruck(campaign(true), contract);

        assertEquals(50, campaignState.getEscalation());
    }

    @Test
    void deployingToAnEmptyHexLowersAGarrisonsEscalationByOne() {
        campaignState.setEscalation(50);

        StratConEscalation.onEmptyHexDeployment(campaign(true), contract(ContractObjectiveType.GARRISON_DUTY));

        assertEquals(49, campaignState.getEscalation());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void winningAScenarioLowersAGarrisonsEscalationByOneDie(boolean isHostileFacilityScenario) {
        campaignState.setEscalation(50);

        StratConEscalation.onScenarioCompleted(campaign(true), contract(ContractObjectiveType.GARRISON_DUTY), true,
              isHostileFacilityScenario);

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 44) && (escalation <= 49), "-1d6 Escalation, got " + escalation);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void losingAScenarioLeavesAGarrisonsEscalationAlone(boolean isHostileFacilityScenario) {
        campaignState.setEscalation(50);

        StratConEscalation.onScenarioCompleted(campaign(true), contract(ContractObjectiveType.GARRISON_DUTY), false,
              isHostileFacilityScenario);

        assertEquals(50, campaignState.getEscalation());
    }

    @Test
    void aShowOfForceLowersAGarrisonsEscalationByThreeToEighteen() {
        campaignState.setEscalation(50);

        StratConEscalation.onShowOfForce(campaign(true), contract(ContractObjectiveType.GARRISON_DUTY));

        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 32) && (escalation <= 47), "-3d6 Escalation, got " + escalation);
    }

    @Test
    void aGarrisonsEscalationStopsAtZero() {
        campaignState.setEscalation(2);

        StratConEscalation.decreaseEscalation(campaign(true), contract(ContractObjectiveType.GARRISON_DUTY), 10);

        assertEquals(0, campaignState.getEscalation());
    }

    @Test
    void onlyAGarrisonsEscalationFalls() {
        campaignState.setEscalation(50);

        StratConEscalation.decreaseEscalation(campaign(true), contract(ContractObjectiveType.RECON_RAID), 10);
        StratConEscalation.decreaseEscalation(campaign(false), contract(ContractObjectiveType.GARRISON_DUTY), 10);

        assertEquals(50, campaignState.getEscalation());
    }

    @Test
    void aGarrisonsMoraleRollRaisesMoraleAtOrBelowEscalation() {
        AbstractContract contract = contract(ContractObjectiveType.GARRISON_DUTY);
        campaignState.setEscalation(40);

        String report = StratConEscalation.applyMoraleRoll(contract, 40);

        verify(contract).changeMorale(ContractMoraleLevel.ADVANCING);
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
    }

    @Test
    void aGarrisonsMoraleRollLowersMoraleAboveEscalation() {
        AbstractContract contract = contract(ContractObjectiveType.GARRISON_DUTY);
        campaignState.setEscalation(40);

        String report = StratConEscalation.applyMoraleRoll(contract, 41);

        verify(contract).changeMorale(ContractMoraleLevel.WEAKENED);
        assertTrue(isResourceKeyValid(report), "missing resource key: " + report);
    }

    @Test
    void moraleFallsOneLevelAndNoFurtherThanRouted() {
        assertEquals(ContractMoraleLevel.WEAKENED,
              StratConEscalation.getLoweredMoraleLevel(ContractMoraleLevel.STALEMATE));
        assertEquals(ContractMoraleLevel.ROUTED, StratConEscalation.getLoweredMoraleLevel(ContractMoraleLevel.ROUTED));
    }

    // The Diversionary Raid objective

    @Test
    void aDiversionaryRaidMustReachFiftyEscalationPerPointOfScale() {
        StratConEscalation.addDiversionaryRaidObjective(contract(ContractObjectiveType.DIVERSIONARY_RAID),
              campaignState);

        StratConStrategicObjective objective = escalationObjective();
        assertEquals(50 * SCALE, objective.getDesiredObjectiveCount());
        assertEquals(50 * SCALE, StratConEscalation.getEscalationTarget(campaignState));
        assertFalse(objective.isObjectiveCompleted(campaignState.getTrack(0)));
        assertFalse(objective.isObjectiveFailed(campaignState.getTrack(0)), "short of the target is not a failure");
    }

    @Test
    void theObjectiveFollowsEscalationUntilItIsMet() {
        AbstractContract contract = contract(ContractObjectiveType.DIVERSIONARY_RAID);
        Campaign campaign = campaign(true);
        StratConEscalation.addDiversionaryRaidObjective(contract, campaignState);
        StratConStrategicObjective objective = escalationObjective();
        StratConTrackState track = campaignState.getTrack(0);

        StratConEscalation.increaseEscalation(campaign, contract, 30);
        assertEquals(30, objective.getCurrentObjectiveCount());
        assertFalse(objective.isObjectiveCompleted(track));

        StratConEscalation.increaseEscalation(campaign, contract, 150);
        assertEquals(50 * SCALE, objective.getCurrentObjectiveCount(), "capped at the target");
        assertTrue(objective.isObjectiveCompleted(track));
    }

    @Test
    void aContractWithoutAnEscalationObjectiveHasNoTarget() {
        assertNull(StratConEscalation.getEscalationTarget(campaignState));
    }
}
