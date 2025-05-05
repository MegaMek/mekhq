/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.market.personnelMarket.enums;

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

public class PersonnelMarketStyleTest {
    @ParameterizedTest
    @CsvSource(value = { "MEKHQ,MEKHQ", "CAMPAIGN_OPERATIONS,CAMPAIGN_OPERATIONS", "mekhq,MEKHQ",
                         "campaign_operations,CAMPAIGN_OPERATIONS", "'CAMPAIGN OPERATIONS',CAMPAIGN_OPERATIONS",
                         "2,CAMPAIGN_OPERATIONS", "'InvalidValue',NONE", "'-1',NONE" })
    void testFromString_Parameterized(String input, PersonnelMarketStyle expected) {
        assertEquals(expected, PersonnelMarketStyle.fromString(input));
    }

    @ParameterizedTest
    @NullSource
    @EmptySource
    void testFromString_NullOrEmpty(String input) {
        assertEquals(PersonnelMarketStyle.NONE, PersonnelMarketStyle.fromString(input));
    }

    @ParameterizedTest
    @EnumSource(value = PersonnelMarketStyle.class)
    void testToString_notInvalid(PersonnelMarketStyle status) {
        String label = status.toString();
        assertTrue(isResourceKeyValid(label));
    }
}
