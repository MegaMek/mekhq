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
package mekhq.gui.menus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Set;

import mekhq.campaign.Campaign;
import mekhq.campaign.FixedLocation;
import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * The transport menus only offer a transport at the same place as the units being loaded.
 */
class AssignForceToTransportMenuPlaceTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private PlayerBase base;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
    }

    private Unit keepAtTheBase(Unit unit) {
        campaign.getPlayerForce().getHangar().removeUnit(unit.getId());
        base.getBaseHangar().addUnit(unit);
        return unit;
    }

    @Test
    void aTransportWithTheUnitsIsOffered() {
        Unit dropShip = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        Unit mek = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        assertTrue(AssignForceToTransportMenu.isAtThePlaceOfEveryUnit(dropShip, Set.of(mek)));
    }

    @Test
    void aTransportAtABaseIsNotOfferedToUnitsWithTheMainForce() {
        Unit dropShip = keepAtTheBase(scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP));
        Unit mek = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);

        assertFalse(AssignForceToTransportMenu.isAtThePlaceOfEveryUnit(dropShip, Set.of(mek)));
    }

    @Test
    void aTransportIsNotOfferedWhenOneOfTheUnitsIsElsewhere() {
        Unit dropShip = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        Unit mekWithTheMainForce = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        Unit mekAtTheBase = keepAtTheBase(scenario.withUnit(UnitFixture.LOCUST_LCT_1V));

        assertFalse(AssignForceToTransportMenu.isAtThePlaceOfEveryUnit(dropShip,
              Set.of(mekWithTheMainForce, mekAtTheBase)));
    }
}
