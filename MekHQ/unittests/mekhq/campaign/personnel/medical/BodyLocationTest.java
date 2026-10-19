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
package mekhq.campaign.personnel.medical;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BodyLocation}, covering the spine location added for wing and extraneous limb prosthetics.
 */
class BodyLocationTest {
    @Test
    void spineSitsUnderTheChest() {
        assertSame(BodyLocation.CHEST, BodyLocation.SPINE.getParent());
        assertTrue(BodyLocation.SPINE.isChildOf(BodyLocation.CHEST));
        assertTrue(BodyLocation.SPINE.isImmediateChildOf(BodyLocation.CHEST));
        assertSame(BodyLocation.CHEST, BodyLocation.SPINE.getPrimaryLocation());
    }

    @Test
    void spineIsNotALimb() {
        assertFalse(BodyLocation.SPINE.isLimb(), "a hit to the spine must not risk severing it like a limb");
    }

    @Test
    void spineResolvesFromItsIdAndName() {
        assertSame(BodyLocation.SPINE, BodyLocation.of(48));
        assertSame(BodyLocation.SPINE, BodyLocation.of("48"));
        assertSame(BodyLocation.SPINE, BodyLocation.of("SPINE"));
        assertSame(BodyLocation.SPINE, BodyLocation.of("spine"));
    }

    @Test
    void spineSurvivesTheXmlRoundTrip() {
        BodyLocation.XMLAdapter adapter = new BodyLocation.XMLAdapter();
        assertSame(BodyLocation.SPINE, adapter.unmarshal(adapter.marshal(BodyLocation.SPINE)));
    }

    @Test
    void spineHasADisplayName() {
        assertFalse(BodyLocation.SPINE.locationName().isBlank());
    }
}
