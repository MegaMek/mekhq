/*
 * Copyright (c) 2021 - The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.companyGeneration;

import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.enums.CompanyGenerationPersonType;

public class CompanyGenerationPersonTracker {
    //region Variable Declarations
    private CompanyGenerationPersonType personType;
    private Person person;
    private AtBRandomMechParameters parameters;
    private Entity entity;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationPersonTracker(final Person person) {
        this(CompanyGenerationPersonType.MECHWARRIOR, person);
    }

    public CompanyGenerationPersonTracker(final CompanyGenerationPersonType personType,
                                          final Person person) {
        setPersonType(personType);
        setPerson(person);
        setParameters(null);
        setEntity(null);
    }
    //endregion Constructors

    //region Getters/Setters
    public CompanyGenerationPersonType getPersonType() {
        return personType;
    }

    public void setPersonType(final CompanyGenerationPersonType personType) {
        this.personType = personType;
    }

    public Person getPerson() {
        return person;
    }

    public void setPerson(final Person person) {
        this.person = person;
    }

    public @Nullable AtBRandomMechParameters getParameters() {
        return parameters;
    }

    public void setParameters(final @Nullable AtBRandomMechParameters parameters) {
        this.parameters = parameters;
    }

    public @Nullable Entity getEntity() {
        return entity;
    }

    public void setEntity(final @Nullable Entity entity) {
        this.entity = entity;
    }
    //endregion Getters/Setters
}
