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
package mekhq.campaign.mission.contract.contractGeneration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractData.ChaosContractStepsTable;
import mekhq.campaign.mission.contract.contractData.ContractNature;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.mission.contract.contractData.ContractTermsData;
import mekhq.campaign.mission.contract.contractData.EmployerData;
import mekhq.campaign.mission.contract.contractData.NonNegotiableTermsData;
import org.junit.jupiter.api.Test;

/**
 * Tests the acts-of-piracy generator's search-type-specific handlers: a fixed pirate-raid objective, covert status only
 * when the raid is secretly bankrolled, and the dictated, locked terms every pirate raid carries.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ActsOfPiracyContractGenerationTest {

    private final ActsOfPiracyContractGeneration generator = new ActsOfPiracyContractGeneration();

    /** A rolled baseline of mid-table steps, so the pirate overrides are visibly distinct from what was rolled. */
    private static ContractTermsData baselineTerms() {
        return new ContractTermsData(ChaosContractStepsTable.STEP_FIVE, ChaosContractStepsTable.STEP_FIVE,
              ChaosContractStepsTable.STEP_FIVE, ChaosContractStepsTable.STEP_FIVE, ChaosContractStepsTable.STEP_FIVE);
    }

    /** An underworld-contact employer, optionally fronting a hidden real-power sponsor. */
    private static EmployerData employer(final String sponsorFactionCode) {
        return new EmployerData(ChaosEmployerType.UNDERWORLD_CONTACT, "PIR", "PIR", sponsorFactionCode, "Contact",
              null, null, null);
    }

    // --- search type ---

    @Test
    void searchTypeIsPirate() {
        assertSame(ContractSearchType.PIRATE, generator.getSearchType());
    }

    // --- objective ---

    @Test
    void pickObjectiveIsAlwaysAPirateRaid() {
        ChaosContract contract = new ChaosContract();

        generator.pickObjective(0, contract);

        assertSame(ContractObjectiveType.PIRATE_RAID, contract.getObjectiveType());
    }

    // --- covert status ---

    @Test
    void determineCovertStatusLeavesAnUnsponsoredRaidOvert() {
        ChaosContract contract = new ChaosContract();
        contract.setEmployerData(employer(null));

        generator.determineCovertStatus(ChaosObjectiveType.PIRATE_RAID, contract);

        assertSame(ContractNature.NORMAL, contract.getNature());
        assertFalse(contract.isCovertOperation(), "an opportunistic pirate raid is overt");
    }

    @Test
    void determineCovertStatusMakesASponsoredRaidCovert() {
        ChaosContract contract = new ChaosContract();
        contract.setEmployerData(employer("LA"));

        generator.determineCovertStatus(ChaosObjectiveType.PIRATE_RAID, contract);

        assertSame(ContractNature.COVERT, contract.getNature());
    }

    @Test
    void determineCovertStatusNeverOverridesAProvingGround() {
        ChaosContract contract = new ChaosContract();
        contract.setNature(ContractNature.PROVING_GROUND);
        contract.setEmployerData(employer("LA"));

        generator.determineCovertStatus(ChaosObjectiveType.PIRATE_RAID, contract);

        assertSame(ContractNature.PROVING_GROUND, contract.getNature());
    }

    // --- dictated terms ---

    @Test
    void unsponsoredRaidFixesAndLocksCommandSalvageSupportAndTransport() {
        ChaosContract contract = new ChaosContract();
        contract.setContractTerms(baselineTerms());

        generator.applyTypeSpecificTerms(contract);

        ContractTermsData terms = contract.getContractTerms();
        assertSame(ChaosContractStepsTable.STEP_SEVENTEEN, terms.commandRights(), "command maxed");
        assertSame(ChaosContractStepsTable.STEP_SEVENTEEN, terms.salvageRights(), "salvage maxed");
        assertSame(ChaosContractStepsTable.STEP_ONE, terms.support(), "no support");
        assertSame(ChaosContractStepsTable.STEP_ONE, terms.transport(), "no transport");

        NonNegotiableTermsData locked = contract.getNonNegotiableTermsData();
        assertTrue(locked.commandLocked());
        assertTrue(locked.salvageLocked());
        assertTrue(locked.supportLocked());
        assertTrue(locked.transportLocked());
        assertFalse(locked.payLocked(), "pay stays negotiable");
    }

    @Test
    void sponsoredRaidFixesOnlyCommandAndSalvageLeavingSupportAndTransportToTheSponsor() {
        ChaosContract contract = new ChaosContract();
        contract.setNature(ContractNature.COVERT);
        contract.setContractTerms(baselineTerms());

        generator.applyTypeSpecificTerms(contract);

        ContractTermsData terms = contract.getContractTerms();
        assertSame(ChaosContractStepsTable.STEP_SEVENTEEN, terms.commandRights(), "command maxed");
        assertSame(ChaosContractStepsTable.STEP_SEVENTEEN, terms.salvageRights(), "salvage maxed");
        assertSame(ChaosContractStepsTable.STEP_FIVE, terms.support(), "support left as rolled");
        assertSame(ChaosContractStepsTable.STEP_FIVE, terms.transport(), "transport left as rolled");

        NonNegotiableTermsData locked = contract.getNonNegotiableTermsData();
        assertTrue(locked.commandLocked());
        assertTrue(locked.salvageLocked());
        assertFalse(locked.supportLocked(), "support stays negotiable for a sponsored raid");
        assertFalse(locked.transportLocked(), "transport stays negotiable for a sponsored raid");
    }

    @Test
    void dictatedLocksMergeOntoExistingNonNegotiableLocks() {
        ChaosContract contract = new ChaosContract();
        contract.setContractTerms(baselineTerms());
        // A prior random lock on pay must survive the pirate overrides.
        contract.setNonNegotiableTermsData(new NonNegotiableTermsData(true, false, false, false, false));

        generator.applyTypeSpecificTerms(contract);

        NonNegotiableTermsData locked = contract.getNonNegotiableTermsData();
        assertTrue(locked.payLocked(), "an existing pay lock is preserved");
        assertTrue(locked.commandLocked());
        assertTrue(locked.salvageLocked());
    }
}
