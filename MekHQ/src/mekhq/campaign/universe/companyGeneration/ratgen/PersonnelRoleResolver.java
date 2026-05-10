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
package mekhq.campaign.universe.companyGeneration.ratgen;

import megamek.common.units.UnitType;
import mekhq.campaign.personnel.enums.PersonnelRole;

/**
 * Maps a MegaMek {@link UnitType} integer to the {@link PersonnelRole} a primary crew member should hold.
 *
 * <p>Phase 1 returns {@link PersonnelRole#MEKWARRIOR} for every unit type so the new pipeline produces the
 * same flat-list Mek output as the legacy strategies. Phase 2 fills in the full table covering vehicles,
 * VTOLs, infantry, BA, aero, vessels, and ProtoMeks.</p>
 */
public final class PersonnelRoleResolver {

    private PersonnelRoleResolver() {
        // utility class
    }

    /**
     * Returns the primary crew role for a unit of the given type. Phase 1 stub returns
     * {@link PersonnelRole#MEKWARRIOR}; full table arrives in Phase 2.
     *
     * @param unitType the {@link UnitType} integer of the unit
     * @return the primary {@link PersonnelRole} for that unit's commander
     */
    public static PersonnelRole primaryRole(int unitType) {
        // Phase 1: Mek-only behavior to match the existing AbstractCompanyGenerator output.
        return PersonnelRole.MEKWARRIOR;
    }
}
