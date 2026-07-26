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
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Fills the senior staff appointments of a generated command - chief medical officer, head technician
 * and chief administrator.
 *
 * <p>An appointment is a post within a command, which is a different thing from rank (what someone is)
 * and from role (what they do). A command's most senior doctor is its chief medical officer whatever
 * rank the ladder gave them, and the post is what lets the campaign name a person rather than a job.</p>
 *
 * <p>Each post goes to the highest-ranking eligible person the generation produced, resolved with
 * {@link Person#outRanksUsingSkillTiebreaker(Campaign, Person)} so that a tie on rank falls to the more
 * skilled candidate - the same comparison MekHQ already uses to pick a commander. A post with no
 * eligible candidate is simply left vacant, which is correct rather than a failure: a command that
 * generated no administrators has nobody to be its chief administrator.</p>
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
              Person::isChiefMedicalOfficer,
              Person::setChiefMedicalOfficer),

        /**
         * Every technical role, as {@link PersonnelRole#isTech()} defines it - which includes vessel
         * crew, so a command whose only technical staff keep its large craft running still has a head
         * technician.
         */
        HEAD_TECHNICIAN("head technician",
              PersonnelRole::isTech,
              Person::isHeadTechnician,
              Person::setHeadTechnician),

        CHIEF_ADMINISTRATOR("chief administrator",
              PersonnelRole::isAdministrator,
              Person::isChiefAdministrator,
              Person::setChiefAdministrator);

        private final String description;
        private final Predicate<PersonnelRole> isEligible;
        private final Predicate<Person> isHeldBy;
        private final BiConsumer<Person, Boolean> appoint;

        Appointment(String description, Predicate<PersonnelRole> isEligible, Predicate<Person> isHeldBy,
              BiConsumer<Person, Boolean> appoint) {
            this.description = description;
            this.isEligible = isEligible;
            this.isHeldBy = isHeldBy;
            this.appoint = appoint;
        }
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

        Person seniorCandidate = null;
        int eligibleCount = 0;
        for (Person candidate : candidates) {
            if ((candidate == null) || !appointment.isEligible.test(candidate.getPrimaryRole())) {
                continue;
            }
            eligibleCount++;
            if ((seniorCandidate == null)
                      || candidate.outRanksUsingSkillTiebreaker(campaign, seniorCandidate)) {
                seniorCandidate = candidate;
            }
        }

        if (seniorCandidate == null) {
            LOGGER.debug("[CompanyGen][Appointments] {} left vacant; no generated person holds an "
                  + "eligible primary role", appointment.description);
            return;
        }

        appointment.appoint.accept(seniorCandidate, true);
        LOGGER.info("[CompanyGen][Appointments] {} -> '{}' ({}), chosen from {} eligible",
              appointment.description, seniorCandidate.getFullName(),
              seniorCandidate.getPrimaryRole(), eligibleCount);
    }

    /**
     * @return the first person the predicate accepts, or {@code null} when none does
     */
    private static @Nullable Person firstMatching(Collection<Person> people, Predicate<Person> predicate) {
        List<Person> matches = people.stream().filter(predicate).limit(1).toList();
        return matches.isEmpty() ? null : matches.getFirst();
    }
}
