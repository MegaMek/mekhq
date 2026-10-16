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
package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import megamek.common.units.BipedMek;
import megamek.common.units.Entity;
import megamek.common.util.UUIDUtil;
import org.junit.jupiter.api.Test;

/**
 * A customized refit design is edited from a copy of its source unit file, so it starts out with the source's
 * unit-file UUID. Before it is saved as a custom it must get its own UUID, or the custom and the canon design collide
 * in the unit cache and UUID-first lookups load the wrong design.
 */
class RefitCustomUnitFileIdentityTest {

    /**
     * Builds an entity the way a unit file loader leaves it: with the given UUID read from the file and the original
     * identity stored.
     */
    private static Entity loadedFromFileWithUUID(String unitFileUUID) {
        Entity entity = new BipedMek();
        entity.setChassis("Charger");
        entity.setModel("CGR-1A5");
        entity.setUnitFileUUID(unitFileUUID);
        entity.storeOriginalUnitData();
        return entity;
    }

    @Test
    void customRefitGetsNewUUIDAndRecordsSourceDesign() {
        String sourceUUID = UUIDUtil.newUUIDv7().toString();
        Entity customEntity = loadedFromFileWithUUID(sourceUUID);
        customEntity.setModel("CGR-1A5 Mk II");

        Refit.assignCustomUnitFileIdentity(customEntity);

        assertNotNull(customEntity.getUnitFileUUID());
        assertNotEquals(sourceUUID, customEntity.getUnitFileUUID(),
              "The custom must not keep the source design's unit-file UUID");
        assertEquals(sourceUUID, customEntity.getRefitFromUUID(),
              "The custom records the source design it was refit from");
    }

    @Test
    void customRefitOfCustomRecordsImmediateSourceDesign() {
        String canonUUID = UUIDUtil.newUUIDv7().toString();
        String firstCustomUUID = UUIDUtil.newUUIDv7().toString();
        Entity customEntity = loadedFromFileWithUUID(firstCustomUUID);
        customEntity.setRefitFromUUID(canonUUID);

        Refit.assignCustomUnitFileIdentity(customEntity);

        assertNotEquals(firstCustomUUID, customEntity.getUnitFileUUID());
        assertNotEquals(canonUUID, customEntity.getUnitFileUUID());
        assertEquals(firstCustomUUID, customEntity.getRefitFromUUID(),
              "A refit of a custom points at the custom it was edited from");
    }

    @Test
    void sourceWithoutUUIDClearsInheritedRefitReference() {
        Entity customEntity = new BipedMek();
        customEntity.setRefitFromUUID(UUIDUtil.newUUIDv7().toString());
        customEntity.storeOriginalUnitData();
        String generatedUUID = customEntity.getUnitFileUUID();

        Refit.assignCustomUnitFileIdentity(customEntity);

        assertNull(customEntity.getRefitFromUUID(),
              "With no UUID in the source file there is no source design to reference");
        assertNotNull(customEntity.getUnitFileUUID());
        assertNotEquals(generatedUUID, customEntity.getUnitFileUUID());
    }
}
