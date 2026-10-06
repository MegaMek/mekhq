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

/**
 * The overall shape a generated sector takes: how wide it is relative to how tall. A sector's area is decided first,
 * from the scouting budget and the planet's water coverage; the shape profile then decides how that area is laid out.
 *
 * <p>Shapes exist for variety - two contracts on identical worlds should not produce identically proportioned maps -
 * so they are weighted rather than derived from planetary data. Wide shapes are favored because the map pane is wider
 * than it is tall.</p>
 *
 * @author Illiani
 * @since 0.51.01
 */
public enum SectorShapeProfileType {
    /** As tall as it is wide. */
    SQUARE,
    /** Modestly wider than tall - the common case. */
    BROAD,
    /** Clearly wider than tall. */
    WIDE,
    /** A long east-west belt; think a river valley or a front line. */
    CORRIDOR,
    /** Taller than wide, for contrast. */
    DEEP
}
