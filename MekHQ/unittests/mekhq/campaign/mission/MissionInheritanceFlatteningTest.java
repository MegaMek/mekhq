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
package mekhq.campaign.mission;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.missions.MissionNewEvent;
import mekhq.campaign.mission.enums.MissionStatus;
import mekhq.campaign.personnel.backgrounds.RandomCompanyNameGenerator;
import mekhq.campaign.universe.Factions;
import mekhq.campaign.universe.Systems;
import mekhq.campaign.universe.TestSystems;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MissionInheritanceFlatteningTest {

    @BeforeAll
    static void initSingletons() {
        EquipmentType.initializeTypes();
        RandomCallsignGenerator.getInstance(true); // Ignore IntelliJ telling you this is ignored
        RandomCompanyNameGenerator.getInstance();
        try {
            Factions.setInstance(Factions.loadDefault(true));
            Systems.setInstance(TestSystems.loadDefault());
        } catch (Exception ex) {
            LogManager.getLogger().error("", ex);
        }
    }

    @Test
    void allConcreteTypesAreAbstractMissionTransition() {
        assertInstanceOf(AbstractMissionTransition.class, new Mission("plain"),
              "Mission must extend AbstractMissionTransition");
        assertInstanceOf(AbstractMissionTransition.class, new Contract("contract", "FS"),
              "Contract must extend AbstractMissionTransition");
        assertInstanceOf(AbstractMissionTransition.class, new AtBContract("atb"),
              "AtBContract must extend AbstractMissionTransition");
    }

    /**
     * {@code MissionEvent.isContract()} is implemented as {@code !(mission instanceof Mission)}. Post-flattening this
     * must be {@code true} for both {@link Contract} and {@link AtBContract} and {@code false} for a plain
     * {@link Mission}. Finance and award code keys off this, so the semantics are pinned here.
     */
    @Test
    void missionEventIsContractTrueForContractTypes() {
        assertTrue(new MissionNewEvent(new Contract("contract", "FS")).isContract(),
              "A Contract-backed event must report isContract() == true");
        assertTrue(new MissionNewEvent(new AtBContract("atb")).isContract(),
              "An AtBContract-backed event must report isContract() == true");
    }

    @Test
    void missionEventIsContractFalseForPlainMission() {
        assertFalse(new MissionNewEvent(new Mission("plain")).isContract(),
              "A plain-Mission-backed event must report isContract() == false");
    }

    /**
     * A plain {@link Mission} overrides {@code isActiveOn} to ignore the calendar entirely and report activity purely
     * from its {@link MissionStatus}. A {@link Contract} (and {@link AtBContract}) inherit the date-aware base
     * implementation. This contrast is exactly the kind of behavior the flattening could have silently broken, so both
     * branches are exercised against the same dates.
     */
    @Test
    void missionIsActiveOnIgnoresDatesWhileContractRespectsThem() {
        LocalDate start = LocalDate.of(3051, 1, 1);
        LocalDate end = LocalDate.of(3051, 12, 31);
        LocalDate beforeStart = LocalDate.of(3050, 6, 1);

        Mission mission = new Mission("plain");
        mission.setStatus(MissionStatus.ACTIVE);
        mission.setStartDate(start);
        mission.setEndingDate(end);
        assertTrue(mission.isActiveOn(beforeStart),
              "Mission.isActiveOn must ignore the date and report active purely from status");

        Contract contract = new Contract("contract", "FS");
        contract.setStatus(MissionStatus.ACTIVE);
        contract.setStartDate(start);
        contract.setEndingDate(end);
        assertFalse(contract.isActiveOn(beforeStart),
              "Contract.isActiveOn must respect the start date and report inactive before it");
        assertTrue(contract.isActiveOn(LocalDate.of(3051, 6, 1)),
              "Contract.isActiveOn must report active for a date inside the window");
    }

    @Test
    void missionIsActiveOnReflectsCompletedStatusRegardlessOfDate() {
        Mission mission = new Mission("plain");
        mission.setStatus(MissionStatus.SUCCESS);
        mission.setStartDate(LocalDate.of(3051, 1, 1));
        mission.setEndingDate(LocalDate.of(3051, 12, 31));
        assertFalse(mission.isActiveOn(LocalDate.of(3051, 6, 1)),
              "A completed Mission must report inactive even within its date window");
    }

    /**
     * {@link Mission} overrides {@code calculateContract} as a no-op so that plain missions never accrue contract
     * money. The base implementation immediately reaches into {@code campaign.getAccountant()}; verifying the
     * accountant is never touched (and the amounts stay zero) proves the override is in force and that a plain mission
     * cannot NPE its way through a contract calculation.
     */
    @Test
    void missionCalculateContractIsNoOp() {
        Campaign campaign = mock(Campaign.class);
        Mission mission = new Mission("plain");

        mission.calculateContract(campaign);

        verify(campaign, never()).getAccountant();
        assertTrue(mission.getBaseAmount().isZero(), "Mission.calculateContract must not assign a base amount");
        assertTrue(mission.getTotalAmount().isZero(),
              "Mission.calculateContract must leave the total amount at zero");
    }
}
