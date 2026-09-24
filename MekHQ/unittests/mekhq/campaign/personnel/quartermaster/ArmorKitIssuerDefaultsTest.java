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

import static mekhq.campaign.personnel.enums.PersonnelRole.AEROSPACE_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.CONVENTIONAL_AIRCRAFT_PILOT;
import static mekhq.campaign.personnel.enums.PersonnelRole.DOCTOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.enums.PersonnelRole.VEHICLE_CREW_GROUND;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_AEROSPACE_PILOT;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_ADVANCED;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_BASIC;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.count;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.kit;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.person;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.setRoster;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.warehouseWith;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.wireArmorKit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.market.ForceShoppingList;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.AbstractKitIssuer.KitIssueTotals;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Coverage for the default-kit paths of {@link ArmorKitIssuer}: GM adds, role changes, bulk default issue, default
 * switching, and the one-off aerospace pilot kit handout.
 */
class ArmorKitIssuerDefaultsTest {
    private Campaign campaign;
    private CampaignOptions options;
    private EquipmentType basic;
    private EquipmentType advanced;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.mockCampaign();
        options = new CampaignOptions();
        when(campaign.getCampaignOptions()).thenReturn(options);
        basic = kit(KIT_MEKWARRIOR_BASIC);
        advanced = kit(KIT_MEKWARRIOR_ADVANCED);
    }

    private ForceShoppingList shoppingList() {
        return campaign.getPlayerForce().getShoppingList();
    }

    // region equipDefaultKitOnRecruitment
    @Test
    void gmAddGrantsTheDefaultDirectlyEvenWhenProcurementIsOff() {
        Person mekWarrior = person(MEKWARRIOR, NONE);
        when(mekWarrior.getWarehouse()).thenReturn(warehouseWith());
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);
        options.set(CampaignOption.ADD_DEFAULT_KIT_TO_PROCUREMENT, false);

        ArmorKitIssuer.equipDefaultKitOnRecruitment(mekWarrior, campaign, true);

        assertEquals(KIT_MEKWARRIOR_BASIC, mekWarrior.getArmorKitName());
        verify(shoppingList(), never()).addShoppingItem(any(), anyInt(), any());
    }

    @Test
    void aRegularRecruitGetsNothingWhenOutOfStockAndProcurementIsOff() {
        Person mekWarrior = person(MEKWARRIOR, NONE);
        when(mekWarrior.getWarehouse()).thenReturn(warehouseWith());
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        ArmorKitIssuer.equipDefaultKitOnRecruitment(mekWarrior, campaign, false);

        assertEquals(DEFAULT_ARMOR_KIT_NAME, mekWarrior.getArmorKitName());
        assertNull(mekWarrior.getIntendedArmorKitName());
    }
    // endregion equipDefaultKitOnRecruitment

    // region equipDefaultKitOnRoleChange
    @Test
    void roleChangeIssuesTheDefaultToSomeoneInCoveralls() {
        LocalWarehouse warehouse = warehouseWith(basic);
        Person mekWarrior = person(MEKWARRIOR, NONE);
        when(mekWarrior.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        ArmorKitIssuer.equipDefaultKitOnRoleChange(mekWarrior, campaign);

        assertEquals(KIT_MEKWARRIOR_BASIC, mekWarrior.getArmorKitName());
        assertEquals(0, count(warehouse, basic));
    }

    @Test
    void roleChangeLeavesSomeoneAlreadyWearingAKit() {
        LocalWarehouse warehouse = warehouseWith(basic);
        Person mekWarrior = person(MEKWARRIOR, NONE);
        wireArmorKit(mekWarrior, KIT_MEKWARRIOR_ADVANCED);
        when(mekWarrior.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        ArmorKitIssuer.equipDefaultKitOnRoleChange(mekWarrior, campaign);

        assertEquals(KIT_MEKWARRIOR_ADVANCED, mekWarrior.getArmorKitName());
        assertEquals(1, count(warehouse, basic));
    }

    @Test
    void roleChangeLeavesSomeoneAlreadyAwaitingAKit() {
        LocalWarehouse warehouse = warehouseWith(basic);
        Person mekWarrior = person(MEKWARRIOR, NONE);
        mekWarrior.setIntendedArmorKitName(KIT_MEKWARRIOR_ADVANCED);
        when(mekWarrior.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        ArmorKitIssuer.equipDefaultKitOnRoleChange(mekWarrior, campaign);

        assertEquals(DEFAULT_ARMOR_KIT_NAME, mekWarrior.getArmorKitName());
        assertEquals(KIT_MEKWARRIOR_ADVANCED, mekWarrior.getIntendedArmorKitName());
    }
    // endregion equipDefaultKitOnRoleChange

    // region issueDefaultKits
    @Test
    void issueDefaultKitsSwapsTheDefaultInAndReturnsTheOldKit() {
        LocalWarehouse warehouse = warehouseWith(basic);
        Person mekWarrior = person(MEKWARRIOR, NONE);
        wireArmorKit(mekWarrior, KIT_MEKWARRIOR_ADVANCED);
        when(mekWarrior.getWarehouse()).thenReturn(warehouse);
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        KitIssueTotals totals = new KitIssueTotals();
        ArmorKitIssuer.issueDefaultKits(List.of(mekWarrior), campaign, totals);

        assertEquals(KIT_MEKWARRIOR_BASIC, mekWarrior.getArmorKitName());
        assertEquals(1, count(warehouse, advanced));
        assertEquals(1, totals.issued);
    }

    @Test
    void issueDefaultKitsSkipsSoldiersAndPeopleAlreadyInTheKit() {
        Person soldier = person(SOLDIER, NONE);
        Person alreadyKitted = person(MEKWARRIOR, NONE);
        wireArmorKit(alreadyKitted, KIT_MEKWARRIOR_BASIC);
        when(soldier.getWarehouse()).thenReturn(warehouseWith());
        when(alreadyKitted.getWarehouse()).thenReturn(warehouseWith());
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        KitIssueTotals totals = new KitIssueTotals();
        ArmorKitIssuer.issueDefaultKits(List.of(soldier, alreadyKitted), campaign, totals);

        assertEquals(DEFAULT_ARMOR_KIT_NAME, soldier.getArmorKitName());
        assertNull(soldier.getIntendedArmorKitName());
        assertEquals(KIT_MEKWARRIOR_BASIC, alreadyKitted.getArmorKitName());
        assertEquals(0, totals.issued + totals.ordered);
        verify(shoppingList(), never()).addShoppingItem(any(), anyInt(), any());
    }

    @Test
    void issueDefaultKitsOrdersTheShortfallOnce() {
        Person first = person(MEKWARRIOR, NONE);
        Person second = person(MEKWARRIOR, NONE);
        when(first.getWarehouse()).thenReturn(warehouseWith());
        when(second.getWarehouse()).thenReturn(warehouseWith());
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        KitIssueTotals totals = new KitIssueTotals();
        ArmorKitIssuer.issueDefaultKits(List.of(first, second), campaign, totals);

        verify(shoppingList(), times(1)).addShoppingItem(any(), eq(2), any());
        assertEquals(2, totals.ordered);
        assertEquals(KIT_MEKWARRIOR_BASIC, first.getIntendedArmorKitName());
        assertEquals(KIT_MEKWARRIOR_BASIC, second.getIntendedArmorKitName());
    }
    // endregion issueDefaultKits

    // region switchDefaultKit
    @Test
    void switchDefaultKitOnlyMovesTheGroupsHoldersOfTheOldKit() {
        Person holder = person(MEKWARRIOR, NONE);
        wireArmorKit(holder, KIT_MEKWARRIOR_BASIC);
        when(holder.getWarehouse()).thenReturn(warehouseWith(advanced));

        Person otherKit = person(MEKWARRIOR, NONE);
        wireArmorKit(otherKit, KIT_AEROSPACE_PILOT);
        when(otherKit.getWarehouse()).thenReturn(warehouseWith(advanced));

        Person otherGroup = person(VEHICLE_CREW_GROUND, NONE);
        wireArmorKit(otherGroup, KIT_MEKWARRIOR_BASIC);
        when(otherGroup.getWarehouse()).thenReturn(warehouseWith(advanced));

        setRoster(campaign, List.of(holder, otherKit, otherGroup));

        KitIssueTotals totals = new KitIssueTotals();
        ArmorKitIssuer.switchDefaultKit(campaign, Category.MEKWARRIOR, KIT_MEKWARRIOR_BASIC, advanced, totals);

        assertEquals(KIT_MEKWARRIOR_ADVANCED, holder.getArmorKitName());
        assertEquals(KIT_AEROSPACE_PILOT, otherKit.getArmorKitName());
        assertEquals(KIT_MEKWARRIOR_BASIC, otherGroup.getArmorKitName(), "vehicle crew are a different group");
        assertEquals(1, totals.issued);
    }

    @Test
    void switchDefaultKitKeepsTheOldKitUntilTheOrderedOneArrives() {
        Person holder = person(MEKWARRIOR, NONE);
        wireArmorKit(holder, KIT_MEKWARRIOR_BASIC);
        when(holder.getWarehouse()).thenReturn(warehouseWith());
        setRoster(campaign, List.of(holder));

        KitIssueTotals totals = new KitIssueTotals();
        ArmorKitIssuer.switchDefaultKit(campaign, Category.MEKWARRIOR, KIT_MEKWARRIOR_BASIC, advanced, totals);

        assertEquals(KIT_MEKWARRIOR_BASIC, holder.getArmorKitName());
        assertEquals(KIT_MEKWARRIOR_ADVANCED, holder.getIntendedArmorKitName());
        assertEquals(1, totals.ordered);
    }
    // endregion switchDefaultKit

    // region equipAllAerospacePilotsWithKit
    @Test
    void aerospaceHandoutEquipsPrimaryAndSecondaryAerospacePilotsOnly() {
        Person primaryPilot = person(AEROSPACE_PILOT, NONE);
        Person secondaryPilot = person(DOCTOR, AEROSPACE_PILOT);
        Person conventionalPilot = person(CONVENTIONAL_AIRCRAFT_PILOT, NONE);
        Person mekWarrior = person(MEKWARRIOR, NONE);
        setRoster(campaign, List.of(primaryPilot, secondaryPilot, conventionalPilot, mekWarrior));

        ArmorKitIssuer.equipAllAerospacePilotsWithKit(campaign);

        assertEquals(KIT_AEROSPACE_PILOT, primaryPilot.getArmorKitName());
        assertEquals(KIT_AEROSPACE_PILOT, secondaryPilot.getArmorKitName());
        assertEquals(DEFAULT_ARMOR_KIT_NAME, conventionalPilot.getArmorKitName());
        assertEquals(DEFAULT_ARMOR_KIT_NAME, mekWarrior.getArmorKitName());
        verify(shoppingList(), never()).addShoppingItem(any(), anyInt(), any());
    }

    @Test
    void aerospaceHandoutClearsAnAwaitedKit() {
        Person pilot = person(AEROSPACE_PILOT, NONE);
        pilot.setIntendedArmorKitName(KIT_AEROSPACE_PILOT);
        setRoster(campaign, List.of(pilot));

        ArmorKitIssuer.equipAllAerospacePilotsWithKit(campaign);

        assertEquals(KIT_AEROSPACE_PILOT, pilot.getArmorKitName());
        assertNull(pilot.getIntendedArmorKitName());
    }
    // endregion equipAllAerospacePilotsWithKit
}
