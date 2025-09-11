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
package mekhq.campaign.personnel;

import static java.lang.Math.ceil;
import static megamek.common.compute.Compute.d6;
import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;
import static mekhq.campaign.personnel.enums.BloodmarkLevel.BLOODMARK_ZERO;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.ReportingUtilities.CLOSING_SPAN_TAG;
import static mekhq.utilities.ReportingUtilities.getNegativeColor;
import static mekhq.utilities.ReportingUtilities.getPositiveColor;
import static mekhq.utilities.ReportingUtilities.getWarningColor;
import static mekhq.utilities.ReportingUtilities.spanOpeningWithCustomColor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import megamek.client.generator.RandomCallsignGenerator;
import megamek.logging.MMLogger;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.events.persons.PersonChangedEvent;
import mekhq.campaign.personnel.enums.BloodmarkLevel;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.personnel.medical.advancedMedical.InjuryUtil;

/**
 * Handles Bloodmark-related logic for characters, including assassination attempt scheduling and execution.
 *
 * <p>A Bloodmark in MekHQ represents a standing bounty or vendetta against a person, triggering periodic
 * assassination attempts ("bloodhunts") against them based on their {@link BloodmarkLevel}.</p>
 *
 * <p>This class provides static utility methods to:</p>
 * <ul>
 *     <li>Generate future dates for possible bloodhunts depending on bloodmark severity</li>
 *     <li>Check whether an attempt should occur on a given date and whether the target is eligible</li>
 *     <li>Execute an assassination attempt: calculating results, inflicting wounds or injuries, and updating reports
 *     and character status accordingly</li>
 *     <li>Handle special modifiers like the Toughness or Glass Jaw traits, advanced vs. basic medical handling, and
 *     generate appropriately formatted feedback for the campaign event log</li>
 * </ul>
 *
 * <p>The class also encapsulates internal utility logic for wounds calculation, bounty hunter name generation, and
 * formatting of result reports.</p>
 *
 * @author Illiani
 * @since 0.50.07
 */
public class Bloodmark {
    private static final MMLogger LOGGER = MMLogger.create(Bloodmark.class);
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Bloodmark";

    /** A default name assigned to the bounty hunter when a randomly generated name is unavailable. */
    private static final String DEFAULT_BOUNTY_HUNTER_NAME = "DOG";

    /**
     * Generates a schedule of future dates for assassination attempts ("bloodhunts") based on the provided
     * {@link BloodmarkLevel} and current date.
     *
     * <p>The frequency and number of attempts are determined by the properties of the given level. If no bloodhunt
     * is active or parameters indicate none should occur, returns an empty list. Each returned {@link LocalDate}
     * represents a scheduled day for an attempt, spaced randomly by 1d6 days.</p>
     *
     * <p><b>Usage:</b> this method should be run once a month, per character with a {@link BloodmarkLevel} greater
     * than 0.</p>
     *
     * @param level the {@link BloodmarkLevel} used to determine scheduling and attempt frequency
     * @param today the starting date from which to schedule the bloodhunt attempts
     *
     * @return a list of {@link LocalDate} objects, one for each scheduled assassination attempt; or an empty list if no
     *       bloodhunt should be scheduled
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static List<LocalDate> getBloodhuntSchedule(final BloodmarkLevel level, LocalDate today,
          boolean characterHasAlternativeIDSPA) {
        LocalDate currentDate = today;

        // How often do bloodhunts occur?
        int frequency = level.getRollFrequency();

        if (characterHasAlternativeIDSPA) {
            frequency *= 2;
        }

        if (frequency == 0) {
            return List.of();
        }

        // Is there an active bloodhunt?
        int bloodhuntRoll = randomInt(frequency);
        boolean isBloodhuntActive = bloodhuntRoll == 0;
        if (!isBloodhuntActive) {
            return List.of();
        }

        // How many assassination attempts should occur?
        int divisor = level.getRollDivisor();
        if (divisor == 0) {
            return List.of();
        }

        int assassinationAttempts = d6(1) / divisor; // The loss of precision here is deliberate

        // Schedule the assassination attempts (this is very polite of the assassins)
        List<LocalDate> assassinationSchedule = new ArrayList<>();
        for (int attempt = 0; attempt < assassinationAttempts; attempt++) {
            int lag = d6(1);
            currentDate = currentDate.plusDays(lag); // We don't want multiple attempts on the same day
            assassinationSchedule.add(currentDate);
        }

        return assassinationSchedule;
    }

    /**
     * Checks whether an assassination attempt should be triggered for the specified person on the given date.
     *
     * <p>This method examines the person's bloodhunt schedule. If the current date matches a scheduled bloodhunt,
     * the date is removed from the schedule regardless of further checks (representing a missed opportunity or a failed
     * attempt). The method then verifies if the campaign is planetside (or the target is absent from the campaign), and
     * the target is not currently deployed.</p>
     *
     * @param target               the person who may be the target of an assassination attempt
     * @param today                the date to check for a scheduled bloodhunt
     * @param isCampaignPlanetside {@code true} if the campaign is currently planetside, allowing an attempt;
     *                             {@code false} otherwise
     *
     * @return {@code true} if an assassination attempt should occur; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static boolean checkForAssassinationAttempt(Person target, LocalDate today, boolean isCampaignPlanetside) {
        List<LocalDate> bloodhuntSchedule = target.getBloodhuntSchedule();
        if (bloodhuntSchedule.isEmpty()) {
            return false;
        }

        if (!bloodhuntSchedule.contains(today)) {
            return false;
        }

        // Whether the Bloodhunt occurs, or not, we remove it from the array so that we're not parsing over old dates.
        // Removing the date even if the hunt is skips is a way of representing the character getting lucky and just
        // not being accessible when the bounty hunter makes their move.
        target.removeBloodhuntDate(today);

        if (!isCampaignPlanetside && !target.getStatus().isAbsent()) {
            return false;
        }

        // We check that the target is not deployed to avoid any weirdness that might come from the character
        // suddenly dying without the player having any reasonable way of recalling their unit.
        return !target.isDeployed();
    }

    /**
     * Performs an assassination attempt on a specified person based on their bloodmark level.
     *
     * <p>If the person's bloodmark indicates no threat, the attempt is skipped. Otherwise, the method:</p>
     * <ul>
     *   <li>Generates a bounty hunter with the appropriate skill level and name based on the
     *   {@link BloodmarkLevel}.</li>
     *   <li>Determines if the assassination attempt is successful by calculating wounds.</li>
     *   <li>If the person escapes (zero wounds), adds a report to the campaign and exits.</li>
     *   <li>If wounded, applies any special adjustments, processes the wounds, and reports the outcome.</li>
     * </ul>
     *
     * @param campaign the {@link Campaign} in which the event takes place
     * @param person   the {@link Person} targeted by the assassination attempt
     * @param today    the current {@link LocalDate} to associate with the event
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static void performAssassinationAttempt(Campaign campaign, Person person, LocalDate today) {
        int level = person.getBloodmark();
        BloodmarkLevel bloodmark = BloodmarkLevel.parseBloodmarkLevelFromInt(level);
        if (bloodmark == BLOODMARK_ZERO) {
            LOGGER.info("An assassination attempt was made on a character with no bloodmark. Skipping the hunt.");
            return;
        }

        // Generate the bounty hunter
        int bountyHunterSkill = bloodmark.getBountyHunterSkill();
        String bountyHunterName = getBountyHunterName();

        // Check whether the assassination attempt was successful
        int wounds = getWounds(bountyHunterSkill);

        // If wounds are 0, the character escaped the assassination attempt
        if (wounds == 0) {
            String report = getFormattedTextAt(RESOURCE_BUNDLE,
                  "Bloodmark.assassinationAttempt.unsuccessful",
                  person.getHyperlinkedFullTitle(),
                  spanOpeningWithCustomColor(getPositiveColor()),
                  CLOSING_SPAN_TAG,
                  bountyHunterName);
            campaign.addReport(report);
            return;
        }

        // Inflict injuries or wounds as appropriate
        wounds = adjustmentWoundsForSPAs(person, wounds);
        processWounds(campaign, person, today, wounds);

        String report = getReport(person.getStatus().isDead(), person.getHyperlinkedFullTitle(), bountyHunterName);
        campaign.addReport(report);

        MekHQ.triggerEvent(new PersonChangedEvent(person));
    }

    /**
     * Calculates the total number of wounds based on the given bounty hunter skill.
     *
     * <p>This method repeatedly rolls a random value based on the provided skill. For each roll that results in
     * zero, a die is rolled and its result is added to the wound total. The process continues until the random roll is
     * non-zero, increasing the number of wounds with each iteration.</p>
     *
     * @param bountyHunterSkill the skill value used to determine wound severity
     *
     * @return the total number of wounds determined by the skill-based rolls
     *
     * @author Illiani
     * @since 0.50.07
     */
    static int getWounds(int bountyHunterSkill) {
        int wounds = 0;
        int bountyHunterRoll = randomInt(bountyHunterSkill);
        while (bountyHunterRoll == 0) { // Increase severity with each successful loop
            wounds += d6(1);
            bountyHunterRoll = randomInt(bountyHunterSkill);
        }
        return wounds;
    }

    /**
     * Returns a randomly generated bounty hunter name in uppercase letters.
     *
     * <p>If a random name cannot be generated, the default bounty hunter name, {@link #DEFAULT_BOUNTY_HUNTER_NAME},
     * is returned instead.</p>
     *
     * @return an uppercase bounty hunter name, either randomly generated or the default if generation fails
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getBountyHunterName() {
        String bountyHunterName = RandomCallsignGenerator.getInstance().generate();
        if (bountyHunterName == null) {
            return DEFAULT_BOUNTY_HUNTER_NAME;
        }

        bountyHunterName = bountyHunterName.toUpperCase();
        return bountyHunterName;
    }

    /**
     * Adjusts the number of wounds based on the target person's special personnel abilities (SPAs).
     * <ul>
     *     <li>If the person has the {@code Glass Jaw} flaw (and not {@code Toughness}), the wounds are doubled.</li>
     *     <li>If the person has the {@code Toughness} ability (and not {@code Glass Jaw}), the wounds are reduced to
     *     75% of the original amount, rounded up.</li>
     *     <li>If the person has both or neither, the wounds remain unchanged.</li>
     * </ul>
     *
     * @param person the person whose SPA modifiers should be checked
     * @param wounds the initial number of wounds to adjust
     *
     * @return the adjusted number of wounds after applying SPA effects
     *
     * @author Illiani
     * @since 0.50.07
     */
    static int adjustmentWoundsForSPAs(Person person, int wounds) {
        boolean hasGlassJaw = person.getOptions().booleanOption(FLAW_GLASS_JAW);
        boolean hasToughness = person.getOptions().booleanOption(ATOW_TOUGHNESS);

        if (hasGlassJaw && hasToughness) {
            return wounds;
        }

        if (hasGlassJaw) {
            return wounds * 2;
        } else if (hasToughness) {
            return (int) ceil(wounds * 0.75);
        }

        return wounds;
    }

    /**
     * Generates a formatted report string describing the outcome of an assassination attempt.
     *
     * <p>The message content depends on whether the target was killed or only wounded in the attempt. The report
     * includes the bounty hunter's name and the target's hyperlinked full title and uses appropriately styled
     * formatting to indicate success or failure.</p>
     *
     * @param isDead               {@code true} if the target was killed; {@code false} if only wounded
     * @param hyperlinkedFullTitle the formatted full title of the target person
     * @param bountyHunterName     the name of the bounty hunter
     *
     * @return a formatted report string summarizing the assassination attempt outcome
     *
     * @author Illiani
     * @since 0.50.07
     */
    private static String getReport(boolean isDead, String hyperlinkedFullTitle, String bountyHunterName) {
        if (isDead) {
            return getFormattedTextAt(RESOURCE_BUNDLE, "Bloodmark.assassinationAttempt.assassinated",
                  bountyHunterName, spanOpeningWithCustomColor(getNegativeColor()), CLOSING_SPAN_TAG,
                  hyperlinkedFullTitle);
        } else {
            return getFormattedTextAt(RESOURCE_BUNDLE, "Bloodmark.assassinationAttempt.wounded",
                  bountyHunterName, spanOpeningWithCustomColor(getWarningColor()), CLOSING_SPAN_TAG,
                  hyperlinkedFullTitle);
        }
    }

    /**
     * Processes the injuries or hits a {@link Person} receives from an assassination attempt.
     *
     * <p>This method applies wounds to the target based on campaign options:</p>
     * <ul>
     *   <li>If Advanced Medical is enabled, wounds are converted to injuries, and the person's medical status is
     *   updated accordingly. If six or more injuries are present after processing, the person is marked as deceased
     *   via {@code PersonnelStatus.HOMICIDE}.</li>
     *   <li>If Advanced Medical is not enabled, wounds are added to the person's hit count, with a maximum of
     *   six. If the resulting hit count reaches six, the person is marked as deceased.</li>
     * </ul>
     *
     * @param campaign the campaign context in which the event occurs
     * @param person   the target who has received wounds or injuries
     * @param today    the date of the incident, used for status updates
     * @param wounds   the number of wounds to apply as a result of the assassination attempt
     *
     * @author Illiani
     * @since 0.50.07
     */
    static void processWounds(Campaign campaign, Person person, LocalDate today, int wounds) {
        boolean isUseAdvancedMedical = campaign.getCampaignOptions().isUseAdvancedMedical();
        if (isUseAdvancedMedical) {
            if (wounds > 0) {
                InjuryUtil.resolveCombatDamage(campaign, person, wounds);
            }
            if (person.getInjuries().size() >= 6) {
                person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
            }
        } else {
            int currentWounds = person.getHits();
            int newWounds = currentWounds + wounds;
            if (newWounds >= 6) {
                newWounds = 6;
                person.changeStatus(campaign, today, PersonnelStatus.HOMICIDE);
            }
            person.setHits(newWounds);
        }
    }
}
