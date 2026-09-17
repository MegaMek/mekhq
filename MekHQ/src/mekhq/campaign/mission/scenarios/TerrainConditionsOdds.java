/*
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.mission.scenarios;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One authored weather/condition profile: the odds of each {@link megamek.common.planetaryConditions} value for a single
 * condition {@link #type} (Light, Wind, Weather, Fog, BlowingSand or EMI), shared by every terrain listed in
 * {@link #terrain}. Loaded from {@code TerrainConditionsOddsManifest.yaml} by {@link TerrainConditionsOddsManifest}.
 *
 * <p>Fields are public and bound by field name so the YAML shape and the Java stay in step; the biome map type names in
 * {@link #terrain} are the keys of {@link mekhq.campaign.digitalGM.stratCon.biome.StratConBiomeManifest#getBiomeMapTypes()},
 * and the keys of {@link #odds} are the condition enums' external ids.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public class TerrainConditionsOdds {
    public String type;
    public String name;
    public List<String> terrain = new ArrayList<>();
    public Map<String, Integer> odds = new HashMap<>();
}
