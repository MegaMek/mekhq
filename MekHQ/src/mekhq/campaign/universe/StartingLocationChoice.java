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
package mekhq.campaign.universe;

import jakarta.annotation.Nullable;
import mekhq.campaign.universe.enums.StartingLocationMode;

/**
 * The player's starting-location choice, made in the campaign options when a new campaign is created.
 *
 * <p>Mercenary and pirate campaigns use every field: {@code mode} (together with {@code specificFaction} and
 * {@code includeDeepPeriphery} where the mode calls for them) selects the faction to attach to, and
 * {@code useFactionCapital} then selects the world. Aligned campaigns (Great Houses, Periphery states, Clans, and so
 * on) always begin with their own faction, so only {@code useFactionCapital} applies; {@code mode},
 * {@code specificFaction}, and {@code includeDeepPeriphery} are ignored.</p>
 *
 * @param mode                 the kind of starting location requested (ignored for aligned campaigns)
 * @param specificFaction      the faction to start with when {@code mode} is
 *                             {@link StartingLocationMode#SPECIFIC_FACTION}; otherwise {@code null}
 * @param useFactionCapital    {@code true} to start on the chosen faction's capital, {@code false} to start on a
 *                             random hiring hall in the chosen faction's territory
 * @param includeDeepPeriphery {@code true} to include Deep Periphery factions when {@code mode} is
 *                             {@link StartingLocationMode#RANDOM_PERIPHERY}
 *
 * @since 0.51.0
 */
public record StartingLocationChoice(StartingLocationMode mode, @Nullable Faction specificFaction,
                                     boolean useFactionCapital, boolean includeDeepPeriphery) {}
