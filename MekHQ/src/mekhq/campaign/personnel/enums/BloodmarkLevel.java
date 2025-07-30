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
package mekhq.campaign.personnel.enums;

import megamek.logging.MMLogger;
import mekhq.campaign.finances.Money;

/**
 * Represents the different Bloodmark levels available to a character, including bounties, bounty hunter skill, and
 * rules for roll checks.
 *
 * @author Illiani
 * @since 0.50.07
 */
public enum BloodmarkLevel {
    /** 0 TP, no bounty, no encounters. */
    BLOODMARK_ZERO(0, Money.of(0), 20, 0, 0),
    /** 1 TP, 10,000 C-Bill bounty, 1d6/3 (rounded down) encounters every 6 months (on average). */
    BLOODMARK_ONE(1, Money.of(10000), 20, 3, 6),
    /** 2 TP, 37,500 C-Bill bounty, 1d6/2 (rounded down) encounters every 3 months (on average). */
    BLOODMARK_TWO(2, Money.of(37500), 20, 2, 3),
    /** 3 TP, 125,000 C-Bill bounty, 1d6/3 (rounded down) encounters a month (on average). */
    BLOODMARK_THREE(3, Money.of(125000), 10, 3, 1),
    /** 4 TP, 500,000 C-Bill bounty, 1d6/2 (rounded down) encounters a month (on average). */
    BLOODMARK_FOUR(4, Money.of(500000), 10, 2, 1),
    /** 5 TP, 1,000,000 C-Bill bounty, 1d6 encounters a month (on average). */
    BLOODMARK_FIVE(5, Money.of(1000000), 5, 1, 1);

    private static final MMLogger LOGGER = MMLogger.create(BloodmarkLevel.class);

    private final int level;
    private final Money bounty;
    private final int bountyHunterSkill;
    private final int rollDivisor;
    private final int rollFrequency;

    /**
     * Constructs a {@link BloodmarkLevel} enum constant with the specified properties.
     *
     * @param level             the trait training point value for this bloodmark
     * @param bounty            the monetary bounty awarded for this bloodmark
     * @param bountyHunterSkill the skill of the character attempting to collect on the bounty
     * @param rollDivisor       divisor used in roll calculations
     * @param rollFrequency     how often rolls for this bloodmark occur
     */
    BloodmarkLevel(int level, Money bounty, int bountyHunterSkill, int rollDivisor, int rollFrequency) {
        this.level = level;
        this.bounty = bounty;
        this.bountyHunterSkill = bountyHunterSkill;
        this.rollDivisor = rollDivisor;
        this.rollFrequency = rollFrequency;
    }

    /**
     * Returns the level value associated with this bloodmark.
     *
     * @return the level as an {@link Integer}
     */
    public int getLevel() {
        return level;
    }

    /**
     * Returns the bounty, in {@link Money}, associated with this bloodmark level.
     *
     * @return the {@link Money} bounty value
     */
    public Money getBounty() {
        return bounty;
    }

    /**
     * Returns the chance a bounty collection is successful.
     *
     * <p>This is equal to 1-in-n, where n is the value here.</p>
     *
     * @return the bounty hunter skill
     */
    public int getBountyHunterSkill() {
        return bountyHunterSkill;
    }

    /**
     * Returns the divisor used in roll calculations for this bloodmark level.
     *
     * @return the roll divisor value
     */
    public int getRollDivisor() {
        return rollDivisor;
    }

    /**
     * Returns how frequently rolls occur at this bloodmark level.
     *
     * @return the roll frequency value
     */
    public int getRollFrequency() {
        return rollFrequency;
    }

    /**
     * Attempts to parse a {@link BloodmarkLevel} constant from a supplied int value.
     *
     * <p>If the value does not map directly to an enum value, {@link #BLOODMARK_ZERO} is returned and a warning is
     * logged.</p>
     *
     * @param value the integer value to parse
     *
     * @return the corresponding {@link BloodmarkLevel}, or {@link #BLOODMARK_ZERO} if invalid
     */
    public static BloodmarkLevel parseBloodmarkLevelFromInt(int value) {
        for (BloodmarkLevel bloodmarkLevel : BloodmarkLevel.values()) {
            if (bloodmarkLevel.level == value) {
                return bloodmarkLevel;
            }
        }

        LOGGER.warn("Failed to parse BloodmarkData from int: {} - returning BLOODMARK_ZERO", value);
        return BLOODMARK_ZERO;
    }
}
