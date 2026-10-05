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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.UUID;

import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Removing a part or a unit kept at a base removes it from the base, wherever the request comes from (issue #10343).
 */
class BasePartRemovalTest {
    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private PlayerBase base;
    private Unit unitAtBase;

    @BeforeEach
    void setUp() {
        PartsScenario scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);

        // A unit kept at the base, with its parts in the base's warehouse
        unitAtBase = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        campaign.getPlayerForce().getHangar().removeUnit(unitAtBase.getId());
        base.getBaseHangar().addUnit(unitAtBase);
        for (Part part : List.copyOf(unitAtBase.getParts())) {
            mainWarehouse.removePart(part);
            base.getBaseWarehouse().addPart(part);
        }
        // and a unit in the main force, whose parts share the base's part numbers
        scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
    }

    @Test
    void theMainWarehouseRemovesABasePartFromTheBase() {
        Part basePart = unitAtBase.getParts().getFirst();
        int sharedNumber = basePart.getId();
        Part mainPartWithTheSameNumber = mainWarehouse.getPart(sharedNumber);
        int mainCount = mainWarehouse.getParts().size();

        boolean isRemoved = mainWarehouse.removePart(basePart);

        assertTrue(isRemoved);
        assertNull(base.getBaseWarehouse().getPart(basePart.getUniqueId()), "The part left the base");
        assertEquals(mainCount, mainWarehouse.getParts().size(), "Nothing left the main force");
        assertSame(mainPartWithTheSameNumber, mainWarehouse.getPart(sharedNumber));
    }

    @Test
    void removingAUnitAtABaseTakesItAndItsPartsFromTheBase() {
        int mainCount = mainWarehouse.getParts().size();

        boolean isRemoved = campaign.removeUnit(unitAtBase.getId());

        assertTrue(isRemoved);
        assertNull(base.getBaseHangar().getUnit(unitAtBase.getId()));
        assertTrue(base.getBaseWarehouse().getParts().isEmpty(), "The unit's parts left with it");
        assertEquals(mainCount, mainWarehouse.getParts().size(), "Nothing left the main force");
    }

    @Test
    void removingAUnitNoHangarHoldsRemovesNothing() {
        assertFalse(campaign.removeUnit(UUID.randomUUID()));
    }

    @Test
    void sellingAUnitAtABasePaysForItAndRemovesIt() {
        Money fundsBefore = campaign.getPlayerForce().getFinances().getBalance();
        Money sellValue = unitAtBase.getSellValue();

        campaign.getQuartermaster().sellUnit(unitAtBase);

        assertNull(campaign.getUnit(unitAtBase.getId()), "The unit was sold, not kept");
        assertEquals(fundsBefore.plus(sellValue), campaign.getPlayerForce().getFinances().getBalance());
    }
}
