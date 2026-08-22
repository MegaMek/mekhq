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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import megamek.Version;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.scenarios.Scenario;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import testUtilities.MHQTestUtilities;

/**
 * Tests {@link ContractHistoryData}, the campaign's single store of every contract it has ever held.
 *
 * <p>The whole contract UI reads from these filters, so the boundaries matter: a contract counts from the day it
 * starts through the day it ends, an accepted contract that has not begun is "future" rather than "active", and an
 * un-accepted market offer - which has no status at all - must be skipped rather than throwing. The sort order is the
 * mission tab's own: running contracts oldest-first, then finished ones most-recent-first.</p>
 */
class ContractHistoryDataTest {
    private static final LocalDate TODAY = LocalDate.of(3051, 6, 1);

    /** Any version at or above the current release; keeps the version-gated compatibility branches dormant. */
    private static final Version VERSION = new Version(999, 0, 0);

    private ContractHistoryData history;
    private Campaign campaign;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        // A full campaign save writes the skill-type table, so it has to exist before writeToXML is exercised.
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setUp() {
        history = new ContractHistoryData();
        campaign = MHQTestUtilities.getTestCampaign();
    }

    private AbstractContract add(MissionStatus status, LocalDate startDate, LocalDate endDate) {
        AbstractContract contract = new ChaosContract();
        contract.setContractId(UUID.randomUUID());
        contract.setStatus(status);
        contract.setScheduleData(new ContractScheduleData(startDate, endDate, 0));
        history.contractHistory().put(contract.getId(), contract);
        return contract;
    }

    // region lookup

    @Test
    void aStoredContractIsRetrievableByItsId() {
        AbstractContract contract = add(MissionStatus.ACTIVE, TODAY.minusMonths(1), TODAY.plusMonths(1));

        assertSame(contract, history.get(contract.getId()));
        assertEquals(1, history.size());
    }

    @Test
    void anUnknownIdYieldsNullRatherThanThrowing() {
        assertNull(history.get(UUID.randomUUID()));
    }

    @Test
    void aFreshHistoryIsEmpty() {
        assertTrue(history.isEmpty());
        assertEquals(0, history.size());
    }

    @Test
    void clearingRemovesEverything() {
        add(MissionStatus.ACTIVE, TODAY, TODAY.plusMonths(1));

        history.clear();

        assertTrue(history.isEmpty());
    }

    // endregion lookup

    // region running contracts

    @Test
    void aRunningContractCountsFromItsStartThroughItsEnd() {
        AbstractContract contract = add(MissionStatus.ACTIVE, TODAY, TODAY.plusMonths(3));

        assertEquals(List.of(contract), history.getActiveAndStarted(TODAY), "the day it starts counts");
        assertEquals(List.of(contract), history.getActiveAndStarted(TODAY.plusMonths(3)),
              "the day it ends counts too");
    }

    @Test
    void aContractThatHasNotBegunIsNotRunningYet() {
        add(MissionStatus.ACTIVE, TODAY.plusMonths(1), TODAY.plusMonths(4));

        assertTrue(history.getActiveAndStarted(TODAY).isEmpty());
    }

    @Test
    void aContractPastItsEndDateIsNoLongerRunning() {
        add(MissionStatus.ACTIVE, TODAY.minusMonths(6), TODAY.minusDays(1));

        assertTrue(history.getActiveAndStarted(TODAY).isEmpty());
    }

    @Test
    void aFinishedContractIsNeverRunningEvenWithinItsDates() {
        add(MissionStatus.SUCCESS, TODAY.minusMonths(1), TODAY.plusMonths(1));

        assertTrue(history.getActiveAndStarted(TODAY).isEmpty(),
              "status decides whether a contract is live; the dates only narrow it further");
    }

    // endregion running contracts

    // region future contracts

    @Test
    void anAcceptedContractStartingLaterIsAFutureContract() {
        AbstractContract contract = add(MissionStatus.ACTIVE, TODAY.plusMonths(1), TODAY.plusMonths(4));

        assertEquals(List.of(contract), history.getActiveAndNotYetStarted(TODAY));
    }

    @Test
    void aContractStartingTodayHasStartedAndIsNotAFutureContract() {
        add(MissionStatus.ACTIVE, TODAY, TODAY.plusMonths(3));

        assertTrue(history.getActiveAndNotYetStarted(TODAY).isEmpty());
    }

    @Test
    void aContractWithNoStartDateCountsAsAlreadyBegunRatherThanFuture() {
        add(MissionStatus.ACTIVE, null, null);

        assertTrue(history.getActiveAndNotYetStarted(TODAY).isEmpty(),
              "an absent start date means the contract is already under way");
    }

    // endregion future contracts

    // region active including not-yet-started

    @Test
    void theAcceptedListSpansBothRunningAndFutureContracts() {
        AbstractContract running = add(MissionStatus.ACTIVE, TODAY.minusMonths(1), TODAY.plusMonths(1));
        AbstractContract future = add(MissionStatus.ACTIVE, TODAY.plusMonths(2), TODAY.plusMonths(5));
        add(MissionStatus.SUCCESS, TODAY.minusMonths(9), TODAY.minusMonths(6));

        assertEquals(List.of(running, future), history.getActiveIncludingNotYetStarted());
    }

    // endregion active including not-yet-started

    // region completed contracts

    @Test
    void everyConcludedOutcomeCountsAsCompleted() {
        AbstractContract success = add(MissionStatus.SUCCESS, TODAY.minusMonths(9), TODAY.minusMonths(8));
        AbstractContract partial = add(MissionStatus.PARTIAL, TODAY.minusMonths(7), TODAY.minusMonths(6));
        AbstractContract failed = add(MissionStatus.FAILED, TODAY.minusMonths(5), TODAY.minusMonths(4));
        AbstractContract breach = add(MissionStatus.BREACH, TODAY.minusMonths(3), TODAY.minusMonths(2));
        add(MissionStatus.ACTIVE, TODAY.minusMonths(1), TODAY.plusMonths(1));

        assertEquals(List.of(success, partial, failed, breach), history.getCompleted());
    }

    // endregion completed contracts

    // region un-accepted offers

    /**
     * A contract only has a status once it has been accepted. An offer that somehow reaches this collection must be
     * skipped by every filter rather than throwing on its missing status.
     */
    @Test
    void anUnAcceptedOfferWithNoStatusIsSkippedByEveryFilter() {
        AbstractContract offer = new ChaosContract();
        offer.setContractId(UUID.randomUUID());
        offer.setScheduleData(new ContractScheduleData(TODAY, TODAY.plusMonths(3), 3));
        history.contractHistory().put(offer.getId(), offer);

        assertTrue(history.getActiveAndStarted(TODAY).isEmpty());
        assertTrue(history.getActiveAndNotYetStarted(TODAY).isEmpty());
        assertTrue(history.getActiveIncludingNotYetStarted().isEmpty());
        assertTrue(history.getCompleted().isEmpty());
    }

    // endregion un-accepted offers

    // region sorting

    @Test
    void runningContractsSortOldestFirstAheadOfFinishedOnes() {
        AbstractContract finished = add(MissionStatus.SUCCESS, TODAY.minusYears(2), TODAY.minusYears(2).plusMonths(3));
        AbstractContract newerRunning = add(MissionStatus.ACTIVE, TODAY.minusMonths(1), TODAY.plusMonths(1));
        AbstractContract olderRunning = add(MissionStatus.ACTIVE, TODAY.minusMonths(6), TODAY.plusMonths(1));

        assertEquals(List.of(olderRunning, newerRunning, finished), history.getSortedMissions(TODAY));
    }

    @Test
    void finishedContractsSortMostRecentFirst() {
        AbstractContract older = add(MissionStatus.SUCCESS, TODAY.minusYears(3), TODAY.minusYears(3).plusMonths(2));
        AbstractContract newer = add(MissionStatus.FAILED, TODAY.minusYears(1), TODAY.minusYears(1).plusMonths(2));

        assertEquals(List.of(newer, older), history.getSortedMissions(TODAY));
    }

    @Test
    void aRunningContractWithNoStartDateSortsAsIfItStartedToday() {
        AbstractContract undated = add(MissionStatus.ACTIVE, null, null);
        AbstractContract older = add(MissionStatus.ACTIVE, TODAY.minusMonths(6), TODAY.plusMonths(1));
        AbstractContract newer = add(MissionStatus.ACTIVE, TODAY.plusMonths(1), TODAY.plusMonths(4));

        assertEquals(List.of(older, undated, newer), history.getSortedMissions(TODAY));
    }

    @Test
    void aFinishedContractWithNoStartDateSortsLast() {
        AbstractContract undated = add(MissionStatus.SUCCESS, null, null);
        AbstractContract dated = add(MissionStatus.SUCCESS, TODAY.minusYears(3), TODAY.minusYears(3).plusMonths(2));

        assertEquals(List.of(dated, undated), history.getSortedMissions(TODAY));
    }

    // endregion sorting

    // region save and load

    /**
     * Accepts a contract into {@code target}'s history the way {@code ContractAcceptance} does - status first, then
     * {@link Campaign#addMission(AbstractContract)}.
     */
    private static AbstractContract acceptInto(Campaign target, String name) {
        AbstractContract contract = new ChaosContract();
        contract.setContractId(UUID.randomUUID());
        contract.setContractName(name);
        contract.setStatus(MissionStatus.ACTIVE);
        contract.setScheduleData(new ContractScheduleData(TODAY, TODAY.plusMonths(6), 6));
        target.addMission(contract);
        return contract;
    }

    private static String write(Campaign source) {
        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            source.getContractHistoryData().writeToXML(printWriter, 0, source);
        }
        return stringWriter.toString();
    }

    private static void load(Campaign target, String contractsXml) throws Exception {
        try (InputStream inputStream = new ByteArrayInputStream(contractsXml.getBytes(StandardCharsets.UTF_8))) {
            Document document = MHQXMLUtility.newSafeDocumentBuilder().parse(inputStream);
            assertNotNull(document.getDocumentElement());
            ContractHistoryData.loadFromXML(document.getDocumentElement(), target, VERSION);
        }
    }

    /**
     * The regression behind issue #9804. Contract history used to be written from a second, always-empty map on
     * {@code PlayerForce}, so nothing reached the save and the Briefing Room came back empty after every reload.
     */
    @Test
    void anAcceptedContractSurvivesASaveAndReload() throws Exception {
        AbstractContract accepted = acceptInto(campaign, "Reach Garrison");

        Campaign reloaded = MHQTestUtilities.getTestCampaign();
        load(reloaded, write(campaign));

        AbstractContract restored = reloaded.getContract(accepted.getId());
        assertNotNull(restored, "a contract in the campaign's history must still be there after a save and reload");
        assertEquals("Reach Garrison", restored.getName());
        assertEquals(1, reloaded.getContractHistoryData().size());
    }

    /** The history is what the save is written from, so a campaign holding contracts must emit a block. */
    @Test
    void aCampaignHoldingContractsActuallyWritesThem() {
        acceptInto(campaign, "Reach Garrison");

        String xml = write(campaign);

        assertTrue(xml.contains('<' + ContractHistoryData.CONTRACTS_TAG + '>'),
              "a campaign with contracts must emit a <contracts> block");
        assertTrue(xml.contains("Reach Garrison"));
    }

    /**
     * Guards the wiring rather than the codec. Issue #9804 was not a broken serializer - the serializer worked, but
     * {@code Campaign.writeToXML} handed it a second, always-empty map on {@code PlayerForce}, so the block never
     * reached the save. Pointing the save back at anything other than the campaign's own history fails here.
     */
    @Test
    void aFullCampaignSaveCarriesTheContractHistory() {
        acceptInto(campaign, "Reach Garrison");

        StringWriter stringWriter = new StringWriter();
        try (PrintWriter printWriter = new PrintWriter(stringWriter)) {
            campaign.writeToXML(printWriter, false);
        }
        String saved = stringWriter.toString();

        assertTrue(saved.contains('<' + ContractHistoryData.CONTRACTS_TAG + '>'),
              "a full campaign save must contain the <contracts> block");
        assertTrue(saved.contains("Reach Garrison"), "the accepted contract must appear in the campaign save");
    }

    @Test
    void anEmptyHistoryWritesNothingAtAll() {
        assertTrue(write(campaign).isEmpty(), "a campaign with no contracts must not emit an empty block");
    }

    @Test
    void aLoadFullyReplacesTheCurrentHistory() throws Exception {
        acceptInto(campaign, "Saved");
        String saved = write(campaign);

        Campaign target = MHQTestUtilities.getTestCampaign();
        AbstractContract stale = acceptInto(target, "Stale");
        load(target, saved);

        assertNull(target.getContract(stale.getId()), "a load must not leave the previous history behind");
        assertEquals(1, target.getContractHistoryData().size());
    }

    /**
     * Loading goes through {@link Campaign#importMission(AbstractContract)} rather than a bare map put, so a restored
     * contract's scenarios are registered with the campaign. A plain map put would bring the contract back with its
     * scenarios unreachable through {@link Campaign#getScenario(int)}.
     */
    @Test
    void aRestoredContractsScenariosAreRegisteredWithTheCampaign() throws Exception {
        AbstractContract accepted = acceptInto(campaign, "Reach Garrison");
        Scenario scenario = new Scenario("Ambush at Rasalhague");
        scenario.setId(4242);
        accepted.addScenario(scenario);

        Campaign reloaded = MHQTestUtilities.getTestCampaign();
        load(reloaded, write(campaign));

        assertNotNull(reloaded.getScenario(4242),
              "a restored contract's scenarios must be reachable through the campaign");
    }

    /**
     * {@code StratConCampaignState.contract} is an {@code @XmlTransient} back-pointer, so it is null on the way back
     * in and has to be restored. Deploying a force to a StratCon scenario reads it, so a null there crashes scenario
     * generation with no contract to draw an enemy faction from.
     */
    @Test
    void restoredStratConStatePointsBackAtItsOwnContract() throws Exception {
        AbstractContract accepted = acceptInto(campaign, "Reach Garrison");
        accepted.setStratConCampaignState(new StratConCampaignState(accepted));

        Campaign reloaded = MHQTestUtilities.getTestCampaign();
        load(reloaded, write(campaign));

        AbstractContract restored = reloaded.getContract(accepted.getId());
        assertNotNull(restored);
        StratConCampaignState restoredState = restored.getStratConCampaignState();
        assertNotNull(restoredState, "the StratCon campaign state must survive the save");
        assertSame(restored, restoredState.getContract(),
              "a restored StratCon state must point back at its own contract, not null");
    }

    /**
     * A contract in the history was accepted, so it has a status. Saves written before acceptance set one carry none;
     * those load as active rather than dropping out of every status filter.
     */
    @Test
    void aContractSavedWithoutAStatusLoadsAsActive() throws Exception {
        AbstractContract accepted = acceptInto(campaign, "Reach Garrison");
        accepted.setStatus(null);

        Campaign reloaded = MHQTestUtilities.getTestCampaign();
        load(reloaded, write(campaign));

        AbstractContract restored = reloaded.getContract(accepted.getId());
        assertNotNull(restored);
        assertEquals(MissionStatus.ACTIVE, restored.getStatus());
    }

    // endregion save and load
}
