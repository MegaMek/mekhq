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
package mekhq.campaign.personnel.autoAwards;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ShellContract;
import mekhq.campaign.mission.contract.contractData.ContractScheduleData;
import org.junit.jupiter.api.Test;

class AutoAwardsControllerTest {
    private static final LocalDate START = LocalDate.of(3051, 1, 1);

    private static AbstractContract contractEnding(LocalDate endDate) {
        AbstractContract contract = new ShellContract();
        contract.setScheduleData(new ContractScheduleData(START, endDate, 0));
        return contract;
    }

    @Test
    void lastContractEndingDateIsTheLatestEndDate() {
        List<AbstractContract> completedContracts = List.of(contractEnding(START.plusMonths(3)),
              contractEnding(START.plusMonths(9)), contractEnding(START.plusMonths(6)));

        assertEquals(START.plusMonths(9), AutoAwardsController.getLastContractEndingDate(completedContracts));
    }

    @Test
    void lastContractEndingDateSkipsContractsWithoutAnEndDate() {
        List<AbstractContract> completedContracts = List.of(contractEnding(START.plusMonths(3)),
              contractEnding(null));

        assertEquals(START.plusMonths(3), AutoAwardsController.getLastContractEndingDate(completedContracts),
              "an open-ended contract must not break the comparison");
    }

    @Test
    void lastContractEndingDateIsNullWhenNoContractHasAnEndDate() {
        assertNull(AutoAwardsController.getLastContractEndingDate(List.of(contractEnding(null))));
    }
}
