/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.camOpsSalvage;

import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.unit.Unit;

public record SalvageTechData(Person tech, UUID techId, String rank, int rankNumeric, PersonnelRole primaryRole,
      PersonnelRole secondaryRole, List<String> techUnits, String firstName, String lastName, String skillLevelName,
      int injuries, int minutesAvailable) {
    public static SalvageTechData buildData(Campaign campaign, Person tech) {
        boolean isSecondaryTech = tech.getSecondaryRole().isTechSecondary();
        List<String> techUnits = new ArrayList<>();
        for (Unit unit : tech.getTechUnits()) {
            techUnits.add(unit.getName());
        }
        return new SalvageTechData(tech,
              tech.getId(),
              tech.getRankName(),
              tech.getRankNumeric(),
              tech.getPrimaryRole(),
              tech.getSecondaryRole(),
              List.copyOf(techUnits),
              tech.getFirstName(),
              tech.getLastName(),
              tech.getSkillLevel(campaign, isSecondaryTech, true).toString(),
              max(tech.getHits(), tech.getInjuries().size()),
              tech.getMinutesLeft());
    }
}
