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

import static mekhq.MHQConstants.NAG_CONTRACT_SPECIAL_MECHANICS_PREFIX;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics;
import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics.EscalationMode;
import mekhq.campaign.mission.contract.AbstractContract;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;

/**
 * Briefs the player, when they accept a contract, on the special mechanics that contract type brings to StratCon under
 * the "Contracts Use Special Mechanics" option (see {@link StratConContractMechanics}).
 *
 * <p>The briefing is out-of-character and sets out the rules exactly: the contract type's own rules, followed by the
 * shared Escalation rules if the contract escalates. As these briefings are the only tutorial the player gets for
 * these mechanics, the text must be kept in step with the rules.</p>
 *
 * <p>The player can acknowledge the briefing, or acknowledge it and suppress future briefings for that contract type
 * (see {@link #getNagKey}). Each contract type is suppressed separately, since each has its own rules.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ContractSpecialMechanicsNagDialog extends ImmersiveDialogNag {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSpecialMechanicsNagDialog";
    private static final String KEY_PREFIX = "ContractSpecialMechanicsNagDialog.";

    private static final int CHOICE_ACKNOWLEDGE = 0;
    private static final int CHOICE_SUPPRESS = 1;

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
            new ContractSpecialMechanicsNagDialog(campaign, contract);
        }
    }

    /**
     * Decides whether a newly accepted contract should be briefed on its special mechanics: the "Contracts Use Special
     * Mechanics" option is on, the contract is run on a StratCon map (special mechanics are only set up there), its
     * contract type has special mechanics, and the player has not suppressed the briefing for its contract type.
     *
     * @param campaign the current campaign
     * @param contract the contract just accepted
     *
     * @return {@code true} if the briefing should be shown
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isBriefingDue(Campaign campaign, AbstractContract contract) {
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        if (!campaignOptions.get(CampaignOption.CONTRACTS_USE_SPECIAL_MECHANICS)) {
            return false;
        }

        if ((contract.getStratConCampaignState() == null) || campaignOptions.isUseStratConMaplessMode()) {
            return false;
        }

        ContractObjectiveType objectiveType = contract.getObjectiveType();
        if (!hasSpecialMechanics(StratConContractMechanics.forObjectiveType(objectiveType))) {
            return false;
        }

        return !MekHQ.getMHQOptions().getNagDialogIgnore(getNagKey(objectiveType));
    }

    /**
     * @param objectiveType a contract type with special mechanics
     *
     * @return the key under which the player's choice to suppress that contract type's briefing is stored
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getNagKey(ContractObjectiveType objectiveType) {
        return NAG_CONTRACT_SPECIAL_MECHANICS_PREFIX + objectiveType.name();
    }

    /**
     * @return every contract type that has special mechanics, and so a briefing of its own, in declaration order
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<ContractObjectiveType> getBriefedContractTypes() {
        List<ContractObjectiveType> briefedContractTypes = new ArrayList<>();
        for (ContractObjectiveType objectiveType : ContractObjectiveType.values()) {
            if (hasSpecialMechanics(StratConContractMechanics.forObjectiveType(objectiveType))) {
                briefedContractTypes.add(objectiveType);
            }
        }
        return briefedContractTypes;
    }

    /**
     * @param mechanics a contract type's mechanics
     *
     * @return {@code true} if the contract type has any special mechanic to brief the player on
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean hasSpecialMechanics(StratConContractMechanics mechanics) {
        return mechanics.hasSpecialPointOfInterest()
                     || mechanics.isUsingReconnaissance()
                     || (mechanics.escalationMode() != EscalationMode.NONE);
    }

    private ContractSpecialMechanicsNagDialog(Campaign campaign, AbstractContract contract) {
        super();
        this.contract = contract;

        ImmersiveDialogCore dialog = constructDialog(campaign, getMessageKey());
        processDialogChoice(dialog.getDialogChoice(), getNagKey(contract.getObjectiveType()));
    }

    private String getMessageKey() {
        return KEY_PREFIX + contract.getObjectiveType().name();
    }

    @Override
    protected ImmersiveDialogCore constructDialog(Campaign campaign, String messageKey) {
        return new ImmersiveDialogCore(campaign,
              getSpeaker(campaign),
              null,
              getBriefingText(contract.getObjectiveType()),
              createButtons(),
              null,
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

    /**
     * The contract type's own rules, then the shared Escalation rules if the contract escalates, then the note on
     * suppressing the briefing. A Garrison Duty contract's Escalation runs the other way around, so its rules explain
     * it themselves.
     *
     * @param objectiveType the contract type being briefed
     *
     * @return the briefing text
     *
     * @author Illiani
     * @since 0.51.01
     */
    // Package-private and static so the briefing's make-up can be tested without showing the dialog.
    static String getBriefingText(ContractObjectiveType objectiveType) {
        StringBuilder message = new StringBuilder(getFormattedTextAt(RESOURCE_BUNDLE,
              KEY_PREFIX + objectiveType.name()));

        EscalationMode escalationMode = StratConContractMechanics.forObjectiveType(objectiveType).escalationMode();
        if (escalationMode == EscalationMode.ESCALATING) {
            message.append(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "escalation"));
        }

        message.append(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "footer"));
        return message.toString();
    }

    /**
     * There is nothing to cancel when accepting a contract, so the player can only acknowledge the briefing, or
     * acknowledge it and suppress future briefings for this contract type.
     *
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected List<ButtonLabelTooltipPair> createButtons() {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.acknowledge"),
              null));
        buttons.add(new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE, KEY_PREFIX + "button.suppress",
              contract.getObjectiveType().toString()), null));
        return buttons;
    }

    @Override
    protected void processDialogChoice(int choiceIndex, String nagConstant) {
        if (choiceIndex == CHOICE_SUPPRESS) {
            MekHQ.getMHQOptions().setNagDialogIgnore(nagConstant, true);
        } else if (choiceIndex != CHOICE_ACKNOWLEDGE) {
            throw new IllegalStateException("Unexpected value in ContractSpecialMechanicsNagDialog/processDialogChoice: "
                                                  + choiceIndex);
        }
        cancelAdvanceDay = false;
    }
}
