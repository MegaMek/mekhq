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
package mekhq.campaign.mission.contract.contractData;

import static megamek.client.ui.util.PlayerColour.RED;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

public record EnemyData(String factionCode,
      @Nullable String sponsorFactionCode,
      String displayName,
      SkillLevel forceSkill,
      int equipmentRating,
      Person opposingCommander,
      Camouflage camouflage,
      PlayerColour color,
      boolean batchallAccepted
) {
    private static final boolean DEFAULT_BATCHALL_ACCEPTED = true;

    public EnemyData(String factionCode, @Nullable String sponsorFactionCode, String displayName,
          Person opposingCommander, Camouflage camouflage) {
        this(factionCode,
              sponsorFactionCode,
              displayName,
              SkillLevel.REGULAR,
              DragoonRating.DRAGOON_C.getRating(),
              opposingCommander,
              camouflage,
              RED,
              DEFAULT_BATCHALL_ACCEPTED);
    }

    public EnemyData(EnemyData existingData, SkillLevel forceSkill, int equipmentRating) {
        this(existingData.factionCode,
              existingData.sponsorFactionCode,
              existingData.displayName,
              forceSkill,
              equipmentRating,
              existingData.opposingCommander,
              existingData.camouflage,
              existingData.color,
              existingData.batchallAccepted
        );
    }

    public EnemyData(EnemyData existingData, boolean batchallAccepted) {
        this(existingData.factionCode,
              existingData.sponsorFactionCode,
              existingData.displayName,
              existingData.forceSkill,
              existingData.equipmentRating,
              existingData.opposingCommander,
              existingData.camouflage,
              existingData.color,
              batchallAccepted
        );
    }

    public Faction getFaction() {
        return Factions.getInstance().getFaction(factionCode);
    }
}
