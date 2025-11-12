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
package mekhq.gui.baseComponents.immersiveDialogs;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.Campaign.AdministratorSpecialization;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore.ButtonLabelTooltipPair;

/**
 * Handles the display and processing of immersive nag dialogs.
 *
 * <p>The {@code ImmersiveDialogNag} class provides functionality to show an immersive dialog for specific nag
 * notifications, allowing users to either acknowledge, suppress, or cancel advancing the day. The class integrates with
 * MekHQ Options to handle suppression preferences and dialog messaging mechanisms.</p>
 *
 * <p>This class also supports the creation of both in-character and out-of-character messages, formatted using a
 * resource bundle, and provides fallback mechanisms to ensure a valid speaker is always presented in the dialog.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 *   <li>Processing user dialog choices to determine campaign behavior (e.g., cancel advancing the day).</li>
 *   <li>Allowing users to suppress future dialogs for specific nag constants.</li>
 *   <li>Providing localized and formatted message content for immersive dialogs.</li>
 * </ul>
 */
public class ImmersiveDialogNag {
    /**
     * Represents the available user choices for the nag dialog.
     */
    private enum DialogChoice {
        CHOICE_CANCEL(0), CHOICE_CONTINUE(1), CHOICE_SUPPRESS(2);

        private final int choiceIndex;

        DialogChoice(int choiceIndex) {
            this.choiceIndex = choiceIndex;
        }

        /**
         * Retrieves the enum value corresponding to the given choice index.
         *
         * @param choiceIndex The index of the choice.
         *
         * @return The corresponding {@link DialogChoice}.
         *
         * @throws IllegalArgumentException If no matching choice is found.
         */
        public static DialogChoice fromIndex(int choiceIndex) {
            for (DialogChoice choice : DialogChoice.values()) {
                if (choice.choiceIndex == choiceIndex) {
                    return choice;
                }
            }
            throw new IllegalArgumentException("Invalid choice index in ImmersiveDialogNag/DialogChoice/fromIndex: " +
                                                     choiceIndex);
        }
    }

    private final String RESOURCE_BUNDLE = "mekhq.resources.NagDialogs";

    private boolean cancelAdvanceDay;

    protected String getResourceBundle() {
        return RESOURCE_BUNDLE;
    }

    /**
     * Constructs an {@code ImmersiveDialogNag} instance to display a nag dialog.
     *
     * <p>This constructor initializes the nag dialog with in-character and out-of-character messages, dialog
     * buttons, and a speaker determined by the given {@code specialization}. If the {@code specialization} parameter is
     * {@code null}, a fallback speaker with the {@code "COMMAND"} specialization is used. It also processes the user's
     * dialog choice to update campaign state or suppression settings as necessary.</p>
     *
     * @param campaign       The {@link Campaign} instance associated with this nag dialog. Used to fetch relevant
     *                       campaign data and update settings.
     * @param specialization The {@link AdministratorSpecialization} specifying the desired type of administrator to act
     *                       as the speaker. If {@code null}, the fallback speaker with the {@code "COMMAND"}
     *                       specialization will be used.
     * @param nagConstant    A {@code String} identifying the nag constant associated with the dialog. Used to manage
     *                       whether future dialogs with this constant should be suppressed.
     * @param messageKey     A {@code String} key to retrieve localized text for the in-character and out-of-character
     *                       messages from the resource bundle.
     */
    public ImmersiveDialogNag(final Campaign campaign, final @Nullable AdministratorSpecialization specialization,
          final String nagConstant, final String messageKey) {
        ImmersiveDialogCore dialog = constructDialog(campaign, specialization, messageKey);
        processDialogChoice(dialog.getDialogChoice(), nagConstant);
    }

    /**
     * Constructs an {@link ImmersiveDialogSimple} instance for the nag dialog.
     *
     * <p>This method creates an immersive dialog, setting the speaker based on the provided {@code specialization},
     * and populates it with in-character and out-of-character messages along with dialog button labels.</p>
     *
     * @param campaign       The {@link Campaign} instance to associate with the dialog. Used to fetch relevant campaign
     *                       data, such as the commander's address.
     * @param specialization The {@link AdministratorSpecialization} specifying the desired administrator to act as the
     *                       speaker.
     * @param messageKey     A {@code String} key used to retrieve localized text for the dialog's in-character and
     *                       out-of-character messages.
     *
     * @return The constructed {@link ImmersiveDialogSimple} instance containing the specified speaker, messages, and
     *       button labels.
     */
    protected ImmersiveDialogCore constructDialog(Campaign campaign, AdministratorSpecialization specialization,
          String messageKey) {
        return new ImmersiveDialogCore(campaign,
              getSpeaker(campaign, specialization),
              null,
              getInCharacterMessage(campaign, messageKey, campaign.getCommanderAddress()),
              createButtons(),
              getOutOfCharacterMessage(messageKey),
              null,
              true,
              null,
              null,
              true);
    }

    /**
     * Retrieves an in-character message formatted with the provided commander address.
     *
     * <p>This method fetches the text associated with an in-character message key from the
     * resource bundle and formats it using the provided address of the commander.</p>
     *
     * @param campaign         The campaign context.
     * @param key              The reference bundle key.
     * @param commanderAddress The address of the commander to be inserted into the formatted message.
     *
     * @return A formatted in-character message as a {@code String}.
     */
    protected String getInCharacterMessage(Campaign campaign, String key, String commanderAddress) {
        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ic", commanderAddress);
    }

    /**
     * Retrieves an out-of-character message.
     *
     * <p>This method fetches the text associated with the out-of-character message key
     * from the resource bundle without requiring any additional parameters for formatting.</p>
     *
     * @param key The reference bundle key.
     *
     * @return An out-of-character message as a {@code String}.
     */
    protected String getOutOfCharacterMessage(String key) {
        return getFormattedTextAt(RESOURCE_BUNDLE, key + ".ooc");
    }

    /**
     * Handles the processing of a dialog choice based on the given index and updates related settings or state.
     *
     * <p>This method performs different actions depending on the value of the {@code choiceIndex}:</p>
     * <ul>
     *   <li>If {@code CHOICE_CANCEL} is selected, it sets the operation to cancel advancing the day.</li>
     *   <li>If {@code CHOICE_CONTINUE} is selected, it allows advancing the day.</li>
     *   <li>If {@code CHOICE_SUPPRESS} is selected, it suppresses future dialog prompts associated with the
     *       given nag constant and allows advancing the day.</li>
     *   <li>Throws an exception if the {@code choiceIndex} does not match any expected value.</li>
     * </ul>
     *
     * @param choiceIndex An {@code int} representing the user's dialog choice. Must match one of the predefined
     *                    constants (e.g., {@code CHOICE_CANCEL}, {@code CHOICE_CONTINUE}, or {@code CHOICE_SUPPRESS}).
     * @param nagConstant A {@code String} identifying the nag constant associated with the dialog choice. Used to
     *                    manage whether future prompts with this constant should be suppressed.
     *
     * @throws IllegalStateException If the {@code choiceIndex} does not match any of the expected constants.
     */
    private void processDialogChoice(int choiceIndex, String nagConstant) {
        DialogChoice choice = DialogChoice.fromIndex(choiceIndex);

        switch (choice) {
            case CHOICE_CANCEL -> cancelAdvanceDay = true;
            case CHOICE_CONTINUE -> cancelAdvanceDay = false;
            case CHOICE_SUPPRESS -> {
                MekHQ.getMHQOptions().setNagDialogIgnore(nagConstant, true);
                cancelAdvanceDay = false;
            }
            default -> throw new IllegalStateException("Unexpected value in ImmersiveDialogNag/processDialogChoice: " +
                                                             choiceIndex);
        }
    }

    /**
     * Builds the list of buttons displayed in the dialog.
     *
     * <p>
     * This method creates buttons allowing the player to either cancel the mission conclusion or continue.
     * </p>
     *
     * @return A list of button-label and tooltip pairs for this dialog.
     */
    protected List<ButtonLabelTooltipPair> createButtons() {
        List<ButtonLabelTooltipPair> buttons = new ArrayList<>();

        ButtonLabelTooltipPair btnCancel = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "button.cancel"), null);
        buttons.add(btnCancel);

        ButtonLabelTooltipPair btnContinue = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "button.continue"), null);
        buttons.add(btnContinue);

        ButtonLabelTooltipPair btnSuppress = new ButtonLabelTooltipPair(getFormattedTextAt(RESOURCE_BUNDLE,
              "button.suppress"), null);
        buttons.add(btnSuppress);

        return buttons;
    }

    /**
     * Retrieves the senior administrator assigned as the speaker for the given specialization.
     *
     * <p>This method attempts to fetch the senior administrator associated with the specified
     * {@code specialization}. If the {@code specialization} is {@code null}, it defaults to retrieving the senior
     * administrator with the {@code "COMMAND"} specialization. If no administrator is found for the provided
     * specialization, it also falls back to the {@code "COMMAND"} specialization.</p>
     *
     * @param campaign       The campaign context.
     * @param specialization The {@link AdministratorSpecialization} specifying the required type of administrator. Can
     *                       be {@code null}, in which case the fallback to the {@code "COMMAND"} specialization is
     *                       applied directly.
     *
     * @return The {@link Person} assigned as the speaker. This will either be the person matching the given
     *       specialization or, if unavailable, the one assigned to the {@code "COMMAND"} specialization.
     */
    protected @Nullable Person getSpeaker(Campaign campaign, @Nullable AdministratorSpecialization specialization) {
        if (specialization == null) {
            return campaign.getSeniorAdminPerson(COMMAND);
        }

        Person speaker = campaign.getSeniorAdminPerson(specialization);

        if (speaker == null && specialization != COMMAND) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        }

        return speaker;
    }

    /**
     * Determines whether the advance day operation should be canceled.
     *
     * @return {@code true} if advancing the day should be canceled, {@code false} otherwise.
     */
    public boolean shouldCancelAdvanceDay() {
        return cancelAdvanceDay;
    }
}
