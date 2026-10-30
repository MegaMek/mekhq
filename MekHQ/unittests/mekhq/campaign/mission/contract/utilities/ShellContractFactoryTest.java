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
package mekhq.campaign.mission.contract.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.finances.Money;
import mekhq.campaign.mission.contract.ShellContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractCommandRights;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.MissionStatus;
import mekhq.campaign.mission.contract.utilities.ShellContractFactory.ShellContractDetails;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ShellContractFactoryTest {
    private static final LocalDate START = LocalDate.of(3051, 1, 1);
    private static final int LENGTH_IN_MONTHS = 4;

    private static ShellContractDetails details(String employerCode, String enemyCode, boolean openEnded) {
        return new ShellContractDetails("Shell", "A holder for manual scenarios", ContractObjectiveType.RECON_RAID,
              employerCode, employerCode + " Name", enemyCode, enemyCode + " Name", "Terra", "Terra", START,
              LENGTH_IN_MONTHS, openEnded, Money.of(25_000), ChaosContractStepsTable.STEP_FIVE,
              ContractCommandRights.LIAISON);
    }

    @Test
    void createsAnActiveContractFromTheDetails() {
        Person liaison = mock(Person.class);
        Person commander = mock(Person.class);

        ShellContract contract = ShellContractFactory.create(details("FS", "DC", false), liaison, commander);

        assertNotNull(contract.getId());
        assertEquals(MissionStatus.ACTIVE, contract.getStatus());
        assertNull(contract.getStratConCampaignState(), "a shell contract is never seeded with StratCon");
        assertEquals("Shell", contract.getName());
        assertEquals("A holder for manual scenarios", contract.getDescription());
        assertEquals(ContractObjectiveType.RECON_RAID, contract.getObjectiveType());
        assertEquals("FS", contract.getEmployerFactionCode());
        assertEquals("FS Name", contract.getEmployerDisplayName());
        assertEquals("DC", contract.getEnemyFactionCode());
        assertEquals("DC Name", contract.getEnemyDisplayName());
        assertSame(liaison, contract.getEmployerLiaison());
        assertSame(commander, contract.getEnemyData().opposingCommander());
        assertEquals("Terra", contract.getTargetSystemId());
        assertEquals("Terra", contract.getTargetPlanetId());
        assertEquals(Money.of(25_000), contract.getMonthlyPayOut());
        assertEquals(ChaosContractStepsTable.STEP_FIVE, contract.getSalvageRightsStep());
        assertEquals(ContractCommandRights.LIAISON, contract.getCommandRights());
    }

    @Test
    void defaultRightsAreFullSalvageAndIndependentCommand() {
        assertEquals(1.0, ShellContractFactory.DEFAULT_SALVAGE_RIGHTS_STEP.getSalvageMultiplier());
        assertFalse(ShellContractFactory.DEFAULT_SALVAGE_RIGHTS_STEP.isExchangeSalvage());
        assertTrue(ShellContractFactory.getSalvageRightsSteps()
                         .contains(ShellContractFactory.DEFAULT_SALVAGE_RIGHTS_STEP),
              "the default salvage rights must be one of the choices offered");
        assertEquals(ContractCommandRights.INDEPENDENT, ShellContractFactory.DEFAULT_COMMAND_RIGHTS);
    }

    @Test
    void schedulesTheContractFromItsStartAndLength() {
        ShellContract contract = ShellContractFactory.create(details("FS", "DC", false), null, null);

        assertEquals(START, contract.getStartDate());
        assertEquals(START.plusMonths(LENGTH_IN_MONTHS), contract.getEndingDate());
        assertEquals(LENGTH_IN_MONTHS, contract.getLengthInMonths());
    }

    @Test
    void openEndedContractHasNoEndDate() {
        ShellContract contract = ShellContractFactory.create(details("FS", "DC", true), null, null);

        assertEquals(START, contract.getStartDate());
        assertNull(contract.getEndingDate());
        assertTrue(contract.isActiveOn(START.plusYears(10)), "an open-ended contract runs until it is completed");
        assertEquals(0, contract.getMonthsLeft(START), "an open-ended contract has no escrow left to pay out");
    }

    @Test
    void createdContractRunsNoSupportOrTransport() {
        ShellContract contract = ShellContractFactory.create(details("FS", "DC", false), null, null);

        assertEquals(0.0, contract.getSupportStep().getStraightSupportMultiplier());
        assertEquals(0.0, contract.getSupportStep().getBattlefieldLossMultiplier());
        assertEquals(0.0, contract.getTransportStep().getTransportMultiplier());
        assertTrue(contract.getTransportPayment().isZero());
        assertTrue(contract.getContractFinanceData().combatPay().isZero());
    }

    @Test
    void updateKeepsNpcsForUnchangedFactions() {
        Person liaison = mock(Person.class);
        Person commander = mock(Person.class);
        ShellContract contract = ShellContractFactory.create(details("FS", "DC", false), liaison, commander);
        String originalId = contract.getId().toString();

        ShellContractFactory.update(contract, details("FS", "DC", true), mock(Person.class), mock(Person.class));

        assertEquals(originalId, contract.getId().toString(), "updating must not replace the contract's id");
        assertSame(liaison, contract.getEmployerLiaison());
        assertSame(commander, contract.getEnemyData().opposingCommander());
        assertNull(contract.getEndingDate(), "the updated schedule must be applied");
    }

    @Test
    void updateReplacesNpcsForChangedFactions() {
        ShellContract contract = ShellContractFactory.create(details("FS", "DC", false), mock(Person.class),
              mock(Person.class));
        Person newLiaison = mock(Person.class);
        Person newCommander = mock(Person.class);

        ShellContractFactory.update(contract, details("LA", "CC", false), newLiaison, newCommander);

        assertEquals("LA", contract.getEmployerFactionCode());
        assertEquals("CC", contract.getEnemyFactionCode());
        assertSame(newLiaison, contract.getEmployerLiaison());
        assertSame(newCommander, contract.getEnemyData().opposingCommander());
    }

    @Test
    void salvageRightsStepsOfferEachOutcomeOnce() {
        List<ChaosContractStepsTable> steps = ShellContractFactory.getSalvageRightsSteps();

        Set<String> outcomes = new HashSet<>();
        for (ChaosContractStepsTable step : steps) {
            assertTrue(outcomes.add(step.getSalvageMultiplier() + "/" + step.isExchangeSalvage()),
                  "salvage outcome offered twice: " + step);
        }
        assertEquals(0.0, steps.getFirst().getSalvageMultiplier(), "the choices must start with no salvage");
        assertFalse(steps.getFirst().isExchangeSalvage());
        assertEquals(1.0, steps.getLast().getSalvageMultiplier(), "the choices must end with full salvage");
    }

    @ParameterizedTest
    @EnumSource(ContractCommandRights.class)
    void commandRightsStepGrantsThoseRights(ContractCommandRights commandRights) {
        assertEquals(commandRights,
              ShellContractFactory.getCommandRightsStep(commandRights).getContractCommandRights());
    }
}
