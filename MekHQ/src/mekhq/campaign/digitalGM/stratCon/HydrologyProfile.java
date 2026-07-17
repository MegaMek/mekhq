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
package mekhq.campaign.digitalGM.stratCon;

/**
 * A single hydrology profile's tunable parameters, deserialized from {@code HydrologyProfiles.yaml}. The profile's
 * ocean coverage is rolled uniformly within {@code [minOceanPercent, maxOceanPercent]}, and {@code gaussianCenter} is
 * the water coverage this profile is most likely to be chosen for.
 *
 * @param type            the profile's identity, which also selects its ocean-shape algorithm
 * @param minOceanPercent the lowest ocean coverage this profile produces, as a percentage
 * @param maxOceanPercent the highest ocean coverage this profile produces, as a percentage
 * @param gaussianCenter  the planetary water coverage at which this profile is most strongly favored
 *
 * @author Illiani
 * @since 0.51.01
 */
public record HydrologyProfile(HydrologyProfileType type, int minOceanPercent, int maxOceanPercent,
      double gaussianCenter) {}
