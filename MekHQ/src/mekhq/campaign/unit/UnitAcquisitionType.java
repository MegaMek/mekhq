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
package mekhq.campaign.unit;

/**
 * The means by which a {@link Unit} entered the campaign.
 *
 * <p>Every unit added to the campaign records how it was acquired in its unit log, so each constant maps to the
 * resource key of the log message describing that form of acquisition.</p>
 */
public enum UnitAcquisitionType {
    /** The unit joined the campaign without a more specific acquisition form being known. */
    ACQUIRED("unitAcquired.text"),
    /** The unit was bought, whether from a market, an auction, or the acquisition system. */
    PURCHASED("unitPurchased.text"),
    /** The unit was claimed as loot after a scenario. */
    LOOT("unitLooted.text"),
    /** The unit was recovered from the battlefield as salvage. */
    SALVAGED("unitSalvaged.text"),
    /** The unit was given to the campaign, for example as a faction accolade. */
    GIFT("unitGifted.text"),
    /** The unit was added directly by the player acting as GM. */
    GM_ADDED("unitGMAdded.text");

    private final String resourceKey;

    UnitAcquisitionType(String resourceKey) {
        this.resourceKey = resourceKey;
    }

    /**
     * @return the key of the {@code mekhq.resources.LogEntries} string describing this form of acquisition
     */
    public String getResourceKey() {
        return resourceKey;
    }
}
