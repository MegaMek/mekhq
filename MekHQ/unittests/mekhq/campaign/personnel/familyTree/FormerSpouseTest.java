/*
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ. If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.familyTree;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.io.idReferenceClasses.PersonIdReference;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class FormerSpouseTest {
    @Mock
    private Campaign mockCampaign;

    //region File I/O
    @Test
    public void testWriteToXML() throws IOException {
        final UUID id = UUID.randomUUID();

        final Person mockPerson = mock(Person.class);
        when(mockPerson.getId()).thenReturn(id);

        final LocalDate today = LocalDate.of(3025, 1, 1);

        final FormerSpouse formerSpouse = new FormerSpouse(mockPerson, today, FormerSpouseReason.DIVORCE);
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            formerSpouse.writeToXML(pw, 0);

            // Assert the written XML equals to the expected text, ignoring line ending differences
            assertEquals(String.format("<formerSpouse>\t<id>%s</id>\t<date>3025-01-01</date>\t<reason>DIVORCE</reason></formerSpouse>", id),
                    sw.toString().replaceAll("\\n|\\r\\n", ""));
        }
    }

    @Test
    public void testGenerateInstanceFromXML() throws Exception {
        final UUID id = UUID.randomUUID();

        final String text = String.format("<formerSpouse>\n\t<id>%s</id>\n\t<date>3025-01-01</date>\n\t<reason>DIVORCE</reason>\n</formerSpouse>\n", id);

        final Document document;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            document = MHQXMLUtility.newSafeDocumentBuilder().parse(bais);
        }

        final Element element = document.getDocumentElement();
        element.normalize();

        assertTrue(element.hasChildNodes());
        final FormerSpouse formerSpouse = FormerSpouse.generateInstanceFromXML(element);
        assertTrue(formerSpouse.getFormerSpouse() instanceof PersonIdReference);
        assertEquals(id, formerSpouse.getFormerSpouse().getId());
        assertEquals(LocalDate.of(3025, 1, 1), formerSpouse.getDate());
        assertEquals(FormerSpouseReason.DIVORCE, formerSpouse.getReason());
    }
    //endregion File I/O

    @Test
    public void testToString() {
        final Person mockPerson = mock(Person.class);
        when(mockPerson.getFullTitle()).thenReturn("Lee Jae-dong");

        final LocalDate date = LocalDate.of(3025, 1, 1);

        final FormerSpouse formerSpouse = new FormerSpouse(mockPerson, date, FormerSpouseReason.DIVORCE);

        final String expected = String.format("%s: Lee Jae-dong (%s)", FormerSpouseReason.DIVORCE,
                MekHQ.getMHQOptions().getDisplayFormattedDate(date));
        assertEquals(expected, formerSpouse.toString());
    }

    @Test
    public void testEqualsAndHashCode() {
        final Person mockPerson1 = mock(Person.class);
        when(mockPerson1.getId()).thenReturn(UUID.randomUUID());

        final Person mockPerson2 = mock(Person.class);
        when(mockPerson2.getId()).thenReturn(UUID.randomUUID());

        final LocalDate today = LocalDate.of(3025, 1, 1);
        final LocalDate tomorrow = LocalDate.of(3025, 1, 2);
        final FormerSpouse formerSpouse1 = new FormerSpouse(mockPerson1, today, FormerSpouseReason.DIVORCE);
        final FormerSpouse formerSpouse2 = new FormerSpouse(mockPerson2, today, FormerSpouseReason.DIVORCE);
        final FormerSpouse formerSpouse3 = new FormerSpouse(mockPerson1, tomorrow, FormerSpouseReason.DIVORCE);
        final FormerSpouse formerSpouse4 = new FormerSpouse(mockPerson1, today, FormerSpouseReason.WIDOWED);
        final FormerSpouse formerSpouse5 = new FormerSpouse(mockPerson1, today, FormerSpouseReason.DIVORCE);

        // Testing Equals
        assertEquals(formerSpouse1, formerSpouse1);
        assertNotEquals(formerSpouse1, mockPerson1);
        assertNotEquals(formerSpouse1, formerSpouse2);
        assertNotEquals(formerSpouse1, formerSpouse3);
        assertNotEquals(formerSpouse1, formerSpouse4);
        assertEquals(formerSpouse1, formerSpouse5);

        // Testing HashCode
        assertEquals(formerSpouse1.hashCode(), formerSpouse1.hashCode());
        assertNotEquals(formerSpouse1.hashCode(), formerSpouse2.hashCode());
        assertNotEquals(formerSpouse1.hashCode(), formerSpouse3.hashCode());
        assertNotEquals(formerSpouse1.hashCode(), formerSpouse4.hashCode());
        assertEquals(formerSpouse1.hashCode(), formerSpouse5.hashCode());
    }
}
