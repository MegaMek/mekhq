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

import megamek.common.annotations.Nullable;

/**
 * One sector shape a generated map can take, loaded from {@code SectorShapeProfiles.yaml}.
 *
 * <p>The nullable fields let an omitted value fall back to a neutral default rather than to zero, which would
 * otherwise make a profile either unselectable or degenerate.</p>
 *
 * @param type        the shape this profile describes
 * @param aspectRatio width divided by height. Greater than one is wider than tall, less than one is taller than wide.
 *                    {@code null} means square.
 * @param weight      relative likelihood of being chosen against the other profiles, or {@code null} for one
 *
 * @author Illiani
 * @since 0.51.01
 */
public record SectorShapeProfile(SectorShapeProfileType type, @Nullable Double aspectRatio, @Nullable Double weight) {

    /** The aspect ratio used when a profile omits its own: square. */
    public static final double DEFAULT_ASPECT_RATIO = 1.0;

    /** The selection weight used when a profile omits its own. */
    public static final double DEFAULT_WEIGHT = 1.0;

    /**
     * Aspect ratios are clamped to this range. A sector far outside it stops being a map and becomes a corridor one hex
     * thick, which neither reads well nor plays well.
     */
    public static final double MIN_ASPECT_RATIO = 0.4;
    public static final double MAX_ASPECT_RATIO = 2.5;

    public double aspectRatioOrDefault() {
        return (aspectRatio == null) ?
                     DEFAULT_ASPECT_RATIO :
                     Math.clamp(aspectRatio, MIN_ASPECT_RATIO, MAX_ASPECT_RATIO);
    }

    public double weightOrDefault() {
        return ((weight == null) || (weight <= 0.0)) ? DEFAULT_WEIGHT : weight;
    }
}
