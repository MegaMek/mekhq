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
package mekhq.campaign.unit.crewAssignment;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;

import megamek.common.enums.SkillLevel;
import megamek.common.units.Entity;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.familiarity.Familiarity;
import mekhq.campaign.unit.Unit;

/**
 * Fills open seats from a pool of people, best first. The filling is done through an {@link AssignmentPlan}, so it is
 * staged like any other change and can be undone in one step.
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class AutoFill {
    /** The familiarity points between steps where familiarity bonuses change. */
    static final int FAMILIARITY_STEP = 100;

    private AutoFill() {
    }

    /**
     * Stages the best qualified unassigned people from {@code candidates} into every open seat of one type on the given
     * units. Units are filled in the order given; within that, the best person takes the first seat. Someone who fits
     * none of the units is passed over, so the next best person who does fit is tried instead.
     *
     * @param plan       the plan to stage into
     * @param units      the units, in the order they should be filled
     * @param type       the seat to fill
     * @param candidates the people to choose from; anyone with a unit, or a staged change, is skipped
     * @param ranking    best first; see {@link #byExperience}
     *
     * @return what was staged
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static AssignmentPlan.StageResult fill(final AssignmentPlan plan, final List<Unit> units,
          final SeatType type, final Collection<Person> candidates, final Comparator<Person> ranking) {
        return fill(plan, units, type, candidates, ranking, Familiarity.DISABLED);
    }

    /**
     * As {@link #fill(AssignmentPlan, List, SeatType, Collection, Comparator)}, but people are first seated on units
     * whose chassis they are familiar with, highest familiarity step first; see {@link #byFamiliarity}. Everyone else
     * is then seated best first.
     *
     * @param plan            the plan to stage into
     * @param units           the units, in the order they should be filled
     * @param type            the seat to fill
     * @param candidates      the people to choose from; anyone with a unit, or a staged change, is skipped
     * @param ranking         best first; see {@link #byExperience}
     * @param familiarityMode the campaign's chassis familiarity mode; while disabled, familiarity is ignored
     *
     * @return what was staged
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static AssignmentPlan.StageResult fill(final AssignmentPlan plan, final List<Unit> units,
          final SeatType type, final Collection<Person> candidates, final Comparator<Person> ranking,
          final Familiarity familiarityMode) {
        final List<Person> pool = new ArrayList<>();
        for (Person person : candidates) {
            if ((person.getUnit() == null) && (plan.changeFor(person) == null)) {
                pool.add(person);
            }
        }
        pool.sort(ranking);
        return plan.fillSeats(pool, units, type, byFamiliarity(familiarityMode));
    }

    /**
     * How strongly a person should crew a unit, by their familiarity with its chassis: the number of whole
     * {@value #FAMILIARITY_STEP}-point steps they have reached, as that is where familiarity bonuses change. Below the
     * first step, or while familiarity is disabled, there is no preference, so experience alone decides.
     *
     * @param familiarityMode the campaign's chassis familiarity mode
     *
     * @return the preference, zero for none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static ToIntBiFunction<Person, Unit> byFamiliarity(final Familiarity familiarityMode) {
        if (!familiarityMode.isEnabled()) {
            return (person, unit) -> 0;
        }
        return (person, unit) -> {
            final Entity entity = unit.getEntity();
            if ((entity == null) || !entity.isChassisFamiliarityEligible()) {
                return 0;
            }
            return person.getChassisFamiliarity(entity.getChassis()) / FAMILIARITY_STEP;
        };
    }

    /**
     * Gives each of the given units that will have no tech a tech from {@code candidates}, chosen as the Hangar's
     * automatic tech assignment chooses; see {@link AssignmentPlan#autoStageTechs}.
     *
     * @param plan       the plan to stage into
     * @param units      the units
     * @param candidates the people to choose from; anyone who is not a tech is skipped
     *
     * @return what was staged
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static AssignmentPlan.TechStageResult fillTechs(final AssignmentPlan plan, final List<Unit> units,
          final Collection<Person> candidates) {
        final List<Person> techs = new ArrayList<>();
        for (Person person : candidates) {
            if (person.isTech()) {
                techs.add(person);
            }
        }
        return plan.autoStageTechs(techs, units);
    }

    /**
     * Ranks people by the experience level of their primary role, highest first, ignoring injuries; ties go to the
     * person with the better (lower) gunnery-plus-piloting total the caller supplies, then to the name.
     *
     * @param campaignOptions the campaign options
     * @param isClanCampaign  whether the campaign is a Clan campaign
     * @param today           the campaign date
     * @param combatSkill     a person's combined combat skill target, lower being better
     *
     * @return the ranking
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static Comparator<Person> byExperience(final CampaignOptions campaignOptions, final boolean isClanCampaign,
          final LocalDate today, final ToIntFunction<Person> combatSkill) {
        // A sort compares each person many times, so each person's level is worked out once
        final Map<Person, SkillLevel> levels = new HashMap<>();
        final Comparator<Person> experience = Comparator.comparing(person -> levels.computeIfAbsent(person,
              key -> key.getSkillLevel(campaignOptions, isClanCampaign, today, false, true)));
        return experience.reversed()
                     .thenComparingInt(combatSkill)
                     .thenComparing(Person::getFullName);
    }
}
