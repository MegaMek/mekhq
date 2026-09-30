/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.factionStanding;

import com.fasterxml.jackson.annotation.JsonProperty;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * One side the player can choose in a Faction Standing ultimatum, represented by the person asking for the campaign's
 * support.
 *
 * <p>The person is never added to the campaign's personnel, so the immersive dialogs and the side picker show their
 * faction's logo rather than a portrait. That is why no gender or portrait data is stored here.</p>
 *
 * @param id          the side's ID, unique within its ultimatum and in upper case. It is used to build the side's text
 *                    keys ({@code FactionStandingUltimatumDialog.<ultimatum>.side.<id>.pitch} and {@code .news}) and
 *                    to name the side the dissenting officer prefers
 * @param name        the name of the person leading this side, including any title
 * @param role        the role of that person
 * @param factionCode the code of the faction the campaign joins if it chooses this side
 *
 * @author Illiani
 * @since 0.50.07
 */
public record FactionStandingUltimatumSide(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("role") PersonnelRole role,
      @JsonProperty("factionCode") String factionCode
) {}
