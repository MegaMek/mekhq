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
package mekhq.campaign.unit;

import java.util.ArrayList;
import java.util.List;

import megamek.common.units.Mek;
import megamek.logging.MMLogger;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.missing.MissingMekLocation;

/**
 * Brings a unit's parts back in line after a Game Master edits its damage. Destroying a location turns it into a
 * missing location that needs replacing; restoring the location's structure afterwards left that task behind, with
 * no way for the Game Master to clear it short of restoring the whole unit (issue #3422).
 */
public final class EditedDamage {
    private static final MMLogger LOGGER = MMLogger.create(EditedDamage.class);

    private EditedDamage() {}

    /**
     * Puts back every missing Mek location whose structure the Game Master restored, without using spares.
     *
     * @param unit the unit whose damage was edited
     *
     * @return how many locations were put back
     */
    public static int reinstateRestoredLocations(Unit unit) {
        if (!(unit.getEntity() instanceof Mek mek)) {
            return 0;
        }
        List<MissingMekLocation> restoredLocations = new ArrayList<>();
        for (Part part : unit.getParts()) {
            if ((part instanceof MissingMekLocation missingLocation)
                      && isStructureBack(mek, missingLocation.getLocation())) {
                restoredLocations.add(missingLocation);
            }
        }
        for (MissingMekLocation restoredLocation : restoredLocations) {
            restoredLocation.reinstate();
        }
        if (!restoredLocations.isEmpty()) {
            LOGGER.debug("[EditDamage] {}: {} locations put back after their structure was restored", unit.getName(),
                  restoredLocations.size());
        }
        return restoredLocations.size();
    }

    private static boolean isStructureBack(Mek mek, int location) {
        boolean hasStructure = mek.getInternal(location) > 0;
        boolean isBlownOff = mek.isLocationBlownOff(location);
        return hasStructure && !isBlownOff;
    }
}
