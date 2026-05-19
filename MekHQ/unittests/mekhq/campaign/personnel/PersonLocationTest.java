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
package mekhq.campaign.personnel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.Set;

import mekhq.campaign.FixedLocation;
import mekhq.campaign.location.AcademyCampusLocation;
import mekhq.campaign.universe.PlanetarySystem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PersonLocationTest {

    Person person;

    @BeforeEach
    void setUp() {
        person = new Person("Test", "Person", null, "MERC");
    }

    @Test
    void getLocationNode_isNotNull() {
        assertNotNull(person.getLocationNode());
    }

    @Test
    void getLocationNode_locatableIsThis() {
        assertSame(person, person.getLocationNode().getLocatable());
    }

    @Test
    void getLocationNode_noParentByDefault() {
        assertNull(person.getLocationNode().getParent());
    }

    @Test
    void getPersonnelAtLocation_returnsSelf() {
        Set<Person> result = person.getPersonnelAtLocation();
        assertTrue(result.contains(person));
    }

    @Test
    void getPersonnelAtLocation_returnsExactlyOnePerson() {
        assertEquals(1, person.getPersonnelAtLocation().size());
    }

    @Nested
    class ParentLinking {

        FixedLocation fixed;
        AcademyCampusLocation campus;

        @BeforeEach
        void setUp() {
            fixed = new FixedLocation(mock(PlanetarySystem.class));
            campus = new AcademyCampusLocation("TestSet", "TestAcademy");
            campus.setParent(fixed);
        }

        @Test
        void setParent_toCampus_succeeds() {
            assertTrue(person.setParent(campus));
        }

        @Test
        void setParent_toCampus_wiresParentLink() {
            person.setParent(campus);
            assertSame(campus.getLocationNode(), person.getLocationNode().getParent());
        }

        @Test
        void setParent_toCampus_addsToChildrenOfCampus() {
            person.setParent(campus);
            assertTrue(campus.getLocationNode().getChildren().contains(person.getLocationNode()));
        }

        @Test
        void setParent_null_clearsParentLink() {
            person.setParent(campus);
            person.setParent(null);
            assertNull(person.getLocationNode().getParent());
        }

        @Test
        void setParent_null_removesFromCampusChildren() {
            person.setParent(campus);
            person.setParent(null);
            assertFalse(campus.getLocationNode().getChildren().contains(person.getLocationNode()));
        }

        @Test
        void campus_getPersonnelAtLocation_includesPersonAtCampus() {
            person.setParent(campus);
            assertTrue(campus.getPersonnelAtLocation().contains(person));
        }

        @Test
        void campus_getPersonnelAtLocation_excludesPersonAfterDetach() {
            person.setParent(campus);
            person.setParent(null);
            assertFalse(campus.getPersonnelAtLocation().contains(person));
        }

        @Test
        void fixed_getPersonnelAtLocation_includesPersonViaCampus() {
            person.setParent(campus);
            assertTrue(fixed.getPersonnelAtLocation().contains(person));
        }
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
