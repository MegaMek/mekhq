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
package mekhq.gui.dialog;

import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_COMBAT;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_RETIREE;
import static mekhq.MHQConstants.NAG_SOMEONE_RANDOMLY_DIED_TECH;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;

import java.time.LocalDate;

import megamek.common.enums.SkillLevel;
import mekhq.MHQConstants;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.InterruptAdvanceMultipleDaysEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogCore;
import mekhq.gui.baseComponents.immersiveDialogs.ImmersiveDialogNag;
import org.jspecify.annotations.NonNull;

/**
 * Displays an immersive notification dialog when a person dies from random causes during campaign advancement and
 * allows the player to cancel, continue, or suppress future notifications of the same category.
 *
 * <p>The dialog presents an in-character eulogy-style report drawn from the deceased's role, skill level, age, and
 * service record, alongside an out-of-character explanation of which nag-suppression constant governs this notification
 * type. The player's choice is then acted upon via {@link #processDialogChoice(int, String)}:</p>
 * <ul>
 *     <li><b>Cancel</b> — fires {@link InterruptAdvanceMultipleDaysEvent} to halt any ongoing Advance Multiple Days
 *     process.</li>
 *     <li><b>Continue</b> — dismisses the dialog without interruption.</li>
 *     <li><b>Suppress</b> — permanently suppresses this notification category and dismisses without interruption.</li>
 * </ul>
 *
 * <p>Death notifications are bucketed into six categories (combat, tech, other support, camp follower, retiree,
 * civilian), each governed by its own nag constant defined in {@code MHQConstants}. Use
 * {@link #getRandomDeathAnnouncementNagConstant(Person)} to resolve the correct constant for a given deceased, and
 * {@link #checkNag(String)} to test whether the player has already suppressed that category before constructing the
 * dialog.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class RandomDeathAnnouncement extends ImmersiveDialogNag {
    private final static String RESOURCE_BUNDLE = "mekhq.resources.RandomDeathAnnouncement";

    private final Campaign campaign;

    /**
     * Constructs and displays the random-death announcement dialog for the given deceased person, then processes the
     * player's response.
     *
     * @param campaign     the active {@link Campaign}; used for date, commander address, and event dispatch
     * @param deceased     the {@link Person} who has died; drives both the portrait shown and the narrative text
     * @param causeOfDeath the {@link PersonnelStatus} describing how the person died; its log-text label is embedded in
     *                     the in-character message
     * @param nagConstant  the nag-suppression key (one of the {@code NAG_SOMEONE_RANDOMLY_DIED_*} constants from
     *                     {@code MHQConstants}) that controls whether this dialog category is shown and can be
     *                     silenced
     *
     * @author Illiani
     * @since 0.51.01
     */
    public RandomDeathAnnouncement(Campaign campaign, Person deceased, PersonnelStatus causeOfDeath,
          String nagConstant) {
        super();
        this.campaign = campaign;

        ImmersiveDialogCore dialog = constructDialog(campaign, deceased, causeOfDeath, nagConstant);
        processDialogChoice(dialog.getDialogChoice(), nagConstant);
    }

    /**
     * Builds the underlying {@link ImmersiveDialogCore} for the death announcement.
     *
     * <p>The dialog uses the deceased person as its speaker avatar, presents in-character and out-of-character text
     * blocks, and exposes the standard nag button set.</p>
     *
     * @param campaign     the active {@link Campaign}
     * @param deceased     the {@link Person} who has died
     * @param causeOfDeath the {@link PersonnelStatus} that caused the death
     * @param nagConstant  the nag-suppression key for this notification category
     *
     * @return a fully constructed, already-displayed {@link ImmersiveDialogCore} whose {@code getDialogChoice()} value
     *       is ready for processing
     *
     * @author Illiani
     * @since 0.51.01
     */
    private ImmersiveDialogCore constructDialog(Campaign campaign, Person deceased, PersonnelStatus causeOfDeath,
          String nagConstant) {
        return new ImmersiveDialogCore(campaign,
              deceased,
              null,
              getInCharacterText(campaign, deceased, causeOfDeath),
              createButtons(),
              getOutOfCharacterText(nagConstant),
              null,
              true,
              null,
              null,
              true);
    }

    /**
     * Composes the in-character eulogy text shown in the dialog body.
     *
     * <p>The message is tailored to whether the deceased was a camp follower or a regular campaign employee and
     * incorporates their profession labels (with colored skill-level suffixes), age, cause of death, and service
     * tenure.</p>
     *
     * @param campaign     the active {@link Campaign}; used to resolve skill levels and service-year calculations
     * @param deceased     the {@link Person} who has died
     * @param causeOfDeath the {@link PersonnelStatus} that caused the death
     *
     * @return a non-null, resource-bundle-formatted HTML string suitable for display in the immersive dialog
     *
     * @author Illiani
     * @since 0.51.01
     */
    private String getInCharacterText(Campaign campaign, Person deceased, PersonnelStatus causeOfDeath) {
        LocalDate today = campaign.getLocalDate();
        String causeOfDeathLabel = causeOfDeath.getLogText();

        String primaryProfessionLabel = getPrimaryProfessionLabel(deceased, campaign);
        String secondaryProfessionLabel = getSecondaryProfessionLabel(deceased, campaign);

        String professions = primaryProfessionLabel +
                                   (secondaryProfessionLabel.isBlank() ? "" : ", " + secondaryProfessionLabel);

        if (deceased.getStatus().isCampFollower()) {
            return getCampFollowerDeathReport(deceased, campaign, causeOfDeathLabel, professions, today);
        } else {
            return getEmployeeDeathReport(deceased, campaign, causeOfDeathLabel, professions, today);
        }
    }

    /**
     * Builds the in-character death report for a camp follower (unemployed) decedent.
     *
     * <p>Camp followers have a shorter report than employees because they lack a formal service record; the message
     * omits years-in-service, XP earnings, and total pay.</p>
     *
     * @param deceased          the {@link Person} who has died
     * @param campaign          the active {@link Campaign}
     * @param causeOfDeathLabel the human-readable log text for the cause of death
     * @param professions       the formatted primary (and optional secondary) role string
     * @param today             the current in-game date
     *
     * @return a non-null formatted string for the dialog's in-character text block
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @NonNull String getCampFollowerDeathReport(Person deceased, Campaign campaign,
          String causeOfDeathLabel, String professions, LocalDate today) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.inCharacter.unemployed",
              deceased.getHyperlinkedFullTitle(),
              causeOfDeathLabel,
              professions,
              deceased.getAge(today),
              deceased.getYearsSinceJoiningCampaign(campaign));
    }

    /**
     * Builds the in-character death report for a regular campaign employee.
     *
     * <p>Employee reports are richer than camp-follower reports, including years of active service, total XP earned,
     * and lifetime earnings.</p>
     *
     * @param deceased          the {@link Person} who has died
     * @param campaign          the active {@link Campaign}
     * @param causeOfDeathLabel the human-readable log text for the cause of death
     * @param professions       the formatted primary (and optional secondary) role string
     * @param today             the current in-game date
     *
     * @return a non-null formatted string for the dialog's in-character text block
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @NonNull String getEmployeeDeathReport(Person deceased, Campaign campaign,
          String causeOfDeathLabel, String professions, LocalDate today) {
        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.inCharacter.employed",
              deceased.getHyperlinkedFullTitle(),
              causeOfDeathLabel,
              professions,
              deceased.getAge(today),
              deceased.getYearsSinceJoiningCampaign(campaign),
              deceased.getYearsInService(campaign),
              deceased.getTotalXPEarnings(),
              deceased.getTotalEarnings().toAmountString());
    }

    /**
     * Returns a display string combining the deceased's primary role description with a colored skill-level label.
     *
     * <p>Example output: {@code "MechWarrior (Regular)"} where "Regular" is rendered in the skill-level color
     * defined by {@link SkillType#getColoredExperienceLevelName}.</p>
     *
     * @param deceased the {@link Person} whose primary role is being described
     * @param campaign the active {@link Campaign}; used to resolve the effective skill level
     *
     * @return a non-null formatted role-and-skill string
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static @NonNull String getPrimaryProfessionLabel(Person deceased, Campaign campaign) {
        String primaryProfession = deceased.getPrimaryRoleDesc();
        SkillLevel primarySkillLevel = deceased.getSkillLevel(campaign, false, true);
        String primarySkillLabel = SkillType.getColoredExperienceLevelName(primarySkillLevel);
        return primaryProfession + " (" + primarySkillLabel + ')';
    }

    /**
     * Returns a display string combining the deceased's secondary role description with a colored skill-level label, or
     * an empty string if no secondary role is assigned.
     *
     * @param deceased the {@link Person} whose secondary role is being described
     * @param campaign the active {@link Campaign}; used to resolve the effective skill level
     *
     * @return a formatted role-and-skill string, or {@code ""} when the secondary role is
     *       {@link PersonnelRole#isNone() none}
     *
     * @author Illiani
     * @since 0.51.01
     */
    private static String getSecondaryProfessionLabel(Person deceased, Campaign campaign) {
        PersonnelRole secondaryRole = deceased.getSecondaryRole();
        if (secondaryRole.isNone()) {
            return "";
        }

        String secondaryProfession = deceased.getSecondaryRoleDesc();
        SkillLevel secondarySkillLevel = deceased.getSkillLevel(campaign, true, true);
        String secondarySkillLabel = SkillType.getColoredExperienceLevelName(secondarySkillLevel);

        return secondaryProfession + " (" + secondarySkillLabel + ')';
    }

    /**
     * Builds the out-of-character text for the dialog, explaining which suppression category this notification belongs
     * to and how the player can silence it permanently.
     *
     * @param nagConstant the nag-suppression key used to look up the category-specific message in the resource bundle
     *
     * @return a formatted string for the dialog's out-of-character text block
     *
     * @author Illiani
     * @since 0.51.01
     */
    private String getOutOfCharacterText(String nagConstant) {
        String messageKey = "RandomDeathAnnouncement.outOfCharacter";
        String classificationKey = messageKey + '.' + nagConstant;
        return getFormattedTextAt(RESOURCE_BUNDLE, "RandomDeathAnnouncement.outOfCharacter", classificationKey);
    }

    /**
     * Returns {@code true} if the nag dialog for the given constant has <em>not</em> been suppressed by the player,
     * meaning the dialog should be shown.
     *
     * <p>Callers should invoke this before constructing a {@link RandomDeathAnnouncement} to avoid building a dialog
     * the player has already chosen to silence.</p>
     *
     * @param nagConstant the nag-suppression key to check (one of the {@code NAG_SOMEONE_RANDOMLY_DIED_*} constants)
     *
     * @return {@code true} if the dialog should be displayed; {@code false} if the player has permanently suppressed
     *       this category
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean checkNag(String nagConstant) {
        return !MekHQ.getMHQOptions().getNagDialogIgnore(nagConstant);
    }

    /**
     * Acts on the player's button selection from the death-announcement dialog.
     *
     * <ul>
     *     <li>{@link DialogChoice#CHOICE_CANCEL} — fires {@link InterruptAdvanceMultipleDaysEvent} to halt any
     *     ongoing Advance Multiple Days process.</li>
     *     <li>{@link DialogChoice#CHOICE_CONTINUE} — sets {@code cancelAdvanceDay} to {@code false}, allowing
     *     advancement to proceed uninterrupted.</li>
     *     <li>{@link DialogChoice#CHOICE_SUPPRESS} — permanently suppresses this nag category via
     *     {@link MekHQ#getMHQOptions()} and then continues as above.</li>
     * </ul>
     *
     * @param choiceIndex the zero-based index of the button the player clicked
     * @param nagConstant the nag-suppression key to persist when the player chooses to suppress
     *
     * @throws IllegalStateException if {@code choiceIndex} does not map to a known {@link DialogChoice}
     * @author Illiani
     * @since 0.51.01
     */
    @Override
    protected void processDialogChoice(int choiceIndex, String nagConstant) {
        DialogChoice choice = DialogChoice.fromIndex(choiceIndex);

        switch (choice) {
            case CHOICE_CANCEL -> MekHQ.triggerEvent(new InterruptAdvanceMultipleDaysEvent(campaign));
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
     * Resolves the appropriate nag-suppression constant for a given deceased person based on their status and role at
     * the time of death.
     *
     * <p>The resolution order is:</p>
     * <ol>
     *     <li>Camp follower ({@link MHQConstants#NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER})</li>
     *     <li>Retiree ({@link MHQConstants#NAG_SOMEONE_RANDOMLY_DIED_RETIREE})</li>
     *     <li>Combat role ({@link MHQConstants#NAG_SOMEONE_RANDOMLY_DIED_COMBAT})</li>
     *     <li>Tech role ({@link MHQConstants#NAG_SOMEONE_RANDOMLY_DIED_TECH})</li>
     *     <li>Other support role ({@link MHQConstants#NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT})</li>
     *     <li>Civilian/everything else ({@link MHQConstants#NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN})</li>
     * </ol>
     *
     * @param deceased the {@link Person} who has died
     *
     * @return the matching nag-suppression constant string from {@code MHQConstants}
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getRandomDeathAnnouncementNagConstant(Person deceased) {
        return switch (deceased) {
            case Person d when d.getStatus().isCampFollower() -> NAG_SOMEONE_RANDOMLY_DIED_CAMP_FOLLOWER;
            case Person d when d.getStatus().isRetired() -> NAG_SOMEONE_RANDOMLY_DIED_RETIREE;
            case Person d when d.isCombat() -> NAG_SOMEONE_RANDOMLY_DIED_COMBAT;
            case Person d when d.isTechExpanded() -> NAG_SOMEONE_RANDOMLY_DIED_TECH;
            case Person d when d.isSupport() -> NAG_SOMEONE_RANDOMLY_DIED_OTHER_SUPPORT;
            default -> NAG_SOMEONE_RANDOMLY_DIED_CIVILIAN;
        };
    }
}
