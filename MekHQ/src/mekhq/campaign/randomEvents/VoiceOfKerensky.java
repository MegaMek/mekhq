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
package mekhq.campaign.randomEvents;

import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;
import java.util.List;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogWidth;

/**
 * Easter egg event: The Voice of Kerensky.
 *
 * <p>On September 9, 2786, General Aleksandr Kerensky transmitted a broadband microwave radio
 * message from the Pentagon Worlds. Unlike HPG transmissions, this broadcast travels at the speed
 * of light (1 light-year per year). The signal expands as a spherical wavefront from the Pentagon
 * Worlds.</p>
 *
 * <p>This event triggers when the player's current system falls within {@value #DETECTION_BAND_LY}
 * light-years of the expanding wavefront on September 9th, the anniversary of the original
 * broadcast.</p>
 *
 * @since 0.50.07
 */
public class VoiceOfKerensky {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.VoiceOfKerensky";

    /**
     * The broadcast date: September 9, 2786.
     */
    private static final int BROADCAST_YEAR = 2786;
    private static final int BROADCAST_MONTH = 9;
    private static final int BROADCAST_DAY = 9;

    /**
     * Approximate center of the Pentagon Worlds in map coordinates (light-years).
     * Averaged from Arcadia (Clan), Babylon, Circe, Dagda (Clan), and Eden.
     */
    private static final double PENTAGON_WORLDS_X = -125.491;
    private static final double PENTAGON_WORLDS_Y = 1608.425;

    /**
     * The detection band in light-years. The event can fire when the player's system distance from
     * the Pentagon Worlds is within this many light-years of the current wavefront radius.
     */
    private static final double DETECTION_BAND_LY = 30.0;

    /**
     * Checks whether the Voice of Kerensky event should trigger on the given date for the given
     * campaign location.
     *
     * <p>The event triggers when all of the following conditions are met:</p>
     * <ul>
     *     <li>Today is September 9th (the anniversary of the broadcast)</li>
     *     <li>The campaign year is after the broadcast year (2786)</li>
     *     <li>The player's current system is within the detection band of the wavefront</li>
     * </ul>
     *
     * @param today the current campaign date
     * @param campaign the current campaign
     *
     * @return {@code true} if the event should trigger
     */
    public static boolean shouldTrigger(LocalDate today, Campaign campaign) {
        if (today.getMonthValue() != BROADCAST_MONTH || today.getDayOfMonth() != BROADCAST_DAY) {
            return false;
        }

        int campaignYear = today.getYear();
        if (campaignYear <= BROADCAST_YEAR) {
            return false;
        }

        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        if (currentSystem == null) {
            return false;
        }

        Double systemX = currentSystem.getX();
        Double systemY = currentSystem.getY();
        if (systemX == null || systemY == null) {
            return false;
        }

        double distanceFromPentagonWorlds = Math.sqrt(
              Math.pow(systemX - PENTAGON_WORLDS_X, 2) +
              Math.pow(systemY - PENTAGON_WORLDS_Y, 2));

        double wavefrontRadius = campaignYear - BROADCAST_YEAR;

        double distanceFromWavefront = Math.abs(distanceFromPentagonWorlds - wavefrontRadius);

        return distanceFromWavefront <= DETECTION_BAND_LY;
    }

    /**
     * Displays the Voice of Kerensky easter egg dialog.
     *
     * <p>Shows an immersive dialog where the senior command administrator reports picking up
     * Kerensky's ancient broadcast. The dialog includes the decoded transmission text and an
     * out-of-character explanation of the easter egg.</p>
     *
     * @param campaign the current campaign
     */
    public static void trigger(Campaign campaign) {
        Person speaker = getSpeaker(campaign);

        String commanderAddress = campaign.getCommanderAddress();
        String inCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "voiceOfKerensky.message.ic", commanderAddress);

        PlanetarySystem currentSystem = campaign.getCurrentSystem();
        double distanceFromPentagonWorlds = Math.sqrt(
              Math.pow(currentSystem.getX() - PENTAGON_WORLDS_X, 2) +
              Math.pow(currentSystem.getY() - PENTAGON_WORLDS_Y, 2));
        int yearsInTransit = campaign.getGameYear() - BROADCAST_YEAR;

        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "voiceOfKerensky.message.ooc",
              String.format("%.1f", distanceFromPentagonWorlds),
              String.valueOf(yearsInTransit));

        List<String> buttons = List.of(
              getFormattedTextAt(RESOURCE_BUNDLE, "button.acknowledge"));

        new ImmersiveDialogSimple(campaign,
              speaker,
              null,
              inCharacterMessage,
              buttons,
              outOfCharacterMessage,
              null,
              false,
              ImmersiveDialogWidth.LARGE);
    }

    /**
     * Retrieves the speaker for the dialog.
     *
     * @param campaign the current campaign
     *
     * @return the senior command administrator, or {@code null} if none exists
     */
    private static @Nullable Person getSpeaker(Campaign campaign) {
        return campaign.getSeniorAdminPerson(COMMAND);
    }
}
