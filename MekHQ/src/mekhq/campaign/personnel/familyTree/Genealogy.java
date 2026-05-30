/*
 * Copyright (C) 2020-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.familyTree;

import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.logging.MMLogger;
import mekhq.campaign.HumanResources;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.procreation.AbstractProcreation;
import mekhq.io.idReferenceClasses.PersonIdReference;
import mekhq.utilities.MHQXMLUtility;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The Genealogy class is used to track immediate familial relationships, spouses, and former spouses. It is also used
 * to determine familial relationships between people
 */
public class Genealogy {
    private static final MMLogger LOGGER = MMLogger.create(Genealogy.class);

    // region Variables
    private final Person origin;
    private Person spouse;
    private Person originSpouse; // the person who originated the marriage
    private final List<FormerSpouse> formerSpouses = new ArrayList<>();
    private final Map<FamilialRelationshipType, List<Person>> family = new HashMap<>();
    // endregion Variables

    // region Constructors

    /**
     * @param origin the origin person
     */
    public Genealogy(final Person origin) {
        this.origin = origin;
        setSpouse(null);
    }
    // endregion Constructors

    // region Getters/Setters

    /**
     * @return the origin person
     */
    public Person getOrigin() {
        return origin;
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
    public void setSpouse(final @Nullable Person spouse) {
        this.spouse = spouse;
    }

    /**
     * @return the person who originated the marriage
     */
    public @Nullable Person getOriginSpouse() {
        return originSpouse;
    }

    /**
     * @param originSpouse the person who originated the marriage
     */
    public void setOriginSpouse(final @Nullable Person originSpouse) {
        this.originSpouse = originSpouse;
    }

    /**
     * @return a list of FormerSpouse objects for all the former spouses of the current person
     */
    public List<FormerSpouse> getFormerSpouses() {
        return formerSpouses;
    }

    /**
     * @param formerSpouse a former spouse to add to the current person's list
     */
    public void addFormerSpouse(final FormerSpouse formerSpouse) {
        getFormerSpouses().add(formerSpouse);
    }

    /**
     * @param formerSpouse the former spouse object to remove from the current person's list. Do note that this may
     *                     remove multiple identical former spouses, as we do not require uniqueness for former
     *                     spouses.
     */
    public void removeFormerSpouse(final FormerSpouse formerSpouse) {
        getFormerSpouses().removeIf(ex -> ex.equals(formerSpouse));
    }

    /**
     * @param formerSpouse the former spouse to remove from the current person's list
     */
    public void removeFormerSpouse(final Person formerSpouse) {
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
     *
     * @param relationshipType the relationship type between the two people
     * @param person           the person to add
     */
    public void addFamilyMember(final FamilialRelationshipType relationshipType,
          final @Nullable Person person) {
        if (person != null) {
            getFamily().putIfAbsent(relationshipType, new ArrayList<>());
            getFamily().get(relationshipType).add(person);
        }
    }

    /**
     * @param relationshipType the FamilialRelationshipType of the person to remove
     * @param person           the person to remove
     */
    public void removeFamilyMember(final @Nullable FamilialRelationshipType relationshipType,
          final Person person) {
        if (relationshipType == null) {
            for (final FamilialRelationshipType type : FamilialRelationshipType.values()) {
                final List<Person> familyMembers = getFamily().getOrDefault(type, new ArrayList<>());
                if (!familyMembers.isEmpty() && familyMembers.contains(person)) {
                    familyMembers.remove(person);
                    if (familyMembers.isEmpty()) {
                        getFamily().remove(type);
                    }
                    break;
                }
            }
        } else if (getFamily().get(relationshipType) == null) {
            LOGGER.error(
                  "Could not remove family member of unknown relationship {} between person {}and unknown potential relation {} {}.",
                  relationshipType.name(),
                  person.getFullTitle(),
                  person.getFullTitle(),
                  person.getId());
        } else {
            final List<Person> familyTypeMembers = getFamily().get(relationshipType);
            familyTypeMembers.remove(person);
            if (familyTypeMembers.isEmpty()) {
                getFamily().remove(relationshipType);
            }
        }
    }
    // endregion Getters/Setters

    // region Boolean Checks

    /**
     * @return whether the Genealogy object is empty or not
     */
    public boolean isEmpty() {
        return (getSpouse() == null) && getFormerSpouses().isEmpty() && familyIsEmpty();
    }

    /**
     * @return whether the family side of the Genealogy object is empty or not (i.e. spouse and formerSpouses are not
     *       included, just family)
     */
    public boolean familyIsEmpty() {
        return getFamily().isEmpty();
    }

    /**
     * @return true if the person has a spouse, false otherwise
     */
    public boolean hasSpouse() {
        return getSpouse() != null;
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
     * @return {@code true} if the person has at least one child, {@code false} otherwise
     */
    public boolean hasNonAdultChildren(LocalDate localDate) {
        return getChildren().stream().anyMatch(child -> child.isChild(localDate));
    }

    /**
     * @return true if the Person has any parents, otherwise false
     */
    public boolean hasParents() {
        return getFamily().get(FamilialRelationshipType.PARENT) != null;
    }

    /**
     * @return {@code true} if the person at least one living parent, otherwise {@code false}. Will also return false if
     *       the person has no parents
     */
    public boolean hasLivingParents() {
        if (hasParents()) {
            return getFamily().get(FamilialRelationshipType.PARENT).stream()
                         .anyMatch(parent -> !parent.getStatus().isDead());
        }

        return false;
    }

    /**
     * This is used to determine if two people have mutual ancestors based on their genealogies
     *
     * @param person the person to check if they are related or not
     * @param depth  the depth to check mutual ancestry up to
     *
     * @return true if they have mutual ancestors, otherwise false
     */
    public boolean checkMutualAncestors(final Person person, final int depth) {
        if (getOrigin().equals(person)) {
            // Same person will always return true, to prevent any weirdness
            return true;
        } else if (depth == 0) {
            // Check is disabled, return false for no mutual ancestors
            return false;
        } else {
            final Set<Person> originAncestors = getAncestors(depth);
            return person.getGenealogy().getAncestors(depth).stream().anyMatch(originAncestors::contains);
        }
    }

    /**
     * @param depth the depth of ancestors to get
     *
     * @return a set of all unique ancestors of a person back depth generations
     *
     * @note this is a recursive search to ensure it goes to a specified depth of relation
     */
    private Set<Person> getAncestors(int depth) {
        // Create the return value
        final Set<Person> ancestors = new HashSet<>();

        // Add this person to the return set
        ancestors.add(getOrigin());

        // Then check if we need to continue down the tree
        if (depth > 0) {
            // If so, decrease remaining search depth
            depth--;
            // Then parse through the parents
            for (final Person parent : getParents()) {
                // And add all of their returned ancestors to the list
                ancestors.addAll(parent.getGenealogy().getAncestors(depth));
            }
        }

        // Finally, return the ancestors
        return ancestors;
    }
    // endregion Boolean Checks

    // region Basic Family Getters

    /**
     * @return a list of the current person's grandparent(s)
     */
    public List<Person> getGrandparents() {
        return getParents().stream()
                     .flatMap(parent -> parent.getGenealogy().getParents().stream())
                     .distinct()
                     .collect(Collectors.toList());
    }

    /**
     * @return the person's parent(s)
     */
    public List<Person> getParents() {
        return getFamily().getOrDefault(FamilialRelationshipType.PARENT, new ArrayList<>());
    }

    /**
     * @param gender the gender of the parent(s) to get
     *
     * @return a list of the person's parent(s) of the specified gender
     */
    public List<Person> getParentsByGender(final Gender gender) {
        return getFamily()
                     .getOrDefault(FamilialRelationshipType.PARENT, new ArrayList<>())
                     .stream()
                     .filter(parent -> parent.getGender() == gender)
                     .distinct()
                     .collect(Collectors.toList());
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
     * Siblings are defined as sharing either parent. In laws are not counted.
     *
     * @return the siblings of the current person
     */
    public List<Person> getSiblings() {
        return getParents().stream()
                     .flatMap(parent -> parent.getGenealogy().getChildren().stream())
                     .distinct()
                     .filter(sibling -> !getOrigin().equals(sibling))
                     .collect(Collectors.toList());
    }

    /**
     * @return a list of the person's siblings with spouses (if any)
     */
    public List<Person> getSiblingsAndSpouses() {
        final List<Person> siblingsAndSpouses = new ArrayList<>();
        for (final Person sibling : getSiblings()) {
            siblingsAndSpouses.remove(sibling);
            siblingsAndSpouses.add(sibling);
            if (sibling.getGenealogy().hasSpouse()) {
                siblingsAndSpouses.remove(sibling.getGenealogy().getSpouse());
                siblingsAndSpouses.add(sibling.getGenealogy().getSpouse());
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
        return getChildren().stream()
                     .flatMap(child -> child.getGenealogy().getChildren().stream())
                     .distinct()
                     .collect(Collectors.toList());
    }

    /**
     * @return a list of the person's Aunts and Uncles
     */
    public List<Person> getsAuntsAndUncles() {
        return getParents().stream()
                     .flatMap(parent -> parent.getGenealogy().getSiblingsAndSpouses().stream())
                     .distinct()
                     .collect(Collectors.toList());
    }

    /**
     * @return a list of the person's cousins
     */
    public List<Person> getCousins() {
        return getsAuntsAndUncles().stream()
                     .flatMap(auntOrUncle -> auntOrUncle.getGenealogy().getChildren().stream())
                     .distinct()
                     .collect(Collectors.toList());
    }
    // endregion Basic Family Getters

    /**
     * This is used to remove all external Genealogy links to a person, as part of clearing out any data related to a
     * person during their removal.
     */
    public void clearGenealogyLinks() {
        // Clear Spouse
        if (getSpouse() != null) {
            getSpouse().getGenealogy().setSpouse(null);
        }

        // Clear Former Spouses
        if (!getFormerSpouses().isEmpty()) {
            getFormerSpouses().forEach(
                  formerSpouse -> formerSpouse.getFormerSpouse().getGenealogy().removeFormerSpouse(getOrigin()));
        }

        // Clear Family
        if (!familyIsEmpty()) {
            getFamily().values().stream()
                  .flatMap(Collection::stream)
                  .forEach(person -> person.getGenealogy().removeFamilyMember(null, getOrigin()));
        }
    }

    /**
     * Determines whether this family unit is considered "active" by checking whether any member of the extended family
     * network is currently active (i.e., has not left the unit).
     *
     * <p>Traverses the full genealogical graph rooted at {@code origin}, including parents, children, current and
     * former spouses, and pregnancy fathers, returning {@code true} as soon as any such relative is found to be
     * active.</p>
     *
     * @param humanResources the {@link HumanResources} registry used to resolve person lookups by ID (e.g., pregnancy
     *                       father resolution)
     *
     * @return {@code true} if at least one member of the extended family is active; {@code false} if all reachable
     *       relatives have left the unit
     *
     * @author Illiani
     * @since 0.51.0
     */
    public boolean isActive(HumanResources humanResources) {
        HashSet<Person> allFamilyMembers = new HashSet<>();
        return collectRelatives(humanResources, origin, allFamilyMembers, true);
    }

    /**
     * Recursively traverses the genealogical graph from {@code currentPerson}, collecting all reachable relatives and
     * optionally short-circuiting as soon as a genealogically active person is found.
     *
     * <p>Traversal covers the following relationships at each node:</p>
     * <ul>
     *     <li>Parents</li>
     *     <li>Children</li>
     *     <li>Former spouses (via {@link FormerSpouse} wrapper)</li>
     *     <li>Current spouse</li>
     *     <li>Pregnancy father, resolved through {@link HumanResources} when the current person is pregnant and
     *     father ID data is present</li>
     * </ul>
     *
     * <p>Already-visited persons are tracked in {@code allFamilyMembers} to prevent infinite recursion across
     * cyclical or bidirectional relationships.</p>
     *
     * @param humanResources          the {@link HumanResources} registry for resolving persons by UUID
     * @param currentPerson           the person whose relatives are currently being examined
     * @param allFamilyMembers        the set of already-visited persons; modified in place to prevent revisiting nodes
     * @param checkForActiveGenealogy if {@code true}, the traversal will return {@code true} immediately upon finding
     *                                any non-departed relative; if {@code false}, no activity check is performed
     *
     * @return {@code true} if {@code checkForActiveGenealogy} is {@code true} and an active relative was found;
     *       {@code false} otherwise
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static boolean collectRelatives(HumanResources humanResources, Person currentPerson,
          HashSet<Person> allFamilyMembers, boolean checkForActiveGenealogy) {
        // Short-circuit if already visited (prevents infinite recursion on bidirectional links)
        // or if this person is themselves active
        if (!allFamilyMembers.add(currentPerson)) {
            return false;
        }

        if (isGenealogicallyActive(checkForActiveGenealogy, currentPerson)) {
            return true;
        }

        Genealogy currentGenealogy = currentPerson.getGenealogy();

        // Lots of null protection throughout to make sure we're not processing null entries. That shouldn't
        // happen, but it is a possibility with very old campaigns from before we had better protections
        for (Person parent : currentGenealogy.getParents()) {
            if (parent != null) {
                if (collectRelatives(humanResources, parent, allFamilyMembers, checkForActiveGenealogy)) {
                    return true;
                }
            }
        }

        for (Person child : currentGenealogy.getChildren()) {
            if (child != null) {
                if (collectRelatives(humanResources, child, allFamilyMembers, checkForActiveGenealogy)) {
                    return true;
                }
            }
        }

        for (FormerSpouse formerSpouse : currentGenealogy.getFormerSpouses()) {
            if (formerSpouse != null) {
                Person formerSpousePerson = formerSpouse.getFormerSpouse();
                if (formerSpousePerson != null) {
                    if (collectRelatives(humanResources,
                          formerSpousePerson,
                          allFamilyMembers,
                          checkForActiveGenealogy)) {
                        return true;
                    }
                }
            }
        }

        Person spouse = currentGenealogy.getSpouse();
        if (spouse != null) {
            if (collectRelatives(humanResources, spouse, allFamilyMembers, checkForActiveGenealogy)) {
                return true;
            }
        }

        if (currentPerson.isPregnant()) {
            String fatherIdString = currentPerson.getExtraData().get(AbstractProcreation.PREGNANCY_FATHER_DATA);
            UUID fatherId = (fatherIdString != null) ? UUID.fromString(fatherIdString) : null;
            if (fatherId != null) {
                Person father = humanResources.getPerson(fatherId);
                if (father != null) {
                    return collectRelatives(humanResources, father, allFamilyMembers, checkForActiveGenealogy);
                }
            }
        }

        return false;
    }

    /**
     * Evaluates whether a given person should be considered genealogically active.
     *
     * <p>A person is considered active if the activity check is enabled, and they have not left the unit. Returns
     * {@code false} unconditionally for {@code null} persons.</p>
     *
     * @param checkForActiveGenealogy {@code true} to apply the active status check; {@code false} to always return
     *                                {@code false}
     * @param currentPerson           the person to evaluate; may be {@code null}
     *
     * @return {@code true} if {@code checkForActiveGenealogy} is {@code true}, {@code currentPerson} is non-null, and
     *       their status indicates they have not left the unit; {@code false} otherwise
     *
     * @author Illiani
     * @since 0.51.0
     */
    private static boolean isGenealogicallyActive(boolean checkForActiveGenealogy, Person currentPerson) {
        if (currentPerson == null) {
            return false;
        }

        return checkForActiveGenealogy && !currentPerson.getStatus().isDepartedUnit();
    }

    // region File I/O

    /**
     * @param pw     the PrintWriter to write to
     * @param indent the indent for the baseline (i.e. the line containing genealogy)
     */
    public void writeToXML(final PrintWriter pw, int indent) {
        MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "genealogy");
        if (getSpouse() != null) {
            MHQXMLUtility.writeSimpleXMLTag(pw, indent, "spouse", getSpouse().getId());
        }

        if (!getFormerSpouses().isEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "formerSpouses");
            for (final FormerSpouse ex : getFormerSpouses()) {
                ex.writeToXML(pw, indent);
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "formerSpouses");
        }

        if (!familyIsEmpty()) {
            MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "family");
            for (final FamilialRelationshipType relationshipType : getFamily().keySet()) {
                MHQXMLUtility.writeSimpleXMLOpenTag(pw, indent++, "relationship");
                MHQXMLUtility.writeSimpleXMLTag(pw, indent, "type", relationshipType.name());
                for (final Person person : getFamily().get(relationshipType)) {
                    MHQXMLUtility.writeSimpleXMLTag(pw, indent, "personId", person.getId());
                }
                MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "relationship");
            }
            MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "family");
        }
        MHQXMLUtility.writeSimpleXMLCloseTag(pw, --indent, "genealogy");
    }

    /**
     * @param nl the NodeList containing the saved Genealogy
     */
    public void fillFromXML(final NodeList nl) {
        for (int x = 0; x < nl.getLength(); x++) {
            final Node wn = nl.item(x);
            if (wn.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            try {
                switch (wn.getNodeName()) {
                    case "spouse":
                        setSpouse(new PersonIdReference(wn.getTextContent().trim()));
                        break;
                    case "formerSpouses":
                        if (wn.hasChildNodes()) {
                            loadFormerSpouses(wn.getChildNodes());
                        } else {
                            LOGGER.error("Cannot parse a former spouses node without child nodes for {}",
                                  getOrigin().getId());
                        }
                        break;
                    case "family":
                        if (wn.hasChildNodes()) {
                            loadFamily(wn.getChildNodes());
                        } else {
                            LOGGER.error("Cannot parse a family node without child nodes for {}", getOrigin().getId());
                        }
                        break;
                    default:
                        break;
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to parse a node with name {} containing {} for {}",
                      wn.getNodeName(),
                      wn.getTextContent(),
                      getOrigin().getId(),
                      ex);
            }
        }
    }

    /**
     * This loads the FormerSpouses from their saved nodes Note: This must be public for migration reasons
     *
     * @param nl the NodeList containing the saved former spouses
     */
    public void loadFormerSpouses(final NodeList nl) {
        for (int y = 0; y < nl.getLength(); y++) {
            try {
                final Node wn = nl.item(y);
                if ("formerSpouse".equals(wn.getNodeName())) {
                    getFormerSpouses().add(FormerSpouse.generateInstanceFromXML(wn));
                }
            } catch (Exception ex) {
                LOGGER.error("", ex);
            }
        }
    }

    /**
     * This loads the familial relationships from their saved nodes
     *
     * @param nl the NodeList containing the saved Genealogy familial relationships
     */
    private void loadFamily(final NodeList nl) {
        for (int y = 0; y < nl.getLength(); y++) {
            final Node wn = nl.item(y);
            if (!"relationship".equals(wn.getNodeName())) {
                continue;
            }
            final NodeList nl2 = wn.getChildNodes();

            // The default value should never be used, but we need a default to prevent IDE
            // complaints
            FamilialRelationshipType type = FamilialRelationshipType.PARENT;
            final List<Person> people = new ArrayList<>();
            for (int i = 0; i < nl2.getLength(); i++) {
                final Node wn2 = nl2.item(i);
                switch (wn2.getNodeName()) {
                    case "type":
                        type = FamilialRelationshipType.valueOf(wn2.getTextContent().trim());
                        break;
                    case "personId":
                        people.add(new PersonIdReference(wn2.getTextContent().trim()));
                        break;
                    default:
                        break;
                }
            }
            getFamily().put(type, people);
        }
    }
    // endregion File I/O
}
