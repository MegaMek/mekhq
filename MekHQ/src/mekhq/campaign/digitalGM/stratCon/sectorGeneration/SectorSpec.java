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
 * A blueprint for a single StratCon sector, produced by {@link StratConSectorPlanner} before any track is built. It
 * carries everything downstream sizing and terrain generation needs that is decided at the whole-contract level: how
 * many combat teams this sector demands, and which latitude band drives its temperature.
 *
 * <p>The contract's required combat teams are shared out evenly across its sectors, so {@code requiredLances} is this
 * sector's slice of them. Sector size follows directly from it: a third of those teams are assumed to be recon, and the
 * sector is sized to what they can scout in three months.</p>
 *
 * @param requiredLances the number of combat teams required to scout and hold this sector
 * @param latitudeBand   the latitude band that biases this sector's temperature
 *
 * @author Illiani
 * @since 0.51.01
 */
public record SectorSpec(int requiredLances, LatitudeBand latitudeBand) {}
