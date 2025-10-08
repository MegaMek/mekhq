/*
 * Copyright (C) 2021-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.universe.companyGeneration;

import megamek.common.units.Entity;
import megamek.common.annotations.Nullable;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.enums.CompanyGenerationPersonType;

/**
 * This is used to track a person and their specific setup during Company Generator generation. It has been designed to
 * allow for any portion of the setup to be changed by the frontend.
 *
 * @author Justin "Windchild" Bowen
 */
public class CompanyGenerationPersonTracker {
    //region Variable Declarations
    private CompanyGenerationPersonType personType;
    private Person person;
    private AtBRandomMekParameters parameters;
    private Entity entity;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationPersonTracker(final Person person) {
        this(CompanyGenerationPersonType.MEKWARRIOR, person);
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

    public @Nullable AtBRandomMekParameters getParameters() {
        return parameters;
    }

    public void setParameters(final @Nullable AtBRandomMekParameters parameters) {
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
