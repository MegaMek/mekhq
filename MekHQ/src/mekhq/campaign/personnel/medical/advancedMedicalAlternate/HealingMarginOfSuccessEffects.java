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
package mekhq.campaign.personnel.medical.advancedMedicalAlternate;

import static megamek.common.compute.Compute.d6;

import megamek.codeUtilities.MathUtility;

/**
 * Represents the possible outcomes of a healing attempt under the "Advanced Medical Alternate" ruleset.
 *
 * <p>Each enum constant describes a combination of effects that can occur after resolving a healing roll: whether
 * the injury is recovered, whether fatigue is inflicted, whether recovery is delayed, and whether the injury becomes
 * permanent. The mapping from margin of success to these effects is handled by
 * {@link #getEffectFromHealingAttempt(int)}.</p>
 *
 * @author Illiani
 * @since 0.50.10
 */
public enum HealingMarginOfSuccessEffects {
    /**
     * The injury fully recovers with no fatigue or delay.
     */
    RECOVERY(true, false, false, false),
    /**
     * The injury fully recovers but inflicts a point of fatigue.
     */
    RECOVERY_WITH_FATIGUE(true, true, false, false),
    /**
     * The injury will eventually recover, but its healing is delayed.
     */
    RECOVERY_DELAYED(false, false, true, false),
    /**
     * The injury will eventually recover, but healing is delayed and inflicts a point of fatigue.
     */
    RECOVERY_DELAYED_WITH_FATIGUE(false, true, true, false),
    /**
     * The injury fails to heal and becomes permanent.
     */
    PERMANENT_INJURY(false, false, false, true);

    private final static int MINIMUM_MARGIN_OF_SUCCESS = -6;
    private final static int MAXIMUM_MARGIN_OF_SUCCESS = 3;

    private final boolean isRecovery;
    private final boolean inflictsFatigue;
    private final boolean isDelayed;
    private final boolean isPermanent;


    /**
     * Creates a new healing outcome descriptor.
     *
     * @param isRecovery      {@code true} if the injury ultimately recovers under this outcome; {@code false}
     *                        otherwise
     * @param inflictsFatigue {@code true} if this outcome inflicts fatigue damage on the patient; {@code false}
     *                        otherwise
     * @param isDelayed       {@code true} if this outcome delays the healing time of the injury; {@code false}
     *                        otherwise
     * @param isPermanent     {@code true} if this outcome makes the injury permanent; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    HealingMarginOfSuccessEffects(boolean isRecovery, boolean inflictsFatigue, boolean isDelayed, boolean isPermanent) {
        this.isRecovery = isRecovery;
        this.inflictsFatigue = inflictsFatigue;
        this.isDelayed = isDelayed;
        this.isPermanent = isPermanent;
    }

    /**
     * Returns whether this outcome results in the injury being healed.
     *
     * @return {@code true} if the injury eventually recovers under this outcome; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isHealed() {
        return isRecovery;
    }

    /**
     * Gets the amount of fatigue damage inflicted by this outcome.
     *
     * <p>Currently this is either {@code 1} if fatigue is inflicted or {@code 0} if it is not.</p>
     *
     * @return the fatigue damage to apply to the patient
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getFatigueDamage() {
        return inflictsFatigue ? 1 : 0;
    }


    /**
     * Gets the additional delay applied to the injury's healing time.
     *
     * <p>If this outcome indicates a delayed recovery, a single d6 is rolled and added to the injury's remaining
     * healing time. Otherwise, the delay is {@code 0}.</p>
     *
     * @return a random delay in days if healing is delayed, or {@code 0} if there is no delay
     *
     * @author Illiani
     * @since 0.50.10
     */
    public int getHealingDelay() {
        return isDelayed ? d6(1) : 0;
    }

    /**
     * Returns whether this outcome makes the injury permanent.
     *
     * @return {@code true} if the injury becomes permanent; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.50.10
     */
    public boolean isPermanent() {
        return isPermanent;
    }


    /**
     * Determines the healing outcome corresponding to a given margin of success.
     *
     * <p>The provided margin of success is first clamped between {@link #MINIMUM_MARGIN_OF_SUCCESS} and
     * {@link #MAXIMUM_MARGIN_OF_SUCCESS}. The resulting value is then mapped to one of the defined enum constants:</p>
     *
     * <ul>
     *   <li>3: {@link #RECOVERY}</li>
     *   <li>0, 1, 2: {@link #RECOVERY_WITH_FATIGUE}</li>
     *   <li>-1, -2: {@link #RECOVERY_DELAYED}</li>
     *   <li>-3, -4, -5: {@link #RECOVERY_DELAYED_WITH_FATIGUE}</li>
     *   <li>-6: {@link #PERMANENT_INJURY}</li>
     * </ul>
     *
     * @param marginOfSuccess the raw margin of success from the healing roll
     *
     * @return the {@link HealingMarginOfSuccessEffects} corresponding to the given margin of success
     *
     * @author Illiani
     * @since 0.50.10
     */
    public static HealingMarginOfSuccessEffects getEffectFromHealingAttempt(int marginOfSuccess) {
        int clampedRoll = MathUtility.clamp(marginOfSuccess, MINIMUM_MARGIN_OF_SUCCESS, MAXIMUM_MARGIN_OF_SUCCESS);

        return switch (clampedRoll) {
            case 3 -> RECOVERY;
            case -1, -2 -> RECOVERY_DELAYED;
            case -3, -4, -5 -> RECOVERY_DELAYED_WITH_FATIGUE;
            case -6 -> PERMANENT_INJURY;
            default -> RECOVERY_WITH_FATIGUE; // 0, 1, 2
        };
    }
}
