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
package mekhq.campaign.personnel.lifeEvents;

import static megamek.client.ui.util.UIUtil.scaleForGUI;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;
import javax.swing.ImageIcon;

import megamek.common.annotations.Nullable;
import megamek.utilities.ImageUtilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * This class is responsible for managing the immersive announcement for Commander's Day. It generates both in-character
 * (IC) and out-of-character (OOC) messages, displays them in an interactive dialog, and allows users to suppress future
 * announcements if desired.
 *
 * <p>Commander's Day is a special event date that recognizes the campaign's commander and promotes camaraderie
 * within the unit. This class defines handling for this event and ensures appropriate messaging is shown based on the
 * campaign context.</p>
 *
 * @since 0.50.05
 */
public record CommandersDayAnnouncement(Campaign campaign) {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.CommandersDayAnnouncement";

    private final static int COMMANDERS_DAY_MONTH = 6;
    private final static int COMMANDERS_DAY_DAY = 16;

    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 3;

    /**
     * Constructs a new {@code CommandersDayAnnouncement}.
     *
     * <p>Initializes the announcement for Commander's Day by generating immersive in-character (IC) and
     * out-of-character (OOC) messages. These messages are displayed in an interactive dialog, allowing users to select
     * options or suppress future events related to Commander's Day.</p>
     *
     * @param campaign the {@link Campaign} instance containing the relevant context for the announcement
     */
    public CommandersDayAnnouncement(Campaign campaign) {
        this.campaign = campaign;

        String inCharacterMessage = getInCharacterMessage();
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "commandersDay.message.ooc");

        ImageIcon banner = new ImageIcon("data/images/misc/hcdbanner.png");
        banner = ImageUtilities.scaleImageIcon(banner, scaleForGUI(400), true);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              inCharacterMessage,
              getButtonLabels(),
              outOfCharacterMessage,
              banner,
              true);

        if (dialog.getDialogChoice() == SUPPRESS_DIALOG_RESPONSE_INDEX) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            campaignOptions.setShowLifeEventDialogCelebrations(false);
        }
    }


    /**
     * Generates and retrieves the in-character (IC) message to be displayed during the Commander's Day announcement.
     * This message is personalized with the commander's information, such as their address and surname.
     *
     * @return the generated IC message as a {@link String}
     */
    private String getInCharacterMessage() {
        // Commander Data
        Person commander = campaign.getCommander();
        String commanderAddress = campaign.getCommanderAddress();

        String commanderSurname = commander.getSurname();
        if (commanderSurname.isBlank()) {
            commanderSurname = commander.getFirstName();
        }

        // Build the in character message
        String resourceKey = "commandersDay.message." + randomInt(50) + ".ic";

        // {0} Commander Address
        // {1} Commander Name
        return getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress, commanderSurname);
    }


    /**
     * Retrieves a {@link Person} who will act as the in-character speaker for the Commander's Day announcement. The
     * speaker is typically chosen from the campaign's administrative personnel, prioritizing HR, then COMMAND.
     *
     * @return the selected {@link Person}, or {@code null} if no suitable speaker is available
     */
    private @Nullable Person getSpeaker() {
        Person speaker = campaign.getSeniorAdminPerson(HR);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }


    /**
     * Generates and retrieves a list of button labels for the dialog interaction during Commander's Day. These labels
     * represent user options, such as positive, neutral, or negative responses and a suppression option.
     *
     * @return a {@link List} of button label strings for dialog interactions
     */
    private List<String> getButtonLabels() {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.positive"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.neutral"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.negative"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));
    }


    /**
     * Checks if the provided {@link LocalDate} corresponds to Commander's Day, which is predetermined to occur on June
     * 16th.
     *
     * @param date the {@link LocalDate} to check
     *
     * @return {@code true} if the provided date is Commander's Day; {@code false} otherwise
     */
    public static boolean isCommandersDay(LocalDate date) {
        return date.getMonthValue() == COMMANDERS_DAY_MONTH && date.getDayOfMonth() == COMMANDERS_DAY_DAY;
    }
}
