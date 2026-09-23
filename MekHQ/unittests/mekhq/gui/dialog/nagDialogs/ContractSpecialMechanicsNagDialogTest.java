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
package mekhq.gui.dialog.nagDialogs;

import static mekhq.utilities.MHQInternationalization.getTextAt;
import static mekhq.utilities.MHQInternationalization.isResourceKeyValid;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekhq.campaign.digitalGM.stratCon.StratConContractMechanics;
import mekhq.campaign.mission.contract.contractData.ContractObjectiveType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests for {@link ContractSpecialMechanicsNagDialog}: every contract type with special mechanics must have a briefing,
 * since the briefing is the only tutorial the player gets for them.
 *
 * @author Illiani
 * @since 0.51.01
 */
class ContractSpecialMechanicsNagDialogTest {
    private static final String RESOURCE_BUNDLE = "mekhq.resources.ContractSpecialMechanicsNagDialog";
    private static final String KEY_PREFIX = "ContractSpecialMechanicsNagDialog.";

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class)
    void everyContractTypeWithSpecialMechanicsHasABriefing(ContractObjectiveType objectiveType) {
        StratConContractMechanics mechanics = StratConContractMechanics.forObjectiveType(objectiveType);
        if (!ContractSpecialMechanicsNagDialog.hasSpecialMechanics(mechanics)) {
            return;
        }

        String key = KEY_PREFIX + objectiveType.name();
        assertTrue(isResourceKeyValid(getTextAt(RESOURCE_BUNDLE, key)), key + " is missing");
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, mode = EnumSource.Mode.EXCLUDE, names = { "UNDEFINED" })
    void everyDefinedContractTypeHasSpecialMechanics(ContractObjectiveType objectiveType) {
        assertTrue(ContractSpecialMechanicsNagDialog.hasSpecialMechanics(
              StratConContractMechanics.forObjectiveType(objectiveType)));
    }

    @ParameterizedTest
    @EnumSource(value = ContractObjectiveType.class, names = { "UNDEFINED" })
    void undefinedContractTypeHasNoSpecialMechanics(ContractObjectiveType objectiveType) {
        assertFalse(ContractSpecialMechanicsNagDialog.hasSpecialMechanics(
              StratConContractMechanics.forObjectiveType(objectiveType)));
    }

    @Test
    void everyBriefedContractTypeIsSuppressedSeparately() {
        List<ContractObjectiveType> briefedContractTypes = ContractSpecialMechanicsNagDialog.getBriefedContractTypes();
        assertFalse(briefedContractTypes.contains(ContractObjectiveType.UNDEFINED));

        Set<String> nagKeys = new HashSet<>();
        for (ContractObjectiveType objectiveType : briefedContractTypes) {
            nagKeys.add(ContractSpecialMechanicsNagDialog.getNagKey(objectiveType));
        }
        assertEquals(briefedContractTypes.size(), nagKeys.size());
    }
}
