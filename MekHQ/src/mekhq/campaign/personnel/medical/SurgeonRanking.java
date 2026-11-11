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
package mekhq.campaign.personnel.medical;

import static mekhq.campaign.personnel.skills.SkillType.S_SURGERY;

import java.time.LocalDate;
import java.util.Collection;

import jakarta.annotation.Nullable;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.Skill;

/**
 * Ranks doctors by how good they are at surgery.
 *
 * <p>A doctor's Surgery skill decides who treats patients and who operates. Administration is deliberately not part
 * of this ranking: it changes how many patients a doctor can handle, not how well they treat them.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public final class SurgeonRanking {
    /** Returned by {@link #getSurgerySkillLevel(Person)} for someone without the Surgery skill. */
    public static final int NO_SURGERY_SKILL = -1;

    private SurgeonRanking() {
    }

    /**
     * Returns the person's total Surgery skill level, including attribute, trait, and injury modifiers.
     *
     * @param person the person to check
     *
     * @return the total Surgery skill level, or {@link #NO_SURGERY_SKILL} if the person has no Surgery skill
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static int getSurgerySkillLevel(Person person) {
        Skill surgerySkill = person.getSkill(S_SURGERY);
        if (surgerySkill == null) {
            return NO_SURGERY_SKILL;
        }
        return surgerySkill.getTotalSkillLevel(person.getSkillModifierData());
    }

    /**
     * Picks the best surgeon from the given people: the active doctor with the highest Surgery skill. When two doctors
     * have the same Surgery skill, the higher-ranked one is chosen, with experience breaking any remaining tie.
     *
     * @param candidates      the people to choose from; anyone who is not an active doctor is ignored
     * @param campaignOptions the campaign options, used for the rank tiebreaker
     * @param isClanCampaign  whether this is a Clan campaign, used for the rank tiebreaker
     * @param today           the current date, used for the rank tiebreaker
     *
     * @return the best surgeon, or {@code null} if there are no active doctors
     *
     * @author Illiani
     * @since 0.51.01
     */
    public static @Nullable Person getBestSurgeon(Collection<Person> candidates, CampaignOptions campaignOptions,
          boolean isClanCampaign, LocalDate today) {
        Person bestSurgeon = null;
        int bestSurgeryLevel = NO_SURGERY_SKILL;

        for (Person candidate : candidates) {
            if (!candidate.isActive() || !candidate.isDoctor()) {
                continue;
            }

            int surgeryLevel = getSurgerySkillLevel(candidate);
            if (bestSurgeon == null || surgeryLevel > bestSurgeryLevel) {
                bestSurgeon = candidate;
                bestSurgeryLevel = surgeryLevel;
            } else if (surgeryLevel == bestSurgeryLevel &&
                             candidate.outRanksUsingSkillTiebreaker(campaignOptions, isClanCampaign, today,
                                   bestSurgeon)) {
                bestSurgeon = candidate;
            }
        }

        return bestSurgeon;
    }
}
