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
package mekhq.campaign.personnel.medical;

import static java.lang.Math.ceil;
import static mekhq.campaign.personnel.PersonnelOptions.ATOW_TOUGHNESS;
import static mekhq.campaign.personnel.PersonnelOptions.FLAW_GLASS_JAW;

import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;

public class InjurySPAUtility {
    /**
     * Checks whether injuries should increase Fatigue. This requires both Fatigue and Injury Fatigue to be enabled.
     *
     * @param campaignOptions the campaign's options
     *
     * @return {@code true} if injuries should increase Fatigue
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isUseInjuryFatigue(CampaignOptions campaignOptions) {
        return campaignOptions.get(CampaignOption.USE_INJURY_FATIGUE) &&
                     campaignOptions.get(CampaignOption.USE_FATIGUE);
    }

    /**
     * Adjusts the number of injuries for a person based on their Special Pilot Abilities (SPAs) and optionally updates
     * their fatigue.
     *
     * <p>If the person has the "Glass Jaw" flaw (but not both "Glass Jaw" and "Toughness"), the number of injuries
     * is doubled. If the person has the "Toughness" ability (but not both "Glass Jaw" and "Toughness"), the number of
     * injuries is reduced to 75% (rounded up). If both traits are present, no adjustment is made.</p>
     *
     * <p>If {@code isUseInjuryFatigue} is {@code true}, the method also increases the person's fatigue by {@code
     * fatigueRate} multiplied by the base (unmodified) number of injuries. The "Glass Jaw" and "Toughness"
     * adjustments are deliberately excluded from this calculation. Glass Jaw doubles injuries suffered and fatigue
     * gained, and Toughness reduces injuries suffered and halves fatigue gained, but in both cases the fatigue gained
     * from sustaining injuries is neither doubled nor halved. For the same reason, this fatigue is not scaled by the
     * person's fatigue multiplier.</p>
     *
     * <p>Callers should use {@link #isUseInjuryFatigue(CampaignOptions)} to determine {@code isUseInjuryFatigue}.</p>
     *
     * @param person             the {@link Person} whose injuries and fatigue are to be adjusted
     * @param isUseInjuryFatigue whether to apply fatigue increase based on injuries
     * @param fatigueRate        the rate at which fatigue increases per injury
     * @param injuries           the base number of injuries before adjustments
     *
     * @return the adjusted number of injuries after applying SPAs
     *
     * @author Illiani
     * @since 0.50.07
     */
    public static int adjustInjuriesAndFatigueForSPAs(Person person, boolean isUseInjuryFatigue, int fatigueRate,
          int injuries) {
        boolean hasGlassJaw = person.getOptions().booleanOption(FLAW_GLASS_JAW);
        boolean hasToughness = person.getOptions().booleanOption(ATOW_TOUGHNESS);
        boolean hasGlassJawAndToughness = hasGlassJaw && hasToughness;

        // Capture the base injury count for the fatigue calculation before applying the Glass Jaw doubling or
        // Toughness reduction.
        int fatigueInjuries = injuries;

        if (hasGlassJaw && !hasGlassJawAndToughness) {
            injuries = injuries * 2;
        } else if (hasToughness && !hasGlassJawAndToughness) {
            injuries = (int) ceil(injuries * 0.75);
        }

        if (isUseInjuryFatigue) {
            int fatigueIncrease = fatigueRate * fatigueInjuries;
            // Injury fatigue is deliberately unscaled, so the fatigue multiplier isn't applied
            person.changeFatigue(fatigueIncrease, false);
        }

        return injuries;
    }
}
