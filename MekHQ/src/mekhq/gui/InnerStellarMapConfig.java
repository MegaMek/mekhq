/*
 * Copyright (C) 2011-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.gui;

/**
 * All the configuration behavior of InterStellarMap is saved here.
 *
 * @author Imi (immanuel.scholz@gmx.de)
 */
public final class InnerStellarMapConfig {
    /**
     * Whether to scale planet dots on zoom or not
     */
    int minDotSize = 3;
    int maxDotSize = 25;
    /**
     * The scaling maximum dimension
     */
    int reverseScaleMax = 100;
    /**
     * The scaling minimum dimension
     */
    int reverseScaleMin = 2;
    /**
     * Threshold to not show planet names. 0 means show always
     */
    double showPlanetNamesThreshold = 3.0;
    /**
     * The actual scale factor. 1.0 for default, higher means bigger.
     */
    double scale = 0.5;
    /**
     * The scrolling offset
     */
    double centerX = 0.0;
    double centerY = 0.0;
    /**
     * The current selected Planet-id
     */
    int planetID;
}
