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
package mekhq.campaign.personnel.skills;

import static mekhq.campaign.personnel.skills.SkillType.S_TECH_AERONAUTICS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_CYBERNETICS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_ELECTRONIC;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_JETS;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MECHANICAL;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_MYOMER;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_NUCLEAR;
import static mekhq.campaign.personnel.skills.SkillType.S_TECH_WEAPONS;
import static mekhq.utilities.MHQInternationalization.getFormattedTextAt;
import static mekhq.utilities.MHQInternationalization.getTextAt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.Refit;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.work.IPartWork;

/**
 * Works out a technician's specialization, a short label such as {@code Myomer, Engineer} that tells the player at a
 * glance what a tech is best used for, without reading through their skills and abilities.
 *
 * <p>Nothing is stored: the specialization is read from what the tech already has, so it can never fall out of step
 * with the rules that use it. It has two parts:</p>
 *
 * <ul>
 *     <li><b>Discipline</b> - one granular {@code Tech/...} skill. A Tech Specialist SPA names it directly. Without
 *     one, a discipline is picked from skill levels, but only when one granular skill is at least
 *     {@link #INFERRED_DISCIPLINE_MARGIN} levels above every other granular skill the tech has, so ties and near ties
 *     are never guessed. There is no discipline while the campaign uses global tech skills only, as granular skills are
 *     not rolled then.</li>
 *     <li><b>Focus abilities</b> - the older Weapon, Armor and Internal Specialist SPAs, Engineer, Maintainer and
 *     Mr/Ms Fix-it.</li>
 * </ul>
 *
 * <p>Only people with a technician profession, primary or secondary, have a specialization.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class TechSpecialization {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.Campaign";

    /**
     * How many levels a granular tech skill must be above every other granular tech skill the tech has before it is
     * picked as their discipline without a Tech Specialist SPA.
     */
    static final int INFERRED_DISCIPLINE_MARGIN = 2;

    /** The granular tech skills that can be a discipline, in the order they are checked */
    private static final List<String> DISCIPLINE_SKILLS = List.of(S_TECH_ELECTRONIC,
          S_TECH_NUCLEAR,
          S_TECH_AERONAUTICS,
          S_TECH_MECHANICAL,
          S_TECH_MYOMER,
          S_TECH_JETS,
          S_TECH_WEAPONS,
          S_TECH_CYBERNETICS);

    /** The resource key of each discipline's label */
    private static final Map<String, String> DISCIPLINE_LABEL_KEYS = Map.of(
          S_TECH_ELECTRONIC, "TechSpecialization.discipline.electronic",
          S_TECH_NUCLEAR, "TechSpecialization.discipline.nuclear",
          S_TECH_AERONAUTICS, "TechSpecialization.discipline.aeronautics",
          S_TECH_MECHANICAL, "TechSpecialization.discipline.mechanical",
          S_TECH_MYOMER, "TechSpecialization.discipline.myomer",
          S_TECH_JETS, "TechSpecialization.discipline.jets",
          S_TECH_WEAPONS, "TechSpecialization.discipline.weapons",
          S_TECH_CYBERNETICS, "TechSpecialization.discipline.cybernetics");

    /** The focus abilities, in the order they are listed after the discipline */
    private static final List<String> FOCUS_ABILITIES = List.of(PersonnelOptions.TECH_WEAPON_SPECIALIST,
          PersonnelOptions.TECH_ARMOR_SPECIALIST,
          PersonnelOptions.TECH_INTERNAL_SPECIALIST,
          PersonnelOptions.TECH_ENGINEER,
          PersonnelOptions.TECH_MAINTAINER,
          PersonnelOptions.TECH_FIXER);

    /** The resource key of each focus ability's label */
    private static final Map<String, String> FOCUS_LABEL_KEYS = Map.of(
          PersonnelOptions.TECH_WEAPON_SPECIALIST, "TechSpecialization.focus.weaponSpecialist",
          PersonnelOptions.TECH_ARMOR_SPECIALIST, "TechSpecialization.focus.armorSpecialist",
          PersonnelOptions.TECH_INTERNAL_SPECIALIST, "TechSpecialization.focus.internalSpecialist",
          PersonnelOptions.TECH_ENGINEER, "TechSpecialization.focus.engineer",
          PersonnelOptions.TECH_MAINTAINER, "TechSpecialization.focus.maintainer",
          PersonnelOptions.TECH_FIXER, "TechSpecialization.focus.fixer");

    private TechSpecialization() {}

    /**
     * @param campaign the campaign
     *
     * @return {@code true} if the campaign resolves repairs with global tech skills only, in which case no tech has a
     *       discipline
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean usesGlobalTechSkillsOnly(Campaign campaign) {
        return campaign.getCampaignOptions().get(CampaignOption.USE_GLOBAL_TECH_SKILLS_ONLY);
    }

    /**
     * @param person a person
     *
     * @return {@code true} if the person has a technician profession, primary or secondary
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isTechnician(Person person) {
        return person.getPrimaryRole().isTech() || person.getSecondaryRole().isTech();
    }

    /**
     * @param person                  a person
     * @param useGlobalTechSkillsOnly whether the campaign resolves repairs with global tech skills only
     *
     * @return the name of the granular tech skill that is the person's discipline, or {@code null} if they have none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable String getDisciplineSkill(Person person, boolean useGlobalTechSkillsOnly) {
        if (useGlobalTechSkillsOnly || !isTechnician(person)) {
            return null;
        }

        String specialistSkill = getTechSpecialistSkill(person.getOptions());
        if (specialistSkill != null) {
            return specialistSkill;
        }

        return inferDisciplineFromSkillLevels(person);
    }

    /**
     * @param options a person's abilities
     *
     * @return the granular tech skill named by the person's Tech Specialist SPA, or {@code null} if they hold none
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable String getTechSpecialistSkill(PersonnelOptions options) {
        for (String skillName : DISCIPLINE_SKILLS) {
            String specialistAbility = Skill.TECH_SPECIALIST_SPA_BY_SKILL.get(skillName);
            if ((specialistAbility != null) && options.booleanOption(specialistAbility)) {
                return skillName;
            }
        }
        return null;
    }

    /**
     * Picks a discipline from skill levels: the granular tech skill whose level is at least
     * {@link #INFERRED_DISCIPLINE_MARGIN} above every other granular tech skill the person has. Bonuses and modifiers
     * are ignored, so the pick follows what the person has trained.
     *
     * @param person a person
     *
     * @return the granular tech skill that stands out, or {@code null} if none does or the person has fewer than two
     *       granular tech skills to compare
     *
     * @author Illiani
     * @since 0.51.01
     */
    static @Nullable String inferDisciplineFromSkillLevels(Person person) {
        String highestSkillName = null;
        int highestLevel = Integer.MIN_VALUE;
        int secondHighestLevel = Integer.MIN_VALUE;
        int skillsCompared = 0;

        for (String skillName : DISCIPLINE_SKILLS) {
            Skill skill = person.getSkill(skillName);
            if (skill == null) {
                continue;
            }

            skillsCompared++;
            int level = skill.getLevel();
            if (level > highestLevel) {
                secondHighestLevel = highestLevel;
                highestLevel = level;
                highestSkillName = skillName;
            } else if (level > secondHighestLevel) {
                secondHighestLevel = level;
            }
        }

        if (skillsCompared < 2) {
            return null;
        }

        return ((highestLevel - secondHighestLevel) >= INFERRED_DISCIPLINE_MARGIN) ? highestSkillName : null;
    }

    /**
     * @param person                  a person
     * @param useGlobalTechSkillsOnly whether the campaign resolves repairs with global tech skills only
     *
     * @return the labels of the person's specialization, discipline first, for example {@code [Myomer, Engineer]}; empty
     *       if they have none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static List<String> getLabels(Person person, boolean useGlobalTechSkillsOnly) {
        List<String> labels = new ArrayList<>();
        if (!isTechnician(person)) {
            return labels;
        }

        String disciplineSkill = getDisciplineSkill(person, useGlobalTechSkillsOnly);
        if (disciplineSkill != null) {
            labels.add(getTextAt(RESOURCE_BUNDLE, DISCIPLINE_LABEL_KEYS.get(disciplineSkill)));
        }

        PersonnelOptions options = person.getOptions();
        for (String ability : FOCUS_ABILITIES) {
            if (!options.booleanOption(ability)) {
                continue;
            }

            // The Weapon Specialist and the Tech/Weapons discipline share a label, so it is only listed once
            String label = getTextAt(RESOURCE_BUNDLE, FOCUS_LABEL_KEYS.get(ability));
            if (!labels.contains(label)) {
                labels.add(label);
            }
        }
        return labels;
    }

    /**
     * @param person                  a person
     * @param useGlobalTechSkillsOnly whether the campaign resolves repairs with global tech skills only
     *
     * @return the person's specialization as one comma separated label, for example {@code Myomer, Engineer}, or an
     *       empty string if they have none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String describe(Person person, boolean useGlobalTechSkillsOnly) {
        return String.join(", ", getLabels(person, useGlobalTechSkillsOnly));
    }

    /**
     * @param person   a person
     * @param campaign the campaign
     *
     * @return the person's specialization in brackets with a leading space, ready to follow their role, for example
     *       {@code  (Myomer, Engineer)}, or an empty string if they have none
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static String describeAsRoleSuffix(Person person, Campaign campaign) {
        String specialization = describe(person, usesGlobalTechSkillsOnly(campaign));
        if (specialization.isEmpty()) {
            return "";
        }
        return getFormattedTextAt(RESOURCE_BUNDLE, "TechSpecialization.roleSuffix", specialization);
    }

    /**
     * Whether the person's specialization fits a job: their discipline is one of the skills the job names (jobs that
     * accept any tech skill fit no discipline), a focus ability helps with that kind of part, or they are an Engineer
     * and the job is a refit.
     *
     * @param person                  a person
     * @param useGlobalTechSkillsOnly whether the campaign resolves repairs with global tech skills only
     * @param work                    the job, or {@code null} if no job is selected
     *
     * @return {@code true} if the person is a specialist for the job
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static boolean isSpecialistFor(Person person, boolean useGlobalTechSkillsOnly, @Nullable IPartWork work) {
        if ((work == null) || !isTechnician(person)) {
            return false;
        }

        PersonnelOptions options = person.getOptions();
        if (work instanceof Refit) {
            return options.booleanOption(PersonnelOptions.TECH_ENGINEER);
        }

        String disciplineSkill = getDisciplineSkill(person, useGlobalTechSkillsOnly);
        if ((disciplineSkill != null) && isSkillSpecific(work) && work.isRightTechType(disciplineSkill)) {
            return true;
        }

        if (work instanceof Part part) {
            return (options.booleanOption(PersonnelOptions.TECH_WEAPON_SPECIALIST) && part.isWeaponSpecialistWork()) ||
                         (options.booleanOption(PersonnelOptions.TECH_ARMOR_SPECIALIST) &&
                                part.isArmorSpecialistWork()) ||
                         (options.booleanOption(PersonnelOptions.TECH_INTERNAL_SPECIALIST) &&
                                part.isInternalSpecialistWork());
        }
        return false;
    }

    /**
     * Whether a job names the skills it is repaired with. Some jobs, such as ammo bins and equipment that is not a
     * weapon, accept every tech skill, so no discipline is special to them.
     *
     * @param work a job
     *
     * @return {@code true} if at least one tech skill is not accepted for the job
     *
     * @author Illiani
     * @since 0.51.01
     */
    static boolean isSkillSpecific(IPartWork work) {
        for (String skillName : SkillType.getTechSkills()) {
            if (!work.isRightTechType(skillName)) {
                return true;
            }
        }
        return false;
    }
}
