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
package mekhq.campaign.digitalGM.stratCon.facility;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;

import megamek.common.annotations.Nullable;
import megamek.common.compute.Compute;
import mekhq.campaign.digitalGM.stratCon.facility.StratConFacility.FacilityTier;

/**
 * Something particular about one facility, rolled when it is placed, so that two facilities of the same type need not
 * play the same. The player sees a facility's traits once they have detailed intel on it.
 *
 * <p>A <b>garrison</b> trait describes the troops holding the facility, so it is lost when the facility changes
 * sides. A <b>site</b> trait describes the place itself and stays with it.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum FacilityTrait {
    /** One garrison step fewer than its tier allows. Never rolled on an Outpost, which would be left empty. */
    UNDERMANNED(true, FacilityTier.BASE),
    /**
     * Held by veterans: fights at an enemy facility face better-skilled troops; an employer's facility defends itself
     * better when you stay away from a counterattack.
     */
    VETERAN_GARRISON(true, FacilityTier.OUTPOST),
    /**
     * Its troops would rather not be there: an enemy facility under siege surrenders one garrison step sooner; an
     * employer's facility defends itself worse when you stay away from a counterattack.
     */
    POOR_MORALE(true, FacilityTier.OUTPOST),
    /** Built to take punishment: an attacker who wins short of capturing it does no damage, and sabotage only one step. */
    HARDENED(false, FacilityTier.OUTPOST),
    /**
     * Full stores: a won raid on an enemy one brings in extra Employer Support, and one your side holds provides extra
     * Employer Support each month.
     */
    WELL_STOCKED(false, FacilityTier.OUTPOST),
    /**
     * Prototype gear: fights at an enemy facility face better-equipped troops, and capturing it brings in Employer
     * Support once, as the gear is traded to your employer.
     */
    EXPERIMENTAL_WEAPONS(false, FacilityTier.OUTPOST);

    /** Employer Support a won raid on a well-stocked enemy facility brings in.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final int WELL_STOCKED_RAID_SUPPORT = 2;
    /** Extra Employer Support a well-stocked facility your side holds provides each month.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final int WELL_STOCKED_MONTHLY_SUPPORT = 1;
    /** Employer Support capturing a facility with experimental weapons brings in.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final int EXPERIMENTAL_WEAPONS_CAPTURE_SUPPORT = 2;
    /** The off-screen defence modifier for a veteran (or, negated, a poor-morale) garrison.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final int OFF_SCREEN_DEFENSE_MODIFIER = 2;
    /** The most traits a facility is given.
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static final int MAXIMUM_TRAITS = 2;

    private final boolean isGarrisonTrait;
    private final FacilityTier minimumTier;

    FacilityTrait(boolean isGarrisonTrait, FacilityTier minimumTier) {
        this.isGarrisonTrait = isGarrisonTrait;
        this.minimumTier = minimumTier;
    }

    /**
     * @return {@code true} if the trait describes the facility's troops, and so is lost when it changes sides
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isGarrisonTrait() {
        return isGarrisonTrait;
    }

    /**
     * @param tier a facility's tier
     *
     * @return {@code true} if a facility of that tier can be given this trait
     *
     * @author Illiani
     * @since 0.51.01
     */
    public boolean isAllowedFor(FacilityTier tier) {
        return tier.ordinal() >= minimumTier.ordinal();
    }

    /**
     * @return the trait that cannot sit alongside this one, or {@code null} if any may
     *
     * @author Illiani
     * @since 0.51.01
     */
    public @Nullable FacilityTrait getConflictingTrait() {
        return switch (this) {
            case VETERAN_GARRISON -> POOR_MORALE;
            case POOR_MORALE -> VETERAN_GARRISON;
            default -> null;
        };
    }

    /**
     * @param roll a 2d6 roll
     *
     * @return how many traits a newly placed facility gets: none on 6 or less, one on 7 to 9, two on 10 or more
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getTraitCount(int roll) {
        if (roll >= 10) {
            return MAXIMUM_TRAITS;
        }
        return (roll >= 7) ? 1 : 0;
    }

    /**
     * Rolls how many traits a newly placed facility gets and picks them (see {@link #getTraitCount} and
     * {@link #pickTraits}), giving them to the facility. Call it once the facility's tier is set; its garrison is
     * trimmed to what the traits allow.
     *
     * @param facility the facility just placed
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static void assignRandomTraits(StratConFacility facility) {
        int count = getTraitCount(Compute.d6(2));
        for (FacilityTrait trait : pickTraits(facility.getTier(), count, Compute::randomInt)) {
            facility.addTrait(trait);
        }
    }

    /**
     * Picks traits for a newly placed facility, skipping any its tier does not allow and any that conflict with one
     * already picked.
     *
     * @param tier       the facility's tier
     * @param count      how many traits to pick (see {@link #getTraitCount})
     * @param randomizer gives a random index below its argument
     *
     * @return the traits, in the order picked
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<FacilityTrait> pickTraits(FacilityTier tier, int count,
          IntUnaryOperator randomizer) {
        List<FacilityTrait> picked = new ArrayList<>();
        for (int attempt = 0; attempt < count; attempt++) {
            List<FacilityTrait> candidates = new ArrayList<>();
            for (FacilityTrait trait : values()) {
                boolean isConflicting = false;
                for (FacilityTrait pickedTrait : picked) {
                    if ((pickedTrait == trait) || (pickedTrait.getConflictingTrait() == trait)) {
                        isConflicting = true;
                        break;
                    }
                }
                if (!isConflicting && trait.isAllowedFor(tier)) {
                    candidates.add(trait);
                }
            }
            if (candidates.isEmpty()) {
                break;
            }
            picked.add(candidates.get(randomizer.applyAsInt(candidates.size())));
        }
        return picked;
    }
}
