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
package mekhq.campaign.mission.contract.contractGeneration.negotiationsAndNPCs;

import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Bloodname;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;
import mekhq.campaign.universe.Faction;

/**
 * Generates the commander of the opposing (enemy) force for a contract. The commander is created as a MekWarrior of the
 * enemy faction and then elevated to a senior officer rank so they read as the person leading the opposition.
 *
 * <p>The commander is made faction-appropriate from the faction's own data rather than any hardcoded assumptions: the
 * enemy faction supplies the person's origin (and therefore culture and naming), and the rank the commander is given is
 * drawn from that faction's own rank system &mdash; the Clan system for Clans, ComStar's for ComStar, a House's for an
 * Inner Sphere power, and so on. Clan enemies additionally receive a Bloodname. This is a static utility class and is
 * not instantiable.</p>
 */
public class OpposingCommander {
    /**
     * The senior-officer rank the opposing commander is raised to within their faction's rank system. This sits in the
     * officer rank band ({@link mekhq.campaign.personnel.ranks.Rank#RO_MIN} to
     * {@link mekhq.campaign.personnel.ranks.Rank#RO_MAX}) and marks the person out as a force commander rather than a
     * line trooper.
     */
    private static final int COMMANDER_RANK = 38;

    private OpposingCommander() {}

    /**
     * Generates the commander of the opposing force for the given enemy faction, raising the person to a senior officer
     * rank in that faction's own rank system. Clan enemies are additionally given a Bloodname.
     *
     * @param campaign     the current campaign, used to create and adjust the person
     * @param enemyFaction the enemy faction, which supplies the commander's origin, rank system, and (for Clans)
     *                     Bloodname
     *
     * @return the generated opposing commander, or {@code null} if the person could not be created
     */
    public static Person generateOpposingCommander(Campaign campaign, Faction enemyFaction) {
        final String factionCode = enemyFaction.getShortName();

        Person commander = campaign.getPlayerForce()
                                 .getHumanResources()
                                 .newPerson(campaign, MEKWARRIOR, factionCode, Gender.RANDOMIZE);

        if (commander == null) {
            return null;
        }

        if (enemyFaction.isClan()) {
            assignBloodname(campaign, commander, factionCode);
        }

        // assignRankSystemFromFaction resolves the commander's origin faction's own rank system, so the resulting rank
        // is faction-appropriate for Clans, ComStar, and Inner Sphere powers alike.
        AutomaticRankAssigner.assignRankSystemFromFaction(commander, COMMANDER_RANK);

        return commander;
    }

    /**
     * Gives a Clan opposing commander a Bloodname appropriate to their faction and era.
     *
     * @param campaign    the current campaign, used to source the game year for Bloodname selection
     * @param commander   the commander to adjust
     * @param factionCode the enemy faction's short name, used to pick an appropriate Bloodname
     */
    private static void assignBloodname(Campaign campaign, Person commander, String factionCode) {
        Bloodname bloodname = Bloodname.randomBloodname(factionCode, Phenotype.MEKWARRIOR, campaign.getGameYear());
        if (bloodname != null) {
            commander.setBloodname(bloodname.getName());
        }
    }
}
