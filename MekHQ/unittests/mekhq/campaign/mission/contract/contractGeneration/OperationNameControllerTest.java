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
package mekhq.campaign.mission.contract.contractGeneration;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumMap;
import java.util.Map;

import megamek.common.util.weightedMaps.WeightedIntMap;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OperationNameControllerTest {

    private static final String RESOURCE_BUNDLE = "mekhq.resources.RandomOperationNameGenerator";

    private static WeightedIntMap<String> pool(final String... words) {
        WeightedIntMap<String> map = new WeightedIntMap<>();
        for (String word : words) {
            map.add(1, word);
        }
        return map;
    }

    private static Map<ContractObjectiveType, WeightedIntMap<String>> nounPool(final ContractObjectiveType type,
          final String... words) {
        Map<ContractObjectiveType, WeightedIntMap<String>> nouns = new EnumMap<>(ContractObjectiveType.class);
        nouns.put(type, pool(words));
        return nouns;
    }

    @BeforeEach
    void reset() {
        RandomOperationNameGenerator.resetForTesting();
    }

    @AfterEach
    void tearDown() {
        RandomOperationNameGenerator.resetForTesting();
    }

    @Test
    void composesDescriptorAndNounWithASingleSpace() {
        RandomOperationNameGenerator.createForTesting(pool("RED"),
              nounPool(ContractObjectiveType.PIRATE_HUNTING, "EAGLE"));

        assertEquals("RED EAGLE",
              OperationNameController.generateOperationName(ContractObjectiveType.PIRATE_HUNTING));
    }

    @Test
    void producedNameIsAlwaysTwoUppercaseTokens() {
        RandomOperationNameGenerator.createForTesting(pool("RED", "IRON", "SILENT"),
              nounPool(ContractObjectiveType.GARRISON_DUTY, "BULWARK", "BASTION", "AEGIS"));

        for (int i = 0; i < 100; i++) {
            String name = OperationNameController.generateOperationName(ContractObjectiveType.GARRISON_DUTY);
            assertTrue(name.matches("[A-Z]+ [A-Z]+"), "unexpected codename shape: '" + name + "'");
        }
    }

    @Test
    void fallsBackToTheResourceValueWhenTheDescriptorPoolIsUnavailable() {
        RandomOperationNameGenerator.createForTesting(null,
              nounPool(ContractObjectiveType.GARRISON_DUTY, "BULWARK"));

        assertEquals(getTextAt(RESOURCE_BUNDLE, "fallbackValue"),
              OperationNameController.generateOperationName(ContractObjectiveType.GARRISON_DUTY));
    }

    @Test
    void fallsBackToTheResourceValueWhenTheNounPoolIsUnavailable() {
        // Descriptor present, but no noun pool for the objective and no UNDEFINED fallback pool.
        RandomOperationNameGenerator.createForTesting(pool("RED"), Map.of());

        assertEquals(getTextAt(RESOURCE_BUNDLE, "fallbackValue"),
              OperationNameController.generateOperationName(ContractObjectiveType.GARRISON_DUTY));
    }
}
