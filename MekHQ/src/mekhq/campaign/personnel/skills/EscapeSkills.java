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
package mekhq.campaign.personnel.skills;

import static java.lang.Math.floor;
import static megamek.common.compute.Compute.d6;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.List;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.InjurySPAUtility;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;
import mekhq.campaign.personnel.skills.enums.MarginOfSuccess;

/**
 * Provides utilities for handling escape attempts by prisoners with the "Acting", "Escape Artist", "Disguise", or
 * "Forgery" skill.
 *
 * <p>This class contains static methods to evaluate the results of escape attempts, apply the consequences (such as
 * altering prisoner status or inflicting injuries), and generate reports about the outcomes. Outcomes are determined
 * based on the margin of success from a skill check, campaign options, and the specifics of the attempt.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class EscapeSkills {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.EscapeArtist";

    private final static int NO_SKILL_AVAILABLE = -1;

    /**
     * Performs an escape attempt skill check for the given person and processes the result.
     *
     * <p>The attempt outcome is determined by performing a skill check and processing the resulting margin of
     * success value.</p>
     *
     * @param campaign the active campaign
     * @param person   the prisoner performing the escape attempt
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void performEscapeAttemptCheck(Campaign campaign, Person person) {
        String skillToUse = getHighestEscapeSkill(person);

        // No attempt is made if the prisoner doesn't have any escape skills.
        if (skillToUse.isBlank()) {
            return;
        }

        LocalDate today = campaign.getLocalDate();
        SkillCheckUtility skillCheckUtility = new SkillCheckUtility(person, skillToUse, List.of(), 0,
              true, false, false, false, today);
        int marginOfSuccessValue = skillCheckUtility.getMarginOfSuccess();
        MarginOfSuccess marginOfSuccess = MarginOfSuccess.getMarginOfSuccessObjectFromMarginValue(marginOfSuccessValue);

        // Nothing happens for these cases, so we can just early exit
        List<MarginOfSuccess> noFurtherActionCases = List.of(MarginOfSuccess.IT_WILL_DO, MarginOfSuccess.BARELY_MADE_IT,
              MarginOfSuccess.ALMOST);
        if (noFurtherActionCases.contains(marginOfSuccess)) {
            return;
        }

        processEscapeAttempt(campaign, person, marginOfSuccess, today);
    }

    /**
     * Determines which escape-related skill the given {@link Person} is best at.
     *
     * <p>This method examines the person's skill levels in Escape Artist, Disguise, Forgery, or Sleight of Hand, and
     * returns the skill type with the highest total skill level. Evaluation uses each skill's
     * {@code getTotalSkillLevel} method, considering personnel options and ATOW attributes.</p>
     *
     * <p>If the person does not possess any of these skills, the method returns an empty string.</p>
     *
     * @param person the {@link Person} whose skills will be checked
     *
     * @return the skill type constant (one of {@link SkillType#S_ACTING}, {@link SkillType#S_ESCAPE_ARTIST},
     *       {@link SkillType#S_DISGUISE}, {@link SkillType#S_FORGERY}, or {@link SkillType#S_SLEIGHT_OF_HAND})
     *       corresponding to the highest escape-related skill level, or an empty string if none are present
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getHighestEscapeSkill(Person person) {
        PersonnelOptions options = person.getOptions();
        Attributes attributes = person.getATOWAttributes();

        int highestSkillLevel = NO_SKILL_AVAILABLE;
        String skillToUse = "";

        Skill escapeArtistSkill = person.getSkill(SkillType.S_ESCAPE_ARTIST);
        if (escapeArtistSkill != null) {
            int level = escapeArtistSkill.getTotalSkillLevel(options, attributes, 0);
            if (level > highestSkillLevel) {
                highestSkillLevel = level;
                skillToUse = SkillType.S_ESCAPE_ARTIST;
            }
        }

        Skill disguiseSkill = person.getSkill(SkillType.S_DISGUISE);
        if (disguiseSkill != null) {
            int level = disguiseSkill.getTotalSkillLevel(options, attributes, 0);
            if (level > highestSkillLevel) {
                highestSkillLevel = level;
                skillToUse = SkillType.S_DISGUISE;
            }
        }

        Skill forgerySkill = person.getSkill(SkillType.S_FORGERY);
        if (forgerySkill != null) {
            int level = forgerySkill.getTotalSkillLevel(options, attributes, 0);
            if (level > highestSkillLevel) {
                highestSkillLevel = level;
                skillToUse = SkillType.S_FORGERY;
            }
        }

        Skill actingSkill = person.getSkill(SkillType.S_ACTING);
        if (actingSkill != null) {
            int level = actingSkill.getTotalSkillLevel(options, attributes, 0);
            if (level > highestSkillLevel) {
                highestSkillLevel = level;
                skillToUse = SkillType.S_ACTING;
            }
        }

        Skill sleightOfHandSkill = person.getSkill(SkillType.S_SLEIGHT_OF_HAND);
        if (sleightOfHandSkill != null) {
            int level = sleightOfHandSkill.getTotalSkillLevel(options, attributes, 0);
            if (level > highestSkillLevel) {
                skillToUse = SkillType.S_SLEIGHT_OF_HAND;
            }
        }

        return skillToUse;
    }

    /**
     * Processes the outcome of an escape attempt based on the calculated margin of success.
     *
     * <p>Updates prisoner status, applies failure consequences, and generates reports as appropriate.</p>
     *
     * @param campaign        the current campaign instance
     * @param prisoner        the {@link Person} attempting escape
     * @param marginOfSuccess the result of the skill check
     * @param today           the current in-game date
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processEscapeAttempt(Campaign campaign, Person prisoner, MarginOfSuccess marginOfSuccess,
          LocalDate today) {
        String report = getEscapeAttemptReport(prisoner, marginOfSuccess);
        if (!report.isBlank()) {
            campaign.addReport(report);
        }

        switch (marginOfSuccess) {
            // Nothing happens for MarginOfSuccess.IT_WILL_DO, MarginOfSuccess.BARELY_MADE_IT, or MarginOfSuccess.ALMOST
            case SPECTACULAR, EXTRAORDINARY, GOOD -> prisoner.changeStatus(campaign, today, PersonnelStatus.ACTIVE);
            case BAD -> getEscapeAttemptReport(prisoner, MarginOfSuccess.BAD);
            case TERRIBLE, DISASTROUS -> {
                boolean wasDisastrous = marginOfSuccess == MarginOfSuccess.DISASTROUS;
                processNotableFailure(campaign, prisoner, wasDisastrous);
            }
        }
    }

    /**
     * Applies consequences for a failed escape attempt, including injuries and possible changes in personnel status for
     * catastrophic failures. Uses campaign options to determine injury handling.
     *
     * @param campaign      the active campaign
     * @param prisoner      the {@link Person} who attempted escape
     * @param wasDisastrous whether the failure was disastrous (affects severity of consequences)
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static void processNotableFailure(Campaign campaign, Person prisoner, boolean wasDisastrous) {
        int roll = wasDisastrous ? d6(2) : d6(1);
        int injuries = (int) floor(roll / 2.0);

        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        InjurySPAUtility.adjustInjuriesAndFatigueForSPAs(prisoner, campaignOptions.isUseInjuryFatigue(),
              campaignOptions.getFatigueRate(), injuries);

        boolean useAdvancedMedical = campaignOptions.isUseAdvancedMedical();
        if (useAdvancedMedical) {
            InjuryUtil.resolveCombatDamage(campaign, prisoner, injuries);
            if (prisoner.getInjuries().size() > 5) {
                prisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.HOMICIDE);
            }
        } else {
            int currentHits = prisoner.getHits();
            int newHits = currentHits + injuries;
            prisoner.setHits(newHits);
            if (newHits > 5) {
                prisoner.changeStatus(campaign, campaign.getLocalDate(), PersonnelStatus.HOMICIDE);
            }
        }

        MekHQ.triggerEvent(new PersonChangedEvent(prisoner));
    }

    /**
     * Retrieves a formatted report for the given prisoner's escape attempt based on the result margin.
     *
     * @param prisoner        the {@link Person} whose escape attempt is being reported
     * @param marginOfSuccess the margin of success result for the escape attempt
     *
     * @return a formatted string for user display or an empty string if no report is generated
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getEscapeAttemptReport(Person prisoner, MarginOfSuccess marginOfSuccess) {
        String reportColor = MarginOfSuccess.getMarginOfSuccessColor(marginOfSuccess);
        String reportKey = "EscapeArtist.report." + marginOfSuccess.name();

        return getFormattedTextAt(RESOURCE_BUNDLE,
              reportKey,
              prisoner.getHyperlinkedFullTitle(),
              spanOpeningWithCustomColor(reportColor),
              CLOSING_SPAN_TAG);
    }
}
