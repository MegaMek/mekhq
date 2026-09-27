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

import static mekhq.utilities.MHQInternationalization.getTextAt;

/**
 * How many steps a {@link PlotThread}'s progress track has.
 */
public enum PlotThreadLength {
    SHORT("SHORT", 10),
    MEDIUM("MEDIUM", 15),
    LONG("LONG", 20);

    private static final String RESOURCE_BUNDLE = "mekhq.resources.Roleplay";

    private final String label;
    private final int steps;

    PlotThreadLength(String lookupName, int steps) {
        this.label = getTextAt(RESOURCE_BUNDLE, "PlotThreadLength." + lookupName + ".label");
        this.steps = steps;
    }

    /**
     * @return the number of steps on the progress track
     */
    public int getSteps() {
        return steps;
    }

    /**
     * @return the localized display label for this length
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
