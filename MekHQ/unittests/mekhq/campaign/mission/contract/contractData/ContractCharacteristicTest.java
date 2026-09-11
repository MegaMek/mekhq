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

import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import mekhq.campaign.mission.contract.contractData.ContractCharacteristic.Category;
import mekhq.campaign.mission.contract.contractData.ContractCharacteristic.Polarity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Structural invariants for the {@link ContractCharacteristic} catalog. These do not pin the exact tuning values (which
 * are expected to change) - they assert that every entry is well formed, its display text resolves, its polarity
 * predicates agree, and every {@link Category} is represented.
 */
class ContractCharacteristicTest {

    @ParameterizedTest
    @EnumSource(ContractCharacteristic.class)
    void everyCharacteristicIsWellFormed(ContractCharacteristic characteristic) {
        assertNotNull(characteristic.getCategory(), "category must be set");
        assertNotNull(characteristic.getPolarity(), "polarity must be set");
        assertTrue(characteristic.getWeight() > 0, "weight must be positive: " + characteristic);
    }

    @ParameterizedTest
    @EnumSource(ContractCharacteristic.class)
    void everyCharacteristicHasResolvableDisplayText(ContractCharacteristic characteristic) {
        assertTrue(isResourceKeyValid(characteristic.getName()),
              "name does not resolve for " + characteristic.name() + ": '" + characteristic.getName() + '\'');
        assertTrue(isResourceKeyValid(characteristic.getToolTipText()),
              "tooltip does not resolve for " + characteristic.name());
    }

    @ParameterizedTest
    @EnumSource(ContractCharacteristic.class)
    void polarityPredicatesAgreeWithPolarity(ContractCharacteristic characteristic) {
        assertEquals(characteristic.getPolarity() == Polarity.BENEFICIAL, characteristic.isBeneficial(),
              "isBeneficial disagrees with polarity for " + characteristic.name());
        assertEquals(characteristic.getPolarity() == Polarity.ADVERSE, characteristic.isAdverse(),
              "isAdverse disagrees with polarity for " + characteristic.name());
    }

    @ParameterizedTest
    @EnumSource(Category.class)
    void everyCategoryHasAtLeastOneCharacteristic(Category category) {
        assertTrue(Arrays.stream(ContractCharacteristic.values()).anyMatch(c -> c.getCategory() == category),
              "no characteristic belongs to category " + category);
    }

    @Test
    void displayNamesAreDistinct() {
        Set<String> seen = new HashSet<>();
        for (ContractCharacteristic characteristic : ContractCharacteristic.values()) {
            assertTrue(seen.add(characteristic.getName()),
                  "duplicate display name '" + characteristic.getName() + "' at " + characteristic.name());
        }
    }
}
