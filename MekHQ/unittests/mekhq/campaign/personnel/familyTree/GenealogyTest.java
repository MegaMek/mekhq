/*
 * Copyright (c) 2020-2022 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.utilities.MHQXMLUtility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static mekhq.campaign.personnel.PersonnelTestUtilities.matchPersonUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenealogyTest {
    private static Campaign mockCampaign;
    private static Person alpha;
    private static Person beta;
    private static Person gamma;
    private static Person delta;
    private static Person epsilon;
    private static Person zeta;
    private static Person eta;
    private static Person theta;
    private static Person iota;
    private static Person kappa;
    private static Person lambda;
    private static Person mu;
    private static Person nu;
    private static Person xi;
    private static Person omicron;
    private static Person pi;
    private static Person rho;
    private static Person sigma;
    private static Person tau;

    @BeforeAll
    public static void createFamilyTree() {
        mockCampaign = mock(Campaign.class);

        // Create a bunch of people
        alpha = new Person("Alpha", "Alpha", mockCampaign, "MERC");
        beta = new Person("Beta", "Beta", mockCampaign, "MERC");
        gamma = new Person("Gamma", "Gamma", mockCampaign, "MERC");
        delta = new Person("Delta", "Delta", mockCampaign, "MERC");
        epsilon = new Person("Epsilon", "Epsilon", mockCampaign, "MERC");
        zeta = new Person("Zeta", "Zeta", mockCampaign, "MERC");
        eta = new Person("Eta", "Eta", mockCampaign, "MERC");
        theta = new Person("Theta", "Theta", mockCampaign, "MERC");
        iota = new Person("Iota", "Iota", mockCampaign, "MERC");
        kappa = new Person("Kappa", "Kappa", mockCampaign, "MERC");
        lambda = new Person("Lambda", "Lambda", mockCampaign, "MERC");
        mu = new Person("Mu", "Mu", mockCampaign, "MERC");
        nu = new Person("Nu", "Nu", mockCampaign, "MERC");
        xi = new Person("Xi", "Xi", mockCampaign, "MERC");
        omicron = new Person("Omicron", "Omicron", mockCampaign, "MERC");
        pi = new Person("Pi", "Pi", mockCampaign, "MERC");
        rho = new Person("Rho", "Rho", mockCampaign, "MERC");
        sigma = new Person("Sigma", "Sigma", mockCampaign, "MERC");
        tau = new Person("Tau", "Tau", mockCampaign, "MERC");

        // Setup the getPerson methods
        given(mockCampaign.getPerson(argThat(matchPersonUUID(alpha.getId())))).willReturn(alpha);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(beta.getId())))).willReturn(beta);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(gamma.getId())))).willReturn(gamma);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(delta.getId())))).willReturn(delta);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(epsilon.getId())))).willReturn(epsilon);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(zeta.getId())))).willReturn(zeta);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(eta.getId())))).willReturn(eta);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(theta.getId())))).willReturn(theta);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(iota.getId())))).willReturn(iota);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(kappa.getId())))).willReturn(kappa);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(lambda.getId())))).willReturn(lambda);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(mu.getId())))).willReturn(mu);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(nu.getId())))).willReturn(nu);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(xi.getId())))).willReturn(xi);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(omicron.getId())))).willReturn(omicron);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(pi.getId())))).willReturn(pi);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(rho.getId())))).willReturn(rho);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(sigma.getId())))).willReturn(sigma);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(tau.getId())))).willReturn(tau);

        // Setup Gender (where needed)
        alpha.setGender(Gender.FEMALE);
        beta.setGender(Gender.MALE);
        eta.setGender(Gender.FEMALE);
        rho.setGender(Gender.MALE);
        sigma.setGender(Gender.FEMALE);
        tau.setGender(Gender.MALE);

        // Create Family Tree:
        // Beta, Eta and Epsilon are the topmost nodes
        // Alpha is Beta and Eta's child, Zeta is Eta and Epsilon's child, and Theta is Epsilon's child
        // Gamma and Iota are the children of Alpha, Kappa is Zeta's child
        // Delta is the child of Gamma
        // Mu is the child of Delta
        // Nu and Omicron are the children of Mu
        // Xi is married to Nu, Pi is married to Iota, Alpha is married to Rho
        // Lambda was married to Sigma and Tau
        // Lambda is no longer related to anyone
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, beta);
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, eta);
        beta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, alpha);
        eta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, alpha);

        zeta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, eta);
        zeta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, epsilon);
        eta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, zeta);
        epsilon.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, zeta);

        theta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, epsilon);
        epsilon.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, theta);

        gamma.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, alpha);
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, gamma);

        iota.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, alpha);
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, iota);

        kappa.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, zeta);
        zeta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, kappa);

        delta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, gamma);
        gamma.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, delta);

        mu.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, delta);
        delta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, mu);

        nu.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, mu);
        mu.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, nu);

        omicron.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, mu);
        mu.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, omicron);

        alpha.getGenealogy().setSpouse(rho);
        rho.getGenealogy().setSpouse(alpha);

        xi.getGenealogy().setSpouse(nu);
        nu.getGenealogy().setSpouse(xi);

        pi.getGenealogy().setSpouse(iota);
        iota.getGenealogy().setSpouse(pi);

        lambda.getGenealogy().addFormerSpouse(new FormerSpouse(sigma,
                LocalDate.ofYearDay(3025, 1), FormerSpouseReason.DIVORCE));
        sigma.getGenealogy().addFormerSpouse(new FormerSpouse(lambda,
                LocalDate.ofYearDay(3025, 1), FormerSpouseReason.DIVORCE));

        lambda.getGenealogy().addFormerSpouse(new FormerSpouse(tau,
                LocalDate.ofYearDay(3026, 1), FormerSpouseReason.WIDOWED));
        tau.getGenealogy().addFormerSpouse(new FormerSpouse(lambda,
                LocalDate.ofYearDay(3026, 1), FormerSpouseReason.WIDOWED));
    }

    //region Getters/Setters
    @Test
    public void testOrigin() {
        assertEquals(alpha, alpha.getGenealogy().getOrigin());
    }

    @Test
    public void testAddAndRemoveFormerSpouse() {
        final FormerSpouse formerSpouse1 = new FormerSpouse(new Person(mockCampaign, "MERC"),
                LocalDate.now(), FormerSpouseReason.DIVORCE);
        final FormerSpouse formerSpouse2 = new FormerSpouse(new Person(mockCampaign, "MERC"),
                LocalDate.now(), FormerSpouseReason.DIVORCE);

        final Person person = new Person(mockCampaign, "MERC");
        person.getGenealogy().addFormerSpouse(formerSpouse1);
        person.getGenealogy().addFormerSpouse(formerSpouse2);
        assertEquals(2, person.getGenealogy().getFormerSpouses().size());

        person.getGenealogy().removeFormerSpouse(formerSpouse1);
        assertEquals(1, person.getGenealogy().getFormerSpouses().size());

        person.getGenealogy().removeFormerSpouse(formerSpouse2.getFormerSpouse());
        assertTrue(person.getGenealogy().getFormerSpouses().isEmpty());
    }

    @Test
    public void testAddFamilyMember() {
        final Person person = new Person(mockCampaign, "MERC");
        person.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, null);
        assertTrue(person.getGenealogy().getFamily().getOrDefault(FamilialRelationshipType.CHILD, new ArrayList<>()).isEmpty());

        person.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, new Person(mockCampaign, "MERC"));
        assertEquals(1, person.getGenealogy().getParents().size());
    }

    @Test
    public void testRemoveFamilyMemberNullRelationshipType() {
        final Person parent = new Person(mockCampaign, "MERC");
        final Person origin = new Person(mockCampaign, "MERC");
        final Person child1 = new Person(mockCampaign, "MERC");
        final Person child2 = new Person(mockCampaign, "MERC");

        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, parent);
        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child1);
        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child2);

        origin.getGenealogy().removeFamilyMember(null, child1);
        assertEquals(2, origin.getGenealogy().getFamily().size());
        assertEquals(1, origin.getGenealogy().getChildren().size());

        origin.getGenealogy().removeFamilyMember(null, child2);
        assertEquals(1, origin.getGenealogy().getFamily().size());
        assertTrue(origin.getGenealogy().getChildren().isEmpty());

        origin.getGenealogy().removeFamilyMember(null, parent);
        assertTrue(origin.getGenealogy().getFamily().isEmpty());
        assertTrue(origin.getGenealogy().getParents().isEmpty());
    }

    @Test
    public void testRemoveFamilyMemberUnknownRelationshipType() {
        final Person mockOrigin = mock(Person.class);
        final Genealogy genealogy = new Genealogy(mockOrigin);
        when(mockOrigin.getGenealogy()).thenReturn(genealogy);
        when(mockOrigin.getFullTitle()).thenReturn("Origin");

        final Person mockChild = mock(Person.class);
        when(mockChild.getId()).thenReturn(UUID.randomUUID());
        when(mockChild.getFullTitle()).thenReturn("Child");
        mockOrigin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, mockChild);

        // Will write an error to log, and otherwise just ignore the ask
        mockOrigin.getGenealogy().removeFamilyMember(FamilialRelationshipType.PARENT, mockChild);
        assertEquals(1, mockOrigin.getGenealogy().getFamily().size());
        assertEquals(1, mockOrigin.getGenealogy().getChildren().size());
    }

    @Test
    public void testRemoveFamilyMemberCorrectRelationshipType() {
        final Person origin = new Person(mockCampaign, "MERC");
        final Person child1 = new Person(mockCampaign, "MERC");
        final Person child2 = new Person(mockCampaign, "MERC");

        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child1);
        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child2);

        origin.getGenealogy().removeFamilyMember(FamilialRelationshipType.CHILD, child1);
        assertEquals(1, origin.getGenealogy().getFamily().size());
        assertEquals(1, origin.getGenealogy().getChildren().size());

        origin.getGenealogy().removeFamilyMember(FamilialRelationshipType.CHILD, child2);
        assertTrue(origin.getGenealogy().getFamily().isEmpty());
        assertTrue(origin.getGenealogy().getChildren().isEmpty());
    }
    //endregion Getters/Setters

    //region Boolean Checks
    @Test
    public void testIsEmpty() {
        assertTrue(new Person(mockCampaign, "MERC").getGenealogy().isEmpty());
        assertFalse(alpha.getGenealogy().isEmpty());
        assertFalse(mu.getGenealogy().isEmpty());
        assertFalse(lambda.getGenealogy().isEmpty());
    }

    @Test
    public void testFamilyIsEmpty() {
        assertFalse(alpha.getGenealogy().familyIsEmpty());
        assertFalse(mu.getGenealogy().familyIsEmpty());
        assertTrue(lambda.getGenealogy().familyIsEmpty());
    }

    @Test
    public void testHasSpouse() {
        assertTrue(xi.getGenealogy().hasSpouse());
        assertFalse(sigma.getGenealogy().hasSpouse());
    }

    @Test
    public void testHasFormerSpouse() {
        assertTrue(lambda.getGenealogy().hasFormerSpouse());
        assertFalse(alpha.getGenealogy().hasFormerSpouse());
    }

    @Test
    public void testHasChildren() {
        assertTrue(alpha.getGenealogy().hasChildren());
        assertFalse(nu.getGenealogy().hasChildren());
    }

    @Test
    public void testHasParents() {
        assertTrue(alpha.getGenealogy().hasParents());
        assertFalse(beta.getGenealogy().hasParents());
    }

    @Test
    public void testCheckMutualAncestors() {
        // Same person test
        assertTrue(alpha.getGenealogy().checkMutualAncestors(alpha, 4));

        // Option disabled test
        assertFalse(alpha.getGenealogy().checkMutualAncestors(beta, 0));

        // No relationship Test
        assertFalse(alpha.getGenealogy().checkMutualAncestors(lambda, 4));
        assertFalse(lambda.getGenealogy().checkMutualAncestors(alpha, 4));

        // One level of Ancestry Testing
        assertTrue(alpha.getGenealogy().checkMutualAncestors(beta, 1));
        assertTrue(beta.getGenealogy().checkMutualAncestors(alpha, 1));
        assertFalse(gamma.getGenealogy().checkMutualAncestors(zeta, 1));
        assertFalse(kappa.getGenealogy().checkMutualAncestors(theta, 1));

        // Two levels of Ancestry Testing
        assertTrue(delta.getGenealogy().checkMutualAncestors(iota, 2));
        assertTrue(iota.getGenealogy().checkMutualAncestors(delta, 2));
        assertTrue(iota.getGenealogy().checkMutualAncestors(kappa, 2));
        assertFalse(delta.getGenealogy().checkMutualAncestors(kappa, 2));

        // Three levels of Ancestry Testing
        assertTrue(delta.getGenealogy().checkMutualAncestors(zeta, 3));
        assertTrue(delta.getGenealogy().checkMutualAncestors(kappa, 3));
        assertFalse(delta.getGenealogy().checkMutualAncestors(theta, 3));
        assertFalse(mu.getGenealogy().checkMutualAncestors(kappa, 3));

        // Four levels of Ancestry Testing
        assertTrue(mu.getGenealogy().checkMutualAncestors(kappa, 4));
        assertTrue(mu.getGenealogy().checkMutualAncestors(eta, 4));
        assertFalse(delta.getGenealogy().checkMutualAncestors(epsilon, 4));
        assertTrue(nu.getGenealogy().checkMutualAncestors(alpha, 4));
        assertFalse(nu.getGenealogy().checkMutualAncestors(eta, 4));
        assertFalse(nu.getGenealogy().checkMutualAncestors(zeta, 4));
    }
    //endregion Boolean Checks

    //region Basic Family Getters
    @Test
    public void testGetGrandparents() {
        // Testing Gamma's grandparents, which are Beta and Eta
        List<Person> answer = new ArrayList<>();
        answer.add(beta);
        answer.add(eta);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> grandparents = gamma.getGenealogy().getGrandparents();
        grandparents.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, grandparents);
    }

    @Test
    public void testGetParents() {
        // Testing Alpha's parents, which are Beta and Eta
        List<Person> answer = new ArrayList<>();
        answer.add(beta);
        answer.add(eta);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> parents = alpha.getGenealogy().getParents();
        parents.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, parents);
    }

    @Test
    public void testGetFathers() {
        // Testing Alpha's father, which is Beta
        List<Person> answer = new ArrayList<>();
        answer.add(beta);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> fathers = alpha.getGenealogy().getFathers();
        fathers.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, fathers);
    }

    @Test
    public void testGetMothers() {
        // Testing Alpha's mother, which is Eta
        List<Person> answer = new ArrayList<>();
        answer.add(eta);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> mothers = alpha.getGenealogy().getMothers();
        mothers.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, mothers);
    }

    @Test
    public void testGetSiblings() {
        // Testing Omicron's siblings, which is just Nu
        List<Person> answer = new ArrayList<>();
        answer.add(nu);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> siblings = omicron.getGenealogy().getSiblings();
        siblings.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, siblings);
    }

    @Test
    public void testGetSiblingsAndSpouses() {
        // Testing Omicron's siblings and their spouses, which is just Nu and Nu's spouse Xi
        List<Person> answer = new ArrayList<>();
        answer.add(nu);
        answer.add(xi);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> siblings = omicron.getGenealogy().getSiblingsAndSpouses();
        siblings.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, siblings);
    }

    @Test
    public void testGetChildren() {
        // Testing Alpha's children, which are Gamma and Iota
        List<Person> answer = new ArrayList<>();
        answer.add(gamma);
        answer.add(iota);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> children = alpha.getGenealogy().getChildren();
        children.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, children);
    }

    @Test
    public void testGetGrandchildren() {
        // Testing Beta's grandchildren, which are Gamma and Iota
        List<Person> answer = new ArrayList<>();
        answer.add(gamma);
        answer.add(iota);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> grandchildren = beta.getGenealogy().getGrandchildren();
        grandchildren.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, grandchildren);
    }

    @Test
    public void testGetAuntsAndUncles() {
        // Testing Delta's Aunts and Uncles, which are Iota and Pi
        List<Person> answer = new ArrayList<>();
        answer.add(iota);
        answer.add(pi);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> auntsAndUncles = delta.getGenealogy().getsAuntsAndUncles();
        auntsAndUncles.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, auntsAndUncles);
    }

    @Test
    public void testGetCousins() {
        // Testing Kappa's cousins, which are Gamma and Iota
        List<Person> answer = new ArrayList<>();
        answer.add(gamma);
        answer.add(iota);
        answer.sort(Comparator.comparing(Person::getId));

        List<Person> cousins = kappa.getGenealogy().getCousins();
        cousins.sort(Comparator.comparing(Person::getId));

        assertEquals(answer, cousins);
    }
    //endregion Basic Family Getters

    //region File I/O
    @Test
    public void testWriteToXML() throws IOException {
        final Genealogy genealogy = new Genealogy(mock(Person.class));
        final LocalDate today = LocalDate.of(3025, 1, 1);

        // Empty Genealogy
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            genealogy.writeToXML(pw, 0);

            // Assert the written XML equals to the expected text, ignoring line ending differences
            assertEquals("<genealogy></genealogy>", sw.toString().replaceAll("\\n|\\r\\n", ""));
        }

        // Full Genealogy
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            final UUID spouseId = UUID.randomUUID();
            final Person mockSpouse = mock(Person.class);
            when(mockSpouse.getId()).thenReturn(spouseId);
            genealogy.setSpouse(mockSpouse);

            final UUID formerSpouseId = UUID.randomUUID();
            final Person mockFormerSpouse = mock(Person.class);
            when(mockFormerSpouse.getId()).thenReturn(formerSpouseId);
            genealogy.addFormerSpouse(new FormerSpouse(mockFormerSpouse, today, FormerSpouseReason.DIVORCE));

            final UUID childId = UUID.randomUUID();
            final Person mockChild = mock(Person.class);
            when(mockChild.getId()).thenReturn(childId);
            genealogy.addFamilyMember(FamilialRelationshipType.CHILD, mockChild);

            genealogy.writeToXML(pw, 0);
            assertEquals(String.format(
                    "<genealogy>\t<spouse>%s</spouse>\t<formerSpouses>\t\t<formerSpouse>\t\t\t<id>%s</id>\t\t\t<date>3025-01-01</date>\t\t\t<reason>DIVORCE</reason>\t\t</formerSpouse>\t</formerSpouses>\t<family>\t\t<relationship>\t\t\t<type>CHILD</type>\t\t\t<personId>%s</personId>\t\t</relationship>\t</family></genealogy>",
                            spouseId, formerSpouseId, childId),
                    sw.toString().replaceAll("\\n|\\r\\n", ""));
        }
    }

    @Test
    public void testGenerateInstanceFromXML() throws Exception {
        final Person spouse = new Person(mockCampaign);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(spouse.getId())))).willReturn(spouse);
        final Person formerSpouse = new Person(mockCampaign);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(formerSpouse.getId())))).willReturn(formerSpouse);
        final Person child = new Person(mockCampaign);
        given(mockCampaign.getPerson(argThat(matchPersonUUID(child.getId())))).willReturn(child);

        final String text = String.format(
                "<genealogy>\t<spouse>%s</spouse>\t<formerSpouses>\t\t<formerSpouse>\t\t\t<id>%s</id>\t\t\t<date>3025-01-01</date>\t\t\t<reason>DIVORCE</reason>\t\t</formerSpouse>\t</formerSpouses>\t<family>\t\t<relationship>\t\t\t<type>CHILD</type>\t\t\t<personId>%s</personId>\t\t</relationship>\t</family></genealogy>",
                spouse.getId(), formerSpouse.getId(), child.getId());

        final Document document;
        try (ByteArrayInputStream bais = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            document = MHQXMLUtility.newSafeDocumentBuilder().parse(bais);
        }

        final Element element = document.getDocumentElement();
        element.normalize();

        assertTrue(element.hasChildNodes());

        final Genealogy genealogy = new Genealogy(mock(Person.class));
        genealogy.fillFromXML(element.getChildNodes());

        assertEquals(spouse.getId(), genealogy.getSpouse().getId());
        assertEquals(1, genealogy.getFormerSpouses().size());
        assertEquals(formerSpouse.getId(), genealogy.getFormerSpouses().get(0).getFormerSpouse().getId());
        assertEquals(LocalDate.of(3025, 1, 1), genealogy.getFormerSpouses().get(0).getDate());
        assertEquals(FormerSpouseReason.DIVORCE, genealogy.getFormerSpouses().get(0).getReason());
        assertEquals(1, genealogy.getFamily().size());
        assertTrue(genealogy.getFamily().containsKey(FamilialRelationshipType.CHILD));
        assertEquals(1, genealogy.getFamily().get(FamilialRelationshipType.CHILD).size());
        assertEquals(child.getId(), genealogy.getFamily().get(FamilialRelationshipType.CHILD).get(0).getId());
    }
    //endregion File I/O

    //region Clear Genealogy
    @Test
    public void clearGenealogyLinksSpouseOnly() {
        final Person origin = new Person(mockCampaign, "MERC");
        final Person spouse = new Person(mockCampaign, "MERC");

        origin.getGenealogy().setSpouse(spouse);
        spouse.getGenealogy().setSpouse(origin);

        origin.getGenealogy().clearGenealogyLinks();

        assertNull(spouse.getGenealogy().getSpouse());
    }

    @Test
    public void clearGenealogyLinksFormerSpouseOnly() {
        final Person origin = new Person(mockCampaign, "MERC");
        final Person formerSpouse = new Person(mockCampaign, "MERC");

        origin.getGenealogy().addFormerSpouse(new FormerSpouse(formerSpouse, LocalDate.now(), FormerSpouseReason.WIDOWED));
        formerSpouse.getGenealogy().addFormerSpouse(new FormerSpouse(origin, LocalDate.now(), FormerSpouseReason.WIDOWED));

        origin.getGenealogy().clearGenealogyLinks();

        assertFalse(formerSpouse.getGenealogy().hasFormerSpouse());
    }

    @Test
    public void clearGenealogyLinksFamilyOnly() {
        final Person origin = new Person(mockCampaign, "MERC");
        final Person child = new Person(mockCampaign, "MERC");

        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child);
        child.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, origin);

        origin.getGenealogy().clearGenealogyLinks();

        assertFalse(child.getGenealogy().hasParents());
    }

    @Test
    public void clearGenealogyLinksAllTypesExist() {
        final Person origin = new Person(mockCampaign, "MERC");
        final Person spouse = new Person(mockCampaign, "MERC");
        final Person formerSpouse = new Person(mockCampaign, "MERC");
        final Person child = new Person(mockCampaign, "MERC");

        origin.getGenealogy().setSpouse(spouse);
        spouse.getGenealogy().setSpouse(origin);

        origin.getGenealogy().addFormerSpouse(new FormerSpouse(formerSpouse, LocalDate.now(), FormerSpouseReason.WIDOWED));
        formerSpouse.getGenealogy().addFormerSpouse(new FormerSpouse(origin, LocalDate.now(), FormerSpouseReason.WIDOWED));

        origin.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child);
        child.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, origin);

        origin.getGenealogy().clearGenealogyLinks();

        assertNull(spouse.getGenealogy().getSpouse());
        assertFalse(formerSpouse.getGenealogy().hasFormerSpouse());
        assertFalse(child.getGenealogy().hasParents());
    }
    //endregion Clear Genealogy
}
