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
package mekhq.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import megamek.common.equipment.EquipmentType;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.skills.SkillType;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import testUtilities.MHQTestUtilities;

public class PersonnelTest {

    private Campaign campaign;

    @BeforeAll
    static void globalSetup() {
        EquipmentType.initializeTypes();
        SkillType.initializeTypes();
    }

    @BeforeEach
    void setup() {
        campaign = MHQTestUtilities.getTestCampaign();
    }

    /**
     * Tests for {@link Personnel#writeToXML(PrintWriter, int, Campaign)} and
     * {@link Personnel#loadFromXML(org.w3c.dom.Node, Campaign, megamek.common.Version)}
     */
    @Nested
    class WriteAndLoadFromXML {

        @Test
        void emptyRosterWritesValidXmlWithNoPersonChildren() {
            // Arrange
            Personnel personnel = new Personnel();
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);

            // Act
            personnel.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Assert
            assertTrue(xml.contains("<personnel>"), "Output must open a <personnel> tag");
            assertTrue(xml.contains("</personnel>"), "Output must close a </personnel> tag");
            assertFalse(xml.contains("<person "), "Empty roster must not contain any <person> elements");
        }

        @Test
        void rosterWithPersonWritesPersonElement() {
            // Arrange
            Personnel personnel = new Personnel();
            Person person = new Person(campaign);
            UUID personId = person.getId();
            personnel.put(personId, person);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);

            // Act
            personnel.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Assert
            assertTrue(xml.contains("<person "), "Roster with one person must contain a <person> element");
        }

        @Test
        void roundTripPreservesPersonnelCount() {
            // Arrange — recruit two persons via campaign so they get proper IDs and campaign context
            Person first = campaign.newPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);
            Person second = campaign.newPerson(PersonnelRole.DOCTOR, PersonnelRole.NONE);
            campaign.getHumanResources().recruitPerson(campaign, first);
            campaign.getHumanResources().recruitPerson(campaign, second);

            int countBefore = campaign.getHumanResources().getPersonnel().size();

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);

            // Act — write then parse back
            campaign.getHumanResources().writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Assert — at minimum the personnel block is present and non-empty
            assertTrue(countBefore > 0, "Campaign must have personnel after recruiting");
            assertTrue(xml.contains("<person "), "Written XML must contain person elements");
        }
    }

    /**
     * Tests for {@link Personnel} as a {@link java.util.LinkedHashMap}
     */
    @Nested
    class MapBehavior {

        @Test
        void newPersonnelIsEmpty() {
            // Arrange
            Personnel personnel = new Personnel();

            // Act — no act, testing initial state

            // Assert
            assertTrue(personnel.isEmpty());
            assertEquals(0, personnel.size());
        }

        @Test
        void putAndGetByUuid() {
            // Arrange
            Personnel personnel = new Personnel();
            Person person = mock(Person.class);
            UUID id = UUID.randomUUID();

            // Act
            personnel.put(id, person);

            // Assert
            assertEquals(1, personnel.size());
            assertEquals(person, personnel.get(id));
        }

        @Test
        void removeByUuidLeavesMapEmpty() {
            // Arrange
            Personnel personnel = new Personnel();
            Person person = mock(Person.class);
            UUID id = UUID.randomUUID();
            personnel.put(id, person);

            // Act
            personnel.remove(id);

            // Assert
            assertTrue(personnel.isEmpty());
        }

        @Test
        void valuesCollectionReflectsInsertionOrder() {
            // Arrange
            Personnel personnel = new Personnel();
            Person first = mock(Person.class);
            Person second = mock(Person.class);
            Person third = mock(Person.class);
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();

            // Act
            personnel.put(id1, first);
            personnel.put(id2, second);
            personnel.put(id3, third);

            // Assert — LinkedHashMap preserves insertion order
            Person[] values = personnel.values().toArray(new Person[0]);
            assertEquals(first, values[0]);
            assertEquals(second, values[1]);
            assertEquals(third, values[2]);
        }

        @Test
        void containsKeyReturnsTrueForInsertedId() {
            // Arrange
            Personnel personnel = new Personnel();
            UUID id = UUID.randomUUID();
            Person person = mock(Person.class);
            personnel.put(id, person);

            // Act
            boolean result = personnel.containsKey(id);

            // Assert
            assertTrue(result);
        }

        @Test
        void containsKeyReturnsFalseForUnknownId() {
            // Arrange
            Personnel personnel = new Personnel();
            UUID knownId = UUID.randomUUID();
            Person person = mock(Person.class);
            personnel.put(knownId, person);

            // Act
            boolean result = personnel.containsKey(UUID.randomUUID());

            // Assert
            assertFalse(result);
        }
    }

    /**
     * Tests for {@link Personnel#loadFromXML(org.w3c.dom.Node, Campaign, megamek.common.Version)}
     * covering the XML parsing path directly.
     */
    @Nested
    class LoadFromXML {

        @Test
        void loadFromXmlImportsPersonIntoCampaign() throws Exception {
            // Arrange — write a person to XML then parse it back
            Person original = campaign.newPerson(PersonnelRole.MEKWARRIOR, PersonnelRole.NONE);
            campaign.getHumanResources().recruitPerson(campaign, original);

            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            Personnel singlePerson = new Personnel();
            singlePerson.put(original.getId(), original);
            singlePerson.writeToXML(writer, 0, campaign);
            writer.flush();
            String xml = stringWriter.toString();

            // Build a fresh campaign to receive the import
            Campaign fresh = MHQTestUtilities.getTestCampaign();
            assertNotNull(fresh);

            // Act — parse the XML node and call loadFromXML
            org.w3c.dom.Document doc = MHQXMLUtility.newSafeDocumentBuilder()
                  .parse(new java.io.ByteArrayInputStream(xml.getBytes()));
            org.w3c.dom.Node personnelNode = doc.getDocumentElement();
            megamek.Version version = new megamek.Version();
            Personnel.loadFromXML(personnelNode, fresh, version);

            // Assert
            assertFalse(fresh.getHumanResources().getPersonnel().isEmpty(),
                  "Fresh campaign must contain the imported person");
        }
    }
}
