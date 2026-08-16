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
package mekhq.campaign.campaignOptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CampaignOptionsStore} and its {@link CampaignOption} typed keys.
 *
 * <p>A type mismatch such as {@code set(CHECK_MAINTENANCE, 7)} is intentionally not tested here: the typed key
 * makes it a compile error, which is the whole point of the design.</p>
 */
class CampaignOptionsStoreTest {
    @Test
    void freshStore_seedsEveryManagedKeyToItsDeclaredDefault() {
        CampaignOptionsStore store = new CampaignOptionsStore(CampaignOption.values());

        for (CampaignOption<?> option : CampaignOption.values()) {
            Object seeded = store.get(option);
            // Every managed key is seeded with a non-null value of its declared type. (Value-equality for immutable
            // defaults is covered by the round-trip and default-drift tests; mutable defaults like RandomOriginOptions
            // have no equals(), and each defaultValue() call is a fresh instance by design.)
            assertNotNull(seeded, () -> "no seeded value for " + option.xmlTag());
            assertTrue(option.type().isInstance(seeded),
                  () -> "seeded value for " + option.xmlTag() + " is not a " + option.type().getSimpleName());
        }
    }

    @Test
    void set_thenGet_roundTripsBooleanKey() {
        CampaignOptionsStore store = new CampaignOptionsStore(CampaignOption.values());

        store.set(CampaignOption.CHECK_MAINTENANCE, false);

        assertEquals(false, store.get(CampaignOption.CHECK_MAINTENANCE));
    }

    @Test
    void set_thenGet_roundTripsIntegerKey() {
        CampaignOptionsStore store = new CampaignOptionsStore(CampaignOption.values());

        store.set(CampaignOption.MAINTENANCE_CYCLE_DAYS, 30);

        assertEquals(30, store.get(CampaignOption.MAINTENANCE_CYCLE_DAYS));
    }

    /**
     * Guards against default drift: a brand-new {@link CampaignOptions} must report the same maintenance defaults it
     * did before these options were migrated onto the typed-key store.
     */
    @Test
    void newCampaignOptions_reportsPreRefactorMaintenanceDefaults() {
        CampaignOptions campaignOptions = new CampaignOptions();

        assertEquals(true, campaignOptions.isCheckMaintenance());
        assertEquals(7, campaignOptions.getMaintenanceCycleDays());
        assertEquals(-1, campaignOptions.getMaintenanceBonus());
        assertEquals(true, campaignOptions.isUseQualityMaintenance());
        assertEquals(false, campaignOptions.isReverseQualityNames());
        assertEquals(true, campaignOptions.isUseRandomUnitQualities());
        assertEquals(false, campaignOptions.isUseUnofficialMaintenance());
        assertEquals(false, campaignOptions.isLogMaintenance());
        assertEquals(4, campaignOptions.getDefaultMaintenanceTime());
    }
}
