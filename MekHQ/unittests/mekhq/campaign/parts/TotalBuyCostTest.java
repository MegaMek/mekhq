/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;

import megamek.common.equipment.EquipmentType;
import megamek.common.units.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.ShoppingList;
import mekhq.campaign.parts.meks.MekCockpit;
import mekhq.campaign.parts.meks.MekSensor;
import mekhq.campaign.work.IAcquisitionWork;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;


public class TotalBuyCostTest {


    @Mock
    private Campaign mockCampaign;

    @Mock
    private CampaignOptions mockCampaignOptions;

    @BeforeAll
    static void before() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    public void beforeEach() {
        mockCampaign = mock(Campaign.class);
        mockCampaignOptions = mock(CampaignOptions.class);
        lenient().when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        lenient().when(mockCampaignOptions.getCommonPartPriceMultiplier()).thenReturn(1d);
        lenient().when(mockCampaignOptions.getInnerSphereUnitPriceMultiplier()).thenReturn(1d);
        lenient().when(mockCampaignOptions.getInnerSpherePartPriceMultiplier()).thenReturn(1d);
        double[] usedPartMultipliers = { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0 };
        lenient().when(mockCampaignOptions.getUsedPartPriceMultipliers()).thenReturn(usedPartMultipliers);

    }

    @Test
    public void emptyShoppingList() {
        ShoppingList testShoppingList = new ShoppingList();
        Money totalBuyValue = testShoppingList.getTotalBuyCost();
        assertTrue(testShoppingList.getPartList().isEmpty());
        assertTrue(totalBuyValue.isZero());
    }

    @Test
    public void onePartInShoppingList() {
        ShoppingList testShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(testShoppingList);
        Part part = new MekSensor(1, mockCampaign);
        IAcquisitionWork shoppingListItem = part.getAcquisitionWork();
        Money partValue = shoppingListItem.getBuyCost();
        assertFalse(partValue.isZero());
        testShoppingList.addShoppingItemWithoutChecking(shoppingListItem);
        assertEquals(testShoppingList.getTotalBuyCost().getAmount(), partValue.getAmount());
    }

    @Test
    public void incrementPartInShoppingList() {
        ShoppingList testShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(testShoppingList);
        Part part = new MekSensor(1, mockCampaign);
        IAcquisitionWork shoppingListItem = part.getAcquisitionWork();
        Money partValue = shoppingListItem.getBuyCost();
        assertFalse(partValue.isZero());
        shoppingListItem.incrementQuantity();
        testShoppingList.addShoppingItemWithoutChecking(shoppingListItem);
        assertEquals(testShoppingList.getTotalBuyCost()
                           .getAmount(), partValue.getAmount().multiply(BigDecimal.valueOf(2)));
        shoppingListItem.incrementQuantity();
        assertEquals(testShoppingList.getTotalBuyCost()
                           .getAmount(), partValue.getAmount().multiply(BigDecimal.valueOf(3)));
    }

    @Test
    public void decrementPartInShoppingList() {
        ShoppingList testShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(testShoppingList);
        Part part = new MekSensor(1, mockCampaign);
        IAcquisitionWork shoppingListItem = part.getAcquisitionWork();
        shoppingListItem.incrementQuantity();
        shoppingListItem.incrementQuantity();
        Money partValue = shoppingListItem.getBuyCost();
        assertFalse(partValue.isZero());
        Money partTotalValue = shoppingListItem.getTotalBuyCost();
        assertEquals(0, partValue.multipliedBy(3).compareTo(partTotalValue));
        testShoppingList.addShoppingItemWithoutChecking(shoppingListItem);
        assertEquals(testShoppingList.getTotalBuyCost()
                           .getAmount(), partValue.getAmount().multiply(BigDecimal.valueOf(3)));
        shoppingListItem.decrementQuantity();
        assertEquals(testShoppingList.getTotalBuyCost()
                           .getAmount(), partValue.getAmount().multiply(BigDecimal.valueOf(2)));
        shoppingListItem.decrementQuantity();
        assertEquals(testShoppingList.getTotalBuyCost().getAmount(), partValue.getAmount());
    }

    @Test
    public void addDifferentPartsInShoppingList() {
        ShoppingList testShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(testShoppingList);
        Part partA = new MekSensor(1, mockCampaign);
        Part partB = new MekCockpit(2, Mek.COCKPIT_SMALL, false, mockCampaign);
        IAcquisitionWork shoppingListItemA = partA.getAcquisitionWork();
        IAcquisitionWork shoppingListItemB = partB.getAcquisitionWork();
        Money partValueA = shoppingListItemA.getBuyCost();
        Money partValueB = shoppingListItemB.getBuyCost();
        Money partTotalValue = partValueA.plus(partValueB);
        testShoppingList.addShoppingItem(shoppingListItemA, 1, mockCampaign);
        testShoppingList.addShoppingItem(shoppingListItemB, 1, mockCampaign);
        assertEquals(testShoppingList.getTotalBuyCost().getAmount(), partTotalValue.getAmount());


    }

}
