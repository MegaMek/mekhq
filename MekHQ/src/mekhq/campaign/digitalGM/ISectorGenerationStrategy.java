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
package mekhq.campaign.digitalGM;

import java.util.Collection;

import mekhq.campaign.digitalGM.stratCon.LatitudeBand;
import mekhq.campaign.digitalGM.stratCon.PlanetProfile;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Strategy for laying down a sector's (track's) terrain. This is the "how is the sector built" seam: a GM can generate
 * terrain differently &mdash; the legacy biome-stripe placer, the improved geography-aware pipeline, or something new
 * &mdash; without touching sizing, scenario cadence, or enemy generation.
 *
 * <p>Which implementation a GM uses is its own decision; the default StratCon GM selects between the legacy and
 * improved strategies from the {@code useStratConAlternateSectorTerrain} campaign option, so the option keeps its
 * meaning while the branch becomes a proper seam.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface ISectorGenerationStrategy {

    /**
     * Places terrain onto a freshly-sized track.
     *
     * @param track         the track to fill (dimensions and temperature already set)
     * @param planetProfile the destination planet's resolved data
     * @param latitudeBand  the sector's latitude band
     * @param allowCities   {@code true} to allow cities to be placed (ignored by strategies that place none)
     */
    void initializeTrack(StratConTrackState track, PlanetProfile planetProfile, LatitudeBand latitudeBand,
          boolean allowCities);

    /**
     * Clears and re-places a track's terrain in place, as used by the GM "Regenerate Sector" tool. The track's
     * dimensions and temperature are kept; scenarios, facilities, and assigned forces are left for the caller to
     * reconcile.
     *
     * @param track         the track to regenerate
     * @param planetProfile the destination planet's resolved data
     * @param latitudeBand  the latitude band to regenerate under
     * @param allowCities   {@code true} to allow cities to be placed (ignored by strategies that place none)
     */
    void regenerateTrack(StratConTrackState track, PlanetProfile planetProfile, LatitudeBand latitudeBand,
          boolean allowCities);

    /**
     * Folds the given facility hexes into the track's road network, so bases belonging to the planet's owner sit on the
     * road grid alongside cities. A strategy that produces no roads (e.g. the legacy placer) does nothing.
     *
     * @param track          the track whose road network to update
     * @param facilityCoords the facility hexes to connect
     */
    void connectFacilitiesToRoads(StratConTrackState track, Collection<StratConCoords> facilityCoords);
}
