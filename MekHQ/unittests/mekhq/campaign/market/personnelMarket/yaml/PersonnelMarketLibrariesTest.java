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
package mekhq.campaign.market.personnelMarket.yaml;

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests that each market style's profession tables are loaded into that style's own maps. The CamOps (Strict) tables
 * were once loaded into the CamOps (Revised) maps, leaving Strict empty and Revised overwritten.
 */
class PersonnelMarketLibrariesTest {
    private static PersonnelMarketLibraries libraries;

    @BeforeAll
    static void loadLibraries() {
        libraries = new PersonnelMarketLibraries();
    }

    @Test
    void everyMarketHasProfessions() {
        assertFalse(libraries.getClanMarketMekHQ().isEmpty());
        assertFalse(libraries.getInnerSphereMarketMekHQ().isEmpty());
        assertFalse(libraries.getClanMarketCamOpsRevised().isEmpty());
        assertFalse(libraries.getInnerSphereMarketCamOpsRevised().isEmpty());
        assertFalse(libraries.getClanMarketCamOpsStrict().isEmpty());
        assertFalse(libraries.getInnerSphereMarketCamOpsStrict().isEmpty());
    }

    @Test
    void camOpsRevisedKeepsProfessionsThatStrictLeavesOut() {
        // Strict follows Campaign Operations' table as written, which has no Administrators; Revised adds them
        assertTrue(libraries.getClanMarketCamOpsRevised().containsKey(ADMINISTRATOR));
        assertTrue(libraries.getInnerSphereMarketCamOpsRevised().containsKey(ADMINISTRATOR));
        assertFalse(libraries.getClanMarketCamOpsStrict().containsKey(ADMINISTRATOR));
        assertFalse(libraries.getInnerSphereMarketCamOpsStrict().containsKey(ADMINISTRATOR));
    }
}
