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
package mekhq.campaign.parts.kfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.Entity;
import megamek.common.units.Jumpship;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsScenario;

/**
 * K-F drive parts on large WarShips cost billions of C-bills and must not overflow (issue #10263). A compact core
 * multiplies a drive part's price by 5, and a lithium-fusion battery by a further 3.
 */
class KFDrivePriceTest {
    private static final double TOLERANCE = 1.0;

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static <T extends Part> T drivePartOn(String fileName, Class<T> partClass) throws Exception {
        Campaign campaign = PartsScenario.create().getCampaign();
        Entity entity = new MekFileParser(new File(MHQTestUtilities.TEST_UNIT_DATA_DIR + fileName)).getEntity();
        Unit unit = campaign.addNewUnit(entity, false, 0, PartQuality.QUALITY_D);
        return PartsScenario.unitParts(unit, partClass).getFirst();
    }

    @Test
    void aCommonwealthWithSixCollarsPricesItsDriveCoilAtTwoPointFiveFiveBillion() throws Exception {
        KFDriveCoil coil = drivePartOn("Commonwealth Light Cruiser Block I.blk", KFDriveCoil.class);
        Jumpship ship = (Jumpship) coil.getUnit().getEntity();
        assertEquals(6, ship.getDocks());

        assertEquals(2_550_000_000.0, coil.getStickerPrice().getAmount().doubleValue(), TOLERANCE,
              "(60 million + 6 x 75 million) x 5 for the compact core");
    }

    @Test
    void aClanPotemkinWithALithiumFusionBatteryPricesItsDriveCoilAtTwentyNineBillion() throws Exception {
        KFDriveCoil coil = drivePartOn("Potemkin Troop Cruiser (2875).blk", KFDriveCoil.class);
        Jumpship ship = (Jumpship) coil.getUnit().getEntity();
        assertTrue(ship.hasLF());
        assertEquals(25, ship.getDocks());

        assertEquals(29_025_000_000.0, coil.getStickerPrice().getAmount().doubleValue(), TOLERANCE,
              "(60 million + 25 x 75 million) x 5 x 3");
    }

    @Test
    void everyDrivePartOnThePotemkinHasAPositivePrice() throws Exception {
        assertTrue(drivePartOn("Potemkin Troop Cruiser (2875).blk", KFFieldInitiator.class).getStickerPrice()
                         .isPositive());
        assertTrue(drivePartOn("Potemkin Troop Cruiser (2875).blk", KFDriveController.class).getStickerPrice()
                         .isPositive());
        assertTrue(drivePartOn("Potemkin Troop Cruiser (2875).blk", KFChargingSystem.class).getStickerPrice()
                         .isPositive());
        assertTrue(drivePartOn("Potemkin Troop Cruiser (2875).blk", KFHeliumTank.class).getStickerPrice()
                         .isPositive());
    }
}
