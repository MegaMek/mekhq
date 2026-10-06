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
package mekhq.campaign.universe.commandGeneration.ratgen;

import static mekhq.campaign.personnel.skills.SkillType.S_LEADER;
import static mekhq.campaign.personnel.skills.SkillType.S_STRATEGY;
import static mekhq.campaign.personnel.skills.SkillType.S_TACTICS;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.Utilities;
import mekhq.campaign.force.FormationLevel;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;

/**
 * Gives the command's officers the skills that come with the post (Generate Captains).
 *
 * <p>The rule is the retired AtB Company Generator's. The commanding officer gains a level of both gunnery and
 * piloting and two random command skill increases. Every other officer gains a level of gunnery or piloting -
 * whichever is better, or whichever is worse when Apply Officer Stat Bonus to Weakest Skill is on - and then
 * random command skill increases: two for a captain or anyone above, one for a lance leader. A command skill the
 * officer did not have is opened at level one rather than left at zero.</p>
 */
public final class OfficerSkillBooster {

    private static final MMLogger LOGGER = MMLogger.create(OfficerSkillBooster.class);
    private static final String LOG_TAG = "[CompanyGen][OfficerSkills]";

    /** The skills a command skill increase can land on, chosen at random with equal weight. */
    static final List<String> COMMAND_SKILLS = List.of(S_LEADER, S_STRATEGY, S_TACTICS);

    private static final int COMMANDING_OFFICER_INCREASES = 2;
    private static final int CAPTAIN_INCREASES = 2;
    private static final int LIEUTENANT_INCREASES = 1;

    private OfficerSkillBooster() {
    }

    /**
     * Improves the promoted officers' skills.
     *
     * @param options the generation options; nothing happens unless Generate Captains is on
     * @param ranks   what the rank pass promoted
     *
     * @return how many officers were improved
     */
    public static int apply(CommandGenerationOptions options, RulesetRankAssigner.Result ranks) {
        return apply(options, ranks, bound -> Utilities.dice(1, bound) - 1);
    }

    /**
     * The same, with the random choice of command skill supplied by the caller so a test can fix it.
     *
     * @param options    the generation options; nothing happens unless Generate Captains is on
     * @param ranks      what the rank pass promoted
     * @param randomIndex given a bound, returns an index from zero to one below it
     *
     * @return how many officers were improved
     */
    static int apply(CommandGenerationOptions options, RulesetRankAssigner.Result ranks,
          IntUnaryOperator randomIndex) {
        if (!options.isGenerateCaptains()) {
            LOGGER.info("{} off; officers keep the skills they rolled", LOG_TAG);
            return 0;
        }
        if (ranks == null) {
            LOGGER.info("{} no ranks were assigned, so there are no officers to improve", LOG_TAG);
            return 0;
        }
        boolean toWeakestSkill = options.isApplyOfficerStatBonusToWorstSkill();
        int improved = 0;
        for (Map.Entry<Person, FormationLevel> promotion : ranks.officers().entrySet()) {
            Person officer = promotion.getKey();
            Map<String, Integer> before = skillLevels(officer);
            boolean isCommandingOfficer = officer.equals(ranks.rootCommander());
            if (isCommandingOfficer) {
                improveBothCombatSkills(officer);
                improveCommandSkills(officer, COMMANDING_OFFICER_INCREASES, randomIndex);
            } else {
                improveOneCombatSkill(officer, toWeakestSkill);
                int increases = isCaptainOrAbove(promotion.getValue()) ? CAPTAIN_INCREASES : LIEUTENANT_INCREASES;
                improveCommandSkills(officer, increases, randomIndex);
            }
            improved++;
            // One line per officer, a dozen or so in a battalion: it is the only way to check from the log that
            // each post got what the rule says, so it is worth the lines.
            LOGGER.info("{} '{}' ({}{}): {}", LOG_TAG, officer.getFullName(), promotion.getValue(),
                  isCommandingOfficer ? ", commanding officer" : "", changes(before, skillLevels(officer)));
        }
        LOGGER.info("{} {} officer(s) improved; combat bonus goes to the {} skill", LOG_TAG, improved,
              toWeakestSkill ? "weakest" : "strongest");
        return improved;
    }

    /**
     * @return {@code true} when the level's rank is a captain's or higher
     */
    static boolean isCaptainOrAbove(FormationLevel level) {
        return RulesetRankAssigner.rankIndexForLevel(level) >= (Rank.RWO_MAX + 4);
    }

    private static void improveBothCombatSkills(Person officer) {
        CombatSkills skills = combatSkillsOf(officer);
        if (skills == null) {
            return;
        }
        officer.improveSkill(skills.gunnery());
        officer.improveSkill(skills.piloting());
    }

    /**
     * Raises the officer's gunnery or piloting by one level: the better of the two, or the worse when asked. A
     * skill the officer lacks is the one raised, since a level in it is worth more than another level elsewhere.
     */
    static void improveOneCombatSkill(Person officer, boolean toWeakestSkill) {
        CombatSkills skills = combatSkillsOf(officer);
        if (skills == null) {
            return;
        }
        Skill gunnery = officer.getSkill(skills.gunnery());
        Skill piloting = officer.getSkill(skills.piloting());
        if ((gunnery == null) && (piloting == null)) {
            officer.improveSkill(skills.gunnery());
            officer.improveSkill(skills.piloting());
            return;
        }
        if (gunnery == null) {
            officer.improveSkill(skills.gunnery());
            return;
        }
        if (piloting == null) {
            officer.improveSkill(skills.piloting());
            return;
        }
        boolean gunneryIsHigher = gunnery.getLevel() > piloting.getLevel();
        boolean raiseGunnery = toWeakestSkill != gunneryIsHigher;
        officer.improveSkill(raiseGunnery ? skills.gunnery() : skills.piloting());
    }

    /**
     * Raises a random command skill one level per increase. A skill the officer did not have opens at level zero,
     * so it is raised once more to make the increase count.
     */
    static void improveCommandSkills(Person officer, int increases, IntUnaryOperator randomIndex) {
        for (int increase = 0; increase < increases; increase++) {
            String skillName = COMMAND_SKILLS.get(randomIndex.applyAsInt(COMMAND_SKILLS.size()));
            officer.improveSkill(skillName);
            Skill skill = officer.getSkill(skillName);
            if ((skill != null) && (skill.getLevel() == 0)) {
                officer.improveSkill(skillName);
            }
        }
    }

    /** The officer's levels in the skills this pass can touch, {@code -1} for a skill they do not have. */
    private static Map<String, Integer> skillLevels(Person officer) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        CombatSkills combat = combatSkillsOf(officer);
        List<String> names = new ArrayList<>();
        if (combat != null) {
            names.add(combat.gunnery());
            names.add(combat.piloting());
        }
        names.addAll(COMMAND_SKILLS);
        for (String name : names) {
            Skill skill = officer.getSkill(name);
            levels.put(name, (skill == null) ? -1 : skill.getLevel());
        }
        return levels;
    }

    /** For example {@code Gunnery/Mek 4->5, Leadership none->2}; skills that did not change are left out. */
    private static String changes(Map<String, Integer> before, Map<String, Integer> after) {
        List<String> changed = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : after.entrySet()) {
            int was = before.getOrDefault(entry.getKey(), -1);
            int now = entry.getValue();
            if (was != now) {
                changed.add(entry.getKey() + " " + ((was < 0) ? "none" : String.valueOf(was)) + "->" + now);
            }
        }
        return changed.isEmpty() ? "no change" : String.join(", ", changed);
    }

    /** The gunnery and piloting skill names for the unit the officer sits in. */
    private record CombatSkills(String gunnery, String piloting) {
    }

    private static CombatSkills combatSkillsOf(Person officer) {
        Unit unit = officer.getUnit();
        Entity entity = (unit == null) ? null : unit.getEntity();
        if (entity == null) {
            LOGGER.info("{} '{}' has no unit, so there is no gunnery or piloting skill to raise", LOG_TAG,
                  officer.getFullName());
            return null;
        }
        return new CombatSkills(SkillType.getGunnerySkillFor(entity), SkillType.getDrivingSkillFor(entity));
    }
}
