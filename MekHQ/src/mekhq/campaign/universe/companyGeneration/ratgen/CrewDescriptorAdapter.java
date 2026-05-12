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
package mekhq.campaign.universe.companyGeneration.ratgen;

import megamek.client.ratgenerator.CrewDescriptor;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Seeds a freshly-generated MekHQ {@link Person} from a MegaMek {@link CrewDescriptor} produced by the
 * Force Generator engine.
 *
 * <p>The descriptor carries name, gunnery skill, and piloting skill — the engine generates these in
 * {@code CrewDescriptor.setSkills()} from the user-selected experience level (Green / Regular / Veteran
 * / Elite) on the {@code ForceDescriptor}, with bonuses for unit rating and Clan unit type. This
 * adapter applies all three to the Person.</p>
 *
 * <p>Skill translation: ratgen's {@code gunnery} and {@code piloting} fields are <em>target numbers</em>
 * (a lower value means a more skilled crew). MekHQ stores skills as a {@code (level, bonus)} pair on top
 * of a fixed base target on the {@link SkillType}. Because every relevant combat skill (Piloting/Mek,
 * Gunnery/Mek, Piloting/Aero, Gunnery/Vehicle, etc.) is count-down, the final target equals
 * {@code base - level - bonus}. So MekHQ {@code level = base - descriptorValue} reproduces the
 * descriptor's target number exactly.</p>
 */
public final class CrewDescriptorAdapter {

    private static final MMLogger LOGGER = MMLogger.create(CrewDescriptorAdapter.class);

    private CrewDescriptorAdapter() {
        // utility class
    }

    /**
     * Applies the descriptor's name (when {@code overrideName} is true) and its gunnery / piloting
     * skill target numbers to the given Person. Skills are always applied; the name is gated by
     * {@code overrideName} so that only the commander on a multi-seat crew picks up the descriptor's
     * name while the rest share the descriptor's skill level but get random names.
     *
     * @param descriptor   the source descriptor from {@code ForceDescriptor.getCo()}; if {@code null},
     *                     this method is a no-op
     * @param person       the target Person; must be non-null
     * @param entity       the live {@link Entity} this Person is being assigned to; used to pick the
     *                     right skill names (Piloting/Aero vs Piloting/Mek vs Piloting/Ground Vehicle…)
     * @param overrideName when {@code true}, the descriptor's name replaces MekHQ's randomly assigned
     *                     name; when {@code false}, MekHQ's name is kept
     */
    public static void apply(CrewDescriptor descriptor, Person person, Entity entity,
          boolean overrideName) {
        if (descriptor == null || person == null) {
            LOGGER.info("[CompanyGen]         CrewDescriptorAdapter.apply skipped (descriptor or person null)");
            return;
        }
        if (overrideName) {
            applyName(descriptor, person);
        }
        if (entity != null) {
            applySkills(descriptor, person, entity);
        }
    }

    /**
     * Backward-compatible single-arg overload. Skips skill application because no entity is known.
     */
    public static void apply(CrewDescriptor descriptor, Person person, boolean overrideName) {
        apply(descriptor, person, null, overrideName);
    }

    private static void applyName(CrewDescriptor descriptor, Person person) {
        String fullName = descriptor.getName();
        if (fullName == null || fullName.isBlank()) {
            LOGGER.info("[CompanyGen]         CrewDescriptorAdapter: descriptor has blank name, keeping random");
            return;
        }
        int firstSpace = fullName.indexOf(' ');
        if (firstSpace > 0 && firstSpace < fullName.length() - 1) {
            person.setGivenName(fullName.substring(0, firstSpace));
            person.setSurname(fullName.substring(firstSpace + 1));
        } else {
            person.setGivenName(fullName);
            person.setSurname("");
        }
        person.setFullName();
        LOGGER.info("[CompanyGen]         CrewDescriptorAdapter renamed person to '{}' (gunnery={} piloting={} rank={})",
              fullName, descriptor.getGunnery(), descriptor.getPiloting(), descriptor.getRank());
    }

    private static void applySkills(CrewDescriptor descriptor, Person person, Entity entity) {
        int gunneryTarget = descriptor.getGunnery();
        int pilotingTarget = descriptor.getPiloting();

        String pilotSkillName = SkillType.getDrivingSkillFor(entity);
        String gunSkillName = SkillType.getGunnerySkillFor(entity);

        int pilotLevel = -1;
        int gunLevel = -1;
        if (pilotSkillName != null && !pilotSkillName.isBlank()) {
            pilotLevel = mekhqLevelFromTarget(pilotSkillName, pilotingTarget);
            person.addSkill(pilotSkillName, pilotLevel, 0);
        }
        if (gunSkillName != null && !gunSkillName.isBlank()) {
            gunLevel = mekhqLevelFromTarget(gunSkillName, gunneryTarget);
            person.addSkill(gunSkillName, gunLevel, 0);
        }
        LOGGER.info("[CompanyGen]         CrewDescriptorAdapter applied skills to '{}': {}=lvl{} {}=lvl{} (descriptor targets gunnery={} piloting={})",
              person.getFullName(),
              pilotSkillName, pilotLevel, gunSkillName, gunLevel,
              gunneryTarget, pilotingTarget);
    }

    /**
     * Converts a ratgen target-number (the gunnery/piloting field on {@link CrewDescriptor}) into the
     * {@code level} value MekHQ's {@link mekhq.campaign.personnel.skills.Skill} expects, so that the
     * resulting final-skill-value matches the descriptor's target. Falls back to a neutral level of 3
     * (≈ Regular) if the skill type isn't registered for some reason.
     */
    private static int mekhqLevelFromTarget(String skillName, int target) {
        SkillType type = SkillType.getType(skillName);
        if (type == null) {
            return 3;
        }
        int base = type.getTarget();
        int level = base - target;
        if (level < 0) {
            level = 0;
        }
        return level;
    }
}
