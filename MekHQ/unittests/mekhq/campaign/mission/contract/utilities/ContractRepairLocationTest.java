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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;

/**
 * Tests {@link ContractRepairLocation}. Repair sites are ranked numerically ({@link Unit#SITE_IMPROVISED} 0 &lt;
 * {@link Unit#SITE_FIELD_WORKSHOP} 1 &lt; {@link Unit#SITE_FACILITY_BASIC} 2), so guerrilla work gets the worst site,
 * raids a field workshop, and everything else a basic facility - and the "best" across active contracts is the highest
 * of those.
 */
class ContractRepairLocationTest {

    private static AbstractContract contractWith(final ContractObjectiveType objectiveType) {
        AbstractContract contract = mock(AbstractContract.class);
        when(contract.getObjectiveType()).thenReturn(objectiveType);
        return contract;
    }

    @Test
    void guerrillaWorkGetsAnImprovisedSite() {
        assertEquals(Unit.SITE_IMPROVISED,
              ContractRepairLocation.getRepairLocation(ContractObjectiveType.GUERRILLA_WARFARE));
    }

    @Test
    void raidsGetAFieldWorkshop() {
        assertEquals(Unit.SITE_FIELD_WORKSHOP,
              ContractRepairLocation.getRepairLocation(ContractObjectiveType.OBJECTIVE_RAID));
    }

    @Test
    void otherContractsGetABasicFacility() {
        assertEquals(Unit.SITE_FACILITY_BASIC,
              ContractRepairLocation.getRepairLocation(ContractObjectiveType.GARRISON_DUTY));
    }

    @Test
    void withNoActiveContractsABasicFacilityIsAssumed() {
        assertEquals(Unit.SITE_FACILITY_BASIC, ContractRepairLocation.getBestRepairLocation(List.of()));
    }

    @Test
    void theBestSiteAcrossContractsIsTheHighestRanked() {
        int best = ContractRepairLocation.getBestRepairLocation(List.of(
              contractWith(ContractObjectiveType.GUERRILLA_WARFARE),   // SITE_IMPROVISED (0)
              contractWith(ContractObjectiveType.OBJECTIVE_RAID)));    // SITE_FIELD_WORKSHOP (1)

        assertEquals(Unit.SITE_FIELD_WORKSHOP, best, "a field workshop beats an improvised site");
    }

    @Test
    void aBasicFacilityContractWinsOverLesserSites() {
        int best = ContractRepairLocation.getBestRepairLocation(List.of(
              contractWith(ContractObjectiveType.GUERRILLA_WARFARE),   // SITE_IMPROVISED (0)
              contractWith(ContractObjectiveType.GARRISON_DUTY)));     // SITE_FACILITY_BASIC (2)

        assertEquals(Unit.SITE_FACILITY_BASIC, best);
    }

    @Test
    void onlyGuerrillaContractsLeaveNothingBetterThanImprovised() {
        int best = ContractRepairLocation.getBestRepairLocation(List.of(
              contractWith(ContractObjectiveType.GUERRILLA_WARFARE)));

        assertEquals(Unit.SITE_IMPROVISED, best);
    }
}
