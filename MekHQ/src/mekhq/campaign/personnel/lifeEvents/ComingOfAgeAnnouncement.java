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
import static megamek.common.compute.Compute.randomInt;
import static megamek.common.enums.Gender.FEMALE;
import static megamek.common.enums.Gender.MALE;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Handles the coming-of-age announcement for a character.
 *
 * <p>This class is responsible for generating both in-character and out-of-character messages, determining the
 * speaker for the announcement, and displaying the dialog to the user. The announcement is tailored based on the
 * settings, genealogy, and status of the involved characters in the campaign.</p>
 *
 * <p>Announcements prioritize parents as speakers but fallback to HR or other administrators if needed. Dialog
 * choices may affect the campaign options, such as suppressing future life event dialogs.</p>
 *
 * @since MekHQ 0.50.05
 */
public class ComingOfAgeAnnouncement {
    private static final MMLogger logger = MMLogger.create(ComingOfAgeAnnouncement.class);

    private static String RESOURCE_BUNDLE = "mekhq.resources.ComingOfAgeAnnouncement";

    private final Campaign campaign;
    private final Person birthdayHaver;
    private SpeakerType speakerType;

    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 3;

    private enum SpeakerType {
        PARENT, OTHER_PARENT, HR_REMINDER, HR_ORPHAN
    }


    /**
     * Constructs and initializes a coming-of-age announcement dialog.
     *
     * <p>During initialization, the speaker for the announcement is determined, messages are generated, and the
     * immersive dialog is displayed to the user. User responses are processed to possibly adjust campaign options.</p>
     *
     * @param campaign      the {@link Campaign} instance managing the event. Provides context such as the commander,
     *                      genealogy, and personnel data needed for processing.
     * @param birthdayHaver the {@link Person} who is "coming of age". This individual is the subject of the
     *                      announcement.
     */
    public ComingOfAgeAnnouncement(Campaign campaign, Person birthdayHaver) {
        this.campaign = campaign;
        this.birthdayHaver = birthdayHaver;

        Person speaker = getSpeaker();
        String birthdayHaverFirstName = birthdayHaver.getFirstName();
        String inCharacterMessage = getInCharacterMessage(birthdayHaverFirstName);
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE,
              "comingOfAge.message.ooc",
              birthdayHaver.getHyperlinkedFullTitle());

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              speaker,
              birthdayHaver,
              inCharacterMessage,
              getButtonLabels(birthdayHaverFirstName),
              outOfCharacterMessage,
              null,
              true);

        if (dialog.getDialogChoice() == SUPPRESS_DIALOG_RESPONSE_INDEX) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            campaignOptions.setShowLifeEventDialogComingOfAge(false);
        }
    }

    /**
     * Generates the in-character narrative text for the coming-of-age announcement.
     *
     * <p>The message is built dynamically by considering the attributes of the birthday haver, such as their first
     * name, gender, and title. It also incorporates the speaker's context (e.g., parent, HR representative) to deliver
     * a personalized message.</p>
     *
     * @param birthdayHaverFirstName the first name of the person coming of age. This is used for personalization within
     *                               the message.
     *
     * @return a formatted string containing the in-character announcement message.
     */
    private String getInCharacterMessage(String birthdayHaverFirstName) {
        // Birthday Haver Data
        Gender birthdayHaverGender = birthdayHaver.getGender();
        PronounData birthdayHaverPronounData = new PronounData(birthdayHaverGender);
        String birthdayHaverTitle = getGenderedTitle(birthdayHaverGender);
        String birthdayHaverFullTitle = birthdayHaver.getFullTitle();

        // Build the in character message
        String resourceKey = "comingOfAge.message." + switch (speakerType) {
            case PARENT -> randomInt(50) + ".fromParent.ic";
            case OTHER_PARENT -> randomInt(10) + ".reminder.ic";
            case HR_REMINDER -> randomInt(10) + ".hrReminder.ic";
            case HR_ORPHAN -> randomInt(10) + ".noParents.ic";
        };

        // {0} Gender Neutral = 0, Otherwise 1 (used to determine whether to use plural case)
        // {1} Full name
        // {2} First name
        // {3} Gendered Title
        // {4} He/She/They
        // {5} he/she/they
        // {6} him/her/them
        // {7} his/her/their
        // {8} commander address
        return getFormattedTextAt(RESOURCE_BUNDLE,
              resourceKey,
              birthdayHaverPronounData.pluralizer(),
              birthdayHaverFullTitle,
              birthdayHaverFirstName,
              birthdayHaverTitle,
              birthdayHaverPronounData.subjectPronoun(),
              birthdayHaverPronounData.subjectPronounLowerCase(),
              birthdayHaverPronounData.objectPronounLowerCase(),
              birthdayHaverPronounData.possessivePronounLowerCase(),
              campaign.getCommanderAddress());
    }

    /**
     * Determines the most appropriate speaker for the coming-of-age announcement.
     *
     * <p>The speaker is selected based on the birthday haver’s genealogy, present and active parents, and campaign
     * context.</p>
     *
     * @return the selected {@link Person} who will act as the speaker, or {@code null} if no suitable speaker can be
     *       found.
     */
    private @Nullable Person getSpeaker() {
        Genealogy genealogy = birthdayHaver.getGenealogy();
        Person commander = campaign.getCommander();

        if (genealogy == null) {
            logger.debug("No genealogy found for {}. Using fallback speaker.", birthdayHaver.getFullName());
            return getFallbackSpeaker();
        }

        List<Person> parents = genealogy.getParents();
        List<Person> presentParents = new ArrayList<>();

        for (Person parent : parents) {
            if (parent == null) {
                logger.debug("Null parent found for {}. Skipping.", birthdayHaver.getFullName());
                continue;
            }

            PersonnelStatus parentStatus = parent.getStatus();

            if (parentStatus.isDepartedUnit() || parentStatus.isAbsent()) {
                continue;
            }

            presentParents.add(parent);
        }

        if (presentParents.isEmpty()) {
            speakerType = SpeakerType.HR_ORPHAN;
            return getFallbackSpeaker();
        } else {
            if (commander != null) {
                if (commander.equals(presentParents.get(0))) {
                    speakerType = SpeakerType.OTHER_PARENT;
                    return presentParents.get(0);
                } else if ((presentParents.size() > 1) && commander.equals(presentParents.get(1))) {
                    speakerType = SpeakerType.OTHER_PARENT;
                    return presentParents.get(1);
                }
            }
        }

        Person speaker = getRandomItem(presentParents);

        // This means only one object is in the pool, and it's the campaign commander. They shouldn't message
        // themselves, so instead HR (or COMMAND) sends them a reminder.
        if (Objects.equals(speaker, commander)) {
            speakerType = SpeakerType.HR_ORPHAN;
            return getFallbackSpeaker();
        }

        speakerType = SpeakerType.PARENT;
        return speaker;
    }

    /**
     * Provides a fallback speaker if no parents or suitable personnel are found.
     *
     * <p>The fallback speaker is determined from the campaign's available administrators. Priority is given to HR
     * personnel, with a secondary fallback to the senior COMMAND character.</p>
     *
     * @return the fallback {@link Person} to act as the speaker, or {@code null} if no fallback is available.
     */
    private @Nullable Person getFallbackSpeaker() {
        Person speaker = campaign.getSeniorAdminPerson(HR);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }

    /**
     * Retrieves a gender-appropriate title for the birthday haver.
     *
     * <p>The title is generated based on the birthday haver's gender and localized using resource bundle keys.
     * Titles are categorized as:</p>
     * <ul>
     *   <li>Neutral</li>
     *   <li>Female</li>
     *   <li>Male</li>
     * </ul>
     *
     * @param gender the {@link Gender} of the person coming of age.
     *
     * @return a localized and formatted string representing the title.
     */
    private static String getGenderedTitle(Gender gender) {
        String titleKey = "title.";

        if (gender.isGenderNeutral()) {
            titleKey = titleKey + "neutral";
        } else if (gender == FEMALE) {
            titleKey = titleKey + "female";
        } else if (gender == MALE) {
            titleKey = titleKey + "male";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, titleKey);
    }

    /**
     * Generates the button labels for the immersive announcement dialog.
     *
     * <p>These labels include positive, neutral, negative, and suppress responses, ailored to the birthday haver's
     * first name and localized based on the resource bundle.</p>
     *
     * @param birthdayHaverFirstName the first name of the birthday haver, used for personalizing button labels.
     *
     * @return a list of strings representing the localized labels for the dialog buttons.
     */
    private List<String> getButtonLabels(String birthdayHaverFirstName) {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.positive", birthdayHaverFirstName),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.neutral"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.negative"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));
    }
}
