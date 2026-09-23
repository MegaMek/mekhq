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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
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
import mekhq.campaign.mission.resupplyAndCaches.PerformResupply;
import mekhq.campaign.mission.resupplyAndCaches.Resupply;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import testUtilities.MHQTestUtilities;

/**
 * Tests for the plunder target, which shares the ambush rules of vulnerable infrastructure (see
 * {@link StratConVulnerableInfrastructureBehaviorTest} for those in depth): these cover what differs - a plundered
 * target's loot arriving as a size 1 Resupply, the Escalation it raises, an ambushed target yielding nothing, and its
 * text and visibility.
 *
 * <p>The Resupply is intercepted: building and delivering one does real work and shows dialogs.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
class StratConPlunderTargetBehaviorTest {
    private static final String TYPE_ID = "UnitTestPlunderTarget";
    private static final LocalDate TODAY = LocalDate.of(3025, 1, 15);
    private static final int FORMATION_ID = 7;
    private static final int SCENARIO_ID = 42;
    private static final StratConCoords TARGET_COORDS = new StratConCoords(1, 1);
    private static final Money COMBAT_PAY = Money.of(25000);

    private StratConTrackState track;
    private StratConPointOfInterest target;
    private StratConStrategicObjective objective;
    // the contract, and its StratCon state, in the campaign built by deploymentCampaign
    private AbstractContract contract;
    private StratConCampaignState campaignState;

    @BeforeEach
    void setUp() {
        StratConPointOfInterestDefinition definition = new StratConPointOfInterestDefinition();
        definition.setTypeId(TYPE_ID);
        definition.setBehaviorId(StratConPlunderTargetBehavior.BEHAVIOR_ID);
        definition.setOccupiesHex(true);
        definition.setHiddenUntilScouted(false);
        definition.setLifespanDieSides(6);
        definition.setRemoveOnExpiry(true);
        StratConPointOfInterestDefinitions.registerDefinition(definition);

        track = new StratConTrackState();
        track.setWidth(5);
        track.setHeight(5);

        target = new StratConPointOfInterest(TYPE_ID, TARGET_COORDS);
        assertTrue(track.addPointOfInterest(target), "test setup: the target should be placed");
        objective = StratConPointOfInterestPlacer.addStrategicObjective(track, target);
    }

    @AfterEach
    void tearDown() {
        StratConPointOfInterestDefinitions.unregisterDefinition(TYPE_ID);
    }

    /**
     * A campaign with special mechanics on, holding one active Pirate Raid - with combat pay - whose map holds the test
     * sector, and one player formation of the given primary unit type. With Essential Scenarios Only on, no scenario
     * can break out, so there is never an ambush.
     */
    private Campaign deploymentCampaign(int primaryUnitType) {
        Campaign campaign = MHQTestUtilities.mockCampaign();
        when(campaign.getLocalDate()).thenReturn(TODAY);

        CampaignOptions options = mock(CampaignOptions.class);
        when(options.get(CampaignOption.ESSENTIAL_SCENARIOS_ONLY)).thenReturn(true);
        when(options.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)).thenReturn(true);
        when(campaign.getCampaignOptions()).thenReturn(options);

        Formation formation = mock(Formation.class);
        when(formation.getPrimaryUnitType(campaign)).thenReturn(primaryUnitType);
        when(campaign.getPlayerForce().getFormation(FORMATION_ID)).thenReturn(formation);

        contract = mock(AbstractContract.class);
        campaignState = new StratConCampaignState();
        campaignState.addTrack(track);
        when(contract.getStratConCampaignState()).thenReturn(campaignState);
        when(contract.getObjectiveType()).thenReturn(ContractObjectiveType.PIRATE_RAID);
        when(contract.getScale()).thenReturn(1);
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
    void thePlunderTargetBehaviorIsRegisteredUnderItsId() {
        assertInstanceOf(StratConPlunderTargetBehavior.class,
              StratConPointOfInterestBehaviors.getBehavior(StratConPlunderTargetBehavior.BEHAVIOR_ID));
        assertInstanceOf(StratConPlunderTargetBehavior.class, target.getBehavior());
    }

    @Test
    void raidersAreAmbushedInATemplateSuitedToThem() {
        assertNull(new StratConPlunderTargetBehavior().getScenarioTemplateName(),
              "no fixed template: the ambush is drawn for the raiders' unit type");
    }

    @Test
    void theObjectiveHasItsOwnText() {
        String description = target.getBehavior().getObjectiveDescription(target, track);

        assertNotNull(description);
        assertTrue(isResourceKeyValid(description), "missing resource key: " + description);
    }

    @Test
    void aPlunderTargetIsVisibleWithoutBeingScouted() {
        assertTrue(target.isVisibleToPlayer(track));
    }

    // Plundering

    @ParameterizedTest
    @ValueSource(ints = { MEK, AEROSPACE_FIGHTER })
    void anyFormationPlundersTheTargetForLootTheBonusAndEscalation(int primaryUnitType) {
        Campaign campaign = deploymentCampaign(primaryUnitType);
        Finances finances = campaign.getPlayerForce().getFinances();

        try (MockedConstruction<Resupply> resupplies = mockConstruction(Resupply.class);
              MockedStatic<PerformResupply> performResupply = mockStatic(PerformResupply.class)) {
            assertEquals(PointOfInterestDeploymentOutcome.SUPPRESS_SCENARIO,
                  StratConPointOfInterestRules.processFormationDeployment(track, TARGET_COORDS, FORMATION_ID,
                        campaign));

            assertEquals(1, resupplies.constructed().size(), "one lot of loot");
            Resupply loot = resupplies.constructed().get(0);
            performResupply.verify(() -> PerformResupply.performResupply(loot, contract, 1));
        }

        assertNull(track.getPointOfInterest(target.getId()));
        assertEquals(PointOfInterestStatus.RESOLVED, target.getStatus());
        assertTrue(objective.isObjectiveCompleted(track));
        verify(finances).credit(eq(TransactionType.CONTRACT_PAYMENT), eq(TODAY), eq(COMBAT_PAY), anyString());
        int escalation = campaignState.getEscalation();
        assertTrue((escalation >= 3) && (escalation <= 18), "3d6 Escalation, got " + escalation);
        assertGeneralReportIsValid(campaign);
    }

    // An ambushed target is spent, with no loot

    @Test
    void anAmbushedTargetIsSpentWithNoLootWhateverTheResult() {
        target.setLinkedScenarioId(SCENARIO_ID);
        Campaign campaign = deploymentCampaign(MEK);
        Finances finances = campaign.getPlayerForce().getFinances();

        try (MockedStatic<PerformResupply> performResupply = mockStatic(PerformResupply.class)) {
            StratConPointOfInterestRules.processScenarioEnded(track, SCENARIO_ID, true, campaign);

            performResupply.verify(() -> PerformResupply.performResupply(any(), any(), anyInt()), never());
        }

        assertNull(track.getPointOfInterest(target.getId()));
        assertFalse(track.getStrategicObjectives().contains(objective), "its objective is removed, not failed");
        verify(finances, never()).credit(any(), any(), any(), anyString());
        assertEquals(0, campaignState.getEscalation(), "the target itself adds nothing");
    }

    @Test
    void aPlunderTargetNotRaidedInTimeIsGone() {
        target.setExpiryDate(TODAY);
        Campaign campaign = mock(Campaign.class);
        when(campaign.getLocalDate()).thenReturn(TODAY);

        StratConPointOfInterestRules.processNewDay(track, campaign);

        assertEquals(PointOfInterestStatus.EXPIRED, target.getStatus());
        assertTrue(objective.isObjectiveFailed(track));
    }
}
