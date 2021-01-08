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
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.io.idReferenceClasses.PersonIdReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.*;

/**
 * The Genealogy class is used to track immediate familial relationships, spouses, and former spouses.
 * It is also used to determine familial relationships between people
 */
public class Genealogy implements Serializable, MekHqXmlSerializable {
    //region Variables
    private static final long serialVersionUID = -6350146649504329173L;
    private Person origin;
    private Person spouse;
    private List<FormerSpouse> formerSpouses;
    private Map<FamilialRelationshipType, List<Person>> family;
    //endregion Variables

    //region Constructors
    /**
     * This creates an empty Genealogy object
     * This case should only be used for reading from XML
     */
    private Genealogy() {
        this(null);
    }

    /**
     * This is the standard constructor, and follow the below warning
     * @param origin the origin person. This must ONLY be null when reading from XML
     */
    public Genealogy(@Nullable Person origin) {
        this.origin = origin; // null only when reading from XML
        setSpouse(null);
        formerSpouses = new ArrayList<>();
        family = new HashMap<>();
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
     * @param person the person to add
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
        return (getFamily().get(FamilialRelationshipType.CHILD) != null);
    }

    /**
     * @return true if the Person has any parents, otherwise false
     */
    public boolean hasParents() {
        return (getFamily().get(FamilialRelationshipType.PARENT) != null);
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
     * @param pw1 the PrintWriter to write to
     * @param indent the indent for the base line (i.e. the line containing genealogy)
     */
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "genealogy");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "origin", getOrigin().getId());
        if (getSpouse() != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "spouse", getSpouse().getId());
        }

        if (!getFormerSpouses().isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "formerSpouses");
            for (FormerSpouse ex : getFormerSpouses()) {
                ex.writeToXml(pw1, indent);
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "formerSpouses");
        }

        if (!familyIsEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "family");
            for (FamilialRelationshipType relationshipType : getFamily().keySet()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent++, "relationship");
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "type", relationshipType.name());
                for (Person person : getFamily().get(relationshipType)) {
                    MekHqXmlUtil.writeSimpleXmlTag(pw1, indent, "personId", person.getId());
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "relationship");
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "family");
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, --indent, "genealogy");
    }

    /**
     * @param nl the NodeList containing the saved Genealogy
     * @return the Genealogy generated from the provided NodeList
     */
    public static Genealogy generateInstanceFromXML(NodeList nl) {
        Genealogy retVal = new Genealogy();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn1 = nl.item(x);

                if (wn1.getNodeName().equalsIgnoreCase("origin")) {
                    retVal.setOrigin(new PersonIdReference(wn1.getTextContent().trim()));
                } else if (wn1.getNodeName().equalsIgnoreCase("spouse")) {
                    retVal.setSpouse(new PersonIdReference(wn1.getTextContent().trim()));
                } else if (wn1.getNodeName().equalsIgnoreCase("formerSpouses")) {
                    loadFormerSpouses(retVal, wn1.getChildNodes());
                } else if (wn1.getNodeName().equalsIgnoreCase("family")) {
                    loadFamily(retVal, wn1.getChildNodes());
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error("Failed to generate Genealogy for "
                    + ((retVal.getOrigin() == null) ? "unknown person." : retVal.getOrigin().toString()));
        }

        return retVal;
    }

    /**
     * This loads the FormerSpouses from their saved nodes
     * @param retVal the Genealogy to load the FormerSpouse nodes into
     * @param nl2 the NodeList containing the saved former spouses
     * @note This must be public for migration reasons
     */
    public static void loadFormerSpouses(Genealogy retVal, NodeList nl2) {
        for (int y = 0; y < nl2.getLength(); y++) {
            Node wn2 = nl2.item(y);
            // If it's not an element node, we ignore it.
            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (!wn2.getNodeName().equalsIgnoreCase("formerSpouse")) {
                MekHQ.getLogger().error("Unknown node type not loaded in formerSpouses nodes: "
                        + wn2.getNodeName());
                continue;
            }
            retVal.formerSpouses.add(FormerSpouse.generateInstanceFromXML(wn2));
        }
    }

    /**
     * This loads the familial relationships from their saved nodes
     * @param retVal the Genealogy to load the family nodes into
     * @param nl2 the NodeList containing the saved Genealogy familial relationships
     */
    private static void loadFamily(Genealogy retVal, NodeList nl2) {
        for (int y = 0; y < nl2.getLength(); y++) {
            Node wn2 = nl2.item(y);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("relationship")) {
                NodeList nl3 = wn2.getChildNodes();
                // The default value should never be used, but it is useful to have a default
                FamilialRelationshipType type = FamilialRelationshipType.PARENT;
                List<Person> people = new ArrayList<>();
                for (int i = 0; i < nl3.getLength(); i++) {
                    Node wn3 = nl3.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("type")) {
                        type = FamilialRelationshipType.valueOf(wn3.getTextContent());
                    } else if (wn3.getNodeName().equalsIgnoreCase("personId")) {
                        people.add(new PersonIdReference(wn3.getTextContent().trim()));
                    }
                }

                retVal.getFamily().put(type, people);
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
