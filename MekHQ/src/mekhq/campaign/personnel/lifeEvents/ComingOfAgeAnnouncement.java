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
 */
package mekhq.campaign.personnel.lifeEvents;

import static megamek.codeUtilities.ObjectUtility.getRandomItem;
import static megamek.common.Compute.randomInt;
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
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.familyTree.Genealogy;
import mekhq.campaign.randomEvents.personalities.PersonalityController.PronounData;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

public class ComingOfAgeAnnouncement {
    private static final MMLogger logger = MMLogger.create(ComingOfAgeAnnouncement.class);

    private static String RESOURCE_BUNDLE = "mekhq.resources." + ComingOfAgeAnnouncement.class.getSimpleName();

    private final Campaign campaign;
    private final Person birthdayHaver;
    private SpeakerType speakerType;

    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 3;

    private enum SpeakerType {
        PARENT, OTHER_PARENT, HR_REMINDER, HR_ORPHAN
    }

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
              true);

        //        if (dialog.getDialogChoice() == SUPPRESS_DIALOG_RESPONSE_INDEX) {
        //            CampaignOptions campaignOptions = campaign.getCampaignOptions();
        //            campaignOptions.setShowLifeEventDialogBirths(false);
        //        }
    }

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
              campaign.getCommanderAddress(false));
    }

    private @Nullable Person getSpeaker() {
        Genealogy genealogy = birthdayHaver.getGenealogy();
        Person commander = campaign.getFlaggedCommander();

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

    private @Nullable Person getFallbackSpeaker() {
        Person speaker = campaign.getSeniorAdminPerson(HR);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }

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

    private List<String> getButtonLabels(String birthdayHaverFirstName) {
        return List.of(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.positive", birthdayHaverFirstName),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.neutral"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.negative"),
              getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));
    }
}
