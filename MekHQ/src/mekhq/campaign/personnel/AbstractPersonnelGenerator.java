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
package mekhq.campaign.personnel;

import java.util.Calendar;
import java.util.GregorianCalendar;

import megamek.client.RandomNameGenerator;
import megamek.common.Compute;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.RandomSkillPreferences;

/**
 * Represents a class which can generate new {@link Person} objects
 * for a {@link Campaign}.
 */
public abstract class AbstractPersonnelGenerator {

    private final RandomNameGenerator randomNameGenerator = new RandomNameGenerator();
    private final RandomSkillPreferences rskillPrefs = new RandomSkillPreferences();

    /**
     * Gets the {@link RandomNameGenerator}.
     * @return The {@link RandomNameGenerator} to use.
     */
    protected RandomNameGenerator getRNG() {
        return randomNameGenerator;
    }

    /**
     * Gets the {@link RandomSkillPreferences}.
     * @return The {@link RandomSkillPreferences} to use.
     */
    protected RandomSkillPreferences getSkillPreferences() {
        return rskillPrefs;
    }

    /**
     * Generates a new {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param primaryRole The primary role of the person.
     * @return A new {@link Person}.
     */
    public abstract Person generate(Campaign campaign, int primaryRole);

    /**
     * Generates a new {@link Person}.
     * @param campaign The {@link Campaign} which tracks the person.
     * @param primaryRole The primary role of the person.
     * @param secondaryRole The secondary role of the person.
     * @return A new {@link Person}.
     */
    public abstract Person generate(Campaign campaign, int primaryRole, int secondaryRole);


    protected int calculateExperienceLevel(Campaign campaign, Person person) {
        int bonus = getSkillPreferences().getOverallRecruitBonus()
                + getSkillPreferences().getRecruitBonus(person.getPrimaryRole());

        // LAM pilots get +3 to random experience roll
        if ((person.getPrimaryRole() == Person.T_MECHWARRIOR) && (person.getSecondaryRole() == Person.T_AERO_PILOT)) {
            bonus += 3;
        }

        return Utilities.generateExpLevel(bonus);
    }

    protected void generateName(Campaign campaign, Person person) {
        boolean isFemale = getRNG().isFemale();
        if (isFemale) {
            person.setGender(Person.G_FEMALE);
        }
        person.setName(getRNG().generate(isFemale));
    }

    protected void generateXp(Campaign campaign, Person person, int expLvl) {
        if (campaign.getCampaignOptions().useDylansRandomXp()) {
            person.setXp(Utilities.generateRandomExp());
        }
    }

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

    protected void generateBirthday(Campaign campaign, Person person, int expLvl, boolean isClanner) {
        GregorianCalendar birthdate = (GregorianCalendar) campaign.getCalendar().clone();
        birthdate.set(Calendar.YEAR, birthdate.get(Calendar.YEAR) - Utilities.getAgeByExpLevel(expLvl, isClanner));

        // choose a random day and month
        int nDays = 365;
        if (birthdate.isLeapYear(birthdate.get(Calendar.YEAR))) {
            nDays = 366;
        }

        int randomDay = Compute.randomInt(nDays) + 1;
        birthdate.set(Calendar.DAY_OF_YEAR, randomDay);

        person.setBirthday(birthdate);
    }
}
