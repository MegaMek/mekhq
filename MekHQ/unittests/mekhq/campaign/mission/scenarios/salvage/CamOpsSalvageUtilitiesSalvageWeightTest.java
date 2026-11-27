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
package mekhq.campaign.mission.scenarios.salvage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import megamek.common.equipment.EquipmentType;
import megamek.common.loaders.MekFileParser;
import megamek.common.units.ConvInfantry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests that conventional infantry wrecks are weighed at full strength during salvage operations, so a wiped-out
 * platoon is not weightless.
 */
class CamOpsSalvageUtilitiesSalvageWeightTest {
    private static final String FOOT_PLATOON_FILE = "testresources/data/mekfiles/Foot Platoon (DCMS) (Laser 2620+).blk";

    @BeforeAll
    static void initializeEquipment() {
        EquipmentType.initializeTypes();
    }

    private static ConvInfantry loadFootPlatoon() throws Exception {
        return (ConvInfantry) new MekFileParser(new File(FOOT_PLATOON_FILE)).getEntity();
    }

    @Test
    @DisplayName("An intact platoon's salvage weight is its normal weight")
    void intactPlatoon() throws Exception {
        ConvInfantry infantry = loadFootPlatoon();

        assertTrue(infantry.getWeight() > 0.0);
        assertEquals(infantry.getWeight(), CamOpsSalvageUtilities.getSalvageWeight(infantry));
    }

    @Test
    @DisplayName("A wiped-out platoon keeps its full-strength salvage weight")
    void destroyedPlatoon() throws Exception {
        ConvInfantry infantry = loadFootPlatoon();
        double fullStrengthWeight = infantry.getWeight();
        infantry.setInternal(0, ConvInfantry.LOC_INFANTRY);

        assertEquals(0.0, infantry.getWeight());
        assertEquals(fullStrengthWeight, CamOpsSalvageUtilities.getSalvageWeight(infantry));
    }

    @Test
    @DisplayName("A platoon that has taken casualties keeps its full-strength salvage weight")
    void damagedPlatoon() throws Exception {
        ConvInfantry infantry = loadFootPlatoon();
        double fullStrengthWeight = infantry.getWeight();
        infantry.setInternal(3, ConvInfantry.LOC_INFANTRY);

        assertEquals(fullStrengthWeight, CamOpsSalvageUtilities.getSalvageWeight(infantry));
    }

    @Test
    @DisplayName("A missing entity weighs nothing")
    void missingEntity() {
        assertEquals(0.0, CamOpsSalvageUtilities.getSalvageWeight(null));
    }
}
