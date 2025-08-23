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

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * This class manages the creation of immersive messages displayed during Winter Holiday events. It is responsible for
 * generating both in-character (IC) and out-of-character (OOC) messages, handling dialog interactions, and determining
 * specific behaviors based on the current date.
 *
 * <p>The Winter Holiday denotes several significant dates, each with unique messages and corresponding responses.
 * Users can suppress further dialogs if desired.</p>
 *
 * @since 0.50.05
 */
public class WinterHolidayAnnouncement {
    private static final MMLogger logger = MMLogger.create(WinterHolidayAnnouncement.class);

    private static String RESOURCE_BUNDLE = "mekhq.resources.WinterHolidayAnnouncement";

    private final Campaign campaign;

    // Constants for significant holiday-related dates.
    private final static int WINTER_HOLIDAY_START_YEAR = 2957;
    private final static int WINTER_HOLIDAY_MONTH = 12;
    private final static int WINTER_HOLIDAY_DAY_ZERO = 10;
    private final static int WINTER_HOLIDAY_DAY_ELEVEN = 27;

    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 3;

    /**
     * Constructs a new {@code WinterHolidayAnnouncement}.
     *
     * <p>Initializes the announcement by generating immersive in-character (IC) and out-of-character (OOC) messages.
     * The messages are displayed as part of a dialog, allowing the user to select options or suppress future
     * announcements.</p>
     *
     * @param campaign the campaign instance associated with this announcement
     */
    public WinterHolidayAnnouncement(Campaign campaign) {
        this.campaign = campaign;

        String inCharacterMessage = getInCharacterMessage();
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "winterHoliday.message.ooc");

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
     * Builds the in-character message for the Winter Holiday event based on the current campaign date.
     *
     * @return the message specific to the current Winter Holiday day, or an empty string if the day is not significant
     */
    private String getInCharacterMessage() {
        // Campaign Data
        LocalDate currentDate = campaign.getLocalDate();

        // Build the in character message
        return switch (currentDate.getDayOfMonth()) {
            case WINTER_HOLIDAY_DAY_ZERO -> getInCharacterMessageDayZero();
            case WINTER_HOLIDAY_DAY_ELEVEN -> getInCharacterMessageDayEleven();
            default -> {
                logger.error("WinterHolidayAnnouncement: getInCharacterMessage: unexpected day of month: {}",
                      currentDate.getDayOfMonth());
                yield "";
            }
        };
    }

    /**
     * Builds the in-character message for Day Zero of the Winter Holiday event.
     *
     * @return the Day Zero message as a string
     */
    private String getInCharacterMessageDayZero() {
        // Commander Data
        String commanderAddress = campaign.getCommanderAddress();

        // Determine the location context
        String location = campaign.getLocation().isOnPlanet() ? "planetside" : "transit";

        // Generate each paragraph and concatenate the full message
        StringBuilder messageBuilder = new StringBuilder();
        int totalParagraphs = 3;

        for (int i = 0; i < totalParagraphs; i++) {
            messageBuilder.append(constructDayZeroParagraph(i, commanderAddress, location));
        }

        return messageBuilder.toString();
    }


    /**
     * Constructs a specific paragraph of the Day Zero message.
     *
     * @param paragraphIndex   the index of the paragraph to construct
     * @param commanderAddress the address or title of the commander
     * @param location         the current campaign location ("planetside" or "transit")
     *
     * @return the paragraph as a string
     */
    private String constructDayZeroParagraph(int paragraphIndex, String commanderAddress, String location) {
        int variant = randomInt(50);

        String resourceKey = "winterHoliday.message.dayZero." +
                                   variant +
                                   '.' +
                                   location +
                                   ".paragraph." +
                                   paragraphIndex +
                                   ".ic";

        return getFormattedTextAt(RESOURCE_BUNDLE, resourceKey, commanderAddress);
    }

    /**
     * Builds the in-character message for Day Eleven of the Winter Holiday event.
     *
     * @return the message as a string for the specified day
     */
    private String getInCharacterMessageDayEleven() {
        String commanderAddress = campaign.getCommanderAddress();

        return getFormattedTextAt(RESOURCE_BUNDLE, "winterHoliday.message.dayEleven." + randomInt(50) + ".ic",
              commanderAddress);
    }


    /**
     * Retrieves the speaker for the Winter Holiday announcement.
     *
     * <p>Prioritizes the HR administrator; falls back to the Command administrator if no HR administrator is
     * found.</p>
     *
     * @return the {@link Person} representing the speaker, or {@code null} if no suitable person is found
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
     * Generates the button labels for user responses during Winter Holiday announcements.
     *
     * @return a list of localized button labels
     */
    private List<String> getButtonLabels() {
        // Campaign Data
        LocalDate currentDate = campaign.getLocalDate();
        int dayOfMonth = currentDate.getDayOfMonth();

        // Build the list of messages responses
        List<String> messageLabels = generateDaySpecificLabels(dayOfMonth);
        messageLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));

        return messageLabels;
    }


    /**
     * Generates response labels for a given Winter Holiday day.
     *
     * @param dayOfMonth the day of the month to generate responses for
     *
     * @return a list of localized labels for the specified day, or an empty list if the day is not significant
     */
    private List<String> generateDaySpecificLabels(int dayOfMonth) {
        String dayKey = switch (dayOfMonth) {
            case WINTER_HOLIDAY_DAY_ZERO -> "dayZero";
            case WINTER_HOLIDAY_DAY_ELEVEN -> "dayEleven";
            default -> null;
        };

        // If the day is not a Winter Holiday major day, return an empty list
        if (dayKey == null) {
            return new ArrayList<>();
        }

        List<String> buttonLabels = new ArrayList<>();

        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response." + dayKey + ".positive"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response." + dayKey + ".neutral"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response." + dayKey + ".negative"));

        return buttonLabels;
    }

    /**
     * Determines whether a given date is a major Winter Holiday day.
     *
     * @param date the {@link LocalDate} to evaluate
     *
     * @return {@code true} if the specified date is a major Winter Holiday day, {@code false} otherwise
     */
    public static boolean isWinterHolidayMajorDay(LocalDate date) {
        if (date.getYear() < WINTER_HOLIDAY_START_YEAR) {
            return false;
        }

        if (date.getMonthValue() != WINTER_HOLIDAY_MONTH) {
            return false;
        }

        return switch (date.getDayOfMonth()) {
            case WINTER_HOLIDAY_DAY_ZERO, WINTER_HOLIDAY_DAY_ELEVEN -> true;
            default -> false;
        };
    }
}
