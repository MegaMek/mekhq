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
package mekhq.campaign.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import mekhq.campaign.FixedLocation;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class AcademyCampusLocationTest {

    static final String ACADEMY_SET = "TestSet";
    static final String ACADEMY_NAME = "Test Academy";

    AcademyCampusLocation campus;

    @BeforeEach
    void setUp() {
        campus = new AcademyCampusLocation(ACADEMY_SET, ACADEMY_NAME);
    }

    @Test
    void getLocationNode_isNotNull() {
        assertNotNull(campus.getLocationNode());
    }

    @Test
    void getLocationNode_locatableIsThis() {
        assertSame(campus, campus.getLocationNode().getLocatable());
    }

    @Test
    void getLocationNode_noParentByDefault() {
        assertNull(campus.getLocationNode().getParent());
    }

    @Test
    void getLocationNode_noChildrenByDefault() {
        assertTrue(campus.getLocationNode().getChildren().isEmpty());
    }

    @Test
    void getAcademySet_returnsConstructorArg() {
        assertEquals(ACADEMY_SET, campus.getAcademySet());
    }

    @Test
    void getAcademyName_returnsConstructorArg() {
        assertEquals(ACADEMY_NAME, campus.getAcademyName());
    }

    @Nested
    class ParentLinking {

        FixedLocation fixed;

        @BeforeEach
        void setUp() {
            fixed = new FixedLocation(mock(PlanetarySystem.class));
        }

        @Test
        void setParent_toFixedLocation_succeeds() {
            assertTrue(campus.setParent(fixed));
        }

        @Test
        void setParent_toFixedLocation_wiresParentLink() {
            campus.setParent(fixed);
            assertSame(fixed.getLocationNode(), campus.getLocationNode().getParent());
        }

        @Test
        void setParent_toFixedLocation_addsToFixedChildren() {
            campus.setParent(fixed);
            assertTrue(fixed.getLocationNode().getChildren().contains(campus.getLocationNode()));
        }

        @Test
        void setParent_null_clearsParentLink() {
            campus.setParent(fixed);
            campus.setParent(null);
            assertNull(campus.getLocationNode().getParent());
        }

        @Test
        void setParent_null_removesFromFixedChildren() {
            campus.setParent(fixed);
            campus.setParent(null);
            assertFalse(fixed.getLocationNode().getChildren().contains(campus.getLocationNode()));
        }

        @Test
        void canSetParent_toNakedCampusWithoutRoot_returnsFalse() {
            AcademyCampusLocation orphan = new AcademyCampusLocation("X", "Y");
            assertFalse(campus.canSetParent(orphan));
        }
    }
}
