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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Profession;
import mekhq.campaign.personnel.ranks.Rank;
import mekhq.campaign.personnel.ranks.RankSystem;
import mekhq.campaign.personnel.skills.Skill;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Fills the senior staff appointments of a generated command - chief medical officer, head technician
 * and chief administrator.
 *
 * <p>An appointment is a post within a command, which is a different thing from rank (what someone is)
 * and from role (what they do). A command's most senior doctor is its chief medical officer whatever
 * rank the ladder gave them, and the post is what lets the campaign name a person rather than a job.</p>
 *
 * <p>Each post goes to the best-qualified eligible person the generation produced. "Best qualified"
 * is scored on the skills the post actually calls for: competence in the discipline itself, plus
 * Leadership and Administration, because running a section is a management job on top of a technical
 * one. Ties fall to whoever already outranks the other, so the result is stable rather than dependent
 * on the order people happened to be generated in.</p>
 *
 * <p>Posts come in two tiers. Each specific role that has a department of its own gets a department
 * head - the head Mek Tech, the head logistics administrator - and above them sit the overall posts
 * that span a whole branch: head technician across every technical role, chief administrator across
 * every administrative one, chief medical officer over medicine. Both tiers are filled on the same
 * criteria, a department head simply being judged against their own speciality.</p>
 *
 * <p>Every appointee is then promoted one populated rung above the highest-ranked of the staff they
 * lead, so a head outranks their people rather than sharing a rank with one of them. Departments are
 * filled first, so an overall head is raised above their department heads in turn. The step skips
 * rungs the rank system leaves unnamed, and stops at whichever is lower of the force commander's rank
 * and Colonel - support services neither outrank the unit commander nor rise above Colonel.</p>
 *
 * <p>Taking a post also confers Leadership, at the experience level of the appointee's own discipline
 * competence. Support staff are generated without any command skills - {@code DefaultSkillGenerator}
 * grants those to combat roles only - so a section head would otherwise be running a section with no
 * ability to lead one. This is granted after the pick, never before, so it cannot feed back into the
 * scoring that chose them.</p>
 *
 * <p>A post with no eligible candidate is simply left vacant, which is correct rather than a failure:
 * a command that generated no administrators has nobody to be its chief administrator.</p>
 */
public final class SeniorAppointmentAssigner {
    private static final MMLogger LOGGER = MMLogger.create(SeniorAppointmentAssigner.class);

    /**
     * A department needs at least this many people before it gets a head. A lone technician leads
     * nobody, and titling them head of a department they are the entirety of adds nothing.
     */
    private static final int MINIMUM_DEPARTMENT_SIZE = 2;

    /**
     * Colonel, the highest rank any support post may reach. Index 38 is the Colonel equivalent across
     * the shipped rank systems - Colonel, Sang-shao, Tai-sa, Star Colonel - and the systems that leave
     * it unnamed simply stop lower, since promotions only ever step to a named rung.
     *
     * <p>This is a hard cap on the post itself. A head is separately held to no higher than the
     * officer commanding the force, so the effective ceiling is whichever of the two is lower.</p>
     */
    private static final int RANK_COLONEL = 38;

    private SeniorAppointmentAssigner() {
    }

    /**
     * The senior posts this assigner fills, each paired with the roles eligible to hold it and the
     * {@link Person} flag that records it.
     */
    private enum Appointment {
        CHIEF_MEDICAL_OFFICER("chief medical officer",
              PersonnelRole::isDoctor,
              Set.of(SkillType.S_SURGERY),
              Person::isChiefMedicalOfficer,
              Person::setChiefMedicalOfficer),

        /**
         * Every technical role, as {@link PersonnelRole#isTech()} defines it - which includes vessel
         * crew, so a command whose only technical staff keep its large craft running still has a head
         * technician.
         */
        HEAD_TECHNICIAN("head technician",
              PersonnelRole::isTech,
              Set.of(SkillType.S_TECH_MEK, SkillType.S_TECH_MECHANIC, SkillType.S_TECH_AERO,
                    SkillType.S_TECH_BA, SkillType.S_TECH_VESSEL),
              Person::isHeadTechnician,
              Person::setHeadTechnician),

        CHIEF_ADMINISTRATOR("chief administrator",
              SeniorAppointmentAssigner::isAdministrativeRole,
              Set.of(SkillType.S_ADMIN),
              Person::isChiefAdministrator,
              Person::setChiefAdministrator);

        private final String description;
        private final Predicate<PersonnelRole> isEligible;
        private final Set<String> disciplineSkills;
        private final Predicate<Person> isHeldBy;
        private final BiConsumer<Person, Boolean> appoint;

        Appointment(String description, Predicate<PersonnelRole> isEligible, Set<String> disciplineSkills,
              Predicate<Person> isHeldBy, BiConsumer<Person, Boolean> appoint) {
            this.description = description;
            this.isEligible = isEligible;
            this.disciplineSkills = disciplineSkills;
            this.isHeldBy = isHeldBy;
            this.appoint = appoint;
        }
    }

    /**
     * Scores a candidate for a post: their best skill in the discipline, plus the two skills running a
     * section calls for whatever the discipline is.
     *
     * <p>A skill the person does not have scores zero rather than excluding them. Support staff are
     * currently generated without Leadership, so that term contributes nothing today and the pick rests
     * on the skills they do have; it starts counting the moment they have it.</p>
     *
     * @param person           the candidate to score
     * @param disciplineSkills the skills that define competence for the post being filled
     *
     * @return the candidate's score, higher being better qualified
     */
    private static int scoreFor(Person person, Set<String> disciplineSkills) {
        int disciplineScore = 0;
        for (String skillName : disciplineSkills) {
            disciplineScore = Math.max(disciplineScore, skillLevel(person, skillName));
        }

        int managementScore = skillLevel(person, SkillType.S_LEADER);
        // Administration is itself a discipline skill for administrative posts, so adding it again
        // would count it twice for those alone.
        if (!disciplineSkills.contains(SkillType.S_ADMIN)) {
            managementScore += skillLevel(person, SkillType.S_ADMIN);
        }
        return disciplineScore + managementScore;
    }

    /**
     * @return the person's level in the named skill, or {@code 0} if they do not have it
     */
    private static int skillLevel(Person person, String skillName) {
        Skill skill = person.getSkills().getSkill(skillName);
        return (skill == null) ? 0 : skill.getLevel();
    }

    /**
     * Appoints the senior staff of a generated command.
     *
     * <p>A post already held by somebody in the campaign is left alone. That matters when support is
     * regenerated against a committed TOE, or when support is added to an existing campaign: a player
     * who appointed their own chief medical officer should not have the generator quietly replace
     * them.</p>
     *
     * @param campaign   the campaign being generated into, used for the rank comparison and to detect
     *                   posts that are already filled
     * @param candidates the people this generation created, from which the posts are filled
     */
    public static void assign(Campaign campaign, Collection<Person> candidates) {
        if (candidates.isEmpty()) {
            LOGGER.debug("[CompanyGen][Appointments] no generated personnel; nothing to appoint");
            return;
        }

        Collection<Person> existingPersonnel = campaign.getPlayerForce().getHumanResources().getPersonnel();
        // Two limits, both of which must hold: no support post outranks the force commander, and no
        // support post exceeds Colonel however senior that commander is.
        int rankCeiling = Math.min(RANK_COLONEL, commanderRankCeiling(existingPersonnel, candidates));
        // Departments first: an overall head is promoted above the heads below them, which only works
        // if those heads already hold the rank they are entitled to.
        assignDepartmentHeads(campaign, candidates, existingPersonnel, rankCeiling);
        for (Appointment appointment : Appointment.values()) {
            assignOne(campaign, appointment, candidates, existingPersonnel, rankCeiling);
        }
    }

    /**
     * Works out how far a post-holder may be promoted: no further than the officer commanding the
     * force, because support services do not outrank the unit commander.
     *
     * <p>The commander is identified by rank rather than by the commander flag, which the pipeline
     * does not set until after support generation has run. Ranks have been assigned by that point, so
     * the highest-ranked person on the books is the commander.</p>
     *
     * <p>The staff being appointed are excluded from the search. They are already on the books by this
     * point, and letting them count would have a section measuring itself against its own people
     * rather than against the officer commanding the force.</p>
     *
     * @param existingPersonnel everyone already on the books, including the ranked combat command
     * @param candidates        the people this generation created, excluded from the search
     *
     * @return the commander's rank, or {@link #RANK_COLONEL} if nobody outside the generated staff
     *       holds a rank to measure against
     */
    private static int commanderRankCeiling(Collection<Person> existingPersonnel,
          Collection<Person> candidates) {
        Set<Person> generatedStaff = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Person candidate : candidates) {
            if (candidate != null) {
                generatedStaff.add(candidate);
            }
        }

        int highestRank = Integer.MIN_VALUE;
        for (Person person : existingPersonnel) {
            if ((person != null) && !generatedStaff.contains(person)) {
                highestRank = Math.max(highestRank, person.getRankNumeric());
            }
        }
        if (highestRank == Integer.MIN_VALUE) {
            LOGGER.debug("[CompanyGen][Appointments] no commander outside the generated staff; "
                  + "capping promotions at {}", RANK_COLONEL);
            return RANK_COLONEL;
        }
        LOGGER.debug("[CompanyGen][Appointments] capping promotions at rank {}, the force commander's",
              highestRank);
        return highestRank;
    }

    /**
     * Gives every department represented in the generated staff its own head, judged on that
     * department's own skills.
     *
     * <p>A department of one is left without a head: a lone Mek Tech leads nobody, and titling them
     * head of a department they are the entirety of adds nothing. Departments are the specific roles
     * beneath the branch-wide posts, so medicine is excluded - the chief medical officer already heads
     * it.</p>
     *
     * @param campaign          the campaign supplying rank comparison rules
     * @param candidates        the people this generation created
     * @param existingPersonnel everyone already on the books, used to spot posts that are filled
     * @param rankCeiling       the highest rank a head may be promoted to
     */
    private static void assignDepartmentHeads(Campaign campaign, Collection<Person> candidates,
          Collection<Person> existingPersonnel, int rankCeiling) {
        Map<PersonnelRole, List<Person>> byDepartment = new LinkedHashMap<>();
        for (Person candidate : candidates) {
            if (candidate == null) {
                continue;
            }
            PersonnelRole role = candidate.getPrimaryRole();
            if (!isDepartmentRole(role)) {
                continue;
            }
            byDepartment.computeIfAbsent(role, key -> new ArrayList<>()).add(candidate);
        }

        for (Map.Entry<PersonnelRole, List<Person>> department : byDepartment.entrySet()) {
            PersonnelRole role = department.getKey();
            List<Person> staff = department.getValue();
            if (staff.size() < MINIMUM_DEPARTMENT_SIZE) {
                LOGGER.debug("[CompanyGen][Appointments] no head for {}; only {} in the department",
                      role, staff.size());
                continue;
            }

            Person incumbent = firstMatching(existingPersonnel,
                  person -> person.isDepartmentHead() && (person.getPrimaryRole() == role));
            if (incumbent != null) {
                LOGGER.debug("[CompanyGen][Appointments] head of {} left as is; already held by '{}'",
                      role, incumbent.getFullName());
                continue;
            }

            Set<String> disciplineSkills = Set.copyOf(role.getSkillsForProfession());
            Person head = bestCandidate(campaign, staff, disciplineSkills);
            if (head == null) {
                continue;
            }
            head.setDepartmentHead(true);
            LOGGER.info("[CompanyGen][Appointments] head of {} -> '{}', chosen from {} in the department",
                  role, head.getFullName(), staff.size());
            promoteToOutrankTheirStaff(head, staff, "head of " + role, rankCeiling);
            grantLeadership(head, disciplineSkills, "head of " + role);
        }
    }

    /**
     * @return {@code true} if this role runs as a department of its own, beneath a branch-wide post
     */
    private static boolean isDepartmentRole(PersonnelRole role) {
        return role.isTech() || isAdministrativeRole(role);
    }

    /**
     * Whether the role heads an administrative department.
     *
     * <p>{@link PersonnelRole#isAdministrator()} matches only the consolidated {@code ADMINISTRATOR}
     * role, but the support generator still creates the four speciality roles, so a force built here
     * would otherwise have no administrative department heads at all. When those roles are finally
     * removed this collapses back to the single consolidated role.</p>
     *
     * @param role the role to test
     *
     * @return {@code true} if the role belongs to an administrative department
     */
    private static boolean isAdministrativeRole(PersonnelRole role) {
        return switch (role) {
            case ADMINISTRATOR, ADMINISTRATOR_COMMAND, ADMINISTRATOR_LOGISTICS, ADMINISTRATOR_TRANSPORT,
                 ADMINISTRATOR_HR -> true;
            default -> false;
        };
    }

    /**
     * Picks the best-qualified person from a pool, scoring on the given discipline skills.
     *
     * @param campaign         the campaign supplying rank comparison rules
     * @param pool             the people to choose between
     * @param disciplineSkills the skills that define competence for this post
     *
     * @return the best-qualified person, or {@code null} if the pool is empty
     */
    private static @Nullable Person bestCandidate(Campaign campaign, List<Person> pool,
          Set<String> disciplineSkills) {
        Person bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        for (Person candidate : pool) {
            int score = scoreFor(candidate, disciplineSkills);
            boolean outscoresBest = (bestCandidate == null) || (score > bestScore);
            boolean tiedButOutranksBest = (bestCandidate != null) && (score == bestScore)
                  && candidate.outRanksUsingSkillTiebreaker(campaign, bestCandidate);
            if (outscoresBest || tiedButOutranksBest) {
                bestCandidate = candidate;
                bestScore = score;
            }
        }
        return bestCandidate;
    }

    /**
     * Fills a single post with the highest-ranking eligible candidate, if it is vacant and anyone
     * qualifies.
     */
    private static void assignOne(Campaign campaign, Appointment appointment, Collection<Person> candidates,
          Collection<Person> existingPersonnel, int rankCeiling) {
        Person incumbent = firstMatching(existingPersonnel, appointment.isHeldBy);
        if (incumbent != null) {
            LOGGER.debug("[CompanyGen][Appointments] {} left as is; already held by '{}'",
                  appointment.description, incumbent.getFullName());
            return;
        }

        List<Person> eligibleStaff = new ArrayList<>();
        for (Person candidate : candidates) {
            if ((candidate != null) && appointment.isEligible.test(candidate.getPrimaryRole())) {
                eligibleStaff.add(candidate);
            }
        }

        Person bestCandidate = bestCandidate(campaign, eligibleStaff, appointment.disciplineSkills);
        if (bestCandidate == null) {
            LOGGER.debug("[CompanyGen][Appointments] {} left vacant; no generated person holds an "
                  + "eligible primary role", appointment.description);
            return;
        }

        appointment.appoint.accept(bestCandidate, true);
        LOGGER.info("[CompanyGen][Appointments] {} -> '{}' ({}), chosen from {} eligible",
              appointment.description, bestCandidate.getFullName(),
              bestCandidate.getPrimaryRole(), eligibleStaff.size());
        promoteToOutrankTheirStaff(bestCandidate, eligibleStaff, appointment.description, rankCeiling);
        grantLeadership(bestCandidate, appointment.disciplineSkills, appointment.description);
    }

    /**
     * Gives the appointee the Leadership skill their new post requires, at the experience level of
     * their own discipline competence - an elite surgeon leads their medical section as capably as they
     * operate.
     *
     * <p>Does nothing if they already lead at least that well, so a person who arrived with Leadership
     * is never downgraded. Does nothing either if they have no discipline skill to scale from, since
     * there is then no defensible level to grant.</p>
     *
     * @param appointee        the person who just took the post
     * @param disciplineSkills the skills to scale the granted Leadership from
     * @param description      the post, for logging
     */
    private static void grantLeadership(Person appointee, Set<String> disciplineSkills,
          String description) {
        int disciplineLevel = 0;
        String disciplineSkillName = null;
        for (String skillName : disciplineSkills) {
            int level = skillLevel(appointee, skillName);
            if (level > disciplineLevel) {
                disciplineLevel = level;
                disciplineSkillName = skillName;
            }
        }
        if (disciplineSkillName == null) {
            LOGGER.debug("[CompanyGen][Appointments] no Leadership granted to {} '{}'; they have no "
                  + "discipline skill to scale from", description, appointee.getFullName());
            return;
        }

        int experienceLevel = SkillType.getType(disciplineSkillName).getExperienceLevel(disciplineLevel);
        Skill granted = Skill.createFromExperience(SkillType.S_LEADER, experienceLevel, 0);
        Skill existing = appointee.getSkills().getSkill(SkillType.S_LEADER);
        if ((existing != null) && (existing.getLevel() >= granted.getLevel())) {
            LOGGER.debug("[CompanyGen][Appointments] {} '{}' already leads at level {}; leaving it",
                  description, appointee.getFullName(), existing.getLevel());
            return;
        }

        appointee.addSkill(SkillType.S_LEADER, granted);
        LOGGER.info("[CompanyGen][Appointments] granted Leadership {} to {} '{}', scaled from {} {}",
              granted.getLevel(), description, appointee.getFullName(),
              disciplineSkillName, disciplineLevel);
    }

    /**
     * Raises the appointee one named rung above the highest-ranked of the staff they lead, so a head
     * outranks their people instead of sharing a rank with one of them.
     *
     * <p>Ranks are stepped to rather than calculated: the search walks up until it finds a rung this
     * rank system actually names, so a promotion can never strand someone on a blank rung. It stops at
     * {@code rankCeiling}, so no post-holder is carried past the officer commanding the force.</p>
     *
     * @param appointee   the person who just took the post
     * @param staff       everyone eligible for the post, including the appointee, who is skipped
     * @param description the post, for logging
     * @param rankCeiling the highest rank this promotion may reach
     */
    private static void promoteToOutrankTheirStaff(Person appointee, List<Person> staff,
          String description, int rankCeiling) {
        int topStaffRank = Integer.MIN_VALUE;
        for (Person member : staff) {
            if (!member.equals(appointee)) {
                topStaffRank = Math.max(topStaffRank, member.getRankNumeric());
            }
        }
        if (topStaffRank == Integer.MIN_VALUE) {
            LOGGER.debug("[CompanyGen][Appointments] {} '{}' leads nobody; rank unchanged",
                  description, appointee.getFullName());
            return;
        }

        int floorRank = Math.max(topStaffRank, appointee.getRankNumeric());
        int targetRank = nextNamedRankAbove(appointee, floorRank, rankCeiling);
        if (targetRank <= appointee.getRankNumeric()) {
            LOGGER.debug("[CompanyGen][Appointments] {} '{}' stays at rank {}; no named rung above {} "
                        + "within the ladder ceiling", description, appointee.getFullName(),
                  appointee.getRankNumeric(), floorRank);
            return;
        }
        LOGGER.info("[CompanyGen][Appointments] promoting {} '{}' from rank {} to {}, one step above "
                    + "their highest-ranked staff at {}", description, appointee.getFullName(),
              appointee.getRankNumeric(), targetRank, topStaffRank);
        appointee.setRank(targetRank);
    }

    /**
     * Finds the lowest rank above {@code floorRank} that this person's rank system gives a real name,
     * so a promotion never lands on a blank rung.
     *
     * @param person      the person whose rank system and profession decide which rungs are named
     * @param floorRank   the rank to step up from
     * @param rankCeiling the highest rank this search may reach
     *
     * @return the next named rank above {@code floorRank}, or {@code floorRank} if the system names
     *       none below the ceiling
     */
    private static int nextNamedRankAbove(Person person, int floorRank, int rankCeiling) {
        RankSystem rankSystem = person.getRankSystem();
        if (rankSystem == null) {
            return floorRank;
        }
        Profession baseProfession = Profession.getProfessionFromPersonnelRole(person.getPrimaryRole());
        boolean hasOwnLadder = namesRanksOfItsOwn(rankSystem, baseProfession);
        int ceiling = Math.min(rankCeiling, rankSystem.getRanks().size() - 1);
        for (int index = floorRank + 1; index <= ceiling; index++) {
            Rank candidate = rankSystem.getRank(index);
            if (candidate == null) {
                continue;
            }
            boolean isNamedForThisPerson = hasOwnLadder
                  ? namesThisProfession(candidate, baseProfession)
                  : !candidate.isEmpty(baseProfession.getProfession(rankSystem, candidate));
            if (isNamedForThisPerson) {
                return index;
            }
        }
        return floorRank;
    }

    /**
     * Whether this profession has ranks of its own in this system, as opposed to borrowing another
     * profession's column.
     *
     * <p>Most systems name only the warrior column and point everything else at it, so a doctor's rank
     * legitimately comes from there. The Clan system names technicians, medics and administrators
     * separately, and a promotion inside those professions must stay in their own column - stepping up
     * from Head Technician would otherwise land on the warrior rung above it and retitle the person a
     * Point Commander.</p>
     *
     * @param rankSystem the rank system to inspect
     * @param profession the profession to look for
     *
     * @return {@code true} if any rank in the system names this profession directly
     */
    private static boolean namesRanksOfItsOwn(RankSystem rankSystem, Profession profession) {
        for (Rank rank : rankSystem.getRanks()) {
            if ((rank != null) && namesThisProfession(rank, profession)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return {@code true} if this rank gives the profession a name of its own, rather than being blank
     *       or a pointer at another profession's column
     */
    private static boolean namesThisProfession(Rank rank, Profession profession) {
        return !rank.isEmpty(profession) && !rank.indicatesAlternativeSystem(profession);
    }

    /**
     * @return the first person the predicate accepts, or {@code null} when none does
     */
    private static @Nullable Person firstMatching(Collection<Person> people, Predicate<Person> predicate) {
        List<Person> matches = people.stream().filter(predicate).limit(1).toList();
        return matches.isEmpty() ? null : matches.getFirst();
    }
}
