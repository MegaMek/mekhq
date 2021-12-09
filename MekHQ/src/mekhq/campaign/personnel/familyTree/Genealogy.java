/*
 * Copyright (c) 2020-2021 - The MegaMek Team. All Rights Reserved.
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

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.io.idReferenceClasses.PersonIdReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The Genealogy class is used to track immediate familial relationships, spouses, and former spouses.
 * It is also used to determine familial relationships between people
 */
public class Genealogy implements Serializable {
    //region Variables
    private static final long serialVersionUID = -6350146649504329173L;
    private Person origin;
    private Person spouse;
    private List<FormerSpouse> formerSpouses;
    private Map<FamilialRelationshipType, List<Person>> family;
    //endregion Variables

    //region Constructors
    /**
     * This is the standard constructor, and follow the below warning
     * @param origin the origin person
     */
    public Genealogy(final Person origin) {
        setOrigin(origin);
        setSpouse(null);
        formerSpouses = new ArrayList<>();
        setFamily(new HashMap<>());
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return the origin person
     */
    public Person getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin person
     */
    public void setOrigin(Person origin) {
        this.origin = Objects.requireNonNull(origin);
    }

    /**
     * @return the current person's spouse
     */
    public @Nullable Person getSpouse() {
        return spouse;
    }

    /**
     * @param spouse the new spouse for the current person
     */
    public void setSpouse(@Nullable Person spouse) {
        this.spouse = spouse;
    }

    /**
     * @return a list of FormerSpouse objects for all the former spouses of the current person
     */
    public List<FormerSpouse> getFormerSpouses() {
        return formerSpouses;
    }

    /**
     * @param formerSpouse a former spouse to add the the current person's list
     */
    public void addFormerSpouse(FormerSpouse formerSpouse) {
        getFormerSpouses().add(Objects.requireNonNull(formerSpouse));
    }

    /**
     * @param formerSpouse the former spouse to remove from the current person's list
     */
    public void removeFormerSpouse(Person formerSpouse) {
        getFormerSpouses().removeIf(ex -> ex.getFormerSpouse().equals(formerSpouse));
    }

    /**
     * @return the family map for this person
     */
    public Map<FamilialRelationshipType, List<Person>> getFamily() {
        return family;
    }

    /**
     * @param family the new family map for this person
     */
    public void setFamily(Map<FamilialRelationshipType, List<Person>> family) {
        this.family = family;
    }

    /**
     * This is used to add a new family member
     * @param relationshipType the relationship type between the two people
     * @param person the person to add
     */
    public void addFamilyMember(FamilialRelationshipType relationshipType, @Nullable Person person) {
        if (person != null) {
            getFamily().putIfAbsent(relationshipType, new ArrayList<>());
            getFamily().get(relationshipType).add(person);
        }
    }

    /**
     * @param relationshipType the FamilialRelationshipType of the person to remove
     * @param person the person to remove
     */
    public void removeFamilyMember(@Nullable FamilialRelationshipType relationshipType, Person person) {
        if (relationshipType == null) {
            for (FamilialRelationshipType type : FamilialRelationshipType.values()) {
                List<Person> familyMembers = getFamily().getOrDefault(type, new ArrayList<>());
                if (!familyMembers.isEmpty() && familyMembers.contains(person)) {
                    familyMembers.remove(person);
                    if (familyMembers.isEmpty()) {
                        getFamily().remove(type);
                    }
                    break;
                }
            }
        } else if (getFamily().get(relationshipType) == null) {
            MekHQ.getLogger().error("Could not remove unknown family member of relationship "
                    + relationshipType.name() + " and person " + person.getFullTitle() + " " + person.getId() + ".");
        } else {
            List<Person> familyTypeMembers = getFamily().get(relationshipType);
            familyTypeMembers.remove(person);
            if (familyTypeMembers.isEmpty()) {
                getFamily().remove(relationshipType);
            }
        }
    }
    //endregion Getters/Setters

    //region Boolean Checks
    /**
     * @return true if the person has either a spouse, any children, or specified parents.
     *          These are required for any extended family to exist.
     */
    public boolean hasAnyFamily() {
        return hasChildren() || hasSpouse() || hasParents();
    }

    /**
     * @return true if the person has a spouse, false otherwise
     */
    public boolean hasSpouse() {
        return (getSpouse() != null);
    }

    /**
     * @return true if the person has a former spouse, false otherwise
     */
    public boolean hasFormerSpouse() {
        return !getFormerSpouses().isEmpty();
    }

    /**
     * @return true if the person has at least one kid, false otherwise
     */
    public boolean hasChildren() {
        return getFamily().get(FamilialRelationshipType.CHILD) != null;
    }

    /**
     * @return true if the Person has any parents, otherwise false
     */
    public boolean hasParents() {
        return getFamily().get(FamilialRelationshipType.PARENT) != null;
    }

    /**
     * This is used to determine if two people have mutual ancestors based on their genealogies
     * @param person the person to check if they are related or not
     * @param campaign the campaign the two people are a part of
     * @return true if they have mutual ancestors, otherwise false
     */
    public boolean checkMutualAncestors(Person person, Campaign campaign) {
        if (getOrigin().equals(person)) {
            // Same person will always return true, to prevent any weirdness
            return true;
        }

        final int depth = campaign.getCampaignOptions().checkMutualAncestorsDepth();
        if (depth == 0) {
            // Check is disabled, return false for no mutual ancestors
            return false;
        }

        Set<Person> originAncestors = getAncestors(depth);
        Set<Person> checkAncestors = person.getGenealogy().getAncestors(depth);

        for (Person ancestor : checkAncestors) {
            if (originAncestors.contains(ancestor)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param depth the depth of ancestors to get
     * @return a set of all unique ancestors of a person back depth generations
     * @note this is a recursive search to ensure it goes to a specified depth of relation
     */
    private Set<Person> getAncestors(int depth) {
        // Create the return value
        Set<Person> ancestors = new HashSet<>();

        // Add this person to the return set
        ancestors.add(getOrigin());

        // Then check if we need to continue down the tree
        if (depth > 0) {
            // If so, decrease remaining search depth
            depth--;
            // Then parse through the parents
            for (Person parent : getParents()) {
                // And add all of their returned ancestors to the list
                ancestors.addAll(parent.getGenealogy().getAncestors(depth));
            }
        }

        // Finally, return the ancestors
        return ancestors;
    }
    //endregion Boolean Checks

    //region Basic Family Getters
    /**
     * @return a list of the current person's grandparent(s)
     */
    public List<Person> getGrandparents() {
        List<Person> grandparents = new ArrayList<>();
        List<Person> tempGrandparents;

        for (Person parent : getParents()) {
            tempGrandparents = parent.getGenealogy().getParents();
            // prevents duplicates, if anyone uses a small number of depth for their ancestry
            grandparents.removeAll(tempGrandparents);
            grandparents.addAll(tempGrandparents);
        }

        return grandparents;
    }

    /**
     * @return the person's parent(s)
     */
    public List<Person> getParents() {
        return getFamily().getOrDefault(FamilialRelationshipType.PARENT, new ArrayList<>());
    }

    /**
     * @param gender the gender of the parent(s) to get
     * @return a list of the person's parent(s) of the specified gender
     */
    public List<Person> getParentsByGender(Gender gender) {
        List<Person> parents = new ArrayList<>();
        for (Person parent : getFamily().getOrDefault(FamilialRelationshipType.PARENT, new ArrayList<>())) {
            if (parent.getGender() == gender) {
                parents.add(parent);
            }
        }

        return parents;
    }

    /**
     * @return the person's father(s)
     */
    public List<Person> getFathers() {
        return getParentsByGender(Gender.MALE);
    }

    /**
     * @return the person's mother(s)
     */
    public List<Person> getMothers() {
        return getParentsByGender(Gender.FEMALE);
    }

    /**
     * Siblings are defined as sharing either parent. Inlaws are not counted.
     * @return the siblings of the current person
     */
    public List<Person> getSiblings() {
        List<Person> siblings = new ArrayList<>();
        for (Person parent : getParents()) {
            for (Person sibling : parent.getGenealogy().getChildren()) {
                if (!getOrigin().equals(sibling)) {
                    siblings.remove(sibling);
                    siblings.add(sibling);
                }
            }
        }

        return siblings;
    }

    /**
     * @return a list of the person's siblings with spouses (if any)
     */
    public List<Person> getSiblingsAndSpouses() {
        List<Person> siblingsAndSpouses = new ArrayList<>();
        for (Person parent : getParents()) {
            for (Person sibling : parent.getGenealogy().getChildren()) {
                if (!getOrigin().equals(sibling)) {
                    siblingsAndSpouses.remove(sibling);
                    siblingsAndSpouses.add(sibling);
                    Person spouse = sibling.getGenealogy().getSpouse();
                    if (spouse != null) {
                        siblingsAndSpouses.remove(spouse);
                        siblingsAndSpouses.add(spouse);
                    }
                }
            }
        }

        return siblingsAndSpouses;
    }

    /**
     * @return a list of the current person's children
     */
    public List<Person> getChildren() {
        return getFamily().getOrDefault(FamilialRelationshipType.CHILD, new ArrayList<>());
    }

    /**
     * @return a list of the person's grandchildren
     */
    public List<Person> getGrandchildren() {
        List<Person> grandchildren = new ArrayList<>();
        List<Person> tempGrandchildren;
        for (Person child : getChildren()) {
            // We want to get the children of any children, without duplicates
            tempGrandchildren = child.getGenealogy().getChildren();
            grandchildren.removeAll(tempGrandchildren);
            grandchildren.addAll(tempGrandchildren);
        }

        return grandchildren;
    }

    /**
     * @return a list of the person's Aunts and Uncles
     */
    public List<Person> getsAuntsAndUncles() {
        List<Person> auntsAndUncles = new ArrayList<>();
        List<Person> tempAuntsAndUncles;
        for (Person parent : getParents()) {
            tempAuntsAndUncles = parent.getGenealogy().getSiblingsAndSpouses();
            auntsAndUncles.removeAll(tempAuntsAndUncles);
            auntsAndUncles.addAll(tempAuntsAndUncles);
        }

        return auntsAndUncles;
    }

    /**
     * @return a list of the person's cousins
     */
    public List<Person> getCousins() {
        List<Person> cousins = new ArrayList<>();
        List<Person> tempCousins;

        for (Person auntOrUncle : getsAuntsAndUncles()) {
            tempCousins = auntOrUncle.getGenealogy().getChildren();
            cousins.removeAll(tempCousins);
            cousins.addAll(tempCousins);
        }

        return cousins;
    }
    //endregion Basic Family Getters

    //region Read/Write to XML
    /**
     * @param pw the PrintWriter to write to
     * @param indent the indent for the base line (i.e. the line containing genealogy)
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "genealogy");
        if (getSpouse() != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "spouse", getSpouse().getId());
        }

        if (!getFormerSpouses().isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "formerSpouses");
            for (FormerSpouse ex : getFormerSpouses()) {
                ex.writeToXML(pw, indent);
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "formerSpouses");
        }

        if (!familyIsEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "family");
            for (FamilialRelationshipType relationshipType : getFamily().keySet()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw, indent++, "relationship");
                MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "type", relationshipType.name());
                for (Person person : getFamily().get(relationshipType)) {
                    MekHqXmlUtil.writeSimpleXmlTag(pw, indent, "personId", person.getId());
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "relationship");
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "family");
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw, --indent, "genealogy");
    }

    /**
     * @param nl the NodeList containing the saved Genealogy
     */
    public void fillFromXML(final NodeList nl) {
        for (int x = 0; x < nl.getLength(); x++) {
            Node wn = nl.item(x);
            try {
                if (wn.getNodeName().equalsIgnoreCase("spouse")) {
                    setSpouse(new PersonIdReference(wn.getTextContent().trim()));
                } else if (wn.getNodeName().equalsIgnoreCase("formerSpouses")) {
                    if (wn.hasChildNodes()) {
                        loadFormerSpouses(wn.getChildNodes());
                    }
                } else if (wn.getNodeName().equalsIgnoreCase("family")) {
                    if (wn.hasChildNodes()) {
                        loadFamily(wn.getChildNodes());
                    }
                }
            } catch (Exception e) {
                MekHQ.getLogger().error("Failed to parse " + wn.getTextContent() + " for " + getOrigin().getId());
            }
        }
    }

    /**
     * This loads the FormerSpouses from their saved nodes
     * @param nl the NodeList containing the saved former spouses
     * @note This must be public for migration reasons
     */
    public void loadFormerSpouses(final NodeList nl) {
        for (int y = 0; y < nl.getLength(); y++) {
            final Node wn = nl.item(y);
            // If it's not an element node, we ignore it.
            if (wn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn.getNodeName().equalsIgnoreCase("formerSpouse")) {
                MekHQ.getLogger().error("Unknown node type not loaded in formerSpouses nodes: "
                        + wn.getNodeName());
                continue;
            }
            getFormerSpouses().add(FormerSpouse.generateInstanceFromXML(wn));
        }
    }

    /**
     * This loads the familial relationships from their saved nodes
     * @param nl the NodeList containing the saved Genealogy familial relationships
     */
    private void loadFamily(final NodeList nl) {
        for (int y = 0; y < nl.getLength(); y++) {
            final Node wn = nl.item(y);

            if (wn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn.getNodeName().equalsIgnoreCase("relationship")) {
                final NodeList nl2 = wn.getChildNodes();
                // The default value should never be used, but it is useful to have a default
                FamilialRelationshipType type = FamilialRelationshipType.PARENT;
                final List<Person> people = new ArrayList<>();
                for (int i = 0; i < nl2.getLength(); i++) {
                    Node wn2 = nl2.item(i);
                    if (wn2.getNodeName().equalsIgnoreCase("type")) {
                        type = FamilialRelationshipType.valueOf(wn2.getTextContent().trim());
                    } else if (wn2.getNodeName().equalsIgnoreCase("personId")) {
                        people.add(new PersonIdReference(wn2.getTextContent().trim()));
                    }
                }
                getFamily().put(type, people);
            }
        }
    }

    /**
     * @return whether the Genealogy object is empty or not
     */
    public boolean isEmpty() {
        return (getSpouse() == null) && getFormerSpouses().isEmpty() && familyIsEmpty();
    }

    /**
     * @return whether the family side of the Genealogy object is empty or not
     * (i.e. spouse and formerSpouses are not included, just family)
     */
    public boolean familyIsEmpty() {
        for (List<Person> list : getFamily().values()) {
            if ((list != null) && !list.isEmpty()) {
                return false;
            }
        }

        return true;
    }
    //endregion Read/Write to XML

    //region Clear Genealogy
    /**
     * This is used to remove Genealogy links to a person
     */
    public void clearGenealogy() {
        // Clear Spouse
        if (getSpouse() != null) {
            getSpouse().getGenealogy().setSpouse(null);
        }

        // Clear Former Spouses
        if (!getFormerSpouses().isEmpty()) {
            for (FormerSpouse formerSpouse : getFormerSpouses()) {
                Person person = formerSpouse.getFormerSpouse();
                if (person != null) {
                    person.getGenealogy().removeFormerSpouse(getOrigin());
                }
            }
        }

        // Clear Family
        if (!familyIsEmpty()) {
            for (List<Person> list : getFamily().values()) {
                for (Person person : list) {
                    person.getGenealogy().removeFamilyMember(null, getOrigin());
                }
            }
        }
    }
    //endregion Clear Genealogy
}
