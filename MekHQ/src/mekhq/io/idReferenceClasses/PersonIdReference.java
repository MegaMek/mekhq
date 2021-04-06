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
package mekhq.io.idReferenceClasses;

import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.familyTree.FormerSpouse;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.FamilialRelationshipType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PersonIdReference extends Person {
    //region Variables
    private static final long serialVersionUID = -2450241955642276590L;
    //endregion Variables

    //region Constructors
    public PersonIdReference(String text) {
        super(UUID.fromString(text));
    }
    //endregion Constructors

    public static void fixPersonIdReferences(Campaign campaign) {
        for (Person person : campaign.getPersonnel()) {
            fixGenealogyReferences(campaign, person);
        }
    }

    private static void fixGenealogyReferences(Campaign campaign, Person person) {
        List<Person> unknownPersonnel = new ArrayList<>();

        // Origin
        person.getGenealogy().setOrigin(person);

        // Spouse
        if (person.getGenealogy().getSpouse() instanceof PersonIdReference) {
            person.getGenealogy().setSpouse(campaign.getPerson(person.getGenealogy().getSpouse().getId()));
        }

        // Former Spouse
        for (FormerSpouse formerSpouse : person.getGenealogy().getFormerSpouses()) {
            if (!(formerSpouse.getFormerSpouse() instanceof PersonIdReference)) {
                continue;
            }
            final Person ex = campaign.getPerson(formerSpouse.getFormerSpouse().getId());
            if (ex == null) {
                MekHQ.getLogger().warning("Failed to find a person with id " + formerSpouse.getFormerSpouse().getId());
                unknownPersonnel.add(formerSpouse.getFormerSpouse());
            } else {
                formerSpouse.setFormerSpouse(ex);
            }
        }
        for (Person unknown : unknownPersonnel) {
            person.getGenealogy().removeFormerSpouse(unknown);
        }

        // Family
        Map<FamilialRelationshipType, List<Person>> family = new HashMap<>();
        for (Map.Entry<FamilialRelationshipType, List<Person>> entry : person.getGenealogy().getFamily().entrySet()) {
            for (Person familyMemberRef : entry.getValue()) {
                if (familyMemberRef == null) {
                    continue;
                }
                final Person familyMember = (familyMemberRef instanceof PersonIdReference)
                        ? campaign.getPerson(familyMemberRef.getId()) : familyMemberRef;
                if (familyMember == null) {
                    MekHQ.getLogger().warning("Failed to find a person with id " + familyMemberRef.getId());
                } else {
                    family.putIfAbsent(entry.getKey(), new ArrayList<>());
                    family.get(entry.getKey()).add(familyMember);
                }
            }
        }
        person.getGenealogy().setFamily(family);
    }
}
