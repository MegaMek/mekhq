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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
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
 * <p>The appointee is then promoted to match the highest-ranked person eligible for the same post, so
 * a section head is never outranked by their own staff. That rank is copied from the peer rather than
 * calculated, so it can only land on a rung the rank system actually names.</p>
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
              PersonnelRole::isAdministrator,
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
     * @param person      the candidate to score
     * @param appointment the post being filled, which decides the discipline skills
     *
     * @return the candidate's score, higher being better qualified
     */
    private static int scoreFor(Person person, Appointment appointment) {
        int disciplineScore = 0;
        for (String skillName : appointment.disciplineSkills) {
            disciplineScore = Math.max(disciplineScore, skillLevel(person, skillName));
        }

        int managementScore = skillLevel(person, SkillType.S_LEADER);
        // Administration is the discipline skill for the chief administrator, so adding it again would
        // count it twice for that post alone.
        if (!appointment.disciplineSkills.contains(SkillType.S_ADMIN)) {
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

        Collection<Person> existingPersonnel = campaign.getPersonnel().values();
        for (Appointment appointment : Appointment.values()) {
            assignOne(campaign, appointment, candidates, existingPersonnel);
        }
    }

    /**
     * Fills a single post with the highest-ranking eligible candidate, if it is vacant and anyone
     * qualifies.
     */
    private static void assignOne(Campaign campaign, Appointment appointment, Collection<Person> candidates,
          Collection<Person> existingPersonnel) {
        Person incumbent = firstMatching(existingPersonnel, appointment.isHeldBy);
        if (incumbent != null) {
            LOGGER.debug("[CompanyGen][Appointments] {} left as is; already held by '{}'",
                  appointment.description, incumbent.getFullName());
            return;
        }

        Person bestCandidate = null;
        int bestScore = Integer.MIN_VALUE;
        Person highestRankedPeer = null;
        int eligibleCount = 0;
        for (Person candidate : candidates) {
            if ((candidate == null) || !appointment.isEligible.test(candidate.getPrimaryRole())) {
                continue;
            }
            eligibleCount++;

            if ((highestRankedPeer == null)
                      || candidate.outRanksUsingSkillTiebreaker(campaign, highestRankedPeer)) {
                highestRankedPeer = candidate;
            }

            int score = scoreFor(candidate, appointment);
            boolean outscoresBest = (bestCandidate == null) || (score > bestScore);
            boolean tiedButOutranksBest = (bestCandidate != null) && (score == bestScore)
                  && candidate.outRanksUsingSkillTiebreaker(campaign, bestCandidate);
            if (outscoresBest || tiedButOutranksBest) {
                bestCandidate = candidate;
                bestScore = score;
            }
        }

        if (bestCandidate == null) {
            LOGGER.debug("[CompanyGen][Appointments] {} left vacant; no generated person holds an "
                  + "eligible primary role", appointment.description);
            return;
        }

        appointment.appoint.accept(bestCandidate, true);
        LOGGER.info("[CompanyGen][Appointments] {} -> '{}' ({}) score={}, chosen from {} eligible",
              appointment.description, bestCandidate.getFullName(),
              bestCandidate.getPrimaryRole(), bestScore, eligibleCount);
        promoteToOutrankTheirStaff(bestCandidate, highestRankedPeer, appointment);
        grantLeadership(bestCandidate, appointment);
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
     * @param appointee   the person who just took the post
     * @param appointment the post, which decides the discipline skills to scale from
     */
    private static void grantLeadership(Person appointee, Appointment appointment) {
        int disciplineLevel = 0;
        String disciplineSkillName = null;
        for (String skillName : appointment.disciplineSkills) {
            int level = skillLevel(appointee, skillName);
            if (level > disciplineLevel) {
                disciplineLevel = level;
                disciplineSkillName = skillName;
            }
        }
        if (disciplineSkillName == null) {
            LOGGER.debug("[CompanyGen][Appointments] no Leadership granted to {} '{}'; they have no "
                  + "discipline skill to scale from", appointment.description, appointee.getFullName());
            return;
        }

        int experienceLevel = SkillType.getType(disciplineSkillName).getExperienceLevel(disciplineLevel);
        Skill granted = Skill.createFromExperience(SkillType.S_LEADER, experienceLevel, 0);
        Skill existing = appointee.getSkills().getSkill(SkillType.S_LEADER);
        if ((existing != null) && (existing.getLevel() >= granted.getLevel())) {
            LOGGER.debug("[CompanyGen][Appointments] {} '{}' already leads at level {}; leaving it",
                  appointment.description, appointee.getFullName(), existing.getLevel());
            return;
        }

        appointee.addSkill(SkillType.S_LEADER, granted);
        LOGGER.info("[CompanyGen][Appointments] granted Leadership {} to {} '{}', scaled from {} {}",
              granted.getLevel(), appointment.description, appointee.getFullName(),
              disciplineSkillName, disciplineLevel);
    }

    /**
     * Raises the appointee to the rank of the highest-ranked person eligible for the same post, so a
     * section head is not outranked by their own staff.
     *
     * <p>The rank is copied from that peer rather than calculated. A peer's rank already resolves to a
     * real name in this rank system, so copying it cannot strand the appointee on an unnamed rung the
     * way picking an index can.</p>
     *
     * @param appointee         the person who just took the post
     * @param highestRankedPeer the highest-ranked candidate for the same post, or {@code null} if
     *                          there were none
     * @param appointment       the post, for logging
     */
    private static void promoteToOutrankTheirStaff(Person appointee, @Nullable Person highestRankedPeer,
          Appointment appointment) {
        if ((highestRankedPeer == null) || highestRankedPeer.equals(appointee)) {
            return;
        }
        int peerRank = highestRankedPeer.getRankNumeric();
        if (appointee.getRankNumeric() >= peerRank) {
            return;
        }
        LOGGER.info("[CompanyGen][Appointments] promoting {} '{}' from rank {} to {}, matching their "
                    + "highest-ranked peer '{}'", appointment.description, appointee.getFullName(),
              appointee.getRankNumeric(), peerRank, highestRankedPeer.getFullName());
        appointee.setRank(peerRank);
    }

    /**
     * @return the first person the predicate accepts, or {@code null} when none does
     */
    private static @Nullable Person firstMatching(Collection<Person> people, Predicate<Person> predicate) {
        List<Person> matches = people.stream().filter(predicate).limit(1).toList();
        return matches.isEmpty() ? null : matches.getFirst();
    }
}
