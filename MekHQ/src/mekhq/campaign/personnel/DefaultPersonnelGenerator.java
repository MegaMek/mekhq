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

import mekhq.campaign.Campaign;

public class DefaultPersonnelGenerator extends AbstractPersonnelGenerator {

    private final String factionCode;

    public DefaultPersonnelGenerator() {
        this(null);
    }

    public DefaultPersonnelGenerator(String factionCode) {
        this.factionCode = factionCode;
    }

    @Override
    public Person generate(Campaign campaign, int primaryRole) {
        return generate(campaign, primaryRole, Person.T_NONE);
    }

    @Override
    public Person generate(Campaign campaign, int primaryRole, int secondaryRole) {
        Person person = new Person(campaign, this.factionCode != null ? this.factionCode : campaign.getFactionCode());

        person.setPrimaryRole(primaryRole);
        person.setSecondaryRole(secondaryRole);

        generateName(campaign, person);

        int expLvl = calculateExperienceLevel(campaign, person);

        generateXp(campaign, person, expLvl);

        generatePhenotype(campaign, person, expLvl);

        generateBirthday(campaign, person, expLvl, person.isClanner() && person.getPhenotype() != Person.PHENOTYPE_NONE);

        AbstractSkillGenerator skillGenerator = new DefaultSkillGenerator(campaign.getCampaignOptions());
        skillGenerator.generateSkills(person, expLvl);

        AbstractSpecialAbilityGenerator specialAbilityGenerator = new DefaultSpecialAbilityGenerator(campaign.getCampaignOptions());
        specialAbilityGenerator.generateSpecialAbilities(person, expLvl);

        //
        // Miscellaneous (after skills and SPAs)
        //

        //check for Bloodname
        if (person.isClanner()) {
            campaign.checkBloodnameAdd(person, primaryRole, person.getOriginFaction().getShortName());
        }

        person.setDaysToWaitForHealing(campaign.getCampaignOptions().getNaturalHealingWaitingPeriod());

        return person;
    }
}
