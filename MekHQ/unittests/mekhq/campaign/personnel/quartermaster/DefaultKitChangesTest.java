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

import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.DEFAULT_ARMOR_KIT_NAME;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_ADVANCED;
import static mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.KIT_MEKWARRIOR_BASIC;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_BASIC_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_DELUXE_TOOLKIT;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_NOTEPUTER;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KIT_PERSONAL_COMPUTER;
import static mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.NO_DEFAULT_KIT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.campaignOptions.CampaignOption;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.quartermaster.ArmorKitCatalog.Category;
import mekhq.campaign.personnel.quartermaster.DefaultKitChanges.Change;
import mekhq.campaign.personnel.quartermaster.EquipmentKitCatalog.KitProfession;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DefaultKitChangesTest {
    @BeforeAll
    static void beforeAll() {
        EquipmentType.initializeTypes();
    }

    /** Snapshot {@code before}, then apply {@code change} to the options, then detect. */
    private static List<Change> detectAfter(CampaignOptions options, Runnable change) {
        Map<CampaignOption<String>, String> before = DefaultKitChanges.snapshot(options);
        change.run();
        return DefaultKitChanges.detect(before, options);
    }

    // region snapshot
    @Test
    void snapshotCoversEveryProfessionAndEveryArmorGroupButSoldiers() {
        Map<CampaignOption<String>, String> snapshot = DefaultKitChanges.snapshot(new CampaignOptions());

        assertEquals(KitProfession.values().length + 3, snapshot.size());
        for (KitProfession profession : KitProfession.values()) {
            assertTrue(snapshot.containsKey(EquipmentKitIssuer.defaultKitOption(profession)), profession.name());
        }
        assertTrue(snapshot.containsKey(CampaignOption.MEKWARRIOR_DEFAULT_KIT));
        assertTrue(snapshot.containsKey(CampaignOption.VEHICLE_CREW_DEFAULT_KIT));
        assertTrue(snapshot.containsKey(CampaignOption.AIRCRAFT_DEFAULT_KIT));
        assertNull(ArmorKitIssuer.defaultKitOption(Category.SOLDIER));
    }
    // endregion snapshot

    // region detect — equipment kits
    @Test
    void detectReportsARealToolKitSwitchedToAnotherRealKit() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.ADMIN_DEFAULT_TOOL_KIT, KIT_PERSONAL_COMPUTER);

        List<Change> changes = detectAfter(options,
              () -> options.set(CampaignOption.ADMIN_DEFAULT_TOOL_KIT, KIT_NOTEPUTER));

        assertEquals(1, changes.size());
        Change change = changes.get(0);
        assertEquals(KitProfession.ADMIN, change.profession());
        assertNull(change.category());
        assertEquals(KIT_PERSONAL_COMPUTER, change.oldKitName());
        assertEquals(KIT_NOTEPUTER, change.newKit().getInternalName());
    }

    @Test
    void detectIgnoresAToolKitTurnedOnFromNone() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, NO_DEFAULT_KIT);

        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT)).isEmpty());
    }

    @Test
    void detectIgnoresAToolKitTurnedOffToNone() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);

        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, NO_DEFAULT_KIT)).isEmpty());
    }

    @Test
    void detectIgnoresAnUnchangedDefault() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);

        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT)).isEmpty());
    }

    @Test
    void detectIgnoresAnUnknownKitOnEitherSide() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, "Not A Real Kit");
        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT)).isEmpty());

        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, "Not A Real Kit")).isEmpty());
    }

    @Test
    void detectReportsEachChangedProfessionSeparately() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);
        options.set(CampaignOption.MECHANIC_DEFAULT_TOOL_KIT, KIT_BASIC_TOOLKIT);

        List<Change> changes = detectAfter(options, () -> {
            options.set(CampaignOption.MEK_TECH_DEFAULT_TOOL_KIT, KIT_DELUXE_TOOLKIT);
            options.set(CampaignOption.MECHANIC_DEFAULT_TOOL_KIT, KIT_DELUXE_TOOLKIT);
        });

        assertEquals(2, changes.size());
        assertEquals(KitProfession.MEK_TECH, changes.get(0).profession());
        assertEquals(KitProfession.MECHANIC, changes.get(1).profession());
    }
    // endregion detect — equipment kits

    // region detect — armor kits
    @Test
    void detectReportsARealArmorKitSwitchedToAnotherRealKit() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);

        List<Change> changes = detectAfter(options,
              () -> options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_ADVANCED));

        assertEquals(1, changes.size());
        Change change = changes.get(0);
        assertEquals(Category.MEKWARRIOR, change.category());
        assertNull(change.profession());
        assertEquals(KIT_MEKWARRIOR_BASIC, change.oldKitName());
        assertEquals(KIT_MEKWARRIOR_ADVANCED, change.newKit().getInternalName());
    }

    @Test
    void detectTreatsCoverallsAsTheArmorKitOffValue() {
        CampaignOptions options = new CampaignOptions();
        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, DEFAULT_ARMOR_KIT_NAME);
        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC)).isEmpty(),
              "coveralls -> kit is turning the default on, not a switch");

        options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, KIT_MEKWARRIOR_BASIC);
        assertTrue(detectAfter(options,
              () -> options.set(CampaignOption.MEKWARRIOR_DEFAULT_KIT, DEFAULT_ARMOR_KIT_NAME)).isEmpty(),
              "kit -> coveralls is turning the default off, not a switch");
    }
    // endregion detect — armor kits

    @Test
    void changeFallsBackToTheRawNameForAnUnknownOldKit() {
        Change change = new Change(KitProfession.ADMIN, null, "Gone Kit", EquipmentType.get(KIT_NOTEPUTER));
        assertEquals("Gone Kit", change.oldKitDisplayName());
        assertFalse(change.oldKitDisplayName().isBlank());
    }
}
