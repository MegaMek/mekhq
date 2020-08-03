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
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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

    @Test
    public void testHasAnyFamily() {
        assertTrue(alpha.getGenealogy().hasAnyFamily());
        assertTrue(xi.getGenealogy().hasAnyFamily());
        assertFalse(lambda.getGenealogy().hasAnyFamily());
    }

    @Test
    public void testHasSpouse() {
        assertTrue(xi.getGenealogy().hasSpouse());
        assertFalse(alpha.getGenealogy().hasSpouse());
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

    @Test
    public void testGetGrandparents() {
        // Testing Gamma's grandparents, which are Beta and Eta
        List<UUID> answer = new ArrayList<>();
        answer.add(beta.getId());
        answer.add(eta.getId());
        answer.sort(UUID::compareTo);

        List<UUID> grandparents = gamma.getGenealogy().getGrandparents(mockCampaign);
        grandparents.sort(UUID::compareTo);

        assertEquals(answer, grandparents);
    }

    @Test
    public void testGetParents() {
        // Testing Alpha's parents, which are Beta and Eta
        List<UUID> answer = new ArrayList<>();
        answer.add(beta.getId());
        answer.add(eta.getId());
        answer.sort(UUID::compareTo);

        List<UUID> parents = alpha.getGenealogy().getParents();
        parents.sort(UUID::compareTo);

        assertEquals(answer, parents);
    }

    @Test
    public void testGetFathers() {
        // Testing Alpha's father, which is Beta
        List<UUID> answer = new ArrayList<>();
        answer.add(beta.getId());
        answer.sort(UUID::compareTo);

        List<UUID> fathers = alpha.getGenealogy().getFathers(mockCampaign);
        fathers.sort(UUID::compareTo);

        assertEquals(answer, fathers);
    }

    @Test
    public void testGetMothers() {
        // Testing Alpha's mother, which is Eta
        List<UUID> answer = new ArrayList<>();
        answer.add(eta.getId());
        answer.sort(UUID::compareTo);

        List<UUID> mothers = alpha.getGenealogy().getMothers(mockCampaign);
        mothers.sort(UUID::compareTo);

        assertEquals(answer, mothers);
    }

    @Test
    public void testGetSiblings() {
        // Testing Omicron's siblings, which is just Nu
        List<UUID> answer = new ArrayList<>();
        answer.add(nu.getId());
        answer.sort(UUID::compareTo);

        List<UUID> siblings = omicron.getGenealogy().getSiblings(mockCampaign);
        siblings.sort(UUID::compareTo);

        assertEquals(answer, siblings);
    }

    @Test
    public void testGetSiblingsAndSpouses() {
        // Testing Omicron's siblings and their spouses, which is just Nu and Nu's spouse Xi
        List<UUID> answer = new ArrayList<>();
        answer.add(nu.getId());
        answer.add(xi.getId());
        answer.sort(UUID::compareTo);

        List<UUID> siblings = omicron.getGenealogy().getSiblingsAndSpouses(mockCampaign);
        siblings.sort(UUID::compareTo);

        assertEquals(answer, siblings);
    }

    @Test
    public void testGetChildren() {
        // Testing Alpha's children, which are Gamma and Iota
        List<UUID> answer = new ArrayList<>();
        answer.add(gamma.getId());
        answer.add(iota.getId());
        answer.sort(UUID::compareTo);

        List<UUID> children = alpha.getGenealogy().getChildren();
        children.sort(UUID::compareTo);

        assertEquals(answer, children);
    }

    @Test
    public void testGetGrandchildren() {
        // Testing Beta's grandchildren, which are Gamma and Iota
        List<UUID> answer = new ArrayList<>();
        answer.add(gamma.getId());
        answer.add(iota.getId());
        answer.sort(UUID::compareTo);

        List<UUID> grandchildren = beta.getGenealogy().getGrandchildren(mockCampaign);
        grandchildren.sort(UUID::compareTo);

        assertEquals(answer, grandchildren);
    }

    @Test
    public void testGetAuntsAndUncles() {
        // Testing Delta's Aunts and Uncles, which are Iota and Pi
        List<UUID> answer = new ArrayList<>();
        answer.add(iota.getId());
        answer.add(pi.getId());
        answer.sort(UUID::compareTo);

        List<UUID> auntsAndUncles = delta.getGenealogy().getsAuntsAndUncles(mockCampaign);
        auntsAndUncles.sort(UUID::compareTo);

        assertEquals(answer, auntsAndUncles);
    }

    @Test
    public void testGetCousins() {
        // Testing Kappa's cousins, which are Gamma and Iota
        List<UUID> answer = new ArrayList<>();
        answer.add(gamma.getId());
        answer.add(iota.getId());
        answer.sort(UUID::compareTo);

        List<UUID> cousins = kappa.getGenealogy().getCousins(mockCampaign);
        cousins.sort(UUID::compareTo);

        assertEquals(answer, cousins);
    }

    @Before
    public void createFamilyTree() {
        mockCampaign = mock(Campaign.class);

        // Create a bunch of people
        alpha = spy(new Person("Alpha", "Alpha", mockCampaign, "MERC"));
        beta = spy(new Person("Beta", "Beta", mockCampaign, "MERC"));
        gamma = spy(new Person("Gamma", "Gamma", mockCampaign, "MERC"));
        delta = spy(new Person("Delta", "Delta", mockCampaign, "MERC"));
        epsilon = spy(new Person("Epsilon", "Epsilon", mockCampaign, "MERC"));
        zeta = spy(new Person("Zeta", "Zeta", mockCampaign, "MERC"));
        eta = spy(new Person("Eta", "Eta", mockCampaign, "MERC"));
        theta = spy(new Person("Theta", "Theta", mockCampaign, "MERC"));
        iota = spy(new Person("Iota", "Iota", mockCampaign, "MERC"));
        kappa = spy(new Person("Kappa", "Kappa", mockCampaign, "MERC"));
        lambda = spy(new Person("Lambda", "Lambda", mockCampaign, "MERC"));
        mu = spy(new Person("Mu", "Mu", mockCampaign, "MERC"));
        nu = spy(new Person("Nu", "Nu", mockCampaign, "MERC"));
        xi = spy(new Person("Xi", "Xi", mockCampaign, "MERC"));
        omicron = spy(new Person("Omicron", "Omicron", mockCampaign, "MERC"));
        pi = spy(new Person("Pi", "Pi", mockCampaign, "MERC"));

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

        // Setup Gender (where needed)
        beta.setGender(Gender.MALE);
        eta.setGender(Gender.FEMALE);

        // Create Family Tree:
        // Beta, Eta and Epsilon are the topmost nodes
        // Alpha is Beta and Eta's child, Zeta is Eta and Epsilon's child, and Theta is Epsilon's child
        // Gamma and Iota are the children of Alpha, Kappa is Zeta's child
        // Delta is the child of Gamma
        // Mu is the child of Delta
        // Nu and Omicron are the children of Mu
        // Xi is married to Nu, Pi is married to Iota
        // Lambda is not related to anyone
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, beta.getId());
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, eta.getId());
        beta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, alpha.getId());
        eta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, alpha.getId());

        zeta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, eta.getId());
        zeta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, epsilon.getId());
        eta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, zeta.getId());
        epsilon.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, zeta.getId());

        theta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, epsilon.getId());
        epsilon.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, theta.getId());

        gamma.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, alpha.getId());
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, gamma.getId());

        iota.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, alpha.getId());
        alpha.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, iota.getId());

        kappa.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, zeta.getId());
        zeta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, kappa.getId());

        delta.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, gamma.getId());
        gamma.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, delta.getId());

        mu.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, delta.getId());
        delta.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, mu.getId());

        nu.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, mu.getId());
        mu.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, nu.getId());

        omicron.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, mu.getId());
        mu.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, omicron.getId());

        xi.getGenealogy().setSpouse(nu.getId());
        nu.getGenealogy().setSpouse(xi.getId());

        pi.getGenealogy().setSpouse(iota.getId());
        iota.getGenealogy().setSpouse(pi.getId());
    }

    private ArgumentMatcher<UUID> matchPersonUUID(final UUID target) {
        return target::equals;
    }
}
