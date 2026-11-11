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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.equipment.Engine;
import megamek.common.units.Entity;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The refit list grades refits as kit work; Customize to Model is told apart when it grades differently (issue
 * #10741). An engine swap is Class D with a refit kit and Class E without one, and custom work beyond Class B costs
 * the unit a quality level (Campaign Operations p.211-212).
 */
class RefitWithoutKitNoticeTest {
    @Test
    void engineSwapWithoutAKitNeedsAFactory() {
        PartsScenario scenario = PartsScenario.create();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Refit kitRefit = new Refit(wolverine, wolverineWithXlEngine(), false, false, false);
        Refit customRefit = new Refit(wolverine, wolverineWithXlEngine(), true, false, false);

        assertEquals(Refit.CLASS_D, kitRefit.getRefitClass(), kitRefit.getRefitClassReasons().toString());
        assertEquals(Refit.CLASS_E, customRefit.getRefitClass(), customRefit.getRefitClassReasons().toString());
        assertEquals("Customize to Model works without a refit kit, which makes this Class E (Factory) work that "
                           + "needs Factory Conditions. The reason: Changing the engine type or rating. With the "
                           + "refit kit it stays Class D (Maintenance). It also lowers the quality of every part on "
                           + "the unit by one level.",
              RefitWithoutKitNotice.describe(kitRefit, customRefit));
    }

    @Test
    void sameClassWithoutAKitStillCostsQuality() {
        PartsScenario scenario = PartsScenario.create();
        Unit wolverine = scenario.withUnit(UnitFixture.WOLVERINE_WVR_6R);
        Refit kitRefit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), false, false, false);
        Refit customRefit = new Refit(wolverine, UnitFixture.WOLVERINE_WVR_6M.loadEntity(), true, false, false);

        assertEquals(kitRefit.getRefitClass(), customRefit.getRefitClass());
        assertEquals("Customize to Model works without a refit kit, which lowers the quality of every part on the "
                           + "unit by one level.",
              RefitWithoutKitNotice.describe(kitRefit, customRefit));
    }

    @Test
    void fieldWorkWithoutAKitNeedsNoNotice() {
        Unit unit = mock(Unit.class);
        when(unit.getName()).thenReturn("Locust LCT-1V");
        Refit kitRefit = mock(Refit.class);
        Refit customRefit = mock(Refit.class);
        when(kitRefit.getOriginalUnit()).thenReturn(unit);
        when(kitRefit.getRefitClass()).thenReturn(Refit.CLASS_B);
        when(customRefit.getRefitClass()).thenReturn(Refit.CLASS_B);
        when(customRefit.isLosingQualityForNoKit()).thenReturn(false);

        assertNull(RefitWithoutKitNotice.describe(kitRefit, customRefit));
    }

    /**
     * @return a Wolverine WVR-6R whose standard 275 engine is swapped for a 275 XL, and nothing else changed
     */
    private static Entity wolverineWithXlEngine() {
        Entity wolverine = UnitFixture.WOLVERINE_WVR_6R.loadEntity();
        wolverine.setEngine(new Engine(275, Engine.XL_ENGINE, 0));
        return wolverine;
    }
}
