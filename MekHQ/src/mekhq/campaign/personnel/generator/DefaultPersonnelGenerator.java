/*
 * Copyright (C) 2019 MegaMek team
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MekHQ.  If not, see <http://www.gnu.org/licenses/>.
 */
package mekhq.campaign.personnel.generator;

import java.util.Objects;

import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.*;
import mekhq.campaign.universe.AbstractFactionSelector;
import mekhq.campaign.universe.AbstractPlanetSelector;
import mekhq.campaign.universe.DefaultFactionSelector;
import mekhq.campaign.universe.DefaultPlanetSelector;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;

/**
 * Creates {@link Person} instances using the default MekHQ algorithm.
 */
public class DefaultPersonnelGenerator extends AbstractPersonnelGenerator {

    private final AbstractFactionSelector factionSelector;
    private final AbstractPlanetSelector planetSelector;

    /**
     * Creates a new DefaultPersonnelGenerator, which will create
     * {@link Person} objects using the faction and current location
     * in a {@link Campaign}.
     */
    public DefaultPersonnelGenerator() {
        this(new DefaultFactionSelector(), new DefaultPlanetSelector());
    }

    /**
     * Creates a new DefaultPersonGenerator with a faction selector.
     * @param factionSelector The faction selector to use with all generated persons.
     */
    public DefaultPersonnelGenerator(AbstractFactionSelector factionSelector, AbstractPlanetSelector planetSelector) {
        this.factionSelector = Objects.requireNonNull(factionSelector);
        this.planetSelector = Objects.requireNonNull(planetSelector);
    }

    @Override
    protected Person createPerson(Campaign campaign) {
        Faction faction = this.factionSelector.selectFaction(campaign);

        Person person;
        if (faction != null) {
            person = new Person(campaign, faction.getShortName());
        } else {
            person = super.createPerson(campaign);
        }

        Planet planet = this.planetSelector.selectPlanet(campaign, faction);
        if (planet != null) {
            person.setOriginPlanet(planet);
        }

        return person;
    }

    @Override
    public Person generate(Campaign campaign, int primaryRole, int secondaryRole, int gender) {
        Person person = createPerson(campaign);

        person.setPrimaryRole(primaryRole);
        person.setSecondaryRole(secondaryRole);

        int expLvl = generateExperienceLevel(campaign, person);

        generateXp(campaign, person, expLvl);

        generatePhenotype(campaign, person, expLvl);

        generateBirthday(campaign, person, expLvl, person.isClanner() && person.getPhenotype() != Person.PHENOTYPE_NONE);

        AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator();
        skillGenerator.setSkillPreferences(getSkillPreferences());
        skillGenerator.generateSkills(person, expLvl);

        AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
        specialAbilityGenerator.setSkillPreferences(getSkillPreferences());
        specialAbilityGenerator.generateSpecialAbilities(person, expLvl);

        // Do naming at the end, to ensure the keys are set
        generateName(person, gender);

        //check for Bloodname
        if (person.isClanner()) {
            campaign.checkBloodnameAdd(person, primaryRole, person.getOriginFaction().getShortName());
        }

        person.setDaysToWaitForHealing(campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());

        return person;
    }
}
