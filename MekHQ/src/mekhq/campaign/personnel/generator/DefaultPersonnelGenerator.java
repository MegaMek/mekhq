/*
 * Copyright (C) 2019-2025 The MegaMek Team. All Rights Reserved.
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
package mekhq.campaign.personnel.generator;

import static megamek.common.compute.Compute.randomInt;
import static mekhq.campaign.personnel.education.EducationController.setInitialEducationLevel;
import static mekhq.campaign.personnel.skills.Aging.updateAllSkillAgeModifiers;
import static mekhq.campaign.personnel.skills.SkillType.EXP_NONE;

import java.util.Objects;

import megamek.common.compute.Compute;
import megamek.common.enums.Gender;
import mekhq.campaign.Campaign;
import mekhq.campaign.campaignOptions.CampaignOptions;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.PersonnelOptions;
import mekhq.campaign.personnel.SpecialAbility;
import mekhq.campaign.personnel.backgrounds.BackgroundsController;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.randomEvents.personalities.PersonalityController;
import mekhq.campaign.universe.Faction;
import mekhq.campaign.universe.Planet;
import mekhq.campaign.universe.selectors.factionSelectors.AbstractFactionSelector;
import mekhq.campaign.universe.selectors.planetSelectors.AbstractPlanetSelector;

/**
 * Creates {@link Person} instances using the default MekHQ algorithm.
 */
public class DefaultPersonnelGenerator extends AbstractPersonnelGenerator {


    private final AbstractFactionSelector factionSelector;
    private final AbstractPlanetSelector planetSelector;

    /**
     * Creates a new DefaultPersonGenerator with a faction selector.
     *
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
        CampaignOptions campaignOptions = campaign.getCampaignOptions();
        Person person = createPerson(campaign);

        person.setPrimaryRoleDirect(primaryRole);
        person.setSecondaryRoleDirect(secondaryRole);
        int expLvl = generateExperienceLevel(person);

        generateXp(campaign, person);

        generatePhenotype(campaign, person);

        generateBirthday(campaign, person, expLvl, person.isClanPersonnel() && !person.getPhenotype().isNone());

        AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(getSkillPreferences());

        skillGenerator.generateSkills(campaign, person, expLvl);
        skillGenerator.generateAttributes(person, campaignOptions.isUseEdge());
        skillGenerator.generateTraits(person);

        // Limit skills by age for children and adolescents
        int age = person.getAge(campaign.getLocalDate());

        if (age < 16) {
            person.removeAllSkills();
            expLvl = EXP_NONE;
            person.setPrimaryRole(campaign.getLocalDate(), PersonnelRole.DEPENDENT);
        } else if (age < 18) {
            person.limitSkills(1);
            // regenerate expLvl to factor in skill changes from age
            expLvl = person.getExperienceLevel(campaign, false);
        }

        // set SPAs
        if (expLvl >= EXP_ULTRA_GREEN) {
            AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator();
            specialAbilityGenerator.setSkillPreferences(new RandomSkillPreferences());
            specialAbilityGenerator.generateSpecialAbilities(campaign, person, expLvl);
        }

        // Set relationship flags
        determineOrientation(person, campaignOptions.getNoInterestInRelationshipsDiceSize(),
              campaignOptions.getInterestedInSameSexDiceSize(), campaignOptions.getInterestedInBothSexesDiceSize());

        int interestInChildren = campaignOptions.getNoInterestInChildrenDiceSize();
        person.setTryingToConceive(((interestInChildren != 0) && (randomInt(interestInChildren)) != 0));

        // Do naming at the end, to ensure the keys are set
        generateNameAndGender(campaign, person, gender);

        //check for Bloodname
        campaign.checkBloodnameAdd(person, false);

        if (person.getOriginFaction().isClan() &&
                  campaignOptions.isUseAbilities() &&
                  !(person.getPrimaryRole().isSoldierOrBattleArmour() || person.getPrimaryRole().isProtoMekPilot())) {
            if (SpecialAbility.getSpecialAbilities().containsKey("clan_pilot_training")) {
                PersonnelOptions personnelOptions = person.getOptions();
                personnelOptions.acquireAbility(PersonnelOptions.LVL3_ADVANTAGES, "clan_pilot_training", true);
            }
        }

        person.setDaysToWaitForHealing(campaignOptions.getNaturalHealingWaitingPeriod());

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

        // update skill age modifiers
        if (campaignOptions.isUseAgeEffects()) {
            updateAllSkillAgeModifiers(campaign.getLocalDate(), person);
        }

        return person;
    }

    /**
     * Determines a person's sexual orientation based on weighted random rolls.
     *
     * <p>Orientations are checked in priority order (no interest → same-sex → both sexes → opposite-sex). The first
     * successful roll determines the orientation, ensuring mutual exclusivity.</p>
     *
     * <p>Each dice size parameter represents the probability denominator for that orientation:
     * a value of {@code 0} means the orientation never occurs, {@code 1} means it always occurs, and higher values
     * reduce the probability (e.g., {@code 100} gives a 1% chance).</p>
     *
     * @param person                            the {@link Person} whose orientation is being determined
     * @param noInterestInRelationshipsDiceSize dice size for aromantic/asexual orientation (rolled first)
     * @param interestedInSameSexDiceSize       dice size for homosexual orientation (rolled second)
     * @param interestedInBothSexesDiceSize     dice size for bisexual/pansexual orientation (rolled third)
     */
    public static void determineOrientation(Person person, int noInterestInRelationshipsDiceSize,
          int interestedInSameSexDiceSize, int interestedInBothSexesDiceSize) {
        boolean isMale = person.getGender().isMale();

        // Based on default campaign options:
        // Aromantic (die size 100): 1.00%
        // Homosexual (die size 14): 7.07%
        // Bisexual (die size 33): 2.82%
        // Heterosexual: 89.11%

        // Check orientations in priority order - first match wins
        if (rollsOrientation(noInterestInRelationshipsDiceSize)) {
            // Aromantic/Asexual
            person.setPrefersMen(false);
            person.setPrefersWomen(false);
        } else if (rollsOrientation(interestedInSameSexDiceSize)) {
            // Homosexual
            person.setPrefersMen(isMale);
            person.setPrefersWomen(!isMale);
        } else if (rollsOrientation(interestedInBothSexesDiceSize)) {
            // Bisexual/Pansexual
            person.setPrefersMen(true);
            person.setPrefersWomen(true);
        } else {
            // Heterosexual
            person.setPrefersMen(!isMale);
            person.setPrefersWomen(isMale);
        }
    }

    /**
     * Performs a weighted random roll to determine if an orientation applies.
     *
     * @param diceSize the size of the die (0 means never applies, 1 means always applies)
     *
     * @return {@code true} if the roll succeeds (rolls a 1), {@code false} otherwise
     */
    private static boolean rollsOrientation(int diceSize) {
        return (diceSize != 0) && (Compute.randomInt(diceSize) == 0);
    }
}
