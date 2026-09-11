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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The edge trigger names a save carries must keep mapping onto live options after a trigger is renamed or split,
 * otherwise every person in an old save logs an error on load and silently loses that setting.
 */
class PersonEdgeTriggerMigrationTest {

    @Test
    void currentTriggerNameMapsToItself() {
        assertEquals(List.of(PersonnelOptions.EDGE_REPAIR_BREAK_PART),
              Person.resolveEdgeTriggerNames(PersonnelOptions.EDGE_REPAIR_BREAK_PART));
    }

    @Test
    void legacyAdminAcquisitionTriggerMapsToAllThreeReplacements() {
        List<String> resolved = Person.resolveEdgeTriggerNames(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_LEGACY);

        assertEquals(List.of(PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_OTHER,
              PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_EIGHT,
              PersonnelOptions.EDGE_ADMIN_ACQUIRE_FAIL_ELEVEN), resolved);
    }
}
