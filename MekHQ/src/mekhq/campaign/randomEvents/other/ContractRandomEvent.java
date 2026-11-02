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
package mekhq.campaign.randomEvents.other;

import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.units.UnitType.AEROSPACE_FIGHTER;
import static megamek.common.units.UnitType.MEK;
import static megamek.common.units.UnitType.TANK;
import static mekhq.campaign.enums.DailyReportType.GENERAL;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.utilities.ReportingUtilities;

/**
 * Rolls the monthly random event for an active StratCon contract.
 *
 * <p>Each month a contract has a 1-in-{@value #EVENT_CHANCE} chance of an event. When one happens, a 1d6 decides
 * what it is: on a 1-4 the employer grants bonus Employer Support, on a 5 a {@link RoninOffer} is made, and on a 6 a
 * {@link MercenaryAuction} opens for a tank, aerospace fighter, or 'Mek.</p>
 */
public class ContractRandomEvent {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractRandomEvent";

    /** The event fires on a 1-in-this roll. */
    static final int EVENT_CHANCE = 10;

    private static final int[] AUCTION_UNIT_TYPES = { TANK, AEROSPACE_FIGHTER, MEK };

    /** The kind of event a contract's monthly roll produces. */
    enum EventType {
        EMPLOYER_SUPPORT,
        RONIN,
        AUCTION
    }

    private ContractRandomEvent() {
    }

    /**
     * Rolls the contract's monthly chance of a random event and, if one happens, resolves it. Contracts without a
     * StratCon campaign state have no Employer Support to grant or spend, so they never roll.
     *
     * @param campaign the current campaign
     * @param contract the active contract to roll for
     */
    public static void processMonthlyEvent(Campaign campaign, AbstractContract contract) {
        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if (campaignState == null || randomInt(EVENT_CHANCE) != 0) {
            return;
        }

        switch (getEventType(d6())) {
            case EMPLOYER_SUPPORT -> grantEmployerSupport(campaign, contract, campaignState);
            case RONIN -> new RoninOffer(campaign, campaignState, contract.getScale());
            case AUCTION -> new MercenaryAuction(campaign, contract.getScale(), campaignState,
                  AUCTION_UNIT_TYPES[randomInt(AUCTION_UNIT_TYPES.length)]);
        }
    }

    /**
     * Maps the 1d6 event roll to the event it produces.
     *
     * @param roll the 1d6 roll
     *
     * @return the event the roll produces
     *
     * @throws IllegalArgumentException if the roll is not between 1 and 6
     */
    static EventType getEventType(int roll) {
        return switch (roll) {
            case 1, 2, 3, 4 -> EventType.EMPLOYER_SUPPORT;
            case 5 -> EventType.RONIN;
            case 6 -> EventType.AUCTION;
            default -> throw new IllegalArgumentException("Unexpected contract event roll: " + roll);
        };
    }

    /**
     * Grants the contract bonus Employer Support equal to its Scale, announces it with an immersive dialog from the
     * senior administrator, and logs it in the daily report.
     */
    private static void grantEmployerSupport(Campaign campaign, AbstractContract contract,
          StratConCampaignState campaignState) {
        int bonusSupport = contract.getScale();
        campaignState.changeSupportPoints(bonusSupport);

        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "employerSupport.ic",
              campaign.getCommanderAddress(),
              contract.getName());
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "employerSupport.ooc",
              bonusSupport,
              campaignState.getSupportPoints());

        new ImmersiveDialogSimple(campaign,
              campaign.getPlayerForce().getHumanResources()
                    .getSeniorAdminPerson(campaign.getCampaignOptions(),
                          campaign.getPlayerForce().isClanForce(),
                          campaign.getLocalDate()),
              null,
              inCharacterMessage,
              null,
              outOfCharacterMessage,
              null,
              true);

        campaign.addReport(GENERAL, getFormattedTextAt(RESOURCE_BUNDLE, "employerSupport.report",
              contract.getHyperlinkedName(),
              spanOpeningWithCustomColor(ReportingUtilities.getPositiveColor()),
              bonusSupport,
              CLOSING_SPAN_TAG));
    }
}
