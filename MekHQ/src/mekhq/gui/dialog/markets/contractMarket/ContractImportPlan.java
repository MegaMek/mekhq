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

import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractGeneration.ChaosContractDeterminationDifficulty;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.mission.contract.io.ContractExchange.ImportResult;
import mekhq.campaign.mission.contract.io.ContractExchange.ImportedOffer;
import mekhq.campaign.mission.contract.io.ContractExchange.SkipReason;
import mekhq.campaign.mission.contract.io.ContractExchange.SkippedOffer;

/**
 * Where each imported offer goes on the market, worked out before any offer is added so that one offer the market
 * cannot rate never leaves the board half-imported.
 *
 * <p>Each offer goes on the board it was listed on, if this campaign may use that board, and otherwise on the board on
 * screen. An offer whose difficulty cannot be worked out is skipped as unreadable.</p>
 *
 * @param placements the offers to add, in file order
 * @param skipped    the offers that could not be imported: those the file reader skipped, then those skipped here
 *
 * @author Illiani
 * @since 0.51.01
 */
record ContractImportPlan(List<Placement> placements, List<SkippedOffer> skipped) {
    private static final MMLogger LOGGER = MMLogger.create(ContractImportPlan.class);

    /**
     * One offer and the board it goes on.
     *
     * @param contract    the offer, with its difficulty worked out
     * @param board       the board it goes on
     * @param listedBoard the board the exporting campaign listed it on, or {@code null} if the file did not say
     */
    record Placement(AbstractContract contract, ContractSearchType board, @Nullable ContractSearchType listedBoard) {
        /** @return {@code true} if the offer goes on a different board from the one it was listed on */
        boolean wasMoved() {
            return (listedBoard != null) && (listedBoard != board);
        }
    }

    /**
     * Plans where the imported offers go.
     *
     * @param campaign      the campaign importing the offers
     * @param result        the offers read from the file
     * @param allowedBoards the boards this campaign may use
     * @param boardOnScreen the board on screen, for offers whose own board this campaign may not use
     *
     * @return the plan
     *
     * @author Illiani
     * @since 0.51.01
     */
    static ContractImportPlan of(Campaign campaign, ImportResult result, List<ContractSearchType> allowedBoards,
          ContractSearchType boardOnScreen) {
        List<Placement> placements = new ArrayList<>();
        List<SkippedOffer> skipped = new ArrayList<>(result.skipped());
        for (ImportedOffer offer : result.imported()) {
            AbstractContract contract = offer.contract();
            try {
                contract.setCachedContractDifficulty(
                      ChaosContractDeterminationDifficulty.calculateContractDifficulty(campaign, contract));
            } catch (Exception ex) {
                LOGGER.error(ex, "Unable to rate imported contract offer '{}'", contract.getName());
                skipped.add(new SkippedOffer(contract.getName(), SkipReason.UNREADABLE));
                continue;
            }
            ContractSearchType listedBoard = offer.searchType();
            // List.of rejects contains(null), so an offer with no board is checked before the lookup.
            ContractSearchType board = ((listedBoard != null) && allowedBoards.contains(listedBoard))
                                             ? listedBoard
                                             : boardOnScreen;
            placements.add(new Placement(contract, board, listedBoard));
        }
        return new ContractImportPlan(placements, skipped);
    }
}
