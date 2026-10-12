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

import mekhq.campaign.base.PlayerBase;
import mekhq.campaign.finances.Money;
import mekhq.campaign.parts.Part;
import mekhq.campaign.parts.equipment.HeatSink;
import mekhq.campaign.parts.meks.MekLocation;
import mekhq.campaign.parts.missing.MissingBayDoor;
import mekhq.campaign.unit.Unit;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.parts.PartsScenario;
import testUtilities.parts.UnitFixture;

/**
 * Loading puts right the parts older versions left duplicated or scattered across a base and the main force (issue
 * #10343).
 */
class UnitPartsRepairTest {
    private PartsScenario scenario;
    private Campaign campaign;
    private LocalWarehouse mainWarehouse;
    private LocalWarehouse baseWarehouse;

    @BeforeEach
    void setUp() {
        scenario = PartsScenario.create();
        campaign = scenario.getCampaign();
        mainWarehouse = campaign.getPlayerForce().getWarehouse();
        PlayerBase base = new PlayerBase(new FixedLocation(mock(PlanetarySystem.class)));
        campaign.getCampaignLocationManager().addPlayerBase(base);
        baseWarehouse = base.getBaseWarehouse();
    }

    /** Puts a copy of the part on its unit, kept in the given warehouse, as an older version could leave it. */
    private Part copyOnUnit(Part part, LocalWarehouse warehouse) {
        Part copy = part.clone();
        copy.setUnit(part.getUnit());
        warehouse.addPart(copy, false);
        part.getUnit().addPart(copy);
        return copy;
    }

    @Test
    void aSecondCopyOfALocationKeptAtABaseIsRemoved() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        MekLocation centreTorso = PartsScenario.unitParts(locust, MekLocation.class).getFirst();
        int partCount = locust.getParts().size();
        Part copy = copyOnUnit(centreTorso, baseWarehouse);
        Money fundsBefore = campaign.getPlayerForce().getFinances().getBalance();

        assertTrue(UnitPartsRepair.repair(campaign, locust));

        assertEquals(partCount, locust.getParts().size());
        assertTrue(locust.getParts().contains(centreTorso), "The part in the unit's own warehouse is kept");
        assertFalse(locust.getParts().contains(copy));
        assertNull(baseWarehouse.getPart(copy.getUniqueId()));
        assertEquals(fundsBefore, campaign.getPlayerForce().getFinances().getBalance(), "Nothing is paid for it");
    }

    @Test
    void partsAUnitCarriesSeveralOfAreNotTreatedAsDuplicates() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        int heatSinkCount = PartsScenario.unitParts(locust, HeatSink.class).size();

        UnitPartsRepair.repair(campaign, locust);

        assertEquals(heatSinkCount, PartsScenario.unitParts(locust, HeatSink.class).size());
        assertNull(UnitPartsRepair.slotOf(PartsScenario.unitParts(locust, HeatSink.class).getFirst()));
    }

    @Test
    void aPartKeptAwayFromItsUnitIsMovedToTheUnitsWarehouse() {
        Unit locust = scenario.withUnit(UnitFixture.LOCUST_LCT_1V);
        HeatSink heatSink = PartsScenario.unitParts(locust, HeatSink.class).getFirst();
        mainWarehouse.transferPart(heatSink, baseWarehouse);

        assertTrue(UnitPartsRepair.repair(campaign, locust));

        assertSame(heatSink, mainWarehouse.getPart(heatSink.getUniqueId()));
        assertTrue(locust.getParts().contains(heatSink));
    }

    @Test
    void missingDoorsBeyondWhatTheBaysHaveAreRemoved() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        int partCount = leopard.getParts().size();
        for (int index = 0; index < 3; index++) {
            MissingBayDoor strayDoor = new MissingBayDoor(0, campaign);
            strayDoor.setUnit(leopard);
            mainWarehouse.addPart(strayDoor, false);
            leopard.addPart(strayDoor);
        }

        assertTrue(UnitPartsRepair.repair(campaign, leopard));

        assertEquals(partCount, leopard.getParts().size(), "Every bay already has its doors");
    }

    @Test
    void aSoundUnitIsLeftAlone() {
        Unit leopard = scenario.withUnit(UnitFixture.LEOPARD_DROPSHIP);
        int partCount = leopard.getParts().size();

        assertFalse(UnitPartsRepair.repair(campaign, leopard));

        assertEquals(partCount, leopard.getParts().size());
    }
}
