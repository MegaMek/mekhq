/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
 */
package mekhq.campaign.parts;


import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;

import megamek.common.EquipmentType;
import megamek.common.Mek;
import mekhq.campaign.Campaign;
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.finances.Money;
import mekhq.campaign.market.ShoppingList;
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
        ShoppingList mockShoppingList = new ShoppingList();
        Money totalBuyValue = mockShoppingList.getTotalBuyCost();
        assertTrue(mockShoppingList.getPartList().isEmpty());
        assertTrue(totalBuyValue.isZero());
    }

    @Test
    public void onePartInShoppingList() {
        ShoppingList mockShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(mockShoppingList);
        Part part = new MekSensor(1, mockCampaign);
        IAcquisitionWork shoppinglistItem = part.getAcquisitionWork();
        Money partValue = shoppinglistItem.getBuyCost();
        assertFalse(partValue.isZero());
        mockShoppingList.addShoppingItemWithoutChecking(shoppinglistItem);
        assertTrue(mockShoppingList.getTotalBuyCost().getAmount().equals(partValue.getAmount()));
    }

    @Test
    public void incrementPartInShoppingList() {
        ShoppingList mockShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(mockShoppingList);
        Part part = new MekSensor(1, mockCampaign);
        IAcquisitionWork shoppinglistItem = part.getAcquisitionWork();
        Money partValue = shoppinglistItem.getBuyCost();
        assertFalse(partValue.isZero());
        shoppinglistItem.incrementQuantity();
        mockShoppingList.addShoppingItemWithoutChecking(shoppinglistItem);
        assertTrue(mockShoppingList.getTotalBuyCost()
                         .getAmount()
                         .equals(partValue.getAmount().multiply(BigDecimal.valueOf(2))));
        shoppinglistItem.incrementQuantity();
        assertTrue(mockShoppingList.getTotalBuyCost()
                         .getAmount()
                         .equals(partValue.getAmount().multiply(BigDecimal.valueOf(3))));
    }

    @Test
    public void decrementPartInShoppingList() {
        ShoppingList mockShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(mockShoppingList);
        Part part = new MekSensor(1, mockCampaign);
        IAcquisitionWork shoppinglistItem = part.getAcquisitionWork();
        shoppinglistItem.incrementQuantity();
        shoppinglistItem.incrementQuantity();
        Money partValue = shoppinglistItem.getBuyCost();
        assertFalse(partValue.isZero());
        Money partTotalValue = shoppinglistItem.getTotalBuyCost();
        assertTrue(partValue.multipliedBy(2).compareTo(partTotalValue) == 0);
        mockShoppingList.addShoppingItemWithoutChecking(shoppinglistItem);
        assertTrue(mockShoppingList.getTotalBuyCost()
                         .getAmount()
                         .equals(partValue.getAmount().multiply(BigDecimal.valueOf(2))));
        shoppinglistItem.decrementQuantity();
        assertTrue(mockShoppingList.getTotalBuyCost().getAmount().equals(partValue.getAmount()));
        shoppinglistItem.decrementQuantity();
        assertTrue(mockShoppingList.getTotalBuyCost().isZero());
    }

    @Test
    public void addDifferentPartsInShoppingList() {
        ShoppingList mockShoppingList = new ShoppingList();
        mockCampaign.setShoppingList(mockShoppingList);
        Part partA = new MekSensor(1, mockCampaign);
        Part partB = new MekCockpit(2, Mek.COCKPIT_SMALL, false, mockCampaign);
        IAcquisitionWork shoppinglistItemA = partA.getAcquisitionWork();
        IAcquisitionWork shoppinglistItemB = partB.getAcquisitionWork();
        Money partValueA = shoppinglistItemA.getBuyCost();
        Money partValueB = shoppinglistItemB.getBuyCost();
        Money partTotalValue = partValueA.plus(partValueB);
        mockShoppingList.addShoppingItem(shoppinglistItemA, 1, mockCampaign);
        mockShoppingList.addShoppingItem(shoppinglistItemB, 1, mockCampaign);
        assertTrue(mockShoppingList.getTotalBuyCost().getAmount().equals(partTotalValue.getAmount()));


    }

}
