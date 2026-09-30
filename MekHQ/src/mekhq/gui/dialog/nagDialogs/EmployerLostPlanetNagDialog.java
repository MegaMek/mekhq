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

import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.PlanetLossResponse;
import mekhq.campaign.mission.contract.utilities.EmployerLostPlanet;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * Asks the player how to respond when their employer is about to lose control of the planet a defensive contract is
 * being fought on (see {@link EmployerLostPlanet}).
 *
 * <p>Shown as the day ends, before the loss takes effect. The player can join the evacuation, lead the resistance, or
 * cancel the day advance - which also interrupts any multi-day advance or travel - and decide later. The response is
 * stored on the contract and applied as the new day is processed. It cannot be suppressed, as the player must
 * decide.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class EmployerLostPlanetNagDialog extends ImmersiveDialogNag {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EmployerLostPlanet";
    private static final String KEY_PREFIX = "EmployerLostPlanetNagDialog.";

    // Button 1, Join the Evacuation, is the default and so is not matched explicitly (see processDialogChoice)
    private static final int CHOICE_CANCEL = 0;
    private static final int CHOICE_RESIST = 2;

    private final AbstractContract contract;

    /**
     * Shows the dialog for a contract whose employer loses control of its planet tomorrow, and records the player's
     * response on the contract.
     *
     * @param campaign the current campaign
     * @param contract the affected contract
     *
     * @author Illiani
     * @since 0.51.01
     */
    public EmployerLostPlanetNagDialog(Campaign campaign, AbstractContract contract) {
        super();
        this.contract = contract;

        ImmersiveDialogCore dialog = constructDialog(campaign, null);
        processDialogChoice(dialog.getDialogChoice(), null);
    }

    /**
     * @param isEnabled       whether the campaign reacts to employers losing their planets
     * @param awaitingContracts the contracts whose employer loses control of their planet tomorrow, where the unit has
     *                        arrived
     *
     * @return {@code true} if the player must respond before the day can end
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean checkNag(boolean isEnabled, List<AbstractContract> awaitingContracts) {
        return isEnabled && !awaitingContracts.isEmpty();
    }

    @Override
    protected ImmersiveDialogCore constructDialog(Campaign campaign, @Nullable String messageKey) {
        LocalDate tomorrow = campaign.getLocalDate().plusDays(1);
        String planetName = EmployerLostPlanet.getPlanetName(contract, tomorrow);
        String newOwnerName = EmployerLostPlanet.getNewOwnerName(contract, tomorrow);
        if (newOwnerName == null) {
            newOwnerName = getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "unknownOwner");
        }

        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "ic",
              campaign.getCommanderAddress(),
              contract.getEmployerDisplayName(),
              planetName,
              newOwnerName);
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "ooc",
              contract.getName(),
              planetName);

        return new ImmersiveDialogCore(campaign,
              getSpeaker(campaign),
              null,
              inCharacterMessage,
              createButtons(),
              outOfCharacterMessage,
              null,
              true,
              null,
              null,
              true);
    }

    /**
     * The news comes from the employer's liaison, falling back to the campaign's senior administrator.
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
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.cancel"),
              getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.cancel.tooltip")));
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.evacuate"),
              getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.evacuate.tooltip")));
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.resist"),
              getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.resist.tooltip")));
        return buttons;
    }

    /**
     * Records the player's response. Joining the evacuation is the default, so any choice other than cancelling or
     * leading the resistance is taken as joining the evacuation.
     */
    @Override
    protected void processDialogChoice(int choiceIndex, @Nullable String nagConstant) {
        switch (choiceIndex) {
            case CHOICE_CANCEL -> {
                contract.setPendingPlanetLossResponse(null);
                cancelAdvanceDay = true;
            }
            case CHOICE_RESIST -> {
                contract.setPendingPlanetLossResponse(PlanetLossResponse.LEAD_RESISTANCE);
                cancelAdvanceDay = false;
            }
            default -> {
                contract.setPendingPlanetLossResponse(PlanetLossResponse.JOIN_EVACUATION);
                cancelAdvanceDay = false;
            }
        }
    }
}
