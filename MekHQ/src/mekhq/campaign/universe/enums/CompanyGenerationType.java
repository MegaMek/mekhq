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
package mekhq.campaign.universe.enums;

import megamek.common.util.EncodeControl;
import mekhq.campaign.Campaign;
import mekhq.campaign.universe.generators.companyGeneration.AbstractCompanyGenerator;
import mekhq.campaign.universe.generators.companyGeneration.AtBCompanyGenerator;
import mekhq.campaign.universe.generators.companyGeneration.CompanyGenerationOptions;
import mekhq.campaign.universe.generators.companyGeneration.WindchildCompanyGenerator;

import java.util.ResourceBundle;

public enum CompanyGenerationType {
    //region Enum Declarations
    AGAINST_THE_BOT("CompanyGenerationType.AGAINST_THE_BOT.text", "CompanyGenerationType.AGAINST_THE_BOT.toolTipText"),
    WINDCHILD("CompanyGenerationType.WINDCHILD.text", "CompanyGenerationType.WINDCHILD.toolTipText");
    //endregion Enum Declarations

    //region Variable Declarations
    private final String name;
    private final String toolTipText;

    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Universe", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    CompanyGenerationType(String name, String toolTipText) {
        this.name = resources.getString(name);
        this.toolTipText = resources.getString(toolTipText);
    }
    //endregion Constructors

    //region Getters
    public String getToolTipText() {
        return toolTipText;
    }
    //endregion Getters

    //region Boolean Comparisons
    public boolean isAtB() {
        return this == AGAINST_THE_BOT;
    }

    public boolean isWindchild() {
        return this == WINDCHILD;
    }
    //endregion Boolean Comparisons

    public AbstractCompanyGenerator getGenerator(final Campaign campaign,
                                                 final CompanyGenerationOptions options) {
        switch (this) {
            case AGAINST_THE_BOT:
                return new AtBCompanyGenerator(campaign, options);
            case WINDCHILD:
            default:
                return new WindchildCompanyGenerator(campaign, options);
        }
    }

    @Override
    public String toString() {
        return name;
    }
}
