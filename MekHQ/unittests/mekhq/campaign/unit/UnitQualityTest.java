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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.equipment.EquipmentPart;
import mekhq.campaign.parts.missing.MissingPart;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A unit's quality is the average quality of its maintained parts. A destroyed part has no quality, so it does not pull
 * the grade down: a Hunchback whose parts are all quality F stays F after losing its weapons (issue #8829).
 */
class UnitQualityTest {
    private final PartsScenario scenario = PartsScenario.create();

    @Test
    void destroyedPartsDoNotLowerAUnitsQuality() {
        Unit hunchback = scenario.withUnit(UnitFixture.HUNCHBACK_HBK_4G);
        hunchback.setQuality(PartQuality.QUALITY_F);
        List<EquipmentPart> equipment = PartsScenario.unitParts(hunchback, EquipmentPart.class);
        for (EquipmentPart part : equipment) {
            part.remove(false);
        }
        assertTrue(PartsScenario.unitParts(hunchback, MissingPart.class).size() >= 6, "The weapons are destroyed");

        assertEquals(PartQuality.QUALITY_F, hunchback.getQuality());
    }

    @Test
    void aUnitWithEveryPartAtOneQualityHasThatQuality() {
        Unit hunchback = scenario.withUnit(UnitFixture.HUNCHBACK_HBK_4G);
        hunchback.setQuality(PartQuality.QUALITY_B);

        assertEquals(PartQuality.QUALITY_B, hunchback.getQuality());
    }
}
