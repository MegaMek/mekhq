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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.swing.ImageIcon;

import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * This class manages the immersive announcement for Freedom Day. Freedom Day is a special event celebrated annually
 * between the years 3131 and 3151 on March 18th. The class generates and displays announcements with in-character (IC)
 * and out-of-character (OOC) messages, provides options for user interaction, and allows users to suppress the
 * announcement for future occasions.
 *
 * <p>The announcement uses campaign context, selecting an appropriate speaker and presenting buttons
 * for user choices in an interactive dialog.</p>
 *
 * @since 0.50.05
 */
public record FreedomDayAnnouncement(Campaign campaign) {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.FreedomDayAnnouncement";

    // Constants for significant holiday-related dates.
    private final static int FREEDOM_DAY_START_YEAR = 3131;
    private final static int FREEDOM_DAY_END_YEAR = 3151;
    private final static int FREEDOM_DAY_MONTH = 3;
    private final static int FREEDOM_DAY_DAY = 18;

    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 3;

    /**
     * Constructs a new {@code FreedomDayAnnouncement}.
     *
     * <p>This initializes the announcement by creating immersive dialog messages. The dialog consists of
     * in-character (IC) content, out-of-character (OOC) notes, user-selectable options, and the ability to suppress
     * future announcements. Campaign context is used to generate appropriate content.</p>
     *
     * @param campaign the {@link Campaign} instance that provides the relevant context for the announcement
     */
    public FreedomDayAnnouncement(Campaign campaign) {
        this.campaign = campaign;
        Person commander = campaign.getCommander();

        String inCharacterMessage = getInCharacterMessage();
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "freedomDay.message.ooc");

        Person speaker = getSpeaker(commander);
        if (speaker == null) {
            // If there is no one to celebrate, we don't display the celebration
            return;
        }

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(commander),
              null,
              inCharacterMessage,
              getButtonLabels(),
              outOfCharacterMessage,
              new ImageIcon("data/images/universe/factions/logo_republic_of_the_sphere.png"),
              true);

        if (dialog.getDialogChoice() == SUPPRESS_DIALOG_RESPONSE_INDEX) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            campaignOptions.setShowLifeEventDialogCelebrations(false);
        }
    }

    /**
     * Generates and retrieves the in-character (IC) message for the announcement. This message is personalized using
     * the campaign's commander information.
     *
     * @return the generated IC message as a {@link String}
     */
    private String getInCharacterMessage() {
        String commanderAddress = campaign.getCommanderAddress();
        return getFormattedTextAt(RESOURCE_BUNDLE, "freedomDay.message.ic", commanderAddress);
    }

    /**
     * Selects a {@link Person} to act as the speaker for the event, prioritizing personnel belonging to the Republic of
     * The Sphere (identified by the "ROS" short name). If no personnel from the prioritized faction are eligible, the
     * method selects randomly from the remaining active personnel if the commander belongs to the "ROS" faction.
     *
     * <p>The selection process filters out certain personnel based on a series of conditions:</p>
     *
     * <ul>
     *     <li>The person must not be the flagged commander.</li>
     *     <li>The person must be free (not a prisoner) or a bondsman.</li>
     *     <li>The person must have active status.</li>
     *     <li>The person must not be classified as a civilian.</li>
     * </ul>
     *
     * <p>Personnel meeting these conditions are split into two pools:</p>
     *
     * <ul>
     *     <li><b>Faction Pool:</b> Personnel originating from the "ROS" faction.</li>
     *     <li><b>Active Pool:</b> Personnel from other factions.</li>
     * </ul>
     *
     * <p>The selection prioritizes personnel in the faction pool, falling back to the active pool if no faction matches
     * are available. Additional handling is applied for scenarios involving commanders originating from the "ROS"
     * faction, including:</p>
     *
     * <ul>
     *     <li>If there is no commander set ({@code commander} is {@code null}), the method returns {@code null}.</li>
     *     <li>If the commander originates from the "ROS" faction, a random person is selected from the active pool,
     *     regardless of the lack of faction pool candidates.</li>
     * </ul>
     *
     * @param commander the {@link Person} designated as the campaign's commander, or {@code null} if the commander is
     *                  unknown or not applicable
     *
     * @return a randomly selected {@link Person} from the eligible pool, or {@code null} if no suitable candidates
     *       exist
     */
    private @Nullable Person getSpeaker(@Nullable Person commander) {
        List<Person> factionPool = new ArrayList<>();
        List<Person> activePool = new ArrayList<>();

        for (Person person : campaign.getPersonnel()) {
            if (isIneligible(commander, person)) {
                continue;
            }

            if (Objects.equals(person.getOriginFaction().getShortName(), "ROS")) {
                factionPool.add(person);
            } else {
                activePool.add(person);
            }
        }

        if (factionPool.isEmpty() && activePool.isEmpty()) {
            return null;
        }

        if (!factionPool.isEmpty()) {
            return getRandomItem(factionPool);
        }

        // If the commander is from the Republic of the Sphere, their personnel may still wish them well, despite not
        // celebrating themselves.
        if (commander == null) {
            return null;
        }

        if (commander.getOriginFaction().getShortName().equals("ROS")) {
            return getRandomItem(activePool);
        }

        return null;
    }

    /**
     * Determines if a {@link Person} is ineligible to be a speaker, based on predefined criteria.
     *
     * <p>A person is ineligible if they:</p>
     * <ul>
     *     <li>Are the flagged commander</li>
     *     <li>Are not free or acting as a bondsman</li>
     *     <li>Are not active</li>
     *     <li>Are classified as a civilian</li>
     * </ul>
     *
     * @param commander the {@link Person} designated as the commander
     * @param person    the {@link Person} being checked
     *
     * @return {@code true} if the person is ineligible, {@code false} if they meet the criteria to be selected
     */
    private static boolean isIneligible(Person commander, Person person) {
        return Objects.equals(person, commander) ||
                     !person.getPrisonerStatus().isFreeOrBondsman() ||
                     !person.getStatus().isActive() ||
                     person.getPrimaryRole().isCivilian();
    }

    /**
     * Generates and retrieves a list of button labels for user interaction in the announcement dialog. These labels
     * correspond to user choices, such as positive, neutral, or negative responses, and an option to suppress future
     * announcements.
     *
     * @return a {@link List} of button label strings
     */
    private List<String> getButtonLabels() {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.positive"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.neutral"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.negative"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));
    }


    /**
     * Determines whether a given {@link LocalDate} corresponds to Freedom Day for the campaign. Freedom Day is observed
     * annually between the years 3131 and 3151 on March 18th.
     *
     * @param date the {@link LocalDate} to check
     *
     * @return {@code true} if the date is Freedom Day; {@code false} otherwise
     */
    public static boolean isFreedomDay(LocalDate date) {
        int year = date.getYear();
        if (year < FREEDOM_DAY_START_YEAR || year > FREEDOM_DAY_END_YEAR) {
            return false;
        }

        if (date.getMonthValue() != FREEDOM_DAY_MONTH) {
            return false;
        }

        return date.getDayOfMonth() == FREEDOM_DAY_DAY;
    }
}
