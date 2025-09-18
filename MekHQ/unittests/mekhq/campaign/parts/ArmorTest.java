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
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.campaign.parts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.stream.Stream;

import megamek.common.enums.TechRating;
import megamek.common.equipment.EquipmentType;
import megamek.common.units.Entity;
import mekhq.campaign.Campaign;
import mekhq.campaign.Warehouse;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.parts.enums.PartQuality;
import mekhq.campaign.parts.protomeks.ProtoMekArmor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link Armor} and its children, {@link BAArmor}, {@link SVArmor}, and {@link ProtoMekArmor}.
 */

public class ArmorTest {
    static final int ARMOR_AMOUNT = 5;
    static final int ARMOR_TYPE = EquipmentType.T_ARMOR_STANDARD;
    static final int DIFFERENT_ARMOR_TYPE = EquipmentType.T_ARMOR_FERRO_FIBROUS;
    static final int SV_ARMOR_BAR = 5;
    static final int DIFFERENT_SV_ARMOR_BAR = 7;

    static Campaign mockCampaign;
    static CampaignOptions mockCampaignOptions;
    Warehouse warehouse;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();

        mockCampaignOptions = mock(CampaignOptions.class);
        mockCampaign = mock(Campaign.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
    }

    /**
     * For best results, don't use these directly, clone them first and use the clones!
     *
     */
    public static Stream<Armor> armorParameter() {
        return Stream.of(new Armor(1, ARMOR_TYPE, ARMOR_AMOUNT, Entity.LOC_NONE, false, false, mockCampaign),
              new ProtoMekArmor(1, ARMOR_TYPE, ARMOR_AMOUNT, Entity.LOC_NONE, false, mockCampaign),
              new BAArmor(1, ARMOR_AMOUNT, ARMOR_TYPE, Entity.LOC_NONE, false, mockCampaign),
              new SVArmor(SV_ARMOR_BAR, TechRating.D, ARMOR_AMOUNT, Entity.LOC_NONE, mockCampaign));
    }

    @BeforeEach
    public void beforeEach() {
        warehouse = new Warehouse();
        when(mockCampaign.getWarehouse()).thenReturn(warehouse);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void getAmountAvailableDifferentQualities(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        // Act
        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);
        int result = armor.getAmountAvailable();

        // Assert
        // Tests for #6780
        assertEquals(2 * ARMOR_AMOUNT, result);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDifferentQualitiesAdd(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);

        // Act
        armor.changeAmountAvailable(10);
        int amountAvailable = armor.getAmountAvailable();

        // Assert
        // Tests for #6780
        assertEquals((2 * ARMOR_AMOUNT) + 10, amountAvailable);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDifferentQualitiesRemovePartialPart(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);

        // Act
        armor.changeAmountAvailable(-1); // Less than any part stack
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        // Tests for #6780
        assertEquals(((2 * ARMOR_AMOUNT) - 1), amountAvailable);
        assertEquals(2, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDifferentQualitiesRemoveFullPart(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);

        // Act
        armor.changeAmountAvailable(-ARMOR_AMOUNT); // Exactly one part stack
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        // Tests for #6780
        assertEquals(ARMOR_AMOUNT, amountAvailable);
        assertEquals(1, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDifferentQualitiesRemoveMoreThanOnePart(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);

        // Act
        armor.changeAmountAvailable(-(ARMOR_AMOUNT + 1)); // More than a single stack
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        // Tests for #6780
        assertEquals(ARMOR_AMOUNT - 1, amountAvailable);
        assertEquals(1, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDifferentQualitiesRemoveAll(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);

        // Act
        armor.changeAmountAvailable(-(ARMOR_AMOUNT * 2)); // All the stacks
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        // Tests for #6780
        assertEquals(0, amountAvailable);
        assertEquals(0, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDifferentQualitiesRemoveMoreThanAll(Armor armor) {
        // Arrange
        Armor armorQualityD = armor.clone();
        armorQualityD.setQuality(PartQuality.QUALITY_D);

        Armor armorQualityC = armor.clone();
        armorQualityC.setQuality(PartQuality.QUALITY_C);

        warehouse.addPart(armorQualityD, false);
        warehouse.addPart(armorQualityC, false);

        // Act
        armor.changeAmountAvailable(-(ARMOR_AMOUNT * 2) - 1); // More than everything!
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        // Tests for #6780
        assertEquals(0, amountAvailable);
        assertEquals(0, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void getAmountAvailableDoesntConsiderDifferentType(Armor armor) {
        // Arrange
        Armor armorSameType = armor.clone();

        Armor armorDifferentType = getDifferentArmorType(armor);

        warehouse.addPart(armorSameType, false);
        warehouse.addPart(armorDifferentType, false);

        // Act
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        assertEquals(ARMOR_AMOUNT, amountAvailable);
        assertEquals(2, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDoesntConsiderDifferentTypeAdd(Armor armor) {
        // Arrange
        Armor armorSameType = armor.clone();

        Armor armorDifferentType = getDifferentArmorType(armor);

        warehouse.addPart(armorSameType, false);
        warehouse.addPart(armorDifferentType, false);

        // Act
        armor.changeAmountAvailable((12));
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        assertEquals(ARMOR_AMOUNT + 12, amountAvailable);
        assertEquals(2, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDoesntConsiderDifferentTypeRemove(Armor armor) {
        // Arrange
        Armor armorSameType = armor.clone();

        Armor armorDifferentType = getDifferentArmorType(armor);

        warehouse.addPart(armorSameType, false);
        warehouse.addPart(armorDifferentType, false);

        // Act
        armor.changeAmountAvailable(-1);
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        assertEquals(ARMOR_AMOUNT - 1, amountAvailable);
        assertEquals(2, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDoesntConsiderDifferentTypeRemoveMore(Armor armor) {
        // Arrange
        Armor armorSameType = armor.clone();

        Armor armorDifferentType = getDifferentArmorType(armor);

        warehouse.addPart(armorSameType, false);
        warehouse.addPart(armorDifferentType, false);

        // Act
        armor.changeAmountAvailable(-(ARMOR_AMOUNT + 12));
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        assertEquals(0, amountAvailable);
        assertEquals(1, partCount);
    }

    @ParameterizedTest
    @MethodSource(value = "armorParameter")
    public void changeAmountAvailableDoesntConsiderDifferentTypeRemoveNoMatch(Armor armor) {
        // Arrange
        Armor armorDifferentType = getDifferentArmorType(armor);

        warehouse.addPart(armorDifferentType, false);

        // Act
        armor.changeAmountAvailable(-12);
        int amountAvailable = armor.getAmountAvailable();
        int partCount = warehouse.getSpareParts().size();

        // Assert
        assertEquals(0, amountAvailable);
        assertEquals(1, partCount);
    }

    private Armor getDifferentArmorType(Armor armor) {
        if (armor instanceof SVArmor) {
            return new SVArmor(DIFFERENT_SV_ARMOR_BAR, TechRating.D, ARMOR_AMOUNT, Entity.LOC_NONE, mockCampaign);
        } else {
            Armor differentArmor = armor.clone();
            differentArmor.changeType(DIFFERENT_ARMOR_TYPE, false);
            return differentArmor;
        }

    }
}
