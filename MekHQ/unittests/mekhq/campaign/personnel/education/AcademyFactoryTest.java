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
package mekhq.campaign.personnel.education;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link AcademyFactory} focusing on the academy merging behavior
 * when user files override base academy files.
 */
class AcademyFactoryTest {

    private AcademyFactory factory;

    @BeforeEach
    void setUp() throws Exception {
        // Reset the singleton instance
        Field instanceField = AcademyFactory.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // Create a new instance using reflection to access the private constructor
        Constructor<AcademyFactory> constructor = AcademyFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        factory = constructor.newInstance();

        // Clear the academy map to start fresh (constructor loads from disk)
        Field mapField = AcademyFactory.class.getDeclaredField("academyMap");
        mapField.setAccessible(true);
        mapField.set(factory, new HashMap<String, Map<String, Academy>>());
    }

    private String createAcademyXml(String... academyNames) {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<academy>\n");
        for (String name : academyNames) {
            xml.append("  <academy>\n");
            xml.append("    <name>").append(name).append("</name>\n");
            xml.append("    <tuition>1000</tuition>\n");
            xml.append("  </academy>\n");
        }
        xml.append("</academy>\n");
        return xml.toString();
    }

    @Test
    void testBaseAcademiesAreLoaded() {
        String xml = createAcademyXml("Academy A", "Academy B");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        List<Academy> academies = factory.getAllAcademiesForSet("TestSet");
        assertEquals(2, academies.size());

        Academy academyA = academies.stream()
            .filter(a -> a.getName().equals("Academy A"))
            .findFirst().orElse(null);
        Academy academyB = academies.stream()
            .filter(a -> a.getName().equals("Academy B"))
            .findFirst().orElse(null);

        assertNotNull(academyA);
        assertNotNull(academyB);
        assertEquals(0, academyA.getId());
        assertEquals(1, academyB.getId());
    }

    @Test
    void testUserAcademyOverridesBaseWithSameName() {
        // Load base academies
        String baseXml = createAcademyXml("Academy A", "Academy B");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(baseXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        // Load user academies that override Academy B
        String userXml = createAcademyXml("Academy B");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(userXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        List<Academy> academies = factory.getAllAcademiesForSet("TestSet");

        // Should still have both academies
        assertEquals(2, academies.size());

        // Academy B should have preserved its original ID (1)
        Academy academyB = academies.stream()
            .filter(a -> a.getName().equals("Academy B"))
            .findFirst().orElse(null);
        assertNotNull(academyB);
        assertEquals(1, academyB.getId(), "Overriding academy should preserve original ID");
    }

    @Test
    void testBaseAcademiesPreservedWhenNotInUserFile() {
        // Load base academies
        String baseXml = createAcademyXml("Academy A", "Academy B", "Academy C");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(baseXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        // Load user academies with only Academy B (A and C should be preserved)
        String userXml = createAcademyXml("Academy B");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(userXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        List<Academy> academies = factory.getAllAcademiesForSet("TestSet");

        // Should have all three academies
        assertEquals(3, academies.size());

        assertTrue(academies.stream().anyMatch(a -> a.getName().equals("Academy A")));
        assertTrue(academies.stream().anyMatch(a -> a.getName().equals("Academy B")));
        assertTrue(academies.stream().anyMatch(a -> a.getName().equals("Academy C")));
    }

    @Test
    void testNewUserAcademiesGetUniqueIds() {
        // Load base academies with IDs 0 and 1
        String baseXml = createAcademyXml("Academy A", "Academy B");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(baseXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        // Load user academies with a new academy
        String userXml = createAcademyXml("Academy C");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(userXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        List<Academy> academies = factory.getAllAcademiesForSet("TestSet");
        assertEquals(3, academies.size());

        // New academy should get ID 2 (after max existing ID of 1)
        Academy academyC = academies.stream()
            .filter(a -> a.getName().equals("Academy C"))
            .findFirst().orElse(null);
        assertNotNull(academyC);
        assertEquals(2, academyC.getId(), "New academy should get ID after max existing ID");
    }

    @Test
    void testMixedOverrideAndNewAcademies() {
        // Load base academies: A(0), B(1), C(2)
        String baseXml = createAcademyXml("Academy A", "Academy B", "Academy C");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(baseXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        // User file: Override B, add D and E
        String userXml = createAcademyXml("Academy B", "Academy D", "Academy E");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(userXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        List<Academy> academies = factory.getAllAcademiesForSet("TestSet");

        // Should have 5 academies total
        assertEquals(5, academies.size());

        // Check IDs
        Map<String, Integer> idMap = new java.util.HashMap<>();
        for (Academy academy : academies) {
            idMap.put(academy.getName(), academy.getId());
        }

        assertEquals(0, idMap.get("Academy A"), "Academy A should keep ID 0");
        assertEquals(1, idMap.get("Academy B"), "Academy B (overridden) should keep ID 1");
        assertEquals(2, idMap.get("Academy C"), "Academy C should keep ID 2");
        assertEquals(3, idMap.get("Academy D"), "Academy D (new) should get ID 3");
        assertEquals(4, idMap.get("Academy E"), "Academy E (new) should get ID 4");
    }

    @Test
    void testNoIdConflictsAfterMultipleLoads() {
        // Load base academies
        String baseXml = createAcademyXml("Academy A", "Academy B");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(baseXml.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        // First user load: override A, add C
        String userXml1 = createAcademyXml("Academy A", "Academy C");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(userXml1.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        // Second user load: add D
        String userXml2 = createAcademyXml("Academy D");
        factory.loadAcademyFromStream(
            new ByteArrayInputStream(userXml2.getBytes(StandardCharsets.UTF_8)),
            "TestSet.xml");

        List<Academy> academies = factory.getAllAcademiesForSet("TestSet");

        // Check all IDs are unique
        List<Integer> ids = academies.stream()
            .map(Academy::getId)
            .sorted()
            .toList();

        for (int i = 0; i < ids.size() - 1; i++) {
            assertTrue(ids.get(i) < ids.get(i + 1),
                "IDs should be unique, found duplicate or out of order: " + ids);
        }
    }
}
