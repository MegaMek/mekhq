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
package mekhq.gui.dialog;

import static mekhq.campaign.universe.Faction.MERCENARY_FACTION_CODE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static testUtilities.MHQTestUtilities.mockCampaign;

import java.time.LocalDate;
import java.util.List;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.enums.FormerSpouseReason;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import mekhq.campaign.universe.Faction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FamilyTreeGraph}, the Swing-free structural model behind the family tree dialog: bloodline
 * collection, generation assignment, marriage/half-sibling grouping, birth-ordering, former spouses, ancestor couples,
 * lone parents and pedigree-collapse links.
 */
class FamilyTreeGraphTest {
    private Campaign campaign;

    @BeforeEach
    void setUp() {
        campaign = mockCampaign();
        Faction faction = mock(Faction.class);
        when(faction.isMercenary()).thenReturn(true);
        when(faction.getShortName()).thenReturn(MERCENARY_FACTION_CODE);
        when(campaign.getPlayerForce().getFaction()).thenReturn(faction);
    }

    // region helpers
    private Person person(String name) {
        return new Person(name, name, campaign, MERCENARY_FACTION_CODE);
    }

    /** Wires a bidirectional parent/child relationship, as the game stores it on both people. */
    private static void addChild(Person parent, Person child) {
        parent.getGenealogy().addFamilyMember(FamilialRelationshipType.CHILD, child);
        child.getGenealogy().addFamilyMember(FamilialRelationshipType.PARENT, parent);
    }

    private static void marry(Person a, Person b) {
        a.getGenealogy().setSpouse(b);
        b.getGenealogy().setSpouse(a);
    }

    private static void divorce(Person a, Person b, LocalDate date) {
        a.getGenealogy().addFormerSpouse(new FormerSpouse(b, date, FormerSpouseReason.DIVORCE));
        b.getGenealogy().addFormerSpouse(new FormerSpouse(a, date, FormerSpouseReason.DIVORCE));
    }

    private static TreeNodeBox node(FamilyTreeGraph graph, Person person) {
        return graph.nodesByPerson.get(person);
    }

    /** The descendant union of {@code anchor} whose spouse is {@code spouse} (null for a lone-parent union). */
    private static Union unionWithSpouse(FamilyTreeGraph graph, Person anchor, Person spouse) {
        for (Union union : node(graph, anchor).descendantUnions) {
            Person unionSpouse = union.right == null ? null : union.right.person;
            if (unionSpouse == spouse) {
                return union;
            }
        }
        return null;
    }

    private static List<Person> childrenOf(Union union) {
        return union.children.stream().map(child -> child.person).toList();
    }
    // endregion helpers

    @Test
    void bloodlineIncludesAncestorsAndDescendantsButNotMarriedInOrUnrelated() {
        Person grandFather = person("GrandFather");
        Person grandMother = person("GrandMother");
        Person father = person("Father");
        Person mother = person("Mother");
        Person origin = person("Origin");
        Person spouse = person("Spouse");
        Person child = person("Child");
        Person unrelated = person("Unrelated");

        addChild(grandFather, father);
        addChild(grandMother, father);
        addChild(father, origin);
        addChild(mother, origin);
        marry(origin, spouse);
        addChild(origin, child);
        addChild(spouse, child);

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        // Blood relatives (origin, ancestors, descendants) are in the bloodline.
        assertTrue(graph.bloodline.contains(origin));
        assertTrue(graph.bloodline.contains(father));
        assertTrue(graph.bloodline.contains(grandFather));
        assertTrue(graph.bloodline.contains(child));
        // A married-in spouse gets a node but is not part of the bloodline.
        assertFalse(graph.bloodline.contains(spouse));
        assertTrue(graph.nodesByPerson.containsKey(spouse));
        // An unrelated person appears nowhere.
        assertFalse(graph.bloodline.contains(unrelated));
        assertFalse(graph.nodesByPerson.containsKey(unrelated));

        assertEquals(origin, graph.root.person);
    }

    @Test
    void generationsRunFromOriginWithSpousesSharingTheirPartnersRow() {
        Person grandFather = person("GrandFather");
        Person father = person("Father");
        Person mother = person("Mother");
        Person origin = person("Origin");
        Person spouse = person("Spouse");
        Person child = person("Child");

        addChild(grandFather, father);
        addChild(father, origin);
        addChild(mother, origin);
        marry(origin, spouse);
        addChild(origin, child);
        addChild(spouse, child);

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        assertEquals(0, node(graph, origin).generation);
        assertEquals(-1, node(graph, father).generation);
        assertEquals(-1, node(graph, mother).generation);
        assertEquals(-2, node(graph, grandFather).generation);
        assertEquals(1, node(graph, child).generation);
        // A spouse shares its partner's generation.
        assertEquals(0, node(graph, spouse).generation);
        assertEquals(-2, graph.minGeneration);
    }

    @Test
    void halfSiblingsAreSplitAcrossSeparateMarriages() {
        Person origin = person("Origin");
        Person spouse = person("Spouse");
        Person formerSpouse = person("FormerSpouse");
        Person fullChild = person("FullChild");
        Person halfChild = person("HalfChild");

        marry(origin, spouse);
        divorce(origin, formerSpouse, LocalDate.of(3025, 1, 1));
        addChild(origin, fullChild);
        addChild(spouse, fullChild);
        addChild(origin, halfChild);
        addChild(formerSpouse, halfChild);

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        Union currentMarriage = unionWithSpouse(graph, origin, spouse);
        Union formerMarriage = unionWithSpouse(graph, origin, formerSpouse);
        assertNotNull(currentMarriage);
        assertNotNull(formerMarriage);

        assertEquals(List.of(fullChild), childrenOf(currentMarriage));
        assertEquals(List.of(halfChild), childrenOf(formerMarriage));

        // The current spouse is a live marriage; the divorced one is flagged as former.
        assertFalse(currentMarriage.former);
        assertTrue(formerMarriage.former);
    }

    @Test
    void childrenAreOrderedByBirthDate() {
        Person origin = person("Origin");
        Person spouse = person("Spouse");
        Person elder = person("Elder");
        Person younger = person("Younger");

        marry(origin, spouse);
        // Add the younger child first so ordering can't come for free from insertion order.
        addChild(origin, younger);
        addChild(spouse, younger);
        addChild(origin, elder);
        addChild(spouse, elder);
        elder.setDateOfBirth(LocalDate.of(3000, 1, 1));
        younger.setDateOfBirth(LocalDate.of(3005, 1, 1));

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        Union marriage = unionWithSpouse(graph, origin, spouse);
        assertNotNull(marriage);
        assertEquals(List.of(elder, younger), childrenOf(marriage));
    }

    @Test
    void ancestorsFormCouplesAndDescendantsDoNot() {
        Person grandFather = person("GrandFather");
        Person grandMother = person("GrandMother");
        Person father = person("Father");
        Person mother = person("Mother");
        Person origin = person("Origin");
        Person child = person("Child");

        addChild(grandFather, father);
        addChild(grandMother, father);
        addChild(father, origin);
        addChild(mother, origin);
        addChild(origin, child);

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        // The origin's parents form a couple whose (only) child is the origin.
        Union parentCouple = node(graph, origin).parentCouple;
        assertNotNull(parentCouple);
        assertEquals(List.of(origin), childrenOf(parentCouple));
        assertEquals(java.util.Set.of(father, mother),
              java.util.Set.of(parentCouple.left.person, parentCouple.right.person));

        // Grandparents form the father's parent couple.
        assertNotNull(node(graph, father).parentCouple);

        // Descendants are laid out on the descendant side, not as ancestor couples.
        assertNull(node(graph, child).parentCouple);
    }

    @Test
    void loneParentProducesASpouselessUnion() {
        Person origin = person("Origin");
        Person child = person("Child");

        // A child whose only recorded parent is the origin.
        addChild(origin, child);

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        Union loneParent = unionWithSpouse(graph, origin, null);
        assertNotNull(loneParent);
        assertNull(loneParent.right);
        assertEquals(List.of(child), childrenOf(loneParent));
    }

    @Test
    void twoBloodRelativesMarryingBecomeARelatedLinkNotACouple() {
        // Origin has two children who marry each other (pedigree collapse) and have a child of their own.
        Person origin = person("Origin");
        Person firstChild = person("FirstChild");
        Person secondChild = person("SecondChild");
        Person grandChild = person("GrandChild");

        addChild(origin, firstChild);
        addChild(origin, secondChild);
        marry(firstChild, secondChild);
        addChild(firstChild, grandChild);
        addChild(secondChild, grandChild);

        FamilyTreeGraph graph = new FamilyTreeGraph(origin);

        // Both partners are bloodline, so they are not fused into a couple...
        assertTrue(graph.bloodline.contains(firstChild));
        assertTrue(graph.bloodline.contains(secondChild));
        assertNull(unionWithSpouse(graph, firstChild, secondChild));
        assertNull(unionWithSpouse(graph, secondChild, firstChild));

        // ...instead the marriage is recorded once as a dashed related link.
        assertEquals(1, graph.relatedLinks.size());
        java.util.Set<Person> linked = java.util.Set.of(graph.relatedLinks.get(0)[0].person,
              graph.relatedLinks.get(0)[1].person);
        assertEquals(java.util.Set.of(firstChild, secondChild), linked);

        // The grandchild is still placed exactly once, under a lone-parent union of one of them.
        long placements = graph.unions.stream()
                                .flatMap(union -> union.children.stream())
                                .filter(box -> box.person == grandChild)
                                .count();
        assertEquals(1, placements);
    }
}
