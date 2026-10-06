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
package mekhq.campaign.digitalGM.stratCon.sectorGeneration;

/**
 * The hydrology profiles available to improved sector generation. Each identifies both a bucket of tunable numeric
 * parameters (authored in {@code HydrologyProfiles.yaml}) and a distinct ocean-shape algorithm in the generator. A new
 * profile therefore needs both a YAML entry and a shape implementation.
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum HydrologyProfileType {
    /** Almost dry: one or two small, isolated water spots. */
    INLAND,
    /** A meandering river with tributaries: connected, linear water. */
    RIVERLANDS,
    /** Scattered medium lakes across the sector. */
    LAKELANDS,
    /** Dense scattered small water, wetter than lakelands; pairs with a swamp bias. */
    MARSHLANDS,
    /** One large body grown from a corner or edge. */
    COASTAL,
    /** One large body in the center of the sector. */
    INLAND_SEA,
    /** A land finger with ocean on two or three sides, still joined to one edge. */
    PENINSULA,
    /** Water carved around a single central landmass. */
    ISLAND,
    /** Several medium land bodies surrounded by water. */
    ARCHIPELAGO
}
