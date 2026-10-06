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
package mekhq.campaign.mission.contract.contractData;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests {@link ContractCommandRights} - chiefly {@code parseFromString}, which the save loader relies on and which must
 * keep understanding both the enum names and the legacy numeric codes, and the mutually-exclusive {@code isX}
 * predicates.
 */
class ContractCommandRightsTest {

    // region parseFromString - enum names

    @ParameterizedTest
    @EnumSource(ContractCommandRights.class)
    void parsesEveryEnumNameBackToItself(final ContractCommandRights rights) {
        assertSame(rights, ContractCommandRights.parseFromString(rights.name()));
    }

    // region parseFromString - legacy numeric codes

    @Test
    void parsesLegacyNumericCodes() {
        assertSame(ContractCommandRights.INTEGRATED, ContractCommandRights.parseFromString("0"));
        assertSame(ContractCommandRights.HOUSE, ContractCommandRights.parseFromString("1"));
        assertSame(ContractCommandRights.LIAISON, ContractCommandRights.parseFromString("2"));
        assertSame(ContractCommandRights.INDEPENDENT, ContractCommandRights.parseFromString("3"));
    }

    // region parseFromString - fallback

    @Test
    void parsingAnUnknownNumberFallsBackToHouse() {
        assertSame(ContractCommandRights.HOUSE, ContractCommandRights.parseFromString("4"),
              "an out-of-range numeric code must not throw - it falls back to HOUSE");
        assertSame(ContractCommandRights.HOUSE, ContractCommandRights.parseFromString("-1"));
    }

    @Test
    void parsingGibberishFallsBackToHouse() {
        assertSame(ContractCommandRights.HOUSE, ContractCommandRights.parseFromString("not-a-command-right"));
        assertSame(ContractCommandRights.HOUSE, ContractCommandRights.parseFromString(""));
    }

    // region predicates

    @ParameterizedTest
    @EnumSource(ContractCommandRights.class)
    void exactlyOnePredicateIsTrueForEachConstant(final ContractCommandRights rights) {
        int trueCount = (rights.isIntegrated() ? 1 : 0)
                              + (rights.isHouse() ? 1 : 0)
                              + (rights.isLiaison() ? 1 : 0)
                              + (rights.isIndependent() ? 1 : 0);
        assertEquals(1, trueCount, rights + " must satisfy exactly one of the isX predicates");
    }

    @Test
    void predicatesIdentifyTheRightConstant() {
        assertTrue(ContractCommandRights.INTEGRATED.isIntegrated());
        assertTrue(ContractCommandRights.HOUSE.isHouse());
        assertTrue(ContractCommandRights.LIAISON.isLiaison());
        assertTrue(ContractCommandRights.INDEPENDENT.isIndependent());

        assertFalse(ContractCommandRights.HOUSE.isIntegrated());
    }
}
