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
package mekhq.campaign.universe.generators.companyGenerators;

import megamek.common.Entity;
import megamek.common.annotations.Nullable;
import mekhq.campaign.Campaign;
import mekhq.campaign.mission.Contract;
import mekhq.campaign.parts.AmmoStorage;
import mekhq.campaign.parts.Armor;
import mekhq.campaign.parts.Part;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.unit.Unit;

import java.util.ArrayList;
import java.util.List;

public class CompanyGenerationWizard {
    //region Variable Declarations
    private final Campaign campaign;
    private final AbstractCompanyGenerator generator;
    //endregion Variable Declarations

    //region Constructors
    public CompanyGenerationWizard(final Campaign campaign, final AbstractCompanyGenerator generator) {
        this.campaign = campaign;
        this.generator = generator;
    }
    //endregion Constructors

    //region Getters/Setters
    public Campaign getCampaign() {
        return campaign;
    }

    public AbstractCompanyGenerator getGenerator() {
        return generator;
    }
    //endregion Getters/Setters

    //region Apply to Campaign
    /**
     * TODO : UNFINISHED
     * This method takes the campaign and applies all changes to it. No method not directly
     * called from here may alter the campaign.
     *
     * @param campaign the campaign to apply the generation to
     * @param combatPersonnel the list of generated combat personnel
     * @param supportPersonnel the list of generated support personnel
     * @param entities the list of generated entities, with null holding spaces without 'Mechs
     * @param mothballedEntities the list of generated spare 'Mech entities to mothball
     * @param contract the selected contract, or null if one has not been selected
     */
    public void applyToCampaign(final Campaign campaign, final List<Person> combatPersonnel,
                                final List<Person> supportPersonnel, final List<Entity> entities,
                                final List<Entity> mothballedEntities, final @Nullable Contract contract) {
        getGenerator().moveToStartingPlanet(campaign);

        // Phase One: Personnel, Units, and Unit
        final List<Person> personnel = new ArrayList<>();
        final List<Unit> units = new ArrayList<>();
        applyPhaseOneToCampaign(campaign, combatPersonnel, supportPersonnel, personnel, entities, units);

        // Phase 2: Spares
        units.addAll(getGenerator().createMothballedSpareUnits(campaign, mothballedEntities));

        final List<Part> parts = getGenerator().generateSpareParts(units);
        final List<Armor> armour = getGenerator().generateArmour(units);
        final List<AmmoStorage> ammunition = getGenerator().generateAmmunition(campaign, units);

        // Phase 3: Contract
        getGenerator().processContract(campaign, contract);

        // Phase 4: Finances
        getGenerator().processFinances(campaign, personnel, units, parts, armour, ammunition, contract);

        // Phase 5: Applying Spares
        parts.forEach(p -> campaign.getWarehouse().addPart(p, true));
        armour.forEach(a -> campaign.getWarehouse().addPart(a, true));
        ammunition.forEach(a -> campaign.getWarehouse().addPart(a, true));

        // Phase 6: Surprises!
        getGenerator().generateSurprises(campaign);
    }

    private void applyPhaseOneToCampaign(final Campaign campaign, final List<Person> combatPersonnel,
                                         final List<Person> supportPersonnel, final List<Person> personnel,
                                         final List<Entity> entities, final List<Unit> units) {
        // Process Personnel
        personnel.addAll(combatPersonnel);
        personnel.addAll(supportPersonnel);

        // If we aren't using the pool, generate all of the Astechs and Medics required
        getGenerator().generateAssistants(campaign, personnel);

        // This does all of the final personnel processing, including recruitment and running random
        // marriages
        getGenerator().finalizePersonnel(campaign, personnel);

        // We can only fill the pool after recruiting our support personnel
        if (getGenerator().getOptions().isPoolAssistants()) {
            campaign.fillAstechPool();
            campaign.fillMedicPool();
        }

        // Process Units
        units.addAll(getGenerator().createUnits(campaign, combatPersonnel, entities));

        // Assign Techs to Units
        getGenerator().assignTechsToUnits(supportPersonnel, units);

        // Generate the Forces and Assign Units to them
        getGenerator().generateUnit(campaign, getGenerator().sortPersonnelIntoLances(combatPersonnel));
    }
    //endregion Apply to Campaign

    //region Revert Application to Campaign
    // TODO : ADD ME
    //endregion Revert Application to Campaign


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
