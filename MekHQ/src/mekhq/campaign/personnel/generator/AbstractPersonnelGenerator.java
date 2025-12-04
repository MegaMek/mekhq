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

import java.time.LocalDate;
import java.util.Objects;

import megamek.client.generator.RandomGenderGenerator;
import megamek.client.generator.RandomNameGenerator;
import megamek.common.enums.Gender;
import mekhq.Utilities;
import mekhq.campaign.Campaign;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.PersonnelRole;
import mekhq.campaign.personnel.enums.Phenotype;
import mekhq.campaign.personnel.skills.RandomSkillPreferences;
import mekhq.campaign.personnel.skills.SkillType;

/**
 * Represents a class which can generate new {@link Person} objects for a {@link Campaign}.
 */
public abstract class AbstractPersonnelGenerator {

    private RandomNameGenerator randomNameGenerator = RandomNameGenerator.getInstance();

    private RandomSkillPreferences rSkillPrefs = new RandomSkillPreferences();

    /**
     * Gets the {@link RandomNameGenerator}.
     *
     * @return The {@link RandomNameGenerator} to use.
     */
    public RandomNameGenerator getNameGenerator() {
        return randomNameGenerator;
    }

    /**
     * Sets the {@link RandomNameGenerator}.
     *
     * @param rng A {@link RandomNameGenerator} to use.
     */
    public void setNameGenerator(RandomNameGenerator rng) {
        randomNameGenerator = Objects.requireNonNull(rng);
    }

    /**
     * Gets the {@link RandomSkillPreferences}.
     *
     * @return The {@link RandomSkillPreferences} to use.
     */
    public RandomSkillPreferences getSkillPreferences() {
        return rSkillPrefs;
    }

    /**
     * Sets the {@link RandomSkillPreferences}.
     *
     * @param skillPreferences A {@link RandomSkillPreferences} to use.
     */
    public void setSkillPreferences(RandomSkillPreferences skillPreferences) {
        rSkillPrefs = Objects.requireNonNull(skillPreferences);
    }

    /**
     * Generates a new {@link Person}.
     *
     * @param campaign      The {@link Campaign} which tracks the person.
     * @param primaryRole   The primary role of the person.
     * @param secondaryRole The secondary role of the person.
     * @param gender        The person's gender, or a randomize value
     *
     * @return A new {@link Person}.
     */
    public abstract Person generate(Campaign campaign, PersonnelRole primaryRole, PersonnelRole secondaryRole,
          Gender gender);

    /**
     * Creates a {@link Person} object for the given {@link Campaign}.
     *
     * @param campaign The {@link Campaign} to create the person within.
     *
     * @return A new {@link Person} object for the given campaign.
     */
    protected Person createPerson(Campaign campaign) {
        return new Person(campaign, campaign.getFaction().getShortName());
    }

    /**
     * Generates an experience level for a {@link Person}.
     *
     * @param person The {@link Person} being generated.
     *
     * @return An integer value between {@link SkillType#EXP_ULTRA_GREEN} and {@link SkillType#EXP_ELITE}.
     */
    public int generateExperienceLevel(Person person) {
        int bonus = rSkillPrefs.getOverallRecruitBonus() + rSkillPrefs.getRecruitmentBonus(person.getPrimaryRole());

        // LAM pilots get +3 to random experience roll
        if (person.getPrimaryRole().isLAMPilot()) {
            bonus += 3;
        }

        return Utilities.generateExpLevel(bonus);
    }

    /**
     * Generates and sets the name and gender of a person.
     *
     * @param campaign the campaign the person belongs to
     * @param person   the person whose name and gender is being generated
     * @param gender   the gender of the person. Can be Gender.MALE, Gender.FEMALE, Gender.NON_BINARY, or
     *                 Gender.RANDOMIZE
     */
    protected void generateNameAndGender(Campaign campaign, Person person, Gender gender) {
        int nonBinaryDiceSize = campaign.getCampaignOptions().getNonBinaryDiceSize();

        if (gender != Gender.RANDOMIZE) {
            person.setGender(gender);
        } else {
            if (nonBinaryDiceSize > 0 && randomInt(nonBinaryDiceSize) == 0) {
                person.setGender(RandomGenderGenerator.generateOther());
            } else {
                person.setGender(RandomGenderGenerator.generate());
            }
        }

        String factionCode = campaign.getCampaignOptions().isUseOriginFactionForNames() ?
                                   person.getOriginFaction().getShortName() :
                                   RandomNameGenerator.getInstance().getChosenFaction();

        String[] name = getNameGenerator().generateGivenNameSurnameSplit(person.getGender(),
              person.isClanPersonnel(),
              factionCode);
        person.setGivenName(name[0]);
        person.setSurname(name[1]);
    }

    /**
     * Generates the starting XP for a {@link Person}.
     *
     * @param campaign The {@link Campaign} which tracks the person.
     * @param person   The {@link Person} being generated.
     */
    protected void generateXp(Campaign campaign, Person person) {
        if (campaign.getCampaignOptions().isUseDylansRandomXP()) {
            person.setXP(campaign, Utilities.generateRandomExp());
        }
    }

    /**
     * Generates the clan phenotype, if applicable, for a {@link Person}.
     *
     * @param campaign The {@link Campaign} which tracks the person.
     * @param person   The {@link Person} being generated.
     */
    protected void generatePhenotype(Campaign campaign, Person person) {
        //check for clan phenotypes
        if (person.isClanPersonnel()) {
            switch (person.getPrimaryRole()) {
                case MEKWARRIOR, LAM_PILOT -> {
                    if (Utilities.rollProbability(campaign.getCampaignOptions()
                                                        .getPhenotypeProbability(Phenotype.MEKWARRIOR))) {
                        person.setPhenotype(Phenotype.MEKWARRIOR);
                    }
                }
                case VEHICLE_CREW_GROUND, VEHICLE_CREW_NAVAL, VEHICLE_CREW_VTOL -> {
                    if (person.getOriginFaction().getShortName().equalsIgnoreCase("CHH") &&
                              (campaign.getGameYear() >= 3100) &&
                              Utilities.rollProbability(campaign.getCampaignOptions()
                                                              .getPhenotypeProbability(Phenotype.VEHICLE))) {
                        person.setPhenotype(Phenotype.VEHICLE);
                    }
                }
                case AEROSPACE_PILOT, CONVENTIONAL_AIRCRAFT_PILOT -> {
                    if (Utilities.rollProbability(campaign.getCampaignOptions()
                                                        .getPhenotypeProbability(Phenotype.AEROSPACE))) {
                        person.setPhenotype(Phenotype.AEROSPACE);
                    }
                }
                case PROTOMEK_PILOT -> {
                    if ((campaign.getGameYear() > 3060) &&
                              Utilities.rollProbability(campaign.getCampaignOptions()
                                                              .getPhenotypeProbability(Phenotype.PROTOMEK))) {
                        person.setPhenotype(Phenotype.PROTOMEK);
                    }
                }
                case BATTLE_ARMOUR -> {
                    if (Utilities.rollProbability(campaign.getCampaignOptions()
                                                        .getPhenotypeProbability(Phenotype.ELEMENTAL))) {
                        person.setPhenotype(Phenotype.ELEMENTAL);
                    }
                }
                case VESSEL_PILOT, VESSEL_GUNNER, VESSEL_CREW, VESSEL_NAVIGATOR -> {
                    if ((person.getOriginFaction().getShortName().equalsIgnoreCase("CSR") ||
                               person.getOriginFaction().getShortName().equalsIgnoreCase("RA")) &&
                              Utilities.rollProbability(campaign.getCampaignOptions()
                                                              .getPhenotypeProbability(Phenotype.NAVAL))) {
                        person.setPhenotype(Phenotype.NAVAL);
                    }
                }
                default -> {}
            }
        }
    }

    /**
     * Assigns a realistic date of birth to the specified {@link Person} using their experience level and affiliation.
     *
     * <p>This method determines the person's approximate age based on their experience, then calculates a date of
     * birth ensuring the resulting age matches the campaign's timeline. The generated birthday always falls on or
     * before the current campaign date, guaranteeing age accuracy.</p>
     *
     * @param campaign        The current campaign, used to obtain the reference date for calculation.
     * @param person          The person whose birthday is being set.
     * @param expLvl          Numeric experience level used to determine age.
     * @param isClanPersonnel Whether the person is affiliated with a Clan, modifying age calculation rules.
     */
    protected void generateBirthday(Campaign campaign, Person person, int expLvl, boolean isClanPersonnel) {
        LocalDate currentDate = campaign.getLocalDate();
        int age = Utilities.getAgeByExpLevel(expLvl, isClanPersonnel);

        // Generate an initial birthday
        int year = campaign.getGameYear();
        int daysInYear = 365; // We ignore leap years for simplicity's sake
        int randomDay = randomInt(daysInYear) + 1; // random int from 1 to daysInYear
        LocalDate birthday = LocalDate.ofYearDay(year, randomDay);

        // Subtract age to get the target year
        birthday = birthday.minusYears(age);

        // Constrain the random day to ensure the birthday is on or before the current date
        if (birthday.isAfter(currentDate)) {
            birthday = birthday.minusYears(1);
        }

        person.setDateOfBirth(birthday);
    }
}
