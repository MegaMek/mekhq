/*
 * Copyright (c) 2020 - The MegaMek Team. All Rights Reserved.
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
import mekhq.campaign.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GenealogyTest {
    private Campaign mockCampaign;
    private Person alpha;
    private Person beta;
    private Person gamma;
    private Person delta;
    private Person epsilon;
    private Person zeta;
    private Person eta;
    private Person theta;
    private Person iota;
    private Person kappa;
    private Person lambda;
    private Person mu;
    private Person nu;
    private Person xi;
    private Person omicron;
    private Person pi;
    private Person rho;
    private Person sigma;
    private Person tau;

    @Test
    public void testOrigin() {
        assertEquals(alpha, alpha.getGenealogy().getOrigin());
    }

    //region Boolean Checks
    @Test
    public void testHasAnyFamily() {
        assertTrue(alpha.getGenealogy().hasAnyFamily());
        assertTrue(xi.getGenealogy().hasAnyFamily());
        assertFalse(lambda.getGenealogy().hasAnyFamily());
    }

    @Test
    public void testHasSpouse() {
        assertTrue(xi.getGenealogy().hasSpouse());
        assertFalse(sigma.getGenealogy().hasSpouse());
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
        // Setup the options
        CampaignOptions mockCampaignOptions = mock(CampaignOptions.class);
        when(mockCampaign.getCampaignOptions()).thenReturn(mockCampaignOptions);
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(4);

        // Same person test
        assertTrue(alpha.getGenealogy().checkMutualAncestors(alpha, mockCampaign));

        // Option disabled test
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(0);
        assertFalse(alpha.getGenealogy().checkMutualAncestors(beta, mockCampaign));

        // No relationship Test
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(4);
        assertFalse(alpha.getGenealogy().checkMutualAncestors(lambda, mockCampaign));
        assertFalse(lambda.getGenealogy().checkMutualAncestors(alpha, mockCampaign));

        // One level of Ancestry Testing
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(1);
        assertTrue(alpha.getGenealogy().checkMutualAncestors(beta, mockCampaign));
        assertTrue(beta.getGenealogy().checkMutualAncestors(alpha, mockCampaign));
        assertFalse(gamma.getGenealogy().checkMutualAncestors(zeta, mockCampaign));
        assertFalse(kappa.getGenealogy().checkMutualAncestors(theta, mockCampaign));

        // Two levels of Ancestry Testing
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(2);
        assertTrue(delta.getGenealogy().checkMutualAncestors(iota, mockCampaign));
        assertTrue(iota.getGenealogy().checkMutualAncestors(delta, mockCampaign));
        assertTrue(iota.getGenealogy().checkMutualAncestors(kappa, mockCampaign));
        assertFalse(delta.getGenealogy().checkMutualAncestors(kappa, mockCampaign));

        // Three levels of Ancestry Testing
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(3);
        assertTrue(delta.getGenealogy().checkMutualAncestors(zeta, mockCampaign));
        assertTrue(delta.getGenealogy().checkMutualAncestors(kappa, mockCampaign));
        assertFalse(delta.getGenealogy().checkMutualAncestors(theta, mockCampaign));
        assertFalse(mu.getGenealogy().checkMutualAncestors(kappa, mockCampaign));

        // Four levels of Ancestry Testing
        when(mockCampaignOptions.checkMutualAncestorsDepth()).thenReturn(4);
        assertTrue(mu.getGenealogy().checkMutualAncestors(kappa, mockCampaign));
        assertTrue(mu.getGenealogy().checkMutualAncestors(eta, mockCampaign));
        assertFalse(delta.getGenealogy().checkMutualAncestors(epsilon, mockCampaign));
        assertTrue(nu.getGenealogy().checkMutualAncestors(alpha, mockCampaign));
        assertFalse(nu.getGenealogy().checkMutualAncestors(eta, mockCampaign));
        assertFalse(nu.getGenealogy().checkMutualAncestors(zeta, mockCampaign));
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

    @Before
    public void createFamilyTree() {
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
        // Alpha was married to Sigma and Tau
        // Lambda is not related to anyone
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

    private ArgumentMatcher<UUID> matchPersonUUID(final UUID target) {
        return target::equals;
    }
}
