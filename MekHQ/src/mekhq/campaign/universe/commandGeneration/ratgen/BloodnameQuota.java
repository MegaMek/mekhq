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
import java.util.Collections;
import java.util.List;

import megamek.client.ratgenerator.ForceDescriptor;
import megamek.common.annotations.Nullable;
import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.ForceHumanResources;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.commandGeneration.CommandGenerationOptions;

/**
 * Awards a generated Clan force the Bloodnames its calibre warrants, as a share of its warriors.
 *
 * <p>A Bloodname is scarce. Each Clan holds about forty of them, each with twenty-five holders at most, so a
 * thousand Bloodnamed warriors in a Clan of some seven thousand (the warrior caste was about 115,000 strong
 * across the Clans in 3062) is the ceiling: fifteen in a hundred if every slot were filled, which they never
 * all are. The Clans post the Bloodnamed to their better formations, so a front-line Cluster carries most of
 * them and a garrison or solahma unit almost none.</p>
 *
 * <p>Rolling for each warrior on their own skills, as MekHQ does when it hires one, cannot produce that: an
 * elite force rolls well for everyone and comes out mostly Bloodnamed. So the force is given a quota, a share
 * of its eligible warriors set by its experience level, and the Bloodnames go to the warriors who best merit
 * them: Tactical Genius first, then combat experience, chance breaking ties. Bloodnames the roll itself
 * seeded count toward the quota.</p>
 */
public final class BloodnameQuota {

    private static final MMLogger LOGGER = MMLogger.create(BloodnameQuota.class);
    private static final String LOG_TAG = "[CompanyGen][Pipeline][Bloodname]";

    /** The share of an elite force's warriors who hold a Bloodname: the Clan-wide ceiling. */
    static final double SHARE_ELITE = 0.15;
    /** A veteran, front-line force. */
    static final double SHARE_VETERAN = 0.10;
    /** A regular force, and a force whose experience was left to chance. */
    static final double SHARE_REGULAR = 0.05;
    /** A green or garrison force. */
    static final double SHARE_GREEN = 0.02;

    /**
     * What the quota did.
     *
     * @param eligible    Clan warriors with a phenotype, the only people a Bloodname can go to
     * @param quota       how many of them the force's calibre allows a Bloodname
     * @param alreadyHeld how many arrived with one from the roll
     * @param awarded     how many were awarded one here
     */
    public record Result(int eligible, int quota, int alreadyHeld, int awarded) {

        public static Result none() {
            return new Result(0, 0, 0, 0);
        }
    }

    private BloodnameQuota() {
    }

    /**
     * Awards the force its Bloodnames.
     *
     * @param campaign         the campaign the warriors belong to
     * @param options          the generation options, read for the force's experience level
     * @param generatedPersons every person this generation created; non-Clan people and anyone without a
     *                         phenotype are passed over
     *
     * @return what was awarded
     */
    public static Result award(Campaign campaign, CommandGenerationOptions options, List<Person> generatedPersons) {
        List<Person> eligible = new ArrayList<>();
        List<Person> candidates = new ArrayList<>();
        int alreadyHeld = 0;
        for (Person person : generatedPersons) {
            if (!isEligible(person)) {
                continue;
            }
            eligible.add(person);
            if (OfficerSelector.hasBloodname(person)) {
                alreadyHeld++;
            } else {
                candidates.add(person);
            }
        }
        if (eligible.isEmpty()) {
            LOGGER.info("{} no Clan warriors with a phenotype; nothing to award", LOG_TAG);
            return Result.none();
        }

        double share = share(experienceOf(options));
        int quota = quota(eligible.size(), share);
        int toAward = Math.max(0, quota - alreadyHeld);

        // Chance breaks ties between equals, so the same roster does not always favour the same seat.
        Collections.shuffle(candidates);
        candidates.sort(OfficerSelector.mostSkilledFirst(campaign));
        ForceHumanResources humanResources = campaign.getPlayerForce().getHumanResources();
        int awarded = 0;
        for (Person person : candidates.subList(0, Math.min(toAward, candidates.size()))) {
            humanResources.checkBloodnameAdd(campaign, person, true);
            if (OfficerSelector.hasBloodname(person)) {
                awarded++;
            }
        }
        LOGGER.info("{} share {}% for the force's calibre: {} of {} eligible warrior(s) may hold a Bloodname;"
                    + " {} arrived with one, {} awarded", LOG_TAG, Math.round(share * 100), quota, eligible.size(),
              alreadyHeld, awarded);
        return new Result(eligible.size(), quota, alreadyHeld, awarded);
    }

    /**
     * @param experience the force's experience level, one of the {@code ForceDescriptor.EXP_} values, or
     *                   {@code null} when it was left to chance
     *
     * @return the share of the force's warriors who hold a Bloodname
     */
    static double share(@Nullable Integer experience) {
        if (experience == null) {
            return SHARE_REGULAR;
        }
        return switch (experience) {
            case ForceDescriptor.EXP_ELITE -> SHARE_ELITE;
            case ForceDescriptor.EXP_VETERAN -> SHARE_VETERAN;
            case ForceDescriptor.EXP_GREEN -> SHARE_GREEN;
            default -> SHARE_REGULAR;
        };
    }

    /**
     * @return how many of the eligible warriors the share allows, rounded to the nearest whole warrior
     */
    static int quota(int eligible, double share) {
        return (int) Math.round(eligible * share);
    }

    private static @Nullable Integer experienceOf(CommandGenerationOptions options) {
        ForceDescriptorSnapshot snapshot = options.getForceDescriptorSnapshot();
        return (snapshot == null) ? null : snapshot.getExperience();
    }

    private static boolean isEligible(Person person) {
        if (person == null) {
            return false;
        }
        boolean isClan = person.isClanPersonnel();
        boolean hasPhenotype = (person.getPhenotype() != null) && !person.getPhenotype().isNone();
        return isClan && hasPhenotype;
    }
}
