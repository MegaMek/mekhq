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
package mekhq.campaign.randomEvents.prisoners.enums;

/**
 * Defines the possible effects of an event within the prisoner system. Each effect represents a specific outcome or
 * consequence of an event, such as improving a skill, causing injury, or even death.
 */
public enum EventResultEffect {
    /**
     * The event produces no effect.
     */
    NONE,

    /**
     * A unique effect specific to the event context. Represents an effect that does not fall into standard categories.
     * Requires specific code support.
     */
    UNIQUE,

    /**
     * Affects the Prisoner Capacity. Increased by a positive {@code magnitude}, decreased by negative. Change is 10% *
     * magnitude.
     */
    PRISONER_CAPACITY,

    /**
     * Inflicts Hits (or Injuries) to a single guard or prisoner. The number of hits/injuries is equal to
     * {@code magnitude}. Any magnitude under 1 will be counted as 1, while magnitudes greater than 5 will be treated as
     * 5.
     */
    INJURY,

    /**
     * Inflicts d6 Hits to a percentage of guards or prisoners. The percentage of affected characters is equal to
     * {@code magnitude}.
     */
    INJURY_PERCENT,

    /**
     * Results in the death of a prisoner or guard. The number of fatalities is equal to {@code magnitude}.
     */
    DEATH,

    /**
     * Kills a percentage of guards or prisoners. The percentage of affected characters is equal to {@code magnitude}.
     */
    DEATH_PERCENT,

    /**
     * Grants a prisoner or guard a new skill, as defined by {@code skillType}. The level of the skill is equal to
     * {@code magnitude}. If the character already has the specified skill, they improve it instead. If the skill is
     * already at the specified level, nothing happens.
     */
    SKILL,

    /**
     * Adjusts a prisoner or guard's Loyalty. The amount of change is equal to {@code magnitude}, with a positive
     * magnitude increasing loyalty, while a negative decreases it. Only has an effect if Loyalty is enabled in the
     * player's Campaign Options.
     */
    LOYALTY_ONE,

    /**
     * Adjusts the loyalty of all prisoners or guards. The amount of change is equal to {@code magnitude}, with a
     * positive magnitude increasing loyalty, while a negative decreases it. Only has an effect if Loyalty is enabled in
     * the player's Campaign Options.
     */
    LOYALTY_ALL,

    /**
     * Frees a set number of prisoners equal to {@code magnitude}.
     */
    ESCAPE,

    /**
     * Frees a percentage of prisoners. The percentage of affected characters is equal to {@code magnitude}.
     */
    ESCAPE_PERCENT,

    /**
     * Modify the Fatigue of a single Prisoner or Guard by {@code magnitude}. Only has an effect if Fatigue is enabled
     * in the player's Campaign Options.
     */
    FATIGUE_ONE,

    /**
     * Modify the Fatigue of all Prisoners or Guards by {@code magnitude}. Only has an effect if Fatigue is enabled in
     * the player's Campaign Options.
     */
    FATIGUE_ALL,

    /**
     * Modifies the Support Points available in the current Campaign State by the value in {@code magnitude}. Only has
     * an effect is StratCon is enabled in the player's Campaign Options.
     */
    SUPPORT_POINT
}
