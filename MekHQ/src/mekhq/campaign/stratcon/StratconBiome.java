/*
 * Copyright (C) 2019-2025 The MegaMek Team
 *
 * This file is part of MekHQ.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community. BattleMech,
 * BattleTech, and MechWarrior are trademarks of The Topps Company, Inc.
 * The MegaMek organization is not affiliated with The Topps Company, Inc.
 * or Catalyst Game Labs.
 */

package mekhq.campaign.stratcon;

import java.util.List;

/**
 * Data structure holding information relevant to a StratCon biome
 * @author NickAragua
 */
public class StratconBiome {
    // biomes will be sorted into buckets based on this field
    public String biomeCategory;

    // lower bound temperature, in degrees kelvin
    public int allowedTemperatureLowerBound;

    // upper bound temperature, in degrees kelvin
    public int allowedTemperatureUpperBound;

    public List<String> allowedTerrainTypes;
}
