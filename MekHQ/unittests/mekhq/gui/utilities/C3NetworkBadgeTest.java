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
package mekhq.gui.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import megamek.common.units.Entity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the network badge shown against a unit in the force tree.
 *
 * <p>What matters is that units on one network share a colour and units on different networks do not -
 * which colour they get carries no meaning on its own.</p>
 */
class C3NetworkBadgeTest {

    @BeforeEach
    void forgetPreviousAssignments() {
        C3NetworkBadge.resetColourAssignments();
    }

    private static Entity c3iUnit(String networkId, int freeNodes) {
        Entity entity = mock(Entity.class);
        when(entity.hasC3i()).thenReturn(true);
        when(entity.getC3NetId()).thenReturn(networkId);
        when(entity.calculateFreeC3Nodes()).thenReturn(freeNodes);
        return entity;
    }

    /** Strips the colour out of the badge so two badges can be compared by colour alone. */
    private static String colourOf(String badge) {
        int start = badge.indexOf('\'') + 1;
        return badge.substring(start, badge.indexOf('\'', start));
    }

    @Test
    void unitsOnOneNetworkShareAColour() {
        String first = C3NetworkBadge.forEntity(c3iUnit("C3i.42", 2));
        String second = C3NetworkBadge.forEntity(c3iUnit("C3i.42", 2));

        assertTrue(first.contains("[C3i]"), "the badge names the network type");
        assertEquals(colourOf(first), colourOf(second),
              "two units on one network must be the same colour");
    }

    @Test
    void unitsOnDifferentNetworksDoNotShareAColour() {
        String first = C3NetworkBadge.forEntity(c3iUnit("C3i.42", 2));
        String second = C3NetworkBadge.forEntity(c3iUnit("C3i.7", 2));

        assertNotEquals(colourOf(first), colourOf(second),
              "a unit on another network must be distinguishable");
    }

    /**
     * A unit whose network holds only itself reads as unconnected: it has the hardware and nobody to
     * talk to, which is what the player needs to notice.
     */
    @Test
    void aUnitAloneOnItsNetworkIsMarkedUnconnected() {
        String alone = C3NetworkBadge.forEntity(c3iUnit("C3i.42", 5));
        String connected = C3NetworkBadge.forEntity(c3iUnit("C3i.42", 2));

        assertEquals("#808080", colourOf(alone), "unconnected is grey, not a network colour");
        assertNotEquals(colourOf(alone), colourOf(connected));
    }

    @Test
    void aUnitWithNoNetworkHardwareGetsNoBadge() {
        Entity plain = mock(Entity.class);
        when(plain.hasC3i()).thenReturn(false);
        when(plain.hasC3()).thenReturn(false);
        when(plain.hasNavalC3()).thenReturn(false);

        assertEquals("", C3NetworkBadge.forEntity(plain));
    }

    @Test
    void aNullUnitGetsNoBadge() {
        assertEquals("", C3NetworkBadge.forEntity(null));
    }

    /** A C3 master and its slaves colour alike, since the master is what identifies the network. */
    @Test
    void aC3MasterAndItsSlavesShareAColour() {
        Entity master = mock(Entity.class);
        when(master.hasC3()).thenReturn(true);
        when(master.getC3UUIDAsString()).thenReturn("master-uuid");
        when(master.getC3Master()).thenReturn(master);

        Entity slave = mock(Entity.class);
        when(slave.hasC3()).thenReturn(true);
        when(slave.getC3Master()).thenReturn(master);

        assertEquals(colourOf(C3NetworkBadge.forEntity(master)),
              colourOf(C3NetworkBadge.forEntity(slave)),
              "a lance on one master is one network");
        assertTrue(C3NetworkBadge.forEntity(slave).contains("[C3]"));
    }
}
