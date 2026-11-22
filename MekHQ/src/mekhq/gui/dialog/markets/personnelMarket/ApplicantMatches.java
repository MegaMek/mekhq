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
package mekhq.gui.dialog.markets.personnelMarket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.gui.dialog.markets.personnelMarket.PersonnelMarketDialog.ApplicantFacts;

/**
 * Which applicants the personnel market's filters let through, and the count beside each filter on the rail. A filter's
 * count goes by every choice the player has made except those it competes with: a profession's count leaves out the
 * chosen role, and an experience level's leaves out the ticked levels, so each says how many applicants choosing it
 * would find.
 *
 * @author Illiani
 * @since 0.51.01
 */
final class ApplicantMatches {
    private final List<Person> shown = new ArrayList<>();
    private final Map<PersonnelRole, Integer> professions = new EnumMap<>(PersonnelRole.class);
    private final Map<Integer, Integer> levels = new HashMap<>();
    private int withAbilities;
    private int withFlaws;

    /**
     * @param applicants       the applicants the search lets through
     * @param facts            each applicant's experience level and ability and flaw counts
     * @param role             the chosen role filter
     * @param isAbilitiesOnly  {@code true} to only let through applicants with special abilities
     * @param isFlawsOnly      {@code true} to only let through applicants with flaws
     * @param experienceLevels the experience levels to let through; empty lets every level through
     */
    ApplicantMatches(Collection<Person> applicants, Function<Person, ApplicantFacts> facts, ApplicantFilter role,
          boolean isAbilitiesOnly, boolean isFlawsOnly, Set<Integer> experienceLevels) {
        for (Person applicant : applicants) {
            ApplicantFacts applicantFacts = facts.apply(applicant);
            boolean hasAbilities = applicantFacts.abilities() > 0;
            boolean hasFlaws = applicantFacts.flaws() > 0;
            int level = applicantFacts.experienceLevel();

            boolean isRoleMatch = role.matches(applicant.getPrimaryRole());
            boolean isAbilitiesMatch = !isAbilitiesOnly || hasAbilities;
            boolean isFlawsMatch = !isFlawsOnly || hasFlaws;
            boolean isLevelMatch = experienceLevels.isEmpty() || experienceLevels.contains(level);

            if (isAbilitiesMatch && isFlawsMatch && isLevelMatch) {
                professions.merge(applicant.getPrimaryRole(), 1, Integer::sum);
            }
            if (isRoleMatch && isFlawsMatch && isLevelMatch && hasAbilities) {
                withAbilities++;
            }
            if (isRoleMatch && isAbilitiesMatch && isLevelMatch && hasFlaws) {
                withFlaws++;
            }
            if (isRoleMatch && isAbilitiesMatch && isFlawsMatch) {
                levels.merge(level, 1, Integer::sum);
            }
            if (isRoleMatch && isAbilitiesMatch && isFlawsMatch && isLevelMatch) {
                shown.add(applicant);
            }
        }
    }

    /** @return the applicants every filter lets through, in the order they were given */
    List<Person> shown() {
        return new ArrayList<>(shown);
    }

    /** @return how many applicants a role filter would show, with the player's other choices as they are */
    int countOf(ApplicantFilter role) {
        int count = 0;
        for (Map.Entry<PersonnelRole, Integer> entry : professions.entrySet()) {
            if (role.matches(entry.getKey())) {
                count += entry.getValue();
            }
        }
        return count;
    }

    /** @return how many applicants have special abilities, with the player's other choices as they are */
    int withAbilities() {
        return withAbilities;
    }

    /** @return how many applicants have flaws, with the player's other choices as they are */
    int withFlaws() {
        return withFlaws;
    }

    /** @return how many applicants have an experience level, with the player's other choices as they are */
    int withLevel(int level) {
        return levels.getOrDefault(level, 0);
    }
}
