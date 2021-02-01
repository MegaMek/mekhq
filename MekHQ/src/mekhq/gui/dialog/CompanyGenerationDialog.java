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
import mekhq.campaign.finances.Money;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.generators.companyGeneration.AbstractCompanyGenerator;

import java.util.ArrayList;
import java.util.List;

public class CompanyGenerationDialog {
    //region Variable Declarations
    private Campaign campaign;
    private AbstractCompanyGenerator companyGenerator;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationDialog(Campaign campaign, AbstractCompanyGenerator companyGenerator) {
        setCampaign(campaign);
        setCompanyGenerator(companyGenerator);
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

    private void applyToCampaign() {
        List<Person> personnel = new ArrayList<>();
        if (getCompanyGenerator().getOptions().isPoolAssistants()) {
            getCampaign().fillAstechPool();
            getCampaign().fillMedicPool();
        } else {
            for (int i = 0; i < getCampaign().getAstechNeed(); i++) {
                personnel.add(getCompanyGenerator().generatePerson(Person.T_ASTECH));
            }
            for (int i = 0; i < getCampaign().getMedicsNeed(); i++) {
                personnel.add(getCompanyGenerator().generatePerson(Person.T_MEDIC));
            }
        }

        if (getCompanyGenerator().getOptions().isPayForSetup()) {
            Money unitCosts = Money.zero();
            Money hiringCosts = Money.zero();
            Money partCosts = Money.zero();
            for (Person person : personnel) {
                hiringCosts = hiringCosts.plus(person.getSalary().multipliedBy(2));
            }
        }
    }
}
