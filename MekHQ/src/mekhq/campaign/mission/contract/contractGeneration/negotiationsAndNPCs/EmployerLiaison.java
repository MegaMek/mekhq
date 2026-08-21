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

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR_COMMAND;
import static mekhq.campaign.personnel.enums.PersonnelRole.BROKER;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MERCHANT;
import static mekhq.campaign.personnel.enums.PersonnelRole.MILITARY_LIAISON;
import static mekhq.campaign.personnel.ranks.Rank.RO_MIN;

import java.util.List;

import jakarta.annotation.Nullable;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.contract.contractGeneration.ContractSearchType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.ranks.AutomaticRankAssigner;

public class EmployerLiaison {
    private final static PersonnelRole PIRATE_LIAISON_ROLE = BROKER;
    private final static PersonnelRole GOVERNMENT_LIAISON_NORMAL = ADMINISTRATOR_COMMAND;
    private final static PersonnelRole GOVERNMENT_LIAISON_CLAN = MEKWARRIOR;
    private final static PersonnelRole MERCENARY_LIAISON_NORMAL = MILITARY_LIAISON;
    private final static PersonnelRole MERCENARY_LIAISON_CLAN = MERCHANT;

    /** These negotiators do not receive military ranks */
    private final static List<PersonnelRole> UNRANKED_ROLES = List.of(PIRATE_LIAISON_ROLE,
          MERCENARY_LIAISON_NORMAL,
          MERCENARY_LIAISON_CLAN);

    private EmployerLiaison() {}

    public static @Nullable Person generateLiaison(Campaign campaign, ContractSearchType searchType,
          boolean employerIsClan, String employerCode) {
        PersonnelRole role = getLiaisonRole(searchType, employerIsClan);

        Person negotiator = campaign.getPlayerForce()
                                  .getHumanResources()
                                  .newPerson(campaign, role, employerCode, Gender.RANDOMIZE);

        assignRank(negotiator);

        return negotiator;
    }

    private static PersonnelRole getLiaisonRole(ContractSearchType searchType, boolean employerIsClan) {
        return switch (searchType) {
            case PIRATE -> PIRATE_LIAISON_ROLE;
            // TOURNAMENT contacts (arena games organizers) reuse the mercenary-hall broker role until dedicated
            // tournament generation lands.
            case MERCENARY, TOURNAMENT -> employerIsClan ? MERCENARY_LIAISON_CLAN : MERCENARY_LIAISON_NORMAL;
            case GOVERNMENT -> employerIsClan ? GOVERNMENT_LIAISON_CLAN : GOVERNMENT_LIAISON_NORMAL;
        };
    }

    private static void assignRank(@Nullable Person negotiator) {
        // Personnel generation can fail on an unrecognized faction code, the same way it can for the opposing
        // commander; there is simply no one to rank in that case.
        if (negotiator == null) {
            return;
        }

        final PersonnelRole primaryRole = negotiator.getPrimaryRole();
        if (UNRANKED_ROLES.contains(primaryRole)) {
            return;
        }

        AutomaticRankAssigner.assignRankSystemFromFaction(negotiator, RO_MIN);
    }
}
