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

import megamek.common.annotations.Nullable;
import mekhq.campaign.digitalGM.stratCon.AbstractStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConCoords;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Strategy for deciding <i>where on the track</i> the OpFor deploys &mdash; which coordinates a generated scenario is
 * placed at. This is distinct from {@link IOpForGenerationStrategy} (what the OpFor fields) and from
 * {@link IMapGenerationStrategy} (the terrain of the resulting battle): a GM can keep the standard enemy composition
 * but change where hostile scenarios appear on the strategic map.
 *
 * <p>The default StratCon implementation delegates to
 * {@link mekhq.campaign.digitalGM.stratCon.StratConContractInitializer#getUnoccupiedCoords
 * StratConContractInitializer.getUnoccupiedCoords} &mdash; the existing weighted-random selection of an unoccupied,
 * non-ocean hex. The accessor lives on {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IOpForDeploymentStrategy {

    /**
     * Selects the coordinates a hostile scenario deploys to on the track.
     *
     * @param track                     the track to place the scenario on
     * @param allowPlayerFacilities     {@code true} to allow placement on player-allied facility hexes
     * @param allowPlayerForces         {@code true} to allow placement on hexes occupied by player forces
     * @param emphasizeStrategicTargets {@code true} to weight placement toward strategic targets
     *
     * @return the chosen coordinates, or {@code null} if the track has no eligible hex
     */
    @Nullable
    StratConCoords getUnoccupiedCoords(StratConTrackState track, boolean allowPlayerFacilities,
          boolean allowPlayerForces, boolean emphasizeStrategicTargets);

    /**
     * Selects the coordinates a hostile scenario deploys to, using the default constraints (no placement on player
     * facilities or forces, no strategic emphasis).
     *
     * @param track the track to place the scenario on
     *
     * @return the chosen coordinates, or {@code null} if the track has no eligible hex
     */
    @Nullable
    default StratConCoords getUnoccupiedCoords(StratConTrackState track) {
        return getUnoccupiedCoords(track, false, false, false);
    }
}
