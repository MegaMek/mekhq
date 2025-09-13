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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */
package mekhq.campaign.randomEvents;

import static java.lang.Math.max;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.SkillLevel.REGULAR;
import static mekhq.campaign.Campaign.AdministratorSpecialization.TRANSPORT;
import static mekhq.campaign.mission.AtBDynamicScenarioFactory.getEntity;
import static mekhq.campaign.mission.BotForceRandomizer.UNIT_WEIGHT_UNSPECIFIED;
import static mekhq.campaign.unit.Unit.getRandomUnitQuality;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.stratcon.StratconCampaignState;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.dialog.MercenaryAuctionDialog;

/**
 * This class handles the logic for determining auction eligibility based on the player's resources and provides the
 * interface for bidding in mercenary auctions. Successful auctions result in the unit being added to the campaign,
 * while failures notify the player of the outcome.
 */
public class MercenaryAuction {
    private static final MMLogger LOGGER = MMLogger.create(MercenaryAuction.class);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.MercenaryAuctionDialog";

    private static final int AUCTION_TIER_SUCCESS_PERCENT = 20;
    private static final int DECLINE_AUCTION_OPTION = 0;

    /**
     * Creates and processes a mercenary auction.
     *
     * <p>The auction determines eligibility for bidding, calculates the maximum bid based on campaign
     * resources, and displays an auction dialog for the player to place their bid. Additionally, it handles the outcome
     * of the auction, applying the results to the campaign accordingly.</p>
     *
     * @param campaign The current {@link Campaign} instance where the auction takes place.
     * @param unitType The type of unit being auctioned (e.g., `MEK`, `VEHICLE`).
     */
    public MercenaryAuction(Campaign campaign, int requiredCombatTeams, StratconCampaignState campaignState,
          int unitType) {
        String faction = campaign.getFaction().getShortName();

        Entity entity = getEntity(faction,
              REGULAR,
              getRandomUnitQuality(-2).toNumeric(),
              unitType,
              UNIT_WEIGHT_UNSPECIFIED,
              null,
              campaign);

        if (entity == null) {
            LOGGER.error("Unable to find entity for unit type {} in 'MercenaryAuction'", unitType);
            return;
        }

        // Fallback for non-StratCon campaigns
        if (campaignState == null) {
            int deliveryTime = d6();
            campaign.addNewUnit(entity, false, deliveryTime);
            return;
        }

        int maximumBid = campaignState.getSupportPoints();
        int minimumBid = max(requiredCombatTeams / 2, 1);
        boolean cannotAffordOpeningBid = maximumBid < minimumBid;

        // If the player can't afford the minimum bid, we just tell them about the opportunity and
        // then close out the auction.
        if (cannotAffordOpeningBid) {
            String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                  "auction.ic.noFunds",
                  campaign.getCommanderAddress(),
                  entity.getShortName());

            String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
                  "auction.ooc.noFunds",
                  minimumBid,
                  maximumBid);

            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorAdminPerson(TRANSPORT),
                  null,
                  inCharacterMessage,
                  null,
                  outOfCharacterMessage,
                  null,
                  true);
            return;
        }

        // Otherwise, we show the Auction dialog.
        MercenaryAuctionDialog mercenaryAuctionDialog = new MercenaryAuctionDialog(campaign,
              entity,
              minimumBid,
              maximumBid,
              AUCTION_TIER_SUCCESS_PERCENT);
        int bidSuccessChance = (mercenaryAuctionDialog.getSpinnerValue() / minimumBid) *
                                     AUCTION_TIER_SUCCESS_PERCENT;

        // If the player confirmed the auction, then check whether they were successful,
        // deliver the unit, and deduct funds.
        if (mercenaryAuctionDialog.getDialogChoice() == DECLINE_AUCTION_OPTION) {
            return;
        }

        // The use of <= is important here as it ensures that even if the user bids 50 %, they can
        // still win.
        if (randomInt(100) <= bidSuccessChance) {
            campaignState.changeSupportPoints(-mercenaryAuctionDialog.getSpinnerValue());

            // The delivery time is so that the unit addition is picked up by the 'mothball'
            // campaign option. It also makes sense the unit wouldn't magically materialize in your
            // hangar and has to get there.
            int deliveryTime = d6();
            // The +1 here is to account for this being an end of day event, so we automatically
            // eat the first day.
            campaign.addNewUnit(entity, false, deliveryTime + 1);

            // This dialog informs the player their bid was successful
            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorAdminPerson(TRANSPORT),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "auction.successful", entity.getChassis(), deliveryTime),
                  null,
                  null,
                  null,
                  true);
        } else {
            // This dialog informs the player their bid was unsuccessful
            new ImmersiveDialogSimple(campaign,
                  campaign.getSeniorAdminPerson(TRANSPORT),
                  null,
                  getFormattedTextAt(RESOURCE_BUNDLE, "auction.failure", entity.getChassis()),
                  null,
                  null,
                  null,
                  true);
        }
    }
}
