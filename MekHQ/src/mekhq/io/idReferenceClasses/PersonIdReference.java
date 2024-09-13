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
package mekhq.io.idReferenceClasses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import megamek.logging.MMLogger;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.familyTree.FormerSpouse;

public class PersonIdReference extends Person {
    private static final MMLogger logger = MMLogger.create(PersonIdReference.class);

    // region Constructors
    public PersonIdReference(final String text) {
        super(UUID.fromString(text));
    }
    // endregion Constructors

    public static void fixPersonIdReferences(final Campaign campaign) {
        for (final Person person : campaign.getPersonnel()) {
            fixGenealogyReferences(campaign, person);
        }
    }

    /**
     * This fixes a person's Genealogy PersonIdReferences. It is public ONLY for
     * unit testing
     * 
     * @param campaign the campaign the person is in
     * @param person   the person to fix genealogy for
     */
    public static void fixGenealogyReferences(final Campaign campaign, final Person person) {
        if (person.getGenealogy().isEmpty()) {
            return;
        }

        // Spouse
        if (person.getGenealogy().getSpouse() instanceof PersonIdReference) {
            final Person spouse = campaign.getPerson(person.getGenealogy().getSpouse().getId());
            if (spouse == null) {
                logger.warn("Failed to find the spouse for " + person.getFullTitle()
                        + " with id " + person.getGenealogy().getSpouse().getId());
            }
            person.getGenealogy().setSpouse(spouse);
        }

        // Former Spouse
        if (!person.getGenealogy().getFormerSpouses().isEmpty()) {
            final List<Person> unknownPersonnel = new ArrayList<>();

            for (final FormerSpouse formerSpouse : person.getGenealogy().getFormerSpouses()) {
                if (!(formerSpouse.getFormerSpouse() instanceof PersonIdReference)) {
                    continue;
                }
                final Person ex = campaign.getPerson(formerSpouse.getFormerSpouse().getId());
                if (ex == null) {
                    logger.warn("Failed to find a person with id " + formerSpouse.getFormerSpouse().getId());
                    unknownPersonnel.add(formerSpouse.getFormerSpouse());
                } else {
                    formerSpouse.setFormerSpouse(ex);
                }
            }

            for (final Person unknown : unknownPersonnel) {
                person.getGenealogy().removeFormerSpouse(unknown);
            }
        }

        // Family
        if (person.getGenealogy().familyIsEmpty()) {
            return;
        }

        // Create a shallow copy of the current family
        final Map<FamilialRelationshipType, List<Person>> family = new HashMap<>(person.getGenealogy().getFamily());

        // Clear the person's family
        person.getGenealogy().getFamily().clear();

        // Then we can migrate
        for (final Entry<FamilialRelationshipType, List<Person>> entry : family.entrySet()) {
            for (final Person familyMemberReference : entry.getValue()) {
                if (familyMemberReference == null) {
                    continue;
                }
                final Person familyMember = (familyMemberReference instanceof PersonIdReference)
                        ? campaign.getPerson(familyMemberReference.getId())
                        : familyMemberReference;
                if (familyMember == null) {
                    logger.warn("Failed to find a person with id " + familyMemberReference.getId());
                } else {
                    person.getGenealogy().getFamily().putIfAbsent(entry.getKey(), new ArrayList<>());
                    person.getGenealogy().getFamily().get(entry.getKey()).add(familyMember);
                }
            }
        }
    }
}
