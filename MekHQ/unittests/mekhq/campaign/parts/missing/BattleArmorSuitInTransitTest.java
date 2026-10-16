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
package mekhq.campaign.parts.missing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import mekhq.campaign.parts.BattleArmorSuit;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * A replacement battle armor suit on its way counts as in transit for the suit it replaces (issue #4531).
 */
class BattleArmorSuitInTransitTest {
    private static final int TRANSIT_DAYS = 5;

    private final PartsScenario scenario = PartsScenario.create();

    private MissingBattleArmorSuit destroyOneSuit(UnitFixture fixture) {
        Unit squad = scenario.withUnit(fixture);
        BattleArmorSuit suit = PartsScenario.unitParts(squad, BattleArmorSuit.class).getFirst();
        suit.remove(false);
        return PartsScenario.unitParts(squad, MissingBattleArmorSuit.class).getFirst();
    }

    private void assertReplacementInTransit(UnitFixture fixture) {
        MissingBattleArmorSuit missingSuit = destroyOneSuit(fixture);

        scenario.getCampaign().getQuartermaster().addPart(missingSuit.getNewPart(), TRANSIT_DAYS);

        assertEquals(1, missingSuit.getReplacementInventory().getTransit(), fixture.name());
    }

    @Test
    void anInnerSphereSuitOnItsWayIsInTransit() {
        assertReplacementInTransit(UnitFixture.IS_STANDARD_BATTLE_ARMOR_LASER);
    }

    @Test
    void aSuitThatHasArrivedIsOnHand() {
        MissingBattleArmorSuit missingSuit = destroyOneSuit(UnitFixture.IS_STANDARD_BATTLE_ARMOR_LASER);

        scenario.getCampaign().getQuartermaster().addPart(missingSuit.getNewPart(), 0);

        assertEquals(1, missingSuit.getReplacementInventory().getSupply());
        assertEquals(0, missingSuit.getReplacementInventory().getTransit());
    }

    @Test
    void anElementalSuitOnItsWayIsInTransit() {
        assertReplacementInTransit(UnitFixture.ELEMENTAL_BATTLE_ARMOR_LASER);
    }
}
