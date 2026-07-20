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

import mekhq.campaign.digitalGM.stratCon.AbstractStratConGM;
import mekhq.campaign.digitalGM.stratCon.StratConScenario;
import mekhq.campaign.digitalGM.stratCon.StratConTrackState;

/**
 * Strategy for building the battlefield &mdash; picking a scenario's terrain and temperature from the biome of its
 * coordinates. This is the "how is the map built" seam: a GM can select terrain differently (scripted, fixed, or from a
 * different biome model) without touching enemy generation or mission cadence.
 *
 * <p>The default StratCon implementation delegates to
 * {@link mekhq.campaign.digitalGM.stratCon.StratConRulesManager#setScenarioParametersFromBiome
 * StratConRulesManager.setScenarioParametersFromBiome}, so the rules themselves are unchanged. The accessor lives on
 * {@link AbstractStratConGM AbstractStratConGM}.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public interface IMapGenerationStrategy {

    /**
     * Sets the terrain and temperature of a scenario from the biome at its coordinates.
     *
     * @param track         the track the scenario is on (source of temperature and facility biomes)
     * @param scenario      the scenario whose terrain is set
     * @param isNoTornadoes {@code true} if tornado weather should be suppressed
     */
    void setScenarioTerrain(StratConTrackState track, StratConScenario scenario, boolean isNoTornadoes);
}
