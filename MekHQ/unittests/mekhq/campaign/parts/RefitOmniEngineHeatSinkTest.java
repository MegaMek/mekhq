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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;
import testUtilities.parts.PartsCensus;
import testUtilities.parts.PartsScenario;

/**
 * Every Avatar carries 10 heat sinks in a 280 engine that has room for 11. A refit worked the engine's heat sinks out
 * from its capacity, so a configuration with more than 10 heat sinks, such as the AV1-OH with 16, seemed to add one to
 * the engine. That made a pod swap a Class B refit taking days, and bought one heat sink too many (#10673).
 *
 * <p>The AV1-OB and AV1-OH are pinned copies from mm-data.</p>
 */
class RefitOmniEngineHeatSinkTest {
    private final PartsScenario scenario = PartsScenario.create();

    @BeforeAll
    static void loadEquipment() {
        EquipmentType.initializeTypes();
    }

    private static Entity load(String unitName) {
        Entity entity = MHQTestUtilities.getEntityForUnitTesting(unitName, false);
        assertNotNull(entity, unitName);
        return entity;
    }

    private Refit planAvatarRefit() {
        Unit avatar = scenario.getCampaign().addNewUnit(load("Avatar AV1-OB"), false, 0, PartQuality.QUALITY_D);
        return new Refit(avatar, load("Avatar AV1-OH"), false, false, false);
    }

    @Test
    void addingHeatSinkPodsToAnAvatarIsAPodSwap() {
        Refit refit = planAvatarRefit();

        assertEquals(Refit.CLASS_OMNI, refit.getRefitClass(), String.join("; ", refit.getRefitClassReasons()));
    }

    @Test
    void anAvatarPodSwapLeavesTheEngineHeatSinksAlone() {
        Refit refit = planAvatarRefit();

        String reasons = String.join("; ", refit.getRefitClassReasons());
        assertFalse(reasons.contains("built into the engine"), reasons);
    }

    @Test
    void anAvatarPodSwapBuysOnlyTheHeatSinkPods() {
        Refit refit = planAvatarRefit();

        Map<String, Integer> shoppingList = PartsCensus.ofParts(refit.getShoppingList());
        assertEquals(6, shoppingList.get("Double Heat Sink"), shoppingList.toString());
    }
}
