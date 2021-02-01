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
package mekhq.campaign.universe.generators.companyGeneration;

import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.enums.CompanyGenerationType;

import java.util.List;

public class AbstractCompanyGenerator {
    //region Variable Declarations
    private CompanyGenerationType type;
    private CompanyGenerationOptions options;
    //endregion Variable Declarations

    //region Constructors
    protected AbstractCompanyGenerator(CompanyGenerationType type) {
        this(type, new CompanyGenerationOptions());
    }

    protected AbstractCompanyGenerator(CompanyGenerationType type, CompanyGenerationOptions options) {

    }
    //endregion Constructors

    //region Getters/Setters

    public CompanyGenerationType getType() {
        return type;
    }

    public void setType(CompanyGenerationType type) {
        this.type = type;
    }

    public CompanyGenerationOptions getOptions() {
        return options;
    }

    public void setOptions(CompanyGenerationOptions options) {
        this.options = options;
    }
    //endregion Getters/Setters
}
