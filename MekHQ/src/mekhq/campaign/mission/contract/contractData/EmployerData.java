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

import static megamek.client.ui.util.PlayerColour.BLUE;

import jakarta.annotation.Nullable;
import megamek.client.ui.util.PlayerColour;
import megamek.common.enums.SkillLevel;
import megamek.common.icons.Camouflage;
import mekhq.campaign.enums.DragoonRating;
import mekhq.campaign.mission.contract.contractGeneration.ChaosEmployerType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Factions;

/**
 * @param factionCode        the employer's <em>flavor</em> faction &mdash; who is paying the unit and whose theme
 *                           matches the {@link ChaosEmployerType}. Used for display, negotiator/liaison, and
 *                           camouflage. May be a landless faction (rebels, a mercenary command, a corporation).
 * @param anchorFactionCode  the employer's <em>territorial anchor</em> faction &mdash; a faction that actually holds
 *                           ground near the player, used to situate the enemy and target system. Equal to
 *                           {@code factionCode} when the flavor faction is itself a nearby territorial owner.
 * @param sponsorFactionCode the covert <em>patron</em> bankrolling the employer, or {@code null} when the employer acts
 *                           on its own account. Currently only rebels can be sponsored, by a ComStar/Word of Blake
 *                           patron backing their uprising &mdash; the mirror of an enemy fielding sponsored
 *                           mercenaries.
 */
public record EmployerData(ChaosEmployerType type,
      String factionCode,
      String anchorFactionCode,
      @Nullable String sponsorFactionCode,
      String displayName,
      Person negotiator,
      Person liaison,
      SkillLevel forceSkill,
      int equipmentRating,
      Camouflage camouflage,
      PlayerColour color
) {
    public EmployerData(ChaosEmployerType type, String factionCode, String anchorFactionCode,
          @Nullable String sponsorFactionCode, String displayName, Person negotiator, Person liaison,
          Camouflage camouflage) {
        this(type,
              factionCode,
              anchorFactionCode,
              sponsorFactionCode,
              displayName,
              negotiator,
              liaison,
              SkillLevel.REGULAR,
              DragoonRating.DRAGOON_C.getRating(),
              camouflage,
              BLUE);
    }

    public EmployerData(EmployerData existingData, SkillLevel forceSkill, int equipmentRating) {
        this(existingData.type,
              existingData.factionCode,
              existingData.anchorFactionCode,
              existingData.sponsorFactionCode,
              existingData.displayName,
              existingData.negotiator,
              existingData.liaison,
              forceSkill,
              equipmentRating,
              existingData.camouflage,
              existingData.color
        );
    }

    /**
     * @return the flavor faction &mdash; who is paying the unit (see {@link #factionCode()})
     */
    public Faction getFaction() {
        return Factions.getInstance().getFaction(factionCode);
    }

    /**
     * @return the territorial anchor faction used to situate the enemy and target system (see
     *       {@link #anchorFactionCode()})
     */
    public Faction getAnchorFaction() {
        return Factions.getInstance().getFaction(anchorFactionCode);
    }

    /**
     * @return the covert patron bankrolling the employer, or {@code null} if the employer has none (see
     *       {@link #sponsorFactionCode()})
     */
    public @Nullable Faction getSponsorFaction() {
        return sponsorFactionCode == null ? null : Factions.getInstance().getFaction(sponsorFactionCode);
    }
}
