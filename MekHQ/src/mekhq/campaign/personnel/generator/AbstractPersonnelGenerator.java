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

import java.time.LocalDate;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Objects;

import megamek.client.RandomNameGenerator;
import megamek.common.Compute;
import megamek.common.Crew;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.SkillType;

/**
 * Represents a class which can generate new {@link Person} objects
 * for a {@link Campaign}.
 */
public abstract class AbstractPersonnelGenerator {
    private RandomNameGenerator randomNameGenerator = RandomNameGenerator.getInstance();

    private RandomSkillPreferences rSkillPrefs = new RandomSkillPreferences();

    /**
     * Gets the {@link RandomNameGenerator}.
     * @return The {@link RandomNameGenerator} to use.
     */
    public RandomNameGenerator getNameGenerator() {
        return randomNameGenerator;
    }

    /**
     * Sets the {@link RandomNameGenerator}.
     * @param rng A {@link RandomNameGenerator} to use.
     */
    public void setNameGenerator(RandomNameGenerator rng) {
        randomNameGenerator = Objects.requireNonNull(rng);
    }

    /**
     * Gets the {@link RandomSkillPreferences}.
     * @return The {@link RandomSkillPreferences} to use.
     */
    public RandomSkillPreferences getSkillPreferences() {
        return rSkillPrefs;
    }

    /**
     * Sets the {@link RandomSkillPreferences}.
     * @param skillPreferences A {@link RandomSkillPreferences} to use.
     */
    public void setSkillPreferences(RandomSkillPreferences skillPreferences) {
        rSkillPrefs = Objects.requireNonNull(skillPreferences);
    }

    /**
     * Generates a new {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param primaryRole The primary role of the person.
     * @param secondaryRole The secondary role of the person.
     * @param gender The person's gender, or a randomize value
     * @return A new {@link Person}.
     */
    public abstract Person generate(Campaign campaign, int primaryRole, int secondaryRole, int gender);

    /**
     * Creates a {@link Person} object for the given {@link Campaign}.
     * @param campaign The {@link Campaign} to create the person within.
     * @return A new {@link Person} object for the given campaign.
     */
    protected Person createPerson(Campaign campaign) {
        return new Person(campaign, campaign.getFactionCode());
    }

    /**
     * Generates an experience level for a {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param person The {@link Person} being generated.
     * @return An integer value between {@link SkillType#EXP_ULTRA_GREEN} and {@link SkillType#EXP_ELITE}.
     */
    protected int generateExperienceLevel(Campaign campaign, Person person) {
        int bonus = getSkillPreferences().getOverallRecruitBonus()
                + getSkillPreferences().getRecruitBonus(person.getPrimaryRole());

        // LAM pilots get +3 to random experience roll
        if ((person.getPrimaryRole() == Person.T_MECHWARRIOR) && (person.getSecondaryRole() == Person.T_AERO_PILOT)) {
            bonus += 3;
        }

        return Utilities.generateExpLevel(bonus);
    }

    /**
     * Generates a name for a {@link Person}.
     * @param person The {@link Person} being generated.
     * @param gender The person's gender, or a randomize value
     */
    protected void generateName(Person person, int gender) {
        boolean isFemale = gender == Crew.G_RANDOMIZE
                ? getNameGenerator().isFemale()
                : gender == Crew.G_FEMALE;

        if (isFemale) {
            person.setGender(Crew.G_FEMALE);
        }

        String[] name = getNameGenerator().generateGivenNameSurnameSplit(isFemale, person.isClanner(),
                person.getOriginFaction().getShortName());
        person.setGivenName(name[0]);
        person.setSurname(name[1]);
    }

    /**
     * Generates the starting XP for a {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param person The {@link Person} being generated.
     * @param expLvl The experience level of {@code person}.
     */
    protected void generateXp(Campaign campaign, Person person, int expLvl) {
        if (campaign.getCampaignOptions().useDylansRandomXp()) {
            person.setXp(Utilities.generateRandomExp());
        }
    }

    /**
     * Generates the clan phenotype, if applicable, for a {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param person The {@link Person} being generated.
     * @param expLvl The experience level of {@code person}.
     */
    protected void generatePhenotype(Campaign campaign, Person person, int expLvl) {
        //check for clan phenotypes
        if (person.isClanner()) {
            switch (person.getPrimaryRole()) {
                case (Person.T_MECHWARRIOR):
                    if (Utilities.rollProbability(campaign.getCampaignOptions().getProbPhenoMW())) {
                        person.setPhenotype(Person.PHENOTYPE_MW);
                    }
                    break;
                case (Person.T_GVEE_DRIVER):
                case (Person.T_NVEE_DRIVER):
                case (Person.T_VTOL_PILOT):
                case (Person.T_VEE_GUNNER):
                    if (Utilities.rollProbability(campaign.getCampaignOptions().getProbPhenoVee())) {
                        person.setPhenotype(Person.PHENOTYPE_VEE);
                    }
                    break;
                case (Person.T_CONV_PILOT):
                case (Person.T_AERO_PILOT):
                case (Person.T_PROTO_PILOT):
                    if (Utilities.rollProbability(campaign.getCampaignOptions().getProbPhenoAero())) {
                        person.setPhenotype(Person.PHENOTYPE_AERO);
                    }
                    break;
                case (Person.T_BA):
                    if (Utilities.rollProbability(campaign.getCampaignOptions().getProbPhenoBA())) {
                        person.setPhenotype(Person.PHENOTYPE_BA);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Generates the birthday for a {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param person The {@link Person} being generated.
     * @param expLvl The experience level of {@code person}.
     * @param isClanner A value indicating if {@code person} is a clanner.
     */
    protected void generateBirthday(Campaign campaign, Person person, int expLvl, boolean isClanner) {
        LocalDate birthday = campaign.getLocalDate();
        birthday = birthday.minusYears(Utilities.getAgeByExpLevel(expLvl, isClanner));

        // choose a random day and month
        int nDays = 365;
        if (birthday.isLeapYear()) {
            nDays = 366;
        }

        int randomDay = Compute.randomInt(nDays) + 1;
        person.setBirthday(birthday.withDayOfYear(randomDay));
    }
}
