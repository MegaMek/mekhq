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

package mekhq.gui.dialog.quartermaster;

import static mekhq.campaign.personnel.enums.PersonnelRole.ADMINISTRATOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEKWARRIOR;
import static mekhq.campaign.personnel.enums.PersonnelRole.MEK_TECH;
import static mekhq.campaign.personnel.enums.PersonnelRole.NONE;
import static mekhq.campaign.personnel.enums.PersonnelRole.SOLDIER;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_ADVANCED;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_BASIC;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DESCARTES_MK_XXV;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_PERSONAL_COMPUTER;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.count;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.kit;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.person;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.warehouseWith;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.wireArmorKit;
import static mekhq.campaign.personnel.quartermaster.KitTestSupport.wireEquipmentKits;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.Campaign;
import mekhq.campaign.LocalWarehouse;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.KitSlot;
import mekhq.campaign.unit.Unit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

/**
 * Coverage for the commit and tally helpers behind the kit-issue dialog's armor and equipment-kit tabs: only people
 * lacking a kit are counted or issued one, a shortfall is remembered and ordered in one go, and stripping touches only
 * what it should.
 */
class KitDialogCommitTest {
    private Campaign campaign;
    private EquipmentType mekWarriorBasic;
    private EquipmentType toolkit;

    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    @BeforeEach
    void beforeEach() {
        campaign = MHQTestUtilities.mockCampaign();
        mekWarriorBasic = kit(KIT_MEKWARRIOR_BASIC);
        toolkit = kit(KIT_BASIC_TOOLKIT);
    }

    // region armor tally
    @Test
    void wearsKitReadsACrewMembersOwnKit() {
        Person mekWarrior = person(MEKWARRIOR, NONE);
        wireArmorKit(mekWarrior, KIT_MEKWARRIOR_BASIC);

        assertTrue(IssueEquipmentDialog.wearsKit(mekWarrior, Category.MEKWARRIOR, mekWarriorBasic));
        assertFalse(IssueEquipmentDialog.wearsKit(mekWarrior, Category.MEKWARRIOR, kit(KIT_MEKWARRIOR_ADVANCED)));
    }

    @Test
    void wearsKitReadsASoldiersPlatoonKit() {
        Person soldier = person(SOLDIER, NONE);
        wireArmorKit(soldier, DEFAULT_ARMOR_KIT_NAME);
        Unit platoon = mock(Unit.class);
        when(platoon.getArmorKitName()).thenReturn(KIT_MEKWARRIOR_BASIC);
        when(soldier.getUnit()).thenReturn(platoon);

        assertTrue(IssueEquipmentDialog.wearsKit(soldier, Category.SOLDIER, mekWarriorBasic));
    }

    @Test
    void countLackingSkipsPeopleAlreadyWearingTheKit() {
        Person wearing = person(MEKWARRIOR, NONE);
        wireArmorKit(wearing, KIT_MEKWARRIOR_BASIC);
        Person lacking = person(MEKWARRIOR, NONE);

        assertEquals(1, IssueEquipmentDialog.countLacking(Category.MEKWARRIOR, List.of(wearing, lacking),
              mekWarriorBasic));
    }
    // endregion armor tally

    // region armor commit
    @Test
    void commitCrewKitIssuesFromStockOrdersTheShortfallAndSkipsTheAlreadyKitted() {
        LocalWarehouse warehouse = warehouseWith(mekWarriorBasic);
        Person wearing = person(MEKWARRIOR, NONE);
        wireArmorKit(wearing, KIT_MEKWARRIOR_BASIC);
        Person fromStock = person(MEKWARRIOR, NONE);
        Person shortfall = person(MEKWARRIOR, NONE);
        for (Person person : List.of(wearing, fromStock, shortfall)) {
            when(person.getWarehouse()).thenReturn(warehouse);
        }

        KitIssueSection.CommitTotals totals = new KitIssueSection.CommitTotals();
        IssueEquipmentDialog.commitCrewKit(List.of(wearing, fromStock, shortfall), Category.MEKWARRIOR,
              mekWarriorBasic, campaign, totals);

        assertEquals(1, totals.issued);
        assertEquals(1, totals.ordered);
        assertFalse(totals.changed.contains(wearing));
        assertEquals(KIT_MEKWARRIOR_BASIC, fromStock.getArmorKitName());
        assertEquals(KIT_MEKWARRIOR_BASIC, shortfall.getIntendedArmorKitName());
        assertEquals(DEFAULT_ARMOR_KIT_NAME, shortfall.getArmorKitName());
        verify(campaign.getPlayerForce().getShoppingList(), times(1)).addShoppingItem(any(), eq(1), any());
    }

    @Test
    void commitCrewStripReturnsKitsAndIgnoresPeopleInCoveralls() {
        LocalWarehouse warehouse = warehouseWith();
        Person wearing = person(MEKWARRIOR, NONE);
        wireArmorKit(wearing, KIT_MEKWARRIOR_BASIC);
        Person inCoveralls = person(MEKWARRIOR, NONE);
        inCoveralls.setIntendedArmorKitName(KIT_MEKWARRIOR_BASIC);
        for (Person person : List.of(wearing, inCoveralls)) {
            when(person.getWarehouse()).thenReturn(warehouse);
        }

        KitIssueSection.CommitTotals totals = new KitIssueSection.CommitTotals();
        IssueEquipmentDialog.commitCrewStrip(List.of(wearing, inCoveralls), campaign, totals);

        assertEquals(1, totals.removed);
        assertEquals(DEFAULT_ARMOR_KIT_NAME, wearing.getArmorKitName());
        assertEquals(1, count(warehouse, mekWarriorBasic));
        assertNull(inCoveralls.getIntendedArmorKitName(), "stripping cancels an awaited kit");
    }
    // endregion armor commit

    // region equipment-kit commit
    @Test
    void commitIssueFillsOnlyTheChosenSlotAndRemembersTheShortfall() {
        LocalWarehouse warehouse = warehouseWith(toolkit);
        Person alreadyOwns = person(MEK_TECH, NONE);
        wireEquipmentKits(alreadyOwns, KIT_DESCARTES_MK_XXV, KIT_BASIC_TOOLKIT); // owns it, in the other slot
        Person fromStock = person(MEK_TECH, ADMINISTRATOR);
        wireEquipmentKits(fromStock, KIT_DESCARTES_MK_XXV, KIT_PERSONAL_COMPUTER);
        Person shortfall = person(MEK_TECH, NONE);
        for (Person person : List.of(alreadyOwns, fromStock, shortfall)) {
            when(person.getWarehouse()).thenReturn(warehouse);
        }

        KitIssueSection.CommitTotals totals = new KitIssueSection.CommitTotals();
        ToolKitSection.commitIssue(List.of(alreadyOwns, fromStock, shortfall), toolkit, KitSlot.PRIMARY, campaign,
              totals);

        assertEquals(KIT_DESCARTES_MK_XXV, alreadyOwns.getRepairKitName(), "not issued a second copy");
        assertEquals(KIT_BASIC_TOOLKIT, fromStock.getRepairKitName());
        assertEquals(KIT_PERSONAL_COMPUTER, fromStock.getSecondaryKitName(), "the other slot is untouched");
        assertEquals(KIT_BASIC_TOOLKIT, shortfall.getIntendedKitName(KitSlot.PRIMARY));
        assertNull(shortfall.getIntendedKitName(KitSlot.SECONDARY));
        assertEquals(1, totals.issued);
        assertEquals(1, totals.ordered);
        assertEquals(1, count(warehouse, kit(KIT_DESCARTES_MK_XXV)), "the replaced primary kit goes back to stores");
    }

    @Test
    void commitStripEmptiesOnlyTheChosenSlotAndCancelsItsAwaitedKit() {
        LocalWarehouse warehouse = warehouseWith();
        Person tech = person(MEK_TECH, ADMINISTRATOR);
        wireEquipmentKits(tech, KIT_BASIC_TOOLKIT, KIT_PERSONAL_COMPUTER);
        tech.setIntendedKitName(KitSlot.SECONDARY, KIT_DESCARTES_MK_XXV);
        tech.setIntendedKitName(KitSlot.PRIMARY, KIT_DESCARTES_MK_XXV);
        when(tech.getWarehouse()).thenReturn(warehouse);

        KitIssueSection.CommitTotals totals = new KitIssueSection.CommitTotals();
        ToolKitSection.commitStrip(List.of(tech), KitSlot.SECONDARY, campaign, totals);

        assertEquals(KIT_BASIC_TOOLKIT, tech.getRepairKitName());
        assertNull(tech.getSecondaryKitName());
        assertNull(tech.getIntendedKitName(KitSlot.SECONDARY));
        assertEquals(KIT_DESCARTES_MK_XXV, tech.getIntendedKitName(KitSlot.PRIMARY), "the other slot's intent stays");
        assertEquals(1, totals.removed);
        assertEquals(1, count(warehouse, kit(KIT_PERSONAL_COMPUTER)));
    }
    // endregion equipment-kit commit
}
