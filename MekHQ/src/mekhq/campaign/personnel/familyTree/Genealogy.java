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

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import mekhq.MekHQ;
import mekhq.MekHqXmlSerializable;
import mekhq.MekHqXmlUtil;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.FormerSpouse;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
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
    private UUID origin;
    private UUID spouse;
    private List<FormerSpouse> formerSpouses;
    private Map<FamilialRelationshipType, List<UUID>> family;
    //endregion Variables

    //region Constructors
    /**
     * This creates an empty Genealogy object
     * This case should only be used for reading from XML
     */
    public Genealogy() {
        this(null);
    }

    /**
     * This is the standard constructor, and follow the below warning
     * @param origin the person's UUID. This must ONLY be null when reading from XML
     */
    public Genealogy(@Nullable UUID origin) {
        this.origin = origin; // null only when reading from XML
        this.spouse = null;
        this.formerSpouses = new ArrayList<>();
        this.family = new HashMap<>();
    }
    //endregion Constructors

    //region Getters/Setters
    /**
     * @return the person's UUID
     */
    public UUID getOrigin() {
        return origin;
    }

    /**
     * @param origin the person's UUID
     */
    public void setOrigin(UUID origin) {
        this.origin = origin;
    }

    /**
     * @param campaign the campaign the person is in
     * @return the person's spouse, or null if they don't have a spouse
     */
    @Nullable
    public Person getSpouse(Campaign campaign) {
        return campaign.getPerson(getSpouseId());
    }

    /**
     * @return the current person's spouse's id
     */
    @Nullable
    public UUID getSpouseId() {
        return spouse;
    }

    /**
     * @param spouse the new spouse id for the current person
     */
    public void setSpouse(@Nullable UUID spouse) {
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
        getFormerSpouses().add(formerSpouse);
    }

    /**
     * This is implemented for future use, as it will be required for the family tree implementation
     * @param formerSpouse the former spouse to remove from the current person's list
     */
    public void removeFormerSpouse(FormerSpouse formerSpouse) {
        getFormerSpouses().remove(formerSpouse);
    }

    /**
     * @return the family map for this person
     */
    public Map<FamilialRelationshipType, List<UUID>> getFamily() {
        return family;
    }

    /**
     * This is used to add a new family member
     * @param relationshipType the relationship type between the two people
     * @param id the id of the person to add
     */
    public void addFamilyMember(FamilialRelationshipType relationshipType, UUID id) {
        if (id != null) {
            getFamily().putIfAbsent(relationshipType, new ArrayList<>());
            getFamily().get(relationshipType).add(id);
        }
    }

    /**
     * This is implemented for future use, as it will be required for the family tree implementation
     * @param relationshipType the FamilialRelationshipType of the person to remove
     * @param id the id of the person to remove
     */
    public void removeFamilyMember(FamilialRelationshipType relationshipType, UUID id) {
        if (getFamily().get(relationshipType) == null) {
            MekHQ.getLogger().error(getClass(), "removeFamilyMember",
                    "Could not remove unknown family member of relationship "
                            + relationshipType.name() + " and UUID " + id.toString() + ".");
            return;
        }

        List<UUID> familyTypeMembers = getFamily().get(relationshipType);
        familyTypeMembers.remove(id);
        if (familyTypeMembers.isEmpty()) {
            getFamily().remove(relationshipType);
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
        return (getSpouseId() != null);
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
        if (getOrigin().equals(person.getId())) {
            // Same person will always return true, to prevent any weirdness
            return true;
        }

        final int depth = campaign.getCampaignOptions().checkMutualAncestorsDepth();
        if (depth == 0) {
            // Check is disabled, return false for no mutual ancestors
            return false;
        }

        Set<UUID> originAncestors = getAncestors(depth, campaign);
        Set<UUID> checkAncestors = person.getGenealogy().getAncestors(depth, campaign);

        for (UUID id : checkAncestors) {
            if (originAncestors.contains(id)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param depth the depth of ancestors to get
     * @param campaign the campaign the person is part of
     * @return a set of all unique ancestors of a person back depth generations
     * @note this is a recursive search to ensure it goes to a specified depth of relation
     */
    public Set<UUID> getAncestors(int depth, Campaign campaign) {
        // Create the return value
        Set<UUID> ancestors = new HashSet<>();

        // Add this person to the return set
        ancestors.add(getOrigin());

        // Then check if we need to continue down the tree
        if (depth > 0) {
            // If so, decrease remaining search depth
            depth--;
            // Then parse through the parents
            for (UUID parent : getParents()) {
                // And add all of their returned ancestors to the list
                ancestors.addAll(campaign.getPerson(parent).getGenealogy().getAncestors(depth, campaign));
            }
        }

        // Finally, return the ancestors
        return ancestors;
    }
    //endregion Boolean Checks

    //region Basic Family Getters
    /**
     * @param campaign the campaign they are part of
     * @return a list of the current person's grandparent(s) id(s)
     */
    public List<UUID> getGrandparents(Campaign campaign) {
        List<UUID> grandparents = new ArrayList<>();
        List<UUID> tempGrandparents;

        for (UUID parentId : getParents()) {
            Person parent = campaign.getPerson(parentId);
            if (parent != null) {
                tempGrandparents = parent.getGenealogy().getParents();
                // prevents duplicates, if anyone uses a small number of depth for their ancestry
                grandparents.removeAll(tempGrandparents);
                grandparents.addAll(tempGrandparents);
            }
        }

        return grandparents;
    }

    /**
     * @return the id(s) of the person's parent(s)
     */
    public List<UUID> getParents() {
        return getFamily().getOrDefault(FamilialRelationshipType.PARENT, new ArrayList<>());
    }

    /**
     * @param campaign the campaign they are part of
     * @param gender the gender of the parent(s) to get
     * @return a list of UUIDs of the person's parent(s) of the specified gender
     */
    public List<UUID> getParentsByGender(Campaign campaign, Gender gender) {
        List<UUID> parents = new ArrayList<>();
        for (UUID parentId : getFamily().getOrDefault(FamilialRelationshipType.PARENT, new ArrayList<>())) {
            Person parent = campaign.getPerson(parentId);
            if ((parent != null) && (parent.getGender() == gender)) {
                parents.add(parentId);
            }
        }

        return parents;
    }

    /**
     * @param campaign the campaign they are part of
     * @return the person's father(s) id(s)
     */
    public List<UUID> getFathers(Campaign campaign) {
        return getParentsByGender(campaign, Gender.MALE);
    }

    /**
     * @param campaign the campaign they are part of
     * @return the person's mother(s) id(s)
     */
    public List<UUID> getMothers(Campaign campaign) {
        return getParentsByGender(campaign, Gender.FEMALE);
    }

    /**
     * Siblings are defined as sharing either parent. Inlaws are not counted.
     * @return the siblings of the current person
     */
    public List<UUID> getSiblings(Campaign campaign) {
        List<UUID> siblings = new ArrayList<>();
        for (UUID parentId : getParents()) {
            siblings.addAll(campaign.getPerson(parentId).getGenealogy().getChildren());
        }
        siblings.remove(getOrigin());
        return siblings;
    }

    /**
     * @param campaign the campaign they are part of
     * @return a list of the person's siblings with spouses (if any)
     */
    public List<UUID> getSiblingsAndSpouses(Campaign campaign) {
        List<UUID> siblingsAndSpouses = new ArrayList<>();
        for (UUID parent : getParents()) {
            for (UUID sibling : campaign.getPerson(parent).getGenealogy().getChildren()) {
                if (!getOrigin().equals(sibling)) {
                    siblingsAndSpouses.remove(sibling);
                    siblingsAndSpouses.add(sibling);
                    UUID spouse = campaign.getPerson(sibling).getGenealogy().getSpouseId();
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
     * @return a list of UUIDs of the current person's children
     */
    public List<UUID> getChildren() {
        return getFamily().getOrDefault(FamilialRelationshipType.CHILD, new ArrayList<>());
    }

    /**
     * @param campaign the campaign they are part of
     * @return a list of UUIDs of the person's grandchildren
     */
    public List<UUID> getGrandchildren(Campaign campaign) {
        List<UUID> grandchildren = new ArrayList<>();
        List<UUID> tempGrandchildren;
        for (UUID childId : getChildren()) {
            // We want to get the children of any children, without duplicates
            tempGrandchildren = campaign.getPerson(childId).getGenealogy().getChildren();
            grandchildren.removeAll(tempGrandchildren);
            grandchildren.addAll(tempGrandchildren);
        }

        return grandchildren;
    }

    /**
     * @param campaign the campaign they are part of
     * @return a list of the person's Aunts and Uncles
     */
    public List<UUID> getsAuntsAndUncles(Campaign campaign) {
        List<UUID> auntsAndUncles = new ArrayList<>();
        List<UUID> tempAuntsAndUncles;
        for (UUID parentId : getParents()) {
            Person parent = campaign.getPerson(parentId);
            if (parent != null) {
                tempAuntsAndUncles = parent.getGenealogy().getSiblingsAndSpouses(campaign);
                auntsAndUncles.removeAll(tempAuntsAndUncles);
                auntsAndUncles.addAll(tempAuntsAndUncles);
            }
        }

        return auntsAndUncles;
    }

    /**
     * @param campaign the campaign they are part of
     * @return a list of the person's cousins
     */
    public List<UUID> getCousins(Campaign campaign) {
        List<UUID> cousins = new ArrayList<>();
        List<UUID> tempCousins;

        for (UUID auntOrUncleId : getsAuntsAndUncles(campaign)) {
            Person auntOrUncle = campaign.getPerson(auntOrUncleId);
            if (auntOrUncle != null) {
                tempCousins = auntOrUncle.getGenealogy().getChildren();
                cousins.removeAll(tempCousins);
                cousins.addAll(tempCousins);
            }
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
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "genealogy");
        MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "origin", getOrigin().toString());
        if (getSpouseId() != null) {
            MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 1, "spouse", getSpouseId().toString());
        }
        if (!getFormerSpouses().isEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "formerSpouses");
            for (FormerSpouse ex : getFormerSpouses()) {
                ex.writeToXml(pw1, indent + 2);
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "formerSpouses");
        }
        if (!familyIsEmpty()) {
            MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 1, "family");
            for (FamilialRelationshipType relationshipType : getFamily().keySet()) {
                MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent + 2, "relationship");
                MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "type", relationshipType.name());
                for (UUID id : getFamily().get(relationshipType)) {
                    MekHqXmlUtil.writeSimpleXmlTag(pw1, indent + 3, "personId", id.toString());
                }
                MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 2, "relationship");
            }
            MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent + 1, "family");
        }
        MekHqXmlUtil.writeSimpleXMLCloseIndentedLine(pw1, indent, "genealogy");
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
                    retVal.setOrigin(UUID.fromString(wn1.getTextContent()));
                } else if (wn1.getNodeName().equalsIgnoreCase("spouse")) {
                    retVal.setSpouse(UUID.fromString(wn1.getTextContent()));
                } else if (wn1.getNodeName().equalsIgnoreCase("formerSpouses")) {
                    loadFormerSpouses(retVal, wn1.getChildNodes());
                } else if (wn1.getNodeName().equalsIgnoreCase("family")) {
                    loadFamily(retVal, wn1.getChildNodes());
                }
            }
        } catch (Exception e) {
            MekHQ.getLogger().error(Genealogy.class, "generateInstanceFromXML", "");
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
                MekHQ.getLogger().error(Genealogy.class, "generateInstanceFromXML",
                        "Unknown node type not loaded in formerSpouses nodes: " + wn2.getNodeName());
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
                List<UUID> ids = new ArrayList<>();
                for (int i = 0; i < nl3.getLength(); i++) {
                    Node wn3 = nl3.item(i);
                    if (wn3.getNodeName().equalsIgnoreCase("type")) {
                        type = FamilialRelationshipType.valueOf(wn3.getTextContent());
                    } else if (wn3.getNodeName().equalsIgnoreCase("personId")) {
                        ids.add(UUID.fromString(wn3.getTextContent().trim()));
                    }
                }

                retVal.getFamily().put(type, ids);
            }
        }
    }

    /**
     * @return whether the Genealogy object is empty or not
     */
    public boolean isEmpty() {
        if ((getSpouseId() != null) || !getFormerSpouses().isEmpty()) {
            return false;
        }

        return familyIsEmpty();
    }

    /**
     * @return whether the family side of the Genealogy object is empty or not
     * (i.e. spouse and formerSpouses are not included, just family)
     */
    public boolean familyIsEmpty() {
        for (List<UUID> list : getFamily().values()) {
            if ((list != null) && !list.isEmpty()) {
                return false;
            }
        }

        return true;
    }
    //endregion Read/Write to XML
}
