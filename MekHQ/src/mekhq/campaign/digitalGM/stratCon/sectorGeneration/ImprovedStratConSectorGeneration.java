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

import java.util.Collection;

import mekhq.campaign.digitalGM.ISectorGenerationStrategy;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * The improved, geography-aware sector generation: the {@link StratConSectorGenerator} pipeline (biome, hydrology,
 * orogeny, geographic terrain fill, cities, farmland, roads), plus road connection for the planet-owner's facilities.
 *
 * @author Illiani
 * @since 0.51.01
 */
public class ImprovedStratConSectorGeneration implements ISectorGenerationStrategy {

    @Override
    public void initializeTrack(StratConTrackState track, PlanetProfile planetProfile, LatitudeBand latitudeBand,
          boolean allowCities) {
        StratConSectorGenerator.generate(track, planetProfile, latitudeBand, allowCities);
    }

    @Override
    public void regenerateTrack(StratConTrackState track, PlanetProfile planetProfile, LatitudeBand latitudeBand,
          boolean allowCities) {
        track.clearForRegeneration();
        StratConSectorGenerator.generate(track, planetProfile, latitudeBand, allowCities);
    }

    @Override
    public void connectFacilitiesToRoads(StratConTrackState track, Collection<StratConCoords> facilityCoords) {
        StratConRoadPlacer.recalculateRoads(track, facilityCoords);
    }
}
