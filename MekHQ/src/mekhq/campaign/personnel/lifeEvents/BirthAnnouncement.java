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
import static megamek.common.enums.Gender.FEMALE;
import static megamek.common.enums.Gender.MALE;
import static mekhq.campaign.Campaign.AdministratorSpecialization.COMMAND;
import static mekhq.campaign.Campaign.AdministratorSpecialization.HR;
import static mekhq.campaign.personnel.enums.PersonnelRole.BATTLE_ARMOUR;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static org.apache.commons.text.WordUtils.capitalize;

import java.util.ArrayList;
import java.util.List;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PronounData;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.universe.Faction;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogSimple;

/**
 * Handles the creation of birth announcements.
 *
 * <p>This class generates and displays the in-character and out-of-character messages for a character's childbirth
 * event, incorporating campaign context and personalized pronoun and title handling for both parent and child.</p>
 */
public class BirthAnnouncement {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.BirthAnnouncement";

    private final Campaign campaign;
    private final Person parent;

    private final static double AVERAGE_BABY_WEIGHT_IN_KG = 3.5;
    private final static int SUPPRESS_DIALOG_RESPONSE_INDEX = 2;


    /**
     * Constructs a birth announcement for a given campaign, parent, and baby gender.
     *
     * @param campaign   The campaign context providing data like faction and active personnel.
     * @param parent     The parent {@link Person} giving birth, whose details are used in the announcement.
     * @param babyGender The gender of the newborn.
     */
    public BirthAnnouncement(Campaign campaign, Person parent, Gender babyGender, int babyCount) {
        this.campaign = campaign;
        this.parent = parent;

        String parentFirstName = parent.getFirstName();
        String inCharacterMessage = getInCharacterMessage(babyGender, babyCount, parentFirstName);
        String outOfCharacterMessage = getFormattedTextAt(RESOURCE_BUNDLE, "babyBorn.message.ooc", parentFirstName);

        ImmersiveDialogSimple dialog = new ImmersiveDialogSimple(campaign,
              getSpeaker(campaign),
              parent,
              inCharacterMessage,
              getButtonLabels(parentFirstName),
              outOfCharacterMessage,
              null,
              true);

        if (dialog.getDialogChoice() == SUPPRESS_DIALOG_RESPONSE_INDEX) {
            CampaignOptions campaignOptions = campaign.getCampaignOptions();
            campaignOptions.setShowLifeEventDialogBirths(false);
        }
    }

    /**
     * Generates the in-character message for a baby's birth announcement.
     *
     * <p>This method constructs the message using localized resource keys and various contextual details, such as
     * the baby's gender, the number of babies born, the parent's details, and campaign-specific data (e.g., faction and
     * personnel roles). Pronoun-specific and role-specific titles are dynamically incorporated into the message .</p>
     *
     * @param babyGender      The {@link Gender} of the baby-babies, used for gender-specific pronouns and titles.
     * @param babyCount       The number of babies born, which determines the appropriate message key:
     *                        <ul>
     *                            <li>{@code 1}: Single baby message (randomly selects from 50 variants).</li>
     *                            <li>{@code 2}: Twins message (randomly selects from 10 variants).</li>
     *                            <li>{@code 3}: Triplets message (randomly selects from 5 variants).</li>
     *                            <li>{@code 4+}: Quadruplets or more, using a generic message.</li>
     *                        </ul>
     * @param parentFirstName The first name of the parent, included in the message.
     *
     * @return A localized string containing the in-character birth announcement message.
     */
    private String getInCharacterMessage(Gender babyGender, int babyCount, String parentFirstName) {
        // Campaign Data
        Faction campaignFaction = campaign.getFaction();

        // Parent Data
        Gender parentGender = parent.getGender();
        String parentFullTitle = parent.getHyperlinkedFullTitle();
        PersonnelRole primaryRole = parent.getPrimaryRole();

        PronounData parentPronounData = new PronounData(parentGender);
        String parentTitle = getGenderedTitle("babyBorn.parentTitle.", parentGender);

        // Child Data
        PronounData childPronounData = new PronounData(babyGender);
        String childTitle = getGenderedTitle("babyBorn.childTitle.", babyGender);
        String lanceLabel = getLanceLabel(campaignFaction, primaryRole);

        // Build the in character message
        String resourceKey = switch (babyCount) {
            case 1 -> "babyBorn.message." + randomInt(50) + ".ic";
            case 2 -> "babyBorn.message." + randomInt(10) + ".twins.ic";
            case 3 -> "babyBorn.message." + randomInt(5) + ".triplets.ic";
            default -> "babyBorn.message.quadrupletsPlus.ic";
        };

        // {0} Parent Gender Neutral = 0, Otherwise 1 (used to determine whether to use plural case)
        // {1} Delivery Time

        // {2} Parent Full Name
        // {3} Parent First Name
        // {4} Parent Title (Capitalized)
        // {5} Parent Title
        // {6} Parent He/She/They
        // {7} Parent he/she/they
        // {8} Parent him/her/them
        // {9} Parent his/her/their

        // {10} Baby Gendered Title
        // {11} Baby He/She/They
        // {12} Baby he/she/they
        // {13} Baby him/her/them
        // {14} Baby his/her/their
        // {15} Baby Weight
        // {16} Baby Gender Neutral = 0, Otherwise 1 (used to determine whether to use plural case)

        // {17} Faction Lance-Level label (lance, star, etc.)
        // {18} Baby Count
        return getFormattedTextAt(RESOURCE_BUNDLE,
              resourceKey,
              parentPronounData.pluralizer(),
              getRandomTimeOfDay(),
              parentFullTitle,
              parentFirstName,
              capitalize(parentTitle),
              parentTitle,
              parentPronounData.subjectPronoun(),
              parentPronounData.subjectPronounLowerCase(),
              parentPronounData.objectPronounLowerCase(),
              parentPronounData.possessivePronounLowerCase(),
              childTitle,
              childPronounData.subjectPronoun(),
              childPronounData.subjectPronounLowerCase(),
              childPronounData.objectPronounLowerCase(),
              childPronounData.possessivePronounLowerCase(),
              getBabyWeightInOunces(),
              childPronounData.pluralizer(),
              lanceLabel,
              babyCount);
    }

    /**
     * Retrieves the appropriate speaker for a campaign dialog based on {@link PersonnelRole}.
     *
     * <p>This method evaluates the active personnel within the campaign to determine the most suitable speaker.
     * It prioritizes personnel with doctor roles, using rank and skills to select the optimal candidate. If no medical
     * specialist is available, the method falls back to senior administrators with the "HR" or "COMMAND"
     * specialization, ensuring a valid speaker is selected whenever possible.</p>
     *
     * <p>If there are no active personnel available, a fallback mechanism is employed to determine the speaker based
     * on senior administrators.</p>
     *
     * @param campaign The {@link Campaign} instance providing access to personnel data.
     *
     * @return The {@link Person} designated as the speaker, prioritizing medical specialists, then senior
     *       administrators with "HR" or "COMMAND" specializations. Returns {@code null} if no suitable speaker can be
     *       found.
     */
    private @Nullable Person getSpeaker(Campaign campaign) {
        List<Person> potentialSpeakers = campaign.getActivePersonnel(false);

        if (potentialSpeakers.isEmpty()) {
            return getFallbackSpeaker(campaign);
        }

        Person speaker = null;

        for (Person person : potentialSpeakers) {
            if (!person.isDoctor()) {
                continue;
            }

            if (speaker == null) {
                speaker = person;
                continue;
            }

            if (person.outRanksUsingSkillTiebreaker(campaign, speaker)) {
                speaker = person;
            }
        }

        // First fallback
        if (speaker == null) {
            return getFallbackSpeaker(campaign);
        } else {
            return speaker;
        }
    }

    /**
     * Retrieves a fallback speaker based on senior administrators within the campaign.
     *
     * <p>This method attempts to retrieve a senior administrator with the "HR" specialization first.
     * If no such administrator is available, it falls back to one with the "COMMAND" specialization.</p>
     *
     * @param campaign The {@link Campaign} instance providing access to administrator data.
     *
     * @return The {@link Person} designated as the fallback speaker. Returns {@code null} if no suitable administrator
     *       is available.
     */
    private @Nullable Person getFallbackSpeaker(Campaign campaign) {
        Person speaker = campaign.getSeniorAdminPerson(HR);

        if (speaker == null) {
            speaker = campaign.getSeniorAdminPerson(COMMAND);
        } else {
            return speaker;
        }

        return speaker;
    }

    /**
     * Retrieves a gendered title based on the provided {@code titleKey} and {@code gender}.
     *
     * <p>The key is adjusted to a gender-specific version by appending "neutral", "male", or "female" based on the
     * provided gender.</p>
     *
     * @param titleKey The base key for the title in the resource bundle.
     * @param gender   The gender of the character, used to determine the gendered variation.
     *
     * @return The gendered title retrieved from the resource bundle.
     */
    private static String getGenderedTitle(String titleKey, Gender gender) {
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
     * Retrieves the label for a lance or equivalent formation for the given faction and role.
     *
     * <p>For infantry roles (e.g., soldier or battle armor), this method returns squad-related labels. Other roles
     * return lance-related labels.</p>
     *
     * @param campaignFaction The faction of the campaign.
     * @param primaryRole     The primary role of the parent.
     *
     * @return A localized label describing the lance (or equivalent unit) based on faction and role.
     */
    private static String getLanceLabel(Faction campaignFaction, PersonnelRole primaryRole) {
        String formationKey;
        if (primaryRole == SOLDIER || primaryRole == BATTLE_ARMOUR) {
            formationKey = "squad";
        } else {
            formationKey = "lance";
        }

        String factionKey;
        if (campaignFaction.isClan()) {
            factionKey = "clan";
        } else if (campaignFaction.isComStarOrWoB()) {
            factionKey = "comStar";
        } else {
            factionKey = "innerSphere";
        }

        return getFormattedTextAt(RESOURCE_BUNDLE, formationKey + '.' + factionKey).toLowerCase();
    }

    /**
     * Calculates and returns the weight of the newborn in pounds as a string. The value is rounded conditionally to 0,
     * 1, or 2 decimal places based on the weight.
     *
     * @return A string representation of the newborn's weight in pounds with conditional decimal rounding.
     */
    private static String getBabyWeightInOunces() {
        boolean isBabyHeavy = randomInt(2) == 0;
        double weightVariance = (double) randomInt(10) / 10;

        double weight = AVERAGE_BABY_WEIGHT_IN_KG;
        if (isBabyHeavy) {
            weight += weightVariance;
        } else {
            weight -= weightVariance;
        }

        return String.valueOf(weight);
    }

    /**
     * <p>A random time will be generated with 24-hour format hours and minutes.</p>
     *
     * @return A string representing a random time of day.
     */
    private static String getRandomTimeOfDay() {
        int randomHours = randomInt(24);
        int randomMinutes = randomInt(60);

        return String.format("%02d:%02d", randomHours, randomMinutes);
    }

    /**
     * Retrieves a list of localized button labels for the birth announcement dialog.
     *
     * <p>The labels represent different user responses to the birth announcement, such as expressing
     * a positive reaction, a neutral reaction incorporating the parent's first name, a negative reaction, or an option
     * to suppress future dialogs of this type.</p>
     *
     * @param parentFirstName The first name of the parent, used in the neutral response button label.
     *
     * @return A {@link List} of localized strings containing the button labels.
     */
    private List<String> getButtonLabels(String parentFirstName) {
        List<String> buttonLabels = new ArrayList<>();

        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.positive"));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.neutral", parentFirstName));
        buttonLabels.add(getFormattedTextAt(RESOURCE_BUNDLE, "button.response.suppress"));

        return buttonLabels;
    }
}
