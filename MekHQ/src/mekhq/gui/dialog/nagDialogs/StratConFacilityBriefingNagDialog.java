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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.MHQConstants.NAG_STRATCON_FACILITY_BRIEFING;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.digitalGM.stratCon.StratConCampaignState;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacilityOperations;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * Briefs the player, the first time they accept a contract whose StratCon map has facilities, on what facilities are
 * for and how to act on them. It is shown once: acknowledging it suppresses it, and the player can bring it back from
 * the reminders options.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class StratConFacilityBriefingNagDialog extends ImmersiveDialogNag {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.StratConFacilityOperations";

    private final AbstractContract contract;

    /**
     * Shows the briefing for a newly accepted contract, if one is due (see {@link #isBriefingDue}).
     *
     * @param campaign the current campaign
     * @param contract the contract just accepted, with its StratCon state already set up
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void showIfDue(Campaign campaign, AbstractContract contract) {
        if (isBriefingDue(campaign, contract)) {
            new StratConFacilityBriefingNagDialog(campaign, contract);
        }
    }

    /**
     * @param campaign the current campaign
     * @param contract the contract just accepted
     *
     * @return {@code true} if Facility Operations are on, the contract is run on a StratCon map with at least one
     *       facility, and the player has not yet seen (or has asked to see again) the briefing
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isBriefingDue(Campaign campaign, AbstractContract contract) {
        // Facility Operations are never on in mapless play.
        if (!StratConFacilityOperations.isEnabled(campaign)) {
            return false;
        }

        StratConCampaignState campaignState = contract.getStratConCampaignState();
        if ((campaignState == null) || !hasAnyFacility(campaignState)) {
            return false;
        }

        return !MekHQ.getMHQOptions().getNagDialogIgnore(NAG_STRATCON_FACILITY_BRIEFING);
    }

    private static boolean hasAnyFacility(StratConCampaignState campaignState) {
        for (StratConTrackState track : campaignState.getTracks()) {
            if (!track.getFacilities().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private StratConFacilityBriefingNagDialog(Campaign campaign, AbstractContract contract) {
        super();
        this.contract = contract;

        ImmersiveDialogCore dialog = constructDialog(campaign, "briefing");
        processDialogChoice(dialog.getDialogChoice(), NAG_STRATCON_FACILITY_BRIEFING);
    }

    @Override
    protected ImmersiveDialogCore constructDialog(Campaign campaign, String messageKey) {
        return new ImmersiveDialogCore(campaign,
              getSpeaker(campaign),
              null,
              getFormattedTextAt(RESOURCE_BUNDLE, messageKey + ".ic", campaign.getCommanderAddress()),
              createButtons(),
              getFormattedTextAt(RESOURCE_BUNDLE, messageKey + ".ooc"),
              null,
              true,
              null,
              null,
              true);
    }

    /**
     * The briefing comes from the employer's liaison, falling back to the campaign's senior administrator.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected @Nullable Person getSpeaker(Campaign campaign) {
        Person liaison = contract.getEmployerLiaison();
        return (liaison == null) ? super.getSpeaker(campaign) : liaison;
    }

    @Override
    protected List<ButtonLabelTooltipPair> createButtons() {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, "briefing.button"), null));
        return buttons;
    }

    /**
     * The briefing is shown once, so acknowledging it is all there is, and it suppresses it from then on.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void processDialogChoice(int choiceIndex, String nagConstant) {
        MekHQ.getMHQOptions().setNagDialogIgnore(nagConstant, true);
        cancelAdvanceDay = false;
    }
}
