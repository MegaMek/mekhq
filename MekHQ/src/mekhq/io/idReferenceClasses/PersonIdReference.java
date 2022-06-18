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

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import org.apache.logging.log4j.LogManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

public class PersonIdReference extends Person {
    //region Constructors
    public PersonIdReference(final String text) {
        super(UUID.fromString(text));
    }
    //endregion Constructors

    public static void fixPersonIdReferences(final Campaign campaign) {
        for (Person person : campaign.getPersonnel()) {
            fixGenealogyReferences(campaign, person);
        }
    }

    private static void fixGenealogyReferences(final Campaign campaign, final Person person) {
        // Spouse
        if (person.getGenealogy().getSpouse() instanceof PersonIdReference) {
            person.getGenealogy().setSpouse(campaign.getPerson(person.getGenealogy().getSpouse().getId()));
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
                    LogManager.getLogger().warn("Failed to find a person with id " + formerSpouse.getFormerSpouse().getId());
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
        for (final Entry<FamilialRelationshipType, List<Person>> entry : person.getGenealogy().getFamily().entrySet()) {
            for (final Person familyMemberReference : entry.getValue()) {
                if (familyMemberReference == null) {
                    continue;
                }
                final Person familyMember = (familyMemberReference instanceof PersonIdReference)
                        ? campaign.getPerson(familyMemberReference.getId()) : familyMemberReference;
                if (familyMember == null) {
                    LogManager.getLogger().warn("Failed to find a person with id " + familyMemberReference.getId());
                } else {
                    person.getGenealogy().getFamily().putIfAbsent(entry.getKey(), new ArrayList<>());
                    person.getGenealogy().getFamily().get(entry.getKey()).add(familyMember);
                }
            }
        }
    }
}
