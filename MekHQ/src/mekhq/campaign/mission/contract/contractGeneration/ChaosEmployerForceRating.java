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
package mekhq.campaign.mission.contract.contractGeneration;

import static java.lang.Math.clamp;
import static megamek.common.enums.SkillLevel.REGULAR;
import static megamek.common.enums.SkillLevel.VETERAN;

import megamek.common.enums.SkillLevel;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.universe.Faction;

/**
 * Decides how good a force &mdash; skill and equipment &mdash; an employer or enemy commits to a Chaos contract, from
 * where it sits rather than a detached die roll.
 *
 * <p>The rating is a baseline-plus-delta model centered on a {@link SkillLevel#REGULAR},
 * {@link DragoonRating#DRAGOON_C} outfit. The delta is built from four grounded contributions:</p>
 *
 * <ul>
 *   <li><b>Faction</b> &mdash; Clans and ComStar/Word of Blake field better forces; pirates, rebels, independents, and
 *       minor powers field worse (see {@link #factionDelta}).</li>
 *   <li><b>Era</b> &mdash; the Succession Wars degrade Inner Sphere skill and equipment (see {@link #eraDelta}).</li>
 *   <li><b>Contract importance</b> &mdash; the synthesized stake of the engagement, which is where the objective and
 *       the target world enter (see {@link ContractImportance}).</li>
 *   <li><b>Role and player scaling</b> &mdash; a small edge to the attacker, and, when the campaign opts into it, an
 *       adjustment that eases contracts for less-skilled players (see {@link #playerScalingDelta}).</li>
 * </ul>
 *
 * <p>The same engine rates both sides of a contract: the employer (ally) side and the enemy side each call it with
 * their own faction and role. It is deterministic in its inputs so callers can apply their own variance if desired.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class ChaosEmployerForceRating {

    /** A committed force's skill and equipment quality. */
    public record ForceRating(SkillLevel forceSkill, int equipmentRating) {
    }

    private ChaosEmployerForceRating() {
    }

    /**
     * Rates the force a single side commits to a contract.
     *
     * @param faction        the side's faction
     * @param isAttacker     whether this side is the attacker (the attacker fields a slightly better force)
     * @param isEmployerSide whether this side is the player's employer/ally (as opposed to the enemy); only affects the
     *                       direction of player scaling
     * @param year           the contract year, for era-based degradation
     * @param importance     the synthesized importance of the contract
     * @param scaleToPlayer  whether to scale difficulty toward the player's skill (a campaign option)
     * @param averageSkill   the player's average skill, used only when {@code scaleToPlayer} is {@code true}
     *
     * @return the committed force's skill and equipment rating
     */
    public static ForceRating determine(final Faction faction, final boolean isAttacker, final boolean isEmployerSide,
          final int year, final ContractImportance importance, final boolean scaleToPlayer,
          final SkillLevel averageSkill) {
        SkillLevel skill = resolveSkill(faction, isAttacker, isEmployerSide, year, importance, scaleToPlayer,
              averageSkill);
        int equipment = resolveEquipment(faction, year, importance);
        return new ForceRating(skill, equipment);
    }

    /**
     * Resolves the committed force's skill level.
     */
    static SkillLevel resolveSkill(final Faction faction, final boolean isAttacker, final boolean isEmployerSide,
          final int year, final ContractImportance importance, final boolean scaleToPlayer,
          final SkillLevel averageSkill) {
        int delta = factionDelta(faction)
                          + importance.getForceQualityModifier()
                          + eraDelta(faction, year)
                          + (isAttacker ? 1 : 0)
                          + (scaleToPlayer ? playerScalingDelta(isEmployerSide, averageSkill) : 0);

        SkillLevel skill = SkillLevel.changeByDelta(REGULAR, delta);

        // Clans never field truly raw troops: floor their crews at Veteran regardless of the other modifiers.
        if (faction.isClan() && (skill.getExperienceLevel() < VETERAN.getExperienceLevel())) {
            skill = VETERAN;
        }

        return skill;
    }

    /**
     * Resolves the committed force's equipment rating, as a {@link DragoonRating} rating value. Player scaling and the
     * attacker's edge are skill concerns and deliberately do not touch equipment.
     */
    static int resolveEquipment(final Faction faction, final int year, final ContractImportance importance) {
        int delta = factionDelta(faction) + importance.getForceQualityModifier() + eraDelta(faction, year);
        int rating = DragoonRating.DRAGOON_C.getRating() + delta;
        return clamp(rating, DragoonRating.DRAGOON_F.getRating(), DragoonRating.DRAGOON_ASTAR.getRating());
    }

    /**
     * The faction's inherent standing. Penalties stack (an independent minor power is doubly disadvantaged); Clan and
     * ComStar/Word of Blake bonuses reflect their superior training and materiel.
     */
    static int factionDelta(final Faction faction) {
        int delta = 0;

        if (faction.isRebelOrPirate()) {
            delta -= 2;
        }
        if (faction.isIndependent()) {
            delta -= 1;
        }
        if (faction.isMinorPower()) {
            delta -= 1;
        }
        if (faction.isClan()) {
            delta += 2;
        }
        if (faction.isComStarOrWoB()) {
            delta += 1;
        }

        return delta;
    }

    /**
     * Inner Sphere skill and equipment degradation across the Succession Wars and into the early Renaissance. Clans are
     * untouched by this decline.
     */
    static int eraDelta(final Faction faction, final int year) {
        if (faction.isClan()) {
            return 0;
        }

        if ((year >= 2830) && (year <= 2865)) {
            // Second Succession War
            return -1;
        } else if ((year >= 2866) && (year <= 3038)) {
            // Third Succession War
            return -2;
        } else if ((year >= 3039) && (year <= 3048)) {
            // Early Renaissance recovery
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * The difficulty-smoothing adjustment applied when a campaign opts into scaling contract forces toward the player's
     * skill. A weaker-than-Regular player gets stronger allies and weaker enemies; a stronger player, the reverse.
     */
    static int playerScalingDelta(final boolean isEmployerSide, final SkillLevel averageSkill) {
        int difference = REGULAR.getExperienceLevel() - averageSkill.getExperienceLevel();
        return isEmployerSide ? difference : -difference;
    }
}
