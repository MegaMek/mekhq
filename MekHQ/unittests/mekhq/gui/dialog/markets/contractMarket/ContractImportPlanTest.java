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
package mekhq.gui.dialog.markets.contractMarket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.ChaosContract;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationDifficulty;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.io.ContractExchange.ImportResult;
import mekhq.campaign.mission.contract.io.ContractExchange.ImportedOffer;
import mekhq.campaign.mission.contract.io.ContractExchange.SkipReason;
import mekhq.campaign.mission.contract.io.ContractExchange.SkippedOffer;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * Tests for {@link ContractImportPlan}, which decides where imported offers go on the market. An offer belongs on the
 * board it was listed on, but only where the importing campaign may use that board; and an offer the market cannot
 * rate must be turned away before anything is added, so a bad offer never leaves the market half-imported.
 */
class ContractImportPlanTest {
    private static final List<ContractSearchType> MERCENARY_BOARDS = List.of(ContractSearchType.MERCENARY,
          ContractSearchType.TOURNAMENT);

    private final Campaign campaign = mock(Campaign.class);

    private static AbstractContract offer(String name) {
        AbstractContract contract = new ChaosContract();
        contract.setContractName(name);
        return contract;
    }

    private static ImportResult imported(AbstractContract contract, ContractSearchType board) {
        return new ImportResult(List.of(new ImportedOffer(contract, board)), List.of());
    }

    /** Plans the import with every offer rated at the given difficulty. */
    private ContractImportPlan plan(ImportResult result, int difficulty) {
        try (MockedStatic<ChaosContractDeterminationDifficulty> rating = mockStatic(
              ChaosContractDeterminationDifficulty.class)) {
            rating.when(() -> ChaosContractDeterminationDifficulty.calculateContractDifficulty(any(), any()))
                  .thenReturn(difficulty);
            return ContractImportPlan.of(campaign, result, MERCENARY_BOARDS, ContractSearchType.MERCENARY);
        }
    }

    @Test
    void anOfferGoesOnTheBoardItWasListedOn() {
        AbstractContract contract = offer("Operation Sundowner");

        ContractImportPlan plan = plan(imported(contract, ContractSearchType.TOURNAMENT), 6);

        ContractImportPlan.Placement placement = plan.placements().getFirst();
        assertSame(contract, placement.contract());
        assertEquals(ContractSearchType.TOURNAMENT, placement.board());
        assertFalse(placement.wasMoved());
    }

    @Test
    void anOfferOnABoardTheCampaignCannotUseGoesOnTheBoardOnScreenAndIsReported() {
        ContractImportPlan plan = plan(imported(offer("Raid"), ContractSearchType.PIRATE), 6);

        ContractImportPlan.Placement placement = plan.placements().getFirst();
        assertEquals(ContractSearchType.MERCENARY, placement.board());
        assertEquals(ContractSearchType.PIRATE, placement.listedBoard());
        assertTrue(placement.wasMoved(), "the player is told the offer was moved");
    }

    @Test
    void anOfferWithNoBoardGoesOnTheBoardOnScreenWithoutBeingReported() {
        ContractImportPlan plan = plan(imported(offer("Raid"), null), 6);

        ContractImportPlan.Placement placement = plan.placements().getFirst();
        assertEquals(ContractSearchType.MERCENARY, placement.board());
        assertFalse(placement.wasMoved(), "there was no board to move it from");
    }

    @Test
    void eachOfferIsRated() {
        AbstractContract contract = offer("Operation Sundowner");

        plan(imported(contract, ContractSearchType.MERCENARY), 8);

        assertEquals(8, contract.getCachedContractDifficulty());
    }

    @Test
    void anOfferTheMarketCannotRateIsSkippedRatherThanAdded() {
        // No enemy data, as from a hand-edited file: rating the offer fails on the enemy's faction.
        AbstractContract unrated = offer("Hand Edited");

        ContractImportPlan plan = ContractImportPlan.of(campaign, imported(unrated, ContractSearchType.MERCENARY),
              MERCENARY_BOARDS, ContractSearchType.MERCENARY);

        assertTrue(plan.placements().isEmpty());
        assertEquals(List.of(new SkippedOffer("Hand Edited", SkipReason.UNREADABLE)), plan.skipped());
    }

    @Test
    void offersTheFileReaderSkippedAreKept() {
        SkippedOffer unknownSystem = new SkippedOffer("Far Away", SkipReason.UNKNOWN_TARGET_SYSTEM);
        ImportResult result = new ImportResult(List.of(), List.of(unknownSystem));

        ContractImportPlan plan = plan(result, 6);

        assertEquals(List.of(unknownSystem), plan.skipped());
    }
}
