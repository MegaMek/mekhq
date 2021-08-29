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
package mekhq.campaign.personnel.marriage;

import megamek.common.Compute;
import megamek.common.annotations.Nullable;
import megamek.common.enums.Gender;
import megamek.common.util.EncodeControl;
import mekhq.MekHQ;
import mekhq.campaign.Campaign;
import mekhq.campaign.event.PersonChangedEvent;
import mekhq.campaign.log.PersonalLogger;
import mekhq.campaign.personnel.Person;
import mekhq.campaign.personnel.enums.MarriageSurnameStyle;
import mekhq.campaign.personnel.enums.RandomMarriageMethod;

import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public abstract class AbstractMarriage {
    //region Variable Declarations
    private final RandomMarriageMethod method;
    private boolean useRandomSameSexMarriages;
    private final ResourceBundle resources = ResourceBundle.getBundle("mekhq.resources.Personnel", new EncodeControl());
    //endregion Variable Declarations

    //region Constructors
    protected AbstractMarriage(final RandomMarriageMethod method,
                               final boolean useRandomSameSexMarriages) {
        this.method = method;
        setUseRandomSameSexMarriages(useRandomSameSexMarriages);
    }
    //endregion Constructors

    //region Getters
    public RandomMarriageMethod getMethod() {
        return method;
    }

    public boolean isUseRandomSameSexMarriages() {
        return useRandomSameSexMarriages;
    }

    public void setUseRandomSameSexMarriages(final boolean useRandomSameSexMarriages) {
        this.useRandomSameSexMarriages = useRandomSameSexMarriages;
    }
    //endregion Getters

    public void marry(final Campaign campaign, final LocalDate today, final Person origin,
                      final @Nullable Person spouse, final MarriageSurnameStyle surnameStyle) {
        if (spouse == null) {
            return;
        }

        // Immediately set both Maiden Names, to avoid any divorce bugs (as the default is now an empty string)
        origin.setMaidenName(origin.getSurname());
        spouse.setMaidenName(spouse.getSurname());

        // Then add them as spouses
        origin.getGenealogy().setSpouse(spouse);
        spouse.getGenealogy().setSpouse(origin);

        // Apply the surname style changes
        surnameStyle.apply(campaign, today, origin, spouse);

        // Do the logging
        PersonalLogger.marriage(origin, spouse, today);
        PersonalLogger.marriage(spouse, origin, today);

        campaign.addReport(String.format(resources.getString("marriage.report"), origin.getHyperlinkedName(),
                spouse.getHyperlinkedName()));

        // And finally we trigger person changed events
        MekHQ.triggerEvent(new PersonChangedEvent(origin));
        MekHQ.triggerEvent(new PersonChangedEvent(spouse));
    }

    /**
     * Determines if a person is old enough to marry
     * @param campaign the campaign the person is a part of
     * @param today the current day
     * @param person the person to determine if they are old enough
     * @return true if they are, otherwise false
     */
    public boolean oldEnoughToMarry(final Campaign campaign, final LocalDate today, final Person person) {
        return person.getAge(today) >= campaign.getCampaignOptions().getMinimumMarriageAge();
    }

    /**
     * Determines if the potential spouse is a safe spouse for a person
     * @param campaign the campaign to check using
     * @param today the current day
     * @param person the person trying to marry
     * @param potentialSpouse the person to determine if they are a safe spouse
     * @return true if the potential spouse is a safe spouse for the provided person
     */
    public boolean safeSpouse(final Campaign campaign, final LocalDate today, final Person person,
                              final Person potentialSpouse) {
        // Huge convoluted return statement, with the following restrictions
        // can't marry yourself
        // can't marry someone who is already married
        // can't marry someone who doesn't want to be married
        // can't marry a prisoner, unless you are also a prisoner (this is purposely left open for prisoners to marry who they want)
        // can't marry a person who is dead or MIA
        // can't marry inactive personnel (this is to show how they aren't part of the force anymore)
        // TODO : can't marry anyone who is not located at the same planet as the person - GitHub #1672: Implement current planet tracking for personnel
        // can't marry a close relative
        return (
                !person.equals(potentialSpouse)
                        && !potentialSpouse.getGenealogy().hasSpouse()
                        && potentialSpouse.isMarriageable()
                        && oldEnoughToMarry(campaign, today, potentialSpouse)
                        && (!potentialSpouse.getPrisonerStatus().isPrisoner() || person.getPrisonerStatus().isPrisoner())
                        && !potentialSpouse.getStatus().isDeadOrMIA()
                        && potentialSpouse.getStatus().isActive()
                        && !person.getGenealogy().checkMutualAncestors(potentialSpouse, campaign.getCampaignOptions().getCheckMutualAncestorsDepth())
        );
    }

    //region New Day
    /**
     * Process new day random marriage for an individual
     * @param campaign the campaign to process
     * @param today the current day
     * @param person the person to process
     */
    public void processNewDay(final Campaign campaign, final LocalDate today, final Person person) {
        // Don't attempt to generate is someone isn't marriageable, has a spouse, isn't old enough
        // to marry, or is actively deployed
        if (!person.isMarriageable() || person.getGenealogy().hasSpouse()
                || !oldEnoughToMarry(campaign, today, person) || person.isDeployed()) {
            return;
        }

        if (randomMarriage(person)) {
            addRandomSpouse(campaign, today, person, false);
        } else if (isUseRandomSameSexMarriages() && randomSameSexMarriage(person)) {
            addRandomSpouse(campaign, today, person, true);
        }
    }

    //region Random Marriage
    protected abstract boolean randomMarriage(final Person person);

    protected abstract boolean randomSameSexMarriage(final Person person);

    private void addRandomSpouse(final Campaign campaign, final LocalDate today,
                                 final Person person, final boolean sameSex) {
        final Gender gender = sameSex ? person.getGender() : (person.getGender().isMale() ? Gender.FEMALE : Gender.MALE);
        final List<Person> potentials = campaign.getActivePersonnel().stream()
                .filter(potentialSpouse -> isPotentialRandomSpouse(campaign, today, person, potentialSpouse, gender))
                .collect(Collectors.toList());
        if (!potentials.isEmpty()) {
            marry(campaign, today, person, potentials.get(Compute.randomInt(potentials.size())),
                    MarriageSurnameStyle.WEIGHTED);
        }
    }

    private boolean isPotentialRandomSpouse(final Campaign campaign, final LocalDate today,
                                            final Person person, final Person potentialSpouse,
                                            final Gender gender) {
        if ((potentialSpouse.getGender() != gender)
                || !safeSpouse(campaign, today, person, potentialSpouse)
                || !(person.getPrisonerStatus().isFree()
                || (person.getPrisonerStatus().isPrisoner() && potentialSpouse.getPrisonerStatus().isPrisoner()))) {
            return false;
        }

        final int ageDifference = Math.abs(potentialSpouse.getAge(today) - person.getAge(today));
        return ageDifference <= campaign.getCampaignOptions().getRandomMarriageAgeRange();
    }
    //endregion Random Marriage
    //endregion New Day
}
