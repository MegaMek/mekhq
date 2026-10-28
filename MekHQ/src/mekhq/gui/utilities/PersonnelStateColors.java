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
package mekhq.gui.utilities;

import static mekhq.campaign.personnel.turnoverAndRetention.Fatigue.getEffectiveFatigue;

import java.util.List;

import jakarta.annotation.Nullable;
import mekhq.MHQOptions;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelStatus;
import mekhq.campaign.work.TechJobs;
import mekhq.utilities.MHQInternationalization;

/**
 * Picks the row colors used to flag a person's state (absent, deployed, injured, fatigued, and so on) in personnel
 * tables, so every table highlights the same states in the same way.
 *
 * <p>The colors themselves are user-configurable through {@link MHQOptions}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class PersonnelStateColors {
    /**
     * A person with at least this much effective fatigue is flagged as fatigued.
     */
    public static final int FATIGUE_HIGHLIGHT_THRESHOLD = 5;

    /**
     * The resource key added to the color reasons of an injured person.
     */
    public static final String INJURED_REASON_KEY = "colorReason.personnel.injured";

    /**
     * The resource key added to the color reasons of a tech in the middle of a job; its text names the job.
     */
    public static final String WORKING_REASON_KEY = "colorReason.personnel.working";

    private PersonnelStateColors() {}

    /**
     * Why a person's row is highlighted: a resource key in the default GUI bundle, with any values its text needs.
     *
     * @param key       the reason's resource key
     * @param arguments the values for the reason's text, such as the job a tech is doing; empty for most reasons
     */
    public record ColorReason(String key, List<Object> arguments) {
        /**
         * @param key the reason's resource key, for a reason whose text needs no values
         */
        public ColorReason(String key) {
            this(key, List.of());
        }

        /**
         * @return the reason, in words the player can read
         */
        public String text() {
            return arguments.isEmpty() ? MHQInternationalization.getText(key)
                         : MHQInternationalization.getFormattedText(key, arguments.toArray());
        }
    }

    /**
     * Determines the colors for a person's row, and collects the reasons they are highlighted.
     *
     * <p>A person may be in several highlighted states at once. The color comes from the most important state, while
     * every applicable state is added to {@code colorReasons}.</p>
     *
     * @param campaign     the current campaign
     * @param person       the person to check
     * @param colorReasons receives a reason for every state that applies
     * @param jobs         the jobs the person is in the middle of, from {@link TechJobs}; empty for anyone not working
     *                     on one
     *
     * @return the colors for the most important state, or {@code null} if no highlighted state applies
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable ComponentColors getStateColors(Campaign campaign, Person person,
          List<ColorReason> colorReasons, List<String> jobs) {
        // Set color based on priority (first match wins for display color)
        // But collect ALL applicable reasons for tooltip
        MHQOptions mhqOptions = MekHQ.getMHQOptions();
        CampaignOptions campaignOptions = campaign.getCampaignOptions();

        ComponentColors cellColors = null;
        if (person.getStatus().isAbsent()) {
            colorReasons.add(new ColorReason("colorReason.personnel.absent"));
            cellColors = mhqOptions.getAbsentColors();
        }
        if (person.getStatus().isDepartedUnit()) {
            colorReasons.add(new ColorReason("colorReason.personnel.departed"));
            cellColors = (cellColors == null) ? mhqOptions.getGoneColors() : cellColors;
        }
        if (person.isDeployed()) {
            colorReasons.add(new ColorReason("colorReason.personnel.deployed"));
            cellColors = (cellColors == null) ? mhqOptions.getDeployedColors() : cellColors;
        }
        if (person.isQueuedForTravel(campaign.getCampaignLocationManager())) {
            colorReasons.add(new ColorReason("colorReason.personnel.queuedForTravel"));
            cellColors = (cellColors == null) ? mhqOptions.getQueuedForTravelColors() : cellColors;
        }
        if (PersonnelStatus.computeIsAwayFromMainForce(campaign, person)) {
            colorReasons.add(new ColorReason("colorReason.personnel.awayFromMainForce"));
            cellColors = (cellColors == null) ? mhqOptions.getAwayFromMainForceColors() : cellColors;
        }
        if (campaignOptions.isUseAdvancedMedical() ? person.hasInjuries(true) : (person.getHits() > 0)) {
            colorReasons.add(new ColorReason(INJURED_REASON_KEY));
            cellColors = (cellColors == null) ? mhqOptions.getInjuredColors() : cellColors;
        }
        if (person.isPregnant()) {
            colorReasons.add(new ColorReason("colorReason.personnel.pregnant"));
            cellColors = (cellColors == null) ? mhqOptions.getPregnantColors() : cellColors;
        }
        if (campaignOptions.get(CampaignOption.USE_FATIGUE) &&
                  (getEffectiveFatigue(person, campaign) >= FATIGUE_HIGHLIGHT_THRESHOLD)) {
            colorReasons.add(new ColorReason("colorReason.personnel.fatigued"));
            cellColors = (cellColors == null) ? mhqOptions.getFatiguedColors() : cellColors;
        }
        // A tech in the middle of a job keeps spending time on it every day until it is done
        for (String job : jobs) {
            colorReasons.add(new ColorReason(WORKING_REASON_KEY, List.of(job)));
            cellColors = (cellColors == null) ? mhqOptions.getWorkingColors() : cellColors;
        }
        boolean isUseAlternativeAdvancedMedical = campaignOptions.get(CampaignOption.USE_ALTERNATIVE_ADVANCED_MEDICAL);
        if (person.hasNonProstheticPermanentInjuries(isUseAlternativeAdvancedMedical)) {
            colorReasons.add(new ColorReason("colorReason.personnel.healedInjuries"));
            cellColors = (cellColors == null) ? mhqOptions.getHealedInjuriesColors() : cellColors;
        }
        return cellColors;
    }

    /**
     * Converts the reasons collected by {@link #getStateColors(Campaign, Person, List, List)} into tooltip text, one
     * reason per line.
     *
     * @param colorReasons the collected reasons
     *
     * @return the reasons as HTML-ready text (without {@code <html>} tags), or an empty string if there are none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String getColorReasonsText(List<ColorReason> colorReasons) {
        StringBuilder reasonsText = new StringBuilder();
        for (ColorReason reason : colorReasons) {
            if (!reasonsText.isEmpty()) {
                reasonsText.append("<br>");
            }
            reasonsText.append(reason.text());
        }
        return reasonsText.toString();
    }
}
