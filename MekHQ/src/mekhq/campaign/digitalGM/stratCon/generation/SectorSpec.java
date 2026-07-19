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
package mekhq.campaign.digitalGM.stratCon.generation;

/**
 * A blueprint for a single StratCon sector, produced by {@link StratConSectorPlanner} before any track is built. It
 * carries everything downstream sizing and terrain generation needs that is decided at the whole-contract level: how
 * many base sectors' worth of area this sector represents (its condense {@code unitCount}), how many combat teams it
 * demands, and which latitude band drives its temperature.
 *
 * @param unitCount      the number of base-sector "size units" this sector represents; {@code 1} for an ordinary
 *                       sector, higher when sectors have been condensed. Scales both map size and required lances.
 * @param requiredLances the number of combat teams required to scout and hold this sector
 * @param latitudeBand   the latitude band that biases this sector's temperature
 *
 * @author Illiani
 * @since 0.51.01
 */
public record SectorSpec(int unitCount, int requiredLances, LatitudeBand latitudeBand) {}
