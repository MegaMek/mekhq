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
package mekhq.campaign.roleplay;

import java.util.List;

import mekhq.campaign.roleplay.Concepts.Concept;

/**
 * One step on a {@link PlotThread}'s progress track, holding the concept rolled for it when the thread was created.
 *
 * @param number           the step's number, starting at 1
 * @param majorRevelation  {@code true} if this step is a flashpoint, and so a Major Revelation
 * @param conclusion       {@code true} if this is the thread's final step
 * @param concepts         the concept rolled for this step: a theme followed by adventure table results
 */
public record PlotThreadStep(int number, boolean majorRevelation, boolean conclusion, List<Concept> concepts) {
    public PlotThreadStep {
        concepts = List.copyOf(concepts);
    }
}
