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

import java.time.LocalDate;

import jakarta.annotation.Nullable;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.PlanetarySystem;
import mekhq.campaign.universe.Systems;

/**
 * Where a contract is fought, as ids into the universe data.
 *
 * <p>Both ids are optional. A contract need not name a planet within its system, and a converted legacy contract may
 * not name a system at all when the save records none and the campaign has no current system to stand in for it. Every
 * accessor here copes: an unknown or absent id yields {@code null}, and the display accessors render {@code "-"}.</p>
 *
 * @param systemId the id of the target system, or {@code null} when none is known
 * @param planetId the id of the target planet within that system, or {@code null} when the contract names no planet
 */
public record SystemsTargetData(@Nullable String systemId, @Nullable String planetId) {
    public @Nullable PlanetarySystem getSystem() {
        return Systems.getInstance().getSystemById(systemId);
    }

    public String getSystemName(LocalDate currentDate) {
        PlanetarySystem planetarySystem = getSystem();
        return planetarySystem == null ? "-" : planetarySystem.getName(currentDate);
    }

    public @Nullable Planet getPlanet() {
        PlanetarySystem planetarySystem = getSystem();
        return planetarySystem == null ? null : planetarySystem.getPlanetById(planetId);
    }

    public String getPlanetName(LocalDate currentDate) {
        Planet planet = getPlanet();
        return planet == null ? "-" : planet.getName(currentDate);
    }
}
