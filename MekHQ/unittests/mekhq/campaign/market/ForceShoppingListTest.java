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
package mekhq.campaign.market;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.parts.Part;
import mekhq.campaign.unit.UnitOrder;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.Test;

/**
 * Building a unit order's equipment reads the unit file from disk, so looking up a part on the shopping list does not
 * build it: a unit order can never be the same equipment as a part (issue #10552).
 */
class ForceShoppingListTest {
    private static Entity unit(String chassis, String model) {
        Entity entity = mock(Entity.class);
        when(entity.getChassis()).thenReturn(chassis);
        when(entity.getModel()).thenReturn(model);
        return entity;
    }

    private static UnitOrder unitOrder(String chassis, String model) {
        UnitOrder order = mock(UnitOrder.class);
        Entity unit = unit(chassis, model);
        when(order.getNewEquipment()).thenReturn(unit);
        return order;
    }

    @Test
    void lookingUpAPartDoesNotBuildUnitOrders() {
        UnitOrder order = unitOrder("Atlas", "AS7-D");
        ForceShoppingList shoppingList = new ForceShoppingList();
        shoppingList.addShoppingItemWithoutChecking(order);

        assertNull(shoppingList.getShoppingItem(mock(Part.class)));
        verify(order, never()).getNewEquipment();
    }

    @Test
    void removingAPartDoesNotBuildOrRemoveUnitOrders() {
        UnitOrder order = unitOrder("Atlas", "AS7-D");
        ForceShoppingList shoppingList = new ForceShoppingList();
        shoppingList.addShoppingItemWithoutChecking(order);

        shoppingList.removeItem(mock(Part.class));

        assertEquals(1, shoppingList.getShoppingList().size());
        verify(order, never()).getNewEquipment();
    }

    @Test
    void aUnitIsStillFoundByChassisAndModel() {
        UnitOrder otherOrder = unitOrder("Locust", "LCT-1V");
        UnitOrder order = unitOrder("Atlas", "AS7-D");
        ForceShoppingList shoppingList = new ForceShoppingList();
        shoppingList.addShoppingItemWithoutChecking(otherOrder);
        shoppingList.addShoppingItemWithoutChecking(order);

        assertSame(order, shoppingList.getShoppingItem(unit("Atlas", "AS7-D")));
    }

    @Test
    void aPartIsStillFoundByPartType() {
        Part wanted = mock(Part.class);
        Part ordered = mock(Part.class);
        when(ordered.isSamePartType(wanted)).thenReturn(true);
        IAcquisitionWork partOrder = mock(IAcquisitionWork.class);
        when(partOrder.getNewEquipment()).thenReturn(ordered);
        ForceShoppingList shoppingList = new ForceShoppingList();
        shoppingList.addShoppingItemWithoutChecking(unitOrder("Atlas", "AS7-D"));
        shoppingList.addShoppingItemWithoutChecking(partOrder);

        assertSame(partOrder, shoppingList.getShoppingItem(wanted));
    }

    @Test
    void addingAnOrderBuildsItsEquipmentOnce() {
        ForceShoppingList shoppingList = new ForceShoppingList();
        UnitOrder firstUnitOrder = unitOrder("Atlas", "AS7-D");
        UnitOrder secondUnitOrder = unitOrder("Locust", "LCT-1V");
        shoppingList.addShoppingItemWithoutChecking(firstUnitOrder);
        shoppingList.addShoppingItemWithoutChecking(secondUnitOrder);
        IAcquisitionWork newOrder = mock(IAcquisitionWork.class);
        when(newOrder.getNewEquipment()).thenReturn(mock(Part.class));

        shoppingList.addShoppingItem(newOrder, 0, mock(Campaign.class));

        verify(newOrder, times(1)).getNewEquipment();
        verify(firstUnitOrder, never()).getNewEquipment();
        verify(secondUnitOrder, never()).getNewEquipment();
    }
}
