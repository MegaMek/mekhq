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
package mekhq.campaign.mission.contract.contractData;

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * A random flavor characteristic that a contract can carry for its whole duration. Characteristics are rolled when a
 * contract is generated (gated behind the {@code USE_RANDOM_CONTRACT_CHARACTERISTICS} campaign option) and are
 * predominantly there to add texture: each one nudges a single facet of the offer up or down. They only ever touch
 * things MekHQ processes on its own - pay, contract length, objective requirements, unit reputation, and faction
 * standing - so a characteristic never reaches into scenario generation.
 *
 * <p>Most characteristics come in a beneficial/adverse pair sharing a {@link Category}. The category doubles as a
 * mutual-exclusion group: a single contract carries at most one characteristic from any given category, so it can never
 * be handed, say, both {@link #HIGH_PAY} and {@link #LOW_PAY}.</p>
 *
 * <p>{@code magnitude} is a general-purpose modifier whose meaning depends on the category (documented on each
 * constant and on {@link Category}); the application code interprets it per category. {@code weight} is the relative
 * likelihood of the characteristic being chosen when one from its category is rolled.</p>
 */
public enum ContractCharacteristic {
    /** The reputation gain on success is doubled (a breach is unaffected). */
    CAREER_MAKER(Category.UNIT_REPUTATION, Polarity.BENEFICIAL, 6, 2.0),
    /** On successful completion, a lump equal to 5% of total pay is credited as a bonus on top of the contract. */
    COMPLETION_BONUS(Category.COMPLETION_PAYMENT, Polarity.BENEFICIAL, 6, 0.05),
    /** Combat pay is reduced by 5%. */
    CUT_RATE(Category.COMBAT_PAY, Polarity.ADVERSE, 10, 0.95),
    /** Required victory points are raised, so the contract is harder to fulfill. */
    DEMANDING(Category.OBJECTIVES, Polarity.ADVERSE, 10, 1.0),
    /** The enemy negotiator is Elite rather than the usual Veteran. */
    ELITE_NEGOTIATOR(Category.NEGOTIATOR, Polarity.ADVERSE, 6, 1.0),
    /** Combat pay is boosted by 5%. */
    HAZARD_PAY(Category.COMBAT_PAY, Polarity.BENEFICIAL, 10, 1.05),
    /** The contract has one more track. */
    HIGH_INTENSITY(Category.INTENSITY, Polarity.NEUTRAL, 10, 1.0),
    /** Monthly pay is boosted by 25%. */
    HIGH_PAY(Category.PAY, Polarity.BENEFICIAL, 3, 1.25),
    /** The reputation gain on success is doubled (a breach is unaffected). */
    HIGH_PROFILE(Category.UNIT_REPUTATION, Polarity.NEUTRAL, 6, 2.0),
    /** The enemy faction-standing loss for taking this contract is amplified. */
    ITS_PERSONAL(Category.ENEMY_STANDING, Polarity.ADVERSE, 6, 1.5),
    /** The enemy faction-standing loss for taking this contract is reduced. */
    JUST_BUSINESS(Category.ENEMY_STANDING, Polarity.BENEFICIAL, 6, 0.5),
    /** The contract runs longer than the rolled length. */
    LENGTHY(Category.LENGTH, Polarity.NEUTRAL, 10, 1.5),
    /** Required victory points are lowered, so the contract is easier to fulfill. */
    LENIENT(Category.OBJECTIVES, Polarity.BENEFICIAL, 10, -1.0),
    /** The contract has one fewer track, to a minimum of one. */
    LOW_INTENSITY(Category.INTENSITY, Polarity.NEUTRAL, 10, -1.0),
    /** Monthly pay is reduced by 25%. */
    LOW_PAY(Category.PAY, Polarity.ADVERSE, 3, 0.75),
    /** No reputation is gained on success (a breach is unaffected). */
    MEDIA_BLACKOUT(Category.UNIT_REPUTATION, Polarity.NEUTRAL, 6, 0.0),
    /** The employer faction-standing change is amplified. */
    MONITORED(Category.EMPLOYER_STANDING, Polarity.BENEFICIAL, 6, 1.5),
    /** The enemy negotiator is only Regular rather than the usual Veteran, making negotiation easier for the player. */
    NOVICE_NEGOTIATOR(Category.NEGOTIATOR, Polarity.BENEFICIAL, 6, -1.0),
    /** The contract runs shorter than the rolled length. */
    QUICK(Category.LENGTH, Polarity.NEUTRAL, 10, 0.75),
    /** Total pay is unchanged, but 5% of it is paid up front on signing */
    SIGNING_BONUS(Category.PAY, Polarity.NEUTRAL, 6, 0.95),
    /** No reputation is gained on success (a breach is unaffected). */
    THANKLESS(Category.UNIT_REPUTATION, Polarity.ADVERSE, 6, 0.0),
    /** The employer faction-standing change is amplified against you on failure. */
    UNMONITORED(Category.EMPLOYER_STANDING, Polarity.ADVERSE, 6, 0.5);

    private final static String RESOURCE_BUNDLE = "mekhq.resources.Mission";

    private final Category category;
    private final Polarity polarity;
    private final int weight;
    private final double magnitude;
    private final String name;
    private final String toolTipText;

    ContractCharacteristic(final Category category, final Polarity polarity, final int weight,
          final double magnitude) {
        this.category = category;
        this.polarity = polarity;
        this.weight = weight;
        this.magnitude = magnitude;
        this.name = getTextAt(RESOURCE_BUNDLE, "ContractCharacteristic." + name() + ".text");
        this.toolTipText = getTextAt(RESOURCE_BUNDLE, "ContractCharacteristic." + name() + ".toolTipText");
    }

    /** @return the mutual-exclusion / lever grouping this characteristic belongs to */
    public Category getCategory() {
        return category;
    }

    /** @return whether this characteristic is, on balance, good, bad, or neutral for the player */
    public Polarity getPolarity() {
        return polarity;
    }

    /** @return the relative likelihood of this characteristic being chosen when one from its category is rolled */
    public int getWeight() {
        return weight;
    }

    /**
     * The size of the modifier this characteristic applies. The meaning depends on {@link #getCategory()}: a multiplier
     * for the scaling categories, a signed step for {@link Category#NEGOTIATOR} and {@link Category#OBJECTIVES}, and a
     * fraction of total pay for {@link Category#COMPLETION_PAYMENT}. See each constant and {@link Category}.
     *
     * @return the modifier magnitude
     */
    public double getMagnitude() {
        return magnitude;
    }

    /** @return the localized display name */
    public String getName() {
        return name;
    }

    /** @return the localized tooltip / description text */
    public String getToolTipText() {
        return toolTipText;
    }

    /** @return {@code true} if this characteristic is, on balance, good for the player */
    public boolean isBeneficial() {
        return polarity == Polarity.BENEFICIAL;
    }

    /** @return {@code true} if this characteristic is, on balance, bad for the player */
    public boolean isAdverse() {
        return polarity == Polarity.ADVERSE;
    }

    /**
     * The lever a characteristic acts on. A contract carries at most one characteristic per category, so a category
     * also serves as a mutual-exclusion group. The category determines how {@link #getMagnitude()} is interpreted and
     * at which point in the contract lifecycle the modifier is applied.
     */
    public enum Category {
        /**
         * Scales monthly pay at generation (a multiplier). {@link #SIGNING_BONUS} additionally credits a lump on
         * accept.
         */
        PAY,
        /** Scales combat pay at generation (a multiplier). */
        COMBAT_PAY,
        /** Credits a fraction of total pay as a lump on successful completion (the fraction is the magnitude). */
        COMPLETION_PAYMENT,
        /** Sets the enemy negotiator's skill tier at generation (a signed step from Veteran). */
        NEGOTIATOR,
        /** Scales contract length at generation (a multiplier). */
        LENGTH,
        /** Adjusts required victory points at generation (a signed step). */
        OBJECTIVES,
        /** Adjusts the contract's track count at generation (a signed step, floored at one). */
        INTENSITY,
        /** Scales the unit reputation change at completion (a multiplier). */
        UNIT_REPUTATION,
        /** Scales the employer faction-standing change at completion (a multiplier). */
        EMPLOYER_STANDING,
        /** Scales the enemy faction-standing loss taken at acceptance (a multiplier). */
        ENEMY_STANDING
    }

    /** Whether a characteristic is, on balance, good, bad, or neutral (double-edged) for the player. */
    public enum Polarity {
        BENEFICIAL,
        ADVERSE,
        NEUTRAL
    }
}
