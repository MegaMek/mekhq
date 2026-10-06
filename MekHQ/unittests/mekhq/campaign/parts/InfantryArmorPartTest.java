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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.Campaign;
import mekhq.campaign.parts.enums.PartQuality;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for {@link InfantryArmorPart#clone()}, which used to drop the spare's quality and condition, so a
 * quality A armor kit taken from the warehouse was installed as quality D.
 */
class InfantryArmorPartTest {
    @Test
    void cloneKeepsQualityAndCondition() {
        InfantryArmorPart armorKit = new InfantryArmorPart(0, mock(Campaign.class), 2.0, false, false, true, false,
              false, false);
        armorKit.setQuality(PartQuality.QUALITY_A);
        armorKit.setHits(1);
        armorKit.setBrandNew(false);

        Part clone = armorKit.clone();

        assertEquals(PartQuality.QUALITY_A, clone.getQuality());
        assertEquals(1, clone.getHits());
        assertFalse(clone.isBrandNew());
        assertTrue(armorKit.isSamePartType(clone));
    }
}
