/*
 * Copyright (C) 2019-2025 MegaMek team
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
package mekhq.campaign.personnel.generator;

import megamek.common.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.randomEvents.personalities.PersonalityController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;

import java.util.Objects;

import static mekhq.campaign.personnel.education.EducationController.setInitialEducationLevel;

/**
 * Creates {@link Person} instances using the default MekHQ algorithm.
 */
public class DefaultPersonnelGenerator extends AbstractPersonnelGenerator {

    private final AbstractFactionSelector factionSelector;
    private final AbstractPlanetSelector planetSelector;

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
    public Person generate(Campaign campaign, PersonnelRole primaryRole, PersonnelRole secondaryRole, Gender gender) {
        Person person = createPerson(campaign);

        person.setPrimaryRoleDirect(primaryRole);
        person.setSecondaryRoleDirect(secondaryRole);

        int expLvl = generateExperienceLevel(person);

        generateXp(campaign, person);

        generatePhenotype(campaign, person);

        generateBirthday(campaign, person, expLvl, person.isClanPersonnel() && !person.getPhenotype().isNone());

        AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(getSkillPreferences());
        skillGenerator.generateSkills(campaign, person, expLvl);

        // Limit skills by age for children and adolescents
        int age = person.getAge(campaign.getLocalDate());

        if (age < 16) {
            person.removeAllSkills();
            // regenerate expLvl to factor in skill changes from age
            expLvl = generateExperienceLevel(person);
        } else if (age < 18) {
            person.limitSkills(1);

            expLvl = generateExperienceLevel(person);
        }

        // set SPAs
        if (expLvl >= 0) {
            AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
            specialAbilityGenerator.setSkillPreferences(new RandomSkillPreferences());
            specialAbilityGenerator.generateSpecialAbilities(campaign, person, expLvl);
        }

        // set interest in marriage and children flags
        int interestInMarriageDiceSize = campaign.getCampaignOptions().getNoInterestInMarriageDiceSize();
        person.setMarriageable(((interestInMarriageDiceSize != 0) && (Compute.randomInt(interestInMarriageDiceSize)) != 0));

        int interestInChildren = campaign.getCampaignOptions().getNoInterestInChildrenDiceSize();
        person.setTryingToConceive(((interestInChildren != 0) && (Compute.randomInt(interestInChildren)) != 0));

        // Do naming at the end, to ensure the keys are set
        generateNameAndGender(campaign, person, gender);

        //check for Bloodname
        campaign.checkBloodnameAdd(person, false);

        person.setDaysToWaitForHealing(campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());

        // set loyalty
        if (expLvl <= 0) {
            person.setLoyalty(Compute.d6(3) + 2);
        } else if (expLvl == 1) {
            person.setLoyalty(Compute.d6(3) + 1);
        } else {
            person.setLoyalty(Compute.d6(3));
        }

        // set starting education
        setInitialEducationLevel(campaign, person);

        // generate background
        BackgroundsController.generateBackground(campaign, person);

        // generate personality
        PersonalityController.generatePersonality(person);

        return person;
    }
}
