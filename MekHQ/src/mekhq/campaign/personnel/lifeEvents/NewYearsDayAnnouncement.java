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

import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * This class handles the creation and display of immersive announcements for the New Year's Day event. It generates
 * both in-character (IC) and out-of-character (OOC) messages, provides options for user interactions, and allows users
 * to suppress future announcements.
 *
 * <p>New Year's Day is identified by specific dates, and this class determines messaging and behaviors for
 * the event accordingly. It relies on campaign-wide data provided via the {@link Campaign} instance.</p>
 *
 * @since 0.50.05
 */
public record NewYearsDayAnnouncement(Campaign campaign) {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.NewYearsDayAnnouncement";

    // Constants for significant holiday-related dates.
    private final static int NEW_YEARS_MONTH = 1;
    private final static int NEW_YEARS_DAY = 1;

    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 3;


    /**
     * Constructs a new {@code NewYearsDayAnnouncement}.
     *
     * <p>Initializes the announcement with immersive in-character (IC) and out-of-character (OOC) messages.
     * These messages are displayed in a dialog window where users can make choices or suppress future messages related
     * to the event.</p>
     *
     * @param campaign the {@link Campaign} instance containing the relevant campaign context
     */
    public NewYearsDayAnnouncement(Campaign campaign) {
        this.campaign = campaign;

        String inCharacterMessage = getInCharacterMessage();
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "newYear.message.ooc");

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(),
              null,
              inCharacterMessage,
              getButtonLabels(),
              outOfCharacterMessage,
              null,
              true);

        if (dialog.getDialogChoice() == SUPPRESS_DIALOG_RESPONSE_INDEX) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            campaignOptions.setShowLifeEventDialogCelebrations(false);
        }
    }


    /**
     * Generates and retrieves the in-character (IC) message to be displayed during the New Year's event announcement.
     *
     * @return the generated IC message as a {@link String}
     */
    private String getInCharacterMessage() {
        String commanderAddress = campaign.getCommanderAddress();
        return getFormattedTextAt(RESOURCE_BUNDLE, "newYear.message.ic", commanderAddress);
    }


    /**
     * Retrieves a randomly selected {@link Person} to serve as the in-character speaker for the New Year's event
     * announcement. The speaker is chosen based on a random active member of the campaign.
     *
     * @return the selected {@link Person}, or {@code null} if no suitable speaker is found
     */
    private @Nullable Person getSpeaker() {
        List<Person> activePersonnel = campaign.getActivePersonnel(false, false);

        Person commander = campaign.getCommander();
        if (commander != null) {
            activePersonnel.remove(commander);
        }

        if (activePersonnel.isEmpty()) {
            return null;
        }

        return getRandomItem(activePersonnel);
    }


    /**
     * Generates a list of button label options to display in the dialog for the New Year's event announcement.
     *
     * @return a {@link List} of button label strings that represent user choices
     */
    private List<String> getButtonLabels() {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.positive"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.neutral"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.negative"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));
    }


    /**
     * Determines whether the provided {@link LocalDate} corresponds to New Year's Day (January 1st).
     *
     * @param date the {@link LocalDate} to check
     *
     * @return {@code true} if the provided date is New Year's Day; {@code false} otherwise
     */
    public static boolean isNewYear(LocalDate date) {
        if (date.getMonthValue() != NEW_YEARS_MONTH) {
            return false;
        }

        return date.getDayOfMonth() == NEW_YEARS_DAY;
    }
}
