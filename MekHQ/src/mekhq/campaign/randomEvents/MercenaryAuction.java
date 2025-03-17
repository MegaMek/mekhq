/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.randomEvents;

import megamek.common.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.finances.Finances;
import mekhq.campaign.finances.Money;
import mekhq.gui.dialog.GenericImmersiveMessageDialog;
import mekhq.gui.dialog.MercenaryAuctionDialog;

import static java.lang.Math.floor;
import static java.lang.Math.min;
import static megamek.common.Compute.d6;
import static megamek.common.Compute.randomInt;
import static megamek.common.enums.SkillLevel.REGULAR;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.finances.enums.TransactionType.EQUIPMENT_PURCHASE;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.getEntity;
import static mekhq.campaign.mission.BotForceRandomizer.UNIT_WEIGHT_UNSPECIFIED;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

/**
 * This class handles the logic for determining auction eligibility based on the player's finances
 * and provides the interface for bidding in mercenary auctions. Successful auctions result in the
 * unit being added to the campaign, while failures notify the player of the outcome.
 */
public class MercenaryAuction {
    private static final String RESOURCE_BUNDLE = "mekhq.resources." + MercenaryAuctionDialog.class.getSimpleName();

    /**
     * The minimum bid percentage required to participate in the auction.
     *
     * <p>This constant defines the lowest percentage of the unit's base value that a player is
     * allowed to bid in an auction. It ensures that all bids meet a minimum threshold, preventing
     * unreasonably low offers during the auction process.</p>
     *
     * <p>This is stored as an {@link Integer}, not a {@link Double}, as it's used to set up a
     * {@link javax.swing.JSpinner} and players tend to have an easier time understanding percentages
     * when presented as integers.
     */
    private static final int AUCTION_MINIMUM_BID_PERCENT = 50;

    /**
     * Represents the maximum bid percentage allowed during a mercenary auction.
     *
     * <p>This constant defines the upper limit for the bid multiplier a player can apply when
     * participating in an auction, where the value is represented as a multiplier (e.g., 1.5
     * corresponds to 150%). It ensures that the auction bidding process is capped at a predefined
     * value as bids above this value have the same % chance of success, so we don't want the player
     * paying more than they need.</p>
     */
    private static final double AUCTION_MAXIMUM_BID_PERCENT = 1.5;

    /**
     * Creates and processes a mercenary auction.
     *
     * <p>The auction determines eligibility for bidding, calculates the maximum bid based on campaign
     * finances, and displays an auction dialog for the player to place their bid. Additionally, it
     * handles the outcome of the auction, applying the results to the campaign accordingly.</p>
     *
     * @param campaign The current {@link Campaign} instance where the auction takes place.
     * @param unitType The type of unit being auctioned (e.g., `MECH`, `VEHICLE`).
     */
    public MercenaryAuction(Campaign campaign, int unitType) {
        String faction = campaign.getFaction().getShortName();

        Entity entity = getEntity(faction, REGULAR, getRandomUnitQuality(-2).toNumeric(),
              unitType, UNIT_WEIGHT_UNSPECIFIED, null, campaign);

        if (entity == null) {
            return;
        }

        double value = entity.getCost(false);
        Money valueAsMoney = Money.of(value);

        Finances campaignFinances = campaign.getFinances();
        Money campaignFunds = campaignFinances.getBalance();

        int maximumBid = (int) floor(getMaxBidPercentage(valueAsMoney, campaignFunds) * 100);

        // If the player can't afford the minimum bid, we just tell them about the opportunity and
        // then close out the auction.
        if (maximumBid < AUCTION_MINIMUM_BID_PERCENT) {
            new GenericImmersiveMessageDialog(campaign, campaign.getSeniorAdminPerson(TRANSPORT),
                  null, getFormattedTextAt(RESOURCE_BUNDLE, "auction.ic.noFunds",
                  campaign.getCommanderAddress(false), entity.getShortName()),
                  null);
            return;
        }

        // Otherwise, we show the Auction dialog.
        MercenaryAuctionDialog mercenaryAuctionDialog = new MercenaryAuctionDialog(campaign, entity,
              min(maximumBid, AUCTION_MINIMUM_BID_PERCENT * 2), AUCTION_MINIMUM_BID_PERCENT, maximumBid, 5);
        int adjustedBid = mercenaryAuctionDialog.getSpinnerValue() - AUCTION_MINIMUM_BID_PERCENT;

        // If the player confirmed the auction (option 0) then check whether they were successful,
        // deliver the unit, and deduct funds.
        if (mercenaryAuctionDialog.getDialogChoice() == 0) {
            // The use of <= is important here as it ensures that even if the user bids 50 %, they can
            // still win.
            if (randomInt(AUCTION_MINIMUM_BID_PERCENT * 2) <= adjustedBid) {
                Money finalBid = valueAsMoney.multipliedBy(mercenaryAuctionDialog.getSpinnerValue() / 100);

                campaignFinances.debit(EQUIPMENT_PURCHASE, campaign.getLocalDate(), finalBid,
                      getFormattedTextAt(RESOURCE_BUNDLE, "auction.transaction", entity.getShortName()));

                // The delivery time is so that the unit addition is picked up by the 'mothball'
                // campaign option. It also makes sense the unit wouldn't magically materialize in your
                // hangar and has to get there.
                int deliveryTime = d6();
                campaign.addNewUnit(entity, false, deliveryTime);

                // This dialog informs the player their bid was successful
                new GenericImmersiveMessageDialog(campaign, campaign.getSeniorAdminPerson(TRANSPORT),
                      null, getFormattedTextAt(RESOURCE_BUNDLE, "auction.successful",
                      entity.getChassis(), deliveryTime), null);
            } else {

                // This dialog informs the player their bid was unsuccessful
                new GenericImmersiveMessageDialog(campaign, campaign.getSeniorAdminPerson(TRANSPORT),
                      null, getFormattedTextAt(RESOURCE_BUNDLE, "auction.failure",
                      entity.getChassis()), null);
            }
        }
    }

    /**
     * Calculates the maximum bid percentage a player can afford.
     *
     * <p>This method iterates through possible percentages (starting at 50% and incrementing by 5%)
     * to determine the maximum bid percentage the player can afford. The final amount is constrained
     * by a maximum of 150%.</p>
     *
     * @param valueAsMoney The monetary value of the unit being auctioned, represented as {@link Money}.
     * @param campaignFunds The player's current campaign funds, represented as {@link Money}.
     * @return The maximum bid percentage the player can afford as a multiplier
     *         (e.g., 0.5 for 50%, 1.0 for 100%).
     */
    private double getMaxBidPercentage(Money valueAsMoney, Money campaignFunds) {
        for (double i = 0.5; i <= AUCTION_MAXIMUM_BID_PERCENT; i += 0.05) {
            Money adjustedValueAsMoney = valueAsMoney.multipliedBy(i);

            if (adjustedValueAsMoney.isGreaterOrEqualThan(campaignFunds)) {
                return i - 0.05;
            }
        }

        return 1.5;
    }
}
