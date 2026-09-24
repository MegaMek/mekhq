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
package mekhq.campaign.personnel.quartermaster;

import static mekhq.campaign.personnel.enums.PersonnelRole.MEK_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.kit;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.person;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.warehouseWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.personnel.Person;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Coverage for {@link AbstractKitIssuer#warehouseFor(Person, Campaign)} — a person's local stores, falling back to the
 * main warehouse — and the stock counts built on it.
 */
class AbstractKitIssuerWarehouseTest {
    private Campaign campaign;
    private LocalWarehouse main;
    private EquipmentType basic;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.mockCampaign();
        basic = kit(KIT_BASIC_TOOLKIT);
        main = warehouseWith(basic);
        when(campaign.getPlayerForce().getWarehouse()).thenReturn(main);
    }

    @Test
    void warehouseForPrefersThePersonsLocalStores() {
        LocalWarehouse local = warehouseWith();
        Person person = person(MEK_TECH, NONE);
        when(person.getWarehouse()).thenReturn(local);

        assertSame(local, AbstractKitIssuer.warehouseFor(person, campaign));
    }

    @Test
    void warehouseForFallsBackToTheMainWarehouse() {
        Person person = person(MEK_TECH, NONE);
        when(person.getWarehouse()).thenReturn(null);

        assertSame(main, AbstractKitIssuer.warehouseFor(person, campaign));
    }

    @Test
    void warehouseForIsNullSafeWithoutAPlayerForceOrCampaign() {
        Person person = person(MEK_TECH, NONE);
        when(person.getWarehouse()).thenReturn(null);
        Campaign noForce = mock(Campaign.class);
        when(noForce.getPlayerForce()).thenReturn(null);

        assertNull(AbstractKitIssuer.warehouseFor(person, noForce));
        assertNull(AbstractKitIssuer.warehouseFor(person, null));
    }

    @Test
    void localStockForOnePersonUsesTheFallback() {
        Person person = person(MEK_TECH, NONE);
        when(person.getWarehouse()).thenReturn(null);

        assertEquals(1, AbstractKitIssuer.localStock(person, basic, campaign));
        assertEquals(0, AbstractKitIssuer.localStock(person, basic), "the campaign-less overload stays local-only");
    }

    @Test
    void localStockAcrossPeopleCountsEachWarehouseOnce() {
        LocalWarehouse shared = warehouseWith(basic, basic);
        Person first = person(MEK_TECH, NONE);
        Person second = person(MEK_TECH, NONE);
        Person noStores = person(MEK_TECH, NONE);
        Person alsoNoStores = person(MEK_TECH, NONE);
        when(first.getWarehouse()).thenReturn(shared);
        when(second.getWarehouse()).thenReturn(shared);
        when(noStores.getWarehouse()).thenReturn(null);
        when(alsoNoStores.getWarehouse()).thenReturn(null);

        Map<EquipmentType, Integer> stock = AbstractKitIssuer.localStock(
              List.of(first, second, noStores, alsoNoStores), campaign);

        assertEquals(3, stock.get(basic), "2 in the shared stores + 1 in the main warehouse, each counted once");
    }
}
