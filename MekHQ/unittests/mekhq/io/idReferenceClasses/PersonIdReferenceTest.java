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
package mekhq.io.idReferenceClasses;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import mekhq.campaign.personnel.familyTree.Genealogy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static mekhq.campaign.personnel.PersonnelTestUtilities.matchPersonUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(value = MockitoExtension.class)
public class PersonIdReferenceTest {
    @Mock
    private Campaign mockCampaign;

    @Test
    public void testFixPersonIdReferences() {
        // This is a smoke test to ensure we don't have an obvious ConMod. Deeper tests to fix each
        // reference type are separated below.
        final List<Person> personnel = IntStream.range(0, 100)
                .mapToObj(i -> new Person(mockCampaign, "MERC"))
                .collect(Collectors.toList());
        when(mockCampaign.getPersonnel()).thenReturn(personnel);
        PersonIdReference.fixPersonIdReferences(mockCampaign);
    }

    @Test
    public void testFixGenealogyReferencesEmptyGenealogy() {
        final Person origin = new Person(mockCampaign, "MERC");
        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertTrue(origin.getGenealogy().isEmpty());
    }

    @Test
    public void testFixGenealogyReferencesSpouseOnly() {
        final Person spouse = new Person(mockCampaign, "MERC");

        final Person origin = mock(Person.class);
        when(origin.getFullTitle()).thenReturn("Origin");

        final Genealogy genealogy = new Genealogy(origin);
        genealogy.setSpouse(new PersonIdReference(spouse.getId().toString()));
        when(origin.getGenealogy()).thenReturn(genealogy);

        // Testing Unknown Spouse
        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertFalse(origin.getGenealogy().hasSpouse());

        // Testing Known Spouse
        genealogy.setSpouse(new PersonIdReference(spouse.getId().toString()));
        given(mockCampaign.getPerson(argThat(matchPersonUUID(spouse.getId())))).willReturn(spouse);

        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertEquals(spouse, origin.getGenealogy().getSpouse());
    }

    @Test
    public void testFixGenealogyReferencesFormerSpousesOnly() {
        final Person origin = mock(Person.class);
        final Person formerSpouse = new Person(mockCampaign, "MERC");

        final Genealogy genealogy = new Genealogy(origin);
        genealogy.addFormerSpouse(new FormerSpouse(new PersonIdReference(formerSpouse.getId().toString()),
                LocalDate.now(), FormerSpouseReason.DIVORCE));
        when(origin.getGenealogy()).thenReturn(genealogy);

        // Testing Unknown Former Spouse
        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertFalse(origin.getGenealogy().hasFormerSpouse());

        // Testing Known Former Spouse
        genealogy.addFormerSpouse(new FormerSpouse(new PersonIdReference(formerSpouse.getId().toString()),
                LocalDate.now(), FormerSpouseReason.DIVORCE));
        given(mockCampaign.getPerson(argThat(matchPersonUUID(formerSpouse.getId())))).willReturn(formerSpouse);

        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertEquals(1, origin.getGenealogy().getFormerSpouses().size());
        assertEquals(formerSpouse, origin.getGenealogy().getFormerSpouses().get(0).getFormerSpouse());

        // Testing Known But Migrated Former Spouse
        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertEquals(1, origin.getGenealogy().getFormerSpouses().size());
        assertEquals(formerSpouse, origin.getGenealogy().getFormerSpouses().get(0).getFormerSpouse());
    }

    @Test
    public void testFixGenealogyReferencesFamilyOnly() {
        // This tests each case bar null together, with an unknown Child, a known Parent, and an
        // already migrated Parent
        final Person origin = new Person(mockCampaign, "MERC");
        final Person child = new Person(mockCampaign, "MERC");
        final Person parent1 = new Person(mockCampaign, "MERC");
        final Person parent2 = new Person(mockCampaign, "MERC");

        origin.getGenealogy().getFamily().put(FamilialRelationshipType.CHILD, new ArrayList<>());
        origin.getGenealogy().getFamily().get(FamilialRelationshipType.CHILD).add(new PersonIdReference(child.getId().toString()));
        origin.getGenealogy().getFamily().put(FamilialRelationshipType.PARENT, new ArrayList<>());
        origin.getGenealogy().getFamily().get(FamilialRelationshipType.PARENT).add(parent1);
        origin.getGenealogy().getFamily().get(FamilialRelationshipType.PARENT).add(new PersonIdReference(parent2.getId().toString()));

        doReturn(null).when(mockCampaign).getPerson(argThat(matchPersonUUID(child.getId())));
        doReturn(parent1).when(mockCampaign).getPerson(argThat(matchPersonUUID(parent1.getId())));
        doReturn(parent2).when(mockCampaign).getPerson(argThat(matchPersonUUID(parent2.getId())));

        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertTrue(origin.getGenealogy().getFamily().containsKey(FamilialRelationshipType.PARENT));
        assertEquals(1, origin.getGenealogy().getFamily().size());
        assertEquals(2, origin.getGenealogy().getFamily().get(FamilialRelationshipType.PARENT).size());
        assertEquals(parent1, origin.getGenealogy().getFamily().get(FamilialRelationshipType.PARENT).get(0));
        assertEquals(parent2, origin.getGenealogy().getFamily().get(FamilialRelationshipType.PARENT).get(1));
    }

    @Test
    public void testFixGenealogyReferencesNullFamilyOnly() {
        final Person origin = new Person(mockCampaign, "MERC");
        origin.getGenealogy().getFamily().put(FamilialRelationshipType.PARENT, new ArrayList<>());
        origin.getGenealogy().getFamily().get(FamilialRelationshipType.PARENT).add(null);

        PersonIdReference.fixGenealogyReferences(mockCampaign, origin);
        assertFalse(origin.getGenealogy().getFamily().containsKey(FamilialRelationshipType.PARENT));
        assertEquals(0, origin.getGenealogy().getFamily().size());
    }
}
