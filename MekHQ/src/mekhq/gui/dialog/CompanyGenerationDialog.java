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
package mekhq.gui.dialog;

import mekhq.campaign.Campaign;
import mekhq.campaign.universe.generators.companyGeneration.AbstractCompanyGenerator;

import javax.swing.*;

public class CompanyGenerationDialog extends JDialog {
    //region Variable Declarations
    private Campaign campaign;
    private AbstractCompanyGenerator companyGenerator;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(JFrame frame, Campaign campaign, AbstractCompanyGenerator companyGenerator) {
        super(frame);

        setCampaign(campaign);
        setCompanyGenerator(companyGenerator);
        initialize();
    }
    //endregion Constructors


    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public AbstractCompanyGenerator getCompanyGenerator() {
        return companyGenerator;
    }

    public void setCompanyGenerator(AbstractCompanyGenerator companyGenerator) {
        this.companyGenerator = companyGenerator;
    }
    //endregion Getters/Setters

    //region Initialization
    private void initialize() {

    }
    //endregion Initialization

    private boolean validateData() {
        // ensure you have a minimum of 1 lance or company generated
        return true;
    }
}
