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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.familyTree;

import megamek.common.Crew;
import megamek.common.annotations.Nullable;
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

public class Genealogy implements Serializable, MekHqXmlSerializable {
    //region Variables
    private static final long serialVersionUID = -6350146649504329173L;
    private UUID spouse;
    private List<FormerSpouse> formerSpouses;
    private Map<FamilialRelationshipType, List<UUID>> family;
    //endregion Variables

    //region Constructors
    public Genealogy() {
        this.spouse = null;
        this.formerSpouses = new ArrayList<>();
        this.family = new HashMap<>();
    }
    //endregion Constructors

    //region Getters/Setters
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

    // Recursive Search Format to determine mutual ancestry
    public boolean checkMutualAncestors(Person person, Campaign campaign) {
        return checkMutualAncestors(person.getGenealogy(), campaign,
                campaign.getCampaignOptions().checkMutualAncestorsDepth());
    }

    public boolean checkMutualAncestors(Genealogy secondaryGenealogy, Campaign campaign, int remainingDepth) {
        // We don't need to go any deeper if:
        // 1. the remaining depth is less than or equal to 0
        // 2. This genealogy does not have parents
        // 3. The secondary genealogy does not have parents
        if ((remainingDepth <= 0) || !hasParents() || !secondaryGenealogy.hasParents()) {
            return false;
        }

        // Update the remaining depth to check
        remainingDepth--;

        // First, we check if there are any shared parents
        // TODO : determine how we want to handle this for stepparents
        for (UUID id : getParents()) {
            if (secondaryGenealogy.getParents().contains(id)) {
                return true;
            }
        }

        // Then, we check based on mutual ancestry
        for (UUID id : getParents()) {
            Person person = campaign.getPerson(id);
            for (UUID secondaryId : secondaryGenealogy.getParents()) {
                Person secondaryPerson = campaign.getPerson(secondaryId);
                if (person.getGenealogy().checkMutualAncestors(secondaryPerson.getGenealogy(),
                        campaign, remainingDepth)) {
                    return true;
                }
                if (secondaryPerson.getGenealogy().checkMutualAncestors(person.getGenealogy(),
                        campaign, remainingDepth)) {
                    return true;
                }
                if (checkMutualAncestors(secondaryPerson.getGenealogy(), campaign, remainingDepth)) {
                    return true;
                }
            }

            if (checkMutualAncestors(person.getGenealogy(), campaign, remainingDepth)) {
                return true;
            }
        }

        return false;
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
    public List<UUID> getParentsByGender(Campaign campaign, int gender) {
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
        return getParentsByGender(campaign, Crew.G_MALE);
    }

    /**
     * @param campaign the campaign they are part of
     * @return the person's mother(s) id(s)
     */
    public List<UUID> getMothers(Campaign campaign) {
        return getParentsByGender(campaign, Crew.G_FEMALE);
    }

    /**
     * Siblings are defined as sharing either parent, or any stepsiblings. Inlaws are not counted.
     * @return the siblings of the current person
     */
    public List<UUID> getSiblings() {
        List<UUID> siblings = new ArrayList<>();
        for (FamilialRelationshipType relationshipType : FamilialRelationshipType.getSiblingRelationshipTypes()) {
            siblings.addAll(getFamily().getOrDefault(relationshipType, new ArrayList<>()));
        }
        return siblings;
    }

    /**
     * @param campaign the campaign they are part of
     * @return a list of the person's siblings with spouses (if any)
     */
    public List<UUID> getSiblingsAndSpouses(Campaign campaign) {
        List<UUID> siblingsAndSpouses = new ArrayList<>();

        for (FamilialRelationshipType relationshipType : FamilialRelationshipType.getSiblingRelationshipTypes()) {
            for (UUID sibling : getFamily().getOrDefault(relationshipType, new ArrayList<>())) {
                // This prevents duplicates, in the case of two brothers marrying two sisters
                siblingsAndSpouses.remove(sibling);
                siblingsAndSpouses.add(sibling);
                UUID spouse = campaign.getPerson(sibling).getGenealogy().getSpouseId();
                if (spouse != null) {
                    siblingsAndSpouses.remove(spouse);
                    siblingsAndSpouses.add(spouse);
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
    @Override
    public void writeToXml(PrintWriter pw1, int indent) {
        MekHqXmlUtil.writeSimpleXMLOpenIndentedLine(pw1, indent, "genealogy");
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

    public static Genealogy generateInstanceFromXML(NodeList nl) {
        Genealogy retVal = new Genealogy();

        try {
            for (int x = 0; x < nl.getLength(); x++) {
                Node wn1 = nl.item(x);

                if (wn1.getNodeName().equalsIgnoreCase("spouse")) {
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

    // This must be public for migration reasons
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

    private static void loadFamily(Genealogy retVal, NodeList nl2) {
        for (int y = 0; y < nl2.getLength(); y++) {
            Node wn2 = nl2.item(y);

            if (wn2.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            if (wn2.getNodeName().equalsIgnoreCase("relationship")) {
                NodeList nl3 = wn2.getChildNodes();
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

    public boolean isEmpty() {
        if ((getSpouseId() != null) || !getFormerSpouses().isEmpty()) {
            return false;
        }

        return familyIsEmpty();
    }

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
