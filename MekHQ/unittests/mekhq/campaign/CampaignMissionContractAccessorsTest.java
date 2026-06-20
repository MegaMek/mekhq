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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.mission.AbstractMissionTransition;
import mekhq.campaign.mission.AtBContract;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.mission.Mission;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Regression coverage for the {@link Campaign} mission/contract accessors after PR #9417 flattened the inheritance
 * chain.
 *
 * <p>These accessors are the highest-traffic consumers of the old {@code AtBContract -> Contract -> Mission}
 * relationship: they filter the single {@code missions} map down to "contracts", "AtB contracts", etc. by testing
 * {@code instanceof Mission} / {@code instanceof AtBContract}. Because {@link AtBContract} and {@link Contract} are no
 * longer {@link Mission} subclasses, a single mistaken {@code instanceof} would silently drop the wrong rows. This test
 * seeds one campaign with every mission flavour (plain mission, active/future/completed plain contracts and AtB
 * contracts) and pins exactly which collection each one lands in.</p>
 *
 * @author Claude (test author for PR #9417 review)
 */
class CampaignMissionContractAccessorsTest {
    /** Fixed "today" so the active/future/completed windows below are deterministic. */
    private static final LocalDate TODAY = LocalDate.of(3051, 6, 1);

    private Campaign campaign;

    private Mission plainMission;
    private Contract activeContract;
    private Contract futureContract;
    private AtBContract activeAtBContract;
    private AtBContract futureAtBContract;
    private AtBContract completedAtBContract;

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        RandomCallsignGenerator.getInstance(true);
        RandomCompanyNameGenerator.getInstance();
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @BeforeEach
    void setUp() {
        campaign = MHQTestUtilities.getTestCampaign();
        campaign.setLocalDate(TODAY);

        plainMission = new Mission("Plain Mission");
        plainMission.setStatus(MissionStatus.ACTIVE);

        activeContract = makeContract("Active Contract", MissionStatus.ACTIVE,
              TODAY.minusMonths(1), TODAY.plusMonths(5));
        futureContract = makeContract("Future Contract", MissionStatus.ACTIVE,
              TODAY.plusMonths(3), TODAY.plusMonths(9));

        activeAtBContract = makeAtBContract("Active AtB", MissionStatus.ACTIVE,
              TODAY.minusMonths(2), TODAY.plusMonths(4));
        futureAtBContract = makeAtBContract("Future AtB", MissionStatus.ACTIVE,
              TODAY.plusMonths(6), TODAY.plusMonths(12));
        completedAtBContract = makeAtBContract("Completed AtB", MissionStatus.SUCCESS,
              TODAY.minusMonths(18), TODAY.minusMonths(6));

        // Order is intentionally mixed to make sure ordering is not what makes a test pass.
        campaign.addMission(plainMission);
        campaign.addMission(activeAtBContract);
        campaign.addMission(futureContract);
        campaign.addMission(completedAtBContract);
        campaign.addMission(activeContract);
        campaign.addMission(futureAtBContract);
    }

    private static Contract makeContract(String name, MissionStatus status, LocalDate start, LocalDate end) {
        Contract contract = new Contract(name, "FS");
        contract.setStatus(status);
        contract.setStartDate(start);
        contract.setEndingDate(end);
        return contract;
    }

    private static AtBContract makeAtBContract(String name, MissionStatus status, LocalDate start, LocalDate end) {
        AtBContract contract = new AtBContract(name);
        contract.setStatus(status);
        contract.setStartDate(start);
        contract.setEndingDate(end);
        return contract;
    }

    @Test
    void getActiveContractsExcludesPlainMissionsAndIncludesActiveContractsOfBothKinds() {
        List<AbstractMissionTransition> activeContracts = campaign.getActiveContracts();

        assertEquals(2, activeContracts.size(), "Only the two active contracts should be returned");
        assertTrue(activeContracts.contains(activeContract), "the active plain contract must be included");
        assertTrue(activeContracts.contains(activeAtBContract), "the active AtB contract must be included");
        assertFalse(activeContracts.contains(plainMission), "a plain Mission must never be treated as a contract");
        assertFalse(activeContracts.contains(futureContract), "a future-start contract is not active today");
        assertFalse(activeContracts.contains(completedAtBContract), "a completed contract is not active");
    }

    @Test
    void getFutureContractsReturnsFutureStartContractsOfBothKindsOnly() {
        List<AbstractMissionTransition> futureContracts = campaign.getFutureContracts();

        assertEquals(2, futureContracts.size(), "Only the two future-start contracts should be returned");
        assertTrue(futureContracts.contains(futureContract), "the future plain contract must be included");
        assertTrue(futureContracts.contains(futureAtBContract), "the future AtB contract must be included");
        assertFalse(futureContracts.contains(plainMission), "a plain Mission must never be treated as a contract");
        assertFalse(futureContracts.contains(activeContract), "a currently-active contract is not a future contract");
    }

    @Test
    void getAtBContractsReturnsEveryAtBContractRegardlessOfStatus() {
        List<AbstractMissionTransition> atbContracts = campaign.getAtBContracts();

        assertEquals(3, atbContracts.size(), "All three AtB contracts must be returned regardless of status");
        assertTrue(atbContracts.contains(activeAtBContract));
        assertTrue(atbContracts.contains(futureAtBContract));
        assertTrue(atbContracts.contains(completedAtBContract));
        assertFalse(atbContracts.contains(activeContract), "a plain Contract is not an AtB contract");
        assertFalse(atbContracts.contains(plainMission), "a plain Mission is not an AtB contract");
    }

    @Test
    void getActiveAtBContractsReturnsOnlyActiveAtBContracts() {
        List<AbstractMissionTransition> activeAtB = campaign.getActiveAtBContracts();

        assertEquals(1, activeAtB.size(), "Only the active AtB contract should be returned");
        assertTrue(activeAtB.contains(activeAtBContract));
        assertFalse(activeAtB.contains(activeContract), "a plain Contract must not appear in the AtB-only accessor");
    }

    @Test
    void getCompletedAtBContractsReturnsOnlyCompletedAtBContracts() {
        List<AbstractMissionTransition> completedAtB = campaign.getCompletedAtBContracts();

        assertEquals(1, completedAtB.size(), "Only the completed AtB contract should be returned");
        assertTrue(completedAtB.contains(completedAtBContract));
    }

    @Test
    void getActiveMissionsIncludesActiveContractsAndActivePlainMissions() {
        List<AbstractMissionTransition> activeMissions = campaign.getActiveMissions(false);

        assertTrue(activeMissions.contains(plainMission),
              "a status-active plain Mission must be reported by getActiveMissions (it ignores dates)");
        assertTrue(activeMissions.contains(activeContract), "an active contract is an active mission");
        assertTrue(activeMissions.contains(activeAtBContract), "an active AtB contract is an active mission");
        assertFalse(activeMissions.contains(futureContract), "a future-start contract is not active today");
        assertFalse(activeMissions.contains(completedAtBContract), "a completed contract is not active");
        assertEquals(3, activeMissions.size(), "exactly the plain mission and the two active contracts are active");
    }

    @Test
    void getFutureAtBContractsReturnsOnlyFutureStartAtBContracts() {
        List<AbstractMissionTransition> futureAtB = campaign.getFutureAtBContracts();

        assertEquals(1, futureAtB.size(), "Only the future-start AtB contract should be returned");
        assertTrue(futureAtB.contains(futureAtBContract));
        assertFalse(futureAtB.contains(futureContract), "the plain future contract is not an AtB contract");
        assertTrue(campaign.hasFutureAtBContract(), "hasFutureAtBContract must report true when a future AtB exists");
    }

    @Test
    void getMissionReturnsTheStoredInstanceAndNullForUnknownIds() {
        assertEquals(activeAtBContract, campaign.getMission(activeAtBContract.getId()),
              "getMission must return the stored instance for a known id");
        assertEquals(plainMission, campaign.getMission(plainMission.getId()),
              "getMission must return the stored plain mission for its id");
        assertNull(campaign.getMission(999_999), "getMission must return null for an unknown id");
    }
}
