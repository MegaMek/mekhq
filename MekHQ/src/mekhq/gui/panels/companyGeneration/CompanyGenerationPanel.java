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
package mekhq.gui.panels.companyGeneration;

import megamek.common.Entity;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.universe.generators.companyGenerators.AbstractCompanyGenerator;

import java.util.List;

public class CompanyGenerationPanel {
    //region Variable Declarations
    private AbstractCompanyGenerator companyGenerator = null;

    // Data
    private List<Person> combatPersonnel;
    private List<Person> supportPersonnel;
    private List<Entity> entities;
    //endregion Variable Declarations

    //region Getters/Setters
    public AbstractCompanyGenerator getCompanyGenerator() {
        return companyGenerator;
    }

    public void setCompanyGenerator(AbstractCompanyGenerator companyGenerator) {
        this.companyGenerator = companyGenerator;
    }

    //region Data
    public List<Person> getCombatPersonnel() {
        return combatPersonnel;
    }

    public void setCombatPersonnel(List<Person> combatPersonnel) {
        this.combatPersonnel = combatPersonnel;
    }

    public List<Person> getSupportPersonnel() {
        return supportPersonnel;
    }

    public void setSupportPersonnel(List<Person> supportPersonnel) {
        this.supportPersonnel = supportPersonnel;
    }

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }
    //endregion Data
    //endregion Getters/Setters
/**

    public void generate() {
        if ((getCompanyGenerator() != null) && (JOptionPane.showConfirmDialog(getFrame(),
                resources.getString("CompanyGenerationPanel.OverwriteGenerationWarning.text"),
                resources.getString("CompanyGenerationPanel.OverwriteGenerationWarning.title"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION)) {
            return;
        } else if (!validateOptions()) {
            return;
        }

        setCompanyGenerator(getCompanyGenerationMethod().getGenerator(getCampaign(), createOptionsFromPanel()));

        setCombatPersonnel(getCompanyGenerator().generateCombatPersonnel(getCampaign()));
        setSupportPersonnel(getCompanyGenerator().generateSupportPersonnel(getCampaign()));
        setEntities(getCompanyGenerator().generateUnits(getCampaign(), getCombatPersonnel()));
    }

    public void apply() {
        if (getCompanyGenerator() == null) {
            if (JOptionPane.showConfirmDialog(getFrame(),
                    resources.getString("CompanyGenerationPanel.ImmediateApplicationWarning.text"),
                    resources.getString("CompanyGenerationPanel.ImmediateApplicationWarning.title"),
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                generate();
                // Catch statement for bad data
                if (getCompanyGenerator() == null) {
                    return;
                }
            } else {
                return;
            }
        }

        getCompanyGenerator().applyToCampaign(getCampaign(), getCombatPersonnel(),
                getSupportPersonnel(), getEntities());
    }
 */
}
