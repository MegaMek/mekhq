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

import mekhq.campaign.parts.equipment.LargeCraftAmmoBin;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A failure at Legendary is permanent, even on a part that can never be scrapped. Only a task that can never genuinely
 * fail - a routine reload - can have been left impossible in error, and only such a task is repaired on load (issue
 * #10372).
 */
class ImpossibleTaskRecoveryTest {
    private static final int IMPOSSIBLE = SkillType.EXP_LEGENDARY + 1;

    private Unit leopard;
    private SpacecraftCoolingSystem coolingSystem;

    @BeforeEach
    void setUp() {
        leopard = PartsScenario.create().withUnit(UnitFixture.LEOPARD_DROPSHIP);
        coolingSystem = PartsScenario.unitParts(leopard, SpacecraftCoolingSystem.class).getFirst();
        coolingSystem.updateConditionFromEntity(false);
    }

    private LargeCraftAmmoBin ammoBin() {
        return PartsScenario.unitParts(leopard, LargeCraftAmmoBin.class).getFirst();
    }

    @Test
    void aLegendaryFailureOnAnUnscrappablePartIsPermanent() {
        StructuralIntegrity structuralIntegrity = PartsScenario.unitParts(leopard, StructuralIntegrity.class)
                                                        .getFirst();

        structuralIntegrity.fail(SkillType.EXP_LEGENDARY);

        assertEquals(IMPOSSIBLE, structuralIntegrity.getSkillMin());
        assertFalse(structuralIntegrity.clearImpossibleDifficulty(), "Loading does not undo it");
        assertEquals(IMPOSSIBLE, structuralIntegrity.getSkillMin());
    }

    @Test
    void aLegendaryFailureOnTheCoolingSystemIsPermanent() {
        coolingSystem.fail(SkillType.EXP_LEGENDARY);

        assertEquals(IMPOSSIBLE, coolingSystem.getSkillMin());
        assertFalse(coolingSystem.clearImpossibleDifficulty(), "Loading does not undo it");
    }

    @Test
    void aFailedUnloadWhileSalvagingIsPermanent() {
        LargeCraftAmmoBin ammoBin = ammoBin();
        leopard.setSalvage(true);
        ammoBin.setSkillMin(IMPOSSIBLE);

        assertFalse(ammoBin.clearImpossibleDifficulty());
        assertEquals(IMPOSSIBLE, ammoBin.getSkillMin());
    }

    @Test
    void anImpossibleReloadIsRepairedOnLoad() {
        LargeCraftAmmoBin ammoBin = ammoBin();
        ammoBin.setSkillMin(IMPOSSIBLE);

        assertTrue(ammoBin.clearImpossibleDifficulty());
        assertEquals(SkillType.EXP_GREEN, ammoBin.getSkillMin());
    }
}
